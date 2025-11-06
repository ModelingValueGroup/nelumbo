//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
// (C) Copyright 2018-2025 Modeling Value Group B.V. (http://modelingvalue.org)                                        ~
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

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.Serial;
import java.net.URL;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.prefs.Preferences;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultHighlighter.DefaultHighlightPainter;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import javax.swing.text.StyledEditorKit;
import javax.swing.text.TextAction;
import javax.swing.text.ViewFactory;

import org.modelingvalue.nelumbo.integers.Integer;
import org.modelingvalue.nelumbo.syntax.ParseException;
import org.modelingvalue.nelumbo.syntax.Parser;
import org.modelingvalue.nelumbo.syntax.ParserResult;
import org.modelingvalue.nelumbo.syntax.Token;
import org.modelingvalue.nelumbo.syntax.TokenType;
import org.modelingvalue.nelumbo.syntax.Tokenizer;
import org.modelingvalue.nelumbo.syntax.Tokenizer.TokenizerResult;

import com.formdev.flatlaf.FlatLightLaf;

public class NelumboEditor extends WindowAdapter implements WindowListener, Runnable, DocumentListener {

    private final static String                  INCREASE   = "INCREASE";
    private final static String                  DECREASE   = "DECREASE";

    private final static DefaultHighlightPainter redPainter = new DefaultHighlightPainter(new Color(0xffaaaa));

    /**
     * Defines a color scheme for a token type with foreground and background colors,
     * and text style attributes (bold, italic, underline, subscript, superscript).
     */
    private record ColorScheme(Color foreground, Color background, boolean bold, boolean italic, boolean underline, boolean subscript, boolean superscript, SimpleAttributeSet attr) {

        public ColorScheme(java.lang.Integer fore, java.lang.Integer back, boolean bold, boolean italic, boolean underline, boolean subscript, boolean superscript) {
            this(fore == null ? null : new Color(fore), back == null ? null : new Color(back), bold, italic, underline, subscript, superscript, makeAttSet(fore, back, bold, italic, underline, subscript, superscript));
        }

        public ColorScheme(Color fg, Color bg, boolean bold, boolean italic, boolean underline, boolean subscript, boolean superscript) {
            this(fg, bg, bold, italic, underline, subscript, superscript, makeAttSet(fg == null ? null : fg.getRGB(), bg == null ? null : bg.getRGB(), bold, italic, underline, subscript, superscript));
        }

        static SimpleAttributeSet makeAttSet(java.lang.Integer fore, java.lang.Integer back, boolean bold, boolean italic, boolean underline, boolean subscript, boolean superscript) {
            return makeAttSet(fore == null ? null : new Color(fore), back == null ? null : new Color(back), bold, italic, underline, subscript, superscript);
        }

        static SimpleAttributeSet makeAttSet(Color fore, Color back, boolean bold, boolean italic, boolean underline, boolean subscript, boolean superscript) {
            SimpleAttributeSet attr = new SimpleAttributeSet();
            StyleConstants.setForeground(attr, new Color(0x000000));
            StyleConstants.setBackground(attr, new Color(0xffffff));
            if (fore != null) {
                StyleConstants.setForeground(attr, fore);
            }
            if (back != null) {
                StyleConstants.setBackground(attr, back);
            }
            StyleConstants.setBold(attr, bold);
            StyleConstants.setItalic(attr, italic);
            StyleConstants.setUnderline(attr, underline);
            StyleConstants.setSubscript(attr, subscript);
            StyleConstants.setSuperscript(attr, superscript);
            return attr;
        }
    }

    private static final String[]                    FONT_NAMES              = {                                   //
            "input mono",                                                                                          //
            "dejavu sans mono",                                                                                    //
            "overpass mono",                                                                                       //
            Font.MONOSPACED                                                                                        //
    };

