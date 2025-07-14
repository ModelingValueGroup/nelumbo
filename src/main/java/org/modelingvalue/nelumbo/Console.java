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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

import javax.swing.*;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.DefaultHighlighter.DefaultHighlightPainter;
import javax.swing.text.Document;
import javax.swing.text.Element;

import org.modelingvalue.nelumbo.integers.Integer;
import org.modelingvalue.nelumbo.syntax.ParseException;
import org.modelingvalue.nelumbo.syntax.Parser;

public class Console extends WindowAdapter implements WindowListener, ActionListener, Runnable, DocumentListener, CaretListener {

    private final static String           READ        = "    ";
    private final static String           WRITE       = "    ";
    private final static String           ERROR       = "    ERROR: ";

    private final static String           INCREASE    = "INCREASE";
    private final static String           DECREASE    = "DECREASE";

    private final DefaultHighlightPainter pinkPainter = new DefaultHighlighter.DefaultHighlightPainter(Color.pink);

    private JFrame                        frame;
    private JTextArea                     textArea;
    private boolean                       quit;
    private int                           lineCount   = -1;
    private KnowledgeBase                 knowledgeBase;

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
                Parser.parse(Integer.class);
            } catch (ParseException e) {
                error(e.getMessage());
            }
        }));
    }

    private void init() {
        ImageIcon icon = new ImageIcon(getClass().getResource("nelumbo.png"));
        frame = new JFrame("Nelumbo");
        frame.setIconImage(icon.getImage());
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        Dimension frameSize = new Dimension((int) (screenSize.width / 2), (int) (screenSize.height / 2));
        int x = (int) (frameSize.width / 2);
        int y = (int) (frameSize.height / 2);
        frame.setBounds(x, y, frameSize.width, frameSize.height);

        textArea = new JTextArea();
        textArea.setFont(frame.getFont());
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
        textArea.getActionMap().put(INCREASE, new AbstractAction() {
            private static final long serialVersionUID = -425923171136898022L;

            @Override
            public void actionPerformed(ActionEvent e) {
                increase();
            }
        });
        textArea.getActionMap().put(DECREASE, new AbstractAction() {
            private static final long serialVersionUID = 3357017446274657221L;

            @Override
            public void actionPerformed(ActionEvent e) {
                decrease();
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
        textArea.append(READ);
        textArea.setCaretPosition(textArea.getDocument().getLength());
    }

    private void write(String output) {
        textArea.append(WRITE + output + "\n");
    }

    private void error(String error) {
        textArea.append(ERROR + error + "\n");
    }

    @Override
    public synchronized void run() {
        knowledgeBase = KnowledgeBase.CURRENT.get();
        String line = readLine();
        while (line != null) {
            try {
                for (Node root : Parser.parse(line)) {
                    if (root.type() == Type.RESULT) {
                        write(root.toString(2));
                    }
                }
            } catch (ParseException pe) {
                parseError(pe);
            }
            line = readLine();
        }
    }

    private void parseError(ParseException pe) {
        int start = getStartLength()[0] + pe.position() - 1;
        try {
            textArea.getHighlighter().addHighlight(start, start + pe.text().length(), pinkPainter);
        } catch (BadLocationException ble) {
            error(ble.getMessage());
        }
        error(pe.getShortMessage());
    }

    private String readLine() {
        prepareRead();
        while (true) {
            try {
                wait(100);
            } catch (InterruptedException ie) {
            }
            if (lineCount < textArea.getLineCount()) {
                int[] sl = getStartLength();
                if (sl[1] > 0) {
                    try {
                        return textArea.getText(sl[0], sl[1]);
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

    private int[] getStartLength() {
        Document doc = textArea.getDocument();
        Element root = doc.getDefaultRootElement();
        Element line = root.getElement(lineCount - 1);
        int start = line.getStartOffset();
        int length = line.getEndOffset() - start;
        return new int[]{start, length};
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
    }
}
