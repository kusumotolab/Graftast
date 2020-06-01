package webdiff;

import com.github.gumtreediff.tree.ITree;
import com.github.gumtreediff.tree.TreeContext;
import com.github.gumtreediff.utils.Pair;
import graftast.GraftFileList;
import graftast.SourceElement;

public class TreeModifier {

    private GraftFileList graftFileList;

    public TreeModifier(GraftFileList graftFileList) {
        this.graftFileList = graftFileList;
    }

    public void modify(Pair<TreeContext, TreeContext> treeContextPair) {
        ITree srcTree = treeContextPair.first.getRoot();
        int length = 0;
        for (ITree iTree : srcTree.getChildren()) {
            String name = iTree.getLabel();
            SourceElement se = graftFileList.getSrcSourceElement(name);
            length += 106 + name.length();
            iTree.setPos(length);
            fixTreePosLength(iTree, length);
            length += se.getContent().length();
        }
        srcTree.setLength(length);

        ITree dstTree = treeContextPair.second.getRoot();
        length = 0;
        for (ITree iTree : dstTree.getChildren()) {
            String name = iTree.getLabel();
            SourceElement se = graftFileList.getDstSourceElement(name);
            length += 106 + name.length();
            iTree.setPos(length);
            fixTreePosLength(iTree, length);
            length += se.getContent().length();
        }
        dstTree.setLength(length);
    }

    /**
     * ITreeのposおよびlengthを修正する
     * @param iTree
     * @param length
     */
    private void fixTreePosLength(ITree iTree, int length) {
        for (ITree it: iTree.getChildren()) {
            if (it.getChildren().size() == 0) { //葉ノード
                it.setPos(it.getPos() + length);
            } else { //枝ノード
                fixTreePosLength(it, length);
                it.setPos(it.getPos() + length);
            }
        }
    }
}
