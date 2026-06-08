import    nelumbo.datetime
import    nelumbo.integers

DateTime a, b
Date     c, d
Time     e, f
Period   x, y, z
Integer  n

// =========================================================================
// DateTime literal parsing
// Note: ':00' seconds are dropped from the display, but a trailing 'Z' or
// '+HH:MM' offset is preserved (timezone info is kept), so e.g.
// 2024-01-15T10:30:00Z binds as 2024-01-15T10:30Z.
// =========================================================================
2024-01-15T10:30Z             = a    ? [(a=2024-01-15T10:30Z)][..]
2024-01-15T10:30:00+01:00     = a    ? [(a=2024-01-15T10:30+01:00)][..]
2024-01-15T10:30              = a    ? [(a=2024-01-15T10:30)][..]

// FLAG (untested / unsupported): sub-second precision is not parsed.
2024-01-15T10:30:00.00Z       = a    ?
2024-01-15T10:30:00.30+01:00  = a    ? 

// Timezone information is kept: offset literals compare by instant, so
// 10:30+01:00 is the same moment as 09:30Z, not 10:30Z.
2024-01-15T10:30:00+01:00 = 2024-01-15T10:30:00Z    ? [][()]
2024-01-15T10:30:00+01:00 = 2024-01-15T09:30:00Z    ? [()][]

// =========================================================================
// Date literal parsing
// =========================================================================
2024-01-15 = c                  ? [(c=2024-01-15)][..]
2024-01-15 = 2024-01-15         ? [()][]

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

// FLAG (untested / unsupported): sub-second time and bare T-prefixed
// time-only literals are not parsed.
// 20:04:00.00 = e               ?
// T10:30:00 = e                 ?

// =========================================================================
// DateTime equality (compared by instant)
// =========================================================================
2024-01-15T10:30:00Z = 2024-01-15T10:30:00Z       ? [()][]
2024-01-15T10:30:00Z = 2024-01-16T10:30:00Z       ? [][()]

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
// instant + duration = instant  /  instant - instant = duration
//
// Ground-check forms of the Add predicate (datetime_add / date_add / time_add):
//     instant +/- duration = <literal instant>
//     instant -  instant   = <literal duration>
2024-01-15 + P1D = 2024-01-16                            ? [()][]
2024-01-15T10:00:00Z + PT1H30M = 2024-01-15T11:30:00Z    ? [()][]
2024-01-16T00:00:00Z - 2024-01-15T00:00:00Z = PT24H      ? [()][]
20:04 + PT1H = 21:04                                     ? [()][]
//
// forward-compute: solve the resulting instant (offset is preserved)
2024-01-15T10:00:00Z + PT1H30M = a        ? [(a=2024-01-15T11:30Z)][..]
2024-01-15T10:00:00+01:00 + PT1H30M = a   ? [(a=2024-01-15T11:30+01:00)][..]
// =========================================================================

// duration +/- duration (AddPeriod predicate — deterministic)
PT1H + PT30M = PT1H30M                                 ? [()][]
PT1H30M - PT30M = PT1H                                 ? [()][]

// reversible solve-forms (Add with one unbound operand — deterministic)
// solve the duration operand of an instant sum
2024-01-15 + x = 2024-01-16                                ? [(x=P1D)][..]
// solve the duration between two datetimes (exact Duration)
2024-01-16T00:00:00Z - 2024-01-15T00:00:00Z = x            ? [(x=PT24H)][..]
// solve the left instant (instant - duration)
c + P1D = 2024-01-16                                       ? [(c=2024-01-15)][..]
2024-01-15 - P1D = c                                       ? [(c=2024-01-14)][..]
// forward: compute the resulting instant
2024-01-15 + P1D = c                                       ? [(c=2024-01-16)][..]

// scaling: Duration * Integer 
P1D * 3 = P3D                                              ? [()][]
PT1H * 2 = PT2H                                            ? [()][]

// =========================================================================
// Comparison operators >, <, <=, >= (datetime.nl lines 38-56).
// Previously untested entirely — covered here forward only.
// =========================================================================
2024-01-16 > 2024-01-15                                    ? [()][]
2024-01-15 > 2024-01-16                                    ? [][()]
2024-01-15 < 2024-01-16                                    ? [()][]
2024-01-15 <= 2024-01-15                                   ? [()][]
2024-01-16T10:30:00Z > 2024-01-15T10:30:00Z                ? [()][]
20:05 > 20:04                                              ? [()][]
20:04 > 20:05                                              ? [][()]
P2D > P1D                                                  ? [()][]
P1D > P2D                                                  ? [][()]
PT2H >= PT2H                                               ? [()][]

// FLAG (untested): comparison operators are only exercised on ground terms.
// Reverse-solving a comparison (e.g. 2024-01-16 > c) is not covered.

// =========================================================================
// Time arithmetic (time_add / date_add); previously untested.
// =========================================================================
20:04 + PT1H = e                                          ? [(e=21:04)][..]
21:04 - PT1H = e                                          ? [(e=20:04)][..]
e + PT1H = 21:04                                          ? [(e=20:04)][..]
