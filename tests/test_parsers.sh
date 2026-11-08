#!/bin/sh
set -eu
cd "$(dirname "$0")/.."
JSON_JAR=${JSON_JAR:?Set JSON_JAR to org.json:json:20240303 for JVM parser tests}
TEST_DIR=$(mktemp -d)
trap 'rm -rf "$TEST_DIR"' EXIT HUP INT TERM
python3 - "$TEST_DIR/project.json" <<'PY'
import json,sys
from pathlib import Path
config=json.loads(Path('project.json').read_text())
config['widgets']['f1']['constructorId']='example'
config['widgets']['football']['teamId']='17'
config['widgets']['football']['leagueId']='example.1'
Path(sys.argv[1]).write_text(json.dumps(config))
PY
python3 tools/configure.py "$TEST_DIR/project.json" --output "$TEST_DIR/gen"
javac -cp "$JSON_JAR" -d "$TEST_DIR/classes" "$TEST_DIR/gen/Config.java" src/dev/sketchtv/home/BuiltinParsers.java src/dev/sketchtv/home/WidgetProvider.java src/dev/sketchtv/home/BuiltinWidgets.java tests/WidgetParsersTest.java
java -cp "$TEST_DIR/classes:$JSON_JAR" dev.sketchtv.home.WidgetParsersTest
