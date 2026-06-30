/*
 * Basic usage example for the freeplane_grpc Kotlin client.
 *
 * This example demonstrates:
 * - Connecting to a Freeplane gRPC server
 * - Getting the current map and root node
 * - Creating and modifying nodes
 * - Reading node information
 * - Using high-level object methods
 * - AutoCloseable resource management (try-with-resources)
 *
 * Usage (requires a running Freeplane instance with the gRPC plugin):
 *
 *   # Build and run via Gradle:
 *   ./gradlew run
 *
 * To connect to a non-default host/port:
 *
 *   FREEPLANE_HOST=192.168.1.100 FREEPLANE_PORT=9000 ./gradlew run
 */

package org.freeplane.grpc.examples

import kotlinx.coroutines.runBlocking
import org.freeplane.grpc.FreeplaneClient
import org.freeplane.grpc.FreeplaneOperationError

fun main() = runBlocking {
    val host = System.getenv("FREEPLANE_HOST") ?: "127.0.0.1"
    val port = System.getenv("FREEPLANE_PORT")?.toIntOrNull() ?: 50051

    println("Connecting to Freeplane gRPC server at $host:$port...")

    // Use try-with-resources for automatic cleanup (AutoCloseable)
    FreeplaneClient.create(host, port).use { client ->
        // Connect to server
        client.connect()

        // Get the currently open mind map
        println("\n--- Current Map ---")
        val mindMap = client.currentMap()
        val (mapId, nodeId) = mindMap.info()
        println("Map ID: $mapId")
        println("Current node: $nodeId")

        // Get the root node
        println("\n--- Root Node ---")
        val root = mindMap.root()
        println("Root text: ${root.getText()}")

        // Create a child node under root
        println("\n--- Creating Child Node ---")
        val child = root.addChild("Hello from Kotlin!")
        println("Created child: ${child.nodeID()}")

        // Modify the child node
        child.setText("Updated text via Kotlin")
        println("Updated text: ${child.getText()}")

        // Center the view on the child node
        child.center()
        println("Centered view on child node")

        // Add a child to the child node
        val grandchild = child.addChild("Grandchild node")
        grandchild.setAttribute("type", "example")
        grandchild.setNotes("This is a note for the grandchild node.")
        println("Grandchild: ${grandchild.nodeID()}, notes: ${grandchild.getNotes()}")

        // List all children of root
        println("\n--- Children of Root ---")
        val children = root.children()
        for (c in children) {
            println("  [${c.nodeID()}] ${c.getText()}")
        }

        // Search for nodes
        println("\n--- Search: 'Kotlin' ---")
        val matches = mindMap.findNodes("Kotlin")
        for (m in matches) {
            println("  [${m.nodeID()}] ${m.getText()}")
        }

        // Set a tag on a node
        child.addTags(listOf("kotlin", "example"))
        println("\nAdded tags to child node")

        // Get parent of child
        val parentNode = child.parent()
        println("Parent of child: ${parentNode?.nodeID()} — ${parentNode?.getText()}")

        // Set foreground color (red)
        child.setColor(255, 0, 0, 255)
        println("Set child node color to red")

        // Set background color
        child.setBackgroundColor(255, 255, 200, 255)
        println("Set child node background color to light yellow")

        // Export map as JSON
        println("\n--- Map JSON (truncated) ---")
        val jsonData = client.getMapToJSON()
        println("JSON length: ${jsonData.length} characters")
        println("First 200 chars: ${jsonData.take(200)}...")

        println("\n--- Example completed successfully ---")
    }
}
