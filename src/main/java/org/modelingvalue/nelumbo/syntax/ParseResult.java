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
import org.modelingvalue.nelumbo.AstElement;
import org.modelingvalue.nelumbo.Node;
import org.modelingvalue.nelumbo.patterns.Functor;

public final class ParseResult {

    private final MutableList<AstElement> elements;
    private final MutableList<Object>     args;

    private Functor                       functor;
    private Token                         token;
    private int                           pre  = 0;
    private int                           post = 0;

    public ParseResult() {
        elements = MutableList.of(List.of());
        args = MutableList.of(List.of());
    }

    public Functor functor() {
        return functor;
    }

    public Token nextToken() {
        return token;
    }

    public int precedence() {
        Integer precedence = functor.precedence();
        return precedence != null ? precedence : Integer.MAX_VALUE;
    }

    public void endPreParse(Functor functor, Token token) {
        this.functor = functor;
        this.token = token;
    }

    public List<AstElement> elements() {
        return elements.toImmutable();
    }

    public void add(Node node) {
        elements.add(node);
        args.add(node);
        if (functor == null) {
            pre++;
        }
    }

    public void add(Token token) {
        elements.add(token);
        if (functor == null) {
            pre++;
        }
    }

    public void add(String val) {
        args.add(val);
    }

    public List<Object> args() {
        return args.toImmutable();
    }

    public Node postParse(String group, Parser parser) throws ParseException {
        return functor.postParse(group, parser, this);
    }

    public boolean isDone() {
        return post++ < pre;
    }

    @Override
    public String toString() {
        return functor.toString() + args().toString().substring(4);
    }

}
