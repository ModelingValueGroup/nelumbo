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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

class NelumboReplTest {

    private static final String FIB = """
            import nelumbo.integers

            Integer ::= fib(<Integer>)

            Integer n, f

            fib(n)=f <=>  f=n                 if n>=0 & n<=1,
                          f=fib(n-1)+fib(n-2) if n>1
            """;

    /** Runs a REPL session over the given input lines; out/err are captured per session. */
    private static final class Session {
        final ByteArrayOutputStream outBytes = new ByteArrayOutputStream();
        final ByteArrayOutputStream errBytes = new ByteArrayOutputStream();
        final NelumboRepl           repl;
        int                         exitCode;

        Session(String input, boolean prompts) {
            PrintStream out = new PrintStream(outBytes, true, StandardCharsets.UTF_8);
            repl = new NelumboRepl(NelumboRepl.plainSource(new BufferedReader(new StringReader(input)), out, prompts),
                    out, new PrintStream(errBytes, true, StandardCharsets.UTF_8), 0);
        }

        Session run() {
            exitCode = repl.run();
            return this;
        }

        String out() {
            return outBytes.toString(StandardCharsets.UTF_8);
        }

        String err() {
            return errBytes.toString(StandardCharsets.UTF_8);
        }
    }

    @Test
    void queryPrintsItsResultAndDeclarationsPersistAcrossLines() {
        // the import on line 1 and the declaration on line 2 must still be there on line 3
        Session s = new Session("import nelumbo.integers\nInteger r\n3*4=r ?\n", false).run();
        assertEquals(0, s.exitCode);
        assertTrue(s.err().isEmpty(), () -> "stderr: " + s.err());
        assertTrue(s.out().contains("3*4=r ? [(r=12)]"), () -> "stdout: " + s.out());
    }

    @Test
    void errorLineLeavesPreviousStateIntact() {
        Session s = new Session("import nelumbo.integers\nflurb @@ blarg\nInteger r\n3*4=r ?\n", false).run();
        assertEquals(0, s.exitCode);
        // the bad line is reported under its per-line input name ...
        assertTrue(s.err().contains("<repl:2>"), () -> "stderr: " + s.err());
        // ... and the import from before the bad line still works after it
        assertTrue(s.out().contains("3*4=r ? [(r=12)]"), () -> "stdout: " + s.out());
    }

    @Test
    void backslashContinuationJoinsLines() {
        Session s = new Session("""
                import nelumbo.integers
                Integer ::= fib(<Integer>)
                Integer n, f
                fib(n)=f <=> f=n if n>=0 & n<=1, \\
                             f=fib(n-1)+fib(n-2) if n>1
                Integer r
                fib(7)=r ?
                """, false).run();
        assertEquals(0, s.exitCode);
        assertTrue(s.err().isEmpty(), () -> "stderr: " + s.err());
        assertTrue(s.out().contains("r=13"), () -> "stdout: " + s.out());
    }

    @Test
    void quitCommandStopsReadingInput() {
        // the query after :quit would print an error if it were still evaluated
        Session s = new Session(":quit\n3*4=r ?\n", false).run();
        assertEquals(0, s.exitCode);
        assertTrue(s.err().isEmpty(), () -> "stderr: " + s.err());
    }

    @Test
    void endOfInputEndsSession() {
        Session s = new Session("", false).run();
        assertEquals(0, s.exitCode);
    }

    @Test
    void helpListsCommands() {
        Session s = new Session(":help\n", false).run();
        assertEquals(0, s.exitCode);
        assertTrue(s.out().contains(":quit"), () -> "stdout: " + s.out());
    }

    @Test
    void unknownCommandIsReportedNotEvaluated() {
        Session s = new Session(":frobnicate\n", false).run();
        assertEquals(0, s.exitCode);
        assertTrue(s.err().contains(":frobnicate"), () -> "stderr: " + s.err());
    }

    @Test
    void loadedSourcesAreAvailableInTheSession() {
        Session s = new Session("Integer r\nfib(7)=r ?\n", false);
        s.repl.load("fib.nl", FIB);
        s.run();
        assertEquals(0, s.exitCode);
        assertTrue(s.err().isEmpty(), () -> "stderr: " + s.err());
        assertTrue(s.out().contains("r=13"), () -> "stdout: " + s.out());
    }

    @Test
    void loadErrorIsReportedAndSessionStillWorks() {
        Session s = new Session("import nelumbo.integers\nInteger r\n3*4=r ?\n", false);
        s.repl.load("bad.nl", "flurb @@ blarg\n");
        s.run();
        assertEquals(0, s.exitCode);
        assertTrue(s.err().contains("bad.nl"), () -> "stderr: " + s.err());
        assertTrue(s.out().contains("3*4=r ? [(r=12)]"), () -> "stdout: " + s.out());
    }

    @Test
    void promptsAreShownWhenEnabled() {
        Session s = new Session("Integer r \\\nInteger f\n", true).run();
        assertEquals(0, s.exitCode);
        assertTrue(s.out().contains("nl> "), () -> "stdout: " + s.out());
        // the backslash-continued line gets the continuation prompt
        assertTrue(s.out().contains("... "), () -> "stdout: " + s.out());
    }

    @Test
    void promptsAreAbsentWhenDisabled() {
        Session s = new Session("Integer r\n", false).run();
        assertFalse(s.out().contains("nl> "), () -> "stdout: " + s.out());
    }
}
