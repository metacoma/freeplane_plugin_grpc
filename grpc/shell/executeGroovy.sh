#!/usr/bin/env bash
# executeGroovy.sh — Execute a Groovy snippet via the Freeplane gRPC plugin.
#
# Usage: ./executeGroovy.sh [SERVER_ADDRESS]
#   SERVER_ADDRESS: gRPC server address (default: localhost:50051)

set -euo pipefail

GRPC_SERVER="${1:-localhost:50051}"
GRPC_CALL="grpcurl -plaintext -proto ./freeplane.proto -d @ ${GRPC_SERVER}"

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
cd "${SCRIPT_DIR}"

# Send a Groovy snippet that reads the current node's text.
# The script uses the Freeplane 'node' binding available in scripts.
cat <<'EOF' | jq | ${GRPC_CALL} freeplane.Freeplane/Groovy
{
  "groovy_code": "def n = node.text\n\"Current node text: \" + n"
}
EOF
