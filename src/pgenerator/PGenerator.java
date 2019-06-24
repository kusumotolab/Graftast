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

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;


public class PGenerator {

    public static void main(String[] args) {
        Run.initGenerators();
        ITree srcProject;
        ITree dstProject;
        try {
            //srcProject = Generators.getInstance().getTree("./testData/1/Main.java").getRoot();
            //dstProject = Generators.getInstance().getTree("./testData/2/Main.java").getRoot();
            srcProject = getProjectTree(args[0], "java");
            dstProject = getProjectTree(args[1], "java");
        } catch(IOException e) {
            e.printStackTrace();
            return;
        }

        Matcher m = Matchers.getInstance().getMatcher();
        MappingStore mappingStore = m.match(srcProject, dstProject);

        EditScriptGenerator editScriptGenerator;
        editScriptGenerator = new ChawatheScriptGenerator();
        EditScript editScript = editScriptGenerator.computeActions(mappingStore);
        for (Action action : editScript) {
            action.getName();
            //System.out.println(action.getName());
            System.out.println(action.toString());
        }

        //WebDiffのテスト→実行可能
        /*String[] argsWebDiff = {"./testData/1/Main.java", "./testData/2/Main.java"};
        WebDiff wd = new WebDiff(argsWebDiff);
        wd.run();*/

        //自作WebDiff→うまくいってない
        /*String[] argsWebDiffMod = {"./testData/1_2/", "./testData/2_2/"};
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
    private static List<String> findAllFiles(File rootDir, String type) {
        List<String> fileNames = new LinkedList<>();
        File[] files = rootDir.listFiles();
        if (files == null)
            return new LinkedList<>();
        for (File file: files) {
            if (file.isDirectory()) {
                fileNames.addAll(findAllFiles(file, type));
            } else if (file.getPath().endsWith(type)){
                fileNames.add(file.getPath());
            }
        }
        return fileNames;
    }

    /**
     * 指定ディレクトリ以下のファイルからITreeを作る．
     * @param dir 対象のディレクトリ
     * @param type ファイルの種類
     * @return プロジェクト全体（指定ディレクトリ以下）のITree
     * @throws IOException
     */
    public static ITree getProjectTree(String dir, String type) throws IOException {
        List<String> files = findAllFiles(new File(dir), type);
        ITree projectTree = new Tree(TypeSet.type("CompilationUnit")); //土台となる木の元
        int totalLength = 0; //プロジェクト全体のLengthを記録．処理中は累積の長さになってる
        for (String file: files) {
            ITree it = Generators.getInstance().getTree(file).getRoot();
            fixTreePosLength(it, totalLength); //posとlengthを修正 意味があるかは知らん むしろない方がいい気が... TODO:要検証
            totalLength += it.getLength() + 1;
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

}
