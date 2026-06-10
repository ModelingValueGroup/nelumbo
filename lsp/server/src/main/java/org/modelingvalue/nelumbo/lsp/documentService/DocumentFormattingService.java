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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;

import org.eclipse.lsp4j.DocumentFormattingParams;
import org.eclipse.lsp4j.DocumentRangeFormattingParams;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.TextEdit;
import org.modelingvalue.nelumbo.lsp.NlDocument;
import org.modelingvalue.nelumbo.lsp.NlDocumentManager;
import org.modelingvalue.nelumbo.lsp.U;
import org.modelingvalue.nelumbo.syntax.Token;
import org.modelingvalue.nelumbo.syntax.TokenType;

@SuppressWarnings("DuplicatedCode")
public class DocumentFormattingService extends DocumentServiceAdapter {
    public DocumentFormattingService(NlDocumentManager documentManager) {
        super(documentManager);
    }

    /** Declaration/definition operators that are aligned together as one column. The query {@code ?} is aligned separately. */
    private static final Set<String> DECLARATION_OPERATORS = Set.of("::", "::=", "<=>");

    /** Spaces emitted after an aligned marker. {@code <=>} takes two (corpus convention); everything else one. */
    private static final Map<String, Integer> SPACE_AFTER = Map.of("<=>", 2);

    private static int spaceAfter(Token marker) {
        return SPACE_AFTER.getOrDefault(marker.text(), 1);
    }

    @Override
    public CompletableFuture<List<? extends TextEdit>> formatting(DocumentFormattingParams params) {
        NlDocument document = documentManager.getDocument(params.getTextDocument().getUri());
        if (document == null) {
            return CompletableFuture.completedFuture(null);
        }
        return CompletableFuture.completedFuture(computeEdits(document));
    }

    @Override
    public CompletableFuture<List<? extends TextEdit>> rangeFormatting(DocumentRangeFormattingParams params) {
        NlDocument document = documentManager.getDocument(params.getTextDocument().getUri());
        if (document == null) {
            return CompletableFuture.completedFuture(null);
        }
        // Compute the edits the whole document would get (so alignment stays consistent with the rest of
        // each block), then keep only the ones on the selected lines.
        Range selection = params.getRange();
        List<TextEdit> inSelection = computeEdits(document).stream() //
                                                           .filter(e -> onSelectedLine(selection, e.getRange())) //
                                                           .toList();
        return CompletableFuture.completedFuture(inSelection);
    }

    private List<TextEdit> computeEdits(NlDocument document) {
        List<Token>         tokens         = document.tokens();
        List<TextEdit>      edits          = new ArrayList<>();
        Map<Token, Integer> operatorColumn = new HashMap<>();
        Map<Integer, Token> firstOnLine    = new HashMap<>();
        for (Token t : tokens) {
            if (t.type() == TokenType.BEGINOFFILE || t.type() == TokenType.ENDOFFILE) {
                continue;
            }
            firstOnLine.putIfAbsent(U.range(t).getStart().getLine(), t);
        }

        stripBaseIndent(tokens, firstOnLine, edits);
        alignMarkers(document, markers(tokens, t -> t.text().equals("?")), edits, null, firstOnLine);
        alignMarkers(document, markers(tokens, t -> DECLARATION_OPERATORS.contains(t.text())), edits, operatorColumn, firstOnLine);
        alignVarDeclNames(document, tokens, firstOnLine, edits);
        alignContinuations(tokens, operatorColumn, firstOnLine, edits);
        removeTrailingWhitespace(tokens, edits);
        return edits;
    }

    /** Whether an edit lands on a line covered by the selection (a whole-line selection ends at column 0 of the next line). */
    private static boolean onSelectedLine(Range selection, Range edit) {
        int firstLine = selection.getStart().getLine();
        int lastLine  = selection.getEnd().getLine();
        if (lastLine > firstLine && selection.getEnd().getCharacter() == 0) {
            lastLine--;
        }
        int editLine = edit.getStart().getLine();
        return firstLine <= editLine && editLine <= lastLine;
    }

