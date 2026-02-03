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
        GraphicsEnvironment ge      = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice[]    screens = ge.getScreenDevices();

        // Find the screen that has the most overlap with the window bounds
        Rectangle bestScreen  = null;
        int       maxOverlap  = 0;

        for (GraphicsDevice gd : screens) {
            for (GraphicsConfiguration gc : gd.getConfigurations()) {
                Rectangle screenBounds = gc.getBounds();
                Rectangle intersection = screenBounds.intersection(bounds);
                int       overlap      = intersection.isEmpty() ? 0 : intersection.width * intersection.height;
                if (overlap > maxOverlap) {
                    maxOverlap = overlap;
                    bestScreen = screenBounds;
                }
            }
        }

        // If window doesn't overlap with any screen, find the closest screen
        if (bestScreen == null) {
            int centerX = bounds.x + bounds.width / 2;
            int centerY = bounds.y + bounds.height / 2;

            double minDistance = Double.MAX_VALUE;
            for (GraphicsDevice gd : screens) {
                for (GraphicsConfiguration gc : gd.getConfigurations()) {
                    Rectangle screenBounds  = gc.getBounds();
                    int       screenCenterX = screenBounds.x + screenBounds.width / 2;
                    int       screenCenterY = screenBounds.y + screenBounds.height / 2;
                    double    distance      = Math.hypot(centerX - screenCenterX, centerY - screenCenterY);
                    if (distance < minDistance) {
                        minDistance = distance;
                        bestScreen  = screenBounds;
                    }
                }
            }
        }

        // Fallback to primary screen if still no screen found
        if (bestScreen == null) {
            bestScreen = ge.getDefaultScreenDevice().getDefaultConfiguration().getBounds();
        }

        // Constrain window to be within the chosen screen bounds
        // Ensure at least MIN_VISIBLE_SIZE pixels are visible

        // Adjust horizontal position
        if (bounds.x + bounds.width < bestScreen.x + MIN_VISIBLE_SIZE) {
            bounds.x = bestScreen.x + MIN_VISIBLE_SIZE - bounds.width;
        }
        if (bounds.x > bestScreen.x + bestScreen.width - MIN_VISIBLE_SIZE) {
            bounds.x = bestScreen.x + bestScreen.width - MIN_VISIBLE_SIZE;
        }

        // Adjust vertical position
        if (bounds.y + bounds.height < bestScreen.y + MIN_VISIBLE_SIZE) {
            bounds.y = bestScreen.y + MIN_VISIBLE_SIZE - bounds.height;
        }
        if (bounds.y > bestScreen.y + bestScreen.height - MIN_VISIBLE_SIZE) {
            bounds.y = bestScreen.y + bestScreen.height - MIN_VISIBLE_SIZE;
        }

        // Also ensure the window is not larger than the screen
        if (bounds.width > bestScreen.width) {
            bounds.width = bestScreen.width;
        }
        if (bounds.height > bestScreen.height) {
            bounds.height = bestScreen.height;
        }

        // Final adjustment: if window was pushed too far, ensure it stays on screen
        if (bounds.x < bestScreen.x) {
            bounds.x = bestScreen.x;
        }
        if (bounds.y < bestScreen.y) {
            bounds.y = bestScreen.y;
        }

        return bounds;
    }
}
