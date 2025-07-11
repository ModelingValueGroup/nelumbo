package org.modelingvalue.nelumbo;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
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

    private final static String           READ             = "    ";
    private final static String           WRITE            = "    ";
    private final static String           ERROR            = "    ERROR: ";

    private final DefaultHighlightPainter pinkPainter      = new DefaultHighlighter.DefaultHighlightPainter(Color.pink);
    private final DefaultHighlightPainter lightGrayPainter = new DefaultHighlighter.DefaultHighlightPainter(Color.lightGray);

    private JFrame                        frame;
    private JTextArea                     textArea;
    private boolean                       quit;
    private int                           lineCount        = -1;
    private KnowledgeBase                 knowledgeBase;

    public Console() {
        frame = new JFrame("Nelumbo");
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        Dimension frameSize = new Dimension((int) (screenSize.width / 2), (int) (screenSize.height / 2));
        int x = (int) (frameSize.width / 2);
        int y = (int) (frameSize.height / 2);
        frame.setBounds(x, y, frameSize.width, frameSize.height);

        textArea = new JTextArea();
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

        quit = false; // signals the Threads that they should exit

        KnowledgeBase.run(this);
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
        knowledgeBase.init();
        textArea.setText("");
        prepareRead();
    }

    private void prepareRead() {
        lineCount = textArea.getLineCount();
        textArea.append(READ);
        textArea.setCaretPosition(textArea.getDocument().getLength());
    }

    @Override
    public synchronized void run() {
        knowledgeBase = KnowledgeBase.CURRENT.get();
        try {
            Parser.parse(Integer.class);
        } catch (ParseException e) {
            textArea.append(ERROR + e.getMessage() + "\n");
        }
        String line = readLine();
        while (line != null) {
            try {
                for (Node root : Parser.parse(line)) {
                    if (root.type() == Type.RESULT) {
                        textArea.append(WRITE + root.toString(2) + "\n");
                    }
                }
            } catch (ParseException pe) {
                int start = getStartLength()[0] + pe.position() - 1;
                try {
                    textArea.getHighlighter().addHighlight(start, start + pe.text().length(), pinkPainter);
                } catch (BadLocationException ble) {
                    textArea.append(ERROR + ble.getMessage() + "\n");
                }
                textArea.append(ERROR + pe.getShortMessage() + "\n");
            }
            line = readLine();
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
                int[] sl = getStartLength();
                if (sl[1] > 0) {
                    try {
                        return textArea.getText(sl[0], sl[1]);
                    } catch (BadLocationException ble) {
                        textArea.append(ERROR + ble.getMessage() + "\n");
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
