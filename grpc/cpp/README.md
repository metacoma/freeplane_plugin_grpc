# Freeplane gRPC C++ Client

C++ client library for the Freeplane gRPC plugin. Provides object-oriented access to Freeplane mind maps via gRPC.

## Features

- Full coverage of all 27 gRPC RPC methods
- Object-oriented API with `FreeplaneClient`, `Node`, and `MindMap` classes
- Consistent exception hierarchy across all Freeplane gRPC clients
- Synchronous (blocking) API
- CMake build system

## Prerequisites

- C++17 compatible compiler (GCC 7+, Clang 5+, MSVC 2017+)
- CMake 3.14+
- Protocol Buffers compiler (`protobuf-compiler`)
- gRPC C++ plugin (`protobuf-compiler-grpc` on Debian/Ubuntu, or `grpc_cpp_plugin`)
- gRPC C++ library (`libgrpc++-dev`)
- Protocol Buffers C++ library (`libprotobuf-dev`)
- GoogleTest (for tests, optional)

### Installing Dependencies (Debian/Ubuntu)

```bash
sudo apt-get update
sudo apt-get install -y \
    cmake \
    protobuf-compiler \
    protobuf-compiler-grpc \
    libgrpc++-dev \
    libprotobuf-dev \
    libgtest-dev
```

### Installing Dependencies (macOS with Homebrew)

```bash
brew install cmake protobuf grpc googletest
```

## Building

```bash
cd grpc/cpp
mkdir -p build
cd build
cmake ..
make -j$(nproc)
```

### Running Tests

```bash
cd build
ctest --output-on-failure
```

## Usage

### Basic Example

```cpp
#include <iostream>
#include "freeplane_grpc/client.h"
#include "freeplane_grpc/mindmap.h"
#include "freeplane_grpc/node.h"

int main() {
    try {
        // Create client and connect
        freeplane::grpc::FreeplaneClient client("127.0.0.1", 50051);
        client.connect();
        
        // Get current mind map
        auto mindmap = client.currentMap();
        
        // Get root node
        auto root = mindmap->root();
        std::cout << "Root node text: " << root->getText() << std::endl;
        
        // Add a child node
        auto child = root->addChild("New Child Node");
        std::cout << "Created child with ID: " << child->nodeId() << std::endl;
        
        // Find nodes
        auto matches = mindmap->findNodes("Child");
        std::cout << "Found " << matches.size() << " matching nodes" << std::endl;
        
        // Close connection
        client.close();
    } catch (const freeplane::grpc::FreeplaneGrpcError& e) {
        std::cerr << "Freeplane gRPC error: " << e.what() << std::endl;
        return 1;
    } catch (const std::exception& e) {
        std::cerr << "Error: " << e.what() << std::endl;
        return 1;
    }
    
    return 0;
}
```

### Exception Handling

```cpp
#include "freeplane_grpc/error.h"

try {
    // ... gRPC operations ...
} catch (const freeplane::grpc::FreeplaneConnectionError& e) {
    // Network/connection error
    std::cerr << "Connection failed: " << e.what() << std::endl;
} catch (const freeplane::grpc::NodeNotFoundError& e) {
    // Node not found
    std::cerr << "Node not found: " << e.what() << std::endl;
} catch (const freeplane::grpc::FreeplaneOperationError& e) {
    // Server-reported operation failure
    std::cerr << "Operation failed: " << e.what() << std::endl;
} catch (const freeplane::grpc::FreeplaneGrpcError& e) {
    // Base exception for all Freeplane gRPC errors
    std::cerr << "Freeplane gRPC error: " << e.what() << std::endl;
}
```

## API Reference

### FreeplaneClient

Main entry point for interacting with Freeplane via gRPC.

| Method | Description |
|--------|-------------|
| `connect()` | Connect to the Freeplane gRPC server |
| `close()` | Close the connection |
| `currentMap()` | Get the currently open mind map |
| `selectedMap()` | Alias for currentMap() |
| `openMap(path)` | Open a mind map file |
| `getMapToJson()` | Export current map as JSON |
| `mindMapFromJson(json)` | Import map from JSON |
| `groovy(code)` | Execute Groovy code on server |
| `focusNode(nodeId)` | Focus a node in the UI |
| `setStatusInfo(info)` | Set status bar text |

#### Raw RPC Methods (27 total)

All 27 RPC methods from the proto definition are exposed:

| Method | Request Fields | Response Fields |
|--------|---------------|-----------------|
| `createChild(name, parentId)` | name, parent_node_id | node_id, node_text, success |
| `deleteChild(nodeId)` | node_id | success |
| `nodeAttributeAdd(nodeId, name, value)` | node_id, attribute_name, attribute_value | success |
| `nodeLinkSet(nodeId, link)` | node_id, link | success |
| `nodeDetailsSet(nodeId, details)` | node_id, details | success |
| `nodeNoteSet(nodeId, note)` | node_id, note | success |
| `nodeTagSet(nodeId, tags)` | node_id, tags[] | success |
| `nodeTagAdd(nodeId, tags)` | node_id, tags[] | success |
| `nodeConnect(src, tgt, rel)` | source_node_id, target_node_id, relationship | success |
| `nodeAddIcon(nodeId, icon)` | node_id, icon_name | success |
| `groovyRpc(code)` | groovy_code | success, result, error_message |
| `nodeColorSet(...)` | node_id, red, green, blue, alpha | success |
| `nodeBackgroundColorSet(...)` | node_id, red, green, blue, alpha | success |
| `statusInfoSet(info)` | status_info | success |
| `textFSM(json)` | json | success |
| `mindMapFromJsonRpc(json)` | json | success |
| `mindMapToJson()` | - | success, json |
| `getCurrentNode()` | - | map_id, node_id, success |
| `openMapRpc(path)` | file_path | success |
| `focusNodeRpc(nodeId)` | node_id | success |
| `getNodeText(nodeId)` | node_id | success, node_id, text, error_message |
| `getParentNode(nodeId)` | node_id | success, node_id, parent_node_id, parent_node_text, error_message |
| `listChildNodes(nodeId)` | node_id | success, children[], error_message |
| `getNodeNote(nodeId)` | node_id | success, node_id, note, has_note, error_message |
| `getNodeLink(nodeId)` | node_id | success, node_id, link, has_link, error_message |
| `setNodeText(nodeId, text)` | node_id, text | success, node_id, error_message |
| `moveNode(nodeId, parentId)` | node_id, new_parent_node_id | success, error_message |

### Node

Represents a Freeplane mind map node.

| Method | Description |
|--------|-------------|
| `getText()` | Get node text |
| `setText(text)` | Set node text |
| `addChild(text, style)` | Add child node |
| `children()` | Get child nodes |
| `parent()` | Get parent node |
| `deleteNode()` | Delete this node |
| `move(newParentId)` | Move node under new parent |
| `setStyle(style)` | Set node style |
| `setColor(r, g, b, a)` | Set foreground color |
| `setBackgroundColor(r, g, b, a)` | Set background color |
| `getNote()` | Get node note |
| `setNote(note)` | Set node note |
| `setAttribute(name, value)` | Set custom attribute |
| `setAttributes(attrs)` | Set multiple attributes |
| `getLinks()` | Get node links |
| `setLinks(links)` | Set node links |
| `setTags(tags)` | Set node tags |
| `addTags(tags)` | Add tags |
| `addIcon(iconName)` | Add icon |
| `getFolded()` | Get folded state |
| `setFolded(folded)` | Set folded state |
| `select()` | Select node in UI |
| `center()` | Center view on node |
| `refresh()` | Reload node state |

### MindMap

Represents a Freeplane mind map.

| Method | Description |
|--------|-------------|
| `root()` | Get root node |
| `selectedNode()` | Get selected node |
| `findNodes(pattern)` | Find nodes by text pattern |
| `info()` | Get map metadata |
| `size()` | Count nodes |
| `save(path)` | Save map |
| `exportMap(path, format)` | Export to file |
| `importMap(path)` | Import from file |
| `createNode(text, parentId, style)` | Create new node |
| `createChild(parent, text, style)` | Create child node |
| `getToJson()` | Export map as JSON |
| `setFromJson(json)` | Import map from JSON |

## Exception Hierarchy

```
std::exception
  └── freeplane::grpc::FreeplaneGrpcError
        ├── freeplane::grpc::FreeplaneConnectionError
        └── freeplane::grpc::FreeplaneOperationError
              ├── freeplane::grpc::NodeNotFoundError
              └── freeplane::grpc::MindMapError
```

## Regenerating Stubs

If the proto file changes, regenerate the C++ stubs:

```bash
cd grpc/cpp
bash generate_stubs.sh
```

Requirements:
- `protoc` (Protocol Buffers compiler)
- `grpc_cpp_plugin` (gRPC C++ plugin)

## License

Same license as the parent project.
