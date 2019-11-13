package experimenter.analyzer;

class MoveInfo {
    private final String identifier;
    private final int size;
    private final String srcFileName;
    private final String dstFileName;
    private final int commitNum;
    private final String content;


    MoveInfo(String src, String dst, int num, String i, int s, String content) {
        this.identifier = i;
        this.size = s;
        this.srcFileName = src;
        this.dstFileName = dst;
        this.commitNum = num;
        this.content = content;
    }

    @Override
    public String toString() {
        return "commit num: " + commitNum + "\n" + srcFileName + " -> " + dstFileName + "\n" + content ;
    }

    public String getIdentifier() {
        return identifier;
    }

    public int getSize() {
        return size;
    }
}