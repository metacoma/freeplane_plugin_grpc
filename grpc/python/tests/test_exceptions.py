"""Tests for the exception hierarchy."""

import sys
import os
import unittest

sys.path.insert(0, os.path.join(os.path.dirname(__file__), ".."))

from freeplane_grpc.exceptions import (
    FreeplaneGrpcError,
    FreeplaneConnectionError,
    FreeplaneOperationError,
    NodeNotFoundError,
    MindMapError,
)


class TestExceptionHierarchy(unittest.TestCase):
    """Test that exceptions form the correct hierarchy."""

    def test_base_exception_is_exception(self):
        self.assertTrue(issubclass(FreeplaneGrpcError, Exception))

    def test_connection_error_is_grpc_error(self):
        self.assertTrue(issubclass(FreeplaneConnectionError, FreeplaneGrpcError))

    def test_operation_error_is_grpc_error(self):
        self.assertTrue(issubclass(FreeplaneOperationError, FreeplaneGrpcError))

    def test_node_not_found_is_operation_error(self):
        self.assertTrue(issubclass(NodeNotFoundError, FreeplaneOperationError))

    def test_mind_map_error_is_operation_error(self):
        self.assertTrue(issubclass(MindMapError, FreeplaneOperationError))

    def test_all_exceptions_are_subclasses_of_each_other_correctly(self):
        # NodeNotFoundError should be a subclass of both FreeplaneOperationError
        # and FreeplaneGrpcError
        self.assertTrue(issubclass(NodeNotFoundError, FreeplaneGrpcError))

        # MindMapError should be a subclass of both FreeplaneOperationError
        # and FreeplaneGrpcError
        self.assertTrue(issubclass(MindMapError, FreeplaneGrpcError))


class TestExceptionMessages(unittest.TestCase):
    """Test that exceptions carry meaningful messages."""

    def test_base_exception_message(self):
        exc = FreeplaneGrpcError("Something went wrong")
        self.assertIn("Something went wrong", str(exc))

    def test_connection_error_message(self):
        exc = FreeplaneConnectionError("Cannot reach server")
        self.assertIn("Cannot reach server", str(exc))

    def test_operation_error_message(self):
        exc = FreeplaneOperationError("Operation failed")
        self.assertIn("Operation failed", str(exc))

    def test_node_not_found_error_message(self):
        exc = NodeNotFoundError("Node abc123 not found")
        self.assertIn("abc123", str(exc))

    def test_mind_map_error_message(self):
        exc = MindMapError("Map is corrupted")
        self.assertIn("corrupted", str(exc))

    def test_exception_no_raw_stack_trace(self):
        """Exceptions should not expose raw stack traces."""
        exc = FreeplaneOperationError("Failed")
        msg = str(exc)
        self.assertNotIn("Traceback", msg)
        self.assertNotIn("File ", msg)


class TestExceptionRaising(unittest.TestCase):
    """Test that exceptions can be raised and caught correctly."""

    def test_catch_grpc_error_base(self):
        try:
            raise FreeplaneOperationError("test")
        except FreeplaneGrpcError as exc:
            self.assertIsInstance(exc, FreeplaneOperationError)
        else:
            self.fail("Exception was not caught")

    def test_catch_connection_error(self):
        try:
            raise FreeplaneConnectionError("test")
        except FreeplaneConnectionError:
            pass
        except Exception:
            self.fail("Wrong exception type caught")

    def test_catch_node_not_found(self):
        try:
            raise NodeNotFoundError("not found")
        except NodeNotFoundError:
            pass
        except Exception:
            self.fail("Wrong exception type caught")

    def test_catch_node_not_found_as_operation_error(self):
        try:
            raise NodeNotFoundError("not found")
        except FreeplaneOperationError:
            pass
        except Exception:
            self.fail("Wrong exception type caught")


if __name__ == "__main__":
    unittest.main()
