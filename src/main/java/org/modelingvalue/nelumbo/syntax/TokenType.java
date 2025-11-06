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
    SINGLEQUOTE("'", false, "'", false), //
    SEMICOLON(";", false, ";", false), //
    COMMA(",", false, ",", true), //
    LEFT("[\\(\\[\\{]", false, null, true), //
    RIGHT("[\\)\\]\\}]", false, null, false), //
    STRING("\"([^\"\\\\]|\\\\[\\s\\S])*\"", false, null, false), //
    NUMBER("-?[0-9]+(#[0-9a-zA-Z]+)?", false, null, false), //
    DECIMAL("-?[0-9]+\\.[0-9]+", false, null, false), //
    NAME("[a-zA-Z_][0-9a-zA-Z_]*", false, null, false), //
    TYPE("<[a-zA-Z_][0-9a-zA-Z_]*>", false, null, false), //
    META_OPERATOR("<(\\(|\\)|\\)\\?|\\)\\*|\\)\\+|\\,|\\|)>", false, null, false), //
    OPERATOR("[~!@#$%^&*=+|:<>.?/-]+", false, null, true), //
    NEWLINE("\\v", false, "", true), //
    HSPACE("\\h+", true, "", false), //
    END_LINE_COMMENT("//[^\\v]*", true, null, false), //
    IN_LINE_COMMENT("/\\*.*?(?:\\*/|\\z)", true, null, false), //
    ERROR(".", false, null, false), //
    BEGINOFFILE("", false, "", false), //
    ENDOFFILE("", false, "", false), //
    ENDOFLINE("", false, "", false), //
    VARIABLE("[a-zA-Z_][0-9a-zA-Z_]*", false, null, false), //
    ;

    private final Pattern pattern; // the pattern that matches tokens of this token type
    private final boolean skip;    // indicates a non semantic part that may be ignored by the parser
    private final String  fixed;   // indicates a token type that has a singular content with the text, otherwise null;
    private final boolean more;    // indicates that a sequence of NEWLINE tokens after this token is to be ignored when parsing

    TokenType(String regexp, boolean skip, String fixed, boolean more) {
        this.pattern = Pattern.compile(regexp, Pattern.MULTILINE | Pattern.DOTALL);
        this.skip = skip;
        this.fixed = fixed;
        this.more = more;
    }

    public Pattern pattern() {
        return pattern;
    }

    public boolean skip() {
        return skip;
    }

    public boolean variable() {
        return fixed == null;
    }

    public String fixed() {
        return fixed;
    }

    public boolean more() {
        return more;
    }

    public static TokenType of(String text) {
        for (TokenType tt : TokenType.values()) {
            if (tt.pattern().matcher(text).matches()) {
                return tt;
            }
        }
        return null;
    }
}
