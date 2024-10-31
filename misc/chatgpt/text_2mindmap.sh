#!/usr/bin/env bash
(
cd ~/freeplane_plugin_grpc/misc/chatgpt
source ./.env
python3 ./send_request.py | jq | (cd ~/freeplane_plugin_grpc/grpc/shell/; bash ./import_json.sh )
)
