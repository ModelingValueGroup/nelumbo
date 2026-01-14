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

package org.modelingvalue.nelumbo.lsp;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Stream;

import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.ProgressParams;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.SelectionRange;
import org.eclipse.lsp4j.WorkDoneProgressBegin;
import org.eclipse.lsp4j.WorkDoneProgressCreateParams;
import org.eclipse.lsp4j.WorkDoneProgressEnd;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.modelingvalue.nelumbo.AstElement;
import org.modelingvalue.nelumbo.Node;
import org.modelingvalue.nelumbo.collections.NList;
import org.modelingvalue.nelumbo.syntax.Token;

@SuppressWarnings("unused")
public class U {
    /**
     * Escape a string so it is safe to embed inside Markdown (for LSP hover, etc.).
     * - Converts HTML-sensitive characters to entities: &amp;, &lt;, &gt;
     * - Backslash-escapes common Markdown meta characters: *_`#[](){}!|
     */
    public static String escapeMarkdown(String s) {
        if (s == null || s.isEmpty()) {
            return "";
        }
        // First escape HTML special chars to avoid HTML interpretation in Markdown renderers
        String        htmlSafe = s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
        StringBuilder out      = new StringBuilder(htmlSafe.length() * 2);
        String        toEscape = "*_`#[](){}!|";
        for (int i = 0; i < htmlSafe.length(); i++) {
            char c = htmlSafe.charAt(i);
            if (toEscape.indexOf(c) >= 0) {
                out.append('\\');
            }
            out.append(c);
        }
        return out.toString();
    }

    private static final List<String> CLASSPATH_DIRS          = Arrays.asList(//
            "out/server/classes/java/main",//
            "out/server/classes/java/test" //
                                                                             );
    private static final List<String> NO_CLASS_PATH_DIR_NAMES = Arrays.asList(//
            "node_modules", //
            "src"//
                                                                             );
    private static final List<String> PROJECT_INDICATORS      = Arrays.asList(//
            "build.gradle.kts",//
            "build.gradle", //
            "pom.xml", //
            ".git"//
                                                                             );

    @SuppressWarnings("unused")
    public static List<Path> findClasspath(Path path) {
        List<Path> results = new ArrayList<>();

        for (String classpathDir : CLASSPATH_DIRS) {
            Path classpathPath = path.resolve(classpathDir);
            if (Files.exists(classpathPath)) {
                results.add(classpathPath);
            }
        }

        if (results.isEmpty()) {
            findClasspath(path, results);
        }

        return results;
    }

    public static void findClasspath(Path path, List<Path> results) {
        File[] files = path.toFile().listFiles();
        if (files == null) {
            return;
        }

        for (File file : files) {
            Path filePath = file.toPath();
            if (!Files.isDirectory(filePath) || filePath.getFileName().toString().startsWith(".") || NO_CLASS_PATH_DIR_NAMES.contains(filePath.getFileName().toString())) {
                continue;
            }

            boolean ok = false;
            for (String classpathDir : CLASSPATH_DIRS) {
                String[]     splitClasspath = classpathDir.split("/");
                List<String> reversedSplit  = new ArrayList<>();
                for (int i = splitClasspath.length - 1; i >= 0; i--) {
                    reversedSplit.add(splitClasspath[i]);
                }

                if (reversedSplit.getFirst().equals(filePath.getFileName().toString())) {
                    Path parent  = filePath.getParent();
                    int  okCount = 0;
                    for (int i = 1; i < reversedSplit.size(); i++) {
                        if (parent != null && parent.getFileName().toString().equals(reversedSplit.get(i))) {
                            parent = parent.getParent();
                            okCount++;
                        }
                    }
                    if (okCount == reversedSplit.size() - 1) {
                        ok = true;
                        break;
                    }
                }
            }

            if (!ok) {
                findClasspath(filePath, results);
            } else {
                results.add(filePath);
            }
        }
    }

    public static boolean isProject(Path path) {
        return PROJECT_INDICATORS.stream().anyMatch(file -> Files.exists(path.resolve(file)));
    }

    public static Collection<Path> findProjects(Path rootProject) {
        return findProjects(rootProject, new HashSet<>(), 0);
    }

    private static Collection<Path> findProjects(Path rootProject, Set<Path> results, int level) {
        if (isProject(rootProject)) {
            results.add(rootProject);
        }
        File[] files = rootProject.toFile().listFiles();
        if (files != null) {
            for (File file : files) {
                Path filePath = file.toPath();
                if (Files.isDirectory(filePath) && isProject(filePath)) {
                    results.add(filePath);
                    if (level < 4) {
                        findProjects(filePath, results, level + 1);
                    }
                }
            }
        }
        return results;
    }

