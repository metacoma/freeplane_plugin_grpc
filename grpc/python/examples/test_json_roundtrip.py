#!/usr/bin/env python3
"""test_json_roundtrip.py — Round-trip smoke test for canonical JSON import/export.

This test validates that:
1. Export → Import → Export preserves logical mind map structure
2. Both import and export use the same canonical JSON format
3. Supported node properties are preserved through the round trip
4. Legacy import format is still accepted

Usage:
    python3 test_json_roundtrip.py
"""

from __future__ import annotations

import json
import os
import sys
import time
import secrets

# Ensure the parent directory is on the path so we can import freeplane_grpc
sys.path.insert(0, os.path.join(os.path.dirname(__file__), ".."))

from freeplane_grpc import FreeplaneClient, FreeplaneOperationError, FreeplaneConnectionError


def generate_marker() -> str:
    """Generate a unique marker string for smoke test verification."""
    timestamp = time.strftime("%Y%m%d_%H%M%S")
    random_suffix = secrets.token_hex(2)
    return f"json-roundtrip-test-{timestamp}_{random_suffix}"


def create_canonical_json(marker: str) -> str:
    """Create a mind map in the canonical JSON format for import.

    The canonical format uses explicit fields:
    - text: node text
    - children: array of child node objects
    - attributes: key-value map
    - detail: detail/deep text
    - note: note text
    - link: hyperlink URI
    - tags: array of tag strings
    - icons: array of icon names
    - background_color: rgba(r,g,b,a) string
    - folded: boolean
    - relationships: array of {target_id, label} objects
    """
    mindmap = {
        "text": f"Root Node ({marker})",
        "children": [
            {
                "text": "Child 1 - With Note",
                "note": "This is a note on child 1",
                "detail": "This is a detail on child 1",
                "link": "https://example.com",
                "tags": ["tag1", "tag2"],
                "icons": ["star", "flag"],
                "attributes": {"custom_key": "custom_value", "number_attr": "42"},
                "children": [
                    {
                        "text": "Grandchild 1.1",
                        "note": "Note on grandchild 1.1",
                        "detail": "Detail on grandchild 1.1",
                    },
                    {
                        "text": "Grandchild 1.2",
                        "background_color": "rgba(255,128,0,128)",
                        "folded": True,
                    }
                ],
            },
            {
                "text": "Child 2 - Simple",
            },
            {
                "text": "Child 3 - With Attributes",
                "attributes": {
                    "priority": "high",
                    "status": "active"
                },
            }
        ]
    }
    return json.dumps({"_fp_import_root_node": "root", "mindmap": mindmap}, indent=2)


def create_legacy_json(marker: str) -> str:
    """Create a mind map in the legacy key-as-text format for import.

    The legacy format uses keys as node text:
    {"parent": {"child1": "value", "child2": {"grandchild": "value"}}}
    """
    legacy = {
        f"Root Node ({marker})": {
            "Child 1": {
                "Grandchild 1.1": "Note on grandchild 1.1",
                "Grandchild 1.2": "",
            },
            "Child 2": "",
            "Child 3": "",
        }
    }
    return json.dumps(legacy, indent=2)


