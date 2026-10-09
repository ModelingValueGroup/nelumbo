// expect-error-per-query: Inconsistent results
// Bug: an inconsistency between two rules of the same relation is detected
// in only ONE of the two rule orders. Rule B below claims f(1)=r has NO facts
// (complete: its body is false for n=1), rule A claims f(1)=1. The docs
// (reference/logic/writing-rules.md) call that an inconsistent program that
// raises an InconsistencyException. Rule.biimply (logic/Rule.java:217-230)
// only checks the facts/falsehoods of the rules tried EARLIER against the
// completeness of the CURRENT rule, not the other way round. Rules are tried
// in Set<Rule> hash order, so A-then-B throws and B-then-A silently answers
// [(r=1)][(r=2),..]. The 32 copies are identical apart from the functor
// name; which of them hit which order depends on hashes, and those depend on
// what the JVM parsed before (at a3014b5b 22 of 32 answered silently on the
// CLI, 20 in KnownBugsTest alone, 26 in the full core suite; the
// REVERSE_NELUMBO flag flips them). With 32 copies a run in which all are
// detected by chance is very unlikely.
// Correct: every query reports "Inconsistent results". Actual: most answer.
// No .nl expectation can state an error, so both harnesses special-case this
// file: KnownBugsTest checks the diagnostics, ./run-all-tests-with-CLI the
// expect-error-per-query line at the top.
// Found 2026-10-09: examples/sudoku-4x4-smart.nl has this defect in scanB
// (its forced branch has no guard). It went red when Wim's parser-lookahead
// commits 17744db9..19aea5fb (2026-09-30..10-02) shifted the variable suffixes
// and with them the rule order - the parse trees did not change.
import nelumbo.integers

Integer ::= f1(<Integer>),
            f2(<Integer>),
            f3(<Integer>),
            f4(<Integer>),
            f5(<Integer>),
            f6(<Integer>),
            f7(<Integer>),
            f8(<Integer>),
            f9(<Integer>),
            f10(<Integer>),
            f11(<Integer>),
            f12(<Integer>),
            f13(<Integer>),
            f14(<Integer>),
            f15(<Integer>),
            f16(<Integer>),
            f17(<Integer>),
            f18(<Integer>),
            f19(<Integer>),
            f20(<Integer>),
            f21(<Integer>),
            f22(<Integer>),
            f23(<Integer>),
            f24(<Integer>),
            f25(<Integer>),
            f26(<Integer>),
            f27(<Integer>),
            f28(<Integer>),
            f29(<Integer>),
            f30(<Integer>),
            f31(<Integer>),
            f32(<Integer>)
Integer n, r

f1(n)=r  <=> r=1 if n>0   // A
f1(n)=r  <=> r=2 & n<0    // B: no guard
f2(n)=r  <=> r=1 if n>0   // A
f2(n)=r  <=> r=2 & n<0    // B: no guard
f3(n)=r  <=> r=1 if n>0   // A
f3(n)=r  <=> r=2 & n<0    // B: no guard
f4(n)=r  <=> r=1 if n>0   // A
f4(n)=r  <=> r=2 & n<0    // B: no guard
f5(n)=r  <=> r=1 if n>0   // A
f5(n)=r  <=> r=2 & n<0    // B: no guard
f6(n)=r  <=> r=1 if n>0   // A
f6(n)=r  <=> r=2 & n<0    // B: no guard
f7(n)=r  <=> r=1 if n>0   // A
f7(n)=r  <=> r=2 & n<0    // B: no guard
f8(n)=r  <=> r=1 if n>0   // A
f8(n)=r  <=> r=2 & n<0    // B: no guard
f9(n)=r  <=> r=1 if n>0   // A
f9(n)=r  <=> r=2 & n<0    // B: no guard
f10(n)=r <=> r=1 if n>0   // A
f10(n)=r <=> r=2 & n<0    // B: no guard
f11(n)=r <=> r=1 if n>0   // A
f11(n)=r <=> r=2 & n<0    // B: no guard
f12(n)=r <=> r=1 if n>0   // A
f12(n)=r <=> r=2 & n<0    // B: no guard
f13(n)=r <=> r=1 if n>0   // A
f13(n)=r <=> r=2 & n<0    // B: no guard
f14(n)=r <=> r=1 if n>0   // A
f14(n)=r <=> r=2 & n<0    // B: no guard
f15(n)=r <=> r=1 if n>0   // A
f15(n)=r <=> r=2 & n<0    // B: no guard
f16(n)=r <=> r=1 if n>0   // A
f16(n)=r <=> r=2 & n<0    // B: no guard
f17(n)=r <=> r=1 if n>0   // A
f17(n)=r <=> r=2 & n<0    // B: no guard
f18(n)=r <=> r=1 if n>0   // A
f18(n)=r <=> r=2 & n<0    // B: no guard
f19(n)=r <=> r=1 if n>0   // A
f19(n)=r <=> r=2 & n<0    // B: no guard
f20(n)=r <=> r=1 if n>0   // A
f20(n)=r <=> r=2 & n<0    // B: no guard
f21(n)=r <=> r=1 if n>0   // A
f21(n)=r <=> r=2 & n<0    // B: no guard
f22(n)=r <=> r=1 if n>0   // A
f22(n)=r <=> r=2 & n<0    // B: no guard
f23(n)=r <=> r=1 if n>0   // A
f23(n)=r <=> r=2 & n<0    // B: no guard
f24(n)=r <=> r=1 if n>0   // A
f24(n)=r <=> r=2 & n<0    // B: no guard
f25(n)=r <=> r=1 if n>0   // A
f25(n)=r <=> r=2 & n<0    // B: no guard
f26(n)=r <=> r=1 if n>0   // A
f26(n)=r <=> r=2 & n<0    // B: no guard
f27(n)=r <=> r=1 if n>0   // A
f27(n)=r <=> r=2 & n<0    // B: no guard
f28(n)=r <=> r=1 if n>0   // A
f28(n)=r <=> r=2 & n<0    // B: no guard
f29(n)=r <=> r=1 if n>0   // A
f29(n)=r <=> r=2 & n<0    // B: no guard
f30(n)=r <=> r=1 if n>0   // A
f30(n)=r <=> r=2 & n<0    // B: no guard
f31(n)=r <=> r=1 if n>0   // A
f31(n)=r <=> r=2 & n<0    // B: no guard
f32(n)=r <=> r=1 if n>0   // A
f32(n)=r <=> r=2 & n<0    // B: no guard

f1(1)=r  ?
f2(1)=r  ?
f3(1)=r  ?
f4(1)=r  ?
f5(1)=r  ?
f6(1)=r  ?
f7(1)=r  ?
f8(1)=r  ?
f9(1)=r  ?
f10(1)=r ?
f11(1)=r ?
f12(1)=r ?
f13(1)=r ?
f14(1)=r ?
f15(1)=r ?
f16(1)=r ?
f17(1)=r ?
f18(1)=r ?
f19(1)=r ?
f20(1)=r ?
f21(1)=r ?
f22(1)=r ?
f23(1)=r ?
f24(1)=r ?
f25(1)=r ?
f26(1)=r ?
f27(1)=r ?
f28(1)=r ?
f29(1)=r ?
f30(1)=r ?
f31(1)=r ?
f32(1)=r ?
