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

public class Tokenizer {

    private final String input;
    private final String fileName;

    public Tokenizer(String input, String fileName) {
        this.input = input;
        this.fileName = fileName;
    }

    public LinkedList<Token> tokenize() throws ParseException {
        LinkedList<Token> tokens = new LinkedList<>();
        TokenType[] tokenTypes = TokenType.values();
        Matcher[] matchers = new Matcher[tokenTypes.length];
        for (int i = 0; i < tokenTypes.length; i++) {
            matchers[i] = tokenTypes[i].pattern().matcher(input);
            if (!matchers[i].find()) {
                matchers[i] = null;
            }
        }
        int index = 0;
        int line = 1;
        int position = 1;
        while (index < input.length()) {
            String text = null;
            TokenType type = null;
            for (int i = 0; i < tokenTypes.length; i++) {
                while (matchers[i] != null && matchers[i].start() < index) {
                    if (!matchers[i].find()) {
                        matchers[i] = null;
                    }
                }
                if (matchers[i] != null && matchers[i].start() == index) {
                    String group = matchers[i].group();
                    if (text == null || text.length() < group.length()) {
                        text = group;
                        type = tokenTypes[i];
                    }
                }
            }
            if (text == null) {
                text = input.substring(index, Math.min(input.length(), index + 8));
                throw new ParseException("Unexpected input '" + text + "'", line, position, index, text, fileName);
            } else {
                if (type != TokenType.HSPACE && (type != TokenType.NEWLINE || tokens.isEmpty() || !tokens.getLast().type().more())) {
                    tokens.add(new Token(type, text, line, position, index, fileName));
                }
                index += text.length();
                if (type == TokenType.NEWLINE) {
                    int i = text.indexOf('\n');
                    while (i >= 0) {
                        line++;
                        i = text.indexOf('\n', i + 1);
                    }
                    position = 0;
                } else {
                    position += text.length();
                }
            }
        }
        return tokens;
    }
}
