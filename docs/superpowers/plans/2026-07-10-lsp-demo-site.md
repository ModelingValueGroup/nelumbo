# LSP-Powered Demo Site Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Serve demo pages from the Nelumbo HTTP server where every editable field is a Monaco editor with full LSP features (over a `/lsp` WebSocket) plus on-demand execution via the existing `POST /eval`.

**Architecture:** The `lsp/server` module is made embeddable (per-instance `LanguageClient` instead of the static `Main.client`, injectable base `KnowledgeBase`, no `System.exit` on `exit`). The `http` module gains a `/lsp` WebSocket route that bridges Javalin ws frames to an LSP4J `RemoteEndpoint` (plain JSON per frame, the format `vscode-ws-jsonrpc` speaks), one fresh `NelumboLanguageServer` per connection seeded with the loaded KB. A small npm project bundles Monaco + monaco-languageclient into one JS file; pages mark fields with `<div class="nelumbo-field">`.

**Tech Stack:** Java 21, Javalin 6.3.0 (Jetty 11), LSP4J 1.0.0, Monaco + monaco-languageclient + vscode-ws-jsonrpc, esbuild, JUnit 6.

**Spec:** `docs/superpowers/specs/2026-07-10-lsp-demo-site-design.md`

**Discoveries during planning (spec addendum):**
- `NelumboLanguageServer.exit()` calls `System.exit(0)` — embedded sessions must not kill the host JVM. Fixed via an injectable exit handler.
- All client callbacks go through the static `Main.client` (`NlDocument`, `QueryResultCache`, `WorkspaceExecuteCommandService`, `U` progress helpers) — wrong client wins with concurrent sessions. Fixed by storing the client on `Workspace`.
- The LSP's debounced query auto-eval (`QueryResultCache` → `QueryEvaluator`) has **no timeout** — a public visitor typing `fib(100)` would burn CPU forever. Fixed by an `evalDeadlineMs` on `Workspace` (0 = disabled, preserving IDE behavior; the http server sets its own eval timeout value).
- `QueryResultCache`'s scheduler thread is never shut down — per-connection servers would leak threads. Fixed via `Workspace.dispose()`.
- `WsServer.java` in lsp/server is dead code (no usages); it is left untouched.

**Conventions for every task:** All Java files need the LGPL header (copy the first 15 lines from any existing file in the same module; `./gradlew mvgCorrector` fixes them too). Run commands from the repo root `/Users/tom/projects/mvg-nelumbo/nelumbo`. ASCII only in all new files.

---

### Task 1: Make NelumboLanguageServer embeddable

**Files:**
- Modify: `lsp/server/src/main/java/org/modelingvalue/nelumbo/lsp/Workspace.java`
- Modify: `lsp/server/src/main/java/org/modelingvalue/nelumbo/lsp/NelumboLanguageServer.java`
- Modify: `lsp/server/src/main/java/org/modelingvalue/nelumbo/lsp/QueryResultCache.java`
- Create: `lsp/server/src/test/java/org/modelingvalue/nelumbo/lsp/RecordingClient.java`
- Test: `lsp/server/src/test/java/org/modelingvalue/nelumbo/lsp/EmbeddedServerTest.java`

- [ ] **Step 1: Write the test helper `RecordingClient`**

`lsp/server/src/test/java/org/modelingvalue/nelumbo/lsp/RecordingClient.java` (add LGPL header):

```java
package org.modelingvalue.nelumbo.lsp;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.eclipse.lsp4j.MessageActionItem;
import org.eclipse.lsp4j.MessageParams;
import org.eclipse.lsp4j.PublishDiagnosticsParams;
import org.eclipse.lsp4j.ShowMessageRequestParams;
import org.eclipse.lsp4j.services.LanguageClient;

/** Test double: records diagnostics published by the server under test. */
public class RecordingClient implements LanguageClient {
    public final List<PublishDiagnosticsParams> diagnostics      = new CopyOnWriteArrayList<>();
    private final CountDownLatch                firstDiagnostics = new CountDownLatch(1);

    @Override
    public void telemetryEvent(Object object) {
    }

    @Override
    public void publishDiagnostics(PublishDiagnosticsParams params) {
        diagnostics.add(params);
        firstDiagnostics.countDown();
    }

    @Override
    public void showMessage(MessageParams params) {
    }

    @Override
    public CompletableFuture<MessageActionItem> showMessageRequest(ShowMessageRequestParams params) {
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public void logMessage(MessageParams params) {
    }

    @Override
    public CompletableFuture<Void> refreshInlayHints() {
        return CompletableFuture.completedFuture(null);
    }

    public boolean awaitDiagnostics(long seconds) throws InterruptedException {
        return firstDiagnostics.await(seconds, TimeUnit.SECONDS);
    }
}
```

- [ ] **Step 2: Write the failing test**

`lsp/server/src/test/java/org/modelingvalue/nelumbo/lsp/EmbeddedServerTest.java` (add LGPL header):

```java
package org.modelingvalue.nelumbo.lsp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;
import org.modelingvalue.nelumbo.KnowledgeBase;

public class EmbeddedServerTest {

    @Test
    public void embeddedConstructorSeedsWorkspace() {
        KnowledgeBase         kb     = KnowledgeBase.BASE.run(() -> {
        });
        NelumboLanguageServer server = new NelumboLanguageServer(kb, 1234, () -> {
        });
        assertSame(kb, server.getWorkspace().getBaseKnowledgeBase());
        assertEquals(1234, server.getWorkspace().getEvalDeadlineMs());
    }

    @Test
    public void connectStoresClientPerInstance() {
        NelumboLanguageServer serverA = new NelumboLanguageServer(KnowledgeBase.BASE, 0, () -> {
        });
        NelumboLanguageServer serverB = new NelumboLanguageServer(KnowledgeBase.BASE, 0, () -> {
        });
        RecordingClient       clientA = new RecordingClient();
        RecordingClient       clientB = new RecordingClient();
        serverA.connect(clientA);
        serverB.connect(clientB);
        assertSame(clientA, serverA.getWorkspace().getClient());
        assertSame(clientB, serverB.getWorkspace().getClient());
    }

    @Test
    public void defaultConstructorUsesBase() {
        assertSame(KnowledgeBase.BASE, new NelumboLanguageServer().getWorkspace().getBaseKnowledgeBase());
        assertEquals(0, new NelumboLanguageServer().getWorkspace().getEvalDeadlineMs());
    }
}
```

- [ ] **Step 3: Run test to verify it fails**

Run: `./gradlew :lsp:server:test --tests "org.modelingvalue.nelumbo.lsp.EmbeddedServerTest"`
Expected: COMPILATION FAILURE (`getBaseKnowledgeBase`, `getEvalDeadlineMs`, `getClient`, 3-arg constructor, `connect` do not exist).

- [ ] **Step 4: Add fields to Workspace**

In `Workspace.java`, add imports:

```java
import org.eclipse.lsp4j.services.LanguageClient;
import org.modelingvalue.nelumbo.KnowledgeBase;
```

Add fields after the existing `documentManager` field:

```java
    private       LanguageClient     client;
    private       KnowledgeBase      baseKnowledgeBase = KnowledgeBase.BASE;
    private       long               evalDeadlineMs;
```

Add accessors after `getDocumentManager()`:

