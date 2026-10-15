#!/usr/bin/env sh
# Builds the project (skipping tests) and runs the alerter in one step.
# Usage: ./run.sh <location> [--mock]     e.g.  ./run.sh Bangkok   /   ./run.sh "Chiang Mai"   /   ./run.sh Phuket --mock

cd "$(dirname "$0")" || exit 1

if ! command -v mvn >/dev/null 2>&1; then
    echo "Error: Maven (mvn) was not found on PATH. Install Maven or open a new terminal." >&2
    exit 1
fi

# Build output is shown only when the build fails.
build_log=$(mktemp)
if ! mvn -q -B -DskipTests clean package >"$build_log" 2>&1; then
    cat "$build_log" >&2
    rm -f "$build_log"
    echo "Error: build failed" >&2
    exit 1
fi
rm -f "$build_log"

exec java -jar target/app.jar "$@"
