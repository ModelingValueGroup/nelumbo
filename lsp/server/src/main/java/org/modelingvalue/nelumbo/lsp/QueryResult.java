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

package org.modelingvalue.nelumbo.lsp;

import org.eclipse.lsp4j.Range;

/**
 * The outcome of evaluating a single {@code Query}.
 * <ul>
 *   <li>{@code RESULT}   — query without an expected clause; {@code inferred} is the inference result.</li>
 *   <li>{@code MATCH}    — query whose expected clause matches; rendered as just a checkmark.</li>
 *   <li>{@code MISMATCH} — query whose expected clause differs; {@code inferred} is the calculated result
 *       (shown inline), {@code message} describes the difference, {@code expectedRange} is the source span of
 *       the expected clause to underline.</li>
 *   <li>{@code ERROR}    — evaluation failed; {@code inferred} holds the error message.</li>
 * </ul>
 */
public record QueryResult(Kind kind, String inferred, String message, Range expectedRange) {

    public enum Kind {
        RESULT,
        MATCH,
        MISMATCH,
        ERROR,
    }

    public static QueryResult result(String inferred) {
        return new QueryResult(Kind.RESULT, inferred, null, null);
    }

    public static QueryResult match(String inferred) {
        return new QueryResult(Kind.MATCH, inferred, null, null);
    }

    public static QueryResult mismatch(String inferred, String message, Range expectedRange) {
        return new QueryResult(Kind.MISMATCH, inferred, message, expectedRange);
    }

    public static QueryResult error(String message) {
        return new QueryResult(Kind.ERROR, message, message, null);
    }

    /** Short label used for the end-of-line inlay hint. */
    public String inlineLabel() {
        return switch (kind) {
            case RESULT, MISMATCH -> inferred;
            case MATCH -> "✓";
            case ERROR -> "⚠ " + inferred;
        };
    }
}