    public static Map<Path, List<String>> findClassNames(List<Path> classpath) {
        Map<Path, List<String>> results = new HashMap<>();

        for (Path cp : classpath) {
            List<String> classNames = new ArrayList<>();

            if (Files.isDirectory(cp)) {
                try (Stream<Path> walk = Files.walk(cp)) {
                    walk.filter(path -> path.toString().endsWith(".class") && !path.getFileName().toString().contains("$")).forEach(classFile -> {
                        try {
                            Path   relativePath = cp.relativize(classFile);
                            String name         = relativePath.toString().substring(0, relativePath.toString().lastIndexOf(".")).replace(File.separator, ".");
                            classNames.add(name);
                        } catch (Exception e) {
                            // Handle path resolution errors
                        }
                    });
                } catch (IOException e) {
                    System.err.println("Error walking directory " + cp + ": " + e.getMessage());
                }
            } else if (cp.toString().endsWith(".jar")) {
                try (JarFile jarFile = new JarFile(cp.toFile())) {
                    Enumeration<JarEntry> entries = jarFile.entries();
                    while (entries.hasMoreElements()) {
                        JarEntry entry = entries.nextElement();
                        if (entry.isDirectory()) {
                            continue;
                        }

                        if (entry.getName().endsWith(".class") && Stream.of("META-INF", "module-info.class", "$").noneMatch(exclude -> entry.getName().contains(exclude))) {
                            String name = entry.getName().substring(0, entry.getName().lastIndexOf(".")).replace("/", ".");
                            classNames.add(name);
                        }
                    }
                } catch (IOException e) {
                    System.err.println("Error processing JAR file " + cp + ": " + e.getMessage());
                }
            }

            if (!classNames.isEmpty()) {
                results.put(cp, classNames);
            }
        }

        return results;
    }

    public static String snakeToCamel(String s) {
        if (s == null || s.isEmpty()) {
            return s;
        }
        String[]      parts = s.toLowerCase().split("_");
        StringBuilder out   = new StringBuilder(parts[0]);
        for (int i = 1; i < parts.length; i++) {
            String p = parts[i];
            if (!p.isEmpty()) {
                out.append(Character.toUpperCase(p.charAt(0)))//
                   .append(p.substring(1));
            }
        }
        return out.toString();
    }

    public static Path getLocation(Class<?> clazz) {
        try {
            return Paths.get(clazz.getProtectionDomain().getCodeSource().getLocation().toURI());
        } catch (Exception e) {
            throw new RuntimeException("Failed to get location for class: " + clazz.getName(), e);
        }
    }

    public static void progressBegin(String what) {
        Main.client.createProgress(new WorkDoneProgressCreateParams(Either.forLeft(what)));
        WorkDoneProgressBegin begin = new WorkDoneProgressBegin();
        begin.setTitle(what + " in progress");
        begin.setCancellable(false);
        Main.client.notifyProgress(new ProgressParams(Either.forLeft(what), Either.forLeft(begin)));
    }

    public static void progressEnd(String what) {
        WorkDoneProgressEnd end = new WorkDoneProgressEnd();
        end.setMessage(what + " done");
        Main.client.notifyProgress(new ProgressParams(Either.forLeft(what), Either.forLeft(end)));
    }

    public static void withProgress(String what, Runnable runnable) {
        progressBegin(what);
        runnable.run();
        progressEnd(what);
    }

    public static String render(SelectionRange sr) {
        StringBuilder b = new StringBuilder();
        while (sr != null) {
            if (!b.isEmpty()) {
                b.append(" <- ");
            }
            b.append(render(sr.getRange()));
            sr = sr.getParent();
        }
        return b.toString();
    }

    public static String render(Range r) {
        return renderSpan(r.getStart(), r.getEnd());
    }

    public static String render(Position p) {
        return String.format("%d:%d", p.getLine() + 1, p.getCharacter() + 1);
    }

    public static String render(Position p, List<Token> l) {
        return render(p) + '=' + findToken(p, l);
    }

    public static String render(List<Token> l) {
        return l.stream().map(U::render).reduce((a, b) -> a + " " + b).orElse("");
    }

    public static String render(Token t) {
        assert t != null;
        return t.toString();
    }

    public static String renderSpan(Node n) {
        return n == null ? "???" : renderSpan(n.firstToken(), n.lastToken());
    }

    public static String renderSpan(Token t) {
        return t == null ? "???" : renderSpan(t, t);
    }

    public static String renderSpan(Position t1, Position t2) {
        Integer l1 = t1 == null ? null : t1.getLine();
        Integer l2 = t1 == null ? null : t2.getLine();
        Integer c1 = t1 == null ? null : t1.getCharacter();
        Integer c2 = t1 == null ? null : t2.getCharacter();
        return renderSpan(l1, l2, c1, c2);
    }

    public static String renderSpan(Token t1, Token t2) {
        Integer l1 = t1 == null ? null : t1.line();
        Integer l2 = t1 == null ? null : (t2.numLines() == 0 ? t2.line() : t2.lastLine());
        Integer c1 = t1 == null ? null : t1.position();
        Integer c2 = t1 == null ? null : (t2.numChars() == 0 ? t2.position() : t2.lastPosition());
        return renderSpan(l1, l2, c1, c2);
    }

