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

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JTextPane;
import javax.swing.KeyStroke;
import javax.swing.UIManager;
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
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Insets;
import java.awt.Taskbar;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.Serial;
import java.net.URL;
import java.util.Objects;
import java.util.prefs.Preferences;

import com.formdev.flatlaf.FlatLightLaf;
import org.modelingvalue.nelumbo.integers.Integer;
import org.modelingvalue.nelumbo.syntax.ParseException;
import org.modelingvalue.nelumbo.syntax.Parser;
import org.modelingvalue.nelumbo.syntax.Token;
import org.modelingvalue.nelumbo.syntax.TokenType;
import org.modelingvalue.nelumbo.syntax.Tokenizer;
import org.modelingvalue.nelumbo.syntax.Tokenizer.TokenizerResult;

public class NelumboEditor extends WindowAdapter implements WindowListener, Runnable, DocumentListener {

    private final static String INCREASE = "INCREASE";
    private final static String DECREASE = "DECREASE";

    private final static DefaultHighlightPainter redPainter = new DefaultHighlightPainter(new Color(0xff8888));

    private static final String PREF_TEXT_CONTENT    = "textContent";
    private static final String PREF_CARET_POSITION  = "caretPosition";
    private static final String PREF_SELECTION_START = "selectionStart";
    private static final String PREF_SELECTION_END   = "selectionEnd";

    public static void main(String[] arg) {
        new NelumboEditor();
    }

    //===========================================================================================================================================
    private       JFrame        frame;
    private       JTextPane     textArea;
    private       boolean       quit        = false;
    private       KnowledgeBase knowledgeBase;
    private       JTextArea     message;
    private final Preferences   preferences = Preferences.userNodeForPackage(NelumboEditor.class);

    public NelumboEditor() {
        initWindow();
        initActions();
        loadTextContent();
        initKnowledgeBase(); // execution stays here until the user quits the application. This is by design!
        // only reaching this point after the user quits
    }

