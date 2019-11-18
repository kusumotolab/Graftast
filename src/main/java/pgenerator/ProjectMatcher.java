package pgenerator;

import com.github.gumtreediff.matchers.Mapping;
import com.github.gumtreediff.matchers.MappingStore;
import com.github.gumtreediff.matchers.Matcher;
import com.github.gumtreediff.matchers.Matchers;
import com.github.gumtreediff.tree.ITree;

import java.util.Iterator;

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
        mappingStore = m.match(src, dst, mappingStore); //全体のマッチング
        return mappingStore;
    }

}
