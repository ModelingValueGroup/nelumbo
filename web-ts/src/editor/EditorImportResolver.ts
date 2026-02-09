/**
 * EditorImportResolver - Cross-window import resolution for the Nelumbo editor.
 * Enables editors to import content from other editor windows.
 */

import type { EditorWindow } from './EditorWindow';
import type { WindowManager } from './WindowManager';

/**
 * Import change listener.
 */
export type ImportChangeListener = (importName: string) => void;

/**
 * EditorImportResolver - resolves imports between editor windows.
 */
export class EditorImportResolver {
  private windowManager: WindowManager | null = null;
  private dependencies: Map<string, Set<EditorWindow>> = new Map();
  private listeners: Map<string, Set<ImportChangeListener>> = new Map();

  /**
   * Set the window manager.
   */
  setWindowManager(windowManager: WindowManager): void {
    this.windowManager = windowManager;
  }

  /**
   * Get the import name for a window number.
   */
  static getImportName(windowNumber: number): string {
    return `editor.nelumbo_${windowNumber}`;
  }

  /**
   * Parse an import name to get the window number.
   * Returns -1 if not a valid editor import.
   */
  static parseImportName(importName: string): number {
    if (importName.startsWith('editor.nelumbo_')) {
      const numberStr = importName.substring('editor.nelumbo_'.length);
      const num = parseInt(numberStr, 10);
      if (!isNaN(num)) {
        return num;
      }
    }
    return -1;
  }

  /**
   * Resolve an import name to content.
   * Returns null if the import cannot be resolved.
   */
  resolve(importName: string): string | null {
    if (!this.windowManager) return null;

    // Check for editor import
    const windowNumber = EditorImportResolver.parseImportName(importName);
    if (windowNumber > 0) {
      // Find window with this number
      for (const window of this.windowManager.getWindowsInOrder()) {
        if (window.getFileName().includes(`_${windowNumber}`)) {
          return window.getContent();
        }
      }
    }

    // Check for library/example imports
    const resources = this.windowManager.getExampleResources();
    for (const resource of resources) {
      if (resource.displayName === importName) {
        // Return null and let the caller handle fetching
        return null;
      }
    }

    return null;
  }

  /**
   * Add a dependency - window depends on importName.
   */
  addDependency(importName: string, window: EditorWindow): void {
    if (!this.dependencies.has(importName)) {
      this.dependencies.set(importName, new Set());
    }
    this.dependencies.get(importName)!.add(window);
  }

  /**
   * Remove a dependency.
   */
  removeDependency(importName: string, window: EditorWindow): void {
    const deps = this.dependencies.get(importName);
    if (deps) {
      deps.delete(window);
      if (deps.size === 0) {
        this.dependencies.delete(importName);
      }
    }
  }

  /**
   * Remove all dependencies for a window.
   */
  removeAllDependencies(window: EditorWindow): void {
    this.dependencies.forEach((deps, _importName) => {
      deps.delete(window);
    });

    // Clean up empty sets
    const emptyKeys: string[] = [];
    this.dependencies.forEach((deps, importName) => {
      if (deps.size === 0) {
        emptyKeys.push(importName);
      }
    });
    emptyKeys.forEach(key => this.dependencies.delete(key));
  }

  /**
   * Notify that an import has changed.
   */
  notifyImportChanged(importName: string): void {
    // Notify dependent windows
    const deps = this.dependencies.get(importName);
    if (deps) {
      deps.forEach(window => {
        // Trigger refresh on the window
        window.refresh();
      });
    }

    // Notify listeners
    const listeners = this.listeners.get(importName);
    if (listeners) {
      listeners.forEach(listener => {
        try {
          listener(importName);
        } catch {
          // Ignore listener errors
        }
      });
    }
  }

  /**
   * Add a listener for import changes.
   */
  addImportChangeListener(importName: string, listener: ImportChangeListener): void {
    if (!this.listeners.has(importName)) {
      this.listeners.set(importName, new Set());
    }
    this.listeners.get(importName)!.add(listener);
  }

  /**
   * Remove a listener.
   */
  removeImportChangeListener(importName: string, listener: ImportChangeListener): void {
    const listeners = this.listeners.get(importName);
    if (listeners) {
      listeners.delete(listener);
      if (listeners.size === 0) {
        this.listeners.delete(importName);
      }
    }
  }

  /**
   * Get all windows that depend on an import.
   */
  getDependents(importName: string): EditorWindow[] {
    const deps = this.dependencies.get(importName);
    return deps ? Array.from(deps) : [];
  }

  /**
   * Check if an import can be resolved.
   */
  canResolve(importName: string): boolean {
    if (!this.windowManager) return false;

    // Check editor imports
    const windowNumber = EditorImportResolver.parseImportName(importName);
    if (windowNumber > 0) {
      for (const window of this.windowManager.getWindowsInOrder()) {
        if (window.getFileName().includes(`_${windowNumber}`)) {
          return true;
        }
      }
    }

    // Check library/example imports
    const resources = this.windowManager.getExampleResources();
    for (const resource of resources) {
      if (resource.displayName === importName) {
        return true;
      }
    }

    return false;
  }
}

// Global instance
export const editorImportResolver = new EditorImportResolver();
