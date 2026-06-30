package org.freeplane.grpc

import io.grpc.Status
import io.grpc.Status.Code
import io.grpc.StatusException
import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.future.await
import kotlin.coroutines.CoroutineContext
import org.freeplane.plugin.grpc.*

/**
 * Main entry point for interacting with Freeplane via gRPC.
 *
 * Manages the gRPC channel/stub lifecycle and provides wrappers for all 27 RPC methods.
 * Supports coroutine-based async/await pattern.
 *
 * @property host The host to connect to (default: 127.0.0.1, overridable via FREEPLANE_HOST env var)
 * @property port The port to connect to (default: 50051, overridable via FREEPLANE_PORT env var)
 */
class FreeplaneClient private constructor(
    private val host: String,
    private val port: Int
) : AutoCloseable {

    private var channel: ManagedChannel? = null
    private var stub: FreeplaneGrpc.FreeplaneBlockingStub? = null
    private var isConnected = false

    init {
        // Apply environment variable overrides
        val envHost = System.getenv("FREEPLANE_HOST")
        val envPort = System.getenv("FREEPLANE_PORT")
        
        val finalHost = if (!envHost.isNullOrBlank()) envHost else host
        val finalPort = if (!envPort.isNullOrBlank()) {
            envPort.toIntOrNull() ?: port
        } else port

        this.channel = ManagedChannelBuilder.forAddress(
            finalHost.ifBlank { "127.0.0.1" },
            finalPort.takeIf { it != 0 } ?: 50051
        ).usePlaintext().build()
    }

    /**
     * Opens a gRPC channel to the server.
     */
    suspend fun connect() {
        if (isConnected) return
        
        channel?.let { ch ->
            stub = FreeplaneGrpc.newBlockingStub(ch)
            isConnected = true
        }
    }

    /**
     * Closes the gRPC connection.
     */
    override fun close() {
        channel?.let { ch ->
            ch.shutdown()
            ch.awaitTermination(5, java.util.concurrent.TimeUnit.SECONDS)
            channel = null
            stub = null
            isConnected = false
        }
    }

    /**
     * Internal helper that wraps a gRPC call and maps errors to domain errors.
     */
    private suspend fun <T : Any> call(
        context: CoroutineContext,
        fn: suspend () -> T
    ): T {
        return try {
            withContext(context) { fn() }
        } catch (e: StatusException) {
            throw statusToError(e.status)
        } catch (e: Exception) {
            if (isConnectionError(e)) {
                throw FreeplaneConnectionError("gRPC call failed: ${e.message}")
            }
            throw FreeplaneOperationError("gRPC call failed: ${e.message}")
        }
    }

    /**
     * Checks if the response has success=false and throws an appropriate error.
     */
    private fun checkSuccess(response: Any) {
        when (response) {
            is DeleteChildResponse -> if (!response.success) throw FreeplaneOperationError("operation failed")
            is NodeAttributeAddResponse -> if (!response.success) throw FreeplaneOperationError("operation failed")
            is NodeLinkSetResponse -> if (!response.success) throw FreeplaneOperationError("operation failed")
            is NodeDetailsSetResponse -> if (!response.success) throw FreeplaneOperationError("operation failed")
            is NodeNoteSetResponse -> if (!response.success) throw FreeplaneOperationError("operation failed")
            is NodeTagSetResponse -> if (!response.success) throw FreeplaneOperationError("operation failed")
            is NodeTagAddResponse -> if (!response.success) throw FreeplaneOperationError("operation failed")
            is NodeConnectResponse -> if (!response.success) throw FreeplaneOperationError("operation failed")
            is NodeAddIconResponse -> if (!response.success) throw FreeplaneOperationError("operation failed")
            is NodeColorSetResponse -> if (!response.success) throw FreeplaneOperationError("operation failed")
            is NodeBackgroundColorSetResponse -> if (!response.success) throw FreeplaneOperationError("operation failed")
            is StatusInfoSetResponse -> if (!response.success) throw FreeplaneOperationError("operation failed")
            is TextFSMResponse -> if (!response.success) throw FreeplaneOperationError("operation failed")
            is MindMapFromJSONResponse -> if (!response.success) throw FreeplaneOperationError("operation failed")
            is OpenMapResponse -> if (!response.success) throw FreeplaneOperationError("operation failed")
            is FocusNodeResponse -> if (!response.success) throw FreeplaneOperationError("operation failed")
            is SetNodeTextResponse -> if (!response.success) {
                val msg = response.errorMessage.takeIf { it.isNotBlank() } ?: "operation failed"
                throw FreeplaneOperationError(msg)
            }
            is MoveNodeResponse -> if (!response.success) {
                val msg = response.errorMessage.takeIf { it.isNotBlank() } ?: "operation failed"
                throw FreeplaneOperationError(msg)
            }
        }
    }

    // ==================== 27 RPC Wrappers ====================

    /**
     * Creates a child node under the specified parent.
     */
    suspend fun createChild(
        name: String,
        parentNodeId: String,
        context: CoroutineContext = Dispatchers.Default
    ): CreateChildResponse = call(context) {
        stub!!.createChild(
            CreateChildRequest.newBuilder()
                .setName(name)
                .setParentNodeId(parentNodeId)
                .build()
        )
    }

    /**
     * Deletes a node by its ID.
     */
    suspend fun deleteChild(
        nodeId: String,
        context: CoroutineContext = Dispatchers.Default
    ): DeleteChildResponse = call(context) {
        val resp = stub!!.deleteChild(
            DeleteChildRequest.newBuilder().setNodeId(nodeId).build()
        )
        checkSuccess(resp)
        resp
    }

    /**
     * Adds an attribute to a node.
     */
    suspend fun nodeAttributeAdd(
        nodeId: String,
        attributeName: String,
        attributeValue: String,
        context: CoroutineContext = Dispatchers.Default
    ): NodeAttributeAddResponse = call(context) {
        val resp = stub!!.nodeAttributeAdd(
            NodeAttributeAddRequest.newBuilder()
                .setNodeId(nodeId)
                .setAttributeName(attributeName)
                .setAttributeValue(attributeValue)
                .build()
        )
        checkSuccess(resp)
        resp
    }

    /**
     * Sets a link on a node.
     */
    suspend fun nodeLinkSet(
        nodeId: String,
        link: String,
        context: CoroutineContext = Dispatchers.Default
    ): NodeLinkSetResponse = call(context) {
        val resp = stub!!.nodeLinkSet(
            NodeLinkSetRequest.newBuilder()
                .setNodeId(nodeId)
                .setLink(link)
                .build()
        )
        checkSuccess(resp)
        resp
    }

    /**
     * Sets the details (body) of a node.
     */
    suspend fun nodeDetailsSet(
        nodeId: String,
        details: String,
        context: CoroutineContext = Dispatchers.Default
    ): NodeDetailsSetResponse = call(context) {
        val resp = stub!!.nodeDetailsSet(
            NodeDetailsSetRequest.newBuilder()
                .setNodeId(nodeId)
                .setDetails(details)
                .build()
        )
        checkSuccess(resp)
        resp
    }

    /**
     * Sets the note of a node.
     */
    suspend fun nodeNoteSet(
        nodeId: String,
        note: String,
        context: CoroutineContext = Dispatchers.Default
    ): NodeNoteSetResponse = call(context) {
        val resp = stub!!.nodeNoteSet(
            NodeNoteSetRequest.newBuilder()
                .setNodeId(nodeId)
                .setNote(note)
                .build()
        )
        checkSuccess(resp)
        resp
    }

    /**
     * Sets tags on a node (replaces existing tags).
     */
    suspend fun nodeTagSet(
        nodeId: String,
        tags: List<String>,
        context: CoroutineContext = Dispatchers.Default
    ): NodeTagSetResponse = call(context) {
        val resp = stub!!.nodeTagSet(
            NodeTagSetRequest.newBuilder()
                .setNodeId(nodeId)
                .addAllTags(tags)
                .build()
        )
        checkSuccess(resp)
        resp
    }

    /**
     * Adds tags to a node (appends to existing tags).
     */
    suspend fun nodeTagAdd(
        nodeId: String,
        tags: List<String>,
        context: CoroutineContext = Dispatchers.Default
    ): NodeTagAddResponse = call(context) {
        val resp = stub!!.nodeTagAdd(
            NodeTagAddRequest.newBuilder()
                .setNodeId(nodeId)
                .addAllTags(tags)
                .build()
        )
        checkSuccess(resp)
        resp
    }

    /**
     * Connects two nodes with a relationship.
     */
    suspend fun nodeConnect(
        sourceNodeId: String,
        targetNodeId: String,
        relationship: String,
        context: CoroutineContext = Dispatchers.Default
    ): NodeConnectResponse = call(context) {
        val resp = stub!!.nodeConnect(
            NodeConnectRequest.newBuilder()
                .setSourceNodeId(sourceNodeId)
                .setTargetNodeId(targetNodeId)
                .setRelationship(relationship)
                .build()
        )
        checkSuccess(resp)
        resp
    }

    /**
     * Adds an icon to a node.
     */
    suspend fun nodeAddIcon(
        nodeId: String,
        iconName: String,
        context: CoroutineContext = Dispatchers.Default
    ): NodeAddIconResponse = call(context) {
        val resp = stub!!.nodeAddIcon(
            NodeAddIconRequest.newBuilder()
                .setNodeId(nodeId)
                .setIconName(iconName)
                .build()
        )
        checkSuccess(resp)
        resp
    }

    /**
     * Executes Groovy code on the Freeplane server.
     */
    suspend fun groovy(
        groovyCode: String,
        context: CoroutineContext = Dispatchers.Default
    ): GroovyResponse = call(context) {
        stub!!.groovy(
            GroovyRequest.newBuilder().setGroovyCode(groovyCode).build()
        )
    }

    /**
     * Sets the text color of a node.
     */
    suspend fun nodeColorSet(
        nodeId: String,
        red: Int,
        green: Int,
        blue: Int,
        alpha: Int,
        context: CoroutineContext = Dispatchers.Default
    ): NodeColorSetResponse = call(context) {
        val resp = stub!!.nodeColorSet(
            NodeColorSetRequest.newBuilder()
                .setNodeId(nodeId)
                .setRed(red)
                .setGreen(green)
                .setBlue(blue)
                .setAlpha(alpha)
                .build()
        )
        checkSuccess(resp)
        resp
    }

    /**
     * Sets the background color of a node.
     */
    suspend fun nodeBackgroundColorSet(
        nodeId: String,
        red: Int,
        green: Int,
        blue: Int,
        alpha: Int,
        context: CoroutineContext = Dispatchers.Default
    ): NodeBackgroundColorSetResponse = call(context) {
        val resp = stub!!.nodeBackgroundColorSet(
            NodeBackgroundColorSetRequest.newBuilder()
                .setNodeId(nodeId)
                .setRed(red)
                .setGreen(green)
                .setBlue(blue)
                .setAlpha(alpha)
                .build()
        )
        checkSuccess(resp)
        resp
    }

    /**
     * Sets the status bar info in Freeplane.
     */
    suspend fun statusInfoSet(
        statusInfo: String,
        context: CoroutineContext = Dispatchers.Default
    ): StatusInfoSetResponse = call(context) {
        val resp = stub!!.statusInfoSet(
            StatusInfoSetRequest.newBuilder().setStatusInfo(statusInfo).build()
        )
        checkSuccess(resp)
        resp
    }

    /**
     * Processes JSON data through a TextFSM template.
     */
    suspend fun textFSM(
        json: String,
        context: CoroutineContext = Dispatchers.Default
    ): TextFSMResponse = call(context) {
        val resp = stub!!.textFSM(
            TextFSMRequest.newBuilder().setJson(json).build()
        )
        checkSuccess(resp)
        resp
    }

    /**
     * Imports a mind map from JSON.
     */
    suspend fun mindMapFromJSON(
        json: String,
        context: CoroutineContext = Dispatchers.Default
    ): MindMapFromJSONResponse = call(context) {
        val resp = stub!!.mindMapFromJSON(
            MindMapFromJSONRequest.newBuilder().setJson(json).build()
        )
        checkSuccess(resp)
        resp
    }

    /**
     * Exports the current mind map to JSON.
     */
    suspend fun mindMapToJSON(
        context: CoroutineContext = Dispatchers.Default
    ): MindMapToJSONResponse = call(context) {
        stub!!.mindMapToJSON(MindMapToJSONRequest.getDefaultInstance())
    }

    /**
     * Gets the current node.
     */
    suspend fun getCurrentNode(
        context: CoroutineContext = Dispatchers.Default
    ): GetCurrentNodeResponse = call(context) {
        stub!!.getCurrentNode(GetCurrentNodeRequest.getDefaultInstance())
    }

    /**
     * Opens a mind map file.
     */
    suspend fun openMap(
        filePath: String,
        context: CoroutineContext = Dispatchers.Default
    ): OpenMapResponse = call(context) {
        val resp = stub!!.openMap(
            OpenMapRequest.newBuilder().setFilePath(filePath).build()
        )
        checkSuccess(resp)
        resp
    }

    /**
     * Focuses (selects) a node.
     */
    suspend fun focusNode(
        nodeId: String,
        context: CoroutineContext = Dispatchers.Default
    ): FocusNodeResponse = call(context) {
        val resp = stub!!.focusNode(
            FocusNodeRequest.newBuilder().setNodeId(nodeId).build()
        )
        checkSuccess(resp)
        resp
    }

    /**
     * Gets the text of a node.
     */
    suspend fun getNodeText(
        nodeId: String,
        context: CoroutineContext = Dispatchers.Default
    ): GetNodeTextResponse = call(context) {
        stub!!.getNodeText(
            GetNodeTextRequest.newBuilder().setNodeId(nodeId).build()
        )
    }

    /**
     * Gets the parent of a node.
     */
    suspend fun getParentNode(
        nodeId: String,
        context: CoroutineContext = Dispatchers.Default
    ): GetParentNodeResponse = call(context) {
        stub!!.getParentNode(
            GetParentNodeRequest.newBuilder().setNodeId(nodeId).build()
        )
    }

    /**
     * Lists the children of a node.
     */
    suspend fun listChildNodes(
        nodeId: String,
        context: CoroutineContext = Dispatchers.Default
    ): ListChildNodesResponse = call(context) {
        stub!!.listChildNodes(
            ListChildNodesRequest.newBuilder().setNodeId(nodeId).build()
        )
    }

    /**
     * Gets the note of a node.
     */
    suspend fun getNodeNote(
        nodeId: String,
        context: CoroutineContext = Dispatchers.Default
    ): GetNodeNoteResponse = call(context) {
        stub!!.getNodeNote(
            GetNodeNoteRequest.newBuilder().setNodeId(nodeId).build()
        )
    }

    /**
     * Gets the link of a node.
     */
    suspend fun getNodeLink(
        nodeId: String,
        context: CoroutineContext = Dispatchers.Default
    ): GetNodeLinkResponse = call(context) {
        stub!!.getNodeLink(
            GetNodeLinkRequest.newBuilder().setNodeId(nodeId).build()
        )
    }

    /**
     * Sets the text of a node.
     */
    suspend fun setNodeText(
        nodeId: String,
        text: String,
        context: CoroutineContext = Dispatchers.Default
    ): SetNodeTextResponse = call(context) {
        val resp = stub!!.setNodeText(
            SetNodeTextRequest.newBuilder()
                .setNodeId(nodeId)
                .setText(text)
                .build()
        )
        checkSuccess(resp)
        resp
    }

    /**
     * Moves a node to a new parent.
     */
    suspend fun moveNode(
        nodeId: String,
        newParentNodeId: String,
        context: CoroutineContext = Dispatchers.Default
    ): MoveNodeResponse = call(context) {
        val resp = stub!!.moveNode(
            MoveNodeRequest.newBuilder()
                .setNodeId(nodeId)
                .setNewParentNodeId(newParentNodeId)
                .build()
        )
        checkSuccess(resp)
        resp
    }

    // ==================== High-Level Operations ====================

    /**
     * Gets the currently open mind map.
     */
    suspend fun currentMap(context: CoroutineContext = Dispatchers.Default): MindMap {
        val resp = getCurrentNode(context)
        if (!resp.success) {
            throw FreeplaneOperationError("no map currently open")
        }
        return MindMap(this, resp.mapId, resp.nodeId).withContext(context)
    }

    /**
     * Gets the current mind map rooted at the selected node.
     */
    suspend fun selectedMap(context: CoroutineContext = Dispatchers.Default): MindMap {
        return currentMap(context)
    }

    /**
     * Opens a mind map file and returns the current map.
     */
    suspend fun openMapAndGetCurrent(
        filePath: String,
        context: CoroutineContext = Dispatchers.Default
    ): MindMap {
        openMap(filePath, context)
        return currentMap(context)
    }

    /**
     * Exports the current mind map as JSON string.
     */
    suspend fun getMapToJSON(context: CoroutineContext = Dispatchers.Default): String {
        val resp = mindMapToJSON(context)
        return resp.json
    }

    /**
     * Imports a mind map from JSON data.
     */
    suspend fun mindMapFromJSONData(
        jsonData: String,
        context: CoroutineContext = Dispatchers.Default
    ) {
        mindMapFromJSON(jsonData, context)
    }

    /**
     * Executes Groovy code on the Freeplane server and returns the result.
     */
    suspend fun groovyCode(
        code: String,
        context: CoroutineContext = Dispatchers.Default
    ): String {
        val resp = groovy(code, context)
        return resp.result
    }

    /**
     * Focuses (selects) a node by its ID.
     */
    suspend fun focusNodeByID(
        nodeId: String,
        context: CoroutineContext = Dispatchers.Default
    ) {
        focusNode(nodeId, context)
    }

    /**
     * Sets the status bar info in Freeplane.
     */
    suspend fun setStatusInfoText(
        info: String,
        context: CoroutineContext = Dispatchers.Default
    ) {
        statusInfoSet(info, context)
    }

    // ==================== Error Mapping ====================

    private fun isConnectionError(err: Throwable): Boolean {
        val status = Status.fromThrowable(err)
        return status.code in connectionCodes
    }

    private fun statusToError(status: Status): Exception {
        return when (status.code) {
            Code.UNAVAILABLE, Code.DEADLINE_EXCEEDED, Code.RESOURCE_EXHAUSTED ->
                FreeplaneConnectionError("connection error: ${status.description}")
            Code.NOT_FOUND ->
                NodeNotFoundError("node not found: ${status.description}")
            else ->
                FreeplaneOperationError("operation failed: ${status.description}")
        }
    }

    companion object {
        private val connectionCodes = setOf(
            Code.UNAVAILABLE,
            Code.DEADLINE_EXCEEDED,
            Code.RESOURCE_EXHAUSTED
        )

        /**
         * Creates a new FreeplaneClient with the given host and port.
         * If host is empty, it defaults to "127.0.0.1".
         * If port is 0, it defaults to 50051.
         * Environment variables FREEPLANE_HOST and FREEPLANE_PORT can override defaults.
         */
        fun create(
            host: String = "",
            port: Int = 0
        ): FreeplaneClient {
            return FreeplaneClient(host, port)
        }
    }
}