    /**
     * Default color schemes for token types with style attributes
     */
    private static final Map<TokenType, ColorScheme> DEFAULT_TOKEN_COLORS    = Map.ofEntries(                      //
            Map.entry(TokenType.NUMBER,                                                                            //
                    new ColorScheme(0x000077, null, true, false, false, false, false)),                            //
            Map.entry(TokenType.DECIMAL,                                                                           //
                    new ColorScheme(0x000077, null, true, false, false, false, false)),                            //
            Map.entry(TokenType.STRING,                                                                            //
                    new ColorScheme(0x007700, null, false, false, false, false, false)),                           //
            Map.entry(TokenType.NAME,                                                                              //
                    new ColorScheme(0x0000ff, null, false, false, false, false, false)),                           //
            Map.entry(TokenType.TYPE,                                                                              //
                    new ColorScheme(0x880088, null, true, false, false, false, false)),                            //
            Map.entry(TokenType.META_OPERATOR,                                                                     //
                    new ColorScheme(0xffffff, 0x558855, true, false, false, false, false)),                        //
            Map.entry(TokenType.OPERATOR,                                                                          //
                    new ColorScheme(0x666666, null, false, false, false, false, false)),                           //
            Map.entry(TokenType.END_LINE_COMMENT,                                                                  //
                    new ColorScheme(0xcccccc, null, false, true, false, false, false)),                            //
            Map.entry(TokenType.IN_LINE_COMMENT,                                                                   //
                    new ColorScheme(0xcccccc, null, false, true, false, false, false)),                            //
            Map.entry(TokenType.ERROR,                                                                             //
                    new ColorScheme(0xff0000, 0xffdddd, true, true, false, false, false)),                         //
            Map.entry(TokenType.VARIABLE,                                                                          //
                    new ColorScheme(0x0000ff, null, true, false, false, false, false))                             //
    );

    /**
     * Map from TokenType to ColorScheme defining how each token type should be colored.
     * This is mutable so users can customize colors.
     */
    private static final Map<TokenType, ColorScheme> TOKEN_COLORS            = new HashMap<>(DEFAULT_TOKEN_COLORS);

    private static final String                      PREF_TEXT_CONTENT       = "textContent";
    private static final String                      PREF_CARET_POSITION     = "caretPosition";
    private static final String                      PREF_SELECTION_START    = "selectionStart";
    private static final String                      PREF_SELECTION_END      = "selectionEnd";
    private static final String                      PREF_TOKEN_COLOR_PREFIX = "tokenColor.";

    public static void main(String[] arg) {
        new NelumboEditor();
    }

    //===========================================================================================================================================
    private KnowledgeBase     knowledgeBase;
    private JFrame            frame;
    private JTextPane         messagesPane;
    private JTextPane         textPane;
    private boolean           quit;
    private boolean           refreshRequested;
    private final Preferences preferences = Preferences.userNodeForPackage(NelumboEditor.class);

    public NelumboEditor() {
        loadTokenColors(); // Load saved colors before creating UI
        initWindow();
        initActions();
        loadTextContent();
        initKnowledgeBase(); // execution stays here until the user quits the application. This is by design!
        // only reaching this point after the user quits
    }

    private static class NonWrappingJTextPane extends JTextPane {
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
            StyleConstants.setLineSpacing(paragraphStyle, 0.2f); // 20% extra spacing
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

    private void initWindow() {
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
        URL resource = getClass().getResource("nelumbo.png");
        assert resource != null;
        ImageIcon icon = new ImageIcon(resource);
        frame = new JFrame("Nelumbo Editor");
        frame.setIconImage(icon.getImage());
        if (Taskbar.getTaskbar().isSupported(Taskbar.Feature.ICON_IMAGE)) {
            Taskbar.getTaskbar().setIconImage(icon.getImage());
        }
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        Dimension frameSize = new Dimension(screenSize.width / 2, screenSize.height / 2);
        int x = frameSize.width / 2;
        int y = frameSize.height / 2;
        frame.setBounds(x, y, frameSize.width, frameSize.height);

        textPane = new NonWrappingJTextPane(true, 0xffffff);
        messagesPane = new NonWrappingJTextPane(false, 0xF5F5F5);

        // Create scroll panes with borders
        JScrollPane textScroll = new JScrollPane(textPane);
        JScrollPane messageScroll = new JScrollPane(messagesPane);
        textScroll.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 5));
        messageScroll.setBorder(BorderFactory.createEmptyBorder(10, 5, 10, 10));
        messageScroll.getVerticalScrollBar().setModel(textScroll.getVerticalScrollBar().getModel());

