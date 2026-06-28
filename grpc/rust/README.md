# freeplane-grpc (Rust)

Rust gRPC client for the [Freeplane](https://www.freeplane.org/) mind-map application.

## Requirements

- Rust 1.70+ (edition 2021)
- Freeplane with the gRPC plugin enabled (default port: 50051)

## Installation

```bash
cd grpc/rust
cargo build
```

Or add as a dependency in your `Cargo.toml`:

```toml
[dependencies]
freeplane-grpc = { git = "https://github.com/metacoma/freeplane_plugin_grpc", branch = "main" }
```

## Usage

```rust
use freeplane_grpc::FreeplaneClient;

#[tokio::main]
async fn main() -> Result<(), Box<dyn std::error::Error>> {
    let mut client = FreeplaneClient::connect("127.0.0.1", 50051).await?;

    let mindmap = client.mind_map();
    let root = mindmap.root().await?;
    println!("Root: {}", root.get_text().await?);

    let child = root.add_child("Hello from Rust", "").await?;
    child.set_text("Modified text").await?;
    child.set_note("A note").await?;

    client.close().await;
    Ok(())
}
```

## Configuration

| Environment Variable | Default     | Description              |
|---------------------|-------------|--------------------------|
| `GRPC_LISTEN_ADDR`  | `0.0.0.0`   | Freeplane gRPC server bind address |
| `GRPC_LISTEN_PORT`  | `50051`     | Freeplane gRPC server port |

## API

### FreeplaneClient

- `connect(host, port)` — Open gRPC channel
- `close()` — Close gRPC channel
- `current_map()` — Get the current mind map
- `open_map(filePath)` — Open a .mm file
- `get_map_to_json()` — Export current map as JSON
- `mind_map_from_json(json)` — Import map from JSON
- `groovy(code)` — Execute Groovy code on the server
- `focus_node(nodeId)` — Focus a node in the UI
- `status_info_set(info)` — Set status bar text
- `text_fsm(json)` — Process JSON through TextFSM
- `mind_map()` — Create a `MindMap` instance
- `node(node_id)` — Create a `Node` instance

### Node

- `get_text()` / `set_text(text)`
- `add_child(text, style)` / `children()` / `parent()` / `delete()` / `move_to(new_parent_id)`
- `get_note()` / `set_note(note)`
- `get_attributes()` / `set_attribute(name, value)` / `set_attributes(attrs)`
- `get_links()` / `set_links(links)`
- `set_tags(tags)` / `add_tags(tags)`
- `add_icon(icon_name)`
- `set_color(r, g, b, a)` / `set_background_color(r, g, b, a)`
- `select()` / `center()` / `refresh()`
- `get_style()` / `set_style(style)`

### MindMap

- `root()` — Get the root node
- `selected_node()` — Get the currently selected node
- `find_nodes(pattern)` — Search nodes by text
- `create_node(text, parent_id, style)` / `create_child(parent, text, style)`
- `info()` / `size()`
- `save(path)` / `export(path, format)` / `import_map(path)`

### Error Types

- `FreeplaneGrpcError` — Base error
- `FreeplaneConnectionError` — Connection/network failures
- `FreeplaneOperationError` — Server-reported operation failures
- `NodeNotFoundError` — Requested node does not exist
- `MindMapError` — Map-level operation failures

## Testing

```bash
# Unit tests (mocked, no server required)
cargo test

# Integration tests (requires running Freeplane with gRPC plugin)
cargo test --test integration
```

## License

MIT
