"""Custom exception types for the freeplane_grpc client library."""


class FreeplaneGrpcError(Exception):
    """Base exception for all Freeplane gRPC errors."""


class FreeplaneConnectionError(FreeplaneGrpcError):
    """Raised when a connection to the Freeplane gRPC server fails."""


class FreeplaneOperationError(FreeplaneGrpcError):
    """Raised when a gRPC operation fails (server reported failure)."""


class NodeNotFoundError(FreeplaneOperationError):
    """Raised when a requested node is not found."""


class MindMapError(FreeplaneOperationError):
    """Raised when a map-level operation fails."""
