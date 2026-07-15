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

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

import org.modelingvalue.json.Json;
import org.modelingvalue.json.JsonPrettyfier;

public final class NelumboCli {

    private NelumboCli() {
    }

    /** One unit of work: either a file path (or {@code -} for stdin), or an inline source from {@code -nl}. */
    private record Input(String file, String inlineSource) {
    }

    /** In {@code --json} mode all output is collected here and printed as one JSON object at the end. */
    private static final class JsonOutput {
        final java.util.List<String>                        errors    = new ArrayList<>();
        final java.util.List<java.util.Map<String, Object>> queries   = new ArrayList<>();
        final java.util.List<java.util.Map<String, Object>> parseTree = new ArrayList<>();
    }

    public static void main(String[] args) {
        // A double-clicked jar has no console and no arguments: open an interactive evaluate window
        // instead of printing usage to an invisible stderr.
        if (args.length == 0 && System.console() == null && !GraphicsEnvironment.isHeadless()) {
            runInteractively();
            return;
        }
        boolean quiet = false;
        JsonOutput json = null;
        java.util.List<Input> inputs = new ArrayList<>();
        for (int i = 0; i < args.length; i++) {
            String a = args[i];
            switch (a) {
            case "-q":
            case "--quiet":
                quiet = true;
                break;
            case "-j":
            case "--json":
                json = new JsonOutput();
                break;
            case "-n":
            case "--nelumbo":
                if (i + 1 >= args.length) {
                    System.err.println("nelumbo: missing value for " + a);
                    printUsage(System.err);
                    System.exit(2);
                    return;
                }
                inputs.add(new Input(null, args[++i]));
                break;
            case "-h":
            case "--help":
                printUsage(System.out);
                System.exit(0);
                return;
            case "-":
                inputs.add(new Input(a, null));
                break;
            default:
                if (a.startsWith("-")) {
                    System.err.println("nelumbo: unknown option: " + a);
                    printUsage(System.err);
                    System.exit(2);
                    return;
                }
                inputs.add(new Input(a, null));
            }
        }
        if (inputs.isEmpty()) {
            printUsage(System.err);
            System.exit(2);
            return;
        }
        int failed = 0;
        for (Input input : inputs) {
            boolean ok = input.file() != null ? runFile(input.file(), quiet, json)
                    : runSource(input.inlineSource(), "<nelumbo>", quiet, json);
            if (!ok) {
                failed++;
            }
        }
        if (json != null) {
            java.util.Map<String, Object> out = new LinkedHashMap<>();
            out.put("errors", json.errors);
            out.put("queries", json.queries);
            out.put("parseTree", json.parseTree);
            System.out.println(Json.toJson(out));
        }
        System.exit(failed == 0 ? 0 : 1);
    }

