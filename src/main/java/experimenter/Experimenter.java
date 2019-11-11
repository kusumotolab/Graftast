package experimenter;

import com.github.gumtreediff.actions.EditScript;
import com.github.gumtreediff.actions.model.Action;
import com.github.gumtreediff.actions.model.Move;
import com.github.gumtreediff.tree.ITree;
import com.github.gumtreediff.utils.Pair;
import me.tongfei.progressbar.ProgressBar;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.FileMode;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.treewalk.TreeWalk;
import pgenerator.FileContainer;
import pgenerator.PGenerator;

import java.io.*;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class Experimenter {

    private final String[] args;
    private final Repository repository;
    private final List<RevCommit> commits = new LinkedList<>();

    public Experimenter(String[] args) throws IOException {
        this.args = args;
        //リポジトリを取得
        repository = new FileRepositoryBuilder()
                .findGitDir(new File(args[0]))
                .build();
    }

    public static void main(String[] args) {
        try {
            new Experimenter(args).start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void start() {
        long startTime = System.currentTimeMillis();

        try {
            setCommitLogs();
        } catch (GitAPIException e) {
            e.printStackTrace();
            return;
        }
        compareAllVersions();

        long endTime = System.currentTimeMillis();
        double time = (double)(endTime - startTime) / 1000;
        System.out.println("Calculate Time: " + time);
    }

    private void setCommitLogs() throws GitAPIException {
        final Git git = new Git(repository);
        Iterable<RevCommit> revCommits = git.log().call(); //"git log"の結果と同じ

        revCommits.forEach(commits::add);
        System.out.println("Found " + commits.size() + " commits");
    }

    private void compareAllVersions() {
        if (args.length >= 3 && args[2].equals("-m")){
            //マルチスレッド
            ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
            List<Future<?>> futureList = new ArrayList<>();
            try (ProgressBar pb = new ProgressBar("Comparing(M)", commits.size() - 1)) {
                for (int i = 1; i < commits.size(); i++) {
                    Compare compare;
                    compare = new Compare(args, repository, commits, i);
                    Future<?> future = executorService.submit(compare);
                    futureList.add(future);
                }
                executorService.shutdown();
                for (Future<?> future : futureList) {
                    try {
                        future.get();
                    } catch (InterruptedException | ExecutionException e) {
                        e.printStackTrace();
                    }
                    pb.step();
                }
            }
        } else {
            //シングルスレッド
            try (ProgressBar pb = new ProgressBar("Comparing(S)", commits.size() - 1)) {

                for (int i = 1; i < commits.size(); i++) {
                    new Compare(args, repository, commits, i).run();
                    pb.step();
                }
            }
        }
    }

}

class Compare implements Runnable {

    private final String[] args;
    private final List<RevCommit> commits;
    private final int index;
    private final Repository repository;

    Compare(String[] args,Repository repository, List<RevCommit> commits, int i) {
        this.args = args;
        this.repository = repository;
        this.commits = commits;
        index = i;
    }


    @Override
    public void run() {
        List<FileContainer> src = getFileContainers(index - 1, args[0]); //newProject
        List<FileContainer> dst = getFileContainers(index, args[0]); //oldProject

        PGenerator pGenerator = new PGenerator(src, dst);
        Pair<ITree, ITree> projectTrees;
        try {
            projectTrees = pGenerator.getProjectTreePair(src, dst, "java");
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        EditScript editScript = pGenerator.calculateEditScript(projectTrees);

        PrintWriter printLogWriter, printCSVWriter;
        try {
            String logPath = args[1] + index;
            printLogWriter = new PrintWriter(new BufferedWriter(new FileWriter(new File(logPath))));
            String csvPath = args[1] +index + ".csv";
            printCSVWriter = new PrintWriter(new BufferedWriter(new FileWriter(new File(csvPath))));
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        int[] moveCount = new int[100 + 1];
        for (Action action : editScript) {
            if (action instanceof Move) {
                Move mv = (Move)action;
                String srcFileName = pGenerator.getAffiliatedFileName(mv.getNode());
                String dstFileName = pGenerator.getAffiliatedFileName(mv.getParent());
                if (!srcFileName.equals(dstFileName)) {
                    printLogWriter.println(action.toString());
                    int size = mv.getNode().getMetrics().size;
                    if (size >= 100)
                        moveCount[100] += 1;
                    else
                        moveCount[size] += 1;
                }
            }
        }
        for(int val: moveCount) {
            printCSVWriter.println(val);
        }
        printLogWriter.close();
        printCSVWriter.close();
        //System.out.println("Done: " + index + "/" + (commits.size() - 1));
    }

    private List<FileContainer> getFileContainers(int index, String path) {
        String relativePath = path.replace(repository.getWorkTree().getAbsolutePath() , "");
        relativePath = relativePath.replace("\\", "/"); //Windows用の対策
        if (relativePath.charAt(0) == '/')
            relativePath = relativePath.replaceFirst("/", ""); //先頭の"/"を除去
        List<FileContainer> containers = new LinkedList<>();
        try (RevWalk walk = new RevWalk(repository)) {
            RevCommit commit = walk.parseCommit(commits.get(index));
            RevTree tree = walk.parseTree(commit.getTree());

            TreeWalk treeWalk = new TreeWalk(repository);
            treeWalk.addTree(tree);
            treeWalk.setRecursive(true);

            while (treeWalk.next()) {
                if (treeWalk.getFileMode().equals(FileMode.GITLINK)) //Submoduleは無視
                    continue;
                if (!treeWalk.getPathString().startsWith(relativePath)) //指定ディレクトリ以外は無視
                    continue;
                ObjectLoader loader;
                loader = repository.open(treeWalk.getObjectId(0));
                OutputStream outputStream = new ByteArrayOutputStream();
                loader.copyTo(outputStream);
                String contents = outputStream.toString();
                outputStream.close();
                containers.add(new FileContainer(treeWalk.getNameString(), contents));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return containers;
    }
}