package pgenerator;

import com.github.gumtreediff.actions.*;
import com.github.gumtreediff.actions.model.Action;
import com.github.gumtreediff.actions.model.Move;
import com.github.gumtreediff.client.Run;
import com.github.gumtreediff.gen.Generators;
import com.github.gumtreediff.matchers.Mapping;
import com.github.gumtreediff.matchers.MappingStore;
import com.github.gumtreediff.matchers.Matcher;
import com.github.gumtreediff.matchers.Matchers;
import com.github.gumtreediff.tree.*;
import com.github.gumtreediff.utils.Pair;
import webdiff.WebDiffMod;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;


public class PGenerator {

    private static List<File> noChangedFiles = new LinkedList<>();
    static int num = 1;

    public static void main(String[] args) {
        Run.initGenerators();

        long startTime = System.currentTimeMillis();
        getNoChangedFiles(args[0], args[1], "java");

        ITree srcProject;
        ITree dstProject;
        Pair<ITree, ITree> projectTrees;
        try {
            //srcProject = Generators.getInstance().getTree("./testData/1/Main.java").getRoot();
            //dstProject = Generators.getInstance().getTree("./testData/2/Main.java").getRoot();
            //srcProject = getProjectTree(args[0], "java", "tmp/srcSource");
            //dstProject = getProjectTree(args[1], "java", "tmp/dstSource");
            projectTrees = getProjectTreePair(args[0], args[1], "java", "tmp/srcSource", "tmp/dstSource");
            srcProject = projectTrees.first;
            dstProject = projectTrees.second;
        } catch(IOException e) {
            e.printStackTrace();
            return;
        }

        Matcher m = Matchers.getInstance().getMatcher();
        //MappingStore mappingStore = new MappingStore(srcProject, dstProject);
        //new ProjectMatcher().match(srcProject, dstProject, mappingStore);
        MappingStore mappingStore = new SubtreeMatcher().match(srcProject, dstProject);
        //MappingStore mappingStore = m.match(srcProject, dstProject);

        long endTime = System.currentTimeMillis();
        double time = (double)(endTime - startTime) / 1000;

        //System.out.println("Calculate Time: " + time);

        EditScriptGenerator editScriptGenerator;
        editScriptGenerator = new ChawatheScriptGenerator();
        EditScript editScript = editScriptGenerator.computeActions(mappingStore);
        /*PrintWriter printWriter;
        try {
            //printWriter = new PrintWriter(new BufferedWriter(new FileWriter(new File("experiment/moveDistribution/kGenProg.csv"))));
            //printWriter = new PrintWriter(new BufferedWriter(new FileWriter(new File(args[2]))));
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }*/
        int moveOverFiles = 0;
        int[] moveCount = new int[100 + 1];
        for (Action action : editScript) {
            action.getName();
            //printWriter.println(action.toString());
            //printWriter.println(action.getName());
            //System.out.println(action.getName());
            if (action instanceof Move) {
                Move mv = (Move)action;
                String srcFileName = getAffiliatedFileName(mv.getNode());
                String dstFileName = getAffiliatedFileName(mv.getParent());
                if (!srcFileName.equals(dstFileName)) {
                    moveOverFiles += 1;
                    int size = mv.getNode().getMetrics().size;
                    if (size >= 100)
                        moveCount[100] += 1;
                    else
                        moveCount[size] += 1;
                    if (size == 2)
                        System.out.println(mv.toString());
                }
            }
            //System.out.println(action.toString());
        }
        System.out.println("moveOverFiles: " + moveOverFiles);

        for (int count: moveCount) {
            System.out.println(count);
        }

        //WebDiffのテスト→実行可能
        /*String[] argsWebDiff = {"./testData/1/Main.java", "./testData/2/Main.java"};
        WebDiff wd = new WebDiff(argsWebDiff);
        wd.run();*/

        //自作WebDiff
        /*String[] argsWebDiffMod = {args[0], args[1]};
        WebDiffMod wdm = new WebDiffMod(argsWebDiffMod);
        wdm.run();*/
    }


