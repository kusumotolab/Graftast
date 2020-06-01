package graftast;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

public class GraftFileList {

    private List<SourceElement> srcFiles = new LinkedList<>();
    private List<SourceElement> dstFiles = new LinkedList<>();

    public GraftFileList() {}

    public List<SourceElement> getSrcFiles() {
        return srcFiles;
    }

    public List<SourceElement> getDstFiles() {
        return dstFiles;
    }

    public void addSrcFile(SourceElement se) {
        srcFiles.add(se);
    }

    public void addSrcFile(File file, String projectRoot) {
        srcFiles.add(new SourceElementFile(file, projectRoot));
    }

    public void addSrcFile(FileContainer fileContainer) {
        srcFiles.add(new SourceElementContainer(fileContainer));
    }

    public void addDstFile(SourceElement se) {
        dstFiles.add(se);
    }

    public void addDstFile(File file, String projectRoot) {
        dstFiles.add(new SourceElementFile(file, projectRoot));
    }

    public void addDstFile(FileContainer fileContainer) {
        dstFiles.add(new SourceElementContainer(fileContainer));
    }

    public SourceElement getSrcSourceElement(String name) {
        for (SourceElement se: srcFiles) {
            if (name.equals(se.getName()) || name.equals(se.getProjectRelativePath()))
                return se;
        }
        return null;
    }

    public SourceElement getDstSourceElement(String name) {
        for (SourceElement se: dstFiles) {
            if (name.equals(se.getName()) || name.equals(se.getProjectRelativePath()))
                return se;
        }
        return null;
    }
}
