#!/usr/bin/env bash
GRPC_SERVER=127.0.0.1:50051
GRPC_CALL="grpcurl -plaintext -proto ./freeplane.proto -d @ ${GRPC_SERVER}"

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
cd ${SCRIPT_DIR}

setNodeText() {
  local node_id=$1
  local text=$2
cat<<EOF | jq | ${GRPC_CALL} freeplane.Freeplane/SetNodeText
{
  "node_id": "${node_id}",
  "text": "${text}"
}
EOF
}

# Example: set text of a node
# Usage: setNodeText <node_id> <new_text>
setNodeText $1 "$2"
