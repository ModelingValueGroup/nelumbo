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

import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTree;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.util.ArrayList;
import java.util.Comparator;

import org.modelingvalue.collections.Entry;
import org.modelingvalue.collections.List;
import org.modelingvalue.collections.Map;
import org.modelingvalue.collections.Set;
import org.modelingvalue.nelumbo.AstElement;
import org.modelingvalue.nelumbo.InferResult;
import org.modelingvalue.nelumbo.KnowledgeBase;
import org.modelingvalue.nelumbo.Node;
import org.modelingvalue.nelumbo.Rule;
import org.modelingvalue.nelumbo.Transform;
import org.modelingvalue.nelumbo.logic.Predicate;
import org.modelingvalue.nelumbo.patterns.Functor;

public class KnowledgeBaseViewerDialog extends JDialog {

    private static final String[] FUNCTOR_COLUMNS   = {"Result Type", "Name", "Pattern", "Arg Types"};
    private static final String[] RULE_COLUMNS      = {"Consequence", "Condition"};
    private static final String[] FACT_COLUMNS      = {"Predicate"};
    private static final String[] TRANSFORM_COLUMNS = {"Source", "Targets"};

    private static final Color FUNCTOR_COLOR   = new Color(0xE3F2FD);  // Light blue
    private static final Color RULE_COLOR      = new Color(0xFFF3E0);  // Light orange
    private static final Color FACT_COLOR      = new Color(0xE8F5E9);  // Light green
    private static final Color TRANSFORM_COLOR = new Color(0xF3E5F5);  // Light purple

    private final JTable            functorTable;
    private final DefaultTableModel functorTableModel;
    private final JTable            ruleTable;
    private final DefaultTableModel ruleTableModel;
    private final JTable            factTable;
    private final DefaultTableModel factTableModel;
    private final JTable            transformTable;
    private final DefaultTableModel transformTableModel;
    private final JTree             typeHierarchyTree;
    private final JTextArea         detailsArea;
    private final JLabel            statsLabel;

    private final java.util.List<Functor>   functorList   = new ArrayList<>();
    private final java.util.List<Rule>      ruleList      = new ArrayList<>();
    private final java.util.List<Predicate> factList      = new ArrayList<>();
    private final java.util.List<Transform> transformList = new ArrayList<>();

