#!/bin/bash
# Builds World Templates Reforged.
# Requires a Java 17 JDK (set JAVA_HOME, or have javac 17 on PATH).
set -e
cd "$(dirname "$0")"

if [ -n "$JAVA_HOME" ]; then
    export PATH="$JAVA_HOME/bin:$PATH"
fi

./gradlew build

echo
echo "Build finished. Jar(s) in build/libs/:"
ls -la build/libs/