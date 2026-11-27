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
//     Wim Bast, Tom Brus, Victor Lap                                                                                  ~
//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

package org.modelingvalue.nelumbo.syntax;

import static org.modelingvalue.nelumbo.syntax.TokenType.Flag.*;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public enum TokenType {
    SINGLEQUOTE("'"), //
    SEMICOLON(";"), //
    COMMA(",", CONTINUES_ON_NEXT_LINE), //
    LEFT("[\\(\\[\\{]", CONTINUES_ON_NEXT_LINE, VARIABLE_CONTENT), //
    RIGHT("[\\)\\]\\}]", /* CONTINUES_ON_NEXT_LINE, */ VARIABLE_CONTENT), //
    STRING("\"([^\"\\\\]|\\\\[\\s\\S])*\"", VARIABLE_CONTENT), //
    NUMBER("[0-9]+", VARIABLE_CONTENT), //
    // letters of any script and picture emoji (emoji presentation by default, or a pictograph with variation selector
    // 16); inside also marks, digits of any script, connectors, skin tones, tags and joiners (UAX #31, emoji sequences)
    NAME("(?:[\\p{L}\\p{Nl}_\\p{IsEmoji_Presentation}]|\\p{IsExtended_Pictographic}\\uFE0F)"
            + "(?:[\\p{L}\\p{Nl}\\p{Mn}\\p{Mc}\\p{Nd}\\p{Pc}\\p{IsEmoji_Presentation}\\u200C\\x{1F3FB}-\\x{1F3FF}\\x{E0020}-\\x{E007F}]"
            + "|\\p{IsExtended_Pictographic}\\uFE0F|\\u200D\\p{IsExtended_Pictographic}?)*", VARIABLE_CONTENT), //
    END_LINE_COMMENT("//[^\\n\\r]*", SKIP, VARIABLE_CONTENT), //
    IN_LINE_COMMENT("/\\*.*?(?:\\*/|\\z)", SKIP, VARIABLE_CONTENT), //
    // also every other non-ASCII symbol, punctuation and other number (math symbols, text symbols like a heart,
    // superscripts); inside also marks, joiners and tags
    // A // anywhere ends the operator and starts an end-of-line comment, also after an initial operator character.
    OPERATOR("(?!//)(?:[~!@#$%^&*=+|:<>.?/-]|(?!\\p{IsExtended_Pictographic}\\uFE0F)[\\p{S}\\p{P}\\p{No}&&[^\\x00-\\x7F]&&[^\\p{IsEmoji_Presentation}]])"
            + "(?:(?!//)[~!@#$%^&*=+|:<>.?/-]|(?!\\p{IsExtended_Pictographic}\\uFE0F)[\\p{S}\\p{P}\\p{No}&&[^\\x00-\\x7F]&&[^\\p{IsEmoji_Presentation}]]"
            + "|[\\p{M}\\u200D\\x{E0020}-\\x{E007F}])*", CONTINUES_ON_NEXT_LINE, VARIABLE_CONTENT), //
    // the line breaks of LSP and the editors; other Unicode line and paragraph separators are spaces
    NEWLINE("\\r\\n|[\\n\\r]", CONTINUES_ON_NEXT_LINE, LAYOUT), //
    // also the invisible format characters (a BOM, zero-width space, soft hyphen, direction marks)
    HSPACE("[\\h\\x0B\\f\\x85\\u2028\\u2029\\u00AD\\u200B\\u200E\\u200F\\u202A-\\u202E\\u2060-\\u2064\\u2066-\\u2069\\uFEFF]+", SKIP, LAYOUT), //
    ERROR(".", VARIABLE_CONTENT), //
    // ================ rest is not actually matched:
    BEGINOFFILE("", LAYOUT), //
    ENDOFFILE("", LAYOUT), //
    VARIABLE, //
    KEYWORD, //
    TYPE, META_OPERATOR,//
    ;

    // Combined patterns for efficient matching - built once at class load
    private static final Pattern     COMBINED_FULL_MATCH_PATTERN;
    private static final Pattern     COMBINED_PATTERN;
    private static final TokenType[] COMBINED_PATTERN_LOOKUP;

    static {
        StringBuilder sb = new StringBuilder();
        String sep = "";
        int group = 1;
        TokenType[] lookup = new TokenType[TokenType.values().length + 1]; // index 0 unused (groups start at 1)
        for (TokenType tt : TokenType.values()) {
            if (!tt.isNotMatched()) {
                // Convert internal capturing groups to non-capturing (but not escaped literal
                // parens like \()
                String p = tt.pattern.pattern().replaceAll("(?<!\\\\)\\((?!\\?)", "(?:");
                sb.append(sep).append("(").append(p).append(")");
                sep = "|";
                lookup[group] = tt;
                group++;
            }
        }
        String fullRegexp = sb.toString();
        // noinspection RegExpUnnecessaryNonCapturingGroup
        COMBINED_FULL_MATCH_PATTERN = Pattern.compile("^(?:" + fullRegexp + ")$", Pattern.DOTALL);
        COMBINED_PATTERN = Pattern.compile(fullRegexp, Pattern.MULTILINE | Pattern.DOTALL);
        COMBINED_PATTERN_LOOKUP = Arrays.copyOf(lookup, group);
    }

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
        Matcher m = COMBINED_FULL_MATCH_PATTERN.matcher(text);
        if (m.matches()) {
            int group = findGroup(m);
            if (group != 0) {
                return COMBINED_PATTERN_LOOKUP[group];
            }
        }
        return null;
    }

    private static int findGroup(Matcher m) {
        for (int group = 1; group < COMBINED_PATTERN_LOOKUP.length; group++) {
            if (m.start(group) >= 0) {
                return group;
            }
        }
        return 0;
    }

    public static TokenMatcher getMatcher(String input) {
        return new TokenMatcher(input);
    }

    public static class TokenMatcher {
        private final Matcher matcher;
        private final int     inputLength;
        private String        matchedText;
        private TokenType     matchedType;

        private TokenMatcher(String input) {
            this.matcher = COMBINED_PATTERN.matcher(input);
            this.inputLength = input.length();
        }

        public boolean hasMore() {
            if (inputLength <= matcher.regionStart()) {
                return false;
            }
            if (matcher.lookingAt()) {
                int group = findGroup(matcher);
                if (group != 0) {
                    matchedText = matcher.group(group);
                    matchedType = COMBINED_PATTERN_LOOKUP[group];
                    matcher.region(matcher.end(), inputLength);
                    return true;
                }
            }
            throw new IllegalStateException("TokenMatcher does not match input at: " + matcher.regionStart());
        }

        public String text() {
            return matchedText;
        }

        public TokenType type() {
            return matchedType;
        }
    }
}
