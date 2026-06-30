#!/usr/bin/env bash
set -euo pipefail

# Generate C++ protobuf and gRPC stubs from freeplane.proto
# Usage: bash generate_stubs.sh
#
# Requirements:
#   protobuf-compiler, protobuf-compiler-grpc (Debian/Ubuntu)
#   or grpc_cpp_plugin available in PATH

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROTO_DIR="$(cd "$SCRIPT_DIR/../../src/main/proto" && pwd)"
OUT_DIR="$SCRIPT_DIR/freeplane"

echo "Generating C++ stubs from $PROTO_DIR/freeplane.proto"

# Ensure output directory exists
mkdir -p "$OUT_DIR"

# Run protoc with C++ plugins
protoc \
  -I"$PROTO_DIR" \
  --cpp_out "$OUT_DIR" \
  --grpc_out "$OUT_DIR" \
  "$PROTO_DIR/freeplane.proto"

echo "Generated stubs in $OUT_DIR/"
ls -1 "$OUT_DIR"/*.h "$OUT_DIR"/*.cc 2>/dev/null || true
