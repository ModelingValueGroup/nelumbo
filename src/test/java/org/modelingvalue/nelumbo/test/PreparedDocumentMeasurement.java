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

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.modelingvalue.nelumbo.KnowledgeBase;
import org.modelingvalue.nelumbo.syntax.Tokenizer;
import org.modelingvalue.nelumbo.tools.PreparedDocument;
import org.modelingvalue.nelumbo.tools.PreparedQuery;

/** Measurements for the live query results plan (long runs): MEASURE_PREPARED=true. Prints tables on stdout. */
@EnabledIfEnvironmentVariable(named = "MEASURE_PREPARED", matches = "true")
public class PreparedDocumentMeasurement {

    private static final String DIR = "/org/modelingvalue/nelumbo/examples/";

    private static String text(String file) throws IOException {
        try (InputStream in = PreparedDocumentMeasurement.class.getResourceAsStream(DIR + file)) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static PreparedDocument prepare(String file) throws IOException {
        KnowledgeBase base = new KnowledgeBase(KnowledgeBase.BASE);
        base.setMaxLogicMemoiz(100000);
        String text = text(file);
        return PreparedDocument.prepare(base, new Tokenizer(text, file).tokenize(), 0);
    }

    @Test
    public void memoizationSequentialVsParallel() throws Exception {
        for (String file : List.of("sudoku-smart-4x4.nl", "sudoku-csp-4x4.nl", "sudoku-smart-9x9.nl")) {
            List<Long> sequential = new ArrayList<>();
            for (PreparedQuery q : prepare(file).queries()) {
                sequential.add(q.evaluate(0).micros());
            }
            List<Long>          parallel = new ArrayList<>();
            ExecutorService     pool     = Executors.newFixedThreadPool(4);
            List<Future<Long>>  futures  = new ArrayList<>();
            for (PreparedQuery q : prepare(file).queries()) {
                futures.add(pool.submit(() -> q.evaluate(0).micros()));
            }
            for (Future<Long> f : futures) {
                parallel.add(f.get());
            }
            pool.shutdown();
            System.out.println("MEASURE " + file + " query: sequential micros / parallel micros");
            for (int i = 0; i < sequential.size(); i++) {
                System.out.println("MEASURE   " + i + ": " + sequential.get(i) + " / " + parallel.get(i));
            }
        }
    }

    @Test
    public void cancelLatency() throws Exception {
        cancel("sudoku-csp-9x9.nl", null);
        cancel("sudoku-brute-9x9.nl", "guessing");
    }

    private void cancel(String file, String contains) throws Exception {
        PreparedQuery target = null;
        String        text   = text(file);
        for (PreparedQuery q : prepare(file).queries()) {
            if (target == null && (contains == null || text.substring(q.span().start(), q.span().end()).contains(contains))) {
                target = q;
            }
        }
        PreparedQuery       q    = target;
        ExecutorService     pool = Executors.newSingleThreadExecutor();
        Future<Long>        end  = pool.submit(() -> {
            PreparedQuery.Outcome o = q.evaluate(0);
            System.out.println("MEASURE outcome " + file + ": " + o);
            assertTrue(o.cancelled(), "outcome is cancelled");
            return System.nanoTime();
        });
        Thread.sleep(10_000);
        assertFalse(end.isDone(), "still running at cancel");
        long start = System.nanoTime();
        q.cancel();
        System.out.println("MEASURE cancel " + file + ": " + (end.get() - start) / 1_000_000 + " ms");
        pool.shutdown();
    }
}
