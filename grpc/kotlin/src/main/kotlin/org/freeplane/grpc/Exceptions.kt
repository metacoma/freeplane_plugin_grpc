package org.freeplane.grpc

/**
 * Base exception for all Freeplane gRPC errors.
 */
open class FreeplaneGrpcError(message: String, cause: Throwable? = null) : RuntimeException(message, cause) {
    override val message: String get() = super.message ?: ""
}

/**
 * Indicates a failure to connect or communicate with the server.
 * Covers gRPC transport failures (UNAVAILABLE, DEADLINE_EXCEEDED, RESOURCE_EXHAUSTED).
 */
class FreeplaneConnectionError(message: String, cause: Throwable? = null) : 
    FreeplaneGrpcError(message, cause)

/**
 * Indicates a failure reported by the server during an operation.
 * Covers server-reported failures (success=false).
 */
open class FreeplaneOperationError(message: String, cause: Throwable? = null) : 
    FreeplaneGrpcError(message, cause)

/**
 * Indicates that a requested node was not found.
 */
class NodeNotFoundError(message: String, cause: Throwable? = null) : 
    FreeplaneOperationError(message, cause)

/**
 * Indicates a failure at the mind map level.
 */
class MindMapError(message: String, cause: Throwable? = null) : 
    FreeplaneOperationError(message, cause)
