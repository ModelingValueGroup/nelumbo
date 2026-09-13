# Known-bugs as JUnit tests Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Fold the `bug-repros/` red-by-design `.nl` files into the JUnit suite as self-reporting `@KnownBug` tests, keep a root-level CLI runner, delete `bug-repros/`.

**Architecture:** A ~25-line JUnit 5 extension turns a failing known-bug test into ABORTED (grey) and a passing one into FAILED ("appears fixed"). The `.nl` files move to `src/main/resources/org/modelingvalue/nelumbo/bugs/`; `KnownBugsTest` runs each via a `bugResource()` helper with a preemptive timeout. Per-bug documentation moves into the `.nl` headers and test comments; findings without a repro go into the class javadoc.

**Tech Stack:** JUnit 5 (jupiter, already present), bash for the CLI runner. No new dependencies.

---

## Execution protocol

- Spec: `docs/superpowers/specs/2026-09-13-known-bugs-junit-design.md`. Key rules:
  dedup ONLY on truly identical constructs (default: keep both); every shrunk
  file must show the SAME symptom before and after (grep string); every
  converted test must be proven RED in the test JVM.
- CLI verify command (phase 1, while files are still in `bug-repros/`):
  ```sh
  java -ea -DPARALLEL_COLLECTIONS=false -jar cli/build/libs/nelumbo-cli-*.jar bug-repros/<f>.nl
  ```
- Git: feature branch, small commits, no push (Tom pushes).

Setup once:
```sh
git checkout develop && git pull --ff-only && git checkout -b known-bugs-junit
./gradlew :cli:cliJar -q
```

## File Structure

- Create: `src/test/java/org/modelingvalue/nelumbo/test/KnownBug.java` (annotation)
- Create: `src/test/java/org/modelingvalue/nelumbo/test/KnownBugExtension.java`
- Create: `src/test/java/org/modelingvalue/nelumbo/test/KnownBugsTest.java`
- Modify: `src/test/java/org/modelingvalue/nelumbo/test/NelumboTestBase.java` (add `bugResource`)
- Modify: `src/main/java/org/modelingvalue/nelumbo/NelumboConstants.java` (add `NELUMBO_BUGS`)
- Move: `bug-repros/*.nl` -> `src/main/resources/org/modelingvalue/nelumbo/bugs/`
- Create: `run-all-tests-with-CLI` (repo root; successor of `bug-repros/run-all.sh`)
- Delete: `bug-repros/` (README.md content redistributed, run-all.sh superseded)
- Modify: `CLAUDE.md` (bug-repros section rewritten)

---

### Task 1: Branch + symptom inventory

- [ ] **Step 1:** `git checkout develop && git pull --ff-only && git checkout -b known-bugs-junit`
- [ ] **Step 2:** Build the CLI jar: `./gradlew :cli:cliJar -q`
- [ ] **Step 3:** Record the per-file symptom table (the phase-1 truth):

```sh
for f in bug-repros/*.nl; do
  O=$(timeout 90 java -ea -DPARALLEL_COLLECTIONS=false -jar cli/build/libs/nelumbo-cli-*.jar "$f" 2>&1)
  S=$(echo "$O" | grep -m1 -E 'Expected result|Exception|Error' || echo "NO-MARKER")
  printf '%s\t%s\n' "$(basename $f)" "$(echo $S | cut -c1-100)"
done | tee /tmp/known-bug-symptoms.tsv
```

Expected: 25 lines, each with a mismatch/exception marker (nondeterministic-inference may show NO-MARKER on a lucky run - note it).
- [ ] **Step 4:** Keep `/tmp/known-bug-symptoms.tsv` open as the reference for Tasks 2-4. No commit (nothing changed).

---

### Task 2: Shrink `speculative-guard-index-crash.nl` (92 -> ~25 lines)

**Files:** Modify `bug-repros/speculative-guard-index-crash.nl`

