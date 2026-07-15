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

package org.modelingvalue.nelumbo.server;

import java.awt.Desktop;
import java.awt.FlowLayout;
import java.awt.GraphicsEnvironment;
import java.io.File;
import java.net.URI;
import java.util.List;
import java.util.function.Function;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.modelingvalue.nelumbo.tools.AppIcon;
import org.modelingvalue.nelumbo.tools.NelumboLaf;

/**
 * A status window for a double-clicked server jar: without it a double-click starts the server invisibly, with no way
 * to see the URL or stop it. Since a double-click cannot pass file arguments, the window also offers loading
 * {@code .nl} files into the running server (via the {@code loadFiles} callback, which swaps the knowledge base).
 * Shown only when there is no attached console and a display is available; {@code --no-gui} suppresses it.
 */
public final class ServerGui {

    private ServerGui() {
    }

    /** True when the process was (most likely) launched by double-click: no console, but a display. */
    public static boolean wanted(boolean noGui) {
        return !noGui && System.console() == null && !GraphicsEnvironment.isHeadless();
    }

    /** {@code loadFiles} gets the chosen files/directories and returns the new status line (or throws). */
    public static void show(String title, String url, String detail, Function<List<File>, String> loadFiles) {
        NelumboLaf.setup();
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame(title);
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            AppIcon.install(frame);

            JLabel detailLabel = new JLabel(detail, JLabel.CENTER);

            JPanel panel = new JPanel();
            panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
            panel.setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));
            panel.add(new JLabel(title + " is running at " + url, JLabel.CENTER));
            panel.add(detailLabel);

            JButton open = new JButton("Open in Browser");
            open.addActionListener(e -> browse(url));
            JButton load = new JButton("Load .nl files...");
            load.addActionListener(e -> chooseAndLoad(frame, load, detailLabel, loadFiles));
            JButton stop = new JButton("Stop");
            stop.addActionListener(e -> System.exit(0));
            JPanel buttons = new JPanel(new FlowLayout(FlowLayout.CENTER));
            buttons.add(open);
            buttons.add(load);
            buttons.add(stop);
            panel.add(buttons);

            frame.setContentPane(panel);
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }

    private static void chooseAndLoad(JFrame frame, JButton load, JLabel detailLabel, Function<List<File>, String> loadFiles) {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Choose .nl files or directories to load");
        chooser.setMultiSelectionEnabled(true);
        chooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
        chooser.setFileFilter(new FileNameExtensionFilter("Nelumbo files (*.nl)", "nl"));
        if (chooser.showOpenDialog(frame) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        List<File> files = List.of(chooser.getSelectedFiles());
        load.setEnabled(false);
        detailLabel.setText("loading...");
        new Thread(() -> {
            try {
                String newDetail = loadFiles.apply(files);
                SwingUtilities.invokeLater(() -> {
                    detailLabel.setText(newDetail);
                    load.setEnabled(true);
                });
            } catch (RuntimeException ex) {
                SwingUtilities.invokeLater(() -> {
                    detailLabel.setText("load failed - previous knowledge base still active");
                    load.setEnabled(true);
                    JOptionPane.showMessageDialog(frame, ex.getMessage(), "Load failed", JOptionPane.ERROR_MESSAGE);
                });
            }
        }, "nelumbo-server-load").start();
    }

    private static void browse(String url) {
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(URI.create(url));
            }
        } catch (Exception e) {
            System.err.println("cannot open browser: " + e);
        }
    }
}
