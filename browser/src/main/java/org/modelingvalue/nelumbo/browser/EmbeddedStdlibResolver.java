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

import org.modelingvalue.nelumbo.ImportResolver;
import org.modelingvalue.nelumbo.KnowledgeBase;
import org.modelingvalue.nelumbo.lang.Import;
import org.modelingvalue.nelumbo.syntax.ParseException;
import org.modelingvalue.nelumbo.syntax.Parser;
import org.modelingvalue.nelumbo.syntax.Tokenizer;

/**
 * The TeaVM stand-in for {@code ResourceImportResolver}: resolves stdlib imports from the
 * build-time-generated {@link StdlibSources} map instead of classpath resources (TeaVM has no
 * {@code Class.getResource}). Same name expansion: {@code nelumbo.X} maps to
 * {@code org.modelingvalue.nelumbo.X.X}.
 */
public class EmbeddedStdlibResolver implements ImportResolver {

    public static final String NELUMBO_PREFIX           = "nelumbo.";
    public static final String ORG_MODELINGVALUE_PREFIX = "org.modelingvalue.";

    @Override
    public ImportResult resolve(String name, Import imp) throws ParseException {
        String path = getResourcePath(name);
        if (path == null) {
            return null; // Let another resolver try
        }
        String content = StdlibSources.SOURCES.get(path);
        ParseException[] exc = new ParseException[1];
        KnowledgeBase kb = KnowledgeBase.BASE.run(() -> {
            try {
                new Parser(new Tokenizer(content, path).tokenize()).parseEvaluate();
            } catch (ParseException e) {
                exc[0] = e;
            }
        });
        if (exc[0] != null) {
            throw exc[0];
        }
        return new ImportResult(kb, true); // embedded sources are cacheable
    }

    @Override
    public boolean canHandle(String name) {
        return name.startsWith(NELUMBO_PREFIX) || getResourcePath(name) != null;
    }

    private static String getResourcePath(String name) {
        String resolvedName = name;
        if (name.startsWith(NELUMBO_PREFIX)) {
            int i = name.lastIndexOf('.');
            resolvedName = name + "." + name.substring(i + 1);
            resolvedName = ORG_MODELINGVALUE_PREFIX + resolvedName;
        }
        String path = "/" + resolvedName.replace('.', '/') + ".nl";
        return StdlibSources.SOURCES.containsKey(path) ? path : null;
    }
}
