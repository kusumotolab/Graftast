package graftast;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

public class GraftFileList {

    private List<File> srcFiles = new LinkedList<>();
    private List<File> dstFiles = new LinkedList<>();

    public GraftFileList() {}

    public List<File> getSrcFiles() {
        return srcFiles;
    }

    public List<File> getDstFiles() {
        return dstFiles;
    }

    public void addSrcFiles(File file) {
        srcFiles.add(file);
    }

    public void addDstFiles(File file) {
        dstFiles.add(file);
    }
}
