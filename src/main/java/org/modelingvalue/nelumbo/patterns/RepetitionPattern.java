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
import org.modelingvalue.collections.Map;
import org.modelingvalue.collections.Set;
import org.modelingvalue.collections.mutable.MutableList;
import org.modelingvalue.nelumbo.AstElement;
import org.modelingvalue.nelumbo.Node;
import org.modelingvalue.nelumbo.Type;
import org.modelingvalue.nelumbo.Variable;
import org.modelingvalue.nelumbo.syntax.ParseState;
import org.modelingvalue.nelumbo.syntax.Token;
import org.modelingvalue.nelumbo.syntax.TokenType;

public class RepetitionPattern extends Pattern {
    @Serial
    private static final long serialVersionUID = 7257418785045060245L;

    public RepetitionPattern(Type type, List<AstElement> elements, Pattern repeated, boolean mandatory, Pattern separator) {
        super(type, elements, repeated, mandatory, separator);
    }

    protected RepetitionPattern(Object[] args, List<AstElement> elements, RepetitionPattern declaration) {
        super(args, elements, declaration);
    }

    @Override
    protected RepetitionPattern struct(Object[] array, List<AstElement> elements, Node declaration) {
        return new RepetitionPattern(array, elements, (RepetitionPattern) declaration);
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
    public Pattern setPresedence(int precedence) {
        return set(0, repeated().setPresedence(precedence));
    }

    @Override
    public Pattern setTypes(Function<Type, Type> typeFunction) {
        return set(0, repeated().setTypes(typeFunction));
    }

    @Override
    public ParseState state(ParseState next) {
        Pattern repeated = repeated();
        boolean mandatory = mandatory();
        Pattern separator = separator();
        ParseState startOrNext = new ParseState(Set.of(this), Set.of()).merge(next);
        ParseState repeatedEnd = repeated.state(new ParseState(Set.of(), Set.of(this)));
        ParseState state;
        if (separator == null) {
            state = repeatedEnd.merge(startOrNext);
            if (mandatory) {
                state = repeated.state(state);
            }
        } else {
            state = separator.state(repeatedEnd).merge(startOrNext);
            state = repeated.state(state);
            if (!mandatory) {
                state = state.merge(next);
            }
        }
        return state;
    }

    @Override
    public List<Type> argTypes(List<Type> types) {
        return repeated().argTypes(types);
        // return types.add(repeated().argTypes(List.of()).first().list());
    }

    @Override
    protected int string(List<Object> args, int ai, StringBuffer sb, TokenType[] previous, boolean alt) {
        if (ai < 0 || args.size() <= ai) {
            return -1;
        }
        if (args.get(ai) instanceof List<?> list) {
            Pattern repeated = repeated();
            Pattern separator = separator();
            StringBuffer inner = new StringBuffer();
            for (Object o : list) {
                if (separator != null && !inner.isEmpty()) {
                    if (separator.string(List.of(o), 0, inner, previous, false) < 0) {
                        return -1;
                    }
                }
                if (repeated.string(List.of(o), 0, inner, previous, false) < 0) {
                    return -1;
                }
            }
            sb.append(inner);
            return ai + 1;
        }
        return -1;
    }

    @Override
    protected int args(List<AstElement> elements, int i, MutableList<Object> args, boolean alt, Functor functor, Map<Variable, Type> typeArgs) {
        Pattern repeated = repeated();
        Pattern separator = separator();
        boolean mandatory = mandatory();
        List<Object> result = List.of();
        while (true) {
            MutableList<Object> inner = MutableList.of(List.of());
            int ii = repeated.args(elements, i, inner, false, functor, typeArgs);
            if (ii >= 0) {
                result = result.addAll(inner.toImmutable());
                i = ii;
                mandatory = false;
            } else if (mandatory) {
                return -1;
            } else {
                break;
            }
            if (separator != null) {
                inner = MutableList.of(List.of());
                ii = separator.args(elements, i, inner, false, functor, typeArgs);
                if (ii >= 0) {
                    mandatory = true;
                    i = ii;
                } else {
                    break;
                }
            }
        }
        args.add(result);
        return i;

    }

    @Override
    public Pattern declaration(Token token) {
        Pattern decl = repeated().declaration(token);
        if (decl != null) {
            return decl;
        }
        Pattern separator = separator();
        decl = separator != null ? separator.declaration(token) : null;
        return decl;
    }

}
