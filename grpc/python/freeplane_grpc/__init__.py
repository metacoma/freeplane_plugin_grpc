"""freeplane_grpc - Python client library for the Freeplane gRPC plugin.

Provides high-level abstractions (FreeplaneClient, MindMap, Node) for
interacting with Freeplane mind maps through the gRPC plugin.

Example usage::

    from freeplane_grpc import FreeplaneClient

    with FreeplaneClient(host="127.0.0.1", port=50051) as client:
        mindmap = client.current_map()
        root = mindmap.root()
        print(root.get_text())
"""

from __future__ import annotations


def __getattr__(name: str):
    """Lazy import to avoid circular dependencies at module load time."""
    if name == "FreeplaneClient":
        from freeplane_grpc.client import FreeplaneClient
        return FreeplaneClient
    if name == "MindMap":
        from freeplane_grpc.mindmap import MindMap
        return MindMap
    if name == "Node":
        from freeplane_grpc.node import Node
        return Node
    if name == "FreeplaneGrpcError":
        from freeplane_grpc.exceptions import FreeplaneGrpcError
        return FreeplaneGrpcError
    if name == "FreeplaneConnectionError":
        from freeplane_grpc.exceptions import FreeplaneConnectionError
        return FreeplaneConnectionError
    if name == "FreeplaneOperationError":
        from freeplane_grpc.exceptions import FreeplaneOperationError
        return FreeplaneOperationError
    if name == "NodeNotFoundError":
        from freeplane_grpc.exceptions import NodeNotFoundError
        return NodeNotFoundError
    if name == "MindMapError":
        from freeplane_grpc.exceptions import MindMapError
        return MindMapError
    raise AttributeError(f"module {__name__!r} has no attribute {name!r}")


__all__ = [
    "FreeplaneClient",
    "MindMap",
    "Node",
    "FreeplaneGrpcError",
    "FreeplaneConnectionError",
    "FreeplaneOperationError",
    "NodeNotFoundError",
    "MindMapError",
]
