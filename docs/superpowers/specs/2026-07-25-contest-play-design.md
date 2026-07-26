# Contest Play — Design

Adds a playable guessing game to the existing Contest app. Users open a contest
by ID, submit a named integer guess, and after the contest's deadline the
closest guesses claim prizes in order. A contest is "won" when every prize is
assigned a winner.

## Goals

- Let anyone submit one integer guess per contest, keyed by a self-declared
  player name.
- Close a contest automatically at a creator-chosen deadline.
- Award prizes to the closest guesses in `FIRST → SECOND → THIRD` order.
- Mark a contest `isWon = true` iff every prize slot got a winner.
- Keep the secret hidden from the public API until the deadline has passed.

Non-goals: authentication, prize claim/redemption, notifications, multi-round
contests, historical guess visibility, non-integer secrets.

## Data model

### `Contest` (existing entity, modified)

| Field | Type | Notes |
|---|---|---|
| `id` | `Long` | unchanged |
| `name` | `String` | unchanged, not null |
| `secretValue` | `int` | **replaces** `String secret`; not null |
| `deadline` | `Instant` | **new**, not null |
| `isWon` | `Boolean` | existing; set true only when every prize is assigned |
| `resolvedAt` | `Instant` | **new**, nullable; set by the resolver so we run it once |
| `prizes` | `List<Prize>` | unchanged (`@ElementCollection`) |

### `Prize` (existing embeddable, modified)

| Field | Type | Notes |
|---|---|---|
| `value` | `String` | unchanged |
| `place` | `Place` enum | unchanged |
| `isWon` | `Boolean` | existing; set true when a winner is assigned |
| `winnerName` | `String` | **new**, nullable |

### `Guess` (new entity)

| Field | Type | Notes |
|---|---|---|
| `id` | `Long` | identity |
| `contest` | `Contest` (`@ManyToOne`, not null) | fk `contest_id` |
| `playerName` | `String` | not null |
| `value` | `int` | not null |
| `submittedAt` | `Instant` | not null |

Constraint: unique index on `(contest_id, player_name)`.

### Migration

Dev-only Postgres via `docker compose`. The `secret String → secretValue int`
change is a hard schema break; instructions include `docker compose down -v`
before restart. No production data exists.

## API

All endpoints under `/contests`. Request bodies are `application/x-www-form-urlencoded`
(matches the current controller style). Responses are JSON.

| Method | Path | Body | Response |
|---|---|---|---|
| `POST` | `/contests` | `contestName`, `secretValue` (int), `deadline` (ISO-8601 instant), `firstPrize`, `secondPrize?`, `thirdPrize?` | `201 Created` — public DTO |
| `GET` | `/contests` | — | `200 OK` — list of public DTOs (all contests) |
| `GET` | `/contests/{id}` | — | `200 OK` — public DTO. If deadline has passed and `resolvedAt` is null, resolve first, then respond. |
| `POST` | `/contests/{id}/guesses` | `playerName`, `value` (int) | `201 Created` — `{"accepted": true}` |

`GET /contests` (list) replaces the current `allActive()` behavior — the list
now returns every contest, open or closed, so the UI can render open ones.

### Public DTO shape

```json
{
  "id": 1,
  "name": "Summer Hackathon",
  "deadline": "2026-08-01T20:00:00Z",
  "isWon": false,
  "prizes": [
    { "place": "FIRST",  "value": "Gold medal",   "isWon": true,  "winnerName": "Alice" },
    { "place": "SECOND", "value": "Silver medal", "isWon": true,  "winnerName": "Bob"   },
    { "place": "THIRD",  "value": "Bronze medal", "isWon": false, "winnerName": null    }
  ],
  "secretValue": 5000
}
```

`secretValue` is included **only after the deadline has passed**. Individual
guesses are never exposed.

### Error responses

| Case | Status | Body |
|---|---|---|
| Guess after deadline | `409` | `{"error":"contest_closed"}` |
| Duplicate player name in same contest | `409` | `{"error":"name_already_guessed"}` |
| Missing / non-integer field | `400` | `{"error":"invalid_input"}` |
| Unknown contest id | `404` | `{"error":"not_found"}` |

