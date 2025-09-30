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
import org.modelingvalue.nelumbo.syntax.Patterns;

public class AlternationPattern extends Pattern {
    @Serial
    private static final long serialVersionUID = -2652813935675033086L;

    public AlternationPattern(Type type, List<AstElement> elements, Object... args) {
        super(type, elements, args);
    }

    protected AlternationPattern(Object[] args) {
        super(args);
    }

    @Override
    protected AlternationPattern struct(Object[] array) {
        return new AlternationPattern(array);
    }

    @SuppressWarnings("unchecked")
    public List<Pattern> options() {
        return (List<Pattern>) get(0);
    }

    @Override
    public Patterns patterns(Patterns nextPatterns, NodeTypePattern left) {
        Patterns result = Patterns.EMPTY;
        for (Pattern option : options()) {
            result = result.merge(option.patterns(nextPatterns, left));
        }
        return result;
    }

    @Override
    public String toString() {
        String string = options().toString();
        return "a(" + string.substring(5, string.length() - 1) + ")";
    }

    @Override
    public Pattern setPresedence(List<Integer> precedence, int[] p) {
        List<Pattern> options = options();
        for (int i = 0; i < options.size(); i++) {
            Pattern pa = options.get(i);
            Pattern pb = pa.setPresedence(precedence, p);
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
    public int args(List<AstElement> elements, int i, Ref<List<Object>> args, boolean alt) {
        List<Object> pre = args.get();
        for (Pattern option : options()) {
            int ii = option.args(elements, i, args, true);
            if (ii >= 0) {
                return ii;
            } else {
                args.set(pre);
            }
        }
        return -1;
    }

    @Override
    public int string(List<Object> args, int i, Ref<String> string, boolean alt) {
        String pre = string.get();
        for (Pattern option : options()) {
            int ii = option.string(args, i, string, true);
            if (ii >= 0) {
                return ii;
            } else {
                string.set(pre);
            }
        }
        return -1;
    }

}
