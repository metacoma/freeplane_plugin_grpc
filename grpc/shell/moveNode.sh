#!/usr/bin/env bash
GRPC_SERVER=127.0.0.1:50051
GRPC_CALL="grpcurl -plaintext -proto ./freeplane.proto -d @ ${GRPC_SERVER}"

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
cd ${SCRIPT_DIR}

moveNode() {
  local node_id=$1
  local new_parent_node_id=$2
cat<<EOF | jq | ${GRPC_CALL} freeplane.Freeplane/MoveNode
{
  "node_id": "${node_id}",
  "new_parent_node_id": "${new_parent_node_id}"
}
EOF
}

# Example: move a node under a new parent
# Usage: moveNode <node_id> <new_parent_node_id>
moveNode $1 $2
