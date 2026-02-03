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

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextPane;
import javax.swing.KeyStroke;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultHighlighter.DefaultHighlightPainter;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import javax.swing.text.TextAction;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Taskbar;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Serial;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;
import java.util.UUID;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;

import org.modelingvalue.collections.List;
import org.modelingvalue.nelumbo.Evaluatable;
import org.modelingvalue.nelumbo.Import;
import org.modelingvalue.nelumbo.KnowledgeBase;
import org.modelingvalue.nelumbo.Node;
import org.modelingvalue.nelumbo.Query;
import org.modelingvalue.nelumbo.syntax.ParseException;
import org.modelingvalue.nelumbo.syntax.Parser;
import org.modelingvalue.nelumbo.syntax.ParserResult;
import org.modelingvalue.nelumbo.syntax.Token;
import org.modelingvalue.nelumbo.syntax.Tokenizer;
import org.modelingvalue.nelumbo.syntax.Tokenizer.TokenizerResult;

/**
 * Represents an individual editor window in the multi-window architecture.
 * Each window has its own JFrame, text panes, knowledge base execution loop,
 * and viewer dialogs.
 */
public class EditorWindow extends WindowAdapter implements WindowListener, Runnable, DocumentListener, EditorImportResolver.ImportChangeListener {

    private static final String EDITOR_FILE_NAME   = "editor.nl";
    private static final String MESSAGES_FILE_NAME = "messages.nl";
    private final static String INCREASE           = "INCREASE";
    private final static String DECREASE           = "DECREASE";

    private final static DefaultHighlightPainter redPainter   = new DefaultHighlightPainter(new Color(0xffaaaa));
    private final static DefaultHighlightPainter greenPainter = new DefaultHighlightPainter(new Color(0xaaffaa));

    private final String        windowId;
    private final NelumboEditor application;
    private       boolean       isExample;
    private final String        examplePath;
    private final String        exampleDisplayName;
    private final Preferences   preferences;
    private       int           windowNumber;  // Window number for regular windows

    private KnowledgeBase                    knowledgeBase;
    private JFrame                           frame;
    private JTextPane                        messagesPane;
    private JTextPane                        textPane;
    private JMenu                            windowsMenu;
    private WindowManager.WindowListListener windowListListener;
    private boolean                          quit;
    private boolean                          refreshRequested;
    private TreeViewerDialog                 treeViewerDialog;
    private KnowledgeBaseViewerDialog        knowledgeBaseViewerDialog;
    private TokenizerResult                  lastTokenizerResult;
    private ParserResult                     lastParserResult;
    private Set<String>                      currentImports = new HashSet<>();  // Tracks current editor imports

    /**
     * Creates a new regular editor window with a pre-assigned window number.
     */
    public EditorWindow(NelumboEditor application, String windowId, int windowNumber) {
        this.application        = application;
        this.windowId           = windowId != null ? windowId : UUID.randomUUID().toString();
        this.isExample          = false;
        this.examplePath        = null;
        this.exampleDisplayName = null;
        this.windowNumber       = windowNumber;
        this.preferences        = Preferences.userNodeForPackage(NelumboEditor.class);
    }

    /**
     * Creates a new editor window for an example.
     */
    public EditorWindow(NelumboEditor application, String windowId, boolean isExample, String examplePath, String exampleDisplayName) {
        this.application        = application;
        this.windowId           = windowId != null ? windowId : UUID.randomUUID().toString();
        this.isExample          = isExample;
        this.examplePath        = examplePath;
        this.exampleDisplayName = exampleDisplayName;
        this.windowNumber       = -1;  // Examples don't have window numbers
        this.preferences        = Preferences.userNodeForPackage(NelumboEditor.class);
    }

    public String getWindowId() {
        return windowId;
    }

    public JFrame getFrame() {
        return frame;
    }

    public int getWindowNumber() {
        return windowNumber;
    }

    public KnowledgeBase getKnowledgeBase() {
        return knowledgeBase;
    }

    /**
     * Initializes and shows the window.
     */
    public void init() {
        initWindow();
        initActions();
        if (isExample && examplePath != null) {
            loadExampleContent();
        } else {
            loadTextContent();
        }
        restoreDialogVisibility();
    }

