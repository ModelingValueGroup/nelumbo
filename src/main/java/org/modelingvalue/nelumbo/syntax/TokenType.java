//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
// (C) Copyright 2018-2025 Modeling Value Group B.V. (http://modelingvalue.org)                                        ~
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

package org.modelingvalue.nelumbo.syntax;

import java.util.regex.Pattern;

public enum TokenType {
    SINGLEQUOTE("\'", false, false, false), //
    SEMICOLON(";", false, false, false), //
    COMMA(",", false, false, true), //
    LPAREN("\\(", false, false, true), //
    RPAREN("\\)", false, false, false), //
    LBRACKET("\\[", false, false, true), //
    RBRACKET("\\]", false, false, false), //
    LBRACE("\\{", false, false, true), //
    RBRACE("\\}", false, false, false), //
    STRING("\"([^\"\\\\]|\\\\[\\s\\S])*\"", false, true, false), //
    NUMBER("[0-9]+(#[0-9a-zA-Z]+)?", false, true, false), //
    DECIMAL("[0-9]+\\.[0-9]+", false, true, false), //
    NAME("[a-zA-Z_][0-9a-zA-Z_]*", false, true, false), //
    TYPE("<[a-zA-Z_][0-9a-zA-Z_]*>", false, true, false), //
    OPERATOR("[~!@#$%^&*=+|:<>.?/-]+", false, true, true), //
    META_OPERATOR("<[\\[\\]\\{\\}\\(\\)\\|]>", false, true, false), //
    NEWLINE("\\v", false, false, true), //
    HSPACE("\\h+", true, false, false), //
    END_LINE_COMMENT("//[^\\v]*", true, true, false), //
    IN_LINE_COMMENT("/\\*.*?(?:\\*/|\\z)", true, true, false), //
    ERROR(".", false, false, false), //
    ENDOFFILE(".", false, false, false), //
    ;

    private final Pattern pattern;  // the pattern that matches tokens of this token type
    private final boolean skip;     // indicates a non semantic part that may be ignored by the parser
    private final boolean variable; // indicates a token type that has no singular content
    private final boolean more;     // indicates that a sequence of NEWLINE tokens after this token is to be ignored when parsing

    TokenType(String regexp, boolean skip, boolean variable, boolean more) {
        this.pattern = Pattern.compile(regexp, Pattern.MULTILINE | Pattern.DOTALL);
        this.skip = skip;
        this.variable = variable;
        this.more = more;
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

    public boolean more() {
        return more;
    }
}
