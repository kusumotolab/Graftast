package experimenter.validator;

import com.github.gumtreediff.client.Run;
import com.github.gumtreediff.client.diff.webdiff.WebDiff;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class GumtreeValidator {

    private final String gitDir;
    private final Repository repository;
    private final String srcFile;
    private final String dstFile;
    private final int commitNum;

    public GumtreeValidator(String gitDir, String srcFile, String dstFile, int num) throws IOException {
        this.gitDir = gitDir;
        this.srcFile = srcFile;
        this.dstFile = dstFile;
        this.commitNum = num;
        repository = new FileRepositoryBuilder()
                .findGitDir(new File(gitDir))
                .build();
        Run.initGenerators();
    }

    public static void main(String[] args) {
        if (args.length >= 4) {
            try {
                new GumtreeValidator(args[0], args[1], args[2], Integer.parseInt(args[3])).start();
            } catch (IOException | GitAPIException e) {
                e.printStackTrace();
            }
        }
        else
            System.out.println("Argument usage: [git directory] [srcFile] [dstFile] [commitNumber]");
    }

    public void start() throws GitAPIException, IOException {
        final Git git = new Git(repository);
        Iterator<RevCommit> revCommits = git.log().call().iterator();
        RevCommit srcCommit = null;
        RevCommit dstCommit = null;
        int counter = 1;
        while (revCommits.hasNext()) {
            if (counter == commitNum) {
                dstCommit = revCommits.next();
                srcCommit = revCommits.next();
                break;
            }
            revCommits.next();
            counter += 1;
        }

        String srcContent = Util.getFileContents(repository, srcCommit, srcFile);
        String dstContent = Util.getFileContents(repository, dstCommit, dstFile);

        writeFile("tmp/" + srcFile, srcContent);
        writeFile("tmp/" + dstFile, dstContent);

        String[] argsWebDiff = {"tmp/" + srcFile, "tmp/" + dstFile};
        WebDiff wd = new WebDiff(argsWebDiff);
        wd.run();
    }


    private void writeFile(String filePath, String content) throws IOException {
        if (Files.exists(Paths.get(filePath))) {
            Files.delete(Paths.get(filePath));
        }
        if (!Files.exists(Paths.get("tmp"))) {
            Files.createDirectory(Paths.get("tmp"));
        }
        List<String> list = new ArrayList<>();
        list.add(content);
        Files.write(Paths.get(filePath), list, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);

    }
}