    /**
     * Starts the knowledge base execution loop.
     * This method blocks until the window is closed.
     */
    public void startExecutionLoop() {
        KnowledgeBase.BASE.run(this);
    }

    private void initWindow() {
        URL       resource = getClass().getResource("/org/modelingvalue/nelumbo/nelumbo.png");
        ImageIcon icon     = resource != null ? new ImageIcon(resource) : new ImageIcon();

        // Determine window title
        String title;
        if (isExample && exampleDisplayName != null) {
            // Example/library windows use just the display name
            title = exampleDisplayName;
        } else {
            // Regular windows use "editor.nelumbo_<n>" format for easy import reference
            title = "editor.nelumbo_" + windowNumber;
        }

        frame = new JFrame(title);
        frame.setIconImage(icon.getImage());
        if (Taskbar.getTaskbar().isSupported(Taskbar.Feature.ICON_IMAGE)) {
            Taskbar.getTaskbar().setIconImage(icon.getImage());
        }
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        Dimension frameSize  = new Dimension(screenSize.width / 2, screenSize.height / 2);
        frame.setPreferredSize(frameSize);
        frame.setSize(frameSize);

        // Example windows start as read-only (non-editable)
        textPane     = new NelumboEditor.NonWrappingJTextPane(!isExample, 0xffffff);
        messagesPane = new NelumboEditor.NonWrappingJTextPane(false, 0xF5F5F5);

        // Add key listener to detect edit attempts on read-only windows
        if (isExample) {
            textPane.addKeyListener(new KeyAdapter() {
                @Override
                public void keyTyped(KeyEvent e) {
                    if (!textPane.isEditable()) {
                        char c = e.getKeyChar();
                        // Detect printable characters or common edit keys
                        if (c != KeyEvent.CHAR_UNDEFINED && !e.isControlDown() && !e.isMetaDown()) {
                            promptToConvertToEditable();
                        }
                    }
                }

                @Override
                public void keyPressed(KeyEvent e) {
                    if (!textPane.isEditable()) {
                        int keyCode = e.getKeyCode();
                        // Detect delete, backspace, or paste shortcuts
                        if (keyCode == KeyEvent.VK_DELETE || keyCode == KeyEvent.VK_BACK_SPACE ||
                            ((e.isControlDown() || e.isMetaDown()) && (keyCode == KeyEvent.VK_V || keyCode == KeyEvent.VK_X))) {
                            promptToConvertToEditable();
                        }
                    }
                }
            });
        }

        // Create scroll panes with borders
        JScrollPane textScroll    = new JScrollPane(textPane);
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

        // Setup menu bar
        JMenuBar menuBar = createMenuBar();
        frame.setJMenuBar(menuBar);

        // Use window-specific key for bounds persistence
        new DialogBoundsUtil(frame, NelumboEditor.class, "window." + windowId, null);
        frame.setVisible(true);

        frame.addWindowListener(this);
        textPane.getDocument().addDocumentListener(this);

        // Set focus on text area
        textPane.requestFocusInWindow();
    }

