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

import org.modelingvalue.nelumbo.syntax.ParseException;

/**
 * Functional interface for resolving import names to KnowledgeBases.
 * Implementations can handle different types of imports (classpath resources, editor windows, etc.).
 */
@FunctionalInterface
public interface ImportResolver {

    /**
     * Result of an import resolution attempt.
     *
     * @param knowledgeBase the resolved KnowledgeBase, or null if not found
     * @param cacheable     whether the result can be cached (false for dynamic sources like editor windows)
     */
    record ImportResult(KnowledgeBase knowledgeBase, boolean cacheable) {}

    /**
     * Attempts to resolve the given import name to a KnowledgeBase.
     *
     * @param name the import name to resolve
     *
     * @return the import result, or null if this resolver cannot handle the name
     *
     * @throws ParseException if resolution fails
     */
    ImportResult resolve(String name, Import imp) throws ParseException;

    /**
     * Returns true if this resolver can potentially handle the given import name.
     * This is a quick check that can be used to skip resolvers that won't match.
     * The default implementation returns true, meaning the resolver will always be tried.
     *
     * @param name the import name to check
     * @return true if this resolver might be able to resolve the name
     */
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    default boolean canHandle(String name) {
        return true;
    }
}
