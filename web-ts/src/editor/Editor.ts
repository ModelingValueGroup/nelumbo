/**
 * Editor - Main editor component for the Nelumbo web application.
 * Coordinates the window manager, tabs, and UI.
 */

import { WindowManager } from './WindowManager';
import { EditorTheme } from './EditorTheme';

/**
 * Editor configuration options.
 */
export interface EditorConfig {
  container: HTMLElement;
  theme?: 'light' | 'dark';
}

/**
 * Editor - the main Nelumbo editor application.
 */
export class Editor {
  private container: HTMLElement;
  private windowManager: WindowManager;
  private headerElement: HTMLElement | null = null;
  private tabsElement: HTMLElement | null = null;
  private mainElement: HTMLElement | null = null;
  private editorContainer: HTMLElement | null = null;
  private messagesContainer: HTMLElement | null = null;

  constructor(config: EditorConfig) {
    this.container = config.container;
    this.windowManager = new WindowManager();

    // Set theme
    if (config.theme === 'dark') {
      EditorTheme.useDarkTheme();
    } else {
      EditorTheme.useLightTheme();
    }

    this.initUI();
    this.initWindowManager();
  }

  private initUI(): void {
    // Clear container
    this.container.innerHTML = '';
    this.container.className = 'nelumbo-editor-app';

    // Apply base styles
    this.applyBaseStyles();

    // Create header
    this.headerElement = this.createHeader();
    this.container.appendChild(this.headerElement);

    // Create tab bar
    this.tabsElement = document.createElement('div');
    this.tabsElement.className = 'nelumbo-tabs';
    this.container.appendChild(this.tabsElement);

    // Create main content area
    this.mainElement = document.createElement('div');
    this.mainElement.className = 'nelumbo-main';
    this.container.appendChild(this.mainElement);

    // Create split pane
    this.editorContainer = document.createElement('div');
    this.editorContainer.className = 'nelumbo-editor-pane';

    this.messagesContainer = document.createElement('div');
    this.messagesContainer.className = 'nelumbo-messages-pane';

    this.mainElement.appendChild(this.editorContainer);
    this.mainElement.appendChild(this.messagesContainer);
  }

  private applyBaseStyles(): void {
    const isDark = EditorTheme.isDarkTheme();
    const bgColor = isDark ? '#1e1e2e' : '#ffffff';
    const textColor = isDark ? '#f8f8f2' : '#1e1e2e';
    const borderColor = isDark ? '#333' : '#e0e0e0';
    const accentColor = isDark ? '#bd93f9' : '#5a5acf';

    // Inject CSS
    const styleId = 'nelumbo-editor-styles';
    let styleEl = document.getElementById(styleId) as HTMLStyleElement;
    if (!styleEl) {
      styleEl = document.createElement('style');
      styleEl.id = styleId;
      document.head.appendChild(styleEl);
    }

    styleEl.textContent = `
      .nelumbo-editor-app {
        display: flex;
        flex-direction: column;
        height: 100%;
        background: ${bgColor};
        color: ${textColor};
        font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
      }

      .nelumbo-header {
        display: flex;
        align-items: center;
        padding: 8px 16px;
        background: ${isDark ? '#16161d' : '#f5f5f5'};
        border-bottom: 1px solid ${borderColor};
        gap: 16px;
        position: relative;
        z-index: 100;
      }

      .nelumbo-header h1 {
        font-size: 16px;
        font-weight: 600;
        color: ${accentColor};
        margin: 0;
      }

      .nelumbo-header-actions {
        display: flex;
        gap: 8px;
        margin-left: auto;
      }

      .nelumbo-header button, .nelumbo-header select {
        padding: 6px 12px;
        border: 1px solid ${borderColor};
        border-radius: 4px;
        background: ${isDark ? '#2a2a3a' : '#ffffff'};
        color: ${textColor};
        cursor: pointer;
        font-size: 13px;
      }

      .nelumbo-header button:hover {
        background: ${isDark ? '#3a3a4a' : '#f0f0f0'};
      }

      .nelumbo-tabs {
        display: flex;
        background: ${isDark ? '#16161d' : '#f5f5f5'};
        border-bottom: 1px solid ${borderColor};
        overflow-x: auto;
        padding: 0 8px;
        position: relative;
        z-index: 100;
      }

      .editor-tab {
        display: flex;
        align-items: center;
        padding: 8px 12px;
        border: 1px solid transparent;
        border-bottom: none;
        cursor: pointer;
        font-size: 13px;
        gap: 8px;
        white-space: nowrap;
        background: transparent;
        color: ${isDark ? '#888' : '#666'};
        border-radius: 4px 4px 0 0;
        margin-right: 2px;
      }

      .editor-tab:hover {
        background: ${isDark ? '#2a2a3a' : '#e8e8e8'};
      }

      .editor-tab.active {
        background: ${bgColor};
        color: ${textColor};
        border-color: ${borderColor};
        border-bottom-color: ${bgColor};
        margin-bottom: -1px;
      }

      .editor-tab .tab-close {
        width: 18px;
        height: 18px;
        display: flex;
        align-items: center;
        justify-content: center;
        border-radius: 3px;
        font-size: 16px;
        line-height: 1;
      }

      .editor-tab .tab-close:hover {
        background: ${isDark ? '#ff5555' : '#ff4444'};
        color: white;
      }

      .editor-tab.new-tab {
        font-size: 18px;
        padding: 8px 16px;
        color: ${isDark ? '#666' : '#999'};
      }

      .editor-tab.new-tab:hover {
        color: ${accentColor};
      }

      .nelumbo-main {
        flex: 1;
        display: flex;
        overflow: hidden;
        position: relative;
        z-index: 1;
      }

      .nelumbo-editor-pane {
        flex: 2;
        position: relative;
        overflow: hidden;
        border-right: 1px solid ${borderColor};
      }

      .nelumbo-messages-pane {
        flex: 1;
        min-width: 300px;
        position: relative;
        overflow: auto;
        background: ${isDark ? '#1a1a24' : '#fafafa'};
      }

      .editor-window, .messages-window {
        position: absolute;
        top: 0;
        left: 0;
        width: 100%;
        height: 100%;
      }

      /* Token highlighting classes */
      ${this.generateTokenCSS()}
    `;
  }

