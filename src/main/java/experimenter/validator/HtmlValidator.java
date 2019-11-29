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
        for (int i = 1; i < args.length; i += 5) {
            list.add(new Container(args[i], args[i+1], args[i+2], args[i+3], args[i+4]));
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
        List<RevCommit> revCommits = new ArrayList<>();
        log.forEach(revCommits::add);
        int index = 1;
        for (Container sample: list) {
            RevCommit srcCommit = revCommits.get(sample.commitNum);
            RevCommit dstCommit = revCommits.get(sample.commitNum - 1);

            String srcContentOld = Util.getFileContents(repository, srcCommit, sample.srcFile);
            String dstContentNew = Util.getFileContents(repository, dstCommit, sample.dstFile);
            String srcContentNew = Util.getFileContents(repository, dstCommit, sample.srcFile);
            String dstContentOld = Util.getFileContents(repository, srcCommit, sample.dstFile);
            htmlContents.add(generateHtml(sample, srcContentOld, srcContentNew, dstContentOld, dstContentNew, String.valueOf(index)));
            index += 1;
        }

        writeHtmlContents();
    }

    private String generateHtml(Container c, String srcContentOld, String srcContentNew, String dstContentOld, String dstContentNew, String title) {
        srcContentOld = escapeHtmlTagPre(srcContentOld);
        srcContentNew = escapeHtmlTagPre(srcContentNew);
        dstContentOld = escapeHtmlTagPre(dstContentOld);
        dstContentNew = escapeHtmlTagPre(dstContentNew);
        String[] contents = insertMarker(srcContentOld, dstContentNew, c);
        srcContentOld = contents[0];
        dstContentNew = contents[1];

        StringBuilder builder = new StringBuilder("<!DOCTYPE html><html lang=\"en\"><head><meta charset=\"utf8\"/><meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\"/><link type=\"text/css\" href=\"dist/bootstrap.min.css\" rel=\"stylesheet\"/><title>");
        builder.append(title);
        builder.append("</title><link rel=\"stylesheet\" href=\"https://stackpath.bootstrapcdn.com/bootstrap/4.3.1/css/bootstrap.min.css\" integrity=\"sha384-ggOyR0iXCbMQv3Xipma34MD+dH/1fQ784/j6cY/iJTQUOhcWr7x9JvoRxT2MZw1T\" crossorigin=\"anonymous\"><link type=\"text/css\" href=\"dist/gumtree.css\" rel=\"stylesheet\"/></head><body><div class=\"container-fluid\"><div class=\"row\"><div class=\"col-lg-12\"></div></div><div class=\"row\"><div class=\"col-lg-6 max-height\"><h5>");
        builder.append(c.srcFile).append("(Old)");
        builder.append("</h5><div style=\"height:50vh;overflow-auto;\"><pre class=\"pre max-height\">");
        builder.append(escapeHtmlTag(srcContentOld));
        builder.append("</pre></div></div><div class=\"col-lg-6 max-height\"><h5>");
        if (c.dstFile.equals(c.dstFileOrig))
            builder.append(c.dstFile).append(")(New)");
        else
            builder.append(c.dstFile).append("(Original:").append(c.dstFileOrig).append(")(New)");
        builder.append("</h5><div style=\"height:50vh;overflow-auto;\"><pre class=\"pre max-height\">");
        builder.append(escapeHtmlTag(dstContentNew));
        builder.append("</pre></div></div><div class=\"col-lg-6 max-height\"><h5>");

        builder.append(c.srcFile).append("(New)");
        builder.append("</h5><div style=\"height:50vh;overflow-auto;\"><pre class=\"pre max-height\">");
        builder.append(escapeHtmlTag(srcContentNew));
        builder.append("</pre></div></div><div class=\"col-lg-6 max-height\"><h5>");
        builder.append(c.dstFile).append("(Old)");
        builder.append("</h5><div style=\"height:50vh;overflow-auto;\"><pre class=\"pre max-height\">");
        builder.append(escapeHtmlTag(dstContentOld));
        builder.append("</pre></div></div></div></div><script src=\"https://code.jquery.com/jquery-3.3.1.slim.min.js\" integrity=\"sha384-q8i/X+965DzO0rT7abK41JStQIAqVgRVzpbzo5smXKp4YfRvH+8abtTE1Pi6jizo\" crossorigin=\"anonymous\"></script><script src=\"https://cdnjs.cloudflare.com/ajax/libs/popper.js/1.14.6/umd/popper.min.js\" integrity=\"sha384-wHAiFfRlMFy6i5SRaxvfOCifBUQy1xHdJ/yoi7FRNXMRBu5WHdZYu1hA6ZOblgut\" crossorigin=\"anonymous\"></script><script src=\"https://stackpath.bootstrapcdn.com/bootstrap/4.2.1/js/bootstrap.min.js\" integrity=\"sha384-B0UglyR+jN6CkvvICOB2joaf5I4l3gm9GU6Hc1og6Ls7i6U/mkkaduKaBhlAXv9k\" crossorigin=\"anonymous\"></script><script type=\"text/javascript\" src=\"dist/diff.js\"></script></body></html>");
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

    private String[] insertMarker(String srcContent, String dstContent, Container c) {
        StringBuilder srcBuilder = new StringBuilder(srcContent);
        try {
            srcBuilder.insert(c.srcRangeEnd, "</span>");
            srcBuilder.insert(c.srcRangeStart, "<span class=\"marker\" id=\"mapping-1\"></span><span class=\"token mv\" id=\"move-src-1\" data-title=\"\">");
        } catch (StringIndexOutOfBoundsException e) {
        }
        StringBuilder dstBuilder = new StringBuilder(dstContent);
        try {
            dstBuilder.insert(c.dstRangeEnd, "</span>");
            dstBuilder.insert(c.dstRangeStart, "<span class=\"marker\" id=\"mapping-2\"></span><span class=\"token mv\" id=\"move-dst-1\" data-title=\"\">");
        } catch (StringIndexOutOfBoundsException e) {
        }
        return new String[]{new String(srcBuilder), new String(dstBuilder)};
    }

    private String escapeHtmlTag(String content) {
        content = content.replace("あんど", "&amp;");
        content = content.replace("だいなり", "&lt;");
        content = content.replace("しょうなり", "&gt;");
        return content;
    }

    private String escapeHtmlTagPre(String content) {
        content = content.replace("&", "あんど");
        content = content.replace("<", "だいなり");
        content = content.replace(">", "しょうなり");
        return content;
    }

}

class Container {
    String srcFile;
    String dstFile;
    String dstFileOrig;
    int commitNum;

    int srcRangeStart;
    int srcRangeEnd;
    int dstRangeStart;
    int dstRangeEnd;

    Container(String src, String dst, String dstOrig, String n, String range) {
        this.srcFile = src;
        this.dstFile = dst;
        this.dstFileOrig = dstOrig;
        this.commitNum = Integer.parseInt(n);
        String[] str = range.split(",");
        srcRangeStart = Integer.parseInt(str[0]);
        srcRangeEnd = Integer.parseInt(str[1]);
        dstRangeStart = Integer.parseInt(str[2]);
        dstRangeEnd = Integer.parseInt(str[3]);
    }
}