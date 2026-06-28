#!/usr/bin/env bash
set -euo pipefail

PASS=0
FAIL=0
RESULTS=""

run_suite() {
    local name="$1"
    shift
    echo "========================================"
    echo "=== $name ==="
    echo "========================================"
    set +e
    "$@"
    local exit_code=$?
    set -e
    if [ $exit_code -eq 0 ]; then
        echo "$name: PASS"
        RESULTS="$RESULTS\n$name: PASS"
        PASS=$((PASS + 1))
    else
        echo "$name: FAIL (exit code $exit_code)"
        RESULTS="$RESULTS\n$name: FAIL"
        FAIL=$((FAIL + 1))
    fi
    echo ""
}

# Python unit tests (all mocked — no server required)
run_suite "Python Tests" bash -c 'cd grpc/python && python3 -m unittest discover -s tests -v'

# Ruby unit tests (integration tests auto-skipped without FREEPLANE_HOST)
run_suite "Ruby Tests" bash -c 'cd grpc/ruby && bundle exec rake spec'

# Node.js unit tests (all mocked — no server required)
run_suite "Node.js Tests" bash -c 'cd grpc/nodejs && npm test'

# Rust unit tests (all mocked — no server required)
run_suite "Rust Tests" bash -c 'cd grpc/rust && cargo test'

echo "========================================"
echo "=== Summary ==="
echo "========================================"
echo -e "$RESULTS"
echo ""
echo "Passed: $PASS, Failed: $FAIL"

if [ $FAIL -gt 0 ]; then
    echo "OVERALL: FAIL"
    exit 1
fi

echo "OVERALL: PASS"
exit 0
