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

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.io.Serial;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.prefs.Preferences;

import javax.swing.*;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import javax.swing.text.StyledEditorKit;
import javax.swing.text.ViewFactory;

import org.modelingvalue.nelumbo.KnowledgeBase;
import org.modelingvalue.nelumbo.NelumboConstants;
import org.modelingvalue.nelumbo.syntax.TokenType;

import com.formdev.flatlaf.FlatLightLaf;

/**
 * Main application controller for the Nelumbo Editor.
 * Manages global settings (look and feel, token colors) and coordinates
 * multiple editor windows through the WindowManager.
 */
public class NelumboEditor {
    /**
     * Defines a color scheme for a token type with foreground and background colors,
     * and text style attributes (bold, italic, underline, subscript, superscript).
     */
    public record ColorScheme(Color foreground, Color background, boolean bold, boolean italic, boolean underline, boolean subscript, boolean superscript, SimpleAttributeSet attr) {

        public ColorScheme(Integer fore, Integer back, boolean bold, boolean italic, boolean underline, boolean subscript, boolean superscript) {
            this(fore == null ? null : new Color(fore), back == null ? null : new Color(back), bold, italic, underline, subscript, superscript, makeAttSet(fore, back, bold, italic, underline, subscript, superscript));
        }

        public ColorScheme(Color fg, Color bg, boolean bold, boolean italic, boolean underline, boolean subscript, boolean superscript) {
            this(fg, bg, bold, italic, underline, subscript, superscript, makeAttSet(fg == null ? null : fg.getRGB(), bg == null ? null : bg.getRGB(), bold, italic, underline, subscript, superscript));
        }

        static SimpleAttributeSet makeAttSet(Integer fore, Integer back, boolean bold, boolean italic, boolean underline, boolean subscript, boolean superscript) {
            SimpleAttributeSet a = new SimpleAttributeSet();
            StyleConstants.setForeground(a, new Color(Objects.requireNonNullElse(fore, 0x000000)));
            StyleConstants.setBackground(a, new Color(Objects.requireNonNullElse(back, 0xffffff)));
            StyleConstants.setBold(a, bold);
            StyleConstants.setItalic(a, italic);
            StyleConstants.setUnderline(a, underline);
            StyleConstants.setSubscript(a, subscript);
            StyleConstants.setSuperscript(a, superscript);
            return a;
        }
    }

    private static final String[]                    FONT_NAMES              = {"input mono", "dejavu sans mono", "overpass mono", Font.MONOSPACED};

    /**
     * Default color schemes for token types with style attributes
     */
    private static final Map<TokenType, ColorScheme> DEFAULT_TOKEN_COLORS    = Map.ofEntries(Map.entry(TokenType.STRING, new ColorScheme(0x006633, null, false, false, false, false, false)), Map.entry(TokenType.DECIMAL, new ColorScheme(0x000077, null, false, false, false, false, false)), Map.entry(TokenType.NUMBER, new ColorScheme(0x000077, null, false, false, false, false, false)), Map.entry(TokenType.NAME, new ColorScheme(0x0000ff, null, false, false, false, false, false)), Map.entry(TokenType.END_LINE_COMMENT, new ColorScheme(0xcccccc, null, false, false, false, false, false)), Map.entry(TokenType.IN_LINE_COMMENT, new ColorScheme(0xcccccc, null, false, false, false, false, false)), Map.entry(TokenType.OPERATOR, new ColorScheme(0x333333, null, true, false, false, false, false)), Map.entry(TokenType.ERROR, new ColorScheme(0xff0000, 0xffdddd, false, false, false, false, false)), Map.entry(TokenType.VARIABLE, new ColorScheme(0x339900, null, false, false, false, false, false)), Map.entry(TokenType.KEYWORD, new ColorScheme(0x0000ff, null, true, false, false, false, false)), Map.entry(TokenType.TYPE, new ColorScheme(0x880088, null, false, false, false, false, false)), Map.entry(TokenType.META_OPERATOR, new ColorScheme(0x00cccc, 0xffffff, false, false, false, false, false)));

