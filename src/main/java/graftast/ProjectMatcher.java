package graftast;

import com.github.gumtreediff.matchers.Mapping;
import com.github.gumtreediff.matchers.MappingStore;
import com.github.gumtreediff.matchers.Matcher;
import com.github.gumtreediff.matchers.Matchers;
import com.github.gumtreediff.tree.ITree;

public class ProjectMatcher implements Matcher {

    @Override
    public MappingStore match(ITree src, ITree dst, MappingStore mappingStore) {
        Matcher m = Matchers.getInstance().getMatcher();
        mappingStore.addMapping(src, dst);
        for (ITree sit : src.getChildren()) {
            for (ITree dit : dst.getChildren()) {
                if (sit.getLabel().equals(dit.getLabel())) {
                    mappingStore.addMapping(sit, dit);
                    m.match(sit, dit, mappingStore); //先に同じファイル同士でマッチング
                }
            }
        }
        MappingStore mappingStoreAll = m.match(src, dst); //全体のマッチング
        for (Mapping mapping : mappingStoreAll) {
            ITree srcCandidate = mapping.first;
            ITree dstCandidate = mapping.second;
            if (mappingStore.isMappingAllowed(srcCandidate, dstCandidate))
                mappingStore.addMapping(srcCandidate, dstCandidate);
        }
        return mappingStore;
    }

}
