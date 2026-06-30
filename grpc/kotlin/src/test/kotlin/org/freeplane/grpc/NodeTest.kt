package org.freeplane.grpc

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/**
 * Unit tests for Node class.
 */
class NodeTest {

    @Test
    fun `test node creation`() = runBlocking {
        val client = FreeplaneClient.create()
        val node = Node(client, "test-node-id")
        
        assertNotNull(node)
        assertEquals("test-node-id", node.nodeID())
        
        runCatching { client.close() }
    }

    @Test
    fun `test node with context`() = runBlocking {
        val client = FreeplaneClient.create()
        val node = Node(client, "test-node-id")
        val nodeWithContext = node.withContext(kotlinx.coroutines.Dispatchers.Default)
        
        assertNotNull(nodeWithContext)
        assertEquals("test-node-id", nodeWithContext.nodeID())
        
        runCatching { client.close() }
    }

    @Test
    fun `test node toString`() = runBlocking {
        val client = FreeplaneClient.create()
        val node = Node(client, "node-123")
        
        assertTrue(node.toString().contains("node-123"))
        
        runCatching { client.close() }
    }
}
