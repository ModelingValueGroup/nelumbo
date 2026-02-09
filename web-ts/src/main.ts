/**
 * Nelumbo Web Editor - Main Entry Point
 */

import { Editor } from './editor';

// Export for use in the browser
declare global {
  interface Window {
    NelumboEditor: typeof Editor;
    nelumboEditor: Editor | null;
  }
}

// Expose Editor class to window
if (typeof window !== 'undefined') {
  window.NelumboEditor = Editor;
  window.nelumboEditor = null;
}

// Initialize the UI when DOM is ready
if (typeof document !== 'undefined') {
  document.addEventListener('DOMContentLoaded', initUI);
}

function initUI(): void {
  const container = document.getElementById('app');

  if (!container) {
    console.error('App container not found');
    return;
  }

  // Hide loading indicator
  const loading = document.getElementById('loading');
  if (loading) {
    loading.style.display = 'none';
  }

  // Check for dark mode preference
  const prefersDark = window.matchMedia('(prefers-color-scheme: dark)').matches;

  // Create editor
  const editor = new Editor({
    container,
    theme: prefersDark ? 'dark' : 'light',
  });

  // Store reference
  window.nelumboEditor = editor;

  console.log('Nelumbo Editor initialized');
}
