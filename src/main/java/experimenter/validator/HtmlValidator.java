package experimenter.validator;

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
import java.util.LinkedList;
import java.util.List;

public class HtmlValidator {

    private final Repository repository;
    private List<Container> list = new LinkedList<>();
    private List<String> htmlContents = new LinkedList<>();

    public HtmlValidator(String[] args) throws IOException {
        repository = new FileRepositoryBuilder()
                .findGitDir(new File(args[0]))
                .build();
        for (int i = 1; i < args.length; i += 3) {
            list.add(new Container(args[i], args[i+1], args[i+2]));
        }
    }

    public static void main(String[] args) {
        try {
            new HtmlValidator(args).start();
        } catch (IOException | GitAPIException e) {
            e.printStackTrace();
        }
    }

    public void start() throws GitAPIException, IOException {
        final Git git = new Git(repository);
        Iterable<RevCommit> log = git.log().call();
        List<RevCommit> revCommits = new LinkedList<>();
        log.forEach(revCommits::add);
        int index = 1;
        for (Container sample: list) {
            RevCommit srcCommit = null;
            RevCommit dstCommit = null;
            int counter = 1;
            for (int i = 0; i < revCommits.size(); i++) {
                if (counter == sample.commitNum) {
                    srcCommit = revCommits.get(i);
                    dstCommit = revCommits.get(i + 1);
                    break;
                }
                counter += 1;
            }
            String srcContent = Util.getFileContents(repository, srcCommit, sample.srcFile);
            String dstContent = Util.getFileContents(repository, dstCommit, sample.dstFile);
            htmlContents.add(generateHtml(sample.srcFile, srcContent, sample.dstFile, dstContent, String.valueOf(index)));
            index += 1;
        }

        writeHtmlContents();
    }

    private String generateHtml(String srcFile, String srcContent, String dstFile, String dstContent, String title) {
        StringBuilder builder = new StringBuilder("<!DOCTYPE html><html lang=\"en\"><head><meta charset=\"utf8\"/><meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\"/><link type=\"text/css\" href=\"dist/bootstrap.min.css\" rel=\"stylesheet\"/><title>");
        builder.append(title);
        builder.append("</title><link rel=\"stylesheet\" href=\"https://stackpath.bootstrapcdn.com/bootstrap/4.3.1/css/bootstrap.min.css\" integrity=\"sha384-ggOyR0iXCbMQv3Xipma34MD+dH/1fQ784/j6cY/iJTQUOhcWr7x9JvoRxT2MZw1T\" crossorigin=\"anonymous\"><link type=\"text/css\" href=\"dist/gumtree.css\" rel=\"stylesheet\"/></head><body><div class=\"container-fluid\"><div class=\"row\"><div class=\"col-lg-12\"></div></div><div class=\"row\"><div class=\"col-lg-6 max-height\"><h5>");
        builder.append(srcFile);
        builder.append("</h5><div style=\"height:100vh;overflow-auto;\"><pre class=\"pre max-height\">");
        builder.append(escapeHtmlTag(srcContent));
        builder.append("</pre></div></div><div class=\"col-lg-6 max-height\"><h5>");
        builder.append(dstFile);
        builder.append("</h5><div style=\"height:100vh;overflow-auto;\"><pre class=\"pre max-height\">");
        builder.append(escapeHtmlTag(dstContent));
        builder.append("</pre></div></div></div></div><script src=\"https://code.jquery.com/jquery-3.3.1.slim.min.js\" integrity=\"sha384-q8i/X+965DzO0rT7abK41JStQIAqVgRVzpbzo5smXKp4YfRvH+8abtTE1Pi6jizo\" crossorigin=\"anonymous\"></script><script src=\"https://cdnjs.cloudflare.com/ajax/libs/popper.js/1.14.6/umd/popper.min.js\" integrity=\"sha384-wHAiFfRlMFy6i5SRaxvfOCifBUQy1xHdJ/yoi7FRNXMRBu5WHdZYu1hA6ZOblgut\" crossorigin=\"anonymous\"></script><script src=\"https://stackpath.bootstrapcdn.com/bootstrap/4.2.1/js/bootstrap.min.js\" integrity=\"sha384-B0UglyR+jN6CkvvICOB2joaf5I4l3gm9GU6Hc1og6Ls7i6U/mkkaduKaBhlAXv9k\" crossorigin=\"anonymous\"></script></body></html>");
        return new String(builder);
    }

    private void writeHtmlContents() throws IOException {
        int index = 1;
        if (!Files.exists(Paths.get("tmp/html"))) {
            Files.createDirectory(Paths.get("tmp/html"));
        }
        for (String str: htmlContents) {
            List<String> list = new ArrayList<>();
            list.add(str);
            String filePath = "tmp/html/" + index + ".html";
            if (Files.exists(Paths.get(filePath))) {
                Files.delete(Paths.get(filePath));
            }
            Files.write(Paths.get(filePath), list, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
            index += 1;
        }
    }

    private String escapeHtmlTag(String content) {
        content = content.replace("&", "&amp;");
        content = content.replace("<", "&lt;");
        content = content.replace(">", "&gt;");
        return content;
    }

}

class Container {
    String srcFile;
    String dstFile;
    int commitNum;

    Container(String src, String dst, String n) {
        this.srcFile = src;
        this.dstFile = dst;
        this.commitNum = Integer.parseInt(n);
    }
}