- [ ] **Step 1:** Note the current symptom line from the tsv (expect `IndexOutOfBoundsException`).
- [ ] **Step 2:** Replace the file body (keep the header comment, updated) with the minimal trigger - a guarded scan whose false guard still probes an out-of-range `pos`:

```
import nelumbo.collections

Integer       i, r, x
List<Integer> l

Integer ::= at(<List<Integer>>,<Integer>),
            scan(<List<Integer>>,<Integer>)

// deliberately UNguarded accessor - the bug is that the r<4 guard below is
// evaluated speculatively at r=4, probing at(l,4) anyway
at(l,i)=x   <=> x pos l = i
scan(l,r)=x <=> x=0                              if r=4,
                x=scan(l,r+1)                    if r<4 & at(l,r)>=0

scan([1,2,3,4],0)=x ? [(x=0)][..]
```

- [ ] **Step 3:** Run the CLI on it. Expected: SAME symptom class as the tsv line (IndexOutOfBounds via Predicate.callMethod/Collections.indexOf). If the symptom differs (e.g. it now hits the bba88fc8 reduction crash instead), iterate on the shape until the ORIGINAL symptom shows; if that proves impossible on current develop, keep the original file unshrunk and note why in its header.
- [ ] **Step 4:** Commit: `git add -u && git commit -m "test(bug-repros): shrink speculative-guard repro to minimal trigger"`

---

### Task 3: Shrink `where-filter-npe-in-recursion.nl` (78 -> as small as the symptom allows)

**Files:** Modify `bug-repros/where-filter-npe-in-recursion.nl`

- [ ] **Step 1:** Note the tsv symptom (expect NPE at InferResult via Lambda).
- [ ] **Step 2:** Iteratively strip the file: remove rules/queries not needed for the NPE, shrink the recursion depth and grid, re-running the CLI after each cut. The bug is documented as "fine standalone, NPEs inside deep recursion" - so the recursion wrapper must stay; try halving until the NPE disappears, then step back.
- [ ] **Step 3:** Final CLI run shows the SAME NPE. If no reduction below ~40 lines keeps the NPE, keep the smallest red version found.
- [ ] **Step 4:** Commit: `test(bug-repros): shrink where-filter NPE repro`

---

### Task 4: Dedup review (conservative)

- [ ] **Step 1:** Review these candidate pairs against the criterion "truly identical construct" (spec: when in doubt, keep both):
  - `collections-index-out-of-range` vs `speculative-guard-index-crash` - expected verdict: KEEP BOTH (native indexOf crash vs speculative guard evaluation)
  - `datetime-multiply-overflow` vs `datetime-year-360` - KEEP BOTH (int overflow vs 360-day arithmetic)
  - `rational-sign` vs `rational-zero-factor` - KEEP BOTH (normalize sign vs zero denominator)
  - the three reduction repros (`nested-list-functor-arg-type-mismatch`, `set-functor-rhs-equals-not-reduced`, `recursive-list-concat-classcast`) - KEEP ALL (same suspected root cause, three distinct constructs)
- [ ] **Step 2:** If any pair IS the identical construct, merge into one file (union of queries) and delete the other; verify red. Document each verdict in the commit message.
- [ ] **Step 3:** Commit (or skip if nothing merged): `test(bug-repros): dedup review - verdicts in message`

---

### Task 5: `@KnownBug` infrastructure + self-test

**Files:** Create `KnownBug.java`, `KnownBugExtension.java`; modify `NelumboTestBase.java`, `NelumboConstants.java`

- [ ] **Step 1:** Add to `src/main/java/org/modelingvalue/nelumbo/NelumboConstants.java` next to `NELUMBO_EXAMPLES`/`NELUMBO_TESTS`:

```java
String NELUMBO_BUGS = NELUMBO_LIBRARY + "bugs/";
```

- [ ] **Step 2:** Create `src/test/java/org/modelingvalue/nelumbo/test/KnownBug.java` (LGPL header like the other files):

