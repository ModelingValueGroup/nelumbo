# Plans

Open discussions and todo items, one section per topic. Remove a topic once it is decided and done.

## Discussions

### A better word for "falsehood"

Status: open (2026-11-10). Tom and Wim have not found the right English words yet.

The problem:
- A query result is `[facts][falsehoods]`: the bindings proven true and the bindings proven false. A binding in neither is unknown.
- "falsehood" also means "lie".
- The term is everywhere: 413 occurrences in 62 files (2026-11-10), including the JSON field `falsehoods` of CLI `--json`, `/eval` and MCP `eval_nl`. A rename changes those APIs.

What the word must do:
- Be a noun with a plural.
- Mean "proven false", not "unknown".
- Not suggest lying or deceit.
- Not suggest "sort of" (`-y`, `-ish`, `-oid`): JavaScript's `truthy`/`falsy` mean "counts as true/false".
- Preferably pair with "fact", which is also the keyword `fact`.

Earlier proposal: "negative fact", keeping "fact" for the true side.

German and Dutch (searched 2026-11-10):
- No better noun. The native words have the same "lie" problem: "Unwahrheit", "Falschheit" (deceitfulness), "onwaarheid", "valsheid" (forgery).
- Coinages like "Untatsache", "Nichttatsache", "onfeit", "niet-feit" are not in use.
- Wittgenstein, Tractatus 2.06: "positive Tatsache" / "negative Tatsache" (positive / negative fact). German and Belgian law use "negative Tatsache" / "negatief feit" as well.
- In English, Russell (1918), Reiter's closed-world paper (1978) and OWL 2 ("negative property assertion") use "negative fact".

