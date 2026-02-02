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

import java.util.Objects;

import org.modelingvalue.collections.List;
import org.modelingvalue.nelumbo.AstElement;
import org.modelingvalue.nelumbo.Node;
import org.modelingvalue.nelumbo.Type;
import org.modelingvalue.nelumbo.U;
import org.modelingvalue.nelumbo.Variable;
import org.modelingvalue.nelumbo.patterns.Functor;
import org.modelingvalue.nelumbo.patterns.Pattern;

@SuppressWarnings({"unused"})
public final class Token implements AstElement {

    private final TokenType type;
    private final String    text;
    private final int       numLines;     // number of lines of this token (1...n)
    private final int       numChars;     // number of characters in this token (0...n)
    private final int       index;        // position in the input stream (0-based)
    private final int       indexEnd;     // position in the input stream (0-based) after the token
    private final int       line;         // line number in the input file (0-based)
    private final int       lineEnd;      // line number in the input file (0-based) after the token
    private final int       lastLine;     // last line number in the input file (0-based)
    private final int       position;     // position (column) in the line (0-based)
    private final int       positionEnd;  // position (column) in the line (0-based) after the token
    private final int       lastPosition; // last position (column) in the line (0-based)
    private final String    fileName;

    private Token           next;
    private Token           previous;

    private Token           nextAll;
    private Token           previousAll;

    private Node            node;
    private boolean         isKeyword;

    public Token(TokenType type, String text, int line, int position, int index, String fileName) {
        if (type == null) {
            throw new NullPointerException("type can not be null");
        }
        if (text == null) {
            throw new NullPointerException("text can not be null");
        }
        this.type = type;
        this.text = text;
        this.index = index;
        this.numLines = U.numLines(text);
        this.numChars = text.length();
        this.indexEnd = index + numChars;
        this.line = line;
        this.lineEnd = line + numLines;
        this.lastLine = numLines == 0 ? lineEnd : lineEnd - 1;
        this.position = position;
        this.positionEnd = numLines == 0 || numLines == 1 ? position + numChars : numChars - text.lastIndexOf('\n');
        this.lastPosition = positionEnd - 1;
        this.fileName = fileName;
    }

    public void setNext(Token next) {
        this.next = next;
        if (next != null) {
            next.previous = this;
        }
    }

    public void setNextAll(Token next) {
        this.nextAll = next;
        if (next != null) {
            next.previousAll = this;
        }
    }

    public void setPrevious(Token previous) {
        this.previous = previous;
        if (previous != null) {
            previous.next = this;
        }
    }

    public void setPreviousAll(Token previous) {
        this.previousAll = previous;
        if (previous != null) {
            previous.nextAll = this;
        }
    }

    public Token split(int i) {
        Token t1 = splitGet1(i);
        Token t2 = splitGet2(i);
        t1.next = t2;
        t1.nextAll = t2;
        t2.next = next();
        t2.nextAll = nextAll();
        return t1;
    }

    private Token splitGet1(int len) {
        String sub = text.substring(0, len);
        return new Token(TokenType.of(sub), sub, line, position, index, fileName);
    }

    private Token splitGet2(int len) {
        String sub = text.substring(len);
        return new Token(TokenType.of(sub), sub, line, position + len, index + len, fileName);
    }

    public void connect(Token t1) {
        Token t2 = t1.next;
        t2.previous = t1;
        t2.previousAll = t1;
        previous.setNext(t1);
        previousAll.setNextAll(t1);
        next.setPrevious(t2);
        nextAll.setPreviousAll(t2);
    }

    public Token prepend(String prefix) {
        String sup = prefix + text;
        Token merge = new Token(TokenType.of(sup), sup, line, position - prefix.length(), index - prefix.length(), fileName);
        merge.next = next;
        merge.nextAll = nextAll;
        return merge;
    }

    public void merge(Token merge) {
        merge.setPrevious(previous);
        merge.setPreviousAll(previousAll);
        previous.setNext(merge);
        previousAll.setNextAll(merge);
        merge.next.setPrevious(merge);
        merge.nextAll.setPreviousAll(merge);
    }

    public Token next() {
        return next;
    }

    public Token previous() {
        return previous;
    }

