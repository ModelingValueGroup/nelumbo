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

import java.util.LinkedList;
import java.util.regex.Matcher;

@SuppressWarnings("ClassCanBeRecord")
public class Tokenizer {
    private final String  input;
    private final String  fileName;
    private final boolean includeAllTokens;

    public Tokenizer(String input, String fileName) {
        this(input, fileName, false);
    }

    public Tokenizer(String input, String fileName, boolean includeAllTokens) {
        this.input            = input;
        this.fileName         = fileName;
        this.includeAllTokens = includeAllTokens;
    }

    public LinkedList<Token> tokenize() throws ParseException {
        LinkedList<Token> tokens     = new LinkedList<>();
        TokenType[]       tokenTypes = TokenType.values();
        Matcher[]         matchers   = new Matcher[tokenTypes.length];
        for (int i = 0; i < tokenTypes.length; i++) {
            matchers[i] = tokenTypes[i].pattern().matcher(input);
            if (!matchers[i].find()) {
                matchers[i] = null;
            }
        }
        int index    = 0;
        int line     = 0;
        int position = 0;
        while (index < input.length()) {
            String    text = null;
            TokenType type = null;
            for (int i = 0; i < tokenTypes.length; i++) {
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
            // adjust index:
            index += text.length();
            // adjust line:
            int lineInc = text.replaceAll("\\V", "").length();
            if (0 < lineInc) {
                line += lineInc;
                position = 0;
            }
            //adjust position:
            position += text.replaceAll(".*\\v", "").length();
        }
        return tokens;
    }

    private void addToken(LinkedList<Token> tokens, TokenType type, String text, int line, int position, int index) {
        if (!includeAllTokens) {
            if (type.comment()) {
                // ignore comments
                return;
            }
            if (type == TokenType.HSPACE) {
                // ignore whitespace
                return;
            }
            if (type == TokenType.NEWLINE) {
                if (tokens.isEmpty()) {
                    // ignore newlines at the start of the input
                    return;
                }
                if (tokens.getLast().type().more()) {
                    // ignore newlines after a token that can be continued
                    return;
                }
            }
        }
        tokens.add(new Token(type, text, line, position, index, fileName));
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
}
