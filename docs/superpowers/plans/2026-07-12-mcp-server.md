# Nelumbo MCP Server Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** An MCP stdio server (`nelumbo-mcp-server` jar) that lets any MCP-capable LLM author self-contained `.nl` decision models via four tools: `eval_nl`, `search_docs`, `get_example`, `new_model`.

**Architecture:** A new Gradle subproject `mcp/` depends on the root project and the official MCP Java SDK 2.0.0 (stdio transport). The CLI's parse/evaluate logic is first extracted into a structured `NelumboEvaluator` in the core project (mirroring the LSP's `QueryEvaluator` deadline pattern); the MCP tool handlers are plain classes returning `Map<String,Object>` (JSON via Jackson 2), with the SDK wiring isolated in `Main`.

**Tech Stack:** Java 21, Gradle 8 (Kotlin DSL), `io.modelcontextprotocol.sdk:mcp:2.0.0` (brings Jackson 3 under `tools.jackson`, no clash with Jackson 2), `com.fasterxml.jackson.core:jackson-databind:2.22.0` for our own JSON, JUnit Jupiter 6.0.3, com.gradleup.shadow 9.4.3.

**Spec:** `docs/superpowers/specs/2026-07-12-mcp-server-design.md`

---

## Conventions for every task

- **License header:** every new `.java` file MUST start with the 15-line LGPL header block. Copy lines 1-15 verbatim from `src/main/java/org/modelingvalue/nelumbo/tools/NelumboCli.java`. (The `mvgCorrector` Gradle task enforces this; Task 10 runs it as a final check.)
- **ASCII only** in all files: plain hyphens, straight quotes, no trailing spaces.
- **Test filters:** the root `test` task chains `:lsp:server:test` and `:http:test`; use `./gradlew :test --tests "..."` (note the leading `:`) to run only a core test class, and `./gradlew :mcp:test --tests "..."` for mcp classes. Add `--rerun-tasks` if Gradle says UP-TO-DATE after a resource-only change.
- Work on `develop` (current branch). Commit after each task with the message given in the task.

## File structure

```
src/main/java/org/modelingvalue/nelumbo/tools/NelumboEvaluator.java   (new: structured eval)
src/main/java/org/modelingvalue/nelumbo/tools/NelumboCli.java         (modify: delegate)
src/test/java/org/modelingvalue/nelumbo/test/NelumboEvaluatorTest.java (new)
settings.gradle.kts                                                    (modify: include mcp)
build.gradle.kts                                                       (modify: test chain)
mcp/build.gradle.kts                                                   (new)
mcp/src/main/java/org/modelingvalue/nelumbo/mcp/DocSearch.java         (new: search_docs backend)
mcp/src/main/java/org/modelingvalue/nelumbo/mcp/ExampleCatalog.java    (new: get_example backend)
mcp/src/main/java/org/modelingvalue/nelumbo/mcp/Hints.java             (new: curated hint table)
mcp/src/main/java/org/modelingvalue/nelumbo/mcp/ModelSkeleton.java     (new: new_model backend)
mcp/src/main/java/org/modelingvalue/nelumbo/mcp/NelumboTools.java      (new: 4 tool handlers, SDK-free)
mcp/src/main/java/org/modelingvalue/nelumbo/mcp/Main.java              (new: stdio server wiring)
mcp/src/test/java/org/modelingvalue/nelumbo/mcp/DocIndexTest.java      (new)
mcp/src/test/java/org/modelingvalue/nelumbo/mcp/DocSearchTest.java     (new)
mcp/src/test/java/org/modelingvalue/nelumbo/mcp/ExampleCatalogTest.java (new)
mcp/src/test/java/org/modelingvalue/nelumbo/mcp/HintsTest.java         (new)
mcp/src/test/java/org/modelingvalue/nelumbo/mcp/ModelSkeletonTest.java (new)
mcp/src/test/java/org/modelingvalue/nelumbo/mcp/NelumboToolsTest.java  (new)
.github/workflows/build.yaml                                           (modify: build mcpJar)
CLAUDE.md                                                              (modify: document module)
```

Key core APIs (already existing, verified):

- `KnowledgeBase(KnowledgeBase parent)` ctor, `setDeadlineNanos(long)`, `run(Runnable)` (synchronous, worker thread), static `KnowledgeBase.BASE` and `KnowledgeBase.CURRENT.get()`.
- `new Parser(new Tokenizer(src, name).tokenize()).parseNonThrowing()` -> `ParserResult` with `roots()`, `exceptions()`.
- `new ParserResult(null, true)` = throwing exception handler; `Evaluatable.evaluate(KnowledgeBase, ParserResult)` throws `ParseException` per node (expectation mismatch message starts with `"Expected result "`).
- `ParseException`: `line()`, `position()` (both 0-based), `length()`, `getShortMessage()`.
- `Query`: `predicate()` (deparse via `predicate().deparse(StringBuffer)`), `inferResult()`, `hasExpected()`.
- `org.modelingvalue.nelumbo.NelumboTimeoutException` is thrown by inference past the deadline and escapes `run()`.
- Reference implementation of this whole dance: `lsp/server/src/main/java/org/modelingvalue/nelumbo/lsp/QueryEvaluator.java:58-105`.

---

### Task 1: NelumboEvaluator (core, structured eval results)

**Files:**
- Create: `src/test/java/org/modelingvalue/nelumbo/test/NelumboEvaluatorTest.java`
- Create: `src/main/java/org/modelingvalue/nelumbo/tools/NelumboEvaluator.java`

- [ ] **Step 1: Write the failing test**

`src/test/java/org/modelingvalue/nelumbo/test/NelumboEvaluatorTest.java` (license header first, then):

```java
package org.modelingvalue.nelumbo.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.modelingvalue.nelumbo.tools.NelumboEvaluator;
import org.modelingvalue.nelumbo.tools.NelumboEvaluator.EvalResult;

public class NelumboEvaluatorTest {

    private static final String FIB = """
            import  nelumbo.integers

            Integer ::= fib(<Integer>)

            Integer n, f

            fib(n)=f <=>  f=n                 if n>=0 & n<=1,
                          f=fib(n-1)+fib(n-2) if n>1

            fib(5)=f    ? [(f=5)][..]
            """;

    @Test
    public void goodFile() {
        EvalResult r = NelumboEvaluator.evaluate(FIB, "good.nl", 0);
        assertTrue(r.ok(), () -> "diagnostics: " + r.diagnostics());
        assertTrue(r.diagnostics().isEmpty());
        assertEquals(1, r.queries().size());
        assertNotNull(r.queries().get(0).result());
        assertEquals(Boolean.TRUE, r.queries().get(0).expectationMatched());
    }

    @Test
    public void parseError() {
        EvalResult r = NelumboEvaluator.evaluate("flurb @@ blarg\n", "bad.nl", 0);
        assertFalse(r.ok());
        assertFalse(r.diagnostics().isEmpty());
        assertTrue(r.diagnostics().get(0).line() >= 1);
        assertTrue(r.diagnostics().get(0).col() >= 1);
        assertNotNull(r.diagnostics().get(0).message());
    }

    @Test
    public void expectationMismatch() {
        String src = FIB.replace("[(f=5)]", "[(f=99)]");
        EvalResult r = NelumboEvaluator.evaluate(src, "mismatch.nl", 0);
        assertFalse(r.ok());
        assertTrue(r.diagnostics().stream().anyMatch(d -> d.message().startsWith("Expected result ")));
        assertEquals(1, r.queries().size());
        assertEquals(Boolean.FALSE, r.queries().get(0).expectationMatched());
    }

    @Test
    public void deadline() {
        String src = """
                import  nelumbo.integers

                Integer ::= loop(<Integer>)

                Integer n, f

                loop(n)=f <=>  loop(n+1)=f if n>=0

                loop(0)=f ?
                """;
        EvalResult r = NelumboEvaluator.evaluate(src, "loop.nl", 300);
        assertFalse(r.ok());
        assertTrue(r.diagnostics().stream().anyMatch(d -> d.message().contains("deadline")),
                () -> "diagnostics: " + r.diagnostics());
    }

    @Test
    public void missingTrailingNewlineIsTolerated() {
        EvalResult r = NelumboEvaluator.evaluate("import  nelumbo.integers", "nonewline.nl", 0);
        assertTrue(r.ok(), () -> "diagnostics: " + r.diagnostics());
    }
}
```

