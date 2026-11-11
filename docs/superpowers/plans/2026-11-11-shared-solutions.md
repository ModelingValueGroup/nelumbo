# Shared Solutions Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Tutorial and sandbox editors get `Save as...` and `Load...`: a text is stored on the server under a global name and anyone can load it by that name.

**Architecture:** Three small classes in the website module: `BlockedWords` (swear word check), `SolutionStore` (one JSON file per name on a volume, expiry, total size) and `SolutionService` (validation, ownership, limits, the status + message per request). `NelumboHttpServer` exposes them as `PUT`/`GET /solutions/{name}`; `nelumbo-fields.ts` adds the buttons to the field toolbar.

**Tech Stack:** Java 21, Javalin 7.2.4, Jackson 2.23 (already dependencies), JUnit 5; TypeScript + Monaco 0.34 frontend, Playwright e2e.

**Spec:** `docs/superpowers/specs/2026-11-11-shared-solutions-design.md`

## Global Constraints

- No new dependencies.
- Limits: name `[A-Za-z0-9-]{1,40}`, case-insensitive; text at most 32 * 1024 chars JSON-escaped; 30 saves per browser per hour; 200 new names per IP per hour; 100 MB total (UTF-8 bytes of the texts); kept 7 days from the last save.
- Header `X-Nelumbo-Browser` carries the browser id; the server keeps only its SHA-256 hex. IPs and ids are never written to disk.
- Client IP = last entry of `X-Forwarded-For`, else `ctx.ip()`.
- UI copy is English and exactly as in this plan.
- Java: new `switch`es in arrow form; the existing colon `switch` in `Main` keeps its form.
- TypeScript: type every variable and argument, one variable per definition, aligned in columns, `if`/`for` always multi-line with braces.
- CSS: colours only through `--nf-*` variables.
- ASCII only in code and copy (straight quotes, plain hyphens).
- New Java files get the LGPL header (`docs/header-template.txt`; `mvgCorrector` fixes it in the build).
- Commit on `local/work`; never push or merge.
- Update `CLAUDE.md` in the task that changes the behavior it describes.

## Review Focus

1. Two browsers save the same new name at the same moment: exactly one owns it, the other gets 409 (Task 3, `concurrentSavesOfOneNameHaveOneOwner`).
2. A name typed with spaces around it (" Frits "): trimmed in the browser, saves and loads as "Frits" (Task 5, e2e uses a padded name).
3. A half-written or broken file in the solutions dir (crash, full disk): skipped, the server still starts (Task 2, `aBrokenFileIsSkipped`).
4. Non-ASCII and CRLF in a text (`Zoë ✓`, `\r\n`): comes back byte-identical over HTTP (Task 4, `aSolutionIsSavedAndLoadedByName`).
5. Forged input: a name like `../etc` never reaches the file system; a client-sent `X-Forwarded-For` cannot replace the address Traefik appends (Task 3, `badNamesAreRefused`, `clientIpIsTheLastForwardedEntry`).

---

### Task 1: BlockedWords

**Files:**
- Create: `website/src/main/java/org/modelingvalue/nelumbo/website/BlockedWords.java`
- Create: `website/src/main/resources/blocked-words.txt`
- Test: `website/src/test/java/org/modelingvalue/nelumbo/website/BlockedWordsTest.java`

**Interfaces:**
- Produces:
  - `BlockedWords(Collection<String> words)` (words in lowercase)
  - `static BlockedWords load()` (classpath `/blocked-words.txt`)
  - `boolean inName(String name)`
  - `boolean inText(String text)`

- [ ] **Step 1: Write the failing test**

```java
class BlockedWordsTest {
    private final BlockedWords words = new BlockedWords(List.of("fuck", "kut", "ass", "tit", "lul", "cock", "dick"));

    @Test
    void aLongWordIsBlockedAnywhereInAName() {
        assertTrue(words.inName("fuckyou123"));
        assertTrue(words.inName("Hancock"));
    }

    @Test
    void aShortWordIsBlockedOnlyAsAWholeRunOfLetters() {
        assertTrue(words.inName("Kut-123"));
        assertTrue(words.inName("Dick"));
        assertFalse(words.inName("Hassan"));
        assertFalse(words.inName("Titus"));
        assertFalse(words.inName("Kutlu"));
        assertFalse(words.inName("Lulu"));
        assertFalse(words.inName("Cassandra"));
    }

    @Test
    void aTextIsCheckedOnWholeWords() {
        assertTrue(words.inText("fact kut(A)"));
        assertTrue(words.inText("// FUCK"));
        assertFalse(words.inText("Thing ::= class <Cassandra>"));
        assertFalse(words.inText("// Scunthorpe"));
    }

    @Test
    void theBundledListHasDutchAndEnglish() {
        BlockedWords bundled = BlockedWords.load();
        assertTrue(bundled.inText("fuck"));
        assertTrue(bundled.inText("kut"));
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :website:test --tests "*BlockedWordsTest*"`
Expected: FAIL, compilation error "cannot find symbol BlockedWords".

