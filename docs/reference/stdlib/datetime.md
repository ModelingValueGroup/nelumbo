# `nelumbo.datetime`

ISO 8601 dates, times, date-times, and durations, with instant-aware comparison and reversible arithmetic.

**Source:** [`src/main/resources/org/modelingvalue/nelumbo/datetime/datetime.nl`](../../../src/main/resources/org/modelingvalue/nelumbo/datetime/datetime.nl) — 97 lines.

**Import:**

```
import nelumbo.datetime
```

`nelumbo.datetime` imports `nelumbo.integers` (which transitively imports `nelumbo.logic`), so `Integer`, the Boolean type, connectives, and equality all come along automatically. `Integer` is needed for the `Period * Integer` scaling operator.

---

## Types

```
DateTime :: Object
Date     :: Object
Time     :: Object
Period   :: Object
```

Four independent value types. Each is backed by a `java.time` value:

| Type       | Backed by                                              | Example literal        |
|---|---|---|
| `Date`     | `LocalDate`                                            | `2024-01-15`           |
| `Time`     | `LocalTime`                                            | `20:04`                |
| `DateTime` | `LocalDateTime` (no offset) or `OffsetDateTime` (`Z`/`±HH:MM`) | `2024-01-15T10:30Z`    |
| `Period`   | `IsoDuration` — a calendar `Period` (Y/M/W/D) plus an exact `Duration` (H/M/S) | `P1DT1H30M` |

They are **not** in a subtype relationship with one another, and there is no implicit conversion between them: a `Date` plus a `Time` does not silently become a `DateTime`, and a `Period` never converts to an `Integer`.

---

## Literals

All four literals are written inside a **connected-token group** (`<[> … <]>`), which forbids whitespace between the inner tokens — `2024-01-15` must be written tightly, not `2024 - 01 - 15`. See [`built-in-tokens.md`](../built-in-tokens.md) for the connected-token mechanism.

```
Date     ::= <[> <NUMBER> - <NUMBER> - <NUMBER> <]>                              @nelumbo.datetime.NDate
Time     ::= <[> <NUMBER> : <NUMBER> <(> : <NUMBER> <(> . <NUMBER> <)?> <)?> <]> @nelumbo.datetime.NTime
DateTime ::= <[> <Date> T <Time#50> <(> <(> Z <|> <(> <(> + <|> - <)> <NUMBER> : <NUMBER> <)> <)> <)?> <]>
                                                                                 @nelumbo.datetime.NDateTime
Period   ::= <[> P … <]>                                                         @nelumbo.datetime.NPeriod
```

- **`Date`** — `YYYY-MM-DD`. Parsed into a `LocalDate`; out-of-range values (e.g. month 13) are rejected **at parse time** with a `file:line:col` error, not as a query falsehood.
- **`Time`** — `HH:MM`, optionally `:SS` and `.fff`. Backed by `LocalTime`. A `:00` seconds field is dropped on display (`20:04:00` prints as `20:04`).
- **`DateTime`** — a `Date`, a literal `T`, a `Time`, and an optional timezone: either `Z` or a signed `±HH:MM` offset. With an offset (or `Z`) it is an `OffsetDateTime` and **timezone information is kept**; without one it is a zone-less `LocalDateTime`.
- **`Period`** — an ISO 8601 duration: `P` followed by date units `Y`/`M`/`W`/`D` and/or a `T`-introduced time section with `H`/`M`/`S` (`M` is months before the `T`, minutes after it). Units must appear in canonical order without repeats — `P1D1Y` and `P1D1D` are parse errors. The value normalizes the time part on construction, so `P1YT90M` becomes `P1YT1H30M` (the calendar part is left as written).

```
2024-01-15T10:30Z          = a    ? [(a=2024-01-15T10:30Z)][..]
2024-01-15T10:30:00+01:00  = a    ? [(a=2024-01-15T10:30+01:00)][..]
2024-01-15                 = c    ? [(c=2024-01-15)][..]
20:04:00                   = e    ? [(e=20:04)][..]
P1YT90M                    = x    ? [(x=P1YT1H30M)][..]
```

