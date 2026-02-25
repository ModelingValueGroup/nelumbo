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
import org.modelingvalue.collections.mutable.MutableList;
import org.modelingvalue.nelumbo.AstElement;
import org.modelingvalue.nelumbo.Node;
import org.modelingvalue.nelumbo.Type;
import org.modelingvalue.nelumbo.Variable;
import org.modelingvalue.nelumbo.syntax.ParseState;
import org.modelingvalue.nelumbo.syntax.Token;
import org.modelingvalue.nelumbo.syntax.TokenType;

public class AlternationPattern extends Pattern {
    @Serial
    private static final long serialVersionUID = -2652813935675033086L;

    public AlternationPattern(Type type, List<AstElement> elements, List<Pattern> options) {
        super(type, elements, options);
    }

    protected AlternationPattern(Object[] args, AlternationPattern declaration) {
        super(args, declaration);
    }

    @Override
    protected AlternationPattern struct(Object[] array, Node declaration) {
        return new AlternationPattern(array, (AlternationPattern) declaration);
    }

    @SuppressWarnings("unchecked")
    public List<Pattern> options() {
        return (List<Pattern>) get(0);
    }

    @Override
    public ParseState state(ParseState next) {
        ParseState result = ParseState.EMPTY;
        for (Pattern option : options()) {
            result = result.merge(option.state(next));
        }
        return result;
    }

    @Override
    public String toString(TokenType[] previous) {
        return "<(>" + options().map(Object::toString).reduce("", (a, b) -> a.isEmpty() || b.isEmpty() ? a + b : a + "<|>" + b) + "<)>";
    }

    @Override
    public Pattern setPresedence(int precedence) {
        List<Pattern> options = options();
        for (int i = 0; i < options.size(); i++) {
            Pattern pa = options.get(i);
            Pattern pb = pa.setPresedence(precedence);
            if (!pb.equals(pa)) {
                options = options.replace(i, pb);
            }
        }
        return set(0, options);
    }

    @Override
    public Pattern setTypes(Function<Type, Type> typeFunction) {
        List<Pattern> options = options();
        for (int i = 0; i < options.size(); i++) {
            Pattern pa = options.get(i);
            Pattern pb = pa.setTypes(typeFunction);
            if (!pb.equals(pa)) {
                options = options.replace(i, pb);
            }
        }
        return set(0, options);
    }

    @Override
    public List<Type> argTypes(List<Type> types) {
        return types.add(Type.$OBJECT);
    }

    @Override
    protected int string(List<Object> args, int ai, StringBuffer sb, TokenType[] previous, boolean alt) {
        if (ai < 0 || args.size() <= ai) {
            return -1;
        }
        Object o = args.get(ai);
        StringBuffer inner = new StringBuffer();
        for (Pattern option : options()) {
            int ii = option.string(List.of(o), 0, inner, previous, true);
            if (ii < 0) {
                inner = new StringBuffer();
            } else {
                sb.append(inner);
                return ai + 1;
            }
        }
        return -1;
    }

    @Override
    protected int args(List<AstElement> elements, int i, MutableList<Object> args, boolean alt, Functor functor, Map<Variable, Type> typeArgs) {
        for (Pattern option : options()) {
            MutableList<Object> inner = MutableList.of(List.of());
            int ii = option.args(elements, i, inner, true, functor, typeArgs);
            if (ii >= 0) {
                args.addAll(inner);
                return ii;
            }
        }
        return -1;
    }

    @Override
    public Pattern declaration(Token token) {
        for (Pattern option : options()) {
            Pattern decl = option.declaration(token);
            if (decl != null) {
                return decl;
            }
        }
        return null;
    }

}