    /**
     * Map from TokenType to ColorScheme defining how each token type should be colored.
     * This is mutable so users can customize colors.
     */
    private static final Map<TokenType, ColorScheme> TOKEN_COLORS            = new ConcurrentHashMap<>(DEFAULT_TOKEN_COLORS);

    private static final String                      PREF_TOKEN_COLOR_PREFIX = "tokenColor.";

    /**
     * Example .nl files bundled with the application: {category, filename, displayName}.
     */
    private static final String[][]                  EXAMPLE_RESOURCES       = {
            // Library files - display names match import names (e.g., "nelumbo.logic")
            {"Library", "logic/logic.nl", "nelumbo.logic"},
            {"Library", "integers/integers.nl", "nelumbo.integers"},
            {"Library", "strings/strings.nl", "nelumbo.strings"},
            {"Library", "collections/collections.nl", "nelumbo.collections"},
            // Examples
            {"Examples", "familyTest.nl", "Family"},
            {"Examples", "friendsTest.nl", "Friends"},
            {"Examples", "fibonacciTest.nl", "Fibonacci"},
            {"Examples", "belastingTest.nl", "Belasting"},
            {"Examples", "whoIsTest.nl", "Who Is"},
            {"Examples", "logicTest.nl", "Logic Test"},
            {"Examples", "integersTest.nl", "Integers Test"},
            {"Examples", "stringsTest.nl", "Strings Test"},
            {"Examples", "collectionsTest.nl", "Collections Test"},
            {"Examples", "transformationTest.nl", "Transformation"},
            {"Examples", "queryOnly.nl", "Query Only"},
            {"Examples", "hiddenTest.nl", "Hidden Test"},
            {"Examples", "maxTest.nl", "Max Test"},
            {"Examples", "scopingTest.nl", "Scoping Test"}};

    private final Preferences                        preferences             = Preferences.userNodeForPackage(NelumboEditor.class);
    private final WindowManager                      windowManager;
    private final EditorImportResolver               editorImportResolver;

    public WindowManager getWindowManager() {
        return windowManager;
    }

    public EditorImportResolver getEditorImportResolver() {
        return editorImportResolver;
    }

    public static void main(String[] args) {
        new NelumboEditor();
    }

    public NelumboEditor() {
        initLookAndFeel();
        loadTokenColors();
        windowManager = new WindowManager(this);

        // Create and register the editor import resolver
        editorImportResolver = new EditorImportResolver(windowManager);
        KnowledgeBase.registerResolver(editorImportResolver);

        windowManager.restoreWindows();

        if (!windowManager.hasOpenWindows()) {
            windowManager.createNewWindow();
        }
    }

    private void initLookAndFeel() {
        // Use native macOS menu bar
        System.setProperty("apple.laf.useScreenMenuBar", "true");
        System.setProperty("apple.awt.application.name", "Nelumbo");
        System.setProperty("flatlaf.useWindowDecorations", "false");

        try {
            FlatLightLaf.setup();
            UIManager.put("Button.arc", 8);
            UIManager.put("Component.arc", 8);
            UIManager.put("TextComponent.arc", 8);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Unable to set FlatLaf look and feel", "Warning", JOptionPane.WARNING_MESSAGE);
        }
    }

    /**
     * Creates a new editor window.
     */
    public void createNewWindow() {
        windowManager.createNewWindow();
    }

    /**
     * Quits the application, saving all windows first.
     */
    public void quit() {
        windowManager.saveAllWindows();
        System.exit(0);
    }

    /**
     * Opens an example in a new window.
     */
    public void openExample(String resourcePath, String displayName) {
        windowManager.createExampleWindow(resourcePath, displayName);
    }

    /**
     * Resolves an import name to its display name and resource path.
     * Returns a String array [displayName, resourcePath], or null if not found.
     */
    public String[] resolveImportName(String importName) {
        // Search in EXAMPLE_RESOURCES for a matching entry by display name
        for (String[] entry : EXAMPLE_RESOURCES) {
            if (entry[2].equals(importName)) {
                String category = entry[0];
                String resourcePath = category.equals("Library") ? NelumboConstants.NELUMBO_LIBRARY + entry[1] : NelumboConstants.NELUMBO_EXAMPLES + entry[1];
                return new String[]{importName, resourcePath};
            }
        }
        return null;
    }

