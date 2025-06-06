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

import java.math.BigDecimal;
import java.math.BigInteger;

import org.modelingvalue.collections.Set;
import org.modelingvalue.nelumbo.Logic.Constant;
import org.modelingvalue.nelumbo.Logic.Function;
import org.modelingvalue.nelumbo.Logic.Functor1;
import org.modelingvalue.nelumbo.Logic.Functor2;
import org.modelingvalue.nelumbo.Logic.Functor3;
import org.modelingvalue.nelumbo.Logic.Relation;
import org.modelingvalue.nelumbo.Logic.Structure;
import org.modelingvalue.nelumbo.impl.InferContext;
import org.modelingvalue.nelumbo.impl.InferResult;
import org.modelingvalue.nelumbo.impl.RelationImpl;
import org.modelingvalue.nelumbo.impl.StructureImpl;

public final class Rationals {

    private Rationals() {
    }

    // Types

    public interface Rational extends Structure {
    }

    public interface RationalCons extends Rational, Constant {
    }

    public interface RationalFunc extends Rational, Function {
    }

    // Constants and variables

    private static Functor2<RationalCons, BigInteger, BigInteger> R_FUNCTOR = functor2(Rationals::r, normalize(r -> {
        BigInteger numerator = r.getVal(1);
        BigInteger denominator = r.getVal(2);
        BigInteger gcd = numerator.gcd(denominator);
        return r.set(1, numerator.divide(gcd), denominator.divide(gcd));
    }), render(s -> s.getVal(2).equals(BigInteger.ONE) ? s.toString(1) : s.toString(1) + "/" + s.toString(2)));

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

    public static RationalCons r(BigDecimal decimal) {
        BigInteger numerator = decimal.unscaledValue();
        BigInteger denominator = BigInteger.TEN.pow(decimal.scale());
        return r(numerator, denominator);
    }

    public static RationalCons r(double value) {
        return r(BigDecimal.valueOf(value));
    }

    public static RationalCons rConsVar(String name) {
        return variable(RationalCons.class, name);
    }

    public static Rational rVar(String name) {
        return variable(Rational.class, name);
    }

    // Defined by native logic

    private static final StructureImpl<RationalCons> ZERO_RATIONAL = StructureImpl.unproxy(r(0));

