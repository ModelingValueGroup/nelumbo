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

package org.modelingvalue.nelumbo;

import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTree;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.Enumeration;

import org.modelingvalue.collections.List;
import org.modelingvalue.nelumbo.syntax.ParserResult;
import org.modelingvalue.nelumbo.syntax.Token;
import org.modelingvalue.nelumbo.syntax.Tokenizer.TokenizerResult;

public class TreeViewerDialog extends JDialog {

    private static final String[] TOKEN_COLUMNS            = {"Line", "Position", "Index", "Type", "Text", "ColorType"};
    private static final String[] AST_ELEMENT_NODE_LABELS  = {"class", "type", "functor", "line", "position", "index", "#args", "tokens"};
    private static final String[] AST_ELEMENT_TOKEN_LABELS = {"class", "type", "color", "line", "position", "index", "text", "variable"};

    private static final Color NODE_BACKGROUND         = new Color(0xC8E6C9);  // Light green
    private static final Color TOKEN_BACKGROUND        = new Color(0xE1BEE7);  // Light purple
    private static final Color LAYOUT_TOKEN_TEXT_COLOR = new Color(0x888888);

    private final JTable                tokenListView;
    private final DefaultTableModel     tokenListViewModel;
    private final JTree                 nodeTree;
    private final JLabel[]              astElementDetailLabels;
    private final JLabel[]              astElementDetailText;
    private final java.util.List<Token> tokenList = new ArrayList<>();
    private       boolean               isSyncing = false;

    public TreeViewerDialog(JFrame parent, TokenizerResult tokenizerResult, ParserResult parserResult) {
        super(parent, "Token & Node Tree Viewer", false);
        setLayout(new BorderLayout());

        // Create token list
        tokenListViewModel = new DefaultTableModel(TOKEN_COLUMNS, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        tokenListView      = new JTable(tokenListViewModel);
        tokenListView.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        tokenListView.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
        tokenListView.getColumnModel().getColumn(0).setPreferredWidth(80);  // Line
        tokenListView.getColumnModel().getColumn(1).setPreferredWidth(80);  // Col
        tokenListView.getColumnModel().getColumn(2).setPreferredWidth(80);  // Index
        tokenListView.getColumnModel().getColumn(3).setPreferredWidth(120); // Type
        tokenListView.getColumnModel().getColumn(4).setPreferredWidth(150); // Text
        tokenListView.getColumnModel().getColumn(5).setPreferredWidth(100); // ColorType
        tokenListView.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));

        // Custom renderer for skip/layout token grey text
        TokenTableCellRenderer rightRenderer = new TokenTableCellRenderer(SwingConstants.RIGHT);
        TokenTableCellRenderer leftRenderer  = new TokenTableCellRenderer(SwingConstants.LEFT);
        tokenListView.getColumnModel().getColumn(0).setCellRenderer(rightRenderer); // Line
        tokenListView.getColumnModel().getColumn(1).setCellRenderer(rightRenderer); // Col
        tokenListView.getColumnModel().getColumn(2).setCellRenderer(rightRenderer); // Index
        tokenListView.getColumnModel().getColumn(3).setCellRenderer(leftRenderer);  // Type
        tokenListView.getColumnModel().getColumn(4).setCellRenderer(leftRenderer);  // Text
        tokenListView.getColumnModel().getColumn(5).setCellRenderer(leftRenderer);  // ColorType

        populateTokenTable(tokenizerResult);

        // Add table selection listener for synced selection
        tokenListView.getSelectionModel().addListSelectionListener(this::onTokenListSelected);

        // Create node tree
        DefaultMutableTreeNode nodeRoot = buildNodeTree(parserResult);
        nodeTree = new JTree(new DefaultTreeModel(nodeRoot));
        nodeTree.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        nodeTree.setCellRenderer(new AstElementTreeCellRenderer());
        expandAllNodes(nodeTree);

