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

package org.modelingvalue.nelumbo.browser;

/**
 * Force-links every {@code @NelumboConstructor}/{@code @NelumboMethod}/{@code @NelumboFunctorField}
 * bound stdlib class into the TeaVM output: the classes are only reached via {@code Class.forName}
 * at runtime, so without these literals (and the reflective touches in {@link #link()}) TeaVM would
 * dead-code-eliminate them. {@link NelumboReflectionSupplier} exposes their members to reflection
 * at compile time from the same {@link #CLASSES} array.
 */
public final class ReflectionKeep {

    /** Every class in core carrying one of the Nelumbo binding annotations. */
    public static final Class<?>[] CLASSES = {
            org.modelingvalue.nelumbo.Node.class,
            org.modelingvalue.nelumbo.collections.BuildSet.class,
            org.modelingvalue.nelumbo.collections.Collections.class,
            org.modelingvalue.nelumbo.collections.NList.class,
            org.modelingvalue.nelumbo.collections.NSet.class,
            org.modelingvalue.nelumbo.datetime.Add.class,
            org.modelingvalue.nelumbo.datetime.GreaterThan.class,
            org.modelingvalue.nelumbo.datetime.Multiply.class,
            org.modelingvalue.nelumbo.datetime.NDate.class,
            org.modelingvalue.nelumbo.datetime.NDateTime.class,
            org.modelingvalue.nelumbo.datetime.NPeriod.class,
            org.modelingvalue.nelumbo.datetime.NTime.class,
            org.modelingvalue.nelumbo.integers.Integers.class,
            org.modelingvalue.nelumbo.integers.NInteger.class,
            org.modelingvalue.nelumbo.lang.Functor.class,
            org.modelingvalue.nelumbo.lang.Import.class,
            org.modelingvalue.nelumbo.lang.Lambda.class,
            org.modelingvalue.nelumbo.lang.Namespace.class,
            org.modelingvalue.nelumbo.lang.Parenthesized.class,
            org.modelingvalue.nelumbo.lang.PatternPart.class,
            org.modelingvalue.nelumbo.lang.Transform.class,
            org.modelingvalue.nelumbo.lang.Type.class,
            org.modelingvalue.nelumbo.lang.Variable.class,
            org.modelingvalue.nelumbo.logic.And.class,
            org.modelingvalue.nelumbo.logic.BooleanVariable.class,
            org.modelingvalue.nelumbo.logic.Equal.class,
            org.modelingvalue.nelumbo.logic.ExistentialQuantifier.class,
            org.modelingvalue.nelumbo.logic.Fact.class,
            org.modelingvalue.nelumbo.logic.NBoolean.class,
            org.modelingvalue.nelumbo.logic.NIs.class,
            org.modelingvalue.nelumbo.logic.Not.class,
            org.modelingvalue.nelumbo.logic.Or.class,
            org.modelingvalue.nelumbo.logic.Predicate.class,
            org.modelingvalue.nelumbo.logic.Query.class,
            org.modelingvalue.nelumbo.logic.Rule.class,
            org.modelingvalue.nelumbo.logic.UniversalQuantifier.class,
            org.modelingvalue.nelumbo.logic.When.class,
            org.modelingvalue.nelumbo.patterns.AlternationPattern.class,
            org.modelingvalue.nelumbo.patterns.NodeTypePattern.class,
            org.modelingvalue.nelumbo.patterns.OptionalPattern.class,
            org.modelingvalue.nelumbo.patterns.PatternPartPattern.class,
            org.modelingvalue.nelumbo.patterns.RepetitionPattern.class,
            org.modelingvalue.nelumbo.patterns.SequencePattern.class,
            org.modelingvalue.nelumbo.patterns.TokenTextPattern.class,
            org.modelingvalue.nelumbo.patterns.TokenTypePattern.class,
            org.modelingvalue.nelumbo.rationals.Rational.class,
            org.modelingvalue.nelumbo.rationals.Rationals.class,
            org.modelingvalue.nelumbo.strings.NString.class,
            org.modelingvalue.nelumbo.strings.Strings.class,
    };

    private ReflectionKeep() {
    }

    /** Touch the reflective member lists so TeaVM links reflection data for every kept class. */
    @SuppressWarnings("ResultOfMethodCallIgnored")
    public static void link() {
        for (Class<?> cls : CLASSES) {
            cls.getConstructors();
            cls.getDeclaredMethods();
            cls.getDeclaredFields();
        }
    }
}
