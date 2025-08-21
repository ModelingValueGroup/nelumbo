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

import org.modelingvalue.nelumbo.Node;
import org.modelingvalue.nelumbo.Type;

public abstract class Parselet {

    private final Type      expected;
    private final TokenType type1;
    private final String    oper1;
    private final TokenType type2;
    private final String    oper2;

    protected Parselet(Type expected, TokenType type1, String oper1, TokenType type2, String oper2) {
        this.expected = expected;
        this.type1 = type1;
        this.oper1 = oper1;
        this.type2 = type2;
        this.oper2 = oper2;
    }

    public Type expected() {
        return expected;
    }

    public TokenType type1() {
        return type1;
    }

    public String oper1() {
        return oper1;
    }

    public TokenType type2() {
        return type2;
    }

    public String oper2() {
        return oper2;
    }

    public Object key1() {
        return oper1 != null ? oper1 : type1;
    }

    public Object key2() {
        return oper2 != null ? oper2 : type2;
    }

    public abstract Type left();

    public abstract int precedence();

    public abstract Node parse(Type expected, Parser parser, Node left, Token token) throws ParseException;

}