        // Create ASTElement detail panel
        JPanel astElementDetailPanel = new JPanel(new GridBagLayout());
        astElementDetailPanel.setBorder(BorderFactory.createTitledBorder("ASTElement Details"));
        astElementDetailLabels = new JLabel[AST_ELEMENT_NODE_LABELS.length];
        astElementDetailText   = new JLabel[AST_ELEMENT_NODE_LABELS.length];
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(1, 5, 1, 5);
        for (int i = 0; i < AST_ELEMENT_NODE_LABELS.length; i++) {
            JLabel label = new JLabel(AST_ELEMENT_NODE_LABELS[i] + ":");
            JLabel text  = new JLabel("-");
            astElementDetailLabels[i] = label;
            astElementDetailText[i]   = text;

            label.setFont(label.getFont().deriveFont(Font.BOLD));
            label.setHorizontalAlignment(SwingConstants.RIGHT);
            gbc.gridx   = 0;
            gbc.gridy   = i;
            gbc.anchor  = GridBagConstraints.EAST;
            gbc.weightx = 0;
            gbc.fill    = GridBagConstraints.NONE;
            astElementDetailPanel.add(label, gbc);

            text.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
            gbc.gridx   = 1;
            gbc.anchor  = GridBagConstraints.WEST;
            gbc.weightx = 1.0;
            gbc.fill    = GridBagConstraints.HORIZONTAL;
            astElementDetailPanel.add(text, gbc);
        }

        // Add tree selection listener
        nodeTree.addTreeSelectionListener(this::onNodeSelected);

        // Create scroll panes
        JScrollPane tokenScrollPane = new JScrollPane(tokenListView);
        JScrollPane nodeScrollPane  = new JScrollPane(nodeTree);

        // Create panels with titles
        JPanel tokenPanel = new JPanel(new BorderLayout());
        tokenPanel.setBackground(TOKEN_BACKGROUND);
        JLabel tokenLabel = new JLabel("Token List", SwingConstants.CENTER);
        tokenLabel.setOpaque(true);
        tokenLabel.setBackground(TOKEN_BACKGROUND);
        tokenPanel.add(tokenLabel, BorderLayout.NORTH);
        tokenPanel.add(tokenScrollPane, BorderLayout.CENTER);
        tokenScrollPane.getViewport().setBackground(TOKEN_BACKGROUND);

        JPanel nodePanel = new JPanel(new BorderLayout());
        nodePanel.add(new JLabel("Node Tree", SwingConstants.CENTER), BorderLayout.NORTH);
        nodePanel.add(nodeScrollPane, BorderLayout.CENTER);
        nodePanel.add(astElementDetailPanel, BorderLayout.SOUTH);

