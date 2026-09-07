#!/bin/bash
# Launch script for Tomato Media Ripper
# Compiles and runs the application if no JAR exists,
# or runs the JAR directly if it's been built.

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
SRC_DIR="$SCRIPT_DIR/src/main/java"
OUT_DIR="$SCRIPT_DIR/out"
JAR="$SCRIPT_DIR/tomato-media-ripper.jar"

# Check for yt-dlp first
if ! command -v yt-dlp &> /dev/null; then
    echo "Error: yt-dlp is not installed."
    echo "Install it with: pip install yt-dlp"
    echo ""
    echo "Also recommended: sudo apt install ffmpeg"
    read -p "Press Enter to exit..."
    exit 1
fi

# If JAR exists, run it directly
if [ -f "$JAR" ]; then
    java -jar "$JAR"
    exit $?
fi

# Otherwise, compile and run from source
echo "No JAR found, compiling from source..."
mkdir -p "$OUT_DIR"

# Find all Java source files
find "$SRC_DIR" -name "*.java" > /tmp/tomato-mediaripper-sources.txt

# Compile
javac -d "$OUT_DIR" @/tmp/tomato-mediaripper-sources.txt
if [ $? -ne 0 ]; then
    echo ""
    echo "Compilation failed."
    read -p "Press Enter to exit..."
    exit 1
fi

# Run
java -cp "$OUT_DIR" dev.tomato.ripper.Main