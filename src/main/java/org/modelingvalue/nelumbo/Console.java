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
import java.awt.Taskbar;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.Serial;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.swing.*;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Caret;
import javax.swing.text.DefaultEditorKit;
import javax.swing.text.DefaultHighlighter.DefaultHighlightPainter;
import javax.swing.text.Element;
import javax.swing.text.Highlighter;
import javax.swing.text.TextAction;

import org.modelingvalue.nelumbo.integers.Integer;
import org.modelingvalue.nelumbo.syntax.ParseException;
import org.modelingvalue.nelumbo.syntax.Parser;
import org.modelingvalue.nelumbo.syntax.Token;
import org.modelingvalue.nelumbo.syntax.Tokenizer;

public class Console extends WindowAdapter implements WindowListener, ActionListener, Runnable, DocumentListener, CaretListener {

    private final boolean                        COLOR_CONSOLE    = java.lang.Boolean.getBoolean("COLOR_CONSOLE_NELUMBO");

    private final static int                     COMMENT_POSITION = 32;
    private final static String                  PREFIX           = "  ";
    //
    private final static String                  INCREASE         = "INCREASE";
    private final static String                  DECREASE         = "DECREASE";
    private final static String                  UP_HISTORY       = "UP_HISTORY";
    private final static String                  DOWN_HISTORY     = "DOWN_HISTORY";
    //
    private final static DefaultHighlightPainter whitePainter     = new DefaultHighlightPainter(new Color(0xffffff));
    private final static DefaultHighlightPainter lightGreyPainter = new DefaultHighlightPainter(new Color(0xdddddd));
    private final static DefaultHighlightPainter greyPainter      = new DefaultHighlightPainter(new Color(0xaaaaaa));
    private final static DefaultHighlightPainter bluePainter      = new DefaultHighlightPainter(new Color(0x88ffff));
    private final static DefaultHighlightPainter pinkPainter      = new DefaultHighlightPainter(new Color(0xff88ff));
    private final static DefaultHighlightPainter yellowPainter    = new DefaultHighlightPainter(new Color(0xffff88));
    private final static DefaultHighlightPainter purplePainter    = new DefaultHighlightPainter(new Color(0x8888ff));
    private final static DefaultHighlightPainter greenPainter     = new DefaultHighlightPainter(new Color(0x88ff88));
    private final static DefaultHighlightPainter redPainter       = new DefaultHighlightPainter(new Color(0xff8888));

    public static void main(String[] arg) {
        new Console();
    }

    //===========================================================================================================================================
    private JFrame             frame;
    private JTextArea          textArea;
    private boolean            quit                 = false;
    private int                lineCount            = -1;
    private KnowledgeBase      knowledgeBase;
    private Action             deletePreviousAction;
    private final List<String> lineHistory          = new ArrayList<>();
    private int                currentLineInHistory = 0;

    public Console() {
        initWindow();
        initActions();
        initKnowledgeBase();
    }

