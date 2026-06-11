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
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

import org.modelingvalue.collections.List;
import org.modelingvalue.collections.util.Pair;
import org.modelingvalue.nelumbo.syntax.ParseException;

/**
 * Marks a method that is through introspection (reflection) and therefore
 * appears unused in static code analysis. This annotation serves as
 * documentation that the field is intentionally present for reflective usage.
 * <p>
 * Methods marked with this annotation have the signature that corresponds with
 * the Functor declaration in Nelumbo files.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface NelumboMethod {
    public class Finder {

        private static final Map<Pair<Class<?>, String>, Object> CACHE          = new ConcurrentHashMap<>();
        private static final Pattern                             METHOD_PATTERN = Pattern
                .compile("[a-zA-Z_][0-9a-zA-Z_]*\\(\\,*\\)");
        private static final Object                              NULL           = new Object();

        public static Method find(Class<?> clazz, String signature, KnowledgeBase kb, List<AstElement> list)
                throws ParseException {
            if (!METHOD_PATTERN.matcher(signature).matches()) {
                return null;
            }
            Pair<Class<?>, String> key = Pair.of(clazz, signature);
            Object val = CACHE.get(key);
            if (val != null) {
                return val instanceof Method method ? method : null;
            }
            Method method = null;
            try {
                method = find(clazz, signature);
                if (method != null) {
                    method.setAccessible(true);
                    CACHE.put(key, method);
                } else {
                    CACHE.put(key, NULL);
                }
            } catch (SecurityException | NoSuchMethodException ex) {
                kb.addException(
                        new ParseException(ex, ex + " during finding FUNCTOR field in " + clazz.getName(), list));
            }
            return method;
        }

        private static Method find(Class<?> clazz, String signature) throws NoSuchMethodException {
            String name = signature.substring(0, signature.indexOf('('));
            int nrOfArgs = signature.split(",", 0).length;
            for (Method method : clazz.getDeclaredMethods()) {
                if (method.isAnnotationPresent(NelumboMethod.class) && method.getName().equals(name)
                        && method.getParameterCount() == nrOfArgs) {
                    return method;
                }
            }
            return null;
        }
    }
}