- [ ] **Step 3: Implement `BlockedWords` and the word list**

- `inName`: lowercase (`Locale.ROOT`), split into runs of `[a-z]+`. A list word of 4+ letters blocks when a run contains it; a shorter word only when a run equals it.
- `inText`: lowercase, split on `\P{L}+`, blocked when any word is in the set.
- `blocked-words.txt`: one lowercase word per line; `#` lines and blank lines are ignored. Common Dutch and English swear words and slurs (roughly 50-150). Written by hand, or from a public list whose license allows redistribution in an LGPL project; then credit it in a `#` header line.

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :website:test --tests "*BlockedWordsTest*"`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add website/src/main/java/org/modelingvalue/nelumbo/website/BlockedWords.java website/src/main/resources/blocked-words.txt website/src/test/java/org/modelingvalue/nelumbo/website/BlockedWordsTest.java
git commit -m "website: blocked word check for shared solutions"
```

---

### Task 2: SolutionStore

**Files:**
- Create: `website/src/main/java/org/modelingvalue/nelumbo/website/Solution.java`
- Create: `website/src/main/java/org/modelingvalue/nelumbo/website/SolutionStore.java`
- Test: `website/src/test/java/org/modelingvalue/nelumbo/website/SolutionStoreTest.java`

**Interfaces:**
- Produces:
  - `record Solution(String name, String ownerHash, long savedAt, String text)` with `long bytes()` = UTF-8 length of `text`
  - `SolutionStore(Path dir, LongSupplier clock)`: `dir` null = memory only; loads the dir's `*.json`, skips broken files, deletes expired ones
  - `static final long KEEP_MS = 7 * 86_400_000L`
  - `synchronized Optional<Solution> get(String name)`: case-insensitive; an expired one is empty
  - `synchronized void put(Solution solution)`: replaces any solution with the same name
  - `synchronized long totalBytes()`: sum of `bytes()` of the stored solutions
  - `synchronized void expire()`: removes expired solutions from memory and disk

- [ ] **Step 1: Write the failing test**

```java
class SolutionStoreTest {
    @TempDir Path    dir;
    private final AtomicLong now = new AtomicLong(1_000_000_000_000L);

    private SolutionStore store(Path d) {
        return new SolutionStore(d, now::get);
    }

    @Test
    void namesIgnoreCase() {
        SolutionStore store = store(dir);
        store.put(new Solution("Frits", "h1", now.get(), "fact p(A)"));
        assertEquals("fact p(A)", store.get("FRITS").orElseThrow().text());
        assertEquals("Frits", store.get("frits").orElseThrow().name());
    }

    @Test
    void aSolutionExpiresSevenDaysAfterItsLastSave() {
        SolutionStore store = store(dir);
        store.put(new Solution("Frits", "h1", now.get(), "v1"));
        now.addAndGet(SolutionStore.KEEP_MS - 1);
        store.put(new Solution("Frits", "h1", now.get(), "v2"));
        now.addAndGet(2);
        assertEquals("v2", store.get("Frits").orElseThrow().text());
        now.addAndGet(SolutionStore.KEEP_MS);
        assertTrue(store.get("Frits").isEmpty());
        store.expire();
        assertEquals(0, store.totalBytes());
        assertFalse(Files.exists(dir.resolve("frits.json")));
    }

    @Test
    void solutionsSurviveARestart() {
        store(dir).put(new Solution("Frits", "h1", now.get(), "Zoë\r\n"));
        SolutionStore again = store(dir);
        assertEquals(new Solution("Frits", "h1", now.get(), "Zoë\r\n"), again.get("frits").orElseThrow());
        assertEquals(6, again.totalBytes());
    }

    @Test
    void expiredFilesAreDeletedAtStart() throws IOException {
        store(dir).put(new Solution("Frits", "h1", now.get(), "x"));
        now.addAndGet(SolutionStore.KEEP_MS + 1);
        store(dir);
        try (Stream<Path> files = Files.list(dir)) {
            assertEquals(0, files.count());
        }
    }

    @Test
    void aBrokenFileIsSkipped() throws IOException {
        store(dir).put(new Solution("Frits", "h1", now.get(), "x"));
        Files.writeString(dir.resolve("broken.json"), "{\"name\":\"bro");
        SolutionStore again = store(dir);
        assertTrue(again.get("broken").isEmpty());
        assertEquals("x", again.get("Frits").orElseThrow().text());
    }

    @Test
    void totalBytesFollowsReplacements() {
        SolutionStore store = store(dir);
        store.put(new Solution("Frits", "h1", now.get(), "ë"));
        assertEquals(2, store.totalBytes());
        store.put(new Solution("frits", "h1", now.get(), "abc"));
        assertEquals(3, store.totalBytes());
    }

    @Test
    void withoutADirItIsMemoryOnly() {
        SolutionStore store = store(null);
        store.put(new Solution("Frits", "h1", now.get(), "x"));
        assertEquals("x", store.get("Frits").orElseThrow().text());
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :website:test --tests "*SolutionStoreTest*"`
Expected: FAIL, compilation error "cannot find symbol SolutionStore".