    /**
     * The aligning operator on each line (the first, if a line happens to have several), in document order.
     * Only whitespace-delimited operators count: the parser splits glued operator runs to match patterns
     * (e.g. {@code <)?>} becomes {@code <} {@code )} {@code ?} {@code >}), and that pattern {@code ?} must
     * not be mistaken for a query {@code ?}.
     */
    private static List<Token> markers(List<Token> tokens, Predicate<Token> isMarker) {
        List<Token> result   = new ArrayList<>();
        int         lastLine = Integer.MIN_VALUE;
        Token       previous = null;
        for (Token t : tokens) {
            if (t.type() == TokenType.OPERATOR && isMarker.test(t) && whitespaceBefore(previous)) {
                int line = U.range(t).getStart().getLine();
                if (line != lastLine) {
                    result.add(t);
                    lastLine = line;
                }
            }
            previous = t;
        }
        return result;
    }

    /** A statement-level operator is preceded by whitespace or the start of a line/file, not glued to another token. */
    private static boolean whitespaceBefore(Token previous) {
        return previous == null //
                || previous.type() == TokenType.HSPACE //
                || previous.type() == TokenType.NEWLINE //
                || previous.type() == TokenType.BEGINOFFILE;
    }

    /**
     * Force every non-blank, non-continuation line to start at column 0 (decision #7). Continuation lines
     * are left for {@link #alignContinuations} to indent. Blank/whitespace-only lines are left to the
     * trailing-whitespace pass.
     */
    private static void stripBaseIndent(List<Token> tokens, Map<Integer, Token> firstOnLine, List<TextEdit> edits) {
        Map<Integer, List<Token>> significant = significantByLine(tokens);
        for (Map.Entry<Integer, Token> e : firstOnLine.entrySet()) {
            int   line  = e.getKey();
            Token first = e.getValue();
            if (first.type() != TokenType.HSPACE) {
                continue; // already at the margin
            }
            if (significant.get(line) == null) {
                continue; // blank / whitespace-only line: trailing-whitespace pass handles it
            }
            if (endsWithContinuation(significant.get(line - 1))) {
                continue; // a continuation line keeps its hanging indent
            }
            edits.add(new TextEdit(U.range(first), "")); // remove the leading indent
        }
    }

    /**
     * Within each run of adjacent marker lines, pad every marker to a shared column and follow it with one
     * space. When {@code finalColumn} is non-null, the column each marker ends up at is recorded there (used
     * by the comma-continuation indent to anchor under the first list item).
     * <p>
     * The target column is computed in indent-relative coordinates (absolute column − line indent), so that
     * after leading-indent stripping every marker lands at the same absolute column on the (now-unindented)
     * output line.
     */
    private static void alignMarkers(NlDocument document, List<Token> markers, List<TextEdit> edits,
            Map<Token, Integer> finalColumn, Map<Integer, Token> firstOnLine) {
        for (List<Token> block : consecutiveBlocks(markers)) {
            int targetColumn = block.stream()
                    .mapToInt(m -> leftEnd(document, m) - indentOf(m.line(), firstOnLine))
                    .max().orElse(0) + 1;
            for (Token m : block) {
                if (finalColumn != null) {
                    finalColumn.put(m, targetColumn);
                }
                addEdits(document, m, targetColumn, firstOnLine, edits);
            }
        }
    }

    /** Split the markers into runs of adjacent lines; a blank or non-marker line ends a run. */
    private static List<List<Token>> consecutiveBlocks(List<Token> markers) {
        List<List<Token>> blocks       = new ArrayList<>();
        List<Token>       current      = null;
        int               previousLine = Integer.MIN_VALUE;
        for (Token m : markers) {
            int line = U.range(m).getStart().getLine();
            if (current == null || line - previousLine > 1) {
                current = new ArrayList<>();
                blocks.add(current);
            }
            current.add(m);
            previousLine = line;
        }
        return blocks;
    }