```java
    public LanguageClient getClient() {
        return client;
    }

    public void setClient(LanguageClient client) {
        this.client = client;
    }

    public KnowledgeBase getBaseKnowledgeBase() {
        return baseKnowledgeBase;
    }

    public void setBaseKnowledgeBase(KnowledgeBase baseKnowledgeBase) {
        this.baseKnowledgeBase = baseKnowledgeBase;
    }

    public long getEvalDeadlineMs() {
        return evalDeadlineMs;
    }

    public void setEvalDeadlineMs(long evalDeadlineMs) {
        this.evalDeadlineMs = evalDeadlineMs;
    }

    /** Release per-instance resources (embedded servers create one Workspace per connection). */
    public void dispose() {
        if (documentManager != null) {
            documentManager.queryResultCache().shutdown();
        }
    }
```

Do NOT add `client`/`baseKnowledgeBase`/`evalDeadlineMs` to `equals`/`hashCode` (they compare configuration, not runtime wiring).

- [ ] **Step 5: Add `shutdown()` to QueryResultCache**

In `QueryResultCache.java`, after the `remove` method:

```java
    /** Stop the debounce scheduler; used when an embedded server's connection closes. */
    public void shutdown() {
        scheduler.shutdownNow();
    }
```

- [ ] **Step 6: Make NelumboLanguageServer embeddable**

In `NelumboLanguageServer.java`:

Add imports:

```java
import org.eclipse.lsp4j.services.LanguageClient;
import org.eclipse.lsp4j.services.LanguageClientAware;
import org.modelingvalue.nelumbo.KnowledgeBase;
```

Change the class declaration and add constructors + `connect`:

```java
public class NelumboLanguageServer implements LanguageServer, LanguageClientAware {
    private final Workspace          workspace = new Workspace();
    private final Runnable           exitHandler;
    @SuppressWarnings({"FieldCanBeLocal", "unused"})
    private       ClientCapabilities capabilities;

    public NelumboLanguageServer() {
        this.exitHandler = () -> System.exit(0);
    }

    /** Embedded use: seed documents with a custom base KB, bound query auto-eval, and never exit the host JVM. */
    public NelumboLanguageServer(KnowledgeBase baseKnowledgeBase, long evalDeadlineMs, Runnable exitHandler) {
        this.exitHandler = exitHandler;
        workspace.setBaseKnowledgeBase(baseKnowledgeBase);
        workspace.setEvalDeadlineMs(evalDeadlineMs);
    }

    @Override
    public void connect(LanguageClient client) {
        workspace.setClient(client);
    }
```

Replace the body of `exit()`:

```java
    @Override
    public void exit() {
        // a clean and quick shutdown:
        System.err.println("~~~ exit");
        exitHandler.run();
    }
```

- [ ] **Step 7: Run test to verify it passes**

Run: `./gradlew :lsp:server:test --tests "org.modelingvalue.nelumbo.lsp.EmbeddedServerTest"`
Expected: PASS (3 tests).

- [ ] **Step 8: Run the full lsp/server test suite**

Run: `./gradlew :lsp:server:test`
Expected: PASS (no existing behavior changed; default constructor still exits via `System.exit`).

- [ ] **Step 9: Commit**

```bash
git add lsp/server/src
git commit -m "feat(lsp): make NelumboLanguageServer embeddable with per-instance workspace config"
```

---

### Task 2: Route client callbacks through the per-instance client

**Files:**
- Modify: `lsp/server/src/main/java/org/modelingvalue/nelumbo/lsp/NlDocument.java`
- Modify: `lsp/server/src/main/java/org/modelingvalue/nelumbo/lsp/QueryResultCache.java`
- Modify: `lsp/server/src/main/java/org/modelingvalue/nelumbo/lsp/workspaceService/WorkspaceExecuteCommandService.java`
- Modify: `lsp/server/src/main/java/org/modelingvalue/nelumbo/lsp/U.java`
- Modify: `lsp/server/src/main/java/org/modelingvalue/nelumbo/lsp/Workspace.java` (progress call sites)
- Modify: `lsp/server/src/main/java/org/modelingvalue/nelumbo/lsp/Main.java`
- Test: `lsp/server/src/test/java/org/modelingvalue/nelumbo/lsp/EmbeddedServerTest.java`

- [ ] **Step 1: Write the failing test**

Add to `EmbeddedServerTest.java`:

```java
    @Test
    public void diagnosticsGoToTheInstanceClient() throws InterruptedException {
        NelumboLanguageServer server = new NelumboLanguageServer(KnowledgeBase.BASE, 0, () -> {
        });
        RecordingClient       client = new RecordingClient();
        server.connect(client);
        server.getWorkspace().getDocumentManager().addDocument("inmemory://t.nl", "import nelumbo.logic\ntrue ?\n", 1);
        org.junit.jupiter.api.Assertions.assertTrue(client.awaitDiagnostics(10), "expected publishDiagnostics on the per-instance client");
        server.getWorkspace().dispose();
    }
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :lsp:server:test --tests "org.modelingvalue.nelumbo.lsp.EmbeddedServerTest.diagnosticsGoToTheInstanceClient"`
Expected: FAIL — `NlDocument.publishDiagnostics` NPEs on `Main.client` (which is null in tests) or times out; either way the diagnostics never reach `client`.

- [ ] **Step 3: Thread the workspace/client through NlDocument**

In `NlDocument.java`, replace the three diagnostic methods (`publishDiagnosticsAsync` call site in `of` stays, signature changes):

In `of(Workspace workspace, ...)` change the call:

```java
        publishDiagnosticsAsync(workspace, uri, tokenizerResult, parserResult);
```

Replace the method definitions:

```java
    private static void publishDiagnosticsAsync(Workspace workspace, String uri, TokenizerResult tokenizerResult, ParserResult parserResult) {
        publishDiagnostics(workspace, uri, baseDiagnostics(tokenizerResult, parserResult));
    }
```

```java
    public static void publishDiagnostics(Workspace workspace, String uri, List<Diagnostic> diagnostics) {
        LanguageClient client = workspace.getClient();
        if (client == null) {
            return;
        }
        if (Main.debugging() && !diagnostics.isEmpty()) {
            U.DEBUG("    #errors    : %4d", diagnostics.size());
        }
        try (ExecutorService svc = Executors.newSingleThreadExecutor()) {
            svc.submit(() -> client.publishDiagnostics(new PublishDiagnosticsParams(uri, diagnostics)));
        }
    }
```

Add import: `import org.eclipse.lsp4j.services.LanguageClient;`

- [ ] **Step 4: Update QueryResultCache**

In `QueryResultCache.java` `evaluate(String uri)`, replace the last block (from `NlDocument.publishDiagnostics(...)` to the end of the method):

```java
        // republish parse diagnostics together with the query mismatches so neither clobbers the other.
        Workspace workspace = documentManager.workspace();
        NlDocument.publishDiagnostics(workspace, uri, diagnostics);
        LanguageClient client = workspace.getClient();
        if (client != null) {
            try {
                client.refreshInlayHints();
            } catch (Exception ex) {
                // client may not support inlay-hint refresh; the next pull picks up the new results anyway.
            }
        }
```

Add import: `import org.eclipse.lsp4j.services.LanguageClient;`

- [ ] **Step 5: Update WorkspaceExecuteCommandService**

In `WorkspaceExecuteCommandService.java`, replace the three `Main.client.showMessage(...)` calls with a local helper. Add the method:

```java
    private void showMessage(MessageParams message) {
        org.eclipse.lsp4j.services.LanguageClient client = getWorkspace().getClient();
        if (client != null) {
            client.showMessage(message);
        }
    }
```

