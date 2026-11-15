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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.modelingvalue.nelumbo.KnowledgeBase;
import org.modelingvalue.nelumbo.tools.PreparedDocument;
import org.modelingvalue.nelumbo.tools.PreparedQuery;
import org.modelingvalue.nelumbo.tools.PreparedQuery.Outcome;
import org.modelingvalue.nelumbo.syntax.Tokenizer;

public class PreparedDocumentTest {

    private static final String ABOVE = """
            import nelumbo.logic
            Person  :: Object
            Person  ::= Piet, Kees
            Boolean ::= likes(<Person>), knows(<Person>)
            Person  p
            fact likes(Piet)
            likes(Kees) ?
            E[p](knows(p)) ?
            """;

    private static final String BELOW = """
            fact likes(Kees)
            Person  ::= Jan
            fact knows(Jan)
            likes(p) <=> knows(p)
            likes(Kees) ?
            E[p](knows(p)) ?
            """;

    private static final String EQUAL = """
            import nelumbo.logic
            Person  :: Object
            Person  ::= Piet
            Food    :: Object
            Food    ::= Apple, Nut
            Boolean ::= likes(<Person>,<Food>), allergic(<Person>,<Food>), eats(<Person>,<Food>)
            Person  p
            Food    f
            fact likes(Piet, Apple)
            fact allergic(Piet, Nut)
            eats(p,f) <=> likes(p,f)
            eats(Piet,f) ?
            eats(p,f) <=> false if allergic(p,f)
            eats(Piet,f) ?
            """;

    private static final String FACTORIAL = """
            import nelumbo.integers
            Integer ::= factorial(<Integer>)
            Integer n, r
            factorial(n)=r <=> r=1 if n<=0, r=n*factorial(n-1) if n>0
            """;

    private static PreparedDocument prepare(String src) {
        return PreparedDocument.prepare(KnowledgeBase.BASE, new Tokenizer(src, "test").tokenize(), 0);
    }

    private static List<String> results(String src) {
        List<String> out = new ArrayList<>();
        for (PreparedQuery q : prepare(src).queries()) {
            out.add(q.evaluate(0).result());
        }
        return out;
    }

    @Test
    void aQueryIgnoresEverythingBelowIt() {
        List<String> above = results(ABOVE);
        List<String> below = results(ABOVE + BELOW);
        assertEquals(above, below.subList(0, 2));
        assertEquals("[()][]", below.get(2));
    }

    @Test
    void equalQueriesAreDistinctNodes() {
        PreparedDocument doc = prepare(EQUAL);
        assertNotSame(doc.queries().get(0).query(), doc.queries().get(1).query());
        assertEquals(List.of("[(f=Apple),..][..]", "[(f=Apple),..][(f=Nut),..]"), results(EQUAL));
    }

    @RepeatedTest(10)
    void parallelResultsEqualSequentialOnes() throws InterruptedException {
        StringBuilder src = new StringBuilder(FACTORIAL);
        for (int i = 5; i <= 12; i++) {
            src.append("factorial(").append(i).append(")=r ?\n");
        }
        List<String>        sequential = results(src.toString());
        List<PreparedQuery> queries    = prepare(src.toString()).queries();
        String[]            parallel   = new String[queries.size()];
        List<Thread>        threads    = new ArrayList<>();
        for (int t = 0; t < 4; t++) {
            int first = t;
            Thread thread = new Thread(() -> {
                for (int i = first; i < parallel.length; i += 4) {
                    parallel[i] = queries.get(i).evaluate(0).result();
                }
            });
            threads.add(thread);
            thread.start();
        }
        for (Thread thread : threads) {
            thread.join();
        }
        assertEquals(sequential, List.of(parallel));
    }

    @Test
    void aFailingFactDoesNotStopTheWalk() {
        PreparedDocument doc = prepare("import nelumbo.logic\nPerson :: Object\nPerson ::= Piet\nBoolean ::= likes(<Person>)\nPerson p\nfact likes(p)\nlikes(Piet) ?\n");
        assertEquals(1, doc.statementErrors().size());
        assertTrue(doc.statementErrors().get(0).getShortMessage().contains("has variables"));
        assertEquals(1, doc.queries().size());
        assertNotNull(doc.queries().get(0).evaluate(0).result());
    }

    @Test
    void aCancelStopsARunningQuery() throws InterruptedException {
        PreparedQuery            query   = prepare(FACTORIAL + "factorial(100000)=r ?\n").queries().get(0);
        AtomicReference<Outcome> outcome = new AtomicReference<>();
        Thread                  thread  = new Thread(() -> outcome.set(query.evaluate(0)));
        try {
            thread.start();
            Thread.sleep(500);
            query.cancel();
            thread.join(2000);
            assertFalse(thread.isAlive());
            assertTrue(outcome.get().cancelled());
        } finally {
            query.cancel();
        }
    }

    @Test
    void aCancelBeforeTheStartSkipsTheQuery() {
        PreparedQuery query = prepare(FACTORIAL + "factorial(100000)=r ?\n").queries().get(0);
        try {
            query.cancel();
            long    start   = System.nanoTime();
            Outcome outcome = query.evaluate(0);
            assertTrue(System.nanoTime() - start < 100_000_000L);
            assertTrue(outcome.cancelled());
            assertNull(outcome.result());
        } finally {
            query.cancel();
        }
    }

    @Test
    void aDeadlineStopsAQuery() {
        PreparedQuery query = prepare(FACTORIAL + "factorial(100000)=r ?\n").queries().get(0);
        try {
            long    start   = System.nanoTime();
            Outcome outcome = query.evaluate(300);
            assertTrue(System.nanoTime() - start < 2_000_000_000L);
            assertTrue(outcome.timedOut());
            assertFalse(outcome.cancelled());
        } finally {
            query.cancel();
        }
    }
}
