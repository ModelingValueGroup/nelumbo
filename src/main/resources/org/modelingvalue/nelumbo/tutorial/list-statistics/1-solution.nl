import nelumbo.collections
Integer ::= sum <List<Integer>>                     #45,
            sum <List<Integer>> from <Integer>      #45,
            max <List<Integer>>                     #45,
            max <List<Integer>> from <Integer>      #45,
            mean <List<Integer>>                    #45,
            median <List<Integer>>                  #45,
            <List<Integer>> dot <List<Integer>>     #50,
            products of <List<Integer>> and <List<Integer>> from <Integer> #45
List<Integer> l, m
Integer       i, j
sum l from i == 0 if i >= |l|, (at i in l) + sum l from (i+1) if i < |l|
sum l        == sum l from 0
max l from i == at i in l if i = |l|-1,
                (at i in l > max l from (i+1) then at i in l else max l from (i+1)) if i < |l|-1
max l        == max l from 0
mean l       == (sum l) / (|l|)
median l     == at ((|l|-1)/2) in (l sort [i,j](i<j))
products of l and m from i == 0 if i >= |l|, (at i in l) * (at i in m) + products of l and m from (i+1) if i < |l|
l dot m      == products of l and m from 0

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
