# Shared solutions - design

Date: 2026-11-11

## Intent

During a course a student saves their solution under a name, for example "Frits".
The teacher loads "Frits" in their own editor on the big screen and discusses it with the group.
Other students can load "Frits" too, to study it further.

## Goals

- Every editor in the tutorial and the sandbox can save its text under a name and load a text by name.
- Names are global: one name is one text, whatever editor or page it came from.
- The server cannot be abused to fill the disk, show vulgar language, or overwrite someone else's text.

## Non-goals (now)

- A list of saved names, a teacher screen, deleting texts. Maybe later: keep the stored data such that a list (name, time) is easy to add.
- Links/URLs that open a saved text. Only the name.
- Login, accounts, course codes. Anyone can save.
- Privacy: whoever knows (or guesses) a name can load its text.

## Anti-goals

- A classroom behind one NAT address being blocked by the limits.
- Saved texts lost on a container restart or deploy.
- Buttons on editors that are not for practising (landing page, library).

## Decided by Tom

UI (mockup option A):
- The toolbar below the editor (`.nelumbo-field-toolbar`, next to "Show solution" and "Reset") gets two buttons: `Save as...` and `Load...`.
- Clicking one replaces the two buttons with a name field plus `Save` (or `Load`) and `Cancel`.
- Afterwards a short message shows in the toolbar.
- Loading replaces the editor text as one undoable step (like Reset: `replaceText`); Cmd+Z brings back what was there.
- When the editor holds own work (its text differs from the original), Load first asks "Replace your own text?". Reason: the loaded text overwrites the field's saved edit in localStorage (`saveText`), so after a reload Cmd+Z is gone and Reset only brings back the original exercise, not the own attempt.
- Tutorial and sandbox editors get the buttons. The library page does not; the landing page does not.
- The site is English, so are buttons and messages.

Rules:
- A name belongs to the browser that saved it first. Only that browser may update it; any other gets "taken".
- A text is kept one week.
- The server refuses a save when the name or the text contains a word from a list of swear words (Dutch and English).
- Texts are stored on a mounted volume of the Docker container, so they survive restarts.