    /**
     * Called when a window is closed.
     */
    public void windowClosed(EditorWindow window) {
        windowManager.windowClosed(window);
    }

    // ==================== Token Colors ====================

    public static ColorScheme getTokenColor(TokenType tokenType) {
        synchronized (TOKEN_COLORS) {
            return TOKEN_COLORS.get(tokenType);
        }
    }

    private void loadTokenColors() {
        for (TokenType tokenType : DEFAULT_TOKEN_COLORS.keySet()) {
            String fgKey = PREF_TOKEN_COLOR_PREFIX + tokenType.name() + ".fg";
            String bgKey = PREF_TOKEN_COLOR_PREFIX + tokenType.name() + ".bg";
            String boldKey = PREF_TOKEN_COLOR_PREFIX + tokenType.name() + ".bold";
            String italicKey = PREF_TOKEN_COLOR_PREFIX + tokenType.name() + ".italic";
            String underlineKey = PREF_TOKEN_COLOR_PREFIX + tokenType.name() + ".underline";
            String subscriptKey = PREF_TOKEN_COLOR_PREFIX + tokenType.name() + ".subscript";
            String superscriptKey = PREF_TOKEN_COLOR_PREFIX + tokenType.name() + ".superscript";

            String fgValue = preferences.get(fgKey, null);
            String bgValue = preferences.get(bgKey, null);

            ColorScheme defaultScheme = DEFAULT_TOKEN_COLORS.get(tokenType);

            if (fgValue != null || bgValue != null || preferences.get(boldKey, null) != null || preferences.get(italicKey, null) != null || preferences.get(underlineKey, null) != null || preferences.get(subscriptKey, null) != null || preferences.get(superscriptKey, null) != null) {

                Color fg = fgValue != null ? parseColorString(fgValue) : defaultScheme.foreground();
                Color bg = bgValue != null ? parseColorString(bgValue) : defaultScheme.background();
                boolean bold = preferences.getBoolean(boldKey, defaultScheme.bold());
                boolean italic = preferences.getBoolean(italicKey, defaultScheme.italic());
                boolean underline = preferences.getBoolean(underlineKey, defaultScheme.underline());
                boolean subscript = preferences.getBoolean(subscriptKey, defaultScheme.subscript());
                boolean superscript = preferences.getBoolean(superscriptKey, defaultScheme.superscript());

                TOKEN_COLORS.put(tokenType, new ColorScheme(fg, bg, bold, italic, underline, subscript, superscript));
            }
        }
    }

    private void saveTokenColors() {
        try {
            for (Map.Entry<TokenType, ColorScheme> entry : TOKEN_COLORS.entrySet()) {
                TokenType tokenType = entry.getKey();
                ColorScheme scheme = entry.getValue();

                String fgKey = PREF_TOKEN_COLOR_PREFIX + tokenType.name() + ".fg";
                String bgKey = PREF_TOKEN_COLOR_PREFIX + tokenType.name() + ".bg";
                String boldKey = PREF_TOKEN_COLOR_PREFIX + tokenType.name() + ".bold";
                String italicKey = PREF_TOKEN_COLOR_PREFIX + tokenType.name() + ".italic";
                String underlineKey = PREF_TOKEN_COLOR_PREFIX + tokenType.name() + ".underline";
                String subscriptKey = PREF_TOKEN_COLOR_PREFIX + tokenType.name() + ".subscript";
                String superscriptKey = PREF_TOKEN_COLOR_PREFIX + tokenType.name() + ".superscript";

                if (scheme.foreground() != null) {
                    preferences.put(fgKey, colorToString(scheme.foreground()));
                } else {
                    preferences.remove(fgKey);
                }

                if (scheme.background() != null) {
                    preferences.put(bgKey, colorToString(scheme.background()));
                } else {
                    preferences.remove(bgKey);
                }

                preferences.putBoolean(boldKey, scheme.bold());
                preferences.putBoolean(italicKey, scheme.italic());
                preferences.putBoolean(underlineKey, scheme.underline());
                preferences.putBoolean(subscriptKey, scheme.subscript());
                preferences.putBoolean(superscriptKey, scheme.superscript());
            }
            preferences.flush();
        } catch (Exception e) {
            System.err.println("Failed to save token colors: " + e.getMessage());
        }
    }