Then replace:
- `Main.client.showMessage(new MessageParams(MessageType.Error, "Document not found: " + docUri));` → `showMessage(new MessageParams(MessageType.Error, "Document not found: " + docUri));`
- `Main.client.showMessage(new MessageParams(MessageType.Error, "No query found at this position [" + position + "]"));` → `showMessage(new MessageParams(MessageType.Error, "No query found at this position [" + position + "]"));`
- `Main.client.showMessage(message);` → `showMessage(message);`

Remove the now-unused `import org.modelingvalue.nelumbo.lsp.Main;` if nothing else in the file uses `Main`.

- [ ] **Step 6: Update U progress helpers**

In `U.java`, change the three progress methods to take the client and be null-safe:

```java
    public static void progressBegin(LanguageClient client, String what) {
        if (client == null) {
            return;
        }
        client.createProgress(new WorkDoneProgressCreateParams(Either.forLeft(what)));
        WorkDoneProgressBegin begin = new WorkDoneProgressBegin();
        begin.setTitle(what + " in progress");
        begin.setCancellable(false);
        client.notifyProgress(new ProgressParams(Either.forLeft(what), Either.forLeft(begin)));
    }

    public static void progressEnd(LanguageClient client, String what) {
        if (client == null) {
            return;
        }
        WorkDoneProgressEnd end = new WorkDoneProgressEnd();
        end.setMessage(what + " done");
        client.notifyProgress(new ProgressParams(Either.forLeft(what), Either.forLeft(end)));
    }

    public static void withProgress(LanguageClient client, String what, Runnable runnable) {
        progressBegin(client, what);
        runnable.run();
        progressEnd(client, what);
    }
```

Add import: `import org.eclipse.lsp4j.services.LanguageClient;`

In `Workspace.java`, the three `withProgress("...", ...)` call sites (in `findConfiguration`, `processSource`, `indexClasses`) become `withProgress(getClient(), "...", ...)`.

- [ ] **Step 7: Wire connect() in Main**

In `lsp/server/.../Main.java`:

In `start(InputStream, OutputStream)` after `client = launcher.getRemoteProxy();` add:

```java
        server.connect(client);
```

In `LspEndpoint.onOpen`, after `Main.client = launcher.getRemoteProxy();` add:

```java
                server.connect(Main.client);
```

(the local variable `server` of type `NelumboLanguageServer` is already in scope there). Leave the static `Main.client` field itself in place — `WsServer` (dead code) still references it.

- [ ] **Step 8: Run the test to verify it passes**

Run: `./gradlew :lsp:server:test --tests "org.modelingvalue.nelumbo.lsp.EmbeddedServerTest"`
Expected: PASS (4 tests).

- [ ] **Step 9: Run the full suite**

Run: `./gradlew :lsp:server:test`
Expected: PASS.

- [ ] **Step 10: Commit**

```bash
git add lsp/server/src
git commit -m "feat(lsp): route all client callbacks through the per-instance LanguageClient"
```

---

### Task 3: Seed parsing and query evaluation with the workspace KB (+ deadline)

**Files:**
- Modify: `lsp/server/src/main/java/org/modelingvalue/nelumbo/lsp/QueryEvaluator.java`
- Modify: `lsp/server/src/main/java/org/modelingvalue/nelumbo/lsp/NlDocument.java:51`
- Modify: `lsp/server/src/main/java/org/modelingvalue/nelumbo/lsp/QueryResultCache.java:88`
- Modify: `lsp/server/src/main/java/org/modelingvalue/nelumbo/lsp/workspaceService/WorkspaceExecuteCommandService.java:73`
- Test: `lsp/server/src/test/java/org/modelingvalue/nelumbo/lsp/QueryEvaluatorSeedingTest.java`

- [ ] **Step 1: Write the failing test**

`lsp/server/src/test/java/org/modelingvalue/nelumbo/lsp/QueryEvaluatorSeedingTest.java` (add LGPL header):

```java
package org.modelingvalue.nelumbo.lsp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.modelingvalue.nelumbo.KnowledgeBase;
import org.modelingvalue.nelumbo.logic.Query;
import org.modelingvalue.nelumbo.syntax.Parser;
import org.modelingvalue.nelumbo.syntax.ParserResult;
import org.modelingvalue.nelumbo.syntax.Tokenizer;

public class QueryEvaluatorSeedingTest {

    private static final String FIB_SEED = """
            import nelumbo.integers
            Integer ::= fib(<Integer>)
            Integer n, f
            fib(n)=f <=> f=n if n<=1, f=fib(n-1)+fib(n-2) if n>1
            """;

    private static KnowledgeBase seeded(String source) {
        return KnowledgeBase.BASE.run(() -> {
            ParserResult result = new Parser(new Tokenizer(source, "seed.nl").tokenize()).parseNonThrowing();
            result.evaluate();
        });
    }

    @Test
    public void evaluateUsesTheGivenBaseKb() {
        KnowledgeBase           kb      = seeded(FIB_SEED);
        String                  doc     = "Integer r\nfib(5)=r ?\n";
        Map<Query, QueryResult> results = QueryEvaluator.evaluate(kb, 0, doc, "inmemory://field-1.nl");
        assertEquals(1, results.size());
        QueryResult result = results.values().iterator().next();
        assertEquals(QueryResult.Kind.RESULT, result.kind());
        assertTrue(result.inferred().contains("5"), "expected fib(5) result to mention 5, got: " + result.inferred());
    }

    @Test
    public void withoutSeedingTheQueryDoesNotResolve() {
        String                  doc     = "Integer r\nfib(5)=r ?\n";
        Map<Query, QueryResult> results = QueryEvaluator.evaluate(KnowledgeBase.BASE, 0, doc, "inmemory://field-1.nl");
        boolean resolved = results.values().stream().anyMatch(r -> r.kind() == QueryResult.Kind.RESULT && r.inferred().contains("5"));
        assertTrue(!resolved, "fib must be unknown without the seeded KB");
    }
}
```

Note: if `QueryResult.Kind`/`inferred()` names differ, check `lsp/server/src/main/java/org/modelingvalue/nelumbo/lsp/QueryResult.java` — `kind()`, `inferred()`, and enum `Kind { RESULT, MATCH, MISMATCH, ERROR }` are used by `WorkspaceExecuteCommandService` today; mirror whatever that file uses.

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :lsp:server:test --tests "org.modelingvalue.nelumbo.lsp.QueryEvaluatorSeedingTest"`
Expected: COMPILATION FAILURE (no 4-arg `evaluate`).

- [ ] **Step 3: Add the seeded overload to QueryEvaluator**

In `QueryEvaluator.java`, replace the existing `evaluate` method with:

```java
    /** @return result per query, in document order. Queries that could not be reached are absent. */
    public static Map<Query, QueryResult> evaluate(String content, String uri) {
        return evaluate(KnowledgeBase.BASE, 0, content, uri);
    }

    /**
     * Same, but declarations are resolved against {@code base} (a loaded KB for embedded servers) and, when
     * {@code deadlineMs > 0}, inference self-aborts past the deadline (throwing a runtime exception the caller handles).
     */
    public static Map<Query, QueryResult> evaluate(KnowledgeBase base, long deadlineMs, String content, String uri) {
        Map<Query, QueryResult> results = new LinkedHashMap<>();
        KnowledgeBase evalKb = new KnowledgeBase(base);
        if (deadlineMs > 0) {
            evalKb.setDeadlineNanos(System.nanoTime() + deadlineMs * 1_000_000L);
        }
        evalKb.run(() -> {
            KnowledgeBase knowledgeBase = KnowledgeBase.CURRENT.get();
            // ... keep the existing body here verbatim (ParserResult parsed = ...; for (Node root : ...) { ... }) ...
        });
        return results;
    }
