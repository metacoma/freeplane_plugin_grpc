package org.freeplane.grpc

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/**
 * Unit tests for MindMap class.
 */
class MindMapTest {

    @Test
    fun `test mindmap creation`() = runBlocking {
        val client = FreeplaneClient.create()
        val mindMap = MindMap(client, "map-123", "node-456")
        
        assertNotNull(mindMap)
        assertEquals("map-123", mindMap.mapID())
        assertEquals("node-456", mindMap.nodeID())
        
        runCatching { client.close() }
    }

    @Test
    fun `test mindmap with context`() = runBlocking {
        val client = FreeplaneClient.create()
        val mindMap = MindMap(client, "map-123", "node-456")
        val mindMapWithContext = mindMap.withContext(kotlinx.coroutines.Dispatchers.Default)
        
        assertNotNull(mindMapWithContext)
        assertEquals("map-123", mindMapWithContext.mapID())
        
        runCatching { client.close() }
    }

    @Test
    fun `test mindmap info`() = runBlocking {
        val client = FreeplaneClient.create()
        val mindMap = MindMap(client, "map-123", "node-456")
        
        val (mapId, nodeId) = mindMap.info()
        assertEquals("map-123", mapId)
        assertEquals("node-456", nodeId)
        
        runCatching { client.close() }
    }

    @Test
    fun `test mindmap toString`() = runBlocking {
        val client = FreeplaneClient.create()
        val mindMap = MindMap(client, "map-123", "node-456")
        
        val str = mindMap.toString()
        assertTrue(str.contains("map-123"))
        assertTrue(str.contains("node-456"))
        
        runCatching { client.close() }
    }
}
