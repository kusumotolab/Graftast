package experimenter;

import com.github.gumtreediff.actions.ChawatheScriptGenerator;
import com.github.gumtreediff.actions.EditScript;
import com.github.gumtreediff.actions.model.Action;
import com.github.gumtreediff.actions.model.Move;
import com.github.gumtreediff.matchers.MappingStore;
import com.github.gumtreediff.matchers.Matcher;
import com.github.gumtreediff.matchers.Matchers;
import com.github.gumtreediff.tree.ITree;
import com.github.gumtreediff.utils.Pair;
import graftast.FileContainer;
import graftast.GraftastMain;
import graftast.ProjectTreeGenerator;
import graftast.util.TreeUtil;
import me.tongfei.progressbar.ProgressBar;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.diff.RawTextComparator;
import org.eclipse.jgit.diff.RenameDetector;
import org.eclipse.jgit.lib.FileMode;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.util.io.NullOutputStream;

import java.io.*;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.*;

public class Experimenter3 {

    private final String[] args;
    private final Repository repository;
    private final List<RevCommit> commits = new LinkedList<>();

    public Experimenter3(String[] args) throws IOException {
        this.args = args;
        //リポジトリを取得
        repository = new FileRepositoryBuilder()
                .findGitDir(new File(args[0]))
                .build();
    }

