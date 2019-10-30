package experimenter;

import com.github.gumtreediff.actions.ChawatheScriptGenerator;
import com.github.gumtreediff.actions.EditScript;
import com.github.gumtreediff.actions.EditScriptGenerator;
import com.github.gumtreediff.actions.model.Action;
import com.github.gumtreediff.actions.model.Move;
import com.github.gumtreediff.client.Run;
import com.github.gumtreediff.matchers.MappingStore;
import com.github.gumtreediff.matchers.Matcher;
import com.github.gumtreediff.matchers.Matchers;
import com.github.gumtreediff.tree.ITree;
import com.github.gumtreediff.utils.Pair;
import pgenerator.PGenerator;
import pgenerator.SubtreeMatcher;

import javax.swing.*;
import java.io.*;
import java.util.LinkedList;
import java.util.List;

public class Experimenter {

    static List<String> commitLogs = new LinkedList<String>();

    public static void main(String[] args) {

        Run.initGenerators();
        long startTime = System.currentTimeMillis();

        //Git 1.8.5以降
        String[] gitCommand = {"git", "-C", args[0], "log", "--pretty=format:%H"}; //https://qiita.com/harukasan/items/9149542584385e8dea75 より
        Process process = null;
        ProcessBuilder processBuilder = new ProcessBuilder(gitCommand);
        Runtime runtime = Runtime.getRuntime();
        try {
            process = processBuilder.start();
            processBuilder.redirectErrorStream(true);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        if (process != null) {
            try {
                //process.waitFor();
                InputStream is = process.getInputStream();
                BufferedReader br = new BufferedReader(new InputStreamReader(is));
                String str;
                while ((str = br.readLine()) != null) {
                    commitLogs.add(str);
                }
            } catch (IOException | NullPointerException e) {
                e.printStackTrace();
            }
        }
        final int commitLogSize = commitLogs.size();
        System.out.println("Found " + commitLogSize + "commits");

        for (int i = commitLogs.size() - 1; i > 0; i--) {
            try {
                String[] checkoutOld = {"git", "-C", args[0], "checkout", commitLogs.get(i)};
                runtime.exec(checkoutOld);
                String[] checkoutNew = {"git", "-C", args[1], "checkout", commitLogs.get(i - 1)};
                runtime.exec(checkoutNew);
            } catch (IOException e) {
                e.printStackTrace();
            }

            int num = commitLogSize - i;
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

            Pair<ITree, ITree> trees;
            try {
                PGenerator.getNoChangedFiles(args[3], args[4], "java");
                trees = PGenerator.getProjectTreePair(args[3], args[4], "java");
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }
            Matcher m = Matchers.getInstance().getMatcher();
            MappingStore mappingStore = new SubtreeMatcher().match(trees.first, trees.second);
            EditScriptGenerator editScriptGenerator = new ChawatheScriptGenerator();
            EditScript editScript = editScriptGenerator.computeActions(mappingStore);

            int[] moveCount = new int[100 + 1];
            for (Action action : editScript) {
                if (action instanceof Move) {
                    Move mv = (Move)action;
                    String srcFileName = PGenerator.getAffiliatedFileName(mv.getNode());
                    String dstFileName = PGenerator.getAffiliatedFileName(mv.getParent());
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
            System.out.println("Done: " + (commitLogSize - i) + "/" + (commitLogSize - 1));
        }

        long endTime = System.currentTimeMillis();
        double time = (double)(endTime - startTime) / 1000;
        System.out.println("Calculate Time: " + time);
    }
}