Note on `deadline`: the `loop` rule recurses unboundedly. If inference happens to terminate on it (memoization), replace the rule body with `loop(n)=f <=> loop(n+1)=f | loop(n+2)=f if n>=0` to defeat it; the assertion is the contract.

- [ ] **Step 2: Run the test to verify it fails**

Run: `./gradlew :test --tests "org.modelingvalue.nelumbo.test.NelumboEvaluatorTest"`
Expected: compile error, `NelumboEvaluator` does not exist.

- [ ] **Step 3: Implement NelumboEvaluator**

`src/main/java/org/modelingvalue/nelumbo/tools/NelumboEvaluator.java` (license header first, then):

```java
package org.modelingvalue.nelumbo.tools;

import java.util.ArrayList;
import java.util.List;

import org.modelingvalue.nelumbo.Evaluatable;
import org.modelingvalue.nelumbo.KnowledgeBase;
import org.modelingvalue.nelumbo.NelumboTimeoutException;
import org.modelingvalue.nelumbo.Node;
import org.modelingvalue.nelumbo.logic.InferResult;
import org.modelingvalue.nelumbo.logic.Query;
import org.modelingvalue.nelumbo.syntax.ParseException;
import org.modelingvalue.nelumbo.syntax.Parser;
import org.modelingvalue.nelumbo.syntax.ParserResult;
import org.modelingvalue.nelumbo.syntax.Tokenizer;

/**
 * Parses and evaluates a self-contained .nl source and returns structured results
 * (instead of printing, like {@code NelumboCli}, or producing LSP ranges, like the
 * LSP {@code QueryEvaluator}). Line and column in diagnostics are 1-based.
 */
public final class NelumboEvaluator {

    public record Diagnostic(int line, int col, int length, String message) {
    }

    /** expectationMatched is null when the query carries no expected result. */
    public record QueryOutcome(String query, String result, Boolean expectationMatched) {
    }

    public record EvalResult(boolean ok, List<Diagnostic> diagnostics, List<QueryOutcome> queries) {
    }

    private NelumboEvaluator() {
    }

    /** deadlineMs <= 0 means no deadline. */
    public static EvalResult evaluate(String source, String name, long deadlineMs) {
        String src = source.endsWith("\n") ? source : source + "\n";
        List<Diagnostic> diagnostics = new ArrayList<>();
        List<QueryOutcome> queries = new ArrayList<>();
        KnowledgeBase evalKb = new KnowledgeBase(KnowledgeBase.BASE);
        if (deadlineMs > 0) {
            evalKb.setDeadlineNanos(System.nanoTime() + deadlineMs * 1_000_000L);
        }
        try {
            evalKb.run(() -> {
                KnowledgeBase kb = KnowledgeBase.CURRENT.get();
                ParserResult parsed = new Parser(new Tokenizer(src, name).tokenize()).parseNonThrowing();
                for (ParseException e : parsed.exceptions()) {
                    diagnostics.add(toDiagnostic(e));
                }
                ParserResult throwing = new ParserResult(null, true);
                for (Node root : parsed.roots()) {
                    if (!(root instanceof Evaluatable eval)) {
                        continue;
                    }
                    try {
                        eval.evaluate(kb, throwing);
                        if (eval instanceof Query query) {
                            queries.add(toOutcome(query, true));
                        }
                    } catch (NelumboTimeoutException tex) {
                        diagnostics.add(deadlineDiagnostic(deadlineMs));
                        break;
                    } catch (ParseException exc) {
                        diagnostics.add(toDiagnostic(exc));
                        if (eval instanceof Query query) {
                            queries.add(toOutcome(query, !isMismatch(exc)));
                        } else {
                            break; // a fact/rule failed to evaluate: later results can't be trusted
                        }
                    }
                }
            });
        } catch (NelumboTimeoutException tex) {
            diagnostics.add(deadlineDiagnostic(deadlineMs));
        }
        return new EvalResult(diagnostics.isEmpty(), diagnostics, queries);
    }

    private static boolean isMismatch(ParseException exc) {
        String msg = exc.getShortMessage();
        return msg != null && msg.startsWith("Expected result ");
    }

    private static Diagnostic deadlineDiagnostic(long deadlineMs) {
        return new Diagnostic(1, 1, 1, "evaluation exceeded the deadline of " + deadlineMs + " ms");
    }

    private static Diagnostic toDiagnostic(ParseException e) {
        return new Diagnostic(e.line() + 1, e.position() + 1, Math.max(e.length(), 1), e.getShortMessage());
    }

    private static QueryOutcome toOutcome(Query query, boolean matched) {
        StringBuffer sb = new StringBuffer();
        query.predicate().deparse(sb);
        InferResult ir = query.inferResult();
        Boolean expectation = query.hasExpected() ? matched : null;
        return new QueryOutcome(sb.toString().trim(), ir == null ? null : ir.toString(), expectation);
    }
}
```

If `eval.evaluate(kb, throwing)` does not compile because `Evaluatable.evaluate` has a different signature, open `lsp/server/src/main/java/org/modelingvalue/nelumbo/lsp/QueryEvaluator.java:74` and copy its exact call shape.

- [ ] **Step 4: Run the test to verify it passes**

Run: `./gradlew :test --tests "org.modelingvalue.nelumbo.test.NelumboEvaluatorTest"`
Expected: 5 tests PASS.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/org/modelingvalue/nelumbo/tools/NelumboEvaluator.java src/test/java/org/modelingvalue/nelumbo/test/NelumboEvaluatorTest.java
git commit -m "feat(core): NelumboEvaluator returns structured eval results"
```

---

### Task 2: Refactor NelumboCli onto NelumboEvaluator

**Files:**
- Modify: `src/main/java/org/modelingvalue/nelumbo/tools/NelumboCli.java:81-132`

- [ ] **Step 1: Replace runFile and delete report**

Replace the whole `runFile` method and delete the `report` method in `NelumboCli.java`; the imports of `KnowledgeBase`, `Node`, `Query`, `ParseException`, `Parser`, `ParserResult`, `Tokenizer` become unused and must be removed:

```java
    private static boolean runFile(String file, boolean quiet) {
        String source;
        String name;
        try {
            if ("-".equals(file)) {
                source = new String(System.in.readAllBytes(), StandardCharsets.UTF_8);
                name = "<stdin>";
            } else {
                source = Files.readString(Path.of(file), StandardCharsets.UTF_8);
                name = file;
            }
        } catch (NoSuchFileException e) {
            System.err.println(file + ": no such file");
            return false;
        } catch (IOException e) {
            System.err.println(file + ": " + e.getMessage());
            return false;
        }
        NelumboEvaluator.EvalResult result = NelumboEvaluator.evaluate(source, name, 0);
        for (NelumboEvaluator.Diagnostic d : result.diagnostics()) {
            System.err.println(name + ":" + d.line() + ":" + d.col() + ": " + d.message());
        }
        if (!quiet) {
            for (NelumboEvaluator.QueryOutcome q : result.queries()) {
                if (q.result() != null) {
                    System.out.println(q.query() + " ? " + q.result());
                }
            }
        }
        return result.ok();
    }