```

The existing lambda body moves unchanged; only the receiver of `.run(...)` changes from `KnowledgeBase.BASE` to `evalKb`.

- [ ] **Step 4: Parse documents under the workspace KB**

In `NlDocument.java` `of(Workspace workspace, ...)` change line 51:

```java
        ParserResult    parserResult    = Parser.parse(workspace.getBaseKnowledgeBase(), tokenizerResult);
```

- [ ] **Step 5: Update the two evaluate call sites**

`QueryResultCache.java` line 88 (inside `evaluate(String uri)`, `workspace` local from Task 2 must be moved up before this call):

```java
        Workspace workspace = documentManager.workspace();
        ...
            Map<Query, QueryResult> results = QueryEvaluator.evaluate(workspace.getBaseKnowledgeBase(), workspace.getEvalDeadlineMs(), document.content(), uri);
```

(declare `Workspace workspace = documentManager.workspace();` once at the top of the method and delete the duplicate declaration added in Task 2.)

`WorkspaceExecuteCommandService.java` line 73:

```java
        Workspace ws = getWorkspace();
        Map<Query, QueryResult> results = QueryEvaluator.evaluate(ws.getBaseKnowledgeBase(), ws.getEvalDeadlineMs(), document.content(), document.uri());
```

- [ ] **Step 6: Run tests**

Run: `./gradlew :lsp:server:test`
Expected: PASS, including `QueryExecutionFlowTest` (still uses the 2-arg overload) and the new seeding test.

- [ ] **Step 7: Commit**

```bash
git add lsp/server/src
git commit -m "feat(lsp): resolve documents against an injectable base KB with optional eval deadline"
```

---

### Task 4: LSP-over-Javalin bridge in the http module

**Files:**
- Modify: `http/build.gradle.kts`
- Create: `http/src/main/java/org/modelingvalue/nelumbo/http/LspBridge.java`
- Create: `http/src/main/java/org/modelingvalue/nelumbo/http/LspWebSocket.java`
- Modify: `http/src/main/java/org/modelingvalue/nelumbo/http/NelumboHttpServer.java`
- Test: `http/src/test/java/org/modelingvalue/nelumbo/http/LspWebSocketTest.java`

- [ ] **Step 1: Add dependencies**

In `http/build.gradle.kts` `dependencies` block add:

```kotlin
    implementation(project(":lsp:server"))
    implementation("org.eclipse.lsp4j:org.eclipse.lsp4j:1.0.0")
```

(`implementation` deps of `:lsp:server` are not transitive, hence the explicit lsp4j.)

- [ ] **Step 2: Write the failing test**

`http/src/test/java/org/modelingvalue/nelumbo/http/LspWebSocketTest.java` (add LGPL header). This uses the JDK's `java.net.http.WebSocket`; text frames may arrive fragmented, so the listener reassembles until `last`:

```java
package org.modelingvalue.nelumbo.http;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.modelingvalue.nelumbo.KnowledgeBase;

public class LspWebSocketTest {

    private static final String FIB_SEED = """
            import nelumbo.integers
            Integer ::= fib(<Integer>)
            Integer n, f
            fib(n)=f <=> f=n if n<=1, f=fib(n-1)+fib(n-2) if n>1
            """;

    private static NelumboHttpServer server;
    private static int               port;

    @BeforeAll
    static void start() {
        KnowledgeBase kb = KnowledgeBaseLoader.load(List.of(new NamedSource("seed.nl", FIB_SEED)));
        server = new NelumboHttpServer(kb, List.of("seed.nl"), 30_000, 2);
        port   = server.start(0);
    }

    @AfterAll
    static void stop() {
        server.stop();
    }

    /** Reassembles fragmented text frames and queues complete messages. */
    static final class Frames implements WebSocket.Listener {
        final BlockingQueue<String> messages = new LinkedBlockingQueue<>();
        final CountDownLatch        closed   = new CountDownLatch(1);
        volatile int                closeStatus = -1;
        private final StringBuilder partial  = new StringBuilder();

        @Override
        public CompletionStage<?> onText(WebSocket ws, CharSequence data, boolean last) {
            partial.append(data);
            if (last) {
                messages.add(partial.toString());
                partial.setLength(0);
            }
            ws.request(1);
            return null;
        }

        @Override
        public CompletionStage<?> onClose(WebSocket ws, int statusCode, String reason) {
            closeStatus = statusCode;
            closed.countDown();
            return null;
        }

        String await(String needle, long seconds) throws InterruptedException {
            long deadline = System.nanoTime() + seconds * 1_000_000_000L;
            while (System.nanoTime() < deadline) {
                String m = messages.poll(250, TimeUnit.MILLISECONDS);
                if (m != null && m.contains(needle)) {
                    return m;
                }
            }
            return null;
        }
    }

    private static WebSocket connect(Frames frames) throws Exception {
        return HttpClient.newHttpClient().newWebSocketBuilder()
                .buildAsync(URI.create("ws://localhost:" + port + "/lsp"), frames)
                .get(10, TimeUnit.SECONDS);
    }

    @Test
    public void initializeDidOpenDiagnosticsAndInlayHints() throws Exception {
        Frames    frames = new Frames();
        WebSocket ws     = connect(frames);

        ws.sendText("{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"initialize\",\"params\":{\"processId\":null,\"rootUri\":null,\"capabilities\":{}}}", true).join();
        assertNotNull(frames.await("\"id\":1", 15), "initialize response expected");

        ws.sendText("{\"jsonrpc\":\"2.0\",\"method\":\"initialized\",\"params\":{}}", true).join();
        String doc = "Integer r\\nfib(5)=r ?\\n";
        ws.sendText("{\"jsonrpc\":\"2.0\",\"method\":\"textDocument/didOpen\",\"params\":{\"textDocument\":{\"uri\":\"inmemory://field-1.nl\",\"languageId\":\"nelumbo\",\"version\":1,\"text\":\"" + doc + "\"}}}", true).join();

        assertNotNull(frames.await("textDocument/publishDiagnostics", 15), "diagnostics expected after didOpen");

        // inlay hints carry the auto-evaluated query result; poll until the debounced evaluation lands
        String hints = null;
        for (int attempt = 2; attempt < 30 && hints == null; attempt++) {
            ws.sendText("{\"jsonrpc\":\"2.0\",\"id\":" + attempt + ",\"method\":\"textDocument/inlayHint\",\"params\":{\"textDocument\":{\"uri\":\"inmemory://field-1.nl\"},\"range\":{\"start\":{\"line\":0,\"character\":0},\"end\":{\"line\":10,\"character\":0}}}}", true).join();
            String response = frames.await("\"id\":" + attempt, 5);
            if (response != null && response.contains("5")) {
                hints = response;
            } else {
                Thread.sleep(500);
            }
        }
        assertNotNull(hints, "inlay hint with the fib(5) result expected (KB seeding)");
        ws.sendText("{\"jsonrpc\":\"2.0\",\"id\":99,\"method\":\"shutdown\",\"params\":null}", true).join();
        ws.abort();
    }

