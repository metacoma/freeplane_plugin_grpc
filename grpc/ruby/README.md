# freeplane_grpc_client

A reusable Ruby gem providing high-level wrappers over all 27 RPC methods defined in the Freeplane gRPC plugin's `freeplane.proto`.

## Installation

```bash
cd grpc/ruby
bundle install
```

Then in your Ruby code:

```ruby
require "freeplane_grpc_client"
```

## Quick Start

### Low-level Client API

```ruby
require "freeplane_grpc_client"

client = FreeplaneGrpcClient::Client.new("127.0.0.1", 50051)
client.connect

# Create a child node
resp = client.create_child(name: "My Node", parent_node_id: "")
puts "Created node: #{resp.node_id}"

# Get current node info
resp = client.get_current_node
puts "Map: #{resp.map_id}, Node: #{resp.node_id}"

# Set node text
client.set_node_text(node_id: resp.node_id, text: "Hello World")

# Clean up
client.close
```

### High-level API (Client + Node + MindMap)

```ruby
require "freeplane_grpc_client"

client = FreeplaneGrpcClient::Client.new("127.0.0.1", 50051)

begin
  client.connect

  # Get the current map and root node
  map = client.current_map
  root = map.root
  puts "Root: #{root.get_text}"

  # Create a child node
  child = map.create_child(root.node_id, "New Node")
  child.set_text("Hello World")
  child.set_tags(["important"])
  child.set_note("A note")

  # Export the map
  json = map.to_json
  puts json

ensure
  client.close
end
```

### Configuration

The client accepts host and port via constructor or environment variables:

```ruby
# Constructor
client = FreeplaneGrpcClient::Client.new("10.0.0.1", 9000)

# Environment variables
ENV["FREEPLANE_HOST"] = "10.0.0.1"
ENV["FREEPLANE_PORT"] = "9000"
client = FreeplaneGrpcClient::Client.new  # uses env vars
```

## Public API

### Connection Management

| Method | Description |
|--------|-------------|
| `Client.new(host, port)` | Create a client (defaults: `127.0.0.1:50051`) |
| `client.connect` | Open the gRPC channel |
| `client.close` | Close the gRPC channel |
| `client.connected?` | Check if the channel is active |

### RPC Wrappers

All 27 RPC methods are available as snake_case instance methods. Each accepts keyword arguments matching the proto message fields and an optional `timeout:` parameter.

#### Node Operations

| Method | Proto RPC | Description |
|--------|-----------|-------------|
| `create_child(name:, parent_node_id:, timeout: nil)` | CreateChild | Create a child node |
| `delete_child(node_id:, timeout: nil)` | DeleteChild | Delete a node |
| `get_current_node(timeout: nil)` | GetCurrentNode | Get the current node |
| `get_node_text(node_id:, timeout: nil)` | GetNodeText | Get node text |
| `set_node_text(node_id:, text:, timeout: nil)` | SetNodeText | Set node text |
| `get_parent_node(node_id:, timeout: nil)` | GetParentNode | Get parent node info |
| `list_child_nodes(node_id:, timeout: nil)` | ListChildNodes | List child nodes |
| `move_node(node_id:, new_parent_node_id:, timeout: nil)` | MoveNode | Move a node |

#### Node Properties

| Method | Proto RPC | Description |
|--------|-----------|-------------|
| `node_attribute_add(node_id:, attribute_name:, attribute_value:, timeout: nil)` | NodeAttributeAdd | Add an attribute |
| `node_link_set(node_id:, link:, timeout: nil)` | NodeLinkSet | Set a node link |
| `node_details_set(node_id:, details:, timeout: nil)` | NodeDetailsSet | Set node details |
| `node_note_set(node_id:, note:, timeout: nil)` | NodeNoteSet | Set node note |
| `get_node_note(node_id:, timeout: nil)` | GetNodeNote | Get node note |
| `get_node_link(node_id:, timeout: nil)` | GetNodeLink | Get node link |

#### Tags

| Method | Proto RPC | Description |
|--------|-----------|-------------|
| `node_tag_set(node_id:, tags:, timeout: nil)` | NodeTagSet | Set tags (replaces) |
| `node_tag_add(node_id:, tags:, timeout: nil)` | NodeTagAdd | Add tags |

