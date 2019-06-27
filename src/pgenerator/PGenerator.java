package pgenerator;

import com.github.gumtreediff.actions.*;
import com.github.gumtreediff.actions.model.Action;
import com.github.gumtreediff.client.Run;
import com.github.gumtreediff.client.diff.web.WebDiff;
import com.github.gumtreediff.gen.Generators;
import com.github.gumtreediff.matchers.MappingStore;
import com.github.gumtreediff.matchers.Matcher;
import com.github.gumtreediff.matchers.Matchers;
import com.github.gumtreediff.tree.ITree;
import com.github.gumtreediff.tree.Tree;
import com.github.gumtreediff.tree.TreeContext;
import com.github.gumtreediff.tree.TypeSet;
import webdiff.WebDiffMod;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.LinkedList;
import java.util.List;


public class PGenerator {

    private static List<File> noChangedFiles = new LinkedList<>();

    public static void main(String[] args) {
        Run.initGenerators();
        getNoChangedFiles(args[0], args[1], "java");

        ITree srcProject;
        ITree dstProject;
        try {
            //srcProject = Generators.getInstance().getTree("./testData/1/Main.java").getRoot();
            //dstProject = Generators.getInstance().getTree("./testData/2/Main.java").getRoot();
            srcProject = getProjectTree(args[0], "java", "tmp/srcSource");
            dstProject = getProjectTree(args[1], "java", "tmp/dstSource");
        } catch(IOException e) {
            e.printStackTrace();
            return;
        }

        long startTime = System.currentTimeMillis();

        Matcher m = Matchers.getInstance().getMatcher();
        MappingStore mappingStore = m.match(srcProject, dstProject);

        long endTime = System.currentTimeMillis();
        double time = (double)(endTime - startTime) / 1000;

        System.out.println("Calculate Time: " + time);

        EditScriptGenerator editScriptGenerator;
        editScriptGenerator = new ChawatheScriptGenerator();
        EditScript editScript = editScriptGenerator.computeActions(mappingStore);
        for (Action action : editScript) {
            action.getName();
            //System.out.println(action.getName());
            //System.out.println(action.toString());
        }


        //WebDiffのテスト→実行可能
        /*String[] argsWebDiff = {"./testData/1/Main.java", "./testData/2/Main.java"};
        WebDiff wd = new WebDiff(argsWebDiff);
        wd.run();*/

        //自作WebDiff
        String[] argsWebDiffMod = {args[0], args[1]};
        WebDiffMod wdm = new WebDiffMod(argsWebDiffMod);
        wdm.run();

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
        ITree projectTree = new Tree(TypeSet.type("CompilationUnit")); //土台となる木の元
        int totalLength = 0; //プロジェクト全体のLengthを記録．処理中は累積の長さになってる
        for (File file: files) {
            if (isUnChangedFile(file)) //変更されていないファイルとファイル名が一致した時
                continue;
            ITree it = Generators.getInstance().getTree(file.getAbsolutePath()).getRoot();
            //ファイル冒頭につけてる装飾の分だけプラス
            totalLength += 105 + file.getName().length();
            //一番最後の子要素のlengthでそのファイルの実質の長さを求めている
            int length = it.getChild(it.getChildren().size() - 1).getLength() + it.getChild(it.getChildren().size() - 1).getPos();
            fixTreePosLength(it, totalLength); //posとlengthを修正 意味があるかは知らん むしろない方がいい気が... TODO:要検証 → ファイルを1つの画面にまとめて表示する場合は必要
            totalLength += length + 1;
            //ファイル末尾が "}"なら+0でOK ただし，改行とか入っていればそれを消すか+いくつかする必要がある
            for (ITree child: it.getChildren()) {
                //ファイルから得たITreeをprojectTreeに移動する．
                projectTree.addChild(child);
                child.setParent(projectTree); //親をプロジェクト全体（root）に変更
            }
        }
        projectTree.setLength(totalLength);
        return projectTree;
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
    private static void getNoChangedFiles(String srcDir, String dstDir, String type) {
        List<File> srcFiles = findAllFiles(new File(srcDir), type);
        List<File> dstFiles = findAllFiles(new File(dstDir), type);
        Runtime runtime = Runtime.getRuntime();
        for (File src: srcFiles) {
            for (File dst: dstFiles) {
                if (src.getName().equals(dst.getName())) {
                    String[] command = {"diff", src.getAbsolutePath(), dst.getAbsolutePath()};
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
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        } catch (IOException e) {
                            e.printStackTrace();
                        } catch (NullPointerException e) {
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

}