```java
package org.modelingvalue.nelumbo.test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.jupiter.api.extension.ExtendWith;

// Marks a test that reproduces a KNOWN, unfixed engine bug (xfail):
// - test fails  -> ABORTED (grey): bug still present, build stays green
// - test passes -> FAILED: the bug appears fixed - remove @KnownBug and
//   promote the method to RegressionTest
// - flaky=true  -> a pass also aborts (race-dependent repros never turn
//   the build red on a lucky run)
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@ExtendWith(KnownBugExtension.class)
public @interface KnownBug {
    String value();

    boolean flaky() default false;
}
```

- [ ] **Step 3:** Create `src/test/java/org/modelingvalue/nelumbo/test/KnownBugExtension.java`:

```java
package org.modelingvalue.nelumbo.test;

import java.lang.reflect.Method;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.InvocationInterceptor;
import org.junit.jupiter.api.extension.ReflectiveInvocationContext;
import org.opentest4j.TestAbortedException;

public class KnownBugExtension implements InvocationInterceptor {
    @Override
    public void interceptTestMethod(Invocation<Void> invocation, ReflectiveInvocationContext<Method> invocationContext,
            ExtensionContext extensionContext) throws Throwable {
        KnownBug kb = invocationContext.getExecutable().getAnnotation(KnownBug.class);
        try {
            invocation.proceed();
        } catch (Throwable t) {
            String detail = t.getMessage() == null ? t.getClass().getSimpleName() : t.getMessage().lines().findFirst().orElse("");
            throw new TestAbortedException("known bug still present: " + kb.value() + " [" + detail + "]");
        }
        if (kb.flaky()) {
            throw new TestAbortedException("passed this run, but flaky - known bug: " + kb.value());
        }
        Assertions.fail("KNOWN BUG APPEARS FIXED: " + kb.value() + " - remove @KnownBug and promote to RegressionTest");
    }
}
```

- [ ] **Step 4:** Add to `NelumboTestBase.java` (next to `testResource`); the preemptive timeout replaces `@Timeout` so a diverging repro aborts instead of failing the build (the hung thread leaks until the test JVM exits - acceptable):

```java
public void bugResource(String resource) {
    org.junit.jupiter.api.Assertions.assertTimeoutPreemptively(java.time.Duration.ofSeconds(60),
            () -> resource(resource, NelumboConstants.NELUMBO_BUGS));
}
```

- [ ] **Step 5: Self-test the mechanism.** Create `KnownBugsTest.java` with ONLY two temporary methods:

```java
package org.modelingvalue.nelumbo.test;

import org.junit.jupiter.api.Test;

public class KnownBugsTest extends NelumboTestBase {

    static {
        setProp("PARALLEL_COLLECTIONS", "false");
        setProp("REVERSE_NELUMBO", "false");
        setProp("RANDOM_NELUMBO", "false");
        setProp("TRACE_NELUMBO", "false");
        setProp("TRACE_SYNTATIC", "false");
        setProp("VERBOSE_TESTS", "false");
    }

    @KnownBug("self-test: red path")
    @Test
    public void tempRed() {
        org.junit.jupiter.api.Assertions.fail("still broken");
    }

    @KnownBug("self-test: green path")
    @Test
    public void tempGreen() {
    }
}
```

- [ ] **Step 6:** Run: `./gradlew :test --tests "org.modelingvalue.nelumbo.test.KnownBugsTest" --rerun-tasks`
Expected: BUILD FAILED with exactly ONE failure (`tempGreen`, message contains "KNOWN BUG APPEARS FIXED"); the XML (`build/test-results/test/TEST-...KnownBugsTest.xml`) shows `tests="2" skipped="1" failures="1"` (tempRed = skipped/aborted).
- [ ] **Step 7:** Delete both temp methods (leave the empty class + static block).
- [ ] **Step 8:** Commit: `test: add @KnownBug xfail mechanism (fail->aborted, pass->announce fix)`

---