- [ ] **Step 3: Implement `Solution` and `SolutionStore`**

- Key and file name: `name.toLowerCase(Locale.ROOT)`, file `<key>.json` (Jackson `ObjectMapper` writes the record). Names are validated by `SolutionService` before they get here.
- Write to `<key>.json.tmp`, then `Files.move(tmp, file, REPLACE_EXISTING, ATOMIC_MOVE)`; `Files.createDirectories(dir)` first.
- An I/O error on write or delete: print to stderr like `StatsHistory`, keep the solution in memory.
- At start: read every `*.json`, skip what does not parse (catch `IOException | RuntimeException`), then `expire()`.

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :website:test --tests "*SolutionStoreTest*"`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add website/src/main/java/org/modelingvalue/nelumbo/website/Solution.java website/src/main/java/org/modelingvalue/nelumbo/website/SolutionStore.java website/src/test/java/org/modelingvalue/nelumbo/website/SolutionStoreTest.java
git commit -m "website: file store for shared solutions with 7-day expiry"
```

---

### Task 3: SolutionService

**Files:**
- Create: `website/src/main/java/org/modelingvalue/nelumbo/website/SolutionService.java`
- Test: `website/src/test/java/org/modelingvalue/nelumbo/website/SolutionServiceTest.java`

**Interfaces:**
- Consumes: `SolutionStore`, `Solution`, `BlockedWords` (Tasks 1-2).
- Produces:
  - `record Response(int status, String body)` (nested in `SolutionService`)
  - `SolutionService(SolutionStore store, BlockedWords words, LongSupplier clock, long maxTotalBytes)`
  - constants `MAX_NAME_CHARS = 40`, `MAX_TEXT_CHARS = 32 * 1024`, `MAX_TOTAL_BYTES = 100L * 1024 * 1024`, `SAVES_PER_BROWSER_PER_HOUR = 30`, `NEW_NAMES_PER_IP_PER_HOUR = 200`
  - `synchronized Response save(String name, String text, String browserId, String ip)`
  - `Response load(String name)`: 200 with the text as body
  - `static String clientIp(String forwardedFor, String remoteIp)`

`save` checks in this order (first failure wins). Exact bodies, `%s` = the name as given:

| # | Check | Status | Body |
|---|-------|--------|------|
| 1 | name matches `[A-Za-z0-9-]{1,40}` | 400 | `Use only letters a-z, digits and hyphens` |
| 2 | browser id not null/blank | 400 | `Missing browser id` |
| 3 | JSON-escaped length of text (Jackson `writeValueAsString(text).length() - 2`) <= `MAX_TEXT_CHARS` | 413 | `Not saved: too large (max 32 KB)` |
| 4 | no blocked word in name (`inName`) or text (`inText`) | 422 | `Not saved: contains a blocked word` |
| 5 | an existing solution has the same owner hash | 409 | `"%s" is taken` |
| 6 | browser below 30 saves in the last hour | 429 | `Too many saves, try again later` |
| 7 | a NEW name: IP below 200 new names in the last hour | 429 | `Too many saves, try again later` |
| 8 | growth (new bytes - old bytes) <= 0, or total + growth <= max | 503 | `Not saved: the server is full, try again later` |
| - | stored, counters updated | 200 | `Saved as "%s" (kept 7 days)` |

