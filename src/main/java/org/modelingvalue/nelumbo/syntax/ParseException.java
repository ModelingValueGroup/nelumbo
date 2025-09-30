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

import java.io.Serial;

import org.modelingvalue.collections.List;
import org.modelingvalue.nelumbo.AstElement;

public class ParseException extends Exception {
    @Serial
    private static final long serialVersionUID = -8359192414582977261L;

    private final int         line;
    private final int         position;
    private final int         index;
    private final int         length;
    private final String      fileName;

    public ParseException(String s, AstElement... elements) {
        this(null, s, elements);
    }

    public ParseException(Throwable cause, String s, AstElement... elements) {
        this(cause, s, elements[0].firstToken(), elements[elements.length - 1].lastToken());
    }

    public ParseException(String s, List<Token> tokens) {
        this(null, s, tokens.first(), tokens.last());
    }

    private ParseException(Throwable cause, String s, Token firstToken, Token lastToken) {
        this(cause, //
                s, //
                firstToken.line(), //
                firstToken.position(), //
                firstToken.index(), //
                lastToken.position() - firstToken.position() + lastToken.text().length(), //
                firstToken.fileName()//
        );
    }

    public ParseException(String message, String fileName) {
        this(null, message, fileName);
    }

    public ParseException(Throwable cause, String message, String fileName) {
        this(cause, message, 0, 0, 0, 0, fileName);
    }

    public ParseException(Throwable cause, String message, int line, int position, int index, int length, String fileName) {
        super(message, cause);
        this.line = line;
        this.position = position;
        this.index = index;
        this.length = length;
        this.fileName = fileName;
    }

    @Override
    public String getMessage() {
        return getShortMessage() + ", line=" + (line + 1) + ", position=" + (position + 1) + ", file=" + fileName;
    }

    public String getShortMessage() {
        return super.getMessage();
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

    public int length() {
        return length;
    }

    @SuppressWarnings("unused")
    public String fileName() {
        return fileName;
    }
}
