#!/usr/bin/env python3
"""Basic usage example for the freeplane_grpc Python client.

This example demonstrates:
- Connecting to a Freeplane gRPC server
- Getting the current map and root node
- Creating and modifying nodes
- Reading node information
- Using high-level object methods
- Context-manager usage

Usage (requires a running Freeplane instance with the gRPC plugin):

    python3 examples/basic_usage.py

To connect to a non-default host/port:

    FREEPLANE_HOST=192.168.1.100 FREEPLANE_PORT=9000 python3 examples/basic_usage.py
"""

from __future__ import annotations

import os
import sys

# Ensure the parent directory is on the path so we can import freeplane_grpc
# This is needed when running the script directly from the examples directory
sys.path.insert(0, os.path.join(os.path.dirname(__file__), ".."))

from freeplane_grpc import FreeplaneClient, FreeplaneOperationError


def main() -> None:
    host = os.environ.get("FREEPLANE_HOST", "127.0.0.1")
    port = int(os.environ.get("FREEPLANE_PORT", "50051"))

    print(f"Connecting to Freeplane gRPC server at {host}:{port}...")

    # Use context manager for automatic resource cleanup
    try:
        with FreeplaneClient(host=host, port=port) as client:
            # Get the currently open mind map
            print("\n--- Current Map ---")
            mindmap = client.current_map()
            info = mindmap.info()
            print(f"Map ID: {info['map_id']}")
            print(f"Current node: {info['node_id']}")

            # Get the root node
            print("\n--- Root Node ---")
            root = mindmap.root()
            print(f"Root text: {root.get_text()}")

            # Create a child node under root
            print("\n--- Creating Child Node ---")
            child = root.add_child("Hello from Python!")
            print(f"Created child: {child.node_id}")

            # Modify the child node
            child.set_text("Updated text via Python")
            print(f"Updated text: {child.get_text()}")

            # Center the view on the child node
            child.center()
            print("Centered view on child node")

            # Add a child to the child node
            grandchild = child.add_child("Grandchild node")
            grandchild.set_attribute("type", "example")
            grandchild.set_notes("This is a note for the grandchild node.")
            print(
                f"Grandchild: {grandchild.node_id}, notes: {grandchild.get_notes()}"
            )

            # List all children of root
            print("\n--- Children of Root ---")
            children = root.children()
            for c in children:
                print(f"  [{c.node_id}] {c.get_text()}")

            # Search for nodes
            print("\n--- Search: 'Python' ---")
            matches = mindmap.find_nodes("Python")
            for m in matches:
                print(f"  [{m.node_id}] {m.get_text()}")

            # Set a tag on a node
            child.add_tags(["python", "example"])
            print(f"\nAdded tags to child node")

            # Get parent of child
            parent = child.parent()
            print(f"Parent of child: {parent.node_id} — {parent.get_text()}")

            # Set foreground color (red)
            child.set_color(255, 0, 0)
            print("Set child node color to red")

            # Set background color
            child.set_background_color(255, 255, 200)
            print("Set child node background color to light yellow")

            # Export map as JSON
            print("\n--- Map JSON (truncated) ---")
            json_data = client.get_map_to_json()
            print(f"JSON length: {len(json_data)} characters")
            print(f"First 200 chars: {json_data[:200]}...")

            print("\n--- Example completed successfully ---")

    except FreeplaneOperationError as exc:
        print(f"Operation failed: {exc}", file=sys.stderr)
        sys.exit(1)
    except ConnectionRefusedError:
        print(
            f"Cannot connect to Freeplane at {host}:{port}. "
            f"Is Freeplane running with the gRPC plugin?",
            file=sys.stderr,
        )
        sys.exit(1)
    except Exception as exc:
        print(f"Error: {exc}", file=sys.stderr)
        sys.exit(1)


if __name__ == "__main__":
    main()
