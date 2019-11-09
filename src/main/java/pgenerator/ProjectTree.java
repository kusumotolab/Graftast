package pgenerator;

import com.github.gumtreediff.tree.ITree;
import com.github.gumtreediff.tree.Tree;
import com.github.gumtreediff.tree.Type;

public class ProjectTree extends Tree {

    private boolean isomorphic = false; //ファイル毎の比較で一致したかを示すフラグ
    private int mappingID = 0;

    public ProjectTree(Type type) {
        super(type);
    }

    @Override
    public boolean isIsomorphicTo(ITree tree) {
        /*if (this.mappingID == ((ProjectTree)tree).mappingID && mappingID != 0)
            return true;*/
        /*if (this.mappingID == 0)
            return true;*/

        if (!hasSameTypeAndLabel(tree))
            return false;

        if (getChildren().size() != tree.getChildren().size())
            return false;

        for (int i = 0; i < getChildren().size(); i++)  {
            boolean isChildrenIsomophic = getChild(i).isIsomorphicTo(tree.getChild(i));
            if (!isChildrenIsomophic)
                return false;
        }


        /*if (tree instanceof ProjectTree)
            ((ProjectTree) tree).isomorphic = true;
        isomorphic = true; //事前の比較で一致したらフラグを立てる*/
        //mappingID = PGenerator.generateNumber();
        //((ProjectTree)tree).mappingID = mappingID;
        return true;
    }

    public void setMappingID(int ID) {
        this.mappingID = ID;
    }

    public int getMappingID() {
        return this.mappingID;
    }

}
