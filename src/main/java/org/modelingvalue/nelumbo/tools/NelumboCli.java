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

package org.modelingvalue.nelumbo.tools;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.ArrayList;

public final class NelumboCli {

    private NelumboCli() {
    }

    public static void main(String[] args) {
        boolean quiet = false;
        java.util.List<String> files = new ArrayList<>();
        for (String a : args) {
            switch (a) {
            case "-q":
            case "--quiet":
                quiet = true;
                break;
            case "-h":
            case "--help":
                printUsage(System.out);
                System.exit(0);
                return;
            case "-":
                files.add(a);
                break;
            default:
                if (a.startsWith("-")) {
                    System.err.println("nelumbo: unknown option: " + a);
                    printUsage(System.err);
                    System.exit(2);
                    return;
                }
                files.add(a);
            }
        }
        if (files.isEmpty()) {
            printUsage(System.err);
            System.exit(2);
            return;
        }
        int failed = 0;
        for (String file : files) {
            if (!runFile(file, quiet)) {
                failed++;
            }
        }
        System.exit(failed == 0 ? 0 : 1);
    }

    private static boolean runFile(String file, boolean quiet) {
        String source;
        String name;
        try {
            if ("-".equals(file)) {
                source = new String(System.in.readAllBytes(), StandardCharsets.UTF_8);
                name = "<stdin>";
            } else {
                source = Files.readString(Path.of(file), StandardCharsets.UTF_8);
                name = file;
            }
        } catch (NoSuchFileException e) {
            System.err.println(file + ": no such file");
            return false;
        } catch (IOException e) {
            System.err.println(file + ": " + e.getMessage());
            return false;
        }
        NelumboEvaluator.EvalResult result = NelumboEvaluator.evaluate(source, name, 0);
        for (NelumboEvaluator.Diagnostic d : result.diagnostics()) {
            System.err.println(name + ":" + d.line() + ":" + d.col() + ": " + d.message());
        }
        if (!quiet) {
            for (NelumboEvaluator.QueryOutcome q : result.queries()) {
                if (q.result() != null) {
                    System.out.println(q.query() + " ? " + q.result());
                }
            }
        }
        return result.ok();
    }

    private static void printUsage(PrintStream out) {
        out.println("Usage: nelumbo [options] <file>...");
        out.println();
        out.println("Parses and evaluates Nelumbo (.nl) files. Each query is inferred;");
        out.println("queries with expected results [(facts)][(falsehoods)] are compared,");
        out.println("and mismatches are reported as errors.");
        out.println();
        out.println("  <file>         path to a .nl file, or - to read stdin");
        out.println("  -q, --quiet    suppress query result output (errors still printed)");
        out.println("  -h, --help     show this help and exit");
        out.println();
        out.println("Exit codes: 0 success, 1 parse/evaluation/comparison errors, 2 usage error.");
    }
}
