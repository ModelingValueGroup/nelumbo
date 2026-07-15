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

package org.modelingvalue.nelumbo.cli;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
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
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.JTextArea;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;

import org.modelingvalue.json.Json;
import org.modelingvalue.json.JsonPrettyfier;
import org.modelingvalue.nelumbo.KnowledgeBase;
import org.modelingvalue.nelumbo.server.KnowledgeBaseLoader;
import org.modelingvalue.nelumbo.server.NamedSource;
import org.modelingvalue.nelumbo.server.NelumboServer;
import org.modelingvalue.nelumbo.tools.AppIcon;
import org.modelingvalue.nelumbo.tools.NelumboEvaluator;
import org.modelingvalue.nelumbo.tools.NelumboLaf;

public final class NelumboCli {

    private NelumboCli() {
    }

    /** One unit of work: either a file path (or {@code -} for stdin), or an inline source from {@code -nl}. */
    private record Input(String file, String inlineSource) {
    }

    /** The stdlib modules selectable for prepping ({@code --prep} / the prep tab). */
    private static final java.util.List<String> STDLIB_MODULES = java.util.List.of(
            "logic", "integers", "strings", "collections", "rationals", "datetime");

    /** Builds the import preamble for the given stdlib module names; null when none selected. */
    private static String prepPreamble(java.util.List<String> modules) {
        if (modules.isEmpty()) {
            return null;
        }
        StringBuilder preamble = new StringBuilder();
        for (String module : modules) {
            preamble.append("import nelumbo.").append(module).append("\n");
        }
        return preamble.toString();
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
        boolean trace = false;
        JsonOutput json = null;
        Integer serverPort = null;
        long timeoutMs = NelumboServer.DEFAULT_TIMEOUT_MS;
        java.util.List<String> prep = new ArrayList<>();
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
            case "-p":
            case "--prep":
                if (i + 1 >= args.length) {
                    System.err.println("nelumbo: missing value for " + a);
                    printUsage(System.err);
                    System.exit(2);
                    return;
                }
                for (String module : args[++i].split(",")) {
                    module = module.trim();
                    if ("all".equals(module)) {
                        prep.clear();
                        prep.addAll(STDLIB_MODULES);
                    } else if (STDLIB_MODULES.contains(module)) {
                        if (!prep.contains(module)) {
                            prep.add(module);
                        }
                    } else {
                        System.err.println("nelumbo: unknown stdlib module: " + module + " (known: all, " + String.join(", ", STDLIB_MODULES) + ")");
                        System.exit(2);
                        return;
                    }
                }
                break;
            case "-s":
            case "--server":
                if (i + 1 >= args.length) {
                    System.err.println("nelumbo: missing value for " + a);
                    printUsage(System.err);
                    System.exit(2);
                    return;
                }
                serverPort = Integer.parseInt(args[++i]);
                break;
            case "-t":
            case "--timeout":
                if (i + 1 >= args.length) {
                    System.err.println("nelumbo: missing value for " + a);
                    printUsage(System.err);
                    System.exit(2);
                    return;
                }
                timeoutMs = Long.parseLong(args[++i]);
                break;
            case "--trace":
                trace = true;
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
        String preamble = prepPreamble(prep);
        if (serverPort != null) {
            runServer(serverPort, timeoutMs, inputs, preamble);
            return; // no exit: the server's dispatcher thread keeps the JVM alive
        }
        if (inputs.isEmpty()) {
            printUsage(System.err);
            System.exit(2);
            return;
        }
        int failed = 0;
        for (Input input : inputs) {
            boolean ok = input.file() != null ? runFile(input.file(), quiet, json, preamble)
                    : runSource(input.inlineSource(), "<nelumbo>", quiet, json, preamble);
            if (!ok) {
                failed++;
            }
        }
        if (json != null) {
            java.util.Map<String, Object> out = new LinkedHashMap<>();
            out.put("errors", json.errors);
            out.put("queries", json.queries);
            out.put("parseTree", json.parseTree);
            if (trace) {
                addTraceStub(out);
            }
            System.out.println(Json.toJson(out));
        } else if (trace) {
            System.out.println("trace: not-implemented");
        }
        System.exit(failed == 0 ? 0 : 1);
    }