```

- [ ] **Step 2: Verify CLI behavior on a real file**

```bash
./gradlew cliJar
java -jar build/libs/nelumbo-*-cli.jar src/main/resources/org/modelingvalue/nelumbo/examples/family.nl; echo "exit=$?"
java -jar build/libs/nelumbo-*-cli.jar src/main/resources/org/modelingvalue/nelumbo/examples/fibonacci.nl -q; echo "exit=$?"
```

Expected: family.nl prints one `... ? ...` line per query and `exit=0`; fibonacci with `-q` prints nothing and `exit=0`.

- [ ] **Step 3: Run the core test suite**

Run: `./gradlew :test`
Expected: all core tests PASS (this guards the refactor).

- [ ] **Step 4: Commit**

```bash
git add src/main/java/org/modelingvalue/nelumbo/tools/NelumboCli.java
git commit -m "refactor(cli): NelumboCli delegates to NelumboEvaluator"
```

---

### Task 3: mcp Gradle module with bundled docs

**Files:**
- Modify: `settings.gradle.kts`
- Modify: `build.gradle.kts` (root, the `tasks.test` block near lines 135-138)
- Create: `mcp/build.gradle.kts`
- Create: `mcp/src/test/java/org/modelingvalue/nelumbo/mcp/DocIndexTest.java`

- [ ] **Step 1: Add the module to settings.gradle.kts**

After the existing `include("http")` line add:

```kotlin
include("mcp")
```

- [ ] **Step 2: Chain :mcp:test into the root test task**

In root `build.gradle.kts`, in the existing `tasks.test { ... }` block that already contains `dependsOn(":lsp:server:test")` and `dependsOn(":http:test")`, add:

```kotlin
    dependsOn(":mcp:test")
```

- [ ] **Step 3: Write mcp/build.gradle.kts**

`.kts` build files carry the license header too (see `http/build.gradle.kts:1-15`); copy those 15 lines to the top, then the content below:

```kotlin
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    id("com.gradleup.shadow") version "9.4.3"
    java
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

val archiveName = "nelumbo-mcp-server"

repositories {
    mavenCentral()
    mavenLocal()
}

dependencies {
    implementation(project(":"))
    implementation("io.modelcontextprotocol.sdk:mcp:2.0.0")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.22.0")
    runtimeOnly("org.slf4j:slf4j-simple:2.0.16") // logs to stderr; stdout is the protocol channel

    testImplementation("org.junit.jupiter:junit-jupiter:6.0.3")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
    jvmArgs("-ea")
}

// Bundle the language documentation into the jar for the search_docs tool, plus an
// index.txt so it can be enumerated from the classpath at runtime.
val copyDocs by tasks.registering(Sync::class) {
    from(rootProject.layout.projectDirectory.dir("docs")) {
        include("**/*.md")
        exclude("superpowers/**")
        exclude("site/**")
    }
    into(layout.buildDirectory.dir("generated-resources/nelumbo-docs"))
    doLast {
        val dir = destinationDir
        val index = dir.walkTopDown()
                .filter { it.isFile && it.extension == "md" }
                .map { it.relativeTo(dir).invariantSeparatorsPath }
                .sorted()
                .joinToString("\n")
        File(dir, "index.txt").writeText(index + "\n")
    }
}

// Registered as a source-set OUTPUT dir (same pattern as http's frontend bundle): on the
// runtime/test classpath and inside mcpJar, and kept out of any sources jar.
sourceSets.main {
    output.dir(mapOf("builtBy" to copyDocs), layout.buildDirectory.dir("generated-resources"))
}

tasks.register<ShadowJar>("mcpJar") {
    archiveBaseName.set(archiveName)
    archiveClassifier.set("")
    manifest {
        attributes["Main-Class"] = "org.modelingvalue.nelumbo.mcp.Main"
    }
    from(sourceSets.main.get().output)
    configurations = listOf(project.configurations.runtimeClasspath.get())
    exclude("META-INF/*.SF", "META-INF/*.DSA", "META-INF/*.RSA")
    // mergeServiceFiles is REQUIRED: the MCP SDK discovers its JSON mapper via ServiceLoader
    mergeServiceFiles()
}

tasks.shadowJar {
    enabled = false
}

tasks.jar {
    enabled = false
}
```

If `io.modelcontextprotocol.sdk:mcp:2.0.0` fails to resolve, check the latest version on https://central.sonatype.com/artifact/io.modelcontextprotocol.sdk/mcp and use that; the API below was verified against 2.0.0.

- [ ] **Step 4: Write the failing smoke test**

`mcp/src/test/java/org/modelingvalue/nelumbo/mcp/DocIndexTest.java` (license header, then):

```java
package org.modelingvalue.nelumbo.mcp;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

public class DocIndexTest {

    @Test
    public void docsAreBundledWithAnIndex() throws IOException {
        try (InputStream in = DocIndexTest.class.getResourceAsStream("/nelumbo-docs/index.txt")) {
            assertNotNull(in, "nelumbo-docs/index.txt missing from classpath");
            String index = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            assertTrue(index.contains("reference/grammar.md"), index);
            assertTrue(index.lines().count() > 10, index);
        }
        assertNotNull(DocIndexTest.class.getResourceAsStream("/nelumbo-docs/reference/grammar.md"));
    }
}
```

- [ ] **Step 5: Run it**

Run: `./gradlew :mcp:test --tests "org.modelingvalue.nelumbo.mcp.DocIndexTest"`
Expected: PASS (the copyDocs wiring makes it pass immediately; if it fails with a missing resource, the `sourceSets.main` output-dir registration is wrong).

- [ ] **Step 6: Commit**

```bash
git add settings.gradle.kts build.gradle.kts mcp/build.gradle.kts mcp/src/test/java/org/modelingvalue/nelumbo/mcp/DocIndexTest.java
git commit -m "build(mcp): new mcp module with bundled language docs"
```

---

### Task 4: DocSearch

**Files:**
- Create: `mcp/src/test/java/org/modelingvalue/nelumbo/mcp/DocSearchTest.java`
- Create: `mcp/src/main/java/org/modelingvalue/nelumbo/mcp/DocSearch.java`

- [ ] **Step 1: Write the failing test**

```java
package org.modelingvalue.nelumbo.mcp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.modelingvalue.nelumbo.mcp.DocSearch.Match;

public class DocSearchTest {

    private static DocSearch fake() {
        Map<String, String> docs = new LinkedHashMap<>();
        docs.put("a.md", """
                # Alpha
                Nothing relevant here.
                ## Precedence rules
                Functor alternatives on Root subtypes need explicit precedence.
                Precedence is written as #N.
                """);
        docs.put("b.md", """
                # Beta
                This mentions precedence once.
                """);
        return new DocSearch(docs);
    }