        // Create split pane with modern styling
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, textScroll, messageScroll);
        split.setDividerLocation(frameSize.width / 4 * 3);
        split.setDividerSize(8);
        split.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        split.setContinuousLayout(true);

        // Layout
        frame.getContentPane().setLayout(new BorderLayout());
        frame.getContentPane().add(split, BorderLayout.CENTER);

        // Setup menu bar before making frame visible
        JMenuBar menuBar = new JMenuBar();
        JMenu colorsMenu = new JMenu("Colors");
        JMenuItem configureColors = new JMenuItem("Configure Token Colors...");
        configureColors.addActionListener(e -> showColorConfigDialog());

        JMenuItem resetColors = new JMenuItem("Reset to Defaults");
        resetColors.addActionListener(e -> resetTokenColors());

        colorsMenu.add(configureColors);
        colorsMenu.add(resetColors);
        menuBar.add(colorsMenu);
        frame.setJMenuBar(menuBar);

        frame.setVisible(true);

        frame.addWindowListener(this);
        textPane.getDocument().addDocumentListener(this);

        // Set focus on text area
        textPane.requestFocusInWindow();
    }

    private static Font findFont() {
        Font font;
        for (String fontName : FONT_NAMES) {
            font = new Font(fontName, Font.BOLD, 14);
            if (font.getFamily().toLowerCase().contains(fontName)) {
                return font;
            }
        }
        return new Font(Font.MONOSPACED, Font.PLAIN, 14);
    }

    private void initActions() {
        textPane.getInputMap().put(KeyStroke.getKeyStroke('+', InputEvent.CTRL_DOWN_MASK), INCREASE);
        textPane.getInputMap().put(KeyStroke.getKeyStroke('=', InputEvent.CTRL_DOWN_MASK), INCREASE);
        textPane.getInputMap().put(KeyStroke.getKeyStroke('-', InputEvent.CTRL_DOWN_MASK), DECREASE);
        textPane.getInputMap().put(KeyStroke.getKeyStroke('_', InputEvent.CTRL_DOWN_MASK), DECREASE);

        textPane.getInputMap().put(KeyStroke.getKeyStroke('+', InputEvent.META_DOWN_MASK), INCREASE);
        textPane.getInputMap().put(KeyStroke.getKeyStroke('=', InputEvent.META_DOWN_MASK), INCREASE);
        textPane.getInputMap().put(KeyStroke.getKeyStroke('-', InputEvent.META_DOWN_MASK), DECREASE);
        textPane.getInputMap().put(KeyStroke.getKeyStroke('_', InputEvent.META_DOWN_MASK), DECREASE);

        textPane.getActionMap().put(INCREASE, new TextAction(INCREASE) {
            @Serial
            private static final long serialVersionUID = -425923171136898022L;

            @Override
            public void actionPerformed(ActionEvent e) {
                if (textPane == getTextComponent(e)) {
                    increase();
                }
            }
        });
        textPane.getActionMap().put(DECREASE, new TextAction(DECREASE) {
            @Serial
            private static final long serialVersionUID = 3357017446274657221L;

            @Override
            public void actionPerformed(ActionEvent e) {
                if (textPane == getTextComponent(e)) {
                    decrease();
                }
            }
        });
    }

    private void initKnowledgeBase() {
        KnowledgeBase.BASE.run(() -> {
            try {
                Parser.parse(Integer.class);
                Parser.parse(org.modelingvalue.nelumbo.strings.String.class);
            } catch (ParseException e) {
                setMessagesAsError(e.getMessage());
            }
        }).run(this);
    }

    private void increase() {
        Font font = textPane.getFont();
        float newSize = Math.min(100f, font.getSize() * 1.2f);
        font = font.deriveFont(newSize);
        textPane.setFont(font);
        messagesPane.setFont(font);
    }

    private void decrease() {
        Font font = textPane.getFont();
        float newSize = Math.max(7f, font.getSize() / 1.2f);
        font = font.deriveFont(newSize);
        textPane.setFont(font);
        messagesPane.setFont(font);
    }

    @Override
    public synchronized void windowClosed(WindowEvent evt) {
        quit = true;
        refresh();
        System.exit(0);
    }

    @Override
    public synchronized void windowClosing(WindowEvent evt) {
        frame.setVisible(false);
        frame.dispose();
    }

    @Override
    public void run() {
        knowledgeBase = KnowledgeBase.CURRENT.get();
        while (!quit) {
            execute();
            waitForRefreshRequest();
        }
    }

    private void waitForRefreshRequest() {
        if (!quit) {
            synchronized (this) {
                if (!refreshRequested) {
                    try {
                        wait(1_000_000);
                    } catch (InterruptedException ie) {
                        // ignore
                    }
                }
                refreshRequested = false;
            }
        }
    }

    private void refresh() {
        synchronized (this) {
            refreshRequested = true;
            notifyAll();
        }
    }

    private record ExecuteResult(TokenizerResult tokenizerResult, ParserResult parserResult, ParseException pe) {
        public ExecuteResult(TokenizerResult tokenizerResult, ParserResult parserResult) {
            this(tokenizerResult, parserResult, null);
        }

        public ExecuteResult(TokenizerResult tokenizerResult, ParseException pe) {
            this(tokenizerResult, null, pe);
        }
    }

    private void execute() {
        prepareForExecute();
        String text = textPane.getText();
        Tokenizer tokenizer = new Tokenizer(text, "Editor");
        TokenizerResult tokenizerResult = tokenizer.tokenize();
        showColors(tokenizerResult);
        ExecuteResult executeResult;
        try {
            executeResult = new ExecuteResult(tokenizerResult, new Parser(tokenizerResult).parseMutiple());
        } catch (ParseException pe) {
            executeResult = new ExecuteResult(tokenizerResult, pe);
        }
        showColors(executeResult.tokenizerResult());
        showResults(executeResult);
        saveTextContent(text);
    }

    private void prepareForExecute() {
        knowledgeBase.init();
        textPane.getHighlighter().removeAllHighlights();
        StyledDocument doc = textPane.getStyledDocument();
        SimpleAttributeSet defaultAttr = new SimpleAttributeSet();
        StyleConstants.setForeground(defaultAttr, Color.BLACK);
        doc.setCharacterAttributes(0, doc.getLength(), defaultAttr, true);
        messagesPane.setText("...");
    }

    private void showColors(TokenizerResult tokenizerResult) {
        if (tokenizerResult != null) {
            for (Token t = tokenizerResult.firstAll(); t != null; t = t.nextAll()) {
                ColorScheme colorScheme = TOKEN_COLORS.get(t.getNode() instanceof Variable ? TokenType.VARIABLE : t.type());
                if (colorScheme != null) {
                    SimpleAttributeSet attr = colorScheme.attr();
                    textPane.getStyledDocument().setCharacterAttributes(t.index(), t.text().length(), attr, false);
                }
            }
        }
    }

    private void showResults(ExecuteResult executeResult) {
        if (executeResult.pe == null) {
            StringBuilder messages = new StringBuilder();
            int prevLine = 0;
            int nextLine;
            for (Node root : executeResult.parserResult.roots()) {
                if (root instanceof Query query) {
                    nextLine = query.lastToken().line();
                    messages.append(emptyLines(nextLine - prevLine)).append(query.inferResult().toString()).append("\n");
                    prevLine = ++nextLine;
                }
            }
            setMessages(messages.toString());
        } else {
            ParseException pe = executeResult.pe();
            setMessagesAsError(emptyLines(pe.line()) + pe.getShortMessage());
            try {
                textPane.getHighlighter().addHighlight(pe.index(), pe.index() + pe.length(), redPainter);
            } catch (BadLocationException ble) {
                setMessagesAsError(ble.getMessage());
            }
        }
    }

    private String emptyLines(int nr) {
        return "\n".repeat(nr);
    }

    private void setMessagesAsError(String msg) {
        messagesPane.setText(msg);
        // Apply line spacing after setting text
        StyledDocument messageDoc = messagesPane.getStyledDocument();
        SimpleAttributeSet messageParagraphStyle = new SimpleAttributeSet();
        StyleConstants.setLineSpacing(messageParagraphStyle, 0.2f);
        messageDoc.setParagraphAttributes(0, messageDoc.getLength(), messageParagraphStyle, false);
        messagesPane.repaint();
    }

    private void setMessages(String msg) {
        messagesPane.setText(msg);
        // Apply line spacing after setting text
        StyledDocument messageDoc = messagesPane.getStyledDocument();
        SimpleAttributeSet messageParagraphStyle = new SimpleAttributeSet();
        StyleConstants.setLineSpacing(messageParagraphStyle, 0.2f);
        messageDoc.setParagraphAttributes(0, messageDoc.getLength(), messageParagraphStyle, false);
        messagesPane.repaint();
    }

    @Override
    public synchronized void insertUpdate(DocumentEvent e) {
        refresh();
    }

    @Override
    public synchronized void removeUpdate(DocumentEvent e) {
        refresh();
    }

    @Override
    public synchronized void changedUpdate(DocumentEvent e) {
    }

    private void saveTextContent(String text) {
        try {
            preferences.put(PREF_TEXT_CONTENT, text.replaceAll("\r", ""));

            // Save caret position and selection
            int caretPosition = textPane.getCaretPosition();
            int selectionStart = textPane.getSelectionStart();
            int selectionEnd = textPane.getSelectionEnd();

            preferences.putInt(PREF_CARET_POSITION, caretPosition);
            preferences.putInt(PREF_SELECTION_START, selectionStart);
            preferences.putInt(PREF_SELECTION_END, selectionEnd);

            preferences.flush(); // Ensure preferences are written to disk
        } catch (Exception e) {
            System.err.println("Failed to save text content: " + e.getMessage());
        }
    }

    private void loadTextContent() {
        String text = preferences.get(PREF_TEXT_CONTENT, "");
        if (!text.isEmpty()) {
            text = text.replaceAll("\r", "");
            try {
                StyledDocument doc = textPane.getStyledDocument();
                doc.insertString(0, text, null);

                // Apply line spacing
                SimpleAttributeSet paragraphStyle = new SimpleAttributeSet();
                StyleConstants.setLineSpacing(paragraphStyle, 0.2f);
                doc.setParagraphAttributes(0, doc.getLength(), paragraphStyle, false);

                // Restore caret position and selection
                int caretPosition = preferences.getInt(PREF_CARET_POSITION, 0);
                int selectionStart = preferences.getInt(PREF_SELECTION_START, 0);
                int selectionEnd = preferences.getInt(PREF_SELECTION_END, 0);

                // Ensure positions are within bounds
                int maxPos = doc.getLength();
                caretPosition = Math.min(caretPosition, maxPos);
                selectionStart = Math.min(selectionStart, maxPos);
                selectionEnd = Math.min(selectionEnd, maxPos);

                // Restore selection or caret position
                if (selectionStart != selectionEnd) {
                    textPane.setSelectionStart(selectionStart);
                    textPane.setSelectionEnd(selectionEnd);
                } else {
                    textPane.setCaretPosition(caretPosition);
                }
            } catch (BadLocationException e) {
                // Fallback to setText if insertString fails
                textPane.setText(text);
            }
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

    private void resetTokenColors() {
        TOKEN_COLORS.clear();
        TOKEN_COLORS.putAll(DEFAULT_TOKEN_COLORS);
        saveTokenColors();
        refresh();
    }

    private String colorToString(Color color) {
        return String.format("#%06X", color.getRGB() & 0xFFFFFF);
    }

    private Color parseColorString(String colorStr) {
        if (colorStr.startsWith("#")) {
            return new Color(java.lang.Integer.parseInt(colorStr.substring(1), 16));
        }
        return Color.BLACK;
    }

    private void showColorConfigDialog() {
        JDialog dialog = new JDialog(frame, "Configure Token Colors and Styles", true);
        dialog.setLayout(new BorderLayout());

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
        dialog.add(scrollPane, BorderLayout.CENTER);

        // Buttons panel
        JPanel buttonPanel = new JPanel();
        JButton okButton = new JButton("OK");
        okButton.addActionListener(e -> {
            saveTokenColors();
            refresh();
            dialog.dispose();
        });

        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(e -> {
            loadTokenColors(); // Reload original colors
            refresh(); // Refresh to show original colors
            dialog.dispose();
        });

        buttonPanel.add(okButton);
        buttonPanel.add(cancelButton);
        dialog.add(buttonPanel, BorderLayout.SOUTH);

        dialog.pack();
        dialog.setLocationRelativeTo(frame);
        dialog.setVisible(true);
    }

    private Color getContrastColor(Color color) {
        // Calculate luminance to determine if we should use black or white text
        double luminance = (0.299 * color.getRed() + 0.587 * color.getGreen() + 0.114 * color.getBlue()) / 255;
        return luminance > 0.5 ? Color.BLACK : Color.WHITE;
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