    public KnowledgeBaseViewerDialog(JFrame parent, KnowledgeBase knowledgeBase) {
        super(parent, "Knowledge Base Viewer", false);
        setLayout(new BorderLayout());

        // Create tabbed pane
        JTabbedPane tabbedPane = new JTabbedPane();

        // Functors tab
        functorTableModel = new DefaultTableModel(FUNCTOR_COLUMNS, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        functorTable      = createTable(functorTableModel, FUNCTOR_COLOR);
        functorTable.getColumnModel().getColumn(0).setPreferredWidth(120);
        functorTable.getColumnModel().getColumn(1).setPreferredWidth(150);
        functorTable.getColumnModel().getColumn(2).setPreferredWidth(300);
        functorTable.getColumnModel().getColumn(3).setPreferredWidth(200);
        JScrollPane functorScroll = new JScrollPane(functorTable);
        functorScroll.getViewport().setBackground(FUNCTOR_COLOR);
        tabbedPane.addTab("Functors", createTabPanel("Functors", functorScroll, FUNCTOR_COLOR));

        // Rules tab
        ruleTableModel = new DefaultTableModel(RULE_COLUMNS, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        ruleTable      = createTable(ruleTableModel, RULE_COLOR);
        ruleTable.getColumnModel().getColumn(0).setPreferredWidth(300);
        ruleTable.getColumnModel().getColumn(1).setPreferredWidth(500);
        JScrollPane ruleScroll = new JScrollPane(ruleTable);
        ruleScroll.getViewport().setBackground(RULE_COLOR);
        tabbedPane.addTab("Rules", createTabPanel("Rules", ruleScroll, RULE_COLOR));

        // Facts tab
        factTableModel = new DefaultTableModel(FACT_COLUMNS, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        factTable      = createTable(factTableModel, FACT_COLOR);
        factTable.getColumnModel().getColumn(0).setPreferredWidth(600);
        JScrollPane factScroll = new JScrollPane(factTable);
        factScroll.getViewport().setBackground(FACT_COLOR);
        tabbedPane.addTab("Facts", createTabPanel("Facts", factScroll, FACT_COLOR));

        // Transforms tab
        transformTableModel = new DefaultTableModel(TRANSFORM_COLUMNS, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        transformTable      = createTable(transformTableModel, TRANSFORM_COLOR);
        transformTable.getColumnModel().getColumn(0).setPreferredWidth(300);
        transformTable.getColumnModel().getColumn(1).setPreferredWidth(500);
        JScrollPane transformScroll = new JScrollPane(transformTable);
        transformScroll.getViewport().setBackground(TRANSFORM_COLOR);
        tabbedPane.addTab("Transforms", createTabPanel("Transforms", transformScroll, TRANSFORM_COLOR));

        // Type Hierarchy tab
        typeHierarchyTree = new JTree(new DefaultMutableTreeNode("Types"));
        typeHierarchyTree.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        JScrollPane typeScroll = new JScrollPane(typeHierarchyTree);
        tabbedPane.addTab("Type Hierarchy", typeScroll);

        // Details panel
        detailsArea = new JTextArea(8, 60);
        detailsArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        detailsArea.setEditable(false);
        detailsArea.setLineWrap(true);
        detailsArea.setWrapStyleWord(true);
        JScrollPane detailsScroll = new JScrollPane(detailsArea);
        detailsScroll.setBorder(BorderFactory.createTitledBorder("Details"));

        // Add selection listeners
        functorTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                showFunctorDetails();
            }
        });
        ruleTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                showRuleDetails();
            }
        });
        factTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                showFactDetails();
            }
        });
        transformTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                showTransformDetails();
            }
        });

        // Split pane for tabs and details
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, tabbedPane, detailsScroll);
        splitPane.setDividerLocation(400);
        splitPane.setContinuousLayout(true);
        add(splitPane, BorderLayout.CENTER);

        // Stats label
        statsLabel = new JLabel("", SwingConstants.CENTER);
        statsLabel.setFont(statsLabel.getFont().deriveFont(Font.ITALIC, 11f));
        statsLabel.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));
        add(statsLabel, BorderLayout.SOUTH);

        // Populate with initial data
        update(knowledgeBase);

        setPreferredSize(new Dimension(900, 600));
        pack();
        setLocationRelativeTo(parent);
    }

    private JTable createTable(DefaultTableModel model, Color bgColor) {
        JTable table = new JTable(model);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
        table.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        table.setRowHeight(20);

        // Custom renderer with background color
        DefaultTableCellRenderer renderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                                                           boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if (!isSelected) {
                    c.setBackground(bgColor);
                }
                return c;
            }
        };
        for (int i = 0; i < table.getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setCellRenderer(renderer);
        }

        return table;
    }

    private JPanel createTabPanel(String title, JScrollPane scrollPane, Color bgColor) {
        JPanel panel = new JPanel(new BorderLayout());
        JLabel label = new JLabel(title, SwingConstants.CENTER);
        label.setOpaque(true);
        label.setBackground(bgColor);
        label.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));
        panel.add(label, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);
        return panel;
    }

    public void update(KnowledgeBase knowledgeBase) {
        if (knowledgeBase == null) {
            clearAll();
            return;
        }

        populateFunctors(knowledgeBase);
        populateRules(knowledgeBase);
        populateFacts(knowledgeBase);
        populateTransforms(knowledgeBase);
        populateTypeHierarchy(knowledgeBase);

        // Update stats
        statsLabel.setText(String.format("Functors: %d | Rules: %d | Facts: %d | Transforms: %d",
                                         functorList.size(), ruleList.size(), factList.size(), transformList.size()));

        detailsArea.setText("");
    }

    private void clearAll() {
        functorTableModel.setRowCount(0);
        ruleTableModel.setRowCount(0);
        factTableModel.setRowCount(0);
        transformTableModel.setRowCount(0);
        functorList.clear();
        ruleList.clear();
        factList.clear();
        transformList.clear();
        ((DefaultTreeModel) typeHierarchyTree.getModel()).setRoot(new DefaultMutableTreeNode("Types"));
        statsLabel.setText("No knowledge base loaded");
        detailsArea.setText("");
    }

    private void populateFunctors(KnowledgeBase kb) {
        functorTableModel.setRowCount(0);
        functorList.clear();

        Set<Functor> functors = kb.functors();
        if (functors == null) {
            return;
        }

        // Sort by result type name, then by functor name
        java.util.List<Functor> sorted = new ArrayList<>();
        for (Functor f : functors) {
            sorted.add(f);
        }
        sorted.sort(Comparator.comparing((Functor f) -> f.resultType() != null ? f.resultType().name() : "")
                              .thenComparing(Functor::name));

        for (Functor functor : sorted) {
            functorList.add(functor);
            String resultType = functor.resultType() != null ? functor.resultType().name() : "<null>";
            String name       = functor.name();
            String pattern    = formatPattern(functor);
            String argTypes   = formatArgTypes(functor);
            functorTableModel.addRow(new Object[]{resultType, name, pattern, argTypes});
        }
    }

    private String formatPattern(Functor functor) {
        if (functor.pattern() != null) {
            return truncate(functor.pattern().toString(), 80);
        }
        return "<no pattern>";
    }

    private String formatArgTypes(Functor functor) {
        org.modelingvalue.collections.List<org.modelingvalue.nelumbo.Type> argTypes = functor.argTypes();
        if (argTypes == null || argTypes.isEmpty()) {
            return "()";
        }
        StringBuilder sb    = new StringBuilder("(");
        boolean       first = true;
        for (org.modelingvalue.nelumbo.Type t : argTypes) {
            if (!first) {
                sb.append(", ");
            }
            sb.append(t != null ? t.name() : "?");
            first = false;
        }
        sb.append(")");
        return sb.toString();
    }

    private void populateRules(KnowledgeBase kb) {
        ruleTableModel.setRowCount(0);
        ruleList.clear();

        Set<Rule> rules = kb.rules();
        if (rules == null) {
            return;
        }

        for (Rule rule : rules) {
            ruleList.add(rule);
            String consequence = formatPredicate(rule.consequence());
            String condition   = formatCondition(rule);
            ruleTableModel.addRow(new Object[]{consequence, condition});
        }
    }

    private String formatPredicate(Predicate predicate) {
        if (predicate == null) {
            return "<null>";
        }
        return truncate(predicate.toString(), 80);
    }

    private String formatCondition(Rule rule) {
        if (rule.condition() == null) {
            return "<no condition>";
        }
        return truncate(rule.condition().toString(), 100);
    }

    private void populateFacts(KnowledgeBase kb) {
        factTableModel.setRowCount(0);
        factList.clear();

        Map<Predicate, InferResult> facts = kb.facts();
        if (facts == null) {
            return;
        }

        for (Entry<Predicate, InferResult> entry : facts) {
            // Only show facts that are confirmed true
            if (entry.getValue() != null && entry.getValue().isTrueCC()) {
                factList.add(entry.getKey());
                factTableModel.addRow(new Object[]{truncate(entry.getKey().toString(), 120)});
            }
        }
    }

    private void populateTransforms(KnowledgeBase kb) {
        transformTableModel.setRowCount(0);
        transformList.clear();

        Set<Transform> transforms = kb.transforms();
        if (transforms == null) {
            return;
        }

        for (Transform transform : transforms) {
            transformList.add(transform);
            String source  = formatNode(transform.source());
            String targets = formatTargets(transform.targets());
            transformTableModel.addRow(new Object[]{source, targets});
        }
    }

    private String formatNode(Node node) {
        if (node == null) {
            return "<null>";
        }
        StringBuilder sb = new StringBuilder();
        if (node.functor() != null) {
            sb.append(node.functor().name());
        } else if (node.type() != null) {
            sb.append(node.type().name());
        } else {
            sb.append(node.getClass().getSimpleName());
        }
        return truncate(sb.toString(), 80);
    }

    private String formatTargets(List<Node> targets) {
        if (targets == null || targets.isEmpty()) {
            return "<none>";
        }
        StringBuilder sb    = new StringBuilder();
        boolean       first = true;
        for (Node target : targets) {
            if (!first) {
                sb.append(", ");
            }
            sb.append(formatNode(target));
            first = false;
        }
        return truncate(sb.toString(), 100);
    }

    private void populateTypeHierarchy(KnowledgeBase kb) {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("Types");

        // Collect all types from functors - using fully qualified names to avoid conflict with java.awt.Window.Type
        java.util.Set<org.modelingvalue.nelumbo.Type>                                      allTypes  = new java.util.HashSet<>();
        java.util.Map<org.modelingvalue.nelumbo.Type, Set<org.modelingvalue.nelumbo.Type>> supersMap = new java.util.HashMap<>();

        for (Functor f : kb.functors()) {
            org.modelingvalue.nelumbo.Type resultType = f.resultType();
            if (resultType != null) {
                allTypes.add(resultType);
                Set<org.modelingvalue.nelumbo.Type> supers = resultType.supers();
                if (supers != null) {
                    supersMap.put(resultType, supers);
                    for (org.modelingvalue.nelumbo.Type sup : supers) {
                        allTypes.add(sup);
                    }
                }
            }
        }

        // Find root types (types with no supers or only Object as super)
        java.util.List<org.modelingvalue.nelumbo.Type> rootTypes = new ArrayList<>();
        for (org.modelingvalue.nelumbo.Type t : allTypes) {
            Set<org.modelingvalue.nelumbo.Type> supers = supersMap.get(t);
            if (supers == null || supers.isEmpty()) {
                rootTypes.add(t);
            }
        }
        rootTypes.sort(Comparator.comparing(org.modelingvalue.nelumbo.Type::name));

        // Build tree recursively
        java.util.Set<org.modelingvalue.nelumbo.Type> visited = new java.util.HashSet<>();
        for (org.modelingvalue.nelumbo.Type t : rootTypes) {
            DefaultMutableTreeNode typeNode = buildTypeNode(t, allTypes, supersMap, visited);
            root.add(typeNode);
        }

        ((DefaultTreeModel) typeHierarchyTree.getModel()).setRoot(root);
        expandAllNodes(typeHierarchyTree);
    }

    private DefaultMutableTreeNode buildTypeNode(org.modelingvalue.nelumbo.Type theType,
                                                 java.util.Set<org.modelingvalue.nelumbo.Type> allTypes,
                                                 java.util.Map<org.modelingvalue.nelumbo.Type, Set<org.modelingvalue.nelumbo.Type>> supersMap,
                                                 java.util.Set<org.modelingvalue.nelumbo.Type> visited) {
        String nodeText = theType.name();
        if (theType.group() != null && !theType.group().equals(org.modelingvalue.nelumbo.Type.DEFAULT_GROUP)) {
            nodeText += " [" + theType.group() + "]";
        }
        DefaultMutableTreeNode node = new DefaultMutableTreeNode(nodeText);

        if (visited.contains(theType)) {
            return node; // Prevent cycles
        }
        visited.add(theType);

        // Find subtypes
        java.util.List<org.modelingvalue.nelumbo.Type> subtypes = new ArrayList<>();
        for (org.modelingvalue.nelumbo.Type t : allTypes) {
            Set<org.modelingvalue.nelumbo.Type> supers = supersMap.get(t);
            if (supers != null && supers.contains(theType)) {
                subtypes.add(t);
            }
        }
        subtypes.sort(Comparator.comparing(org.modelingvalue.nelumbo.Type::name));

        for (org.modelingvalue.nelumbo.Type subtype : subtypes) {
            node.add(buildTypeNode(subtype, allTypes, supersMap, visited));
        }

        return node;
    }

    private void expandAllNodes(JTree tree) {
        for (int i = 0; i < tree.getRowCount(); i++) {
            tree.expandRow(i);
        }
    }

    private void showFunctorDetails() {
        int row = functorTable.getSelectedRow();
        if (row >= 0 && row < functorList.size()) {
            Functor       functor = functorList.get(row);
            StringBuilder sb      = new StringBuilder();
            sb.append("FUNCTOR DETAILS\n");
            sb.append("===============\n\n");
            sb.append("Name:        ").append(functor.name()).append("\n");
            sb.append("Result Type: ").append(functor.resultType() != null ? functor.resultType().name() : "<null>").append("\n");
            sb.append("Pattern:     ").append(functor.pattern() != null ? functor.pattern().toString() : "<null>").append("\n");
            sb.append("Arg Types:   ").append(formatArgTypes(functor)).append("\n");
            sb.append("Left:        ").append(functor.left() != null ? functor.left().toString() : "<null>").append("\n");
            sb.append("Local:       ").append(functor.local()).append("\n");
            sb.append("\nAST Elements:\n");
            List<AstElement> elements = functor.astElements();
            if (elements != null && !elements.isEmpty()) {
                int i = 0;
                for (AstElement e : elements) {
                    sb.append("  [").append(i++).append("] ").append(e.getClass().getSimpleName());
                    sb.append(": ").append(truncate(e.toString(), 60)).append("\n");
                }
            } else {
                sb.append("  (none)\n");
            }
            detailsArea.setText(sb.toString());
            detailsArea.setCaretPosition(0);
        }
    }

    private void showRuleDetails() {
        int row = ruleTable.getSelectedRow();
        if (row >= 0 && row < ruleList.size()) {
            Rule          rule = ruleList.get(row);
            StringBuilder sb   = new StringBuilder();
            sb.append("RULE DETAILS\n");
            sb.append("============\n\n");
            sb.append("Consequence:\n  ").append(rule.consequence() != null ? rule.consequence().toString() : "<null>").append("\n\n");
            sb.append("Condition:\n  ").append(rule.condition() != null ? rule.condition().toString() : "<null>").append("\n\n");
            sb.append("Functor:     ").append(rule.functor() != null ? rule.functor().name() : "<null>").append("\n");
            sb.append("Type:        ").append(rule.type() != null ? rule.type().name() : "<null>").append("\n");
            sb.append("\nAST Elements:\n");
            List<AstElement> elements = rule.astElements();
            if (elements != null && !elements.isEmpty()) {
                int i = 0;
                for (AstElement e : elements) {
                    sb.append("  [").append(i++).append("] ").append(e.getClass().getSimpleName());
                    sb.append(": ").append(truncate(e.toString(), 60)).append("\n");
                }
            } else {
                sb.append("  (none)\n");
            }
            detailsArea.setText(sb.toString());
            detailsArea.setCaretPosition(0);
        }
    }

    private void showFactDetails() {
        int row = factTable.getSelectedRow();
        if (row >= 0 && row < factList.size()) {
            Predicate     fact = factList.get(row);
            StringBuilder sb   = new StringBuilder();
            sb.append("FACT DETAILS\n");
            sb.append("============\n\n");
            sb.append("Predicate: ").append(fact.toString()).append("\n\n");
            sb.append("Functor:   ").append(fact.functor() != null ? fact.functor().name() : "<null>").append("\n");
            sb.append("Type:      ").append(fact.type() != null ? fact.type().name() : "<null>").append("\n");
            sb.append("Length:    ").append(fact.length()).append("\n\n");
            sb.append("Arguments:\n");
            for (int i = 0; i < fact.length(); i++) {
                Object arg = fact.get(i);
                sb.append("  [").append(i).append("] ");
                if (arg != null) {
                    sb.append(arg.getClass().getSimpleName()).append(": ");
                    sb.append(truncate(arg.toString(), 60));
                } else {
                    sb.append("<null>");
                }
                sb.append("\n");
            }
            detailsArea.setText(sb.toString());
            detailsArea.setCaretPosition(0);
        }
    }

    private void showTransformDetails() {
        int row = transformTable.getSelectedRow();
        if (row >= 0 && row < transformList.size()) {
            Transform     transform = transformList.get(row);
            StringBuilder sb        = new StringBuilder();
            sb.append("TRANSFORM DETAILS\n");
            sb.append("=================\n\n");
            sb.append("Source:\n");
            Node source = transform.source();
            if (source != null) {
                sb.append("  Type:    ").append(source.type() != null ? source.type().name() : "<null>").append("\n");
                sb.append("  Functor: ").append(source.functor() != null ? source.functor().name() : "<null>").append("\n");
                sb.append("  Value:   ").append(truncate(source.toString(), 80)).append("\n");
            } else {
                sb.append("  <null>\n");
            }
            sb.append("\nTargets:\n");
            List<Node> targets = transform.targets();
            if (targets != null && !targets.isEmpty()) {
                int i = 0;
                for (Node target : targets) {
                    sb.append("  [").append(i++).append("] ");
                    if (target != null) {
                        sb.append(target.getClass().getSimpleName());
                        if (target.functor() != null) {
                            sb.append(" '").append(target.functor().name()).append("'");
                        } else if (target.type() != null) {
                            sb.append(" <").append(target.type().name()).append(">");
                        }
                        sb.append("\n       ").append(truncate(target.toString(), 70));
                    } else {
                        sb.append("<null>");
                    }
                    sb.append("\n");
                }
            } else {
                sb.append("  (none)\n");
            }
            sb.append("\nTargets Flattened:\n");
            List<Node> flattened = transform.targetsFlattened();
            if (flattened != null && !flattened.isEmpty()) {
                int i = 0;
                for (Node target : flattened) {
                    sb.append("  [").append(i++).append("] ").append(formatNode(target)).append("\n");
                }
            } else {
                sb.append("  (none)\n");
            }
            sb.append("\nLiterals:\n");
            Set<Functor> literals = transform.literals();
            if (literals != null && !literals.isEmpty()) {
                for (Functor lit : literals) {
                    sb.append("  - ").append(lit.name()).append("\n");
                }
            } else {
                sb.append("  (none)\n");
            }
            detailsArea.setText(sb.toString());
            detailsArea.setCaretPosition(0);
        }
    }

    private String truncate(String text, int maxLen) {
        if (text == null) {
            return "";
        }
        String escaped = text.replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
        if (escaped.length() <= maxLen) {
            return escaped;
        }
        return escaped.substring(0, maxLen - 3) + "...";
    }
}
