import nelumbo.collections

// build your language here: functions, variables and rules



// ---- given: the questions and the answers they should get ----
sum [3,4,5]                  ? [(12)][..]
sum []                       ? [(0)][..]
max [3,9,4]                  ? [(9)][..]
mean [2,4,9]                 ? [(5)][..]
mean [1,2]                   ? [][..]
median [7,1,5]               ? [(5)][..]
[1,2,3] dot [4,5,6]          ? [(32)][..]
sum ([1,2,3] map [i](i*i))   ? [(14)][..]
max ([3,9,4] where [i](i<5)) ? [(4)][..]
