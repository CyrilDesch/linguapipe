#!/usr/bin/env bash
set -e

echo "Setting up LinguaPipe..."

# Check prerequisites
echo "Checking prerequisites..."

# Check if Java is installed and version is 21+
if ! command -v java &> /dev/null; then
    echo "Java is not installed. Please install JDK 21+ and try again."
    exit 1
fi

JAVA_VERSION=$(java -version 2>&1 | head -n 1 | cut -d'"' -f2 | cut -d'.' -f1)
if [ "$JAVA_VERSION" -lt 21 ]; then
    echo "Java version $JAVA_VERSION is too old. Please install JDK 21+ and try again."
    exit 1
fi
echo "Java $JAVA_VERSION found"

# Check if sbt is installed
if ! command -v sbt &> /dev/null; then
    echo "sbt is not installed. Please install sbt and try again."
    echo "   You can install it from: https://www.scala-sbt.org/download.html"
    exit 1
fi
echo "sbt found"

# Check if Docker is installed (optional)
if command -v docker &> /dev/null; then
    echo "Docker found (optional)"
else
    echo " Docker not found (optional for containerization)"
fi

echo ""
echo "Setting up the project..."

# Compile the project
echo "Compiling the project..."
sbt compile

# Run tests to ensure everything is working
echo "Running tests..."
sbt test

echo ""
echo "Setup completed successfully!"
echo ""
echo "Next steps:"
echo "   • Run the server: ./scripts/serverRun.sh"
echo "   • Or run in foreground: ./scripts/backendRun.sh"
echo "   • Or use sbt directly: sbt 'linguapipe-infrastructure/run'"
echo "" 
echo "For more information, see README.md"
