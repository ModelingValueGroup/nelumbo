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
import static org.junit.jupiter.api.Assertions.assertTrue;

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

    /** A single blank line keeps the two {@code ?} aligned to one shared column (single-gap = same block). */
    @Test
    void singleBlankLineKeepsQueriesAligned() throws Exception {
        String formatted = format("""
                fib(50)=f          ? [..]

                maxFib(1000000)=f          ? [..]
                """);
        String[] ls = formatted.split("\n", -1);
        assertEquals(ls[0].indexOf('?'), ls[2].indexOf('?'), "a single blank line must not split the query alignment block");
        assertEquals(formatted, format(formatted), "idempotent");
    }

    /** A double (2+) blank line separates the query alignment block: each side aligns independently. */
    @Test
    void doubleBlankLineSeparatesQueries() throws Exception {
        String formatted = format("""
                fib(50)=f          ? [..]


                maxFib(1000000)=f          ? [..]
                """);
        assertEquals("""
                fib(50)=f ? [..]


                maxFib(1000000)=f ? [..]
                """, formatted);
    }

    /** A single blank-line gap keeps declaration operators in ONE alignment column (the user's integers.nl case). */
    @Test
    void alignsDeclarationOperatorsAcrossSingleBlankLine() throws Exception {
        String out = format("""
                a<b   <=> b>a

                a+b=c <=> add(a,b,c)
                """);
        String[] ls = out.split("\n", -1);
        // both <=> share a column even though a blank line separates the two groups:
        assertEquals(ls[0].indexOf("<=>"), ls[2].indexOf("<=>"), "single blank line must not split the alignment block");
        assertEquals(out, format(out), "idempotent");
    }

    /** A double (2+) blank-line gap is a hard separator: each group aligns independently. */
    @Test
    void doubleBlankLineSeparatesAlignmentBlocks() throws Exception {
        String out = format("""
                a<b   <=> b>a


                longLHS=x <=> y
                """);
        String[] ls = out.split("\n", -1);
        // the short group's <=> is NOT pulled out to the long group's column:
        org.junit.jupiter.api.Assertions.assertTrue(ls[0].indexOf("<=>") < ls[3].indexOf("<=>"),
            "2+ blank lines separate alignment blocks");
        assertEquals(out, format(out), "idempotent");
    }

    /** A content (non-blank) line between two marker lines also separates the alignment block. */
    @Test
    void contentLineBetweenSeparatesAlignmentBlocks() throws Exception {
        // 'Integer a' is a content line between the two rules; the <=> must not align across it.
        String out = format("""
                a<b   <=> b>a
                Integer a
                longLHS=x <=> y
                """);
        String[] ls = out.split("\n", -1);
        org.junit.jupiter.api.Assertions.assertTrue(ls[0].indexOf("<=>") < ls[2].indexOf("<=>"),
            "a content line between separates alignment blocks");
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
                Person ::= p(<Person>),  // parent
                           c(<Person>)   // child
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

    /** The two-space rule after {@code <=>} must be a fixed point: formatting its own output changes nothing. */
    @Test
    void ruleArrowSpacingIsIdempotent() throws Exception {
        String once = format("""
                fib(n)=f <=> f=n,
                         f=fib(n-1)+fib(n-2)
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

    /** Decision #7: leading indentation on top-level statements is stripped to column 0. */
    @Test
    void stripsLeadingIndentOnTopLevelStatements() throws Exception {
        assertEquals("Integer :: Object\nInteger a, b\n", format("    Integer :: Object\n  Integer a, b\n"));
    }

    /** Continuation lines are NOT stripped to 0 — they keep the hanging indent under the first item. */
    @Test
    void doesNotStripContinuationLineIndent() throws Exception {
        assertEquals("""
                Person ::= p(<Person>),
                           c(<Person>)
                """, format("""
                  Person ::= p(<Person>),
                     c(<Person>)
                """));
    }

    /** A declaration operator as the first token on an indented line: no overlapping edits, indent stripped. */
    @Test
    void handlesAlignedMarkerAsFirstTokenOnIndentedLine() throws Exception {
        // ':: Object' with leading indent must not produce overlapping edits; indent is stripped to col 0.
        assertEquals(":: Object\n", format("    :: Object\n"));
    }

    /** Indent stripping is idempotent and leaves blank lines to the trailing-whitespace pass. */
    @Test
    void indentStripIsIdempotent() throws Exception {
        String once = format("   a :: Object\n   b :: Object\n");
        assertEquals(once, format(once));
        assertEquals("a :: Object\nb :: Object\n", once);
    }

    /** A run of {@code Type names...} declarations aligns the variable-name column under the longest type. */
    @Test
    void alignsVariableDeclarationNames() throws Exception {
        assertEquals("""
                Literal  l1, l2
                Function f1, f2
                Object   n1, n2
                """, format("""
                Literal l1, l2
                Function f1, f2
                Object n1, n2
                """));
    }

    /** Variable-name alignment works on indented input: indent is stripped AND names align at column 0-relative. */
    @Test
    void alignsVariableDeclarationNamesOnIndentedInput() throws Exception {
        assertEquals("""
                Literal  l1, l2
                Function f1, f2
                Object   n1, n2
                """, format("""
                    Literal l1, l2
                    Function f1, f2
                    Object n1, n2
                """));
    }

    /** A non-declaration line (one containing an operator) breaks the block; names are not pulled in. */
    @Test
    void variableNameAlignmentStopsAtNonDeclarations() throws Exception {
        assertEquals("""
                Integer n, f
                fib(n)=f <=>  f=n
                """, format("""
                Integer n, f
                fib(n)=f <=> f=n
                """));
    }

    /** Variable-name alignment is idempotent: re-formatting already aligned output changes nothing. */
    @Test
    void variableNameAlignmentIsIdempotent() throws Exception {
        String once = format("Literal  l1, l2\nFunction f1, f2\n");
        assertEquals(once, format(once));
    }

    /** The `#N` precedence markers across the alternatives of one `::=` body align into a single column. */
    @Test
    void alignsPrecedenceMarkersInBody() throws Exception {
        String formatted = format("""
                Integer ::= <Integer> ">"  <Integer> #30,
                            <Integer> "<=" <Integer>   #30,
                                      - <Integer> #80
                """);
        // All three `#` markers must share the same absolute column index.
        String[] lines = formatted.split("\n", -1);
        int col0 = lines[0].indexOf('#');
        int col1 = lines[1].indexOf('#');
        int col2 = lines[2].indexOf('#');
        assertEquals(col0, col1, "line 0 and line 1 # must share a column");
        assertEquals(col0, col2, "line 0 and line 2 # must share a column");
        // The double space inside the pattern on line 0 must be preserved.
        assertTrue(lines[0].contains("\">\"  <Integer>"), "double space inside pattern must survive");
        // Idempotency: re-formatting aligned output must be a no-op.
        assertEquals(formatted, format(formatted));
    }

    @Test
    void precedenceAlignmentIsIdempotent() throws Exception {
        String once = format("""
                Integer ::= <Integer> + <Integer> #40,
                            <Integer> * <Integer> #50
                """);
        assertEquals(once, format(once));
    }

    /** Embedded precedence inside a pattern type-reference (e.g. <Boolean#0>) must NOT be treated as an alignable #N. */
    @Test
    void doesNotAlignPrecedenceEmbeddedInPattern() throws Exception {
        String src = """
                Boolean ::= E[<(> <Variable#100> <,> , <)+>](<Boolean#0>) #20,
                            <Boolean> & <Boolean>                         #22
                """;
        String out = format(src);
        // the embedded #100 / #0 inside <...> must be byte-for-byte preserved:
        org.junit.jupiter.api.Assertions.assertTrue(out.contains("<Variable#100>"), "embedded #100 must be untouched");
        org.junit.jupiter.api.Assertions.assertTrue(out.contains("<Boolean#0>"), "embedded #0 must be untouched");
        // and the two trailing #20 / #22 must align to one column:
        String[] ls = out.split("\n", -1);
        org.junit.jupiter.api.Assertions.assertEquals(ls[0].lastIndexOf('#'), ls[1].lastIndexOf('#'), "trailing # aligned");
    }

    /** `@` annotations align into a column; when `#N` also appears, `@` sits in the next column to its right. */
    @Test
    void alignsAnnotationsAfterPrecedence() throws Exception {
        assertEquals("""
                Boolean ::= true                  @nelumbo.logic.NBoolean,
                            <Boolean> & <Boolean> @nelumbo.logic.And,
                            <Boolean> -> <Boolean>
                """, format("""
                Boolean ::= true   @nelumbo.logic.NBoolean,
                            <Boolean> & <Boolean>   @nelumbo.logic.And,
                            <Boolean> -> <Boolean>
                """));
    }

    /** With #N AND @ on the same lines, the @ column lands right of the aligned #N column (post-#N coordinates). */
    @Test
    void alignsAnnotationsRightOfPrecedenceColumn() throws Exception {
        String out = format("""
                Boolean ::= <Integer> ">"  <Integer> #30   @nelumbo.integers.GreaterThan,
                            <Integer> "<=" <Integer> #30 @nelumbo.integers.LessEqual
                """);
        String[] ls = out.split("\n", -1);
        // both # align, both @ align, and every @ is to the right of its #:
        org.junit.jupiter.api.Assertions.assertEquals(ls[0].indexOf(" #"), ls[1].indexOf(" #"), "# aligned");
        org.junit.jupiter.api.Assertions.assertEquals(ls[0].indexOf('@'), ls[1].indexOf('@'), "@ aligned");
        org.junit.jupiter.api.Assertions.assertTrue(ls[0].indexOf('@') > ls[0].indexOf('#'), "@ right of #");
    }

    @Test
    void annotationAlignmentIsIdempotent() throws Exception {
        String once = format("""
                Boolean ::= true                  @nelumbo.logic.NBoolean,
                            <Boolean> & <Boolean> @nelumbo.logic.And
                """);
        assertEquals(once, format(once));
    }

    @Test
    void annotationAndPrecedenceAlignmentIsIdempotent() throws Exception {
        String once = format("""
                Boolean ::= <Integer> ">"  <Integer> #30   @nelumbo.integers.GreaterThan,
                            <Integer> "<=" <Integer> #30 @nelumbo.integers.LessEqual
                """);
        assertEquals(once, format(once));
    }

    /** The `if` guards of a rule's clauses align into one column (only inside a `<=>` body). */
    @Test
    void alignsIfGuardsInRuleBody() throws Exception {
        assertEquals("""
                fib(n)=f <=>  f=n                 if n>=0 & n<=1,
                              f=fib(n-1)+fib(n-2) if n>1
                """, format("""
                fib(n)=f <=>  f=n if n>=0 & n<=1,
                              f=fib(n-1)+fib(n-2) if n>1
                """));
    }

    /** A bare `if` outside a `<=>` body (e.g. a name in a fact) is never aligned/touched. */
    @Test
    void doesNotAlignIfOutsideRuleBody() throws Exception {
        String src = "fact thenElse(if, then)\n";
        assertEquals(src, format(src));
    }

    @Test
    void ifGuardAlignmentIsIdempotent() throws Exception {
        String once = format("""
                |a|=b <=>  b=a  if a>=0,
                           b=-a if a<0
                """);
        assertEquals(once, format(once));
    }

    /** Head-line operator-shift: `<=>` with ONE space in the source still aligns the `if` guards after formatting. */
    @Test
    void alignsIfGuardsWhenRuleArrowSpacingIsCorrected() throws Exception {
        String out = format("""
                fib(n)=f <=> f=n if n>=0 & n<=1,
                             f=fib(n-1)+fib(n-2) if n>1
                """);
        String[] ls = out.split("\n", -1);
        org.junit.jupiter.api.Assertions.assertEquals(ls[0].indexOf(" if "), ls[1].indexOf(" if "),
            "head-line if must align with continuation if even though <=> spacing was corrected from 1 to 2");
        assertEquals(out, format(out), "must be idempotent");
    }

    /** Trailing `//` comments across one body block align into a column two spaces past the longest line. */
    @Test
    void alignsTrailingComments() throws Exception {
        assertEquals("""
                Person ::= p(<Person>),  // parent
                           c(<Person>),  // child
                           d(<Person>)   // descendant
                """, format("""
                Person ::= p(<Person>),   // parent
                           c(<Person>), // child
                           d(<Person>)    // descendant
                """));
    }

    @Test
    void commentAlignmentIsIdempotent() throws Exception {
        String once = format("""
                fact pc(a, b),  // first
                     pc(c, d)   // second
                """);
        assertEquals(once, format(once));
    }

    /** A single trailing comment (block of one) is left alone; a comment-only line is never a column. */
    @Test
    void leavesLoneAndStandaloneCommentsAlone() throws Exception {
        assertEquals("a :: Object  // note\n", format("a :: Object  // note\n"));
        assertEquals("// just a comment\n", format("// just a comment\n"));
    }

    /** Same hardening for the #N column: a head line whose operator spacing changes keeps #N aligned. */
    @Test
    void alignsPrecedenceWhenHeadOperatorSpacingChanges() throws Exception {
        // ::= with extra spaces in source gets normalized to one; head-line #N must still align with continuations.
        String out = format("""
                Integer ::=    <Integer> + <Integer> #40,
                            <Integer> * <Integer> #50
                """);
        String[] ls = out.split("\n", -1);
        org.junit.jupiter.api.Assertions.assertEquals(ls[0].lastIndexOf('#'), ls[1].lastIndexOf('#'),
            "head-line #N must align with continuation #N after operator spacing normalization");
        assertEquals(out, format(out), "must be idempotent");
    }

    /** Internal blank-line runs are preserved verbatim (no collapsing); only trailing blanks are trimmed. */
    @Test
    void preservesInternalBlankLinesTrimsTrailing() throws Exception {
        assertEquals("a :: Object\n\n\n\nb :: Object\n", format("a :: Object\n\n\n\nb :: Object\n"));
        assertEquals("a :: Object\n", format("a :: Object\n\n\n"));
    }

    /** Blank lines at end of file are removed (the final statement keeps its single trailing newline). */
    @Test
    void trimsTrailingBlankLinesAtEof() throws Exception {
        assertEquals("a :: Object\n", format("a :: Object\n\n\n"));
    }

    /** A single blank line between statements is preserved (does not collapse to zero). */
    @Test
    void keepsSingleBlankLine() throws Exception {
        assertEquals("a :: Object\n\nb :: Object\n", format("a :: Object\n\nb :: Object\n"));
    }

    /** Blank-line normalisation is idempotent; internal double blank preserved, trailing blank trimmed. */
    @Test
    void blankLineCollapseIsIdempotent() throws Exception {
        String once = format("a :: Object\n\n\nb :: Object\n\n");
        assertEquals(once, format(once));
        assertEquals("a :: Object\n\n\nb :: Object\n", once);
    }

    /** A hand-aligned sample touching every pass must be a fixed point: formatting it changes nothing. */
    @Test
    void fullyFormattedSampleIsFixedPoint() throws Exception {
        String sample = """
                Integer :: Object

                Boolean ::= <Integer> ">" <Integer> #30 @nelumbo.integers.GreaterThan,
                            <Integer> "<" <Integer> #30 @nelumbo.integers.LessThan

                Integer a, b
                Object  n

                fib(n)=f <=>  f=n                 if n>=0 & n<=1,
                              f=fib(n-1)+fib(n-2) if n>1
                """;
        assertEquals(sample, format(sample), "a hand-aligned file must survive formatting unchanged");
    }

    /** Leading blank lines are internal (not trailing), so they are preserved verbatim. */
    @Test
    void preservesLeadingBlankLines() throws Exception {
        assertEquals("\n\n\na :: Object\n", format("\n\n\na :: Object\n"));
        assertEquals("\na :: Object\n", format("\na :: Object\n"));
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
