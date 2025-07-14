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

public class ParseException extends Exception {
    private static final long serialVersionUID = -8359192414582977261L;

    private final int         line;
    private final int         position;
    private final int         index;
    private final int         length;
    private final String      fileName;

    public ParseException(String s, Token token) {
        this(s, token.line(), token.position(), token.index(), token.text().length(), token.fileName());
    }

    public ParseException(String s, Token token, Token last) {
        this(s, token.line(), token.position(), token.index(), last.position() - token.position() + last.text().length(), token.fileName());
    }

    public ParseException(String message, int line, int position, int index, int length, String fileName) {
        super(message);
        this.line = line;
        this.position = position;
        this.index = index;
        this.length = length;
        this.fileName = fileName;
    }

    @Override
    public String getMessage() {
        return getShortMessage() + ", line=" + line + ", position=" + position + ", file=" + fileName;
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

    public String fileName() {
        return fileName;
    }

}