    @Test
    public void sessionCapRejectsExcessConnections() throws Exception {
        // own server instance: the other test's sessions must not influence the cap
        NelumboHttpServer capped     = new NelumboHttpServer(KnowledgeBase.BASE, List.of(), 30_000, 1);
        int               cappedPort = capped.start(0);
        try {
            Frames a = new Frames();
            Frames b = new Frames();
            HttpClient http = HttpClient.newHttpClient();
            WebSocket wsA = http.newWebSocketBuilder().buildAsync(URI.create("ws://localhost:" + cappedPort + "/lsp"), a).get(10, TimeUnit.SECONDS);
            WebSocket wsB = http.newWebSocketBuilder().buildAsync(URI.create("ws://localhost:" + cappedPort + "/lsp"), b).get(10, TimeUnit.SECONDS);
            assertTrue(b.closed.await(10, TimeUnit.SECONDS), "second session must be rejected (cap is 1)");
            assertEquals(1013, b.closeStatus);
            wsA.abort();
            wsB.abort();
        } finally {
            capped.stop();
        }
    }
}
```

- [ ] **Step 3: Run test to verify it fails**

Run: `./gradlew :http:test --tests "org.modelingvalue.nelumbo.http.LspWebSocketTest"`
Expected: COMPILATION FAILURE (no 4-arg `NelumboHttpServer` constructor, no `/lsp` route).

- [ ] **Step 4: Write LspBridge**

`http/src/main/java/org/modelingvalue/nelumbo/http/LspBridge.java` (add LGPL header):

```java
package org.modelingvalue.nelumbo.http;

import java.util.List;
import java.util.Map;

import io.javalin.websocket.WsContext;
import org.eclipse.lsp4j.jsonrpc.RemoteEndpoint;
import org.eclipse.lsp4j.jsonrpc.json.JsonRpcMethod;
import org.eclipse.lsp4j.jsonrpc.json.MessageJsonHandler;
import org.eclipse.lsp4j.jsonrpc.messages.Message;
import org.eclipse.lsp4j.jsonrpc.services.ServiceEndpoints;
import org.eclipse.lsp4j.services.LanguageClient;
import org.eclipse.lsp4j.services.LanguageServer;
import org.modelingvalue.nelumbo.KnowledgeBase;
import org.modelingvalue.nelumbo.lsp.NelumboLanguageServer;

/**
 * One embedded LSP session bridged over a Javalin WebSocket. Speaks the same wire format as
 * vscode-ws-jsonrpc: one plain JSON-RPC message per text frame, no Content-Length framing.
 */
final class LspBridge {
    private final NelumboLanguageServer server;
    private final MessageJsonHandler    jsonHandler;
    private final RemoteEndpoint        remoteEndpoint;

    LspBridge(KnowledgeBase baseKb, long evalDeadlineMs, WsContext ctx) {
        // embedded: an 'exit' request must never kill the host JVM
        server = new NelumboLanguageServer(baseKb, evalDeadlineMs, () -> {
        });
        Map<String, JsonRpcMethod> methods = ServiceEndpoints.getSupportedMethods(LanguageServer.class);
        methods.putAll(ServiceEndpoints.getSupportedMethods(LanguageClient.class));
        jsonHandler    = new MessageJsonHandler(methods);
        remoteEndpoint = new RemoteEndpoint(message -> ctx.send(jsonHandler.serialize(message)), ServiceEndpoints.toEndpoint(List.of(server)));
        jsonHandler.setMethodProvider(remoteEndpoint);
        server.connect(ServiceEndpoints.toServiceObject(remoteEndpoint, LanguageClient.class));
    }

    void onMessage(String json) {
        Message message = jsonHandler.parseMessage(json);
        remoteEndpoint.consume(message);
    }

    void close() {
        server.getWorkspace().dispose();
    }
}
```

- [ ] **Step 5: Write LspWebSocket**

`http/src/main/java/org/modelingvalue/nelumbo/http/LspWebSocket.java` (add LGPL header):

```java
package org.modelingvalue.nelumbo.http;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.javalin.websocket.WsConfig;
import org.modelingvalue.nelumbo.KnowledgeBase;

/** The /lsp endpoint: one embedded language server per WebSocket connection, with public-deployment guards. */
final class LspWebSocket {
    static final int      CLOSE_TOO_MANY_SESSIONS = 1013;
    static final int      CLOSE_MESSAGE_TOO_BIG   = 1009;
    static final int      CLOSE_PROTOCOL_ERROR    = 1002;
    static final int      MAX_MESSAGE_CHARS       = 64 * 1024;
    static final Duration IDLE_TIMEOUT            = Duration.ofMinutes(10);

    private final KnowledgeBase          baseKb;
    private final long                   evalDeadlineMs;
    private final int                    maxSessions;
    private final Map<String, LspBridge> sessions = new ConcurrentHashMap<>();

    LspWebSocket(KnowledgeBase baseKb, long evalDeadlineMs, int maxSessions) {
        this.baseKb         = baseKb;
        this.evalDeadlineMs = evalDeadlineMs;
        this.maxSessions    = maxSessions;
    }

    void configure(WsConfig ws) {
        ws.onConnect(ctx -> {
            if (sessions.size() >= maxSessions) {
                ctx.closeSession(CLOSE_TOO_MANY_SESSIONS, "too many LSP sessions, try again later");
                return;
            }
            ctx.session.setIdleTimeout(IDLE_TIMEOUT);
            sessions.put(ctx.sessionId(), new LspBridge(baseKb, evalDeadlineMs, ctx));
        });
        ws.onMessage(ctx -> {
            LspBridge bridge = sessions.get(ctx.sessionId());
            if (bridge == null) {
                return;
            }
            if (ctx.message().length() > MAX_MESSAGE_CHARS) {
                ctx.closeSession(CLOSE_MESSAGE_TOO_BIG, "message too large");
                return;
            }
            try {
                bridge.onMessage(ctx.message());
            } catch (RuntimeException e) {
                System.err.println("LSP session dropped on malformed message: " + e);
                ctx.closeSession(CLOSE_PROTOCOL_ERROR, "protocol error");
            }
        });
        ws.onClose(ctx -> {
            LspBridge bridge = sessions.remove(ctx.sessionId());
            if (bridge != null) {
                bridge.close();
            }
        });
        ws.onError(ctx -> {
            LspBridge bridge = sessions.remove(ctx.sessionId());
            if (bridge != null) {
                bridge.close();
            }
        });
    }
}
```

Note: Jetty's default `maxTextMessageSize` (64 KB) also applies; the explicit check gives a clean close code instead of an abrupt failure. Kotlin interop: if `ctx.session` does not resolve from Java, use `ctx.getSession()`; the Jetty 11 `org.eclipse.jetty.websocket.api.Session` takes `setIdleTimeout(java.time.Duration)`.

- [ ] **Step 6: Wire the route into NelumboHttpServer**

In `NelumboHttpServer.java`:

Add field and constructor:

```java
    /** Default cap on concurrent LSP WebSocket sessions. */
    public static final int DEFAULT_MAX_LSP_SESSIONS = 32;

    private final int maxLspSessions;
```

Change the existing 3-arg constructor to delegate and add the 4-arg one:

```java
    /** {@code timeoutMs} is the per-request inference budget; 0 (or less) disables the timeout. */
    public NelumboHttpServer(KnowledgeBase baseKb, List<String> loadedFiles, long timeoutMs) {
        this(baseKb, loadedFiles, timeoutMs, DEFAULT_MAX_LSP_SESSIONS);
    }

    public NelumboHttpServer(KnowledgeBase baseKb, List<String> loadedFiles, long timeoutMs, int maxLspSessions) {
        this.baseKb         = baseKb;
        this.loadedFiles    = List.copyOf(loadedFiles);
        this.timeoutMs      = timeoutMs;
        this.maxLspSessions = maxLspSessions;
    }
