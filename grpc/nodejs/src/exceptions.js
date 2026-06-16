/**
 * Custom exception types for the freeplane-grpc-client library.
 *
 * Exception hierarchy:
 *   FreeplaneGrpcError (base)
 *     ├── FreeplaneConnectionError  (network/gRPC transport failures)
 *     └── FreeplaneOperationError   (server-reported operation failures)
 *           ├── NodeNotFoundError   (requested node does not exist)
 *           └── MindMapError        (map-level operation failures)
 */

/**
 * Base exception for all Freeplane gRPC errors.
 * @extends Error
 */
class FreeplaneGrpcError extends Error {
  constructor(message) {
    super(message);
    this.name = 'FreeplaneGrpcError';
  }
}

/**
 * Raised when a connection to the Freeplane gRPC server fails.
 * @extends FreeplaneGrpcError
 */
class FreeplaneConnectionError extends FreeplaneGrpcError {
  constructor(message) {
    super(message);
    this.name = 'FreeplaneConnectionError';
  }
}

/**
 * Raised when a gRPC operation fails (server reported failure).
 * @extends FreeplaneGrpcError
 */
class FreeplaneOperationError extends FreeplaneGrpcError {
  constructor(message) {
    super(message);
    this.name = 'FreeplaneOperationError';
  }
}

/**
 * Raised when a requested node is not found.
 * @extends FreeplaneOperationError
 */
class NodeNotFoundError extends FreeplaneOperationError {
  constructor(message) {
    super(message);
    this.name = 'NodeNotFoundError';
  }
}

/**
 * Raised when a map-level operation fails.
 * @extends FreeplaneOperationError
 */
class MindMapError extends FreeplaneOperationError {
  constructor(message) {
    super(message);
    this.name = 'MindMapError';
  }
}

module.exports = {
  FreeplaneGrpcError,
  FreeplaneConnectionError,
  FreeplaneOperationError,
  NodeNotFoundError,
  MindMapError,
};
