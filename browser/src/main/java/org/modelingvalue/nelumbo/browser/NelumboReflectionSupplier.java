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

package org.modelingvalue.nelumbo.browser;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.teavm.classlib.ReflectionContext;
import org.teavm.classlib.ReflectionSupplier;
import org.teavm.model.ClassReader;
import org.teavm.model.FieldReader;
import org.teavm.model.MethodDescriptor;
import org.teavm.model.MethodReader;

/**
 * Build-time SPI (discovered by the TeaVM compiler via META-INF/services): exposes all members of
 * the {@link ReflectionKeep} classes to runtime reflection, so the {@code @NelumboConstructor}/
 * {@code @NelumboMethod}/{@code @NelumboFunctorField} finders and {@code Class.forName} keep
 * working in the browser. Never part of the compiled JS itself.
 */
public class NelumboReflectionSupplier implements ReflectionSupplier {

    private static final Set<String> KEEP = List.of(ReflectionKeep.CLASSES).stream()
            .map(Class::getName)
            .collect(Collectors.toUnmodifiableSet());

    @Override
    public Collection<MethodDescriptor> getAccessibleMethods(ReflectionContext context, String className) {
        if (!KEEP.contains(className)) {
            return List.of();
        }
        ClassReader cls = context.getClassSource().get(className);
        if (cls == null) {
            return List.of();
        }
        List<MethodDescriptor> result = new ArrayList<>();
        for (MethodReader method : cls.getMethods()) {
            result.add(method.getDescriptor());
        }
        return result;
    }

    @Override
    public Collection<String> getAccessibleFields(ReflectionContext context, String className) {
        if (!KEEP.contains(className)) {
            return List.of();
        }
        ClassReader cls = context.getClassSource().get(className);
        if (cls == null) {
            return List.of();
        }
        List<String> result = new ArrayList<>();
        for (FieldReader field : cls.getFields()) {
            result.add(field.getName());
        }
        return result;
    }

    @Override
    public boolean isClassFoundByName(ReflectionContext context, String name) {
        return KEEP.contains(name);
    }
}
