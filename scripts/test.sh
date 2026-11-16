#!/bin/sh
# Compile sources and tests, then run all JUnit 5 tests.
# Prints a tree of test names (Korean descriptions). Everything the run
# produces goes to test-output/ (ignored by git):
#   test-output/test-results.txt   the printed tree and summary
#   test-output/reports/           JUnit XML reports
set -e
cd "$(dirname "$0")/.."
JUNIT=lib/junit-platform-console-standalone-1.10.2.jar
if [ ! -f "$JUNIT" ]; then
  mkdir -p lib
  curl -fsSL -o "$JUNIT" \
    https://repo1.maven.org/maven2/org/junit/platform/junit-platform-console-standalone/1.10.2/junit-platform-console-standalone-1.10.2.jar
fi
compile() {
  if ! output=$("$@" 2>&1); then echo "$output"; exit 1; fi
  echo "$output" | grep -v '^Note:' || true
}
rm -rf out/main out/test && mkdir -p out/main out/test
compile javac -nowarn -d out/main $(find src -name '*.java')
compile javac -nowarn -d out/test -cp out/main:"$JUNIT" $(find test -name '*.java')
# Headless: no window is opened, which shows the model (and the view,
# drawn into an image) are tested apart from the running UI.
# Extra arguments go to JUnit, e.g. --include-tag new-feature
# (tags: existing, defect-fix, new-feature, design).
rm -rf test-output/reports && mkdir -p test-output
status=0
java -ea -Djava.awt.headless=true -Dfile.encoding=UTF-8 -Dstdout.encoding=UTF-8 -jar "$JUNIT" execute \
  --class-path out/main:out/test --scan-class-path out/test \
  --disable-banner --details=tree --details-theme=unicode \
  --reports-dir test-output/reports \
  --config='junit.jupiter.testmethod.order.default=org.junit.jupiter.api.MethodOrderer$DisplayName' \
  --config='junit.jupiter.testclass.order.default=org.junit.jupiter.api.ClassOrderer$DisplayName' \
  "$@" > test-output/test-results.ansi 2>&1 || status=$?
cat test-output/test-results.ansi
sed 's/\x1b\[[0-9;]*m//g' test-output/test-results.ansi > test-output/test-results.txt
rm test-output/test-results.ansi
exit $status
