// The edcore.main entry point (editor API + all editor contributions, minus the built-in language
// grammars) ships no .d.ts of its own; re-export the editor.api types so `import * as monaco from
// '.../edcore.main'` stays fully typed while pulling in the interactive contributions (go-to-definition
// on Cmd/Ctrl+Click, hover, completion, ...).
declare module 'monaco-editor/esm/vs/editor/edcore.main' {
    export * from 'monaco-editor/esm/vs/editor/editor.api';
}

// Monaco 0.34 gives inlay hints no CSS class of their own: their style is a generated rule that
// _fillInColors fills in, which nelumbo-fields.ts extends with a border.
declare module 'monaco-editor/esm/vs/editor/contrib/inlayHints/browser/inlayHintsController.js' {
    export class InlayHintsController {
        _fillInColors(props: Record<string, unknown>, hint: { kind?: number }): void;
    }
}

// Monaco 0.34's hover for an inlay hint; nelumbo-fields.ts switches it off (the server's hover at the end of
// the query replaces it).
declare module 'monaco-editor/esm/vs/editor/contrib/inlayHints/browser/inlayHintsHover.js' {
    export class InlayHintsHover {
        suggestHoverAnchor(mouseEvent: unknown): unknown;
    }
}