### Task 6: Move the `.nl` files + fold in README info

- [ ] **Step 1:**

```sh
mkdir -p src/main/resources/org/modelingvalue/nelumbo/bugs
git mv bug-repros/*.nl src/main/resources/org/modelingvalue/nelumbo/bugs/
```

- [ ] **Step 2:** For each moved file, extend its header comment with its README row info that is not already there: confirmed status, suspected code location (e.g. `logic/Predicate.callMethod`, `Strings.java:55`-style), and for the 2026-09-11 trio the bba88fc8 attribution. Most headers already carry the description - only ADD what is missing, do not rewrite.
- [ ] **Step 3:** Commit: `test: move bug repros to resources/bugs, fold README info into headers`

---

### Task 7: Fill `KnownBugsTest` with all repro methods

**Files:** Modify `KnownBugsTest.java`

- [ ] **Step 1:** Replace the (empty) class body with one method per `.nl` file, grouped in commented sections. Method naming: camelCase of the file name. `@KnownBug` values: one-line issue summary (source: the file's own header). Layout:

```java
// ==== reduction / typing cluster (introduced by bba88fc8 + 2bad3f27) ====

@KnownBug("collection-returning functor call nested as argument crashes (argument type mismatch)")
@Test
public void nestedListFunctorArgTypeMismatch() {
    bugResource("nested-list-functor-arg-type-mismatch.nl");
}

@KnownBug("collection-valued expression on RHS of = is not reduced")
@Test
public void setFunctorRhsEqualsNotReduced() {
    bugResource("set-functor-rhs-equals-not-reduced.nl");
}

@KnownBug("recursive collection accumulation crashes ClassCast Variable->List")
@Test
public void recursiveListConcatClassCast() {
    bugResource("recursive-list-concat-classcast.nl");
}

// ==== state / race cluster ====

@KnownBug("where-filter over user Boolean rule NPEs inside deep recursion")
@Test
public void whereFilterNpeInRecursion() {
    bugResource("where-filter-npe-in-recursion.nl");
}

@KnownBug("guarded Set-valued rule with empty-set branch undecided in map lambda")
@Test
public void emptySetBranchInMapLambda() {
    bugResource("empty-set-branch-in-map-lambda.nl");
}

@KnownBug("query result depends on neighboring queries")
@Test
public void neighborQueryChangesResult() {
    bugResource("neighbor-query-changes-result.nl");
}

@KnownBug(value = "inference results nondeterministic run-to-run (ContextPool race)", flaky = true)
@Test
public void nondeterministicInference() {
    bugResource("nondeterministic-inference.nl");
}

// ==== speculative evaluation ====

@KnownBug("rule guards evaluated speculatively: unguarded pos crashes out-of-range")
@Test
public void speculativeGuardIndexCrash() {
    bugResource("speculative-guard-index-crash.nl");
}

// ==== parser / diagnostics ====

@KnownBug("E/!E with 4 variables parses in rule bodies then crashes at runtime")
@Test
public void fourVarQuantifierCrash() {
    bugResource("four-var-quantifier-crash.nl");
}

@KnownBug("if-guard on its own continuation line silently dropped")
@Test
public void guardOnContinuationLineDropped() {
    bugResource("guard-on-continuation-line-dropped.nl");
}

@KnownBug("undeclared variable: misleading error, rule silently dropped")
@Test
public void undeclaredVariableDiagnostics() {
    bugResource("undeclared-variable-diagnostics.nl");
}

@KnownBug("// directly after operator char swallowed into operator token")
@Test
public void commentAfterOperator() {
    bugResource("comment-after-operator.nl");
}

@KnownBug("multi-line STRING token crashes checkToken with -ea")
@Test
public void multilineStringAssert() {
    bugResource("multiline-string-assert.nl");
}

// ==== patterns ====

@KnownBug("multi-keyword alternation options lose their identity (alt flag dropped)")
@Test
public void alternationOptionIdentity() {
    bugResource("alternation-option-identity.nl");
}

@KnownBug("matched optional with multi-keyword body recorded as absent")
@Test
public void optionalPresenceLost() {
    bugResource("optional-presence-lost.nl");
}

@KnownBug("greedy separator consumption without backtracking crashes on valid input")
@Test
public void repetitionSeparatorGreedy() {
    bugResource("repetition-separator-greedy.nl");
}

// ==== logic / quantifiers / facts ====

@KnownBug("fully-bound quantifiers turn unknown body result into definitive answer")
@Test
public void quantifierDefinitiveFromUnknown() {
    bugResource("quantifier-definitive-from-unknown.nl");
}

@KnownBug("getFacts claims complete-no-facts for un-indexed shapes like r(a,a)")
@Test
public void diagonalFactLookup() {
    bugResource("diagonal-fact-lookup.nl");
}

// ==== stdlib: numbers / strings / collections / datetime ====

@KnownBug("normalize() keeps negative denominators: -2 > 0 inferred")
@Test
public void rationalSign() {
    bugResource("rational-sign.nl");
}

@KnownBug("zero factor builds n/0 rationals")
@Test
public void rationalZeroFactor() {
    bugResource("rational-zero-factor.nl");
}

@KnownBug("division by zero crashes the whole evaluation")
@Test
public void integersDivZero() {
    bugResource("integers-div-zero.nl");
}

@KnownBug("int(...) uses Integer.parseInt: silent 32-bit limit")
@Test
public void integerString32bit() {
    bugResource("integer-string-32bit.nl");
}

@KnownBug("indexOf crashes on out-of-range index; 2^32 wraps to element 0")
@Test
public void collectionsIndexOutOfRange() {
    bugResource("collections-index-out-of-range.nl");
}

@KnownBug("period_multiply crashes on multipliers outside int range")
@Test
public void datetimeMultiplyOverflow() {
    bugResource("datetime-multiply-overflow.nl");
}

@KnownBug("360-day year arithmetic off")
@Test
public void datetimeYear360() {
    bugResource("datetime-year-360.nl");
}
```

- [ ] **Step 2:** Add the class javadoc: purpose (xfail suite, CLI runner note: `run-all-tests-with-CLI` shows the CLI manifestation, which can differ from the test JVM), plus a "findings without an .nl repro" comment block carrying the README rows whose Repro column is `-` (the EditorWindow/LSP/interactive/unverified ones) - copy them verbatim from `bug-repros/README.md` before it is deleted.
- [ ] **Step 3:** Compile check: `./gradlew :compileTestJava -q` - expect success.
- [ ] **Step 4:** Commit: `test: KnownBugsTest - one @KnownBug method per repro`

---

### Task 8: Prove every method RED in the test JVM

- [ ] **Step 1:** Run: `./gradlew :test --tests "org.modelingvalue.nelumbo.test.KnownBugsTest" --rerun-tasks 2>&1 | tail -20`
- [ ] **Step 2:** Read the XML: `grep -oE 'tests="[0-9]+" skipped="[0-9]+" failures="[0-9]+" errors="[0-9]+"' build/test-results/test/TEST-org.modelingvalue.nelumbo.test.KnownBugsTest.xml`
Expected: `failures="0"` and `skipped=` == number of methods (all aborted = all bugs red in the test JVM).
- [ ] **Step 3:** For every method that instead FAILED with "APPEARS FIXED" (= green in the test JVM although the CLI shows it red): first try to sharpen the `.nl` expectation so it is red in BOTH environments (re-verify with the CLI after edits!). If the file fundamentally cannot go red in the test JVM (e.g. the deliberately expectation-free quantifier/diagonal queries), DELETE its test method and list the file under a "CLI-only repros" note in the class javadoc - the file stays for the CLI runner.
- [ ] **Step 4:** Re-run until failures=0. Note the final aborted count.
- [ ] **Step 5:** Commit: `test: prove all known-bug methods red in the test JVM`

---

### Task 9: `run-all-tests-with-CLI` in the repo root

**Files:** Create `run-all-tests-with-CLI`; delete `bug-repros/run-all.sh`

- [ ] **Step 1:** `git mv bug-repros/run-all.sh run-all-tests-with-CLI`
- [ ] **Step 2:** Edit it: `REPRO_DIR` becomes `"$ROOT_DIR/src/main/resources/org/modelingvalue/nelumbo/bugs"` with `ROOT_DIR="$(cd "$(dirname "$0")" && pwd)"` (script now lives in the root); header comment updated (name, location, and that the JUnit twin is `KnownBugsTest` - CLI manifestation may differ from the test JVM). Keep the color feature and per-file PASS/FAIL logic unchanged.
- [ ] **Step 3:** `chmod +x run-all-tests-with-CLI && ./run-all-tests-with-CLI | tail -3` - expected: the familiar `N fixed, M still failing` summary over the files in the new location.
- [ ] **Step 4:** Commit: `chore: move CLI bug runner to root as run-all-tests-with-CLI`

---

### Task 10: Delete `bug-repros/`, update CLAUDE.md, final verify

- [ ] **Step 1:** Double-check nothing in `bug-repros/README.md` is unaccounted for: per-bug rows -> file headers (Task 6), no-repro rows -> class javadoc (Task 7), general intro -> class javadoc + CLAUDE.md. Then `git rm bug-repros/README.md` (directory disappears with it).
- [ ] **Step 2:** Rewrite the CLAUDE.md "Bug Repros" section: repros live in `src/main/resources/org/modelingvalue/nelumbo/bugs/`, run in JUnit via `KnownBugsTest` (@KnownBug xfail: red bug = grey aborted test, fixed bug = red test announcing promotion to RegressionTest), CLI runner `./run-all-tests-with-CLI` in the root, CLI vs test-JVM manifestations can differ. Also update the sudoku section's `bug-repros/...` references to the new path.
- [ ] **Step 3:** Full verify: `./gradlew test 2>&1 | tail -5` - expected BUILD SUCCESSFUL (KnownBugsTest aborted tests do not fail the build; RegressionTest and all others green).
- [ ] **Step 4:** `./run-all-tests-with-CLI | tail -3` - expected: same red set as Task 1 (minus any expectation-sharpening side effects, which were re-verified in Task 8).
- [ ] **Step 5:** `grep -rn "bug-repros" CLAUDE.md README.md docs/ src/ --include="*.md" --include="*.java" --include="*.nl" | grep -v superpowers` - fix any stale references (historical specs/plans under docs/superpowers stay as-is).
- [ ] **Step 6:** Commit: `docs: bug-repros folded into JUnit suite; CLAUDE.md updated`

---

## Self-Review

**Spec coverage:** xfail mechanism incl. flaky + timeout (Task 5); move to resources/bugs (Task 6); per-bug docs into headers/methods, no-repro findings into javadoc (Tasks 6-7); conservative dedup with keep-both default (Task 4); shrink the two big files with symptom-grep guard (Tasks 2-3); test-JVM red gate with sharpen-or-CLI-only fallback (Task 8); root CLI runner named `run-all-tests-with-CLI` (Task 9); `bug-repros/` deleted (Task 10); feature branch + small commits (protocol). ✓

**Placeholder scan:** none. Task 2's replacement body is complete; Task 3 is inherently iterative (explicit smallest-red-version fallback). ✓

**Type consistency:** `bugResource` (NelumboTestBase) used by all KnownBugsTest methods; `NELUMBO_BUGS` constant matches the move target path; `@KnownBug(value, flaky)` matches the extension's reads. Method names = camelCase of the 25 file names, all listed. ✓

## Execution Handoff

Two options: 1) Subagent-driven, 2) Inline with executing-plans. Given the per-file CLI/test-JVM verification judgment calls (same pattern as the CSP session), inline is recommended.