`load`: bad name -> 400 with the body of row 1; missing -> 404 `No solution named "%s"`.

Only a successful save counts for the limits. Owner hash: SHA-256 hex of the browser id (`MessageDigest`, `HexFormat`). Counters: per key a deque of times, entries older than an hour dropped when read. At most once an hour `save` calls `store.expire()` and drops empty counter keys.

- [ ] **Step 1: Write the failing test**

```java
class SolutionServiceTest {
    private final AtomicLong      now     = new AtomicLong(1_000_000_000_000L);
    private SolutionService       service = service(SolutionService.MAX_TOTAL_BYTES);

    private SolutionService service(long max) {
        return new SolutionService(new SolutionStore(null, now::get), new BlockedWords(List.of("kut")), now::get, max);
    }

    @Test
    void aSavedSolutionLoadsByName() {
        assertEquals(new SolutionService.Response(200, "Saved as \"Frits\" (kept 7 days)"), service.save("Frits", "fact p(A)", "b1", "1.1.1.1"));
        assertEquals(new SolutionService.Response(200, "fact p(A)"), service.load("FRITS"));
    }

    @Test
    void onlyTheFirstBrowserMayUpdate() {
        service.save("Frits", "v1", "b1", "1.1.1.1");
        assertEquals(new SolutionService.Response(409, "\"frits\" is taken"), service.save("frits", "v2", "b2", "1.1.1.1"));
        assertEquals(200, service.save("Frits", "v3", "b1", "1.1.1.1").status());
        assertEquals("v3", service.load("Frits").body());
    }

    @Test
    void badNamesAreRefused() {
        for (String name : List.of("", "a b", "Zoë", "../etc", "a/b", "x".repeat(41))) {
            assertEquals(new SolutionService.Response(400, "Use only letters a-z, digits and hyphens"), service.save(name, "x", "b1", "1.1.1.1"), name);
        }
        assertEquals(200, service.save("x".repeat(40), "x", "b1", "1.1.1.1").status());
        assertEquals(400, service.load("../etc").status());
    }

    @Test
    void aSaveNeedsABrowserId() {
        assertEquals(new SolutionService.Response(400, "Missing browser id"), service.save("Frits", "x", null, "1.1.1.1"));
        assertEquals(400, service.save("Frits", "x", " ", "1.1.1.1").status());
    }

    @Test
    void theSizeIsMeasuredJsonEscaped() {
        assertEquals(200, service.save("a", "a".repeat(32 * 1024), "b1", "1.1.1.1").status());
        assertEquals(new SolutionService.Response(413, "Not saved: too large (max 32 KB)"), service.save("b", "a".repeat(32 * 1024 + 1), "b1", "1.1.1.1"));
        assertEquals(413, service.save("c", "\n".repeat(16 * 1024 + 1), "b1", "1.1.1.1").status());
    }

    @Test
    void blockedWordsAreRefusedInNameAndText() {
        assertEquals(new SolutionService.Response(422, "Not saved: contains a blocked word"), service.save("Kut", "x", "b1", "1.1.1.1"));
        assertEquals(422, service.save("Frits", "fact kut(A)", "b1", "1.1.1.1").status());
    }

    @Test
    void aBrowserSavesThirtyTimesPerHour() {
        for (int i = 0; i < 30; i++) {
            assertEquals(200, service.save("n" + i, "x", "b1", "1.1.1." + i).status());
        }
        assertEquals(new SolutionService.Response(429, "Too many saves, try again later"), service.save("n30", "x", "b1", "1.1.1.99"));
        now.addAndGet(3_600_000L);
        assertEquals(200, service.save("n30", "x", "b1", "1.1.1.99").status());
    }

    @Test
    void anIpCreatesTwoHundredNewNamesPerHourButMayUpdate() {
        for (int i = 0; i < 200; i++) {
            assertEquals(200, service.save("n" + i, "x", "b" + i, "1.1.1.1").status());
        }
        assertEquals(429, service.save("n200", "x", "b200", "1.1.1.1").status());
        assertEquals(200, service.save("n0", "y", "b0", "1.1.1.1").status());
    }

    @Test
    void growthCountsAgainstTheTotal() {
        service = service(10);
        assertEquals(200, service.save("a", "12345", "b1", "1.1.1.1").status());
        assertEquals(new SolutionService.Response(503, "Not saved: the server is full, try again later"), service.save("b", "123456", "b2", "1.1.1.1"));
        assertEquals(200, service.save("a", "1234567890", "b1", "1.1.1.1").status());
        assertEquals(503, service.save("b", "1", "b2", "1.1.1.1").status());
        assertEquals(200, service.save("a", "0987654321", "b1", "1.1.1.1").status());
        assertEquals(200, service.save("a", "123", "b1", "1.1.1.1").status());
    }

    @Test
    void anExpiredNameCanBeClaimed() {
        service.save("Frits", "v1", "b1", "1.1.1.1");
        now.addAndGet(SolutionStore.KEEP_MS + 1);
        assertEquals(200, service.save("Frits", "v2", "b2", "1.1.1.1").status());
    }

    @Test
    void anUnknownNameIsNotFound() {
        assertEquals(new SolutionService.Response(404, "No solution named \"Frits\""), service.load("Frits"));
    }

    @Test
    void clientIpIsTheLastForwardedEntry() {
        assertEquals("10.0.0.1", SolutionService.clientIp(null, "10.0.0.1"));
        assertEquals("10.0.0.1", SolutionService.clientIp(" ", "10.0.0.1"));
        assertEquals("5.6.7.8", SolutionService.clientIp("1.2.3.4, 5.6.7.8", "10.0.0.1"));
        assertEquals("9.9.9.9", SolutionService.clientIp(" 9.9.9.9 ", "10.0.0.1"));
    }

    @Test
    void concurrentSavesOfOneNameHaveOneOwner() throws Exception {
        List<Callable<Integer>> saves = IntStream.range(0, 20).<Callable<Integer>>mapToObj(i -> () -> service.save("Frits", "x" + i, "b" + i, "1.1.1." + i).status()).toList();
        try (ExecutorService pool = Executors.newFixedThreadPool(20)) {
            List<Integer> statuses = new ArrayList<>();
            for (Future<Integer> f : pool.invokeAll(saves)) {
                statuses.add(f.get());
            }
            assertEquals(1, statuses.stream().filter(s -> s == 200).count());
            assertEquals(19, statuses.stream().filter(s -> s == 409).count());
        }
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :website:test --tests "*SolutionServiceTest*"`
Expected: FAIL, compilation error "cannot find symbol SolutionService".

