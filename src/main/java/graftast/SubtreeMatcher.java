package graftast;

import com.github.gumtreediff.matchers.MappingStore;
import com.github.gumtreediff.matchers.Matcher;
import com.github.gumtreediff.matchers.Matchers;
import com.github.gumtreediff.tree.ITree;

public class SubtreeMatcher implements Matcher {

    @Override
    public MappingStore match(ITree iTree, ITree iTree1, MappingStore mappingStore) {
        return match(iTree, iTree1);
    }

    @Override
    public MappingStore match(ITree src, ITree dst) {
        Matcher m = Matchers.getInstance().getMatcher();
        MappingStore mappingStore = m.match(src, dst);
        mappingStore.addMapping(src, dst);
        for (ITree sit: src.getChildren()) {
            for (ITree dit: dst.getChildren()) {
                if (sit.getLabel().equals(dit.getLabel()))
                    mappingStore.addMapping(sit, dit);
            }
        }
        return mappingStore;
    }
}