    private void initWindow() {
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
        Dimension frameSize  = new Dimension(screenSize.width / 2, screenSize.height / 2);
        int       x          = frameSize.width / 2;
        int       y          = frameSize.height / 2;
        frame.setBounds(x, y, frameSize.width, frameSize.height);

        // Create text pane with modern styling (no wrapping)
        textArea = new JTextPane() {
            @Serial
            private static final long serialVersionUID = 1L;

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
        };

        // Use a custom EditorKit that doesn't wrap lines
        textArea.setEditorKit(new StyledEditorKit() {
            @Serial
            private static final long serialVersionUID = 1L;

            @Override
            public ViewFactory getViewFactory() {
                return new NoWrapViewFactory();
            }
        });

        Font font = new Font(Font.MONOSPACED, Font.PLAIN, 14);
        textArea.setFont(font);
        textArea.setEditable(true);
        Insets margin = new Insets(15, 15, 15, 15);
        textArea.setMargin(margin);

        // Create message area with modern styling
        message = new JTextArea("");
        message.setEditable(false);
        message.setFont(font);
        message.setMargin(margin);
        message.setLineWrap(false);
        message.setBackground(new Color(0xF5F5F5));

        // Create scroll panes with borders
        JScrollPane textScroll = new JScrollPane(textArea);
        textScroll.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 5));

        JScrollPane messageScroll = new JScrollPane(message);
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
        frame.setVisible(true);

        frame.addWindowListener(this);
        textArea.getDocument().addDocumentListener(this);

        // Set focus on text area
        textArea.requestFocusInWindow();
    }

    private void initActions() {
        textArea.getInputMap().put(KeyStroke.getKeyStroke('+', InputEvent.CTRL_DOWN_MASK), INCREASE);
        textArea.getInputMap().put(KeyStroke.getKeyStroke('=', InputEvent.CTRL_DOWN_MASK), INCREASE);
        textArea.getInputMap().put(KeyStroke.getKeyStroke('-', InputEvent.CTRL_DOWN_MASK), DECREASE);
        textArea.getInputMap().put(KeyStroke.getKeyStroke('_', InputEvent.CTRL_DOWN_MASK), DECREASE);

        textArea.getInputMap().put(KeyStroke.getKeyStroke('+', InputEvent.META_DOWN_MASK), INCREASE);
        textArea.getInputMap().put(KeyStroke.getKeyStroke('=', InputEvent.META_DOWN_MASK), INCREASE);
        textArea.getInputMap().put(KeyStroke.getKeyStroke('-', InputEvent.META_DOWN_MASK), DECREASE);
        textArea.getInputMap().put(KeyStroke.getKeyStroke('_', InputEvent.META_DOWN_MASK), DECREASE);

        textArea.getActionMap().put(INCREASE, new TextAction(INCREASE) {
            @Serial
            private static final long serialVersionUID = -425923171136898022L;

            @Override
            public void actionPerformed(ActionEvent e) {
                if (textArea == getTextComponent(e)) {
                    increase();
                }
            }
        });
        textArea.getActionMap().put(DECREASE, new TextAction(DECREASE) {
            @Serial
            private static final long serialVersionUID = 3357017446274657221L;

            @Override
            public void actionPerformed(ActionEvent e) {
                if (textArea == getTextComponent(e)) {
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
                             error(e.getMessage());
                         }
                     })
                     .run(this);
    }

    private void increase() {
        Font  font    = textArea.getFont();
        float newSize = Math.min(100f, font.getSize() * 1.2f);
        font = font.deriveFont(newSize);
        textArea.setFont(font);
        message.setFont(font);
    }

    private void decrease() {
        Font  font    = textArea.getFont();
        float newSize = Math.max(7f, font.getSize() / 1.2f);
        font = font.deriveFont(newSize);
        textArea.setFont(font);
        message.setFont(font);
    }

    @Override
    public synchronized void windowClosed(WindowEvent evt) {
        quit = true;
        notifyAll();
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
        String pre = null;
        while (!quit) {
            String text = textArea.getText();
            if (!Objects.equals(pre, text)) {
                saveTextContent();
                execute(text);
                pre = text;
            }
            waitForChange();
        }
    }

    private void waitForChange() {
        if (!quit) {
            synchronized (this) {
                try {
                    wait(1_000_000);
                } catch (InterruptedException ie) {
                    // ignore
                }
            }
        }
    }

    private void execute(String text) {
        message.setText("");
        textArea.getHighlighter().removeAllHighlights();
        knowledgeBase.init();

        // Reset all text to default color first
        StyledDocument     doc         = textArea.getStyledDocument();
        SimpleAttributeSet defaultAttr = new SimpleAttributeSet();
        StyleConstants.setForeground(defaultAttr, Color.BLACK);
        doc.setCharacterAttributes(0, doc.getLength(), defaultAttr, true);

        try {
            Tokenizer       tokenizer       = new Tokenizer(text, "Editor");
            TokenizerResult tokenizerResult = tokenizer.tokenize();

            // Set foreground color for NUMBER tokens to green
            SimpleAttributeSet greenAttr = new SimpleAttributeSet();
            StyleConstants.setForeground(greenAttr, Color.GREEN);
            StyleConstants.setBackground(greenAttr, Color.ORANGE);
            for (Token token : tokenizerResult.listAll()) {
                if (token.type() == TokenType.NUMBER) {
                    doc.setCharacterAttributes(token.index(), token.text().length(), greenAttr, false);
                }
            }

            Parser        parser   = new Parser(tokenizerResult);
            StringBuilder out      = new StringBuilder();
            int           prevLine = 0;
            int           nextLine;
            for (Node root : parser.parseMutiple().roots()) {
                if (root instanceof Query query) {
                    nextLine = query.lastToken().line();
                    out.append(emptyLines(nextLine - prevLine)).append(query.inferResult().toString()).append("\n");
                    prevLine = ++nextLine;
                }
            }
            output(out.toString());
        } catch (ParseException pe) {
            error(emptyLines(pe.line()) + pe.getShortMessage());
            try {
                textArea.getHighlighter().addHighlight(pe.index(), pe.index() + pe.length(), redPainter);
            } catch (BadLocationException ble) {
                error(ble.getMessage());
            }
        }
    }

    private String emptyLines(int nr) {
        return "\n".repeat(nr);
    }

    private void error(String msg) {
        message.setText(msg);
        message.repaint();
    }

    private void output(String msg) {
        message.setText(msg);
        message.repaint();
    }

    @Override
    public synchronized void insertUpdate(DocumentEvent e) {
        notifyAll();
    }

    @Override
    public synchronized void removeUpdate(DocumentEvent e) {
        notifyAll();
    }

    @Override
    public synchronized void changedUpdate(DocumentEvent e) {
        notifyAll();
    }

    private void saveTextContent() {
        try {
            String text = textArea.getText();
            preferences.put(PREF_TEXT_CONTENT, text);

            // Save caret position and selection
            int caretPosition  = textArea.getCaretPosition();
            int selectionStart = textArea.getSelectionStart();
            int selectionEnd   = textArea.getSelectionEnd();

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
            try {
                StyledDocument doc = textArea.getStyledDocument();
                doc.insertString(0, text, null);

                // Restore caret position and selection
                int caretPosition  = preferences.getInt(PREF_CARET_POSITION, 0);
                int selectionStart = preferences.getInt(PREF_SELECTION_START, 0);
                int selectionEnd   = preferences.getInt(PREF_SELECTION_END, 0);

                // Ensure positions are within bounds
                int maxPos = doc.getLength();
                caretPosition  = Math.min(caretPosition, maxPos);
                selectionStart = Math.min(selectionStart, maxPos);
                selectionEnd   = Math.min(selectionEnd, maxPos);

                // Restore selection or caret position
                if (selectionStart != selectionEnd) {
                    textArea.setSelectionStart(selectionStart);
                    textArea.setSelectionEnd(selectionEnd);
                } else {
                    textArea.setCaretPosition(caretPosition);
                }
            } catch (BadLocationException e) {
                // Fallback to setText if insertString fails
                textArea.setText(text);
            }
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
