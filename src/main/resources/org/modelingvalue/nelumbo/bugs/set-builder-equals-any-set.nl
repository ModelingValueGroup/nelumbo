// Bug: comparing a set-builder with a closed set literal holds for any set: the builder
// of {1,2,3} equals {1,2} and {4}, and {[i](|i|=10)}={10} is both true and false.
import nelumbo.collections

Integer i

{[i](i in {1,2,3})}={1,2,3} ? [()][]
{[i](i in {1,2,3})}={1,2}   ? [][()]
{[i](i in {1,2,3})}={4}     ? [][()]
{[i](|i|=10)}={10}          ? [][()]
