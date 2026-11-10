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

package org.modelingvalue.nelumbo.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.modelingvalue.nelumbo.KnowledgeBase;
import org.modelingvalue.nelumbo.NelumboConstants;
import org.modelingvalue.nelumbo.tools.NelumboEvaluator.EvalResult;

/**
 * The tutorial's editors, one {@code .nl} file each, in a folder per section ({@code tutorial/<section>/}):
 * {@code N-example.nl}, and {@code N-exercise.nl} with its {@code N-solution.nl}. The website fills them into
 * {@code tutorial.html} ({@code data-src}); its {@code everyTutorialEditorEvaluates} also checks the syntax-only
 * editors, which only the page marks ({@code data-syntax}). Here they run in random rule order.
 */
public class TutorialTest extends NelumboTestBase {

    static {
        setProp("PARALLEL_COLLECTIONS", "false");
        setProp("TRACE_SYNTATIC", "false");
        setProp("VERBOSE_TESTS", "false");
    }

    private static final String GIVEN = "// ---- given";

    @Override
    protected void settings(KnowledgeBase kb) {
        kb.setReverseOrder(env("REVERSE_NELUMBO", false));
        kb.setRandomOrder(env("RANDOM_NELUMBO", true));
        kb.setTrace(env("TRACE_NELUMBO", false));
    }

    /** An example may show a failing expectation on purpose, but nothing else. */
    @TestFactory
    Stream<DynamicTest> everyExampleEvaluates() {
        return files("-example.nl").map(file -> DynamicTest.dynamicTest(name(file), () -> {
            List<String> others = messages(evaluate(file)).stream().filter(m -> !m.startsWith("Expected result ")).toList();
            assertEquals(List.of(), others, name(file));
        }));
    }

    /** A solution evaluates without any error. */
    @TestFactory
    Stream<DynamicTest> everySolutionEvaluates() {
        return files("-solution.nl").map(file -> DynamicTest.dynamicTest(name(file), () -> {
            assertEquals(List.of(), messages(evaluate(file)), name(file));
        }));
    }

    /**
     * An exercise evaluates (it may fail on its expectations or start with a parse error) and has a solution. A
     * project exercise holds only the given part, and its solution ends with that identical part.
     */
    @TestFactory
    Stream<DynamicTest> everyExerciseHasItsSolution() {
        return files("-exercise.nl").map(file -> DynamicTest.dynamicTest(name(file), () -> {
            evaluate(file);
            Path solution = file.resolveSibling(file.getFileName().toString().replace("-exercise.nl", "-solution.nl"));
            assertTrue(Files.exists(solution), "no solution for " + name(file));
            String exercise = read(file);
            int given = exercise.indexOf(GIVEN);
            if (given >= 0) {
                String solutionText = read(solution);
                assertTrue(solutionText.endsWith(exercise.substring(given)),
                        name(solution) + " should end with the given part of " + name(file));
            }
        }));
    }

    @Test
    void everySolutionHasItsExercise() {
        List<String> orphans = files("-solution.nl")
                .filter(s -> !Files.exists(s.resolveSibling(s.getFileName().toString().replace("-solution.nl", "-exercise.nl"))))
                .map(TutorialTest::name).toList();
        assertEquals(List.of(), orphans);
        assertTrue(20 < files(".nl").count(), "expected the tutorial's files");
    }

    private EvalResult evaluate(Path file) {
        return evaluate(read(file), name(file), 60_000);
    }

    private static List<String> messages(EvalResult result) {
        return result.diagnostics().stream().map(d -> d.line() + ":" + d.col() + " " + d.message()).toList();
    }

    private static Stream<Path> files(String suffix) {
        URL url = TutorialTest.class.getResource(NelumboConstants.NELUMBO_TUTORIAL);
        try (Stream<Path> walk = Files.walk(Path.of(url.toURI()))) {
            return walk.filter(p -> p.getFileName().toString().endsWith(suffix)).sorted().toList().stream();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (URISyntaxException e) {
            throw new IllegalStateException(e);
        }
    }

    private static String name(Path file) {
        return file.getParent().getFileName() + "/" + file.getFileName();
    }

    private static String read(Path file) {
        try {
            return Files.readString(file).replace("\r\n", "\n");
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
