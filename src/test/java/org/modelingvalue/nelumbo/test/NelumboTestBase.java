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
//     Wim Bast, Tom Brus, Victor Lap                                                                                  ~
//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

package org.modelingvalue.nelumbo.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import org.modelingvalue.collections.Map;
import org.modelingvalue.collections.Set;
import org.modelingvalue.nelumbo.KnowledgeBase;
import org.modelingvalue.nelumbo.NelumboConstants;
import org.modelingvalue.nelumbo.U;
import org.modelingvalue.nelumbo.logic.InferResult;
import org.modelingvalue.nelumbo.logic.Predicate;
import org.modelingvalue.nelumbo.syntax.ParseException;
import org.modelingvalue.nelumbo.syntax.Parser;
import org.modelingvalue.nelumbo.syntax.Tokenizer;
import org.modelingvalue.nelumbo.tools.NelumboEvaluator;
import org.modelingvalue.nelumbo.tools.NelumboEvaluator.EvalResult;

@SuppressWarnings("unused")
public class NelumboTestBase {

    static void setProp(String name, String def) {
        String env = System.getenv(name);
        System.setProperty(name, env != null ? env : System.getProperty(name, def));
    }
    // an environment variable overrides the value a test class picks
    static boolean env(String name, boolean def) {
        String env = System.getenv(name);
        return env != null ? Boolean.parseBoolean(env) : def;
    }

    static int env(String name, int def) {
        String env = System.getenv(name);
        return env != null ? Integer.parseInt(env) : def;
    }

    // Evaluation settings of the knowledge base every test of this class runs in
    // (rule order, memoization size, ...). Override per test class: unlike the
    // system properties, which are read once per JVM, this works whatever test
    // class ran first.
    protected void settings(KnowledgeBase kb) {
    }

    // Utilities

    public KnowledgeBase run(Runnable test) {
        return KnowledgeBase.BASE.invoke(() -> {
            settings(KnowledgeBase.current());
            test.run();
        });
    }

    // NelumboEvaluator.evaluate with this class's settings (its knowledge base is
    // a child of the configured one, so it copies them)
    public EvalResult evaluate(String source, String name, long deadlineMs) {
        KnowledgeBase base = new KnowledgeBase(KnowledgeBase.BASE);
        settings(base);
        return NelumboEvaluator.evaluate(base, source, name, deadlineMs).result();
    }

    public static InferResult getResult(Predicate pred) {
        return pred.infer();
    }

    public static void haveEqualResult(Predicate query1, Predicate query2) {
        assertEquals(getResult(query1), getResult(query2));
    }

    public static void hasResult(Predicate query, Set<Predicate> facts, boolean completeFacts,
            Set<Predicate> falsehoods, boolean completeFalsehoods) {
        InferResult expectedResult = InferResult.of(query, facts, completeFacts, falsehoods, completeFalsehoods,
                Set.of(), Map.of());
        InferResult queryResult = getResult(query);
        assertEquals(expectedResult, queryResult);
    }

    public void exampleResource(String resource) {
        resource(resource, NelumboConstants.NELUMBO_EXAMPLES);
    }

    public void testResource(String resource) {
        resource(resource, NelumboConstants.NELUMBO_TESTS);
    }

    // known-bug repros: a diverging repro must abort (KnownBugExtension turns the
    // failure into ABORTED), not fail the build or hang it. The engine's own
    // deadline stops it, pool work included (NelumboTimeoutException). JUnit's
    // preemptive timeout only interrupts its thread, which the engine ignores:
    // alone it left the evaluation running and eating memory until the JVM
    // exited. It stays as a backstop for a hang outside inference.
    private static final long BUG_DEADLINE_SECONDS = 50;

    public void bugResource(String resource) {
        org.junit.jupiter.api.Assertions.assertTimeoutPreemptively(java.time.Duration.ofSeconds(BUG_DEADLINE_SECONDS + 10),
                () -> resource(resource, NelumboConstants.NELUMBO_BUGS, BUG_DEADLINE_SECONDS));
    }

    public void resource(String resource, String base) {
        resource(resource, base, 0);
    }

    private void resource(String resource, String base, long deadlineSeconds) {
        run(() -> {
            if (deadlineSeconds > 0) {
                KnowledgeBase.current().setDeadlineNanos(System.nanoTime() + deadlineSeconds * 1_000_000_000L);
            }
            try {
                U.printResults(Parser.parse(NelumboTestBase.class, base + resource));
            } catch (ParseException e) {
                System.err.println(e.getMessage());
                fail(e);
            }
        });
    }

    public void testString(String text, String name) {
        run(() -> {
            try {
                new Parser(new Tokenizer(text, name).tokenize()).parseEvaluate();
            } catch (ParseException e) {
                System.err.println(e.getMessage());
                fail(e);
            }
        });
    }

    // 1-based number of the line that starts with the given text (after its
    // indentation, followed by a space or the line end)
    protected static int lineOf(java.util.List<String> lines, String query) {
        for (int i = 0; i < lines.size(); i++) {
            if ((lines.get(i).strip() + " ").startsWith(query + " ")) {
                return i + 1;
            }
        }
        throw new IllegalArgumentException("no line starts with: " + query);
    }

}
