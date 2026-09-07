#!/usr/bin/env bash
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
    out=$(java -ea -jar "$jar" "$file" 2>&1)
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