> **Known limitation:** sub-second precision (`10:30:00.00`) and bare time-only `T`-prefixed literals are not parsed as of this writing — see the `FLAG` notes in `datetimeTest.nl`.

---

## Arithmetic

```
DateTime ::= <DateTime> + <Period>   #40,   <DateTime> - <Period>   #40
Date     ::= <Date>     + <Period>   #40,   <Date>     - <Period>   #40
Time     ::= <Time>     + <Period>   #40,   <Time>     - <Period>   #40
Period   ::= <DateTime> - <DateTime> #40,   <Date> - <Date> #40,   <Time> - <Time> #40,
             <Period>   + <Period>   #40,   <Period>   - <Period>   #40,
             <Period>   * <Integer>  #50
```

| Pattern                     | `#N` | Meaning                                  |
|---|---|---|
| `<instant> + <Period>`      | 40 | shift an instant forward by a duration   |
| `<instant> - <Period>`      | 40 | shift an instant backward by a duration  |
| `<instant> - <instant>`     | 40 | the duration between two instants        |
| `<Period> + <Period>`       | 40 | duration addition                        |
| `<Period> - <Period>`       | 40 | duration subtraction                     |
| `<Period> * <Integer>`      | 50 | scale a duration by an integer           |

(where `<instant>` is `DateTime`, `Date`, or `Time`). Underneath, five `private` natives do the work:

```
private Boolean ::= datetime_add(<DateTime>,<Period>,<DateTime>)  @nelumbo.datetime.Add,
                    date_add(<Date>,<Period>,<Date>)              @nelumbo.datetime.Add,
                    time_add(<Time>,<Period>,<Time>)              @nelumbo.datetime.Add,
                    period_add(<Period>,<Period>,<Period>)        @nelumbo.datetime.AddPeriod,
                    period_multiply(<Period>,<Integer>,<Period>)  @nelumbo.datetime.MultiplyPeriod

DateTime a, b    Date c, d    Time e, f    Period x, y, z    Integer n

a+x=b  <=>  datetime_add(a,x,b)     // instant + duration = instant
a-x=b  <=>  datetime_add(b,x,a)     // instant - duration : add viewed backward
a-b=x  <=>  datetime_add(b,x,a)     // instant - instant  = duration

x+y=z  <=>  period_add(x,y,z)
x-y=z  <=>  period_add(z,y,x)
x*n=y  <=>  period_multiply(x,n,y)
```

This is the same relational rewrite idiom as [`integers`](integers.md): subtraction is `datetime_add` read from a different angle, so all three of "instant + duration", "instant − duration", and "instant − instant" route through one native (`Add`). Because the relation has one unbound slot, **any** operand can be the unknown:

```
2024-01-15 + P1D = 2024-01-16                              ? [()][]          // verify
2024-01-15 + P1D = c                                       ? [(c=2024-01-16)][..]   // compute result
c + P1D = 2024-01-16                                       ? [(c=2024-01-15)][..]   // solve left instant
2024-01-15 + x = 2024-01-16                                ? [(x=P1D)][..]          // solve duration
2024-01-16T00:00:00Z - 2024-01-15T00:00:00Z = x            ? [(x=PT24H)][..]        // duration between
P1D * 3 = P3D                                              ? [()][]
PT1H + PT30M = PT1H30M                                     ? [()][]
```

**Type-matched durations.** `date_add` only accepts a `Period` whose *time* part is zero, and `time_add` only one whose *calendar* part is zero — adding `PT1H30M` to a bare `Date`, or `P1D` to a bare `Time`, has no result. `DateTime` (and its offset form) accepts both parts. The forward instant keeps any offset it started with:

```
2024-01-15T10:00:00+01:00 + PT1H30M = a   ? [(a=2024-01-15T11:30+01:00)][..]
20:04 + PT1H = 21:04                       ? [()][]
```

---

## Comparison

```
Boolean ::= <DateTime> ">" <DateTime> #30 @nelumbo.datetime.GreaterThan, … "<", "<=", ">="
Boolean ::= <Date>     ">" <Date>     #30 @nelumbo.datetime.GreaterThan, …
Boolean ::= <Time>     ">" <Time>     #30 @nelumbo.datetime.GreaterThan, …
Boolean ::= <Period>   ">" <Period>   #30 @nelumbo.datetime.GreaterThan, …
```

