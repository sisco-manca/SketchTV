#!/bin/sh
set -eu
cd "$(dirname "$0")/.."
TEST_DIR=$(mktemp -d)
trap 'rm -rf "$TEST_DIR"' EXIT HUP INT TERM
javac -d "$TEST_DIR" src/dev/sketchtv/home/CatalogRefresh.java tests/CatalogRefreshTest.java
java -cp "$TEST_DIR" dev.sketchtv.home.CatalogRefreshTest
