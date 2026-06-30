#!/usr/bin/env bash
# generate_stubs.sh — Regenerate gRPC stubs from freeplane.proto for the Kotlin client.
#
# The Kotlin client uses Java gRPC generated stubs (via the Gradle protobuf plugin).
# This script synchronizes the proto file from the project root and triggers
# Gradle to regenerate the Java gRPC stubs.
#
# Usage:
#   ./generate_stubs.sh          # regenerates stubs via Gradle
#   ./generate_stubs.sh --copy   # only copies proto file (no Gradle build)
#
# Requirements:
#   Java 21+
#   Gradle 8.5+ (or ./gradlew wrapper)

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# Resolve proto file location (go up from grpc/kotlin to project root, then to src/main/proto)
PROTO_DIR="$(cd "${SCRIPT_DIR}/../../src/main/proto" && pwd)"
PROTO_FILE="${PROTO_DIR}/freeplane.proto"
KOTLIN_PROTO_DIR="${SCRIPT_DIR}/src/main/proto"

if [[ ! -f "${PROTO_FILE}" ]]; then
    echo "ERROR: Proto file not found at ${PROTO_FILE}" >&2
    exit 1
fi

# Always copy the canonical proto file to the Kotlin module
mkdir -p "${KOTLIN_PROTO_DIR}"
cp "${PROTO_FILE}" "${KOTLIN_PROTO_DIR}/freeplane.proto"
echo "Synced proto file: ${KOTLIN_PROTO_DIR}/freeplane.proto"

# Check for --copy flag (skip Gradle build)
if [[ "${1:-}" == "--copy" ]]; then
    echo "Done. Proto file synced. Run './gradlew generateProto' to regenerate stubs."
    exit 0
fi

# Use Gradle to generate stubs (handles protoc-gen-grpc-java download automatically)
echo "Running Gradle to generate gRPC stubs..."
cd "${SCRIPT_DIR}"
./gradlew generateProto --quiet

echo "Done. Stubs generated in build/generated/source/proto/main/grpc/ and build/generated/source/proto/main/java/"

