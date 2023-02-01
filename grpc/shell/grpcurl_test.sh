#!/usr/bin/env bash

GRPC_SERVER=127.0.0.1:50051
GRPC_ARGS="-plaintext -proto ./freeplane.proto -format json"
GRPC_CALL="grpcurl -plaintext -proto ./freeplane.proto -d @ ${GRPC_SERVER}"

. ~/mindwm/compiled/freeplane_plugin_grpc/functions.bash

createChild() {
  local name=$1
  local parent_node_id=$2
cat<<EOF  | jq | ${GRPC_CALL} freeplane.Freeplane/CreateChild
{
  "name": "${name}",
  "parent_node_id": "${parent_node_id}"
} 
EOF
} 

nodeDetailsSet() {
  local node_id=$1
  local details="$2"
cat<<EOF  | jq | ${GRPC_CALL} freeplane.Freeplane/NodeDetailsSet
{
  "node_id": "${node_id}",
  "details": "${details}"
} 
EOF
} 

CPU_TEMP_NODE_ID=`createChild "cpu" "" | jq -r '.nodeId'`
while :; do
  t=`sensors -j | jq -r '."coretemp-isa-0000"."Package id 0".temp1_input'`
  nodeDetailsSet ${CPU_TEMP_NODE_ID} "${t}"
  sleep 0.1
done
