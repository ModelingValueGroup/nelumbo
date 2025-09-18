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

import org.modelingvalue.collections.List;
import org.modelingvalue.nelumbo.AstElement;
import org.modelingvalue.nelumbo.U;

@SuppressWarnings("ClassCanBeRecord")
public class Token implements AstElement {

    private final TokenType type;
    private final String    text;
    private final int       line;       // line number in the input file (0-based)
    private final int       position;   // position (column) in the line (0-based)
    private final int       index;      // position in the input stream (0-based)
    private final String    fileName;

    private Token           next;
    private Token           previous;

    private Token           nextAll;
    private Token           previousAll;

    public Token(TokenType type, String text, int line, int position, int index, String fileName) {
        if (type == null) {
            throw new NullPointerException("type can not be null");
        }
        if (text == null) {
            throw new NullPointerException("text can not be null");
        }
        this.type = type;
        this.text = text;
        this.line = line;
        this.position = position;
        this.index = index;
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

    public int position() {
        return position;
    }

    public int index() {
        return index;
    }

    public String fileName() {
        return fileName;
    }

    public boolean skip() {
        return type.skip();
    }

    public Token split(int i) {
        Token t1 = splitGet1(i);
        Token t2 = splitGet2(i);
        t1.setPrevious(previous());
        t1.setPreviousAll(previousAll());
        t2.setNext(next());
        t2.setNextAll(nextAll());
        t1.setNext(t2);
        t1.setNextAll(t2);
        return t1;
    }

    private Token splitGet1(int len) {
        return new Token(type, text.substring(0, len), line, position, index, fileName);
    }

    private Token splitGet2(int len) {
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
        return "'" + textTraced() + "'";
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
        Token t = next();
        for (; t != last; t = t.next()) {
            list = list.add(t);
        }
        return list.add(t);
    }

    public List<Token> listAll(Token last) {
        List<Token> list = List.of(this);
        Token t = nextAll();
        for (; t != last; t = t.nextAll()) {
            list = list.add(t);
        }
        return list.add(t);
    }
}