    /**
     * 指定ディレクトリ以下の全ファイルを取得．ただし，typeで終わるファイルのみ引っ張ってくる．<br>
     * 例えば，rootDir=hoge, type=java の時はhogeディレクトリ以下のjavaファイルのみ探す．<br>
     * 結果はファイル名のリストで渡される．
     * @param rootDir 検索対象のディレクトリ
     * @param type ファイル拡張子
     * @return ファイル名のList(String)
     */
    private static List<File> findAllFiles(File rootDir, String type) {
        List<File> files = new LinkedList<>();
        File[] rootFiles = rootDir.listFiles();
        if (rootFiles == null)
            return new LinkedList<>();
        for (File file: rootFiles) {
            if (file.isDirectory()) {
                files.addAll(findAllFiles(file, type));
            } else if (file.getPath().endsWith(type)){
                files.add(file);
            }
        }
        return files;
    }


    /**
     * 指定ディレクトリ以下のファイルからITreeを作る．
     * @param dir 対象のディレクトリ
     * @param type ファイルの種類
     * @return プロジェクト全体（指定ディレクトリ以下）のITree
     * @throws IOException
     */
    public static ITree getProjectTree(String dir, String type) throws IOException {
        List<File> files = findAllFiles(new File(dir), type);
        ITree projectTree = new ProjectTree(TypeSet.type("CompilationUnit")); //土台となる木の元
        int totalLength = 0; //プロジェクト全体のLengthを記録．処理中は累積の長さになってる
        for (File file: files) {
            if (isUnChangedFile(file)) //変更されていないファイルとファイル名が一致した時
                continue;
            ITree it = Generators.getInstance().getTree(file.getAbsolutePath()).getRoot();
            it.setPos(totalLength);            //ファイル冒頭につけてる装飾の分だけプラス
            totalLength += 105 + file.getName().length();
            it.setPos(totalLength);
            //一番最後の子要素のlengthでそのファイルの実質の長さを求めている
            int length = it.getChild(it.getChildren().size() - 1).getLength() + it.getChild(it.getChildren().size() - 1).getPos();
            fixTreePosLength(it, totalLength); //posとlengthを修正 意味があるかは知らん むしろない方がいい気が... TODO:要検証 → ファイルを1つの画面にまとめて表示する場合は必要
            totalLength += length + 1;
            //ファイル末尾が "}"なら+0でOK ただし，改行とか入っていればそれを消すか+いくつかする必要がある
            /*for (ITree child: it.getChildren()) {
                //ファイルから得たITreeをprojectTreeに移動する．
                projectTree.addChild(child);
                child.setParent(projectTree); //親をプロジェクト全体（root）に変更
            }*/
            it.setLabel(file.getName());
            projectTree.addChild(it);
        }
        projectTree.setLength(totalLength);
        //return convertProjectTree(projectTree, new ProjectTree(TypeSet.type("CompilationUnit")));
        return projectTree;
    }

    /**
     * 木の各要素をProjectTreeクラスのオブジェクトに変更する
     * @param iTree 変換元の木
     * @param pt 土台となる木
     * @return
     */
    private static ITree convertProjectTree(ITree iTree, ITree pt) {
        for (ITree it: iTree.getChildren()) {
            if (it.getChildren().size() == 0) {
                ProjectTree projectTree = new ProjectTree(it.getType());
                projectTree.setPos(it.getPos());
                projectTree.setLabel(it.getLabel());
                projectTree.setLength(it.getLength());
                pt.addChild(projectTree);

            } else {
                ProjectTree projectTree = new ProjectTree(it.getType());
                projectTree.setPos(it.getPos());
                projectTree.setLabel(it.getLabel());
                projectTree.setLength(it.getLength());
                pt.addChild(projectTree);
                convertProjectTree(it, projectTree);
            }
        }
        pt.setLength(iTree.getLength());
        return pt;
    }