Mapped via a small `@RestControllerAdvice` catching typed exceptions
(`ContestClosedException`, `DuplicateGuessException`,
`ContestNotFoundException`) plus Spring's built-in binding errors for the 400.

## Resolution algorithm

Triggered lazily on any `GET /contests/{id}` where `deadline <= now` and
`resolvedAt` is null. Wrapped in `@Transactional` with a `SELECT … FOR UPDATE`
on the `contest` row to avoid double-resolution under concurrent reads.

1. Load all guesses for the contest.
2. Sort by `abs(guess.value - contest.secretValue)` ascending; tie-break by
   `submittedAt` ascending (earlier submission wins the tie).
3. Iterate prizes in `Place` order (`FIRST`, `SECOND`, `THIRD`). For each
   prize, pop the next guess (if any); set `prize.winnerName` and
   `prize.isWon = true`.
4. If every prize was assigned a winner, set `contest.isWon = true`.
   Otherwise leave it false.
5. Set `contest.resolvedAt = now` and persist.

Concurrency note: `// ponytail: pessimistic row lock, revisit if contention matters`.

Also triggered by `GET /contests` (list): the list handler resolves any
past-deadline unresolved contests it encounters before serializing, so
listeners on the index page see fresh status without needing per-row fetches.

## Frontend

Two vanilla HTML pages under `src/main/resources/static/`, sharing a CSS file.

- **`index.html`** (existing, modified)
  - Creation form gains `<input type="number" name="secretValue">` and
    `<input type="datetime-local" name="deadline">`.
  - Contest list rows link to `contest.html?id={id}`. Each row shows:
    - name
    - status badge: `Open · closes <local time>` / `Closed · won` /
      `Closed · unclaimed`
- **`contest.html`** (new)
  - Reads `id` from the query string, fetches `GET /contests/{id}`.
  - If open: renders guess form (`playerName`, `value` number input) and
    the prize list (all "unclaimed").
  - If closed: renders the revealed `secretValue`, the prize list with
    winner names, and hides the guess form.
  - Errors from the API (409/400/404) surface in the existing `.error`
    element with the mapped message.
- **`style.css`** — extracted from the current inline `<style>` block in
  `index.html`, shared between both pages.

No frameworks, no build step. The frontend uses the same `fetch` +
`URLSearchParams` pattern already in `main.js`.

## Testing

All tests run against H2 (existing test config). No new dependencies.

- `ContestServiceTest`
  1. `create` persists secret, deadline, and prizes.
  2. `submitGuess` before deadline persists a `Guess` with correct fields.
  3. `submitGuess` after deadline throws `ContestClosedException`.
  4. Second guess from same player name in the same contest throws
     `DuplicateGuessException`; different contests are independent.
  5. `resolve` assigns winners in proximity order; tie-break by
     `submittedAt` ascending.
  6. `resolve` with fewer guesses than prizes leaves surplus prizes unwon
     and `contest.isWon` false.
  7. `resolve` with ≥N guesses sets `contest.isWon` true.
  8. `resolve` is idempotent: a second call after `resolvedAt` is set does
     not mutate winners.

- `ContestControllerTest` (`@SpringBootTest` + `MockMvc`)
  9. Public DTO omits `secretValue` while deadline is in the future.
  10. Public DTO includes `secretValue` once deadline has passed (and the
      first GET triggers resolution).
  11. POST `/contests/{id}/guesses` returns 409 with `contest_closed` after
      the deadline.
  12. POST `/contests/{id}/guesses` returns 409 with `name_already_guessed`
      on the second submission for the same name.
  13. GET `/contests/{unknown}` returns 404 with `not_found`.

## Rollout

1. Merge model + service + controller + tests (backend green).
2. `docker compose down -v && docker compose up -d && ./gradlew bootRun` to
   pick up the new schema.
3. Update `index.html`, add `contest.html`, `contest.js`, `style.css`.
4. Manually smoke: create → guess → wait past deadline → GET reveals winners.

## Deferred / explicitly out of scope

- Authentication and per-user accounts (player identity is trust-based).
- Rate limiting on guess submission.
- Editing or deleting contests after creation.
- Server-driven push updates when a contest closes (client refreshes
  manually or on next navigation).
- Historical or per-player guess visibility.
- Timezone handling beyond ISO instants; the UI uses the browser's local
  time for display and input.
