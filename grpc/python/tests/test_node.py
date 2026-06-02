"""Tests for the Node class."""

import sys
import os
import unittest
from unittest.mock import MagicMock, patch, Mock

sys.path.insert(0, os.path.join(os.path.dirname(__file__), ".."))

from freeplane_grpc.node import Node
from freeplane_grpc.exceptions import FreeplaneOperationError, NodeNotFoundError


class TestNodeInit(unittest.TestCase):
    """Test Node initialization."""

    def test_init_with_all_params(self):
        client = MagicMock()
        mindmap = MagicMock()
        node = Node(client=client, node_id="node123", mindmap=mindmap)
        self.assertEqual(node.node_id, "node123")
        self.assertEqual(node.client, client)
        self.assertEqual(node.mindmap, mindmap)

    def test_init_without_mindmap(self):
        client = MagicMock()
        node = Node(client=client, node_id="node123")
        self.assertIsNone(node.mindmap)


class TestNodeGetText(unittest.TestCase):
    """Test Node.get_text() method."""

    def test_get_text_returns_text(self):
        client = MagicMock()
        node = Node(client=client, node_id="node123")

        mock_resp = MagicMock()
        mock_resp.success = True
        mock_resp.text = "Hello World"
        client._call = MagicMock(return_value=mock_resp)

        text = node.get_text()
        self.assertEqual(text, "Hello World")
        client._call.assert_called_once()


class TestNodeSetText(unittest.TestCase):
    """Test Node.set_text() method."""

    def test_set_text_calls_rpc(self):
        client = MagicMock()
        node = Node(client=client, node_id="node123")

        mock_resp = MagicMock()
        mock_resp.success = True
        client._call = MagicMock(return_value=mock_resp)

        node.set_text("New text")
        client._call.assert_called_once()


class TestNodeAddChild(unittest.TestCase):
    """Test Node.add_child() method."""

    def test_add_child_returns_node(self):
        client = MagicMock()
        node = Node(client=client, node_id="node123")

        mock_resp = MagicMock()
        mock_resp.success = True
        mock_resp.node_id = "child_node_456"
        client._call = MagicMock(return_value=mock_resp)

        child = node.add_child("Child text")
        self.assertIsInstance(child, Node)
        self.assertEqual(child.node_id, "child_node_456")


class TestNodeChildren(unittest.TestCase):
    """Test Node.children() method."""

    def test_children_returns_list(self):
        client = MagicMock()
        node = Node(client=client, node_id="node123")

        mock_child1 = MagicMock()
        mock_child1.node_id = "child1"
        mock_child1.text = "Child 1"
        mock_child2 = MagicMock()
        mock_child2.node_id = "child2"
        mock_child2.text = "Child 2"

        mock_resp = MagicMock()
        mock_resp.success = True
        mock_resp.children = [mock_child1, mock_child2]
        client._call = MagicMock(return_value=mock_resp)

        children = node.children()
        self.assertIsInstance(children, list)
        self.assertEqual(len(children), 2)
        self.assertEqual(children[0].node_id, "child1")
        self.assertEqual(children[1].node_id, "child2")


class TestNodeParent(unittest.TestCase):
    """Test Node.parent() method."""

    def test_parent_returns_node(self):
        client = MagicMock()
        node = Node(client=client, node_id="node123")

        mock_resp = MagicMock()
        mock_resp.success = True
        mock_resp.parent_node_id = "parent_456"
        mock_resp.parent_node_text = "Parent Text"
        client._call = MagicMock(return_value=mock_resp)

        parent = node.parent()
        self.assertIsInstance(parent, Node)
        self.assertEqual(parent.node_id, "parent_456")

    def test_parent_raises_for_root(self):
        client = MagicMock()
        node = Node(client=client, node_id="root_node")

        mock_resp = MagicMock()
        mock_resp.success = True
        mock_resp.parent_node_id = ""
        client._call = MagicMock(return_value=mock_resp)

        with self.assertRaises(NodeNotFoundError):
            node.parent()


class TestNodeDelete(unittest.TestCase):
    """Test Node.delete() method."""

    def test_delete_returns_true_on_success(self):
        client = MagicMock()
        node = Node(client=client, node_id="node123")

        mock_resp = MagicMock()
        mock_resp.success = True
        client._call = MagicMock(return_value=mock_resp)

        result = node.delete()
        self.assertTrue(result)


class TestNodeMove(unittest.TestCase):
    """Test Node.move() method."""

    def test_move_returns_true_on_success(self):
        client = MagicMock()
        node = Node(client=client, node_id="node123")

        mock_resp = MagicMock()
        mock_resp.success = True
        client._call = MagicMock(return_value=mock_resp)

        result = node.move("new_parent_456")
        self.assertTrue(result)


class TestNodeColor(unittest.TestCase):
    """Test Node color methods."""

    def test_set_color_returns_true(self):
        client = MagicMock()
        node = Node(client=client, node_id="node123")

        mock_resp = MagicMock()
        mock_resp.success = True
        client._call = MagicMock(return_value=mock_resp)

        result = node.set_color(255, 0, 0)
        self.assertTrue(result)

    def test_set_background_color_returns_true(self):
        client = MagicMock()
        node = Node(client=client, node_id="node123")

        mock_resp = MagicMock()
        mock_resp.success = True
        client._call = MagicMock(return_value=mock_resp)

        result = node.set_background_color(255, 255, 200)
        self.assertTrue(result)


