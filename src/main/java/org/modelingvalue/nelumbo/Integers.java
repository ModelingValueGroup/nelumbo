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
import org.modelingvalue.nelumbo.Logic.LogicLambda;
import org.modelingvalue.nelumbo.Logic.Relation;
import org.modelingvalue.nelumbo.Logic.Structure;
import org.modelingvalue.nelumbo.Logic.ToStringLambda;
import org.modelingvalue.nelumbo.impl.FunctorImpl;
import org.modelingvalue.nelumbo.impl.InferContext;
import org.modelingvalue.nelumbo.impl.InferResult;
import org.modelingvalue.nelumbo.impl.PredicateImpl;
import org.modelingvalue.nelumbo.impl.StructureImpl;

public final class Integers {

    private Integers() {
    }

    public interface Integer extends Structure {
    }

    public interface IntegerCons extends Integer, Constant<Integer> {
    }

    public interface IntegerFunc extends Integer, Function<Integer> {
    }

    private static FunctorImpl<IntegerCons> I_FUNCTOR_IMPL = FunctorImpl.<IntegerCons, BigInteger> of(Integers::i,   //
            (ToStringLambda) s -> s.toString(1));
    private static Functor<IntegerCons>     I_FUNCTOR      = I_FUNCTOR_IMPL.proxy();

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

    // Predicates

    private static final StructureImpl<IntegerCons> ZERO_INT      = StructureImpl.unproxy(i(0));
    private static final StructureImpl<IntegerCons> ONE_INT       = StructureImpl.unproxy(i(1));
    private static final StructureImpl<IntegerCons> MINUS_ONE_INT = StructureImpl.unproxy(i(-1));

