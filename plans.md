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