class TestNodeNotes(unittest.TestCase):
    """Test Node notes methods."""

    def test_get_notes_returns_empty_when_no_note(self):
        client = MagicMock()
        node = Node(client=client, node_id="node123")

        mock_resp = MagicMock()
        mock_resp.success = True
        mock_resp.has_note = False
        mock_resp.note = ""
        client._call = MagicMock(return_value=mock_resp)

        notes = node.get_notes()
        self.assertEqual(notes, "")

    def test_get_notes_returns_note_when_exists(self):
        client = MagicMock()
        node = Node(client=client, node_id="node123")

        mock_resp = MagicMock()
        mock_resp.success = True
        mock_resp.has_note = True
        mock_resp.note = "<html><body>Note content</body></html>"
        client._call = MagicMock(return_value=mock_resp)

        notes = node.get_notes()
        self.assertEqual(notes, "<html><body>Note content</body></html>")

    def test_set_notes_returns_true(self):
        client = MagicMock()
        node = Node(client=client, node_id="node123")

        mock_resp = MagicMock()
        mock_resp.success = True
        client._call = MagicMock(return_value=mock_resp)

        result = node.set_notes("New note content")
        self.assertTrue(result)


class TestNodeAttributes(unittest.TestCase):
    """Test Node attribute methods."""

    def test_set_attribute_returns_true(self):
        client = MagicMock()
        node = Node(client=client, node_id="node123")

        mock_resp = MagicMock()
        mock_resp.success = True
        client._call = MagicMock(return_value=mock_resp)

        result = node.set_attribute("key", "value")
        self.assertTrue(result)

    def test_set_attributes_returns_true(self):
        client = MagicMock()
        node = Node(client=client, node_id="node123")

        mock_resp = MagicMock()
        mock_resp.success = True
        client._call = MagicMock(return_value=mock_resp)

        result = node.set_attributes({"key1": "val1", "key2": "val2"})
        self.assertTrue(result)


class TestNodeLinks(unittest.TestCase):
    """Test Node link methods."""

    def test_get_links_returns_empty_when_no_link(self):
        client = MagicMock()
        node = Node(client=client, node_id="node123")

        mock_resp = MagicMock()
        mock_resp.success = True
        mock_resp.has_link = False
        mock_resp.link = ""
        client._call = MagicMock(return_value=mock_resp)

        links = node.get_links()
        self.assertEqual(links, [])

    def test_get_links_returns_link_when_exists(self):
        client = MagicMock()
        node = Node(client=client, node_id="node123")

        mock_resp = MagicMock()
        mock_resp.success = True
        mock_resp.has_link = True
        mock_resp.link = "https://example.com"
        client._call = MagicMock(return_value=mock_resp)

        links = node.get_links()
        self.assertEqual(links, ["https://example.com"])

    def test_set_links_returns_true(self):
        client = MagicMock()
        node = Node(client=client, node_id="node123")

        mock_resp = MagicMock()
        mock_resp.success = True
        client._call = MagicMock(return_value=mock_resp)

        result = node.set_links(["https://a.com", "https://b.com"])
        self.assertTrue(result)


class TestNodeTags(unittest.TestCase):
    """Test Node tag methods."""

    def test_set_tags_returns_true(self):
        client = MagicMock()
        node = Node(client=client, node_id="node123")

        mock_resp = MagicMock()
        mock_resp.success = True
        client._call = MagicMock(return_value=mock_resp)

        result = node.set_tags(["tag1", "tag2"])
        self.assertTrue(result)

    def test_add_tags_returns_true(self):
        client = MagicMock()
        node = Node(client=client, node_id="node123")

        mock_resp = MagicMock()
        mock_resp.success = True
        client._call = MagicMock(return_value=mock_resp)

        result = node.add_tags(["tag3"])
        self.assertTrue(result)


class TestNodeIcons(unittest.TestCase):
    """Test Node icon methods."""

    def test_add_icon_returns_true(self):
        client = MagicMock()
        node = Node(client=client, node_id="node123")

        mock_resp = MagicMock()
        mock_resp.success = True
        client._call = MagicMock(return_value=mock_resp)

        result = node.add_icon("star")
        self.assertTrue(result)


class TestNodeActions(unittest.TestCase):
    """Test Node action methods."""

    def test_select_calls_focus_node(self):
        client = MagicMock()
        node = Node(client=client, node_id="node123")
        client.focus_node = MagicMock(return_value=True)

        result = node.select()
        self.assertTrue(result)
        client.focus_node.assert_called_once_with("node123")

    def test_center_calls_focus_node(self):
        client = MagicMock()
        node = Node(client=client, node_id="node123")
        client.focus_node = MagicMock(return_value=True)

        result = node.center()
        self.assertTrue(result)
        client.focus_node.assert_called_once_with("node123")


class TestNodeEquality(unittest.TestCase):
    """Test Node equality and hash."""

    def test_eq_same_id(self):
        client = MagicMock()
        n1 = Node(client=client, node_id="node123")
        n2 = Node(client=client, node_id="node123")
        self.assertEqual(n1, n2)

    def test_eq_different_id(self):
        client = MagicMock()
        n1 = Node(client=client, node_id="node123")
        n2 = Node(client=client, node_id="node456")
        self.assertNotEqual(n1, n2)

    def test_hash(self):
        client = MagicMock()
        n1 = Node(client=client, node_id="node123")
        n2 = Node(client=client, node_id="node123")
        self.assertEqual(hash(n1), hash(n2))


class TestNodeRepr(unittest.TestCase):
    """Test Node.__repr__() method."""

    def test_repr_contains_id(self):
        client = MagicMock()
        node = Node(client=client, node_id="node123")

        mock_resp = MagicMock()
        mock_resp.success = True
        mock_resp.text = "Hello World"
        client._call = MagicMock(return_value=mock_resp)

        repr_str = repr(node)
        self.assertIn("node123", repr_str)
        self.assertIn("Hello World", repr_str)


if __name__ == "__main__":
    unittest.main()
