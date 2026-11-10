import nelumbo.collections

// build your language here: operators, variables and rules



// ---- given: the questions and the answers they should get ----
{1,2,3} xor {2,3,4}   ? [({1,4})][..]
{1,2} xor {2,1}       ? [({})][..]
{1,2} disjoint {3,4}  ? [()][]
{1,2} disjoint {2,3}  ? [][()]
{1,2,3} * 10          ? [({10,20,30})][..]
{1,2} + {10,20}       ? [({11,12,21,22})][..]
|{1,2,3} + {1,2,3}|   ? [(5)][..]
{1,2} + {10} * 2      ? [({21,22})][..]
{1,2} + {0} = {1,2}   ? [()][]
{1,2,..} xor {2}      ? [({1,..})][..]
