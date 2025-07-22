//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
//  (C) Copyright 2018-2025 Modeling Value Group B.V. (http://modelingvalue.org)                                         ~
//                                                                                                                       ~
//  Licensed under the GNU Lesser General Public License v3.0 (the 'License'). You may not use this file except in       ~
//  compliance with the License. You may obtain a copy of the License at: https://choosealicense.com/licenses/lgpl-3.0   ~
//  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on  ~
//  an 'AS IS' BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the   ~
//  specific language governing permissions and limitations under the License.                                           ~
//                                                                                                                       ~
//  Maintainers:                                                                                                         ~
//      Wim Bast, Tom Brus                                                                                               ~
//                                                                                                                       ~
//  Contributors:                                                                                                        ~
//      Ronald Krijgsheld ✝, Arjan Kok, Carel Bast                                                                       ~
// --------------------------------------------------------------------------------------------------------------------- ~
//  In Memory of Ronald Krijgsheld, 1972 - 2023                                                                          ~
//      Ronald was suddenly and unexpectedly taken from us. He was not only our long-term colleague and team member      ~
//      but also our friend. "He will live on in many of the lines of code you see below."                               ~
//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

package org.modelingvalue.nelumbo;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
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
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.DefaultHighlighter.DefaultHighlightPainter;
import javax.swing.text.Document;
import javax.swing.text.Element;
import javax.swing.text.TextAction;

import org.modelingvalue.nelumbo.syntax.ParseException;
import org.modelingvalue.nelumbo.syntax.Parser;

public class Console extends WindowAdapter implements WindowListener, ActionListener, Runnable, DocumentListener, CaretListener {

    private final int                     COMMEND_POSITION     = 32;
    private final static String           PREFIX               = "  ";

    private final static String           INCREASE             = "INCREASE";
    private final static String           DECREASE             = "DECREASE";
    private final static String           UP_HISTORY           = "UP_HISTORY";
    private final static String           DOWN_HISTORY         = "DOWN_HISTORY";

    private final DefaultHighlightPainter pinkPainter          = new DefaultHighlighter.DefaultHighlightPainter(Color.pink);

    private JFrame                        frame;
    private JTextArea                     textArea;
    private boolean                       quit;
    private int                           lineCount            = -1;
    private KnowledgeBase                 knowledgeBase;
    private Action                        deletePreviousAction;
    private List<String>                  lineHistory          = new ArrayList<>();
    private int                           currentLineInHistory = 0;

