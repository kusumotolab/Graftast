package pgenerator;

import com.github.gumtreediff.matchers.Mapping;
import com.github.gumtreediff.matchers.MappingStore;
import com.github.gumtreediff.matchers.Matcher;
import com.github.gumtreediff.matchers.Matchers;
import com.github.gumtreediff.tree.ITree;

import java.util.Iterator;

public class ProjectMatcher implements Matcher {

    @Override
    public MappingStore match(ITree iTree, ITree iTree1, MappingStore mappingStore) {
        return match(iTree, iTree1);
    }

    @Override
    public MappingStore match(ITree src, ITree dst) {
        MappingStore mappingStore = new MappingStore(src, dst);
        mappingStore.addMapping(src, dst);
        for (ITree sit : src.getChildren()) {
            for (ITree dit : dst.getChildren()) {
                if (sit.getLabel().equals(dit.getLabel())) {
                    mappingStore.addMapping(sit, dit);
                    Matcher m = Matchers.getInstance().getMatcher();
                    MappingStore ms = m.match(sit, dit); //先に同じファイル同士でマッチング]
                    Iterator<Mapping> iterator = ms.iterator();
                    while (iterator.hasNext()) {
                        Mapping tmp = iterator.next();
                        mappingStore.addMapping((ITree)tmp.first, (ITree)tmp.second);
                    }
                    //deleteSrcNode(sit, mappingStore);
                    //deleteDstNode(dit, mappingStore);
                }
            }
        }
        return mappingStore;
    }
}
