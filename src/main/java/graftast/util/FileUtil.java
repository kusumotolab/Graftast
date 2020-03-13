package graftast.util;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;

public class FileUtil {

    /**
     * 指定ディレクトリ以下の全ファイルを取得．ただし，typeで終わるファイルのみ引っ張ってくる．<br>
     * 例えば，rootDir=hoge, type=java の時はhogeディレクトリ以下のjavaファイルのみ探す．<br>
     * 結果はファイル名のリストで渡される．
     * @param rootDir 検索対象のディレクトリ
     * @param type ファイル拡張子
     * @return ファイル名のList(String)
     */
    public static List<File> findAllFiles(File rootDir, String type) {
        List<File> files = new LinkedList<>();
        File[] rootFiles = rootDir.listFiles();
        if (rootFiles == null)
            return new LinkedList<>();
        for (File file: rootFiles) {
            if (file.isDirectory()) {
                insertAdd(files, findAllFiles(file, type));
            } else if (file.getPath().endsWith(type)){
                insertAdd(files, file);
            }
        }
        return files;
    }

    private static void insertAdd(List<File> list, File file) {
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).getName().compareTo(file.getName()) > 0) {
                list.add(i, file);
                return;
            }
        }
        list.add(file);
    }

    private static void insertAdd(List<File> list, List<File> files) {
        for (File f: files)
            insertAdd(list, f);
    }


    public static String readFile(String path, Charset encoding)  throws IOException {
        byte[] encoded = Files.readAllBytes(Paths.get(path));
        return new String(encoded, encoding);
    }

}