```

In `start(int port)` add the ws route after the existing routes (before `app.start(port)`):

```java
        app.ws("/lsp", new LspWebSocket(baseKb, timeoutMs, maxLspSessions)::configure);
```

- [ ] **Step 7: Run the tests**

Run: `./gradlew :http:test --tests "org.modelingvalue.nelumbo.http.LspWebSocketTest"`
Expected: PASS (2 tests). The inlay-hint poll can take a few seconds (300 ms debounce + evaluation).

- [ ] **Step 8: Run the full http suite**

Run: `./gradlew :http:test`
Expected: PASS.

- [ ] **Step 9: Commit**

```bash
git add http/build.gradle.kts http/src
git commit -m "feat(http): serve embedded LSP sessions over a /lsp WebSocket route"
```

---

### Task 5: CLI option for the session cap

**Files:**
- Modify: `http/src/main/java/org/modelingvalue/nelumbo/http/Main.java`

- [ ] **Step 1: Add the option**

In `Main.main`, add a variable and a switch case alongside the existing ones:

```java
        int maxLspSessions = NelumboHttpServer.DEFAULT_MAX_LSP_SESSIONS;
```

```java
            case "-s":
            case "--max-lsp-sessions":
                if (i + 1 >= args.length) {
                    fail("missing value for " + a);
                }
                maxLspSessions = Integer.parseInt(args[++i]);
                break;
```

Change the server construction:

```java
        NelumboHttpServer server = new NelumboHttpServer(base, files, timeoutMs, maxLspSessions);
```

In `printUsage`, add after the `--timeout` line:

```java
        out.println("  -s, --max-lsp-sessions N  cap on concurrent LSP editor sessions (default 32)");
