# overview

This project is a programming language called Nelumbo.
It is a logical programming language in the style of Prolog.

# architecture

The main code is in src/ and written in java.
The only dependency is the java immutable collections library.

# derived ts implementation

The web-ts/ directory contains a typescript implementation of the language. Be aware that:
- It was completely generated using claude code.
- It is maintained using claude code.
- The typescript code should exactly follow the java code.
- The java code is always leading.
- When doing maintenance on the ts code you should never change java code.
- the ts tests should exactly follow the java tests.
- all java tests should be translated to ts tests.
- the ts code should be a 1-to-1 translation of the java code.