def compare_nodes(node_a: dict, node_b: dict, path: str = "root") -> list[str]:
    """Compare two node dicts (from canonical JSON) and return list of differences."""
    diffs = []

    # Compare text
    text_a = node_a.get("text", "")
    text_b = node_b.get("text", "")
    if text_a != text_b:
        diffs.append(f"{path}.text: '{text_a}' != '{text_b}'")

    # Compare children count
    children_a = node_a.get("children", [])
    children_b = node_b.get("children", [])
    if len(children_a) != len(children_b):
        diffs.append(f"{path}.children count: {len(children_a)} != {len(children_b)}")

    # Compare children recursively
    for i, (child_a, child_b) in enumerate(zip(children_a, children_b)):
        child_path = f"{path}.children[{i}]"
        diffs.extend(compare_nodes(child_a, child_b, child_path))

    # Compare note
    note_a = node_a.get("note", "")
    note_b = node_b.get("note", "")
    if note_a != note_b:
        diffs.append(f"{path}.note: '{note_a}' != '{note_b}'")

    # Compare detail
    detail_a = node_a.get("detail", "")
    detail_b = node_b.get("detail", "")
    if detail_a != detail_b:
        diffs.append(f"{path}.detail: '{detail_a}' != '{detail_b}'")

    # Compare link
    link_a = node_a.get("link", "")
    link_b = node_b.get("link", "")
    if link_a != link_b:
        diffs.append(f"{path}.link: '{link_a}' != '{link_b}'")

    # Compare tags
    tags_a = node_a.get("tags", [])
    tags_b = node_b.get("tags", [])
    if set(tags_a) != set(tags_b):
        diffs.append(f"{path}.tags: {set(tags_a)} != {set(tags_b)}")

    # Compare icons
    icons_a = node_a.get("icons", [])
    icons_b = node_b.get("icons", [])
    if set(icons_a) != set(icons_b):
        diffs.append(f"{path}.icons: {set(icons_a)} != {set(icons_b)}")

    # Compare attributes
    attrs_a = node_a.get("attributes", {})
    attrs_b = node_b.get("attributes", {})
    if set(attrs_a.keys()) != set(attrs_b.keys()):
        diffs.append(f"{path}.attributes keys: {set(attrs_a.keys())} != {set(attrs_b.keys())}")
    else:
        for key in attrs_a:
            if attrs_a[key] != attrs_b[key]:
                diffs.append(f"{path}.attributes[{key}]: '{attrs_a[key]}' != '{attrs_b[key]}'")

    return diffs


def get_node_text_by_text(mindmap: MindMap, target_text: str) -> str | None:
    """Find a node by its text and return its text (for verification)."""
    root = mindmap.root()
    # BFS search
    queue = [root]
    while queue:
        node = queue.pop(0)
        text = node.get_text()
        if target_text in text:
            return text
        queue.extend(node.get_children())
    return None


