#!/usr/bin/env bash
# generate_stubs.sh — Regenerate Ruby protobuf stubs from freeplane.proto.
#
# Usage:
#   ./generate_stubs.sh          # generates in-place under lib/
#   ./generate_stubs.sh /path    # generates to /path
#
# Requirements:
#   protoc installed and on PATH
#   Ruby grpc and protobuf gems

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
LIB_DIR="${SCRIPT_DIR}/lib"

# Resolve output directory
if [[ -n "${1:-}" ]]; then
    OUT_DIR="$(cd "$1" && pwd)"
else
    OUT_DIR="${LIB_DIR}"
fi

# Resolve proto file location (go up from grpc/ruby to project root, then to src/main/proto)
PROTO_DIR="$(cd "${SCRIPT_DIR}/../../.." && pwd)"
PROTO_FILE="${PROTO_DIR}/src/main/proto/freeplane.proto"

if [[ ! -f "${PROTO_FILE}" ]]; then
    echo "ERROR: Proto file not found at ${PROTO_FILE}" >&2
    exit 1
fi

echo "Generating Ruby gRPC stubs from ${PROTO_FILE}"
echo "Output directory: ${OUT_DIR}"

# Generate Ruby protobuf messages and gRPC service stubs
protoc \
    -I"${PROTO_DIR}/src/main/proto" \
    --ruby_out="${OUT_DIR}" \
    --grpc_ruby_out="${OUT_DIR}" \
    "${PROTO_FILE}"

echo "Done. Generated files:"
ls -1 "${OUT_DIR}"/freeplane*pb.rb 2>/dev/null || true
