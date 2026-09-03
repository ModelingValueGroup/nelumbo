//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
// (C) Copyright 2018-2026 Modeling Value Group B.V. (http://modelingvalue.org)                                        ~
//                                                                                                                     ~
// Licensed under the GNU Lesser General Public License v3.0 (the 'License'). You may not use this file except in      ~
// compliance with the License. You may obtain a copy of the License at: https://choosealicense.com/licenses/lgpl-3.0  ~
// Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on ~
// an 'AS IS' BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the  ~
// specific language governing permissions and limitations under the License.                                          ~
//                                                                                                                     ~
// Maintainers:                                                                                                        ~
//     Wim Bast, Tom Brus                                                                                              ~
//                                                                                                                     ~
// Contributors:                                                                                                       ~
//     Victor Lap                                                                                                      ~
//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

package org.modelingvalue.nelumbo.website;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import org.commonmark.Extension;
import org.commonmark.ext.gfm.tables.TablesExtension;
import org.commonmark.node.AbstractVisitor;
import org.commonmark.node.Code;
import org.commonmark.node.Heading;
import org.commonmark.node.Link;
import org.commonmark.node.Node;
import org.commonmark.node.Text;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.AttributeProvider;
import org.commonmark.renderer.html.HtmlRenderer;

/**
 * The /docs pages: the markdown under {@code docs/} (bundled into the jar by the {@code copyDocs} Gradle task, listed
 * in {@code nelumbo-docs/index.txt}) rendered to HTML once at startup and wrapped in the {@code public/docs.html}
 * template. The docs were written for GitHub, so heading ids follow GitHub's slug rules (the docs' cross-page anchors
 * depend on it), relative {@code .md} links become {@code /docs/...html} URLs, and links that leave the docs folder
 * (source files, the README) go to the GitHub repository.
 */
public final class DocsSite {

    public static final String URL_PREFIX = "/docs/";

    static final String GITHUB_BLOB    = "https://github.com/ModelingValueGroup/nelumbo/blob/master/";
    static final String GITHUB_TREE    = "https://github.com/ModelingValueGroup/nelumbo/tree/master/";
    static final String INDEX_PAGE     = "documentation.md";
    static final String RESOURCE_ROOT  = "/nelumbo-docs/";
    static final String TEMPLATE       = "/public/docs.html";

    /** Sidebar groups: the docs folder, in reading order; pages elsewhere (except the index) are served but not listed. */
    private static final List<String[]> GROUPS = List.of(
            new String[]{"getting-started", "Getting started"},
            new String[]{"reference", "Reference"},
            new String[]{"reference/stdlib", "Standard library"},
            new String[]{"guides", "Guides"},
            new String[]{"explanation", "Explanation"});

    private static final List<Extension> EXTENSIONS = List.of(TablesExtension.create());
    private static final Parser          PARSER     = Parser.builder().extensions(EXTENSIONS).build();

    private record Page(String path, String url, String title, Node document) {
    }

    private final String              template;
    private final Map<String, String> htmlByUrl = new LinkedHashMap<>();

    /** Loads the docs bundled on the classpath. */
    public static DocsSite load() {
        Map<String, String> markdown = new LinkedHashMap<>();
        for (String line : readResource(RESOURCE_ROOT + "index.txt").split("\n")) {
            if (!line.isBlank()) {
                markdown.put(line.trim(), readResource(RESOURCE_ROOT + line.trim()));
            }
        }
        return new DocsSite(markdown, readResource(TEMPLATE));
    }

    /** {@code markdownByPath}: docs-relative path ({@code reference/grammar.md}) to markdown source. */
    DocsSite(Map<String, String> markdownByPath, String template) {
        this.template = template;
        List<Page> pages = new ArrayList<>();
        markdownByPath.forEach((path, md) -> {
            Node document = PARSER.parse(md);
            pages.add(new Page(path, urlOf(path), firstHeading(document).orElse(path), document));
        });
        for (Page page : pages) {
            HtmlRenderer renderer = HtmlRenderer.builder().extensions(EXTENSIONS)
                    .attributeProviderFactory(context -> new DocsAttributes(page.path())).build();
            String title = page.path().equals(INDEX_PAGE) ? page.title() : page.title() + " - Nelumbo docs";
            htmlByUrl.put(page.url(), fill(title, nav(pages, page.url()), renderer.render(page.document())));
        }
    }

    /** The rendered page at {@code urlPath} ({@code /docs/} or {@code /docs/<path>.html}), if there is one. */
    public Optional<String> page(String urlPath) {
        String url = urlPath.equals("/docs") ? URL_PREFIX : urlPath;
        return Optional.ofNullable(htmlByUrl.get(url));
    }

    /** A 404 page in the docs layout, so the sidebar stays available. */
    public String notFoundPage() {
        return fill("Page not found - Nelumbo docs", nav(List.of(), ""),
                "<h1>Page not found</h1><p>There is no such page in the documentation. Try the <a href=\"/docs/\">overview</a>.</p>");
    }

    public int pageCount() {
        return htmlByUrl.size();
    }

    private String fill(String title, String nav, String content) {
        return template.replace("@TITLE@", title).replace("@NAV@", nav).replace("@CONTENT@", content);
    }

