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

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultHighlighter.DefaultHighlightPainter;
import javax.swing.text.TextAction;

import org.modelingvalue.nelumbo.integers.Integer;
import org.modelingvalue.nelumbo.syntax.ParseException;
import org.modelingvalue.nelumbo.syntax.Parser;
import org.modelingvalue.nelumbo.syntax.Tokenizer;

import com.formdev.flatlaf.FlatLightLaf;

public class Editor extends WindowAdapter implements WindowListener, Runnable, DocumentListener {

    private final static String                  INCREASE   = "INCREASE";
    private final static String                  DECREASE   = "DECREASE";

    private final static DefaultHighlightPainter redPainter = new DefaultHighlightPainter(new Color(0xff8888));

    public static void main(String[] arg) {
        new Editor();
    }

    //===========================================================================================================================================
    private JFrame        frame;
    private JTextArea     textArea;
    private boolean       quit = false;
    private KnowledgeBase knowledgeBase;
    private JTextArea     message;

    public Editor() {
        initWindow();
        initActions();
        initKnowledgeBase();
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
        Dimension frameSize = new Dimension(screenSize.width / 2, screenSize.height / 2);
        int x = frameSize.width / 2;
        int y = frameSize.height / 2;
        frame.setBounds(x, y, frameSize.width, frameSize.height);

        // Create text area with modern styling
        textArea = new JTextArea();
        Font font = new Font(Font.MONOSPACED, Font.PLAIN, 14);
        textArea.setFont(font);
        textArea.setEditable(true);
        textArea.setLineWrap(false);
        textArea.setTabSize(4);
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
        KnowledgeBase.run(this, KnowledgeBase.run(() -> {
            try {
                Parser.parse(Integer.class);
                Parser.parse(org.modelingvalue.nelumbo.strings.String.class);
            } catch (ParseException e) {
                error(e.getMessage());
            }
        }));
    }

    private void increase() {
        Font font = textArea.getFont();
        float newSize = Math.min(100f, font.getSize() * 1.2f);
        font = font.deriveFont(newSize);
        textArea.setFont(font);
        message.setFont(font);
    }

    private void decrease() {
        Font font = textArea.getFont();
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
    public synchronized void run() {
        knowledgeBase = KnowledgeBase.CURRENT.get();
        String pre = "";
        for (String text = read(); text != null; text = read()) {
            if (!pre.equals(text)) {
                execute(text);
                pre = text;
            }
        }
    }

    private String read() {
        while (!quit) {
            try {
                wait(1_000_000);
            } catch (InterruptedException ie) {
                // ignore
            }
            return textArea.getText();
        }
        return null;
    }

    private void execute(String text) {
        message.setText("");
        textArea.getHighlighter().removeAllHighlights();
        knowledgeBase.init();
        try {
            Tokenizer tokenizer = new Tokenizer(text, "Editor");
            Parser parser = new Parser(tokenizer.tokenize());
            String out = "";
            int prevLine = 0, nextLine = 0;
            for (Node root : parser.parseMutiple().roots()) {
                if (root instanceof Query query) {
                    nextLine = query.lastToken().line();
                    out += emptyLines(nextLine - prevLine) + query.inferResult().toString() + "\n";
                    prevLine = ++nextLine;
                }
            }
            output(out);
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

}
