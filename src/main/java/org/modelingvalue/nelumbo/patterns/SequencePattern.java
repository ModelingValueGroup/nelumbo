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

package org.modelingvalue.nelumbo.patterns;

import java.io.Serial;
import java.util.function.Function;

import org.modelingvalue.collections.List;
import org.modelingvalue.nelumbo.AstElement;
import org.modelingvalue.nelumbo.Node;
import org.modelingvalue.nelumbo.Type;
import org.modelingvalue.nelumbo.syntax.ParseState;
import org.modelingvalue.nelumbo.syntax.Token;
import org.modelingvalue.nelumbo.syntax.TokenType;

public class SequencePattern extends Pattern {
    @Serial
    private static final long serialVersionUID = 1477171023667359130L;

    public SequencePattern(Type type, List<AstElement> elements, Object... args) {
        super(type, elements, args);
    }

    protected SequencePattern(Object[] args, SequencePattern declaration) {
        super(args, declaration);
    }

    @Override
    protected SequencePattern struct(Object[] array, Node declaration) {
        return new SequencePattern(array, (SequencePattern) declaration);
    }

    @SuppressWarnings("unchecked")
    public List<Pattern> elements() {
        return (List<Pattern>) get(0);
    }

    @Override
    public String name() {
        String name = "";
        for (Pattern element : elements()) {
            name += element.name();
        }
        return name;
    }

    @Override
    public String toString(TokenType[] previous) {
        return elements().map(Object::toString).reduce("", (a, b) -> a + b);
    }

    @Override
    public Pattern setPresedence(List<Integer> precedence, int[] p) {
        List<Pattern> elements = elements();
        for (int i = 0; i < elements.size(); i++) {
            Pattern pa = elements.get(i);
            Pattern pb = pa.setPresedence(precedence, p);
            if (!pb.equals(pa)) {
                elements = elements.replace(i, pb);
            }
        }
        return set(0, elements);
    }

    @Override
    public Pattern setTypes(Function<Type, Type> typeFunction) {
        List<Pattern> elements = elements();
        for (int i = 0; i < elements.size(); i++) {
            Pattern pa = elements.get(i);
            Pattern pb = pa.setTypes(typeFunction);
            if (!pb.equals(pa)) {
                elements = elements.replace(i, pb);
            }
        }
        return set(0, elements);
    }

    @Override
    public ParseState state(ParseState state, NodeTypePattern left, Functor functor, List<Integer> branche) {
        int i = elements().size();
        for (Pattern element : elements().reverse()) {
            state = element.state(state, left, functor, branche.add(--i));
        }
        return state;
    }

    @Override
    public List<Type> argTypes(List<Type> types) {
        for (Pattern element : elements()) {
            types = element.argTypes(types);
        }
        return types;
    }

    @SuppressWarnings("unchecked")
    @Override
    protected List<Object> args(List<Object> args, ElementIterator it, List<Integer> branche, boolean alt) {
        List<Pattern> parts = elements();
        List<Object> inner = List.of();
        for (int i = 0; i < parts.size(); i++) {
            inner = parts.get(i).args(inner, it, branche.add(i), false);
        }
        return args.add(inner.size() > 1 ? inner : inner.first());
    }

    @SuppressWarnings("unchecked")
    @Override
    protected int string(List<Object> args, int ai, StringBuffer sb, TokenType[] previous, boolean alt) {
        if (argTypes(List.of()).size() == 1) {
            args = List.of(List.of(args.get(ai)));
        }
        if (args.get(ai) instanceof List list) {
            StringBuffer inner = new StringBuffer();
            int ii = 0;
            for (Pattern element : elements()) {
                ii = element.string(list, ii, inner, previous, false);
                if (ii < 0) {
                    return -1;
                }
            }
            sb.append(inner);
            return ai + 1;
        }
        return -1;
    }

    @Override
    public Pattern declaration(Token token) {
        for (Pattern element : elements()) {
            Pattern decl = element.declaration(token);
            if (decl != null) {
                return decl;
            }
        }
        return null;
    }

}
