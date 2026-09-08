// Bug: GreaterThan.nominalSeconds (GreaterThan.java:72) counts a year as 12*30=360 days
// although the comment right above promises 365 (it also truncates sub-second precision,
// but fractional-second literals do not parse, so that is not demonstrable here).
// Correct (per the documented convention): P1Y (365d) > P364D is true.
// Actual: P1Y counts as 360 days, so the query yields falsehood.
import nelumbo.datetime

P1Y > P364D ? [()][]