    @Test
    public void ranksSectionWithMostHitsFirst() {
        List<Match> ms = fake().search("precedence");
        assertFalse(ms.isEmpty());
        assertEquals("a.md", ms.get(0).doc());
        assertEquals("Precedence rules", ms.get(0).heading());
        assertTrue(ms.get(0).snippet().contains("#N"));
    }

    @Test
    public void noHitsGivesEmptyList() {
        assertTrue(fake().search("zzzznothing").isEmpty());
    }

    @Test
    public void realBundledDocsAreSearchable() {
        List<Match> ms = new DocSearch().search("pattern");
        assertFalse(ms.isEmpty());
        assertTrue(ms.size() <= 5);
    }
}
```

- [ ] **Step 2: Run to verify it fails**

Run: `./gradlew :mcp:test --tests "org.modelingvalue.nelumbo.mcp.DocSearchTest"`
Expected: compile error, `DocSearch` does not exist.

- [ ] **Step 3: Implement DocSearch**

```java
package org.modelingvalue.nelumbo.mcp;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Keyword search over the markdown docs bundled under /nelumbo-docs/ (see copyDocs in build.gradle.kts). */
public final class DocSearch {

    private record Section(String doc, String heading, String body) {
    }

    public record Match(String doc, String heading, String snippet, int score) {
    }

    private static final int MAX_RESULTS = 5;
    private static final int SNIPPET_LENGTH = 800;

    private final List<Section> sections = new ArrayList<>();

    /** Loads the bundled docs from the classpath. */
    public DocSearch() {
        this(loadBundledDocs());
    }

    /** Visible for tests: doc name -> markdown content. */
    DocSearch(Map<String, String> docs) {
        docs.forEach((doc, content) -> {
            String heading = doc;
            StringBuilder body = new StringBuilder();
            for (String line : content.split("\n", -1)) {
                if (line.startsWith("#")) {
                    addSection(doc, heading, body);
                    heading = line.replaceFirst("^#+\\s*", "").trim();
                    body = new StringBuilder();
                } else {
                    body.append(line).append('\n');
                }
            }
            addSection(doc, heading, body);
        });
    }

    private void addSection(String doc, String heading, StringBuilder body) {
        String text = body.toString().strip();
        if (!text.isEmpty() || !heading.equals(doc)) {
            sections.add(new Section(doc, heading, text));
        }
    }

