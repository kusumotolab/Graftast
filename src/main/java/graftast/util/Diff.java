package graftast.util;

import com.sksamuel.diffpatch.DiffMatchPatch;

import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;

public class Diff {

    public static boolean diff(String srcCode, String dstCode) {
        // google-Diff-match-patch
        DiffMatchPatch dmp = new DiffMatchPatch();
        LinkedList<DiffMatchPatch.Diff> diff = dmp.diff_main(srcCode, dstCode);
        dmp.diff_cleanupEfficiency(diff);
        return isContainOnlySpaceCharDiff(diff);
    }

    private static boolean isContainOnlySpaceCharDiff(List<DiffMatchPatch.Diff> diff) {
        for (DiffMatchPatch.Diff df: diff) {
            if (df.operation != DiffMatchPatch.Operation.EQUAL) {
                Pattern p = Pattern.compile("[\\s]+");
                java.util.regex.Matcher m = p.matcher(df.text);
                if (!m.matches())
                    return false;
            }
        }
        return true;
    }

}
