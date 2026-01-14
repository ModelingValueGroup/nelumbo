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

import static org.modelingvalue.nelumbo.syntax.TokenType.Flag.*;

import java.util.EnumSet;
import java.util.regex.Pattern;

public enum TokenType {
    SINGLEQUOTE("'"), //
    SEMICOLON(";"), //
    COMMA(",", CONTINUES_ON_NEXT_LINE), //
    LEFT("[\\(\\[\\{]", CONTINUES_ON_NEXT_LINE, VARIABLE_CONTENT), //
    RIGHT("[\\)\\]\\}]", VARIABLE_CONTENT), //
    STRING("\"([^\"\\\\]|\\\\[\\s\\S])*\"", VARIABLE_CONTENT), //
    NUMBER("-?[0-9]+(#[0-9a-zA-Z]+)?", VARIABLE_CONTENT), //
    DECIMAL("-?[0-9]+\\.[0-9]+", VARIABLE_CONTENT), //
    NAME("[a-zA-Z_][0-9a-zA-Z_]*", VARIABLE_CONTENT), //
    META_OPERATOR("<(\\(|\\)|\\)\\?|\\)\\*|\\)\\+|\\,|\\|)>", VARIABLE_CONTENT), //
    OPERATOR("(?!//)[~!@#$%^&*=+|:<>.?/-]+", CONTINUES_ON_NEXT_LINE, VARIABLE_CONTENT), //
    NEWLINE("\\v", CONTINUES_ON_NEXT_LINE, LAYOUT), //
    HSPACE("\\h+", SKIP, LAYOUT), //
    END_LINE_COMMENT("//[^\\v]*", SKIP, VARIABLE_CONTENT), //
    IN_LINE_COMMENT("/\\*.*?(?:\\*/|\\z)", SKIP, VARIABLE_CONTENT), //
    ERROR(".", VARIABLE_CONTENT), //
    //================ rest is not actually matched:
    BEGINOFFILE, //
    ENDOFFILE, //
    ENDOFLINE, //
    VARIABLE, //
    KEYWORD, //
    TYPE, //
    ;

    public final static int NR_OF_NON_MATCHED = 6;

    public enum Flag {
        SKIP, // indicates a non semantic part that may be ignored by the parser
        CONTINUES_ON_NEXT_LINE, // indicates that a NEWLINE token after this token is to be ignored when parsing
        NOT_MATCHED, // indicates that this token type is not actually matched by the lexer
        VARIABLE_CONTENT, // indicates that this token type has a variable content
        LAYOUT // indicates that this token type is layout and should be ignored by the parser
    }

    private final Pattern pattern;
    private final boolean skip;
    private final boolean continuesOnNextLine;
    private final boolean layout;
    private final boolean variableContent;
    private final boolean notMatched;

    TokenType() {
        this("", LAYOUT, NOT_MATCHED);
    }

    TokenType(String regexp, Flag... flags) {
        this.pattern = Pattern.compile(regexp, Pattern.MULTILINE | Pattern.DOTALL);
        EnumSet<Flag> flagset = flags.length == 0 ? EnumSet.noneOf(Flag.class) : EnumSet.of(flags[0], flags);
        this.skip = flagset.contains(SKIP);
        this.continuesOnNextLine = flagset.contains(CONTINUES_ON_NEXT_LINE);
        this.layout = flagset.contains(LAYOUT);
        this.variableContent = flagset.contains(VARIABLE_CONTENT);
        this.notMatched = flagset.contains(NOT_MATCHED);
    }

    public Pattern pattern() {
        return pattern;
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
            if (!tt.isNotMatched() && tt.matches(text)) {
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
        private final java.util.regex.Matcher[] matchers   = new java.util.regex.Matcher[tokenTypes.length - NR_OF_NON_MATCHED];
        private final String                    input;
        private int                             offset;
        private String                          matchedText;
        private TokenType                       matchedType;

        private Matcher(String input) {
            this.input = input;
            this.offset = 0;
            for (int i = 0; i < matchers.length; i++) {
                TokenType t = tokenTypes[i];
                java.util.regex.Matcher m = t.pattern.matcher(input);
                if (m.find()) {
                    matchers[i] = m;
                }
            }
        }

        public boolean hasMore() {
            if (input.length() <= offset) {
                return false;
            }
            String text = null;
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
}
