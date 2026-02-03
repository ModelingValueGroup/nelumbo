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

package org.modelingvalue.nelumbo.tools;

import java.util.concurrent.atomic.AtomicReference;

import org.modelingvalue.collections.Map;
import org.modelingvalue.collections.Set;
import org.modelingvalue.nelumbo.Import;
import org.modelingvalue.nelumbo.ImportResolver;
import org.modelingvalue.nelumbo.KnowledgeBase;
import org.modelingvalue.nelumbo.syntax.ParseException;

/**
 * Import resolver for editor window imports (e.g., "editor.nelumbo_1").
 * Returns non-cacheable results since editor content changes dynamically.
 * Also manages dependency tracking between windows.
 */
public class EditorImportResolver implements ImportResolver {

    /**
     * Listener interface for import change notifications.
     */
    public interface ImportChangeListener {
        void onImportChanged(String importName);
    }

    private static final String EDITOR_PREFIX = "editor.nelumbo_";

    private final WindowManager                                           windowManager;
    private final AtomicReference<Map<String, Set<ImportChangeListener>>> dependencies = new AtomicReference<>(Map.of());

    public EditorImportResolver(WindowManager windowManager) {
        this.windowManager = windowManager;
    }

    @Override
    public ImportResult resolve(String name, Import imp) throws ParseException {
        if (!canHandle(name)) {
            return null;
        }

        // Parse the window number from "editor.nelumbo_N"
        try {
            int          number = Integer.parseInt(name.substring(EDITOR_PREFIX.length()));
            EditorWindow window = windowManager.getWindowByNumber(number);

            if (window == null) {
                throw new ParseException("Cannot resolve editor window: " + name, imp);
            }

            KnowledgeBase kb = window.getKnowledgeBase();
            if (kb == null) {
                throw new ParseException("Editor window " + name + " has no KnowledgeBase yet", imp);
            }

            // Editor imports are non-cacheable since they change dynamically
            return new ImportResult(kb, false);
        } catch (NumberFormatException e) {
            throw new ParseException("Invalid editor window name: " + name, imp);
        }
    }

    @Override
    public boolean canHandle(String name) {
        return name != null && name.startsWith(EDITOR_PREFIX);
    }

    /**
     * Adds a dependency: the listener will be notified when the import changes.
     *
     * @param importName the import name (e.g., "editor.nelumbo_1")
     * @param listener   the listener to notify
     */
    public void addDependency(String importName, ImportChangeListener listener) {
        dependencies.updateAndGet(m -> {
            Map<String, Set<ImportChangeListener>> newMap = m.computeIfAbsent(importName, k -> Set.of());
            return newMap.put(importName, newMap.get(importName).add(listener));
        });
    }

    /**
     * Removes a dependency.
     *
     * @param importName the import name
     * @param listener   the listener to remove
     */
    public void removeDependency(String importName, ImportChangeListener listener) {
        dependencies.updateAndGet(m -> {
            Set<ImportChangeListener> s = m.get(importName);
            if (s == null) {
                return m;
            }
            s = s.remove(listener);
            return s.isEmpty() ? m.remove(importName) : m.put(importName, s);
        });
    }

    /**
     * Removes all dependencies for a given listener.
     *
     * @param listener the listener to remove from all dependencies
     */
    public void removeAllDependencies(ImportChangeListener listener) {
        dependencies.get().forEach((k, v) -> removeDependency(k, listener));
    }

    /**
     * Notifies all listeners that depend on the given import that it has changed.
     *
     * @param importName the import name that changed
     */
    public void notifyImportChanged(String importName) {
        Set<ImportChangeListener> listeners = dependencies.get().get(importName);
        if (listeners != null) {
            for (ImportChangeListener listener : listeners) {
                try {
                    listener.onImportChanged(importName);
                } catch (Exception e) {
                    // Ignore listener exceptions
                }
            }
        }
    }

    /**
     * Returns the import name for a given window number.
     *
     * @param windowNumber the window number
     *
     * @return the import name (e.g., "editor.nelumbo_1")
     */
    public static String getImportName(int windowNumber) {
        return EDITOR_PREFIX + windowNumber;
    }
}