    private static Map<String, String> loadBundledDocs() {
        Map<String, String> docs = new LinkedHashMap<>();
        try {
            for (String path : read("/nelumbo-docs/index.txt").split("\n")) {
                if (!path.isBlank()) {
                    docs.put(path, read("/nelumbo-docs/" + path));
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return docs;
    }

    private static String read(String resource) throws IOException {
        try (InputStream in = DocSearch.class.getResourceAsStream(resource)) {
            if (in == null) {
                throw new IOException("missing classpath resource " + resource);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    public List<Match> search(String query) {
        String[] terms = query.toLowerCase().split("\\s+");
        List<Match> matches = new ArrayList<>();
        for (Section s : sections) {
            String headingLower = s.heading().toLowerCase();
            String bodyLower = s.body().toLowerCase();
            int score = 0;
            for (String term : terms) {
                if (term.isBlank()) {
                    continue;
                }
                score += 5 * count(headingLower, term) + count(bodyLower, term);
            }
            if (score > 0) {
                String snippet = s.body().length() <= SNIPPET_LENGTH ? s.body() : s.body().substring(0, SNIPPET_LENGTH) + "...";
                matches.add(new Match(s.doc(), s.heading(), snippet, score));
            }
        }
        matches.sort(Comparator.comparingInt(Match::score).reversed());
        return matches.size() <= MAX_RESULTS ? matches : matches.subList(0, MAX_RESULTS);
    }

    private static int count(String text, String term) {
        int n = 0;
        for (int i = text.indexOf(term); i >= 0; i = text.indexOf(term, i + term.length())) {
            n++;
        }
        return n;
    }
}
```

- [ ] **Step 4: Run to verify it passes**

Run: `./gradlew :mcp:test --tests "org.modelingvalue.nelumbo.mcp.DocSearchTest"`
Expected: 3 tests PASS.

- [ ] **Step 5: Commit**

```bash
git add mcp/src/main/java/org/modelingvalue/nelumbo/mcp/DocSearch.java mcp/src/test/java/org/modelingvalue/nelumbo/mcp/DocSearchTest.java
git commit -m "feat(mcp): keyword search over bundled docs"
```

---

### Task 5: ExampleCatalog

**Files:**
- Create: `mcp/src/test/java/org/modelingvalue/nelumbo/mcp/ExampleCatalogTest.java`
- Create: `mcp/src/main/java/org/modelingvalue/nelumbo/mcp/ExampleCatalog.java`

The `.nl` corpus is already on the classpath via the root-project dependency (they live in the root jar's resources).

- [ ] **Step 1: Write the failing test**

```java
package org.modelingvalue.nelumbo.mcp;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.modelingvalue.nelumbo.mcp.ExampleCatalog.Example;

public class ExampleCatalogTest {

    @Test
    public void listContainsKnownExamplesWithDescriptions() {
        List<Example> all = ExampleCatalog.list();
        assertTrue(all.stream().anyMatch(e -> e.name().equals("family")));
        assertTrue(all.stream().anyMatch(e -> e.name().equals("langOnly")));
        assertTrue(all.stream().allMatch(e -> e.description() != null && !e.description().isBlank()));
    }

    @Test
    public void everyEntryResolvesOnTheClasspath() {
        for (Example e : ExampleCatalog.list()) {
            assertNotNull(ExampleCatalog.content(e.name()), e.name());
        }
    }

    @Test
    public void contentOfFamily() {
        String content = ExampleCatalog.content("family");
        assertNotNull(content);
        assertTrue(content.contains("pc(Hendrik, Juliana)"));
    }

    @Test
    public void unknownNameGivesNull() {
        assertNull(ExampleCatalog.content("no-such-example"));
    }
}
```

- [ ] **Step 2: Run to verify it fails**

Run: `./gradlew :mcp:test --tests "org.modelingvalue.nelumbo.mcp.ExampleCatalogTest"`
Expected: compile error.

- [ ] **Step 3: Implement ExampleCatalog**

The description is derived from the file itself (first `//` comment line, else a generic line) so it never lies about content.

```java
package org.modelingvalue.nelumbo.mcp;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** The bundled .nl corpus (from the core jar's resources), listed and served by name. */
public final class ExampleCatalog {

    public record Example(String name, String resourcePath, String description) {
    }

    private static final String BASE = "/org/modelingvalue/nelumbo/";

    private static final Map<String, String> ENTRIES = new LinkedHashMap<>();
    static {
        for (String ex : new String[]{"family", "fibonacci", "belasting", "deHet", "whoIs", "friends", "even", "max",
                "maxFib", "power", "ternary", "scoping", "koningsdag", "hidden", "queryOnly", "transformation",
                "familyAssignment", "ternaryAssignment", "evenAssignment", "maxAssignment", "maxFibAssignment",
                "powerAssignment", "transformationAssignment"}) {
            ENTRIES.put(ex, BASE + "examples/" + ex + ".nl");
        }
        ENTRIES.put("langOnly", BASE + "tests/langOnly.nl");
        ENTRIES.put("logicTest", BASE + "tests/logicTest.nl");
        ENTRIES.put("lang", BASE + "lang/lang.nl");
        ENTRIES.put("logic", BASE + "logic/logic.nl");
        ENTRIES.put("integers", BASE + "integers/integers.nl");
        ENTRIES.put("strings", BASE + "strings/strings.nl");
        ENTRIES.put("collections", BASE + "collections/collections.nl");
        ENTRIES.put("rationals", BASE + "rationals/rationals.nl");
        ENTRIES.put("datetime", BASE + "datetime/datetime.nl");
    }

    private ExampleCatalog() {
    }

    public static List<Example> list() {
        List<Example> result = new ArrayList<>();
        ENTRIES.forEach((name, path) -> result.add(new Example(name, path, describe(read(path)))));
        return result;
    }

    /** @return the full source of the named example, or null if unknown. */
    public static String content(String name) {
        String path = ENTRIES.get(name);
        return path == null ? null : read(path);
    }

    private static String describe(String content) {
        if (content == null) {
            return "unreadable";
        }
        for (String line : content.split("\n")) {
            String t = line.strip();
            if (t.startsWith("//")) {
                return t.substring(2).strip();
            }
            if (!t.isEmpty()) {
                return "starts with: " + (t.length() > 60 ? t.substring(0, 60) + "..." : t);
            }
        }
        return "empty";
    }

    private static String read(String path) {
        try (InputStream in = ExampleCatalog.class.getResourceAsStream(path)) {
            return in == null ? null : new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            return null;
        }
    }
}
```

- [ ] **Step 4: Run to verify it passes**

Run: `./gradlew :mcp:test --tests "org.modelingvalue.nelumbo.mcp.ExampleCatalogTest"`
Expected: 4 tests PASS. If `everyEntryResolvesOnTheClasspath` fails, a hardcoded path has a typo; the authoritative list is `find src/main/resources -name "*.nl"` in the repo root.

- [ ] **Step 5: Commit**

```bash
git add mcp/src/main/java/org/modelingvalue/nelumbo/mcp/ExampleCatalog.java mcp/src/test/java/org/modelingvalue/nelumbo/mcp/ExampleCatalogTest.java
git commit -m "feat(mcp): example catalog backend for get_example"
```

---

### Task 6: Hints

**Files:**
- Create: `mcp/src/test/java/org/modelingvalue/nelumbo/mcp/HintsTest.java`
- Create: `mcp/src/main/java/org/modelingvalue/nelumbo/mcp/Hints.java`

- [ ] **Step 1: Write the failing test**

```java
package org.modelingvalue.nelumbo.mcp;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.modelingvalue.nelumbo.mcp.Hints.Hint;

public class HintsTest {

    @Test
    public void newlineTokenErrorGetsPrecedenceHint() {
        Hint h = Hints.hintFor("Unexpected token '\n', expected '('");
        assertTrue(h.text().contains("#0"));
        assertTrue(h.docRef().contains("precedence"));
    }

    @Test
    public void expectationMismatchGetsExpectedResultHint() {
        Hint h = Hints.hintFor("Expected result [[(f=99)],true,[],true], found [[(f=5)],true,[],true]");
        assertTrue(h.text().contains("expected result"));
    }

    @Test
    public void otherUnexpectedTokenGetsGrammarHint() {
        Hint h = Hints.hintFor("Unexpected token 'blarg', expected NAME");
        assertTrue(h.docRef().contains("grammar"));
    }

    @Test
    public void unknownMessageGetsNoHint() {
        assertNull(Hints.hintFor("something else entirely"));
    }
}
```

- [ ] **Step 2: Run to verify it fails**

Run: `./gradlew :mcp:test --tests "org.modelingvalue.nelumbo.mcp.HintsTest"`
Expected: compile error.

- [ ] **Step 3: Implement Hints**

```java
package org.modelingvalue.nelumbo.mcp;

/**
 * Curated hints for known LLM failure modes when authoring .nl files. Matched on the
 * diagnostic message; ordered most-specific first. Grow this table as new failure
 * modes are observed in practice.
 */
public final class Hints {

    public record Hint(String text, String docRef) {
    }

    private Hints() {
    }

    public static Hint hintFor(String message) {
        if (message == null) {
            return null;
        }
        if (message.startsWith("Expected result ")) {
            return new Hint("The model parsed and ran, but this query's result differs from the expected result "
                    + "written after '?'. The message shows expected vs found. Expected results have the form "
                    + "[(bindings)][(counterBindings)] where [..] means 'and nothing else'. Either the rules/facts "
                    + "are wrong or the expectation is.", "reference/test-expression-semantics.md");
        }
        if (message.startsWith("Unexpected token '\n'") || message.startsWith("Unexpected token '\\n'")) {
            return new Hint("Likely the known precedence gotcha: when a functor is declared on a Root subtype "
                    + "(X ::= ... where X extends Root), every alternative that contains repetition, optional or "
                    + "alternation patterns needs an explicit precedence suffix - append '#0' to each alternative "
                    + "of the ::= declaration. Later errors in the file usually cascade from this one.",
                    "reference/precedence-and-associativity.md");
        }
        if (message.startsWith("Unexpected token ")) {
            return new Hint("The parser expected one of the listed tokens at this position. Check the pattern "
                    + "(::=) declaration this statement should match, and compare with a working example via "
                    + "get_example.", "reference/grammar.md");
        }
        return null;
    }
}
```

Note: before finalizing the first hint condition, check the real message shape once:
`printf 'Root ::= stuff <NAME+>\nstuff a b c\n' | java -jar build/libs/nelumbo-*-cli.jar -` and look at stderr; if the newline in the message is rendered differently (e.g. `'
'`), adjust the `startsWith` conditions AND the test to the real shape.

- [ ] **Step 4: Run to verify it passes**

Run: `./gradlew :mcp:test --tests "org.modelingvalue.nelumbo.mcp.HintsTest"`
Expected: 4 tests PASS.

- [ ] **Step 5: Commit**

```bash
git add mcp/src/main/java/org/modelingvalue/nelumbo/mcp/Hints.java mcp/src/test/java/org/modelingvalue/nelumbo/mcp/HintsTest.java
git commit -m "feat(mcp): curated hint table for enriched diagnostics"
```

---

### Task 7: ModelSkeleton

**Files:**
- Create: `mcp/src/test/java/org/modelingvalue/nelumbo/mcp/ModelSkeletonTest.java`
- Create: `mcp/src/main/java/org/modelingvalue/nelumbo/mcp/ModelSkeleton.java`

- [ ] **Step 1: Write the failing test**

The contract: the skeleton must be a VALID, self-verifying .nl file.

```java
package org.modelingvalue.nelumbo.mcp;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.modelingvalue.nelumbo.tools.NelumboEvaluator;
import org.modelingvalue.nelumbo.tools.NelumboEvaluator.EvalResult;

public class ModelSkeletonTest {

    @Test
    public void skeletonEvaluatesCleanly() {
        String nl = ModelSkeleton.skeleton("Loan eligibility");
        assertTrue(nl.contains("Loan eligibility"));
        EvalResult r = NelumboEvaluator.evaluate(nl, "skeleton.nl", 30_000);
        assertTrue(r.ok(), () -> "diagnostics: " + r.diagnostics());
        assertFalse(r.queries().isEmpty());
        assertTrue(r.queries().stream().allMatch(q -> Boolean.TRUE.equals(q.expectationMatched())));
    }

    @Test
    public void blankTitleGetsDefault() {
        assertTrue(ModelSkeleton.skeleton(" ").contains("Decision model"));
    }
}
```

- [ ] **Step 2: Run to verify it fails**

Run: `./gradlew :mcp:test --tests "org.modelingvalue.nelumbo.mcp.ModelSkeletonTest"`
Expected: compile error.

- [ ] **Step 3: Implement ModelSkeleton**

Every construct below is taken from working examples (`belasting.nl`, `family.nl`, `fibonacci.nl`): `FactType ::=`, `Boolean ::=` decision functors, `E[n](...)`, `fact ...`, `? [()][]` / `? [][()]` / `? [(p=X)][..]` expectations.

```java
package org.modelingvalue.nelumbo.mcp;

/** Skeleton for a new self-contained decision model; must always evaluate cleanly (see test). */
public final class ModelSkeleton {

    private ModelSkeleton() {
    }

    public static String skeleton(String title) {
        String t = title == null || title.isBlank() ? "Decision model" : title.strip();
        return """
                // %s
                // Skeleton decision model. Replace each section with your domain, then
                // verify with eval_nl until ok=true. Keep the queries: they are the tests.

                import  nelumbo.integers

                // -- Types: domain concepts, rooted in Object -------------------------
                Person   :: Object

                // -- Fact types: the case data asserted as facts below ----------------
                FactType ::= age(<Person>,<Integer>)

                // -- Decisions: Boolean functors derived by rules ---------------------
                Boolean  ::= eligible(<Person>)

                // -- Variables used in rules and queries ------------------------------
                Person  p
                Integer n

                // -- Rules: <=> defines when a decision holds -------------------------
                eligible(p) <=> E[n](age(p,n) & n>=18)

                // -- Instances and facts: concrete cases ------------------------------
                Person ::= Alice, Bob

                fact age(Alice, 34),
                     age(Bob, 15)

                // -- Queries with expected results: [()][] = true, [][()] = false, ----
                // -- [(p=X)][..] = exactly these bindings ------------------------------
                eligible(Alice) ? [()][]
                eligible(Bob)   ? [][()]
                eligible(p)     ? [(p=Alice)][..]
                """.formatted(t);
    }
}
```

- [ ] **Step 4: Run to verify it passes**

Run: `./gradlew :mcp:test --tests "org.modelingvalue.nelumbo.mcp.ModelSkeletonTest"`
Expected: 2 tests PASS.

If `skeletonEvaluatesCleanly` fails: the diagnostics in the assertion message say exactly what the parser rejected. Debug the skeleton (not the test) against the working examples: `src/main/resources/org/modelingvalue/nelumbo/examples/belasting.nl` (FactType/Boolean/E[..] idiom) and `family.nl` (fact/query idiom), and `docs/reference/stdlib/integers.md` for the comparison operators. Iterate quickly by piping candidate content through the CLI: `java -jar build/libs/nelumbo-*-cli.jar -`.

- [ ] **Step 5: Commit**

```bash
git add mcp/src/main/java/org/modelingvalue/nelumbo/mcp/ModelSkeleton.java mcp/src/test/java/org/modelingvalue/nelumbo/mcp/ModelSkeletonTest.java
git commit -m "feat(mcp): self-verifying decision-model skeleton for new_model"
```

---

### Task 8: NelumboTools (the four handlers, SDK-free)

**Files:**
- Create: `mcp/src/test/java/org/modelingvalue/nelumbo/mcp/NelumboToolsTest.java`
- Create: `mcp/src/main/java/org/modelingvalue/nelumbo/mcp/NelumboTools.java`

- [ ] **Step 1: Write the failing test**

```java
package org.modelingvalue.nelumbo.mcp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

public class NelumboToolsTest {

    private final NelumboTools tools = new NelumboTools(10_000);

    @Test
    @SuppressWarnings("unchecked")
    public void evalNlOnGoodModel() {
        Map<String, Object> r = tools.evalNl(ModelSkeleton.skeleton("t"), "t.nl");
        assertEquals(Boolean.TRUE, r.get("ok"));
        assertTrue(((List<Object>) r.get("diagnostics")).isEmpty());
        List<Map<String, Object>> queries = (List<Map<String, Object>>) r.get("queryResults");
        assertEquals(3, queries.size());
        assertEquals(Boolean.TRUE, queries.get(0).get("expectationMatched"));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void evalNlEnrichesDiagnostics() {
        Map<String, Object> r = tools.evalNl("flurb @@ blarg\n", "bad.nl");
        assertEquals(Boolean.FALSE, r.get("ok"));
        List<Map<String, Object>> ds = (List<Map<String, Object>>) r.get("diagnostics");
        Map<String, Object> d = ds.get(0);
        assertEquals("flurb @@ blarg", d.get("sourceLine"));
        assertNotNull(d.get("caret"));
        assertTrue(((Integer) d.get("line")) >= 1);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void evalNlAddsCascadeNoteWhenManyDiagnostics() {
        String bad = "flurb @@ a\nflurb @@ b\nflurb @@ c\nflurb @@ d\nflurb @@ e\n";
        Map<String, Object> r = tools.evalNl(bad, "cascade.nl");
        List<Map<String, Object>> ds = (List<Map<String, Object>>) r.get("diagnostics");
        if (ds.size() > 3) {
            assertTrue(String.valueOf(ds.get(0).get("hint")).contains("cascade"));
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    public void searchDocsReturnsResults() {
        Map<String, Object> r = tools.searchDocs("precedence");
        assertTrue(((List<Object>) r.get("results")).size() > 0);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void getExampleListsAndFetches() {
        Map<String, Object> listing = tools.getExample(null);
        assertTrue(((List<Object>) listing.get("examples")).size() > 10);
        Map<String, Object> one = tools.getExample("family");
        assertTrue(String.valueOf(one.get("content")).contains("pc(Hendrik, Juliana)"));
        Map<String, Object> unknown = tools.getExample("nope");
        assertNotNull(unknown.get("error"));
    }

    @Test
    public void newModelReturnsSkeleton() {
        Map<String, Object> r = tools.newModel("Loan check");
        assertTrue(String.valueOf(r.get("content")).contains("Loan check"));
    }
}
```

- [ ] **Step 2: Run to verify it fails**

Run: `./gradlew :mcp:test --tests "org.modelingvalue.nelumbo.mcp.NelumboToolsTest"`
Expected: compile error.

- [ ] **Step 3: Implement NelumboTools**

```java
package org.modelingvalue.nelumbo.mcp;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.modelingvalue.nelumbo.mcp.Hints.Hint;
import org.modelingvalue.nelumbo.tools.NelumboEvaluator;
import org.modelingvalue.nelumbo.tools.NelumboEvaluator.Diagnostic;
import org.modelingvalue.nelumbo.tools.NelumboEvaluator.EvalResult;
import org.modelingvalue.nelumbo.tools.NelumboEvaluator.QueryOutcome;

/**
 * The four MCP tool handlers, free of any MCP SDK types: each returns a JSON-ready
 * Map. Main serializes them and owns all protocol concerns.
 */
public final class NelumboTools {

    private static final int CASCADE_THRESHOLD = 3;

    private final long deadlineMs;
    private final DocSearch docSearch;

    public NelumboTools(long deadlineMs) {
        this(deadlineMs, new DocSearch());
    }

    NelumboTools(long deadlineMs, DocSearch docSearch) {
        this.deadlineMs = deadlineMs;
        this.docSearch = docSearch;
    }

    public Map<String, Object> evalNl(String content, String name) {
        EvalResult result = NelumboEvaluator.evaluate(content, name == null || name.isBlank() ? "model.nl" : name, deadlineMs);
        String[] lines = content.split("\n", -1);
        List<Map<String, Object>> diagnostics = new ArrayList<>();
        for (Diagnostic d : result.diagnostics()) {
            diagnostics.add(toDiagnosticMap(d, lines));
        }
        if (diagnostics.size() > CASCADE_THRESHOLD) {
            Map<String, Object> first = diagnostics.get(0);
            String note = "Note: " + (diagnostics.size() - 1) + " further errors follow; they usually cascade from this first one - fix it and re-evaluate.";
            first.merge("hint", note, (old, n) -> old + " " + n);
        }
        List<Map<String, Object>> queries = new ArrayList<>();
        for (QueryOutcome q : result.queries()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("query", q.query());
            m.put("result", q.result());
            if (q.expectationMatched() != null) {
                m.put("expectationMatched", q.expectationMatched());
            }
            queries.add(m);
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("ok", result.ok());
        out.put("diagnostics", diagnostics);
        out.put("queryResults", queries);
        return out;
    }

    private static Map<String, Object> toDiagnosticMap(Diagnostic d, String[] lines) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("line", d.line());
        m.put("col", d.col());
        m.put("message", d.message());
        if (d.line() >= 1 && d.line() <= lines.length) {
            String sourceLine = lines[d.line() - 1];
            m.put("sourceLine", sourceLine);
            int col = Math.min(Math.max(d.col(), 1), sourceLine.length() + 1);
            m.put("caret", " ".repeat(col - 1) + "^".repeat(Math.max(d.length(), 1)));
        }
        Hint hint = Hints.hintFor(d.message());
        if (hint != null) {
            m.put("hint", hint.text());
            m.put("docRef", hint.docRef());
        }
        return m;
    }

    public Map<String, Object> searchDocs(String query) {
        List<Map<String, Object>> results = new ArrayList<>();
        for (DocSearch.Match match : docSearch.search(query == null ? "" : query)) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("doc", match.doc());
            m.put("heading", match.heading());
            m.put("snippet", match.snippet());
            results.add(m);
        }
        return Map.of("results", results);
    }

    public Map<String, Object> getExample(String name) {
        if (name == null || name.isBlank()) {
            List<Map<String, Object>> examples = new ArrayList<>();
            for (ExampleCatalog.Example e : ExampleCatalog.list()) {
                examples.add(Map.of("name", e.name(), "description", e.description()));
            }
            return Map.of("examples", examples);
        }
        String content = ExampleCatalog.content(name);
        if (content == null) {
            List<String> names = ExampleCatalog.list().stream().map(ExampleCatalog.Example::name).toList();
            return Map.of("error", "unknown example '" + name + "'", "available", names);
        }
        return Map.of("name", name, "content", content);
    }

    public Map<String, Object> newModel(String title) {
        return Map.of("content", ModelSkeleton.skeleton(title));
    }
}
```

- [ ] **Step 4: Run to verify it passes**

Run: `./gradlew :mcp:test --tests "org.modelingvalue.nelumbo.mcp.NelumboToolsTest"`
Expected: 6 tests PASS. (The 3-query assertion in `evalNlOnGoodModel` matches the skeleton's three queries; if Task 7's skeleton changed, adjust the count.)

- [ ] **Step 5: Commit**

```bash
git add mcp/src/main/java/org/modelingvalue/nelumbo/mcp/NelumboTools.java mcp/src/test/java/org/modelingvalue/nelumbo/mcp/NelumboToolsTest.java
git commit -m "feat(mcp): eval_nl/search_docs/get_example/new_model handlers"
```

---

### Task 9: Main (stdio server) and the mcpJar smoke test

**Files:**
- Create: `mcp/src/main/java/org/modelingvalue/nelumbo/mcp/Main.java`

The MCP Java SDK 2.0.0 API used below was verified against the released source: `StdioServerTransportProvider(McpJsonMapper, InputStream, OutputStream)`, `McpJsonDefaults.getMapper()`, `Tool.builder(name, Map inputSchema)`, `SyncToolSpecification.builder().tool(...).callHandler((exchange, request) -> ...)` with `request.arguments()` a `Map<String,Object>`, `CallToolResult.builder().addTextContent(...).isError(...)`.

- [ ] **Step 1: Implement Main**

```java
package org.modelingvalue.nelumbo.mcp;

import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.modelcontextprotocol.json.McpJsonDefaults;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.server.transport.StdioServerTransportProvider;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.Tool;

/**
 * MCP stdio server for authoring Nelumbo decision models. One session per process;
 * register with any MCP client as: java -jar nelumbo-mcp-server.jar [--eval-deadline-ms N]
 */
public final class Main {

    private static final ObjectMapper JSON = new ObjectMapper();

    private static final String PRIMER = """
            Nelumbo (.nl) syntax essentials:
              type declarations:   Person :: Object
              pattern definitions: Integer ::= fib(<Integer>)
              instances:           Person ::= Alice, Bob
              variables:           Integer n, f
              rules:               fib(n)=f <=> f=n if n>=0 & n<=1, f=fib(n-1)+fib(n-2) if n>1
              fact types:          FactType ::= pc(<Person>,<Person>)
              facts:               fact pc(Alice, Bob)
              queries + expected:  fib(5)=f ? [(f=5)][..]
              logic:               & (and), | (or), E[x](..) exists, A[x](..) forall
              imports:             import nelumbo.integers (also: strings, collections, rationals, datetime, logic)
            Workflow: start from new_model, look up syntax with search_docs / get_example,
            and iterate with eval_nl until ok=true with all expectations matched.
            """;

    private Main() {
    }

    public static void main(String[] args) {
        long deadlineMs = 10_000;
        for (int i = 0; i < args.length - 1; i++) {
            if ("--eval-deadline-ms".equals(args[i])) {
                deadlineMs = Long.parseLong(args[i + 1]);
            }
        }
        NelumboTools tools = new NelumboTools(deadlineMs);
        StdioServerTransportProvider transport = new StdioServerTransportProvider(McpJsonDefaults.getMapper(), System.in, System.out);
        // From here on the real stdout belongs to JSON-RPC: reroute everything else
        // (nelumbo TRACE output, stray prints) to stderr.
        System.setOut(new PrintStream(new FileOutputStream(FileDescriptor.err), true));
        McpServer.sync(transport)
                .serverInfo("nelumbo", "0.1.0")
                .capabilities(McpSchema.ServerCapabilities.builder().tools(true).build())
                .tools(evalNlTool(tools), searchDocsTool(tools), getExampleTool(tools), newModelTool(tools))
                .build();
        // The stdio transport owns non-daemon threads; the JVM stays alive until stdin closes.
    }

    private static McpServerFeatures.SyncToolSpecification evalNlTool(NelumboTools tools) {
        Map<String, Object> schema = Map.of(
                "type", "object",
                "properties", Map.of(
                        "content", Map.of("type", "string", "description", "the complete .nl source to parse and evaluate"),
                        "name", Map.of("type", "string", "description", "optional display name, e.g. model.nl")),
                "required", List.of("content"));
        return tool(Tool.builder("eval_nl", schema)
                        .description("Parse and evaluate a self-contained Nelumbo (.nl) decision model. Returns ok, "
                                + "diagnostics (line/col/message/sourceLine/caret, plus hint and docRef for known "
                                + "traps) and per-query results incl. whether embedded expected results matched.\n\n" + PRIMER)
                        .build(),
                args -> tools.evalNl((String) args.get("content"), (String) args.get("name")));
    }

    private static McpServerFeatures.SyncToolSpecification searchDocsTool(NelumboTools tools) {
        Map<String, Object> schema = Map.of(
                "type", "object",
                "properties", Map.of("query", Map.of("type", "string", "description", "keywords to search for")),
                "required", List.of("query"));
        return tool(Tool.builder("search_docs", schema)
                        .description("Keyword search over the bundled Nelumbo language documentation; returns the "
                                + "best-matching sections. Use before guessing syntax.")
                        .build(),
                args -> tools.searchDocs((String) args.get("query")));
    }

    private static McpServerFeatures.SyncToolSpecification getExampleTool(NelumboTools tools) {
        Map<String, Object> schema = Map.of(
                "type", "object",
                "properties", Map.of("name", Map.of("type", "string", "description", "example name; omit to list all")));
        return tool(Tool.builder("get_example", schema)
                        .description("Without a name: list the bundled working .nl examples. With a name: return the "
                                + "full source. Working examples are the best syntax reference.")
                        .build(),
                args -> tools.getExample((String) args.get("name")));
    }

    private static McpServerFeatures.SyncToolSpecification newModelTool(NelumboTools tools) {
        Map<String, Object> schema = Map.of(
                "type", "object",
                "properties", Map.of("title", Map.of("type", "string", "description", "title of the decision model")));
        return tool(Tool.builder("new_model", schema)
                        .description("Return a commented, self-verifying skeleton .nl decision model to edit, instead "
                                + "of starting from a blank page.")
                        .build(),
                args -> tools.newModel((String) args.get("title")));
    }

    private static McpServerFeatures.SyncToolSpecification tool(Tool tool, Function<Map<String, Object>, Map<String, Object>> handler) {
        return McpServerFeatures.SyncToolSpecification.builder()
                .tool(tool)
                .callHandler((exchange, request) -> {
                    try {
                        return CallToolResult.builder()
                                .addTextContent(JSON.writeValueAsString(handler.apply(request.arguments())))
                                .build();
                    } catch (Exception e) {
                        return CallToolResult.builder().addTextContent(e.toString()).isError(true).build();
                    }
                })
                .build();
    }
}
```

- [ ] **Step 2: Compile and run all mcp tests**

Run: `./gradlew :mcp:test`
Expected: all mcp tests PASS. If `Tool.builder` / `McpJsonDefaults` do not resolve, the SDK version differs from 2.0.0 - check the actual API in the resolved jar (`~/.gradle/caches/.../io.modelcontextprotocol.sdk/`) and adapt; do NOT downgrade to the deprecated String-schema constructors.

- [ ] **Step 3: Build the jar and smoke-test the protocol**

```bash
./gradlew :mcp:mcpJar
printf '%s\n' '{"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2025-06-18","capabilities":{},"clientInfo":{"name":"smoke","version":"0"}}}' \
  | java -jar mcp/build/libs/nelumbo-mcp-server-*.jar 2>/dev/null | head -1
```

Expected: exactly one line of JSON on stdout containing `"serverInfo"` and `"nelumbo"`. Nothing non-JSON may precede it (that would mean something wrote to stdout before the reroute).

- [ ] **Step 4: Optional end-to-end check with a real client**

If a `claude` CLI is available: `claude mcp add nelumbo-test -- java -jar $(pwd)/mcp/build/libs/nelumbo-mcp-server-*.jar`, then in a session ask it to create and verify a small decision model; remove with `claude mcp remove nelumbo-test`. Skip if unavailable - the smoke test above covers the wire format.

- [ ] **Step 5: Commit**

```bash
git add mcp/src/main/java/org/modelingvalue/nelumbo/mcp/Main.java
git commit -m "feat(mcp): stdio MCP server exposing the four authoring tools"
```

---

### Task 10: CI, docs, final verification

**Files:**
- Modify: `.github/workflows/build.yaml` (the "build" step's gradlew invocation, near line 57)
- Modify: `CLAUDE.md`

- [ ] **Step 1: Add mcpJar to the CI build**

In the `./gradlew --info \` invocation in the "build" step, add a line after `:http:serverJar \`:

```yaml
                        :mcp:mcpJar \
```

(`:mcp:test` already runs via the root `test` inside `build`.)

- [ ] **Step 2: Document the module in CLAUDE.md**

In the Module Structure tree add, after the `http` line:

```
├── mcp                     → MCP stdio server (official MCP Java SDK): tools eval_nl, search_docs, get_example, new_model for LLM authoring of .nl decision models
```

In Build Commands add:

```sh
./gradlew :mcp:mcpJar                    # MCP server shaded JAR (stdio; register as: java -jar nelumbo-mcp-server-<version>.jar)
```

And add a short section at the end:

```markdown
## MCP Module - LLM Authoring Tools

`mcp/` is an MCP stdio server (`org.modelingvalue.nelumbo.mcp.Main`, SDK `io.modelcontextprotocol.sdk:mcp`) for LLM authoring of self-contained `.nl` decision models. Tools: `eval_nl` (structured diagnostics via the core `NelumboEvaluator`, enriched by the curated `Hints` table, plus per-query expectation results), `search_docs` (keyword search over `docs/**/*.md`, bundled at build time with an `index.txt`), `get_example` (bundled `.nl` corpus), `new_model` (self-verifying skeleton, `ModelSkeleton` - its test evaluates the skeleton, so it must always stay valid). Handlers live SDK-free in `NelumboTools`; `Main` owns protocol wiring, the eval deadline (`--eval-deadline-ms`, default 10s) and reroutes `System.out` to stderr (stdout is the JSON-RPC channel). `NelumboCli` now delegates to `NelumboEvaluator` (`tools/NelumboEvaluator.java`). Design: `docs/superpowers/specs/2026-07-12-mcp-server-design.md`.
```

- [ ] **Step 3: Full verification**

```bash
./gradlew mvgCorrector
git status --short   # if mvgCorrector changed headers, include those files in the commit
./gradlew test :mcp:mcpJar cliJar
```

Expected: BUILD SUCCESSFUL; all module tests pass.

- [ ] **Step 4: Commit**

```bash
git add .github/workflows/build.yaml CLAUDE.md
git add -u   # any mvgCorrector header fixes
git commit -m "ci: build the mcp server jar; document the mcp module"
```

---

## Deviations from the spec (agreed refinements)

- Spec hint entry 4 ("suggest nearest declared name") is implemented as the general
  "Unexpected token, check the pattern declaration + grammar docRef" hint: the parser
  message alone does not carry the declared-name table, and a Levenshtein pass over the
  KB would be new core API. Revisit when real failure data shows it is needed.
- Jar is named `nelumbo-mcp-server-<version>.jar` (matching `nelumbo-http-server`), not
  `nelumbo-<version>-mcp.jar`.
