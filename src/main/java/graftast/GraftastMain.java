package graftast;

import com.github.gumtreediff.actions.ChawatheScriptGenerator;
import com.github.gumtreediff.actions.EditScript;
import com.github.gumtreediff.actions.EditScriptGenerator;
import com.github.gumtreediff.actions.model.Action;
import com.github.gumtreediff.actions.model.Move;
import com.github.gumtreediff.client.Run;
import com.github.gumtreediff.matchers.MappingStore;
import com.github.gumtreediff.tree.FakeTree;
import com.github.gumtreediff.tree.ITree;
import com.github.gumtreediff.utils.Pair;
import webdiff.WebDiffMod;

import java.io.IOException;
import java.util.Stack;


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

    public void start(String[] args) {
        long startTime = System.currentTimeMillis();

        long endTime = System.currentTimeMillis();
        double time = (double)(endTime - startTime) / 1000;
        System.out.println("Calculate Time: " + time);

    }

    public EditScript calculateEditScript(Pair<ITree, ITree> projectTrees) {
        ITree srcProject = projectTrees.first;
        ITree dstProject = projectTrees.second;

        MappingStore mappingStore = new ProjectMatcher().match(srcProject, dstProject);

        EditScriptGenerator editScriptGenerator;
        editScriptGenerator = new ChawatheScriptGenerator();
        return editScriptGenerator.computeActions(mappingStore);
    }

    //Tree側に実装すべき機能
    public String getAffiliatedFileName(ITree it) {
        ITree parent = it.getParent();
        while (!parent.isRoot() && !(parent.getParent() instanceof FakeTree)) {
            it = parent;
            parent = parent.getParent();
        }
        return it.getLabel();
    }

    public ITree getRoot(ITree tree) {
        if (!tree.isRoot())
            return getRoot(tree.getParent());
        else
            return tree;
    }

    //TODO 仮にここに置いてる
    public String getFinalDstFile(Move mv, EditScript editScript) {
        for (ITree tree = mv.getParent(); !tree.isRoot(); tree = tree.getParent()) {
            for (Action action: editScript) {
                if (action instanceof Move) {
                    if (action.getNode() == tree) {
                        return getFinalDstFile((Move)action, editScript);
                    }
                }
            }
        }
        return getAffiliatedFileName(mv.getParent());
    }

    //TODO 最終的な移動先を知るメソッドを作る．未完成
    private ITree getFinalDst(Move mv, EditScript editScript) {
        Stack<Integer> stack = new Stack<>();
        stack.push(mv.getPosition());
        for (ITree tree = mv.getParent(); !tree.isRoot(); tree = tree.getParent()) {
            for (Action action: editScript) {
                if (action instanceof Move) {
                    if (action.getNode() == tree) {
                        ITree dst = getFinalDst((Move)action, editScript);
                        while (!stack.isEmpty()) {
                            try {
                                dst = dst.getChild(stack.pop());
                            } catch (IndexOutOfBoundsException e) {
                                e.printStackTrace();
                            }
                        }
                        return dst;
                    }
                }
            }
            stack.push(tree.positionInParent());
        }
        return mv.getParent().getChild(mv.getPosition());
    }


}
