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

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Constructor;
import java.util.Arrays;

import org.modelingvalue.collections.List;
import org.modelingvalue.nelumbo.patterns.Functor;

/**
 * Marks a constructor that is called through introspection (reflection) and therefore
 * appears unused in static code analysis. This annotation serves as documentation that
 * the constructor is intentionally present for reflective instantiation of Node subclasses.
 * <p>
 * Constructors marked with this annotation have the signature:<br>
 * {@code (Functor, List<AstElement>, Object[])}
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.CONSTRUCTOR)
@SuppressWarnings("unused")
public @interface NelumboConstructor {
    class Finder {
        private static final Class<?>[] EXPECTED_PARAMS = {Functor.class, List.class, Object[].class};

        @SuppressWarnings("unchecked")
        static Constructor<? extends Node> find(String className) throws ClassNotFoundException {
            Class<?> clazz = Class.forName(className);
            for (Constructor<?> c : clazz.getConstructors()) {
                if (c.isAnnotationPresent(NelumboConstructor.class)) {
                    assert Node.class.isAssignableFrom(c.getDeclaringClass()) //
                            : "@NelumboConstructor on " + c.getDeclaringClass().getName() + " is invalid: class must extend " + Node.class.getSimpleName();
                    assert Arrays.equals(c.getParameterTypes(), EXPECTED_PARAMS) //
                            : "@NelumboConstructor on " + c.getDeclaringClass().getName() + " has wrong signature: " + Arrays.toString(c.getParameterTypes()) + ", expected: " + Arrays.toString(EXPECTED_PARAMS);
                    return (Constructor<? extends Node>) c;
                }
            }
            return null;
        }
    }
}
