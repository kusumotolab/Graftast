package experimenter.analyzer;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

class MoveInfo {
    private final String identifier;
    private final int size;
    private final String srcFileName;
    private final String dstFileName;
    private final String dstFileNameOriginal;
    private final int commitNum;
    private final String content;
    private final String moveTo;

    private final int srcRangeStart;
    private final int srcRangeEnd;
    private final int dstRangeStart;
    private final int dstRangeEnd;

    MoveInfo(String src, String dst, String dstOrig, int num, String i, int s, String content, String moveTo) {
        this.identifier = i;
        this.size = s;
        this.srcFileName = src;
        this.dstFileName = dst;
        this.dstFileNameOriginal = dstOrig;
        this.commitNum = num;
        this.content = content;
        this.moveTo = moveTo;

        Pattern p = Pattern.compile("\\[([0-9]+),([0-9]+)]");
        Matcher srcMatcher = p.matcher(content.split("\n")[0]);
        Matcher dstMatcher = p.matcher(moveTo);
        srcMatcher.find();
        dstMatcher.find();
        this.srcRangeStart = Integer.parseInt(srcMatcher.group(1));
        this.srcRangeEnd = Integer.parseInt(srcMatcher.group(2));
        this.dstRangeStart = Integer.parseInt(dstMatcher.group(1));
        this.dstRangeEnd = Integer.parseInt(dstMatcher.group(2));
    }

    public String getSrcFileName() {
        return srcFileName;
    }

    public String getDstFileName() {
        return dstFileName;
    }

    public String getDstFileNameOriginal() {
        return dstFileNameOriginal;
    }

    public int getCommitNum() {
        return commitNum;
    }

    @Override
    public String toString() {
        return "commit num: " + commitNum + "\n" + srcFileName + " -> " + dstFileName + "\n" + content + moveTo + "\n";
    }

    public String getIdentifier() {
        return identifier;
    }

    public int getSize() {
        return size;
    }

    public int getSrcRangeStart() {
        return srcRangeStart;
    }

    public int getSrcRangeEnd() {
        return srcRangeEnd;
    }

    public int getDstRangeStart() {
        return dstRangeStart;
    }

    public int getDstRangeEnd() {
        return dstRangeEnd;
    }


}