// Bug: a `//` comment directly after an operator character is lexed as part of the
// operator (`?//`), so the first query fails to parse; with a space it works.
import nelumbo.integers

1 + 1 = 2 ?// no space before this comment: parse error while the bug exists
1 + 1 = 2 ? // with a space it works fine
