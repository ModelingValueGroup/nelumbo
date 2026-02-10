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
import org.modelingvalue.collections.mutable.MutableMap;
import org.modelingvalue.collections.util.Pair;
import org.modelingvalue.nelumbo.AstElement;
import org.modelingvalue.nelumbo.Node;
import org.modelingvalue.nelumbo.Type;
import org.modelingvalue.nelumbo.Variable;
import org.modelingvalue.nelumbo.patterns.Functor;
import org.modelingvalue.nelumbo.patterns.RepetitionPattern;

public final class PatternResult implements ParseExceptionHandler {

    private final MutableList<AstElement>         elements;
    private final Parser                          parser;
    private final ParseContext                    context;
    private final MutableList<Pair<Token, Token>> splitted;
    private final MutableList<Pair<Token, Token>> merged;
    private final MutableMap<Variable, Type>      typeArgs;

    private Functor                               functor;
    private ParseState                            state;
    private Integer                               leftPrecedence;
    private Set<RepetitionPattern>                endRepetitions;
    private Token                                 nextToken;
    private boolean                               hasLeft;

    public PatternResult(Parser parser, ParseContext context) {
        this.parser = parser;
        this.context = context;
        elements = MutableList.of(List.of());
        splitted = MutableList.of(List.of());
        merged = MutableList.of(List.of());
        typeArgs = MutableMap.of(Map.of());
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

    public int leftPrecedence() {
        return leftPrecedence;
    }

    public void endPostParse(Functor functor, Token nextToken, Integer leftPrecedence) {
        this.endRepetitions = Set.of();
        this.functor = functor;
        this.nextToken = nextToken;
        this.leftPrecedence = leftPrecedence;
        assert (functor != null);
        assert (!hasLeft || leftPrecedence != null);
    }

    public void endPreParse(ParseState state, Token nextToken, Integer leftPrecedence) {
        this.state = state;
        this.nextToken = nextToken;
        this.leftPrecedence = leftPrecedence;
        assert (!hasLeft || leftPrecedence != null);
    }

    public void endRepetition(Set<RepetitionPattern> endRepetitions, Token nextToken) {
        this.endRepetitions = endRepetitions;
        this.nextToken = nextToken;
    }

    public void startRepetition() {
        this.endRepetitions = Set.of();
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
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public Node postParse(ParseContext ctx) throws ParseException {
        ParseState next = state;
        if (next != null) {
            state = null;
            next.parse(nextToken, this, Map.of(), false);
        }
        if (functor != null) {
            for (Pair<Token, Token> split : splitted) {
                split.a().connect(split.b());
            }
            for (Pair<Token, Token> merge : merged) {
                merge.a().merge(merge.b());
            }
            List<AstElement> elements = elements();
            Map<Variable, Type> ta = typeArgs.toImmutable();
            Object[] args = functor.args(elements, ta);
            if (!ta.isEmpty()) {
                functor = functor.setBinding((Map) ta);
            }
            Node node = functor.construct(elements, args, this);
            if (hasLeft && args.length == 1 && args[0] instanceof Node arg) {
                if (node.functor().equals(arg.functor())) {
                    addException(new ParseException("Circular object construction, caused by " + functor, elements));
                    return null;
                }
            }
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
        parser.addException(exception);
    }

    @Override
    public List<ParseException> exceptions() {
        return parser.exceptions();
    }

    public void putTypeArg(Variable arg, Type val) {
        typeArgs.put(arg, val);
    }

    public Type getTypeArg(Variable arg) {
        return typeArgs.get(arg);
    }

}