  private generateTokenCSS(): string {
    const css: string[] = [];
    const colors = EditorTheme.getAllTokenColors();

    colors.forEach((scheme, typeName) => {
      const className = `.token-${typeName.toLowerCase()}`;
      const styles = EditorTheme.schemeToCSS(scheme);
      if (styles) {
        css.push(`${className} { ${styles}; }`);
      }
    });

    return css.join('\n');
  }

  private createHeader(): HTMLElement {
    const header = document.createElement('div');
    header.className = 'nelumbo-header';

    // Logo/title
    const title = document.createElement('h1');
    title.textContent = 'Nelumbo Editor';
    header.appendChild(title);

    // Actions
    const actions = document.createElement('div');
    actions.className = 'nelumbo-header-actions';

    // Examples dropdown
    const examplesSelect = document.createElement('select');
    examplesSelect.innerHTML = '<option value="">Open Example...</option>';

    const resources = this.windowManager.getExampleResources();
    const categories = new Map<string, typeof resources>();

    for (const resource of resources) {
      if (!categories.has(resource.category)) {
        categories.set(resource.category, []);
      }
      categories.get(resource.category)!.push(resource);
    }

    categories.forEach((items, category) => {
      const optgroup = document.createElement('optgroup');
      optgroup.label = category;

      for (const item of items) {
        const option = document.createElement('option');
        option.value = item.resourcePath;
        option.textContent = item.displayName;
        option.dataset.displayName = item.displayName;
        optgroup.appendChild(option);
      }

      examplesSelect.appendChild(optgroup);
    });

    examplesSelect.addEventListener('change', () => {
      console.log('Example select changed');
      const option = examplesSelect.selectedOptions[0];
      if (option && option.value) {
        const displayName = option.dataset.displayName ?? option.textContent ?? 'Example';
        this.windowManager.createExampleWindow(option.value, displayName);
        examplesSelect.selectedIndex = 0;
      }
    });

    actions.appendChild(examplesSelect);

    // Theme toggle
    const themeBtn = document.createElement('button');
    themeBtn.textContent = EditorTheme.isDarkTheme() ? 'Light' : 'Dark';
    themeBtn.addEventListener('click', () => {
      console.log('Theme button clicked');
      if (EditorTheme.isDarkTheme()) {
        EditorTheme.useLightTheme();
        themeBtn.textContent = 'Dark';
      } else {
        EditorTheme.useDarkTheme();
        themeBtn.textContent = 'Light';
      }
      this.applyBaseStyles();
      // Refresh all windows to apply new theme
      this.windowManager.getWindowsInOrder().forEach(w => w.refresh());
    });
    actions.appendChild(themeBtn);

    header.appendChild(actions);

    return header;
  }

  private initWindowManager(): void {
    if (!this.tabsElement || !this.editorContainer || !this.messagesContainer) {
      throw new Error('UI not initialized');
    }

    this.windowManager.setContainers(
      this.tabsElement,
      this.editorContainer,
      this.messagesContainer
    );

    // Try to restore windows
    this.windowManager.restoreWindows();

    // If no windows, create a new one with example content
    if (!this.windowManager.hasOpenWindows()) {
      const window = this.windowManager.createNewWindow(DEFAULT_CONTENT);
      window.refresh();
    }

    // Listen for window changes to update UI
    this.windowManager.addChangeListener(() => {
      // Could add status bar updates, etc.
    });
  }

  /**
   * Get the window manager.
   */
  getWindowManager(): WindowManager {
    return this.windowManager;
  }

  /**
   * Create a new window.
   */
  createNewWindow(content?: string): void {
    this.windowManager.createNewWindow(content);
  }

  /**
   * Open an example.
   */
  async openExample(resourcePath: string, displayName: string): Promise<void> {
    await this.windowManager.createExampleWindow(resourcePath, displayName);
  }
}

/**
 * Default content for new editors.
 */
const DEFAULT_CONTENT = `// hello world
`;
