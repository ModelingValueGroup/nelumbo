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
        List<Token>        tokens          = document.tokens();
        List<TextEdit>     edits           = new ArrayList<>();
        Map<Token, Integer> operatorColumn = new HashMap<>();

        // queries align on their own; the declaration operators all share one column.
        alignMarkers(document, markers(tokens, t -> t.text().equals("?")), edits, null);
        alignMarkers(document, markers(tokens, t -> DECLARATION_OPERATORS.contains(t.text())), edits, operatorColumn);
        alignContinuations(tokens, operatorColumn, edits);
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
     * Within each run of adjacent marker lines, pad every marker to a shared column and follow it with one
     * space. When {@code finalColumn} is non-null, the column each marker ends up at is recorded there (used
     * by the comma-continuation indent to anchor under the first list item).
     */
    private static void alignMarkers(NlDocument document, List<Token> markers, List<TextEdit> edits, Map<Token, Integer> finalColumn) {
        for (List<Token> block : consecutiveBlocks(markers)) {
            int targetColumn = block.stream().mapToInt(m -> leftEnd(document, m)).max().orElse(0) + 1;
            for (Token m : block) {
                if (finalColumn != null) {
                    finalColumn.put(m, targetColumn);
                }
                addEdits(document, m, targetColumn, edits);
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

    /**
     * Align {@code marker} to {@code targetColumn} (so every marker in a block lines up) and put a single
     * space between it and whatever follows on the line. Ranges are computed against the original document
     * and never overlap, so the before/after edits apply together. Edits are only emitted when the
     * whitespace is actually wrong, which keeps the result idempotent.
     */
    private static void addEdits(NlDocument document, Token marker, int targetColumn, List<TextEdit> edits) {
        // ---- align the marker: exactly (targetColumn - leftEnd) spaces before it ----
        Token    before  = document.prev(marker);
        Position mStart  = U.range(marker).getStart();
        boolean  spaced  = before != null && before.type() == TokenType.HSPACE && before.line() == marker.line();
        int      leftEnd = spaced ? U.range(before).getStart().getCharacter() : mStart.getCharacter();
        int      gap     = targetColumn - leftEnd;
        if (mStart.getCharacter() - leftEnd != gap) {
            Range range = spaced ? U.range(before) : new Range(mStart, mStart);
            edits.add(new TextEdit(range, " ".repeat(gap)));
        }

        // ---- a single space between the marker and what follows (if anything follows on this line) ----
        Token next = document.next(marker);
        if (endsLine(next)) {
            return; // marker ends the line: nothing follows
        }
        Position mEnd = U.range(marker).getEnd();
        if (next.type() == TokenType.HSPACE) {
            if (endsLine(document.next(next))) {
                return; // only trailing whitespace after the marker (handled by the trim pass)
            }
            if (U.range(next).getEnd().getCharacter() - mEnd.getCharacter() != 1) {
                edits.add(new TextEdit(U.range(next), " "));
            }
        } else {
            edits.add(new TextEdit(new Range(mEnd, mEnd), " ")); // content touches the marker: insert one space
        }
    }

    /**
     * Hanging indent for continued statements: when a line's last meaningful token is a continuation token
     * (a {@code ,}, an operator such as {@code |}, an opening bracket — anything the parser carries onto the
     * next line), the lines that continue it are indented to line up under the first item of the head line
     * (the token after {@code ::}/{@code ::=}/{@code <=>}, or after the leading keyword such as {@code fact}).
     */
    private static void alignContinuations(List<Token> tokens, Map<Token, Integer> operatorColumn, List<TextEdit> edits) {
        Map<Integer, List<Token>> significant  = new HashMap<>(); // line -> meaningful tokens, in order
        Map<Integer, Token>       firstOnLine  = new HashMap<>(); // line -> leftmost token (incl. indent whitespace)
        for (Token t : tokens) {
            if (t.type() == TokenType.BEGINOFFILE || t.type() == TokenType.ENDOFFILE) {
                continue;
            }
            int line = U.range(t).getStart().getLine();
            firstOnLine.putIfAbsent(line, t);
            if (isMeaningful(t)) {
                significant.computeIfAbsent(line, k -> new ArrayList<>()).add(t);
            }
        }

        int anchor = -1;
        for (int line : significant.keySet().stream().sorted().toList()) {
            boolean continues = endsWithContinuation(significant.get(line - 1));
            if (endsWithContinuation(significant.get(line)) && !continues) {
                anchor = firstItemColumn(significant.get(line), operatorColumn); // head line of a run
            }
            if (continues && anchor >= 0) {
                indentContinuation(line, anchor, significant.get(line).getFirst(), firstOnLine.get(line), edits);
            }
        }
    }

    /** Column of the first list item on a head line: after the declaration operator, else after the leading keyword. */
    private static int firstItemColumn(List<Token> lineTokens, Map<Token, Integer> operatorColumn) {
        for (Token t : lineTokens) {
            if (t.type() == TokenType.OPERATOR && DECLARATION_OPERATORS.contains(t.text())) {
                int column = operatorColumn.getOrDefault(t, U.range(t).getStart().getCharacter());
                return column + t.text().length() + 1; // operator is followed by exactly one space
            }
        }
        Token item = lineTokens.size() >= 2 ? lineTokens.get(1) : lineTokens.getFirst();
        return U.range(item).getStart().getCharacter();
    }

    /** Re-indent a continuation line so its first token starts at {@code anchor}. */
    private static void indentContinuation(int line, int anchor, Token firstItem, Token leading, List<TextEdit> edits) {
        if (U.range(firstItem).getStart().getCharacter() == anchor) {
            return; // already aligned
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
