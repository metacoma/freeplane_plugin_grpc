#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROTO_DIR="$(cd "$SCRIPT_DIR/../../src/main/proto" && pwd)"

# Ensure protoc plugins are in PATH
go install google.golang.org/protobuf/cmd/protoc-gen-go@v1.36.11
go install google.golang.org/grpc/cmd/protoc-gen-go-grpc@v1.6.2

mkdir -p "$SCRIPT_DIR/pb"

protoc \
    --go_out="$SCRIPT_DIR/pb" \
    --go_opt=paths=source_relative \
    --go-grpc_out="$SCRIPT_DIR/pb" \
    --go-grpc_opt=paths=source_relative \
    -I"$PROTO_DIR" \
    "$PROTO_DIR/freeplane.proto"

echo "Proto files generated in $SCRIPT_DIR/pb/"
