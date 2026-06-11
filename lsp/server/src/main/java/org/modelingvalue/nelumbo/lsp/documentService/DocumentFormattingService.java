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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
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

/**
 * Formats a {@code .nl} document by emitting whitespace-only {@link TextEdit}s against the original document.
 * Every pass is idempotent and the edits it emits never overlap, so applying the formatter to its own output is
 * a no-op. {@code computeEdits} runs the passes in order: strip leading indent to column 0; align the query
 * {@code ?} and the declaration operators ({@code ::}/{@code ::=}/{@code <=>}) into columns; align
 * variable-declaration names; hang continuation lines under the first item; align the in-body columns
 * ({@code #N} precedence, {@code @}-annotations, {@code if} guards); align trailing {@code //} comments; trim
 * leading/trailing blank lines at the file edges and ensure a single final newline; and strip trailing whitespace.
 *
 * <p>Columns are computed in indent-relative coordinates (so they survive the indent strip), and in-body markers
 * are placed in post-format coordinates anchored at the statement's first item (so the operator's own alignment
 * shift is absorbed). See {@code docs/reference/formatting.md} for the user-facing description of the layout.
 */
@SuppressWarnings("DuplicatedCode")
public class DocumentFormattingService extends DocumentServiceAdapter {
    public DocumentFormattingService(NlDocumentManager documentManager) {
        super(documentManager);
    }

    /** Declaration/definition operators that are aligned together as one column. The query {@code ?} is aligned separately. */
    private static final Set<String> DECLARATION_OPERATORS = Set.of("::", "::=", "<=>");

    /** Spaces emitted after an aligned marker. {@code <=>} takes two (corpus convention); everything else one. */
    private static final Map<String, Integer> SPACE_AFTER = Map.of("<=>", 2);

    /** Spaces between the longest content and the aligned trailing {@code //} comment column. */
    private static final int COMMENT_GAP = 2;

    /** Spaces of indentation per {@code { }} scope-block nesting level. */
    private static final int INDENT_UNIT = 4;

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
        Set<Integer>        contentLines   = contentLines(tokens);
        Map<Integer, List<Token>> significant = significantByLine(tokens);
        Map<Integer, Integer>     braceDepth  = braceDepth(tokens);
        for (Token t : tokens) {
            if (t.type() == TokenType.BEGINOFFILE || t.type() == TokenType.ENDOFFILE) {
                continue;
            }
            firstOnLine.putIfAbsent(U.range(t).getStart().getLine(), t);
        }

