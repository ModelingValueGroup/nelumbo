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

import java.util.Objects;
import java.util.stream.Stream;

import org.modelingvalue.collections.List;
import org.modelingvalue.nelumbo.Node;
import org.modelingvalue.nelumbo.U;

@SuppressWarnings("unused")
public class Token {
    public static final Token[] EMPTY = new Token[0];

    private final TokenType type;
    private final String    text;
    private final String    fileName;
    private final int       index;          // position in the input stream (0-based)
    private final int       line;           // line number in the input file (0-based)
    private final int       numLines;       // number of lines of this token (1...n)
    private final int       position;       // position (column) in the line (0-based)
    private final int       positionEnd;    // position (column) in the line (0-based) after the token

    public Token(TokenType type, String text, int line, int position, int index, String fileName) {
        if (type == null) {
            throw new NullPointerException("type can not be null");
        }
        if (text == null) {
            throw new NullPointerException("text can not be null");
        }
        this.type        = type;
        this.text        = text;
        this.fileName    = fileName;
        this.line        = line;
        this.position    = position;
        this.index       = index;
        this.numLines    = (int) text.chars().filter(ch -> ch == '\n').count() + 1;
        this.positionEnd = numLines == 1 ? position + text.length() : text.length() - text.lastIndexOf('\n');
    }

    public Token[] singleton() {
        return new Token[]{this};
    }

    public static Token[] concat(Token t1, Node n, Token t2) {
        return concat(t1.singleton(), n.tokens(), t2.singleton());
    }

    public static Token[] concat(Token t1, Token t2, List<Node> l, Token t3) {
        return concat(t1.singleton(), t2.singleton(), toTokenArray(l), t3.singleton());
    }

    public static Token[] concat(Token t, List<Node> l) {
        return concat(t.singleton(), toTokenArray(l));
    }

    public static Token[] concat(Node n1, Token t, Node n2) {
        return concat(n1.tokens(), t.singleton(), n2.tokens());
    }

    public static Token[] concat(Token t, Node n) {
        return concat(t.singleton(), n.tokens());
    }

    public static Token[] concat(Node n, Token t) {
        return concat(n.tokens(), t.singleton());
    }

    private static Token[] toTokenArray(List<Node> l) {
        return l.flatMap(n -> Stream.of(n.tokens())).toArray(Token[]::new);
    }

    public static Token[] concat(Token[]... all) {
        int     totalLength = Stream.of(all).mapToInt(t -> t.length).reduce(0, Integer::sum);
        Token[] result      = new Token[totalLength];
        int     i           = 0;
        for (Token[] tokens : all) {
            System.arraycopy(tokens, 0, result, i, tokens.length);
            i += tokens.length;
        }
        assert i == totalLength;
        return result;
    }

    public TokenType type() {
        return type;
    }

    public String text() {
        return text;
    }

    public String textTraced() {
        return U.traceable(text);
    }

    public int index() {
        return index;
    }

    public int indexEnd() {
        return index + numChars();
    }

    public int line() {
        return line;
    }

    public int lineEnd() {
        return line + numLines - 1;
    }

    public int numLines() {
        return numLines;
    }

    public int position() {
        return position;
    }

    public int positionEnd() {
        return positionEnd;
    }

    public int numChars() {
        return text.length();
    }

    public boolean contains(int l, int c) {
        if (numLines == 1) {
            return line == l && position <= c && c < positionEnd;
        } else if (l < line || lineEnd() <= l) {
            return false;
        } else if (l == line) {
            return position <= c;
        } else if (l == lineEnd() - 1) {
            return c < positionEnd;
        } else {
            return true;
        }
    }

    public String fileName() {
        return fileName;
    }

    public boolean isCommentOrHspace() {
        return type.comment() || type == TokenType.HSPACE;
    }

    public Token splitGet1(int len) {
        return new Token(type, text.substring(0, len), line, position, index, fileName);
    }

    public Token splitGet2(int len) {
        return new Token(type, text.substring(len), line, position + len, index + len, fileName);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        var that = (Token) obj;
        return Objects.equals(this.type, that.type) && Objects.equals(this.text, that.text) && this.line == that.line && this.position == that.position && this.index == that.index && Objects.equals(this.fileName, that.fileName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, text, line, position, index, fileName);
    }

    @Override
    public String toString() {
        return String.format("TOKEN: %5d (%3d,%3d) %-16s '%s'", index, line, position, type, textTraced());
    }
}
