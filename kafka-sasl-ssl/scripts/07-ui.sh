#!/bin/bash

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"

echo "========================================"
echo "Kafka SSL UI"
echo "========================================"
echo ""
echo "Opening Kafka UI in browser..."
echo ""
echo "URL: http://localhost:8080"
echo ""
echo "Features:"
echo "  - Browse topics and messages"
echo "  - View consumer groups"
echo "  - Monitor broker health"
echo "  - Send test messages"
echo ""

if command -v open &> /dev/null; then
    open http://localhost:8080
elif command -v xdg-open &> /dev/null; then
    xdg-open http://localhost:8080
else
    echo "Please open manually: http://localhost:8080"
fi

echo "Press Ctrl+C to exit"
echo ""