- [ ] **Step 3: Implement `SolutionService`** per the table and the interface above.

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :website:test --tests "*SolutionServiceTest*"`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add website/src/main/java/org/modelingvalue/nelumbo/website/SolutionService.java website/src/test/java/org/modelingvalue/nelumbo/website/SolutionServiceTest.java
git commit -m "website: shared solution rules - ownership, limits, size cap"
```

---

### Task 4: HTTP routes, `--solutions-dir`, deployment

**Files:**
- Modify: `website/src/main/java/org/modelingvalue/nelumbo/website/NelumboHttpServer.java` (constructors around line 66-87, `start()` routes)
- Modify: `website/src/main/java/org/modelingvalue/nelumbo/website/Main.java` (option parsing, constructor call at line 113, `printUsage`)
- Modify: `website/Dockerfile`, `website/docker-compose.yml`
- Modify: `CLAUDE.md` (Website Module, Deployment)
- Test: `website/src/test/java/org/modelingvalue/nelumbo/website/NelumboHttpServerTest.java`

**Interfaces:**
- Consumes: `SolutionService`, `SolutionStore`, `BlockedWords` (Tasks 1-3).
- Produces:
  - `NelumboHttpServer(KnowledgeBase baseKb, List<String> loadedFiles, long timeoutMs, int maxLspSessions, Path statsDir, Path solutionsDir)`; the 5-arg constructor passes `null`
  - `PUT /solutions/{name}`: body text/plain UTF-8, header `X-Nelumbo-Browser`; response text/plain = `Response.body()` with `Response.status()`
  - `GET /solutions/{name}`: same mapping

- [ ] **Step 1: Write the failing tests** (in `NelumboHttpServerTest`; helper `put(path, browserId, body)` next to the existing `get`)

