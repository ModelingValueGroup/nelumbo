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

package org.modelingvalue.nelumbo.lsp.documentService;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.eclipse.lsp4j.DocumentFormattingParams;
import org.eclipse.lsp4j.DocumentRangeFormattingParams;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.TextDocumentIdentifier;
import org.eclipse.lsp4j.TextEdit;
import org.junit.jupiter.api.Test;
import org.modelingvalue.nelumbo.lsp.NlDocumentManager;
import org.modelingvalue.nelumbo.lsp.Workspace;

public class DocumentFormattingServiceTest {

    private static final String URI = "test://format.nl";

    /**
     * Within a run of adjacent query lines, every {@code ?} is padded to the same column (the longest
     * left-hand side + one space), and exactly one space separates {@code ?} from the expected clause.
     * This is what users mean by "aligned across the column": the markers form a vertical line.
     */
    @Test
    void alignsQuestionMarksWithinAConsecutiveBlock() throws Exception {
        String formatted = format("""
                fib(0)=f ? [(f=0)][..]
                fib(1000)=f ? [(f=2)][..]
                """);
        assertEquals("""
                fib(0)=f    ? [(f=0)][..]
                fib(1000)=f ? [(f=2)][..]
                """, formatted);
        assertEquals(indexOfQuestion(formatted, 0), indexOfQuestion(formatted, 1), "the two ? must share a column");
    }

    /** Alignment must both grow short gaps and shrink drifted ones, so any starting whitespace lands on the same column. */
    @Test
    void normalisesAnyStartingWhitespaceToTheSameColumn() throws Exception {
        assertEquals("""
                fib(0)=f    ? [(f=0)][..]
                fib(1000)=f ? [(f=2)][..]
                """, format("""
                fib(0)=f                ?     [(f=0)][..]
                fib(1000)=f    ?  [(f=2)][..]
                """));
    }

    /**
     * The bug users originally reported: re-running the formatter kept pushing the expected value
     * right. Formatting must be idempotent — applying it to its own output is a no-op.
     */
    @Test
    void formattingIsIdempotent() throws Exception {
        String once  = format("""
                fib(0)=f ? [(f=0)][..]
                fib(1000)=f ? [(f=2)][..]
                """);
        assertEquals(once, format(once), "formatting its own output must not change it");
    }

    /** A blank line ends a block, so queries on either side of it are aligned independently, not to each other. */
    @Test
    void blankLineSeparatesAlignmentBlocks() throws Exception {
        String formatted = format("""
                fib(50)=f          ? [..]

                maxFib(1000000)=f          ? [..]
                """);
        assertEquals("""
                fib(50)=f ? [..]

                maxFib(1000000)=f ? [..]
                """, formatted);
    }

    /** A {@code ?} that ends the line has no expected clause, so nothing is added after it. */
    @Test
    void leavesTrailingQuestionMarkUntouched() throws Exception {
        assertEquals("n = 5 ?\n", format("n = 5 ?\n"));
    }

    /**
     * {@code ::}, {@code ::=} and {@code <=>} on adjacent lines form one block whose operators all line up
     * to a single column (longest left-hand side + one space), each followed by exactly one space —
     * except {@code <=>} which gets two spaces (corpus convention).
     */
    @Test
    void alignsDeclarationOperatorsAsOneCombinedBlock() throws Exception {
        assertEquals("""
                Person     :: Object
                LongAnimal ::= legs(<Integer>)
                fib(n)=f   <=>  f=n
                """, format("""
                Person :: Object
                LongAnimal ::= legs(<Integer>)
                fib(n)=f <=> f=n
                """));
    }

    /** Decision #8: the corpus consistently writes two spaces after {@code <=>}; declarations and queries keep one. */
    @Test
    void usesTwoSpacesAfterRuleArrowButOneElsewhere() throws Exception {
        assertEquals("""
                a<b <=>  b>a
                x   :: Object
                y   ::= leaf
                """, format("""
                a<b <=> b>a
                x :: Object
                y ::= leaf
                """));
        assertEquals("n=5 ? [..]\n", format("n=5 ? [..]\n"));
    }

    /** Declaration alignment is its own block; an adjacent query line does not pull into it (? stays separate). */
    @Test
    void queriesAndDeclarationsAlignIndependently() throws Exception {
        assertEquals("""
                Integer ::= fib(<Integer>)
                fib(5)=f ? [(f=5)][..]
                """, format("""
                Integer ::= fib(<Integer>)
                fib(5)=f      ? [(f=5)][..]
                """));
    }

    /** Trailing horizontal whitespace is stripped, including on otherwise-blank lines and the final line. */
    @Test
    void stripsTrailingWhitespace() throws Exception {
        assertEquals("a :: Object\n\nb :: Object", format("a :: Object   \n   \nb :: Object\t"));
    }

    /**
     * Formatting a selection (rangeFormatting) only edits the selected lines, but the alignment column is
     * still taken from the whole block — so a selected line lines up with its (unselected) block-mates.
     */
    @Test
    void rangeFormattingOnlyTouchesSelectedLinesButKeepsBlockAlignment() throws Exception {
        String content = """
                fib(0)=f ? [(f=0)][..]
                fib(1000)=f      ? [(f=2)][..]
                """;
        // select only the first line
        String formatted = formatRange(content, new Range(new Position(0, 0), new Position(1, 0)));
        assertEquals("""
                fib(0)=f    ? [(f=0)][..]
                fib(1000)=f      ? [(f=2)][..]
                """, formatted, "line 0 is padded to the full-block column; line 1 (unselected) keeps its bad spacing");
    }

