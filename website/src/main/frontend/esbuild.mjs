import { build } from 'esbuild';

// monaco-languageclient v1 bundles vscode-languageclient, which imports the node "vscode" module.
// Its own vscode-compatibility shim re-exports a browser-safe surface, so alias "vscode" onto it.
const options = {
    entryPoints: ['src/nelumbo-fields.ts'],
    bundle:      true,
    outdir:      'dist',
    format:      'iife',
    globalName:  'NelumboFields',
    sourcemap:   true,
    minify:      true,
    alias:       { vscode: 'monaco-languageclient/vscode-compatibility' },
    loader:      { '.ttf': 'file', '.css': 'css' },
    logLevel:    'info'
};

await build(options);
