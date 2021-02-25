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
import com.github.gumtreediff.client.diff.webdiff.WebDiff;
import org.rendersnake.DocType;
import org.rendersnake.HtmlCanvas;
import org.rendersnake.Renderable;

import java.io.*;
import java.nio.charset.Charset;

import static org.rendersnake.HtmlAttributesFactory.*;

public class VanillaDiffViewMod implements Renderable {
    private VanillaDiffHtmlBuilderMod rawHtmlDiff;

    private File srcFile;
    private File dstFile;

    private Diff diff;

    private boolean dump;

    public VanillaDiffViewMod(File srcFile, File dstFile, Diff diff, boolean dump, String type) throws IOException {
        this.srcFile = srcFile;
        this.dstFile = dstFile;
        this.diff = diff;
        this.dump = dump;
        rawHtmlDiff = new VanillaDiffHtmlBuilderMod(srcFile, dstFile, diff, type);
        rawHtmlDiff.produce();
    }

    @Override
    public void renderOn(HtmlCanvas html) throws IOException {
        html
        .render(DocType.HTML5)
        .html(lang("en"))
            .render(new Header(dump))
            .body()
                .div(class_("container-fluid"))
                    .div(class_("row"))
                        .render(new MenuBar())
                    ._div()
                    .div(class_("row"))
                        .div(class_("col-6"))
                            .h5().content("srcProject")
                            .pre(class_("pre-scrollable")).content(rawHtmlDiff.getSrcDiff(), false)
                        ._div()
                        .div(class_("col-6"))
                            .h5().content("dstProject")
                            .pre(class_("pre-scrollable")).content(rawHtmlDiff.getDstDiff(), false)
                        ._div()
                    ._div()
                ._div()
            ._body()
        ._html();
    }

    private static class MenuBar implements Renderable {
        @Override
        public void renderOn(HtmlCanvas html) throws IOException {
            html
            .div(class_("col"))
                .div(class_("btn-toolbar justify-content-end"))
                    .div(class_("btn-group mr-2"))
                        .a(class_("btn btn-primary btn-sm").id("legend").href("#").add("data-toggle", "popover")
                                .add("data-html", "true").add("data-placement", "bottom")
                                .add("data-content", "<span class=&quot;del&quot;>&nbsp;&nbsp;</span> deleted<br>"
                                        + "<span class=&quot;add&quot;>&nbsp;&nbsp;</span> added<br>"
                                        + "<span class=&quot;mv&quot;>&nbsp;&nbsp;</span> moved<br>"
                                        + "<span class=&quot;upd&quot;>&nbsp;&nbsp;</span> updated<br>", false)
                                .add("data-original-title", "Legend").title("Legend").role("button")).content("Legend")
                        .a(class_("btn btn-primary btn-sm").id("shortcuts").href("#").add("data-toggle", "popover")
                                .add("data-html", "true").add("data-placement", "bottom")
                                .add("data-content", "<b>q</b> quit<br><b>l</b> list<br><b>n</b> next<br>"
                                        + "<b>t</b> top<br><b>b</b> bottom", false)
                                .add("data-original-title", "Shortcuts").title("Shortcuts").role("button"))
                            .content("Shortcuts")
                    ._div()
                    .div(class_("btn-group"))
                        .a(class_("btn btn-default btn-sm btn-primary").href("/list")).content("Back")
                        .a(class_("btn btn-default btn-sm btn-danger").href("/quit")).content("Quit")
                    ._div()
                ._div()
            ._div();
        }
    }

    private static class Header implements Renderable {

        private boolean dump;

        public Header(boolean dump) throws IOException {
            this.dump = dump;
        }

        @Override
        public void renderOn(HtmlCanvas html) throws IOException {
            if (!dump) {
                html
                        .head()
                           .meta(charset("utf8"))
                           .meta(name("viewport").content("width=device-width, initial-scale=1.0"))
                           .title().content("GumTree")
                           .macros().stylesheet(WebDiff.BOOTSTRAP_CSS_URL)
                           .macros().stylesheet("/dist/vanilla.css")
                           .macros().javascript(WebDiff.JQUERY_JS_URL)
                           .macros().javascript(WebDiff.POPPER_JS_URL)
                           .macros().javascript(WebDiff.BOOTSTRAP_JS_URL)
                           .macros().javascript("/dist/shortcuts.js")
                           .macros().javascript("/dist/vanilla.js")
                        ._head();
            }
            else {
                html
                        .head()
                           .meta(charset("utf8"))
                           .meta(name("viewport").content("width=device-width, initial-scale=1.0"))
                           .title().content("GumTree")
                           .macros().stylesheet(WebDiff.BOOTSTRAP_CSS_URL)
                           .style(type("text/css"))
                           .write(readFile("web/dist/vanilla.css"))
                           ._style()
                           .macros().javascript(WebDiff.JQUERY_JS_URL)
                           .macros().javascript(WebDiff.POPPER_JS_URL)
                           .macros().javascript(WebDiff.BOOTSTRAP_JS_URL)
                           .macros().script(readFile("web/dist/shortcuts.js"))
                           .macros().script(readFile("web/dist/vanilla.js"))
                        ._head();
            }
        }

        private static String readFile(String resourceName)  throws IOException {
            ClassLoader classloader = Thread.currentThread().getContextClassLoader();
            InputStream inputStream = classloader.getResourceAsStream(resourceName);
            InputStreamReader streamReader = new InputStreamReader(inputStream, Charset.defaultCharset());
            BufferedReader reader = new BufferedReader(streamReader);
            String content = "";
            for (String line; (line = reader.readLine()) != null;) {
                content += line + "\n";
            }
            return content;
        }
    }
}
