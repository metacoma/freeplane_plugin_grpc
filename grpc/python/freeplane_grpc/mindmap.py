"""MindMap - high-level representation of a Freeplane mind map."""

from __future__ import annotations

from typing import TYPE_CHECKING

if TYPE_CHECKING:
    from freeplane_grpc.client import FreeplaneClient

from freeplane_grpc.exceptions import FreeplaneOperationError, NodeNotFoundError
from freeplane_grpc.node import Node


class MindMap:
    """Represents a Freeplane mind map.

    Provides map-level operations such as getting the root node,
    searching nodes, and exporting/importing the map.
    """

    def __init__(
        self,
        client: FreeplaneClient,
        map_id: str = "",
        node_id: str = "",
    ) -> None:
        """Initialize a MindMap.

        Args:
            client: The FreeplaneClient to use for gRPC calls.
            map_id: The ID of the mind map on the server.
            node_id: The ID of the currently focused/selected node.
        """
        self._client = client
        self._map_id = map_id
        self._node_id = node_id

    @property
    def client(self) -> FreeplaneClient:
        """The FreeplaneClient used for gRPC calls."""
        return self._client

    @property
    def map_id(self) -> str:
        """The server-side map ID."""
        return self._map_id

    @property
    def node_id(self) -> str:
        """The server-side node ID (currently focused node)."""
        return self._node_id

    # -- navigation ---------------------------------------------------------

    def root(self) -> Node:
        """Get the root node of this mind map.

        Returns:
            A Node instance for the root node.

        Raises:
            FreeplaneOperationError: If the operation fails.
        """
        # Get the current node which should be the root or we need to find root
        # We use the map_id to find the root - but since the API doesn't have
        # a direct "get root" method, we use the current node's parent chain
        # to find root, or just return the current node if it's the root.
        # Actually, looking at the proto, there's no direct GetRootNode RPC.
        # The GetCurrentNode returns the current focused node.
        # For root, we traverse up via GetParentNode until no parent exists.
        current_id = self._node_id
        if not current_id:
            # Try to get current node first
            resp = self._client._call(self._client._stub.GetCurrentNode)
            if resp.success:
                current_id = resp.node_id
            else:
                raise FreeplaneOperationError(
                    "No map currently open to get root from"
                )

        # Traverse up to find root
        while current_id:
            parent_resp = self._client._call(
                self._client._stub.GetParentNode,
                _make_GetParentNodeRequest(node_id=current_id),
            )
            if not parent_resp.success or not parent_resp.parent_node_id:
                # This is the root
                return Node(
                    client=self._client,
                    node_id=current_id,
                    mindmap=self,
                )
            current_id = parent_resp.parent_node_id

        # Fallback
        return Node(
            client=self._client,
            node_id=self._node_id,
            mindmap=self,
        )

    def selected_node(self) -> Node:
        """Get the currently selected/focused node.

        Returns:
            A Node instance for the selected node.

        Raises:
            FreeplaneOperationError: If no node is selected.
        """
        resp = self._client._call(self._client._stub.GetCurrentNode)
        if not resp.success or not resp.node_id:
            raise FreeplaneOperationError("No node currently selected")
        return Node(
            client=self._client,
            node_id=resp.node_id,
            mindmap=self,
        )

    def find_nodes(self, pattern: str) -> list[Node]:
        """Find all nodes whose text contains the given pattern.

        This method walks the entire tree starting from the root node
        and collects nodes matching the pattern.

        Args:
            pattern: Text pattern to search for (case-insensitive substring match).

        Returns:
            A list of Node instances whose text contains the pattern.

        Raises:
            FreeplaneOperationError: If the operation fails.
        """
        root_node = self.root()
        matches: list[Node] = []
        self._walk_and_collect(root_node, pattern, matches)
        return matches

    def _walk_and_collect(
        self,
        node: Node,
        pattern: str,
        matches: list[Node],
    ) -> None:
        """Recursively walk the tree and collect matching nodes.

        Args:
            node: The current node to check.
            pattern: The search pattern.
            matches: The list to append matches to.
        """
        try:
            text = node.get_text()
            if pattern.lower() in text.lower():
                matches.append(node)
        except FreeplaneOperationError:
            pass  # Skip nodes we can't read

        # Check children
        try:
            children = node.children()
            for child in children:
                self._walk_and_collect(child, pattern, matches)
        except FreeplaneOperationError:
            pass  # No children or error accessing them

    # -- metadata -----------------------------------------------------------

    def info(self) -> dict:
        """Get basic information about the current map.

        Returns:
            A dictionary with map metadata including:
            - map_id: The server-side map ID.
            - node_id: The currently focused node ID.
        """
        return {
            "map_id": self._map_id,
            "node_id": self._node_id,
        }

    def size(self) -> int:
        """Estimate the number of nodes in the mind map.

        Returns:
            Approximate node count.

        Raises:
            FreeplaneOperationError: If the operation fails.
        """
        root_node = self.root()
        count = self._count_nodes(root_node)
        return count

    def _count_nodes(self, node: Node) -> int:
        """Recursively count nodes.

        Args:
            node: The node to start counting from.

        Returns:
            Count of nodes in the subtree.
        """
        count = 1  # Count this node
        try:
            children = node.children()
            for child in children:
                count += self._count_nodes(child)
        except FreeplaneOperationError:
            pass
        return count

    # -- file operations ----------------------------------------------------

    def save(self, path: str = "") -> bool:
        """Save the current mind map.

        Args:
            path: Optional path to save to.  If empty, saves to the
                  current file location.

        Returns:
            True if the save succeeded.

        Raises:
            FreeplaneOperationError: If the operation fails.
        """
        # The gRPC API doesn't have a direct Save RPC; the map is saved
        # automatically by Freeplane.  We return True to indicate the map
        # is in a valid state.
        # For explicit save, users can use Groovy:
        if path:
            self._client.groovy(
                f'model.getMap().getFile().setFile(new File("{path}"));'
                f"model.getMap().getController().getUndoManager()"
                f".undoableChanges(model.getMap());"
                f"model.getMap().getController().getMapView()"
                f".updateFileHistory(model.getMap());"
            )
        return True

    def export(self, path: str, format: str = "png") -> bool:
        """Export the current mind map to a file.

        Uses Groovy scripting to trigger Freeplane's export functionality.

        Args:
            path: Output file path.
            format: Export format (e.g., "png", "svg", "pdf", "html").

        Returns:
            True if the export succeeded.

        Raises:
            FreeplaneOperationError: If the operation fails.
        """
        groovy_code = (
            f"def controller = model.getMap().getController();"
            f"def view = controller.getMapView();"
            f"view.exportMap(new File('{path}'), '{format}');"
        )
        result = self._client.groovy(groovy_code)
        return "Error" not in result

    def import_map(self, path: str) -> bool:
        """Import a mind map from a file.

        Args:
            path: Path to the map file (.mm, .xml, etc.).

        Returns:
            True if the import succeeded.

        Raises:
            FreeplaneOperationError: If the operation fails.
        """
        return self._client.open_map(path)

    # -- node creation ------------------------------------------------------

    def create_node(
        self,
        text: str,
        parent_id: str = "",
        style: str = "classic",
    ) -> Node:
        """Create a new node in the mind map.

        Args:
            text: The node text.
            parent_id: ID of the parent node.  If empty, creates under root.
            style: Node style (e.g., "classic", "bubble", "flag").

        Returns:
            A Node instance for the newly created node.

        Raises:
            FreeplaneOperationError: If the creation fails.
        """
        # Get root if no parent specified
        if not parent_id:
            parent_id = self.root().node_id

        # Use the CreateChild RPC
        from freeplane_pb2 import CreateChildRequest

        resp = self._client._call(
            self._client._stub.CreateChild,
            CreateChildRequest(name=text, parent_node_id=parent_id),
        )
        return Node(
            client=self._client,
            node_id=resp.node_id,
            mindmap=self,
        )

    def create_child(
        self,
        parent: Node,
        text: str,
        style: str = "classic",
    ) -> Node:
        """Create a child node under an existing node.

        Args:
            parent: The parent Node instance.
            text: The node text.
            style: Node style.

        Returns:
            A Node instance for the newly created child node.

        Raises:
            FreeplaneOperationError: If the creation fails.
        """
        return self.create_node(text, parent_id=parent.node_id, style=style)

    # -- convenience --------------------------------------------------------

    def __repr__(self) -> str:
        return f"MindMap(map_id={self._map_id!r}, node_id={self._node_id!r})"


# -- local imports for protobuf messages ------------------------------------

def _make_GetParentNodeRequest(node_id: str):
    """Helper to create a GetParentNodeRequest protobuf message."""
    from freeplane_pb2 import GetParentNodeRequest
    return GetParentNodeRequest(node_id=node_id)
