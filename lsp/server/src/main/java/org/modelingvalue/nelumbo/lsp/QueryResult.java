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

package org.modelingvalue.nelumbo.lsp;

import java.util.Locale;

import org.eclipse.lsp4j.Range;

/**
 * The outcome of evaluating a single {@code Query}.
 * <ul>
 *   <li>{@code RESULT}   — query without an expected clause; {@code inferred} is the inference result.</li>
 *   <li>{@code MATCH}    — query whose expected clause matches; rendered as just a checkmark.</li>
 *   <li>{@code MISMATCH} — query whose expected clause differs; {@code inferred} is the calculated result
 *       (shown inline), {@code message} describes the difference, {@code expectedRange} is the source span of
 *       the expected clause to underline.</li>
 *   <li>{@code ERROR}    — evaluation failed outside the model (an internal error); {@code inferred} holds the message.</li>
 *   <li>{@code FAILED} - the model gives no answer: an inconsistency, an error in the model, or the workspace deadline
 *       passed; {@code inferred} holds the message.</li>
 *   <li>{@code CALCULATING} - placeholder until the result is there.</li>
 *   <li>{@code OVERLOADED} - stopped by the short budget of a busy server ({@code EvalGate}); {@code inferred} holds the message.</li>
 * </ul>
 * {@code micros} is the wall-clock time of the query's inference ({@code Query.inferMicros()}), -1 when there is
 * none to show; the tooltip shows it, the inline label does not (it would change on every evaluation).
 */
public record QueryResult(Kind kind, String inferred, String message, Range expectedRange, long micros) {

    public enum Kind {
        RESULT,
        MATCH,
        MISMATCH,
        ERROR,
        FAILED,
        OVERLOADED,
        CALCULATING,
    }

    public static QueryResult result(String inferred, long micros) {
        return new QueryResult(Kind.RESULT, inferred, null, null, micros);
    }

    public static QueryResult match(String inferred, long micros) {
        return new QueryResult(Kind.MATCH, inferred, null, null, micros);
    }

    public static QueryResult mismatch(String inferred, String message, Range expectedRange, long micros) {
        return new QueryResult(Kind.MISMATCH, inferred, message, expectedRange, micros);
    }

    public static QueryResult error(String message, long micros) {
        return new QueryResult(Kind.ERROR, message, message, null, micros);
    }

    public static QueryResult failed(String message, long micros) {
        return new QueryResult(Kind.FAILED, message, message, null, micros);
    }

    /** A query stopped by the workspace deadline, for example "evaluation took too long (over 30 s)". */
    public static QueryResult timeout(long deadlineMs) {
        String limit = deadlineMs % 1000 == 0 ? deadlineMs / 1000 + " s" : deadlineMs + " ms";
        return failed("evaluation took too long (over " + limit + ")", -1);
    }

    /** The placeholder of a query that has no result yet. */
    public static QueryResult calculating() {
        return new QueryResult(Kind.CALCULATING, "", null, null, -1);
    }

    /** Diagnostic code of an evaluation stopped because the server was busy; the contract for clients. */
    public static final String OVERLOAD_CODE = "server-overloaded";

    public static QueryResult overloaded(long budgetMs) {
        String message = "Server busy: evaluation stopped after " + budgetMs + " ms to protect other users - try again in a moment";
        return new QueryResult(Kind.OVERLOADED, message, message, null, -1);
    }

    private static final int    MAX_LABEL_LENGTH = 50;
    // the emoji form of the warning sign (with variation selector 16); the bare sign renders as a text glyph
    private static final String WARNING          = "\u26a0\ufe0f ";
    private static final String CROSS            = "\u274c ";

    /** Short label used for the end-of-line inlay hint; capped so long results don't blow out the line. */
    public String inlineLabel() {
        return switch (kind) {
            case RESULT -> cap(inferred);
            case MATCH -> "✅";
            case MISMATCH, FAILED -> cap(CROSS + inferred);
            case ERROR -> cap(WARNING + inferred);
            case OVERLOADED -> WARNING + "server busy";
            case CALCULATING -> "...calculating";
        };
    }

    /**
     * The inlay-hint tooltip: what the label does not show. That is the time of the inference, and the full result
     * or message only when the label cut it short (a match shows its result in the query's expected result, an
     * overload only "server busy"); a mismatch always shows what was expected and found, its label only the latter.
     * Empty when there is nothing to add.
     */
    public String tooltip() {
        if (kind == Kind.MISMATCH) {
            return message + timing();
        }
        String  full  = switch (kind) {
            case RESULT, MISMATCH, MATCH -> inferred;
            case ERROR, OVERLOADED -> WARNING + inferred;
            case FAILED -> CROSS + inferred;
            case CALCULATING -> "";
        };
        boolean shown = switch (kind) {
            case RESULT -> inferred.length() <= MAX_LABEL_LENGTH;
            case MATCH -> true;
            case MISMATCH -> (CROSS + inferred).length() <= MAX_LABEL_LENGTH;
            case ERROR, FAILED -> full.length() <= MAX_LABEL_LENGTH;
            case OVERLOADED -> false;
            case CALCULATING -> true;
        };
        if (!shown) {
            return full + timing();
        }
        return micros < 0 ? "" : time(micros);
    }

    /**
     * The hover at the end of the query, which the website editor shows for a result instead of its tooltip (the
     * same hover around the result and on it): the tooltip, but for a mismatch or an overload not what their problem
     * marker already says, and how to write the result into the query.
     */
    public String hover() {
        String text = switch (kind) {
            case MISMATCH -> micros < 0 ? "" : time(micros);
            case OVERLOADED, CALCULATING -> "";
            default -> tooltip();
        };
        if (kind == Kind.RESULT || kind == Kind.MISMATCH) {
            text += (text.isEmpty() ? "" : "\n\n") + "Double-click the result to make it the expected result";
        }
        return text;
    }

    /** A time of inference, for example {@code "1,234 \u03bcs"} (Greek mu). */
    public static String time(long micros) {
        return String.format(Locale.ROOT, "%,d \u03bcs", micros);
    }

    /** The time of the inference as a suffix, for example {@code "  (1,234 \u03bcs)"} (Greek mu); empty without one. */
    public String timing() {
        return micros < 0 ? "" : "  (" + time(micros) + ")";
    }

    private static String cap(String s) {
        return s.length() <= MAX_LABEL_LENGTH ? s : s.substring(0, MAX_LABEL_LENGTH - 3) + "...";
    }
}