#### Styling

| Method | Proto RPC | Description |
|--------|-----------|-------------|
| `node_color_set(node_id:, red:, green:, blue:, alpha: 255, timeout: nil)` | NodeColorSet | Set node text color |
| `node_background_color_set(node_id:, red:, green:, blue:, alpha: 255, timeout: nil)` | NodeBackgroundColorSet | Set node background color |

#### Other Operations

| Method | Proto RPC | Description |
|--------|-----------|-------------|
| `node_connect(source_node_id:, target_node_id:, relationship:, timeout: nil)` | NodeConnect | Connect two nodes |
| `node_add_icon(node_id:, icon_name:, timeout: nil)` | NodeAddIcon | Add an icon to a node |
| `groovy(groovy_code:, timeout: nil)` | Groovy | Execute Groovy code |
| `status_info_set(status_info:, timeout: nil)` | StatusInfoSet | Set status bar text |
| `text_fsm(json:, timeout: nil)` | TextFSM | Run TextFSM template |
| `mind_map_from_json(json:, timeout: nil)` | MindMapFromJSON | Import mind map from JSON |
| `mind_map_to_json(timeout: nil)` | MindMapToJSON | Export mind map as JSON |
| `open_map(file_path:, timeout: nil)` | OpenMap | Open a mind map file |
| `focus_node(node_id:, timeout: nil)` | FocusNode | Focus on a node |

### Exceptions

| Exception | Description |
|-----------|-------------|
| `FreeplaneGrpcClient::Error` | Base exception class |
| `FreeplaneGrpcClient::ConnectionError` | Connection/gRPC-level failure |
| `FreeplaneGrpcClient::OperationError` | Server-reported operation failure |
| `FreeplaneGrpcClient::NodeNotFoundError` | Requested node not found |
| `FreeplaneGrpcClient::MindMapError` | Map-level operation failure |

### High-Level Classes

#### MindMap

| Method | Description |
|--------|-------------|
| `root` | Traverse up to find the root node |
| `selected_node` | Get the currently selected node |
| `find_nodes(pattern)` | Find nodes by text (regex or string) |
| `to_json` | Export map as JSON string |
| `create_node(text, parent_id)` | Create a node under parent |
| `create_child(parent, text)` | Create a child node |

#### Node

| Method | Description |
|--------|-------------|
| `get_text` / `set_text(text)` | Read/write node text |
| `add_child(text)` | Create a child node |
| `children` | List child nodes |
| `parent` | Get parent node |
| `delete` | Delete this node |
| `move(new_parent_id)` | Move to new parent |
| `set_color(r, g, b, a)` | Set text color |
| `set_background_color(r, g, b, a)` | Set background color |
| `get_note` / `set_note(note)` | Read/write node note |
| `set_attribute(name, value)` | Set node attribute |
| `get_link` / `set_link(link)` | Read/write node link |
| `set_tags(tags)` / `add_tags(tags)` | Manage tags |
| `add_icon(icon_name)` | Add an icon |
| `set_details(details)` | Set node details |
| `focus` / `select` | Focus/select node |

## Proto Generation

To regenerate the Ruby protobuf stubs from `freeplane.proto`:

```bash
cd grpc/ruby
./bin/generate_proto_ruby
```

This uses `protoc` with the `--ruby_out` and `--grpc_ruby_out` flags to regenerate `lib/freeplane_pb.rb` and `lib/freeplane_services_pb.rb`.

Requirements:
- `protoc` installed and on PATH
- Ruby `grpc` and `protobuf` gems

## Running Tests

Tests use RSpec with mocks/stubs — no Freeplane instance is required:

```bash
cd grpc/ruby
bundle install
bundle exec rspec
```

## Examples

### basic_usage.rb

A high-level example demonstrating the Node and MindMap API:

```bash
FREEPLANE_HOST=127.0.0.1 FREEPLANE_PORT=50051 ruby examples/basic_usage.rb
```

### Existing Examples

The original Ruby examples (`pomodoro.rb`, `setStatusSet.rb`) are preserved and untouched under `grpc/ruby/`.
