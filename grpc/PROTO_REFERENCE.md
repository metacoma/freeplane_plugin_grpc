# Proto Configuration Reference

## Overview

The Freeplane gRPC plugin exposes 27 RPC methods for mind-map manipulation via the `Freeplane` service defined in `src/main/proto/freeplane.proto`.

- **Package**: `freeplane`
- **Service name**: `Freeplane`
- **Default port**: `50051` (configurable via `GRPC_LISTEN_PORT` env var)
- **Address**: `127.0.0.1` (configurable via `GRPC_LISTEN_ADDR` env var)

## RPC Methods (27 total)

### Node Creation & Deletion

| # | RPC Method | Request Message | Response Message | Description |
|---|-----------|----------------|-----------------|-------------|
| 1 | `CreateChild` | `CreateChildRequest` | `CreateChildResponse` | Create a child node under a parent |
| 2 | `DeleteChild` | `DeleteChildRequest` | `DeleteChildResponse` | Delete a node by ID |

### Node Attributes & Styling

| # | RPC Method | Request Message | Response Message | Description |
|---|-----------|----------------|-----------------|-------------|
| 3 | `NodeAttributeAdd` | `NodeAttributeAddRequest` | `NodeAttributeAddResponse` | Add an attribute to a node |
| 4 | `NodeLinkSet` | `NodeLinkSetRequest` | `NodeLinkSetResponse` | Set a hyperlink on a node |
| 5 | `NodeDetailsSet` | `NodeDetailsSetRequest` | `NodeDetailsSetResponse` | Set node details/description |
| 6 | `NodeNoteSet` | `NodeNoteSetRequest` | `NodeNoteSetResponse` | Set node note/annotation |
| 7 | `NodeTagSet` | `NodeTagSetRequest` | `NodeTagSetResponse` | Set tags on a node (replaces existing) |
| 8 | `NodeTagAdd` | `NodeTagAddRequest` | `NodeTagAddResponse` | Add tags to a node (appends) |
| 9 | `NodeConnect` | `NodeConnectRequest` | `NodeConnectResponse` | Create a connection between nodes |
| 10 | `NodeAddIcon` | `NodeAddIconRequest` | `NodeAddIconResponse` | Add an icon to a node |
| 11 | `Groovy` | `GroovyRequest` | `GroovyResponse` | Execute Groovy script (not ready) |
| 12 | `NodeColorSet` | `NodeColorSetRequest` | `NodeColorSetResponse` | Set text color (RGBA) |
| 13 | `NodeBackgroundColorSet` | `NodeBackgroundColorSetRequest` | `NodeBackgroundColorSetResponse` | Set background color (RGBA) |

### Mind Map Operations

| # | RPC Method | Request Message | Response Message | Description |
|---|-----------|----------------|-----------------|-------------|
| 14 | `StatusInfoSet` | `StatusInfoSetRequest` | `StatusInfoSetResponse` | Set status bar info |
| 15 | `TextFSM` | `TextFSMRequest` | `TextFSMResponse` | Apply TextFSM transformation |
| 16 | `MindMapFromJSON` | `MindMapFromJSONRequest` | `MindMapFromJSONResponse` | Import mind map from JSON |
| 17 | `MindMapToJSON` | `MindMapToJSONRequest` | `MindMapToJSONResponse` | Export mind map to JSON |

### Navigation

| # | RPC Method | Request Message | Response Message | Description |
|---|-----------|----------------|-----------------|-------------|
| 18 | `GetCurrentNode` | `GetCurrentNodeRequest` | `GetCurrentNodeResponse` | Get current node and map ID |
| 19 | `OpenMap` | `OpenMapRequest` | `OpenMapResponse` | Open a mind map file |
| 20 | `FocusNode` | `FocusNodeRequest` | `FocusNodeResponse` | Focus on a specific node |

### Node Inspection (Group A)

| # | RPC Method | Request Message | Response Message | Description |
|---|-----------|----------------|-----------------|-------------|
| 21 | `GetNodeText` | `GetNodeTextRequest` | `GetNodeTextResponse` | Get text of a node |
| 22 | `GetParentNode` | `GetParentNodeRequest` | `GetParentNodeResponse` | Get parent node info |
| 23 | `ListChildNodes` | `ListChildNodesRequest` | `ListChildNodesResponse` | List child nodes |
| 24 | `GetNodeNote` | `GetNodeNoteRequest` | `GetNodeNoteResponse` | Get node note |
| 25 | `GetNodeLink` | `GetNodeLinkRequest` | `GetNodeLinkResponse` | Get node hyperlink |

### Node Manipulation (Group B)

| # | RPC Method | Request Message | Response Message | Description |
|---|-----------|----------------|-----------------|-------------|
| 26 | `SetNodeText` | `SetNodeTextRequest` | `SetNodeTextResponse` | Set text of a node |
| 27 | `MoveNode` | `MoveNodeRequest` | `MoveNodeResponse` | Move a node to a new parent |

## Message Types Summary

