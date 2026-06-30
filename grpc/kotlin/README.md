# Freeplane gRPC Kotlin Client

Kotlin client for the Freeplane gRPC plugin. Provides a coroutine-based API for interacting with Freeplane mind maps over gRPC.

## Features

- **Coroutine-based API**: All methods support Kotlin coroutines for async/await patterns
- **Full RPC coverage**: All 27 Freeplane RPC methods are implemented
- **Three-tier architecture**: `FreeplaneClient`, `Node`, and `MindMap` classes
- **Exception hierarchy**: Consistent error handling with `FreeplaneGrpcError`, `FreeplaneConnectionError`, `FreeplaneOperationError`, `NodeNotFoundError`, and `MindMapError`
- **Auto-closeable**: Implements `AutoCloseable` for resource management

## Dependencies

- Kotlin 1.9.20
- gRPC Java 1.52.0
- Kotlin Coroutines 1.7.3
- Protobuf Kotlin 3.21.12

## Quick Start

```kotlin
import org.freeplane.grpc.FreeplaneClient
import kotlinx.coroutines.runBlocking

fun main() = runBlocking {
    // Create client (uses defaults: 127.0.0.1:50051)
    val client = FreeplaneClient.create()
    
    try {
        // Connect to server
        client.connect()
        
        // Get current mind map
        val map = client.currentMap()
        
        // Get root node
        val root = map.root()
        
        // Get node text
        val text = root.getText()
        println("Root text: $text")
        
        // Create child node
        val child = root.addChild("New Child")
        child.setText("Hello, Freeplane!")
        
        // List children
        val children = root.children()
        println("Children: ${children.size}")
        
    } finally {
        client.close()
    }
}
```

## Environment Variables

- `FREEPLANE_HOST`: Override the default host (default: `127.0.0.1`)
- `FREEPLANE_PORT`: Override the default port (default: `50051`)

## Building

```bash
./gradlew build
```

## Running Tests

```bash
./gradlew test
```

## API Reference

### FreeplaneClient

Main entry point for interacting with Freeplane.

```kotlin
// Create client
val client = FreeplaneClient.create(host, port)

// Connect
client.connect()

// Close connection
client.close()

// RPC methods (all suspend functions)
client.createChild(name, parentNodeId)
client.deleteChild(nodeId)
client.getNodeText(nodeId)
client.setNodeText(nodeId, text)
// ... and 23 more RPC methods
```

### Node

Represents a mind map node.

```kotlin
// Get/set text
val text = node.getText()
node.setText("New text")

// Hierarchy
val children = node.children()
val parent = node.parent()
val child = node.addChild("Child text")
node.delete()
node.move(newParentId)

// Styling
node.setStyle("style-name")
node.setColor(255, 0, 0, 255)
node.setBackgroundColor(0, 255, 0, 255)

// Notes
val (note, hasNote) = node.getNotes()
node.setNotes("Note content")

// Attributes
node.setAttribute("key", "value")

// Links
val (link, hasLink) = node.getLinks()
node.setLinks("http://example.com")

// Tags
node.setTags(listOf("tag1", "tag2"))
node.addTags(listOf("tag3"))

// Icons
node.addIcon("icon-name")

// State
node.setFolded(true)

// Actions
node.select()
node.center()
node.refresh()
```

### MindMap

Represents a mind map.

```kotlin
// Navigation
val root = mindMap.root()
val selected = mindMap.selectedNode()
val found = mindMap.findNodes("pattern")

// Metadata
val (mapId, nodeId) = mindMap.info()
val size = mindMap.size()

// File operations
mindMap.save("/path/to/file.mm")
mindMap.export("/path/to/file.pdf", "pdf")
mindMap.importMap("/path/to/file.mm")

// Node creation
val node = mindMap.createNode("Text", parentId)
val child = mindMap.createChild(parent, "Child text")
```

## Error Handling

```kotlin
try {
    client.connect()
    val map = client.currentMap()
} catch (e: FreeplaneConnectionError) {
    // Network/connection failure
    println("Connection failed: ${e.message}")
} catch (e: NodeNotFoundError) {
    // Requested node not found
    println("Node not found: ${e.message}")
} catch (e: FreeplaneOperationError) {
    // Server-reported operation failure
    println("Operation failed: ${e.message}")
}
```

## License

Same as the Freeplane project.
