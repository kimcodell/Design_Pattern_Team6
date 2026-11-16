#!/bin/sh
# Measure step and repaint times for several board sizes.
# Usage: scripts/bench.sh [density]   (default 0.3)
set -e
cd "$(dirname "$0")/.."
rm -rf out/bench && mkdir -p out/bench
javac -nowarn -d out/bench $(find src -name '*.java') test/com/holub/life/bench/Benchmark.java 2>&1 | grep -v '^Note:' || true
java -Djava.awt.headless=true -cp out/bench com.holub.life.bench.Benchmark "$@"