    /** Loads the inputs into a base knowledge base and serves it over HTTP (the old nelumbo-cli-server). */
    private static void runServer(int port, long timeoutMs, java.util.List<Input> inputs, String preamble) {
        java.util.List<NamedSource> sources = new ArrayList<>();
        java.util.List<String>      files   = new ArrayList<>();
        int inlineCount = 0;
        if (preamble != null) {
            sources.add(new NamedSource("<prep>", preamble));
        }
        for (Input input : inputs) {
            if (input.file() != null) {
                for (Path file : expand(Path.of(input.file()))) {
                    String name = file.toString();
                    sources.add(new NamedSource(name, read(file)));
                    files.add(name);
                }
            } else {
                String name = "<nelumbo-" + ++inlineCount + ">";
                sources.add(new NamedSource(name, input.inlineSource()));
                files.add(name);
            }
        }
        KnowledgeBase base   = KnowledgeBaseLoader.load(sources);
        NelumboServer server = new NelumboServer(base, files, timeoutMs);
        int bound = server.start(port);
        System.out.println("Nelumbo server listening on http://localhost:" + bound
                + " (" + files.size() + " source(s) loaded" + (preamble != null ? " + stdlib prep" : "")
                + ", timeout " + timeoutMs + " ms)");
    }

    private static java.util.List<Path> expand(Path path) {
        if (Files.isDirectory(path)) {
            try (java.util.stream.Stream<Path> walk = Files.walk(path)) {
                return walk.filter(Files::isRegularFile).filter(p -> p.toString().endsWith(".nl"))
                        .sorted(java.util.Comparator.comparing(Path::toString)).toList();
            } catch (IOException e) {
                throw new java.io.UncheckedIOException(e);
            }
        }
        return java.util.List.of(path);
    }

