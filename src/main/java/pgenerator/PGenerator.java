package pgenerator;

import com.github.gumtreediff.actions.ChawatheScriptGenerator;
import com.github.gumtreediff.actions.EditScript;
import com.github.gumtreediff.actions.EditScriptGenerator;
import com.github.gumtreediff.client.Run;
import com.github.gumtreediff.gen.Generators;
import com.github.gumtreediff.gen.jdt.JdtTreeGenerator;
import com.github.gumtreediff.matchers.MappingStore;
import com.github.gumtreediff.tree.*;
import com.github.gumtreediff.utils.Pair;
import com.sksamuel.diffpatch.DiffMatchPatch;
import webdiff.WebDiffMod;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;


public class PGenerator {

    private List<String> noChangedFiles = new LinkedList<>();

    public PGenerator(String srcPath, String dstPath) {
        Run.initGenerators();
        getNoChangedFiles(srcPath, dstPath, "java");
    }

    public PGenerator(List<FileContainer> src, List<FileContainer> dst) {
        Run.initGenerators();
        getNoChangeFiles(src, dst);
    }

    public static void main(String[] args) {
        PGenerator pGenerator = new PGenerator(args[0], args[1]);
        pGenerator.start(args);
    }

    public void start(String[] args) {
        long startTime = System.currentTimeMillis();

        try {
            calculateEditScript(args[0], args[1]);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        long endTime = System.currentTimeMillis();
        double time = (double)(endTime - startTime) / 1000;
        System.out.println("Calculate Time: " + time);

        //WebDiffのテスト→実行可能
        /*String[] argsWebDiff = {"./testData/1/Main.java", "./testData/2/Main.java"};
        WebDiff wd = new WebDiff(argsWebDiff);
        wd.run();*/

        //自作WebDiff
        String[] argsWebDiffMod = {args[0], args[1]};
        WebDiffMod wdm = new WebDiffMod(argsWebDiffMod);
        wdm.run();
    }

    public EditScript calculateEditScript(String srcPath, String dstPath) throws IOException{
        Pair<ITree, ITree> projectTrees = getProjectTreePair(srcPath, dstPath, "java", "tmp/srcSource", "tmp/dstSource");
        return calculateEditScript(projectTrees);
    }

    public EditScript calculateEditScript(Pair<ITree, ITree> projectTrees) {
        ITree srcProject = projectTrees.first;
        ITree dstProject = projectTrees.second;

        MappingStore mappingStore = new ProjectMatcher().match(srcProject, dstProject);

        EditScriptGenerator editScriptGenerator;
        editScriptGenerator = new ChawatheScriptGenerator();
        return editScriptGenerator.computeActions(mappingStore);
    }


    /**
     * 指定ディレクトリ以下の全ファイルを取得．ただし，typeで終わるファイルのみ引っ張ってくる．<br>
     * 例えば，rootDir=hoge, type=java の時はhogeディレクトリ以下のjavaファイルのみ探す．<br>
     * 結果はファイル名のリストで渡される．
     * @param rootDir 検索対象のディレクトリ
     * @param type ファイル拡張子
     * @return ファイル名のList(String)
     */
    private List<File> findAllFiles(File rootDir, String type) {
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
    public ITree getProjectTree(String dir, String type) throws IOException {
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
    private ITree convertProjectTree(ITree iTree, ITree pt) {
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
    public ITree getProjectTree(String dir, String type, String tmp) throws IOException {
        ITree projectTree = getProjectTree(dir, type);
        writeProjectSource(tmp, dir, type);
        return projectTree;
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

    /**
     * 指定ディレクトリ以下のファイルからTreeContextを作る．
     * @param dir 対象のディレクトリ
     * @param type ファイルの種類
     * @return プロジェクト全体（指定ディレクトリ以下）のTreeContext
     * @throws IOException
     */
    public TreeContext getProjectTreeContext(String dir, String type) throws IOException {
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
    public TreeContext getProjectTreeContext(String dir, String type, String tmp) throws IOException {
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
    private void writeProjectSource(String fileName, String rootDir, String type) throws IOException {
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
    private void getNoChangedFiles(String srcDir, String dstDir, String type) {
        List<File> srcFiles = findAllFiles(new File(srcDir), type);
        List<File> dstFiles = findAllFiles(new File(dstDir), type);
        Runtime runtime = Runtime.getRuntime();
        for (File src: srcFiles) {
            for (File dst: dstFiles) {
                if (src.getName().equals(dst.getName())) {
                    String[] command = {"diff", "-bBE", src.getAbsolutePath(), dst.getAbsolutePath()};
                    Process process = null;
                    try {
                        process = runtime.exec(command);
                    } catch (IOException e) {
                        //diffコマンドがないとき
                    }
                    if (process != null) {
                        try {
                            process.waitFor();
                            InputStream is = process.getInputStream();
                            BufferedReader br = new BufferedReader(new InputStreamReader(is));
                            if (br.readLine() == null) { //diffの出力が空 => 同じファイル
                                noChangedFiles.add(src.getName());
                                dstFiles.remove(dst);
                                break;
                            }
                        } catch (InterruptedException | IOException | NullPointerException e) {
                            e.printStackTrace();
                        }
                    } else {
                        //diffが実行できなかった時
                        try {
                            String srcCode = readFile(src.getAbsolutePath(), Charset.defaultCharset());
                            String dstCode = readFile(dst.getAbsolutePath(), Charset.defaultCharset());
                            if (diff(srcCode, dstCode)) {
                                noChangedFiles.add(src.getName());
                                dstFiles.remove(dst);
                                break;
                            }
                        } catch (IOException e) {
                            //読み込みに失敗したらそのまま続ける
                            continue;
                        }
                    }
                }
            }
        }
    }

    private void getNoChangeFiles(List<FileContainer> src, List<FileContainer> dst) {
        for (FileContainer s: src) {
            for (FileContainer d: dst) {
                if (s.getFileName().equals(d.getFileName())) {
                    if (diff(s.getContent(), d.getContent()))
                        noChangedFiles.add(s.getFileName());
                    break;
                }
            }
        }
    }

    private boolean diff(String srcCode, String dstCode) {
        // google-diff-match-patch
        DiffMatchPatch dmp = new DiffMatchPatch();
        LinkedList<DiffMatchPatch.Diff> diff = dmp.diff_main(srcCode, dstCode);
        dmp.diff_cleanupEfficiency(diff);
        return isContainOnlySpaceCharDiff(diff);
    }

    private boolean isContainOnlySpaceCharDiff(List<DiffMatchPatch.Diff> diff) {
        for (DiffMatchPatch.Diff df: diff) {
            if (df.operation != DiffMatchPatch.Operation.EQUAL) {
                Pattern p = Pattern.compile("[\\s]+");
                java.util.regex.Matcher m = p.matcher(df.text);
                if (!m.matches())
                    return false;
            }
        }
        return true;
    }

    private String readFile(String path, Charset encoding)  throws IOException {
        byte[] encoded = Files.readAllBytes(Paths.get(path));
        return new String(encoded, encoding);
    }


    /**
     * 与えたファイルが変更されているか調べる
     * @param file 変更されているか調べたいファイル
     * @return 変更されていなければtrue, 変更されていればfalse
     */
    private boolean isUnChangedFile(File file) {
        String fileName = file.getName();
        for (String n: noChangedFiles) {
            if (n.equals(fileName))
                return true;
        }
        return false;
    }

    private boolean isUnchangedFile(FileContainer file) {
        String fileName = file.getFileName();
        for (String n: noChangedFiles) {
            if (n.equals(fileName))
                return true;
        }
        return false;
    }

    public Pair<ITree, ITree> getProjectTreePair(String src, String dst, String type) throws IOException {
        ITree srcTree = getProjectTree(src, type);
        ITree dstTree = getProjectTree(dst, type);
        return new Pair<>(srcTree, dstTree);
    }

    public Pair<ITree, ITree> getProjectTreePair(String src, String dst, String type, String srcTmp, String dstTmp) throws IOException {
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

    public Pair<TreeContext, TreeContext> getProjectTreeContextPair(String src, String dst, String type, String srcTmp, String dstTmp) throws IOException {
        Pair<ITree, ITree> projectTreePair = getProjectTreePair(src, dst, type, srcTmp, dstTmp);
        TreeContext srcTree = new TreeContext();
        TreeContext dstTree = new TreeContext();
        srcTree.setRoot(projectTreePair.first);
        dstTree.setRoot(projectTreePair.second);
        return new Pair<>(srcTree, dstTree);
    }

    private void fixMetrics(ITree tree) {
        TreeVisitor.visitTree(tree, new TreeMetricComputer());
    }

    //Tree側に実装すべき機能
    public String getAffiliatedFileName(ITree it) {
        ITree parent = it.getParent();
        while (!parent.isRoot() && !(parent.getParent() instanceof FakeTree)) {
            it = parent;
            parent = parent.getParent();
        }
        return it.getLabel();
    }

    public ITree getProjectTree(List<FileContainer> containers) throws IOException {
        ITree projectTree = new Tree(TypeSet.type("CompilationUnit")); //土台となる木の元
        int totalLength = 0; //プロジェクト全体のLengthを記録．処理中は累積の長さになってる
        for (FileContainer container: containers) {
            if (isUnchangedFile(container)) //変更されていないファイルとファイル名が一致した時
                continue;
            JdtTreeGenerator jdtTreeGenerator = new JdtTreeGenerator();
            ITree it = jdtTreeGenerator.generateFrom().string(container.getContent()).getRoot();
            //it.setPos(totalLength);
            //一番最後の子要素のlengthでそのファイルの実質の長さを求めている
            //int length = it.getChild(it.getChildren().size() - 1).getLength() + it.getChild(it.getChildren().size() - 1).getPos();
            //fixTreePosLength(it, totalLength);
            //totalLength += length + 1;
            it.setLabel(container.getFileName());
            projectTree.addChild(it);
        }
        projectTree.setLength(totalLength);
        //return convertProjectTree(projectTree, new ProjectTree(TypeSet.type("CompilationUnit")));
        return projectTree;
    }

    public Pair<ITree, ITree> getProjectTreePair(List<FileContainer> src, List<FileContainer> dst, String type) throws IOException {
        typeFilter(src, type);
        typeFilter(dst, type);
        ITree srcTree = getProjectTree(src);
        ITree dstTree = getProjectTree(dst);
        return new Pair<>(srcTree, dstTree);
    }

    private void typeFilter(List<FileContainer> containers, String type) {
        containers.removeIf(fc -> !fc.getFileName().endsWith(type));
    }

    public ITree getRoot(ITree tree) {
        if (!tree.isRoot())
            return getRoot(tree.getParent());
        else
            return tree;
    }


}