    private JMenuBar createMenuBar() {
        JMenuBar menuBar = new JMenuBar();

        // File menu
        JMenu     fileMenu      = new JMenu("File");
        JMenuItem newWindowItem = new JMenuItem("New Window");
        newWindowItem.setAccelerator(KeyStroke.getKeyStroke('N', Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
        newWindowItem.addActionListener(e -> application.createNewWindow());
        fileMenu.add(newWindowItem);

        JMenuItem closeWindowItem = new JMenuItem("Close Window");
        closeWindowItem.setAccelerator(KeyStroke.getKeyStroke('W', Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
        closeWindowItem.addActionListener(e -> closeWindow());
        fileMenu.add(closeWindowItem);

        menuBar.add(fileMenu);

        // Colors menu
        JMenu     colorsMenu      = new JMenu("Colors");
        JMenuItem configureColors = new JMenuItem("Configure Token Colors...");
        configureColors.addActionListener(e -> application.showColorConfigDialog(frame));

        JMenuItem resetColors = new JMenuItem("Reset to Defaults");
        resetColors.addActionListener(e -> {
            application.resetTokenColors();
            refresh();
        });

        colorsMenu.add(configureColors);
        colorsMenu.add(resetColors);
        menuBar.add(colorsMenu);

        // View menu
        JMenu     viewMenu       = new JMenu("View");
        JMenuItem treeViewerItem = new JMenuItem("Tree Viewer...");
        treeViewerItem.setAccelerator(KeyStroke.getKeyStroke('T', Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
        treeViewerItem.addActionListener(e -> toggleTreeViewer());
        viewMenu.add(treeViewerItem);

        JMenuItem knowledgeBaseViewerItem = new JMenuItem("Knowledge Base Viewer...");
        knowledgeBaseViewerItem.setAccelerator(KeyStroke.getKeyStroke('K', Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
        knowledgeBaseViewerItem.addActionListener(e -> toggleKnowledgeBaseViewer());
        viewMenu.add(knowledgeBaseViewerItem);

        menuBar.add(viewMenu);

        // Windows menu
        windowsMenu = new JMenu("Windows");
        updateWindowsMenu();
        menuBar.add(windowsMenu);

        // Examples menu
        JMenu examplesMenu = application.createExamplesMenu();
        menuBar.add(examplesMenu);

        // Register for window list changes
        windowListListener = this::updateWindowsMenu;
        application.getWindowManager().addWindowListListener(windowListListener);

        return menuBar;
    }

    /**
     * Updates the Windows menu to reflect the current list of open windows.
     */
    private void updateWindowsMenu() {
        if (windowsMenu == null) {
            return;
        }
        // Use SwingUtilities to ensure we're on the EDT
        javax.swing.SwingUtilities.invokeLater(() -> {
            windowsMenu.removeAll();

            java.util.List<EditorWindow> windows = application.getWindowManager().getWindowsInOrder();
            int                          index   = 1;
            for (EditorWindow window : windows) {
                String    title = window.getFrame() != null ? window.getFrame().getTitle() : "Nelumbo Editor";
                JMenuItem item  = new JMenuItem(index + " " + title);

                // Add keyboard shortcut for first 9 windows (Cmd-1 through Cmd-9)
                if (index <= 9) {
                    item.setAccelerator(KeyStroke.getKeyStroke(
                            Character.forDigit(index, 10),
                            Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
                }

                // Mark current window
                if (window == this) {
                    item.setEnabled(false);  // Can't switch to self
                }

                item.addActionListener(e -> bringWindowToFront(window));
                windowsMenu.add(item);
                index++;
            }
        });
    }

    /**
     * Brings the specified window to the front.
     */
    private void bringWindowToFront(EditorWindow window) {
        JFrame targetFrame = window.getFrame();
        if (targetFrame != null) {
            targetFrame.toFront();
            targetFrame.requestFocus();
            // On some platforms, we need to restore if minimized
            if (targetFrame.getExtendedState() == JFrame.ICONIFIED) {
                targetFrame.setExtendedState(JFrame.NORMAL);
            }
        }
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

    private void increase() {
        Font  font    = textPane.getFont();
        float newSize = Math.min(100f, font.getSize() * 1.2f);
        font = font.deriveFont(newSize);
        textPane.setFont(font);
        messagesPane.setFont(font);
    }

    private void decrease() {
        Font  font    = textPane.getFont();
        float newSize = Math.max(7f, font.getSize() / 1.2f);
        font = font.deriveFont(newSize);
        textPane.setFont(font);
        messagesPane.setFont(font);
    }

    private void closeWindow() {
        frame.setVisible(false);
        frame.dispose();
    }

    /**
     * Prompts the user to convert this read-only example window to an editable window.
     */
    private void promptToConvertToEditable() {
        int result = JOptionPane.showConfirmDialog(
                frame,
                "This is a read-only example. Would you like to create an editable copy?",
                "Convert to Editable",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE
                                                  );

        if (result == JOptionPane.YES_OPTION) {
            convertToEditableWindow();
        }
    }

    /**
     * Converts this example window to a regular editable window.
     */
    private void convertToEditableWindow() {
        isExample = false;
        textPane.setEditable(true);

        // Atomically assign a window number
        WindowManager windowManager = application.getWindowManager();
        windowNumber = windowManager.assignNextWindowNumber(windowId);

        // Update the title
        String newTitle = "editor.nelumbo_" + windowNumber;
        frame.setTitle(newTitle);

        // Notify that window list changed (title updated)
        windowManager.notifyWindowListChanged();

        // Focus the text pane for editing
        textPane.requestFocusInWindow();
    }

    @Override
    public synchronized void windowClosed(WindowEvent evt) {
        quit = true;
        refresh();
        // Remove window list listener to avoid memory leaks
        if (windowListListener != null) {
            application.getWindowManager().removeWindowListListener(windowListListener);
            windowListListener = null;
        }
        // Clean up all import dependencies
        EditorImportResolver resolver = application.getEditorImportResolver();
        if (resolver != null) {
            resolver.removeAllDependencies(this);
        }
        application.windowClosed(this);
    }

    @Override
    public synchronized void windowClosing(WindowEvent evt) {
        // Save state before closing (content is only saved for non-example windows)
        saveTextContent(getDocumentText(textPane));
        saveDialogVisibility();
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

    /**
     * Called when an import this window depends on has changed.
     * Triggers a re-parse to update the content.
     */
    @Override
    public void onImportChanged(String importName) {
        refresh();
    }

    /**
     * Extracts editor imports (imports starting with "editor.") from the parser result.
     */
    private Set<String> extractEditorImports(ParserResult result) {
        Set<String> imports = new HashSet<>();
        for (Node root : result.roots()) {
            collectEditorImports(root, imports);
        }
        return imports;
    }

    /**
     * Recursively collects editor imports from a node.
     */
    private void collectEditorImports(Node node, Set<String> imports) {
        if (node instanceof Import imp) {
            String name = imp.name();
            if (name.startsWith("editor.")) {
                imports.add(name);
            }
        }
        // Check children
        for (int i = 0; i < node.length(); i++) {
            Object child = node.get(i);
            if (child instanceof Node childNode) {
                collectEditorImports(childNode, imports);
            }
        }
    }

    /**
     * Updates the import dependency tracking based on the current parser result.
     * Registers this window as a listener for imports it depends on,
     * and unregisters from imports it no longer depends on.
     */
    private void updateImportDependencies(ParserResult result) {
        EditorImportResolver resolver = application.getEditorImportResolver();
        if (resolver == null) {
            return;
        }

        Set<String> newImports = extractEditorImports(result);

        // Remove dependencies that are no longer needed
        for (String oldImport : currentImports) {
            if (!newImports.contains(oldImport)) {
                resolver.removeDependency(oldImport, this);
            }
        }

        // Add new dependencies
        for (String newImport : newImports) {
            if (!currentImports.contains(newImport)) {
                resolver.addDependency(newImport, this);
            }
        }

        currentImports = newImports;
    }

    /**
     * Notifies windows that depend on this window's content that it has changed.
     */
    private void notifyDependentWindows() {
        // Only regular windows (with a window number) can be imported
        if (windowNumber <= 0) {
            return;
        }

        EditorImportResolver resolver = application.getEditorImportResolver();
        if (resolver != null) {
            String importName = EditorImportResolver.getImportName(windowNumber);
            resolver.notifyImportChanged(importName);
        }
    }

    private void execute() {
        prepareForExecute();
        String          text            = getDocumentText(textPane);
        Tokenizer       tokenizer       = new Tokenizer(text, EDITOR_FILE_NAME);
        TokenizerResult tokenizerResult = tokenizer.tokenize();
        ParserResult    result          = new Parser(tokenizerResult).parseMutipleNonThrowing();
        showColors(textPane, tokenizerResult);
        showResults(result);

        // Update import dependencies after successful parse
        updateImportDependencies(result);

        // Notify windows that depend on this window's content
        notifyDependentWindows();

        // Save window state (example windows only save their metadata, not content)
        saveTextContent(text);

        // Store results for tree viewer
        lastTokenizerResult = tokenizerResult;
        lastParserResult    = result;

        // Update tree viewer if visible
        if (treeViewerDialog != null && treeViewerDialog.isVisible()) {
            treeViewerDialog.update(tokenizerResult, result);
        }

        // Update knowledge base viewer if visible
        if (knowledgeBaseViewerDialog != null && knowledgeBaseViewerDialog.isVisible()) {
            knowledgeBaseViewerDialog.update(knowledgeBase);
        }
    }

    private void showMessageColors() {
        String          text            = getDocumentText(messagesPane);
        Tokenizer       tokenizer       = new Tokenizer(text, MESSAGES_FILE_NAME);
        TokenizerResult tokenizerResult = tokenizer.tokenize();
        showColors(messagesPane, tokenizerResult);
    }

    private void prepareForExecute() {
        knowledgeBase.init();
        textPane.getHighlighter().removeAllHighlights();
        messagesPane.getHighlighter().removeAllHighlights();
        StyledDocument     doc         = textPane.getStyledDocument();
        SimpleAttributeSet defaultAttr = new SimpleAttributeSet();
        StyleConstants.setForeground(defaultAttr, Color.BLACK);
        doc.setCharacterAttributes(0, doc.getLength(), defaultAttr, true);
        messagesPane.setText("");
    }

    private void showColors(JTextPane pane, TokenizerResult tokenizerResult) {
        if (tokenizerResult != null) {
            for (Token t = tokenizerResult.firstAll(); t != null; t = t.nextAll()) {
                NelumboEditor.ColorScheme colorScheme = NelumboEditor.getTokenColor(t.colorType());
                if (colorScheme != null) {
                    SimpleAttributeSet attr = colorScheme.attr();
                    pane.getStyledDocument().setCharacterAttributes(t.index(), t.text().length(), attr, false);
                }
            }
        }
    }

    private record Highlight(int index,
                             int length,
                             String error) {
    }

    private void showResults(ParserResult result) {
        List<ParseException> exceptions = result.exceptions();
        if (exceptions.isEmpty()) {
            ParserResult          throwing           = new ParserResult(null, true);
            StringBuilder         messages           = new StringBuilder();
            int                   index              = 0, prevLine = 0, nextLine;
            LinkedList<Highlight> messagesHighlights = new LinkedList<>();
            for (Node root : result.roots()) {
                if (root instanceof Evaluatable eval) {
                    ParseException pe   = null;
                    String         mess = null;
                    try {
                        eval.evaluate(knowledgeBase, throwing);
                    } catch (ParseException exc) {
                        pe   = exc;
                        mess = pe.getShortMessage();
                    }
                    if (eval instanceof Query query && query.inferResult() != null) {
                        mess = query.inferResult().toString();
                    }
                    if (mess != null) {
                        nextLine = eval.lastToken().line();
                        messages.append(emptyLines(nextLine - prevLine)).append(mess).append("\n");
                        index += nextLine - prevLine;
                        if (pe != null && eval instanceof Query query && query.inferResult() != null) {
                            messagesHighlights.add(new Highlight(index, mess.length(), pe.getShortMessage()));
                        }
                        if (pe != null) {
                            setHighlight(textPane, pe.index(), pe.length(), pe.getShortMessage(), redPainter);
                        }
                        prevLine = ++nextLine;
                        index += mess.length() + 1;
                        setMessages(messages.toString());
                        showMessageColors();
                        for (Highlight h : messagesHighlights) {
                            setHighlight(messagesPane, h.index, h.length, h.error, greenPainter);
                        }
                    }
                }
            }
        } else {
            StringBuilder messages = new StringBuilder();
            int           prevLine = 0, nextLine;
            for (ParseException pe : exceptions) {
                nextLine = pe.line();
                messages.append(emptyLines(nextLine - prevLine)).append(pe.getShortMessage()).append("\n");
                setHighlight(textPane, pe.index(), pe.length(), pe.getShortMessage(), redPainter);
                prevLine = ++nextLine;
            }
            setMessages(messages.toString());
        }
    }

    private void setHighlight(JTextPane pane, int index, int length, String message, DefaultHighlightPainter painter) {
        try {
            pane.getHighlighter().addHighlight(index, index + length, painter);
        } catch (BadLocationException ble) {
            setMessages(message);
        }
    }

    private String emptyLines(int nr) {
        return "\n".repeat(Math.max(0, nr));
    }

    private static String getDocumentText(JTextPane pane) {
        try {
            javax.swing.text.Document doc = pane.getDocument();
            return doc.getText(0, doc.getLength());
        } catch (BadLocationException e) {
            return "";
        }
    }

    private void setMessages(String msg) {
        messagesPane.setText(msg);
        // Apply line spacing after setting text
        StyledDocument     messageDoc            = messagesPane.getStyledDocument();
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

    // Preference keys use window-specific prefix
    private String prefKey(String key) {
        return "window." + windowId + "." + key;
    }

    private void saveTextContent(String text) {
        try {
            // Save example info (always needed for window restoration)
            preferences.putBoolean(prefKey("isExample"), isExample);
            if (isExample) {
                // For example windows, only save the example path, not the content
                // The content will be reloaded from the resource file on restore
                if (examplePath != null) {
                    preferences.put(prefKey("examplePath"), examplePath);
                }
                if (exampleDisplayName != null) {
                    preferences.put(prefKey("exampleDisplayName"), exampleDisplayName);
                }
                preferences.put(prefKey("title"), frame.getTitle());
            } else {
                // For regular windows, save content and state
                preferences.put(prefKey("content"), text);

                // Save window number
                if (windowNumber > 0) {
                    preferences.putInt(prefKey("windowNumber"), windowNumber);
                }

                // Save caret position and selection
                int caretPosition  = textPane.getCaretPosition();
                int selectionStart = textPane.getSelectionStart();
                int selectionEnd   = textPane.getSelectionEnd();

                preferences.putInt(prefKey("caretPosition"), caretPosition);
                preferences.putInt(prefKey("selectionStart"), selectionStart);
                preferences.putInt(prefKey("selectionEnd"), selectionEnd);
            }

            preferences.flush();
        } catch (Exception e) {
            System.err.println("Failed to save window content: " + e.getMessage());
        }
    }

    private void loadTextContent() {
        String text = preferences.get(prefKey("content"), "");

        if (!text.isEmpty()) {
            try {
                StyledDocument doc = textPane.getStyledDocument();
                doc.insertString(0, text, null);

                // Apply line spacing
                SimpleAttributeSet paragraphStyle = new SimpleAttributeSet();
                StyleConstants.setLineSpacing(paragraphStyle, 0.2f);
                doc.setParagraphAttributes(0, doc.getLength(), paragraphStyle, false);

                // Restore caret position and selection
                int caretPosition  = preferences.getInt(prefKey("caretPosition"), 0);
                int selectionStart = preferences.getInt(prefKey("selectionStart"), 0);
                int selectionEnd   = preferences.getInt(prefKey("selectionEnd"), 0);

                // Ensure positions are within bounds
                int maxPos = doc.getLength();
                caretPosition  = Math.min(caretPosition, maxPos);
                selectionStart = Math.min(selectionStart, maxPos);
                selectionEnd   = Math.min(selectionEnd, maxPos);

                // Restore selection or caret position
                if (selectionStart != selectionEnd) {
                    textPane.setSelectionStart(selectionStart);
                    textPane.setSelectionEnd(selectionEnd);
                } else {
                    textPane.setCaretPosition(caretPosition);
                }
            } catch (BadLocationException e) {
                textPane.setText(text);
            }
        }
    }

    private void loadExampleContent() {
        try (InputStream is = getClass().getResourceAsStream(examplePath)) {
            if (is == null) {
                JOptionPane.showMessageDialog(frame, "Could not find resource: " + examplePath, "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            String content;
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                content = reader.lines().collect(Collectors.joining("\n"));
            }
            textPane.setText(content);
            textPane.setCaretPosition(0);

            // Apply line spacing
            StyledDocument     doc            = textPane.getStyledDocument();
            SimpleAttributeSet paragraphStyle = new SimpleAttributeSet();
            StyleConstants.setLineSpacing(paragraphStyle, 0.2f);
            doc.setParagraphAttributes(0, doc.getLength(), paragraphStyle, false);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(frame, "Error loading example: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void toggleTreeViewer() {
        if (treeViewerDialog != null && treeViewerDialog.isVisible()) {
            treeViewerDialog.setVisible(false);
            saveTreeViewerVisibility(false);
        } else {
            if (treeViewerDialog == null) {
                treeViewerDialog = new TreeViewerDialog(frame, lastTokenizerResult, lastParserResult);
                registerViewMenuShortcuts(treeViewerDialog);
                treeViewerDialog.addWindowListener(new WindowAdapter() {
                    @Override
                    public void windowClosing(WindowEvent e) {
                        saveTreeViewerVisibility(false);
                    }
                });
            } else {
                treeViewerDialog.update(lastTokenizerResult, lastParserResult);
            }
            treeViewerDialog.setVisible(true);
            saveTreeViewerVisibility(true);
        }
    }

    private void toggleKnowledgeBaseViewer() {
        if (knowledgeBaseViewerDialog != null && knowledgeBaseViewerDialog.isVisible()) {
            knowledgeBaseViewerDialog.setVisible(false);
            saveKnowledgeBaseViewerVisibility(false);
        } else {
            if (knowledgeBaseViewerDialog == null) {
                knowledgeBaseViewerDialog = new KnowledgeBaseViewerDialog(frame, knowledgeBase);
                registerViewMenuShortcuts(knowledgeBaseViewerDialog);
                knowledgeBaseViewerDialog.addWindowListener(new WindowAdapter() {
                    @Override
                    public void windowClosing(WindowEvent e) {
                        saveKnowledgeBaseViewerVisibility(false);
                    }
                });
            } else {
                knowledgeBaseViewerDialog.update(knowledgeBase);
            }
            knowledgeBaseViewerDialog.setVisible(true);
            saveKnowledgeBaseViewerVisibility(true);
        }
    }

    private void saveTreeViewerVisibility(boolean visible) {
        try {
            preferences.putBoolean(prefKey("treeViewerVisible"), visible);
            preferences.flush();
        } catch (Exception e) {
            // Ignore save failures
        }
    }

    private void saveKnowledgeBaseViewerVisibility(boolean visible) {
        try {
            preferences.putBoolean(prefKey("kbViewerVisible"), visible);
            preferences.flush();
        } catch (Exception e) {
            // Ignore save failures
        }
    }

    private void saveDialogVisibility() {
        saveTreeViewerVisibility(treeViewerDialog != null && treeViewerDialog.isVisible());
        saveKnowledgeBaseViewerVisibility(knowledgeBaseViewerDialog != null && knowledgeBaseViewerDialog.isVisible());
    }

    private void restoreDialogVisibility() {
        if (preferences.getBoolean(prefKey("treeViewerVisible"), false)) {
            toggleTreeViewer();
        }
        if (preferences.getBoolean(prefKey("kbViewerVisible"), false)) {
            toggleKnowledgeBaseViewer();
        }
    }

    private void registerViewMenuShortcuts(javax.swing.JDialog dialog) {
        int menuShortcutMask = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();

        // Cmd-T: Toggle Tree Viewer
        dialog.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke('T', menuShortcutMask), "toggleTreeViewer");
        dialog.getRootPane().getActionMap().put("toggleTreeViewer", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                toggleTreeViewer();
            }
        });

        // Cmd-K: Toggle Knowledge Base Viewer
        dialog.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke('K', menuShortcutMask), "toggleKnowledgeBaseViewer");
        dialog.getRootPane().getActionMap().put("toggleKnowledgeBaseViewer", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                toggleKnowledgeBaseViewer();
            }
        });
    }

    /**
     * Clears the preferences for this window.
     */
    public void clearPreferences() {
        try {
            String prefix = "window." + windowId + ".";
            for (String key : preferences.keys()) {
                if (key.startsWith(prefix)) {
                    preferences.remove(key);
                }
            }
            preferences.flush();
        } catch (Exception e) {
            // Ignore
        }
    }
}