    public void resetTokenColors() {
        synchronized (TOKEN_COLORS) {
            TOKEN_COLORS.clear();
            TOKEN_COLORS.putAll(DEFAULT_TOKEN_COLORS);
        }
        saveTokenColors();
    }

    private String colorToString(Color color) {
        return String.format("#%06X", color.getRGB() & 0xFFFFFF);
    }

    private Color parseColorString(String colorStr) {
        if (colorStr.startsWith("#")) {
            return new Color(Integer.parseInt(colorStr.substring(1), 16));
        }
        return Color.BLACK;
    }

    public void showColorConfigDialog(JFrame parent) {
        JDialog dialog = new JDialog(parent, "Configure Token Colors and Styles", true);
        dialog.setLayout(new java.awt.BorderLayout());

        JPanel mainPanel = new JPanel(new GridBagLayout());
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        int row = 0;

        // Create a row for each token type that has a color
        for (Map.Entry<TokenType, ColorScheme> entry : TOKEN_COLORS.entrySet().stream().sorted(Comparator.comparingInt(e -> e.getKey().ordinal())).toList()) {
            TokenType tokenType = entry.getKey();
            ColorScheme scheme = entry.getValue();

            // Token type name label (column 0)
            gbc.gridx = 0;
            gbc.gridy = row;
            gbc.weightx = 0.0;
            gbc.anchor = GridBagConstraints.WEST;
            JLabel label = new JLabel(tokenType.name());
            mainPanel.add(label, gbc);

            // Foreground color button (column 1)
            gbc.gridx = 1;
            gbc.weightx = 0.0;
            gbc.anchor = GridBagConstraints.CENTER;
            JButton fgButton = new JButton("Foreground");
            fgButton.setPreferredSize(new Dimension(120, 25));
            if (scheme.foreground() != null) {
                fgButton.setBackground(scheme.foreground());
                fgButton.setForeground(getContrastColor(scheme.foreground()));
            }
            fgButton.addActionListener(e -> {
                Color initialColor = scheme.foreground() != null ? scheme.foreground() : Color.BLACK;
                Color newColor = JColorChooser.showDialog(dialog, "Choose Foreground Color for " + tokenType.name(), initialColor);
                if (newColor != null) {
                    fgButton.setBackground(newColor);
                    fgButton.setForeground(getContrastColor(newColor));
                    TOKEN_COLORS.computeIfPresent(tokenType, (k, currentScheme) -> new ColorScheme(newColor, currentScheme.background(), currentScheme.bold(), currentScheme.italic(), currentScheme.underline(), currentScheme.subscript(), currentScheme.superscript()));
                }
            });
            mainPanel.add(fgButton, gbc);

            // Background color button (column 2)
            gbc.gridx = 2;
            JButton bgButton = new JButton("Background");
            bgButton.setPreferredSize(new Dimension(120, 25));
            if (scheme.background() != null) {
                bgButton.setBackground(scheme.background());
                bgButton.setForeground(getContrastColor(scheme.background()));
            }
            bgButton.addActionListener(e -> {
                Color initialColor = scheme.background() != null ? scheme.background() : Color.WHITE;
                Color newColor = JColorChooser.showDialog(dialog, "Choose Background Color for " + tokenType.name(), initialColor);
                if (newColor != null) {
                    bgButton.setBackground(newColor);
                    bgButton.setForeground(getContrastColor(newColor));
                    TOKEN_COLORS.computeIfPresent(tokenType, (k, currentScheme) -> new ColorScheme(currentScheme.foreground(), newColor, currentScheme.bold(), currentScheme.italic(), currentScheme.underline(), currentScheme.subscript(), currentScheme.superscript()));
                }
            });
            mainPanel.add(bgButton, gbc);

            // Clear background button (column 3)
            gbc.gridx = 3;
            JButton clearBgButton = new JButton("Clear BG");
            clearBgButton.setPreferredSize(new Dimension(100, 25));
            clearBgButton.addActionListener(e -> {
                bgButton.setBackground(UIManager.getColor("Button.background"));
                bgButton.setForeground(UIManager.getColor("Button.foreground"));
                TOKEN_COLORS.computeIfPresent(tokenType, (k, currentScheme) -> new ColorScheme(currentScheme.foreground(), null, currentScheme.bold(), currentScheme.italic(), currentScheme.underline(), currentScheme.subscript(), currentScheme.superscript()));
            });
            mainPanel.add(clearBgButton, gbc);

            // Bold checkbox (column 4)
            gbc.gridx = 4;
            JCheckBox boldCheckbox = new JCheckBox("B", scheme.bold());
            boldCheckbox.setToolTipText("Bold");
            boldCheckbox.addActionListener(e -> TOKEN_COLORS.computeIfPresent(tokenType, (k, currentScheme) -> new ColorScheme(currentScheme.foreground(), currentScheme.background(), boldCheckbox.isSelected(), currentScheme.italic(), currentScheme.underline(), currentScheme.subscript(), currentScheme.superscript())));
            mainPanel.add(boldCheckbox, gbc);

            // Italic checkbox (column 5)
            gbc.gridx = 5;
            JCheckBox italicCheckbox = new JCheckBox("I", scheme.italic());
            italicCheckbox.setToolTipText("Italic");
            italicCheckbox.addActionListener(e -> TOKEN_COLORS.computeIfPresent(tokenType, (k, currentScheme) -> new ColorScheme(currentScheme.foreground(), currentScheme.background(), currentScheme.bold(), italicCheckbox.isSelected(), currentScheme.underline(), currentScheme.subscript(), currentScheme.superscript())));
            mainPanel.add(italicCheckbox, gbc);

            // Underline checkbox (column 6)
            gbc.gridx = 6;
            JCheckBox underlineCheckbox = new JCheckBox("U", scheme.underline());
            underlineCheckbox.setToolTipText("Underline");
            underlineCheckbox.addActionListener(e -> TOKEN_COLORS.computeIfPresent(tokenType, (k, currentScheme) -> new ColorScheme(currentScheme.foreground(), currentScheme.background(), currentScheme.bold(), currentScheme.italic(), underlineCheckbox.isSelected(), currentScheme.subscript(), currentScheme.superscript())));
            mainPanel.add(underlineCheckbox, gbc);

            // Subscript checkbox (column 7)
            gbc.gridx = 7;
            JCheckBox subscriptCheckbox = new JCheckBox("Sub", scheme.subscript());
            subscriptCheckbox.setToolTipText("Subscript");
            subscriptCheckbox.addActionListener(e -> TOKEN_COLORS.computeIfPresent(tokenType, (k, currentScheme) -> new ColorScheme(currentScheme.foreground(), currentScheme.background(), currentScheme.bold(), currentScheme.italic(), currentScheme.underline(), subscriptCheckbox.isSelected(), currentScheme.superscript())));
            mainPanel.add(subscriptCheckbox, gbc);

            // Superscript checkbox (column 8)
            gbc.gridx = 8;
            JCheckBox superscriptCheckbox = new JCheckBox("Sup", scheme.superscript());
            superscriptCheckbox.setToolTipText("Superscript");
            superscriptCheckbox.addActionListener(e -> TOKEN_COLORS.computeIfPresent(tokenType, (k, currentScheme) -> new ColorScheme(currentScheme.foreground(), currentScheme.background(), currentScheme.bold(), currentScheme.italic(), currentScheme.underline(), currentScheme.subscript(), superscriptCheckbox.isSelected())));
            mainPanel.add(superscriptCheckbox, gbc);

            row++;
        }

        JScrollPane scrollPane = new JScrollPane(mainPanel);
        scrollPane.setPreferredSize(new Dimension(800, 400));
        dialog.add(scrollPane, java.awt.BorderLayout.CENTER);

        // Buttons panel
        JPanel buttonPanel = new JPanel();
        JButton okButton = new JButton("OK");
        okButton.addActionListener(e -> {
            saveTokenColors();
            dialog.dispose();
        });

        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(e -> {
            loadTokenColors(); // Reload original colors
            dialog.dispose();
        });

        JButton copySourceButton = new JButton("Copy as Java Source");
        copySourceButton.addActionListener(e -> {
            String source = generateColorSchemeSource();
            StringSelection selection = new StringSelection(source);
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, selection);
            JOptionPane.showMessageDialog(dialog, "Color scheme copied to clipboard as Java source code.", "Copied", JOptionPane.INFORMATION_MESSAGE);
        });

