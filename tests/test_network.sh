#!/bin/sh
set -eu
cd "$(dirname "$0")/.."
TEMP_TEST=$(mktemp -d)
trap 'rm -rf "$TEMP_TEST"' EXIT HUP INT TERM
javac -d "$TEMP_TEST" src/dev/sketchtv/home/NetworkSignal.java tests/NetworkSignalTest.java
java -cp "$TEMP_TEST" dev.sketchtv.home.NetworkSignalTest
