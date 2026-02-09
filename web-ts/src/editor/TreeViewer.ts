/**
 * TreeViewer - AST visualization component for the Nelumbo editor.
 * Displays the token and parse tree in an expandable tree view.
 */

import type { Token } from '../Token';
import type { TokenizerResult } from '../Tokenizer';
import type { ParserResult } from '../syntax/ParserResult';
import { Node } from '../core/Node';
import { List } from 'immutable';

/**
 * Tree node for display.
 */
interface TreeNode {
  label: string;
  type: string;
  children: TreeNode[];
  expanded: boolean;
  data?: unknown;
}

/**
 * TreeViewer - displays AST in a tree view.
 */
export class TreeViewer {
  private container: HTMLElement;
  private tokenizerResult: TokenizerResult | null = null;
  private parserResult: ParserResult | null = null;
  private tokenTreeRoot: TreeNode | null = null;
  private parseTreeRoot: TreeNode | null = null;
  private activeTab: 'tokens' | 'parse' = 'tokens';

  constructor(container: HTMLElement) {
    this.container = container;
    this.setupUI();
  }

  private setupUI(): void {
    this.container.innerHTML = '';
    this.container.className = 'tree-viewer';

    // Apply styles
    Object.assign(this.container.style, {
      display: 'flex',
      flexDirection: 'column',
      height: '100%',
      fontFamily: "'JetBrains Mono', 'Fira Code', monospace",
      fontSize: '12px',
    });

    // Create tab bar
    const tabBar = document.createElement('div');
    tabBar.className = 'tree-viewer-tabs';
    Object.assign(tabBar.style, {
      display: 'flex',
      borderBottom: '1px solid #ccc',
      padding: '4px',
      gap: '4px',
    });

    const tokensTab = this.createTab('Tokens', 'tokens');
    const parseTab = this.createTab('Parse Tree', 'parse');

    tabBar.appendChild(tokensTab);
    tabBar.appendChild(parseTab);
    this.container.appendChild(tabBar);

    // Create tree container
    const treeContainer = document.createElement('div');
    treeContainer.className = 'tree-viewer-content';
    Object.assign(treeContainer.style, {
      flex: '1',
      overflow: 'auto',
      padding: '8px',
    });
    this.container.appendChild(treeContainer);
  }

  private createTab(label: string, id: 'tokens' | 'parse'): HTMLElement {
    const tab = document.createElement('button');
    tab.textContent = label;
    tab.dataset.tab = id;

    Object.assign(tab.style, {
      padding: '4px 12px',
      border: '1px solid #ccc',
      borderRadius: '4px',
      background: this.activeTab === id ? '#e0e0ff' : '#f0f0f0',
      cursor: 'pointer',
    });

    tab.onclick = () => {
      this.activeTab = id;
      this.render();
    };

    return tab;
  }

  /**
   * Update with new tokenizer and parser results.
   */
  update(tokenizerResult: TokenizerResult | null, parserResult: ParserResult | null): void {
    this.tokenizerResult = tokenizerResult;
    this.parserResult = parserResult;

    // Build trees
    this.tokenTreeRoot = this.buildTokenTree();
    this.parseTreeRoot = this.buildParseTree();

    this.render();
  }

  private buildTokenTree(): TreeNode | null {
    if (!this.tokenizerResult) return null;

    const root: TreeNode = {
      label: 'Tokens',
      type: 'root',
      children: [],
      expanded: true,
    };

    let token: Token | null = this.tokenizerResult.firstAll;
    while (token) {
      root.children.push({
        label: `${token.type.name}: "${this.escapeString(token.text)}"`,
        type: 'token',
        children: [],
        expanded: false,
        data: token,
      });
      token = token.nextAll;
    }

    return root;
  }

  private buildParseTree(): TreeNode | null {
    if (!this.parserResult) return null;

    const roots = this.parserResult.roots();
    if (roots.isEmpty()) return null;

    const root: TreeNode = {
      label: 'Parse Tree',
      type: 'root',
      children: [],
      expanded: true,
    };

    roots.forEach(node => {
      root.children.push(this.buildNodeTree(node));
    });

    return root;
  }

  private buildNodeTree(node: Node): TreeNode {
    const functor = node.functor();
    const type = node.type();
    const label = functor ? functor.name() : type.name();

    const treeNode: TreeNode = {
      label: label,
      type: 'node',
      children: [],
      expanded: true,
      data: node,
    };

    // Add children
    for (let i = 0; i < node.length(); i++) {
      const child = node.get(i);
      if (child instanceof Node) {
        treeNode.children.push(this.buildNodeTree(child as Node));
      } else if (List.isList(child)) {
        const listNode: TreeNode = {
          label: `[${(child as List<unknown>).size} items]`,
          type: 'list',
          children: [],
          expanded: false,
        };
        (child as List<unknown>).forEach((item: unknown) => {
          if (item instanceof Node) {
            listNode.children.push(this.buildNodeTree(item as Node));
          } else {
            listNode.children.push({
              label: String(item),
              type: 'value',
              children: [],
              expanded: false,
              data: item,
            });
          }
        });
        treeNode.children.push(listNode);
      } else if (child !== null && child !== undefined) {
        treeNode.children.push({
          label: String(child),
          type: 'value',
          children: [],
          expanded: false,
          data: child,
        });
      }
    }

    return treeNode;
  }

