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

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

public final class NelumboCli {

    private NelumboCli() {
    }

    /** One unit of work: either a file path (or {@code -} for stdin), or an inline source from {@code -nl}. */
    private record Input(String file, String inlineSource) {
    }

    public static void main(String[] args) {
        // A double-clicked jar has no console and no arguments: open an interactive evaluate window
        // instead of printing usage to an invisible stderr.
        if (args.length == 0 && System.console() == null && !GraphicsEnvironment.isHeadless()) {
            runInteractively();
            return;
        }
        boolean quiet = false;
        java.util.List<Input> inputs = new ArrayList<>();
        for (int i = 0; i < args.length; i++) {
            String a = args[i];
            switch (a) {
            case "-q":
            case "--quiet":
                quiet = true;
                break;
            case "-nl":
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
            boolean ok = input.file() != null ? runFile(input.file(), quiet)
                    : runSource(input.inlineSource(), "<nelumbo>", quiet);
            if (!ok) {
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
        return runSource(source, name, quiet);
    }

    private static boolean runSource(String source, String name, boolean quiet) {
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

            JTextArea output = new JTextArea("(press Run to evaluate)");
            output.setEditable(false);
            output.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));

            JButton run = new JButton("Run");
            run.addActionListener(e -> {
                run.setEnabled(false);
                output.setText("running...");
                String source = input.getText();
                new Thread(() -> {
                    String text = evaluateToText(source);
                    SwingUtilities.invokeLater(() -> {
                        output.setText(text);
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
            JScrollPane outputScroll = new JScrollPane(output);
            outputScroll.setPreferredSize(new Dimension(720, 160));
            JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, inputScroll, outputScroll);
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

    private static String evaluateToText(String source) {
        NelumboEvaluator.EvalResult result;
        try {
            result = NelumboEvaluator.evaluate(source, "<input>", 0);
        } catch (RuntimeException e) {
            return "evaluation failed: " + e;
        }
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

                  <file>            path to a .nl file, or - to read stdin
                  -nl, --nelumbo S  evaluate the Nelumbo source given as argument S
                  -q, --quiet       suppress query result output (errors still printed)
                  -h, --help        show this help and exit

                Exit codes: 0 success, 1 parse/evaluation/comparison errors, 2 usage error.
                """;
    }
}
