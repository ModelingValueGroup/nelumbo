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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Path;

import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.UserInterruptException;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.modelingvalue.nelumbo.KnowledgeBase;
import org.modelingvalue.nelumbo.tools.NelumboEvaluator;

/**
 * The {@code -i}/{@code --interactive} read-eval-print loop. Each input (a line, or
 * backslash-continued lines) is evaluated in a child of the session's knowledge base;
 * on success the child becomes the session state, on any error it is discarded, so a
 * bad input never corrupts the session.
 */
final class NelumboRepl {

    /** Where input lines come from: a JLine terminal, or a plain reader (piped stdin, tests). */
    interface LineSource extends AutoCloseable {
        /** The next input line, or null at end of the session (EOF / Ctrl-D). */
        String readLine(String prompt) throws IOException;

        @Override
        default void close() throws IOException {
        }
    }

    /** Reads from {@code in}; when {@code prompts} is set the prompt is written to {@code out}. */
    static LineSource plainSource(BufferedReader in, PrintStream out, boolean prompts) {
        return prompt -> {
            if (prompts) {
                out.print(prompt);
                out.flush();
            }
            return in.readLine();
        };
    }

    /** A JLine terminal: line editing and persistent history; Ctrl-C discards the line, Ctrl-D ends the session. */
    static LineSource terminalSource() throws IOException {
        Terminal terminal = TerminalBuilder.builder().build();
        LineReader reader = LineReaderBuilder.builder().terminal(terminal)
                .variable(LineReader.HISTORY_FILE, Path.of(System.getProperty("user.home"), ".nelumbo_history"))
                .build();
        return new LineSource() {
            @Override
            public String readLine(String prompt) {
                try {
                    return reader.readLine(prompt);
                } catch (UserInterruptException e) {
                    return "";
                } catch (EndOfFileException e) {
                    return null;
                }
            }

            @Override
            public void close() throws IOException {
                terminal.close();
            }
        };
    }

    private final LineSource  in;
    private final PrintStream out;
    private final PrintStream err;
    private final long        deadlineMs;

    private KnowledgeBase kb;
    private int           lineCount = 0;

    NelumboRepl(LineSource in, PrintStream out, PrintStream err, long deadlineMs) {
        this.in = in;
        this.out = out;
        this.err = err;
        this.deadlineMs = deadlineMs;
        // world-scoped variables: an "Integer r" input must still be usable in later inputs
        kb = new KnowledgeBase(KnowledgeBase.BASE);
        kb.setWorldScopedVariables(true);
    }

    /** Evaluates a pre-session source (prep preamble, files) into the session knowledge base. */
    void load(String name, String source) {
        evaluate(name, source);
    }

    /** Runs the loop until end of input or {@code :quit}; the exit code (always 0). */
    int run() {
        try {
            String line;
            while ((line = in.readLine("nl> ")) != null) {
                StringBuilder chunk = new StringBuilder();
                while (line != null && line.endsWith("\\")) {
                    chunk.append(line, 0, line.length() - 1).append("\n");
                    line = in.readLine("... ");
                }
                if (line != null) {
                    chunk.append(line);
                }
                String input = chunk.toString();
                String trimmed = input.trim();
                if (trimmed.isEmpty()) {
                    continue;
                }
                if (":quit".equals(trimmed) || ":exit".equals(trimmed)) {
                    return 0;
                }
                if (":help".equals(trimmed)) {
                    printHelp();
                    continue;
                }
                if (trimmed.startsWith(":")) {
                    err.println("unknown command " + trimmed + " (:help lists the commands)");
                    continue;
                }
                evaluate("<repl:" + ++lineCount + ">", input);
            }
        } catch (IOException e) {
            err.println("nelumbo: cannot read input: " + e.getMessage());
            return 1;
        }
        return 0;
    }

    /** Evaluates in a child of the session KB; keeps the child only when there were no errors. */
    private void evaluate(String name, String source) {
        NelumboEvaluator.SessionResult session = NelumboEvaluator.evaluate(kb, source, name, deadlineMs);
        NelumboEvaluator.EvalResult result = session.result();
        for (NelumboEvaluator.Diagnostic d : result.diagnostics()) {
            err.println(name + ":" + d.line() + ":" + d.col() + ": " + d.message());
        }
        for (NelumboEvaluator.QueryOutcome q : result.queries()) {
            if (q.result() != null) {
                out.println(q.query() + " ? " + q.result());
            }
        }
        if (result.ok()) {
            kb = session.kb();
        }
    }

    private void printHelp() {
        out.print("""
                Every input is a Nelumbo statement or query; end a line with \\ to continue
                it on the next line. Declarations and facts accumulate; an input with
                errors is discarded, so it does not affect the session. Command history
                is kept in ~/.nelumbo_history. Commands:
                  :help         show this help
                  :quit, :exit  end the session (Ctrl-D also works)
                """);
    }
}