```

- [ ] **Step 2: Build and smoke-test**

Run: `./gradlew :http:serverJar && java -jar http/build/libs/nelumbo-http-server-*.jar --help`
Expected: usage text shows the new option; exit 0.

- [ ] **Step 3: Commit**

```bash
git add http/src/main/java/org/modelingvalue/nelumbo/http/Main.java
git commit -m "feat(http): add --max-lsp-sessions option"
```

---

### Task 6: Frontend project — Monaco fields bundle

**Files:**
- Create: `http/src/main/frontend/package.json`
- Create: `http/src/main/frontend/tsconfig.json`
- Create: `http/src/main/frontend/src/nelumbo-fields.ts`
- Create: `http/src/main/frontend/src/fields.css`
- Create: `http/src/main/frontend/.gitignore`

This is the highest-risk task: monaco-languageclient's package layout shifts between majors. The versions below are a known-coherent set; **if `npm install` reports unresolvable peers, or imports fail to typecheck, open `node_modules/monaco-languageclient/README.md` and its bundled examples and adapt the imports/initServices call to the installed major — keep the structure of this file (init once, one socket per page, one model per field).**

- [ ] **Step 1: package.json**

```json
{
  "name": "nelumbo-fields",
  "version": "0.1.0",
  "private": true,
  "scripts": {
    "check": "tsc --noEmit",
    "build": "npm run check && esbuild src/nelumbo-fields.ts --bundle --outdir=dist --format=iife --global-name=NelumboFields --loader:.ttf=file --loader:.css=css --sourcemap --minify",
    "dist": "npm ci && npm run build"
  },
  "dependencies": {
    "monaco-editor": "npm:@codingame/monaco-vscode-editor-api@~11.1.2",
    "monaco-languageclient": "~8.8.1",
    "vscode-ws-jsonrpc": "~3.4.0"
  },
  "devDependencies": {
    "esbuild": "^0.25.0",
    "typescript": "^5.4.5"
  }
}
```

- [ ] **Step 2: tsconfig.json**

```json
{
  "compilerOptions": {
    "target": "ES2022",
    "module": "ESNext",
    "moduleResolution": "bundler",
    "strict": true,
    "skipLibCheck": true,
    "noEmit": true,
    "lib": ["ES2022", "DOM", "DOM.Iterable"]
  },
  "include": ["src"]
}
```

- [ ] **Step 3: .gitignore**

```
node_modules/
dist/
```

- [ ] **Step 4: fields.css**

```css
.nelumbo-field-wrap { border: 1px solid #343843; border-radius: 8px; overflow: hidden; margin: 14px 0; background: #23262e; }
.nelumbo-field-toolbar { display: flex; align-items: center; gap: 12px; padding: 6px 10px; border-bottom: 1px solid #343843; }
.nelumbo-field-toolbar button { background: #6ea8fe; color: #0c1322; border: 0; border-radius: 6px; padding: 4px 12px; font-weight: 600; cursor: pointer; font-size: 13px; }
.nelumbo-field-toolbar .status { color: #9aa0ac; font-size: 12px; margin-left: auto; }
.nelumbo-field-editor { height: 220px; }
.nelumbo-field-results { padding: 8px 12px; font: 12px/1.5 ui-monospace, Menlo, Consolas, monospace; color: #e6e8ee; border-top: 1px solid #343843; white-space: pre-wrap; display: none; }
.nelumbo-field-results.visible { display: block; }
.nelumbo-field-results .q-true { color: #46c98b; }
.nelumbo-field-results .q-false { color: #f1707b; }
.nelumbo-field-results .q-unknown { color: #c9a14a; }
.nelumbo-field-results .q-error { color: #f1707b; }
.nelumbo-lsp-banner { background: rgba(201,161,74,.15); color: #c9a14a; padding: 4px 12px; font-size: 12px; display: none; }
.nelumbo-lsp-banner.visible { display: block; }
```

- [ ] **Step 5: nelumbo-fields.ts**

```ts
import * as monaco from 'monaco-editor';
import { initServices } from 'monaco-languageclient/vscode/services';
import { MonacoLanguageClient } from 'monaco-languageclient';
import { CloseAction, ErrorAction } from 'vscode-languageclient/browser.js';
import { WebSocketMessageReader, WebSocketMessageWriter, toSocket } from 'vscode-ws-jsonrpc';
import './fields.css';

const LANGUAGE_ID: string = 'nelumbo';

interface EvalQuery {
    query: string;
    status: string;
    bindings: Record<string, string>[];
    result: string;
}

interface EvalResponse {
    queries?: EvalQuery[];
    errors?: { line?: number; column?: number; message?: string }[];
    error?: string;
    message?: string;
}

function esc(s: string): string {
    return s.replace(/[&<>]/g, (c: string) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;' }[c] as string));
}

function renderResults(el: HTMLElement, data: EvalResponse): void {
    let html: string = '';
    if (data.error) {
        html += '<div class="q-error">' + esc(data.error + ': ' + (data.message || '')) + '</div>';
    }
    for (const e of data.errors || []) {
        const loc: string = e.line != null ? e.line + ':' + e.column + '  ' : '';
        html += '<div class="q-error">' + esc(loc + (e.message || '')) + '</div>';
    }
    for (const q of data.queries || []) {
        const bindings: string = (q.bindings || [])
            .map((b: Record<string, string>) => Object.entries(b).map(([k, v]: [string, string]) => k + '=' + v).join(', ') || '()')
            .join('  ');
        html += '<div><span class="q-' + esc(q.status) + '">' + esc(q.status) + '</span>  ' + esc(q.query) + (bindings ? '   ' + esc(bindings) : '') + '</div>';
    }
    el.innerHTML = html || '<div>no queries</div>';
    el.classList.add('visible');
}

async function runEval(content: string, statusEl: HTMLElement, resultsEl: HTMLElement): Promise<void> {
    statusEl.textContent = 'running...';
    try {
        const res: Response = await fetch('/eval', { method: 'POST', headers: { 'Content-Type': 'text/plain' }, body: content });
        const data: EvalResponse = await res.json();
        statusEl.textContent = 'HTTP ' + res.status;
        renderResults(resultsEl, data);
    } catch (err) {
        statusEl.textContent = 'request failed';
        resultsEl.innerHTML = '<div class="q-error">' + esc(String(err)) + '</div>';
        resultsEl.classList.add('visible');
    }
}

function connectLanguageClient(): Promise<MonacoLanguageClient | null> {
    return new Promise((resolve: (c: MonacoLanguageClient | null) => void) => {
        const proto: string = location.protocol === 'https:' ? 'wss:' : 'ws:';
        const webSocket: WebSocket = new WebSocket(proto + '//' + location.host + '/lsp');
        webSocket.onerror = () => resolve(null);
        webSocket.onopen = () => {
            const socket = toSocket(webSocket);
            const reader = new WebSocketMessageReader(socket);
            const writer = new WebSocketMessageWriter(socket);
            const client: MonacoLanguageClient = new MonacoLanguageClient({
                name: 'Nelumbo Language Client',
                clientOptions: {
                    documentSelector: [LANGUAGE_ID],
                    errorHandler: {
                        error: () => ({ action: ErrorAction.Continue }),
                        closed: () => ({ action: CloseAction.DoNotRestart }),
                    },
                },
                connectionProvider: { get: () => Promise.resolve({ reader, writer }) },
            });
            client.start();
            reader.onClose(() => client.stop());
            resolve(client);
        };
    });
}

function buildField(div: HTMLElement, index: number): void {
    const initial: string = (div.textContent || '').replace(/^\n/, '');
    div.textContent = '';
    div.classList.add('nelumbo-field-wrap');

    const toolbar: HTMLDivElement = document.createElement('div');
    toolbar.className = 'nelumbo-field-toolbar';
    const runBtn: HTMLButtonElement = document.createElement('button');
    runBtn.textContent = 'Run';
    const status: HTMLSpanElement = document.createElement('span');
    status.className = 'status';
    status.textContent = 'ready';
    toolbar.append(runBtn, status);

    const editorEl: HTMLDivElement = document.createElement('div');
    editorEl.className = 'nelumbo-field-editor';
    if (div.dataset.height) {
        editorEl.style.height = div.dataset.height;
    }
    const results: HTMLDivElement = document.createElement('div');
    results.className = 'nelumbo-field-results';
    div.append(toolbar, editorEl, results);

    const model: monaco.editor.ITextModel = monaco.editor.createModel(
        initial,
        LANGUAGE_ID,
        monaco.Uri.parse('inmemory://field-' + index + '.nl'),
    );
    const editor: monaco.editor.IStandaloneCodeEditor = monaco.editor.create(editorEl, {
        model: model,
        theme: 'vs-dark',
        minimap: { enabled: false },
        automaticLayout: true,
        fontSize: 13,
        'semanticHighlighting.enabled': true,
        scrollBeyondLastLine: false,
    });

    const run = (): void => {
        void runEval(model.getValue(), status, results);
    };
    runBtn.addEventListener('click', run);
    editor.addCommand(monaco.KeyMod.CtrlCmd | monaco.KeyCode.Enter, run);
}

export async function initNelumboFields(): Promise<void> {
    await initServices({});
    monaco.languages.register({ id: LANGUAGE_ID, extensions: ['.nl'] });

    const divs: HTMLElement[] = Array.from(document.querySelectorAll<HTMLElement>('.nelumbo-field'));
    divs.forEach((div: HTMLElement, i: number) => buildField(div, i));

    const client: MonacoLanguageClient | null = await connectLanguageClient();
    if (client === null) {
        const banner: HTMLDivElement = document.createElement('div');
        banner.className = 'nelumbo-lsp-banner visible';
        banner.textContent = 'Language features unavailable (no LSP connection). Editing and Run still work.';
        document.body.prepend(banner);
    }
}
```

- [ ] **Step 6: Install and typecheck**

Run: `cd http/src/main/frontend && npm install && npm run check`
Expected: `npm install` resolves (see the version note at the top of this task if not); `tsc --noEmit` passes. Fix any API drift per the installed package's README/examples before proceeding.

- [ ] **Step 7: Build the bundle**

Run: `cd http/src/main/frontend && npm run build && ls dist/`
Expected: `nelumbo-fields.js`, `nelumbo-fields.css` (plus `.map` files and possibly `.ttf` codicon assets) in `dist/`.

- [ ] **Step 8: Commit**

```bash
git add http/src/main/frontend
git commit -m "feat(http): add Monaco+LSP frontend field component"
```

---

### Task 7: Gradle wiring — bundle into serverJar

**Files:**
- Modify: `http/build.gradle.kts`

- [ ] **Step 1: Add npm tasks and resource wiring**

Append to `http/build.gradle.kts`:

```kotlin
val frontendDir = layout.projectDirectory.dir("src/main/frontend")

val npmBundle by tasks.registering(Exec::class) {
    description = "install frontend deps and build the Monaco fields bundle"
    workingDir = frontendDir.asFile
    commandLine("npm", "run", "dist")
    inputs.dir(frontendDir.dir("src"))
    inputs.file(frontendDir.file("package.json"))
    inputs.file(frontendDir.file("package-lock.json"))
    inputs.file(frontendDir.file("tsconfig.json"))
    outputs.dir(frontendDir.dir("dist"))
}

val copyFrontend by tasks.registering(Sync::class) {
    dependsOn(npmBundle)
    from(frontendDir.dir("dist"))
    into(layout.buildDirectory.dir("generated-resources/public/assets"))
}

sourceSets.main {
    resources.srcDir(layout.buildDirectory.dir("generated-resources"))
}

tasks.processResources {
    dependsOn(copyFrontend)
}
```

- [ ] **Step 2: Serve static resources**

In `NelumboHttpServer.start`, change `Javalin.create()` to serve the classpath `public/` dir (imports: `io.javalin.http.staticfiles.Location`):

```java
        app = Javalin.create(config -> config.staticFiles.add("/public", Location.CLASSPATH));
```

- [ ] **Step 3: Verify the jar contains the bundle**

Run: `./gradlew :http:serverJar && unzip -l http/build/libs/nelumbo-http-server-*.jar | grep "public/assets"`
Expected: `public/assets/nelumbo-fields.js` and `public/assets/nelumbo-fields.css` listed.

- [ ] **Step 4: Run http tests (static files change must not break /eval or /)**

Run: `./gradlew :http:test`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add http/build.gradle.kts http/src/main/java/org/modelingvalue/nelumbo/http/NelumboHttpServer.java
git commit -m "build(http): bundle frontend via npm into serverJar resources"
```

---

### Task 8: Pages — rebuild playground, add demo page

**Files:**
- Modify: `http/src/main/resources/public/playground.html`
- Create: `http/src/main/resources/public/demo.html`

- [ ] **Step 1: Rebuild playground.html on the component**

Replace the `<main>` and `<script>` sections of `playground.html`. Keep the existing `<head>` styles for header/nav; the field component brings its own styles. The body becomes:

```html
<body>
<header>
  <h1>Nelumbo Playground</h1>
  <span class="sub">evaluate a document against the loaded knowledge base</span>
  <nav>
    <a href="/demo.html">demo</a>
    <a href="/metadata" target="_blank">metadata</a>
    <a href="/health" target="_blank">health</a>
  </nav>
</header>

<main style="display:block; padding: 16px; overflow:auto;">
  <div class="nelumbo-field" data-height="55vh">// A posted document is self-contained: declare the variables you query with.
// The loaded knowledge base provides the types, functors, rules and facts.

Integer r
fib(5)=r ?
</div>
</main>

<link rel="stylesheet" href="/assets/nelumbo-fields.css">
<script src="/assets/nelumbo-fields.js"></script>
<script>NelumboFields.initNelumboFields();</script>
</body>
```

Delete the old `<textarea>`, results pane markup, and the old inline `<script>` block (the component now owns Run/results). The `trace` and `raw` toolbar options of the old playground are dropped — `/eval/trace` remains reachable via curl; if wanted later they are a component option, out of scope here.

- [ ] **Step 2: Create demo.html**

`http/src/main/resources/public/demo.html` — same dark styling, prose + multiple fields. All examples must be self-contained (imports), so they work even when the server was started with no .nl files:

```html
<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>Nelumbo Demo</title>
<style>
  body { background: #1b1d23; color: #e6e8ee; font: 15px/1.6 system-ui, sans-serif; margin: 0; }
  header { display: flex; align-items: baseline; gap: 12px; padding: 10px 16px; border-bottom: 1px solid #343843; }
  header h1 { font-size: 15px; font-weight: 600; margin: 0; }
  header nav { margin-left: auto; display: flex; gap: 14px; }
  header nav a { color: #9aa0ac; text-decoration: none; font-size: 12px; }
  main { max-width: 900px; margin: 0 auto; padding: 24px 16px 80px; }
  h2 { font-size: 18px; margin-top: 40px; }
  p { color: #c6cad4; }
</style>
</head>
<body>
<header>
  <h1>Nelumbo Demo</h1>
  <nav><a href="/">playground</a><a href="/metadata" target="_blank">metadata</a></nav>
</header>
<main>
  <h2>Propositional logic</h2>
  <p>Nelumbo evaluates queries against a knowledge base. Edit the code below - you get
     completion, hover, diagnostics and inline results as you type. Press Run (or Cmd/Ctrl+Enter)
     to execute against the server.</p>
  <div class="nelumbo-field">import nelumbo.logic

true &amp; true ?
true | false ?
unknown &amp; true ?
</div>

  <h2>Integers and rules</h2>
  <p>Rules define relations; here the classic Fibonacci definition. The inline hint after the
     query shows the inferred answer.</p>
  <div class="nelumbo-field" data-height="260px">import nelumbo.integers

Integer ::= fib(&lt;Integer&gt;)
Integer n, f
fib(n)=f &lt;=&gt; f=n if n&lt;=1, f=fib(n-1)+fib(n-2) if n&gt;1

Integer r
fib(7)=r ?
</div>

  <h2>Quantifiers</h2>
  <p>Existential and universal quantification over declared variables.</p>
  <div class="nelumbo-field">import nelumbo.integers

Integer x
E[x] x*x = 9 ?
</div>
</main>
<link rel="stylesheet" href="/assets/nelumbo-fields.css">
<script src="/assets/nelumbo-fields.js"></script>
<script>NelumboFields.initNelumboFields();</script>
</body>
</html>
```

Before committing, verify each snippet actually evaluates: save each field's content (HTML-unescaped: `&amp;`→`&`, `&lt;`→`<`, `&gt;`→`>`) to a temp `.nl` file and run `./gradlew cliJar && java -jar build/libs/nelumbo-*-cli.jar /tmp/snippet.nl`; exit 0 required. Adjust snippets that fail (the quantifier example in particular — check `src/main/resources/org/modelingvalue/nelumbo/tests/logicTest.nl` and `integersTest.nl` for known-good syntax).

- [ ] **Step 3: Manual smoke test**

Run: `./gradlew :http:serverJar && java -jar http/build/libs/nelumbo-http-server-*.jar --port 8080`
Open `http://localhost:8080/` and `http://localhost:8080/demo.html` in a browser. Verify per field: syntax colors appear (semantic tokens), hover shows info, Ctrl+Space completes, deliberate error shows a squiggle, query result appears inline after ~1s, Run shows results below, two fields do not see each other's declarations. Verify the fallback: stop/start the server with `--max-lsp-sessions 0` and reload — banner appears, Run still works.

- [ ] **Step 4: Commit**

```bash
git add http/src/main/resources/public
git commit -m "feat(http): LSP-powered playground and multi-field demo page"
```

---

### Task 9: CI — Node setup

**Files:**
- Modify: `.github/workflows/build.yaml`

- [ ] **Step 1: Add Node to the build job**

In `.github/workflows/build.yaml`, in the job that runs the Gradle build, add before the Gradle step (match the file's existing indentation and style):

```yaml
      - name: setup node
        uses: actions/setup-node@v4
        with:
          node-version: 22
```

- [ ] **Step 2: Verify locally that a clean build works**

Run: `./gradlew clean build`
Expected: PASS end to end, including `:http:npmBundle`.

- [ ] **Step 3: Commit and push, watch CI**

```bash
git add .github/workflows/build.yaml
git commit -m "ci: set up Node for the http frontend bundle"
git push
gh run watch
```

Expected: workflow green.

---

### Task 10: Documentation

**Files:**
- Modify: `CLAUDE.md`
- Modify: `docs/superpowers/specs/2026-07-10-lsp-demo-site-design.md`

- [ ] **Step 1: Update CLAUDE.md**

- Build commands: note that `:http` needs Node/npm (frontend bundle) and add `./gradlew :http:serverJar` already listed — extend its comment: `# HTTP server shaded JAR (needs node/npm; serves playground + demo with LSP editors)`.
- Module structure: extend the `http` line: `HTTP server (Javalin + Jackson + embedded LSP over /lsp ws): REST /eval,/metadata,/health + Monaco demo pages`.
- Architecture: add one line under LSP Server section: `NelumboLanguageServer is embeddable: per-instance LanguageClient, injectable base KB and eval deadline (used by the http module's /lsp WebSocket).`

- [ ] **Step 2: Append the planning discoveries to the spec**

Add the "Discoveries during planning" items (exit handler, static client refactor, eval deadline, scheduler dispose) as an "Implementation notes" section at the end of the spec document. Also record two deviations from the spec: (1) the playground's trace/raw-JSON toolbar options were dropped (still reachable via curl on /eval/trace); (2) KB seeding is asserted end-to-end via the inlay-hint result instead of a completion item.

- [ ] **Step 3: Commit**

```bash
git add CLAUDE.md docs/superpowers/specs/2026-07-10-lsp-demo-site-design.md
git commit -m "docs: record LSP demo site build requirements and implementation notes"
```

---

## Verification checklist (after all tasks)

- [ ] `./gradlew clean build` passes (root + lsp + http, with npm bundle)
- [ ] `java -jar http/build/libs/nelumbo-http-server-*.jar` (argless) serves `/`, `/demo.html`; fields get completion/hover/diagnostics/inline results via imports
- [ ] Started with a `.nl` file defining `fib`: a field querying `fib(5)=r ?` WITHOUT declaring fib gets diagnostics-clean parsing, completion for `fib`, and the inline result (KB seeding works end to end)
- [ ] Two demo fields are isolated (a declaration in one is an unknown in the other)
- [ ] `--max-lsp-sessions 1`: second browser tab shows the fallback banner, editing and Run still work
- [ ] IDE plugins unaffected: `./gradlew :lsp:server:test` green; stdio mode still exits on `exit`
