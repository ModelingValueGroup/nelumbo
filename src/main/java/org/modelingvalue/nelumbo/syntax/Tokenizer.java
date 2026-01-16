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

import org.modelingvalue.collections.List;
import org.modelingvalue.nelumbo.U;

@SuppressWarnings("ClassCanBeRecord")
public class Tokenizer {

    private static final int FIRST = 0, FIRST_ALL = 1, LAST = 2, LAST_ALL = 3;

    private final String     input;
    private final String     fileName;

    public Tokenizer(String input, String fileName) {
        this.input = input;
        this.fileName = fileName;
    }

    public TokenizerResult tokenize() {
        Token[] tokens = new Token[4];
        TokenType.TokenMatcher tokenMatcher = TokenType.getMatcher(input);
        int line = 0;
        int position = 0;
        int index = 0;
        addToken(tokens, TokenType.BEGINOFFILE, "", 0, 0, 0);
        String previousVertical = null;
        while (tokenMatcher.hasMore()) {
            String text = tokenMatcher.text();
            TokenType type = tokenMatcher.type();
            addToken(tokens, type, text, line, position, index);
            int lineIncr = U.numNewLines(text);
            if (0 < lineIncr) {
                if (previousVertical == null || previousVertical.contains(text)) {
                    line += lineIncr;
                    position = 0;
                    previousVertical = previousVertical == null ? text : previousVertical + text;
                    index += 1;
                }
            } else {
                previousVertical = null;
                index += text.length();
            }
            position += U.lastLineLength(text);
        }
        addToken(tokens, TokenType.ENDOFFILE, "", line, position, index);
        TokenizerResult result = new TokenizerResult(tokens);
        // assert (isResultOk(result));
        return result;
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
            if (tokens[LAST] == null || tokens[LAST].type() == TokenType.BEGINOFFILE || tokens[LAST].type().isContinuesOnNextLine()) {
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

    private boolean isResultOk(TokenizerResult result) {
        String[] lines = input.split("\\n");
        for (int i = 0; i < lines.length; i++) {
            lines[i] += "\n";
        }
        for (Token token : result.listAll()) {
            if (!isTokenOk(token, lines)) {
                return false;
            }
        }
        return true;
    }

    private boolean isTokenOk(Token token, String[] lines) {
        return token.fileName().equals(fileName) && //
                input.substring(token.index(), token.indexEnd()).equals(token.text()) && //
                lines[token.line()].substring(token.position(), token.position() + token.text().length()).equals(token.text());
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

        @SuppressWarnings("unused")
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
