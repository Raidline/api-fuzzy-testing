#!/bin/bash

# API Fuzzy Testing Tool - Build and Run Script
# This script compiles the project and runs it with the provided arguments

#Example -> ./run.sh --schema api.yaml --server http://localhost:8080

set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$SCRIPT_DIR"

echo "Building project..."
mvn clean compile -q

echo "Running API Fuzzy Testing Tool..."
mvn exec:java -Dexec.mainClass="ApiFuzzyMain" -Dexec.args="$*" -q

