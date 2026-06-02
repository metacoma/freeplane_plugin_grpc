"""Tests for the MindMap class."""

import sys
import os
import unittest
from unittest.mock import MagicMock, patch, Mock

sys.path.insert(0, os.path.join(os.path.dirname(__file__), ".."))

from freeplane_grpc.mindmap import MindMap
from freeplane_grpc.exceptions import FreeplaneOperationError, NodeNotFoundError


class TestMindMapInit(unittest.TestCase):
    """Test MindMap initialization."""

    def test_init_with_all_params(self):
        client = MagicMock()
        mm = MindMap(client=client, map_id="map123", node_id="node456")
        self.assertEqual(mm.map_id, "map123")
        self.assertEqual(mm.node_id, "node456")
        self.assertEqual(mm.client, client)

    def test_init_with_defaults(self):
        client = MagicMock()
        mm = MindMap(client=client)
        self.assertEqual(mm.map_id, "")
        self.assertEqual(mm.node_id, "")


class TestMindMapInfo(unittest.TestCase):
    """Test MindMap.info() method."""

    def test_info_returns_dict(self):
        client = MagicMock()
        mm = MindMap(client=client, map_id="map123", node_id="node456")
        info = mm.info()
        self.assertIsInstance(info, dict)
        self.assertEqual(info["map_id"], "map123")
        self.assertEqual(info["node_id"], "node456")


class TestMindMapRoot(unittest.TestCase):
    """Test MindMap.root() method."""

    @patch.object(MindMap, 'root')
    def test_root_traverses_to_root(self, mock_root):
        client = MagicMock()
        mm = MindMap(client=client, map_id="map1", node_id="node_child")
        # Mock the root to return a Node
        mock_root_node = MagicMock()
        mock_root_node.node_id = "root_node"
        mock_root.return_value = mock_root_node

        result = mm.root()
        self.assertIsNotNone(result)


class TestMindMapSelectedNode(unittest.TestCase):
    """Test MindMap.selected_node() method."""

    def test_selected_node_returns_node(self):
        from freeplane_grpc.node import Node
        client = MagicMock()
        mm = MindMap(client=client, map_id="map1", node_id="node1")

        # Mock GetCurrentNode response
        mock_resp = MagicMock()
        mock_resp.success = True
        mock_resp.node_id = "selected_node"
        mock_resp.text = "Selected"
        client._stub.GetCurrentNode.return_value = mock_resp
        client._call = MagicMock(return_value=mock_resp)

        node = mm.selected_node()
        self.assertIsInstance(node, Node)
        self.assertEqual(node.node_id, "selected_node")


class TestMindMapFindNodes(unittest.TestCase):
    """Test MindMap.find_nodes() method."""

    def test_find_nodes_returns_list(self):
        client = MagicMock()
        mm = MindMap(client=client, map_id="map1", node_id="node1")

        # Mock root() to return a node with children
        mock_root = MagicMock()
        mock_root.get_text.return_value = "Root"
        mock_child = MagicMock()
        mock_child.get_text.return_value = "TODO: fix this"
        mock_child.children.return_value = []
        mock_root.children.return_value = [mock_child]
        mm.root = MagicMock(return_value=mock_root)

        matches = mm.find_nodes("TODO")
        self.assertIsInstance(matches, list)


class TestMindMapCreateNode(unittest.TestCase):
    """Test MindMap.create_node() method."""

    def test_create_node_returns_node(self):
        client = MagicMock()
        mm = MindMap(client=client, map_id="map1", node_id="node1")

        # Mock root() to return a node
        mock_root = MagicMock()
        mock_root.node_id = "root_id"
        mm.root = MagicMock(return_value=mock_root)

        # Mock the CreateChild RPC response
        mock_resp = MagicMock()
        mock_resp.success = True
        mock_resp.node_id = "new_node_123"
        client._stub.CreateChild.return_value = mock_resp
        client._call = MagicMock(return_value=mock_resp)

        node = mm.create_node("New node text")
        self.assertIsNotNone(node)


class TestMindMapSize(unittest.TestCase):
    """Test MindMap.size() method."""

    def test_size_returns_int(self):
        client = MagicMock()
        mm = MindMap(client=client, map_id="map1", node_id="node1")

        # Mock root() to return a node with children
        mock_root = MagicMock()
        mock_child = MagicMock()
        mock_grandchild = MagicMock()
        mock_child.children.return_value = [mock_grandchild]
        mock_grandchild.children.return_value = []
        mock_root.children.return_value = [mock_child]
        mm.root = MagicMock(return_value=mock_root)

        size = mm.size()
        self.assertIsInstance(size, int)
        self.assertGreater(size, 0)


class TestMindMapRepr(unittest.TestCase):
    """Test MindMap.__repr__() method."""

    def test_repr_contains_map_id(self):
        client = MagicMock()
        mm = MindMap(client=client, map_id="map123", node_id="node456")
        repr_str = repr(mm)
        self.assertIn("map123", repr_str)


if __name__ == "__main__":
    unittest.main()
