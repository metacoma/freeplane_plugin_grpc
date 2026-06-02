#!/usr/bin/env bash
# regenerate_stubs.sh — Generate Python gRPC stubs from freeplane.proto.
#
# Usage:
#   ./generate_stubs.sh          # generates in-place
#   ./generate_stubs.sh /path    # generates to /path (relative to this script)
#
# Requirements:
#   python3 -m pip install grpcio-tools protobuf

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# Resolve output directory
if [[ -n "${1:-}" ]]; then
    OUT_DIR="$(cd "$1" && pwd)"
else
    OUT_DIR="${SCRIPT_DIR}"
fi

# Resolve proto file location (go up from grpc/python to project root, then to src/main/proto)
PROTO_DIR="$(cd "${SCRIPT_DIR}/../../.." && pwd)"
PROTO_FILE="${PROTO_DIR}/src/main/proto/freeplane.proto"

if [[ ! -f "${PROTO_FILE}" ]]; then
    echo "ERROR: Proto file not found at ${PROTO_FILE}" >&2
    exit 1
fi

echo "Generating Python gRPC stubs from ${PROTO_FILE}"
echo "Output directory: ${OUT_DIR}"

python3 -m grpc_tools.protoc \
    -I"${PROTO_DIR}/src/main/proto" \
    --python_out="${OUT_DIR}" \
    --pyi_out="${OUT_DIR}" \
    --grpc_python_out="${OUT_DIR}" \
    "${PROTO_FILE}"

echo "Done. Generated files:"
ls -1 "${OUT_DIR}"/freeplane_pb2*.py 2>/dev/null || true
