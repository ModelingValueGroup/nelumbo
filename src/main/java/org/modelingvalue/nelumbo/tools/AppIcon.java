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

import java.awt.Image;
import java.awt.Taskbar;
import java.awt.Window;
import java.io.IOException;
import java.io.InputStream;

import javax.imageio.ImageIO;

/**
 * The Nelumbo lotus as application icon: without it every double-clicked jar shows the default Java icon in the
 * dock/taskbar. All setters are best-effort - a missing resource or unsupported platform never breaks the caller.
 */
public final class AppIcon {

    private static Image icon;
    private static boolean loaded;

    private AppIcon() {
    }

    /** Sets the dock/taskbar icon and, when {@code window} is not null, its window icon. */
    public static synchronized void install(Window window) {
        if (!loaded) {
            loaded = true;
            try (InputStream in = AppIcon.class.getResourceAsStream("nelumbo-icon.png")) {
                icon = in == null ? null : ImageIO.read(in);
            } catch (IOException e) {
                icon = null;
            }
        }
        if (icon == null) {
            return;
        }
        if (window != null) {
            window.setIconImage(icon);
        }
        try {
            if (Taskbar.isTaskbarSupported() && Taskbar.getTaskbar().isSupported(Taskbar.Feature.ICON_IMAGE)) {
                Taskbar.getTaskbar().setIconImage(icon);
            }
        } catch (RuntimeException e) {
            // headless or platform without a taskbar icon: keep going without one
        }
    }
}
