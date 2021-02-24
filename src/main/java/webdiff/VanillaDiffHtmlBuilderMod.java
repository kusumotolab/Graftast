/*
 * This file is part of GumTree.
 *
 * GumTree is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * GumTree is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with GumTree.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Copyright 2011-2015 Jean-Rémy Falleri <jr.falleri@gmail.com>
 * Copyright 2011-2015 Floréal Morandat <florealm@gmail.com>

 * This software is modified on Feb. 2021.
 * Copyright 2021 Akira Fujimoto <a-fujimt@ist.osaka-u.ac.jp>
 */

package webdiff;

import com.github.gumtreediff.actions.Diff;
import com.github.gumtreediff.actions.TreeClassifier;
import com.github.gumtreediff.tree.Tree;
import com.github.gumtreediff.tree.TreeContext;
import com.github.gumtreediff.utils.Pair;
import com.github.gumtreediff.utils.SequenceAlgorithms;
import graftast.GraftFileList;
import graftast.GraftFileSelector;
import graftast.SourceElement;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class VanillaDiffHtmlBuilderMod {

    private static final String SRC_MV_SPAN = "<span class=\"%s\" id=\"move-src-%d\" data-title=\"%s\">";
    private static final String DST_MV_SPAN = "<span class=\"%s\" id=\"move-dst-%d\" data-title=\"%s\">";
    private static final String ADD_DEL_SPAN = "<span class=\"%s\" data-title=\"%s\">";
    private static final String UPD_SPAN = "<span class=\"cupd\">";
    private static final String ID_SPAN = "<span class=\"marker\" id=\"mapping-%d\"></span>";
    private static final String END_SPAN = "</span>";

    private String srcDiff;

    private String dstDiff;

    private File fSrc;

    private File fDst;

    private String fileType;

    private Diff diff;

    public VanillaDiffHtmlBuilderMod(File fSrc, File fDst, Diff diff, String type) {
        this.fSrc = fSrc;
        this.fDst = fDst;
        this.diff = diff;
        this.fileType = type;
    }

    public void produce() throws IOException {
        GraftFileList graftFileList = new GraftFileSelector().run(fSrc.getAbsolutePath(), fDst.getAbsolutePath(), fileType);
        Pair<TreeContext, TreeContext> treePair = new Pair<>(diff.src, diff.dst);
        new TreeModifier(graftFileList).modify(treePair);

        TreeClassifier c = diff.createRootNodesClassifier();
        Object2IntMap<Tree> mappingIds = new Object2IntOpenHashMap<>();

        int uId = 1;
        int mId = 1;

        TagIndex ltags = new TagIndex();
        for (Tree t: diff.src.getRoot().preOrder()) {
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
        for (Tree t: diff.dst.getRoot().preOrder()) {
            if (c.getMovedDsts().contains(t)) {
                int dId = mappingIds.getInt(t);
                rtags.addStartTag(t.getPos(), String.format(ID_SPAN, uId++));
                rtags.addTags(t.getPos(), String.format(
                                DST_MV_SPAN, "token mv", dId, tooltip(diff.dst, t)), t.getEndPos(), END_SPAN);
            }
            if (c.getUpdatedDsts().contains(t)) {
                int dId = mappingIds.getInt(t);
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
        BufferedReader r = new BufferedReader(new StringReader(new String(builder1)));
        int cursor = 0;

        while (r.ready()) {
            char cr = (char) r.read();
            if (cr == '\uFFFF')
                break;
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
        r = new BufferedReader(new StringReader(new String(builder2)));
        cursor = 0;

        while (r.ready()) {
            char cr = (char) r.read();
            if (cr == '\uFFFF')
                break;
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

    private static String tooltip(TreeContext ctx, Tree t) {
        return (t.getParent() != null)
                ? t.getParent().getType() + "/" + t.getType() : t.getType().toString();
    }

    private static void append(char cr, Writer w) throws IOException {
        if (cr == '<') w.append("&lt;");
        else if (cr == '>') w.append("&gt;");
        else if (cr == '&') w.append("&amp;");
        else w.append(cr);
    }

    private static class TagIndex {

        private Map<Integer, List<String>> startTags;

        private Map<Integer, List<String>> endTags;

        public TagIndex() {
            startTags = new HashMap<Integer, List<String>>();
            endTags = new HashMap<Integer, List<String>>();
        }

        public void addTags(int pos, String startTag, int endPos, String endTag) {
            addStartTag(pos, startTag);
            addEndTag(endPos, endTag);
        }

        public void addStartTag(int pos, String tag) {
            if (!startTags.containsKey(pos)) startTags.put(pos, new ArrayList<String>());
            startTags.get(pos).add(tag);
        }

        public void addEndTag(int pos, String tag) {
            if (!endTags.containsKey(pos)) endTags.put(pos, new ArrayList<String>());
            endTags.get(pos).add(tag);
        }

        public String getEndTags(int pos) {
            if (!endTags.containsKey(pos)) return "";
            StringBuilder b = new StringBuilder();
            for (String s: endTags.get(pos)) b.append(s);
            return b.toString();
        }

        public String getStartTags(int pos) {
            if (!startTags.containsKey(pos))
                return "";
            StringBuilder b = new StringBuilder();
            for (String s: startTags.get(pos))
                b.append(s);
            return b.toString();
        }

    }
}