    /** Column right after the last non-whitespace character before {@code marker} on its line. */
    private static int leftEnd(NlDocument document, Token marker) {
        Token before = document.prev(marker);
        if (before != null && before.type() == TokenType.HSPACE && before.line() == marker.line()) {
            return U.range(before).getStart().getCharacter();
        }
        return U.range(marker).getStart().getCharacter();
    }

    /** Leading horizontal-whitespace width of {@code line}, i.e. the indent of its first token (0 if it starts at the margin). */
    private static int indentOf(int line, Map<Integer, Token> firstOnLine) {
        Token first = firstOnLine.get(line);
        return first != null && first.type() == TokenType.HSPACE && first.line() == line
                ? U.range(first).getEnd().getCharacter()
                : 0;
    }

    /**
     * Pad the whitespace run immediately before {@code token} so {@code token} starts at {@code targetColumn}
     * (indent-relative: absolute column on the stripped line), touching nothing after it. Used for trailing
     * columns (variable names, {@code #N}, {@code @}, {@code if}, comments) whose following text is content
     * that must not move. Idempotent: emits no edit when already aligned.
     */
    private static void padBefore(NlDocument document, Token token, int targetColumn,
            Map<Integer, Token> firstOnLine, List<TextEdit> edits) {
        Token    before  = document.prev(token);
        Position start   = U.range(token).getStart();
        boolean  spaced  = before != null && before.type() == TokenType.HSPACE && before.line() == token.line();
        // Guard: if the before-HSPACE token IS the line's leading indent token, stripBaseIndent already
        // owns that range — emitting another edit over it would produce overlapping LSP edits.
        boolean beforeIsLeadingIndent = spaced && before == firstOnLine.get(token.line());
        if (beforeIsLeadingIndent) {
            return; // stripBaseIndent handles stripping to col 0; no additional before-edit needed
        }
        int indent  = indentOf(token.line(), firstOnLine);
        int leftEnd = spaced ? U.range(before).getStart().getCharacter() : start.getCharacter();
        if (start.getCharacter() - indent == targetColumn) {
            return; // already on the (indent-relative) column
        }
        Range range = spaced ? U.range(before) : new Range(start, start);
        edits.add(new TextEdit(range, " ".repeat(Math.max(1, targetColumn - (leftEnd - indent)))));
    }

    /**
     * Align {@code marker} to {@code targetColumn} (indent-relative: absolute column on the stripped line)
     * and emit {@code spaceAfter(marker)} spaces between it and whatever follows on the line (one for most
     * operators, two for {@code <=>}). Ranges are computed against the original document and never overlap,
     * so the before/after edits apply together. Edits are only emitted when the whitespace is actually
     * wrong, which keeps the result idempotent.
     */
    private static void addEdits(NlDocument document, Token marker, int targetColumn,
            Map<Integer, Token> firstOnLine, List<TextEdit> edits) {
        // ---- align the marker: (targetColumn - relLeftEnd) spaces in the before-run ----
        // relLeftEnd is the left-end position relative to the (stripped) line start, i.e. absolute - indent.
        Token    before     = document.prev(marker);
        Position mStart     = U.range(marker).getStart();
        int      indent     = indentOf(marker.line(), firstOnLine);
        boolean  spaced     = before != null && before.type() == TokenType.HSPACE && before.line() == marker.line();
        int      leftEnd    = spaced ? U.range(before).getStart().getCharacter() : mStart.getCharacter();
        int      relLeftEnd = leftEnd - indent;
        int      gap        = targetColumn - relLeftEnd;
        // current gap in original document
        int      curGap     = mStart.getCharacter() - leftEnd;
        // Guard: if the before-HSPACE token IS the line's leading indent token, stripBaseIndent already
        // owns that range — emitting another edit over the same range would produce overlapping LSP edits
        // (undefined behaviour). Skip the before-alignment edit; stripBaseIndent will strip to col 0,
        // which places the marker at the correct absolute column after stripping.
        boolean beforeIsLeadingIndent = spaced && before == firstOnLine.get(marker.line());
        if (!beforeIsLeadingIndent && curGap != gap) {
            Range range = spaced ? U.range(before) : new Range(mStart, mStart);
            edits.add(new TextEdit(range, " ".repeat(gap)));
        }

        // ---- spacing between the marker and what follows (if anything follows on this line) ----
        Token next = document.next(marker);
        if (endsLine(next)) {
            return; // marker ends the line: nothing follows
        }
        int      after = spaceAfter(marker);
        Position mEnd  = U.range(marker).getEnd();
        if (next.type() == TokenType.HSPACE) {
            if (endsLine(document.next(next))) {
                return; // only trailing whitespace after the marker (handled by the trim pass)
            }
            if (U.range(next).getEnd().getCharacter() - mEnd.getCharacter() != after) {
                edits.add(new TextEdit(U.range(next), " ".repeat(after)));
            }
        } else {
            edits.add(new TextEdit(new Range(mEnd, mEnd), " ".repeat(after))); // content touches the marker
        }
    }