    public static void main(String[] args) {
        try {
            new Experimenter3(args).start();
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
        List<Double> similarities = new LinkedList<>();
        if (args.length >= 3 && args[2].equals("-m")){
            //マルチスレッド
            ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
            List<Future<List<Double>>> futureList = new ArrayList<>();
            try (ProgressBar pb = new ProgressBar("Comparing(M)", commits.size() - 1)) {
                for (int i = 0; i < commits.size() - 1; i++) {
                    Compare3 compare3;
                    compare3 = new Compare3(args, repository, commits, i);
                    Future<List<Double>> future = executorService.submit(compare3);
                    futureList.add(future);
                }
                executorService.shutdown();
                for (Future<List<Double>> future : futureList) {
                    try {
                        List<Double> s = future.get();
                        similarities.addAll(s);
                    } catch (InterruptedException | ExecutionException e) {
                        e.printStackTrace();
                    }
                    pb.step();
                }
            }
        } else {
            //シングルスレッド
            try (ProgressBar pb = new ProgressBar("Comparing(S)", commits.size() - 1)) {
                for (int i = 0; i < commits.size() - 1 ; i++) {
                    List<Double> s = new Compare3(args, repository, commits, i).call();
                    similarities.addAll(s);
                    pb.step();
                }
            }
        }
        PrintWriter pw;
        String logPath = args[1] + "similarity.csv";
        try {
            pw = new PrintWriter(new BufferedWriter(new FileWriter(new File(logPath))));
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        similarities.forEach(pw::println);
        pw.close();
    }

}

class Compare3 implements Callable<List<Double>> {

    private final String[] args;
    private final List<RevCommit> commits;
    private final int index;
    private final Repository repository;
    private final RevCommit srcCommit;
    private final RevCommit dstCommit;
    private final int parentCount;
    private List<RenamedFile> reNamedFiles = new LinkedList<>();

    Compare3(String[] args, Repository repository, List<RevCommit> commits, int i) {
        this.args = args;
        this.repository = repository;
        this.commits = commits;
        index = i;
        parentCount = commits.get(index).getParentCount();
        if (parentCount == 1)
            srcCommit = commits.get(index).getParent(0);
        else
            srcCommit = commits.get(index);
        dstCommit = commits.get(index);
        setRenamedFiles(srcCommit, dstCommit);
    }


    @Override
    public List<Double> call() {
        if (parentCount != 1) //マージコミットなどはスルー
            return new LinkedList<>();
        List<FileContainer> src = getFileContainers(srcCommit, args[0]); //oldProject
        List<FileContainer> dst = getFileContainers(dstCommit, args[0]); //newProject

        GraftastMain graftastMain = new GraftastMain();
        Pair<ITree, ITree> projectTrees;
        try {
            projectTrees = new ProjectTreeGenerator(src, dst, "java").getProjectTreePair();
        } catch (IOException e) {
            e.printStackTrace();
            return new LinkedList<>();
        }

        EditScript editScript = graftastMain.calculateEditScript(projectTrees);

        PrintWriter printLogWriter;
        try {
            String logPath = args[1] + "log.txt";
            printLogWriter = new PrintWriter(new BufferedWriter(new FileWriter(new File(logPath), true)));
        } catch (IOException e) {
            e.printStackTrace();
            return new LinkedList<>();
        }

        List<Double> similarities = new LinkedList<>();
        for (Action action : editScript) {
            if (action instanceof Move) {
                Move mv = (Move)action;
                String srcFileName = TreeUtil.getAffiliatedFileName(mv.getNode());
                String dstFileName = TreeUtil.getFinalDstFile(mv, editScript);
                String dstFileNameOriginal = TreeUtil.getAffiliatedFileName(mv.getParent());
                if (!srcFileName.equals(dstFileName)) {
                    if (!isFileRenamed(srcFileName, dstFileName)) { //ファイルがリネームされただけのものではない
                        ITree srcNode = mv.getNode();
                        ITree srcNodeParent = srcNode.getParent();
                        srcNode.setParent(null);
                        ITree dstNode = mv.getParent().getChild(mv.getPosition());
                        ITree dstNodeParent = dstNode.getParent();
                        dstNode.setParent(null);

                        Matcher matcher = Matchers.getInstance().getMatcher();
                        MappingStore mappingStore = matcher.match(srcNode, dstNode);
                        EditScript actions = new ChawatheScriptGenerator().computeActions(mappingStore);
                        double similarity = 1 - ((double) actions.size() / (double) (srcNode.getMetrics().size + dstNode.getMetrics().size));
                        similarities.add(similarity);
                        if (similarity <= 0.5)
                            synchronized (commits) {
                                printLogWriter.println(index);
                                printLogWriter.println(srcFileName + " " + dstFileName + " " + dstFileNameOriginal);
                                printLogWriter.println(action.toString());
                            }
                        srcNode.setParent(srcNodeParent);
                        dstNode.setParent(dstNodeParent);
                    }
                }
            }
        }
        printLogWriter.close();
        return similarities;
        //System.out.println("Done: " + index + "/" + (commits.size() - 1));
    }

    private List<FileContainer> getFileContainers(RevCommit commit, String path) {
        String relativePath = path.replace(repository.getWorkTree().getAbsolutePath() , "");
        relativePath = relativePath.replace("\\", "/"); //Windows用の対策
        if (relativePath.length() != 0 && relativePath.charAt(0) == '/')
            relativePath = relativePath.replaceFirst("/", ""); //先頭の"/"を除去
        List<FileContainer> containers = new LinkedList<>();
        try (RevWalk walk = new RevWalk(repository)) {
            RevTree tree = walk.parseTree(commit.getTree());

            TreeWalk treeWalk = new TreeWalk(repository);
            treeWalk.addTree(tree);
            treeWalk.setRecursive(true);

            while (treeWalk.next()) {
                if (!treeWalk.getFileMode().equals(FileMode.REGULAR_FILE)) //Submoduleは無視
                    continue;
                if (!treeWalk.getPathString().startsWith(relativePath)) //指定ディレクトリ以外は無視
                    continue;
                ObjectLoader loader;
                loader = repository.open(treeWalk.getObjectId(0));
                OutputStream outputStream = new ByteArrayOutputStream();
                loader.copyTo(outputStream);
                String contents = outputStream.toString();
                outputStream.close();
                int insertIndex = getInsertIndex(containers, treeWalk.getPathString());
                containers.add(insertIndex, new FileContainer(treeWalk.getNameString(), treeWalk.getPathString(), contents));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return containers;
    }

    private int getInsertIndex(List<FileContainer> list, String str) {
        for (FileContainer fc: list) {
            if (fc.getPath().compareTo(str) > 0)
                return list.indexOf(fc);
        }
        return list.size();
    }

    private void setRenamedFiles(RevCommit srcCommit, RevCommit dstCommit) {
        DiffFormatter df = new DiffFormatter(NullOutputStream.INSTANCE);
        df.setRepository(repository);
        df.setDiffComparator(RawTextComparator.DEFAULT);
        df.setDetectRenames(true);
        try {
            List<DiffEntry> diffEntries = df.scan(srcCommit.getTree(), dstCommit.getTree());
            RenameDetector renameDetector = new RenameDetector(repository);
            renameDetector.addAll(diffEntries);
            renameDetector.setRenameScore(30);
            List<DiffEntry> compute = renameDetector.compute();
            for (DiffEntry diffEntry: compute) {
                if (diffEntry.getChangeType() == DiffEntry.ChangeType.RENAME || diffEntry.getChangeType() == DiffEntry.ChangeType.COPY) {
                    String oldPath = diffEntry.getOldPath();
                    String newPath = diffEntry.getNewPath();
                    reNamedFiles.add(new RenamedFile(oldPath, newPath));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private boolean isFileRenamed(String src, String dst) {
        for (RenamedFile r: reNamedFiles) {
            if (r.getOldName().endsWith(src) && r.getNewName().endsWith(dst))
                return true;
        }
        return false;
    }

    class RenamedFile {
        final String oldName;
        final String newName;

        RenamedFile(String oldName, String newName) {
            this.oldName = oldName;
            this.newName = newName;
        }

        String getOldName() {
            return oldName;
        }

        String getNewName() {
            return newName;
        }
    }
}