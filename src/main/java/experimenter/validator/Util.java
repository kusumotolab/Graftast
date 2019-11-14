package experimenter.validator;

import org.eclipse.jgit.lib.FileMode;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.TreeWalk;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class Util {

    public static String getFileContents(Repository repository, RevCommit commit, String file) {
        try (RevWalk walk = new RevWalk(repository)) {
            RevTree tree = walk.parseTree(commit.getTree());

            TreeWalk treeWalk = new TreeWalk(repository);
            treeWalk.addTree(tree);
            treeWalk.setRecursive(true);

            while (treeWalk.next()) {
                if (treeWalk.getFileMode().equals(FileMode.GITLINK)) //Submoduleは無視
                    continue;
                if (!treeWalk.getNameString().equals(file)) //指定ディレクトリ以外は無視
                    continue;
                ObjectLoader loader;
                loader = repository.open(treeWalk.getObjectId(0));
                OutputStream outputStream = new ByteArrayOutputStream();
                loader.copyTo(outputStream);
                String contents = outputStream.toString();
                outputStream.close();
                return contents;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "";
    }
}