    /** Map of line -&gt; meaningful tokens in order (no layout, no comments). */
    private static Map<Integer, List<Token>> significantByLine(List<Token> tokens) {
        Map<Integer, List<Token>> significant = new HashMap<>();
        for (Token t : tokens) {
            if (isMeaningful(t)) {
                significant.computeIfAbsent(U.range(t).getStart().getLine(), k -> new ArrayList<>()).add(t);
            }
        }
        return significant;
    }

    /**
     * True if {@code line}'s meaningful tokens are exactly NAME NAME (COMMA NAME)* — a {@code Type a, b}
     * variable declaration. The first NAME is the type; the remaining NAMEs (at least one) are variables
     * separated by commas.
     */
    private static boolean isVariableDeclaration(List<Token> line) {
        if (line == null || line.size() < 2) {
            return false;
        }
        if (line.get(0).type() != TokenType.NAME) {
            return false; // leading type name
        }
        for (int i = 1; i < line.size(); i++) {
            TokenType expected = (i % 2 == 1) ? TokenType.NAME : TokenType.COMMA; // i=1 NAME, i=2 COMMA, ...
            if (line.get(i).type() != expected) {
                return false;
            }
        }
        return true;
    }

    /**
     * Align the first variable name of each declaration in a consecutive block to a shared
     * (indent-relative) column. Blocks are split on non-declaration lines (or gaps in line numbers).
     */
    private static void alignVarDeclNames(NlDocument document, List<Token> tokens,
            Map<Integer, Token> firstOnLine, List<TextEdit> edits) {
        Map<Integer, List<Token>> significant = significantByLine(tokens);
        List<Token> nameMarkers = new ArrayList<>();
        for (int line : significant.keySet().stream().sorted().toList()) {
            List<Token> l = significant.get(line);
            if (isVariableDeclaration(l)) {
                nameMarkers.add(l.get(1)); // the first variable name
            }
        }
        for (List<Token> block : consecutiveBlocks(nameMarkers)) {
            int target = block.stream()
                              .mapToInt(m -> leftEnd(document, m) - indentOf(m.line(), firstOnLine))
                              .max().orElse(0) + 1;
            for (Token m : block) {
                padBefore(document, m, target, firstOnLine, edits);
            }
        }
    }

