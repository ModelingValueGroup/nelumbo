/**
 * WindowManager - Multi-tab coordination for the Nelumbo editor.
 * Manages multiple editor windows/tabs.
 */

import { EditorWindow } from './EditorWindow';

/**
 * Window list change listener.
 */
export type WindowListChangeHandler = () => void;

/**
 * Example resource definition.
 */
export interface ExampleResource {
  category: string;
  fileName: string;
  displayName: string;
  resourcePath: string;
}

/**
 * Built-in examples and libraries.
 */
const EXAMPLE_RESOURCES: ExampleResource[] = [
  // Library files
  { category: 'Library', fileName: 'logic.nl', displayName: 'nelumbo.logic', resourcePath: '/examples/logic.nl' },
  { category: 'Library', fileName: 'integers.nl', displayName: 'nelumbo.integers', resourcePath: '/examples/integers.nl' },
  { category: 'Library', fileName: 'strings.nl', displayName: 'nelumbo.strings', resourcePath: '/examples/strings.nl' },
  { category: 'Library', fileName: 'collections.nl', displayName: 'nelumbo.collections', resourcePath: '/examples/collections.nl' },
  // Examples
  { category: 'Examples', fileName: 'familyTest.nl', displayName: 'Family', resourcePath: '/examples/familyTest.nl' },
  { category: 'Examples', fileName: 'friendsTest.nl', displayName: 'Friends', resourcePath: '/examples/friendsTest.nl' },
  { category: 'Examples', fileName: 'fibonacciTest.nl', displayName: 'Fibonacci', resourcePath: '/examples/fibonacciTest.nl' },
  { category: 'Examples', fileName: 'logicTest.nl', displayName: 'Logic Test', resourcePath: '/examples/logicTest.nl' },
];

/**
 * WindowManager - manages multiple editor windows.
 */
export class WindowManager {
  private windows: Map<string, EditorWindow> = new Map();
  private windowOrder: string[] = [];
  private activeWindowId: string | null = null;
  private nextWindowNumber = 1;
  private changeListeners: WindowListChangeHandler[] = [];
  private tabContainer: HTMLElement | null = null;
  private editorContainer: HTMLElement | null = null;
  private messagesContainer: HTMLElement | null = null;

  constructor() {
    this.loadWindowList();
  }

  /**
   * Set the UI containers.
   */
  setContainers(
    tabContainer: HTMLElement,
    editorContainer: HTMLElement,
    messagesContainer: HTMLElement
  ): void {
    this.tabContainer = tabContainer;
    this.editorContainer = editorContainer;
    this.messagesContainer = messagesContainer;
  }

  /**
   * Create a new editor window.
   */
  createNewWindow(content: string = ''): EditorWindow {
    const id = this.generateWindowId();
    const windowNumber = this.nextWindowNumber++;
    const title = `editor.nelumbo_${windowNumber}`;

    const window = this.createWindowInstance(id, title, false);
    if (content) {
      window.setContent(content);
    }

    this.addWindow(window);
    this.setActiveWindow(id);

    return window;
  }

  /**
   * Create an example window.
   */
  async createExampleWindow(resourcePath: string, displayName: string): Promise<EditorWindow> {
    // Check if already open
    for (const window of this.windows.values()) {
      if (window.getTitle() === displayName) {
        this.setActiveWindow(window.getId());
        return window;
      }
    }

    const id = this.generateWindowId();
    const window = this.createWindowInstance(id, displayName, true, resourcePath);

    // Load content from resource
    try {
      const response = await fetch(resourcePath);
      if (response.ok) {
        const content = await response.text();
        window.setContent(content);
      }
    } catch (e) {
      console.error(`Failed to load example: ${resourcePath}`, e);
    }

    this.addWindow(window);
    this.setActiveWindow(id);

    return window;
  }