```java
@Test
void aSolutionIsSavedAndLoadedByName() throws Exception {
    String text = "fact p(A)\r\n// Zoë ✓\n";
    HttpResponse<String> saved = put("/solutions/Frits", "b1", text);
    assertEquals(200, saved.statusCode());
    assertEquals("Saved as \"Frits\" (kept 7 days)", saved.body());
    HttpResponse<String> loaded = get("/solutions/frits");
    assertEquals(200, loaded.statusCode());
    assertEquals(text, loaded.body());
    assertTrue(loaded.headers().firstValue("Content-Type").orElse("").startsWith("text/plain"));
}

@Test
void anotherBrowserGetsTakenAndANoIdSaveIsRefused() throws Exception {
    put("/solutions/Frits", "b1", "x");
    HttpResponse<String> taken = put("/solutions/Frits", "b2", "y");
    assertEquals(409, taken.statusCode());
    assertEquals("\"Frits\" is taken", taken.body());
    assertEquals(400, put("/solutions/Other", null, "x").statusCode());
    assertEquals(404, get("/solutions/Nobody").statusCode());
}

@Test
void solutionsSurviveARestart(@TempDir Path dir) throws Exception {
    server.stop();
    KnowledgeBase base = KnowledgeBaseLoader.load(List.of(new NamedSource("fibonacci.nl", FIB_BASE)));
    server = new NelumboHttpServer(base, List.of("fibonacci.nl"), NelumboHttpServer.DEFAULT_TIMEOUT_MS, NelumboHttpServer.DEFAULT_MAX_LSP_SESSIONS, null, dir);
    port = server.start(0);
    put("/solutions/Frits", "b1", "kept");
    server.stop();
    server = new NelumboHttpServer(base, List.of("fibonacci.nl"), NelumboHttpServer.DEFAULT_TIMEOUT_MS, NelumboHttpServer.DEFAULT_MAX_LSP_SESSIONS, null, dir);
    port = server.start(0);
    assertEquals("kept", get("/solutions/Frits").body());
}
```

`put` sends `Content-Type: text/plain; charset=utf-8` and adds `X-Nelumbo-Browser` only when the id is not null.

- [ ] **Step 2: Run tests to verify they fail**

Run: `./gradlew :website:test --tests "*NelumboHttpServerTest*"`
Expected: FAIL (compilation error on the 6-arg constructor).

- [ ] **Step 3: Implement**

- `start()`: build `new SolutionService(new SolutionStore(solutionsDir, System::currentTimeMillis), BlockedWords.load(), System::currentTimeMillis, SolutionService.MAX_TOTAL_BYTES)` next to the `StatsHistory`; routes `config.routes.put(...)`/`config.routes.get(...)` on `/solutions/{name}`; client IP via `SolutionService.clientIp(ctx.header("X-Forwarded-For"), ctx.ip())`; reply with `ctx.status(r.status()).contentType("text/plain; charset=utf-8").result(r.body())`.
- `Main`: option `--solutions-dir <dir>` (colon `case` like `--stats-dir`), pass it to the 6-arg constructor. The GUI reload keeps its 4-arg constructor (memory only, like the stats). Usage: endpoints `PUT /solutions/<name>` and `GET /solutions/<name>`, option line `--solutions-dir <dir>   keep shared editor solutions in <dir> (default: memory only)`.
- `website/Dockerfile` `CMD`: add `"--solutions-dir", "/data/solutions"`.
- `website/docker-compose.yml` volumes: add `- ./solutions:/data/solutions` with the comment `# shared editor solutions (PUT/GET /solutions); /data/sites/nelumbo.nl/solutions on the host`.
- `CLAUDE.md`, Website Module server section: one bullet on `/solutions/{name}` (classes, limits, the order of checks lives in `SolutionService`, `X-Forwarded-For` last entry, `--solutions-dir`, memory only without it); Deployment: the second volume.

- [ ] **Step 4: Run the website tests**

