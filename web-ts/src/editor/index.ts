/**
 * Nelumbo Editor - Main exports for the editor module.
 */

export { Editor } from './Editor';
export type { EditorConfig } from './Editor';

export { EditorWindow } from './EditorWindow';
export type { EditorWindowState, EditorChangeHandler } from './EditorWindow';

export { WindowManager } from './WindowManager';
export type { WindowListChangeHandler, ExampleResource } from './WindowManager';

export { EditorTheme } from './EditorTheme';
export type { ColorScheme } from './EditorTheme';

export { SyntaxHighlighter } from './SyntaxHighlighter';
export type { HighlightSpan } from './SyntaxHighlighter';

export { MessagePane } from './MessagePane';
export type { Message, MessageType, HighlightInfo } from './MessagePane';

export { TreeViewer, showTreeViewerDialog } from './TreeViewer';

export { KnowledgeBaseViewer, showKnowledgeBaseViewerDialog } from './KnowledgeBaseViewer';

export { EditorImportResolver, editorImportResolver } from './EditorImportResolver';
export type { ImportChangeListener } from './EditorImportResolver';
