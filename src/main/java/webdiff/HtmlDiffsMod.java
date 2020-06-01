package webdiff;

import com.github.gumtreediff.actions.Diff;
import com.github.gumtreediff.actions.ITreeClassifier;
import com.github.gumtreediff.client.diff.web.TagIndex;
import com.github.gumtreediff.tree.ITree;
import com.github.gumtreediff.tree.TreeContext;
import com.github.gumtreediff.utils.SequenceAlgorithms;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import graftast.GraftFileList;
import graftast.SourceElement;

import java.io.*;
import java.util.List;

public final class HtmlDiffsMod {

    private static final String SRC_MV_SPAN = "<span class=\"%s\" id=\"move-src-%d\" data-title=\"%s\">";
    private static final String DST_MV_SPAN = "<span class=\"%s\" id=\"move-dst-%d\" data-title=\"%s\">";
    private static final String ADD_DEL_SPAN = "<span class=\"%s\" data-title=\"%s\">";
    private static final String UPD_SPAN = "<span class=\"cupd\">";
    private static final String ID_SPAN = "<span class=\"marker\" id=\"mapping-%d\"></span>";
    private static final String END_SPAN = "</span>";

    private String srcDiff;

    private String dstDiff;

    GraftFileList graftFileList;

    private Diff diff;

    public HtmlDiffsMod(GraftFileList graftFileList, Diff diff) {
        this.graftFileList = graftFileList;
        this.diff = diff;
    }

    public void produce() throws IOException {
        ITreeClassifier c = diff.createRootNodesClassifier();
        TObjectIntMap<ITree> mappingIds = new TObjectIntHashMap<>();

        int uId = 1;
        int mId = 1;

        TagIndex ltags = new TagIndex();
        for (ITree t: diff.src.getRoot().preOrder()) {
            if (c.getMovedSrcs().contains(t)) {
                mappingIds.put(diff.mappings.getDstForSrc(t), mId);
                ltags.addStartTag(t.getPos(), String.format(ID_SPAN, uId++));
                ltags.addTags(t.getPos(), String.format(
                        SRC_MV_SPAN, "token mv", mId++, tooltip(diff.src, t)), t.getEndPos(), END_SPAN);
            }
            if (c.getUpdatedSrcs().contains(t)) {
                mappingIds.put(diff.mappings.getDstForSrc(t), mId);
                ltags.addStartTag(t.getPos(), String.format(ID_SPAN, uId++));
                ltags.addTags(t.getPos(), String.format(
                        SRC_MV_SPAN, "token upd", mId++, tooltip(diff.src, t)), t.getEndPos(), END_SPAN);
                List<int[]> hunks = SequenceAlgorithms.hunks(t.getLabel(), diff.mappings.getDstForSrc(t).getLabel());
                for (int[] hunk: hunks)
                    ltags.addTags(t.getPos() + hunk[0], UPD_SPAN, t.getPos() + hunk[1], END_SPAN);

            }
            if (c.getDeletedSrcs().contains(t)) {
                ltags.addStartTag(t.getPos(), String.format(ID_SPAN, uId++));
                ltags.addTags(t.getPos(), String.format(
                        ADD_DEL_SPAN, "token del", tooltip(diff.src, t)), t.getEndPos(), END_SPAN);
            }
        }

        TagIndex rtags = new TagIndex();
        for (ITree t: diff.dst.getRoot().preOrder()) {
            if (c.getMovedDsts().contains(t)) {
                int dId = mappingIds.get(t);
                rtags.addStartTag(t.getPos(), String.format(ID_SPAN, uId++));
                rtags.addTags(t.getPos(), String.format(
                        DST_MV_SPAN, "token mv", dId, tooltip(diff.dst, t)), t.getEndPos(), END_SPAN);
            }
            if (c.getUpdatedDsts().contains(t)) {
                int dId = mappingIds.get(t);
                rtags.addStartTag(t.getPos(), String.format(ID_SPAN, uId++));
                rtags.addTags(t.getPos(), String.format(
                        DST_MV_SPAN, "token upd", dId, tooltip(diff.dst, t)), t.getEndPos(), END_SPAN);
                List<int[]> hunks = SequenceAlgorithms.hunks(diff.mappings.getSrcForDst(t).getLabel(), t.getLabel());
                for (int[] hunk: hunks)
                    rtags.addTags(t.getPos() + hunk[2], UPD_SPAN, t.getPos() + hunk[3], END_SPAN);
            }
            if (c.getInsertedDsts().contains(t)) {
                rtags.addStartTag(t.getPos(), String.format(ID_SPAN, uId++));
                rtags.addTags(t.getPos(), String.format(
                        ADD_DEL_SPAN, "token add", tooltip(diff.dst, t)), t.getEndPos(), END_SPAN);
            }
        }

        StringBuilder builder1 = new StringBuilder();
        for (SourceElement se: graftFileList.getSrcFiles()) {
            builder1.append("\n")
                    .append("--------------------------------------------------\n")
                    .append("\n")
                    .append(se.getProjectRelativePath()).append("\n")
                    .append("\n")
                    .append("--------------------------------------------------\n")
                    .append(se.getContent());
        }
        StringWriter w1 = new StringWriter();
        StringReader r = new StringReader(new String(builder1));
        int cursor = 0;
        int length = builder1.length();

        while (cursor < length) {
            char cr = (char) r.read();
            w1.append(ltags.getEndTags(cursor));
            w1.append(ltags.getStartTags(cursor));
            append(cr, w1);
            cursor++;
        }
        w1.append(ltags.getEndTags(cursor));
        r.close();
        srcDiff = w1.toString();

        StringBuilder builder2 = new StringBuilder();
        for (SourceElement se: graftFileList.getDstFiles()) {
            builder2.append("\n")
                    .append("--------------------------------------------------\n")
                    .append("\n")
                    .append(se.getProjectRelativePath()).append("\n")
                    .append("\n")
                    .append("--------------------------------------------------\n")
                    .append(se.getContent());
        }
        StringWriter w2 = new StringWriter();
        r = new StringReader(new String(builder2));
        cursor = 0;
        length = builder2.length();

        while (cursor < length) {
            char cr = (char) r.read();
            w2.append(rtags.getEndTags(cursor));
            w2.append(rtags.getStartTags(cursor));
            append(cr, w2);
            cursor++;
        }
        w2.append(rtags.getEndTags(cursor));
        r.close();

        dstDiff = w2.toString();
    }

    public String getSrcDiff() {
        return srcDiff;
    }

    public String getDstDiff() {
        return dstDiff;
    }

    private static String tooltip(TreeContext ctx, ITree t) {
        return (t.getParent() != null)
                ? t.getParent().getType() + "/" + t.getType() : t.getType().toString();
    }

    private static void append(char cr, Writer w) throws IOException {
        if (cr == '<') w.append("&lt;");
        else if (cr == '>') w.append("&gt;");
        else if (cr == '&') w.append("&amp;");
        else w.append(cr);
    }
}

