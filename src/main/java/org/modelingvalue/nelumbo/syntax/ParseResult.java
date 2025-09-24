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

import org.modelingvalue.collections.List;
import org.modelingvalue.collections.mutable.MutableList;
import org.modelingvalue.nelumbo.AstElement;
import org.modelingvalue.nelumbo.Node;
import org.modelingvalue.nelumbo.patterns.Functor;
import org.modelingvalue.nelumbo.patterns.RepetitionPattern;

public final class ParseResult {

    private final MutableList<AstElement> elements;
    private final MutableList<Object>     args;

    private Functor                       functor;
    private Patterns                      patterns;
    private RepetitionPattern             endRepetition;
    private Token                         nextToken;

    public ParseResult() {
        elements = MutableList.of(List.of());
        args = MutableList.of(List.of());
    }

    public Functor functor() {
        return functor;
    }

    public Patterns patterns() {
        return patterns;
    }

    public RepetitionPattern endRepetition() {
        return endRepetition;
    }

    public Token nextToken() {
        return nextToken;
    }

    public int leftPrecedence() {
        return patterns != null ? patterns.leftPrecedence() : functor.left().leftPrecedence();
    }

    public void endPostParse(Functor functor, Token nextToken) {
        this.functor = functor;
        this.nextToken = nextToken;
    }

    public void endPreParse(Patterns patterns, Token nextToken) {
        this.patterns = patterns;
        this.nextToken = nextToken;
    }

    public void endRepetition(RepetitionPattern endRepetition, Token nextToken) {
        this.endRepetition = endRepetition;
        this.nextToken = nextToken;
    }

    public List<AstElement> elements() {
        return elements.toImmutable();
    }

    public void add(Node node) {
        elements.add(node);
        args.add(node);
    }

    public void add(Token token) {
        elements.add(token);
    }

    public void add(String val) {
        args.add(val);
    }

    public List<Object> args() {
        return args.toImmutable();
    }

    public Node postParse(Parser parser) throws ParseException {
        if (patterns != null) {
            patterns.parse(nextToken, this, parser, false);
        }
        return functor.construct(elements(), args().toArray());
    }

    @Override
    public String toString() {
        return elements().toString().substring(4);
    }

}
