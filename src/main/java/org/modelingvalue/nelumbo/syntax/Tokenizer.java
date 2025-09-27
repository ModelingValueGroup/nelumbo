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

import java.util.regex.Matcher;

import org.modelingvalue.collections.List;

@SuppressWarnings("ClassCanBeRecord")
public class Tokenizer {

    private static final int FIRST = 0, FIRST_ALL = 1, LAST = 2, LAST_ALL = 3;

    private final String     input;
    private final String     fileName;

    public Tokenizer(String input, String fileName) {
        this.input = input;
        this.fileName = fileName;
    }

    public TokenizerResult tokenize() throws ParseException {
        Token[] tokens = new Token[4];
        TokenType[] tokenTypes = TokenType.values();
        Matcher[] matchers = new Matcher[tokenTypes.length - 2];
        for (int i = 0; i < matchers.length; i++) {
            matchers[i] = tokenTypes[i].pattern().matcher(input);
            if (!matchers[i].find()) {
                matchers[i] = null;
            }
        }
        int index = 0;
        int line = 0;
        int position = 0;
        String previousVertical = null;
        while (index < input.length()) {
            String text = null;
            TokenType type = null;
            for (int i = 0; i < matchers.length; i++) {
                final Matcher m = matchers[i];
                if (m == null) {
                    continue;
                }
                if (!m.find(index)) {
                    matchers[i] = null;
                    continue;
                }
                if (m.start() == index) {
                    String group = m.group();
                    if (text == null || text.length() < group.length()) {
                        text = group;
                        type = tokenTypes[i];
                    }
                }
            }
            if (text == null) {
                String unexpectedChars = getUnexpectedToken(input, index);
                throw new ParseException("Unexpected input '" + unexpectedChars + "'", line, position, index, unexpectedChars.length(), fileName);
            }
            addToken(tokens, type, text, line, position, index);
            index += text.length();
            int lineIncr = text.replaceAll("\\V", "").length();
            if (0 < lineIncr) {
                if (previousVertical == null || previousVertical.contains(text)) {
                    line += lineIncr;
                    position = 0;
                    previousVertical = previousVertical == null ? text : previousVertical + text;
                }
            } else {
                previousVertical = null;
            }
            position += text.replaceAll(".*\\v", "").length();
        }
        addToken(tokens, TokenType.ENDOFFILE, "", line, position, index);
        return new TokenizerResult(tokens);
    }

    private void addToken(Token[] tokens, TokenType type, String text, int line, int position, int index) {
        Token token = new Token(type, text, line, position, index, fileName);
        if (tokens[FIRST_ALL] == null) {
            tokens[FIRST_ALL] = token;
        } else {
            tokens[LAST_ALL].setNextAll(token);
        }
        tokens[LAST_ALL] = token;
        if (token.skip()) {
            return;
        }
        if (token.type() == TokenType.NEWLINE) {
            if (tokens[FIRST] == null || tokens[LAST].type().more()) {
                // ignore newlines after a token that can be continued
                return;
            }
        }
        if (tokens[FIRST] == null) {
            tokens[FIRST] = token;
        } else {
            tokens[LAST].setNext(token);
        }
        tokens[LAST] = token;
    }

    private String getUnexpectedToken(String text, int at) {
        StringBuilder sb = new StringBuilder();
        for (int i = at; i < text.length() && i < at + 8; i++) {
            char c = text.charAt(i);
            if (Character.isSpaceChar(c)) {
                break;
            }
            sb.append(c);
        }
        return sb.toString();
    }

    public static final class TokenizerResult {
        private final Token[] tokens;

        public TokenizerResult(Token[] tokens) {
            this.tokens = tokens;
        }

        public Token firstAll() {
            return tokens[FIRST_ALL];
        }

        public Token first() {
            return tokens[FIRST];
        }

        public Token lastAll() {
            return tokens[LAST_ALL];
        }

        public Token last() {
            return tokens[LAST];
        }

        public List<Token> list() {
            return tokens[FIRST].list(tokens[LAST]);
        }

        public List<Token> listAll() {
            return tokens[FIRST_ALL].listAll(tokens[LAST_ALL]);
        }

    }

}
