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
import java.util.Optional;
import java.util.function.Function;

import org.modelingvalue.collections.List;
import org.modelingvalue.nelumbo.AstElement;
import org.modelingvalue.nelumbo.Node;
import org.modelingvalue.nelumbo.Type;
import org.modelingvalue.nelumbo.syntax.ParseState;
import org.modelingvalue.nelumbo.syntax.TokenType;

public class OptionalPattern extends Pattern {
    @Serial
    private static final long serialVersionUID = 3011113311569598643L;

    public OptionalPattern(Type type, List<AstElement> elements, Object... args) {
        super(type, elements, args);
    }

    protected OptionalPattern(Object[] args, OptionalPattern declaration) {
        super(args, declaration);
    }

    @Override
    protected OptionalPattern struct(Object[] array, Node declaration) {
        return new OptionalPattern(array, (OptionalPattern) declaration);
    }

    public Pattern optional() {
        return (Pattern) get(0);
    }

    @Override
    public ParseState state(ParseState next, NodeTypePattern left, Functor functor, List<Integer> branche) {
        return optional().state(next, left, functor, branche.add(0)).merge(next);
    }

    @Override
    public String toString(TokenType[] previous) {
        return "<(>" + optional() + "<)?>";
    }

    @Override
    public Pattern setPresedence(List<Integer> precedence, int[] p) {
        return set(0, optional().setPresedence(precedence, p));
    }

    @Override
    public Pattern setTypes(Function<Type, Type> typeFunction) {
        return set(0, optional().setTypes(typeFunction));
    }

    @Override
    public List<Type> argTypes(List<Type> types) {
        return optional().argTypes(types);
    }

    @Override
    protected List<Object> args(List<Object> args, ElementIterator it, List<Integer> branche, boolean alt) {
        if (it.match(branche)) {
            List<Object> inner = List.of();
            inner = optional().args(inner, it, branche.add(0), false);
            args = args.add(Optional.of(inner.first()));
        } else {
            args = args.add(Optional.empty());
        }
        return args;
    }

    @Override
    protected int string(List<Object> args, int ai, StringBuffer sb, TokenType[] previous, boolean alt) {
        if (args.get(ai) instanceof Optional<?> opt) {
            StringBuffer inner = new StringBuffer();
            if (opt.isPresent()) {
                int ii = optional().string(List.of(opt.get()), 0, inner, previous, false);
                if (ii < 0) {
                    return -1;
                }
            }
            sb.append(inner);
            return ai + 1;
        }
        return -1;
    }

}
