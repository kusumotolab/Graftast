package graftast;

import com.github.gumtreediff.matchers.Mapping;
import com.github.gumtreediff.matchers.MappingStore;
import com.github.gumtreediff.matchers.Matcher;
import com.github.gumtreediff.matchers.Matchers;
import com.github.gumtreediff.tree.Tree;

public class ProjectMatcher implements Matcher {

    @Override
    public MappingStore match(Tree src, Tree dst, MappingStore mappingStore) {
        Matcher m = Matchers.getInstance().getMatcher();
        mappingStore.addMapping(src, dst);
        for (Tree sit : src.getChildren()) {
            for (Tree dit : dst.getChildren()) {
                if (sit.getLabel().equals(dit.getLabel())) {
                    sit.setParent(null);
                    dit.setParent(null);
                    mappingStore.addMapping(sit, dit);
                    m.match(sit, dit, mappingStore); //先に同じファイル同士でマッチング
                    sit.setParent(src);
                    dit.setParent(dst);
                }
            }
        }
        MappingStore mappingStoreAll = m.match(src, dst); //全体のマッチング
        for (Mapping mapping : mappingStoreAll) {
            Tree srcCandidate = mapping.first;
            Tree dstCandidate = mapping.second;
            if (mappingStore.isMappingAllowed(srcCandidate, dstCandidate))
                mappingStore.addMapping(srcCandidate, dstCandidate);
        }
        return mappingStore;
    }

}