  private createWindowInstance(
    id: string,
    title: string,
    isExample: boolean,
    examplePath?: string
  ): EditorWindow {
    if (!this.editorContainer || !this.messagesContainer) {
      throw new Error('Containers not set. Call setContainers first.');
    }

    // Create container elements for this window
    const editorDiv = document.createElement('div');
    editorDiv.className = 'editor-window';
    editorDiv.style.display = 'none';
    editorDiv.style.width = '100%';
    editorDiv.style.height = '100%';
    this.editorContainer.appendChild(editorDiv);

    const messagesDiv = document.createElement('div');
    messagesDiv.className = 'messages-window';
    messagesDiv.style.display = 'none';
    messagesDiv.style.width = '100%';
    messagesDiv.style.height = '100%';
    this.messagesContainer.appendChild(messagesDiv);

    const window = new EditorWindow(id, editorDiv, messagesDiv, title, isExample, examplePath);

    // Store container references
    (window as unknown as { _editorDiv: HTMLElement })._editorDiv = editorDiv;
    (window as unknown as { _messagesDiv: HTMLElement })._messagesDiv = messagesDiv;

    // Handle window changes
    window.onChange(() => {
      this.saveWindowList();
      window.saveState();
      this.updateTabs();
    });

    return window;
  }

  private addWindow(window: EditorWindow): void {
    this.windows.set(window.getId(), window);
    this.windowOrder.push(window.getId());
    this.saveWindowList();
    this.notifyListeners();
    this.updateTabs();
  }

  /**
   * Close a window.
   */
  closeWindow(windowId: string): void {
    const window = this.windows.get(windowId);
    if (!window) return;

    // Confirm if editable
    if (!window.getIsExample()) {
      if (!confirm('The contents of this window will be lost. Are you sure?')) {
        return;
      }
    }

    // Remove window
    this.windows.delete(windowId);
    const orderIndex = this.windowOrder.indexOf(windowId);
    if (orderIndex >= 0) {
      this.windowOrder.splice(orderIndex, 1);
    }

    // Remove DOM elements
    const editorDiv = (window as unknown as { _editorDiv?: HTMLElement })._editorDiv;
    const messagesDiv = (window as unknown as { _messagesDiv?: HTMLElement })._messagesDiv;
    editorDiv?.remove();
    messagesDiv?.remove();

    window.clearState();
    window.dispose();

    // Switch to another window or create new
    if (this.activeWindowId === windowId) {
      if (this.windowOrder.length > 0) {
        this.setActiveWindow(this.windowOrder[0]);
      } else {
        this.createNewWindow();
      }
    }

    this.saveWindowList();
    this.notifyListeners();
    this.updateTabs();
  }

  /**
   * Set the active window.
   */
  setActiveWindow(windowId: string): void {
    if (!this.windows.has(windowId)) return;

    // Hide current window
    if (this.activeWindowId) {
      const currentWindow = this.windows.get(this.activeWindowId);
      if (currentWindow) {
        const editorDiv = (currentWindow as unknown as { _editorDiv?: HTMLElement })._editorDiv;
        const messagesDiv = (currentWindow as unknown as { _messagesDiv?: HTMLElement })._messagesDiv;
        if (editorDiv) editorDiv.style.display = 'none';
        if (messagesDiv) messagesDiv.style.display = 'none';
      }
    }

    // Show new window
    this.activeWindowId = windowId;
    const newWindow = this.windows.get(windowId);
    if (newWindow) {
      const editorDiv = (newWindow as unknown as { _editorDiv?: HTMLElement })._editorDiv;
      const messagesDiv = (newWindow as unknown as { _messagesDiv?: HTMLElement })._messagesDiv;
      if (editorDiv) editorDiv.style.display = 'block';
      if (messagesDiv) messagesDiv.style.display = 'block';
      newWindow.focus();
    }

    this.updateTabs();
  }

  /**
   * Get the active window.
   */
  getActiveWindow(): EditorWindow | null {
    return this.activeWindowId ? this.windows.get(this.activeWindowId) ?? null : null;
  }

  /**
   * Get all windows in order.
   */
  getWindowsInOrder(): EditorWindow[] {
    return this.windowOrder
      .map(id => this.windows.get(id))
      .filter((w): w is EditorWindow => w !== undefined);
  }

  /**
   * Get window by ID.
   */
  getWindow(id: string): EditorWindow | null {
    return this.windows.get(id) ?? null;
  }

  /**
   * Check if there are open windows.
   */
  hasOpenWindows(): boolean {
    return this.windows.size > 0;
  }