    private String nav(List<Page> pages, String activeUrl) {
        StringBuilder sb = new StringBuilder();
        sb.append(navLink(URL_PREFIX, "Overview", activeUrl)).append('\n');
        for (String[] group : GROUPS) {
            List<Page> members = pages.stream()
                    .filter(p -> dirOf(p.path()).equals(group[0]))
                    .sorted(Comparator.comparing(Page::title, String.CASE_INSENSITIVE_ORDER))
                    .toList();
            if (!members.isEmpty()) {
                sb.append("<div class=\"group\">").append(escape(group[1])).append("</div>\n");
                for (Page p : members) {
                    sb.append(navLink(p.url(), p.title(), activeUrl)).append('\n');
                }
            }
        }
        return sb.toString();
    }

    private static String navLink(String url, String title, String activeUrl) {
        return "<a href=\"" + url + "\"" + (url.equals(activeUrl) ? " class=\"active\"" : "") + ">" + escape(title) + "</a>";
    }

    private static String dirOf(String path) {
        int slash = path.lastIndexOf('/');
        return slash < 0 ? "" : path.substring(0, slash);
    }

    /** Docs-relative {@code .md} path to its URL: the index is {@code /docs/}, everything else {@code /docs/<path>.html}. */
    static String urlOf(String path) {
        if (path.equals(INDEX_PAGE)) {
            return URL_PREFIX;
        }
        return URL_PREFIX + (path.endsWith(".md") ? path.substring(0, path.length() - 3) + ".html" : path);
    }

    /**
     * Rewrites a link as it appears in {@code pagePath}'s markdown. Absolute URLs and in-page anchors pass through;
     * a relative link is resolved against the page, and then either points at another doc page (rewritten to its URL)
     * or leaves the docs folder (rewritten to the GitHub repository, {@code tree} for directories).
     */
    static String rewriteLink(String href, String pagePath) {
        if (href.startsWith("#") || href.contains(":")) {
            return href;
        }
        int    hash     = href.indexOf('#');
        String path     = hash < 0 ? href : href.substring(0, hash);
        String fragment = hash < 0 ? "" : href.substring(hash);
        if (path.isEmpty()) {
            return href;
        }
        String resolved;
        try {
            resolved = URI.create(URL_PREFIX + pagePath).resolve(path).getPath();
        } catch (IllegalArgumentException e) {
            return href;
        }
        if (resolved == null) {
            return href;
        }
        if (resolved.startsWith(URL_PREFIX)) {
            String rel = resolved.substring(URL_PREFIX.length());
            if (rel.endsWith(".md")) {
                return urlOf(rel) + fragment;
            }
            if (rel.endsWith("/") && !rel.isEmpty()) {
                return GITHUB_TREE + "docs/" + rel;
            }
            return resolved + fragment;
        }
        String repoPath = resolved.startsWith("/") ? resolved.substring(1) : resolved;
        return (repoPath.endsWith("/") ? GITHUB_TREE : GITHUB_BLOB) + repoPath + fragment;
    }

    /** GitHub's heading slug: lower-case, drop everything but letters, digits, {@code -} and {@code _}, each space becomes a {@code -}. */
    static String slug(String headingText) {
        StringBuilder sb = new StringBuilder();
        headingText.toLowerCase(Locale.ROOT).codePoints().forEach(cp -> {
            if (Character.isLetterOrDigit(cp) || cp == '-' || cp == '_') {
                sb.appendCodePoint(cp);
            } else if (cp == ' ') {
                sb.append('-');
            }
        });
        return sb.toString();
    }

    private static Optional<String> firstHeading(Node document) {
        for (Node n = document.getFirstChild(); n != null; n = n.getNext()) {
            if (n instanceof Heading h && h.getLevel() == 1) {
                return Optional.of(textOf(h));
            }
        }
        return Optional.empty();
    }

    private static String textOf(Node node) {
        StringBuilder sb = new StringBuilder();
        node.accept(new AbstractVisitor() {
            @Override
            public void visit(Text text) {
                sb.append(text.getLiteral());
            }

            @Override
            public void visit(Code code) {
                sb.append(code.getLiteral());
            }
        });
        return sb.toString();
    }

    private static String escape(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    private static String readResource(String path) {
        try (InputStream in = DocsSite.class.getResourceAsStream(path)) {
            if (in == null) {
                throw new IllegalStateException("resource not found on classpath: " + path);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("cannot read resource " + path, e);
        }
    }

    /** Per-page heading ids (GitHub-numbered on duplicates) and link rewriting. */
    private static final class DocsAttributes implements AttributeProvider {
        private final String               pagePath;
        private final Map<String, Integer> seen = new HashMap<>();

        DocsAttributes(String pagePath) {
            this.pagePath = pagePath;
        }

        @Override
        public void setAttributes(Node node, String tagName, Map<String, String> attributes) {
            if (node instanceof Heading heading) {
                String  base  = slug(textOf(heading));
                Integer count = seen.merge(base, 0, (old, one) -> old + 1);
                attributes.put("id", count == 0 ? base : base + "-" + count);
            } else if (node instanceof Link && attributes.containsKey("href")) {
                attributes.put("href", rewriteLink(attributes.get("href"), pagePath));
            }
        }
    }
}
