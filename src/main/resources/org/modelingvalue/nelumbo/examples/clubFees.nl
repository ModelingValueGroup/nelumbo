// Club Membership Fees
//
// Decision model: the base membership fee is 100 euro; members under 18 or
// over 65 pay half (50 euro). Authored and verified with the Nelumbo MCP
// server: eval_nl returned ok=true with all expected query results matched.
//
// Install the MCP server (stdio) in Claude Code:
//   ./gradlew :mcp:mcpJar
//   claude mcp add nelumbo -- java -jar $(pwd)/mcp/build/libs/nelumbo-mcp-server-*.jar
//
// The question that produced this model:
//   "Using the nelumbo MCP tools, author a Nelumbo decision model for club
//    membership fees: base fee 100 euro, members under 18 or over 65 pay
//    half. Verify it with eval_nl until ok=true, including expected-result
//    queries for at least three cases."

import  nelumbo.integers

Person   :: Object

FactType ::= age(<Person>,<Integer>)

Integer  ::= fee(<Person>)

Person  p
Integer n, f

fee(p)=f <=>  f=50  if E[n](age(p,n) & (n<18 | n>65)),
              f=100 if E[n](age(p,n) & n>=18 & n<=65)

Person ::= Alice, Bob, Carol, Dave, Eve

fact age(Alice, 34),
     age(Bob, 15),
     age(Carol, 70),
     age(Dave, 18),
     age(Eve, 65)

fee(Alice)=f ? [(f=100)][..]
fee(Bob)=f   ? [(f=50)][..]
fee(Carol)=f ? [(f=50)][..]
fee(Dave)=f  ? [(f=100)][..]
fee(Eve)=f   ? [(f=100)][..]
