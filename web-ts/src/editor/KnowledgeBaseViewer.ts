/**
 * KnowledgeBaseViewer - Knowledge base visualization component.
 * Displays facts, rules, and other KB contents.
 */

import type { KnowledgeBase } from '../KnowledgeBase';
import type { Functor } from '../patterns/Functor';

/**
 * KB item for display.
 */
interface KBItem {
  category: string;
  name: string;
  details: string;
  expanded: boolean;
}

/**
 * KnowledgeBaseViewer - displays KB contents.
 */
export class KnowledgeBaseViewer {
  private container: HTMLElement;
  private knowledgeBase: KnowledgeBase | null = null;
  private items: KBItem[] = [];
  private filter: string = '';

  constructor(container: HTMLElement) {
    this.container = container;
    this.setupUI();
  }

  private setupUI(): void {
    this.container.innerHTML = '';
    this.container.className = 'kb-viewer';

    Object.assign(this.container.style, {
      display: 'flex',
      flexDirection: 'column',
      height: '100%',
      fontFamily: "'JetBrains Mono', 'Fira Code', monospace",
      fontSize: '12px',
    });

    // Filter input
    const filterBar = document.createElement('div');
    Object.assign(filterBar.style, {
      padding: '8px',
      borderBottom: '1px solid #ccc',
    });

    const filterInput = document.createElement('input');
    filterInput.type = 'text';
    filterInput.placeholder = 'Filter...';
    Object.assign(filterInput.style, {
      width: '100%',
      padding: '4px 8px',
      border: '1px solid #ccc',
      borderRadius: '4px',
      fontFamily: 'inherit',
      fontSize: 'inherit',
    });
    filterInput.oninput = () => {
      this.filter = filterInput.value.toLowerCase();
      this.render();
    };
    filterBar.appendChild(filterInput);
    this.container.appendChild(filterBar);

    // Content
    const content = document.createElement('div');
    content.className = 'kb-viewer-content';
    Object.assign(content.style, {
      flex: '1',
      overflow: 'auto',
      padding: '8px',
    });
    this.container.appendChild(content);
  }

  /**
   * Update with a knowledge base.
   */
  update(kb: KnowledgeBase | null): void {
    this.knowledgeBase = kb;
    this.buildItems();
    this.render();
  }

  private buildItems(): void {
    this.items = [];

    if (!this.knowledgeBase) return;

    // Functors section
    const functors = this.knowledgeBase.functors();
    if (functors && functors.size > 0) {
      functors.forEach((functor: Functor) => {
        const argTypes = functor.argTypes();
        const argStr = argTypes.map(t => t.name()).join(', ');
        this.items.push({
          category: 'Functors',
          name: functor.name(),
          details: `(${argStr}) -> ${functor.resultType().name()}`,
          expanded: false,
        });
      });
    }

    // Facts section
    try {
      const factsMethod = (this.knowledgeBase as unknown as { allFacts?: () => Iterable<unknown> }).allFacts;
      if (factsMethod) {
        const facts = factsMethod.call(this.knowledgeBase);
        if (facts) {
          for (const fact of facts) {
            this.items.push({
              category: 'Facts',
              name: String(fact),
              details: '',
              expanded: false,
            });
          }
        }
      }
    } catch {
      // Facts method might not exist or throw
    }

    // Rules section
    try {
      const rulesMethod = (this.knowledgeBase as unknown as { allRules?: () => Iterable<unknown> }).allRules;
      if (rulesMethod) {
        const rules = rulesMethod.call(this.knowledgeBase);
        if (rules) {
          for (const rule of rules) {
            this.items.push({
              category: 'Rules',
              name: String(rule),
              details: '',
              expanded: false,
            });
          }
        }
      }
    } catch {
      // Rules method might not exist or throw
    }
  }

  private render(): void {
    const content = this.container.querySelector('.kb-viewer-content');
    if (!content) return;

    content.innerHTML = '';

    if (!this.knowledgeBase) {
      content.textContent = 'No knowledge base loaded';
      return;
    }

    // Filter items
    const filtered = this.filter
      ? this.items.filter(item =>
          item.name.toLowerCase().includes(this.filter) ||
          item.category.toLowerCase().includes(this.filter)
        )
      : this.items;

    // Group by category
    const categories = new Map<string, KBItem[]>();
    for (const item of filtered) {
      if (!categories.has(item.category)) {
        categories.set(item.category, []);
      }
      categories.get(item.category)!.push(item);
    }

    // Render categories
    categories.forEach((items, category) => {
      const section = document.createElement('div');
      section.className = 'kb-section';
      section.style.marginBottom = '16px';

      const header = document.createElement('div');
      header.className = 'kb-section-header';
      Object.assign(header.style, {
        fontWeight: 'bold',
        color: '#0066cc',
        marginBottom: '4px',
        borderBottom: '1px solid #eee',
        paddingBottom: '4px',
      });
      header.textContent = `${category} (${items.length})`;
      section.appendChild(header);

      for (const item of items) {
        const itemEl = this.renderItem(item);
        section.appendChild(itemEl);
      }

      content.appendChild(section);
    });

    if (filtered.length === 0) {
      content.textContent = this.filter
        ? 'No matching items'
        : 'Knowledge base is empty';
    }
  }

  private renderItem(item: KBItem): HTMLElement {
    const div = document.createElement('div');
    div.className = 'kb-item';
    Object.assign(div.style, {
      padding: '2px 0',
      paddingLeft: '8px',
    });

    const name = document.createElement('span');
    name.textContent = item.name;
    name.style.color = this.getCategoryColor(item.category);
    div.appendChild(name);

    if (item.details) {
      const details = document.createElement('span');
      details.textContent = ` ${item.details}`;
      details.style.color = '#666';
      div.appendChild(details);
    }

    return div;
  }

  private getCategoryColor(category: string): string {
    switch (category) {
      case 'Types':
        return '#880088';
      case 'Functors':
        return '#006600';
      case 'Facts':
        return '#0066cc';
      case 'Rules':
        return '#cc6600';
      default:
        return '#333';
    }
  }
}

/**
 * Create and show a knowledge base viewer dialog.
 */
export function showKnowledgeBaseViewerDialog(kb: KnowledgeBase | null): void {
  // Create dialog
  const dialog = document.createElement('div');
  dialog.className = 'kb-viewer-dialog';
  Object.assign(dialog.style, {
    position: 'fixed',
    top: '50px',
    left: '50px',
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
  title.textContent = 'Knowledge Base Viewer';
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

  // Viewer content
  const content = document.createElement('div');
  content.style.flex = '1';
  content.style.overflow = 'hidden';
  dialog.appendChild(content);

  // Create viewer
  const viewer = new KnowledgeBaseViewer(content);
  viewer.update(kb);

  document.body.appendChild(dialog);

  // Make draggable
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
  };

  document.onmouseup = () => {
    isDragging = false;
  };
}
