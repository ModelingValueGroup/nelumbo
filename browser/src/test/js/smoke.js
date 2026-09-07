//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
// (C) Copyright 2018-2026 Modeling Value Group B.V. (http://modelingvalue.org)                                        ~
//                                                                                                                     ~
// Licensed under the GNU Lesser General Public License v3.0 (the 'License'). You may not use this file except in      ~
// compliance with the License. You may obtain a copy of the License at: https://choosealicense.com/licenses/lgpl-3.0  ~
// Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on ~
// an 'AS IS' BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the  ~
// specific language governing permissions and limitations under the License.                                          ~
//                                                                                                                     ~
// Maintainers:                                                                                                        ~
//     Wim Bast, Tom Brus                                                                                              ~
//                                                                                                                     ~
// Contributors:                                                                                                       ~
//     Victor Lap                                                                                                      ~
//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

// Smoke test for the TeaVM browser bundle: runs the two spike programs and pins the
// query results to the JVM NelumboEvaluator output (byte-identical check, and the
// functional drift guard for the source overlays).
//
// Usage: node smoke.js <path-to-nelumbo.js> <path-to-family.nl>

'use strict';

const fs = require('fs');

const [bundlePath, familyPath] = process.argv.slice(2);
if (!bundlePath || !familyPath) {
    console.error('usage: node smoke.js <nelumbo.js> <family.nl>');
    process.exit(2);
}
const {evaluateNl} = require(require('path').resolve(bundlePath));

let failures = 0;
function check(what, actual, expected) {
    if (actual !== expected) {
        failures++;
        console.error(`FAIL ${what}\n  expected: ${JSON.stringify(expected)}\n  actual:   ${JSON.stringify(actual)}`);
    } else {
        console.log(`ok ${what}`);
    }
}

// 1. fib(100): bignum arithmetic (pins the NInteger/BigInteger canonicalization overlay)
const fib = 'import nelumbo.integers\n\n' +
    'Integer ::= fib(<Integer>)\n' +
    'Integer n, f\n' +
    'fib(n)=f <=> f=n if n<=1, f=fib(n-1)+fib(n-2) if n>1\n\n' +
    'fib(100)=f ?\n';
const fibOut = JSON.parse(evaluateNl(fib));
check('fib ok', fibOut.ok, true);
check('fib query count', fibOut.queries.length, 1);
check('fib(100) result', fibOut.queries[0].result, '[(f=36#22r8fozas3n8w3)][..]');

// 2. family.nl: logic + expectations (each query carries an expected result the engine verifies)
const famOut = JSON.parse(evaluateNl(fs.readFileSync(familyPath, 'utf8')));
check('family ok', famOut.ok, true);
check('family query count', famOut.queries.length, 6);
const expected = {
    'a(Amalia)=a': '[(a=Beatrix),(a=Bernhard),(a=Claus),(a=Hendrik),(a=Juliana),(a=Maxima),(a=Wilhelmina),(a=Willem)][..]',
    'm(Amalia)=Maxima': '[()][]',
    'm(Amalia)=Willem': '[][()]',
    'm(Amalia)=a': '[(a=Maxima)][..]',
    'f(Amalia)=a': '[(a=Willem)][..]',
    'f(m(f(Amalia)))=a': '[(a=Bernhard)][..]',
};
for (const q of famOut.queries) {
    check(`family result ${q.query}`, q.result, expected[q.query]);
    check(`family expectation ${q.query}`, q.expectationMatched, true);
}

if (failures > 0) {
    console.error(`${failures} smoke check(s) FAILED`);
    process.exit(1);
}
console.log('browser smoke test passed');
