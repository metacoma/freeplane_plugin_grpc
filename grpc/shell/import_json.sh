#!/usr/bin/env bash

GRPC_SERVER=127.0.0.1:50051
GRPC_ARGS="-plaintext -proto ./freeplane.proto -format json"
GRPC_CALL="grpcurl -plaintext -proto ./freeplane.proto -d @ ${GRPC_SERVER}"

#. ~/mindwm/compiled/freeplane_plugin_grpc/functions.bash

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

JsonToString() {
    jq | jq -c '@json'
} 

MindMapFromJson() {
    jsonString=$(JsonToString)
#cat<<EOF | jq | jq -c '@json' #| 
cat<<EOF | jq | ${GRPC_CALL} freeplane.Freeplane/MindMapFromJSON
{
    "json": ${jsonString}
} 
EOF
} 
MindMapFromJson
# cat<<EOF | MindMapFromJson
# {
#     "parent": {
#         "child": "abc",
#         "detail": "AAAA",
#         "subchild": {
#             "abc": {}, 
#             "bca": {}
#         } 
#     }
# } 
# EOF
