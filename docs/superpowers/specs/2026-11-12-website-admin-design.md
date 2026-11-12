# Website admin page - design

Date: 2026-11-12

## Intent

From the office, Tom and colleagues want one place to see what happens on nelumbo.nl and to clean up:
- the server stats (today's internal `/status.html`),
- what people share from the editors, to see what is done with it and to detect misuse,
- deleting shared snippets, one by one or all at once,
- switching sharing off when needed.

## Goals

- An admin page reachable only from the office IP address (set in the `.env` of docker-compose) and from localhost (for testing).
- An `Admin` link in the site header, visible only to admins.
- Tab "Stats": what `/status.html` shows now.
- Tab "Snippets": the sharing switch and the list of shared snippets with their contents; delete one, delete all.

## Non-goals

- Login, passwords, accounts, roles.
- Editing or renaming snippets, changing their owner.
- Changing limits (sizes, rates, the 7 days) from the page.
- A log of admin actions.

## Anti-goals

- Anyone outside the office (or the local machine) reaching an admin page or action, for example by forging `X-Forwarded-For`.
- An editor tab opened earlier still sharing or loading after sharing was switched off.
- A visible trace of the admin page for non-admins.

## Decided by Tom

Access:
- Admin = a request from an address in `ADMIN_IPS`, or from localhost.
- `ADMIN_IPS` lives in `/data/sites/nelumbo.nl/.env` on server1; docker-compose passes it to the container. The deploy copies only `docker-compose.yml`, so the `.env` stays.
- No password: everyone behind the office IP is admin, including the guest wifi. Accepted because the admin functions are harmless for now. Claude warns when more sensitive information or actions come behind the admin page, so the access can be reconsidered.
- Proxy secret: Traefik adds a header `X-Nelumbo-Proxy` with a secret from `.env` (`PROXY_SECRET`) to every request it forwards. The server trusts `X-Forwarded-For` only when that header carries the secret; otherwise the client IP is the TCP peer (`ctx.ip()`). Reason: the container is on the shared Docker network `web`, so other containers on server1 can reach it directly, around Traefik, and could otherwise put the office IP in `X-Forwarded-For` and be admin.
- Localhost = the client IP (as above) is 127.0.0.1 or ::1. Behind Traefik the client IP is the visitor's real address, never loopback. A client that sends `X-Forwarded-For: 127.0.0.1` itself without the secret (a server jar on a classroom LAN, another container) stays its own TCP address.
- Without `ADMIN_IPS` only localhost is admin.

Header:
- An `Admin` link in the shared header (`.site-header nav`), after Docs, only when the server says the visitor is admin; nothing for others.
- On the tutorial, sandbox, library and docs pages. Not on the landing page (it has its own header).

Page (mockup option B, two tabs):
- Tab Stats: the four charts and the data table of today's `/status.html`.
- Plus a chart "Evaluations per second" (Tom, during the build): "a good indication for how busy our website users are". Second, after CPU; average per point; a table column too.
- `/status.html` and the shift-click on the status dot (`theme.js`) go away: the stats are only in admin. `/stats` stays public (the status dot uses it).
- Tab Snippets: the switch "Sharing from the editor: on/off", then the list.
- List columns: name, saved at, size, owner (a short code per browser), IP.
- Clicking a name folds its text open below the row: plain text, no colouring.
- Delete per snippet, and `Delete all...`; both ask for confirmation. A deleted name is free again for anyone.
- The site is English, so is the admin page.

IP address (Tom accepted after Claude's warning):
- From now on every share stores the client IP on disk with the snippet, and the list shows it.
- An IP is personal data (AVG/GDPR); it stays 7 days in `/data/sites/nelumbo.nl/solutions` and everyone on the office IP sees it.
- Snippets saved before this change have no IP: the list shows `-`.
- Sharers are told: the tooltip of `Share...` becomes "Anyone who knows the name can load it. Your IP address is kept with it for 7 days."
- This replaces the promise of the sharing design (2026-11-11) that IPs are never written to disk.

Sharing switch:
- Off: no globe button on any editor, so no share and no load.
- The server refuses share and load too while off, with the message `Sharing is turned off`, so a tab opened earlier (globe still visible) gets that message.
- Stored snippets stay and expire after 7 days as usual.
- The setting survives restarts and deploys. Default: on.

## Context

- Routes: `NelumboHttpServer.start` (Javalin 7, `config.routes.get/put/...`); `/status.html`, `/status-dot.js`, `/stats*`, `/solutions/{name}` are there.
- Client IP: `SolutionService.clientIp(X-Forwarded-For, ctx.ip())` takes the LAST entry (the one Traefik added), else `ctx.ip()`. `SolutionService.ipKey` reduces an IPv6 address to its /64 and an IPv4-mapped address to IPv4.
- Snippets: record `Solution(name, ownerHash, savedAt, text)` (Jackson), `SolutionStore` (one `<lowercase name>.json` per name in `--solutions-dir`; at start it reads every `.json` as a snippet and deletes `.tmp` files; memory only without the directory), `SolutionService.save/load/usage`.
- Status page: `public/status.html` + `src/main/frontend/src/status-chart.ts` (global `NelumboStatus.start()`), test `NelumboHttpServerTest` (the page is not linked from any page), e2e `status.spec.ts`.
- Globe: `addShareButtons` in `nelumbo-fields.ts`, skipped for fields with `data-no-share`.
- Header: copied in `tutorial.html`, `sandbox.html`, `library.html`, `docs.html`. It is tight: breakpoints 600/440/380/340 px keep it within 315 px; re-check with a width sweep after adding a link (CLAUDE.md, Website Module).
- `website/Main.java` (arguments), `website/Dockerfile` (`CMD`), `website/docker-compose.yml`.
- Earlier design of sharing: `docs/superpowers/specs/2026-11-11-shared-solutions-design.md`.

## Calls made by Claude (check these)

Configuration:
- Environment variables `ADMIN_IPS` (comma-separated) and `PROXY_SECRET`, not command-line arguments: the Dockerfile `CMD` stays as is. docker-compose: `environment: - ADMIN_IPS=${ADMIN_IPS:-}` and `- PROXY_SECRET=${PROXY_SECRET:-}`.
- Traefik label: `traefik.http.middlewares.nelumbo-proxy.headers.customrequestheaders.X-Nelumbo-Proxy=${PROXY_SECRET}`, and the `nelumbo` router's middlewares become `nelumbo-hsts,nelumbo-proxy`.
- The secret is compared in constant time and never echoed or logged.
- The same client-IP rule serves the existing share limits (per-IP new names): another container can no longer forge its IP there either. Without `PROXY_SECRET` (local runs, a classroom jar) `X-Forwarded-For` is never trusted.
- `ADMIN_IPS` set without `PROXY_SECRET`: a warning on stderr at start (behind Traefik every request then has Traefik's address, so no office IP matches and all sharers share one per-IP limit).
- An entry is an address or a CIDR range, for example `ADMIN_IPS=192.0.2.10,2001:db8:1234::/48`. Reason: nelumbo.nl has an AAAA record, so office browsers with IPv6 come in over IPv6, and an IPv6 device changes its address within its /64 from time to time (privacy extensions). An exact IPv6 address stops matching; the office prefix must be a range.
- An IPv4-mapped IPv6 address (`::ffff:192.0.2.10`) matches as its IPv4 address.
- An entry that does not parse is skipped with a message on stderr; the server still starts. Skipping only denies access, and a typo must not take the site down on deploy.
- A range wider than /16 (IPv4) or /32 (IPv6) is skipped the same way: `0.0.0.0/0` would make everyone admin.
- Below 440 px the header hides the `Admin` link (the header is tight there); `/admin` still works.
- An entry in a private or local range (10/8, 172.16/12, 192.168/16, 169.254/16, fc00::/7, fe80::/10, loopback) is skipped the same way. Behind Traefik such an address can only be a Docker gateway or a proxy, standing for many visitors at once: copying it into `ADMIN_IPS` would make all of them admin.

Endpoints (all `/admin` responses `Cache-Control: no-store`; every `/admin` path is 404 for a non-admin, so its existence is not revealed):
- `GET /admin/me`: 204 for an admin, else 404. The header script asks this once per page load.
- `GET /admin`: the page. Tab in the URL hash (`#stats`, `#snippets`), opens on Stats.
- `GET /admin/solutions`: JSON `[{name, savedAt, bytes, owner, ip}]`, newest first, without texts (the store may hold 100 MB).
- `GET /admin/solutions/{name}`: the text, `text/plain`; also while sharing is off.
- `DELETE /admin/solutions/{name}`: 204, or 404 when unknown.
- `DELETE /admin/solutions`: JSON `{deleted: <count>}`.
- `GET /admin/sharing`, `PUT /admin/sharing` with JSON `{enabled: true|false}`.
- Public `GET /solutions`: JSON `{enabled: true|false}`. The editors ask once per page load and show globes only after `enabled: true`; a failed request shows none. No flicker of a globe that disappears.
- While sharing is off, `PUT /solutions/{name}` and `GET /solutions/{name}` answer 403 `Sharing is turned off` (the editor shows the body as is).
- Admin actions only via `PUT`/`DELETE` with JSON, never `GET` or a form `POST`, and no CORS headers. A page on another site that an office browser opens then cannot trigger them: the browser needs a CORS preflight, which the server does not grant.
- `/status.html` is removed and answers 404 (a redirect to `/admin` would reveal it). `/stats/history` and `/stats/stream` stay as they are.

Data:
- `Solution` gets a field `ip`: the full address of the latest save (an update by the owner replaces it), not the `ipKey` network. Files without it still load; `ip` is then null and the list shows `-`.
- Owner code: the first 6 hex characters of `ownerHash` (already a SHA-256 of the browser id). Rows with the same code come from the same browser.
- The switch is stored as a marker file `sharing-off` in the solutions directory (present = off). Not a `.json` file: `SolutionStore` reads every `.json` as a snippet, and a name like `sharing` would collide with a snippet of that name. Without `--solutions-dir` the switch is in memory only.

Page:
- The Stats tab is the HTML of today's `status.html` moved into the admin page, with `status-chart.ts` unchanged.
- The snippet list loads when its tab opens, plus a `Refresh` button; no live updates.
- Confirmations with `confirm()`: `Delete "Frits"?`, `Delete all 14 shared snippets?`.
- Times as on the status page: local time, 24-hour clock.

## Left to the builder

- Class names and the split (for example an `AdminAccess` for the IP check).
- Which shared script inserts the header link (all four pages load `theme.js` and `status-dot.js`).
- Layout and CSS of the page, with the site's CSS variables (light and dark theme) and palette; on narrow screens the list may wrap or become cards (check from 315 px).
- Whether the stats stream keeps running while the Snippets tab shows.

## Testing

- Unit, admin check: exact IPv4 and IPv6, CIDR for both, IPv4-mapped, unset `ADMIN_IPS`, a skipped bad or private entry; `X-Forwarded-For` ignored without the secret, with a wrong secret and with no `PROXY_SECRET` set; with the secret its last entry counts and a forged earlier entry is ignored; `X-Forwarded-For: 127.0.0.1` from a non-local peer is not admin.
- Unit, store: list; delete one (total size goes down, file gone, the name is free for another browser); delete all; `ip` written and read back; an old file without `ip` loads; the switch survives a store restart on a temp dir.
- `NelumboHttpServerTest`: every `/admin` path 404 for a non-admin, working for localhost; share and load 403 while off; `/status.html` 404. Replace the test that the status page is not linked. The test client is local, so to be non-admin it starts the server with a proxy secret and sends `X-Nelumbo-Proxy` plus `X-Forwarded-For` with another address.
- E2E: Playwright connects from localhost, so it is admin: the header link, both tabs, open a text, delete, the switch hides the globe, the new `Share...` tooltip. A non-admin context the same way as above (the e2e server gets a `PROXY_SECRET`, the context `extraHTTPHeaders` with it and `X-Forwarded-For: 203.0.113.5`): no link, `/admin` 404. The switch test turns sharing on again in a `finally` and must not run in parallel with `share.spec.ts` (one shared server). `status.spec.ts` moves to the admin page.

## Deployment

- `website/docker-compose.yml`: the two `environment` entries, the `nelumbo-proxy` middleware label and the router's middleware list.
- Once, by hand on server1, BEFORE that deploy: `/data/sites/nelumbo.nl/.env` with `ADMIN_IPS=<office IPv4>,<office IPv6 prefix>` and `PROXY_SECRET=<random, e.g. openssl rand -hex 32>`. Without the secret every visitor counts as Traefik's address.
- After the first deploy, check from the office over IPv4 and over IPv6 that the server sees the real address (share a snippet, look at its IP column). Nobody has looked at the addresses arriving at the container yet. If an IPv6 visitor shows up as a `172.x` address, Traefik gets IPv6 through Docker's userland proxy: a server1 network setting, outside this repo, and until it is fixed the office IPv6 prefix never matches.
- No deploy while the workshop of 2026-11-12 runs; Tom says when it is over.
- `Main` usage text: mention `ADMIN_IPS` and `PROXY_SECRET`.
- `CLAUDE.md` Website Module: the admin page, the access rule and proxy secret, the endpoints, the IP on disk, the switch; drop the `/status.html` lines and "IPs and ids stay in memory".
