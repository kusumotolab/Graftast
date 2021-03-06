package graftast;

import graftast.util.Diff;
import graftast.util.FileUtil;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.LinkedList;
import java.util.List;

public class GraftFileSelector {

    public GraftFileList run(File srcDir, File dstDir, String fileType) {
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

        srcFiles.stream().filter(srcFile -> !srcUnchanged.contains(srcFile)).forEach(srcFile -> graftFileList.addSrcFile(srcFile, srcDir));
        dstFiles.stream().filter(dstFile -> !dstUnchanged.contains(dstFile)).forEach(dstFile -> graftFileList.addDstFile(dstFile, dstDir));

        return graftFileList;
    }

    public GraftFileList run(String srcDir, String dstDir, String fileType) {
        return run(new File(srcDir), new File(dstDir), fileType);
    }

    public GraftFileList run(List<FileContainer> srcFiles, List<FileContainer> dstFiles, String fileType) {
        GraftFileList graftFileList = new GraftFileList();

        srcFiles.removeIf(srcFile -> !srcFile.getName().endsWith(fileType));
        dstFiles.removeIf(dstFile -> !dstFile.getName().endsWith(fileType));

        List<FileContainer> srcUnchanged = new LinkedList<>();
        List<FileContainer> dstUnchanged = new LinkedList<>();

        for (FileContainer srcFile: srcFiles) {
            for (FileContainer dstFile: dstFiles) {
                if (!srcFile.getPath().equals(dstFile.getPath()))
                    continue;
                if (Diff.diff(srcFile.getContent(), dstFile.getContent())) {
                    srcUnchanged.add(srcFile);
                    dstUnchanged.add(dstFile);
                    break;
                }
            }
        }
        srcFiles.stream().filter(srcFile -> !srcUnchanged.contains(srcFile)).forEach(graftFileList::addSrcFile);
        dstFiles.stream().filter(dstFile -> !dstUnchanged.contains(dstFile)).forEach(graftFileList::addDstFile);
        return  graftFileList;
    }

    public GraftFileList run2(List<FileContainer> srcFiles, List<FileContainer> dstFiles, String fileType) {
        GraftFileList graftFileList = new GraftFileList();

        srcFiles.removeIf(srcFile -> !srcFile.getName().endsWith(fileType));
        dstFiles.removeIf(dstFile -> !dstFile.getName().endsWith(fileType));

        for (FileContainer srcFile: srcFiles) {
            for (FileContainer dstFile: dstFiles) {
                if (!srcFile.getPath().equals(dstFile.getPath()))
                    continue;
                if (!Diff.diff(srcFile.getContent(), dstFile.getContent())) {
                    graftFileList.addSrcFile(srcFile);
                    graftFileList.addDstFile(dstFile);
                }
            }
        }
        return graftFileList;
    }
}
