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

package org.modelingvalue.nelumbo;

import org.modelingvalue.collections.Entry;
import org.modelingvalue.collections.Map;
import org.modelingvalue.collections.mutable.MutableMap;
import org.modelingvalue.collections.util.Mergeable;
import org.modelingvalue.nelumbo.lang.Type;
import org.modelingvalue.nelumbo.lang.Variable;

@SuppressWarnings("rawtypes")
public abstract class AbstractState<S extends AbstractState> implements Mergeable<S> {

    private final TypeMatcher typeMatcher;

    protected AbstractState(TypeMatcher typeMatcher) {
        this.typeMatcher = typeMatcher;
    }

    protected final TypeMatcher typeMatcher() {
        return typeMatcher;
    }

    @SuppressWarnings("unchecked")
    public S matchType(Type type, MutableMap<Variable, Type> typeArgs) {
        S s = null;
        for (Type m : typeMatcher().match(type, typeArgs)) {
            if (s == null) {
                s = typeTransitions().get(m);
            } else {
                s = (S) s.merge(typeTransitions().get(m));
            }
        }
        return s;
    }

    protected abstract Map<Object, S> typeTransitions();

    @SuppressWarnings("unchecked")
    protected <K> Map<K, S> inherit(Map<K, S> transitions) {
        for (Object key : transitions.toKeys()) {
            if (key instanceof Type subType) {
                for (Entry<Type, Type> entry : subType.allSupersList()) {
                    Type superType = entry.getKey();
                    if (!superType.equals(subType)) {
                        S superState = transitions.get((K) superType);
                        if (superState != null) {
                            S subState = transitions.get((K) subType);
                            S mergedState = (S) subState.merge(superState);
                            transitions = transitions.put((K) subType, mergedState);
                        }
                    }
                }
            }
        }
        return transitions;
    }

    public abstract S merge(S merged);

}
