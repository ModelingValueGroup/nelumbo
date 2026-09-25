# Sudoku CSP solver (Norvig) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a third sudoku solver `examples/sudoku-4x4-csp.nl` (then a 9x9 copy, then a grid DSL) that implements Peter Norvig's candidate-set constraint-propagation + search algorithm faithfully, to surface Nelumbo engine bugs.

**Architecture:** Each cell holds a `Set<Digit>` of remaining options (`Digit` is an enum type, not an integer). Norvig's `assign`/`eliminate` propagate naked + hidden singles; `search` does DFS with the MRV heuristic. Everything is functional: the candidate grid `List<List<Set<Digit>>>` is threaded through recursion, contradictions are relational failures. Idioms are copied from the proven-to-parse `examples/sudoku-4x4-smart.nl`.

**Tech Stack:** Nelumbo `.nl`, stdlib `nelumbo.collections` (sets: `-` `||` `|s|` `in`) + `nelumbo.integers` (`+ - / < >` for coordinates). Verified via the CLI jar, not JUnit.

---

## Execution protocol (READ FIRST - this is a bug-finding exercise)

- **The goal is to hit engine bugs, not to dodge them.** When a faithful rule crashes, goes undecided, or gives nondeterministic results, STOP. Do NOT reshape it into the "safe" rule shape. Capture a minimal red-by-design repro in `bug-repros/`, add a line under a `## 2026-09-11 (sudoku-csp)` section of `bug-repros/README.md`, mark the task BLOCKED with the repro filename, and continue to the next independent task. Report blocked tasks to Tom (for Wim).
- Only adjust target code for a **genuine syntax** reason (e.g. a real parse rule), never to work around a defect. If unsure whether something is a bug or a syntax mistake, treat it as a bug and capture the repro.
- **"Tests" are Nelumbo inline query expectations.** Each task adds a probe query `... ? [expected][..]` to the file, runs the CLI, and greps the output. TDD order: add the probe first (it fails: undefined functor / mismatch / crash), then add the rules, then the probe passes.
- **CLI command (all tasks):**
  ```sh
  ./gradlew :cli:cliJar -q
  java -DPARALLEL_COLLECTIONS=false -jar cli/build/libs/nelumbo-cli-*.jar examples/sudoku-4x4-csp.nl 2>&1 | tee /tmp/csp.out
  grep -iE 'unexpected|exception|inconsistent|expected result' /tmp/csp.out
  ```
  A PASS = the CLI exits 0 and the grep finds nothing. Run the solver query (Task 12) TWICE - results are nondeterministic run-to-run.
- **Git:** work on a feature branch `sudoku-csp` off `develop`; commit locally per task. Tom merges/pushes/publishes (do not push).

Setup once:
```sh
git checkout develop && git checkout -b sudoku-csp
```

## File Structure

- Create: `examples/sudoku-4x4-csp.nl` - phase 1, the full 4x4 solver. One file (mirrors how the other sudoku examples are single-file).
- Create: `examples/sudoku-9x9-csp.nl` - phase 2, line-for-line 9x9 rescale.
- Modify: `bug-repros/README.md` + new `bug-repros/*.nl` - only when a bug is hit.
- Phase 3 (DSL) edits both example files.

Probe puzzle used throughout phase 1 (the "real" 4x4 puzzle, unique solution):
`P0 = [[1,0,0,0],[0,0,1,0],[0,3,0,0],[0,0,0,4]]` -> `[[1,2,4,3],[3,4,1,2],[4,3,2,1],[2,1,3,4]]`.

---

# Phase 1 - `examples/sudoku-4x4-csp.nl`

### Task 1: Skeleton, `Digit` enum, integer<->digit map

**Files:** Create `examples/sudoku-4x4-csp.nl`

- [ ] **Step 1: Write the failing probe.** Create the file with just:

```
import nelumbo.collections

Digit :: Object
Digit ::= D1, D2, D3, D4

dig(2)=d   ? [(d=D2)][..]
undig(D3)=v ? [(v=3)][..]
```

- [ ] **Step 2: Run CLI, verify it fails** (`dig`/`undig` undefined).

- [ ] **Step 3: Add the IO map and variable decls** (put decls once, near the top; extend as later tasks need):

```
Digit   ::= dig(<Integer>)
Integer ::= undig(<Digit>)

Integer                i, j, k, m, r, c, r2, c2, v
Digit                  d
Set<Digit>             sc, ns, sy
Set<Integer>           pl
List<Integer>          il
List<List<Integer>>    p
List<Set<Digit>>       rw, rw2
List<List<Set<Digit>>> g, g2, g3, s, pre, suf

dig(i)=d   <=> d=D1 if i=1, d=D2 if i=2, d=D3 if i=3, d=D4 if i=4
undig(d)=v <=> v=1 if d=D1, v=2 if d=D2, v=3 if d=D3, v=4 if d=D4
```

- [ ] **Step 4: Run CLI, verify both probes PASS** (grep clean, exit 0).

- [ ] **Step 5: Commit**

```sh
git add examples/sudoku-4x4-csp.nl
git commit -m "feat(sudoku-csp): digit enum + integer<->digit map"
```

