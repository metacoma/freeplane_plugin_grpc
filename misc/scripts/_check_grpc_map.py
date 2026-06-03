import sys, os, grpc

# Add proto stubs to path
SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
# misc/scripts/ -> plugin root is two levels up
PLUGIN_REPO = os.path.realpath(os.path.join(SCRIPT_DIR, "..", ".."))
sys.path.insert(0, os.path.join(PLUGIN_REPO, "grpc/python"))

from freeplane_pb2 import GetCurrentNodeRequest
import freeplane_pb2_grpc

host = os.environ.get("GRPC_HOST", "127.0.0.1")
port = int(os.environ.get("GRPC_PORT", "50051"))
timeout = float(os.environ.get("GRPC_TIMEOUT", "3"))

try:
    ch = grpc.insecure_channel(f"{host}:{port}")
    stub = freeplane_pb2_grpc.FreeplaneStub(ch)
    resp = stub.GetCurrentNode(GetCurrentNodeRequest(), timeout=timeout)
    if resp.success:
        print("OK")
    else:
        print("NO_MAP")
except Exception:
    print("FAIL")
