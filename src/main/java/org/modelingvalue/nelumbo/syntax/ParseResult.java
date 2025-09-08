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

import org.modelingvalue.collections.List;
import org.modelingvalue.collections.mutable.MutableList;
import org.modelingvalue.nelumbo.Node;
import org.modelingvalue.nelumbo.Type;
import org.modelingvalue.nelumbo.patterns.SyntaxPattern;

public final class ParseResult {

    private final MutableList<Token>  tokens;
    private final MutableList<Object> args;

    private SyntaxPattern             pattern;
    private int                       pre  = 0;
    private int                       post = 0;

    public ParseResult() {
        tokens = MutableList.of(List.of());
        args = MutableList.of(List.of());
    }

    public SyntaxPattern pattern() {
        return pattern;
    }

    public int precedence() {
        Integer precedence = pattern.precedence();
        return precedence != null ? precedence : Integer.MAX_VALUE;
    }

    public void setPattern(SyntaxPattern pattern) {
        this.pattern = pattern;
    }

    public List<Token> tokens() {
        return tokens.toImmutable();
    }

    public void add(Node node) {
        args.add(node);
        if (pattern == null) {
            pre++;
        }
    }

    public void add(Token token) {
        tokens.add(token);
        if (pattern == null) {
            pre++;
        }
    }

    public void add(String val) {
        args.add(val);
    }

    public List<Object> args() {
        return args.toImmutable();
    }

    public Node postParse(Type expected, Parser parser) throws ParseException {
        return pattern.postParse(expected, parser, this);
    }

    public boolean isDone() {
        return post++ < pre;
    }

}
