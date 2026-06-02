"""Tests for the FreeplaneClient class."""

import sys
import os
import unittest
from unittest.mock import MagicMock, patch, Mock

# Ensure the package is importable
sys.path.insert(0, os.path.join(os.path.dirname(__file__), ".."))

from freeplane_grpc import FreeplaneClient
from freeplane_grpc.exceptions import (
    FreeplaneConnectionError,
    FreeplaneOperationError,
)


class TestFreeplaneClientInit(unittest.TestCase):
    """Test FreeplaneClient initialization."""

    def test_default_host_port(self):
        client = FreeplaneClient()
        self.assertEqual(client.host, "127.0.0.1")
        self.assertEqual(client.port, 50051)

    def test_custom_host_port(self):
        client = FreeplaneClient(host="10.0.0.1", port=9999)
        self.assertEqual(client.host, "10.0.0.1")
        self.assertEqual(client.port, 9999)

    def test_channel_is_none_initially(self):
        client = FreeplaneClient()
        self.assertIsNone(client._channel)

    def test_stub_is_none_initially(self):
        client = FreeplaneClient()
        self.assertIsNone(client._grpc_stub)


class TestFreeplaneClientContextManager(unittest.TestCase):
    """Test context-manager protocol."""

    @patch("grpc.insecure_channel")
    def test_context_manager_connects_and_closes(self, mock_channel_cls):
        mock_channel = MagicMock()
        mock_channel.channel_ready.return_value.result.return_value = None
        mock_channel_cls.return_value = mock_channel

        with FreeplaneClient(host="127.0.0.1", port=50051) as client:
            self.assertIsNotNone(client._channel)

        mock_channel.close.assert_called_once()


class TestFreeplaneClientImport(unittest.TestCase):
    """Test that the package can be imported."""

    def test_import_freeplane_grpc(self):
        import freeplane_grpc
        self.assertEqual(freeplane_grpc.__name__, "freeplane_grpc")

    def test_import_freeplane_client(self):
        from freeplane_grpc import FreeplaneClient
        self.assertIsNotNone(FreeplaneClient)

    def test_import_mindmap(self):
        from freeplane_grpc import MindMap
        self.assertIsNotNone(MindMap)

    def test_import_node(self):
        from freeplane_grpc import Node
        self.assertIsNotNone(Node)

    def test_import_exceptions(self):
        from freeplane_grpc import (
            FreeplaneGrpcError,
            FreeplaneConnectionError,
            FreeplaneOperationError,
            NodeNotFoundError,
            MindMapError,
        )
        self.assertIsNotNone(FreeplaneGrpcError)
        self.assertIsNotNone(FreeplaneConnectionError)
        self.assertIsNotNone(FreeplaneOperationError)
        self.assertIsNotNone(NodeNotFoundError)
        self.assertIsNotNone(MindMapError)


class TestFreeplaneClientConnect(unittest.TestCase):
    """Test connection behavior."""

    @patch("grpc.insecure_channel")
    def test_connect_creates_channel(self, mock_channel_cls):
        mock_channel = MagicMock()
        mock_channel.channel_ready.return_value.result.return_value = None
        mock_channel_cls.return_value = mock_channel

        client = FreeplaneClient()
        client.connect()

        mock_channel_cls.assert_called_once_with("127.0.0.1:50051")
        self.assertIsNotNone(client._channel)

    @patch("grpc.insecure_channel")
    def test_connect_timeout_raises(self, mock_channel_cls):
        import grpc
        mock_channel = MagicMock()
        mock_channel.channel_ready.return_value.result.side_effect = grpc.FutureTimeoutError()
        mock_channel_cls.return_value = mock_channel

        client = FreeplaneClient()
        with self.assertRaises(FreeplaneConnectionError):
            client.connect()

    @patch("grpc.insecure_channel")
    def test_close_clears_channel(self, mock_channel_cls):
        mock_channel = MagicMock()
        mock_channel.channel_ready.return_value.result.return_value = None
        mock_channel_cls.return_value = mock_channel

        client = FreeplaneClient()
        client.connect()
        self.assertIsNotNone(client._channel)

        client.close()
        self.assertIsNone(client._channel)


class TestFreeplaneClientCall(unittest.TestCase):
    """Test the internal _call helper."""

    @patch("grpc.insecure_channel")
    def test_call_success(self, mock_channel_cls):
        mock_channel = MagicMock()
        mock_channel.channel_ready.return_value.result.return_value = None
        mock_channel_cls.return_value = mock_channel

        client = FreeplaneClient()
        client.connect()

        # Create a mock stub method
        mock_response = MagicMock()
        mock_response.success = True
        client._grpc_stub = MagicMock()
        client._grpc_stub.TestMethod = MagicMock(return_value=mock_response)

        result = client._call(client._grpc_stub.TestMethod)
        self.assertTrue(result.success)

    @patch("grpc.insecure_channel")
    def test_call_server_failure_raises(self, mock_channel_cls):
        mock_channel = MagicMock()
        mock_channel.channel_ready.return_value.result.return_value = None
        mock_channel_cls.return_value = mock_channel

        client = FreeplaneClient()
        client.connect()

        # Create a mock stub method that returns success=False
        mock_response = MagicMock()
        mock_response.success = False
        mock_response.error_message = "Node not found"
        client._grpc_stub = MagicMock()
        client._grpc_stub.TestMethod = MagicMock(return_value=mock_response)

        with self.assertRaises(FreeplaneOperationError):
            client._call(client._grpc_stub.TestMethod)

    @patch("grpc.insecure_channel")
    def test_call_grpc_error_raises_connection_error(self, mock_channel_cls):
        import grpc
        mock_channel = MagicMock()
        mock_channel.channel_ready.return_value.result.return_value = None
        mock_channel_cls.return_value = mock_channel

        client = FreeplaneClient()
        client.connect()

        client._grpc_stub = MagicMock()
        rpc_error = MagicMock(spec=grpc.RpcError)
        rpc_error.code = MagicMock(return_value=grpc.StatusCode.UNAVAILABLE)
        rpc_error.details = MagicMock(return_value="Server unavailable")
        client._grpc_stub.TestMethod = MagicMock(side_effect=rpc_error)

        with self.assertRaises(FreeplaneConnectionError):
            client._call(client._grpc_stub.TestMethod)


if __name__ == "__main__":
    unittest.main()
