//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
// (C) Copyright 2018-2026 Modeling Value Group B.V. (http://modelingvalue.org)                                        ~
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

import static org.modelingvalue.nelumbo.syntax.TokenType.Constants.CONTINUES_ON_NEXT_LINE;
import static org.modelingvalue.nelumbo.syntax.TokenType.Constants.LAYOUT;
import static org.modelingvalue.nelumbo.syntax.TokenType.Constants.NOT_MATCHED;
import static org.modelingvalue.nelumbo.syntax.TokenType.Constants.SKIP;
import static org.modelingvalue.nelumbo.syntax.TokenType.Constants.VARIABLE_CONTENT;

import java.util.regex.Pattern;

public enum TokenType {
    SINGLEQUOTE("'"), //
    SEMICOLON(";"), //
    COMMA(",", CONTINUES_ON_NEXT_LINE), //
    LEFT("[\\(\\[\\{]", CONTINUES_ON_NEXT_LINE + VARIABLE_CONTENT), //
    RIGHT("[\\)\\]\\}]", VARIABLE_CONTENT), //
    STRING("\"([^\"\\\\]|\\\\[\\s\\S])*\"", VARIABLE_CONTENT), //
    NUMBER("-?[0-9]+(#[0-9a-zA-Z]+)?", VARIABLE_CONTENT), //
    DECIMAL("-?[0-9]+\\.[0-9]+", VARIABLE_CONTENT), //
    NAME("[a-zA-Z_][0-9a-zA-Z_]*", VARIABLE_CONTENT), //
    TYPE("<[a-zA-Z_][0-9a-zA-Z_]*>", VARIABLE_CONTENT), //
    META_OPERATOR("<(\\(|\\)|\\)\\?|\\)\\*|\\)\\+|\\,|\\|)>", VARIABLE_CONTENT), //
    OPERATOR("(?!//)[~!@#$%^&*=+|:<>.?/-]+", CONTINUES_ON_NEXT_LINE + VARIABLE_CONTENT), //
    NEWLINE("\\v", CONTINUES_ON_NEXT_LINE + LAYOUT), //
    HSPACE("\\h+", SKIP + LAYOUT), //
    END_LINE_COMMENT("//[^\\v]*", SKIP + VARIABLE_CONTENT), //
    IN_LINE_COMMENT("/\\*.*?(?:\\*/|\\z)", SKIP + VARIABLE_CONTENT), //
    ERROR(".", VARIABLE_CONTENT), //
    //================ rest is not actually matched:
    BEGINOFFILE, //
    ENDOFFILE, //
    ENDOFLINE, //
    VARIABLE("[a-zA-Z_][0-9a-zA-Z_]*", SKIP + VARIABLE_CONTENT + NOT_MATCHED), //
    KEYWORD("[a-zA-Z_][0-9a-zA-Z_]*", SKIP + VARIABLE_CONTENT + NOT_MATCHED), //
    ;

    private final Pattern pattern;              // the pattern that matches tokens of this token type
    private final boolean skip;                 // indicates a non semantic part that may be ignored by the parser
    private final boolean continuesOnNextLine;  // indicates that a NEWLINE token after this token is to be ignored when parsing
    private final boolean layout;               // indicates that this token type is layout and should be ignored by the parser
    private final boolean variableContent;      // indicates that this token type has a variable content
    private final boolean notMatched;           // indicates that this token type is not actually matched by the lexer

    TokenType() {
        this("", LAYOUT + NOT_MATCHED);
    }

    TokenType(String regexp) {
        this(regexp, "");
    }

    TokenType(String regexp, String flags) {
        this.pattern             = Pattern.compile(regexp, Pattern.MULTILINE | Pattern.DOTALL);
        this.skip                = flags.contains(SKIP);
        this.continuesOnNextLine = flags.contains(CONTINUES_ON_NEXT_LINE);
        this.layout              = flags.contains(LAYOUT);
        this.variableContent     = flags.contains(VARIABLE_CONTENT);
        this.notMatched          = flags.contains(NOT_MATCHED);
    }

    public boolean isSkip() {
        return skip;
    }

    public boolean isContinuesOnNextLine() {
        return continuesOnNextLine;
    }

    public boolean isLayout() {
        return layout;
    }

    public boolean isVariableContent() {
        return variableContent;
    }

    public boolean isNotMatched() {
        return notMatched;
    }

    public boolean matches(String text) {
        return pattern.matcher(text).matches();
    }

    public static TokenType of(String text) {
        for (TokenType tt : TokenType.values()) {
            if (tt.matches(text)) {
                return tt;
            }
        }
        return null;
    }

    public static Matcher getMatcher(String input) {
        return new Matcher(input);
    }

    public static class Matcher {
        private final TokenType[]               tokenTypes = TokenType.values();
        private final java.util.regex.Matcher[] matchers   = new java.util.regex.Matcher[tokenTypes.length];
        private final String                    input;
        private       int                       offset;
        private       String                    matchedText;
        private       TokenType                 matchedType;

        private Matcher(String input) {
            this.input  = input;
            this.offset = 0;
            for (int i = 0; i < matchers.length; i++) {
                TokenType t = tokenTypes[i];
                if (!t.isNotMatched()) {
                    java.util.regex.Matcher m = t.pattern.matcher(input);
                    if (m.find()) {
                        matchers[i] = m;
                    }
                }
            }
        }

        public boolean hasMore() {
            if (input.length() <= offset) {
                return false;
            }
            String    text = null;
            TokenType type = null;
            for (int i = 0; i < matchers.length; i++) {
                final java.util.regex.Matcher m = matchers[i];
                if (m != null) {
                    if (!m.find(offset)) {
                        matchers[i] = null;
                    } else if (m.start() == offset) {
                        String group = m.group();
                        if (text == null || text.length() < group.length()) {
                            text = group;
                            type = tokenTypes[i];
                        }
                    }
                }
            }
            assert text != null;
            assert type != null;
            offset += text.length();
            matchedText = text;
            matchedType = type;
            return true;
        }

        public String text() {
            return matchedText;
        }

        public TokenType type() {
            return matchedType;
        }
    }

    interface Constants {
        String SKIP                   = "S";
        String CONTINUES_ON_NEXT_LINE = "C";
        String NOT_MATCHED            = "N";
        String VARIABLE_CONTENT       = "V";
        String LAYOUT                 = "L";
    }
}
