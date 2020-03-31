package graftast;

import com.github.gumtreediff.gen.Generators;
import com.github.gumtreediff.gen.TreeGenerator;
import com.github.gumtreediff.tree.*;
import com.github.gumtreediff.utils.Pair;

import java.io.IOException;
import java.util.List;

public class ProjectTreeGenerator {

//    /**
//     * 指定ディレクトリ以下のファイルからITreeを作る．
//     * @param dir 対象のディレクトリ
//     * @param type ファイルの種類
//     * @return プロジェクト全体（指定ディレクトリ以下）のITree
//     * @throws IOException
//     */
//    public ITree getProjectTree(String dir, String type) throws IOException {
//        List<File> files = findAllFiles(new File(dir), type);
//        ITree projectTree = new ProjectTree(TypeSet.type("CompilationUnit")); //土台となる木の元
//        int totalLength = 0; //プロジェクト全体のLengthを記録．処理中は累積の長さになってる
//        for (File file: files) {
//            if (isUnChangedFile(file)) //変更されていないファイルとファイル名が一致した時
//                continue;
//            ITree it = Generators.getInstance().getTree(file.getAbsolutePath()).getRoot();
//            it.setPos(totalLength);            //ファイル冒頭につけてる装飾の分だけプラス
//            totalLength += 105 + file.getName().length();
//            it.setPos(totalLength);
//            //一番最後の子要素のlengthでそのファイルの実質の長さを求めている
//            int length = it.getChild(it.getChildren().size() - 1).getLength() + it.getChild(it.getChildren().size() - 1).getPos();
//            fixTreePosLength(it, totalLength); //posとlengthを修正 意味があるかは知らん むしろない方がいい気が... TODO:要検証 → ファイルを1つの画面にまとめて表示する場合は必要
//            totalLength += length + 1;
//            //ファイル末尾が "}"なら+0でOK ただし，改行とか入っていればそれを消すか+いくつかする必要がある
//            /*for (ITree child: it.getChildren()) {
//                //ファイルから得たITreeをprojectTreeに移動する．
//                projectTree.addChild(child);
//                child.setParent(projectTree); //親をプロジェクト全体（root）に変更
//            }*/
//            it.setLabel(file.getName());
//            projectTree.addChild(it);
//        }
//        projectTree.setLength(totalLength);
//        //return convertProjectTree(projectTree, new ProjectTree(TypeSet.type("CompilationUnit")));
//        return projectTree;
//    }

    private GraftFileList graftFileList;

    public ProjectTreeGenerator(String srcDir, String dstDir, String fileType) {
        this.graftFileList = new GraftFileSelector().run(srcDir, dstDir, fileType);
    }

    public ProjectTreeGenerator(List<FileContainer> src, List<FileContainer> dst, String fileType) {
        this.graftFileList = new GraftFileSelector().run(src, dst, fileType);
    }

    public GraftFileList getGraftFileList() {
        return graftFileList;
    }

    public Pair<ITree, ITree> getProjectTreePair() throws IOException {
        List<SourceElement> srcFiles = graftFileList.getSrcFiles();
        List<SourceElement> dstFiles = graftFileList.getDstFiles();
        ITree srcTree = getProjectTree(srcFiles);
        ITree dstTree = getProjectTree(dstFiles);
        return new Pair<>(srcTree, dstTree);
    }

    public ITree getProjectTree(List<SourceElement> sourceElements) throws IOException {
        ITree projectTree = new ProjectTree(TypeSet.type("CompilationUnit")); //土台となる木の元
        for (SourceElement sourceElement: sourceElements) {
            ITree it = getTree(sourceElement).getRoot();
            it.setLabel(sourceElement.getName());
            projectTree.addChild(it);
        }
        return projectTree;
    }

    public Pair<TreeContext, TreeContext> getProjectTreeContextPair() throws IOException {
        Pair<ITree, ITree> projectTreePair = getProjectTreePair();
        TreeContext srcTree = new TreeContext();
        TreeContext dstTree = new TreeContext();
        srcTree.setRoot(projectTreePair.first);
        dstTree.setRoot(projectTreePair.second);
        return new Pair<>(srcTree, dstTree);
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
//
//    /**
//     * 指定ディレクトリ以下のファイルの内容を1つのファイルにコピペする
//     * @param fileName コピー先（書き出し先）のファイル名
//     * @param rootDir　元のファイルが保存されているディレクトリ
//     * @param type ファイルの種類
//     * @throws IOException
//     */
//    private void writeProjectSource(String fileName, String rootDir, String type) throws IOException {
//        if (Files.exists(Paths.get(fileName))) {
//            Files.delete(Paths.get(fileName));
//        }
//        if (!Files.exists(Paths.get("tmp"))) {
//            Files.createDirectory(Paths.get("tmp"));
//        }
//        List<File> files = findAllFiles(new File(rootDir), type);
//        for (File file: files) {
//            if (isUnChangedFile(file))
//                continue;
//            List<String> source = Files.readAllLines(Paths.get(file.getAbsolutePath()), StandardCharsets.UTF_8);
//            //飾りをadd
//            source.add(0, "--------------------------------------------------");
//            source.add(1, "");
//            source.add(2, file.getName());
//            source.add(3, "");
//            source.add(4, "--------------------------------------------------");
//            //ファイル末尾の改行を削除
//            while (source.get(source.size() - 1).equals("")) {
//                source.remove(source.size() - 1);
//            }
//            Files.write(Paths.get(fileName), source, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
//        }
//    }

    private void fixMetrics(ITree tree) {
        TreeVisitor.visitTree(tree, new TreeMetricComputer());
    }

    private TreeContext getTree(SourceElement sourceElement) throws IOException {
        TreeGenerator p = Generators.getInstance().get(sourceElement.getName());
        if (p == null) {
            throw new UnsupportedOperationException("No generator found for file: " + sourceElement.getName());
        } else {
            return p.generateFrom().string(sourceElement.getContent());
        }
    }

}
