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
    SINGLEQUOTE("\'", false, "'"), //
    SEMICOLON(";", false, ";"), //
    COMMA(",", false, ","), //
    LEFT("[\\(\\[\\{]", false, null), //
    RIGHT("[\\)\\]\\}]", false, null), //
    STRING("\"([^\"\\\\]|\\\\[\\s\\S])*\"", false, null), //
    NUMBER("-?[0-9]+(#[0-9a-zA-Z]+)?", false, null), //
    DECIMAL("-?[0-9]+\\.[0-9]+", false, null), //
    NAME("[a-zA-Z_][0-9a-zA-Z_]*", false, null), //
    TYPE("<[a-zA-Z_][0-9a-zA-Z_]*>", false, null), //
    META_OPERATOR("<(\\(|\\)|\\)\\?|\\)\\*|\\)\\+|\\,|\\|)>", false, null), //
    OPERATOR("[~!@#$%^&*=+|:<>.?/-]+", false, null), //
    VSPACE("\\v+", true, ""), //
    HSPACE("\\h+", true, ""), //
    END_LINE_COMMENT("//[^\\v]*", true, null), //
    IN_LINE_COMMENT("/\\*.*?(?:\\*/|\\z)", true, null), //
    ERROR(".", false, null), //
    BEGINOFFILE("", false, ""), //
    ENDOFFILE("", false, ""), //
    ENDOFLINE("", false, ""), //
    ;

    private final Pattern pattern; // the pattern that matches tokens of this token type
    private final boolean skip;    // indicates a non semantic part that may be ignored by the parser
    private final String  fixed;   // indicates a token type that has a singular content with the text, otherwise null;

    TokenType(String regexp, boolean skip, String fixed) {
        this.pattern = Pattern.compile(regexp, Pattern.MULTILINE | Pattern.DOTALL);
        this.skip = skip;
        this.fixed = fixed;
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

    public static TokenType of(String text) {
        for (TokenType tt : TokenType.values()) {
            if (tt.pattern().matcher(text).matches()) {
                return tt;
            }
        }
        return null;
    }
}
