//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
//  (C) Copyright 2018-2025 Modeling Value Group B.V. (http://modelingvalue.org)                                         ~
//                                                                                                                       ~
//  Licensed under the GNU Lesser General Public License v3.0 (the 'License'). You may not use this file except in       ~
//  compliance with the License. You may obtain a copy of the License at: https://choosealicense.com/licenses/lgpl-3.0   ~
//  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on  ~
//  an 'AS IS' BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the   ~
//  specific language governing permissions and limitations under the License.                                           ~
//                                                                                                                       ~
//  Maintainers:                                                                                                         ~
//      Wim Bast, Tom Brus                                                                                               ~
//                                                                                                                       ~
//  Contributors:                                                                                                        ~
//      Ronald Krijgsheld ✝, Arjan Kok, Carel Bast                                                                       ~
// --------------------------------------------------------------------------------------------------------------------- ~
//  In Memory of Ronald Krijgsheld, 1972 - 2023                                                                          ~
//      Ronald was suddenly and unexpectedly taken from us. He was not only our long-term colleague and team member      ~
//      but also our friend. "He will live on in many of the lines of code you see below."                               ~
//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

package org.modelingvalue.nelumbo;

import static org.modelingvalue.nelumbo.Logic.*;

import java.math.BigInteger;

import org.modelingvalue.collections.Set;
import org.modelingvalue.nelumbo.Logic.Constant;
import org.modelingvalue.nelumbo.Logic.Function;
import org.modelingvalue.nelumbo.Logic.Functor;
import org.modelingvalue.nelumbo.Logic.Relation;
import org.modelingvalue.nelumbo.Logic.Structure;
import org.modelingvalue.nelumbo.impl.InferContext;
import org.modelingvalue.nelumbo.impl.InferResult;
import org.modelingvalue.nelumbo.impl.RelationImpl;
import org.modelingvalue.nelumbo.impl.StructureImpl;

public final class Integers {

    private Integers() {
    }

    // Types

    public interface Integer extends Structure {
    }

    public interface IntegerCons extends Integer, Constant<Integer> {
    }

    public interface IntegerFunc extends Integer, Function<Integer> {
    }

    // Constants

    private static Functor<IntegerCons> I_FUNCTOR = Logic.<IntegerCons, BigInteger> functor(Integers::i, //
            render(s -> s.toString(1)));

    public static IntegerCons i(BigInteger val) {
        return constant(I_FUNCTOR, val);
    }

    public static IntegerCons i(String val, int radix) {
        return i(new BigInteger(val, radix));
    }

    public static IntegerCons i(long val) {
        return i(BigInteger.valueOf(val));
    }

    public static IntegerCons iConsVar(String name) {
        return variable(IntegerCons.class, name);
    }

    public static Integer iVar(String name) {
        return variable(Integer.class, name);
    }

    // Relations

    private static final StructureImpl<IntegerCons> ZERO_INT = StructureImpl.unproxy(i(0));

