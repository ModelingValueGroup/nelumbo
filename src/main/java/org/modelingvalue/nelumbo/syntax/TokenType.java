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
    SEMICOLON(";", true, false), //
    COMMA(",", true, false), //
    LPAREN("\\(", true, false), //
    RPAREN("\\)", false, false), //
    LBRACKET("\\[", true, false), //
    RBRACKET("\\]", false, false), //
    LBRACE("\\{", true, false), //
    RBRACE("\\}", false, false), //
    STRING("\"([^\"\\\\]|\\\\[\\s\\S])*\"", false, false), //
    NUMBER("[0-9]+(#[0-9a-zA-Z]+)?", false, false), //
    DECIMAL("[0-9]+\\.[0-9]+", false, false), //
    QNAME("[a-zA-Z_][0-9a-zA-Z_]*(\\.[a-zA-Z_][0-9a-zA-Z_]*)+", false, false), //
    NAME("[a-zA-Z_][0-9a-zA-Z_]*", false, false), //
    TYPE("<[a-zA-Z_][0-9a-zA-Z_]*>", false, false), //
    OPERATOR("[~!@#$%^&*=+|:<>.?/-]+", true, false), //
    META_OPERATOR("`[~!@#$%^&*=+|:<>.?/-]+`", true, false), //
    HSPACE("\\h+", false, false), //
    NEWLINE("\\v", false, false), //
    END_LINE_COMMENT("//[^\\v]*", false, true), //
    IN_LINE_COMMENT("/\\*.*?(?:\\*/|\\z)", false, true), //
    ERROR(".", false, false),//
    ;

    private final Pattern pattern; // the pattern that matches tokens of this token type
    private final boolean more;    // indicates that a sequence of NEWLINE tokens after this token is to be ignored when parsing
    private final boolean comment; // indicates a comment (END_LINE_COMMENT/IN_LINE_COMMENT)

    TokenType(String regexp, boolean more, boolean comment) {
        this.pattern = Pattern.compile(regexp, Pattern.MULTILINE | Pattern.DOTALL);
        this.more = more;
        this.comment = comment;
    }

    public Pattern pattern() {
        return pattern;
    }

    public boolean more() {
        return more;
    }

    public boolean comment() {
        return comment;
    }

    public boolean skip() {
        return this == HSPACE || this == NEWLINE || this == ERROR || comment();
    }
}
