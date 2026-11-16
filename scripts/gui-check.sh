#!/bin/sh
# Start the real Life window and drive it automatically (needs a display).
# Screenshots, files it saves and loads, and the printed results go to
# test-output/gui-check/ (ignored by git).
set -e
cd "$(dirname "$0")/.."
rm -rf out/gui test-output/gui-check && mkdir -p out/gui test-output/gui-check
javac -nowarn -d out/gui $(find src -name '*.java') test/com/holub/life/GuiSmokeCheck.java 2>&1 | grep -v '^Note:' || true
java -ea -Dfile.encoding=UTF-8 -Dstdout.encoding=UTF-8 -cp out/gui -Dcheck.dir=test-output/gui-check com.holub.life.GuiSmokeCheck \
  | tee test-output/gui-check/results.txt
