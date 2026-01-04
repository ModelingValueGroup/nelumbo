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

import org.modelingvalue.collections.List;
import org.modelingvalue.collections.Map;
import org.modelingvalue.collections.Set;
import org.modelingvalue.collections.mutable.MutableList;
import org.modelingvalue.collections.util.Pair;
import org.modelingvalue.nelumbo.AstElement;
import org.modelingvalue.nelumbo.Node;
import org.modelingvalue.nelumbo.Type;
import org.modelingvalue.nelumbo.patterns.Functor;
import org.modelingvalue.nelumbo.patterns.RepetitionPattern;

public final class PatternResult implements ParseExceptionHandler {

    private final MutableList<AstElement>         elements;
    private final Parser                          parser;
    private final ParseContext                    context;
    private final MutableList<Pair<Token, Token>> splitted;
    private final MutableList<Pair<Token, Token>> merged;

    private Functor                               functor;
    private ParseState                            state;
    private Set<RepetitionPattern>                endRepetitions;
    private Token                                 nextToken;
    private int                                   depth;
    private boolean                               hasLeft;
    private boolean                               hasException;

    public PatternResult(Parser parser, ParseContext context) {
        this.parser = parser;
        this.context = context;
        elements = MutableList.of(List.of());
        splitted = MutableList.of(List.of());
        merged = MutableList.of(List.of());
        endRepetitions = Set.of();
    }

    public Token addSplit(Token original, Token split) {
        splitted.add(Pair.of(original, split));
        return split;
    }

    public Token addMerge(Token original, Token merge) {
        merged.add(Pair.of(original, merge));
        return merge;
    }

    public Parser parser() {
        return parser;
    }

    public ParseContext context() {
        return context;
    }

    public Functor functor() {
        return functor;
    }

    public ParseState state() {
        return state;
    }

    public Set<RepetitionPattern> endRepetitions() {
        return endRepetitions;
    }

    public Token nextToken() {
        return nextToken;
    }

    public int depth() {
        return depth;
    }

    public int leftPrecedence() {
        return state != null ? state.leftPrecedence() : functor.left().leftPrecedence();
    }

    public void endPostParse(Functor functor, Token nextToken) {
        this.functor = functor;
        this.nextToken = nextToken;
    }

    public void endPreParse(ParseState state, Token nextToken) {
        this.state = state;
        this.nextToken = nextToken;
    }

    public void clearDepth() {
        depth = 0;
    }

    public void countDepth() {
        if (depth > 0) {
            depth++;
        }
    }

    public void endRepetition(Set<RepetitionPattern> endRepetitions, Token nextToken, int i) {
        this.endRepetitions = endRepetitions;
        this.nextToken = nextToken;
        this.depth += i;
    }

    public List<AstElement> elements() {
        return elements.toImmutable();
    }

    public boolean isEmpty() {
        int size = elements.size();
        return (hasLeft ? size - 1 : size) == 0;
    }

    public boolean hasLeft() {
        return hasLeft;
    }

    public void left(AstElement element) {
        elements.add(element);
        hasLeft = true;
    }

    public void add(AstElement element) {
        elements.add(element);
        if (depth > 0) {
            element.setCycleDepth(depth);
            depth = 0;
            endRepetitions = Set.of();
        }
    }

    public Node postParse(ParseContext ctx) throws ParseException {
        for (Pair<Token, Token> split : splitted) {
            split.a().connect(split.b());
        }
        for (Pair<Token, Token> merge : merged) {
            merge.a().merge(merge.b());
        }
        splitted.clear();
        merged.clear();
        if (state != null) {
            state.parse(nextToken, this, Map.of(), false);
        }
        if (functor != null) {
            List<AstElement> elements = elements();
            Node node = functor.construct(elements, functor.args(elements), this);
            if (Type.ROOT.isAssignableFrom(node.type())) {
                node.init(parser.knowledgeBase());
            }
            return node;
        }
        return null;
    }

    @Override
    public String toString() {
        return elements().toString().substring(4);
    }

    @Override
    public void addException(ParseException exception) throws ParseException {
        hasException = true;
        parser.addException(exception);
    }

    @Override
    public List<ParseException> exceptions() {
        return parser.exceptions();
    }

    public boolean hasException() {
        return hasException;
    }

}
