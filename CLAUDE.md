# overview

This project is a programming language called Nelumbo.
It is a logical programming language in the style of Prolog.

# architecture

The main code is in src/ and written in java.
The only dependency is the java immutable collections library.

# derived ts implementation

The web-ts/ directory contains a typescript implementation of the nelumbo language.
The following points are carved in stone, never ever deviate from them:
- It was completely generated using claude code.
- It is maintained using claude code.
- The typescript code should exactly follow the java code.
- The java code is always leading.
- When doing maintenance on the ts code you should never change java code.
- the ts tests should exactly follow the java tests.
- all java tests should be translated to ts tests.
- the ts code should be a 1-to-1 translation of the java code.
- add recognizable comments (with marker @JAVA_REF) that gives an exact reference to the java construct a class/function/constant/enum is translated from
- use these comments to keep track of the relationship between the two
- concentrate on correctly translating java to ts when debugging ts, dont try to fix problems by deviating from the java code.
