// The edcore.main entry point (editor API + all editor contributions, minus the built-in language
// grammars) ships no .d.ts of its own; re-export the editor.api types so `import * as monaco from
// '.../edcore.main'` stays fully typed while pulling in the interactive contributions (go-to-definition
// on Cmd/Ctrl+Click, hover, completion, ...).
declare module 'monaco-editor/esm/vs/editor/edcore.main' {
    export * from 'monaco-editor/esm/vs/editor/editor.api';
}