    public Token nextAll() {
        return nextAll;
    }

    public Token previousAll() {
        return previousAll;
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

    public int line() {
        return line;
    }

    public int lastLine() {
        return lastLine;
    }

    public int lineEnd() {
        return lineEnd;
    }

    public int numLines() {
        return numLines;
    }

    public int position() {
        return position;
    }

    public int lastPosition() {
        return lastPosition;
    }

    public int positionEnd() {
        return positionEnd;
    }

    public int numChars() {
        return numChars;
    }

    public int index() {
        return index;
    }

    public int indexEnd() {
        return indexEnd;
    }

    public boolean isKeyword() {
        return isKeyword;
    }

    public boolean contains(int l, int c) {
        if (numLines == 1) {
            // check within one liner:
            return line == l && position <= c && c < positionEnd;
        } else {
            // multi line:
            if (!(line <= l && l < lineEnd())) {
                // not within the token lines => not contained
                return false;
            } else if (l == line) {
                // on first line of the token => check column (this token extends to the end of the line)
                return position <= c;
            } else if (l == lastLine()) {
                // on last line of the token => check column (this token starts at the beginning of the line)
                return c < positionEnd;
            } else {
                // on some middle line of the token => always contained
                return true;
            }
        }
    }

    public String fileName() {
        return fileName;
    }

    public boolean skip() {
        return type.isSkip();
    }

    @Override
    public boolean equals(Object obj) {
        return obj == this || obj instanceof Token that//
                && Objects.equals(this.type, that.type)//
                && Objects.equals(this.text, that.text)//
                && this.line == that.line//
                && this.position == that.position//
                && this.index == that.index//
                && Objects.equals(this.fileName, that.fileName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, text, line, position, index, fileName);
    }

    @Override
    public String toString() {
        String textTraced = textTraced();
        return textTraced.isEmpty() && !type().isVariableContent() ? type().name() : "'" + textTraced + "'";
    }

    public String debug() {
        return String.format("%s:[%d:%d...%d:%d]:[#%d:#%d]:[%d:%d]:%s:%s", //
                fileName, //
                line + 1, position + 1, lastLine + 1, lastPosition + 1, //
                numLines, numChars, //
                lineEnd + 1, positionEnd + 1, //
                type, textTraced());
    }

    @Override
    public Token firstToken() {
        return this;
    }

    @Override
    public Token lastToken() {
        return this;
    }

    public List<Token> list(Token last) {
        List<Token> list = List.of(this);
        Token t = this;
        while (t != last) {
            t = t.next();
            list = list.add(t);
        }
        return list;
    }

    public List<Token> listAll(Token last) {
        List<Token> list = List.of(this);
        Token t = this;
        while (t != last) {
            t = t.nextAll();
            list = list.add(t);
        }
        return list;
    }

    public Node getNode() {
        return node;
    }

    public void setNode(Node node) {
        this.node = node;
    }

    public Pattern declaration() {
        if (node != null) {
            Functor functor = node.functor();
            if (functor != null) {
                return functor.declaration(this);
            }
        }
        return null;
    }

    public Variable variable() {
        Variable var = node != null ? node.variable() : null;
        if (var != null) {
            return var;
        }
        Pattern pattern = declaration();
        return pattern != null ? pattern.variable() : null;
    }

    public boolean isVariableNode() {
        return variable() != null;
    }

    public boolean isTypeNode() {
        return node instanceof Type;
    }

    public boolean isPatternNode() {
        return node instanceof Pattern;
    }

    public boolean isLitteralNode() {
        return node != null && Type.LITERAL.isAssignableFrom(node.type());
    }

    public void setKeyword() {
        isKeyword = true;
    }

    public TokenType colorType() {
        return isVariableNode() ? TokenType.VARIABLE : //
                isTypeNode() && !text().equals("::") ? TokenType.TYPE : //
                        type() == TokenType.NAME && isKeyword() && (isLitteralNode() || next().type() != TokenType.LEFT) ? TokenType.KEYWORD : //
                                (text().equals("<") || text().equals(">")) && isPatternNode() ? TokenType.META_OPERATOR : //
                                        type();
    }

    @Override
    public void deparse(StringBuffer sb) {
        sb.append(text);
    }

}