  private render(): void {
    // Update tab styles
    this.container.querySelectorAll('.tree-viewer-tabs button').forEach(btn => {
      const tab = btn as HTMLButtonElement;
      tab.style.background = tab.dataset.tab === this.activeTab ? '#e0e0ff' : '#f0f0f0';
    });

    // Render tree
    const content = this.container.querySelector('.tree-viewer-content');
    if (!content) return;

    content.innerHTML = '';

    const tree = this.activeTab === 'tokens' ? this.tokenTreeRoot : this.parseTreeRoot;
    if (tree) {
      content.appendChild(this.renderNode(tree, 0));
    } else {
      content.textContent = 'No data available';
    }
  }

  private renderNode(node: TreeNode, depth: number): HTMLElement {
    const div = document.createElement('div');
    div.className = `tree-node tree-node-${node.type}`;

    // Node header
    const header = document.createElement('div');
    header.className = 'tree-node-header';
    Object.assign(header.style, {
      display: 'flex',
      alignItems: 'center',
      padding: '2px 0',
      paddingLeft: `${depth * 16}px`,
      cursor: node.children.length > 0 ? 'pointer' : 'default',
    });

    // Expand/collapse icon
    if (node.children.length > 0) {
      const icon = document.createElement('span');
      icon.textContent = node.expanded ? '▼' : '▶';
      icon.style.width = '16px';
      icon.style.fontSize = '10px';
      header.appendChild(icon);
    } else {
      const spacer = document.createElement('span');
      spacer.style.width = '16px';
      header.appendChild(spacer);
    }

    // Label
    const label = document.createElement('span');
    label.textContent = node.label;
    label.style.color = this.getNodeColor(node.type);
    header.appendChild(label);

    // Click to expand/collapse
    if (node.children.length > 0) {
      header.onclick = () => {
        node.expanded = !node.expanded;
        this.render();
      };
    }

    div.appendChild(header);

    // Children
    if (node.expanded && node.children.length > 0) {
      for (const child of node.children) {
        div.appendChild(this.renderNode(child, depth + 1));
      }
    }

    return div;
  }

  private getNodeColor(type: string): string {
    switch (type) {
      case 'root':
        return '#666';
      case 'node':
        return '#0066cc';
      case 'token':
        return '#006600';
      case 'list':
        return '#cc6600';
      case 'value':
        return '#990099';
      default:
        return '#333';
    }
  }

  private escapeString(s: string): string {
    return s
      .replace(/\\/g, '\\\\')
      .replace(/\n/g, '\\n')
      .replace(/\r/g, '\\r')
      .replace(/\t/g, '\\t')
      .replace(/"/g, '\\"');
  }
}

/**
 * Create and show a tree viewer dialog.
 */
export function showTreeViewerDialog(
  tokenizerResult: TokenizerResult | null,
  parserResult: ParserResult | null
): void {
  // Create dialog
  const dialog = document.createElement('div');
  dialog.className = 'tree-viewer-dialog';
  Object.assign(dialog.style, {
    position: 'fixed',
    top: '50px',
    right: '50px',
    width: '400px',
    height: '500px',
    background: '#fff',
    border: '1px solid #ccc',
    borderRadius: '8px',
    boxShadow: '0 4px 20px rgba(0,0,0,0.2)',
    zIndex: '1000',
    display: 'flex',
    flexDirection: 'column',
  });

  // Header
  const header = document.createElement('div');
  Object.assign(header.style, {
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
    padding: '8px 12px',
    borderBottom: '1px solid #ccc',
    background: '#f5f5f5',
    borderRadius: '8px 8px 0 0',
  });

  const title = document.createElement('span');
  title.textContent = 'Tree Viewer';
  title.style.fontWeight = 'bold';
  header.appendChild(title);

  const closeBtn = document.createElement('button');
  closeBtn.textContent = '×';
  Object.assign(closeBtn.style, {
    border: 'none',
    background: 'transparent',
    fontSize: '20px',
    cursor: 'pointer',
  });
  closeBtn.onclick = () => dialog.remove();
  header.appendChild(closeBtn);

  dialog.appendChild(header);

  // Tree content
  const content = document.createElement('div');
  content.style.flex = '1';
  content.style.overflow = 'hidden';
  dialog.appendChild(content);

  // Create tree viewer
  const viewer = new TreeViewer(content);
  viewer.update(tokenizerResult, parserResult);

  document.body.appendChild(dialog);

  // Make draggable (simplified)
  let isDragging = false;
  let startX = 0, startY = 0;
  let origX = 0, origY = 0;

  header.onmousedown = (e) => {
    if (e.target === closeBtn) return;
    isDragging = true;
    startX = e.clientX;
    startY = e.clientY;
    origX = dialog.offsetLeft;
    origY = dialog.offsetTop;
  };

  document.onmousemove = (e) => {
    if (!isDragging) return;
    dialog.style.left = `${origX + e.clientX - startX}px`;
    dialog.style.top = `${origY + e.clientY - startY}px`;
    dialog.style.right = 'auto';
  };

  document.onmouseup = () => {
    isDragging = false;
  };
}