---

### Task 2: Integer-puzzle accessors (input side)

**Files:** Modify `examples/sudoku-4x4-csp.nl`

- [ ] **Step 1: Write the failing probe** (append):

```
icell([[1,0,0,0],[0,0,1,0],[0,3,0,0],[0,0,0,4]],2,1)=v ? [(v=3)][..]
```

- [ ] **Step 2: Run CLI, verify it fails** (`icell` undefined).

- [ ] **Step 3: Add accessors** (bounds-guarded, because the engine probes guards speculatively and an out-of-range `pos` crashes):

```
Integer       ::= iat(<List<Integer>>,<Integer>),
                  icell(<List<List<Integer>>>,<Integer>,<Integer>)
List<Integer> ::= irow(<List<List<Integer>>>,<Integer>)

iat(il,i)=k    <=> k pos il = i  if i>=0 & i<4
irow(p,r)=il   <=> il pos p = r  if r>=0 & r<4
icell(p,r,c)=k <=> k = iat(irow(p,r),c)
```

- [ ] **Step 4: Run CLI, verify PASS.**

- [ ] **Step 5: Commit** `feat(sudoku-csp): integer puzzle accessors`

---

### Task 3: Candidate-grid accessors + `fullGrid`

**Files:** Modify `examples/sudoku-4x4-csp.nl`

- [ ] **Step 1: Write the failing probe** (append):

```
cell(fullGrid([[0,0,0,0],[0,0,0,0],[0,0,0,0],[0,0,0,0]]),1,2)=sc ? [(sc={D1,D2,D3,D4})][..]
```

- [ ] **Step 2: Run CLI, verify it fails** (`cell`/`fullGrid` undefined).

- [ ] **Step 3: Add accessors + the all-full starting grid** (`fullGrid` takes the puzzle only to avoid a nullary functor; it ignores the values and returns the 4x4 grid of full candidate sets):

```
Set<Digit>             ::= catRow(<List<Set<Digit>>>,<Integer>),
                           cell(<List<List<Set<Digit>>>>,<Integer>,<Integer>)
List<Set<Digit>>       ::= crow(<List<List<Set<Digit>>>>,<Integer>)
List<List<Set<Digit>>> ::= fullGrid(<List<List<Integer>>>)

catRow(rw,c)=sc <=> sc pos rw = c if c>=0 & c<4
crow(g,r)=rw    <=> rw pos g = r  if r>=0 & r<4
cell(g,r,c)=sc  <=> sc = catRow(crow(g,r),c)

fullGrid(p)=g <=> g=[[{D1,D2,D3,D4},{D1,D2,D3,D4},{D1,D2,D3,D4},{D1,D2,D3,D4}],
                     [{D1,D2,D3,D4},{D1,D2,D3,D4},{D1,D2,D3,D4},{D1,D2,D3,D4}],
                     [{D1,D2,D3,D4},{D1,D2,D3,D4},{D1,D2,D3,D4},{D1,D2,D3,D4}],
                     [{D1,D2,D3,D4},{D1,D2,D3,D4},{D1,D2,D3,D4},{D1,D2,D3,D4}]]
```

- [ ] **Step 4: Run CLI, verify PASS.**

- [ ] **Step 5: Commit** `feat(sudoku-csp): candidate-grid accessors + full grid`

---

### Task 4: Functional grid update (`put`)

**Files:** Modify `examples/sudoku-4x4-csp.nl`

> **BLOCKED (2026-09-24, second time)** on `bugs/rule-pos-on-mapped-collection-list-undecided.nl`:
> the rules below are in the file and `put` itself decides, but every `cell(...)` read
> on a `putRow`-built row is undecided - a user rule wrapping `pos` over a `map`-produced
> list of collections. Side finding while isolating: `bugs/set-literal-arithmetic-unevaluated.nl`.
> The first block (2026-09-11, the bba88fc8 reduction trio) was lifted on 2026-09-24.
> The two probes are kept in the file, commented out.
>
> **UNBLOCKED (2026-09-25)**: the engine fix landed, the repro moved to
> `tests/rule-pos-on-mapped-collection-list-undecided.nl` (RegressionTest), and both
> probes are live in the file again and pass on the CLI. `set-literal-arithmetic-unevaluated`
> is still open. Continue with Task 5.

- [ ] **Step 1: Write the failing probe** (append): put `{D2}` at (0,0), read it back, and confirm a neighbour is untouched:

```
cell(put(fullGrid([[0,0,0,0],[0,0,0,0],[0,0,0,0],[0,0,0,0]]),0,0,{D2}),0,0)=sc ? [(sc={D2})][..]
cell(put(fullGrid([[0,0,0,0],[0,0,0,0],[0,0,0,0],[0,0,0,0]]),0,0,{D2}),0,1)=sc ? [(sc={D1,D2,D3,D4})][..]
```

- [ ] **Step 2: Run CLI, verify it fails** (`put` undefined).

- [ ] **Step 3: Add the functional update** (identical shape to `sudoku-4x4-smart.nl`, but `Set<Digit>`-valued):