    /**
     * 指定ディレクトリ以下のファイルからITreeを作る．
     * さらにtmpに全ファイルをまとめたファイルを書き出す
     * @param dir 対象のディレクトリ
     * @param type ファイルの種類
     * @param tmp 書き出すファイルのファイル名
     * @return プロジェクト全体（指定ディレクトリ以下）のITree
     * @throws IOException
     */
    public static ITree getProjectTree(String dir, String type, String tmp) throws IOException {
        ITree projectTree = getProjectTree(dir, type);
        writeProjectSource(tmp, dir, type);
        return projectTree;
    }

    /**
     * ITreeのposおよびlengthを修正する
     * @param iTree
     * @param length
     */
    private static void fixTreePosLength(ITree iTree, int length) {
        for (ITree it: iTree.getChildren()) {
            if (it.getChildren().size() == 0) { //葉ノード
                it.setPos(it.getPos() + length);
            } else { //枝ノード
                fixTreePosLength(it, length);
                it.setPos(it.getPos() + length);
            }
        }
    }

    /**
     * 指定ディレクトリ以下のファイルからTreeContextを作る．
     * @param dir 対象のディレクトリ
     * @param type ファイルの種類
     * @return プロジェクト全体（指定ディレクトリ以下）のTreeContext
     * @throws IOException
     */
    public static TreeContext getProjectTreeContext(String dir, String type) throws IOException {
        TreeContext projectTree = new TreeContext();
        projectTree.setRoot(getProjectTree(dir, type)); //TreeContextのrootにITreeをセット
        return projectTree;
    }


    /**
     * 指定ディレクトリ以下のファイルからTreeContextを作る．
     * さらにtmpに全ファイルをまとめたファイルを書き出す
     * @param dir 対象のディレクトリ
     * @param type ファイルの種類
     * @param tmp 書き出すファイルのファイル名
     * @return プロジェクト全体（指定ディレクトリ以下）のTreeContext
     * @throws IOException
     */
    public static TreeContext getProjectTreeContext(String dir, String type, String tmp) throws IOException {
        writeProjectSource(tmp, dir, type);
        return getProjectTreeContext(dir, type);
    }


