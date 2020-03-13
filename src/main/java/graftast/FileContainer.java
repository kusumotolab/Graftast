package graftast;

public class FileContainer {

    private final String path;
    private final String name;
    private final String content;

    public FileContainer(String path, String name, String content) {
        this.path = path;
        this.name = name;
        this.content = content;
    }

    public String getPath() {
        return path;
    }

    public String getContent() {
        return content;
    }

    public String getName() {
        return name;
    }
}
