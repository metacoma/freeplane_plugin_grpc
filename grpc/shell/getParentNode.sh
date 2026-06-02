#!/usr/bin/env bash
GRPC_SERVER=127.0.0.1:50051
GRPC_CALL="grpcurl -plaintext -proto ./freeplane.proto -d @ ${GRPC_SERVER}"

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
cd ${SCRIPT_DIR}

getParentNode() {
  local node_id=$1
cat<<EOF | jq | ${GRPC_CALL} freeplane.Freeplane/GetParentNode
{
  "node_id": "${node_id}"
}
EOF
}

# Example: get parent of a node
# Usage: getParentNode <node_id>
getParentNode $1
