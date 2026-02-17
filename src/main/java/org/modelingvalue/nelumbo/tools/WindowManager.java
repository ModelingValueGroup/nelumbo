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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.prefs.Preferences;

/**
 * Manages multiple editor windows, handling window lifecycle, persistence,
 * and restoration on application startup. Preserves window order.
 */
public class WindowManager {

    private static final String PREF_WINDOW_LIST = "windows.list";

    // Old single-window preference keys for migration
    private static final String OLD_PREF_TEXT_CONTENT        = "textContent";
    private static final String OLD_PREF_CARET_POSITION      = "caretPosition";
    private static final String OLD_PREF_SELECTION_START     = "selectionStart";
    private static final String OLD_PREF_SELECTION_END       = "selectionEnd";
    private static final String OLD_PREF_TREE_VIEWER_VISIBLE = "treeViewerVisible";
    private static final String OLD_PREF_KB_VIEWER_VISIBLE   = "knowledgeBaseViewerVisible";

    /**
     * Listener interface for window list changes.
     */
    public interface WindowListListener {
        void windowListChanged();
    }

    private final NelumboEditor             application;
    private final Preferences               preferences;
    private final List<String>              windowOrder   = new CopyOnWriteArrayList<>();  // Preserves order
    private final Map<String, EditorWindow> windows       = new ConcurrentHashMap<>();
    @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
    private final Map<String, Thread>       windowThreads = new ConcurrentHashMap<>();
    private final List<WindowListListener>  listeners     = new CopyOnWriteArrayList<>();
    private final Map<String, Integer>      windowNumbers = new ConcurrentHashMap<>();  // Track window numbers for regular windows

    public WindowManager(NelumboEditor application) {
        this.application = application;
        this.preferences = Preferences.userNodeForPackage(NelumboEditor.class);
    }

    /**
     * Returns the next available window number for regular (non-example) windows.
     */
    public synchronized int getNextWindowNumber() {
        int maxNumber = windowNumbers.values().stream().mapToInt(Integer::intValue).max().orElse(0);
        return maxNumber + 1;
    }

    /**
     * Atomically assigns the next available window number to a window.
     * Returns the assigned number.
     */
    public synchronized int assignNextWindowNumber(String windowId) {
        int number = getNextWindowNumber();
        windowNumbers.put(windowId, number);
        return number;
    }

    /**
     * Returns the editor window with the given window number, or null if not found.
     */
    public EditorWindow getWindowByNumber(int number) {
        for (EditorWindow window : windows.values()) {
            if (window.getWindowNumber() == number) {
                return window;
            }
        }
        return null;
    }

    /**
     * Adds a listener for window list changes.
     */
    public void addWindowListListener(WindowListListener listener) {
        listeners.add(listener);
    }

    /**
     * Removes a listener for window list changes.
     */
    public void removeWindowListListener(WindowListListener listener) {
        listeners.remove(listener);
    }

    /**
     * Notifies all listeners that the window list has changed.
     */
    public void notifyWindowListChanged() {
        for (WindowListListener listener : listeners) {
            try {
                listener.windowListChanged();
            } catch (Exception e) {
                // Ignore listener exceptions
            }
        }
    }

    /**
     * Creates a new regular editor window with a unique ID.
     */
    public synchronized void createNewWindow() {
        String       windowId     = UUID.randomUUID().toString();
        int          windowNumber = getNextWindowNumber();
        EditorWindow window       = new EditorWindow(application, windowId, windowNumber);
        windowNumbers.put(windowId, windowNumber);
        windowOrder.add(windowId);
        windows.put(windowId, window);
        saveWindowList();
        startWindowInNewThread(window);
        notifyWindowListChanged();
    }

    /**
     * Creates a new window for an example file.
     */
    public synchronized void createExampleWindow(String resourcePath, String displayName) {
        String       windowId = UUID.randomUUID().toString();
        EditorWindow window   = new EditorWindow(application, windowId, true, resourcePath, displayName);
        windowOrder.add(windowId);
        windows.put(windowId, window);
        saveWindowList();
        startWindowInNewThread(window);
        notifyWindowListChanged();
    }

    /**
     * Starts the given window in a new thread.
     */
    private void startWindowInNewThread(EditorWindow window) {
        Thread thread = new Thread(() -> {
            NelumboEditor.runOnEDT(window::init);
            window.startExecutionLoop();
        }, "EditorWindow-" + window.getWindowId());
        windowThreads.put(window.getWindowId(), thread);
        thread.start();
    }

    /**
     * Called when a window is closed.
     */
    public void windowClosed(EditorWindow window) {
        String windowId = window.getWindowId();
        windowOrder.remove(windowId);
        windows.remove(windowId);
        windowThreads.remove(windowId);
        windowNumbers.remove(windowId);

        // Clear window-specific preferences
        window.clearPreferences();

        saveWindowList();
        notifyWindowListChanged();

        // If this was the last window, exit the application
        if (windows.isEmpty()) {
            System.exit(0);
        }
    }

    /**
     * Saves all windows' state (called on quit).
     */
    public void saveAllWindows() {
        // Windows save their own state on close, but we save the window list here
        saveWindowList();
    }

    /**
     * Returns a list of all windows in order.
     */
    public List<EditorWindow> getWindowsInOrder() {
        List<EditorWindow> result = new ArrayList<>();
        for (String windowId : windowOrder) {
            EditorWindow window = windows.get(windowId);
            if (window != null) {
                result.add(window);
            }
        }
        return result;
    }

