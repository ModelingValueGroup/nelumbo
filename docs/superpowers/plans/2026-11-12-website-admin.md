# Website Admin Page Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** An admin page on nelumbo.nl, only for the office IP (`ADMIN_IPS`) and localhost, with the server stats, a switch for sharing and the shared snippets (name, time, size, owner, IP, text, delete).

**Architecture:** One new class `ClientAccess` decides the client IP (trusting `X-Forwarded-For` only with Traefik's secret header) and who is admin. `SolutionStore` gains list/delete/the sharing switch and the IP per snippet. `NelumboHttpServer` adds `/admin*` routes that answer 404 to non-admins. `status.html` becomes `admin.html` with two tabs, driven by a new bundle `admin.ts` that includes `status-chart.ts`.

**Tech Stack:** Java 21, Javalin 7.2, Jackson, JUnit 5; TypeScript + esbuild IIFE bundles; Playwright e2e.

**Spec:** `docs/superpowers/specs/2026-11-12-website-admin-design.md`

## Global Constraints

- NO push, NO merge to `develop`/`master`, NO deploy, nothing on server1: a workshop runs on nelumbo.nl (2026-11-12). Commit on `local/work` only.
- Every new Java file starts with the LGPL header (copy it from `website/src/main/java/org/modelingvalue/nelumbo/website/Solution.java`).
- Java: arrow-form `switch` in new code; align variable definitions in columns (type, name, value).
- TypeScript: type every variable and argument; one variable per definition; `if`/`while`/`for` always multi-line with `{ }`.
- ASCII only in code and text: plain hyphens, straight quotes.
- Comments only where the logic is unclear.
- UI text in English. Exact strings: `Sharing is turned off`; tooltip `Anyone who knows the name can load it. Your IP address is kept with it for 7 days.`; confirmations `Delete "<name>"?` and `Delete all <n> shared snippets?`.
- Env vars `ADMIN_IPS` (comma-separated addresses or CIDR ranges) and `PROXY_SECRET`; header `X-Nelumbo-Proxy`; marker file `sharing-off`.
- Every `/admin` response for a non-admin is the same 404 as an unknown route; admin responses carry `Cache-Control: no-store`.
- Admin actions only via `PUT`/`DELETE`; no CORS headers anywhere.
- Snippet names, texts and IPs are rendered with `textContent`, never `innerHTML`.

## Review Focus

- Jetty may report the TCP peer as `[0:0:0:0:0:0:0:1]`, `0:0:0:0:0:0:0:1` or with a zone (`fe80::1%eth0`): localhost must still be admin, the same address must still match a range. Pinned in Task 1 (`peerAddressFormsAreNormalized`).
- A too-wide range in `ADMIN_IPS` (`0.0.0.0/0`, `::/0`, `1.0.0.0/1`) would make the world admin: skipped. Pinned in Task 1 (`tooWideRangesAreSkipped`).
- A shared text containing HTML (`<img src=x onerror=...>`) opened on the admin page must show as text and run nothing. Pinned in Task 5 (e2e `a snippet text shows as text`).
- Opening `/admin#snippets` and then the Stats tab: charts drawn while hidden would have width 0. Pinned in Task 5 (e2e `stats drawn after the snippets tab have their full width`).
- An editor tab opened before sharing was switched off still shows the globe: its share must get `Sharing is turned off`. Pinned in Task 6 (e2e `switching sharing off hides the globe and refuses shares`).

---

### Task 1: `ClientAccess`: client IP and admin check

**Files:**
- Create: `website/src/main/java/org/modelingvalue/nelumbo/website/ClientAccess.java`
- Test: `website/src/test/java/org/modelingvalue/nelumbo/website/ClientAccessTest.java`

**Interfaces:**
- Produces:
  - `static final String ClientAccess.PROXY_HEADER = "X-Nelumbo-Proxy"`
  - `static ClientAccess ClientAccess.of(String adminIps, String proxySecret)`: both may be null or blank; prints skipped entries and warnings on `System.err`.
  - `String clientIp(String forwardedFor, String proxyHeader, String remoteIp)`
  - `boolean isAdmin(String clientIp)`

- [ ] **Step 1: Write the failing tests**

```java
@Test void forwardedForCountsOnlyWithTheSecret() {
    ClientAccess access = ClientAccess.of(null, "s3cret");
    assertEquals("5.6.7.8",  access.clientIp("1.2.3.4, 5.6.7.8", "s3cret", "10.0.0.1"));
    assertEquals("10.0.0.1", access.clientIp("5.6.7.8", null, "10.0.0.1"));
    assertEquals("10.0.0.1", access.clientIp("5.6.7.8", "wrong", "10.0.0.1"));
    assertEquals("10.0.0.1", access.clientIp(" ", "s3cret", "10.0.0.1"));
    assertEquals("10.0.0.1", ClientAccess.of(null, null).clientIp("5.6.7.8", "", "10.0.0.1"));
}
@Test void loopbackIsAdminWithoutAnyConfiguration() {
    ClientAccess access = ClientAccess.of(null, null);
    assertTrue(access.isAdmin("127.0.0.1"));
    assertTrue(access.isAdmin("::1"));
    assertFalse(access.isAdmin("192.0.2.10"));
    assertFalse(access.isAdmin("localhost"));       // not a literal: no DNS lookup, not admin
}
@Test void addressesAndRangesMatch() {
    ClientAccess access = ClientAccess.of("192.0.2.10, 2001:db8:1234::/48", "s");
    assertTrue(access.isAdmin("192.0.2.10"));
    assertFalse(access.isAdmin("192.0.2.11"));
    assertTrue(access.isAdmin("2001:db8:1234:5:6:7:8:9"));
    assertFalse(access.isAdmin("2001:db8:1235::1"));
    assertTrue(access.isAdmin("::ffff:192.0.2.10"));
}
@Test void peerAddressFormsAreNormalized() {
    ClientAccess access = ClientAccess.of("2001:db8::/32", "s");
    assertTrue(access.isAdmin("[0:0:0:0:0:0:0:1]"));
    assertTrue(access.isAdmin("0:0:0:0:0:0:0:1"));
    assertTrue(access.isAdmin("[2001:db8::5]"));
    assertTrue(access.isAdmin("2001:db8::5%eth0"));
}
@Test void badAndPrivateEntriesAreSkipped() {
    ClientAccess access = ClientAccess.of("nonsense, 10.1.2.3, 192.168.0.0/16, 172.17.0.1, fd00::1, fe80::/10, 1.2.3.4/33, example.com, 198.51.100.7", "s");
    assertFalse(access.isAdmin("10.1.2.3"));
    assertFalse(access.isAdmin("192.168.1.1"));
    assertFalse(access.isAdmin("172.17.0.1"));
    assertFalse(access.isAdmin("fd00::1"));
    assertTrue(access.isAdmin("198.51.100.7"));     // the good entry after the bad ones still counts
}
@Test void tooWideRangesAreSkipped() {
    ClientAccess access = ClientAccess.of("0.0.0.0/0, ::/0, 1.0.0.0/1, 2000::/3, 203.0.0.0/15", "s");
    assertFalse(access.isAdmin("1.2.3.4"));
    assertFalse(access.isAdmin("2001:db8::1"));
    assertFalse(access.isAdmin("203.0.113.1"));
}
```

- [ ] **Step 2: Run them to see them fail**

Run: `./gradlew :website:test --tests "org.modelingvalue.nelumbo.website.ClientAccessTest"`
Expected: compilation failure, `ClientAccess` does not exist.

- [ ] **Step 3: Implement `ClientAccess`**

- `of`: split `adminIps` on commas, strip each entry, skip empty ones. Skip (stderr `admin: skipping ADMIN_IPS entry "<entry>": <reason>`) an entry that is not a literal, has a bad prefix (IPv4 > 32, IPv6 > 128, not a number), is wider than /16 (IPv4) or /32 (IPv6), or whose network address is loopback, any-local, site-local, link-local or ULA (`fc00::/7`: first byte `& 0xfe == 0xfc`). An address without `/` is a full-length range. When `adminIps` is non-blank and `proxySecret` blank: stderr `admin: ADMIN_IPS is set without PROXY_SECRET; behind Traefik no client address will match`.
- Literal check before `InetAddress.getByName` (which resolves host names): after stripping `[`, `]` and a `%zone`, IPv4 must match `\d{1,3}(\.\d{1,3}){3}`, IPv6 must contain `:` and only `[0-9A-Fa-f:.]`. `getByName` turns `::ffff:a.b.c.d` into an `Inet4Address` by itself.
- Range match: same address length, the first `prefix` bits equal.
- `clientIp`: the LAST comma entry of `forwardedFor`, stripped, only when `forwardedFor` is not blank, a secret is configured and `proxyHeader` equals it (`MessageDigest.isEqual` on the UTF-8 bytes); otherwise `remoteIp`.
- `isAdmin`: false for a non-literal; true for a loopback address; else true when a range matches.

- [ ] **Step 4: Run the tests**

Run: `./gradlew :website:test --tests "org.modelingvalue.nelumbo.website.ClientAccessTest"`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add website/src/main/java/org/modelingvalue/nelumbo/website/ClientAccess.java website/src/test/java/org/modelingvalue/nelumbo/website/ClientAccessTest.java
git commit -m "website: ClientAccess decides the client IP (proxy secret) and who is admin"
```

---

### Task 2: Use `ClientAccess` in the server, Main and docker-compose

**Files:**
- Modify: `website/src/main/java/org/modelingvalue/nelumbo/website/NelumboHttpServer.java` (constructors, the `PUT /solutions/{name}` route)
- Modify: `website/src/main/java/org/modelingvalue/nelumbo/website/SolutionService.java` (remove `clientIp`)
- Modify: `website/src/main/java/org/modelingvalue/nelumbo/website/Main.java` (env vars, usage text)
- Modify: `website/docker-compose.yml`
- Test: `website/src/test/java/org/modelingvalue/nelumbo/website/NelumboHttpServerTest.java`, `SolutionServiceTest.java`

**Interfaces:**
- Consumes: `ClientAccess.of`, `clientIp`, `PROXY_HEADER` (Task 1).
- Produces:
  - constructor `NelumboHttpServer(KnowledgeBase baseKb, List<String> loadedFiles, long timeoutMs, int maxLspSessions, Path statsDir, Path solutionsDir, ClientAccess access)`; the 6-argument one passes `ClientAccess.of(null, null)`.
  - `private String clientIp(Context ctx)` in `NelumboHttpServer`: `access.clientIp(ctx.header("X-Forwarded-For"), ctx.header(ClientAccess.PROXY_HEADER), ctx.ip())`.

- [ ] **Step 1: Change the tests**

In `NelumboHttpServerTest` add a helper that restarts `server` with a `ClientAccess` (same base KB, default timeout and sessions, null dirs), and:
- `putFrom` gets a fourth argument `String secret`: sent as `X-Nelumbo-Proxy` unless null.
- `theNewNameLimitCountsTheAddressTraefikAppended`: restart with `ClientAccess.of(null, "s3cret")`; every `putFrom` sends secret `s3cret`. Assertions unchanged.
- New `forwardedForWithoutTheSecretIsIgnored`: restart with `ClientAccess.of(null, "s3cret")`; 200 `putFrom` calls with different `X-Forwarded-For` values and NO secret all count for the one local address, so new name number `NEW_NAMES_PER_IP_PER_HOUR + 1` gets 429.

In `SolutionServiceTest` delete `clientIpIsTheLastForwardedEntry` (covered by `ClientAccessTest`).

- [ ] **Step 2: Run them to see them fail**

Run: `./gradlew :website:test --tests "org.modelingvalue.nelumbo.website.NelumboHttpServerTest"`
Expected: compilation failure (no 7-argument constructor).

- [ ] **Step 3: Implement**

- `NelumboHttpServer`: the new constructor stores `access`; the `PUT /solutions/{name}` route passes `clientIp(ctx)`.
- `SolutionService`: remove `clientIp`.
- `Main`: `ClientAccess.of(System.getenv("ADMIN_IPS"), System.getenv("PROXY_SECRET"))` into the 7-argument constructor. Usage text, after the options: `Environment: ADMIN_IPS  addresses or CIDR ranges that may open /admin (localhost always may)` and `PROXY_SECRET  value of the X-Nelumbo-Proxy header Traefik adds; X-Forwarded-For is trusted only with it`.
- `website/docker-compose.yml`, service `nelumbo`:

```yaml
    environment:
      - ADMIN_IPS=${ADMIN_IPS:-}
      - PROXY_SECRET=${PROXY_SECRET:-}
```

  labels: `"traefik.http.routers.nelumbo.middlewares=nelumbo-hsts,nelumbo-proxy"` and `"traefik.http.middlewares.nelumbo-proxy.headers.customrequestheaders.X-Nelumbo-Proxy=${PROXY_SECRET}"`.

- [ ] **Step 4: Run the website tests**

Run: `./gradlew :website:test`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add website/src/main/java website/src/test/java website/docker-compose.yml
git commit -m "website: trust X-Forwarded-For only with Traefik's secret header"
```

---

### Task 3: Store: IP per snippet, list, delete, sharing switch

**Files:**
- Modify: `website/src/main/java/org/modelingvalue/nelumbo/website/Solution.java`
- Modify: `website/src/main/java/org/modelingvalue/nelumbo/website/SolutionStore.java`
- Modify: `website/src/main/java/org/modelingvalue/nelumbo/website/SolutionService.java` (`save`, `load`)
- Test: `website/src/test/java/org/modelingvalue/nelumbo/website/SolutionStoreTest.java`, `SolutionServiceTest.java`

**Interfaces:**
- Produces:
  - `record Solution(String name, String ownerHash, long savedAt, String text, String ip)`; `ip` may be null.
  - `SolutionStore`: `static final String SHARING_OFF_FILE = "sharing-off"`; `synchronized List<Solution> list()` (unexpired, newest `savedAt` first); `synchronized boolean delete(String name)` (false when unknown); `synchronized int deleteAll()`; `synchronized boolean sharingEnabled()`; `synchronized void setSharingEnabled(boolean enabled)`.
  - `SolutionService`: `static final String SHARING_OFF = "Sharing is turned off"`; `save`/`load` answer `403 SHARING_OFF` first while off; `save` stores its `ip` argument (the full address, not `ipKey`).

- [ ] **Step 1: Write the failing tests**

Existing `new Solution(...)` calls get a fifth argument (`"1.1.1.1"` or `null`). New in `SolutionStoreTest`:

```java
@Test void theIpIsStoredAndAnOldFileWithoutItLoads() throws Exception {
    store(dir).put(new Solution("Frits", "h1", now.get(), "x", "2001:db8::7"));
    assertEquals("2001:db8::7", store(dir).get("Frits").orElseThrow().ip());
    Files.writeString(dir.resolve("old.json"), "{\"name\":\"Old\",\"ownerHash\":\"h2\",\"savedAt\":" + now.get() + ",\"text\":\"y\"}");
    assertNull(store(dir).get("Old").orElseThrow().ip());
}
@Test void listIsNewestFirstWithoutExpired() {
    SolutionStore store = store(dir);
    store.put(new Solution("A", "h1", now.get(), "a", null));
    now.addAndGet(1000);
    store.put(new Solution("B", "h1", now.get(), "b", null));
    assertEquals(List.of("B", "A"), store.list().stream().map(Solution::name).toList());
    now.addAndGet(SolutionStore.KEEP_MS);
    assertEquals(List.of(), store.list());
}
@Test void deleteFreesTheNameAndItsBytes() {
    SolutionStore store = store(dir);
    store.put(new Solution("Frits", "h1", now.get(), "abc", null));
    assertTrue(store.delete("FRITS"));
    assertFalse(store.delete("Frits"));
    assertEquals(0, store.totalBytes());
    assertFalse(Files.exists(dir.resolve("frits.json")));
    assertTrue(store(dir).get("Frits").isEmpty());
}
@Test void deleteAllCountsAndEmpties() {
    SolutionStore store = store(dir);
    store.put(new Solution("A", "h1", now.get(), "a", null));
    store.put(new Solution("B", "h2", now.get(), "b", null));
    assertEquals(2, store.deleteAll());
    assertEquals(0, store.count());
    assertEquals(0, store.totalBytes());
    assertEquals(0, store(dir).count());
}
@Test void theSharingSwitchSurvivesARestart() {
    Path fresh = dir.resolve("not-yet-there");
    assertTrue(store(fresh).sharingEnabled());
    store(fresh).setSharingEnabled(false);
    assertFalse(store(fresh).sharingEnabled());
    assertTrue(Files.exists(fresh.resolve(SolutionStore.SHARING_OFF_FILE)));
    store(fresh).setSharingEnabled(true);
    assertTrue(store(fresh).sharingEnabled());
    SolutionStore memory = store(null);
    memory.setSharingEnabled(false);
    assertFalse(memory.sharingEnabled());
}
```

New in `SolutionServiceTest`:

```java
@Test void sharingOffRefusesSaveAndLoad() {
    service.save("Frits", "x", "b1", "1.1.1.1");
    store.setSharingEnabled(false);        // keep the store in a field of the test
    assertEquals(new SolutionService.Response(403, "Sharing is turned off"), service.save("Other", "x", "b1", "1.1.1.1"));
    assertEquals(new SolutionService.Response(403, "Sharing is turned off"), service.load("Frits"));
    assertEquals(new SolutionService.Response(403, "Sharing is turned off"), service.save("bad name", "x", "b1", "1.1.1.1"));
}
@Test void aSaveKeepsTheFullAddress() {
    service.save("Frits", "x", "b1", "2001:db8:1:2:3:4:5:6");
    assertEquals("2001:db8:1:2:3:4:5:6", store.get("Frits").orElseThrow().ip());
}
```

- [ ] **Step 2: Run them to see them fail**

Run: `./gradlew :website:test --tests "org.modelingvalue.nelumbo.website.Solution*"`
Expected: compilation failure.

- [ ] **Step 3: Implement**

- `SolutionStore`: `sharingEnabled` read at construction (`!Files.exists(dir.resolve(SHARING_OFF_FILE))`, true without a dir); `setSharingEnabled` creates the directory if needed, then writes an empty marker or deletes it; IO failures go to stderr like `write`. `delete`/`deleteAll` adjust `totalBytes` and delete the files. `load()` already ignores files that are not `.json`/`.tmp`, so the marker is never read as a snippet: keep it that way.
- `SolutionService`: the sharing check before every other check in `save` and `load`.

- [ ] **Step 4: Run the website tests**

Run: `./gradlew :website:test`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add website/src/main/java website/src/test/java
git commit -m "website: snippets keep the sharer's IP; list, delete and a sharing switch in the store"
```

---

### Task 4: Admin endpoints, public sharing state, `/status.html` gone

**Files:**
- Modify: `website/src/main/java/org/modelingvalue/nelumbo/website/NelumboHttpServer.java`
- Test: `website/src/test/java/org/modelingvalue/nelumbo/website/NelumboHttpServerTest.java`

**Interfaces:**
- Consumes: `ClientAccess.isAdmin` (Task 1), `clientIp(Context)` (Task 2), the `SolutionStore` methods (Task 3).
- Produces (HTTP):
  - `GET /admin/me` -> 204 (admin).
  - `GET /admin` -> the page `/public/admin.html` (Task 5 creates it; until then serve a placeholder string, Task 5 replaces it).
  - `GET /admin/solutions` -> JSON array of `{name, savedAt, bytes, owner, ip}`: `bytes` = UTF-8 length of the text (without `ENTRY_BYTES`), `owner` = first 6 characters of `ownerHash`, `ip` null when unknown (`Map.of` rejects null: use a record or a `LinkedHashMap`).
  - `GET /admin/solutions/{name}` -> `text/plain` text or 404; works while sharing is off.
  - `DELETE /admin/solutions/{name}` -> 204 or 404.
  - `DELETE /admin/solutions` -> JSON `{deleted: <n>}`.
  - `GET /admin/sharing` -> JSON `{enabled: bool}`; `PUT /admin/sharing` with JSON `{enabled: bool}` -> the same JSON; another body -> 400.
  - Public `GET /solutions` -> JSON `{enabled: bool}`.
  - Every `/admin*` route above wrapped by `private Handler admin(Handler handler)`: non-admin -> `throw new NotFoundResponse()` (io.javalin.http), else header `Cache-Control: no-store`, then the handler.
  - `/status.html` route removed (404).

- [ ] **Step 1: Write the failing tests**

A helper `request(String method, String path, String body, String... headers)` next to `get`/`put`. A non-admin = server restarted with `ClientAccess.of(null, "s3cret")` and headers `X-Nelumbo-Proxy: s3cret`, `X-Forwarded-For: 203.0.113.5`.

```java
@Test void adminPathsAre404ForANonAdmin() // restart; for each of GET /admin, GET /admin/me, GET /admin/solutions,
    // GET /admin/solutions/Frits, DELETE /admin/solutions/Frits, DELETE /admin/solutions, GET /admin/sharing, PUT /admin/sharing:
    // status 404 and the same body as GET /no-such-page
@Test void localhostIsAdmin() // GET /admin/me -> 204 with Cache-Control no-store; GET /admin -> 200
@Test void adminListsOpensAndDeletesSnippets()
    // put Frits (b1, "abc"), put Anna (b2, "x"); GET /admin/solutions: 2 rows;
    // the row named Frits: bytes 3, owner length 6, ip not null; GET /admin/solutions/frits -> "abc";
    // DELETE /admin/solutions/Frits -> 204, again -> 404; GET /solutions/Frits -> 404;
    // DELETE /admin/solutions -> {"deleted":1}
@Test void theSharingSwitchStopsShareAndLoad()
    // put Frits; GET /solutions -> {"enabled":true}; PUT /admin/sharing {"enabled":false} -> {"enabled":false};
    // GET /solutions -> {"enabled":false}; put Other -> 403 "Sharing is turned off"; GET /solutions/Frits -> 403;
    // GET /admin/solutions/Frits -> 200 "x"; PUT /admin/sharing "nonsense" -> 400; PUT {"enabled":true} -> load works again
@Test void theStatusPageIsGone() // GET /status.html -> 404
```

Replace `statusPageAndDotScriptAreServed`: `/status-dot.js` is still served as JavaScript (keep its assertions); drop the `/status.html` and `/assets/status-chart.js` parts (Task 5 adds the admin bundle's).

- [ ] **Step 2: Run them to see them fail**

Run: `./gradlew :website:test --tests "org.modelingvalue.nelumbo.website.NelumboHttpServerTest"`
Expected: the new tests FAIL (404 for localhost, no `/solutions` route, `/status.html` 200).

- [ ] **Step 3: Implement the routes in `NelumboHttpServer.start`**

Keep the `SolutionStore` in a local variable so the admin routes use it directly.

- [ ] **Step 4: Run the website tests**

Run: `./gradlew :website:test`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add website/src/main/java website/src/test/java
git commit -m "website: /admin endpoints for snippets and the sharing switch, 404 for non-admins"
```

---

### Task 5: Admin page and the header link

**Files:**
- Move: `website/src/main/resources/public/status.html` -> `website/src/main/resources/public/admin.html` (`git mv`)
- Create: `website/src/main/frontend/src/admin.ts`
- Modify: `website/src/main/frontend/esbuild.mjs` (second build: entry `src/admin.ts`, `globalName: 'NelumboAdmin'`, replacing the `status-chart.ts` entry)
- Modify: `website/src/main/resources/public/theme.js` (admin link; remove the shift+click to `/status.html`)
- Modify: `tutorial.html`, `sandbox.html`, `library.html`, `docs.html`, `landing.html` in `website/src/main/resources/public/` (drop `data-status-link` from the status dot)
- Modify: `website/src/main/java/org/modelingvalue/nelumbo/website/NelumboHttpServer.java` (serve `admin.html` at `/admin`)
- Modify: `website/src/main/frontend/playwright.config.ts` (third server)
- Test: `NelumboHttpServerTest.java`, `website/src/main/frontend/e2e/admin.spec.ts` (new), `website/src/main/frontend/e2e/status.spec.ts`

**Interfaces:**
- Consumes: the HTTP endpoints of Task 4; `start()` exported by `src/status-chart.ts` (unchanged).
- Produces:
  - `admin.ts`: `export function start(): void` (page script: `<script src="/assets/admin.js"></script><script>NelumboAdmin.start();</script>`, stylesheet `/assets/admin.css`).
  - DOM the e2e tests use: tabs `role="tab"` named `Stats` and `Snippets` (`aria-selected`), panels `#panel-stats`, `#panel-snippets`; checkbox `#sharing` (`role="switch"`, label `Sharing from the editor`); buttons `Refresh` and `Delete all...`; table `#snippets` with header `Name, Saved, Size, Owner, IP` and per row a name button (`.snippet-name`, `aria-expanded`) and a `Delete` button; an opened text in `tr.snippet-text pre`.
  - Header link: `<a class="admin-link" href="/admin">Admin</a>`, before the GitHub link of `.site-header nav`, `aria-current="page"` on `/admin`.

- [ ] **Step 1: Write the failing tests**

`NelumboHttpServerTest`:
- `adminPageLoadsItsBundle`: `GET /admin` contains `src="/assets/admin.js"` and `href="/assets/admin.css"`; both assets 200.
- `everyPageWithAVersionShowsTheStatusDotButNoStatusLink` becomes `everyPageWithAVersionShowsTheStatusDotButNoAdminLink`: the dot and `status-dot.js` as before; no `href="/admin"` and no `data-status-link` in the page's HTML.

`playwright.config.ts`: a third `webServer` on port 8897 with `env: { ...(process.env as Record<string, string>), PROXY_SECRET: 'e2e-secret' }`, same command form as the first. The admin specs that change state use it, so `share.spec.ts` on 8899 is not disturbed.

`e2e/admin.spec.ts` (`test.use({ baseURL: 'http://localhost:8897' })`):
- `localhost sees the admin link and the page opens on Stats`: `/tutorial.html` -> link `Admin` visible -> click -> URL `/admin`; tab `Stats` `aria-selected="true"`; `#sessions` matches `/^\d+ \/ \d+$/`.
- `a non-admin sees no link and gets 404`: new context with `extraHTTPHeaders: { 'X-Nelumbo-Proxy': 'e2e-secret', 'X-Forwarded-For': '203.0.113.5' }`; `/tutorial.html` after the `/admin/me` response: `a.admin-link` count 0; `request.get('/admin')` status 404.
- `snippets: list, open, delete one, delete all`: create two snippets with `request.put('/solutions/<run-unique name>', { headers: { 'X-Nelumbo-Browser': 'e2e-1' }, data: '...' })`; `/admin#snippets`: both rows, IP cell not `-`; click a name -> `aria-expanded="true"` and the `pre` text; `Delete` with the dialog `Delete "<name>"?` accepted -> row gone, `GET /solutions/<name>` 404; `Delete all...` with a dialog matching `/^Delete all \d+ shared snippets\?$/` accepted -> no rows (a reused local server may hold more snippets).
- `a snippet text shows as text`: share `<img src=x onerror="window.__xss=1">`; open it on the admin page; `pre` innerText equals it, `#snippets img` count 0, `window.__xss` undefined.
- `stats drawn after the snippets tab have their full width`: `/admin#snippets`, click `Stats`; `#chart-cpu .uplot` bounding box width > 200.
- `the admin link fits a narrow header`: viewport 315 x 700 on `/tutorial.html`: no horizontal overflow (`scrollWidth <= clientWidth`).

`e2e/status.spec.ts`: every `/status.html` becomes `/admin` (on 8899; Playwright is localhost, so admin). The first test becomes `the status dot shows the server status; shift+click does nothing`: dot class as before, shift+click keeps the URL at `/`.

- [ ] **Step 2: Run them to see them fail**

Run: `./gradlew :website:test --tests "org.modelingvalue.nelumbo.website.NelumboHttpServerTest"`
Expected: FAIL (`/admin` is the placeholder).

- [ ] **Step 3: Implement**

- `admin.html`: title `Nelumbo admin`; the site header as in `sandbox.html` without `#nav-toggle` (copy its header CSS block); the tab list; `#panel-stats` = the current `<main>` content of `status.html` (without its `<h1>` link line to Home); `#panel-snippets` hidden; footer as before.
- `admin.ts`:
  - The tab comes from `location.hash` (`#stats` default, `#snippets`); switching sets the hash.
  - `startStats()` from `status-chart.ts` runs on the FIRST show of Stats, and every later show dispatches `window.dispatchEvent(new Event('resize'))` (charts resized while hidden have width 0).
  - Snippets load on the first show of their tab and on `Refresh`.
  - The switch reads `GET /admin/sharing` and writes `PUT /admin/sharing`; on failure it goes back and shows the error.
  - Times as `YYYY-MM-DD HH:MM`, local, 24-hour; sizes as `<n> B` below 1024, else `<x.x> KB`; `ip` null shows `-`.
  - All text via `textContent`.
- `theme.js`: on `DOMContentLoaded`, when `.site-header nav` exists, `fetch('/admin/me')`; on 204 insert the link and a `<style>` with `@media (max-width: 440px) { .site-header nav a.admin-link { display: none; } }`. Update the header comment (no more status shortcut).
- `NelumboHttpServer`: `GET /admin` serves `loadResource("/public/admin.html")` through `admin(...)`.

- [ ] **Step 4: Run the tests**

Run: `./gradlew :website:test` then `cd website/src/main/frontend && npm run check && npm run test:e2e`
Expected: PASS. No server may still run on 8897-8899 (`reuseExistingServer` would test an old jar).

- [ ] **Step 5: Commit**

```bash
git add -A website/src/main website/src/test
git commit -m "website: admin page with Stats and Snippets tabs, Admin link in the header for admins"
```

---

### Task 6: Editors follow the sharing switch; the IP tooltip

**Files:**
- Modify: `website/src/main/frontend/src/nelumbo-fields.ts` (`addShareButtons` call at the field build, `shareAs.title`)
- Test: `website/src/main/frontend/e2e/admin.spec.ts`, `website/src/main/frontend/e2e/share.spec.ts`

**Interfaces:**
- Consumes: `GET /solutions` -> `{enabled}` and the 403 body of Task 4.
- Produces: `function sharingEnabled(): Promise<boolean>`: one `fetch('/solutions')` per page (cached promise), false on any failure.

- [ ] **Step 1: Write the failing tests**

- `share.spec.ts`, new `the share item tells that the IP is kept`: open the globe menu; menu item `Share...` has title `Anyone who knows the name can load it. Your IP address is kept with it for 7 days.`
- `admin.spec.ts`, new `switching sharing off hides the globe and refuses shares` (8897): page B `/tutorial.html#logic`, globe visible; admin page A turns `#sharing` off; B shares a name -> message `Sharing is turned off`; B reloads -> no `Share or load by name` button in the page; `finally`: `request.put('/admin/sharing', { data: { enabled: true } })`.

- [ ] **Step 2: Run them to see them fail**

Run: `cd website/src/main/frontend && npx playwright test e2e/share.spec.ts e2e/admin.spec.ts` (after `./gradlew :website:serverJar`)
Expected: both new tests FAIL (old tooltip; globe still there after the reload).

- [ ] **Step 3: Implement**

At the field build: `void sharingEnabled().then(...)` calls `addShareButtons(toolbar, field)` only on `true` (adding the buttons later instead of hiding them keeps the toolbar's `:has(button:not([hidden]))` rule right). New tooltip text on `shareAs.title`.

- [ ] **Step 4: Run the e2e tests**

Run: `cd website/src/main/frontend && npm run check && npm run test:e2e`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add website/src/main/frontend
git commit -m "website: editors show the globe only while sharing is on; tooltip says the IP is kept"
```

---

### Task 7: Evaluations per second in the stats charts

Requested by Tom during the build (2026-11-12): "a good indication for how busy our website users are".

**Files:**
- Modify: `website/src/main/frontend/src/status-chart.ts`
- Modify: `website/src/main/resources/public/admin.html` (`#panel-stats`)
- Test: `website/src/main/frontend/e2e/status.spec.ts`

**Interfaces:**
- Consumes: `/stats/history` points: `evaluations` = evaluations finished in the point (per minute for `hour`/`day`, summed over 10 minutes for `week`), and `resolutionSeconds`.
- Produces:
  - Chart container `<div id="chart-evals">` in `#panel-stats`, second, right after the CPU chart, heading `Evaluations per second (average per point)`.
  - One series, legend label `Evaluations/s`, stroke `--series-2` (the colour of "Evaluations running").
  - Data table column `Eval/s` right after `Evaluations`.

- [ ] **Step 1: Write the failing test**

In `status.spec.ts`:

```ts
test('the status page shows evaluations per second', async ({ page }: { page: Page }): Promise<void> => {
    await page.goto('/admin');
    await expect(page.locator('#chart-evals .uplot')).toHaveCount(1, { timeout: 10_000 });
    await expect(page.locator('#chart-evals .u-legend')).toContainText('Evaluations/s');
    await expect(page.locator('#history-table thead')).toContainText('Eval/s');
    const order: string[] = await page.locator('#panel-stats .chart > div[id^="chart-"]').evaluateAll((els: Element[]): string[] => els.map((e: Element): string => e.id));
    expect(order.slice(0, 2)).toEqual(['chart-cpu', 'chart-evals']);
});
```

- [ ] **Step 2: Run it to see it fail**

Run: `./gradlew :website:serverJar` then `cd website/src/main/frontend && npx playwright test e2e/status.spec.ts`
Expected: the new test FAILS (no `#chart-evals`).

- [ ] **Step 3: Implement**

- Rate per point = `evaluations / resolutionSeconds`; axis and legend values with 2 decimals, through an own formatter like the other charts.
- The live point appended from the latest `/stats` sample (it has `evaluations: 0`) gets no rate (null, a gap), not 0.
- Add `'chart-evals'` to `CHART_IDS` in display order (the resize handler maps by index).
- Same conventions as the other charts: one unit per chart, 24-hour clock, downtime as gaps, legend swatch as a line. Load the `dataviz` skill first if it is available.

- [ ] **Step 4: Run the tests**

Run: `cd website/src/main/frontend && npm run check && npx playwright test e2e/status.spec.ts e2e/admin.spec.ts`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add website/src/main/frontend website/src/main/resources/public/admin.html
git commit -m "website: evaluations per second chart on the admin Stats tab"
```

---

### Task 8: CLAUDE.md and the full build

**Files:**
- Modify: `CLAUDE.md` (Website Module)

- [ ] **Step 1: Update `CLAUDE.md`**

- Shared solutions bullet: the sharer's full IP is stored with each snippet (7 days); `ClientAccess.clientIp` trusts `X-Forwarded-For` (last entry) only with `X-Nelumbo-Proxy` = `PROXY_SECRET`; drop "IPs and ids stay in memory" (browser ids still do).
- New "Admin" block: `ADMIN_IPS` (addresses/CIDR, bad, private and too-wide entries skipped) + localhost, no password (accepted: harmless functions; warn before more sensitive data comes behind it); `/admin` tabs; the endpoints; 404 for non-admins; `sharing-off` marker; `GET /solutions` `{enabled}`; globes added only while enabled; header link inserted by `theme.js`.
- Overload banner and stats: `/status.html` is gone, its content is the Stats tab of `/admin` (`admin.ts` bundles `status-chart.ts`); `status-dot.js` no longer has a shift+click shortcut (no `data-status-link`); five charts now, the second is evaluations per second (`evaluations / resolutionSeconds`).
- E2E: the third server on 8897 with `PROXY_SECRET=e2e-secret` for `admin.spec.ts`.
- Deployment: `.env` in `/data/sites/nelumbo.nl` with `ADMIN_IPS` and `PROXY_SECRET` before the deploy; the `nelumbo-proxy` middleware; after the deploy check over IPv4 and IPv6 that the real address arrives.

- [ ] **Step 2: Run the full build and e2e**

Run: `./gradlew build` then `cd website/src/main/frontend && npm run test:e2e`
Expected: BUILD SUCCESSFUL; all e2e tests pass.

- [ ] **Step 3: Commit (no push, no deploy)**

```bash
git add CLAUDE.md
git commit -m "CLAUDE.md: website admin page, client IP rule, sharing switch"
```
