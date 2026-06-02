"""Node - high-level representation of a Freeplane mind map node."""

from __future__ import annotations

from typing import TYPE_CHECKING

from freeplane_grpc.exceptions import FreeplaneOperationError, NodeNotFoundError

if TYPE_CHECKING:
    from freeplane_grpc.client import FreeplaneClient
    from freeplane_grpc.mindmap import MindMap


class Node:
    """Represents a Freeplane mind map node.

    Provides node-level operations such as getting/setting text,
    managing children, styling, notes, attributes, links, and icons.
    """

    def __init__(
        self,
        client: FreeplaneClient,
        node_id: str,
        mindmap: MindMap | None = None,
    ) -> None:
        """Initialize a Node.

        Args:
            client: The FreeplaneClient to use for gRPC calls.
            node_id: The server-side node ID.
            mindmap: Optional parent MindMap reference.
        """
        self._client = client
        self._node_id = node_id
        self._mindmap = mindmap

    @property
    def client(self) -> FreeplaneClient:
        """The FreeplaneClient used for gRPC calls."""
        return self._client

    @property
    def node_id(self) -> str:
        """The server-side node ID."""
        return self._node_id

    @property
    def mindmap(self) -> MindMap | None:
        """The parent MindMap, if available."""
        return self._mindmap

    # -- text ---------------------------------------------------------------

    def get_text(self) -> str:
        """Get the text of this node.

        Returns:
            The node text.

        Raises:
            FreeplaneOperationError: If the operation fails.
        """
        from freeplane_pb2 import GetNodeTextRequest

        resp = self._client._call(
            self._client._stub.GetNodeText,
            GetNodeTextRequest(node_id=self._node_id),
        )
        return resp.text

    def set_text(self, text: str) -> None:
        """Set the text of this node.

        Args:
            text: The new node text.

        Raises:
            FreeplaneOperationError: If the operation fails.
        """
        from freeplane_pb2 import SetNodeTextRequest

        self._client._call(
            self._client._stub.SetNodeText,
            SetNodeTextRequest(node_id=self._node_id, text=text),
        )

    # -- hierarchy ----------------------------------------------------------

    def add_child(self, text: str, style: str = "classic") -> "Node":
        """Add a child node to this node.

        Args:
            text: The child node text.
            style: Node style (e.g., "classic", "bubble", "flag").

        Returns:
            A Node instance for the newly created child node.

        Raises:
            FreeplaneOperationError: If the creation fails.
        """
        from freeplane_pb2 import CreateChildRequest

        resp = self._client._call(
            self._client._stub.CreateChild,
            CreateChildRequest(name=text, parent_node_id=self._node_id),
        )
        child = Node(
            client=self._client,
            node_id=resp.node_id,
            mindmap=self._mindmap,
        )
        # Update the mindmap's node_id to point to the new child
        if self._mindmap is not None:
            self._mindmap._node_id = resp.node_id
        return child

    def children(self) -> list["Node"]:
        """Get the direct children of this node.

        Returns:
            A list of Node instances.

        Raises:
            FreeplaneOperationError: If the operation fails.
        """
        from freeplane_pb2 import ListChildNodesRequest

        resp = self._client._call(
            self._client._stub.ListChildNodes,
            ListChildNodesRequest(node_id=self._node_id),
        )
        return [
            Node(
                client=self._client,
                node_id=child.node_id,
                mindmap=self._mindmap,
            )
            for child in resp.children
        ]

    def parent(self) -> "Node":
        """Get the parent of this node.

        Returns:
            A Node instance for the parent node.

        Raises:
            NodeNotFoundError: If this node has no parent (is root).
            FreeplaneOperationError: If the operation fails.
        """
        from freeplane_pb2 import GetParentNodeRequest

        resp = self._client._call(
            self._client._stub.GetParentNode,
            GetParentNodeRequest(node_id=self._node_id),
        )
        if not resp.success or not resp.parent_node_id:
            raise NodeNotFoundError(
                f"Node {self._node_id} has no parent (is root)"
            )
        return Node(
            client=self._client,
            node_id=resp.parent_node_id,
            mindmap=self._mindmap,
        )

    def delete(self) -> bool:
        """Delete this node.

        Returns:
            True if the deletion succeeded.

        Raises:
            FreeplaneOperationError: If the deletion fails.
        """
        from freeplane_pb2 import DeleteChildRequest

        resp = self._client._call(
            self._client._stub.DeleteChild,
            DeleteChildRequest(node_id=self._node_id),
        )
        return resp.success

    def move(self, new_parent_id: str) -> bool:
        """Move this node under a new parent.

        Args:
            new_parent_id: ID of the new parent node.

        Returns:
            True if the move succeeded.

        Raises:
            FreeplaneOperationError: If the move fails.
        """
        from freeplane_pb2 import MoveNodeRequest

        resp = self._client._call(
            self._client._stub.MoveNode,
            MoveNodeRequest(
                node_id=self._node_id,
                new_parent_node_id=new_parent_id,
            ),
        )
        return resp.success

    # -- styling ------------------------------------------------------------

    def get_style(self) -> dict:
        """Get the style information for this node.

        Returns:
            A dictionary with style properties.

        Raises:
            FreeplaneOperationError: If the operation fails.
        """
        # The gRPC API doesn't have a direct GetNodeStyle RPC.
        # We use Groovy to retrieve style info.
        groovy_code = (
            f"def node = model.getNode('{self._node_id}');"
            f"def style = node.style;"
            f"def result = [:];"
            f"if (style != null) {{"
            f"  result = style.getProperties().collectEntries {{ k, v ->"
            f"    [k.toString(), v.toString()]"
            f"  }}"
            f"}}"
        )
        result = self._client.groovy(groovy_code)
        return {"style": result}

    def set_style(self, style: str) -> bool:
        """Set the style of this node.

        Args:
            style: Style name (e.g., "classic", "bubble", "flag", "diamond").

        Returns:
            True if the style was set successfully.

        Raises:
            FreeplaneOperationError: If the operation fails.
        """
        groovy_code = (
            f"def node = model.getNode('{self._node_id}');"
            f"node.style = model.getStyleLib().getStyle('{style}');"
        )
        result = self._client.groovy(groovy_code)
        return "Error" not in result

    def get_color(self) -> dict:
        """Get the foreground color of this node.

        Returns:
            A dictionary with 'red', 'green', 'blue', 'alpha' keys.

        Raises:
            FreeplaneOperationError: If the operation fails.
        """
        # Use Groovy to get the color
        groovy_code = (
            f"def node = model.getNode('{self._node_id}');"
            f"def color = node.getTextColor();"
            f"[red: color?.red, green: color?.green, "
            f" blue: color?.blue, alpha: color?.alpha]"
        )
        result = self._client.groovy(groovy_code)
        return {"color": result}

    def set_color(self, red: int, green: int, blue: int, alpha: int = 255) -> bool:
        """Set the foreground color of this node.

        Args:
            red: Red component (0-255).
            green: Green component (0-255).
            blue: Blue component (0-255).
            alpha: Alpha component (0-255, default 255).

        Returns:
            True if the color was set successfully.

        Raises:
            FreeplaneOperationError: If the operation fails.
        """
        from freeplane_pb2 import NodeColorSetRequest

        self._client._call(
            self._client._stub.NodeColorSet,
            NodeColorSetRequest(
                node_id=self._node_id,
                red=red,
                green=green,
                blue=blue,
                alpha=alpha,
            ),
        )
        return True

    def set_background_color(
        self,
        red: int,
        green: int,
        blue: int,
        alpha: int = 255,
    ) -> bool:
        """Set the background color of this node.

        Args:
            red: Red component (0-255).
            green: Green component (0-255).
            blue: Blue component (0-255).
            alpha: Alpha component (0-255, default 255).

        Returns:
            True if the color was set successfully.

        Raises:
            FreeplaneOperationError: If the operation fails.
        """
        from freeplane_pb2 import NodeBackgroundColorSetRequest

        self._client._call(
            self._client._stub.NodeBackgroundColorSet,
            NodeBackgroundColorSetRequest(
                node_id=self._node_id,
                red=red,
                green=green,
                blue=blue,
                alpha=alpha,
            ),
        )
        return True

    # -- notes --------------------------------------------------------------

    def get_notes(self) -> str:
        """Get the notes (HTML content) of this node.

        Returns:
            The node notes as a string.

        Raises:
            FreeplaneOperationError: If the operation fails.
        """
        from freeplane_pb2 import GetNodeNoteRequest

        resp = self._client._call(
            self._client._stub.GetNodeNote,
            GetNodeNoteRequest(node_id=self._node_id),
        )
        return resp.note if resp.has_note else ""

    def set_notes(self, notes: str) -> bool:
        """Set the notes (HTML content) of this node.

        Args:
            notes: The node notes content.

        Returns:
            True if the notes were set successfully.

        Raises:
            FreeplaneOperationError: If the operation fails.
        """
        from freeplane_pb2 import NodeNoteSetRequest

        resp = self._client._call(
            self._client._stub.NodeNoteSet,
            NodeNoteSetRequest(node_id=self._node_id, note=notes),
        )
        return resp.success

    # -- attributes ---------------------------------------------------------

    def get_attributes(self) -> dict:
        """Get the custom attributes of this node.

        Returns:
            A dictionary of attribute name-value pairs.

        Raises:
            FreeplaneOperationError: If the operation fails.
        """
        # Use Groovy to get all attributes
        groovy_code = (
            f"def node = model.getNode('{self._node_id}');"
            f"def attrs = node.getAttributes();"
            f"attrs.collectEntries {{ k, v -> [k.toString(), v.toString()] }}"
        )
        result = self._client.groovy(groovy_code)
        return {"attributes": result}

    def set_attribute(self, name: str, value: str) -> bool:
        """Set a custom attribute on this node.

        Args:
            name: Attribute name.
            value: Attribute value.

        Returns:
            True if the attribute was set successfully.

        Raises:
            FreeplaneOperationError: If the operation fails.
        """
        from freeplane_pb2 import NodeAttributeAddRequest

        resp = self._client._call(
            self._client._stub.NodeAttributeAdd,
            NodeAttributeAddRequest(
                node_id=self._node_id,
                attribute_name=name,
                attribute_value=value,
            ),
        )
        return resp.success

    def set_attributes(self, attrs: dict) -> bool:
        """Set multiple custom attributes on this node.

        Args:
            attrs: Dictionary of attribute name-value pairs.

        Returns:
            True if all attributes were set successfully.

        Raises:
            FreeplaneOperationError: If any attribute fails to set.
        """
        for name, value in attrs.items():
            if not self.set_attribute(name, str(value)):
                return False
        return True

    # -- links --------------------------------------------------------------

    def get_links(self) -> list[str]:
        """Get the links of this node.

        Returns:
            A list of link URLs/strings.

        Raises:
            FreeplaneOperationError: If the operation fails.
        """
        from freeplane_pb2 import GetNodeLinkRequest

        resp = self._client._call(
            self._client._stub.GetNodeLink,
            GetNodeLinkRequest(node_id=self._node_id),
        )
        return [resp.link] if resp.has_link else []

    def set_links(self, links: list[str]) -> bool:
        """Set the links of this node (replaces existing links).

        Args:
            links: List of link URLs/strings.

        Returns:
            True if the links were set successfully.

        Raises:
            FreeplaneOperationError: If the operation fails.
        """
        for link in links:
            from freeplane_pb2 import NodeLinkSetRequest

            self._client._call(
                self._client._stub.NodeLinkSet,
                NodeLinkSetRequest(node_id=self._node_id, link=link),
            )
        return True

    # -- tags ---------------------------------------------------------------

    def set_tags(self, tags: list[str]) -> bool:
        """Set the tags of this node (replaces existing tags).

        Args:
            tags: List of tag strings.

        Returns:
            True if the tags were set successfully.

        Raises:
            FreeplaneOperationError: If the operation fails.
        """
        from freeplane_pb2 import NodeTagSetRequest

        resp = self._client._call(
            self._client._stub.NodeTagSet,
            NodeTagSetRequest(node_id=self._node_id, tags=tags),
        )
        return resp.success

    def add_tags(self, tags: list[str]) -> bool:
        """Add tags to this node (does not remove existing tags).

        Args:
            tags: List of tag strings to add.

        Returns:
            True if the tags were added successfully.

        Raises:
            FreeplaneOperationError: If the operation fails.
        """
        from freeplane_pb2 import NodeTagAddRequest

        resp = self._client._call(
            self._client._stub.NodeTagAdd,
            NodeTagAddRequest(node_id=self._node_id, tags=tags),
        )
        return resp.success

    # -- icons --------------------------------------------------------------

    def get_icons(self) -> list[str]:
        """Get the icons of this node.

        Returns:
            A list of icon names.

        Raises:
            FreeplaneOperationError: If the operation fails.
        """
        # Use Groovy to get icons
        groovy_code = (
            f"def node = model.getNode('{self._node_id}');"
            f"def icons = node.getIconIds();"
            f"icons ? icons.toList() : []"
        )
        result = self._client.groovy(groovy_code)
        return {"icons": result}

    def add_icon(self, icon_name: str) -> bool:
        """Add an icon to this node.

        Args:
            icon_name: Name of the icon to add.

        Returns:
            True if the icon was added successfully.

        Raises:
            FreeplaneOperationError: If the operation fails.
        """
        from freeplane_pb2 import NodeAddIconRequest

        resp = self._client._call(
            self._client._stub.NodeAddIcon,
            NodeAddIconRequest(node_id=self._node_id, icon_name=icon_name),
        )
        return resp.success

    # -- state --------------------------------------------------------------

    def get_folded(self) -> bool:
        """Get the folded (collapsed) state of this node.

        Returns:
            True if the node is folded (collapsed).

        Raises:
            FreeplaneOperationError: If the operation fails.
        """
        # Use Groovy to get folded state
        groovy_code = (
            f"def node = model.getNode('{self._node_id}');"
            f"node.isFolded()"
        )
        result = self._client.groovy(groovy_code)
        return "true" in str(result).lower()

    def set_folded(self, folded: bool) -> bool:
        """Set the folded (collapsed) state of this node.

        Args:
            folded: True to fold (collapse), False to unfold (expand).

        Returns:
            True if the state was set successfully.

        Raises:
            FreeplaneOperationError: If the operation fails.
        """
        # Use Groovy to set folded state
        groovy_code = (
            f"def node = model.getNode('{self._node_id}');"
            f"if ({str(folded).lower()}) node.fold() else node.unfold();"
        )
        result = self._client.groovy(groovy_code)
        return "Error" not in str(result)

    # -- actions ------------------------------------------------------------

    def select(self) -> bool:
        """Select this node in the Freeplane UI.

        Returns:
            True if the operation succeeded.

        Raises:
            FreeplaneOperationError: If the operation fails.
        """
        return self._client.focus_node(self._node_id)

    def center(self) -> bool:
        """Center the view on this node.

        Returns:
            True if the operation succeeded.

        Raises:
            FreeplaneOperationError: If the operation fails.
        """
        return self._client.focus_node(self._node_id)

    # -- refresh ------------------------------------------------------------

    def refresh(self) -> None:
        """Reload this node's state from the server.

        This is useful if the node may have been modified by another client
        or by server-side operations.

        Raises:
            FreeplaneOperationError: If the operation fails.
        """
        # Reload by fetching the text
        self.get_text()

    # -- convenience --------------------------------------------------------

    def __repr__(self) -> str:
        try:
            text = self.get_text()[:50]
        except FreeplaneOperationError:
            text = "<unreadable>"
        return f"Node(id={self._node_id!r}, text={text!r})"

    def __eq__(self, other: object) -> bool:
        if not isinstance(other, Node):
            return False
        return self._node_id == other._node_id

    def __hash__(self) -> int:
        return hash(self._node_id)
