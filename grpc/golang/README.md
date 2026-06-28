# Freeplane gRPC Go Client

Object-oriented Go client library for the Freeplane gRPC plugin. Provides three layers:

- **Client** (`FreeplaneClient`) — connection management and all 27 RPC methods
- **MindMap** — map-level operations (root, selected node, find, size, export/import)
- **Node** — node-level operations (text, hierarchy, styling, notes, attributes, links, tags, icons)

## Prerequisites

- Go 1.25+
- `protoc` (protobuf compiler)
- `protoc-gen-go` and `protoc-gen-go-grpc` plugins

## Installation

```bash
cd grpc/golang
bash generate.sh
go mod tidy
```

## Quick Start

```go
package main

import (
    "context"
    "fmt"
    "log"

    freeplane "github.com/metacoma/freeplane_plugin_grpc/grpc/golang"
)

func main() {
    ctx := context.Background()

    // Create and connect client
    client := freeplane.NewFreeplaneClient("127.0.0.1", 50051)
    defer client.Close()

    if err := client.Connect(ctx); err != nil {
        log.Fatal(err)
    }

    // Get current mind map
    mindmap, err := client.CurrentMap(ctx)
    if err != nil {
        log.Fatal(err)
    }

    // Get root node
    root, err := mindmap.Root(ctx)
    if err != nil {
        log.Fatal(err)
    }

    // Get node text
    text, err := root.GetText(ctx)
    if err != nil {
        log.Fatal(err)
    }
    fmt.Println("Root:", text)

    // Add a child node
    child, err := root.AddChild(ctx, "New Child", "classic")
    if err != nil {
        log.Fatal(err)
    }
    fmt.Println("Child ID:", child.NodeID())

    // Find nodes
    matches, err := mindmap.FindNodes(ctx, "Child")
    if err != nil {
        log.Fatal(err)
    }
    fmt.Printf("Found %d matching nodes\n", len(matches))
}
```

## API Reference

### FreeplaneClient

| Method | Description |
|--------|-------------|
| `NewFreeplaneClient(host, port)` | Create a new client (defaults: 127.0.0.1:50051) |
| `Connect(ctx)` | Open gRPC connection |
| `Close()` | Close gRPC connection |
| `Do(fn)` | Context-manager-like execution |
| `CurrentMap(ctx)` | Get current mind map |
| `SelectedMap(ctx)` | Get selected mind map |
| `OpenMap(ctx, path)` | Open a mind map file |
| `GetMapToJSON(ctx)` | Export current map as JSON |
| `MindMapFromJSON(ctx, json)` | Import map from JSON |
| `Groovy(ctx, code)` | Execute Groovy code |
| `FocusNode(ctx, nodeID)` | Focus a node |
| `SetStatusInfo(ctx, info)` | Set status bar text |

#### Raw RPC Methods (all 27)

All 27 gRPC RPC methods are wrapped: `CreateChild`, `DeleteChild`, `NodeAttributeAdd`, `NodeLinkSet`, `NodeDetailsSet`, `NodeNoteSet`, `NodeTagSet`, `NodeTagAdd`, `NodeConnect`, `NodeAddIcon`, `Groovy`, `NodeColorSet`, `NodeBackgroundColorSet`, `StatusInfoSet`, `TextFSM`, `MindMapFromJSON`, `MindMapToJSON`, `GetCurrentNode`, `OpenMap`, `FocusNode`, `GetNodeText`, `GetParentNode`, `ListChildNodes`, `GetNodeNote`, `GetNodeLink`, `SetNodeText`, `MoveNode`.

### MindMap

| Method | Description |
|--------|-------------|
| `Root(ctx)` | Get root node |
| `SelectedNode(ctx)` | Get selected node |
| `FindNodes(ctx, pattern)` | Find nodes by text pattern |
| `Info()` | Get map metadata |
| `Size(ctx)` | Count nodes |
| `Save(ctx, path)` | Save map |
| `Export(ctx, path, format)` | Export map |
| `ImportMap(ctx, path)` | Import map |
| `CreateNode(ctx, text, parentID, style)` | Create a node |
| `CreateChild(ctx, parent, text, style)` | Create a child node |

### Node

| Method | Description |
|--------|-------------|
| `GetText(ctx)` | Get node text |
| `SetText(ctx, text)` | Set node text |
| `AddChild(ctx, text, style)` | Add child node |
| `Children(ctx)` | Get children |
| `Parent(ctx)` | Get parent |
| `Delete(ctx)` | Delete node |
| `Move(ctx, newParentID)` | Move node |
| `GetStyle(ctx)` | Get style (via Groovy) |
| `SetStyle(ctx, style)` | Set style (via Groovy) |
| `GetColor(ctx)` | Get foreground color (via Groovy) |
| `SetColor(ctx, r, g, b, a)` | Set foreground color |
| `GetBackgroundColor(ctx)` | Get background color (via Groovy) |
| `SetBackgroundColor(ctx, r, g, b, a)` | Set background color |
| `GetNote(ctx)` | Get note |
| `SetNote(ctx, note)` | Set note |
| `GetAttributes(ctx)` | Get attributes (via Groovy) |
| `SetAttribute(ctx, name, value)` | Set attribute |
| `SetAttributes(ctx, attrs)` | Set multiple attributes |
| `GetLinks(ctx)` | Get links |
| `SetLinks(ctx, links)` | Set links |
| `SetTags(ctx, tags)` | Set tags |
| `AddTags(ctx, tags)` | Add tags |
| `GetIcons(ctx)` | Get icons (via Groovy) |
| `AddIcon(ctx, iconName)` | Add icon |
| `GetFolded(ctx)` | Get folded state (via Groovy) |
| `SetFolded(ctx, folded)` | Set folded state (via Groovy) |
| `Select(ctx)` | Select node |
| `Center(ctx)` | Center on node |
| `Refresh(ctx)` | Refresh node state |

## Error Handling

The client uses Go custom error types with `errors.Is()` and `errors.As()` support:

```go
import (
    "errors"
    freeplane "github.com/metacoma/freeplane_plugin_grpc/grpc/golang"
)

// Check for connection errors
if freeplane.IsConnectionError(err) {
    log.Println("Connection failed")
}

// Check for operation errors
if freeplane.IsOperationError(err) {
    log.Println("Operation failed")
}

// Check for node not found
if freeplane.IsNodeNotFoundError(err) {
    log.Println("Node not found")
}

// Extract error details
var freeplaneErr *freeplane.FreeplaneError
if errors.As(err, &freeplaneErr) {
    fmt.Printf("Kind: %v, Message: %s\n", freeplaneErr.Kind, freeplaneErr.Msg)
}
```

### Error Types

| Type | Description |
|------|-------------|
| `FreeplaneError` | Base error type with `Kind`, `Msg`, and `Cause` fields |
| `ErrorGrpc` | Generic gRPC error |
| `ErrorConnection` | Connection error (server unavailable, timeout, etc.) |
| `ErrorOperation` | Operation failed on server |
| `ErrorNodeNotFound` | Requested node not found |
| `ErrorMindMap` | Mind map-level error |

## Testing

All tests are mocked and require no running Freeplane server:

```bash
# Run all tests
go test ./...

# Run with race detector
go test -race ./...

# Run with coverage
go test -cover ./...
```

## Proto Code Generation

```bash
bash generate.sh
```

This generates `pb/freeplane.pb.go` and `pb/freeplane_grpc.pb.go` from `src/main/proto/freeplane.proto`.

## License

Same as the parent repository.
