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

import java.util.regex.Pattern;

public enum TokenType {
    COMMA(",", true), //
    LPAREN("\\(", true), //
    RPAREN("\\)", false), //
    LBRACKET("\\[", true), //
    RBRACKET("\\]", false), //
    LBRACE("\\{", true), //
    RBRACE("\\}", false), //
    STRING("\"([^\"\\\\]|\\\\[\\s\\S])*\"", false), //
    NUMBER("[1-9][0-9]*", false), //
    DECIMAL("[1-9][0-9]*\\.[0-9]+", false), //
    QNAME("[a-zA-Z_][a-zA-Z0-9_]*(\\.[a-zA-Z_][a-zA-Z0-9_]*)+", false), //
    NAME("[a-zA-Z_][a-zA-Z0-9_]*", false), //
    NAMEDCL("[a-zA-Z_][a-zA-Z0-9_]*+\\([1-9][0-9]*\\)", false), //
    TYPE("<[a-zA-Z_][a-zA-Z0-9_]*([\\*|\\+])?>", false), //
    OPERATOR("[:\\=\\-\\*\\+<>/!?@#$%^&|~]+", true), //
    OPERATORDCL("[:\\=\\-\\*\\+<>/!@#$%^&|~]+\\([1-9][0-9]*\\)", true), //
    HSPACE("\\h+", false), //
    NEWLINE("((//[^\\v]*)?\\v)+", false);

    private final Pattern pattern;
    private final boolean more;

    private TokenType(String regexp, boolean more) {
        this.pattern = Pattern.compile(regexp, Pattern.MULTILINE | Pattern.DOTALL);
        this.more = more;
    }

    public boolean more() {
        return more;
    }

    public Pattern pattern() {
        return pattern;
    }
}
