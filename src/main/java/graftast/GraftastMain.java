package graftast;

import com.github.gumtreediff.actions.ChawatheScriptGenerator;
import com.github.gumtreediff.actions.EditScript;
import com.github.gumtreediff.actions.EditScriptGenerator;
import com.github.gumtreediff.client.Run;
import com.github.gumtreediff.matchers.MappingStore;
import com.github.gumtreediff.tree.ITree;
import com.github.gumtreediff.utils.Pair;
import webdiff.WebDiffMod;

import java.io.IOException;


public class GraftastMain {

    public GraftastMain() {
        Run.initGenerators();
    }

    public static void main(String[] args) throws IOException {
        GraftastMain graftastMain = new GraftastMain();
        graftastMain.run(args);
    }

    public void run(String[] args) throws IOException {
        String srcDir = args[1];
        String dstDir = args[2];
        String fileType = args.length >= 4 ? args[3] : "";
        switch (args[0]) {
            case "diff":
                diff(srcDir, dstDir, fileType);
                break;
            case "webdiff":
                webdiff(srcDir, dstDir, fileType);
                break;
            default:
                start(args);
        }
    }

    public void diff(String srcDir, String dstDir, String fileType) throws IOException {
        //diffオプションが設定された時
        Pair<ITree, ITree> projectTrees = new ProjectTreeGenerator(srcDir, dstDir, fileType).getProjectTreePair();
        EditScript editScript = calculateEditScript(projectTrees);
        editScript.forEach(System.out::println);
    }

    public void webdiff(String srcDir, String dstDir, String fileType) {
        //webdiffオプションが設定された時
        //自作WebDiff
        String[] argsWebDiffMod = {srcDir, dstDir, fileType};
        WebDiffMod wdm = new WebDiffMod(argsWebDiffMod);
        wdm.run();
    }

    public void start(String[] args) throws IOException {
        long startTime = System.currentTimeMillis();
        diff(args[0], args[1], args[2]);
        long endTime = System.currentTimeMillis();
        double time = (double)(endTime - startTime) / 1000;
        System.out.println("Calculate Time: " + time);
    }

    public EditScript calculateEditScript(Pair<ITree, ITree> projectTrees) {
        MappingStore mappingStore = getMappings(projectTrees);
        EditScriptGenerator editScriptGenerator;
        editScriptGenerator = new ChawatheScriptGenerator();
        return editScriptGenerator.computeActions(mappingStore);
    }

    public MappingStore getMappings(Pair<ITree, ITree> projectTrees) {
        ITree srcProject = projectTrees.first;
        ITree dstProject = projectTrees.second;
        return new ProjectMatcher().match(srcProject, dstProject);
    }

}
