package org.freeplane.grpc

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext

/**
 * Represents a Freeplane mind map.
 *
 * Provides navigation (root, selected_node), search (find_nodes),
 * metadata (info, size), file operations (save, export, import),
 * and node creation.
 */
class MindMap internal constructor(
    private val client: FreeplaneClient,
    private val mapID: String,
    private val nodeID: String
) {

    private var ctx: CoroutineContext = Dispatchers.Default

    /**
     * Returns a copy of the MindMap with the provided coroutine context.
     */
    fun withContext(context: CoroutineContext): MindMap {
        return MindMap(client, mapID, nodeID).also { it.ctx = context }
    }

    // ==================== Navigation ====================

    /**
     * Returns the root node of the mind map.
     * First tries the Groovy script "return root.nodeId". If that returns empty,
     * falls back to traversing up from the currently selected node.
     */
    suspend fun root(): Node {
        // First, try the Groovy script approach
        val resp = client.groovy("return root.nodeId", ctx)
        if (resp.result.isNotBlank()) {
            return Node(client, resp.result, this)
        }

        // Fallback: traverse up from the currently selected node to find the root.
        var currentID = nodeID
        while (true) {
            val parentResp = client.getParentNode(currentID, ctx)
            if (parentResp.parentNodeId.isEmpty()) {
                // This is the root node (no parent)
                return Node(client, currentID, this)
            }
            currentID = parentResp.parentNodeId
        }
    }

    /**
     * Returns the currently selected/focused node.
     */
    suspend fun selectedNode(): Node {
        val resp = client.getCurrentNode(ctx)
        if (!resp.success) {
            throw MindMapError("no node currently selected")
        }
        return Node(client, resp.nodeId, this)
    }

    /**
     * Searches for nodes matching a pattern in their text.
     */
    suspend fun findNodes(pattern: String): List<Node> {
        // Use Groovy to search for nodes matching the pattern
        val escapedPattern = pattern.replace("'", "\\'")
        val resp = client.groovy(
            "def result = []; node.eachNode { n -> if (n.text =~ /$escapedPattern/) result << n.nodeId }; return result",
            ctx
        )
        // Parse the result - for now return empty list
        // Full implementation would parse the Groovy result
        return emptyList()
    }

    // ==================== Metadata ====================

    /**
     * Returns mind map information.
     */
    fun info(): Pair<String, String> {
        return Pair(mapID, nodeID)
    }

    /**
     * Returns the number of nodes in the mind map.
     */
    suspend fun size(): Int {
        val resp = client.groovy("return node.allChildren(true).size() + 1", ctx)
        return resp.result.toIntOrNull() ?: 0
    }

    // ==================== File Operations ====================

    /**
     * Saves the mind map to the given path.
     */
    suspend fun save(path: String) {
        val escaped = path.replace("'", "\\'")
        client.groovy("def f = new File('$escaped'); node.save(f)", ctx)
    }

    /**
     * Exports the mind map to the given path in the specified format.
     */
    suspend fun export(path: String, format: String) {
        val escapedPath = path.replace("'", "\\'")
        val escapedFormat = format.replace("'", "\\'")
        client.groovy("node.export(new File('$escapedPath'), '$escapedFormat')", ctx)
    }

    /**
     * Imports a mind map from the given path.
     */
    suspend fun importMap(path: String) {
        client.openMap(path, ctx)
    }

    // ==================== Node Creation ====================

    /**
     * Creates a new node with the given text under the specified parent.
     */
    suspend fun createNode(text: String, parentID: String, style: String = ""): Node {
        val resp = client.createChild(text, parentID, ctx)
        val node = Node(client, resp.nodeId, this)
        if (style.isNotBlank()) {
            node.setStyle(style)
        }
        return node
    }

    /**
     * Creates a child node with the given text under the specified parent node.
     */
    suspend fun createChild(parent: Node, text: String): Node {
        return parent.addChild(text)
    }

    // ==================== Properties ====================

    /**
     * Returns the FreeplaneClient.
     */
    fun client(): FreeplaneClient = client

    /**
     * Returns the mind map ID.
     */
    fun mapID(): String = mapID

    /**
     * Returns the current node ID.
     */
    fun nodeID(): String = nodeID

    /**
     * Returns a string representation of the mind map.
     */
    override fun toString(): String {
        return "MindMap{id=$mapID, node=$nodeID}"
    }
}