| Message | Fields |
|---------|--------|
| `CreateChildRequest` | `name`, `parent_node_id` |
| `CreateChildResponse` | `node_id`, `node_text` |
| `DeleteChildRequest/Response` | `node_id` / `success` |
| `NodeAttributeAddRequest` | `node_id`, `attribute_name`, `attribute_value` |
| `NodeAttributeAddResponse` | `success` |
| `NodeLinkSetRequest` | `node_id`, `link` |
| `NodeLinkSetResponse` | `success` |
| `NodeDetailsSetRequest` | `node_id`, `details` |
| `NodeDetailsSetResponse` | `success` |
| `NodeNoteSetRequest` | `node_id`, `note` |
| `NodeNoteSetResponse` | `success` |
| `NodeTagSetRequest` | `node_id`, `tags[]` |
| `NodeTagSetResponse` | `success` |
| `NodeTagAddRequest` | `node_id`, `tags[]` |
| `NodeTagAddResponse` | `success` |
| `NodeConnectRequest` | `source_node_id`, `target_node_id`, `relationship` |
| `NodeConnectResponse` | `success` |
| `NodeAddIconRequest` | `node_id`, `icon_name` |
| `NodeAddIconResponse` | `success` |
| `GroovyRequest` | `groovy_code` |
| `GroovyResponse` | `success`, `result`, `error_message` |
| `NodeColorSetRequest` | `node_id`, `red`, `green`, `blue`, `alpha` |
| `NodeColorSetResponse` | `success` |
| `NodeBackgroundColorSetRequest` | `node_id`, `red`, `green`, `blue`, `alpha` |
| `NodeBackgroundColorSetResponse` | `success` |
| `StatusInfoSetRequest` | `statusInfo` |
| `StatusInfoSetResponse` | `success` |
| `TextFSMRequest` | `json` |
| `TextFSMResponse` | `success` |
| `MindMapFromJSONRequest` | `json` |
| `MindMapFromJSONResponse` | `success` |
| `MindMapToJSONRequest` | (empty) |
| `MindMapToJSONResponse` | `success`, `json` |
| `GetCurrentNodeRequest` | (empty) |
| `GetCurrentNodeResponse` | `map_id`, `node_id`, `success` |
| `OpenMapRequest` | `file_path` |
| `OpenMapResponse` | `success` |
| `FocusNodeRequest` | `node_id` |
| `FocusNodeResponse` | `success` |
| `GetNodeTextRequest` | `node_id` |
| `GetNodeTextResponse` | `success`, `node_id`, `text`, `error_message` |
| `GetParentNodeRequest` | `node_id` |
| `GetParentNodeResponse` | `success`, `node_id`, `parent_node_id`, `parent_node_text`, `error_message` |
| `ListChildNodesRequest` | `node_id` |
| `ListChildNodesResponse` | `success`, `children[]`, `error_message` |
| `ChildNodeInfo` | `node_id`, `text` |
| `GetNodeNoteRequest` | `node_id` |
| `GetNodeNoteResponse` | `success`, `node_id`, `note`, `has_note`, `error_message` |
| `GetNodeLinkRequest` | `node_id` |
| `GetNodeLinkResponse` | `success`, `node_id`, `link`, `has_link`, `error_message` |
| `SetNodeTextRequest` | `node_id`, `text` |
| `SetNodeTextResponse` | `success`, `node_id`, `error_message` |
| `MoveNodeRequest` | `node_id`, `new_parent_node_id` |
| `MoveNodeResponse` | `success`, `error_message` |

## Regenerating gRPC Stubs

### Python

```bash
cd grpc/python
./generate_stubs.sh
```

Requires: `grpcio-tools>=1.60.0`, `protobuf>=4.25.0`

### Ruby

```bash
cd grpc/ruby
./generate_stubs.sh
```

Requires: `grpc` gem, `protoc` matching the protobuf version

## Version Compatibility Matrix

| Component | Version | Notes |
|-----------|---------|-------|
| Java gRPC server | 1.52.0 | Built with Java 1.8 source/target |
| Python grpcio client | >= 1.60.0 | pyproject.toml dependency |
| Python protobuf | >= 4.25.0 | pyproject.toml dependency |
| Ruby grpc gem | (unpinned) | gemspec dependency |
| Ruby google-protobuf | >= 3.25, < 5.0 | gemspec dependency; Gemfile pins < 4.0 |
| Protoc | 3.21.12 | build.gradle |
| protobuf-java-util | 3.21.12 | build.gradle |

> **Note**: The Java gRPC version (1.52.0) is older than the Python client's minimum requirement (grpcio >= 1.60.0). Compatibility has been verified through integration testing.

## Adding a New RPC Method

1. Modify `src/main/proto/freeplane.proto` with the new RPC and message types.
2. Regenerate stubs:
   - Python: `cd grpc/python && ./generate_stubs.sh`
   - Ruby: `cd grpc/ruby && ./generate_stubs.sh`
3. Add wrapper method to `grpc/ruby/lib/freeplane_grpc_client/client.rb`.
4. Add wrapper method to `grpc/python/freeplane_grpc/client.py`.
5. Update this document with the new RPC entry.