        // Create split pane
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, tokenPanel, nodePanel);
        splitPane.setDividerLocation(500);
        splitPane.setContinuousLayout(true);

        add(splitPane, BorderLayout.CENTER);

        JLabel coordinateRemark = new JLabel("all coordinates (line, position, index) are 0-based; ranges shown as begin:last:end", SwingConstants.CENTER);
        coordinateRemark.setFont(coordinateRemark.getFont().deriveFont(Font.ITALIC, 11f));
        coordinateRemark.setBorder(BorderFactory.createEmptyBorder(3, 0, 3, 0));
        add(coordinateRemark, BorderLayout.SOUTH);

        setPreferredSize(new Dimension(1100, 700));
        pack();
        setLocationRelativeTo(parent);
    }

    private void populateTokenTable(TokenizerResult tokenizerResult) {
        tokenListViewModel.setRowCount(0);
        tokenList.clear();
        if (tokenizerResult != null) {
            for (Token token = tokenizerResult.firstAll(); token != null; token = token.nextAll()) {
                tokenList.add(token);
                tokenListViewModel.addRow(new Object[]{formatRange(token.line(), token.lastLine(), token.lineEnd()), formatRange(token.position(), token.lastPosition(), token.positionEnd()), formatRange(token.index(), token.indexEnd() - 1, token.indexEnd()), token.type().name(), escapeText(token.text()), token.colorType().name()});
            }
        }
    }

    private String formatRange(int start, int last, int end) {
        if (start == end) {
            return String.valueOf(start);
        }
        return start + ":" + last + ":" + end;
    }

    private DefaultMutableTreeNode buildNodeTree(ParserResult parserResult) {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("Nodes");
        if (parserResult != null) {
            List<Node> roots = parserResult.roots();
            for (Node node : roots) {
                root.add(buildNodeTreeNode(node));
            }
        }
        return root;
    }

    private DefaultMutableTreeNode buildNodeTreeNode(Node node) {
        return buildNodeTreeNode(node, null);
    }

    private DefaultMutableTreeNode buildNodeTreeNode(Node node, Integer argsIndex) {
        NodeInfo               nodeInfo = new NodeInfo(node, argsIndex);
        DefaultMutableTreeNode treeNode = new DefaultMutableTreeNode(nodeInfo);

        // Build a map of element -> args index for marking
        java.util.Map<Object, Integer> argsIndexMap = new java.util.HashMap<>();
        for (int i = 0; i < node.length(); i++) {
            Object arg = node.get(i);
            if (arg != null) {
                argsIndexMap.put(arg, i);
            }
        }

        // Build a map of first token -> child node for child nodes in astElements
        java.util.Map<Token, Node> firstTokenToChildNode = new java.util.HashMap<>();
        List<AstElement>           astElements           = node.astElements();
        if (astElements != null) {
            for (AstElement element : astElements) {
                if (element instanceof Node childNode) {
                    Token firstToken = childNode.firstToken();
                    if (firstToken != null) {
                        firstTokenToChildNode.put(firstToken, childNode);
                    }
                }
            }
        }

        // Get ALL tokens (including SKIP/LAYOUT) from first to last
        Token firstToken = node.firstToken();
        Token lastToken  = node.lastToken();
        if (firstToken != null && lastToken != null) {
            Token current = firstToken;
            while (current != null) {
                // Check if this token is the start of a child node
                Node childNode = firstTokenToChildNode.get(current);
                if (childNode != null) {
                    // Add the child node
                    Integer childArgsIndex = argsIndexMap.get(childNode);
                    treeNode.add(buildNodeTreeNode(childNode, childArgsIndex));
                    // Skip to after the child node's last token
                    Token childLastToken = childNode.lastToken();
                    if (childLastToken != null) {
                        current = childLastToken.nextAll();
                    } else {
                        current = current.nextAll();
                    }
                } else {
                    // Add the token
                    Integer tokenArgsIndex = argsIndexMap.get(current);
                    treeNode.add(new DefaultMutableTreeNode(new TokenInfo(current, tokenArgsIndex)));
                    if (current == lastToken) {
                        break;
                    }
                    current = current.nextAll();
                }
                // Safety check to avoid infinite loop
                if (current != null && current.index() > lastToken.index()) {
                    break;
                }
            }
        }

        // Add any args that are not in astElements (e.g., lists, other values)
        for (int i = 0; i < node.length(); i++) {
            Object child = node.get(i);
            if (child instanceof List<?> list) {
                java.util.List<Object> nonAstItems = new ArrayList<>();
                for (Object item : list) {
                    if (uncontainsElement(astElements, item)) {
                        nonAstItems.add(item);
                    }
                }
                if (!nonAstItems.isEmpty()) {
                    DefaultMutableTreeNode listNode = new DefaultMutableTreeNode("args[" + i + "] (" + nonAstItems.size() + " items)");
                    for (Object item : nonAstItems) {
                        if (item instanceof Node itemNode) {
                            listNode.add(buildNodeTreeNode(itemNode));
                        } else if (item != null) {
                            listNode.add(new DefaultMutableTreeNode(item.getClass().getSimpleName() + ": " + truncate(item.toString(), 50)));
                        }
                    }
                    treeNode.add(listNode);
                }
            } else if (child != null && !(child instanceof Token) && !(child instanceof Node) && uncontainsElement(astElements, child)) {
                treeNode.add(new DefaultMutableTreeNode("args[" + i + "] " + child.getClass().getSimpleName() + ": " + truncate(child.toString(), 50)));
            }
        }

        return treeNode;
    }

    private boolean uncontainsElement(List<AstElement> astElements, Object element) {
        if (astElements == null) {
            return true;
        }
        for (AstElement e : astElements) {
            if (e == element) {
                return false;
            }
        }
        return true;
    }

    private void onNodeSelected(TreeSelectionEvent e) {
        DefaultMutableTreeNode selectedTreeNode = (DefaultMutableTreeNode) nodeTree.getLastSelectedPathComponent();
        Object                 userObject       = selectedTreeNode == null ? null : selectedTreeNode.getUserObject();
        if (userObject instanceof NodeInfo nodeInfo) {
            for (int i = 0; i < AST_ELEMENT_NODE_LABELS.length; i++) {
                astElementDetailLabels[i].setText(AST_ELEMENT_NODE_LABELS[i]);
            }
            Node node = nodeInfo.node;
            astElementDetailText[0].setText(node.getClass().getSimpleName());
            astElementDetailText[1].setText(node.type() != null ? node.type().name() : "<null>");
            astElementDetailText[2].setText(node.functor() != null ? node.functor().name() : "<null>");
            Token first = node.firstToken();
            Token last  = node.lastToken();
            if (first != null && last != null) {
                astElementDetailText[3].setText(String.format("[%d:%d:%d]", first.line(), first.lastLine(), first.lineEnd()));
                astElementDetailText[4].setText(String.format("[%d:%d:%d]", first.position(), first.lastPosition(), first.positionEnd()));
                astElementDetailText[5].setText(String.format("[%d:%d]", first.index(), first.indexEnd()));
            } else {
                astElementDetailText[3].setText("-");
                astElementDetailText[4].setText("-");
                astElementDetailText[5].setText("-");
            }
            astElementDetailText[6].setText(String.valueOf(node.length()));
            astElementDetailText[7].setText(truncate(node.tokens().toString(), 80));

            // Sync selection: select all tokens under this node in the token list
            if (!isSyncing) {
                isSyncing = true;
                selectTokensInTable(node);
                isSyncing = false;
            }
        } else if (userObject instanceof TokenInfo tokenInfo) {
            for (int i = 0; i < AST_ELEMENT_TOKEN_LABELS.length; i++) {
                astElementDetailLabels[i].setText(AST_ELEMENT_TOKEN_LABELS[i]);
            }
            Token token = tokenInfo.token;
            astElementDetailText[0].setText(Token.class.getSimpleName());
            astElementDetailText[1].setText(token.type().name());
            astElementDetailText[2].setText(token.colorType().name());
            astElementDetailText[3].setText(String.format("[%d:%d:%d]", token.line(), token.lastLine(), token.lineEnd()));
            astElementDetailText[4].setText(String.format("[%d:%d:%d]", token.position(), token.lastPosition(), token.positionEnd()));
            astElementDetailText[5].setText(String.format("[%d:%d]", token.index(), token.indexEnd()));
            astElementDetailText[6].setText(escapeText(token.text()));
            astElementDetailText[7].setText(token.variable() == null ? "<null>" : token.variable().toString());

            // Sync selection: select this token in the token list
            if (!isSyncing) {
                isSyncing = true;
                selectTokenInTable(token);
                isSyncing = false;
            }
        } else {
            for (JLabel label : astElementDetailLabels) {
                label.setText("");
            }
            for (JLabel label : astElementDetailText) {
                label.setText("");
            }
        }
    }

    private void onTokenListSelected(ListSelectionEvent e) {
        if (e.getValueIsAdjusting() || isSyncing) {
            return;
        }

        int selectedRow = tokenListView.getSelectedRow();
        if (selectedRow >= 0 && selectedRow < tokenList.size()) {
            Token token = tokenList.get(selectedRow);
            isSyncing = true;
            selectTokenInTree(token);
            isSyncing = false;
        }
    }

    private void selectTokenInTable(Token token) {
        int index = tokenList.indexOf(token);
        if (index >= 0) {
            tokenListView.setRowSelectionInterval(index, index);
            tokenListView.scrollRectToVisible(tokenListView.getCellRect(index, 0, true));
        }
    }

    private void selectTokensInTable(Node node) {
        Token first = node.firstToken();
        Token last  = node.lastToken();
        if (first == null || last == null) {
            tokenListView.clearSelection();
            return;
        }

        int firstIndex = -1;
        int lastIndex  = -1;

        // Find indices of first and last tokens
        for (int i = 0; i < tokenList.size(); i++) {
            Token t = tokenList.get(i);
            if (t.index() >= first.index() && firstIndex < 0) {
                firstIndex = i;
            }
            if (t.index() <= last.index()) {
                lastIndex = i;
            }
        }

        if (firstIndex >= 0 && lastIndex >= 0) {
            tokenListView.setRowSelectionInterval(firstIndex, lastIndex);
            tokenListView.scrollRectToVisible(tokenListView.getCellRect(firstIndex, 0, true));
        }
    }

    private void selectTokenInTree(Token token) {
        DefaultMutableTreeNode root = (DefaultMutableTreeNode) nodeTree.getModel().getRoot();
        TreePath               path = findTokenInTree(root, token);
        if (path != null) {
            nodeTree.setSelectionPath(path);
            nodeTree.scrollPathToVisible(path);
        }
    }

    private TreePath findTokenInTree(DefaultMutableTreeNode node, Token token) {
        Object userObject = node.getUserObject();
        if (userObject instanceof TokenInfo tokenInfo && tokenInfo.token == token) {
            return new TreePath(node.getPath());
        }

        Enumeration<javax.swing.tree.TreeNode> children = node.children();
        while (children.hasMoreElements()) {
            DefaultMutableTreeNode child = (DefaultMutableTreeNode) children.nextElement();
            TreePath               path  = findTokenInTree(child, token);
            if (path != null) {
                return path;
            }
        }
        return null;
    }

    private String escapeText(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
    }

    private String truncate(String text, int maxLen) {
        if (text == null) {
            return "";
        }
        String escaped = escapeText(text);
        if (escaped.length() <= maxLen) {
            return escaped;
        }
        return escaped.substring(0, maxLen - 3) + "...";
    }

    private void expandAllNodes(JTree tree) {
        for (int i = 0; i < tree.getRowCount(); i++) {
            tree.expandRow(i);
        }
    }

    public void update(TokenizerResult tokenizerResult, ParserResult parserResult) {
        // Update token list
        populateTokenTable(tokenizerResult);

        // Update node tree
        DefaultMutableTreeNode nodeRoot = buildNodeTree(parserResult);
        ((DefaultTreeModel) nodeTree.getModel()).setRoot(nodeRoot);
        expandAllNodes(nodeTree);

        // Clear details
        for (JLabel label : astElementDetailText) {
            label.setText("-");
        }
    }

    // Wrapper class to hold Node reference while providing custom toString for tree display
    private record NodeInfo(Node node,
                            Integer argsIndex) {
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();

            // Add args index prefix if present
            if (argsIndex != null) {
                sb.append("args[").append(argsIndex).append("] ");
            }

            // Always show class name first
            sb.append(node.getClass().getSimpleName());

            // Add functor or type name if different from class name
            if (node.functor() != null) {
                sb.append(" '").append(node.functor().name()).append("'");
            } else if (node.type() != null && !node.type().name().equals(node.getClass().getSimpleName())) {
                sb.append(" '").append(node.type().name()).append("'");
            }

            // Add location info
            Token firstToken = node.firstToken();
            if (firstToken != null) {
                sb.append(" [").append(firstToken.line()).append(":").append(firstToken.position()).append("]");
            } else {
                sb.append(" [no tokens]");
            }

            return sb.toString();
        }
    }

    // Wrapper class to hold Token reference while providing custom toString for tree display
    private record TokenInfo(Token token,
                             Integer argsIndex) {
        public boolean isSkipOrLayout() {
            return token.type().isSkip() || token.type().isLayout();
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();

            // Add args index prefix if present
            if (argsIndex != null) {
                sb.append("args[").append(argsIndex).append("] ");
            }

            sb.append(String.format("[%d:%d] %s: %s", token.line(), token.position(), token.type().name(), escapeTokenText(token.text())));

            return sb.toString();
        }

        private static String escapeTokenText(String text) {
            if (text == null) {
                return "";
            }
            String escaped = text.replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
            if (escaped.length() > 30) {
                return escaped.substring(0, 27) + "...";
            }
            return escaped;
        }
    }

    // Custom table cell renderer for Token list - makes SKIP/LAYOUT tokens grey
    private class TokenTableCellRenderer extends DefaultTableCellRenderer {
        public TokenTableCellRenderer(int alignment) {
            setHorizontalAlignment(alignment);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

            if (!isSelected && row >= 0 && row < tokenList.size()) {
                Token token = tokenList.get(row);
                if (token.type().isSkip() || token.type().isLayout()) {
                    c.setForeground(LAYOUT_TOKEN_TEXT_COLOR);
                } else {
                    c.setForeground(Color.BLACK);
                }
            }

            return c;
        }
    }

    // Custom cell renderer with different background colors for Nodes and Tokens
    private static class AstElementTreeCellRenderer extends JLabel implements javax.swing.tree.TreeCellRenderer {
        private static final Color SELECTION_BACKGROUND = javax.swing.UIManager.getColor("Tree.selectionBackground");
        private static final Color SELECTION_FOREGROUND = javax.swing.UIManager.getColor("Tree.selectionForeground");

        public AstElementTreeCellRenderer() {
            setOpaque(true);
        }

        @Override
        public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus) {
            // Determine colors and text
            Color  background = Color.WHITE;
            Color  foreground = Color.BLACK;
            String text       = value != null ? value.toString() : "";

            if (value instanceof DefaultMutableTreeNode treeNode) {
                Object userObject = treeNode.getUserObject();
                if (userObject != null) {
                    text = userObject.toString();
                }
                if (userObject instanceof NodeInfo) {
                    background = NODE_BACKGROUND;
                } else if (userObject instanceof TokenInfo tokenInfo) {
                    background = TOKEN_BACKGROUND;
                    if (tokenInfo.isSkipOrLayout()) {
                        foreground = LAYOUT_TOKEN_TEXT_COLOR;
                    }
                }
            }

            // Apply selection colors if selected
            if (sel) {
                setBackground(SELECTION_BACKGROUND != null ? SELECTION_BACKGROUND : new Color(0x3875D7));
                setForeground(SELECTION_FOREGROUND != null ? SELECTION_FOREGROUND : Color.WHITE);
            } else {
                setBackground(background);
                setForeground(foreground);
            }

            setText(text);
            setFont(tree.getFont());

            return this;
        }
    }
}