```
Set<Digit>             ::= sel(<Integer>,<Integer>,<Set<Digit>>,<Set<Digit>>)
List<Set<Digit>>       ::= putRow(<List<Set<Digit>>>,<Integer>,<Set<Digit>>)
List<List<Set<Digit>>> ::= rowsBefore(<List<List<Set<Digit>>>>,<Integer>),
                           rowsFrom(<List<List<Set<Digit>>>>,<Integer>),
                           put(<List<List<Set<Digit>>>>,<Integer>,<Integer>,<Set<Digit>>)

sel(j,i,ns,sc)=sy   <=> sy=ns if j=i,  sy=sc if j!=i
putRow(rw,i,ns)=rw2 <=> rw2 = [0,1,2,3] map [j](sel(j,i,ns,catRow(rw,j)))
rowsBefore(g,r)=s   <=> s=[]                                                      if r=0,
                        E[rw,pre](crow(g,r-1)=rw & rowsBefore(g,r-1)=pre & s=pre+[rw]) if r>0
rowsFrom(g,r)=s     <=> s=[]                                                      if r=4,
                        E[rw,suf](crow(g,r)=rw & rowsFrom(g,r+1)=suf & s=[rw]+suf) if r<4
put(g,r,c,ns)=s     <=> E[pre,suf](rowsBefore(g,r)=pre & rowsFrom(g,r+1)=suf &
                                   E[rw,rw2](crow(g,r)=rw & putRow(rw,c,ns)=rw2 & s=pre+[rw2]+suf))
```

- [ ] **Step 4: Run CLI, verify both probes PASS.**

- [ ] **Step 5: Commit** `feat(sudoku-csp): functional candidate-grid update`

---

### Task 5: `peer` / `samebox` predicates

**Files:** Modify `examples/sudoku-4x4-csp.nl`

- [ ] **Step 1: Write the failing probe** (append): (0,0) and (1,1) share a box; (0,0) and (2,2) do not; a cell is not its own peer:

```
peer(0,0,1,1) ? [()][]
peer(0,0,2,2) ? [][()]
peer(0,0,0,0) ? [][()]
```

- [ ] **Step 2: Run CLI, verify it fails** (`peer` undefined).

- [ ] **Step 3: Add box base + peer test:**

```
Integer ::= bb(<Integer>)
Boolean ::= samebox(<Integer>,<Integer>,<Integer>,<Integer>),
            peer(<Integer>,<Integer>,<Integer>,<Integer>)

bb(r)=k <=> k=0 if r<2,  k=2 if r>=2
samebox(r,c,r2,c2) <=> E[i,j](bb(r)=i & bb(c)=j & bb(r2)=i & bb(c2)=j)
peer(r,c,r2,c2)    <=> !(r=r2 & c=c2) & (r=r2 | c=c2 | samebox(r,c,r2,c2))
```

- [ ] **Step 4: Run CLI, verify all three PASS.**

- [ ] **Step 5: Commit** `feat(sudoku-csp): peer/samebox predicates`

---

### Task 6: Bare `eliminate` (remove + contradiction), no propagation yet

**Files:** Modify `examples/sudoku-4x4-csp.nl`

This is the core mechanic in isolation: remove `d`; if the cell empties, fail. Naked/hidden propagation is added in Tasks 7-8. **Expect the mutual-recursion / nested-generic machinery to strain here - a prime bug site.**

- [ ] **Step 1: Write the failing probe** (append): eliminating `D1` from a full cell leaves the other three; eliminating a value not present is a no-op; eliminating the last value fails (empty facts side):

```
cell(elim(fullGrid([[0,0,0,0],[0,0,0,0],[0,0,0,0],[0,0,0,0]]),0,0,D1),0,0)=sc ? [(sc={D2,D3,D4})][..]
cell(elim(put(fullGrid([[0,0,0,0],[0,0,0,0],[0,0,0,0],[0,0,0,0]]),0,0,{D2}),0,0,D1),0,0)=sc ? [(sc={D2})][..]
elim(put(fullGrid([[0,0,0,0],[0,0,0,0],[0,0,0,0],[0,0,0,0]]),0,0,{D2}),0,0,D2)=g ? [][..]
```

- [ ] **Step 2: Run CLI, verify it fails** (`elim`/`afterElim` undefined). In this task, temporarily define `afterElim` as identity so `elim`'s core is testable alone:

