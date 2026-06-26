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

package org.modelingvalue.nelumbo.http;

import java.io.IOException;
import java.io.PrintStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

import org.modelingvalue.nelumbo.KnowledgeBase;

/**
 * Command-line entry point: loads the given {@code .nl} files/directories into a base knowledge base and serves it over
 * HTTP. Usage: {@code nelumbo-http [--port N] <file-or-dir>...}.
 */
public final class Main {

    private Main() {
    }

    public static void main(String[] args) {
        int               port      = 8080;
        long              timeoutMs = NelumboHttpServer.DEFAULT_TIMEOUT_MS;
        List<Path>        paths     = new ArrayList<>();
        for (int i = 0; i < args.length; i++) {
            String a = args[i];
            switch (a) {
            case "-p":
            case "--port":
                if (i + 1 >= args.length) {
                    fail("missing value for " + a);
                }
                port = Integer.parseInt(args[++i]);
                break;
            case "-t":
            case "--timeout":
                if (i + 1 >= args.length) {
                    fail("missing value for " + a);
                }
                timeoutMs = Long.parseLong(args[++i]);
                break;
            case "-h":
            case "--help":
                printUsage(System.out);
                return;
            default:
                if (a.startsWith("-")) {
                    fail("unknown option: " + a);
                }
                paths.add(Path.of(a));
            }
        }
        if (paths.isEmpty()) {
            printUsage(System.err);
            System.exit(2);
            return;
        }

        List<NamedSource> sources = new ArrayList<>();
        List<String>      files   = new ArrayList<>();
        for (Path path : paths) {
            for (Path file : expand(path)) {
                sources.add(new NamedSource(file.toString(), read(file)));
                files.add(file.getFileName().toString());
            }
        }
        if (sources.isEmpty()) {
            fail("no .nl files found in: " + paths);
        }

        KnowledgeBase base   = KnowledgeBaseLoader.load(sources);
        NelumboHttpServer server = new NelumboHttpServer(base, files, timeoutMs);
        int bound = server.start(port);
        System.out.println("Nelumbo HTTP server listening on http://localhost:" + bound
                + " (" + files.size() + " file(s) loaded, timeout " + timeoutMs + " ms)");
    }

    private static List<Path> expand(Path path) {
        if (Files.isDirectory(path)) {
            try (Stream<Path> walk = Files.walk(path)) {
                return walk.filter(Files::isRegularFile).filter(p -> p.toString().endsWith(".nl"))
                        .sorted(Comparator.comparing(Path::toString)).toList();
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
        return List.of(path);
    }

    private static String read(Path file) {
        try {
            return Files.readString(file, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("cannot read " + file, e);
        }
    }

    private static void fail(String message) {
        System.err.println("nelumbo-http: " + message);
        System.exit(2);
    }

    private static void printUsage(PrintStream out) {
        out.println("Usage: nelumbo-http [--port N] <file-or-dir>...");
        out.println();
        out.println("Loads the given .nl files (directories are scanned for *.nl) into a knowledge base");
        out.println("and serves it over HTTP. Endpoints:");
        out.println("  POST /eval         evaluate a posted Nelumbo document, returns query results as JSON");
        out.println("  POST /eval/trace   like /eval, with a (currently stubbed) trace field");
        out.println("  GET  /metadata     knowledge base metadata (types, counts, loaded files)");
        out.println("  GET  /health       liveness check");
        out.println();
        out.println("  -p, --port N      port to listen on (default 8080; 0 picks a free port)");
        out.println("  -t, --timeout MS  per-request inference budget in ms (default 30000; 0 disables)");
        out.println("  -h, --help        show this help and exit");
    }
}
