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

public class Token {

    public static final Token[] EMPTY = new Token[0];

    private final TokenType     type;
    private final String        text;
    private final int           line;
    private final int           position;
    private final int           index;
    private final String        fileName;

    public Token(TokenType type, String text, int line, int position, int index, String fileName) {
        this.type = type;
        this.text = text;
        this.line = line;
        this.position = position;
        this.index = index;
        this.fileName = fileName;
    }

    public final Token[] singleton() {
        return new Token[]{this};
    }

    public final Token[] prepend(Token[] tokens) {
        Token[] result = new Token[tokens.length + 1];
        System.arraycopy(tokens, 0, result, 0, tokens.length);
        result[tokens.length] = this;
        return result;
    }

    public final Token[] append(Token[] tokens) {
        Token[] result = new Token[tokens.length + 1];
        result[0] = this;
        System.arraycopy(tokens, 0, result, 1, tokens.length);
        return result;
    }

    public final static Token[] concat(Token[] tokens1, Token[] tokens2) {
        Token[] result = new Token[tokens1.length + tokens2.length];
        System.arraycopy(tokens1, 0, result, 0, tokens1.length);
        System.arraycopy(tokens2, 0, result, tokens1.length, tokens2.length);
        return result;
    }

    @Override
    public String toString() {
        return text;
    }

    public String text() {
        return text;
    }

    public TokenType type() {
        return type;
    }

    public int line() {
        return line;
    }

    public int position() {
        return position;
    }

    public int index() {
        return index;
    }

    public String fileName() {
        return fileName;
    }
}
