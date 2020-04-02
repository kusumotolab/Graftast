package graftast.util;

import com.github.gumtreediff.actions.EditScript;
import com.github.gumtreediff.actions.model.Action;
import com.github.gumtreediff.actions.model.Move;
import com.github.gumtreediff.tree.FakeTree;
import com.github.gumtreediff.tree.ITree;

import java.util.Stack;

public class TreeUtil {

    public static String getAffiliatedFileName(ITree it) {
        ITree parent = it.getParent();
        while (!parent.isRoot() && !(parent.getParent() instanceof FakeTree)) {
            it = parent;
            parent = parent.getParent();
        }
        return it.getLabel();
    }

    public static ITree getRoot(ITree tree) {
        if (!tree.isRoot())
            return getRoot(tree.getParent());
        else
            return tree;
    }

    public static String getFinalDstFile(Move mv, EditScript editScript) {
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
    private static ITree getFinalDst(Move mv, EditScript editScript) {
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
