# Bug repros

Standalone `.nl` reproductions for confirmed findings of the 2026-09-07 multi-agent
code review of core + lsp. Each file demonstrates one bug against the CLI and is
red-by-design: while the bug exists the file fails (expectation mismatch or crash);
after the fix it passes and can be promoted into the regular test resources.

Run all: `./run-all.sh` (needs `./gradlew cliJar` first). Assertions are enabled
(`-ea`); `multiline-string-assert.nl` needs that to show its crash.

Every expectation (`? [..][..]`) states the CORRECT behavior, so the CLI reports a
mismatch (or crashes) today. A few queries in the quantifier/diagonal files carry no
expectation on purpose: their exact correct completeness is debatable, they only
illustrate the contradiction described in the header comment.

Not reproducible via the CLI (LSP-/editor-interactive): the WebSocket
System.exit bug (lsp Main.java:118), blank-document drops (DocumentSyncService),
stale diagnostics after didClose, the editor hang/refresh-loop findings, and the
concurrency findings. See the review report for those.