    private static String formatRange(String content, Range range) throws ExecutionException, InterruptedException {
        NlDocumentManager manager = new NlDocumentManager(new Workspace());
        manager.addDocument(URI, content, 1);
        DocumentRangeFormattingParams params = new DocumentRangeFormattingParams();
        params.setTextDocument(new TextDocumentIdentifier(URI));
        params.setRange(range);
        List<? extends TextEdit> edits = new DocumentFormattingService(manager).rangeFormatting(params).get();
        return apply(content, edits == null ? List.of() : edits);
    }

    /** A line ending in a comma hangs its continuation lines under the first list item, after the operator. */
    @Test
    void indentsCommaContinuationUnderFirstItemAfterOperator() throws Exception {
        assertEquals("""
                Person ::= p(<Person>),
                           c(<Person>),
                           d(<Person>)
                """, format("""
                Person ::= p(<Person>),
                   c(<Person>),
                       d(<Person>)
                """));
    }

    /** With no operator (e.g. a {@code fact} list) the continuation hangs under the first item after the keyword. */
    @Test
    void indentsCommaContinuationUnderFirstItemAfterKeyword() throws Exception {
        assertEquals("""
                fact pc(a, b),
                     pc(c, d)
                """, format("""
                fact pc(a, b),
                        pc(c, d)
                """));
    }

    /** A trailing line comment after the comma must not stop the head line from being recognised. */
    @Test
    void commaFollowedByCommentStillContinues() throws Exception {
        assertEquals("""
                Person ::= p(<Person>),   // parent
                           c(<Person>)    // child
                """, format("""
                Person ::= p(<Person>),   // parent
                      c(<Person>)    // child
                """));
    }

    /** Any continuation token (here a trailing {@code |}) hangs the next line under the first item, not only commas. */
    @Test
    void indentsOperatorContinuationUnderFirstItemAfterOperator() throws Exception {
        assertEquals("""
                d(a)=c <=>  c(a)=c |
                            E[b](d(a)=b & c(b)=c)
                """, format("""
                d(a)=c <=> c(a)=c |
                             E[b](d(a)=b & c(b)=c)
                """));
    }

    /** The continuation indent must be idempotent: re-running over aligned output changes nothing. */
    @Test
    void continuationIndentIsIdempotent() throws Exception {
        String once = format("""
                Person ::= p(<Person>),
                      c(<Person>),
                      d(<Person>)
                """);
        assertEquals(once, format(once), "formatting its own output must not change it");
    }

    /**
     * Regression: the parser splits a glued operator run to match a pattern, so {@code <)?>} becomes the
     * tokens {@code <} {@code )} {@code ?} {@code >}. That pattern {@code ?} is not whitespace-delimited and
     * must not be aligned/spaced like a query {@code ?} (which previously produced {@code <) ? >}).
     */
    @Test
    void doesNotTreatPatternQuestionMarkAsAQuery() throws Exception {
        String src = """
                Integer :: Object
                Integer ::= <(> - <)?> <NUMBER> <]>
                """;
        assertEquals(src, format(src), "a ? glued inside a pattern must be left untouched");
    }

    private static int indexOfQuestion(String text, int lineIndex) {
        String line = text.split("\n", -1)[lineIndex];
        return line.indexOf('?');
    }

    private static String format(String content) throws ExecutionException, InterruptedException {
        NlDocumentManager       manager = new NlDocumentManager(new Workspace());
        manager.addDocument(URI, content, 1);
        DocumentFormattingService service = new DocumentFormattingService(manager);

        DocumentFormattingParams params = new DocumentFormattingParams();
        params.setTextDocument(new TextDocumentIdentifier(URI));

        List<? extends TextEdit> edits = service.formatting(params).get();
        return apply(content, edits == null ? List.of() : edits);
    }

    /** Apply LSP text edits (all single-line here) to {@code text}, splicing from the back so offsets stay valid. */
    private static String apply(String text, List<? extends TextEdit> edits) {
        String[] lines     = text.split("\n", -1);
        int[]    lineStart = new int[lines.length];
        int      acc       = 0;
        for (int i = 0; i < lines.length; i++) {
            lineStart[i] = acc;
            acc += lines[i].length() + 1; // + 1 for the '\n'
        }
        List<TextEdit> sorted = new ArrayList<>(edits);
        sorted.sort(Comparator.comparingInt((TextEdit e) -> offset(lineStart, e.getRange().getStart())).reversed());
        StringBuilder sb = new StringBuilder(text);
        for (TextEdit e : sorted) {
            sb.replace(offset(lineStart, e.getRange().getStart()), offset(lineStart, e.getRange().getEnd()), e.getNewText());
        }
        return sb.toString();
    }

    private static int offset(int[] lineStart, Position p) {
        return lineStart[p.getLine()] + p.getCharacter();
    }
}
