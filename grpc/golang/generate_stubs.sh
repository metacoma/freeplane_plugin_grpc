#!/usr/bin/env bash
set -euo pipefail

# Generate Go protobuf and gRPC stubs from freeplane.proto
# Usage: bash generate_stubs.sh

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROTO_DIR="$(cd "$SCRIPT_DIR/../../src/main/proto" && pwd)"
OUT_DIR="$SCRIPT_DIR/freeplane"

echo "Generating Go stubs from $PROTO_DIR/freeplane.proto"

# Ensure output directory exists
mkdir -p "$OUT_DIR"

# Run protoc with Go plugins, outputting to freeplane/ subdirectory
protoc \
  -I"$PROTO_DIR" \
  --go_out="$OUT_DIR" \
  --go_opt=paths=source_relative \
  --go-grpc_out="$OUT_DIR" \
  --go-grpc_opt=paths=source_relative \
  "$PROTO_DIR/freeplane.proto"

echo "Generated stubs in $OUT_DIR/"
