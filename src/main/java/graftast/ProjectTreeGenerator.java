package graftast;

import com.github.gumtreediff.gen.TreeGenerator;
import com.github.gumtreediff.gen.TreeGenerators;
import com.github.gumtreediff.tree.*;
import com.github.gumtreediff.utils.Pair;

import java.io.IOException;
import java.util.List;

public class ProjectTreeGenerator {

    private final GraftFileList graftFileList;

    public ProjectTreeGenerator(String srcDir, String dstDir, String fileType) {
        this.graftFileList = new GraftFileSelector().run(srcDir, dstDir, fileType);
    }

    public ProjectTreeGenerator(List<FileContainer> src, List<FileContainer> dst, String fileType) {
        this.graftFileList = new GraftFileSelector().run(src, dst, fileType);
    }

    public GraftFileList getGraftFileList() {
        return graftFileList;
    }

    public Pair<Tree, Tree> getProjectTreePair() throws IOException {
        List<SourceElement> srcFiles = graftFileList.getSrcFiles();
        List<SourceElement> dstFiles = graftFileList.getDstFiles();
        Tree srcTree = getProjectTree(srcFiles);
        Tree dstTree = getProjectTree(dstFiles);
        return new Pair<>(srcTree, dstTree);
    }

    public Tree getProjectTree(List<SourceElement> sourceElements) throws IOException {
        Tree projectTree = new DefaultTree(TypeSet.type("CompilationUnit")); //土台となる木の元
        for (SourceElement sourceElement: sourceElements) {
            Tree tree = getTree(sourceElement).getRoot();
            tree.setLabel(sourceElement.getProjectRelativePath());
            projectTree.addChild(tree);
        }
        return projectTree;
    }

    public Pair<TreeContext, TreeContext> getProjectTreeContextPair() throws IOException {
        Pair<Tree, Tree> projectTreePair = getProjectTreePair();
        TreeContext srcTree = new TreeContext();
        TreeContext dstTree = new TreeContext();
        srcTree.setRoot(projectTreePair.first);
        dstTree.setRoot(projectTreePair.second);
        return new Pair<>(srcTree, dstTree);
    }

    private void fixMetrics(Tree tree) {
        TreeVisitor.visitTree(tree, new TreeMetricComputer());
    }

    private TreeContext getTree(SourceElement sourceElement) throws IOException {
        TreeGenerator p = TreeGenerators.getInstance().get(sourceElement.getName());
        if (p == null) {
            throw new UnsupportedOperationException("No generator found for file: " + sourceElement.getName());
        } else {
            return p.generateFrom().string(sourceElement.getContent());
        }
    }

}