    private static StructureImpl<IntegerCons> struct(BigInteger i) {
        return ZERO_INT.set(1, i);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Functor<Relation> COMPARE_FUNCTOR = Logic.<Relation, IntegerCons, IntegerCons, IntegerCons> functor(Integers::compare, (LogicLambda) Integers::compareLogic);

    @SuppressWarnings("rawtypes")
    private static InferResult compareLogic(PredicateImpl predicate, InferContext context) {
        BigInteger compared1 = predicate.getVal(1, 1);
        BigInteger compared2 = predicate.getVal(2, 1);
        BigInteger result = predicate.getVal(3, 1);
        if (compared1 != null && compared2 != null) {
            int r = compared1.compareTo(compared2);
            if (result != null) {
                boolean eq = r == result.intValue();
                return InferResult.trueFalse(eq ? predicate.singleton() : Set.of(), eq ? Set.of() : predicate.singleton());
            } else {
                return InferResult.trueFalse(predicate.set(3, r == 0 ? ZERO_INT : r == 1 ? ONE_INT : MINUS_ONE_INT).singleton(), predicate.singleton());
            }
        } else if (BigInteger.ZERO.equals(result)) {
            if (compared1 != null) {
                return InferResult.trueFalse(predicate.set(2, (StructureImpl) predicate.getVal(1)).singleton(), predicate.singleton());
            } else if (compared2 != null) {
                return InferResult.trueFalse(predicate.set(1, (StructureImpl) predicate.getVal(2)).singleton(), predicate.singleton());
            }
        }
        return predicate.incomplete();
    }

    public static Relation compare(IntegerCons compared1, IntegerCons compared2, IntegerCons result) {
        return pred(COMPARE_FUNCTOR, compared1, compared2, result);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Functor<Relation> PLUS_PRED_FUNCTOR = Logic.<Relation, IntegerCons, IntegerCons, IntegerCons> functor(Integers::plus, (LogicLambda) Integers::plusLogic, //
            (ToStringLambda) s -> s.toString(1) + "+" + s.toString(2) + "=" + s.toString(3));

    private static InferResult plusLogic(PredicateImpl predicate, InferContext context) {
        BigInteger addend1 = predicate.getVal(1, 1);
        BigInteger addend2 = predicate.getVal(2, 1);
        BigInteger sum = predicate.getVal(3, 1);
        if (addend1 != null && addend2 != null) {
            BigInteger s = addend1.add(addend2);
            if (sum != null) {
                boolean eq = s.equals(sum);
                return InferResult.trueFalse(eq ? predicate.singleton() : Set.of(), eq ? Set.of() : predicate.singleton());
            } else {
                return InferResult.trueFalse(predicate.set(3, struct(s)).singleton(), predicate.singleton());
            }
        } else if (addend1 != null && sum != null) {
            return InferResult.trueFalse(predicate.set(2, struct(sum.subtract(addend1))).singleton(), predicate.singleton());
        } else if (addend2 != null && sum != null) {
            return InferResult.trueFalse(predicate.set(1, struct(sum.subtract(addend2))).singleton(), predicate.singleton());
        } else {
            return predicate.incomplete();
        }
    }

    public static Relation plus(IntegerCons addend1, IntegerCons addend2, IntegerCons sum) {
        return pred(PLUS_PRED_FUNCTOR, addend1, addend2, sum);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Functor<Relation> MULTIPLY_PRED_FUNCTOR = Logic.<Relation, IntegerCons, IntegerCons, IntegerCons> functor(Integers::multiply, (LogicLambda) Integers::multiplyLogic, //
            (ToStringLambda) s -> s.toString(1) + "\u00B7" + s.toString(2) + "=" + s.toString(3));

    private static InferResult multiplyLogic(PredicateImpl predicate, InferContext context) {
        BigInteger factor1 = predicate.getVal(1, 1);
        BigInteger factor2 = predicate.getVal(2, 1);
        BigInteger product = predicate.getVal(3, 1);
        if (factor1 != null && factor2 != null) {
            BigInteger p = factor1.multiply(factor2);
            if (product != null) {
                boolean eq = p.equals(product);
                return InferResult.trueFalse(eq ? predicate.singleton() : Set.of(), eq ? Set.of() : predicate.singleton());
            } else {
                return InferResult.trueFalse(predicate.set(3, struct(p)).singleton(), predicate.singleton());
            }
        } else if (factor1 != null && product != null) {
            return InferResult.trueFalse(predicate.set(2, struct(product.divide(factor1))).singleton(), predicate.singleton());
        } else if (factor2 != null && product != null) {
            return InferResult.trueFalse(predicate.set(1, struct(product.divide(factor2))).singleton(), predicate.singleton());
        } else {
            return predicate.incomplete();
        }
    }

    public static Relation multiply(IntegerCons factor1, IntegerCons factor2, IntegerCons product) {
        return pred(MULTIPLY_PRED_FUNCTOR, factor1, factor2, product);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Functor<Relation> SQUARE_PRED_FUNCTOR = Logic.<Relation, IntegerCons, IntegerCons> functor(Integers::square, (LogicLambda) Integers::squareLogic);

    private static InferResult squareLogic(PredicateImpl predicate, InferContext context) {
        BigInteger root = predicate.getVal(1, 1);
        BigInteger square = predicate.getVal(2, 1);
        if (root != null && square != null) {
            boolean eq = root.multiply(root).equals(square);
            return InferResult.trueFalse(eq ? predicate.singleton() : Set.of(), eq ? Set.of() : predicate.singleton());
        } else if (root != null && square == null) {
            return InferResult.trueFalse(predicate.set(2, struct(root.multiply(root))).singleton(), predicate.singleton());
        } else if (root == null && square != null) {
            BigInteger sqrt = square.sqrt();
            return InferResult.trueFalse(Set.of(predicate.set(1, struct(sqrt)), predicate.set(1, struct(sqrt.negate()))), predicate.singleton());
        } else {
            return predicate.incomplete();
        }
    }

    public static Relation square(IntegerCons root, IntegerCons square) {
        return pred(SQUARE_PRED_FUNCTOR, root, square);
    }

    // Functions

    private static Functor<Relation> GT_FUNCTOR = functor(Integers::gt, //
            (ToStringLambda) s -> s.toString(1) + ">" + s.toString(2));

    public static Relation gt(Integer a, Integer b) {
        return pred(GT_FUNCTOR, a, b);
    }

    private static Functor<Relation> LT_FUNCTOR = functor(Integers::lt, //
            (ToStringLambda) s -> s.toString(1) + "<" + s.toString(2));

    public static Relation lt(Integer a, Integer b) {
        return pred(LT_FUNCTOR, a, b);
    }

    private static Functor<Relation> GE_FUNCTOR = functor(Integers::ge, //
            (ToStringLambda) s -> s.toString(1) + "\u2265" + s.toString(2));

    public static Relation ge(Integer a, Integer b) {
        return pred(GE_FUNCTOR, a, b);
    }

    private static Functor<Relation> LE_FUNCTOR = functor(Integers::le, //
            (ToStringLambda) s -> s.toString(1) + "\u2264" + s.toString(2));

    public static Relation le(Integer a, Integer b) {
        return pred(LE_FUNCTOR, a, b);
    }

    private static Functor<IntegerFunc> PLUS_FUNC_FUNCTOR = Logic.<IntegerFunc, Integer, Integer> functor(Integers::plus, //
            (ToStringLambda) s -> s.toString(1) + "+" + s.toString(2));

    public static IntegerFunc plus(Integer a, Integer b) {
        return function(PLUS_FUNC_FUNCTOR, a, b);
    }

    private static Functor<IntegerFunc> MINUS_FUNC_FUNCTOR = Logic.<IntegerFunc, Integer, Integer> functor(Integers::minus, //
            (ToStringLambda) s -> s.toString(1) + "-" + s.toString(2));

    public static IntegerFunc minus(Integer a, Integer b) {
        return function(MINUS_FUNC_FUNCTOR, a, b);
    }

    private static Functor<IntegerFunc> MULTIPLY_FUNC_FUNCTOR = Logic.<IntegerFunc, Integer, Integer> functor(Integers::multiply, //
            (ToStringLambda) s -> s.toString(1) + "\u00B7" + s.toString(2));

    public static IntegerFunc multiply(Integer a, Integer b) {
        return function(MULTIPLY_FUNC_FUNCTOR, a, b);
    }

    private static Functor<IntegerFunc> DIVIDE_FUNC_FUNCTOR = Logic.<IntegerFunc, Integer, Integer> functor(Integers::divide, //
            (ToStringLambda) s -> s.toString(1) + "/" + s.toString(2));

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

    private static final IntegerCons P = iConsVar("PL");
    private static final IntegerCons Q = iConsVar("QL");
    private static final IntegerCons R = iConsVar("RL");

    private static final Integer     X = iVar("X");
    private static final Integer     Y = iVar("Y");

    public static void integerRules() {
        isRules();

        rule(gt(X, Y), and(is(X, P), is(Y, Q), compare(P, Q, i(1))));
        rule(lt(X, Y), and(is(X, P), is(Y, Q), compare(P, Q, i(-1))));
        rule(ge(X, Y), and(is(X, P), is(Y, Q), compare(P, Q, R), or(eq(R, i(1)), eq(R, i(0)))));
        rule(le(X, Y), and(is(X, P), is(Y, Q), compare(P, Q, R), or(eq(R, i(-1)), eq(R, i(0)))));

        rule(is(plus(X, Y), R), and(is(X, P), is(Y, Q), plus(P, Q, R)));
        rule(is(minus(X, Y), R), and(is(X, P), is(Y, Q), plus(R, Q, P)));
        rule(is(multiply(X, Y), R), and(is(X, P), is(Y, Q), multiply(P, Q, R)));
        rule(is(divide(X, Y), R), and(is(X, P), is(Y, Q), multiply(R, Q, P)));
        rule(is(square(X), R), and(is(X, P), square(P, R)));
        rule(is(sqrt(X), R), and(is(X, P), square(R, P)));
    }

}