    /**
     * 指定ディレクトリ以下のファイルの内容を1つのファイルにコピペする
     * @param fileName コピー先（書き出し先）のファイル名
     * @param rootDir　元のファイルが保存されているディレクトリ
     * @param type ファイルの種類
     * @throws IOException
     */
    private static void writeProjectSource(String fileName, String rootDir, String type) throws IOException {
        if (Files.exists(Paths.get(fileName))) {
            Files.delete(Paths.get(fileName));
        }
        if (!Files.exists(Paths.get("tmp"))) {
            Files.createDirectory(Paths.get("tmp"));
        }
        List<File> files = findAllFiles(new File(rootDir), type);
        for (File file: files) {
            if (isUnChangedFile(file))
                continue;
            List<String> source = Files.readAllLines(Paths.get(file.getAbsolutePath()), StandardCharsets.UTF_8);
            //飾りをadd
            source.add(0, "--------------------------------------------------");
            source.add(1, "");
            source.add(2, file.getName());
            source.add(3, "");
            source.add(4, "--------------------------------------------------");
            //ファイル末尾の改行を削除
            while (source.get(source.size() - 1).equals("")) {
                source.remove(source.size() - 1);
            }
            Files.write(Paths.get(fileName), source, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        }
    }


    /**
     * 2つのプロジェクトのファイルにおいて，変更されていないものを見つける．
     * getProjectTree()の実行前にこれを実行しておくことで，高速化が可能
     * @param srcDir 比較元プロジェクトのディレクトリ
     * @param dstDir 比較先プロジェクトのディレクトリ
     * @param type ファイルの種類
     */
    public static void getNoChangedFiles(String srcDir, String dstDir, String type) {
        List<File> srcFiles = findAllFiles(new File(srcDir), type);
        List<File> dstFiles = findAllFiles(new File(dstDir), type);
        noChangedFiles = new LinkedList<>();
        Runtime runtime = Runtime.getRuntime();
        for (File src: srcFiles) {
            for (File dst: dstFiles) {
                if (src.getName().equals(dst.getName())) {
                    String[] command = {"diff", "-bBE", src.getAbsolutePath(), dst.getAbsolutePath()};
                    Process process = null;
                    try {
                        process = runtime.exec(command);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    if (process != null) {
                        try {
                            process.waitFor();
                            InputStream is = process.getInputStream();
                            BufferedReader br = new BufferedReader(new InputStreamReader(is));
                            if (br.readLine() == null) { //diffの出力が空 => 同じファイル
                                noChangedFiles.add(src);
                                dstFiles.remove(dst);
                                break;
                            }
                        } catch (InterruptedException | IOException | NullPointerException e) {
                            e.printStackTrace();
                        }
                    } else {
                        //diffが実行できなかった時
                        //TODO diffっぽいメソッドを作る
                    }
                }
            }
        }
    }


    /**
     * 与えたファイルが変更されているか調べる
     * @param file 変更されているか調べたいファイル
     * @return 変更されていなければtrue, 変更されていればfalse
     */
    private static boolean isUnChangedFile(File file) {
        String fileName = file.getName();
        for (File n :noChangedFiles) {
            String name = n.getName();
            if (name.equals(fileName))
                return true;
        }
        return false;
    }

    public static Pair<ITree, ITree> getProjectTreePair(String src, String dst, String type) throws IOException {
        ITree srcTree = getProjectTree(src, type);
        ITree dstTree = getProjectTree(dst, type);
        return new Pair<>(srcTree, dstTree);
    }

    public static Pair<ITree, ITree> getProjectTreePair(String src, String dst, String type, String srcTmp, String dstTmp) throws IOException {
        ITree srcTree = getProjectTree(src, type, srcTmp);
        ITree dstTree = getProjectTree(dst, type, dstTmp);
        /*for (ITree sit : srcTree.getChildren()) {
            for (ITree dit : dstTree.getChildren()) {
                if (sit.getLabel().equals(dit.getLabel())) {
                    Matcher m = Matchers.getInstance().getMatcher();
                    MappingStore mappingStore = m.match(sit, dit); //先に同じファイル同士でマッチング
                    double sim = (double)mappingStore.size() / (double)getDstMapSize(mappingStore);
                    if (0.9< sim && sim < 1.1) {
                        deleteSrcNode(sit, mappingStore);
                        deleteDstNode(dit, mappingStore);
                    }
                    //setDstMappingID((ProjectTree)dit, mappingStore);
                }
            }
        }*/
        fixMetrics(srcTree);
        fixMetrics(dstTree);
        return new Pair<>(srcTree, dstTree);
    }

    public static Pair<TreeContext, TreeContext> getProjectTreeContextPair(String src, String dst, String type, String srcTmp, String dstTmp) throws IOException {
        Pair<ITree, ITree> projectTreePair = getProjectTreePair(src, dst, type, srcTmp, dstTmp);
        TreeContext srcTree = new TreeContext();
        TreeContext dstTree = new TreeContext();
        srcTree.setRoot(projectTreePair.first);
        dstTree.setRoot(projectTreePair.second);
        return new Pair<>(srcTree, dstTree);
    }

    public static int generateNumber() {
        return num++;
    }

    private static void deleteSrcNode(ITree src, MappingStore mappingStore) {
        for (int i = 0; i < src.getChildren().size(); i++) {
            ITree it = src.getChild(i);
            deleteSrcNode(it, mappingStore);
            if (mappingStore.isSrcMapped(it) && hasSameTypeAndLabel(it, mappingStore.getDstForSrc(it))) {
                i += treeRemoveOperation(it);
            }
        }
    }


    private static void deleteDstNode(ITree dst, MappingStore mappingStore) {
        for (int i = 0; i < dst.getChildren().size(); i++) {
            ITree it = dst.getChild(i);
            deleteDstNode(it, mappingStore);
            if (mappingStore.isDstMapped(it) && hasSameTypeAndLabel(it, mappingStore.getSrcForDst(it))) {
                i += treeRemoveOperation(it);
            }
        }
    }

    private static int treeRemoveOperation(ITree it) {
        if (it.getChildren().size() == 0) {
            it.getParent().getChildren().remove(it);
            return -1;
        } else if (isSingleProgeny(it)) {
            return 0;
        } else {
            int index = it.getParent().getChildPosition(it);
            it.getParent().getChildren().remove(it);
            it.getParent().getChildren().addAll(index, it.getChildren());
            for (ITree c: it.getChildren())
                c.setParent(it.getParent());
            return  it.getChildren().size() - 1;
        }
    }

    private static boolean isSingleProgeny(ITree t) {
        if (t.getChildren().size() == 0) {
            return true;
        } else if (t.getChildren().size() == 1) {
            return isSingleProgeny(t.getChild(0));
        } else {
            return false;
        }
    }

    private static void removeFileNameInfo(ITree it) {
        for (ITree t: it.getChildren()) {
            t.setLabel("");
        }
    }


    /*private static void deleteDstNodeArchive(ITree dst, MappingStore mappingStore) {
        for (int i = 0; i < dst.getChildren().size(); i++) {
            ITree it = dst.getChild(i);
            if (it.getChildren().size() == 0) {
                if (mappingStore.isDstMapped(it) && it.getParent().getType().equals(mappingStore.getSrcForDst(it).getParent().getType())) {
                    it.getParent().getChildren().remove(it);
                    i -= 1;
                }
            } else {
                deleteDstNode(it, mappingStore);
                if (mappingStore.isDstMapped(it) && it.getChildren().size() == 0 && it.getParent().getType().equals(mappingStore.getSrcForDst(it).getParent().getType())) {
                    it.getParent().getChildren().remove(it);
                    i -= 1;
                }
            }
        }
    }*/

    private static boolean hasSameTypeAndLabel(ITree t1, ITree t2) {
        if (t1.hasSameTypeAndLabel(t2) && t1.getParent().hasSameTypeAndLabel(t2.getParent()))
            return true;
        return false;
    }

    private static void setSrcMappingID(ProjectTree src, MappingStore mappingStore) {
        for (ITree s: src.getChildren()) {
            ProjectTree pt = (ProjectTree)s;
            if (s.getChildren().size() == 0) {
                if (mappingStore.isSrcMapped(pt)) {
                    int mappingID = PGenerator.generateNumber();
                    pt.setMappingID(mappingID);
                    ((ProjectTree)mappingStore.getDstForSrc(pt)).setMappingID(mappingID);
                }
            } else {
                setSrcMappingID(pt, mappingStore);
                if (mappingStore.isSrcMapped(pt)) {
                    int mappingID = PGenerator.generateNumber();
                    pt.setMappingID(mappingID);
                    ((ProjectTree)mappingStore.getDstForSrc(pt)).setMappingID(mappingID);
                }
            }
        }
    }

    private static void setDstMappingID(ProjectTree dst, MappingStore mappingStore) {
        for (ITree d: dst.getChildren()) {
            ProjectTree pt = (ProjectTree)d;
            if (d.getChildren().size() == 0) {
                if (mappingStore.isDstMapped(pt)) {
                    int mappingID = PGenerator.generateNumber();
                    pt.setMappingID(mappingID);
                    ((ProjectTree)mappingStore.getSrcForDst(pt)).setMappingID(mappingID);
                }
            } else {
                setDstMappingID(pt, mappingStore);
                if (mappingStore.isDstMapped(pt)) {
                    int mappingID = PGenerator.generateNumber();
                    pt.setMappingID(mappingID);
                    ((ProjectTree) mappingStore.getSrcForDst(pt)).setMappingID(mappingID);
                }
            }
        }
    }

    private static int getDstMapSize(MappingStore ms) {
        Iterator<Mapping> iterator = ms.iterator();
        Set set = new HashSet();
        while (iterator.hasNext()) {
            Mapping element = iterator.next();
            set.add(ms.getSrcForDst(element.second));
        }
        return set.size();
    }

    private static void fixMetrics(ITree tree) {
        TreeVisitor.visitTree(tree, new TreeMetricComputer());
    }

    public static String getAffiliatedFileName(ITree it) {
        ITree parent = it.getParent();
        while (!parent.isRoot() && !(parent.getParent() instanceof FakeTree)) {
            it = parent;
            parent = parent.getParent();
        }
        return it.getLabel();
    }


}
