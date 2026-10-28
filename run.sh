#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"

if ! compgen -G "lib/*.jar" > /dev/null; then
    echo "MySQL Connector/J not found in lib/."
    echo "Download mysql-connector-j from https://dev.mysql.com/downloads/connector/j/"
    echo "and place the jar in the lib folder."
    exit 1
fi

rm -rf out
sources=$(mktemp)
trap 'rm -f "$sources"' EXIT
find src -name "*.java" > "$sources"
javac -d out "@$sources"
java -cp "out:lib/*" com.friction.Main
