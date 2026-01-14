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

import static org.modelingvalue.nelumbo.syntax.TokenType.Flag.CONTINUES_ON_NEXT_LINE;
import static org.modelingvalue.nelumbo.syntax.TokenType.Flag.LAYOUT;
import static org.modelingvalue.nelumbo.syntax.TokenType.Flag.NOT_MATCHED;
import static org.modelingvalue.nelumbo.syntax.TokenType.Flag.SKIP;
import static org.modelingvalue.nelumbo.syntax.TokenType.Flag.VARIABLE_CONTENT;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.regex.Matcher;
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

    public enum Flag {
        SKIP,                   // indicates a non semantic part that may be ignored by the parser
        CONTINUES_ON_NEXT_LINE, // indicates that a NEWLINE token after this token is to be ignored when parsing
        NOT_MATCHED,            // indicates that this token type is not actually matched by the lexer
        VARIABLE_CONTENT,       // indicates that this token type has a variable content
        LAYOUT                  // indicates that this token type is layout and should be ignored by the parser
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
        this.skip                = flagset.contains(SKIP);
        this.continuesOnNextLine = flagset.contains(CONTINUES_ON_NEXT_LINE);
        this.layout              = flagset.contains(LAYOUT);
        this.variableContent     = flagset.contains(VARIABLE_CONTENT);
        this.notMatched          = flagset.contains(NOT_MATCHED);
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

    public static TokenMatcher getMatcher(String input) {
        return new TokenMatcher(input);
    }

    public static class TokenMatcher {
        private final TokenType[] tokenTypes;
        private final Matcher[]   matchers;
        private final String      input;
        private       int         offset;
        private       String      matchedText;
        private       TokenType   matchedType;

        private TokenMatcher(String input) {
            TokenType[] tta = TokenType.values();
            Matcher[]   maa = new Matcher[tta.length];
            this.input  = input;
            this.offset = 0;
            int firstNotMatched = 0;
            for (int i = 0; i < tta.length; i++) {
                TokenType t = tta[i];
                if (!t.isNotMatched()) {
                    Matcher m = t.pattern.matcher(input);
                    if (m.find()) {
                        firstNotMatched = i + 1;
                        maa[i]          = m;
                    }
                }
            }
            if (firstNotMatched < tta.length) {
                tta = Arrays.copyOf(tta, firstNotMatched);
                maa = Arrays.copyOf(maa, firstNotMatched);
            }
            this.tokenTypes = tta;
            this.matchers   = maa;
        }

        public boolean hasMore() {
            int len = input.length();
            if (len <= offset) {
                return false;
            }
            int       matchEnd  = -1;
            TokenType matchType = null;
            for (int i = 0; i < matchers.length; i++) {
                final Matcher m = matchers[i];
                if (m != null) {
                    m.region(offset, len);
                    if (m.lookingAt() && m.end() > matchEnd) {
                        matchEnd  = m.end();
                        matchType = tokenTypes[i];
                    }
                }
            }
            assert matchEnd >= 0;
            assert matchType != null;
            matchedText = input.substring(offset, matchEnd);
            matchedType = matchType;
            offset      = matchEnd;
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
