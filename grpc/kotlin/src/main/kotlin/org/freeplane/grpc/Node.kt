package org.freeplane.grpc

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext

/**
 * Represents a node in a Freeplane mind map.
 *
 * Provides operations for text, hierarchy, styling, notes, attributes, links,
 * tags, icons, state, and actions on the node.
 */
class Node internal constructor(
    private val client: FreeplaneClient,
    private val nodeID: String,
    private val mindMap: MindMap? = null
) {

    private var ctx: CoroutineContext = Dispatchers.Default

    /**
     * Returns a copy of the Node with the provided coroutine context.
     */
    fun withContext(context: CoroutineContext): Node {
        return Node(client, nodeID, mindMap).also { it.ctx = context }
    }

    // ==================== Text Operations ====================

    /**
     * Returns the text of the node.
     */
    suspend fun getText(): String {
        val resp = client.getNodeText(nodeID, ctx)
        return resp.text
    }

    /**
     * Sets the text of the node.
     */
    suspend fun setText(text: String) {
        client.setNodeText(nodeID, text, ctx)
    }

    // ==================== Hierarchy Operations ====================

    /**
     * Creates a child node with the given text and optional style.
     */
    suspend fun addChild(text: String, style: String = ""): Node {
        val resp = client.createChild(text, nodeID, ctx)
        return Node(client, resp.nodeId, mindMap)
    }

    /**
     * Returns the list of child nodes.
     */
    suspend fun children(): List<Node> {
        val resp = client.listChildNodes(nodeID, ctx)
        return resp.childrenList.map { child ->
            Node(client, child.nodeId, mindMap)
        }
    }

    /**
     * Returns the parent node.
     */
    suspend fun parent(): Node? {
        val resp = client.getParentNode(nodeID, ctx)
        if (resp.parentNodeId.isEmpty()) {
            throw NodeNotFoundError("node has no parent")
        }
        return Node(client, resp.parentNodeId, mindMap)
    }

    /**
     * Deletes the node.
     */
    suspend fun delete() {
        client.deleteChild(nodeID, ctx)
    }

    /**
     * Moves the node to a new parent.
     */
    suspend fun move(newParentID: String) {
        client.moveNode(nodeID, newParentID, ctx)
    }

    // ==================== Styling Operations ====================

    /**
     * Returns the style name of the node.
     */
    suspend fun getStyle(): String {
        val resp = client.groovy("return node.getStyle().toString()", ctx)
        return resp.result
    }

    /**
     * Sets the style name of the node.
     */
    suspend fun setStyle(style: String) {
        val escaped = style.replace("'", "\\'")
        client.groovy("node.getStyle().setStyle('$escaped')", ctx)
    }

    /**
     * Sets the text color of the node.
     */
    suspend fun setColor(red: Int, green: Int, blue: Int, alpha: Int = 255) {
        client.nodeColorSet(nodeID, red, green, blue, alpha, ctx)
    }

    /**
     * Sets the background color of the node.
     */
    suspend fun setBackgroundColor(red: Int, green: Int, blue: Int, alpha: Int = 255) {
        client.nodeBackgroundColorSet(nodeID, red, green, blue, alpha, ctx)
    }

    // ==================== Notes Operations ====================

    /**
     * Returns the note content of the node along with whether it has a note.
     */
    suspend fun getNotes(): Pair<String, Boolean> {
        val resp = client.getNodeNote(nodeID, ctx)
        return Pair(resp.note, resp.hasNote)
    }

    /**
     * Sets the note content of the node.
     */
    suspend fun setNotes(notes: String) {
        client.nodeNoteSet(nodeID, notes, ctx)
    }

    // ==================== Attributes Operations ====================

    /**
     * Sets a single attribute on the node.
     */
    suspend fun setAttribute(name: String, value: String) {
        client.nodeAttributeAdd(nodeID, name, value, ctx)
    }

    /**
     * Sets multiple attributes on the node.
     */
    suspend fun setAttributes(attrs: Map<String, String>) {
        attrs.forEach { (name, value) ->
            client.nodeAttributeAdd(nodeID, name, value, ctx)
        }
    }

    // ==================== Links Operations ====================

    /**
     * Returns the node link along with whether it has a link.
     */
    suspend fun getLinks(): Pair<String, Boolean> {
        val resp = client.getNodeLink(nodeID, ctx)
        return Pair(resp.link, resp.hasLink)
    }

    /**
     * Sets the node link.
     */
    suspend fun setLinks(link: String) {
        client.nodeLinkSet(nodeID, link, ctx)
    }

    // ==================== Tags Operations ====================

    /**
     * Sets tags on the node (replaces existing tags).
     */
    suspend fun setTags(tags: List<String>) {
        client.nodeTagSet(nodeID, tags, ctx)
    }

    /**
     * Adds tags to the node (appends to existing tags).
     */
    suspend fun addTags(tags: List<String>) {
        client.nodeTagAdd(nodeID, tags, ctx)
    }

    // ==================== Icons Operations ====================

    /**
     * Adds an icon to the node.
     */
    suspend fun addIcon(name: String) {
        client.nodeAddIcon(nodeID, name, ctx)
    }

    // ==================== State Operations ====================

    /**
     * Sets the folded state of the node.
     */
    suspend fun setFolded(folded: Boolean) {
        val cmd = if (folded) "node.performCollapse()" else "node.performExpand()"
        client.groovy(cmd, ctx)
    }

    // ==================== Action Operations ====================

    /**
     * Selects the node.
     */
    suspend fun select() {
        client.focusNodeByID(nodeID, ctx)
    }

    /**
     * Centers the node in the view.
     */
    suspend fun center() {
        client.groovy("node.centerInView()", ctx)
    }

    /**
     * Refreshes the node display.
     */
    suspend fun refresh() {
        client.groovy("node.refresh()", ctx)
    }

    // ==================== Properties ====================

    /**
     * Returns the FreeplaneClient.
     */
    fun client(): FreeplaneClient = client

    /**
     * Returns the node ID.
     */
    fun nodeID(): String = nodeID

    /**
     * Returns the parent MindMap.
     */
    fun mindMap(): MindMap? = mindMap

    /**
     * Returns a string representation of the node.
     */
    override fun toString(): String {
        return "Node{id=$nodeID}"
    }
}
