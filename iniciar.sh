#!/usr/bin/env bash
set -euo pipefail
cd -- "$(dirname -- "${BASH_SOURCE[0]}")"
mkdir -p bin runtime-tmp
for fuente in "$PWD"/src/*.java; do printf '"%s"\n' "$fuente"; done > runtime-tmp/fuentes.txt
java com.sun.tools.javac.Main -encoding UTF-8 --add-modules jdk.httpserver -cp 'lib/*' -d bin @runtime-tmp/fuentes.txt
exec java -Dfile.encoding=UTF-8 "-Djava.io.tmpdir=$PWD/runtime-tmp" --add-modules jdk.httpserver -cp 'bin:lib/*' Main "$@"
