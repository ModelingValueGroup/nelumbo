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

import org.modelingvalue.collections.List;
import org.modelingvalue.nelumbo.AstElement;
import org.modelingvalue.nelumbo.Type;
import org.modelingvalue.nelumbo.syntax.Patterns;

public class OptionalPattern extends Pattern {
    @Serial
    private static final long serialVersionUID = 3011113311569598643L;

    public OptionalPattern(Type type, List<AstElement> elements, Object... args) {
        super(type, elements, args);
    }

    protected OptionalPattern(Object[] args) {
        super(args);
    }

    @Override
    protected OptionalPattern struct(Object[] array) {
        return new OptionalPattern(array);
    }

    public Pattern optional() {
        return (Pattern) get(0);
    }

    @Override
    public List<Type> argTypes() {
        return optional().argTypes();
    }

    @Override
    public Patterns patterns(Patterns nextPatterns, NodeTypePattern left) {
        return optional().patterns(nextPatterns, left).merge(nextPatterns);
    }

    @Override
    public String toString() {
        return "o(" + optional() + ")";
    }

    @Override
    public Pattern setPresedence(List<Integer> precedence, int[] p) {
        return set(0, optional().setPresedence(precedence, p));
    }

    @Override
    public int args(List<AstElement> elements, int i, Ref<List<Object>> args, boolean alt) {
        if (i == elements.size()) {
            args.set(args.get().add(List.of()));
            return i;
        }
        Ref<List<Object>> ref = new Ref<>(List.of());
        int ii = optional().args(elements, i, ref, alt);
        if (ii < 0) {
            args.set(args.get().add(List.of()));
            return i;
        }
        args.set(args.get().add(ref.get()));
        return ii;
    }

    @Override
    public int string(List<Object> args, int i, Ref<String> string, boolean alt) {
        if (i == args.size()) {
            return i;
        }
        Ref<String> ref = new Ref<>("");
        int ii = optional().string(args, i, ref, alt);
        if (ii < 0) {
            return i;
        }
        string.set(string.get() + ref.get());
        return ii;
    }

}