  /**
   * Get example resources.
   */
  getExampleResources(): ExampleResource[] {
    return EXAMPLE_RESOURCES;
  }

  /**
   * Add a change listener.
   */
  addChangeListener(listener: WindowListChangeHandler): void {
    this.changeListeners.push(listener);
  }

  /**
   * Remove a change listener.
   */
  removeChangeListener(listener: WindowListChangeHandler): void {
    const index = this.changeListeners.indexOf(listener);
    if (index >= 0) {
      this.changeListeners.splice(index, 1);
    }
  }

  private notifyListeners(): void {
    this.changeListeners.forEach(listener => {
      try {
        listener();
      } catch {
        // Ignore errors
      }
    });
  }

  private generateWindowId(): string {
    return `w_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`;
  }

  /**
   * Update the tab bar.
   */
  private updateTabs(): void {
    if (!this.tabContainer) return;

    this.tabContainer.innerHTML = '';

    for (const id of this.windowOrder) {
      const window = this.windows.get(id);
      if (!window) continue;

      const tab = document.createElement('div');
      tab.className = 'editor-tab';
      if (id === this.activeWindowId) {
        tab.classList.add('active');
      }

      // Tab title
      const title = document.createElement('span');
      title.className = 'tab-title';
      title.textContent = window.getTitle();
      title.onclick = () => this.setActiveWindow(id);
      tab.appendChild(title);

      // Close button (not for the last window)
      if (this.windowOrder.length > 1 || window.getIsExample()) {
        const closeBtn = document.createElement('span');
        closeBtn.className = 'tab-close';
        closeBtn.textContent = '×';
        closeBtn.onclick = (e) => {
          e.stopPropagation();
          this.closeWindow(id);
        };
        tab.appendChild(closeBtn);
      }

      this.tabContainer.appendChild(tab);
    }

    // Add new tab button
    const newTabBtn = document.createElement('div');
    newTabBtn.className = 'editor-tab new-tab';
    newTabBtn.textContent = '+';
    newTabBtn.onclick = () => this.createNewWindow();
    this.tabContainer.appendChild(newTabBtn);
  }

  /**
   * Save window list to localStorage.
   */
  private saveWindowList(): void {
    try {
      const data = {
        windowOrder: this.windowOrder,
        activeWindowId: this.activeWindowId,
        nextWindowNumber: this.nextWindowNumber,
      };
      localStorage.setItem('nelumbo.windowManager', JSON.stringify(data));

      // Save each window's state
      this.windows.forEach(window => window.saveState());
    } catch {
      // Ignore storage errors
    }
  }

  /**
   * Load window list from localStorage.
   */
  private loadWindowList(): void {
    try {
      const stored = localStorage.getItem('nelumbo.windowManager');
      if (stored) {
        const data = JSON.parse(stored);
        this.nextWindowNumber = data.nextWindowNumber ?? 1;
        // Note: actual window restoration happens in restoreWindows()
      }
    } catch {
      // Ignore errors
    }
  }

  /**
   * Restore windows from localStorage.
   */
  restoreWindows(): void {
    try {
      const stored = localStorage.getItem('nelumbo.windowManager');
      if (!stored) return;

      const data = JSON.parse(stored);
      const savedOrder = data.windowOrder ?? [];

      for (const id of savedOrder) {
        const windowData = localStorage.getItem(`nelumbo.window.${id}`);
        if (windowData) {
          const state = JSON.parse(windowData);
          const window = this.createWindowInstance(
            state.id,
            state.title,
            state.isExample,
            state.examplePath
          );
          window.setContent(state.content);
          this.windows.set(id, window);
          this.windowOrder.push(id);
        }
      }

      // Set active window
      if (data.activeWindowId && this.windows.has(data.activeWindowId)) {
        this.setActiveWindow(data.activeWindowId);
      } else if (this.windowOrder.length > 0) {
        this.setActiveWindow(this.windowOrder[0]);
      }

      this.updateTabs();
    } catch {
      // Ignore errors - will create new window if needed
    }
  }

  /**
   * Save all windows.
   */
  saveAllWindows(): void {
    this.saveWindowList();
  }
}