    private static StructureImpl<IntegerCons> struct(BigInteger i) {
        return ZERO_INT.set(1, i);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Functor<Relation> GT_CONS_FUNCTOR = Logic.<Relation, IntegerCons, IntegerCons> functor(Integers::gtc, logic(Integers::gtcLogic), //
            render(s -> s.toString(1) + "\u226B" + s.toString(2)));

    @SuppressWarnings("rawtypes")
    private static InferResult gtcLogic(RelationImpl relation, InferContext context) {
        BigInteger compared1 = relation.getVal(1, 1);
        BigInteger compared2 = relation.getVal(2, 1);
        if (compared1 != null && compared2 != null) {
            return compared1.compareTo(compared2) > 0 ? relation.factCC() : relation.falsehoodCC();
        } else {
            return relation.unknown();
        }
    }

    private static Relation gtc(IntegerCons compared1, IntegerCons compared2) {
        return relation(GT_CONS_FUNCTOR, compared1, compared2);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Functor<Relation> PLUS_REL_FUNCTOR = Logic.<Relation, IntegerCons, IntegerCons, IntegerCons> functor(Integers::plus, logic(Integers::plusLogic), //
            render(s -> s.toString(1) + "+" + s.toString(2) + "\u2261" + s.toString(3)));

    private static InferResult plusLogic(RelationImpl relation, InferContext context) {
        BigInteger addend1 = relation.getVal(1, 1);
        BigInteger addend2 = relation.getVal(2, 1);
        BigInteger sum = relation.getVal(3, 1);
        if (addend1 != null && addend2 != null) {
            BigInteger s = addend1.add(addend2);
            if (sum != null) {
                boolean eq = s.equals(sum);
                return eq ? relation.factCC() : relation.falsehoodCC();
            } else {
                return relation.set(3, struct(s)).factCI();
            }
        } else if (addend1 != null && sum != null) {
            return relation.set(2, struct(sum.subtract(addend1))).factCI();
        } else if (addend2 != null && sum != null) {
            return relation.set(1, struct(sum.subtract(addend2))).factCI();
        } else {
            return relation.unknown();
        }
    }

    public static Relation plus(IntegerCons addend1, IntegerCons addend2, IntegerCons sum) {
        return relation(PLUS_REL_FUNCTOR, addend1, addend2, sum);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Functor<Relation> MULTIPLY_REL_FUNCTOR = Logic.<Relation, IntegerCons, IntegerCons, IntegerCons> functor(Integers::multiply, logic(Integers::multiplyLogic), //
            render(s -> s.toString(1) + "\u00B7" + s.toString(2) + "\u2261" + s.toString(3)));

    private static InferResult multiplyLogic(RelationImpl relation, InferContext context) {
        BigInteger factor1 = relation.getVal(1, 1);
        BigInteger factor2 = relation.getVal(2, 1);
        BigInteger product = relation.getVal(3, 1);
        if (factor1 != null && factor2 != null) {
            BigInteger p = factor1.multiply(factor2);
            if (product != null) {
                boolean eq = p.equals(product);
                return eq ? relation.factCC() : relation.falsehoodCC();
            } else {
                return relation.set(3, struct(p)).factCI();
            }
        } else if (factor1 != null && product != null) {
            return relation.set(2, struct(product.divide(factor1))).factCI();
        } else if (factor2 != null && product != null) {
            return relation.set(1, struct(product.divide(factor2))).factCI();
        } else {
            return relation.unknown();
        }
    }

    public static Relation multiply(IntegerCons factor1, IntegerCons factor2, IntegerCons product) {
        return relation(MULTIPLY_REL_FUNCTOR, factor1, factor2, product);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Functor<Relation> SQUARE_REL_FUNCTOR = Logic.<Relation, IntegerCons, IntegerCons> functor(Integers::square, logic(Integers::squareLogic));

    private static InferResult squareLogic(RelationImpl relation, InferContext context) {
        BigInteger root = relation.getVal(1, 1);
        BigInteger square = relation.getVal(2, 1);
        if (root != null && square != null) {
            boolean eq = root.multiply(root).equals(square);
            return eq ? relation.factCC() : relation.falsehoodCC();
        } else if (root != null && square == null) {
            return relation.set(2, struct(root.multiply(root))).factCI();
        } else if (root == null && square != null) {
            BigInteger sqrt = square.sqrt();
            return InferResult.factsCI(Set.of(relation.set(1, struct(sqrt)), relation.set(1, struct(sqrt.negate()))));
        } else {
            return relation.unknown();
        }
    }

    public static Relation square(IntegerCons root, IntegerCons square) {
        return relation(SQUARE_REL_FUNCTOR, root, square);
    }

    // Functions

    private static Functor<Relation> GT_FUNCTOR = functor(Integers::gt, //
            render(s -> s.toString(1) + ">" + s.toString(2)));

    public static Relation gt(Integer a, Integer b) {
        return relation(GT_FUNCTOR, a, b);
    }

    private static Functor<Relation> LT_FUNCTOR = functor(Integers::lt, //
            render(s -> s.toString(1) + "<" + s.toString(2)));

    public static Relation lt(Integer a, Integer b) {
        return relation(LT_FUNCTOR, a, b);
    }

    private static Functor<Relation> GE_FUNCTOR = functor(Integers::ge, //
            render(s -> s.toString(1) + "\u2265" + s.toString(2)));

    public static Relation ge(Integer a, Integer b) {
        return relation(GE_FUNCTOR, a, b);
    }

    private static Functor<Relation> LE_FUNCTOR = functor(Integers::le, //
            render(s -> s.toString(1) + "\u2264" + s.toString(2)));

    public static Relation le(Integer a, Integer b) {
        return relation(LE_FUNCTOR, a, b);
    }

    private static Functor<IntegerFunc> PLUS_FUNC_FUNCTOR = Logic.<IntegerFunc, Integer, Integer> functor(Integers::plus, //
            render(s -> s.toString(1) + "+" + s.toString(2)));

    public static IntegerFunc plus(Integer a, Integer b) {
        return function(PLUS_FUNC_FUNCTOR, a, b);
    }

    private static Functor<IntegerFunc> MINUS_FUNC_FUNCTOR = Logic.<IntegerFunc, Integer, Integer> functor(Integers::minus, //
            render(s -> s.toString(1) + "-" + s.toString(2)));

    public static IntegerFunc minus(Integer a, Integer b) {
        return function(MINUS_FUNC_FUNCTOR, a, b);
    }

    private static Functor<IntegerFunc> MULTIPLY_FUNC_FUNCTOR = Logic.<IntegerFunc, Integer, Integer> functor(Integers::multiply, //
            render(s -> s.toString(1) + "\u00B7" + s.toString(2)));

    public static IntegerFunc multiply(Integer a, Integer b) {
        return function(MULTIPLY_FUNC_FUNCTOR, a, b);
    }

    private static Functor<IntegerFunc> DIVIDE_FUNC_FUNCTOR = Logic.<IntegerFunc, Integer, Integer> functor(Integers::divide, //
            render(s -> s.toString(1) + "\u00F7" + s.toString(2)));

    public static IntegerFunc divide(Integer a, Integer b) {
        return function(DIVIDE_FUNC_FUNCTOR, a, b);
    }

    private static Functor<IntegerFunc> SQUARE_FUNC_FUNCTOR = Logic.<IntegerFunc, Integer> functor(Integers::square);

    public static IntegerFunc square(Integer a) {
        return function(SQUARE_FUNC_FUNCTOR, a);
    }

    private static Functor<IntegerFunc> SQRT_FUNC_FUNCTOR = Logic.<IntegerFunc, Integer> functor(Integers::sqrt);

    public static IntegerFunc sqrt(Integer a) {
        return function(SQRT_FUNC_FUNCTOR, a);
    }

    // Rules

    private static final IntegerCons P = iConsVar("P");
    private static final IntegerCons Q = iConsVar("Q");
    private static final IntegerCons R = iConsVar("R");

    private static final Integer     X = iVar("X");
    private static final Integer     Y = iVar("Y");
    private static final Integer     Z = iVar("Z");

    public static void integerRules() {
        isRules();

        rule(gt(X, Y), and(eq(X, P), eq(Y, Q), gtc(P, Q)));
        rule(lt(X, Y), and(eq(X, P), eq(Y, Q), gtc(Q, P)));
        rule(ge(X, Y), and(eq(X, P), eq(Y, Q), or(gtc(P, Q), eq(P, Q))));
        rule(le(X, Y), and(eq(X, P), eq(Y, Q), or(gtc(Q, P), eq(Q, P))));

        rule(eq(plus(X, Y), Z), and(eq(X, P), eq(Y, Q), eq(Z, R), plus(P, Q, R)));
        rule(eq(minus(X, Y), Z), and(eq(X, P), eq(Y, Q), eq(Z, R), plus(R, Q, P)));
        rule(eq(multiply(X, Y), Z), and(eq(X, P), eq(Y, Q), eq(Z, R), multiply(P, Q, R)));
        rule(eq(divide(X, Y), Z), and(eq(X, P), eq(Y, Q), eq(Z, R), multiply(R, Q, P)));
        rule(eq(square(X), Z), and(eq(X, P), eq(Z, R), square(P, R)));
        rule(eq(sqrt(X), Z), and(eq(X, P), eq(Z, R), square(R, P)));
    }

}
