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

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** The bundled .nl corpus (from the core jar's resources), listed and served by name. */
public final class ExampleCatalog {

    public record Example(String name, String resourcePath, String description) {
    }

    private record Entry(String path, String description) {
    }

    private static final String BASE = "/org/modelingvalue/nelumbo/";

    private static final Map<String, Entry> ENTRIES = new LinkedHashMap<>();
    static {
        // examples/
        ENTRIES.put("family",                 new Entry(BASE + "examples/family.nl",                 "family-tree KB: fact types, functions via rules, E[] quantifier, queries with expected results"));
        ENTRIES.put("fibonacci",              new Entry(BASE + "examples/fibonacci.nl",              "recursive integer function: fib via conditional rules, queries up to fib(1000)"));
        ENTRIES.put("belasting",              new Entry(BASE + "examples/belasting.nl",              "tax decision model: natural-language FactType patterns and a Boolean decision rule (Dutch)"));
        ENTRIES.put("deHet",                  new Entry(BASE + "examples/deHet.nl",                  "DSL macro: Root-extending transform that generates functor, fact, and query patterns from a declaration (Dutch)"));
        ENTRIES.put("whoIs",                  new Entry(BASE + "examples/whoIs.nl",                  "natural-language Boolean and functor syntax layered on top of the friends KB via import"));
        ENTRIES.put("friends",                new Entry(BASE + "examples/friends.nl",                "transitive friend relation: FactType facts, recursive functor rule, queries with expected results"));
        ENTRIES.put("even",                   new Entry(BASE + "examples/even.nl",                   "Boolean predicate using E[] and integer division to test evenness"));
        ENTRIES.put("max",                    new Entry(BASE + "examples/max.nl",                    "ternary operator defined as a Root functor, then max(x,y) built on top of it"));
        ENTRIES.put("maxFib",                 new Entry(BASE + "examples/maxFib.nl",                 "composed recursion: fib and maxFib helper finding the largest Fibonacci number below a bound"));
        ENTRIES.put("power",                  new Entry(BASE + "examples/power.nl",                  "rational exponentiation via recursive rule with base case x**0=1 and inductive step"));
        ENTRIES.put("ternary",                new Entry(BASE + "examples/ternary.nl",                "generic ternary operator (b?t:f) declared as a Type-parametric functor, used with strings"));
        ENTRIES.put("scoping",                new Entry(BASE + "examples/scoping.nl",                "private functor scope blocks: two independent namespaces each with a private XXX constant"));
        ENTRIES.put("koningsdag",             new Entry(BASE + "examples/koningsdag.nl",             "calendar decision rule: Dutch King's Day date shifts from Apr-27 to Apr-26 if it falls on Sunday"));
        ENTRIES.put("clubFees",               new Entry(BASE + "examples/clubFees.nl",               "membership-fee decision model: conditional rule branches on age facts (base fee 100, half under 18 or over 65); authored via the MCP tools"));
        ENTRIES.put("hidden",                 new Entry(BASE + "examples/hidden.nl",                 "hidden variable feature: unnamed variable in queries and in operator functors"));
        ENTRIES.put("queryOnly",              new Entry(BASE + "examples/queryOnly.nl",              "query-only file: exercises integer arithmetic, abs, E[], A[], negation without any custom types"));
        ENTRIES.put("transformation",         new Entry(BASE + "examples/transformation.nl",         "transform statement (::>): Root functor generates sub-functors, facts, and rules at load time"));
        ENTRIES.put("familyAssignment",       new Entry(BASE + "examples/familyAssignment.nl",       "exercise template for family.nl: type/fact scaffolding present, rules left for the student"));
        ENTRIES.put("ternaryAssignment",      new Entry(BASE + "examples/ternaryAssignment.nl",      "exercise template for ternary.nl: functor declaration present, rule body left for the student"));
        ENTRIES.put("evenAssignment",         new Entry(BASE + "examples/evenAssignment.nl",         "exercise template for even.nl: Boolean functor declared, rule body left for the student"));
        ENTRIES.put("maxAssignment",          new Entry(BASE + "examples/maxAssignment.nl",          "exercise template for max.nl: ternary operator and max functor declared, rules left for the student"));
        ENTRIES.put("maxFibAssignment",       new Entry(BASE + "examples/maxFibAssignment.nl",       "exercise template for maxFib.nl: fib and maxFib functors declared, rules left for the student"));
        ENTRIES.put("powerAssignment",        new Entry(BASE + "examples/powerAssignment.nl",        "exercise template for power.nl: ** functor declared, rule body left for the student"));
        ENTRIES.put("transformationAssignment", new Entry(BASE + "examples/transformationAssignment.nl", "exercise template for transformation.nl: DSL macro scaffolding present, rule and transform body left for the student"));
        // tests/
        ENTRIES.put("langOnly",              new Entry(BASE + "tests/langOnly.nl",              "bootstrap smoke test: exercises every Pattern subtype using only nelumbo.lang (no logic, no facts)"));
        ENTRIES.put("logicTest",             new Entry(BASE + "tests/logicTest.nl",             "propositional and predicate logic truth-table tests: true/false/unknown, &/|/!/->/<->, E[], A[], =, !="));
        // stdlib
        ENTRIES.put("lang",                  new Entry(BASE + "lang/lang.nl",                  "stdlib bootstrap: NATIVE token types, Object/Root/Type hierarchy, Pattern subtypes, import/functor/variable syntax"));
        ENTRIES.put("logic",                 new Entry(BASE + "logic/logic.nl",                "stdlib logic layer: Boolean, FactType, true/false/unknown, &/|/!/->/<->, E[]/A[], fact/rule/query syntax"));
        ENTRIES.put("integers",              new Entry(BASE + "integers/integers.nl",           "stdlib integers: Integer type, arithmetic (+/-/*//) and comparison operators, abs via conditional rules"));
        ENTRIES.put("strings",               new Entry(BASE + "strings/strings.nl",            "stdlib strings: String type, concatenation (+), length (len), int<->string conversion (int/str)"));
        ENTRIES.put("collections",           new Entry(BASE + "collections/collections.nl",    "stdlib collections: generic Set<E>/List<E>, set ops (&&/||/-/in/where/subset), list concat/map/filter, size"));
        ENTRIES.put("rationals",             new Entry(BASE + "rationals/rationals.nl",        "stdlib rationals: Rational type, arithmetic/comparison operators, r(x) and r(x/y) constructor rules"));
        ENTRIES.put("datetime",              new Entry(BASE + "datetime/datetime.nl",          "stdlib datetime: DateTime/Date/Time/Period types, ISO-8601 literals, arithmetic and comparison operators"));
    }

    private ExampleCatalog() {
    }

    public static List<Example> list() {
        List<Example> result = new ArrayList<>();
        ENTRIES.forEach((name, entry) -> result.add(new Example(name, entry.path(), entry.description())));
        return result;
    }

    /** @return the full source of the named example, or null if unknown. */
    public static String content(String name) {
        Entry entry = ENTRIES.get(name);
        return entry == null ? null : read(entry.path());
    }

    private static String read(String path) {
        try (InputStream in = ExampleCatalog.class.getResourceAsStream(path)) {
            return in == null ? null : new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            return null;
        }
    }
}
