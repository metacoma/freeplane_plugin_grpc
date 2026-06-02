"""Internal module for loading gRPC stubs.

This module hides protobuf import complexity from users.
The generated code (freeplane_pb2.py, freeplane_pb2_grpc.py) must be
present in the same directory or on the Python path.
"""

from __future__ import annotations

# Import the generated protobuf modules
# These files are either pre-generated and committed, or generated via
# generate_stubs.sh before use.
import freeplane_pb2
import freeplane_pb2_grpc

# Re-export the stub class for use by client.py
FreeplaneStub = freeplane_pb2_grpc.FreeplaneStub

__all__ = ["FreeplaneStub"]
