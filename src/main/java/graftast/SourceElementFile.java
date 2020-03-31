package graftast;

import graftast.util.FileUtil;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;

public class SourceElementFile implements SourceElement {

    private File file;

    public SourceElementFile(File file) {
        this.file = file;
    }

    @Override
    public String getPath() {
        return file.getPath();
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
