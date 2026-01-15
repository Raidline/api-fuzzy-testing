#!/bin/bash

# API Fuzzy Testing Tool - Build and Run Script
# This script compiles the project and runs it with the provided arguments

#Example -> ./run.sh --schema api.yaml --server http://localhost:8080

set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$SCRIPT_DIR"

# Suppress sun.misc.Unsafe warnings from Maven's Guice dependency
export MAVEN_OPTS="--add-opens java.base/java.lang=ALL-UNNAMED --add-opens java.base/sun.misc=ALL-UNNAMED -Xmx512m"

echo "Building project..."
mvn clean compile -q 2>/dev/null

echo "Running API Fuzzy Testing Tool..."
java -cp "target/classes:target/dependency/*" pt.raidline.api.fuzzy.ApiFuzzyMain "$@"

