#!/usr/bin/env bash

set -euo pipefail

# Ensure we execute from the repository root regardless of caller location
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

./gradlew --stacktrace clean :idl-src-gen:image
