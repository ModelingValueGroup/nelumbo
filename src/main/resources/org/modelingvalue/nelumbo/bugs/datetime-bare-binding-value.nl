// Bug: a date-time value with a time cannot be written bare in an expected result:
// `[(2024-01-15T11:30)]` does not parse ("Unexpected token 'T11'"), although the
// result prints that way. Named (`= a ? [(a=2024-01-15T11:30)]`) it works.
import nelumbo.datetime

2024-01-15T10:00 + PT1H30M ? [(2024-01-15T11:30)][..]
