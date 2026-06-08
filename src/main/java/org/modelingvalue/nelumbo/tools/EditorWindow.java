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

import org.modelingvalue.collections.List;
import org.modelingvalue.nelumbo.Evaluatable;
import org.modelingvalue.nelumbo.KnowledgeBase;
import org.modelingvalue.nelumbo.Node;
import org.modelingvalue.nelumbo.lang.Import;
import org.modelingvalue.nelumbo.logic.Query;
import org.modelingvalue.nelumbo.syntax.*;
import org.modelingvalue.nelumbo.syntax.Tokenizer.TokenizerResult;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.AbstractDocument.DefaultDocumentEvent;
import javax.swing.text.*;
import javax.swing.text.DefaultHighlighter.DefaultHighlightPainter;
import javax.swing.undo.CompoundEdit;
import javax.swing.undo.UndoManager;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;

import static org.modelingvalue.nelumbo.tools.NelumboEditor.callOnEDT;
import static org.modelingvalue.nelumbo.tools.NelumboEditor.runOnEDT;

/**
 * Represents an individual editor window in the multi-window architecture. Each
 * window has its own JFrame, text panes, knowledge base execution loop, and
 * viewer dialogs.
 */
public class EditorWindow extends WindowAdapter
        implements WindowListener, Runnable, DocumentListener, EditorImportResolver.ImportChangeListener {

    private static final String MESSAGES_FILE_NAME = "messages.nl";
    private final static String INCREASE           = "INCREASE";
    private final static String DECREASE           = "DECREASE";

    private final static DefaultHighlightPainter redPainter   = new DefaultHighlightPainter(new Color(0xffaaaa));
    private final static DefaultHighlightPainter greenPainter = new DefaultHighlightPainter(new Color(0xaaffaa));

    private final String        windowId;
    private final NelumboEditor application;
    private volatile boolean    isExample;
    private final String        examplePath;
    private final String        exampleDisplayName;
    private final String        filePath;          // Absolute filesystem path for file-backed windows (null otherwise)
    private final Preferences   preferences;
    private volatile int        windowNumber;      // Window number for regular windows

    private volatile KnowledgeBase           knowledgeBase;
    private JFrame                           frame;
    private JTextPane                        messagesPane;
    private JTextPane                        textPane;
    private JMenu                            windowsMenu;
    private JMenuItem                        undoMenuItem;
    private JMenuItem                        redoMenuItem;
    private UndoManager                      undoManager;
    private CompoundEdit                     currentCompoundEdit;
    private Timer                            compoundEditTimer;
    private Timer                            fileSaveTimer;
    private volatile String                  pendingFileSaveText;
    private WindowManager.WindowListListener windowListListener;
    private volatile boolean                 quit;
    private boolean                          refreshRequested;
    private TreeViewerDialog                 treeViewerDialog;
    private KnowledgeBaseViewerDialog        knowledgeBaseViewerDialog;
    private volatile TokenizerResult         lastTokenizerResult;
    private volatile ParserResult            lastParserResult;
    private Set<String>                      currentImports        = new HashSet<>(); // Tracks current editor imports
    private int                              currentUnderlineStart = -1;              // Start index of current
                                                                                      // underline (-1 if none)
    private int                              currentUnderlineEnd   = -1;              // End index of current underline
                                                                                      // (-1 if none)
    private java.awt.Point                   lastMousePosition;                       // Last mouse position for
                                                                                      // key-press underline update

    /**
     * Creates a new regular editor window with a pre-assigned window number.
     */
    public EditorWindow(NelumboEditor application, String windowId, int windowNumber) {
        this.application = application;
        this.windowId = windowId != null ? windowId : UUID.randomUUID().toString();
        this.isExample = false;
        this.examplePath = null;
        this.exampleDisplayName = null;
        this.windowNumber = windowNumber;
        this.preferences = Preferences.userNodeForPackage(NelumboEditor.class);
        this.filePath = null;
    }

    /**
     * Creates a new editor window for an example.
     */
    public EditorWindow(NelumboEditor application, String windowId, boolean isExample, String examplePath,
            String exampleDisplayName) {
        this.application = application;
        this.windowId = windowId != null ? windowId : UUID.randomUUID().toString();
        this.isExample = isExample;
        this.examplePath = examplePath;
        this.exampleDisplayName = exampleDisplayName;
        this.windowNumber = -1; // Examples don't have window numbers
        this.preferences = Preferences.userNodeForPackage(NelumboEditor.class);
        this.filePath = null;
    }

    /**
     * Creates a new editable window backed by a filesystem file. Reuses the
     * regular-window machinery (numbered, importable); the file path drives load,
     * debounced auto-save, and the window title.
     */
    public EditorWindow(NelumboEditor application, String windowId, int windowNumber, String filePath) {
        this.application = application;
        this.windowId = windowId != null ? windowId : UUID.randomUUID().toString();
        this.isExample = false;
        this.examplePath = null;
        this.exampleDisplayName = null;
        this.windowNumber = windowNumber;
        this.preferences = Preferences.userNodeForPackage(NelumboEditor.class);
        this.filePath = filePath;
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
     * Returns the file name used for tokenizing this window's content. Each window
     * uses a unique file name to enable cross-window go-to-definition.
     */
    public String getEditorFileName() {
        if (isExample && exampleDisplayName != null) {
            return exampleDisplayName + ".nl";
        }
        return "editor.nelumbo_" + windowNumber + ".nl";
    }

    /**
     * Initializes and shows the window.
     */
    public void init() {
        initWindow();
        initActions();
        if (isExample && examplePath != null) {
            loadExampleContent();
        } else if (filePath != null) {
            loadFileContent();
        } else {
            loadTextContent();
        }
        restoreDialogVisibility();
        // Clear undo history after loading so users can't undo the initial content
        resetUndoManager();
    }

    /**
     * Starts the knowledge base execution loop. This method blocks until the window
     * is closed.
     */
    public void startExecutionLoop() {
        KnowledgeBase.BASE.run(this);
    }

    private void initWindow() {
        URL resource = getClass().getResource("/org/modelingvalue/nelumbo/nelumbo.png");
        ImageIcon icon = resource != null ? new ImageIcon(resource) : new ImageIcon();

        // Determine window title
        String title;
        if (isExample && exampleDisplayName != null) {
            // Example/library windows use just the display name
            title = exampleDisplayName;
        } else if (filePath != null) {
            // File-backed windows show the file's base name
            title = new java.io.File(filePath).getName();
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
        Dimension frameSize = new Dimension(screenSize.width / 2, screenSize.height / 2);
        frame.setPreferredSize(frameSize);
        frame.setSize(frameSize);

        // Example windows start as read-only (non-editable)
        textPane = new NelumboEditor.NonWrappingJTextPane(!isExample, 0xffffff);
        messagesPane = new NelumboEditor.NonWrappingJTextPane(false, 0xF5F5F5);

        // The messages pane is rebuilt via setText() on every refresh, which leaves
        // its caret at the end and would scroll it to the bottom. Because both panes
        // share one vertical scroll model (see below), that drags the code editor to
        // the bottom too. Never let the messages caret drive scrolling; it only
        // mirrors the code pane via the shared model.
        ((DefaultCaret) messagesPane.getCaret()).setUpdatePolicy(DefaultCaret.NEVER_UPDATE);

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
                        if (keyCode == KeyEvent.VK_DELETE || keyCode == KeyEvent.VK_BACK_SPACE
                                || ((e.isControlDown() || e.isMetaDown())
                                        && (keyCode == KeyEvent.VK_V || keyCode == KeyEvent.VK_X))) {
                            promptToConvertToEditable();
                        }
                    }
                }
            });
        }

        // Create scroll panes with borders
        JScrollPane textScroll = new JScrollPane(textPane);
        textScroll.setRowHeaderView(new NelumboEditor.LineNumberView(textPane));
        JScrollPane messageScroll = new JScrollPane(messagesPane);
        textScroll.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 5));
        messageScroll.setBorder(BorderFactory.createEmptyBorder(10, 5, 10, 10));
        // The messages pane scrolls in lock-step with the code pane via the shared
        // model below, so its own scrollbar is redundant. Hide it; the shared model's
        // change listener still positions the messages viewport.
        messageScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
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

        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        frame.addWindowListener(this);
        textPane.getDocument().addDocumentListener(this);

        // Accept dropped files (open each in a new window) while delegating all
        // other transfers (text paste/drag) to the pane's original handler.
        final TransferHandler originalTransferHandler = textPane.getTransferHandler();
        textPane.setTransferHandler(new TransferHandler() {
            @Override
            public boolean canImport(TransferSupport support) {
                if (support.isDataFlavorSupported(java.awt.datatransfer.DataFlavor.javaFileListFlavor)) {
                    return true;
                }
                return originalTransferHandler != null && originalTransferHandler.canImport(support);
            }

            @Override
            @SuppressWarnings("unchecked")
            public boolean importData(TransferSupport support) {
                if (support.isDataFlavorSupported(java.awt.datatransfer.DataFlavor.javaFileListFlavor)) {
                    try {
                        java.util.List<java.io.File> files = (java.util.List<java.io.File>) support.getTransferable()
                                .getTransferData(java.awt.datatransfer.DataFlavor.javaFileListFlavor);
                        for (java.io.File file : files) {
                            application.getWindowManager().createFileWindow(file);
                        }
                        return true;
                    } catch (Exception ex) {
                        return false;
                    }
                }
                return originalTransferHandler != null && originalTransferHandler.importData(support);
            }
        });

        // Setup undo manager with compound edit grouping
        undoManager = new UndoManager();

        // Timer to end compound edits after a pause in typing (500ms)
        compoundEditTimer = new Timer(500, e -> endCompoundEdit());
        compoundEditTimer.setRepeats(false);

        // Timer to debounce writing the file after a pause in typing (500ms).
        // Only file-backed windows schedule it (see saveTextContent), but it is
        // harmless to create for every window.
        fileSaveTimer = new Timer(500, e -> flushFileSave());
        fileSaveTimer.setRepeats(false);

        textPane.getDocument().addUndoableEditListener(e -> {
            // Only capture INSERT and REMOVE events, not CHANGE (style) events
            if (e.getEdit() instanceof DefaultDocumentEvent docEvent) {
                if (docEvent.getType() != DocumentEvent.EventType.CHANGE) {
                    // Start a new compound edit if needed
                    if (currentCompoundEdit == null) {
                        currentCompoundEdit = new CompoundEdit();
                    }
                    currentCompoundEdit.addEdit(e.getEdit());

                    // Restart the timer - compound edit ends after a pause
                    compoundEditTimer.restart();
                }
            }
        });

        // Set focus on text area
        textPane.requestFocusInWindow();

        // Add mouse listener for go-to-definition (command-click on Mac, control-click
        // on Windows)
        textPane.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                boolean isMac = System.getProperty("os.name").toLowerCase().contains("mac");
                boolean modifierHeld = isMac ? (e.getModifiersEx() & InputEvent.META_DOWN_MASK) != 0
                        : (e.getModifiersEx() & InputEvent.CTRL_DOWN_MASK) != 0;
                if (modifierHeld) {
                    goToDefinition(e);
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                clearUnderline();
            }
        });

        // Add mouse motion listener for underline hint when hovering with modifier key
        textPane.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                updateUnderlineHint(e);
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                clearUnderline();
            }
        });

        // Add key listener to update underline when modifier key state changes
        textPane.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (isGoToDefinitionModifier(e)) {
                    updateUnderlineHintFromLastPosition();
                }
            }

            @Override
            public void keyReleased(KeyEvent e) {
                if (isGoToDefinitionModifier(e)) {
                    clearUnderline();
                }
            }

            private boolean isGoToDefinitionModifier(KeyEvent e) {
                boolean isMac = System.getProperty("os.name").toLowerCase().contains("mac");
                return isMac ? e.getKeyCode() == KeyEvent.VK_META : e.getKeyCode() == KeyEvent.VK_CONTROL;
            }
        });
    }

    /**
     * Handles go-to-definition when the user command-clicks (Mac) or control-clicks
     * (Windows) on a token. Navigates to the definition of the token if one exists,
     * including in other editor windows. Also handles import statements by
     * navigating to the imported file.
     */
    private void goToDefinition(MouseEvent e) {
        if (lastTokenizerResult == null) {
            return;
        }

        // Get the character position at the click location
        int clickPosition = textPane.viewToModel2D(e.getPoint());
        if (clickPosition < 0) {
            return;
        }

        // Check if click is within an Import statement - if so, navigate to the import
        // source
        Import imp = findImportAtPosition(clickPosition);
        if (imp != null) {
            navigateToImport(imp);
            return;
        }

        // Find the token at this position
        Token clickedToken = null;
        for (Token t = lastTokenizerResult.firstAll(); t != null; t = t.nextAll()) {
            if (t.index() <= clickPosition && clickPosition < t.indexEnd()) {
                clickedToken = t;
                break;
            }
        }

        if (clickedToken == null) {
            return;
        }

        // Get the referenced (definition) token
        Token referenced = clickedToken.definition();
        if (referenced == null) {
            return;
        }

        // Check if the definition is in a different file
        String referencedFileName = referenced.fileName();
        String currentFileName = getEditorFileName();

        if (referencedFileName != null && !referencedFileName.equals(currentFileName)) {
            // Definition is in another window - find and navigate to it
            EditorWindow targetWindow = findWindowByFileName(referencedFileName);
            if (targetWindow != null) {
                navigateToTokenInWindow(targetWindow, referenced);
            }
        } else {
            // Definition is in this window - navigate locally
            navigateToTokenInWindow(this, referenced);
        }
    }

    /**
     * Finds the Import node at the given character position by searching through
     * parser results. Returns null if no Import is found at that position.
     */
    private Import findImportAtPosition(int position) {
        if (lastParserResult == null) {
            return null;
        }
        for (Node root : lastParserResult.roots()) {
            Import found = findImportInNode(root, position);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    /**
     * Recursively searches for an Import node containing the given position.
     */
    private Import findImportInNode(Node node, int position) {
        if (node instanceof Import imp) {
            Token first = imp.firstToken();
            Token last = imp.lastToken();
            if (first != null && last != null && first.index() <= position && position < last.indexEnd()) {
                return imp;
            }
        }
        // Search children
        for (int i = 0; i < node.length(); i++) {
            Object child = node.get(i);
            if (child instanceof Node childNode) {
                Import found = findImportInNode(childNode, position);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    /**
     * Navigates to the source of an import statement. For editor imports
     * (editor.nelumbo_N), brings that editor window to front. For library/example
     * imports, opens the corresponding window.
     */
    private void navigateToImport(Import imp) {
        String importName = imp.name();
        if (importName == null) {
            return;
        }

        // Handle editor imports (editor.nelumbo_N)
        if (importName.startsWith("editor.nelumbo_")) {
            try {
                String numberStr = importName.substring("editor.nelumbo_".length());
                int windowNumber = Integer.parseInt(numberStr);
                EditorWindow targetWindow = application.getWindowManager().getWindowByNumber(windowNumber);
                if (targetWindow != null) {
                    bringWindowToFront(targetWindow);
                }
            } catch (NumberFormatException ignored) {
                // Not a valid editor window number
            }
            return;
        }

        // Resolve the import name to display name and resource path
        String[] resolved = application.resolveImportName(importName);
        if (resolved == null) {
            return;
        }
        String displayName = resolved[0];
        String resourcePath = resolved[1];

        // Check if a window with this display name is already open
        String expectedFileName = displayName + ".nl";
        for (EditorWindow window : application.getWindowManager().getWindowsInOrder()) {
            if (expectedFileName.equals(window.getEditorFileName())) {
                bringWindowToFront(window);
                return;
            }
        }

        // Open the library/example
        application.openExample(resourcePath, displayName);
    }

    /**
     * Finds the editor window that uses the given file name.
     */
    private EditorWindow findWindowByFileName(String fileName) {
        // Try to extract window number from file name (format: "editor.nelumbo_N.nl")
        if (fileName.startsWith("editor.nelumbo_") && fileName.endsWith(".nl")) {
            try {
                String numberStr = fileName.substring("editor.nelumbo_".length(), fileName.length() - ".nl".length());
                int windowNumber = Integer.parseInt(numberStr);
                return application.getWindowManager().getWindowByNumber(windowNumber);
            } catch (NumberFormatException ignored) {
                // Not a regular window file name
            }
        }

        // Search all windows by their file name (for example windows)
        for (EditorWindow window : application.getWindowManager().getWindowsInOrder()) {
            if (fileName.equals(window.getEditorFileName())) {
                return window;
            }
        }
        return null;
    }

    /**
     * Navigates to and selects the given token in the specified window.
     */
    private void navigateToTokenInWindow(EditorWindow targetWindow, Token token) {
        JTextPane targetPane = targetWindow.textPane;
        JFrame targetFrame = targetWindow.frame;

        int startPosition = token.index();
        int endPosition = token.indexEnd();
        int docLength = targetPane.getDocument().getLength();

        if (startPosition >= 0 && endPosition <= docLength) {
            // Bring the target window to front if it's a different window
            if (targetWindow != this && targetFrame != null) {
                targetFrame.toFront();
                targetFrame.requestFocus();
                if (targetFrame.getExtendedState() == JFrame.ICONIFIED) {
                    targetFrame.setExtendedState(JFrame.NORMAL);
                }
            }

            // Select the token
            targetPane.setSelectionStart(startPosition);
            targetPane.setSelectionEnd(endPosition);
            targetPane.requestFocusInWindow();
        }
    }

    /**
     * Updates the underline hint based on mouse position and modifier key state.
     */
    private void updateUnderlineHint(MouseEvent e) {
        lastMousePosition = e.getPoint();
        boolean isMac = System.getProperty("os.name").toLowerCase().contains("mac");
        boolean modifierHeld = isMac ? (e.getModifiersEx() & InputEvent.META_DOWN_MASK) != 0
                : (e.getModifiersEx() & InputEvent.CTRL_DOWN_MASK) != 0;

        if (!modifierHeld) {
            clearUnderline();
            return;
        }

        updateUnderlineAtPosition(textPane.viewToModel2D(e.getPoint()));
    }

    /**
     * Updates underline hint when modifier key is pressed, using the last known
     * mouse position.
     */
    private void updateUnderlineHintFromLastPosition() {
        if (lastMousePosition == null) {
            return;
        }
        updateUnderlineAtPosition(textPane.viewToModel2D(lastMousePosition));
    }

    /**
     * Updates the underline at the given character position. Underlines the full
     * import statement if position is within an import, or a single token if it has
     * a definition.
     */
    private void updateUnderlineAtPosition(int position) {
        if (position < 0) {
            clearUnderline();
            return;
        }

        // First check if we're in an import statement - if so, underline the whole
        // import
        Import imp = findImportAtPosition(position);
        if (imp != null) {
            Token first = imp.firstToken();
            Token last = imp.lastToken();
            if (first != null && last != null) {
                int start = first.index();
                int end = last.indexEnd();
                if (start != currentUnderlineStart || end != currentUnderlineEnd) {
                    clearUnderline();
                    setUnderline(start, end);
                }
                return;
            }
        }

        // Check if there's a token with a definition at this position
        if (lastTokenizerResult != null) {
            for (Token t = lastTokenizerResult.firstAll(); t != null; t = t.nextAll()) {
                if (t.index() <= position && position < t.indexEnd()) {
                    if (t.definition() != null) {
                        int start = t.index();
                        int end = t.indexEnd();
                        if (start != currentUnderlineStart || end != currentUnderlineEnd) {
                            clearUnderline();
                            setUnderline(start, end);
                        }
                        return;
                    }
                    break;
                }
            }
        }

        // Nothing to underline
        clearUnderline();
    }

    /**
     * Clears the current underline, if any.
     */
    private void clearUnderline() {
        if (currentUnderlineStart >= 0 && currentUnderlineEnd >= 0) {
            StyledDocument doc = textPane.getStyledDocument();
            SimpleAttributeSet attr = new SimpleAttributeSet();
            StyleConstants.setUnderline(attr, false);
            doc.setCharacterAttributes(currentUnderlineStart, currentUnderlineEnd - currentUnderlineStart, attr, false);
            currentUnderlineStart = -1;
            currentUnderlineEnd = -1;
        }
    }

    /**
     * Underlines the given range.
     */
    private void setUnderline(int start, int end) {
        StyledDocument doc = textPane.getStyledDocument();
        SimpleAttributeSet attr = new SimpleAttributeSet();
        StyleConstants.setUnderline(attr, true);
        doc.setCharacterAttributes(start, end - start, attr, false);
        currentUnderlineStart = start;
        currentUnderlineEnd = end;
    }

    private JMenuBar createMenuBar() {
        JMenuBar menuBar = new JMenuBar();

        // File menu
        JMenu fileMenu = new JMenu("File");
        JMenuItem newWindowItem = new JMenuItem("New Window");
        newWindowItem
                .setAccelerator(KeyStroke.getKeyStroke('N', Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
        newWindowItem.addActionListener(e -> application.createNewWindow());
        fileMenu.add(newWindowItem);

        JMenuItem openItem = new JMenuItem("Open…");
        openItem.setAccelerator(KeyStroke.getKeyStroke('O', Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
        openItem.addActionListener(e -> openFileChooser());
        fileMenu.add(openItem);

        JMenuItem closeWindowItem = new JMenuItem("Close Window");
        closeWindowItem
                .setAccelerator(KeyStroke.getKeyStroke('W', Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
        closeWindowItem.addActionListener(e -> closeWindow());
        fileMenu.add(closeWindowItem);

        fileMenu.addSeparator();

        JMenuItem quitItem = new JMenuItem("Quit");
        quitItem.setAccelerator(KeyStroke.getKeyStroke('Q', Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
        quitItem.addActionListener(e -> application.quit());
        fileMenu.add(quitItem);

        menuBar.add(fileMenu);

        // Edit menu
        JMenu editMenu = new JMenu("Edit");

        undoMenuItem = new JMenuItem("Undo");
        undoMenuItem
                .setAccelerator(KeyStroke.getKeyStroke('Z', Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
        undoMenuItem.addActionListener(e -> performUndo());
        undoMenuItem.setEnabled(false);
        editMenu.add(undoMenuItem);

        redoMenuItem = new JMenuItem("Redo");
        boolean isMac = System.getProperty("os.name").toLowerCase().contains("mac");
        redoMenuItem.setAccelerator(isMac
                ? KeyStroke.getKeyStroke('Z',
                        Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx() | InputEvent.SHIFT_DOWN_MASK)
                : KeyStroke.getKeyStroke('Y', InputEvent.CTRL_DOWN_MASK));
        redoMenuItem.addActionListener(e -> performRedo());
        redoMenuItem.setEnabled(false);
        editMenu.add(redoMenuItem);

        menuBar.add(editMenu);

        // Colors menu
        JMenu colorsMenu = new JMenu("Colors");
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
        JMenu viewMenu = new JMenu("View");
        JMenuItem treeViewerItem = new JMenuItem("Tree Viewer...");
        treeViewerItem
                .setAccelerator(KeyStroke.getKeyStroke('T', Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
        treeViewerItem.addActionListener(e -> toggleTreeViewer());
        viewMenu.add(treeViewerItem);

        JMenuItem knowledgeBaseViewerItem = new JMenuItem("Knowledge Base Viewer...");
        knowledgeBaseViewerItem
                .setAccelerator(KeyStroke.getKeyStroke('K', Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
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

    private void openFileChooser() {
        // Use the AWT FileDialog so the OS-native open panel is shown (Cocoa on macOS).
        FileDialog dialog = new FileDialog(frame, "Open", FileDialog.LOAD);
        dialog.setFilenameFilter((dir, name) -> name.endsWith(".nl")); // honored on macOS
        if (!System.getProperty("os.name").toLowerCase().contains("mac")) {
            dialog.setFile("*.nl"); // Windows/Linux filter via a glob in the file field
        }
        dialog.setVisible(true);
        String name = dialog.getFile();
        if (name != null) {
            application.getWindowManager().createFileWindow(new File(dialog.getDirectory(), name));
        }
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
            int index = 1;
            for (EditorWindow window : windows) {
                String title = window.getFrame() != null ? window.getFrame().getTitle() : "Nelumbo Editor";
                JMenuItem item = new JMenuItem(index + " " + title);

                // Add keyboard shortcut for first 9 windows (Cmd-1 through Cmd-9)
                if (index <= 9) {
                    item.setAccelerator(KeyStroke.getKeyStroke(Character.forDigit(index, 10),
                            Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
                }

                // Mark current window
                if (window == this) {
                    item.setEnabled(false); // Can't switch to self
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

    private void endCompoundEdit() {
        if (currentCompoundEdit != null) {
            currentCompoundEdit.end();
            undoManager.addEdit(currentCompoundEdit);
            currentCompoundEdit = null;
            updateUndoRedoMenuItems();
        }
    }

    private void resetUndoManager() {
        if (compoundEditTimer != null) {
            compoundEditTimer.stop();
        }
        currentCompoundEdit = null;
        if (undoManager != null) {
            undoManager.discardAllEdits();
        }
        updateUndoRedoMenuItems();
    }

    private void performUndo() {
        // End any pending compound edit before undoing
        endCompoundEdit();
        if (undoManager != null && undoManager.canUndo()) {
            undoManager.undo();
            updateUndoRedoMenuItems();
        }
    }

    private void performRedo() {
        // End any pending compound edit before redoing
        endCompoundEdit();
        if (undoManager != null && undoManager.canRedo()) {
            undoManager.redo();
            updateUndoRedoMenuItems();
        }
    }

    private void updateUndoRedoMenuItems() {
        if (undoMenuItem != null) {
            undoMenuItem.setEnabled(undoManager != null && undoManager.canUndo());
        }
        if (redoMenuItem != null) {
            redoMenuItem.setEnabled(undoManager != null && undoManager.canRedo());
        }
    }

    private void closeWindow() {
        if (confirmCloseIfEditable()) {
            saveAndFlush();
            frame.setVisible(false);
            frame.dispose();
        }
    }

    /**
     * Persists this window's content and flushes any pending file write. Shared by
     * the close-window and application-quit paths so neither loses unsaved edits.
     */
    void saveAndFlush() {
        // Content is only persisted for non-example windows; file windows write to disk.
        saveTextContent(getDocumentText(textPane));
        flushFileSaveNow();
        saveDialogVisibility();
    }

    /**
     * Shows a confirmation dialog if this is an editable window. Returns true if
     * the window should be closed, false to cancel.
     */
    private boolean confirmCloseIfEditable() {
        if (!needsCloseConfirmation()) {
            return true; // File windows auto-save; read-only windows have nothing to lose.
        }
        int result = JOptionPane.showConfirmDialog(frame,
                "The contents of this window will be lost. Are you sure you want to close it?", "Close Window",
                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        return result == JOptionPane.YES_OPTION;
    }

    /**
     * Whether closing this window would discard content the user might want to
     * keep: an editable, non-file window. File-backed windows auto-save to disk
     * and read-only windows have nothing to lose, so neither needs confirmation.
     */
    boolean needsCloseConfirmation() {
        return filePath == null && textPane.isEditable();
    }

    /**
     * Prompts the user to convert this read-only example window to an editable
     * window.
     */
    private void promptToConvertToEditable() {
        int result = JOptionPane.showConfirmDialog(frame,
                "This is a read-only example. Would you like to create an editable copy?", "Convert to Editable",
                JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);

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
        closeWindow();
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
     * Called when an import this window depends on has changed. Triggers a re-parse
     * to update the content.
     */
    @Override
    public void onImportChanged(String importName) {
        refresh();
    }

    /**
     * Extracts editor imports (imports starting with "editor.") from the parser
     * result.
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
     * Registers this window as a listener for imports it depends on, and
     * unregisters from imports it no longer depends on.
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
        // Phase 1: Read document text on EDT
        try {
            String text = callOnEDT(() -> getDocumentText(textPane));

            // Phase 2: Compute on worker thread
            knowledgeBase.init();
            Tokenizer tokenizer = new Tokenizer(text, getEditorFileName());
            TokenizerResult tokenizerResult = tokenizer.tokenize();
            ParserResult result = new Parser(tokenizerResult).parseNonThrowing();

            // Phase 3: Apply all pre-compute UI updates on EDT
            runOnEDT(() -> applyUIUpdates(tokenizerResult, result, text,
                    emptyLines(result.getTokenizerResult().lastAll().lastLine()), new ArrayList<>(0),
                    new ArrayList<>(0)));

            // Phase 4: Compute results
            ArrayList<Highlight> textHighlights = new ArrayList<>();
            ArrayList<Highlight> messageHighlights = new ArrayList<>();
            String messagesText = computeResults(result, textHighlights, messageHighlights);

            if (!refreshRequested) {
                // Phase 5: Apply all post-compute UI updates on EDT
                runOnEDT(() -> applyUIUpdates(tokenizerResult, result, text, messagesText, textHighlights,
                        messageHighlights));
            }

            // Phase 6: Non-UI updates on worker thread
            updateImportDependencies(result);
            notifyDependentWindows();
            lastTokenizerResult = tokenizerResult;
            lastParserResult = result;
        } catch (NelumboEditor.EDTException e) {
            System.err.println("EDT call failed in execute: " + e.getCause());
        }
    }

    /**
     * Computes evaluation results on the worker thread without touching Swing
     * components. Returns the messages text; populates textHighlights and
     * messageHighlights.
     */
    private String computeResults(ParserResult result, ArrayList<Highlight> textHighlights,
            ArrayList<Highlight> messageHighlights) {
        List<ParseException> exceptions = result.exceptions();
        int totalLines = result.getTokenizerResult().lastAll().lastLine() + 1;
        ArrayList<String> messages = new ArrayList<>(totalLines);
        for (int i = 0; i < totalLines; i++) {
            messages.add("");
        }

        ArrayList<int[]> pendingMessageHighlights = new ArrayList<>();
        ArrayList<String> pendingMessageHighlightErrors = new ArrayList<>();

        ParserResult throwing = new ParserResult(null, true);
        for (Node root : result.roots()) {
            if (root instanceof Evaluatable eval) {
                ParseException pe = null;
                String mess = null;
                try {
                    eval.evaluate(knowledgeBase, throwing);
                } catch (ParseException exc) {
                    pe = exc;
                    mess = pe.getShortMessage();
                }
                if (eval instanceof Query query && query.inferResult() != null) {
                    mess = query.inferResult().toString();
                }
                if (mess != null) {
                    int line = eval.lastToken().line();
                    if (line >= 0 && line < totalLines) {
                        messages.set(line, mess);
                    }
                    if (pe != null) {
                        textHighlights.add(new Highlight(pe.index(), pe.length(), pe.getShortMessage()));
                        if (eval instanceof Query query && query.inferResult() != null && line >= 0
                                && line < totalLines) {
                            pendingMessageHighlights.add(new int[]{line, mess.length()});
                            pendingMessageHighlightErrors.add(pe.getShortMessage());
                        }
                    }
                }
            }
        }
        for (ParseException pe : exceptions) {
            int line = pe.line();
            if (line >= 0 && line < totalLines) {
                messages.set(line, pe.getShortMessage());
            }
            textHighlights.add(new Highlight(pe.index(), pe.length(), pe.getShortMessage()));
        }

        for (int i = 0; i < pendingMessageHighlights.size(); i++) {
            int line = pendingMessageHighlights.get(i)[0];
            int length = pendingMessageHighlights.get(i)[1];
            int offset = 0;
            for (int j = 0; j < line; j++) {
                offset += messages.get(j).length() + 1;
            }
            messageHighlights.add(new Highlight(offset, length, pendingMessageHighlightErrors.get(i)));
        }

        return String.join("\n", messages);
    }

    /**
     * Applies all UI updates on the EDT after computation is complete.
     */
    private void applyUIUpdates(TokenizerResult tokenizerResult, ParserResult result, String text, String messagesText,
            ArrayList<Highlight> textHighlights, ArrayList<Highlight> messageHighlights) {
        // Reset UI state
        // Create a new Highlighter instead of clearing it because of
        // ArrayIndexOutOfBoundsException when repainting
        textPane.setHighlighter(new DefaultHighlighter());
        messagesPane.setHighlighter(new DefaultHighlighter());
        currentUnderlineStart = -1;
        currentUnderlineEnd = -1;
        StyledDocument doc = textPane.getStyledDocument();
        SimpleAttributeSet defaultAttr = new SimpleAttributeSet();
        StyleConstants.setForeground(defaultAttr, Color.BLACK);
        doc.setCharacterAttributes(0, doc.getLength(), defaultAttr, true);

        // Apply token colors
        showColors(textPane, tokenizerResult);

        // Apply text pane highlights
        for (Highlight h : textHighlights) {
            setHighlight(textPane, h.index(), h.length(), h.error(), redPainter);
        }

        // Set messages, apply colors, and apply message highlights
        setMessages(messagesText);
        showMessageColors();
        for (Highlight h : messageHighlights) {
            setHighlight(messagesPane, h.index(), h.length(), h.error(), greenPainter);
        }

        // Update tree viewer if visible
        if (treeViewerDialog != null && treeViewerDialog.isVisible()) {
            treeViewerDialog.update(tokenizerResult, result);
        }

        // Update knowledge base viewer if visible
        if (knowledgeBaseViewerDialog != null && knowledgeBaseViewerDialog.isVisible()) {
            knowledgeBaseViewerDialog.update(knowledgeBase);
        }

        // Save window state (reads caret position, must be on EDT)
        saveTextContent(text);
    }

    private void showMessageColors() {
        String text = getDocumentText(messagesPane);
        Tokenizer tokenizer = new Tokenizer(text, MESSAGES_FILE_NAME);
        TokenizerResult tokenizerResult = tokenizer.tokenize();
        showColors(messagesPane, tokenizerResult);
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

    private record Highlight(int index, int length, String error) {
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
        StyledDocument messageDoc = messagesPane.getStyledDocument();
        SimpleAttributeSet messageParagraphStyle = new SimpleAttributeSet();
        StyleConstants.setLineSpacing(messageParagraphStyle, 0.2f);
        messageDoc.setParagraphAttributes(0, messageDoc.getLength(), messageParagraphStyle, false);
        messagesPane.repaint();
    }

    @Override
    public synchronized void insertUpdate(DocumentEvent e) {
        refresh();
        javax.swing.SwingUtilities.invokeLater(this::updateUndoRedoMenuItems);
    }

    @Override
    public synchronized void removeUpdate(DocumentEvent e) {
        refresh();
        javax.swing.SwingUtilities.invokeLater(this::updateUndoRedoMenuItems);
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
            if (filePath != null) {
                // File-backed window: the file on disk is the source of truth.
                // Persist only the path (for restore) and debounce the disk write.
                preferences.put(prefKey("filePath"), filePath);
                if (windowNumber > 0) {
                    preferences.putInt(prefKey("windowNumber"), windowNumber);
                }
                scheduleFileSave(text);
            } else if (isExample) {
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
                int caretPosition = textPane.getCaretPosition();
                int selectionStart = textPane.getSelectionStart();
                int selectionEnd = textPane.getSelectionEnd();

                preferences.putInt(prefKey("caretPosition"), caretPosition);
                preferences.putInt(prefKey("selectionStart"), selectionStart);
                preferences.putInt(prefKey("selectionEnd"), selectionEnd);
            }

            preferences.flush();
        } catch (Exception e) {
            System.err.println("Failed to save window content: " + e.getMessage());
        }
    }

    private void scheduleFileSave(String text) {
        pendingFileSaveText = text;
        fileSaveTimer.restart();
    }

    private void flushFileSave() {
        String text = pendingFileSaveText;
        if (text == null || filePath == null) {
            return;
        }
        // Write off the EDT so disk IO never stalls the UI. The text was captured
        // on the EDT in scheduleFileSave.
        new Thread(() -> {
            try {
                EditorFileIO.write(Path.of(filePath), text);
            } catch (IOException ex) {
                // Fail loud: a swallowed auto-save loses edits. Surface in the messages pane.
                javax.swing.SwingUtilities.invokeLater(() -> setMessages(
                        "Failed to save " + new File(filePath).getName() + ": " + ex.getMessage()));
            }
        }, "EditorFileSave-" + windowId).start();
    }

    /**
     * Writes any pending debounced file content immediately and synchronously.
     * Called on close so edits made within the last debounce window are not lost.
     */
    private void flushFileSaveNow() {
        if (filePath == null) {
            return;
        }
        fileSaveTimer.stop(); // cancel the pending async write; we write synchronously here
        String text = pendingFileSaveText;
        if (text == null) {
            return;
        }
        try {
            EditorFileIO.write(Path.of(filePath), text);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(frame,
                    "Failed to save " + new File(filePath).getName() + ": " + ex.getMessage(), "Error",
                    JOptionPane.ERROR_MESSAGE);
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
                int caretPosition = preferences.getInt(prefKey("caretPosition"), 0);
                int selectionStart = preferences.getInt(prefKey("selectionStart"), 0);
                int selectionEnd = preferences.getInt(prefKey("selectionEnd"), 0);

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
                textPane.setText(text);
            }
        }
    }

    private void loadExampleContent() {
        try (InputStream is = getClass().getResourceAsStream(examplePath)) {
            if (is == null) {
                JOptionPane.showMessageDialog(frame, "Could not find resource: " + examplePath, "Error",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }
            String content;
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                content = reader.lines().collect(Collectors.joining("\n"));
            }
            textPane.setText(content);
            textPane.setCaretPosition(0);

            // Apply line spacing
            StyledDocument doc = textPane.getStyledDocument();
            SimpleAttributeSet paragraphStyle = new SimpleAttributeSet();
            StyleConstants.setLineSpacing(paragraphStyle, 0.2f);
            doc.setParagraphAttributes(0, doc.getLength(), paragraphStyle, false);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(frame, "Error loading example: " + e.getMessage(), "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void loadFileContent() {
        try {
            String         content = EditorFileIO.read(Path.of(filePath));
            StyledDocument doc     = textPane.getStyledDocument();
            doc.insertString(0, content, null);

            // Apply line spacing
            SimpleAttributeSet paragraphStyle = new SimpleAttributeSet();
            StyleConstants.setLineSpacing(paragraphStyle, 0.2f);
            doc.setParagraphAttributes(0, doc.getLength(), paragraphStyle, false);

            textPane.setCaretPosition(0);
        } catch (IOException | BadLocationException e) {
            JOptionPane.showMessageDialog(frame, "Error loading file: " + e.getMessage(), "Error",
                    JOptionPane.ERROR_MESSAGE);
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
        dialog.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke('T', menuShortcutMask), "toggleTreeViewer");
        dialog.getRootPane().getActionMap().put("toggleTreeViewer", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                toggleTreeViewer();
            }
        });

        // Cmd-K: Toggle Knowledge Base Viewer
        dialog.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke('K', menuShortcutMask), "toggleKnowledgeBaseViewer");
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
