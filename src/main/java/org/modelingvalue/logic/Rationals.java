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

package org.modelingvalue.logic;

import static org.modelingvalue.logic.Integers.i;
import static org.modelingvalue.logic.Integers.iConsVar;
import static org.modelingvalue.logic.Logic.*;

import java.math.BigInteger;

import org.modelingvalue.collections.Set;
import org.modelingvalue.logic.Integers.IntegerCons;
import org.modelingvalue.logic.Logic.*;
import org.modelingvalue.logic.impl.InferContext;
import org.modelingvalue.logic.impl.InferResult;
import org.modelingvalue.logic.impl.PredicateImpl;
import org.modelingvalue.logic.impl.StructureImpl;

public final class Rationals {

    private Rationals() {
    }

    public interface Rational extends Structure {
    }

    public interface RationalCons extends Rational, Constant<Rational> {
    }

    public interface RationalFunc extends Rational, Function<Rational> {
    }

    private static Functor<RationalCons> R_FUNCTOR = Logic.<RationalCons, BigInteger, BigInteger> functor(Rationals::r, (NormalizeLambda) r -> {
        BigInteger numerator = r.getVal(1);
        BigInteger denominator = r.getVal(2);
        BigInteger gcd = numerator.gcd(denominator);
        return r.set(1, numerator.divide(gcd), denominator.divide(gcd));
    });

    public static RationalCons r(BigInteger numerator, BigInteger denominator) {
        return constant(R_FUNCTOR, numerator, denominator);
    }

    public static RationalCons r(String numerator, String denominator, int radix) {
        return r(new BigInteger(numerator, radix), new BigInteger(denominator, radix));
    }

    public static RationalCons r(long numerator, long denominator) {
        return r(BigInteger.valueOf(numerator), BigInteger.valueOf(denominator));
    }

    public static RationalCons r(BigInteger numerator) {
        return r(numerator, BigInteger.ONE);
    }

    public static RationalCons r(String numerator, int radix) {
        return r(new BigInteger(numerator, radix));
    }

    public static RationalCons r(long numerator) {
        return r(BigInteger.valueOf(numerator));
    }

    public static RationalCons rConsVar(String name) {
        return variable(RationalCons.class, name);
    }

    public static Rational rVar(String name) {
        return variable(Rational.class, name);
    }

    // Predicates

    private static final StructureImpl<IntegerCons>  ZERO_INT      = StructureImpl.unproxy(i(0));
    private static final StructureImpl<IntegerCons>  ONE_INT       = StructureImpl.unproxy(i(1));
    private static final StructureImpl<IntegerCons>  MINUS_ONE_INT = StructureImpl.unproxy(i(-1));

    private static final StructureImpl<RationalCons> ZERO_RATIONAL = StructureImpl.unproxy(r(0));