def main() -> int:
    """Run the round-trip smoke test and return 0 on success, 1 on failure."""
    host = os.environ.get("FREEPLANE_HOST", "127.0.0.1")
    port = int(os.environ.get("FREEPLANE_PORT", "50051"))
    marker = generate_marker()

    print(f"Connecting to Freeplane gRPC server at {host}:{port}...")

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

    all_passed = True
    test_results = []

    # Verify a map is loaded (Freeplane should be started with a blank map)
    try:
        exported = client.get_map_to_json()
        print("Verified map is loaded")
    except FreeplaneOperationError as e:
        print(f"✗ No map loaded: {e}", file=sys.stderr)
        return 1

    try:
        # =====================================================================
        # TEST 1: Canonical format export → import → export
        # =====================================================================
        print("\n" + "=" * 60)
        print("TEST 1: Canonical format round-trip")
        print("=" * 60)

        # Create canonical JSON
        canonical_json = create_canonical_json(marker)
        print(f"\nCreated canonical JSON ({len(canonical_json)} chars):")
        print(canonical_json[:300] + "...")

        # Parse it to verify valid JSON
        try:
            json_data = json.loads(canonical_json)
            print("✓ Valid JSON")
        except json.JSONDecodeError as e:
            print(f"✗ Invalid JSON: {e}", file=sys.stderr)
            return 1

        # Import canonical JSON into Freeplane (wraps in _fp_import_root_node to target root)
        print("\n--- Importing canonical JSON ---")
        try:
            success = client.mind_map_from_json(canonical_json)
            if not success:
                print("✗ Import failed", file=sys.stderr)
                all_passed = False
                test_results.append(("canonical_import", False, "Import returned false"))
            else:
                print("✓ Import succeeded")
                test_results.append(("canonical_import", True, ""))
        except FreeplaneOperationError as exc:
            print(f"✗ Import operation failed: {exc}", file=sys.stderr)
            all_passed = False
            test_results.append(("canonical_import", False, str(exc)))
            return 1

        # Export the imported map
        print("\n--- Exporting imported map ---")
        try:
            exported_json = client.get_map_to_json()
            print(f"Exported JSON ({len(exported_json)} chars)")

            exported_data = json.loads(exported_json)
            print("✓ Valid exported JSON")
        except (FreeplaneOperationError, json.JSONDecodeError) as exc:
            print(f"✗ Export failed: {exc}", file=sys.stderr)
            all_passed = False
            test_results.append(("canonical_export", False, str(exc)))
            return 1

        # Verify exported JSON has the canonical structure
        if "text" not in exported_data:
            print("✗ Exported JSON missing 'text' field (not canonical format)", file=sys.stderr)
            all_passed = False
            test_results.append(("canonical_structure", False, "Missing 'text' field"))
        else:
            print("✓ Exported JSON has canonical 'text' field")
            test_results.append(("canonical_structure", True, ""))

        # Find the imported root node by searching for marker text across all nodes
        # (import creates a new child node under the selected node, not replacing it)
        exported_root = exported_data
        imported_root = None

        def find_node_by_text(node, marker):
            """Recursively search for a node whose text contains marker."""
            if node.get("text", "") and marker in node["text"]:
                return node
            for child in node.get("children", []):
                result = find_node_by_text(child, marker)
                if result:
                    return result
            return None

        imported_root = find_node_by_text(exported_root, marker)

        if imported_root is None:
            # Debug: print all node texts in exported JSON
            all_texts = []
            def collect_texts(node, depth=0):
                t = node.get("text", "")
                if t:
                    all_texts.append("  " * depth + repr(t[:80]))
                for c in node.get("children", []):
                    collect_texts(c, depth + 1)
            collect_texts(exported_root)
            print(f"✗ Could not find imported root node in exported data", file=sys.stderr)
            print(f"  Expected text containing: '{marker}'", file=sys.stderr)
            print(f"  All texts in exported JSON:", file=sys.stderr)
            for t in all_texts:
                print(f"  {t}", file=sys.stderr)
            all_passed = False
            test_results.append(("find_imported_root", False, ""))
            return 1
        print(f"✓ Found imported root in exported data")
        test_results.append(("find_imported_root", True, ""))

        # Compare original vs imported (not vs full exported which includes wrapper root)
        # json_data is the wrapped JSON; extract the actual mindmap
        orig_mindmap = json_data.get("mindmap", json_data)
        print("\n--- Comparing original vs imported ---")
        root_text_orig = orig_mindmap.get("text", "")
        root_text_imported = imported_root.get("text", "")

        if marker in root_text_orig and marker in root_text_imported:
            print(f"✓ Root text preserved: '{root_text_orig}'")
            test_results.append(("root_text", True, ""))
        else:
            print(f"✗ Root text mismatch: '{root_text_orig}' != '{root_text_imported}'", file=sys.stderr)
            all_passed = False
            test_results.append(("root_text", False, f"{root_text_orig} != {root_text_imported}"))

        # Compare children count
        children_orig = orig_mindmap.get("children", [])
        children_imported = imported_root.get("children", [])

        if len(children_orig) == len(children_imported):
            print(f"✓ Children count preserved: {len(children_orig)}")
            test_results.append(("children_count", True, ""))
        else:
            print(f"✗ Children count mismatch: {len(children_orig)} != {len(children_imported)}", file=sys.stderr)
            all_passed = False
            test_results.append(("children_count", False, f"{len(children_orig)} != {len(children_imported)}"))

        # Deep compare nodes
        print("\n--- Deep node comparison ---")
        diffs = compare_nodes(orig_mindmap, imported_root)
        if diffs:
            print(f"✗ Found {len(diffs)} difference(s):")
            for d in diffs[:10]:  # Show first 10
                print(f"  - {d}")
            all_passed = False
            test_results.append(("deep_compare", False, "; ".join(diffs[:5])))
        else:
            print("✓ All node properties match")
            test_results.append(("deep_compare", True, ""))

        # =====================================================================
        # TEST 2: Legacy format import
        # =====================================================================
        print("\n" + "=" * 60)
        print("TEST 2: Legacy format import")
        print("=" * 60)

        # Clear existing children to isolate the legacy import
        mindmap2 = client.current_map()
        root2 = mindmap2.root()
        children_before2 = list(root2.children())
        cleared_count2 = 0
        for child in children_before2:
            try:
                if child.delete():
                    cleared_count2 += 1
            except Exception:
                pass
        print(f"✓ Cleared {cleared_count2}/{len(children_before2)} existing children")

        legacy_json = create_legacy_json(marker)
        print(f"\nCreated legacy JSON ({len(legacy_json)} chars)")

        try:
            success = client.mind_map_from_json(legacy_json)
            if success:
                print("✓ Legacy format import succeeded")
                test_results.append(("legacy_import", True, ""))
            else:
                print("✗ Legacy format import failed", file=sys.stderr)
                all_passed = False
                test_results.append(("legacy_import", False, "Import returned false"))
        except FreeplaneOperationError as exc:
            print(f"✗ Legacy format import operation failed: {exc}", file=sys.stderr)
            all_passed = False
            test_results.append(("legacy_import", False, str(exc)))

        # Verify legacy import produced valid structure
        exported_legacy = client.get_map_to_json()
        legacy_data = json.loads(exported_legacy)

        if "text" in legacy_data:
            print("✓ Legacy import produces canonical export format")
            test_results.append(("legacy_to_canonical", True, ""))
        else:
            print("✗ Legacy import does NOT produce canonical export format", file=sys.stderr)
            all_passed = False
            test_results.append(("legacy_to_canonical", False, "Missing 'text' field"))

        # =====================================================================
        # TEST 3: Verify specific properties through gRPC
        # =====================================================================
        print("\n" + "=" * 60)
        print("TEST 3: Verify properties through gRPC")
        print("=" * 60)

        # Clear existing children to isolate the canonical re-import
        mindmap3 = client.current_map()
        root3 = mindmap3.root()
        children_before3 = list(root3.children())
        cleared_count3 = 0
        for child in children_before3:
            try:
                if child.delete():
                    cleared_count3 += 1
            except Exception:
                pass
        print(f"✓ Cleared {cleared_count3}/{len(children_before3)} existing children")

        # Re-import canonical data for gRPC verification
        client.mind_map_from_json(canonical_json)
        mindmap = client.current_map()
        root = mindmap.root()

        # Find imported node by marker text (import creates a child node)
        imported_node = None
        for child in root.children():
            if marker in child.get_text():
                imported_node = child
                break

        if imported_node is None:
            print(f"✗ Could not find imported node via gRPC", file=sys.stderr)
            all_passed = False
            test_results.append(("grpc_find_node", False, ""))
        else:
            # Verify root text
            root_text = imported_node.get_text()
            print(f"✓ Root text verified via gRPC: '{root_text}'")
            test_results.append(("grpc_root_text", True, ""))

            # Verify children
            children = imported_node.children()
            if len(children) == 3:
                print(f"✓ Children count verified via gRPC: {len(children)}")
                test_results.append(("grpc_children", True, ""))
            else:
                print(f"✗ Children count mismatch via gRPC: {len(children)}", file=sys.stderr)
                all_passed = False
                test_results.append(("grpc_children", False, str(len(children))))

        # Verify first child has note
        if imported_node is not None:
            imported_children = imported_node.children()
            if len(imported_children) > 0:
                child1 = imported_children[0]
                note_text = child1.get_notes()
                if note_text and "This is a note on child 1" in note_text:
                    print(f"✓ Child 1 note verified via gRPC")
                    test_results.append(("grpc_note", True, ""))
                else:
                    print(f"✗ Child 1 note NOT found via gRPC: '{note_text}'", file=sys.stderr)
                    all_passed = False
                    test_results.append(("grpc_note", False, str(note_text)))

        # =====================================================================
        # TEST 4: Export → Import → Export round-trip consistency
        # =====================================================================
        print("\n" + "=" * 60)
        print("TEST 4: Export → Import → Export consistency")
        print("=" * 60)

        # Create a fresh canonical JSON for this test
        roundtrip_marker = f"roundtrip-{marker}"
        roundtrip_json = create_canonical_json(roundtrip_marker)
        roundtrip_data = json.loads(roundtrip_json)
        roundtrip_mindmap = roundtrip_data.get("mindmap", roundtrip_data)

        # Clear existing children to ensure a clean state for round-trip
        mindmap4 = client.current_map()
        root4 = mindmap4.root()
        children_before4 = list(root4.children())
        cleared_count4 = 0
        for child in children_before4:
            try:
                if child.delete():
                    cleared_count4 += 1
            except Exception:
                pass
        print(f"✓ Cleared {cleared_count4}/{len(children_before4)} existing children")

        # Import the canonical JSON
        client.mind_map_from_json(roundtrip_json)

        # Export again
        json2 = client.get_map_to_json()
        data2 = json.loads(json2)

        # Find the imported root in the re-exported data
        imported_root = None
        def find_node_by_text(node, marker):
            if node.get("text", "") and marker in node["text"]:
                return node
            for child in node.get("children", []):
                result = find_node_by_text(child, marker)
                if result:
                    return result
            return None
        imported_root = find_node_by_text(data2, roundtrip_marker)

        if imported_root is None:
            print(f"✗ Could not find imported root in re-exported data", file=sys.stderr)
            all_passed = False
            test_results.append(("roundtrip_consistency", False, "imported root not found"))
        else:
            # Compare the original canonical JSON with the re-exported imported node
            roundtrip_diffs = compare_nodes(roundtrip_mindmap, imported_root)
            if roundtrip_diffs:
                print(f"✗ Round-trip inconsistency: {len(roundtrip_diffs)} difference(s)")
                for d in roundtrip_diffs[:5]:
                    print(f"  - {d}")
                all_passed = False
                test_results.append(("roundtrip_consistency", False, "; ".join(roundtrip_diffs[:3])))
            else:
                print("✓ Export → Import → Export is structurally consistent")
                test_results.append(("roundtrip_consistency", True, ""))

        # =====================================================================
        # Summary
        # =====================================================================
        print("\n" + "=" * 60)
        print("TEST SUMMARY")
        print("=" * 60)

        passed = sum(1 for _, result, _ in test_results if result)
        total = len(test_results)

        for name, result, detail in test_results:
            status = "✓ PASS" if result else "✗ FAIL"
            print(f"  {status}: {name}" + (f" - {detail}" if detail else ""))

        print(f"\n  Result: {passed}/{total} tests passed")

        if all_passed:
            print("\n" + "=" * 60)
            print("ALL TESTS PASSED")
            print("=" * 60)
            return 0
        else:
            print("\n" + "=" * 60)
            print("SOME TESTS FAILED")
            print("=" * 60)
            return 1

    except FreeplaneOperationError as exc:
        print(f"\nOPERATION FAILED: {exc}", file=sys.stderr)
        return 1
    except Exception as exc:
        print(f"\nUNEXPECTED ERROR: {exc}", file=sys.stderr)
        import traceback
        traceback.print_exc()
        return 1
    finally:
        client.close()


if __name__ == "__main__":
    sys.exit(main())
