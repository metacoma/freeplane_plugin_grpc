"""FreeplaneClient - the main entry point for interacting with Freeplane via gRPC."""

from __future__ import annotations

import grpc

from typing import TYPE_CHECKING

from freeplane_grpc._stub import FreeplaneStub
from freeplane_grpc.exceptions import FreeplaneConnectionError, FreeplaneOperationError

if TYPE_CHECKING:
    from freeplane_grpc.mindmap import MindMap


class FreeplaneClient:
    """Client for interacting with a Freeplane gRPC server.

    Provides a high-level interface to the Freeplane mind-map application
    through its gRPC plugin.  Supports context-manager usage for automatic
    resource cleanup.

    Typical usage::

        with FreeplaneClient(host="127.0.0.1", port=50051) as client:
            mindmap = client.current_map()
            root = mindmap.root()
            print(root.get_text())
    """

    def __init__(self, host: str = "127.0.0.1", port: int = 50051) -> None:
        """Initialize the client.

        Args:
            host: Hostname or IP address of the Freeplane gRPC server.
            port: Port of the Freeplane gRPC server.
        """
        self._host = host
        self._port = port
        self._channel: grpc.Channel | None = None
        self._grpc_stub: FreeplaneStub | None = None

    # -- public properties --------------------------------------------------

    @property
    def host(self) -> str:
        """Server hostname."""
        return self._host

    @property
    def port(self) -> int:
        """Server port."""
        return self._port

    # -- lifecycle ----------------------------------------------------------

    def __enter__(self) -> FreeplaneClient:
        """Open the gRPC channel."""
        self.connect()
        return self

    def __exit__(self, *args: object) -> None:
        """Close the gRPC channel."""
        self.close()

    def connect(self) -> None:
        """Open (or re-open) the gRPC channel to the server.

        Raises:
            FreeplaneConnectionError: If the channel cannot be created.
        """
        try:
            self._channel = grpc.insecure_channel(f"{self._host}:{self._port}")
            # Verify the channel is usable
            grpc.channel_ready_future(self._channel).result(timeout=5)
        except grpc.FutureTimeoutError:
            raise FreeplaneConnectionError(
                f"Failed to connect to Freeplane gRPC server at "
                f"{self._host}:{self._port} (timeout)"
            )
        except grpc.RpcError as exc:
            raise FreeplaneConnectionError(
                f"Failed to connect to Freeplane gRPC server at "
                f"{self._host}:{self._port}: {exc}"
            )
        except Exception as exc:
            raise FreeplaneConnectionError(
                f"Failed to connect to Freeplane gRPC server at "
                f"{self._host}:{self._port}: {exc}"
            )

    def close(self) -> None:
        """Close the gRPC channel if it is open."""
        if self._channel is not None:
            try:
                self._channel.close()
            except Exception:
                pass
            self._channel = None
        self._grpc_stub = None

    # -- stub access --------------------------------------------------------

    @property
    def _stub(self) -> FreeplaneStub:
        """Lazily initialize the gRPC stub."""
        if self._grpc_stub is None:
            if self._channel is None:
                raise FreeplaneConnectionError(
                    "No active channel. Call connect() or use as a context manager."
                )
            self._grpc_stub = FreeplaneStub(self._channel)
        return self._grpc_stub

    # -- helper: call a unary-unary RPC and check success --------------------

    def _call(self, method, *args, **kwargs):
        """Call a gRPC method and convert failures to domain exceptions.

        Args:
            method: The gRPC stub method to invoke.
            *args: Positional arguments for the RPC.
            **kwargs: Keyword arguments for the RPC.

        Returns:
            The deserialized response message.

        Raises:
            FreeplaneOperationError: If the server reports success=False.
            FreeplaneConnectionError: If a connection error occurs.
        """
        try:
            response = method(*args, **kwargs)
        except grpc.RpcError as exc:
            code = exc.code()
            if code in (grpc.StatusCode.UNAVAILABLE, grpc.StatusCode.DEADLINE_EXCEEDED,
                         grpc.StatusCode.RESOURCE_EXHAUSTED):
                raise FreeplaneConnectionError(
                    f"gRPC call failed: {exc.details()}"
                ) from exc
            raise FreeplaneOperationError(
                f"gRPC call failed ({code}): {exc.details()}"
            ) from exc
        except Exception as exc:
            raise FreeplaneConnectionError(
                f"gRPC call failed: {exc}"
            ) from exc

        # Check the 'success' field common to most responses
        if hasattr(response, "success") and not response.success:
            error_msg = getattr(response, "error_message", "") or ""
            raise FreeplaneOperationError(
                f"Operation failed: {error_msg}" if error_msg else "Operation failed"
            )
        return response

    # -- high-level operations ----------------------------------------------

    def current_map(self) -> MindMap:
        """Get the currently open / active mind map.

        Returns:
            A MindMap instance bound to the current server state.

        Raises:
            FreeplaneConnectionError: If the connection fails.
            FreeplaneOperationError: If the server reports failure.
        """
        resp = self._call(self._stub.GetCurrentNode)
        if not resp.success:
            raise FreeplaneOperationError(
                resp.error_message if resp.error_message else "No map currently open"
            )
        return MindMap(client=self, map_id=resp.map_id, node_id=resp.node_id)

    def selected_map(self) -> MindMap:
        """Get the current mind map as a context rooted at the selected node.

        Convenience wrapper around current_map() that returns a MindMap
        with the currently focused node set.

        Returns:
            A MindMap instance rooted at the selected node.

        Raises:
            FreeplaneConnectionError: If the connection fails.
            FreeplaneOperationError: If the server reports failure.
        """
        return self.current_map()

    def open_map(self, file_path: str) -> MindMap:
        """Open a mind map file on the Freeplane server.

        Args:
            file_path: Path to the .mm file to open.

        Returns:
            A MindMap instance for the opened map.

        Raises:
            FreeplaneConnectionError: If the connection fails.
            FreeplaneOperationError: If the server reports failure.
        """
        from freeplane_pb2 import OpenMapRequest
        resp = self._call(self._stub.OpenMap, OpenMapRequest(file_path=file_path))
        if not resp.success:
            raise FreeplaneOperationError(
                f"Failed to open map: {file_path}"
            )
        return self.current_map()

    def get_map_to_json(self) -> str:
        """Export the current mind map as JSON.

        Returns:
            JSON string representation of the current mind map.

        Raises:
            FreeplaneConnectionError: If the connection fails.
            FreeplaneOperationError: If the server reports failure.
        """
        from freeplane_pb2 import MindMapToJSONRequest
        resp = self._call(self._stub.MindMapToJSON, MindMapToJSONRequest())
        return resp.json

    def mind_map_from_json(self, json_data: str) -> bool:
        """Import a mind map from JSON data.

        Args:
            json_data: JSON string representing a mind map.

        Returns:
            True if the import succeeded.

        Raises:
            FreeplaneConnectionError: If the connection fails.
            FreeplaneOperationError: If the server reports failure.
        """
        from freeplane_pb2 import MindMapFromJSONRequest
        resp = self._call(
            self._stub.MindMapFromJSON,
            MindMapFromJSONRequest(json=json_data),
        )
        return resp.success

    def groovy(self, code: str) -> str:
        """Execute Groovy code on the Freeplane server.

        Args:
            code: Groovy script to execute.

        Returns:
            The result output from the Groovy execution.

        Raises:
            FreeplaneConnectionError: If the connection fails.
            FreeplaneOperationError: If the server reports failure.
        """
        from freeplane_pb2 import GroovyRequest
        resp = self._call(self._stub.Groovy, GroovyRequest(groovy_code=code))
        return resp.result

    def focus_node(self, node_id: str) -> bool:
        """Focus (select) a node in the Freeplane UI.

        Args:
            node_id: ID of the node to focus.

        Returns:
            True if the operation succeeded.

        Raises:
            FreeplaneConnectionError: If the connection fails.
            FreeplaneOperationError: If the server reports failure.
        """
        from freeplane_pb2 import FocusNodeRequest
        resp = self._call(
            self._stub.FocusNode,
            FocusNodeRequest(node_id=node_id),
        )
        return resp.success

    def set_status_info(self, info: str) -> bool:
        """Set the status bar info in Freeplane.

        Args:
            info: Status text to display.

        Returns:
            True if the operation succeeded.

        Raises:
            FreeplaneConnectionError: If the connection fails.
            FreeplaneOperationError: If the server reports failure.
        """
        from freeplane_pb2 import StatusInfoSetRequest
        resp = self._call(
            self._stub.StatusInfoSet,
            StatusInfoSetRequest(statusInfo=info),
        )
        return resp.success