    private static boolean runFile(String file, boolean quiet, JsonOutput json) {
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
            report(json, file + ": no such file");
            return false;
        } catch (IOException e) {
            report(json, file + ": " + e.getMessage());
            return false;
        }
        return runSource(source, name, quiet, json);
    }

    private static void report(JsonOutput json, String message) {
        if (json != null) {
            json.errors.add(message);
        } else {
            System.err.println(message);
        }
    }

    private static boolean runSource(String source, String name, boolean quiet, JsonOutput json) {
        NelumboEvaluator.EvalResult result = NelumboEvaluator.evaluate(source, name, 0);
        for (NelumboEvaluator.Diagnostic d : result.diagnostics()) {
            report(json, name + ":" + d.line() + ":" + d.col() + ": " + d.message());
        }
        if (json != null) {
            json.parseTree.addAll(result.parseTree());
        }
        for (NelumboEvaluator.QueryOutcome q : result.queries()) {
            if (json != null) {
                json.queries.add(queryEntry(q));
            } else if (!quiet && q.result() != null) {
                System.out.println(q.query() + " ? " + q.result());
            }
        }
        return result.ok();
    }

    private static java.util.Map<String, Object> queryEntry(NelumboEvaluator.QueryOutcome q) {
        java.util.Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("query", q.query());
        entry.put("facts", q.facts());
        entry.put("falsehoods", q.falsehoods());
        return entry;
    }

    /** The same JSON object shape as batch {@code --json} mode, for one result. */
    private static java.util.Map<String, Object> jsonObject(NelumboEvaluator.EvalResult result, String name) {
        java.util.List<String> errors = new ArrayList<>();
        for (NelumboEvaluator.Diagnostic d : result.diagnostics()) {
            errors.add(name + ":" + d.line() + ":" + d.col() + ": " + d.message());
        }
        java.util.List<java.util.Map<String, Object>> queries = new ArrayList<>();
        for (NelumboEvaluator.QueryOutcome q : result.queries()) {
            queries.add(queryEntry(q));
        }
        java.util.Map<String, Object> out = new LinkedHashMap<>();
        out.put("errors", errors);
        out.put("queries", queries);
        out.put("parseTree", result.parseTree());
        return out;
    }

    private static final String EXAMPLE = """
            import nelumbo.integers

            Integer ::= fib(<Integer>)
            Integer n, f

            fib(n)=f <=> f=n if n<=1, f=fib(n-1)+fib(n-2) if n>1

            Integer r
            fib(7)=r ?
            """;

    private static void runInteractively() {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Nelumbo CLI");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            AppIcon.install(frame);

            JTextArea input = new JTextArea(EXAMPLE);
            input.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));

            JTextArea textOutput = new JTextArea("(press Run to evaluate)");
            textOutput.setEditable(false);
            textOutput.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
            JTextArea jsonOutput = new JTextArea("(press Run to evaluate)");
            jsonOutput.setEditable(false);
            jsonOutput.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));

            JButton run = new JButton("Run");
            run.addActionListener(e -> {
                run.setEnabled(false);
                textOutput.setText("running...");
                jsonOutput.setText("running...");
                String source = input.getText();
                new Thread(() -> {
                    String text;
                    String json;
                    try {
                        NelumboEvaluator.EvalResult result = NelumboEvaluator.evaluate(source, "<input>", 0);
                        text = renderText(result);
                        json = JsonPrettyfier.pretty(Json.toJson(jsonObject(result, "<input>")));
                    } catch (RuntimeException ex) {
                        text = "evaluation failed: " + ex;
                        json = text;
                    }
                    String textResult = text;
                    String jsonResult = json;
                    SwingUtilities.invokeLater(() -> {
                        textOutput.setText(textResult);
                        jsonOutput.setText(jsonResult);
                        run.setEnabled(true);
                    });
                }, "nelumbo-cli-run").start();
            });

            JTextArea usage = new JTextArea(usageText());
            usage.setEditable(false);
            usage.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 11));
            usage.setBorder(BorderFactory.createTitledBorder("Command-line usage"));

            JScrollPane inputScroll = new JScrollPane(input);
            inputScroll.setPreferredSize(new Dimension(720, 280));
            JTabbedPane outputTabs = new JTabbedPane();
            outputTabs.addTab("text", new JScrollPane(textOutput));
            outputTabs.addTab("json", new JScrollPane(jsonOutput));
            outputTabs.setPreferredSize(new Dimension(720, 200));
            JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, inputScroll, outputTabs);
            split.setResizeWeight(0.7);

            JPanel buttons = new JPanel();
            buttons.add(run);

            JPanel panel = new JPanel(new BorderLayout(0, 6));
            panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
            panel.add(split, BorderLayout.CENTER);
            JPanel south = new JPanel(new BorderLayout());
            south.add(buttons, BorderLayout.NORTH);
            south.add(usage, BorderLayout.CENTER);
            panel.add(south, BorderLayout.SOUTH);

            frame.setContentPane(panel);
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }

    private static String renderText(NelumboEvaluator.EvalResult result) {
        StringBuilder text = new StringBuilder();
        for (NelumboEvaluator.Diagnostic d : result.diagnostics()) {
            text.append(d.line()).append(":").append(d.col()).append(": ").append(d.message()).append("\n");
        }
        for (NelumboEvaluator.QueryOutcome q : result.queries()) {
            if (q.result() != null) {
                text.append(q.query()).append(" ? ").append(q.result()).append("\n");
            }
        }
        if (text.isEmpty()) {
            text.append("(no queries - input parsed and evaluated without errors)\n");
        }
        return text.toString();
    }

    private static void printUsage(PrintStream out) {
        out.print(usageText());
    }

    private static String usageText() {
        return """
                Usage: nelumbo [options] <file>...

                Parses and evaluates Nelumbo (.nl) files. Each query is inferred;
                queries with expected results [(facts)][(falsehoods)] are compared,
                and mismatches are reported as errors.

                  <file>           path to a .nl file, or - to read stdin
                  -n, --nelumbo S  evaluate the Nelumbo source given as argument S
                  -j, --json       output one JSON object: errors as an array of messages,
                                   per query the facts/falsehoods as name/value pairs,
                                   and the parse tree of the input
                  -q, --quiet      suppress query result output (errors still printed)
                  -h, --help       show this help and exit

                Exit codes: 0 success, 1 parse/evaluation/comparison errors, 2 usage error.
                """;
    }
}
