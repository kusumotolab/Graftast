package graftast;

import graftast.util.FileUtil;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;

public class SourceElementFile implements SourceElement {

    private final File file;
    private final String projectRelativePath;

    public SourceElementFile(File file, File projectRoot) {
        this.file = file;
        String tmp = file.getAbsolutePath().substring(projectRoot.getAbsolutePath().length());
        projectRelativePath = tmp.charAt(0) == File.separatorChar ? tmp.substring(1) : tmp;
    }

    @Override
    public String getProjectRelativePath() {
        return projectRelativePath;
    }

    @Override
    public String getAbsolutePath() {
        return file.getAbsolutePath();
    }

    @Override
    public String getName() {
        return file.getName();
    }

    @Override
    public String getContent() {
        try {
            return FileUtil.readFile(file.getPath(), Charset.defaultCharset());
        } catch (IOException e) {
            return "";
        }
    }
}
