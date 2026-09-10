#!/usr/bin/env bash
##~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
## (C) Copyright 2018-2026 Modeling Value Group B.V. (http://modelingvalue.org)                                        ~
##                                                                                                                     ~
## Licensed under the GNU Lesser General Public License v3.0 (the 'License'). You may not use this file except in      ~
## compliance with the License. You may obtain a copy of the License at: https://choosealicense.com/licenses/lgpl-3.0  ~
## Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on ~
## an 'AS IS' BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the  ~
## specific language governing permissions and limitations under the License.                                          ~
##                                                                                                                     ~
## Maintainers:                                                                                                        ~
##     Wim Bast, Tom Brus                                                                                              ~
##                                                                                                                     ~
## Contributors:                                                                                                       ~
##     Victor Lap                                                                                                      ~
##~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

# Runs every bug repro against the CLI. While the bugs exist every file FAILS
# (expectation mismatch or crash); after a fix the corresponding file passes.

set -u

REPRO_DIR="$(cd "$(dirname "$0")" && pwd)"
ROOT_DIR="$(dirname "$REPRO_DIR")"

find_jar() {
    ls -t "$ROOT_DIR"/cli/build/libs/nelumbo-cli-*.jar 2>/dev/null | head -1
}

run_repro() {
    local file=$1
    local jar=$2
    local out
    # PARALLEL_COLLECTIONS=false is the documented workaround for the parallel
    # map corruption; inference itself still runs on a parallel pool (see
    # nondeterministic-inference.nl), so a lucky run can flip a result
    out=$(java -ea -DPARALLEL_COLLECTIONS=false -jar "$jar" "$file" 2>&1)
    local code=$?
    local name
    name=$(basename "$file")
    if [ $code -eq 0 ] && ! echo "$out" | grep -q "Expected result"; then
        echo "PASS  $name (bug appears fixed)"
        return 0
    else
        echo "FAIL  $name"
        echo "$out" | grep -E "Expected result|Exception" | head -2 | sed 's/^/      /'
        return 1
    fi
}

main() {
    local jar
    jar=$(find_jar)
    if [ -z "$jar" ]; then
        echo "no CLI jar found; run: ./gradlew cliJar" >&2
        exit 2
    fi
    local pass=0
    local fail=0
    local file
    for file in "$REPRO_DIR"/*.nl; do
        if run_repro "$file" "$jar"; then
            pass=$((pass + 1))
        else
            fail=$((fail + 1))
        fi
    done
    echo
    echo "$pass fixed, $fail still failing (= bugs still present)"
}

main "$@"
