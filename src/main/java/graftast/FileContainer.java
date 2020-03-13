package graftast;

public class FileContainer {
    private final String fileName;
    private final String content;

    public FileContainer(String name, String content) {
        this.fileName = name;
        this.content = content;
    }

    public String getContent() {
        return content;
    }

    public String getFileName() {
        return fileName;
    }
}