Run: `./gradlew :website:test`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add website/src/main/java/org/modelingvalue/nelumbo/website/NelumboHttpServer.java website/src/main/java/org/modelingvalue/nelumbo/website/Main.java website/Dockerfile website/docker-compose.yml CLAUDE.md website/src/test/java/org/modelingvalue/nelumbo/website/NelumboHttpServerTest.java
git commit -m "website: PUT/GET /solutions/{name} on a mounted volume"
```

---

### Task 5: Editor buttons

**Files:**
- Modify: `website/src/main/frontend/src/nelumbo-fields.ts` (`Field`, `buildField`, new share functions)
- Modify: `website/src/main/frontend/src/fields.css` (toolbar block, lines 49-83)
- Modify: `website/src/main/resources/public/landing.html:390`, `website/src/main/resources/public/library.html:139` (add `data-no-share`)
- Modify: `website/src/main/frontend/e2e/persistence.spec.ts:65-66`
- Modify: `CLAUDE.md` (Website Module, Frontend)
- Test: `website/src/main/frontend/e2e/share.spec.ts`

**Interfaces:**
- Consumes: `PUT`/`GET /solutions/{name}` (Task 4).
- Produces: in every `.nelumbo-field` without `data-no-share`, inside `.nelumbo-field-toolbar` before Reset, a `span.nelumbo-share` with:
  - buttons `Save as...` (title `Anyone who knows the name can load it`) and `Load...`
  - hidden until opened: `input.nelumbo-share-name` (`maxlength=40`, `placeholder="name"`, `aria-label="Solution name"`), the action button (`Save` or `Load`), `Cancel`
  - `span.nelumbo-share-message` (`role="status"`), class `error` on a failure

Behavior:
- Opening a row clears the message and prefills the input from localStorage `nelumbo-share-name`. Enter = action, Escape = Cancel. The input value is trimmed; an empty name does nothing.
- Save: `PUT /solutions/<encodeURIComponent(name)>`, headers `Content-Type: text/plain; charset=utf-8` and `X-Nelumbo-Browser`, body = model text. Message = response body.
- Load: `GET`; not ok -> message = body. Ok and `model.getValue() !== field.original` -> `confirm('Replace your own text?')`; no -> nothing changes, no message. Then `replaceText(model, text)` and message `Loaded "<name>"`.
- A fetch that throws: `Not saved: no connection to the server` / `Not loaded: no connection to the server`.
- After success the name goes to `nelumbo-share-name`. After any outcome the row closes back to the two buttons; the message stays until the next opening.
- Browser id: localStorage `nelumbo-browser-id`, created with `crypto.randomUUID()`; when storage throws, one id per page (module variable).
- CSS: toolbar `flex-wrap: wrap`; input and message styled with `--nf-*` variables only, error colour `--nf-overload-fg`. Check at 315 px width that the open row wraps without horizontal scroll.

- [ ] **Step 1: Write the failing e2e test** (`share.spec.ts`; imports `test, expect, Page, Locator, Browser, Dialog` from `@playwright/test`; copy the helpers `waitForEditors`, `editorText`, `typeAtEnd` from `persistence.spec.ts`. The sandbox without a `#name` loads no example, so its field keeps the page's text and nothing races the Load)