Each of the four types gets `>`, `<`, `<=`, `>=` at precedence 30. As in `integers`, only `>` is native; the other three are derived per type:

```
a<b   <=>  b>a              a<=b  <=>  a<b | a=b              a>=b  <=>  a>b | a=b
```

```
2024-01-16 > 2024-01-15                       ? [()][]
20:04 > 20:05                                 ? [][()]
P2D > P1D                                     ? [()][]
PT2H >= PT2H                                  ? [()][]
2024-01-16T10:30:00Z > 2024-01-15T10:30:00Z   ? [()][]
```

Two comparison conventions are worth knowing:

- **Instants compare by the actual moment.** `OffsetDateTime` values are compared by their instant, so `10:30+01:00` equals `09:30Z` (the same moment), not `10:30Z`. Equality (`=`) on `DateTime` is instant-based for the same reason.

  ```
  2024-01-15T10:30:00+01:00 = 2024-01-15T09:30:00Z   ? [()][]    // same instant
  2024-01-15T10:30:00+01:00 = 2024-01-15T10:30:00Z   ? [][()]    // different instant
  ```

- **Periods compare by a *nominal* magnitude** — months count as 30 days, years as 365 — because `P1M` versus `P30D` has no exact answer. This is an explicit ordering convention used only by `>`/`<`/`<=`/`>=`. Period **equality**, by contrast, is field-based, so `P1M` and `P30D` are *not* equal.

> **Known limitation:** the comparison operators are only exercised on ground terms; reverse-solving a comparison (`2024-01-16 > c`) is not covered.

---

## Native classes

| Class             | Backs                                        | Role                          |
|---|---|---|
| `NDate`           | the `Date` literal                            | constant (`LocalDate`)        |
| `NTime`           | the `Time` literal                            | constant (`LocalTime`)        |
| `NDateTime`       | the `DateTime` literal                        | constant (`LocalDateTime` / `OffsetDateTime`) |
| `NPeriod`         | the `Period` literal                          | constant (`IsoDuration`)      |
| `Add`             | `datetime_add` / `date_add` / `time_add`      | three-arg relation (instant ± duration, instant − instant) |
| `AddPeriod`       | `period_add`                                  | three-arg relation (duration ± duration) |
| `MultiplyPeriod`  | `period_multiply`                             | three-arg relation (duration × integer) |
| `GreaterThan`     | `>` on all four types                         | comparison predicate          |
| `IsoDuration`     | —                                             | the immutable value record behind `Period` (not `@`-bound) |

`IsoDuration` is the value type, not a functor: it pairs a `java.time.Period` with a `java.time.Duration` and supplies the field-based equality, nominal-magnitude comparison, and `toString` normalization described above. See [`native-classes.md`](../native-classes.md) for the full catalogue.

---

## Exports summary

Added to what `nelumbo.integers` already exports:

| Kind      | Names |
|---|---|
| Types     | `DateTime`, `Date`, `Time`, `Period`                                             |
| Literals  | ISO date `YYYY-MM-DD`, time `HH:MM[:SS]`, date-time `…T…[Z/±HH:MM]`, period `P…` |
| Operators | `+`, `-` on instants and periods, `-` between instants, `*` (period × integer), `<`, `<=`, `>`, `>=` on each type |

`datetime_add`, `date_add`, `time_add`, `period_add`, and `period_multiply` are `private` and are not visible to importers.

---

## See also

- [`integers.md`](integers.md) — the relational `add`/`>` idiom this module mirrors, lifted to dates and durations
- [`built-in-tokens.md`](../built-in-tokens.md) — the `<[> … <]>` connected-token groups the literals are built from
- [`native-classes.md`](../native-classes.md) — catalogue of the `@`-bound classes, including the datetime natives
- [`datetimeTest.nl`](../../../src/main/resources/org/modelingvalue/nelumbo/examples/datetimeTest.nl) — executable specification covering every operator and the parse-time/instant-comparison edge cases
