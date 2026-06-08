import    nelumbo.datetime
import    nelumbo.integers

DateTime a, b
Date     c, d
Time     e, f
Period   x, y, z
Integer  n

// =========================================================================
// DateTime literal parsing
// Note: ':00' seconds are dropped from the display, so e.g.
// 2024-01-15T10:30:00 binds as 2024-01-15T10:30.
// =========================================================================
2024-01-15T10:30:00          = a    ? [(a=2024-01-15T10:30)][..]
2024-01-15T10:30             = a    ? [(a=2024-01-15T10:30)][..]

// Sub-second precision is parsed; a zero fraction is dropped on display.
2024-01-15T10:30:00.00       = a    ? [(a=2024-01-15T10:30)][..]
2024-01-15T10:30:00.30       = a    ? [(a=2024-01-15T10:30:00.300)][..]

// =========================================================================
// Date literal parsing
// =========================================================================
2024-01-15 = c                  ? [(c=2024-01-15)][..]
2024-01-15 = 2024-01-15         ? [()][]
2024-01-15 = 2024-01-16         ? [][()]

// FLAG (untested): invalid dates reject at parse time (file:line:col), not as
// a query falsehood — so they cannot be expressed as queries here.
// 2024-13-08 = c                ?   // month 13 -> parse error
// 202020202 - 0101 - 20#4 = c   ?   // malformed -> parse error

// =========================================================================
// Time literal parsing
// =========================================================================
20:04 = e                       ? [(e=20:04)][..]
20:04:00 = e                    ? [(e=20:04)][..]
20:04:00 = 20:04:00             ? [()][]
20:04 = 20:05                   ? [][()]

// Sub-second precision is parsed; a zero fraction is dropped on display.
20:04:00.00 = e                 ? [(e=20:04)][..]
20:04:00.30 = e                 ? [(e=20:04:00.300)][..]

// FLAG (untested / unsupported): bare T-prefixed time-only literals are
// not parsed.
// T10:30:00 = e                 ?

// =========================================================================
// DateTime equality
// =========================================================================
2024-01-15T10:30:00 = 2024-01-15T10:30:00       ? [()][]
2024-01-15T10:30:00 = 2024-01-16T10:30:00       ? [][()]

// =========================================================================
// Period literal parsing / normalization
// =========================================================================
P1DT1M = x                 ? [(x=P1DT1M)][..]
// 90 minutes normalizes to 1H30M; the year component is kept as-is.
P1YT90M = x                ? [(x=P1YT1H30M)][..]

// FLAG (untested): invalid period literals reject at parse time, not as a
// query falsehood — so they cannot be expressed as queries here.
// P1D1D T1M = x            ?   // duplicate D unit -> parse error
// P1D1Y T1M = x            ?   // units out of order (D before Y) -> parse error

// =========================================================================
// Period equality is field-based, so P1M and P30D are distinct values even
// though they share the same nominal magnitude (see Comparison below).
// =========================================================================
P1M = P30D                 ? [][()]
P1M != P30D                ? [()][]

// =========================================================================
// instant + duration = instant  /  instant - instant = duration
//
// Ground-check forms of the Add predicate (datetime_add / date_add / time_add):
//     instant +/- duration = <literal instant>
//     instant -  instant   = <literal duration>
2024-01-15 + P1D = 2024-01-16                            ? [()][]
2024-01-15T10:00:00 + PT1H30M = 2024-01-15T11:30:00      ? [()][]
2024-01-16T00:00:00 - 2024-01-15T00:00:00 = PT24H        ? [()][]
20:04 + PT1H = 21:04                                     ? [()][]
// negative ground check: the sum does not match the claimed instant
2024-01-15 + P1D = 2024-01-17                            ? [][()]
// type-mismatched duration has no result: a Date rejects a time-bearing
// duration, a Time rejects a calendar one (empty true-set, no binding).
2024-01-15 + PT1H = c                                    ? [][..]
20:04 + P1D = e                                          ? [][..]
//
// forward-compute: solve the resulting instant
2024-01-15T10:00:00 + PT1H30M = a         ? [(a=2024-01-15T11:30)][..]
// =========================================================================

// duration +/- duration (AddPeriod predicate — deterministic)
PT1H + PT30M = PT1H30M                                 ? [()][]
PT1H30M - PT30M = PT1H                                 ? [()][]
// solve either addend of a duration sum (AddPeriod with one unbound)
PT1H + y = PT1H30M                                     ? [(y=PT30M)][..]
x + PT30M = PT1H30M                                    ? [(x=PT1H)][..]

// reversible solve-forms (Add with one unbound operand — deterministic)
// solve the duration operand of an instant sum
2024-01-15 + x = 2024-01-16                                ? [(x=P1D)][..]
// solve the duration between two datetimes (exact Duration)
2024-01-16T00:00:00 - 2024-01-15T00:00:00 = x              ? [(x=PT24H)][..]
// solve the duration between two dates (calendar Period) and two times
2024-01-16 - 2024-01-15 = x                                ? [(x=P1D)][..]
21:04 - 20:04 = x                                          ? [(x=PT1H)][..]
// solve the left instant (instant - duration)
c + P1D = 2024-01-16                                       ? [(c=2024-01-15)][..]
2024-01-15 - P1D = c                                       ? [(c=2024-01-14)][..]
// forward: compute the resulting instant
2024-01-15 + P1D = c                                       ? [(c=2024-01-16)][..]

// scaling: Duration * Integer
P1D * 3 = P3D                                              ? [()][]
PT1H * 2 = PT2H                                            ? [()][]
// forward-compute the scaled duration (MultiplyPeriod with result unbound)
P1D * 3 = y                                               ? [(y=P3D)][..]

// =========================================================================
// Comparison operators >, <, <=, >= (datetime.nl lines 38-56).
// Previously untested entirely — covered here forward only.
// =========================================================================
2024-01-16 > 2024-01-15                                    ? [()][]
2024-01-15 > 2024-01-16                                    ? [][()]
2024-01-15 < 2024-01-16                                    ? [()][]
2024-01-15 <= 2024-01-15                                   ? [()][]
2024-01-16T10:30:00 > 2024-01-15T10:30:00                  ? [()][]
20:05 > 20:04                                              ? [()][]
20:04 > 20:05                                              ? [][()]
P2D > P1D                                                  ? [()][]
P1D > P2D                                                  ? [][()]
PT2H >= PT2H                                               ? [()][]
// Period ordering is by *nominal* magnitude (month = 30 days, year = 365):
P1M > P29D                                                 ? [()][]
P31D > P1M                                                 ? [()][]
// P1M and P30D are nominally equal, but '>=' derives from '>' | '=' and
// Period equality is field-based (P1M != P30D), so this is *false*:
P1M >= P30D                                                ? [][()]

// FLAG (untested): comparison operators are only exercised on ground terms.
// Reverse-solving a comparison (e.g. 2024-01-16 > c) is not covered.

// =========================================================================
// Time arithmetic (time_add / date_add); previously untested.
// =========================================================================
20:04 + PT1H = e                                          ? [(e=21:04)][..]
21:04 - PT1H = e                                          ? [(e=20:04)][..]