```ts
const RUN: string = String(Date.now());

async function share(page: Page, action: 'Save' | 'Load', name: string): Promise<Locator> {
    const wrap: Locator = page.locator('.nelumbo-field-wrap').first();
    await wrap.getByRole('button', { name: action === 'Save' ? 'Save as...' : 'Load...' }).click();
    await wrap.getByLabel('Solution name').fill(name);
    await wrap.getByLabel('Solution name').press('Enter');
    return wrap.locator('.nelumbo-share-message');
}

test('a saved solution loads in another browser, padded name and all', async ({ browser }: { browser: Browser }): Promise<void> => {
    const name: string  = 'a' + RUN;
    const a:    Page    = await (await browser.newContext()).newPage();
    await a.goto('/tutorial.html#logic');
    await waitForEditors(a);
    await typeAtEnd(a, 0, '\n// by frits\n');
    await expect(await share(a, 'Save', '  ' + name + ' ')).toHaveText('Saved as "' + name + '" (kept 7 days)');
    const b: Page = await (await browser.newContext()).newPage();
    await b.goto('/sandbox.html');
    await waitForEditors(b);
    await expect(await share(b, 'Load', name)).toHaveText('Loaded "' + name + '"');
    expect(await editorText(b, 0)).toBe(await editorText(a, 0));
    await expect(await share(b, 'Save', name)).toHaveText('"' + name + '" is taken');
});

test('Load asks first only when the field holds own work', async ({ page }: { page: Page }): Promise<void> => {
    const name: string = 'b' + RUN;
    const dialogs: string[] = [];
    await page.goto('/tutorial.html#logic');
    await waitForEditors(page);
    const original: string = await editorText(page, 0);
    await typeAtEnd(page, 0, '\n// saved\n');
    await expect(await share(page, 'Save', name)).toHaveText('Saved as "' + name + '" (kept 7 days)');
    await page.locator('.nelumbo-field-wrap').first().getByRole('button', { name: 'Reset' }).click();
    page.on('dialog', (d: Dialog): void => { dialogs.push(d.message()); void d.dismiss(); });
    await expect(await share(page, 'Load', name)).toHaveText('Loaded "' + name + '"');
    expect(dialogs).toEqual([]);
    expect(await editorText(page, 0)).toBe(original + '\n// saved\n');
    await typeAtEnd(page, 0, '// mine\n');
    await share(page, 'Load', name);
    await expect.poll((): string[] => dialogs).toEqual(['Replace your own text?']);
    expect(await editorText(page, 0)).toBe(original + '\n// saved\n// mine\n');
});

test('Reset after Load brings back the original', async ({ page }: { page: Page }): Promise<void> => {
    const name: string = 'c' + RUN;
    await page.goto('/tutorial.html#logic');
    await waitForEditors(page);
    const original: string = await editorText(page, 0);
    await typeAtEnd(page, 0, '\n// c\n');
    await expect(await share(page, 'Save', name)).toHaveText('Saved as "' + name + '" (kept 7 days)');
    await page.locator('.nelumbo-field-wrap').first().getByRole('button', { name: 'Reset' }).click();
    await expect(await share(page, 'Load', name)).toHaveText('Loaded "' + name + '"');
    await page.locator('.nelumbo-field-wrap').first().getByRole('button', { name: 'Reset' }).click();
    expect(await editorText(page, 0)).toBe(original);
});

test('the name field remembers the last name', async ({ page }: { page: Page }): Promise<void> => {
    const name: string = 'd' + RUN;
    await page.goto('/sandbox.html');
    await waitForEditors(page);
    await expect(await share(page, 'Save', name)).toHaveText('Saved as "' + name + '" (kept 7 days)');
    const wrap: Locator = page.locator('.nelumbo-field-wrap').first();
    await wrap.getByRole('button', { name: 'Load...' }).click();
    await expect(wrap.getByLabel('Solution name')).toHaveValue(name);
});

test('the landing page and the library have no share buttons', async ({ page }: { page: Page }): Promise<void> => {
    for (const path of ['/', '/library.html']) {
        await page.goto(path);
        await waitForEditors(page);
        await expect(page.getByRole('button', { name: 'Save as...' })).toHaveCount(0);
    }
});
```

In `persistence.spec.ts` replace lines 65-66 (the toolbar is now always there) by `await expect(reset).toBeHidden();` with the comment `// Reset shows only for an edited field`.

- [ ] **Step 2: Run the e2e test to verify it fails**

Stop any server on port 8899 first (a reused server runs an old jar).
Run: `cd website/src/main/frontend && npm run test:e2e -- share.spec.ts`
Expected: FAIL, no button "Save as...".

- [ ] **Step 3: Implement** the behavior above: `addShareButtons(toolbar: HTMLDivElement, field: Field): void` and `browserId(): string` in `nelumbo-fields.ts`, called from `buildField` after the `Field` exists, unless `div.hasAttribute('data-no-share')`; CSS; `data-no-share` on the landing and library fields.

- [ ] **Step 4: Type check, then run the e2e suite**

Run: `cd website/src/main/frontend && npm run check && npm run test:e2e`
Expected: `tsc` without errors; all specs PASS (share, persistence and the rest).

- [ ] **Step 5: Update `CLAUDE.md`**, Website Module frontend: one bullet on the share buttons (`data-no-share` opt-out, the two localStorage keys, the confirm rule and why, message stays until the next opening).

- [ ] **Step 6: Commit**

```bash
git add website/src/main/frontend/src/nelumbo-fields.ts website/src/main/frontend/src/fields.css website/src/main/resources/public/landing.html website/src/main/resources/public/library.html website/src/main/frontend/e2e/share.spec.ts website/src/main/frontend/e2e/persistence.spec.ts CLAUDE.md
git commit -m "website: Save as / Load buttons on tutorial and sandbox editors"
```

---

### Final check

- [ ] Run: `./gradlew :website:test` and `cd website/src/main/frontend && npm run test:e2e`. Expected: all PASS.
