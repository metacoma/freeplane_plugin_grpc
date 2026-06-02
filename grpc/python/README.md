# freeplane-grpc — Python Client Library

A Python client library for the [Freeplane gRPC plugin](https://github.com/metacoma/freeplane_plugin_grpc).

Provides high-level, object-oriented abstractions (`FreeplaneClient`, `MindMap`, `Node`) for interacting with Freeplane mind maps through the gRPC protocol, without requiring users to manually construct protobuf messages.

## Requirements

- Python 3.10+
- A running Freeplane instance with the gRPC plugin (listens on port 50051 by default)

## Installation

```bash
cd grpc/python
pip install -e .
```

For development:

```bash
pip install -e ".[dev]"
```

## Stub Generation

The generated gRPC stubs (`freeplane_pb2.py`, `freeplane_pb2_grpc.py`) are pre-committed in this directory.  If you modify `freeplane.proto`, regenerate them:

```bash
./generate_stubs.sh
```

This invokes `grpc_tools.protoc` using the `.proto` file from `src/main/proto/`.

## Quick Start

```python
from freeplane_grpc import FreeplaneClient

with FreeplaneClient(host="127.0.0.1", port=50051) as client:
    mindmap = client.current_map()
    root = mindmap.root()
    print(root.get_text())

    child = root.add_child("Hello from Python!")
    child.set_text("Updated text")
    child.center()

    for node in root.children():
        print(f"  {node.node_id}: {node.get_text()}")

    matches = mindmap.find_nodes("TODO")
    print(f"Found {len(matches)} nodes matching 'TODO'")
```

## API Overview

### FreeplaneClient

The main entry point. Manages the gRPC channel and exposes top-level operations.

| Method | Description |
|---|---|
| `__init__(host, port)` | Initialize client with server address |
| `connect()` | Open the gRPC channel |
| `close()` | Close the gRPC channel |
| `current_map()` | Get the currently open MindMap |
| `selected_node()` | Get the currently selected node |
| `open_map(path)` | Open a mind map file |
| `get_map_to_json()` | Export the current map as JSON |
| `mind_map_from_json(json)` | Import a mind map from JSON |
| `groovy(code)` | Execute Groovy code on the server |
| `focus_node(node_id)` | Focus (select) a node in the UI |
| `set_status_info(info)` | Set the status bar text |

### MindMap

Represents a Freeplane mind map.

| Method | Description |
|---|---|
| `root()` | Get the root node |
| `selected_node()` | Get the currently selected node |
| `find_nodes(pattern)` | Find all nodes matching a text pattern |
| `info()` | Get map metadata (map_id, node_id) |
| `size()` | Approximate node count |
| `save(path)` | Save the map |
| `export(path, format)` | Export the map to a file |
| `create_node(text, parent_id)` | Create a new node |
| `create_child(parent, text)` | Create a child node |

### Node

Represents a Freeplane mind map node.

| Method | Description |
|---|---|
| `get_text()` | Get node text |
| `set_text(text)` | Set node text |
| `add_child(text)` | Add a child node |
| `children()` | Get direct children |
| `parent()` | Get parent node |
| `delete()` | Delete this node |
| `move(new_parent_id)` | Move node under a new parent |
| `get_style()` | Get node style |
| `set_style(style)` | Set node style |
| `get_color()` | Get foreground color |
| `set_color(r, g, b, a)` | Set foreground color |
| `set_background_color(r, g, b, a)` | Set background color |
| `get_notes()` | Get node notes (HTML) |
| `set_notes(notes)` | Set node notes |
| `get_attributes()` | Get custom attributes |
| `set_attribute(name, value)` | Set a custom attribute |
| `set_attributes(attrs)` | Set multiple attributes |
| `get_links()` | Get node links |
| `set_links(links)` | Set node links |
| `set_tags(tags)` | Set node tags |
| `add_tags(tags)` | Add tags |
| `get_icons()` | Get node icons |
| `add_icon(name)` | Add an icon |
| `get_folded()` | Get folded (collapsed) state |
| `set_folded(folded)` | Set folded state |
| `select()` | Select this node in the UI |
| `center()` | Center the view on this node |
| `refresh()` | Reload node state from server |

## Exception Types

| Exception | Description |
|---|---|
| `FreeplaneGrpcError` | Base exception for all gRPC errors |
| `FreeplaneConnectionError` | Connection/channel failures |
| `FreeplaneOperationError` | Server-reported operation failures |
| `NodeNotFoundError` | Requested node does not exist |
| `MindMapError` | Map-level operation failure |

## Running Examples

```bash
# Requires a running Freeplane with the gRPC plugin
python3 examples/basic_usage.py
```

To connect to a non-default server:

```bash
FREEPLANE_HOST=192.168.1.100 FREEPLANE_PORT=9000 python3 examples/basic_usage.py
```

## Running Tests

```bash
pytest -v
```

## Compatibility

- **Python**: 3.10+
- **grpcio**: >= 1.60.0
- **protobuf**: >= 4.25.0

The generated stubs (`freeplane_pb2.py`, `freeplane_pb2_grpc.py`) are committed to the repository.  If you modify `freeplane.proto`, regenerate them with `./generate_stubs.sh`.

## License

Same as the Freeplane gRPC plugin repository.
