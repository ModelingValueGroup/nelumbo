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
import org.modelingvalue.nelumbo.syntax.Parser;

/**
 * Import resolver that loads .nl files from the classpath.
 * Handles the "nelumbo.X" naming convention by expanding to "org.modelingvalue.nelumbo.X.X".
 */
public class ResourceImportResolver implements ImportResolver {

    public static final String NELUMBO_PREFIX           = "nelumbo.";
    public static final String ORG_MODELINGVALUE_PREFIX = "org.modelingvalue.";

    @Override
    public ImportResult resolve(String name, Import imp) throws ParseException {
        String path = getResourcePath(name);
        if (path == null) {
            return null;  // Let another resolver try
        }
        ParseException[] exc = new ParseException[1];
        KnowledgeBase kb = KnowledgeBase.BASE.run(() -> {
            try {
                Parser.parse(KnowledgeBase.class, path);
            } catch (ParseException e) {
                exc[0] = e;
            }
        });

        if (exc[0] != null) {
            throw exc[0];
        }
        return new ImportResult(kb, true);  // Classpath resources are cacheable
    }

    @Override
    public boolean canHandle(String name) {
        // Don't handle editor imports
        return name.startsWith(NELUMBO_PREFIX) || getResourcePath(name) != null;
    }

    private static String getResourcePath(String name) {
        String resolvedName = name;

        // Handle "nelumbo.X" -> "org.modelingvalue.nelumbo.X.X" expansion
        if (name.startsWith(NELUMBO_PREFIX)) {
            int i = name.lastIndexOf('.');
            resolvedName = name + "." + name.substring(i + 1);
            resolvedName = ORG_MODELINGVALUE_PREFIX + resolvedName;
        }

        String path = "/" + resolvedName.replace('.', '/') + ".nl";

        // Check if resource exists
        return KnowledgeBase.class.getResource(path) == null ? null : path;
    }
}
