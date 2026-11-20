// An open collection ({1,..}, [1,..]) is only partly known, so whether it is the same as
// another value is unknown, unless the elements it lists already tell them apart. That
// holds for = and != between literals, for a result of an operation that is compared with
// a given value, and for a function value in a fact or from a rule: the unique hole of =
// closes it only against a value that cannot be the same.
import nelumbo.collections

Thing        :: Object
Thing        ::= X, Y

Set<Integer> ::= m(<Thing>), r(<Thing>), c(<Thing>)

Set<Integer> s
Integer      n

fact m(X)={1,..}, c(X)={1}

r(X)  ==  {1,..}
c(Y)  ==  {1}

// literals
{1,..} = {1,..}      ? [..][..]
{1} = {1,..}         ? [..][..]
{1,2} = {1,..}       ? [..][..]
{2} = {1,..}         ? [][()]
{1,..} != {1}        ? [..][..]
{1,..} != {2}        ? [()][]
[1,..] = [1,2]       ? [..][..]
[2,1,..] = [1,2]     ? [][()]
{{1,..}} = {{1}}     ? [..][..]

// a result against a given value
{1,..} || {2} = {1,2,..} ? [..][..]
{1,..} || {2} = {1}      ? [][()]
{1,..} && {2} = {}       ? [..][..]
{1,..} && {2} = {2}      ? [..][..]
{1,..} && {2} = {2,..}   ? [..][..]

// false only when it is proven: an intersection or a difference has no element outside a
// closed operand, so a given set that lists one is not the result
{1,..} && {2} = {3}      ? [][()]
{1,..} && {2} = {3,..}   ? [][()]
{2} && {1,..} = {3}      ? [][()]
{1,..} && {2,..} = {3}   ? [..][..]
{1,2} - {1,..} = {2}     ? [..][..]
{1,2} - {1,..} = {1}     ? [][()]
{1,2} - {1,..} = {3}     ? [][()]
{1,..} - {1} = {2}       ? [..][..]

// the value itself is still the answer
m(X) = s             ? [(s={1,..})][..]
r(X) = s             ? [(s={1,..})][..]
m(X)                 ? [({1,..})][..]
2 in m(X)            ? [..][..]
1 in m(X)            ? [()][]
|m(X)| = n           ? [..][..]

// a function value in a fact
m(X) = {1,..}        ? [..][..]
m(X) = {1,2}         ? [..][..]
m(X) = {2}           ? [][()]

// a function value from a rule
r(X) = {1,..}        ? [..][..]
r(X) = {1,2}         ? [..][..]
r(X) = {2}           ? [][()]

// two functions, in both orders, also after their values were asked above
r(X) = m(X)          ? [..][..]
m(X) = r(X)          ? [..][..]

// closed values are as before
c(X) = {1}           ? [()][]
c(X) = {2}           ? [][()]
c(X) = c(Y)          ? [()][]
c(Y) = c(X)          ? [()][]
