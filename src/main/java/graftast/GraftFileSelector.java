package graftast;

import graftast.util.Diff;
import graftast.util.FileUtil;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.LinkedList;
import java.util.List;

public class GraftFileSelector {

    private File srcDir;
    private File dstDir;
    private String fileType;

    public GraftFileSelector(String srcDir, String dstDir, String fileType) {
        this.srcDir = new File(srcDir);
        this.dstDir = new File(dstDir);
        this.fileType = fileType;
    }

    public GraftFileList run() {
        GraftFileList graftFileList = new GraftFileList();

        List<File> srcFiles = FileUtil.findAllFiles(srcDir, fileType);
        List<File> dstFiles = FileUtil.findAllFiles(dstDir, fileType);

        List<File> srcUnchanged = new LinkedList<>();
        List<File> dstUnchanged = new LinkedList<>();

        for (File srcFile: srcFiles) {
            for (File dstFile: dstFiles) {
                if (srcFile.getName().equals(dstFile.getName())) {
                    try {
                        String srcCode = FileUtil.readFile(srcFile.getPath(), Charset.defaultCharset());
                        String dstCode = FileUtil.readFile(dstFile.getPath(), Charset.defaultCharset());
                        if (Diff.diff(srcCode, dstCode)) {
                            srcUnchanged.add(srcFile);
                            dstUnchanged.add(dstFile);
                            break;
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        srcFiles.stream().filter(srcFile -> !srcUnchanged.contains(srcFile)).forEach(graftFileList::addSrcFiles);
        dstFiles.stream().filter(dstFile -> !dstUnchanged.contains(dstFile)).forEach(graftFileList::addDstFiles);

        return graftFileList;
    }
}
