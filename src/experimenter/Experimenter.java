package experimenter;

import com.github.gumtreediff.actions.EditScript;
import com.github.gumtreediff.actions.model.Action;
import com.github.gumtreediff.actions.model.Move;
import com.github.gumtreediff.tree.ITree;
import com.github.gumtreediff.utils.Pair;
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

    private List<String> commitLogs = new LinkedList<>();
    private int commitLogSize;

    public Experimenter() {}

    public static void main(String[] args) {
        new Experimenter().start(args);
    }

    public void start(String[] args) {
        long startTime = System.currentTimeMillis();

        setCommitLogs(args[1]);
        compareAllVersions(args);

        long endTime = System.currentTimeMillis();
        double time = (double)(endTime - startTime) / 1000;
        System.out.println("Calculate Time: " + time);
    }

    private void setCommitLogs(String path) {
        //Git 1.8.5以降
        String[] gitCommand = {"git", "-C", path, "log", "--pretty=format:%H"}; //https://qiita.com/harukasan/items/9149542584385e8dea75 より
        Process process;
        ProcessBuilder processBuilder = new ProcessBuilder(gitCommand);
        try {
            process = processBuilder.start();
            processBuilder.redirectErrorStream(true);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        try {
            InputStream is = process.getInputStream();
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            String str;
            while ((str = br.readLine()) != null) {
                commitLogs.add(str);
            }
        } catch (IOException | NullPointerException e) {
            e.printStackTrace();
        }
        commitLogSize = commitLogs.size();
        System.out.println("Found " + commitLogSize + "commits");
    }

    private void compareAllVersions(String[] args) {
        if (args.length >= 6 && args[5].equals("-m")){
            //マルチスレッド
            ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
            List<Future<?>> futureList = new ArrayList<>();
            for (int i = commitLogs.size() - 1; i > 0; i--) {
                Compare compare = new Compare(args, commitLogs, i);
                Future<?> future = executorService.submit(compare);
                futureList.add(future);
            }
            executorService.shutdown();
            for (Future<?> future : futureList) {
                try {
                    future.get();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (ExecutionException e) {
                    e.printStackTrace();
                }
            }
        } else {
            //シングルスレッド
            for (int i = commitLogs.size() - 1; i > 0; i--) {
                new Compare(args, commitLogs, i).run();
            }
        }
    }


}

class Compare implements Runnable {

    private final String[] args;
    private final List<String> commitLogs;
    private final int commitLogSize;
    private final int index;

    Compare(String[] args, List<String> commitLogs, int i) {
        this.args = args;
        this.commitLogs = commitLogs;
        commitLogSize = commitLogs.size();
        index = i;
    }


    @Override
    public void run() {
        PGenerator pGenerator;
        Pair<ITree, ITree> projectTrees;
        synchronized (commitLogs) {
            try {
                Runtime runtime = Runtime.getRuntime();
                Process process;
                String[] checkoutOld = {"git", "-C", args[0], "checkout", commitLogs.get(index)};
                process = runtime.exec(checkoutOld);
                process.waitFor();
                String[] checkoutNew = {"git", "-C", args[1], "checkout", commitLogs.get(index - 1)};
                process = runtime.exec(checkoutNew);
                process.waitFor();
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
                return;
            }

            pGenerator = new PGenerator(args[3], args[4]);
            try {
                projectTrees = pGenerator.getProjectTreePair(args[3], args[4], "java");
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }
        }

        EditScript editScript = pGenerator.calculateEditScript(projectTrees);

        int num = commitLogSize - index;
        PrintWriter printLogWriter, printCSVWriter;
        try {
            String logPath = args[2] + num;
            printLogWriter = new PrintWriter(new BufferedWriter(new FileWriter(new File(logPath))));
            String csvPath = args[2] + num + ".csv";
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
        System.out.println("Done: " + (commitLogSize - index) + "/" + (commitLogSize - 1));
    }
}