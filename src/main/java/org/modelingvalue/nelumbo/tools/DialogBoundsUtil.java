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

import java.awt.Component;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.Window;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.prefs.Preferences;

/**
 * Utility class for persisting and restoring window bounds (position and size).
 * Works with both JFrame and JDialog. Ensures windows remain at least partially
 * visible on screen when restored.
 */
public class DialogBoundsUtil {

    private static final int MIN_VISIBLE_SIZE = 100;  // Minimum pixels that must be visible

    private final Window      window;
    private final Preferences preferences;
    private final String      prefX;
    private final String      prefY;
    private final String      prefWidth;
    private final String      prefHeight;

    /**
     * Creates a new DialogBoundsUtil and sets up automatic bounds persistence.
     *
     * @param window    the window (JFrame or JDialog) to manage bounds for
     * @param prefClass the class to use for preferences node
     * @param prefKey   the preference key prefix (e.g., "treeViewer" or "editor")
     * @param parent    the parent component for default positioning (can be null)
     */
    public DialogBoundsUtil(Window window, Class<?> prefClass, String prefKey, Component parent) {
        this.window      = window;
        this.preferences = Preferences.userNodeForPackage(prefClass);
        this.prefX       = prefKey + ".x";
        this.prefY       = prefKey + ".y";
        this.prefWidth   = prefKey + ".width";
        this.prefHeight  = prefKey + ".height";

        loadBounds(parent);
        setupAutoSave();
    }

    private void loadBounds(Component parent) {
        int savedX      = preferences.getInt(prefX, Integer.MIN_VALUE);
        int savedY      = preferences.getInt(prefY, Integer.MIN_VALUE);
        int savedWidth  = preferences.getInt(prefWidth, -1);
        int savedHeight = preferences.getInt(prefHeight, -1);

        if (savedX != Integer.MIN_VALUE && savedY != Integer.MIN_VALUE && savedWidth > 0 && savedHeight > 0) {
            // Apply saved bounds and ensure window is at least partially visible
            Rectangle bounds = new Rectangle(savedX, savedY, savedWidth, savedHeight);
            bounds = ensureVisibleOnScreen(bounds);
            window.setBounds(bounds);
        } else {
            window.setLocationRelativeTo(parent);
        }
    }

    private void setupAutoSave() {
        window.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentMoved(ComponentEvent e) {
                saveBounds();
            }

            @Override
            public void componentResized(ComponentEvent e) {
                saveBounds();
            }
        });
    }

    private void saveBounds() {
        try {
            Rectangle bounds = window.getBounds();
            preferences.putInt(prefX, bounds.x);
            preferences.putInt(prefY, bounds.y);
            preferences.putInt(prefWidth, bounds.width);
            preferences.putInt(prefHeight, bounds.height);
            preferences.flush();
        } catch (Exception e) {
            // Ignore save failures
        }
    }

    private Rectangle ensureVisibleOnScreen(Rectangle bounds) {
        // Get the combined bounds of all screens
        Rectangle            virtualBounds = new Rectangle();
        GraphicsEnvironment  ge            = GraphicsEnvironment.getLocalGraphicsEnvironment();
        for (GraphicsDevice gd : ge.getScreenDevices()) {
            for (GraphicsConfiguration gc : gd.getConfigurations()) {
                virtualBounds = virtualBounds.union(gc.getBounds());
            }
        }

        // Ensure at least MIN_VISIBLE_SIZE pixels are visible horizontally
        if (bounds.x + bounds.width < virtualBounds.x + MIN_VISIBLE_SIZE) {
            bounds.x = virtualBounds.x + MIN_VISIBLE_SIZE - bounds.width;
        }
        if (bounds.x > virtualBounds.x + virtualBounds.width - MIN_VISIBLE_SIZE) {
            bounds.x = virtualBounds.x + virtualBounds.width - MIN_VISIBLE_SIZE;
        }

        // Ensure at least MIN_VISIBLE_SIZE pixels are visible vertically
        if (bounds.y + bounds.height < virtualBounds.y + MIN_VISIBLE_SIZE) {
            bounds.y = virtualBounds.y + MIN_VISIBLE_SIZE - bounds.height;
        }
        if (bounds.y > virtualBounds.y + virtualBounds.height - MIN_VISIBLE_SIZE) {
            bounds.y = virtualBounds.y + virtualBounds.height - MIN_VISIBLE_SIZE;
        }

        return bounds;
    }
}