Limits:
- Text at most 32 KB (Tom agreed to 64 KB; lowered, see "Calls made by Claude").
- Name: letters, digits and hyphens, at most 40 characters, case-insensitive ("frits" = "Frits").
- Per browser id: at most 30 saves per hour.
- Per IP address: at most 200 NEW names per hour. Updates do not count: they replace the old text and do not grow the store. High enough for a class behind NAT.
- Total store at most 100 MB. When full, new names are refused until old texts expire. An update that makes a text larger counts too (Claude's call): else an abuser creates many tiny names and then grows them past the cap. An update to the same or a smaller size always passes.
- The browser id alone does not stop abuse (an abuser leaves it out or sends a new one each time); the total cap is the real guard against filling the disk.

## Context

- Website server: `NelumboHttpServer` (Javalin 7), routes in `Javalin.create(config -> config.routes...)`. Arguments in `website/.../Main.java` (`--stats-dir` is the pattern to follow).
- Existing volume: `./stats:/data/stats` in `website/docker-compose.yml`, `--stats-dir /data/stats` in the Dockerfile `CMD`. Host dir `/data/sites/nelumbo.nl` on server1.
- The server sits behind Traefik: `ctx.ip()` is Traefik's address. Traefik appends the client address to `X-Forwarded-For`.
- Frontend: `website/src/main/frontend/src/nelumbo-fields.ts` (`buildField` creates the toolbar, `replaceText`, `saveText` keeps edits in localStorage per field), CSS in `fields.css`. Pages mount fields with `mountFields`/`initNelumboFields`; the sandbox switches examples with `setFieldText`.
- The toolbar hides itself while it has no visible button (`:not(:has(button:not([hidden])))`); with the new buttons it is always visible on tutorial and sandbox editors.

## Calls made by Claude (check these)

- Storage: a second volume `./solutions:/data/solutions`, argument `--solutions-dir /data/solutions`. Not inside `/data/stats`: the stats cleanup deletes old files there. Without the argument (local runs, tests) the store is in memory only, like the stats history.
- The week counts from the last save, not the first.
- The name field is prefilled with the last name used in this browser (localStorage), on both Save and Load.
- Opt-out per field: `data-no-share` on the landing and library fields; every other `.nelumbo-field` gets the buttons.
- Browser id: 128 random bits (`crypto.randomUUID()`), in localStorage `nelumbo-browser-id`, sent as header `X-Nelumbo-Browser`. The server stores only its SHA-256 hash per name. Without localStorage (private mode, blocked) the id lives for the page only: saving works, updating after a reload does not.
- IP address: the LAST entry of `X-Forwarded-For` (the one Traefik added; earlier entries can be forged by the client), else `ctx.ip()`. IPs and browser ids are only counted in memory, never written to disk. Counters reset on restart; that is fine.
- API:
  - `PUT /solutions/{name}`, body `text/plain` UTF-8, header `X-Nelumbo-Browser` required.
  - `GET /solutions/{name}` -> `text/plain` or 404.
  - Refusals with a status and a short plain-text reason the frontend shows as is: 400 bad name or missing browser id, 409 taken, 413 too large, 422 blocked word, 429 rate limit, 503 store full.
- Messages (toolbar, after the action):
  - `Saved as "Frits" (kept 7 days)`
  - `"Frits" is taken`
  - `Not saved: contains a blocked word`
  - `Too many saves, try again later`
  - `Not saved: the server is full, try again later`
  - `Not saved: too large (max 32 KB)`
  - `Use only letters a-z, digits and hyphens`
  - `Loaded "Frits"`
  - `No solution named "Frits"`
  - The `Save as...` button has the tooltip "Anyone who knows the name can load it".
- Store format: one file per name, `<lowercase name>.json`, holding display name, owner hash, saved-at time and text. The name rule (letters, digits, hyphens) makes it a safe file name. Write to a temp file, then atomic move.
- Expiry: at start-up and every hour, delete files older than 7 days; a GET treats an expired text as missing. The store keeps the total size in memory (read at start-up).
- Text size: at most 32 KB, measured as the length of the text JSON-escaped (as in an LSP message). The `/lsp` WebSocket refuses frames over 64 K chars (`LspWebSocket.MAX_MESSAGE_CHARS`) and closes the session (1009); a loaded text near 64 KB would break the editor session of everyone who loads it. The frame limit stays as it is.
- Names are ASCII only: `[A-Za-z0-9-]`. "Zoë" is refused with `Use only letters a-z, digits and hyphens`; the student types "Zoe". Keeps names safe file names without Unicode normalisation.
- Blocked words: a resource file in the website module, one word per line, Dutch and English, all lowercase.
  - Name: split into runs of letters. A list word of 4+ letters is blocked anywhere inside a run ("fuckyou123", "Hancock"); a shorter list word only as a whole run. So "Hassan" ("ass"), "Titus" ("tit") and "Kutlu" ("kut") pass, "Kut-123" does not. Rare false refusals ("Dick") are accepted: the student picks another name.
  - Text: whole-word match, case-insensitive, words split on everything that is not a letter (avoids false hits like "Scunthorpe" in code and comments).

## Left to the builder

- Where the word list comes from. If taken from a public list, check its license is compatible with LGPL 3.0 distribution and credit it.
- Class names and the split between store, limits and route handling.
- Layout of the name row on narrow screens (the toolbar may wrap; check from 315 px width up).
- How long the toolbar message stays (until the next action, or a few seconds).

## Testing

- Unit tests for the store and limits: ownership (same id updates, other id 409), case-insensitive names, expiry by time (injectable clock, like `StatsHistory`), size cap, per-browser and per-IP-new-name limits, blocked words in name and text, persistence across a store restart on a temp dir.
- `NelumboHttpServerTest`: the routes and status codes, `X-Forwarded-For` handling.
- E2E (`share.spec.ts`): save in one browser context, load in a second; a second context gets "taken" on the same name; Reset after Load restores the original; Load into an edited field asks first, into an unedited one does not; no buttons on the landing page and the library.

## Deployment

- `website/docker-compose.yml`: add `- ./solutions:/data/solutions`.
- `website/Dockerfile` `CMD`: add `--solutions-dir /data/solutions`.
- `Main` usage text: the new argument.
- `CLAUDE.md` Website Module section: the feature, the API, the limits and the volume.
