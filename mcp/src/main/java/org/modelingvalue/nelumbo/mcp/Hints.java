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

package org.modelingvalue.nelumbo.mcp;

/**
 * Curated hints for known LLM failure modes when authoring .nl files. Matched on the
 * diagnostic message; ordered most-specific first. Grow this table as new failure
 * modes are observed in practice.
 */
public final class Hints {

    public record Hint(String text, String docRef) {
    }

    private Hints() {
    }

    public static Hint hintFor(String message) {
        if (message == null) {
            return null;
        }
        if (message.startsWith("Expected result ")) {
            return new Hint("The model parsed and ran, but this query's result differs from the expected result "
                    + "written after '?'. The form is [facts][falsehoods]: the first bracket lists bindings proven "
                    + "true, the second lists bindings proven false. A closed bracket (no '..') asserts exactly "
                    + "those bindings; a bracket containing '..' is open and asserts only a lower bound - those "
                    + "bindings must be present but more are permitted. Fix the rules/facts or correct the "
                    + "expectation.", "reference/test-expression-semantics.md");
        }
        // U.traceable() renders a literal newline as the two-char sequence \n in token text
        if (message.startsWith("Unexpected token '\\n'")) {
            return new Hint("Likely the known precedence gotcha: when a functor is declared on a Root subtype "
                    + "(X ::= ... where X extends Root), every alternative that contains repetition, optional or "
                    + "alternation patterns needs an explicit precedence suffix - append '#0' to each alternative "
                    + "of the ::= declaration. Later errors in the file usually cascade from this one.",
                    "reference/precedence-and-associativity.md");
        }
        if (message.startsWith("Unexpected token ")) {
            return new Hint("The parser expected one of the listed tokens at this position. Check the pattern "
                    + "(::=) declaration this statement should match, and compare with a working example via "
                    + "get_example.", "reference/grammar.md");
        }
        return null;
    }
}