        buttonPanel.add(copySourceButton);
        buttonPanel.add(okButton);
        buttonPanel.add(cancelButton);
        dialog.add(buttonPanel, java.awt.BorderLayout.SOUTH);

        dialog.pack();
        new DialogBoundsUtil(dialog, NelumboEditor.class, "colorConfig", parent);
        dialog.setVisible(true);
    }

    private Color getContrastColor(Color color) {
        // Calculate luminance to determine if we should use black or white text
        double luminance = (0.299 * color.getRed() + 0.587 * color.getGreen() + 0.114 * color.getBlue()) / 255;
        return luminance > 0.5 ? Color.BLACK : Color.WHITE;
    }

    private String generateColorSchemeSource() {
        StringBuilder sb = new StringBuilder();
        sb.append("    private static final Map<TokenType, ColorScheme> DEFAULT_TOKEN_COLORS = Map.ofEntries(");
        var entries = TOKEN_COLORS.entrySet().stream().sorted(Comparator.comparingInt(e -> e.getKey().ordinal())).toList();
        for (int i = 0; i < entries.size(); i++) {
            var entry = entries.get(i);
            TokenType tokenType = entry.getKey();
            ColorScheme scheme = entry.getValue();
            String fg = scheme.foreground() != null ? String.format("0x%06x", scheme.foreground().getRGB() & 0xFFFFFF) : "null";
            String bg = scheme.background() != null ? String.format("0x%06x", scheme.background().getRGB() & 0xFFFFFF) : "null";
            String suffix = i < entries.size() - 1 ? ")," : ")";
            sb.append(String.format("%n            Map.entry(TokenType.%-16s new ColorScheme(%s, %s, %s, %s, %s, %s, %s)%s", tokenType.name() + ",", fg, bg, scheme.bold(), scheme.italic(), scheme.underline(), scheme.subscript(), scheme.superscript(), suffix));
        }
        sb.append("\n    );");
        return sb.toString();
    }

    // ==================== Examples Menu ====================

    /**
     * Creates the Examples menu for use in editor windows.
     */
    public JMenu createExamplesMenu() {
        JMenu examplesMenu = new JMenu("Examples");
        Map<String, JMenu> submenus = new HashMap<>();

        for (String[] entry : EXAMPLE_RESOURCES) {
            String category = entry[0];
            String fileName = entry[1];
            String displayName = entry[2];

            // Construct full resource path based on category
            String resourcePath = category.equals("Library") ? NelumboConstants.NELUMBO_LIBRARY + fileName : NelumboConstants.NELUMBO_EXAMPLES + fileName;

            // Get or create submenu for this category
            JMenu submenu = submenus.computeIfAbsent(category, k -> {
                JMenu m = new JMenu(k);
                examplesMenu.add(m);
                return m;
            });

            JMenuItem item = new JMenuItem(displayName);
            item.addActionListener(e -> openExample(resourcePath, displayName));
            submenu.add(item);
        }

        return examplesMenu;
    }

    // ==================== Static Utilities ====================

    /**
     * Runs the given runnable on the EDT and waits for completion.
     * Wraps checked exceptions into RuntimeException.
     */
    static void runOnEDT(Runnable runnable) {
        callOnEDT(() -> {
            runnable.run();
            return null;
        });
    }

    /**
     * Thrown when an EDT call via {@link #callOnEDT} or {@link #runOnEDT} fails
     * due to thread interruption or an exception in the invoked code.
     */
    public static class EDTException extends RuntimeException {
        EDTException(Throwable cause) {
            super(cause);
        }
    }

    /**
     * Runs the given supplier on the EDT and returns its result.
     * Wraps checked exceptions into RuntimeException.
     */
    @SuppressWarnings("unchecked")
    static <T> T callOnEDT(java.util.function.Supplier<T> supplier) {
        try {
            Object[] result = new Object[1];
            javax.swing.SwingUtilities.invokeAndWait(() -> result[0] = supplier.get());
            return (T) result[0];
        } catch (InterruptedException | java.lang.reflect.InvocationTargetException e) {
            throw new EDTException(e);
        }
    }

    public static Font findFont() {
        Font font;
        for (String fontName : FONT_NAMES) {
            font = new Font(fontName, Font.BOLD, 14);
            if (font.getFamily().toLowerCase().contains(fontName)) {
                return font;
            }
        }
        return new Font(Font.MONOSPACED, Font.PLAIN, 14);
    }

    /**
     * JTextPane subclass that disables line wrapping.
     */
    public static class NonWrappingJTextPane extends JTextPane {
        @Serial
        private static final long serialVersionUID = 1L;

        public NonWrappingJTextPane(boolean editable, int backgroundRgb) {
            setEditorKit(new StyledEditorKit() {
                @Serial
                private static final long serialVersionUID = 1L;

                @Override
                public ViewFactory getViewFactory() {
                    return new NoWrapViewFactory();
                }
            });
            setFont(findFont());
            setMargin(new Insets(15, 15, 15, 15));
            setEditable(editable);
            setBackground(new Color(backgroundRgb));
            // Set line spacing for better readability
            StyledDocument doc = getStyledDocument();
            SimpleAttributeSet paragraphStyle = new SimpleAttributeSet();
            StyleConstants.setLineSpacing(paragraphStyle, 0.2f);
            doc.setParagraphAttributes(0, doc.getLength(), paragraphStyle, false);
        }

        @Override
        public boolean getScrollableTracksViewportWidth() {
            return false; // Disable line wrapping
        }

        @Override
        public void setSize(Dimension d) {
            if (d.width < getParent().getSize().width) {
                d.width = getParent().getSize().width;
            }
            super.setSize(d);
        }
    }

    // ViewFactory that prevents line wrapping in JTextPane
    private static class NoWrapViewFactory implements ViewFactory {
        private final ViewFactory defaultFactory = new StyledEditorKit().getViewFactory();

        @Override
        public javax.swing.text.View create(javax.swing.text.Element elem) {
            String kind = elem.getName();
            if (kind != null) {
                switch (kind) {
                case javax.swing.text.AbstractDocument.ContentElementName -> {
                    return new javax.swing.text.LabelView(elem);
                }
                case javax.swing.text.AbstractDocument.ParagraphElementName -> {
                    return new NoWrapParagraphView(elem);
                }
                case StyleConstants.ComponentElementName -> {
                    return new javax.swing.text.ComponentView(elem);
                }
                case StyleConstants.IconElementName -> {
                    return new javax.swing.text.IconView(elem);
                }
                }
            }
            return defaultFactory.create(elem);
        }
    }

    // ParagraphView that doesn't wrap lines
    private static class NoWrapParagraphView extends javax.swing.text.ParagraphView {
        public NoWrapParagraphView(javax.swing.text.Element elem) {
            super(elem);
        }

        @Override
        public void layout(int width, int height) {
            super.layout(java.lang.Short.MAX_VALUE, height);
        }

        @Override
        public float getMinimumSpan(int axis) {
            return super.getPreferredSpan(axis);
        }
    }
}
