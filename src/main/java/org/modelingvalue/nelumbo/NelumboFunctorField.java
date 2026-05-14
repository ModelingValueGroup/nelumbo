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
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.modelingvalue.collections.List;
import org.modelingvalue.nelumbo.lang.Functor;
import org.modelingvalue.nelumbo.syntax.ParseException;

/**
 * Marks a field that is set through introspection (reflection) and therefore
 * appears unused in static code analysis. This annotation serves as
 * documentation that the field is intentionally present for reflective setting.
 * <p>
 * Fields marked with this annotation have the signature: static Functor FUNCTOR
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface NelumboFunctorField {
    public class Finder {

        private static final Map<Class<?>, Field> CACHE = new ConcurrentHashMap<>();

        public static Field find(Class<?> clazz, KnowledgeBase kb, List<AstElement> list) throws ParseException {
            Field field = CACHE.get(clazz);
            if (field == null) {
                try {
                    field = find(clazz);
                    if (field == null) {
                        return null;
                    }
                    field.setAccessible(true);
                    CACHE.put(clazz, field);
                } catch (SecurityException | NoSuchFieldException ex) {
                    kb.addException(
                            new ParseException(ex, ex + " during finding FUNCTOR field in " + clazz.getName(), list));
                }
            }
            return field;
        }

        private static Field find(Class<?> clazz) throws NoSuchFieldException {
            for (Field field : clazz.getDeclaredFields()) {
                if (field.isAnnotationPresent(NelumboFunctorField.class)) {
                    if (!Functor.class.isAssignableFrom(field.getType())) {
                        throw new NoSuchFieldException("@NelumboFunctorField on " + field.getDeclaringClass().getName()
                                + " is invalid: the type of the field must be " + Functor.class.getSimpleName());
                    }
                    if (!Modifier.isStatic(field.getModifiers())) {
                        throw new NoSuchFieldException("@NelumboFunctorField on " + field.getDeclaringClass().getName()
                                + " is invalid: it must be a static field");
                    }
                    return field;
                }
            }
            // throw new NoSuchFieldException("@NelumboFunctorField on " + clazz.getName() +
            // " is not found");
            return null;
        }
    }
}