        indentBaseLines(tokens, firstOnLine, braceDepth, edits);
        alignMarkers(document, markers(tokens, t -> t.text().equals("?")), edits, null, firstOnLine, significant, contentLines, braceDepth);
        alignMarkers(document, markers(tokens, t -> DECLARATION_OPERATORS.contains(t.text())), edits, operatorColumn, firstOnLine, significant, contentLines, braceDepth);
        alignVarDeclNames(document, tokens, firstOnLine, edits, significant, contentLines, braceDepth);
        alignContinuations(tokens, operatorColumn, firstOnLine, braceDepth, edits);
        alignBodyColumns(document, tokens, operatorColumn, firstOnLine, edits);
        alignComments(document, tokens, operatorColumn, firstOnLine, edits);
        trimEdgeBlankLines(tokens, edits);
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
     * Indent every non-blank, non-continuation (head) line to its scope-block base (depth ×
     * {@link #INDENT_UNIT} spaces). Continuation lines are left for {@link #alignContinuations} to indent.
     * Blank/whitespace-only lines are left to the trailing-whitespace pass. At depth 0 this strips the
     * leading indent to column 0 (the previous behaviour); deeper lines are set/inserted to the base indent.
     */
    private static void indentBaseLines(List<Token> tokens, Map<Integer, Token> firstOnLine,
            Map<Integer, Integer> braceDepth, List<TextEdit> edits) {
        Map<Integer, List<Token>> significant = significantByLine(tokens);
        for (Map.Entry<Integer, Token> e : firstOnLine.entrySet()) {
            int   line  = e.getKey();
            Token first = e.getValue();
            if (significant.get(line) == null) {
                continue; // blank / whitespace-only line: trailing-whitespace pass handles it
            }
            if (endsWithContinuation(significant.get(line - 1))) {
                continue; // a continuation line keeps its hanging indent
            }
            int base = braceDepth.getOrDefault(line, 0) * INDENT_UNIT;
            if (first.type() == TokenType.HSPACE) {
                int current = U.range(first).getEnd().getCharacter() - U.range(first).getStart().getCharacter();
                if (current != base) {
                    edits.add(new TextEdit(U.range(first), " ".repeat(base))); // set the leading indent to base
                }
            } else if (base > 0) {
                Position start = U.range(first).getStart();
                edits.add(new TextEdit(new Range(start, start), " ".repeat(base))); // insert the base indent
            }
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
            Map<Token, Integer> finalColumn, Map<Integer, Token> firstOnLine,
            Map<Integer, List<Token>> significant, Set<Integer> contentLines, Map<Integer, Integer> braceDepth) {
        for (List<Token> block : consecutiveBlocks(markers, significant, contentLines, braceDepth)) {
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

    /**
     * Split the markers into alignment blocks. Two consecutive markers stay in the same block when they are
     * on adjacent lines, or are two lines apart with a single BLANK line between them; a gap of two or more
     * blank lines, or an intervening content/comment line, starts a new block.
     */
    private static List<List<Token>> consecutiveBlocks(List<Token> markers, Map<Integer, List<Token>> significant,
            Set<Integer> contentLines, Map<Integer, Integer> braceDepth) {
        List<List<Token>> blocks       = new ArrayList<>();
        List<Token>       current      = null;
        int               previousLine = Integer.MIN_VALUE;
        for (Token m : markers) {
            int line = U.range(m).getStart().getLine();
            if (current == null || !sameAlignmentBlock(previousLine, line, significant, contentLines, braceDepth)) {
                current = new ArrayList<>();
                blocks.add(current);
            }
            current.add(m);
            previousLine = line;
        }
        return blocks;
    }

    /**
     * {@code prevHead} and {@code curHead} are marker (head) lines. They share an alignment block when {@code curHead}
     * immediately follows {@code prevHead}'s statement — the head plus all the continuation lines it carries onto the
     * next line — optionally across a single blank line. An independent statement, or two or more blank lines, between
     * them breaks the block. (A continuation line belongs to the previous statement, so it is transparent here.)
     * The two head lines must also be at the same brace depth, so an alignment block never crosses a {@code { }}
     * scope boundary — including when a marker line itself opens a block with a trailing {@code {}.
     */
    private static boolean sameAlignmentBlock(int prevHead, int curHead, Map<Integer, List<Token>> significant,
            Set<Integer> contentLines, Map<Integer, Integer> braceDepth) {
        if (!Objects.equals(braceDepth.get(prevHead), braceDepth.get(curHead))) {
            return false; // different scope depth: never one alignment block
        }
        int end = prevHead;
        while (endsWithContinuation(significant.get(end))) {
            end++;
        }
        int gap = curHead - end;
        return gap == 1 || (gap == 2 && !contentLines.contains(end + 1));
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

    /** Indent depth (in {@code {}} nesting levels) for each line: a line starting with `}` is at its closing
     *  level; the depth otherwise reflects the number of unclosed `{` before the line. */
    private static Map<Integer, Integer> braceDepth(List<Token> tokens) {
        Map<Integer, List<Token>> byLine = new TreeMap<>();
        for (Token t : tokens) {
            if (t.type() == TokenType.BEGINOFFILE || t.type() == TokenType.ENDOFFILE) continue;
            byLine.computeIfAbsent(U.range(t).getStart().getLine(), k -> new ArrayList<>()).add(t);
        }
        Map<Integer, Integer> depth = new HashMap<>();
        int d = 0;
        for (Map.Entry<Integer, List<Token>> e : byLine.entrySet()) {
            int opens = 0, closes = 0, leadingClose = 0;
            boolean started = false;
            for (Token t : e.getValue()) {
                boolean isOpen  = t.type() == TokenType.LEFT  && t.text().equals("{");
                boolean isClose = t.type() == TokenType.RIGHT && t.text().equals("}");
                if (isClose) { closes++; if (!started) leadingClose++; }
                if (isOpen)  { opens++; }
                if (isMeaningful(t)) started = true; // a comment/layout token does not end the leading-} run
            }
            depth.put(e.getKey(), Math.max(0, d - leadingClose));
            d += opens - closes;
        }
        return depth;
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
     * Index where the variable names begin, after a leading type {@code NAME} with an optional balanced
     * {@code <...>} generic; or -1 if the leading tokens are not a well-formed type. Inside the generic,
     * NAME/COMMA and angle operators are all part of the type; balance is tracked over angle operators
     * (+1 per {@code <} char, -1 per {@code >} char in each OPERATOR token's text).
     */
    private static int typeEnd(List<Token> line) {
        if (line == null || line.isEmpty() || line.get(0).type() != TokenType.NAME) {
            return -1;
        }
        int i = 1;
        if (i < line.size() && line.get(i).type() == TokenType.OPERATOR && line.get(i).text().indexOf('<') >= 0) {
            int depth = 0;
            for (; i < line.size(); i++) {
                Token t = line.get(i);
                if (t.type() == TokenType.OPERATOR) {
                    for (char c : t.text().toCharArray()) {
                        if (c == '<') {
                            depth++;
                        } else if (c == '>') {
                            depth--;
                        } else {
                            return -1; // a non-angle char inside the generic: not a plain type
                        }
                    }
                } else if (t.type() != TokenType.NAME && t.type() != TokenType.COMMA) {
                    return -1; // unexpected token inside the generic
                }
                if (depth == 0) {
                    i++;
                    break;
                }
            }
            if (depth != 0) {
                return -1; // unbalanced
            }
        }
        return i;
    }

    /**
     * The first variable name of a {@code Type name, name, …} declaration, or null if the line isn't one.
     * The type is a leading {@code NAME} with an optional balanced generic; the remainder must be
     * {@code NAME (COMMA NAME)*} with at least one variable name.
     */
    private static Token firstVariableName(List<Token> line) {
        int i = typeEnd(line);
        if (i < 0 || i >= line.size()) {
            return null; // not a type, or a bare type with no variable names
        }
        Token first = line.get(i);
        // remainder must be NAME (COMMA NAME)*
        boolean expectName = true;
        for (int j = i; j < line.size(); j++) {
            TokenType expected = expectName ? TokenType.NAME : TokenType.COMMA;
            if (line.get(j).type() != expected) {
                return null;
            }
            expectName = !expectName;
        }
        if (expectName) {
            return null; // ended on a COMMA (trailing comma)
        }
        return first;
    }

    /**
     * Align the first variable name of each declaration in a consecutive block to a shared
     * (indent-relative) column. Blocks are split on non-declaration lines (or gaps in line numbers).
     */
    private static void alignVarDeclNames(NlDocument document, List<Token> tokens,
            Map<Integer, Token> firstOnLine, List<TextEdit> edits,
            Map<Integer, List<Token>> significant, Set<Integer> contentLines, Map<Integer, Integer> braceDepth) {
        List<Token> nameMarkers = new ArrayList<>();
        for (int line : significant.keySet().stream().sorted().toList()) {
            List<Token> l         = significant.get(line);
            Token       firstName = firstVariableName(l);
            if (firstName != null) {
                nameMarkers.add(firstName); // the first variable name
            }
        }
        for (List<Token> block : consecutiveBlocks(nameMarkers, significant, contentLines, braceDepth)) {
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
            Map<Integer, Token> firstOnLine, Map<Integer, Integer> braceDepth, List<TextEdit> edits) {
        Map<Integer, List<Token>> significant = significantByLine(tokens);

        int anchor = -1;
        for (int line : significant.keySet().stream().sorted().toList()) {
            boolean continues = endsWithContinuation(significant.get(line - 1));
            if (endsWithContinuation(significant.get(line)) && !continues) {
                anchor = firstItemColumn(significant.get(line), operatorColumn, firstOnLine); // head line of a run
            }
            if (continues && anchor >= 0) {
                int base = braceDepth.getOrDefault(line, 0) * INDENT_UNIT;
                indentContinuation(line, base + anchor, significant.get(line).getFirst(), firstOnLine.get(line), firstOnLine, edits);
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
        for (int i = 0; i < lineTokens.size(); i++) {
            Token t = lineTokens.get(i);
            if (t.type() == TokenType.OPERATOR && DECLARATION_OPERATORS.contains(t.text())) {
                int column = operatorColumn.getOrDefault(t,
                        U.range(t).getStart().getCharacter() - indentOf(t.line(), firstOnLine));
                return column + t.text().length() + spaceAfter(t); // operator is followed by spaceAfter spaces
            }
        }
        Token item   = firstItemToken(lineTokens);
        int   indent = indentOf(U.range(item).getStart().getLine(), firstOnLine);
        return U.range(item).getStart().getCharacter() - indent;
    }

    /**
     * The significant token whose post-format column {@link #firstItemColumn} reports: the FIRST item after a
     * declaration operator ({@code ::}/{@code ::=}/{@code <=>}), else the second significant token after a
     * leading keyword (e.g. the first list item after {@code fact}). Kept in lock-step with
     * {@link #firstItemColumn} so head-line body markers can be measured from this item's ORIGINAL column.
     */
    private static Token firstItemToken(List<Token> lineTokens) {
        for (int i = 0; i < lineTokens.size(); i++) {
            Token t = lineTokens.get(i);
            if (t.type() == TokenType.OPERATOR && DECLARATION_OPERATORS.contains(t.text())) {
                return i + 1 < lineTokens.size() ? lineTokens.get(i + 1) : t;
            }
        }
        return lineTokens.size() >= 2 ? lineTokens.get(1) : lineTokens.getFirst();
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
        return lineTokens != null && !lineTokens.isEmpty() && continuesOntoNextLine(lineTokens.getLast());
    }

    /**
     * A trailing token that genuinely continues the statement onto the next line — but a closing-angle
     * operator ({@code >}, {@code >>}) that ends a generic type, or a trailing query {@code ?} with no
     * expected-result clause, does NOT (it terminates the line). Real
     * continuation operators ({@code ,}, {@code |}, {@code &}, {@code ->}, {@code <=>}, opening brackets
     * {@code ([{}) are not all-{@code >}, so they still continue.
     */
    private static boolean continuesOntoNextLine(Token last) {
        if (!last.type().isContinuesOnNextLine()) {
            return false;
        }
        if (last.type() == TokenType.LEFT && last.text().equals("{")) {
            return false; // a trailing `{` opens a scope block; its contents are their own head statements
        }
        return !(last.type() == TokenType.OPERATOR && !last.text().isEmpty()
                && (last.text().equals("?") || last.text().chars().allMatch(c -> c == '>')));
        // a trailing query `?` or a closing-angle `>`/`>>` terminates the line
    }

    /** A meaningful token is anything that is not layout (whitespace/newline/sentinels) or a comment. */
    private static boolean isMeaningful(Token t) {
        return switch (t.type()) {
            case HSPACE, NEWLINE, BEGINOFFILE, ENDOFFILE, END_LINE_COMMENT, IN_LINE_COMMENT -> false;
            default -> true;
        };
    }

    /**
     * Runs of lines forming one multi-line statement: a head line plus every line that continues the previous one.
     * {@code endsWithContinuation(null)} returns false, so a line whose predecessor is blank/absent starts a new block.
     */
    private static List<List<Integer>> bodyBlocks(Map<Integer, List<Token>> significant) {
        List<List<Integer>> blocks  = new ArrayList<>();
        List<Integer>       current = null;
        for (int line : significant.keySet().stream().sorted().toList()) {
            boolean continues = endsWithContinuation(significant.get(line - 1));
            if (!continues || current == null) {
                current = new ArrayList<>();
                blocks.add(current);
            }
            current.add(line);
        }
        return blocks;
    }

    /**
     * The {@code #} of a TRAILING {@code #N} precedence marker on a line, or null.
     * <p>
     * A trailing alternative-precedence {@code #N} differs from an EMBEDDED one (e.g. {@code <Time#50>}) in
     * two reliable ways:
     * <ol>
     *   <li>It is preceded by whitespace ({@code document.prev} returns HSPACE/NEWLINE/BEGINOFFILE), whereas
     *       an embedded {@code #} is glued directly to the type name ({@code <Time#50>} → prev is NAME).</li>
     *   <li>After its NUMBER the next non-HSPACE token is end-of-line (null/NEWLINE/ENDOFFILE), a COMMA, a
     *       comment, or an {@code @}-annotation — NOT a closing bracket or more pattern content.</li>
     * </ol>
     * Returns the LAST qualifying token on the line (if multiple, which should not normally occur).
     */
    private static Token precedenceMarker(List<Token> lineTokens, NlDocument document) {
        Token result = null;
        for (Token t : lineTokens) {
            if (t.type() != TokenType.OPERATOR || !t.text().equals("#")) {
                continue;
            }
            // Condition (a): # must be whitespace-delimited on the left.
            Token prev = document.prev(t);
            if (!whitespaceBefore(prev)) {
                continue; // glued to previous content (e.g. inside <Time#50>)
            }
            // Condition (b): immediately followed by a NUMBER.
            Token number = document.next(t);
            if (number == null || number.type() != TokenType.NUMBER) {
                continue;
            }
            // Condition (b): after the NUMBER (skipping HSPACE), must be end-of-line, COMMA, comment, or @-annotation.
            Token afterNumber = document.next(number);
            // skip any trailing horizontal space
            if (afterNumber != null && afterNumber.type() == TokenType.HSPACE) {
                afterNumber = document.next(afterNumber);
            }
            if (!isTrailingPrecedenceFollower(afterNumber)) {
                continue; // followed by pattern content (e.g. > in <Boolean#0>) — embedded, not trailing
            }
            result = t; // keep searching; return the LAST qualifying one
        }
        return result;
    }

    /**
     * Returns true when the token after a candidate {@code #N} number confirms that the {@code #N} is a
     * trailing alternative-precedence marker rather than an embedded type-reference precedence.
     * <p>
     * Allowed followers: end-of-line (null/NEWLINE/ENDOFFILE), COMMA, end-line or inline comment,
     * or an OPERATOR whose text starts with {@code @} (annotation).  Everything else (brackets, NAME,
     * NUMBER, other operators) means the {@code #} is embedded inside pattern content.
     */
    private static boolean isTrailingPrecedenceFollower(Token t) {
        if (t == null) {
            return true;
        }
        return switch (t.type()) {
            case NEWLINE, ENDOFFILE -> true;
            case COMMA              -> true;
            case END_LINE_COMMENT, IN_LINE_COMMENT -> true;
            case OPERATOR           -> t.text().startsWith("@");
            default                 -> false;
        };
    }

    /** The head line of the body block that {@code line} belongs to (the first non-continuation line at or above it). */
    private static int headLineOf(int line, Map<Integer, List<Token>> significant) {
        int headLine = line;
        while (endsWithContinuation(significant.get(headLine - 1))) {
            headLine--;
        }
        return headLine;
    }

    /**
     * Post-format effective content-end of a body marker, measured against the shared anchor its block hangs
     * under. The anchor ({@code firstItemColumn} of the head line) is the column where the first item / every
     * continuation line begins post-format; a marker keeps its ORIGINAL offset from that block's content
     * region:
     * <pre>effEnd = anchor + (leftEnd(marker) - regionStart)</pre>
     * where {@code regionStart} is the ORIGINAL absolute column where the line's logical content region begins:
     * the first item after the operator/keyword on the HEAD line (because the head's fixed pre-operator part is
     * not part of the aligned region and the operator may shift), or the leading indent on a continuation line.
     * This makes head and continuation markers share one coordinate frame even when the head-line declaration
     * operator's position or trailing spacing changes during formatting.
     */
    private static int bodyMarkerEffEnd(NlDocument document, Token marker, Map<Integer, List<Token>> significant,
            Map<Token, Integer> operatorColumn, Map<Integer, Token> firstOnLine) {
        int         line     = marker.line();
        int         headLine = headLineOf(line, significant);
        List<Token> head     = significant.get(headLine);
        int         anchor   = firstItemColumn(head, operatorColumn, firstOnLine);
        int         regionStart;
        if (line == headLine) {
            Token item = firstItemToken(head);
            regionStart = U.range(item).getStart().getCharacter(); // original absolute column of the first item
        } else {
            regionStart = indentOf(line, firstOnLine); // continuation line: content starts at the leading indent
        }
        return anchor + (leftEnd(document, marker) - regionStart);
    }

    /**
     * Align a set of body-block marker tokens (one per line, in document order) to a single absolute column.
     * Each marker's post-format effective content-end is computed by {@link #bodyMarkerEffEnd} (head and
     * continuation lines share one coordinate frame anchored at the block's first-item column). The shared
     * target column is {@code max(effEnd) + gap}; every marker is then placed there via {@link #placeMarkerAt},
     * sizing its before-gap from its KNOWN final preceding-end (its own {@code effEnd}) so the head-line
     * operator shift is accounted for.
     *
     * @param gap spaces between the longest preceding content and the marker column (1 for operators, 2 for comments)
     * @return the absolute target column that was computed (and all markers placed at), or {@code -1} when
     *         there are fewer than two markers and alignment was skipped.
     */
    private static int alignBodyMarkerColumn(NlDocument document, List<Token> markers,
            Map<Integer, List<Token>> significant, Map<Token, Integer> operatorColumn,
            Map<Integer, Token> firstOnLine, List<TextEdit> edits, int gap) {
        if (markers.size() < 2) {
            return -1;
        }
        Map<Token, Integer> effEnd = new HashMap<>();
        for (Token m : markers) {
            effEnd.put(m, bodyMarkerEffEnd(document, m, significant, operatorColumn, firstOnLine));
        }
        int targetAbs = effEnd.values().stream().mapToInt(Integer::intValue).max().orElse(0) + gap;
        for (Token m : markers) {
            placeMarkerAt(document, m, targetAbs, effEnd.get(m), edits);
        }
        return targetAbs;
    }

    /** True if this body block is a rule body (its head line contains the {@code <=>} operator). */
    private static boolean isRuleBody(List<Integer> block, Map<Integer, List<Token>> significant) {
        List<Token> head = significant.get(block.getFirst());
        return head != null && head.stream().anyMatch(t -> t.type() == TokenType.OPERATOR && t.text().equals("<=>"));
    }

    /** The {@code if} guard keyword on a line (a NAME whose text is exactly {@code "if"}), or null. */
    private static Token ifMarker(List<Token> lineTokens) {
        if (lineTokens == null) {
            return null;
        }
        for (Token t : lineTokens) {
            if (t.type() == TokenType.NAME && t.text().equals("if")) {
                return t;
            }
        }
        return null;
    }

    /** The {@code @} of an annotation on a line (OPERATOR whose text starts with '@'), or null. */
    private static Token annotationMarker(List<Token> lineTokens) {
        if (lineTokens == null) {
            return null;
        }
        for (Token t : lineTokens) {
            if (t.type() == TokenType.OPERATOR && t.text().startsWith("@")) {
                return t;
            }
        }
        return null;
    }

    /**
     * Width of the {@code #N} run ({@code #} + its NUMBER) on a line whose precedence marker is {@code hash}.
     * Expects a token returned by {@link #precedenceMarker} — i.e. a {@code #} immediately followed by a
     * same-line NUMBER token. Do not pass arbitrary {@code #} tokens.
     */
    private static int hashWidth(Token hash, NlDocument document) {
        Token number = document.next(hash);
        return hash.text().length() + (number != null && number.type() == TokenType.NUMBER ? number.text().length() : 0);
    }

    /**
     * Replace the whitespace run immediately before {@code marker} so the marker starts at absolute column
     * {@code targetColumn}, given that the content preceding it ends at {@code precedingEnd} in the final
     * (post-format) document. Used for cumulative columns where the preceding field was itself realigned and so
     * its FINAL position — not its original one — determines the gap. Idempotent: no edit when already placed.
     */
    private static void placeMarkerAt(NlDocument document, Token marker, int targetColumn, int precedingEnd,
            List<TextEdit> edits) {
        Token    before = document.prev(marker);
        Position start  = U.range(marker).getStart();
        boolean  spaced = before != null && before.type() == TokenType.HSPACE && before.line() == marker.line();
        if (start.getCharacter() == targetColumn) {
            return; // already at the target column
        }
        int   width = Math.max(1, targetColumn - precedingEnd);
        Range range = spaced ? U.range(before) : new Range(start, start);
        edits.add(new TextEdit(range, " ".repeat(width)));
    }

    /**
     * Align the {@code #N} precedence markers and {@code @} annotation markers of each body block's
     * alternatives to their own absolute columns.
     * <p>
     * {@code #N} markers are aligned first via {@link #alignBodyMarkerColumn}. The {@code @} column is then
     * computed in post-{@code #N}-alignment coordinates: for a line that also has a {@code #N}, the
     * preceding-content-end of {@code @} is {@code C_hash + hashWidth}, where {@code C_hash} is the shared
     * absolute column the {@code #N} markers were placed at.
     */
    private static void alignBodyColumns(NlDocument document, List<Token> tokens,
            Map<Token, Integer> operatorColumn, Map<Integer, Token> firstOnLine, List<TextEdit> edits) {
        Map<Integer, List<Token>> significant = significantByLine(tokens);
        for (List<Integer> block : bodyBlocks(significant)) {
            // Align `if` guards in rule bodies (<=> blocks).
            if (isRuleBody(block, significant)) {
                List<Token> ifs = new ArrayList<>();
                for (int line : block) {
                    Token ifToken = ifMarker(significant.get(line));
                    if (ifToken != null) {
                        ifs.add(ifToken);
                    }
                }
                alignBodyMarkerColumn(document, ifs, significant, operatorColumn, firstOnLine, edits, 1);
            }

            // Collect #N markers and build a line->hash map.
            List<Token>          hashes    = new ArrayList<>();
            Map<Integer, Token>  hashByLine = new HashMap<>();
            for (int line : block) {
                Token h = precedenceMarker(significant.get(line), document);
                if (h != null) {
                    hashes.add(h);
                    hashByLine.put(line, h);
                }
            }
            int hashColumn = alignBodyMarkerColumn(document, hashes, significant, operatorColumn, firstOnLine, edits, 1);

            // Collect @ markers.
            List<Token> ats = new ArrayList<>();
            for (int line : block) {
                Token a = annotationMarker(significant.get(line));
                if (a != null) {
                    ats.add(a);
                }
            }
            if (ats.size() >= 2) {
                // Compute preceding-content-end for each @ in final (post-format) coordinates.
                Map<Token, Integer> precedingEnd = new HashMap<>();
                for (Token a : ats) {
                    int line = a.line();
                    Token h  = hashByLine.get(line);
                    int pe;
                    if (h != null && hashColumn >= 0) {
                        pe = hashColumn + hashWidth(h, document);
                    } else {
                        // no #N on this line: @'s preceding content is its own pattern; post-format left-edge
                        // = anchor + the @'s original offset from this line's content region.
                        pe = bodyMarkerEffEnd(document, a, significant, operatorColumn, firstOnLine);
                    }
                    precedingEnd.put(a, pe);
                }
                int annotationColumn = precedingEnd.values().stream().mapToInt(Integer::intValue).max().orElse(0) + 1;
                for (Token a : ats) {
                    placeMarkerAt(document, a, annotationColumn, precedingEnd.get(a), edits);
                }
            }
        }
    }

    /** The trailing line comment on a line (an END_LINE_COMMENT token), or null. */
    private static Token trailingComment(List<Token> tokens, int line) {
        for (Token t : tokens) {
            if (t.type() == TokenType.END_LINE_COMMENT && U.range(t).getStart().getLine() == line) {
                return t;
            }
        }
        return null;
    }

    /** Align trailing {@code //} comments of each body block to a shared column, {@link #COMMENT_GAP} past the longest content. */
    private static void alignComments(NlDocument document, List<Token> tokens,
            Map<Token, Integer> operatorColumn, Map<Integer, Token> firstOnLine, List<TextEdit> edits) {
        Map<Integer, List<Token>> significant = significantByLine(tokens);
        for (List<Integer> block : bodyBlocks(significant)) {
            List<Token> comments = new ArrayList<>();
            for (int line : block) {
                Token c = trailingComment(tokens, line);
                if (c != null) {
                    comments.add(c);
                }
            }
            alignBodyMarkerColumn(document, comments, significant, operatorColumn, firstOnLine, edits, COMMENT_GAP);
        }
    }

    /** Every line carrying a meaningful token or a comment token — the document's "content lines". */
    private static Set<Integer> contentLines(List<Token> tokens) {
        Set<Integer> contentLines = new HashSet<>();
        for (Token t : tokens) {
            if (isMeaningful(t) || t.type() == TokenType.END_LINE_COMMENT || t.type() == TokenType.IN_LINE_COMMENT) {
                contentLines.add(U.range(t).getStart().getLine());
            }
        }
        return contentLines;
    }

    /**
     * Normalise the file edges like vim: remove blank lines before the first content line and after the last
     * content line, and guarantee the file ends with exactly one newline. All internal blank-line runs are
     * left exactly as written. A blank line carries no meaningful token and no comment, so a comment-only line
     * (e.g. a license-header line) is content and is preserved.
     * <p>
     * A leading/trailing blank line is deleted by removing its NEWLINE token range, which the tokenizer spans
     * as {@code (L, col)..(L+1, 0)} — exactly the line terminator — so line {@code L} merges into line
     * {@code L+1}. Combined with {@link #removeTrailingWhitespace}, which strips any HSPACE on the blank line
     * (a distinct, earlier range), the whole blank line disappears. A blank line's NEWLINE is deleted only
     * when it is a leading blank ({@code L < firstContentLine}) or trailing blank ({@code L > lastContentLine});
     * internal blanks are never touched.
     * <p>
     * The single-trailing-newline guarantee: if the last content line carries no NEWLINE token (the file did
     * not end with a newline), a {@code \n} is inserted at the ENDOFFILE token's zero-width position — which
     * the tokenizer places immediately after the last consumed character. An empty / all-blank file (no content
     * lines at all) is left empty: every NEWLINE is deleted and no newline is inserted.
     */
    private static void trimEdgeBlankLines(List<Token> tokens, List<TextEdit> edits) {
        Set<Integer>        contentLines = contentLines(tokens);
        Map<Integer, Token> newlineOf    = new HashMap<>();
        Token               endOfFile    = null;
        // A multi-line block comment (/* ... */) is a single token whose interior newlines are consumed by
        // the token, so its interior lines have no NEWLINE token in newlineOf and can never be deleted here.
        for (Token t : tokens) {
            if (t.type() == TokenType.NEWLINE) {
                newlineOf.put(U.range(t).getStart().getLine(), t);
            } else if (t.type() == TokenType.ENDOFFILE) {
                endOfFile = t;
            }
        }
        int firstContentLine = contentLines.stream().mapToInt(Integer::intValue).min().orElse(-1);
        int lastContentLine  = contentLines.stream().mapToInt(Integer::intValue).max().orElse(-1);
        if (lastContentLine < 0) {
            // empty / all-blank file: delete every newline, add nothing.
            for (Token nl : newlineOf.values()) {
                edits.add(new TextEdit(U.range(nl), ""));
            }
            return;
        }
        for (Map.Entry<Integer, Token> e : newlineOf.entrySet()) {
            int line = e.getKey();
            if (line < firstContentLine || line > lastContentLine) { // leading/trailing blank line: drop its newline
                edits.add(new TextEdit(U.range(e.getValue()), ""));
            }
        }
        // Ensure a single trailing newline: when the last content line has no NEWLINE token, the file did not
        // end with a newline, so insert one at the ENDOFFILE position (a zero-width range after the last char).
        if (!newlineOf.containsKey(lastContentLine) && endOfFile != null) {
            edits.add(new TextEdit(U.range(endOfFile), "\n"));
        }
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