    /**
     * Restores windows from preferences on application startup.
     */
    public void restoreWindows() {
        // First check if we need to migrate from old single-window format
        migrateFromSingleWindow();

        String windowList = preferences.get(PREF_WINDOW_LIST, "");
        if (windowList.isEmpty()) {
            return;
        }

        String[] windowIds = windowList.split(",");
        for (String windowId : windowIds) {
            if (windowId.isEmpty()) {
                continue;
            }

            // Check if this was an example window
            boolean isExample          = preferences.getBoolean("window." + windowId + ".isExample", false);
            String  examplePath        = preferences.get("window." + windowId + ".examplePath", null);
            String  exampleDisplayName = preferences.get("window." + windowId + ".exampleDisplayName", null);
            int     savedWindowNumber  = preferences.getInt("window." + windowId + ".windowNumber", -1);

            EditorWindow window;
            if (isExample && examplePath != null) {
                window = new EditorWindow(application, windowId, true, examplePath, exampleDisplayName);
            } else {
                // For regular windows, use saved number or get a new one
                int windowNumber = savedWindowNumber > 0 ? savedWindowNumber : getNextWindowNumber();
                windowNumbers.put(windowId, windowNumber);
                window = new EditorWindow(application, windowId, windowNumber);
            }

            windowOrder.add(windowId);
            windows.put(windowId, window);
            startWindowInNewThread(window);
        }
    }

    /**
     * Migrates from old single-window preferences to new multi-window format.
     */
    private void migrateFromSingleWindow() {
        String windowList = preferences.get(PREF_WINDOW_LIST, null);
        if (windowList != null) {
            // Already migrated or using new format
            return;
        }

        // Check for old single-window format
        String oldContent = preferences.get(OLD_PREF_TEXT_CONTENT, null);
        if (oldContent == null) {
            // No old content to migrate
            return;
        }

        // Migrate to new format
        String newId  = UUID.randomUUID().toString();
        String prefix = "window." + newId + ".";

        try {
            // Migrate content
            preferences.put(prefix + "content", oldContent);
            preferences.put(prefix + "title", "Nelumbo Editor");

            // Migrate caret and selection
            int caretPosition  = preferences.getInt(OLD_PREF_CARET_POSITION, 0);
            int selectionStart = preferences.getInt(OLD_PREF_SELECTION_START, 0);
            int selectionEnd   = preferences.getInt(OLD_PREF_SELECTION_END, 0);
            preferences.putInt(prefix + "caretPosition", caretPosition);
            preferences.putInt(prefix + "selectionStart", selectionStart);
            preferences.putInt(prefix + "selectionEnd", selectionEnd);

            // Migrate dialog visibility
            boolean treeViewerVisible = preferences.getBoolean(OLD_PREF_TREE_VIEWER_VISIBLE, false);
            boolean kbViewerVisible   = preferences.getBoolean(OLD_PREF_KB_VIEWER_VISIBLE, false);
            preferences.putBoolean(prefix + "treeViewerVisible", treeViewerVisible);
            preferences.putBoolean(prefix + "kbViewerVisible", kbViewerVisible);

            // Not an example
            preferences.putBoolean(prefix + "isExample", false);

            // Set window list
            preferences.put(PREF_WINDOW_LIST, newId);

            // Migrate editor bounds to window-specific bounds
            migrateEditorBounds(newId);

            // Remove old keys
            preferences.remove(OLD_PREF_TEXT_CONTENT);
            preferences.remove(OLD_PREF_CARET_POSITION);
            preferences.remove(OLD_PREF_SELECTION_START);
            preferences.remove(OLD_PREF_SELECTION_END);
            preferences.remove(OLD_PREF_TREE_VIEWER_VISIBLE);
            preferences.remove(OLD_PREF_KB_VIEWER_VISIBLE);

            preferences.flush();

            System.out.println("Migrated single-window preferences to multi-window format (window ID: " + newId + ")");
        } catch (Exception e) {
            System.err.println("Failed to migrate preferences: " + e.getMessage());
        }
    }

    /**
     * Migrates old editor bounds to window-specific bounds.
     */
    private void migrateEditorBounds(String windowId) {
        // Old bounds used "editor" as the key prefix
        String[] boundsSuffixes = {".x", ".y", ".width", ".height"};
        String   oldPrefix      = "editor";
        String   newPrefix      = "window." + windowId;

        for (String suffix : boundsSuffixes) {
            int value = preferences.getInt(oldPrefix + suffix, Integer.MIN_VALUE);
            if (value != Integer.MIN_VALUE) {
                preferences.putInt(newPrefix + suffix, value);
                preferences.remove(oldPrefix + suffix);
            }
        }
    }

    /**
     * Returns true if there are any open windows.
     */
    public boolean hasOpenWindows() {
        return !windows.isEmpty();
    }

    /**
     * Saves the list of window IDs to preferences, preserving order.
     */
    private void saveWindowList() {
        String windowList = String.join(",", windowOrder);
        try {
            preferences.put(PREF_WINDOW_LIST, windowList);
            preferences.flush();
        } catch (Exception e) {
            System.err.println("Failed to save window list: " + e.getMessage());
        }
    }
}
