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
 *
 * This software is modified on Feb. 2021.
 * Copyright 2021 Akira Fujimoto <a-fujimt@ist.osaka-u.ac.jp>
 */

package webdiff;

import com.github.gumtreediff.actions.Diff;
import com.github.gumtreediff.actions.EditScript;
import com.github.gumtreediff.actions.SimplifiedChawatheScriptGenerator;
import com.github.gumtreediff.client.Option;
import com.github.gumtreediff.client.Register;
import com.github.gumtreediff.client.diff.AbstractDiffClient;
import com.github.gumtreediff.client.diff.webdiff.*;
import com.github.gumtreediff.gen.Registry;
import com.github.gumtreediff.io.DirectoryComparator;
import com.github.gumtreediff.matchers.MappingStore;
import com.github.gumtreediff.matchers.Matcher;
import com.github.gumtreediff.matchers.Matchers;
import com.github.gumtreediff.tree.TreeContext;
import com.github.gumtreediff.utils.Pair;
import graftast.ProjectTreeGenerator;
import org.rendersnake.HtmlCanvas;
import org.rendersnake.Renderable;
import spark.Spark;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;

import static spark.Spark.*;

@Register(description = "Web diff client", options = WebDiff.WebDiffOptions.class, priority = Registry.Priority.HIGH)
public class WebDiffMod extends AbstractDiffClient<WebDiffMod.WebDiffOptions> {
    public static final String JQUERY_JS_URL = "https://code.jquery.com/jquery-3.4.1.min.js";
    public static final String BOOTSTRAP_CSS_URL = "https://stackpath.bootstrapcdn.com/bootstrap/4.4.1/css/bootstrap.min.css";
    public static final String BOOTSTRAP_JS_URL = "https://stackpath.bootstrapcdn.com/bootstrap/4.4.1/js/bootstrap.min.js";
    public static final String POPPER_JS_URL = " https://cdn.jsdelivr.net/npm/popper.js@1.16.0/dist/umd/popper.min.js";

    private String type;

    public WebDiffMod(String[] args) {
        super(args);
        if (args.length >= 3)
            type = args[2];
    }

    public static class WebDiffOptions extends AbstractDiffClient.DiffOptions {
        public static final int DEFAULT_PORT = 4567;
        public int port = DEFAULT_PORT;

        @Override
        public Option[] values() {
            return Option.Context.addValue(super.values(),
                    new Option("--port", String.format("Set server port (default to %d).", DEFAULT_PORT), 1) {
                        @Override
                        protected void process(String name, String[] args) {
                            int p = Integer.parseInt(args[0]);
                            if (p > 0)
                                port = p;
                            else
                                System.err.printf("Invalid port number (%s), using %d.\n", args[0], port);
                        }
                    }
            );
        }
    }

    @Override
    protected WebDiffOptions newOptions() {
        return new WebDiffOptions();
    }

    @Override
    public void run() {
        DirectoryComparator comparator = new DirectoryComparator(opts.srcPath, opts.dstPath);
        comparator.compare();
        configureSpark(comparator, opts.port);
        Spark.awaitInitialization();
        System.out.println(String.format("Starting server: %s:%d.", "http://127.0.0.1", opts.port));
    }

    public void configureSpark(final DirectoryComparator comparator, int port) {
        port(port);
        staticFiles.location("/web/");
        get("/", (request, response) -> {
            if (comparator.isDirMode())
                response.redirect("/list");
            else
                response.redirect("/vanilla-diff/0");
            return "";
        });
        get("/list", (request, response) -> {
            Renderable view = new DirectoryDiffView(comparator);
            return render(view);
        });
        get("/vanilla-diff/:id", (request, response) -> {
            int id = Integer.parseInt(request.params(":id"));
            Diff diff = getDiff(opts.srcPath, opts.dstPath);
            Renderable view = new VanillaDiffViewMod(new File(opts.srcPath), new File(opts.dstPath), diff, false, type);
            return render(view);
        });
        get("/monaco-diff/:id", (request, response) -> {
            int id = Integer.parseInt(request.params(":id"));
            Pair<File, File> pair = comparator.getModifiedFiles().get(id);
            Diff diff = super.getDiff(pair.first.getAbsolutePath(), pair.second.getAbsolutePath());
            Renderable view = new MonacoDiffView(pair.first, pair.second, diff, id);
            return render(view);
        });
        get("/monaco-native-diff/:id", (request, response) -> {
            int id = Integer.parseInt(request.params(":id"));
            Pair<File, File> pair = comparator.getModifiedFiles().get(id);
            Renderable view = new MonacoNativeDiffView(pair.first, pair.second, id);
            return render(view);
        });
        get("/mergely-diff/:id", (request, response) -> {
            int id = Integer.parseInt(request.params(":id"));
            Renderable view = new MergelyDiffView(id);
            return render(view);
        });
        get("/raw-diff/:id", (request, response) -> {
            int id = Integer.parseInt(request.params(":id"));
            Pair<File, File> pair = comparator.getModifiedFiles().get(id);
            Diff diff = getDiff(opts.srcPath, opts.dstPath);
            Renderable view = new TextDiffView(new File(opts.srcPath), new File(opts.dstPath), diff);
            return render(view);
        });
        get("/left/:id", (request, response) -> {
            int id = Integer.parseInt(request.params(":id"));
            Pair<File, File> pair = comparator.getModifiedFiles().get(id);
            return readFile(pair.first.getAbsolutePath(), Charset.defaultCharset());
        });
        get("/right/:id", (request, response) -> {
            int id = Integer.parseInt(request.params(":id"));
            Pair<File, File> pair = comparator.getModifiedFiles().get(id);
            return readFile(pair.second.getAbsolutePath(), Charset.defaultCharset());
        });
        get("/quit", (request, response) -> {
            System.exit(0);
            return "";
        });
    }

    private static String render(Renderable r) throws IOException {
        HtmlCanvas c = new HtmlCanvas();
        r.renderOn(c);
        return c.toHtml();
    }

    private static String readFile(String path, Charset encoding)  throws IOException {
        byte[] encoded = Files.readAllBytes(Paths.get(path));
        return new String(encoded, encoding);
    }

    @Override
    protected Diff getDiff(String src, String dst) throws IOException {
        Pair<TreeContext, TreeContext> projectTreeContextPair = new ProjectTreeGenerator(src, dst, type).getProjectTreeContextPair();
        TreeContext srcTree = projectTreeContextPair.first;
        TreeContext dstTree = projectTreeContextPair.second;
        Matcher m = Matchers.getInstance().getMatcherWithFallback(opts.matcherId);
        m.configure(opts.properties);
        MappingStore mappings = m.match(srcTree.getRoot(), dstTree.getRoot());
        EditScript editScript = new SimplifiedChawatheScriptGenerator().computeActions(mappings);
        return new Diff(srcTree, dstTree, mappings, editScript);
    }

}
