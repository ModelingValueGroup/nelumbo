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

/** Skeleton for a new self-contained decision model; must always evaluate cleanly (see test). */
public final class ModelSkeleton {

    private ModelSkeleton() {
    }

    public static String skeleton(String title) {
        String t = title == null || title.isBlank() ? "Decision model" : title.strip();
        return """
                // %s
                // Skeleton decision model. Replace each section with your domain, then
                // verify with eval_nl until ok=true. Keep the queries: they are the tests.

                import  nelumbo.integers

                // -- Types: domain concepts, rooted in Object -------------------------
                Person   :: Object

                // -- Fact types: the case data asserted as facts below ----------------
                FactType ::= age(<Person>,<Integer>)

                // -- Decisions: Boolean functors derived by rules ---------------------
                Boolean  ::= eligible(<Person>)

                // -- Variables used in rules and queries ------------------------------
                Person  p
                Integer n

                // -- Rules: <=> defines when a decision holds -------------------------
                eligible(p) <=> E[n](age(p,n) & n>=18)

                // -- Instances and facts: concrete cases ------------------------------
                Person ::= Alice, Bob

                fact age(Alice, 34),
                     age(Bob, 15)

                // -- Queries with expected results: [()][] = true, [][()] = false, ----
                // -- [(x=V)][..] = exactly these true bindings; the [..] falsehoods --
                // -- side is open (no claim about what else is false) ----------------
                // -- Note: eligible(p) with free p yields [..][..] (no enumeration  --
                // -- over decisions); enumerate a fact type with one free arg, then  --
                // -- check the decision per case -------------------------------------
                eligible(Alice) ? [()][]
                eligible(Bob)   ? [][()]
                age(Alice,n)    ? [(n=34)][..]
                age(p,34)       ? [(p=Alice)][..]
                """.formatted(t);
    }
}
