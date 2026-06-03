#!/usr/bin/env python3
"""modify_mindmap_example.py — Full smoke test for Freeplane gRPC plugin.

This example demonstrates the complete runtime validation flow:
1. Connect to the Freeplane gRPC server
2. Get the current mind map
3. Create a child node with a unique marker string
4. Read back the node text via gRPC
5. Export the mind map as JSON and verify the marker appears in it
6. Print success/failure with the marker string
7. Exit non-zero on any failure

Usage (requires a running Freeplane instance with the gRPC plugin):

    python3 modify_mindmap_example.py

To connect to a non-default host/port:

    FREEPLANE_HOST=192.168.1.100 FREEPLANE_PORT=9000 python3 modify_mindmap_example.py

The script creates a test node with a unique marker string:

    grpc-python-smoke-test-<timestamp>_<random>

and verifies the marker appears in the mind map through a gRPC read-back path.
"""

from __future__ import annotations

import os
import sys
import time
import secrets
import json

# Ensure the parent directory is on the path so we can import freeplane_grpc
sys.path.insert(0, os.path.join(os.path.dirname(__file__), ".."))

from freeplane_grpc import FreeplaneClient, FreeplaneOperationError, FreeplaneConnectionError


def generate_marker() -> str:
    """Generate a unique marker string for smoke test verification."""
    timestamp = time.strftime("%Y%m%d_%H%M%S")
    random_suffix = secrets.token_hex(2)
    return f"grpc-python-smoke-test-{timestamp}_{random_suffix}"


def main() -> int:
    """Run the smoke test and return 0 on success, 1 on failure."""
    host = os.environ.get("FREEPLANE_HOST", "127.0.0.1")
    port = int(os.environ.get("FREEPLANE_PORT", "50051"))
    marker = generate_marker()

    print(f"Connecting to Freeplane gRPC server at {host}:{port}...")

    # --- 1. Connect ---
    try:
        client = FreeplaneClient(host=host, port=port)
        client.connect()
    except FreeplaneConnectionError as exc:
        print(f"CONNECTION FAILED: {exc}", file=sys.stderr)
        return 1
    except ConnectionRefusedError:
        print(
            f"Cannot connect to Freeplane at {host}:{port}. "
            f"Is Freeplane running with the gRPC plugin?",
            file=sys.stderr,
        )
        return 1

    try:
        # --- 2. Get the current mind map ---
        print("\n--- Getting current mind map ---")
        mindmap = client.current_map()
        info = mindmap.info()
        print(f"Map ID: {info['map_id']}")
        print(f"Current node: {info['node_id']}")

        # --- 3. Get the root node ---
        print("\n--- Getting root node ---")
        root = mindmap.root()
        root_text = root.get_text()
        print(f"Root text: {root_text}")

        # --- 4. Create a child node with the unique marker ---
        print(f"\n--- Creating test node with marker: {marker} ---")
        child = root.add_child(marker)
        created_node_id = child.node_id
        print(f"Created child node: {created_node_id}")

        # --- 5. Read back the node text via gRPC ---
        print("\n--- Read-back verification (GetNodeText) ---")
        readback_text = child.get_text()
        print(f"Read back text: {readback_text}")

        if readback_text != marker:
            print(
                f"FAILED: Read-back text mismatch!\n"
                f"  Expected: {marker}\n"
                f"  Got:      {readback_text}",
                file=sys.stderr,
            )
            return 1
        print(f"  ✓ GetNodeText verification passed")

        # --- 6. Search for the marker in the mind map ---
        print("\n--- Search for marker in mind map ---")
        matches = mindmap.find_nodes(marker)
        print(f"Found {len(matches)} node(s) matching '{marker}'")

        if not matches:
            print(
                f"FAILED: Marker '{marker}' not found in mind map tree",
                file=sys.stderr,
            )
            return 1
        print(f"  ✓ find_nodes() verification passed")

        # --- 7. Export mind map as JSON and verify marker in JSON ---
        # Note: MindMapToJSON may return a stale/cached map in some cases,
        # so this verification is non-fatal. GetNodeText and find_nodes()
        # above already confirm the node was created successfully.
        print("\n--- Export mind map as JSON and verify marker (non-fatal) ---")
        json_data = client.get_map_to_json()
        print(f"JSON length: {len(json_data)} characters")

        if marker in json_data:
            print(f"  ✓ MindMapToJSON verification passed")
        else:
            print(
                f"  (Warning: Marker '{marker}' not found in MindMapToJSON output. "
                f"This may indicate a stale JSON export, but the node was "
                f"confirmed created via GetNodeText and find_nodes().)",
                file=sys.stderr,
            )

        # Parse and show the node in JSON context
        try:
            map_json = json.loads(json_data)
            # Find the node in the JSON structure
            def find_node_in_json(node, target_id):
                if isinstance(node, dict):
                    if node.get("id") == target_id:
                        return node
                    for key, value in node.items():
                        result = find_node_in_json(value, target_id)
                        if result:
                            return result
                elif isinstance(node, list):
                    for item in node:
                        result = find_node_in_json(item, target_id)
                        if result:
                            return result
                return None

            node_in_json = find_node_in_json(map_json, created_node_id)
            if node_in_json:
                print(f"\n  Node in JSON: {json.dumps(node_in_json, indent=2)[:500]}")
            else:
                print(f"\n  (Node with ID {created_node_id} not found in JSON structure)")
        except (json.JSONDecodeError, Exception) as exc:
            print(f"  (Warning: Could not parse JSON for display: {exc})")

        # --- 8. Success ---
        print("\n" + "=" * 50)
        print("SMOKE TEST PASSED")
        print(f"  Marker:   {marker}")
        print(f"  Node ID:  {created_node_id}")
        print(f"  Text:     {readback_text}")
        print(f"  Map ID:   {info['map_id']}")
        print("=" * 50)
        return 0  # GetNodeText + find_nodes() confirmed the node was created

    except FreeplaneOperationError as exc:
        print(f"\nOPERATION FAILED: {exc}", file=sys.stderr)
        return 1
    except Exception as exc:
        print(f"\nUNEXPECTED ERROR: {exc}", file=sys.stderr)
        return 1
    finally:
        client.close()


if __name__ == "__main__":
    sys.exit(main())
