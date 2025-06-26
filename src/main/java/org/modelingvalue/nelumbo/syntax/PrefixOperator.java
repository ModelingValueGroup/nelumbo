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

package org.modelingvalue.nelumbo.syntax;

import java.text.ParseException;

import org.modelingvalue.nelumbo.Node;
import org.modelingvalue.nelumbo.Type;

public abstract class PrefixOperator {

    protected static final String WILDCARD = "";

    private final String          oper;
    private final Type            right;
    private final int             precedence;

    public PrefixOperator(String oper, Type right, int precedence) {
        this.oper = oper;
        this.right = right;
        this.precedence = precedence;
    }

    public String oper() {
        return oper;
    }

    public Type right() {
        return right;
    }

    public int precedence() {
        return precedence;
    }

    public abstract Node construct(Token token, Node right) throws ParseException;

    public static PrefixOperator of(Type right, int precedence, ThrowingBiFunction<Token, Node, Node> constructor) {
        return of(WILDCARD, right, precedence, constructor);
    }

    public static PrefixOperator of(String oper, Type right, int precedence, ThrowingBiFunction<Token, Node, Node> constructor) {
        return new PrefixOperator(oper, right, precedence) {
            @Override
            public Node construct(Token token, Node right) throws ParseException {
                return constructor.apply(token, right);
            }
        };
    }

}
