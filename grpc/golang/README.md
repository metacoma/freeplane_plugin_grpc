# Freeplane gRPC Go Client

A Go client for interacting with Freeplane via its gRPC plugin. This client provides a high-level API for manipulating mind maps, nodes, and their properties.

## Requirements

- Go 1.21+
- Freeplane with gRPC plugin running

## Installation

```bash
go get github.com/metacoma/freeplane_plugin_grpc/grpc/golang
```

## Stub Generation

To regenerate the Go protobuf stubs from the proto definition:

```bash
bash generate_stubs.sh
```

This will generate the following files in the `freeplane/` subdirectory:
- `freeplane.pb.go` - Protobuf message types
- `freeplane_grpc.pb.go` - gRPC service stubs

## Quick Start

```go
package main

import (
    "context"
    "fmt"
    "log"
    "time"

    "github.com/metacoma/freeplane_plugin_grpc/grpc/golang"
)

func main() {
    // Create a new client
    client, err := freeplane_grpc.NewClient("127.0.0.1", 50051)
    if err != nil {
        log.Fatal(err)
    }

    // Connect to the server
    if err := client.Connect(); err != nil {
        log.Fatal(err)
    }
    defer client.Close()

    ctx := context.Background()

    // Get the current mind map
    mindMap, err := client.CurrentMap(ctx)
    if err != nil {
        log.Fatal(err)
    }

    // Get the root node
    root, err := mindMap.Root()
    if err != nil {
        log.Fatal(err)
    }

    // Get node text
    text, err := root.GetText()
    if err != nil {
        log.Fatal(err)
    }
    fmt.Printf("Root text: %s\n", text)

    // Create a child node
    child, err := root.AddChild("New Node", "")
    if err != nil {
        log.Fatal(err)
    }

    // Set node text
    if err := child.SetText("Hello, Freeplane!"); err != nil {
        log.Fatal(err)
    }

    // Set tags
    if err := child.SetTags([]string{"tag1", "tag2"}); err != nil {
        log.Fatal(err)
    }

    // Export map to JSON
    jsonData, err := client.GetMapToJSON(ctx)
    if err != nil {
        log.Fatal(err)
    }
    fmt.Printf("Map JSON: %s\n", jsonData)
}
```

## API Overview

### FreeplaneClient

The main entry point for interacting with Freeplane.

| Method | Description |
|--------|-------------|
| `NewClient(host, port)` | Create a new client |
| `Connect()` | Connect to the Freeplane server |
| `Close()` | Close the connection |
| `CurrentMap(ctx)` | Get the current mind map |
| `OpenMap(ctx, path)` | Open a mind map file |
| `GetMapToJSON(ctx)` | Export the current map to JSON |
| `MindMapFromJSON(ctx, json)` | Import a map from JSON |
| `Groovy(ctx, code)` | Execute Groovy code |
| `FocusNode(ctx, nodeID)` | Focus on a node |
| `SetStatusInfoText(ctx, info)` | Set status bar info |

### Node

Represents a node in a mind map.

| Method | Description |
|--------|-------------|
| `GetText()` | Get node text |
| `SetText(text)` | Set node text |
| `AddChild(text, style)` | Create a child node |
| `Children()` | Get child nodes |
| `Parent()` | Get parent node |
| `Delete()` | Delete the node |
| `Move(newParentID)` | Move to a new parent |
| `SetColor(r, g, b, a)` | Set text color |
| `SetBackgroundColor(r, g, b, a)` | Set background color |
| `GetNotes()` | Get node notes |
| `SetNotes(notes)` | Set node notes |
| `SetAttribute(name, value)` | Set an attribute |
| `SetTags(tags)` | Set tags |
| `AddTags(tags)` | Add tags |
| `SetLinks(link)` | Set a link |
| `GetFolded()` | Get folded state |
| `SetFolded(folded)` | Set folded state |
| `Select()` | Select the node |
| `Center()` | Center the node in view |
| `Refresh()` | Refresh the node |

### MindMap

Represents a mind map.

| Method | Description |
|--------|-------------|
| `Root()` | Get the root node |
| `SelectedNode()` | Get the selected node |
| `FindNodes(pattern)` | Find nodes matching a pattern |
| `Size()` | Get the number of nodes |
| `Save(path)` | Save the map |
| `Export(path, format)` | Export the map |
| `CreateNode(text, parentID, style)` | Create a node |
| `CreateChild(parent, text)` | Create a child node |

## Exception Types

| Exception | Description |
|-----------|-------------|
| `FreeplaneGrpcError` | Base error type |
| `FreeplaneConnectionError` | Connection failure |
| `FreeplaneOperationError` | Operation failure |
| `NodeNotFoundError` | Node not found |
| `MindMapError` | Mind map error |

## Running Examples

```bash
go run examples/basic_usage.go
```

## Running Tests

```bash
go test ./...
```

All tests are mocked and do not require a running Freeplane server.

## Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `FREEPLANE_HOST` | `127.0.0.1` | Freeplane server hostname |
| `FREEPLANE_PORT` | `50051` | Freeplane server port |

## Compatibility

- Go 1.21+
- gRPC v1.64.0
- Protobuf v1.33.0