    private static StructureImpl<RationalCons> struct(BigInteger numerator, BigInteger denominator) {
        return ZERO_RATIONAL.set(1, numerator, denominator);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Functor<Predicate> compare = Logic.<Predicate, RationalCons, RationalCons, IntegerCons> functor(Rationals::compare, (LogicLambda) Rationals::compareLogic);

    @SuppressWarnings("rawtypes")
    private static InferResult compareLogic(PredicateImpl predicate, InferContext context) {
        BigInteger numComp1 = predicate.getVal(1, 1);
        BigInteger denComp1 = predicate.getVal(1, 2);
        BigInteger numComp2 = predicate.getVal(2, 1);
        BigInteger denComp2 = predicate.getVal(2, 2);
        BigInteger result = predicate.getVal(3, 1);
        if (numComp1 != null && numComp2 != null) {
            int r = numComp1.multiply(denComp2).compareTo(numComp2.multiply(denComp1));
            if (result != null) {
                boolean eq = r == result.intValue();
                return InferResult.trueFalse(eq ? Set.of(predicate) : Set.of(), eq ? Set.of() : Set.of(predicate));
            } else {
                return InferResult.trueFalse(Set.of(predicate.set(3, r == 0 ? ZERO_INT : r == 1 ? ONE_INT : MINUS_ONE_INT)), Set.of());
            }
        } else if (result != null) {
            boolean zero = BigInteger.ZERO.equals(result);
            if (numComp1 != null) {
                Set<PredicateImpl> facts = Set.of(predicate.set(2, (StructureImpl) predicate.getVal(1)));
                return zero ? InferResult.trueFalse(facts, Set.of()) : InferResult.falseIncomplete(facts, predicate);
            } else if (numComp2 != null) {
                Set<PredicateImpl> facts = Set.of(predicate.set(1, (StructureImpl) predicate.getVal(2)));
                return zero ? InferResult.trueFalse(facts, Set.of()) : InferResult.falseIncomplete(facts, predicate);
            }
        }
        return InferResult.incomplete(predicate);
    }

    public static Predicate compare(RationalCons compared1, RationalCons compared2, IntegerCons result) {
        return pred(compare, compared1, compared2, result);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Functor<Predicate> PLUS_PRED_FUNCTOR = Logic.<Predicate, RationalCons, RationalCons, RationalCons> functor(Rationals::plus, (LogicLambda) Rationals::plusLogic);

    private static InferResult plusLogic(PredicateImpl predicate, InferContext context) {
        BigInteger numAddend1 = predicate.getVal(1, 1);
        BigInteger denAddend1 = predicate.getVal(1, 2);
        BigInteger numAddend2 = predicate.getVal(2, 1);
        BigInteger denAddend2 = predicate.getVal(2, 2);
        BigInteger numSum = predicate.getVal(3, 1);
        BigInteger denSum = predicate.getVal(3, 2);
        if (numAddend1 != null && numAddend2 != null) {
            BigInteger a = numAddend1.multiply(denAddend2);
            BigInteger b = numAddend2.multiply(denAddend1);
            StructureImpl<RationalCons> s = struct(a.add(b), denAddend1.multiply(denAddend2));
            if (numSum != null) {
                boolean eq = s.equals(predicate.getVal(3));
                return InferResult.trueFalse(eq ? Set.of(predicate) : Set.of(), eq ? Set.of() : Set.of(predicate));
            } else {
                return InferResult.trueFalse(Set.of(predicate.set(3, s)), Set.of());
            }
        } else if (numAddend1 != null && numSum != null) {
            BigInteger a = numAddend1.multiply(denSum);
            BigInteger c = numSum.multiply(denAddend1);
            return InferResult.trueFalse(Set.of(predicate.set(2, struct(c.subtract(a), denSum.multiply(denAddend1)))), Set.of());
        } else if (numAddend2 != null && numSum != null) {
            BigInteger b = numAddend2.multiply(denSum);
            BigInteger c = numSum.multiply(denAddend2);
            return InferResult.trueFalse(Set.of(predicate.set(1, struct(c.subtract(b), denSum.multiply(denAddend2)))), Set.of());
        } else {
            return InferResult.incomplete(predicate);
        }
    }

    public static Predicate plus(RationalCons addend1, RationalCons addend2, RationalCons sum) {
        return pred(PLUS_PRED_FUNCTOR, addend1, addend2, sum);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Functor<Predicate> MULTIPLY_PRED_FUNCTOR = Logic.<Predicate, RationalCons, RationalCons, RationalCons> functor(Rationals::multiply, (LogicLambda) Rationals::multiplyLogic);

    private static InferResult multiplyLogic(PredicateImpl predicate, InferContext context) {
        BigInteger numFactor1 = predicate.getVal(1, 1);
        BigInteger denFactor1 = predicate.getVal(1, 2);
        BigInteger numFactor2 = predicate.getVal(2, 1);
        BigInteger denFactor2 = predicate.getVal(2, 2);
        BigInteger numProduct = predicate.getVal(3, 1);
        BigInteger denProduct = predicate.getVal(3, 2);
        if (numFactor1 != null && numFactor2 != null) {
            StructureImpl<RationalCons> p = struct(numFactor1.multiply(numFactor2), denFactor1.multiply(denFactor2));
            if (numProduct != null) {
                boolean eq = p.equals(predicate.getVal(3));
                return InferResult.trueFalse(eq ? Set.of(predicate) : Set.of(), eq ? Set.of() : Set.of(predicate));
            } else {
                return InferResult.trueFalse(Set.of(predicate.set(3, p)), Set.of());
            }
        } else if (numFactor1 != null && numProduct != null) {
            return InferResult.trueFalse(Set.of(predicate.set(2, struct(numProduct.multiply(denFactor1), denProduct.multiply(numFactor1)))), Set.of());
        } else if (numFactor2 != null && numProduct != null) {
            return InferResult.trueFalse(Set.of(predicate.set(1, struct(numProduct.multiply(denFactor2), denProduct.multiply(numFactor2)))), Set.of());
        } else {
            return InferResult.incomplete(predicate);
        }
    }

    public static Predicate multiply(RationalCons factor1, RationalCons factor2, RationalCons product) {
        return pred(MULTIPLY_PRED_FUNCTOR, factor1, factor2, product);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Functor<Predicate> SQUARE_PRED_FUNCTOR = Logic.<Predicate, RationalCons, RationalCons> functor(Rationals::square, (LogicLambda) Rationals::squareLogic);

    private static InferResult squareLogic(PredicateImpl predicate, InferContext context) {
        BigInteger numRoot = predicate.getVal(1, 1);
        BigInteger denRoot = predicate.getVal(1, 2);
        BigInteger numSquare = predicate.getVal(2, 1);
        BigInteger denSquare = predicate.getVal(2, 2);
        if (numRoot != null) {
            StructureImpl<RationalCons> s = struct(numRoot.multiply(numRoot), denRoot.multiply(denRoot));
            if (numSquare != null) {
                boolean eq = s.equals(predicate.getVal(2));
                return InferResult.trueFalse(eq ? Set.of(predicate) : Set.of(), eq ? Set.of() : Set.of(predicate));
            } else {
                return InferResult.trueFalse(Set.of(predicate.set(2, s)), Set.of());
            }
        } else if (numSquare != null) {
            BigInteger sqrt = numSquare.multiply(denSquare).sqrt();
            BigInteger abs = denSquare.abs();
            return InferResult.trueFalse(Set.of(predicate.set(1, struct(sqrt, abs)), predicate.set(1, struct(sqrt.negate(), abs))), Set.of());
        } else {
            return InferResult.incomplete(predicate);
        }
    }

    public static Predicate square(RationalCons root, RationalCons square) {
        return pred(SQUARE_PRED_FUNCTOR, root, square);
    }

    // Functions

    private static Functor<Relation> GT_FUNCTOR = functor(Rationals::gt);

    public static Relation gt(Rational a, Rational b) {
        return pred(GT_FUNCTOR, a, b);
    }

    private static Functor<Relation> LT_FUNCTOR = functor(Rationals::lt);

    public static Relation lt(Rational a, Rational b) {
        return pred(LT_FUNCTOR, a, b);
    }

    private static Functor<Relation> GE_FUNCTOR = functor(Rationals::ge);

    public static Relation ge(Rational a, Rational b) {
        return pred(GE_FUNCTOR, a, b);
    }

    private static Functor<Relation> LE_FUNCTOR = functor(Rationals::le);

    public static Relation le(Rational a, Rational b) {
        return pred(LE_FUNCTOR, a, b);
    }

    private static Functor<RationalFunc> PLUS_FUNC_FUNCTOR = Logic.<RationalFunc, Rational, Rational> functor(Rationals::plus);

    public static RationalFunc plus(Rational a, Rational b) {
        return function(PLUS_FUNC_FUNCTOR, a, b);
    }

    private static Functor<RationalFunc> MINUS_FUNC_FUNCTOR = Logic.<RationalFunc, Rational, Rational> functor(Rationals::minus);

    public static RationalFunc minus(Rational a, Rational b) {
        return function(MINUS_FUNC_FUNCTOR, a, b);
    }

    private static Functor<RationalFunc> MULTIPLY_FUNC_FUNCTOR = Logic.<RationalFunc, Rational, Rational> functor(Rationals::multiply);

    public static RationalFunc multiply(Rational a, Rational b) {
        return function(MULTIPLY_FUNC_FUNCTOR, a, b);
    }

    private static Functor<RationalFunc> DIVIDE_FUNC_FUNCTOR = Logic.<RationalFunc, Rational, Rational> functor(Rationals::divide);

    public static RationalFunc divide(Rational a, Rational b) {
        return function(DIVIDE_FUNC_FUNCTOR, a, b);
    }

    private static Functor<RationalFunc> SQUARE_FUNC_FUNCTOR = Logic.<RationalFunc, Rational> functor(Rationals::square);

    public static RationalFunc square(Rational a) {
        return function(SQUARE_FUNC_FUNCTOR, a);
    }

    private static Functor<RationalFunc> SQRT_FUNC_FUNCTOR = Logic.<RationalFunc, Rational> functor(Rationals::sqrt);

    public static RationalFunc sqrt(Rational a) {
        return function(SQRT_FUNC_FUNCTOR, a);
    }

    // Rules

    private static final RationalCons P = rConsVar("PL");
    private static final RationalCons Q = rConsVar("QL");
    private static final RationalCons R = rConsVar("RL");

    private static final IntegerCons  I = iConsVar("IL");

    private static final Rational     X = rVar("X");
    private static final Rational     Y = rVar("Y");

    public static void rationalRules() {
        isRules();

        rule(gt(X, Y), and(is(X, P), is(Y, Q), compare(P, Q, i(1))));
        rule(lt(X, Y), and(is(X, P), is(Y, Q), compare(P, Q, i(-1))));
        rule(ge(X, Y), and(is(X, P), is(Y, Q), compare(P, Q, I), or(eq(I, i(1)), eq(I, i(0)))));
        rule(le(X, Y), and(is(X, P), is(Y, Q), compare(P, Q, I), or(eq(I, i(-1)), eq(I, i(0)))));

        rule(is(plus(X, Y), R), and(is(X, P), is(Y, Q), plus(P, Q, R)));
        rule(is(minus(X, Y), R), and(is(X, P), is(Y, Q), plus(R, Q, P)));
        rule(is(multiply(X, Y), R), and(is(X, P), is(Y, Q), multiply(P, Q, R)));
        rule(is(divide(X, Y), R), and(is(X, P), is(Y, Q), multiply(R, Q, P)));
        rule(is(square(X), R), and(is(X, P), square(P, R)));
        rule(is(sqrt(X), R), and(is(X, P), square(R, P)));
    }

}