    private static String read(Path file) {
        try {
            return Files.readString(file, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new java.io.UncheckedIOException("cannot read " + file, e);
        }
    }

    private static boolean runFile(String file, boolean quiet, JsonOutput json, String preamble) {
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
        return runSource(source, name, quiet, json, preamble);
    }

    private static void report(JsonOutput json, String message) {
        if (json != null) {
            json.errors.add(message);
        } else {
            System.err.println(message);
        }
    }

    private static boolean runSource(String source, String name, boolean quiet, JsonOutput json, String preamble) {
        NelumboEvaluator.EvalResult result = NelumboEvaluator.evaluate(source, name, 0, preamble);
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

    /** The JSON object as a swing tree: maps and lists become branches, everything else leaves. */
    private static DefaultMutableTreeNode treeNode(String label, Object value) {
        if (value instanceof java.util.Map<?, ?> map) {
            DefaultMutableTreeNode node = new DefaultMutableTreeNode(label);
            for (java.util.Map.Entry<?, ?> entry : map.entrySet()) {
                node.add(treeNode(String.valueOf(entry.getKey()), entry.getValue()));
            }
            return node;
        }
        if (value instanceof java.util.List<?> list) {
            DefaultMutableTreeNode node = new DefaultMutableTreeNode(label + " [" + list.size() + "]");
            int i = 0;
            for (Object item : list) {
                node.add(treeNode("[" + i++ + "]", item));
            }
            return node;
        }
        return new DefaultMutableTreeNode(label + ": " + value);
    }

    /** The same stub the server's /eval/trace endpoint returns; real tracing is not implemented yet. */
    private static void addTraceStub(java.util.Map<String, Object> object) {
        object.put("trace", null);
        object.put("traceStatus", "not-implemented");
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
        NelumboLaf.setup();
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Nelumbo CLI");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            AppIcon.install(frame);

            JTextArea input = new JTextArea(EXAMPLE);
            input.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));

            JTextArea textOutput = new JTextArea();
            textOutput.setEditable(false);
            textOutput.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
            JTextArea jsonOutput = new JTextArea();
            jsonOutput.setEditable(false);
            jsonOutput.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
            JTextArea usage = new JTextArea(usageText());
            usage.setEditable(false);
            usage.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));

            JScrollPane outputScroll = new JScrollPane(textOutput);

            // json tab: radio-switched text (pretty json) and tree views of the same object
            JTree jsonTree = new JTree(new DefaultMutableTreeNode("(press Run to evaluate)"));
            jsonTree.setRootVisible(true);
            CardLayout jsonCards = new CardLayout();
            JPanel jsonViews = new JPanel(jsonCards);
            jsonViews.add(new JScrollPane(jsonOutput), "text");
            jsonViews.add(new JScrollPane(jsonTree), "tree");
            JRadioButton viewText = new JRadioButton("text", true);
            JRadioButton viewTree = new JRadioButton("tree");
            ButtonGroup viewGroup = new ButtonGroup();
            viewGroup.add(viewText);
            viewGroup.add(viewTree);
            viewText.addActionListener(e -> jsonCards.show(jsonViews, "text"));
            viewTree.addActionListener(e -> jsonCards.show(jsonViews, "tree"));
            JPanel jsonBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 2));
            jsonBar.add(new JLabel("view as:"));
            jsonBar.add(viewText);
            jsonBar.add(viewTree);
            JPanel jsonTab = new JPanel(new BorderLayout());
            jsonTab.add(jsonBar, BorderLayout.NORTH);
            jsonTab.add(jsonViews, BorderLayout.CENTER);
            JButton eval = new JButton("Eval");
            JButton evalTrace = new JButton("Trace");
            JPanel buttons = new JPanel();
            buttons.add(eval);
            buttons.add(evalTrace);
            // the Eval buttons and the example picker live on the nelumbo tab only
            javax.swing.JComboBox<String> examples = new javax.swing.JComboBox<>();
            examples.addItem("choose...");
            for (String name : NelumboServer.exampleNames()) {
                examples.addItem(name);
            }
            examples.addActionListener(e -> {
                String name = (String) examples.getSelectedItem();
                if (name != null && !"choose...".equals(name)) {
                    String source = NelumboServer.exampleSource(name);
                    if (source != null) {
                        input.setText(source);
                        input.setCaretPosition(0);
                    }
                }
            });
            JPanel examplesBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 2));
            examplesBar.add(new JLabel("example:"));
            examplesBar.add(examples);
            JPanel nelumboTab = new JPanel(new BorderLayout(0, 6));
            nelumboTab.add(examplesBar, BorderLayout.NORTH);
            nelumboTab.add(new JScrollPane(input), BorderLayout.CENTER);
            nelumboTab.add(buttons, BorderLayout.SOUTH);

            // prep tab: which stdlib modules are loaded by default, for Run as well as the server
            java.util.Map<String, javax.swing.JCheckBox> prepBoxes = new LinkedHashMap<>();
            JPanel prepTab = new JPanel();
            prepTab.setLayout(new javax.swing.BoxLayout(prepTab, javax.swing.BoxLayout.Y_AXIS));
            prepTab.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
            prepTab.add(new JLabel("Stdlib modules loaded by default (for Eval and the server):"));
            javax.swing.JCheckBox allBox = new javax.swing.JCheckBox("all");
            prepTab.add(allBox);
            for (String module : STDLIB_MODULES) {
                javax.swing.JCheckBox box = new javax.swing.JCheckBox("import nelumbo." + module);
                prepBoxes.put(module, box);
                prepTab.add(box);
            }
            allBox.addActionListener(e -> prepBoxes.values().forEach(box -> box.setSelected(allBox.isSelected())));
            prepBoxes.values().forEach(box -> box.addActionListener(
                    e -> allBox.setSelected(prepBoxes.values().stream().allMatch(javax.swing.JCheckBox::isSelected))));
            java.util.function.Supplier<String> selectedPreamble = () -> {
                java.util.List<String> selected = new ArrayList<>();
                prepBoxes.forEach((module, box) -> {
                    if (box.isSelected()) {
                        selected.add(module);
                    }
                });
                return prepPreamble(selected);
            };

            // server tab: start/stop the HTTP eval server, serving the nelumbo tab content as knowledge base
            JTextField portField = new JTextField("8080", 6);
            JButton startStop = new JButton("Start");
            JButton openBrowser = new JButton("Open in Browser");
            openBrowser.setEnabled(false);
            JLabel serverStatus = new JLabel(" ");
            NelumboServer[] running = { null };
            int[] boundPort = { 0 };
            JLabel statRequests = new JLabel("-");
            JLabel statTiming = new JLabel("-");
            JLabel statUptime = new JLabel("-");
            JLabel statMemory = new JLabel("-");
            javax.swing.Timer poll = new javax.swing.Timer(1000, e -> {
                if (running[0] != null) {
                    NelumboServer server = running[0];
                    statRequests.setText(server.requestCount() + " (" + server.errorCount() + " errors)");
                    statTiming.setText(String.format("avg %.1f ms, last %.1f ms", server.averageHandleMillis(), server.lastHandleMillis()));
                    long up = server.uptimeMillis() / 1000;
                    statUptime.setText(String.format("%d:%02d:%02d", up / 3600, up / 60 % 60, up % 60));
                    Runtime rt = Runtime.getRuntime();
                    long used = (rt.totalMemory() - rt.freeMemory()) / (1024 * 1024);
                    long max = rt.maxMemory() / (1024 * 1024);
                    statMemory.setText(used + " MB used of " + max + " MB max");
                }
            });
            JPanel statsGrid = new JPanel(new java.awt.GridLayout(0, 2, 12, 2));
            statsGrid.add(new JLabel("requests handled:"));
            statsGrid.add(statRequests);
            statsGrid.add(new JLabel("handling time:"));
            statsGrid.add(statTiming);
            statsGrid.add(new JLabel("uptime:"));
            statsGrid.add(statUptime);
            statsGrid.add(new JLabel("memory (JVM heap):"));
            statsGrid.add(statMemory);
            JPanel statsRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 2));
            statsRow.add(statsGrid);
            // only meaningful while the server runs
            statsRow.setVisible(false);
            openBrowser.setVisible(false);
            openBrowser.addActionListener(e -> {
                try {
                    if (java.awt.Desktop.isDesktopSupported() && java.awt.Desktop.getDesktop().isSupported(java.awt.Desktop.Action.BROWSE)) {
                        java.awt.Desktop.getDesktop().browse(java.net.URI.create("http://localhost:" + boundPort[0] + "/"));
                    }
                } catch (Exception ex) {
                    System.err.println("cannot open browser: " + ex);
                }
            });
            startStop.addActionListener(e -> {
                if (running[0] == null) {
                    int port;
                    try {
                        port = Integer.parseInt(portField.getText().trim());
                    } catch (NumberFormatException nfe) {
                        serverStatus.setText("invalid port: " + portField.getText().trim());
                        return;
                    }
                    startStop.setEnabled(false);
                    serverStatus.setText("starting...");
                    String source = input.getText();
                    String prepped = selectedPreamble.get();
                    new Thread(() -> {
                        try {
                            java.util.List<NamedSource> kbSources = new ArrayList<>();
                            if (prepped != null) {
                                kbSources.add(new NamedSource("<prep>", prepped));
                            }
                            kbSources.add(new NamedSource("<nelumbo-tab>", source));
                            KnowledgeBase kb = KnowledgeBaseLoader.load(kbSources);
                            NelumboServer server = new NelumboServer(kb, java.util.List.of("<nelumbo-tab>"), NelumboServer.DEFAULT_TIMEOUT_MS);
                            int bound = server.start(port);
                            SwingUtilities.invokeLater(() -> {
                                running[0] = server;
                                boundPort[0] = bound;
                                serverStatus.setText("running at http://localhost:" + bound + " (serving the nelumbo tab content)");
                                statRequests.setText("0");
                                statTiming.setText("-");
                                statUptime.setText("-");
                                statMemory.setText("-");
                                startStop.setText("Stop");
                                startStop.setEnabled(true);
                                openBrowser.setEnabled(true);
                                openBrowser.setVisible(true);
                                statsRow.setVisible(true);
                                portField.setEnabled(false);
                                poll.start();
                            });
                        } catch (RuntimeException ex) {
                            SwingUtilities.invokeLater(() -> {
                                serverStatus.setText(" ");
                                startStop.setEnabled(true);
                                javax.swing.JOptionPane.showMessageDialog(frame, ex.getMessage(), "Server start failed",
                                        javax.swing.JOptionPane.ERROR_MESSAGE);
                            });
                        }
                    }, "nelumbo-server-start").start();
                } else {
                    poll.stop();
                    running[0].stop();
                    running[0] = null;
                    serverStatus.setText(" ");
                    startStop.setText("Start");
                    openBrowser.setEnabled(false);
                    openBrowser.setVisible(false);
                    statsRow.setVisible(false);
                    portField.setEnabled(true);
                }
            });
            JPanel serverRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 2));
            serverRow.add(new JLabel("port:"));
            serverRow.add(portField);
            serverRow.add(startStop);
            JPanel statusRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 2));
            statusRow.add(serverStatus);
            statusRow.add(openBrowser);
            JPanel serverContent = new JPanel();
            serverContent.setLayout(new javax.swing.BoxLayout(serverContent, javax.swing.BoxLayout.Y_AXIS));
            serverRow.setAlignmentX(0f);
            statusRow.setAlignmentX(0f);
            statsRow.setAlignmentX(0f);
            serverContent.add(serverRow);
            serverContent.add(statusRow);
            serverContent.add(statsRow);
            // BorderLayout.NORTH keeps the rows compact at the top instead of spreading vertically
            JPanel serverTab = new JPanel(new BorderLayout());
            serverTab.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
            serverTab.add(serverContent, BorderLayout.NORTH);

            // the output and json tabs only appear once there is something to show (after the first Run)
            JTabbedPane tabs = new JTabbedPane();
            tabs.addTab("usage", new JScrollPane(usage));
            tabs.addTab("prep", prepTab);
            tabs.addTab("nelumbo", nelumboTab);
            tabs.addTab("output", outputScroll);
            tabs.addTab("json", jsonTab);
            tabs.addTab("server", serverTab);
            // output and json hold nothing until the first Eval
            tabs.setEnabledAt(tabs.indexOfComponent(outputScroll), false);
            tabs.setEnabledAt(tabs.indexOfComponent(jsonTab), false);
            tabs.setSelectedComponent(nelumboTab);
            tabs.setPreferredSize(new Dimension(760, 460));

            java.util.function.Consumer<Boolean> doEval = trace -> {
                eval.setEnabled(false);
                evalTrace.setEnabled(false);
                textOutput.setText("running...");
                jsonOutput.setText("running...");
                String source = input.getText();
                String prepped = selectedPreamble.get();
                new Thread(() -> {
                    String text;
                    String json;
                    DefaultMutableTreeNode tree;
                    try {
                        NelumboEvaluator.EvalResult result = NelumboEvaluator.evaluate(source, "<input>", 0, prepped);
                        java.util.Map<String, Object> object = jsonObject(result, "<input>");
                        if (trace) {
                            addTraceStub(object);
                        }
                        text = renderText(result);
                        json = JsonPrettyfier.pretty(Json.toJson(object));
                        tree = treeNode("result", object);
                    } catch (RuntimeException ex) {
                        text = "evaluation failed: " + ex;
                        json = text;
                        tree = new DefaultMutableTreeNode(text);
                    }
                    String textResult = text;
                    String jsonResult = json;
                    DefaultMutableTreeNode treeResult = tree;
                    SwingUtilities.invokeLater(() -> {
                        textOutput.setText(textResult);
                        jsonOutput.setText(jsonResult);
                        jsonTree.setModel(new DefaultTreeModel(treeResult));
                        for (int i = 0; i < jsonTree.getRowCount(); i++) {
                            jsonTree.expandRow(i);
                        }
                        eval.setEnabled(true);
                        evalTrace.setEnabled(true);
                        tabs.setEnabledAt(tabs.indexOfComponent(outputScroll), true);
                        tabs.setEnabledAt(tabs.indexOfComponent(jsonTab), true);
                        tabs.setSelectedComponent(outputScroll);
                    });
                }, "nelumbo-cli-run").start();
            };
            eval.addActionListener(e -> doEval.accept(false));
            evalTrace.addActionListener(e -> doEval.accept(true));

            JPanel panel = new JPanel(new BorderLayout());
            panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
            panel.add(tabs, BorderLayout.CENTER);

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

                With --server the inputs are not evaluated one by one but loaded together
                into a knowledge base that is served over HTTP.

                  <file>           path to a .nl file, or - to read stdin
                  -n, --nelumbo S  evaluate the Nelumbo source given as argument S
                  -p, --prep M,..  preload stdlib modules before each evaluation (and into
                                   the served knowledge base): all, logic, integers, strings,
                                   collections, rationals, datetime
                  -j, --json       output one JSON object: errors as an array of messages,
                                   per query the facts/falsehoods as name/value pairs,
                                   and the parse tree of the input
                  --trace          add the (currently stubbed) trace field to the output
                  -s, --server P   serve the inputs over HTTP on port P (0 picks a free port)
                  -t, --timeout MS per-request inference budget in server mode
                                   (default 30000; 0 disables)
                  -q, --quiet      suppress query result output (errors still printed)
                  -h, --help       show this help and exit

                Server endpoints:
                  GET  /            info page with endpoint docs and a try-it form
                  POST /eval        evaluate a posted Nelumbo document, returns query results
                                    and parse tree as JSON (raw text, or a JSON envelope
                                    {"document": "...", "limit": N, "stdlib": bool} where
                                    stdlib=true preloads all stdlib imports)
                  POST /eval/trace  like /eval, with a (currently stubbed) trace field
                  GET  /metadata    knowledge base metadata (types, functors, rules, facts)
                  GET  /examples    bundled example names; /examples/<name> returns the source
                  GET  /health      liveness check

                Exit codes: 0 success, 1 parse/evaluation/comparison errors, 2 usage error.
                """;
    }
}