- [ ] **Step 3: Add bare eliminate** (the `|ns|=0` case has no rule branch -> relational failure = Norvig's `return False`):

```
List<List<Set<Digit>>> ::= elim(<List<List<Set<Digit>>>>,<Integer>,<Integer>,<Digit>),
                           afterElim(<List<List<Set<Digit>>>>,<Integer>,<Integer>,<Digit>,<Set<Digit>>)

elim(g,r,c,d)=s <=> s=g                                              if !(d in cell(g,r,c)),
                    E[ns,g2](cell(g,r,c)-{d}=ns & put(g,r,c,ns)=g2 &
                             afterElim(g2,r,c,d,ns)=s)               if d in cell(g,r,c)

// TEMP (replaced in Tasks 7-8): identity except the empty-set contradiction
afterElim(g,r,c,d,ns)=s <=> s=g if |ns|>=1
```

- [ ] **Step 4: Run CLI, verify all three probes PASS** (the third has an empty facts side).

- [ ] **Step 5: Commit** `feat(sudoku-csp): bare eliminate with contradiction`

---

### Task 7: `assign` + naked-single propagation

**Files:** Modify `examples/sudoku-4x4-csp.nl`

`assign(g,r,c,d)` eliminates every other candidate from the cell (which, via naked single, cascades to peers). Naked single lives in `afterElim`: when a cell drops to one value, eliminate that value from all peers.

- [ ] **Step 1: Write the failing probe** (append): assign `D1` at (0,0); the cell becomes `{D1}` and peer (0,1) loses `D1`:

```
cell(assign(fullGrid([[0,0,0,0],[0,0,0,0],[0,0,0,0],[0,0,0,0]]),0,0,D1),0,0)=sc ? [(sc={D1})][..]
cell(assign(fullGrid([[0,0,0,0],[0,0,0,0],[0,0,0,0],[0,0,0,0]]),0,0,D1),0,1)=sc ? [(sc={D2,D3,D4})][..]
cell(assign(fullGrid([[0,0,0,0],[0,0,0,0],[0,0,0,0],[0,0,0,0]]),0,0,D1),1,1)=sc ? [(sc={D2,D3,D4})][..]
```

- [ ] **Step 2: Run CLI, verify it fails** (`assign`/`elimPeers` undefined; naked single not wired).

- [ ] **Step 3: Add assign, peer elimination, and the real naked-single branch of `afterElim`:**

```
List<List<Set<Digit>>> ::= assign(<List<List<Set<Digit>>>>,<Integer>,<Integer>,<Digit>),
                           assignOthers(<List<List<Set<Digit>>>>,<Integer>,<Integer>,<Digit>,<Integer>),
                           nakedSingle(<List<List<Set<Digit>>>>,<Integer>,<Integer>),
                           elimPeers(<List<List<Set<Digit>>>>,<Integer>,<Integer>,<Digit>),
                           elimPeersR(<List<List<Set<Digit>>>>,<Integer>,<Integer>,<Digit>,<Integer>),
                           elimPeersC(<List<List<Set<Digit>>>>,<Integer>,<Integer>,<Digit>,<Integer>,<Integer>)

// assign d at (r,c) = eliminate every OTHER digit index e (1..4) from the cell
assign(g,r,c,d)=s <=> s=assignOthers(g,r,c,d,1)
assignOthers(g,r,c,d,i)=s <=>
    s=g                                                       if i=5,
    s=assignOthers(g,r,c,d,i+1)                               if i<5 & dig(i)=d,
    E[g2](elim(g,r,c,dig(i))=g2 & s=assignOthers(g2,r,c,d,i+1)) if i<5 & dig(i)!=d

// naked single: cell is a singleton {d} -> remove d from all peers
nakedSingle(g,r,c)=s <=> E[d](d in cell(g,r,c) & elimPeers(g,r,c,d)=s)

elimPeers(g,r,c,d)=s      <=> s=elimPeersR(g,r,c,d,0)
elimPeersR(g,r,c,d,r2)=s  <=> s=g if r2=4,
                              E[g2](elimPeersC(g,r,c,d,r2,0)=g2 & s=elimPeersR(g2,r,c,d,r2+1)) if r2<4
elimPeersC(g,r,c,d,r2,c2)=s <=> s=g                             if c2=4,
                                s=elimPeersC(g,r,c,d,r2,c2+1)   if c2<4 & !peer(r,c,r2,c2),
                                E[g2](elim(g,r2,c2,d)=g2 & s=elimPeersC(g2,r,c,d,r2,c2+1)) if c2<4 & peer(r,c,r2,c2)
```

Replace the TEMP `afterElim` from Task 6 with the naked-single version (hidden single added in Task 8):

```
afterElim(g,r,c,d,ns)=s <=> nakedSingle(g,r,c)=s if |ns|=1,
                            s=g                  if |ns|>1
```

- [ ] **Step 4: Run CLI, verify all three probes PASS.** Note: assign->elim->afterElim->nakedSingle->elimPeers->elim is deep mutual recursion; if it crashes/undecides, capture a repro (Bug protocol) and mark BLOCKED.

- [ ] **Step 5: Commit** `feat(sudoku-csp): assign + naked-single propagation`

---

### Task 8: Hidden-single propagation

**Files:** Modify `examples/sudoku-4x4-csp.nl`

Hidden single: after eliminating `d` from a cell, in each unit that cell belongs to, if `d` now has exactly one possible place, assign it there; if zero places, contradiction. `rowFree`/`colFree`/`boxFree` build the set of free positions without set-builder (recursion + union), to avoid unbounded enumeration.

- [ ] **Step 1: Write the failing probe** (append): construct a row where `D1` fits in only one column, force elimination, and confirm the hidden single is placed. Minimal check - `rowFree` of a full grid's row for `D1` is all four columns:

```
rowFree(fullGrid([[0,0,0,0],[0,0,0,0],[0,0,0,0],[0,0,0,0]]),0,D1,0)=pl ? [(pl={0,1,2,3})][..]
```

- [ ] **Step 2: Run CLI, verify it fails** (`rowFree` undefined).

- [ ] **Step 3: Add the free-place builders, per-unit hidden single, and the full `afterElim`:**

```
Set<Integer>           ::= rowFree(<List<List<Set<Digit>>>>,<Integer>,<Digit>,<Integer>),
                           colFree(<List<List<Set<Digit>>>>,<Integer>,<Digit>,<Integer>),
                           boxFree(<List<List<Set<Digit>>>>,<Integer>,<Integer>,<Digit>,<Integer>)
List<List<Set<Digit>>> ::= hidden(<List<List<Set<Digit>>>>,<Integer>,<Integer>,<Digit>),
                           hRow(<List<List<Set<Digit>>>>,<Integer>,<Digit>,<Set<Integer>>),
                           hCol(<List<List<Set<Digit>>>>,<Integer>,<Digit>,<Set<Integer>>),
                           hBox(<List<List<Set<Digit>>>>,<Integer>,<Integer>,<Digit>,<Set<Integer>>)

// columns of row r where d is still a candidate
rowFree(g,r,d,c)=pl <=> pl={} if c=4,
                        E[pl2](rowFree(g,r,d,c+1)=pl2 & pl=pl2||{c}) if c<4 & d in cell(g,r,c),
                        pl=rowFree(g,r,d,c+1)                        if c<4 & !(d in cell(g,r,c))
// rows of column c where d is still a candidate
colFree(g,c,d,r)=pl <=> pl={} if r=4,
                        E[pl2](colFree(g,c,d,r+1)=pl2 & pl=pl2||{r}) if r<4 & d in cell(g,r,c),
                        pl=colFree(g,c,d,r+1)                        if r<4 & !(d in cell(g,r,c))
// box cells (index m=0..3 -> row bb(r)+m/2, col bb(c)+m%2) where d is a candidate
boxFree(g,r,c,d,m)=pl <=> pl={} if m=4,
                          E[pl2](boxFree(g,r,c,d,m+1)=pl2 & pl=pl2||{m})
                              if m<4 & E[i,j](bb(r)=i & bb(c)=j & d in cell(g,i+m/2,j+(m-(m/2)*2))),
                          pl=boxFree(g,r,c,d,m+1)
                              if m<4 & !E[i,j](bb(r)=i & bb(c)=j & d in cell(g,i+m/2,j+(m-(m/2)*2)))

// place d in a unit if it has exactly one free cell; 0 free -> fail (contradiction)
hRow(g,r,d,pl)=s <=> s=g if |pl|>1,  E[c2](c2 in pl & assign(g,r,c2,d)=s) if |pl|=1
hCol(g,c,d,pl)=s <=> s=g if |pl|>1,  E[r2](r2 in pl & assign(g,r2,c,d)=s) if |pl|=1
hBox(g,r,c,d,pl)=s <=> s=g if |pl|>1,
                       E[m,i,j](m in pl & bb(r)=i & bb(c)=j & assign(g,i+m/2,j+(m-(m/2)*2),d)=s) if |pl|=1

hidden(g,r,c,d)=s <=> E[g2,g3](hRow(g,r,d,rowFree(g,r,d,0))=g2 &
                               hCol(g2,c,d,colFree(g2,c,d,0))=g3 &
                               hBox(g3,r,c,d,boxFree(g3,r,c,d,0))=s)
```

Replace `afterElim` again with the full version (naked single, then hidden single across the three units):

```
afterElim(g,r,c,d,ns)=s <=> E[g2](nakedSingle(g,r,c)=g2 & hidden(g2,r,c,d)=s) if |ns|=1,
                            hidden(g,r,c,d)=s                                  if |ns|>1
```

- [ ] **Step 4: Run CLI, verify the `rowFree` probe PASSES.** `hBox`'s `E[m,i,j]` uses 3 vars (the documented max) - if it parse-errors or crashes, that is a finding: capture the repro, mark BLOCKED.

- [ ] **Step 5: Commit** `feat(sudoku-csp): hidden-single propagation`

---

### Task 9: `parse` (start full, assign givens)

**Files:** Modify `examples/sudoku-4x4-csp.nl`

- [ ] **Step 1: Write the failing probe** (append): after parsing `P0`, cell (0,0) is `{D1}`, and its row peer (0,1) has lost `D1`:

```
cell(parse([[1,0,0,0],[0,0,1,0],[0,3,0,0],[0,0,0,4]]),0,0)=sc ? [(sc={D1})][..]
cell(parse([[1,0,0,0],[0,0,1,0],[0,3,0,0],[0,0,0,4]]),0,1)=sc ? [(sc={D2,D3,D4})][..]
```

- [ ] **Step 2: Run CLI, verify it fails** (`parse`/`parseAt` undefined).

- [ ] **Step 3: Add parse** (walk the puzzle, `assign` each non-zero clue on the full grid):

```
List<List<Set<Digit>>> ::= parse(<List<List<Integer>>>),
                           parseAt(<List<List<Integer>>>,<List<List<Set<Digit>>>>,<Integer>,<Integer>)

parse(p)=g <=> g=parseAt(p, fullGrid(p), 0, 0)
parseAt(p,g,r,c)=s <=> s=g                       if r=4,
                       s=parseAt(p,g,r+1,0)      if r<4 & c=4,
                       s=parseAt(p,g,r,c+1)      if r<4 & c<4 & icell(p,r,c)=0,
                       E[g2](assign(g,r,c,dig(icell(p,r,c)))=g2 & s=parseAt(p,g2,r,c+1))
                                                 if r<4 & c<4 & icell(p,r,c)!=0
```

- [ ] **Step 4: Run CLI, verify both probes PASS.**

- [ ] **Step 5: Commit** `feat(sudoku-csp): parse puzzle into candidate grid`

---

### Task 10: `solved` + MRV cell pick

**Files:** Modify `examples/sudoku-4x4-csp.nl`

- [ ] **Step 1: Write the failing probe** (append): a parsed but unsolved P0 is not solved; the minimum candidate count among unfilled cells is >= 2:

```
solved(parse([[1,0,0,0],[0,0,1,0],[0,3,0,0],[0,0,0,4]])) ? [][()]
minCand(parse([[1,0,0,0],[0,0,1,0],[0,3,0,0],[0,0,0,4]]))=v ? [(v=2)][..]
```

(If P0's real minimum differs from 2 after propagation, set the expectation to the observed value - this probe checks the mechanism, not a specific puzzle fact.)

- [ ] **Step 2: Run CLI, verify it fails** (`solved`/`minCand` undefined).

- [ ] **Step 3: Add solved + MRV support:**

```
Boolean ::= solved(<List<List<Set<Digit>>>>)
Integer ::= minCand(<List<List<Set<Digit>>>>),
            minCandAt(<List<List<Set<Digit>>>>,<Integer>,<Integer>,<Integer>),
            pickCell(<List<List<Set<Digit>>>>,<Integer>,<Integer>)

solved(g) <=> !E[r,c](r in {0,1,2,3} & c in {0,1,2,3} & |cell(g,r,c)|!=1)

// smallest candidate-set size among unfilled cells (best starts at 5 = above max)
minCand(g)=v <=> v=minCandAt(g,0,0,5)
minCandAt(g,r,c,k)=v <=>
    v=k                          if r=4,
    v=minCandAt(g,r+1,0,k)       if r<4 & c=4,
    v=minCandAt(g,r,c+1,k)       if r<4 & c<4 & |cell(g,r,c)|<=1,
    v=minCandAt(g,r,c+1,|cell(g,r,c)|) if r<4 & c<4 & |cell(g,r,c)|>1 & |cell(g,r,c)|<k,
    v=minCandAt(g,r,c+1,k)       if r<4 & c<4 & |cell(g,r,c)|>1 & |cell(g,r,c)|>=k

// flat index (r*4+c) of the first cell whose candidate count = n, scanning from i
pickCell(g,n,i)=k <=> k=i                    if i<16 & |cell(g,i/4,i-(i/4)*4)|=n,
                      k=pickCell(g,n,i+1)    if i<16 & |cell(g,i/4,i-(i/4)*4)|!=n
```

- [ ] **Step 4: Run CLI, verify PASS** (adjust the `minCand` expectation to the observed value if needed).

- [ ] **Step 5: Commit** `feat(sudoku-csp): solved test + MRV cell pick`

---

### Task 11: `search` (DFS with MRV)

**Files:** Modify `examples/sudoku-4x4-csp.nl`

- [ ] **Step 1: Write the failing probe** (append): search on an ALREADY-solved singleton grid returns it unchanged. Build it by parsing the full solution:

```
solved(search(parse([[1,2,4,3],[3,4,1,2],[4,3,2,1],[2,1,3,4]]))) ? [()][]
```

- [ ] **Step 2: Run CLI, verify it fails** (`search` undefined).

- [ ] **Step 3: Add search** (if solved, return; else pick the MRV cell, try each of its candidates, first success wins; a failed `assign` prunes the branch):

```
List<List<Set<Digit>>> ::= search(<List<List<Set<Digit>>>>)

search(g)=s <=> s=g if solved(g),
                E[k,d,g2](k=pickCell(g,minCand(g),0) & d in cell(g,k/4,k-(k/4)*4) &
                          assign(g,k/4,k-(k/4)*4,d)=g2 & s=search(g2))  if !solved(g)
```

- [ ] **Step 4: Run CLI, verify PASS.** `E[k,d,g2]` uses the max 3 vars; `d in cell(...)` enumerates candidates for backtracking. If it crashes/undecides, capture the repro, mark BLOCKED.

- [ ] **Step 5: Commit** `feat(sudoku-csp): DFS search with MRV`

---

### Task 12: `solve` end-to-end + oracle queries

**Files:** Modify `examples/sudoku-4x4-csp.nl`

- [ ] **Step 1: Write the failing probe** (append): convert the solved candidate grid back to integers and assert the full known solution:

```
solve([[1,0,0,0],[0,0,1,0],[0,3,0,0],[0,0,0,4]])=p ? [(p=[[1,2,4,3],[3,4,1,2],[4,3,2,1],[2,1,3,4]])][..]
```

- [ ] **Step 2: Run CLI, verify it fails** (`solve`/`toNums`/`cellNum` undefined).

- [ ] **Step 3: Add unparse + top-level solve:**

```
Integer             ::= cellNum(<Set<Digit>>)
List<Integer>       ::= numRow(<List<Set<Digit>>>)
List<List<Integer>> ::= toNums(<List<List<Set<Digit>>>>),
                        solve(<List<List<Integer>>>)

cellNum(sc)=v <=> E[d](d in sc & |sc|=1 & undig(d)=v)
numRow(rw)=il <=> il=[0,1,2,3] map [c](cellNum(catRow(rw,c)))
toNums(g)=p   <=> p=[0,1,2,3] map [r](numRow(crow(g,r)))
solve(p)=q    <=> q=toNums(search(parse(p)))
```

- [ ] **Step 4: Run CLI, verify PASS. Then run the CLI a SECOND time** (nondeterminism check). If the two runs disagree, or it crashes/undecides, capture a repro (this is the headline finding target), mark BLOCKED.

- [ ] **Step 5: Commit** `feat(sudoku-csp): end-to-end solve + oracle query`

---

### Task 13: Clean up probes, keep the oracle

**Files:** Modify `examples/sudoku-4x4-csp.nl`

- [ ] **Step 1:** Delete the intermediate probe queries from Tasks 1-11 (keep the accessor/`put`/`peer` ones only if they document behaviour cheaply). Keep the Task 12 `solve` oracle query as the file's assertion.
- [ ] **Step 2:** Add a header comment block: what the file is (Norvig CSP, options-as-`Digit`), that it is CLI-only, the `-DPARALLEL_COLLECTIONS=false` note, and a pointer to this plan + the spec.
- [ ] **Step 3: Run CLI, verify the oracle query PASSES, grep clean, twice.**
- [ ] **Step 4: Commit** `docs(sudoku-csp): trim probes, add header`

---

# Phase 2 - `examples/sudoku-9x9-csp.nl`

Line-for-line rescale of phase 1: digits 1-9, 3x3 boxes, indices 0-8, base cases at 9, flat index over 81.

### Task 14: Copy + rescale enum, bounds, full set

**Files:** Create `examples/sudoku-9x9-csp.nl` (copy phase-1 file)

- [ ] **Step 1:** Copy `examples/sudoku-4x4-csp.nl` to `examples/sudoku-9x9-csp.nl`.
- [ ] **Step 2:** Change `Digit ::= D1, D2, D3, D4` to `Digit ::= D1, D2, D3, D4, D5, D6, D7, D8, D9`.
- [ ] **Step 3:** Extend `dig`/`undig` to 1-9. Extend `assignOthers` upper bound `i=5`->`i=10`.
- [ ] **Step 4:** Replace every `<4` / `=4` / `{0,1,2,3}` bound with `<9` / `=9` / `{0,1,2,3,4,5,6,7,8}`; every flat-index `16`->`81`, `/4`->`/9`, `-(i/4)*4`->`-(i/9)*9`; `minCand` best start `5`->`10`; `boxFree` loop `m=4`->`m=9`.
- [ ] **Step 5:** Rewrite `fullGrid` as a 9x9 grid of `{D1..D9}` sets.
- [ ] **Step 6: Commit** `feat(sudoku-csp): 9x9 copy - enum + bounds` (do not run yet; box math still 4x4).

### Task 15: 3x3 box math + probes

**Files:** Modify `examples/sudoku-9x9-csp.nl`

- [ ] **Step 1:** `bb(r) <=> k=0 if r<3, k=3 if r>=3 & r<6, k=6 if r>=6`.
- [ ] **Step 2:** In `boxFree`/`hBox`, change the 2x2 offset math `m/2`,`m-(m/2)*2` to 3x3: `m/3`,`m-(m/3)*3`.
- [ ] **Step 3:** Re-point the phase-1 probes (Tasks 3-10) at 9x9 shapes (full set `{D1..D9}`, `minCand` etc.).
- [ ] **Step 4: Run CLI on the 9x9 file**, verify accessor/put/peer/parse probes PASS.
- [ ] **Step 5: Commit** `feat(sudoku-csp): 9x9 box math`

### Task 16: 9x9 puzzles + oracle

**Files:** Modify `examples/sudoku-9x9-csp.nl`

- [ ] **Step 1:** Add the `solve(...)=p ? [...]` oracle for the puzzles from `sudoku-9x9-smart.nl` (copy the exact grids + solutions).
- [ ] **Step 2: Run CLI twice.** A 9x9 search is heavy; if it does not terminate, note the timeout as a finding (not a workaround target) and keep the forced/easy puzzle as the primary oracle.
- [ ] **Step 3:** Trim intermediate probes, add the header comment.
- [ ] **Step 4: Commit** `feat(sudoku-csp): 9x9 puzzles + oracle`

---

# Phase 3 - Grid DSL (separate bug pass)

Goal: an intuitive grid notation that produces the `List<List<Integer>>` phase-1/2 `solve` consumes. This deliberately exercises Root-extending functors (the explicit `#N` precedence gotcha), nested repetition patterns, and multi-line handling.

### Task 17: Cell token (`.` = blank, `1`..`k` = clue) and a single row

**Files:** Modify `examples/sudoku-4x4-csp.nl`

- [ ] **Step 1: Write the failing probe:** a one-row notation parses to a `List<Integer>`, e.g.

```
sudokuRow . 2 3 4 = il ? [(il=[0,2,3,4])][..]
```

- [ ] **Step 2: Run CLI, verify it fails** (pattern undefined).

- [ ] **Step 3: Add a cell pattern and a row functor.** Blank is `.` mapped to 0; digits map through `dig`/their integer. Use a named sub-pattern for the cell and the repetition marker for the row (see `docs/reference/stdlib/lang.md#named-patterns` and the `Pattern ::=` catalogue in `lang.nl`):

```
List<Integer> ::= sudokuRow <(> <Cell> <)+> #0
```

with `Cell` a named pattern matching `.` or a digit, wired to yield the integer. (Exact pattern text is validated at execution; if the Root-extending repetition fails without an explicit `#N`, that is the documented gotcha - add `#0`, which is a syntax requirement, not a workaround.)

- [ ] **Step 4: Run CLI, verify PASS.**
- [ ] **Step 5: Commit** `feat(sudoku-csp): DSL cell + row`

### Task 18: Full grid statement -> `solve`

**Files:** Modify `examples/sudoku-4x4-csp.nl`

- [ ] **Step 1: Write the failing probe:** a `sudoku` block of 4 rows solves to the known grid via the existing `solve`:

```
sudoku
. . . .
. . . .
. . . .
. . . .
```

(as a smoke test the empty grid should yield some complete valid grid; for the oracle use P0's rows and assert P0's solution.)

- [ ] **Step 2: Run CLI, verify it fails** (grid statement undefined).

- [ ] **Step 3: Add the Root-extending grid functor** producing `List<List<Integer>>` and calling `solve`:

```
Puzzle :: Root
Puzzle ::= sudoku <(> <sudokuRow> <)+> #0
```

wiring the collected rows into `solve(rows)` and surfacing the result (mirror how `deHet.nl` bodies expand a Root functor into rule/query statements). Multi-line rows: per the engine-constraints notes, line breaks are only reliable inside unbalanced parens - if the natural multi-line grid drops rows or mis-parses, capture the repro and mark BLOCKED (this is exactly the parser bug the phase is hunting).

- [ ] **Step 4: Run CLI, verify the P0 grid solves to P0's solution.**
- [ ] **Step 5: Commit** `feat(sudoku-csp): DSL grid statement`

### Task 19: Apply DSL to 9x9 + wrap up

**Files:** Modify `examples/sudoku-9x9-csp.nl`

- [ ] **Step 1:** Port the DSL functors to the 9x9 file (rows of 9 cells).
- [ ] **Step 2:** Convert the phase-2 puzzles to the `sudoku` grid notation, keep the oracle expectations.
- [ ] **Step 3: Run CLI twice.**
- [ ] **Step 4:** Update the CLAUDE.md sudoku section to mention the two `-csp.nl` files + the DSL, and summarise the bugs found (list the `bug-repros/` files).
- [ ] **Step 5: Commit** `feat(sudoku-csp): DSL on 9x9 + docs`

---

## Self-Review

**Spec coverage:**
- Norvig algorithm (assign/eliminate naked+hidden, DFS+MRV) -> Tasks 6-12. ✓
- Options as a type (`Digit`), `Set<Digit>` grid, `Integer` for coords/cardinality -> Tasks 1,3,4. ✓
- IO seam (`dig`/`undig`, `0`=blank in, `toNums` out) -> Tasks 1,9,12. ✓
- Puzzles + oracle from the smart solvers -> Tasks 12,16. ✓
- Phases 1(4x4)/2(9x9)/3(DSL) -> Tasks 1-13 / 14-16 / 17-19. ✓
- CLI-only, `-DPARALLEL_COLLECTIONS=false`, run twice -> Execution protocol + Steps. ✓
- No workarounds; bug -> repro in `bug-repros/` + report -> Execution protocol + per-task BLOCKED notes. ✓

**Placeholder scan:** No "TBD"/"handle edge cases". The two places that say "validated at execution" (DSL pattern text, Task 10 `minCand` value) are inherent to a bug-finding exercise where exact engine acceptance is unknown - flagged, not hidden.

**Type consistency:** `cell`/`crow`/`catRow` return `Set<Digit>`/`List<Set<Digit>>`; `icell`/`irow`/`iat` return `Integer`/`List<Integer>` (input side, distinct names). `put`/`elim`/`assign`/`nakedSingle`/`hidden`/`search` all `List<List<Set<Digit>>> -> List<List<Set<Digit>>>`. `rowFree`/`colFree`/`boxFree` -> `Set<Integer>`. `solve`/`toNums` -> `List<List<Integer>>`. `dig` Integer->Digit, `undig`/`cellNum` ->Integer. Names consistent across tasks.

## Execution Handoff

(filled in after the plan is accepted)