    /**
     * Hanging indent for continued statements: when a line's last meaningful token is a continuation token
     * (a {@code ,}, an operator such as {@code |}, an opening bracket — anything the parser carries onto the
     * next line), the lines that continue it are indented to line up under the first item of the head line
     * (the token after {@code ::}/{@code ::=}/{@code <=>}, or after the leading keyword such as {@code fact}).
     */
    private static void alignContinuations(List<Token> tokens, Map<Token, Integer> operatorColumn,
            Map<Integer, Token> firstOnLine, List<TextEdit> edits) {
        Map<Integer, List<Token>> significant = significantByLine(tokens);

        int anchor = -1;
        for (int line : significant.keySet().stream().sorted().toList()) {
            boolean continues = endsWithContinuation(significant.get(line - 1));
            if (endsWithContinuation(significant.get(line)) && !continues) {
                anchor = firstItemColumn(significant.get(line), operatorColumn, firstOnLine); // head line of a run
            }
            if (continues && anchor >= 0) {
                indentContinuation(line, anchor, significant.get(line).getFirst(), firstOnLine.get(line), firstOnLine, edits);
            }
        }
    }

    /**
     * Column (indent-relative) of the first list item on a head line: after the declaration operator, else
     * after the leading keyword. Indent-relative means the column as it appears on the stripped output line
     * (absolute column in the original − line indent).
     */
    private static int firstItemColumn(List<Token> lineTokens, Map<Token, Integer> operatorColumn,
            Map<Integer, Token> firstOnLine) {
        for (Token t : lineTokens) {
            if (t.type() == TokenType.OPERATOR && DECLARATION_OPERATORS.contains(t.text())) {
                int column = operatorColumn.getOrDefault(t,
                        U.range(t).getStart().getCharacter() - indentOf(t.line(), firstOnLine));
                return column + t.text().length() + spaceAfter(t); // operator is followed by spaceAfter spaces
            }
        }
        Token item   = lineTokens.size() >= 2 ? lineTokens.get(1) : lineTokens.getFirst();
        int   indent = indentOf(U.range(item).getStart().getLine(), firstOnLine);
        return U.range(item).getStart().getCharacter() - indent;
    }

    /** Re-indent a continuation line so its first token starts at {@code anchor}. */
    private static void indentContinuation(int line, int anchor, Token firstItem, Token leading,
            Map<Integer, Token> firstOnLine, List<TextEdit> edits) {
        if (U.range(firstItem).getStart().getCharacter() - indentOf(line, firstOnLine) == anchor) {
            return; // already aligned (indent-relative check)
        }
        Range range = leading != null && leading.type() == TokenType.HSPACE //
                ? U.range(leading) //
                : new Range(new Position(line, 0), new Position(line, 0));
        edits.add(new TextEdit(range, " ".repeat(anchor)));
    }

    private static boolean endsWithContinuation(List<Token> lineTokens) {
        return lineTokens != null && !lineTokens.isEmpty() && lineTokens.getLast().type().isContinuesOnNextLine();
    }

    /** A meaningful token is anything that is not layout (whitespace/newline/sentinels) or a comment. */
    private static boolean isMeaningful(Token t) {
        return switch (t.type()) {
            case HSPACE, NEWLINE, BEGINOFFILE, ENDOFFILE, END_LINE_COMMENT, IN_LINE_COMMENT -> false;
            default -> true;
        };
    }

    /** Drop any horizontal whitespace that sits at the end of a line (immediately before a newline or EOF). */
    private static void removeTrailingWhitespace(List<Token> tokens, List<TextEdit> edits) {
        for (int i = 0; i < tokens.size(); i++) {
            Token t = tokens.get(i);
            if (t.type() == TokenType.HSPACE && endsLine(i + 1 < tokens.size() ? tokens.get(i + 1) : null)) {
                edits.add(new TextEdit(U.range(t), ""));
            }
        }
    }

    /** A {@code null}, newline or end-of-file sentinel marks the end of the current line's content. */
    private static boolean endsLine(Token t) {
        return t == null || t.type() == TokenType.NEWLINE || t.type() == TokenType.ENDOFFILE;
    }
}