    private void initWindow() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException e) {
            JOptionPane.showMessageDialog(null, "Unable to set system look and feel", "Warning", JOptionPane.WARNING_MESSAGE);
        }
        URL resource = getClass().getResource("nelumbo.png");
        assert resource != null;
        ImageIcon icon = new ImageIcon(resource);
        frame = new JFrame("Nelumbo");
        frame.setIconImage(icon.getImage());
        if (Taskbar.getTaskbar().isSupported(Taskbar.Feature.ICON_IMAGE)) {
            Taskbar.getTaskbar().setIconImage(icon.getImage());
        }
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        Dimension frameSize = new Dimension(screenSize.width / 2, screenSize.height / 2);
        int x = frameSize.width / 2;
        int y = frameSize.height / 2;
        frame.setBounds(x, y, frameSize.width, frameSize.height);

        textArea = new JTextArea();
        textArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, textArea.getFont().getSize() + 2));
        textArea.setEditable(true);
        textArea.addCaretListener(this);
        JButton clear = new JButton("clear");

        frame.getContentPane().setLayout(new BorderLayout());
        frame.getContentPane().add(new JScrollPane(textArea), BorderLayout.CENTER);
        frame.getContentPane().add(clear, BorderLayout.SOUTH);
        frame.setVisible(true);

        frame.addWindowListener(this);
        clear.addActionListener(this);
        textArea.getDocument().addDocumentListener(this);
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

        textArea.getInputMap().put(KeyStroke.getKeyStroke("UP"), UP_HISTORY);
        textArea.getInputMap().put(KeyStroke.getKeyStroke("DOWN"), DOWN_HISTORY);

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
        textArea.getActionMap().put(DefaultEditorKit.insertBreakAction, new TextAction(DefaultEditorKit.insertBreakAction) {
            @Serial
            private static final long serialVersionUID = -6025045977165493128L;

            @Override
            public void actionPerformed(ActionEvent e) {
                if (textArea == getTextComponent(e)) {
                    textArea.append("\n");
                }
            }
        });
        deletePreviousAction = textArea.getActionMap().get(DefaultEditorKit.deletePrevCharAction);
        textArea.getActionMap().put(DefaultEditorKit.deletePrevCharAction, new TextAction(DefaultEditorKit.deletePrevCharAction) {
            @Serial
            private static final long serialVersionUID = 846926090871223832L;

            @Override
            public void actionPerformed(ActionEvent e) {
                if (textArea == getTextComponent(e)) {
                    Caret caret = textArea.getCaret();
                    if (getStart() + PREFIX.length() < caret.getMark()) {
                        deletePreviousAction.actionPerformed(e);
                    }
                }
            }
        });
        textArea.getActionMap().put(UP_HISTORY, new TextAction(UP_HISTORY) {
            @Serial
            private static final long serialVersionUID = -8886848398049220330L;

            @Override
            public void actionPerformed(ActionEvent e) {
                if (textArea == getTextComponent(e)) {
                    if (currentLineInHistory > 0) {
                        String line = lineHistory.get(--currentLineInHistory);
                        textArea.replaceRange(line, getStart() + PREFIX.length(), getEnd() - 1);
                    }
                }
            }
        });
        textArea.getActionMap().put(DOWN_HISTORY, new TextAction(DOWN_HISTORY) {
            @Serial
            private static final long serialVersionUID = -5674886754830419207L;

            @Override
            public void actionPerformed(ActionEvent e) {
                if (textArea == getTextComponent(e)) {
                    if (currentLineInHistory < lineHistory.size()) {
                        String line = lineHistory.get(currentLineInHistory++);
                        textArea.replaceRange(line, getStart() + PREFIX.length(), getEnd() - 1);
                    }
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
        textArea.setFont(font.deriveFont(newSize));
    }

    private void decrease() {
        Font font = textArea.getFont();
        float newSize = Math.max(7f, font.getSize() / 1.2f);
        textArea.setFont(font.deriveFont(newSize));
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
    public synchronized void actionPerformed(ActionEvent evt) {
        clear();
    }

    private void clear() {
        textArea.getHighlighter().removeAllHighlights();
        knowledgeBase.init();
        textArea.setText("");
        prepareRead();
    }

    private void prepareRead() {
        lineCount = textArea.getLineCount();
        textArea.append(PREFIX);
        textArea.setCaretPosition(textArea.getDocument().getLength());
    }

    private void write(String output) {
        int pos = Math.max(1, COMMENT_POSITION - getLength());
        textArea.insert(" ".repeat(pos) + "// " + output, getEnd() - 1);
    }

    private void error(String error) {
        int pos = Math.max(1, COMMENT_POSITION - getLength());
        textArea.insert(" ".repeat(pos) + "// " + error, getEnd() - 1);
    }

    @Override
    public synchronized void run() {
        knowledgeBase = KnowledgeBase.CURRENT.get();
        for (String line = readLine(); line != null; line = readLine()) {
            lineHistory.add(line.substring(PREFIX.length(), line.length() - 1));
            currentLineInHistory = lineHistory.size();
            executeLine(line);
        }
    }

    private String readLine() {
        prepareRead();
        while (!quit) {
            try {
                wait(1_000_000);
            } catch (InterruptedException ie) {
                // ignore
            }
            if (lineCount < textArea.getLineCount() && 0 < getEnd()) {
                try {
                    return textArea.getText(getStart(), getLength());
                } catch (BadLocationException ble) {
                    error(ble.getMessage());
                }
            }
        }
        return null;
    }

    private void executeLine(String line) {
        try {
            if (COLOR_CONSOLE) {
                applySyntaxColors(line);
            }
            Tokenizer tokenizer = new Tokenizer(line, line);
            Parser parser = new Parser(tokenizer.tokenize());
            for (Node root : parser.parse()) {
                if (root.type().equals(Type.RESULT)) {
                    write(root.toString(1));
                }
            }
        } catch (ParseException pe) {
            error(pe.getShortMessage());
            try {
                int start = getStart() + pe.position();
                textArea.getHighlighter().addHighlight(start, start + pe.length(), redPainter);
            } catch (BadLocationException ble) {
                error(ble.getMessage());
            }
        }
    }

    private void applySyntaxColors(String line) throws ParseException {
        System.err.println("line=[" + U.traceable(line) + "]");
        textArea.insert(" ", getEnd() - 1);
        Tokenizer tokenizer = new Tokenizer(line, line);
        for (Token token = tokenizer.tokenize().firstAll(); token != null; token = token.nextAll()) {
            try {
                int beg = getStart() + token.position();
                int end = beg + token.text().length();
                System.err.printf("  - [%2d...%2d]: '%s'\n", beg, end, token);
                Highlighter.HighlightPainter hp = switch (token.type()) {
                case STRING -> bluePainter;
                case NUMBER, DECIMAL -> greenPainter;
                case QNAME, NAME -> yellowPainter;
                case SEMICOLON, OPERATOR, LPAREN, RPAREN, LBRACKET, RBRACKET, LBRACE, RBRACE, META_OPERATOR -> greyPainter;
                case COMMA -> purplePainter;
                case TYPE -> pinkPainter;
                case END_LINE_COMMENT, IN_LINE_COMMENT, SINGLEQUOTE -> lightGreyPainter;
                case HSPACE, NEWLINE, SKIP_NEWLINE -> whitePainter;
                case ERROR, ENDOFFILE -> redPainter;
                };
                if (hp != null) {
                    textArea.getHighlighter().addHighlight(beg, end, hp);
                }
            } catch (BadLocationException e) {
                System.err.println("BAD LOCATION: " + token.text() + " " + e.getMessage());
            }
        }
    }

    private int getStart() {
        Element line = getLastLineElement();
        return line == null ? 0 : line.getStartOffset();
    }

    private int getEnd() {
        Element line = getLastLineElement();
        return line == null ? 0 : line.getEndOffset();
    }

    private int getLength() {
        Element line = getLastLineElement();
        return line == null ? 0 : line.getEndOffset() - line.getStartOffset();
    }

    private Element getLastLineElement() {
        Element root = textArea.getDocument().getDefaultRootElement();
        return root.getElementCount() < lineCount ? null : root.getElement(lineCount - 1);
    }

    @Override
    public synchronized void insertUpdate(DocumentEvent e) {
        notifyAll();
    }

    @Override
    public void removeUpdate(DocumentEvent e) {
    }

    @Override
    public void changedUpdate(DocumentEvent e) {
    }

    @Override
    public void caretUpdate(CaretEvent e) {
        int newPos = getStart() + PREFIX.length();
        if (getStart() != 0 && e.getMark() < newPos && newPos < textArea.getDocument().getLength()) {
            textArea.setCaretPosition(newPos);
        }
    }
}
