package graftast;

public class SourceElementContainer implements SourceElement {

    private final FileContainer fileContainer;

    public  SourceElementContainer(FileContainer fileContainer) {
        this.fileContainer = fileContainer;
    }

    @Override
    public String getProjectRelativePath() {
        return fileContainer.getPath();
    }

    @Override
    public String getAbsolutePath() {
        return fileContainer.getPath();
    }

    @Override
    public String getName() {
        return fileContainer.getName();
    }

    @Override
    public String getContent() {
        return fileContainer.getContent();
    }
}