    private static String renderSpan(Integer l1, Integer l2, Integer c1, Integer c2) {
        String sl1 = l1 == null ? "???" : String.format("%d", l1 + 1);
        String sl2 = l2 == null ? "???" : String.format("%d", l2 + 1);
        String sc1 = l1 == null ? "???" : String.format("%d", c1 + 1);
        String sc2 = l2 == null ? "???" : String.format("%d", c2 + 1);
        if (!Objects.equals(l1, l2)) {
            return String.format("%s:%s..%s:%s", sl1, sc1, sl2, sc2);
        } else if (!Objects.equals(c1, c2)) {
            return String.format("%s:%s..%s    ", sl1, sc1, sc2);
        } else {
            return String.format("%s:%s         ", sl1, sc1);
        }
    }

    public static Token findToken(Position p, List<Token> tl) {
        if (tl == null || tl.isEmpty() || p == null) {
            return null;
        }
        return tl.stream()//
                 .filter(t -> contains(p, t))//
                 .findFirst()//
                 .orElse(null);
    }

    public static Node findNode(Position p, List<Node> nl) {
        if (nl == null || nl.isEmpty() || p == null) {
            return null;
        }
        return nl.stream()//
                 .filter(n -> contains(p, n))//
                 .findFirst()//
                 .orElse(null);
    }

    public static Range range(Token t) {
        assert t != null;
        return new Range(startPosition(t), endPosition(t));
    }

    public static Range range(Node n) {
        assert n != null;
        return new Range(startPosition(n), endPosition(n));
    }

    public static Range range(List<Token> ts) {
        assert ts != null && !ts.isEmpty();
        return new Range(startPosition(ts), endPosition(ts));
    }

    private static Position startPosition(Token t) {
        assert t != null;
        return new Position(t.line(), t.position());
    }

    private static Position startPosition(Node n) {
        assert n != null;
        return startPosition(n.firstToken());
    }

    private static Position startPosition(List<Token> ts) {
        assert ts != null && !ts.isEmpty();
        return startPosition(ts.getFirst());
    }

    private static Position endPosition(Token t) {
        assert t != null;
        return new Position(t.lastLine(), t.lastPosition());
    }

    private static Position endPosition(Node n) {
        assert n != null;
        return endPosition(n.lastToken());
    }

    private static Position endPosition(List<Token> ts) {
        assert ts != null && !ts.isEmpty();
        return endPosition(ts.getLast());
    }

    public static boolean contains(Position p, Node n) {
        return contains(p, n.tokens().toList());
    }

    public static boolean contains(Position p, Token t) {
        return t.contains(p.getLine(), p.getCharacter());
    }

    public static boolean contains(Position p, List<Token> ts) {
        return ts.stream().anyMatch(t -> contains(p, t));
    }

    public static SelectionRange makeSelectionRange(NlDocument document, Position p) {
        DEBUG("    SelectionRange at %10s:", U.render(p));
        Node           root  = document.parserResult().root();
        List<Node>     nodes = document.nodesAt(p).reversed();
        SelectionRange sr    = new SelectionRange(range(root), null);
        DEBUG("        - %10s (file root)", U.render(sr));
        for (Node node : nodes) {
            Range range = range(node);
            if (!range.equals(sr.getRange())) {
                DEBUG("        - %10s (node = %s)", U.render(range), node);
                sr = new SelectionRange(range, sr);
            }
        }
        Token token = findToken(p, document.tokens());
        Range range = range(token);
        DEBUG("        - %10s (token = %s)", U.render(range), token.debug());
        sr = new SelectionRange(range, sr);
        return sr;
    }

    public static void DEBUG(String format, Object... args) {
        if (Main.debugging()) {
            synchronized (System.err) {
                //noinspection RedundantStringFormatCall
                System.err.println(String.format(format, args));
            }
        }
    }

    public static void DEBUG_NODE(AstElement node, String indent) {
        if (Main.debugging()) {
            switch (node) {
                case Token t -> DEBUG("    %-20s%sT:%-16s  '%s'", renderSpan(t), indent, t.type(), t.textTraced());
                case NList l -> {
                    Node   declaration = l.declaration();
                    String decl        = declaration == null ? "<none>" : declaration.firstToken() == null ? "" + declaration : declaration.firstToken().fileName() + " @ " + renderSpan(declaration);
                    DEBUG("    %-20s%sL[%d]:%-16s  '%s'  => %s", renderSpan(l), indent, l.elements().size(), l.type(), l, decl);
                    l.astElements().forEach(e -> DEBUG_NODE(e, indent + "  "));
                }
                case Node n -> {
                    Node   declaration = n.declaration();
                    String decl        = declaration == null ? "<none>" : declaration.firstToken() == null ? "" + declaration : declaration.firstToken().fileName() + " @ " + renderSpan(declaration);
                    DEBUG("    %-20s%sN:%-16s  '%s'  => %s", renderSpan(n), indent, n.type(), n, decl);
                    n.astElements().forEach(e -> DEBUG_NODE(e, indent + "  "));
                }
                case null -> {
                    DEBUG("                    %-20s%s<null>", "???", indent);
                }
                default -> {
                    DEBUG("                    %-20s%s????? %s   %s", "???", indent, node.getClass().getSimpleName(), node);
                }
            }
        }
    }
}
