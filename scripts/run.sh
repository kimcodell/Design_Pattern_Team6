#!/bin/sh
# Compile and run the Life game
cd "$(dirname "$0")/.."
rm -rf out && mkdir out
javac -nowarn -d out $(find src -name '*.java') 2>&1 | grep -v '^Note:'
java -ea -cp out com.holub.life.Life
