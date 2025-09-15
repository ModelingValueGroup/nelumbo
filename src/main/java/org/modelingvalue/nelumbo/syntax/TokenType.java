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
    SINGLEQUOTE("\'", false, false), //
    SEMICOLON(";", false, false), //
    COMMA(",", false, false), //
    LPAREN("\\(", false, false), //
    RPAREN("\\)", false, false), //
    LBRACKET("\\[", false, false), //
    RBRACKET("\\]", false, false), //
    LBRACE("\\{", false, false), //
    RBRACE("\\}", false, false), //
    STRING("\"([^\"\\\\]|\\\\[\\s\\S])*\"", false, true), //
    NUMBER("[0-9]+(#[0-9a-zA-Z]+)?", false, true), //
    DECIMAL("[0-9]+\\.[0-9]+", false, true), //
    QNAME("[a-zA-Z_][0-9a-zA-Z_]*(\\.[a-zA-Z_][0-9a-zA-Z_]*)+", false, true), //
    NAME("[a-zA-Z_][0-9a-zA-Z_]*", false, true), //
    TYPE("<[a-zA-Z_][0-9a-zA-Z_]*>", false, true), //
    OPERATOR("[~!@#$%^&*=+|:<>.?/-]+", false, true), //
    META_OPERATOR("<[\\[\\]\\{\\}\\(\\)\\|]>", false, true), //
    NEWLINE("\\v", false, false), //
    SKIP_NEWLINE("\\\\\\v", true, false), //
    HSPACE("\\h+", true, false), //
    END_LINE_COMMENT("//[^\\v]*", true, true), //
    IN_LINE_COMMENT("/\\*.*?(?:\\*/|\\z)", true, true), //
    ERROR(".", false, false), //
    ENDOFFILE(".", false, false),//
    ;

    private final Pattern pattern;  // the pattern that matches tokens of this token type
    private final boolean skip;     // indicates a non semantic part that may be ignored by the parser
    private final boolean variable; // indicates a token type that has no singular content

    TokenType(String regexp, boolean skip, boolean variable) {
        this.pattern = Pattern.compile(regexp, Pattern.MULTILINE | Pattern.DOTALL);
        this.skip = skip;
        this.variable = variable;
    }

    public Pattern pattern() {
        return pattern;
    }

    public boolean skip() {
        return skip;
    }

    public boolean variable() {
        return variable;
    }
}
