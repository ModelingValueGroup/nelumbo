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

package org.modelingvalue.nelumbo.patterns;

import java.io.Serial;
import java.util.function.Function;

import org.modelingvalue.collections.List;
import org.modelingvalue.nelumbo.AstElement;
import org.modelingvalue.nelumbo.Node;
import org.modelingvalue.nelumbo.Type;
import org.modelingvalue.nelumbo.syntax.ParseState;
import org.modelingvalue.nelumbo.syntax.TokenType;

public class RepetitionPattern extends Pattern {
    @Serial
    private static final long serialVersionUID = 7257418785045060245L;

    public RepetitionPattern(Type type, List<AstElement> elements, Object... args) {
        super(type, elements, args);
    }

    protected RepetitionPattern(Object[] args, RepetitionPattern declaration) {
        super(args, declaration);
    }

    @Override
    protected RepetitionPattern struct(Object[] array, Node declaration) {
        return new RepetitionPattern(array, (RepetitionPattern) declaration);
    }

    public Pattern repeated() {
        return (Pattern) get(0);
    }

    public boolean mandatory() {
        return (Boolean) get(1);
    }

    public Pattern separator() {
        return (Pattern) get(2);
    }

    @Override
    public String toString(TokenType[] previous) {
        Pattern separator = separator();
        return "<(>" + repeated() + (separator != null ? "<,>" + separator : "") + (mandatory() ? "<)+>" : "<)*>");
    }

    @Override
    public Pattern setPresedence(List<Integer> precedence, int[] p) {
        return set(0, repeated().setPresedence(precedence, p));
    }

    @Override
    public Pattern setTypes(Function<Type, Type> typeFunction) {
        return set(0, repeated().setTypes(typeFunction));
    }

    @Override
    public ParseState state(ParseState next, NodeTypePattern left, Functor functor, List<Integer> branche) {
        ParseState start = new ParseState(this, left != null ? left.leftPrecedence() : null);
        ParseState end = new ParseState(this);
        Pattern separator = separator();
        if (separator != null) {
            end = separator.state(end, left, functor, branche.add(1));
        }
        end = end.merge(next);
        ParseState state = repeated().state(end, left, functor, branche.add(0)).merge(start);
        return mandatory() ? state : state.merge(next);
    }

    @Override
    public List<Type> argTypes(List<Type> types) {
        return types.add(repeated().argTypes(List.of()).first().list());
    }

    @SuppressWarnings("unchecked")
    @Override
    protected List<Object> args(List<Object> args, ElementIterator it, List<Integer> branche, boolean alt) {
        Pattern repeated = repeated();
        Pattern separator = separator();
        List<Object> inner = List.of();
        while (it.match(branche)) {
            if (separator != null && !inner.isEmpty()) {
                inner = separator.args(inner, it, branche.add(1), false);
            }
            inner = repeated.args(inner, it, branche.add(0), false);
        }
        return args.add(inner);
    }

    @Override
    protected int string(List<Object> args, int ai, StringBuffer sb, TokenType[] previous, boolean alt) {
        if (args.get(ai) instanceof List<?> list) {
            Pattern separator = separator();
            StringBuffer inner = new StringBuffer();
            for (Object o : list) {
                if (separator != null && !inner.isEmpty()) {
                    if (separator.string(List.of(o), 0, inner, previous, false) < 0) {
                        return -1;
                    }
                }
                if (repeated().string(List.of(o), 0, inner, previous, false) < 0) {
                    return -1;
                }
            }
            sb.append(inner);
            return ai + 1;
        }
        return -1;
    }

}