    private static StructureImpl<RationalCons> struct(BigInteger numerator, BigInteger denominator) {
        return ZERO_RATIONAL.set(1, numerator, denominator);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Functor2<Relation, RationalCons, RationalCons> GT_CONS_FUNCTOR = functor2(Rationals::gt, //
            logic(Rationals::gtcLogic));

    @SuppressWarnings("rawtypes")
    private static InferResult gtcLogic(RelationImpl relation, InferContext context) {
        BigInteger numComp1 = relation.getVal(1, 1);
        BigInteger numComp2 = relation.getVal(2, 1);
        if (numComp1 != null && numComp2 != null) {
            BigInteger denComp1 = relation.getVal(1, 2);
            BigInteger denComp2 = relation.getVal(2, 2);
            int r = numComp1.multiply(denComp2).compareTo(numComp2.multiply(denComp1));
            return r > 0 ? relation.factCC() : relation.falsehoodCC();
        } else {
            return relation.unknown();
        }
    }

    private static Relation gt(RationalCons compared1, RationalCons compared2) {
        return relation(GT_CONS_FUNCTOR, compared1, compared2);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Functor3<Relation, RationalCons, RationalCons, RationalCons> PLUS_REL_FUNCTOR = functor3(Rationals::plus, //
            logic(Rationals::plusLogic));

    private static InferResult plusLogic(RelationImpl relation, InferContext context) {
        BigInteger numAddend1 = relation.getVal(1, 1);
        BigInteger denAddend1 = relation.getVal(1, 2);
        BigInteger numAddend2 = relation.getVal(2, 1);
        BigInteger denAddend2 = relation.getVal(2, 2);
        BigInteger numSum = relation.getVal(3, 1);
        BigInteger denSum = relation.getVal(3, 2);
        if (numAddend1 != null && numAddend2 != null) {
            BigInteger a = numAddend1.multiply(denAddend2);
            BigInteger b = numAddend2.multiply(denAddend1);
            StructureImpl<RationalCons> s = struct(a.add(b), denAddend1.multiply(denAddend2));
            if (numSum != null) {
                boolean eq = s.equals(relation.getVal(3));
                return eq ? relation.factCC() : relation.falsehoodCC();
            } else {
                return relation.set(3, s).factCI();
            }
        } else if (numAddend1 != null && numSum != null) {
            BigInteger a = numAddend1.multiply(denSum);
            BigInteger c = numSum.multiply(denAddend1);
            return relation.set(2, struct(c.subtract(a), denSum.multiply(denAddend1))).factCI();
        } else if (numAddend2 != null && numSum != null) {
            BigInteger b = numAddend2.multiply(denSum);
            BigInteger c = numSum.multiply(denAddend2);
            return relation.set(1, struct(c.subtract(b), denSum.multiply(denAddend2))).factCI();
        } else {
            return relation.unknown();
        }
    }

    public static Relation plus(RationalCons addend1, RationalCons addend2, RationalCons sum) {
        return relation(PLUS_REL_FUNCTOR, addend1, addend2, sum);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Functor3<Relation, RationalCons, RationalCons, RationalCons> MULTIPLY_REL_FUNCTOR = functor3(Rationals::multiply, //
            logic(Rationals::multiplyLogic));

    private static InferResult multiplyLogic(RelationImpl relation, InferContext context) {
        BigInteger numFactor1 = relation.getVal(1, 1);
        BigInteger denFactor1 = relation.getVal(1, 2);
        BigInteger numFactor2 = relation.getVal(2, 1);
        BigInteger denFactor2 = relation.getVal(2, 2);
        BigInteger numProduct = relation.getVal(3, 1);
        BigInteger denProduct = relation.getVal(3, 2);
        if (numFactor1 != null && numFactor2 != null) {
            StructureImpl<RationalCons> p = struct(numFactor1.multiply(numFactor2), denFactor1.multiply(denFactor2));
            if (numProduct != null) {
                boolean eq = p.equals(relation.getVal(3));
                return eq ? relation.factCC() : relation.falsehoodCC();
            } else {
                return relation.set(3, p).factCI();
            }
        } else if (numFactor1 != null && numProduct != null) {
            return relation.set(2, struct(numProduct.multiply(denFactor1), denProduct.multiply(numFactor1))).factCI();
        } else if (numFactor2 != null && numProduct != null) {
            return relation.set(1, struct(numProduct.multiply(denFactor2), denProduct.multiply(numFactor2))).factCI();
        } else {
            return relation.unknown();
        }
    }

    public static Relation multiply(RationalCons factor1, RationalCons factor2, RationalCons product) {
        return relation(MULTIPLY_REL_FUNCTOR, factor1, factor2, product);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Functor2<Relation, RationalCons, RationalCons> SQUARE_REL_FUNCTOR = functor2(Rationals::square, //
            logic(Rationals::squareLogic));

    private static InferResult squareLogic(RelationImpl relation, InferContext context) {
        BigInteger numRoot = relation.getVal(1, 1);
        BigInteger denRoot = relation.getVal(1, 2);
        BigInteger numSquare = relation.getVal(2, 1);
        BigInteger denSquare = relation.getVal(2, 2);
        if (numRoot != null) {
            StructureImpl<RationalCons> s = struct(numRoot.multiply(numRoot), denRoot.multiply(denRoot));
            if (numSquare != null) {
                boolean eq = s.equals(relation.getVal(2));
                return eq ? relation.factCC() : relation.falsehoodCC();
            } else {
                return relation.set(2, s).factCI();
            }
        } else if (numSquare != null) {
            BigInteger sqrt = numSquare.multiply(denSquare).sqrt();
            BigInteger abs = denSquare.abs();
            return InferResult.factsCI(Set.of(relation.set(1, struct(sqrt, abs)), relation.set(1, struct(sqrt.negate(), abs))));
        } else {
            return relation.unknown();
        }
    }

    public static Relation square(RationalCons root, RationalCons square) {
        return relation(SQUARE_REL_FUNCTOR, root, square);
    }

    // Defined by rules

    private static Functor2<Relation, Rational, Rational> GT_FUNCTOR = functor2(Rationals::gt, //
            render(s -> s.toString(1) + ">" + s.toString(2)));

    public static Relation gt(Rational a, Rational b) {
        return relation(GT_FUNCTOR, a, b);
    }

    private static Functor2<Relation, Rational, Rational> LT_FUNCTOR = functor2(Rationals::lt, //
            render(s -> s.toString(1) + "<" + s.toString(2)));

    public static Relation lt(Rational a, Rational b) {
        return relation(LT_FUNCTOR, a, b);
    }

    private static Functor2<Relation, Rational, Rational> GE_FUNCTOR = functor2(Rationals::ge, //
            render(s -> s.toString(1) + "\u2265" + s.toString(2)));

    public static Relation ge(Rational a, Rational b) {
        return relation(GE_FUNCTOR, a, b);
    }

    private static Functor2<Relation, Rational, Rational> LE_FUNCTOR = functor2(Rationals::le, //
            render(s -> s.toString(1) + "\u2264" + s.toString(2)));

    public static Relation le(Rational a, Rational b) {
        return relation(LE_FUNCTOR, a, b);
    }

    private static Functor3<Relation, Rational, Rational, Rational> PLUS_FUNCTOR = functor3(Rationals::plus);

    private static Relation plus(Rational a, Rational b, Rational c) {
        return relation(PLUS_FUNCTOR, a, b, c);
    }

    private static Functor2<RationalFunc, Rational, Rational> PLUS_FUNC_FUNCTOR = functor2(Rationals::plus, //
            render(s -> s.toString(1) + "+" + s.toString(2)));

    public static RationalFunc plus(Rational a, Rational b) {
        return function(PLUS_FUNC_FUNCTOR, a, b);
    }

    private static Functor2<RationalFunc, Rational, Rational> MINUS_FUNC_FUNCTOR = functor2(Rationals::minus, //
            render(s -> s.toString(1) + "-" + s.toString(2)));

    public static RationalFunc minus(Rational a, Rational b) {
        return function(MINUS_FUNC_FUNCTOR, a, b);
    }

    private static Functor3<Relation, Rational, Rational, Rational> MULTIPLY_FUNCTOR = functor3(Rationals::multiply);

    private static Relation multiply(Rational a, Rational b, Rational c) {
        return relation(MULTIPLY_FUNCTOR, a, b, c);
    }

    private static Functor2<RationalFunc, Rational, Rational> MULTIPLY_FUNC_FUNCTOR = functor2(Rationals::multiply, //
            render(s -> s.toString(1) + "\u00B7" + s.toString(2)));

    public static RationalFunc multiply(Rational a, Rational b) {
        return function(MULTIPLY_FUNC_FUNCTOR, a, b);
    }

    private static Functor2<RationalFunc, Rational, Rational> DIVIDE_FUNC_FUNCTOR = functor2(Rationals::divide, //
            render(s -> s.toString(1) + "\u00F7" + s.toString(2)));

    public static RationalFunc divide(Rational a, Rational b) {
        return function(DIVIDE_FUNC_FUNCTOR, a, b);
    }

    private static Functor2<Relation, Rational, Rational> SQUARE_FUNCTOR = functor2(Rationals::square);

    private static Relation square(Rational a, Rational b) {
        return relation(SQUARE_FUNCTOR, a, b);
    }

    private static Functor1<RationalFunc, Rational> SQUARE_FUNC_FUNCTOR = functor1(Rationals::square);

    public static RationalFunc square(Rational a) {
        return function(SQUARE_FUNC_FUNCTOR, a);
    }

    private static Functor1<RationalFunc, Rational> SQRT_FUNC_FUNCTOR = functor1(Rationals::sqrt);

    public static RationalFunc sqrt(Rational a) {
        return function(SQRT_FUNC_FUNCTOR, a);
    }

    // Rules

    private static final RationalCons P = rConsVar("P");
    private static final RationalCons Q = rConsVar("Q");
    private static final RationalCons R = rConsVar("R");

    private static final Rational     X = rVar("X");
    private static final Rational     Y = rVar("Y");
    private static final Rational     Z = rVar("Z");

    public static void rationalRules() {
        isRules();

        rule(gt(X, Y), and(eq(X, P), eq(Y, Q), gt(P, Q)));
        rule(lt(X, Y), gt(Y, X));
        rule(ge(X, Y), or(gt(X, Y), eq(X, Y)));
        rule(le(X, Y), or(lt(X, Y), eq(X, Y)));

        rule(plus(X, Y, Z), and(eq(X, P), eq(Y, Q), eq(Z, R), plus(P, Q, R)));
        rule(eq(plus(X, Y), Z), plus(X, Y, Z));
        rule(eq(minus(X, Y), Z), plus(Z, Y, X));

        rule(multiply(X, Y, Z), and(eq(X, P), eq(Y, Q), eq(Z, R), multiply(P, Q, R)));
        rule(eq(multiply(X, Y), Z), multiply(X, Y, Z));
        rule(eq(divide(X, Y), Z), multiply(Z, Y, X));

        rule(square(X, Z), and(eq(X, P), eq(Z, R), square(P, R)));
        rule(eq(square(X), Z), square(X, Z));
        rule(eq(sqrt(X), Z), square(Z, X));
    }

}