    public Console() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException e) {
            e.printStackTrace();
        }
        init();
        actions();
        quit = false; // signals the Threads that they should exit
        KnowledgeBase.run(this, KnowledgeBase.run(() -> {
            try {
                Parser.parse(org.modelingvalue.nelumbo.integers.Integer.class);
                Parser.parse(org.modelingvalue.nelumbo.strings.String.class);
            } catch (ParseException e) {
                error(e.getMessage());
            }
        }));
    }

    private void init() {
        ImageIcon icon = new ImageIcon(getClass().getResource("nelumbo.png"));
        frame = new JFrame("Nelumbo");
        frame.setIconImage(icon.getImage());
        Taskbar.getTaskbar().setIconImage(icon.getImage());
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        Dimension frameSize = new Dimension((int) (screenSize.width / 2), (int) (screenSize.height / 2));
        int x = (int) (frameSize.width / 2);
        int y = (int) (frameSize.height / 2);
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

    private void actions() {
        textArea.getInputMap().put(KeyStroke.getKeyStroke('+', InputEvent.CTRL_DOWN_MASK), INCREASE);
        textArea.getInputMap().put(KeyStroke.getKeyStroke('=', InputEvent.CTRL_DOWN_MASK), INCREASE);
        textArea.getInputMap().put(KeyStroke.getKeyStroke('-', InputEvent.CTRL_DOWN_MASK), DECREASE);
        textArea.getInputMap().put(KeyStroke.getKeyStroke('_', InputEvent.CTRL_DOWN_MASK), DECREASE);
        textArea.getInputMap().put(KeyStroke.getKeyStroke("UP"), UP_HISTORY);
        textArea.getInputMap().put(KeyStroke.getKeyStroke("DOWN"), DOWN_HISTORY);
        textArea.getActionMap().put(INCREASE, new TextAction(INCREASE) {
            private static final long serialVersionUID = -425923171136898022L;

            @Override
            public void actionPerformed(ActionEvent e) {
                if (textArea == getTextComponent(e)) {
                    increase();
                }
            }
        });
        textArea.getActionMap().put(DECREASE, new TextAction(DECREASE) {
            private static final long serialVersionUID = 3357017446274657221L;

            @Override
            public void actionPerformed(ActionEvent e) {
                if (textArea == getTextComponent(e)) {
                    decrease();
                }
            }
        });
        textArea.getActionMap().put(DefaultEditorKit.insertBreakAction, new TextAction(DefaultEditorKit.insertBreakAction) {
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
            private static final long serialVersionUID = 846926090871223832L;

            @Override
            public void actionPerformed(ActionEvent e) {
                if (textArea == getTextComponent(e)) {
                    Caret caret = textArea.getCaret();
                    int[] se = getStartEnd();
                    if (se[0] + PREFIX.length() < caret.getMark()) {
                        deletePreviousAction.actionPerformed(e);
                    }
                }
            }
        });
        textArea.getActionMap().put(UP_HISTORY, new TextAction(UP_HISTORY) {
            private static final long serialVersionUID = -8886848398049220330L;

            @Override
            public void actionPerformed(ActionEvent e) {
                if (textArea == getTextComponent(e)) {
                    if (currentLineInHistory > 0) {
                        String line = lineHistory.get(--currentLineInHistory);
                        int[] se = getStartEnd();
                        textArea.replaceRange(line, se[0] + PREFIX.length(), se[1] - 1);
                    }
                }
            }
        });
        textArea.getActionMap().put(DOWN_HISTORY, new TextAction(DOWN_HISTORY) {
            private static final long serialVersionUID = -5674886754830419207L;

            @Override
            public void actionPerformed(ActionEvent e) {
                if (textArea == getTextComponent(e)) {
                    if (currentLineInHistory < lineHistory.size()) {
                        String line = lineHistory.get(currentLineInHistory++);
                        int[] se = getStartEnd();
                        textArea.replaceRange(line, se[0] + PREFIX.length(), se[1] - 1);
                    }
                }
            }
        });
    }

    private void increase() {
        Font font = textArea.getFont();
        textArea.setFont(font.deriveFont(font.getSize() + 2.0f));
    }

    private void decrease() {
        Font font = textArea.getFont();
        textArea.setFont(font.deriveFont(font.getSize() - 2.0f));
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
        int[] se = getStartEnd();
        int pos = Math.max(1, COMMEND_POSITION - (se[1] - se[0]));
        textArea.insert(" ".repeat(pos) + "// " + output, se[1] - 1);
    }

    private void error(String error) {
        int[] se = getStartEnd();
        int pos = Math.max(1, COMMEND_POSITION - (se[1] - se[0]));
        textArea.insert(" ".repeat(pos) + "// " + error, se[1] - 1);
    }

    @Override
    public synchronized void run() {
        knowledgeBase = KnowledgeBase.CURRENT.get();
        String line = readLine();
        while (line != null) {
            lineHistory.add(line.substring(PREFIX.length(), line.length() - 1));
            currentLineInHistory = lineHistory.size();
            try {
                for (Node root : Parser.parse(line)) {
                    if (root.type().equals(Type.RESULT)) {
                        write(root.toString(1));
                    }
                }
            } catch (ParseException pe) {
                parseError(pe);
            }
            line = readLine();
        }
    }

    private void parseError(ParseException pe) {
        error(pe.getShortMessage());
        int start = getStartEnd()[0] + pe.position() - 1;
        try {
            textArea.getHighlighter().addHighlight(start, start + pe.length(), pinkPainter);
        } catch (BadLocationException ble) {
            error(ble.getMessage());
        }
    }

    private String readLine() {
        prepareRead();
        while (true) {
            try {
                wait(100);
            } catch (InterruptedException ie) {
            }
            if (lineCount < textArea.getLineCount()) {
                int[] se = getStartEnd();
                if (se[1] > 0) {
                    try {
                        return textArea.getText(se[0], se[1] - se[0]);
                    } catch (BadLocationException ble) {
                        error(ble.getMessage());
                    }
                }
            }
            if (quit) {
                return null;
            }
        }
    }

    private int[] getStartEnd() {
        Document doc = textArea.getDocument();
        Element root = doc.getDefaultRootElement();
        Element line = root.getElement(lineCount - 1);
        if (line == null) {
            return null;
        }
        int start = line.getStartOffset();
        int end = line.getEndOffset();
        return new int[]{start, end};
    }

    public static void main(String[] arg) {
        new Console();
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
        int[] se = getStartEnd();
        if (se != null && e.getMark() < se[0] + PREFIX.length()) {
            textArea.setCaretPosition(se[0] + PREFIX.length());
        }
    }
}
