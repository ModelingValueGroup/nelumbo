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
import org.modelingvalue.nelumbo.Type;
import org.modelingvalue.nelumbo.syntax.ParseState;

public class RepetitionPattern extends Pattern {
    @Serial
    private static final long serialVersionUID = 7257418785045060245L;

    public RepetitionPattern(Type type, List<AstElement> elements, Object... args) {
        super(type, elements, args);
    }

    protected RepetitionPattern(Object[] args) {
        super(args);
    }

    @Override
    protected RepetitionPattern struct(Object[] array) {
        return new RepetitionPattern(array);
    }

    public Pattern repeated() {
        return (Pattern) get(0);
    }

    @Override
    public String toString() {
        return "<{>" + repeated() + "<}>";
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
        Integer leftPrecedence = left != null ? left.leftPrecedence() : null;
        return repeated().state(new ParseState(this).merge(next), left, functor, branche.add(0)).//
                merge(new ParseState(this, leftPrecedence)).merge(next);
    }

    @Override
    public List<Type> argTypes(List<Type> types) {
        return types.add(repeated().argTypes(List.of()).first().list());
    }

    @SuppressWarnings("unchecked")
    @Override
    protected List<Object> args(List<Object> args, ElementIterator it, List<Integer> branche, boolean alt) {
        List<Object> inner = List.of();
        Pattern repeated = repeated();
        while (it.match(branche)) {
            inner = repeated.args(inner, it, branche.add(0), false);
        }
        return args.add(inner);
    }

    @Override
    protected int string(List<Object> args, int ai, StringBuffer sb, boolean alt) {
        if (args.get(ai) instanceof List<?> list) {
            StringBuffer inner = new StringBuffer();
            for (Object o : list) {
                int ii = repeated().string(List.of(o), 0, inner, false);
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