Technical pairs that match Nelumbo's structure (jargon):
- extension / anti-extension (Priest): in neither is a gap (unknown), in both a glut (inconsistency).
- verifiers / falsifiers (Kit Fine): negation swaps them, as `!` swaps facts and falsehoods.
- told true / told false (Belnap's four-valued database logic): adjectives, no nouns.

Coined words (no dictionary entry found, 2026-11-10):

| Pair | Reads as | Catch |
|---|---|---|
| fact / disfact | disprove, disproof | "dis" is also slang for an insult |
| fact / falsifact | false + fact | "falsify" also means forge; long |
| verifact / falsifact | verify / falsify | Verifact is a brand of fact-checking apps |
| proven / disproven (as nouns) | proven true / proven false | odd as nouns; stresses proven vs unknown |
| trues / falses | plain plurals | informal |

Claude's pick: "disfact".

Taken or misleading, do not use:
- unfact: Merriam-Webster "a deliberate falsehood made to pass as fact"
- anti-fact / antifact: Wiktionary synonym of "lie"; looks like "artifact"
- misfact: wrong information
- falsies: padding, false eyelashes
- counterfact: suggests "counterfactual" (what if)
- contrafact: jazz term
- refact: reads as "refactor"
- non-fact: reads as "not known"

## Todo

### Review the tutorial: section order and concept coverage

Status: todo (2026-11-12).

Read:
- The 31 sections of `website/src/main/resources/public/tutorial.html`, with their texts.
- Their `.nl` files in `src/main/resources/org/modelingvalue/nelumbo/tutorial/<section>/`.

Check:
- [ ] Is the order of the sections logical?
- [ ] Does each text explain exactly the language concepts its code uses, no more and no less?

Known (2026-11-12): `E[...]` is used before it is explained.
- `#cycles` (section 14) uses `E[f](...)` in its example, exercise and solution, without a word about it.
- `#relations` (17) only glosses it: "`E[x]` (there exists) and `A[y]` (for all)".
- `#quantifiers` (18) is where it is explained.
- Fix: reshuffle the sections, or explain `E[...]` in the opening text of `#cycles`.

### Encourage reuse

Status: todo (2026-11-12). Find a way to encourage reuse of Nelumbo models.

Ideas:
- [ ] A marketplace.
- [ ] Libraries beyond the stdlib.
- [ ] Real-world examples and deployments.

Already there (CLAUDE.md):
- `library.html`: the stdlib modules only (`Library.MODULES`).
- `sandbox.html`: the cli examples.
- Shared solutions: by name, kept 7 days, no public list (admins see them, with the sharer's IP, on `/admin`).
- `import` resolves from the jar (CLI) or the stdlib: there is no way yet to import a module from elsewhere.

### Stats do not show timed-out evaluations

Status: todo (2026-11-12). During the workshop the status page showed no "Stopped (busy)" evaluations, while people saw the timeout message in the editor.

Found (code read, 2026-11-12):
- "Stopped (busy)" is `EvalGate.overloaded`, raised by `gate.recordOverload()` in `QueryResultCache` only when the evaluation ran under the short overload budget (`overloadBudget`).
- A timeout at the normal workspace deadline gives an ERROR result and is counted nowhere.

To do:
- [ ] Count timeouts at the workspace deadline too (a counter of its own next to `overloaded`), in `/stats`, the history and the status charts.
- [ ] Check the text the editor shows for each of the two cases.

### AI on the website

Status: todo (2026-11-12). Find a way to use AI on nelumbo.nl.

Ideas:
- [ ] Ask questions in the tutorial.
- [ ] Hints about what the user is doing in an editor.
- [ ] Generate Nelumbo from a description in plain language.

Already there:
- The MCP server (`mcp/`): `eval_nl`, `search_docs`, `get_example`, `new_model`.
- `llms.txt` at the site root.

Decide:
- [ ] Which model or service, and who pays per request.
- [ ] Editor texts would go to an outside service: what visitors are told.
- [ ] Limits per visitor, as the shares have (public site).

### Progress, stop and parallel queries while evaluating

Status: todo (2026-11-12).

Now the editor shows feedback only after the whole document has been evaluated.

Wanted:
- [ ] A busy indicator while an expression is being evaluated.
- [ ] A stop button.
- [ ] Per query: done (with its result), being evaluated, or waiting.
- [ ] Evaluate `?` statements in parallel.

Current state (CLAUDE.md):
- Until an edit's re-evaluation finishes, `QueryResultCache` serves only the hints on unchanged lines.
- Evaluation stops at the workspace deadline or the overload budget; there is no user stop.
- A fact or rule that fails stops the evaluation: the queries after it get no hint.

Find out:
- [ ] Which editors: the website only, or every LSP client?
- [ ] Parallel queries: a result must not depend on an earlier query. That was once a bug (fixed 2026-10-27, `tests/solve-depends-on-earlier-query.nl`); running queries in parallel tests it harder.
- [ ] Parallel queries share the one inference pool (`KnowledgeBase.POOL`) and the overload threshold.

### Wrong highlighting now and then

Status: todo (2026-11-12).

Sometimes the highlighting is wrong. Clearing the whole editor and pasting the text back often fixes it.

Find out:
- [ ] In which editor (website, IntelliJ, Eclipse, VS Code, Swing editor)?
- [ ] A text and a sequence of edits that reproduce it.

### Full Unicode in names, operators and patterns

Status: todo (2026-11-10). Allow the full Unicode character set wherever it is useful.

Current state (CLI probe, 2026-11-10):
- `TokenType`: `NAME` is `[a-zA-Z_][0-9a-zA-Z_]*`, `OPERATOR` is `(?!//)[~!@#$%^&*=+|:<>.?/-]+`. Any other character becomes an `ERROR` token.
- Patterns use the same tokenizer: `Person ::= Zoë` gives "Unexpected token 'ë'", `<Person> ♥ <Person>` gives "Unexpected token '♥'".
- Strings already accept any character.

Change:
- [ ] `src/main/java/org/modelingvalue/nelumbo/syntax/TokenType.java`: `NAME`, `OPERATOR`
- [ ] `lang/lang.nl` and `docs/reference/lang/index.md`: the token regexes in the comments and the table
- [ ] Tests for names, operators and pattern words with non-ASCII characters

Decide:
- [ ] Names: Unicode letters, digits and marks (`\p{L}`, `\p{N}`, `\p{M}`), following Unicode's identifier rules (UAX #31)?
- [ ] Operators: math symbols (`\p{Sm}`: `≤ ≠ ∧ ∨ ¬ ∀ ∃ →`) and other symbols (`\p{So}`: `♥`, emoji)? `\p{Sm}` also contains `+ < = > | ~`.
- [ ] Normalization: `ë` is one code point or `e` plus a combining mark. Without NFC before tokenizing, two names that look the same differ.
- [ ] Lookalikes: Cyrillic `а` vs Latin `a`. Accept, or warn about mixed scripts?
- [ ] Digits: digits of other scripts in `NUMBER`, or ASCII only?

Check:
- [ ] Positions: Java and LSP count UTF-16 units, and a character outside the BMP (most emoji) takes two. Check CLI `file:line:col`, LSP ranges, semantic tokens and inlay hints.
- [ ] Every reader and writer uses UTF-8: CLI files and stdin, LSP, website `/eval`, editor, `PreferenceText`, the docs highlighter.
