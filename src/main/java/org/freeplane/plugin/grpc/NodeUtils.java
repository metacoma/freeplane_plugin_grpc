package org.freeplane.plugin.grpc;

import org.freeplane.features.attribute.Attribute;
import org.freeplane.features.attribute.NodeAttributeTableModel;
import org.freeplane.features.map.NodeModel;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Static utility methods for Freeplane node operations.
 */
public final class NodeUtils {

    NodeUtils() {
        // Utility class — prevent instantiation
    }

    /**
     * Finds a node in the given list whose "_uuid" attribute matches the specified UUID.
     */
    public static NodeModel findNodeByAttributeUUID(List<NodeModel> nodes, String uuid) {
        for (NodeModel node : nodes) {
            NodeAttributeTableModel natm = NodeAttributeTableModel.getModel(node);
            for (int i = 0; i < natm.getRowCount(); i++) {
                Attribute attr = natm.getAttribute(i);
                if ("_uuid".equals(attr.getName()) && uuid.equals(attr.getValue().toString())) {
                    return node;
                }
            }
        }
        return null;
    }

    /**
     * Parses a relationship string of the form "uuid1:type1,uuid2:type2" into a list of entries.
     */
    public static List<Map.Entry<String, String>> parseRelationships(String relationshipValue) {
        List<Map.Entry<String, String>> relationships = new ArrayList<>();
        if (relationshipValue == null || relationshipValue.isEmpty()) {
            return relationships;
        }

        String[] entries = relationshipValue.split(",");
        for (String entry : entries) {
            String[] parts = entry.split(":");
            if (parts.length == 2) {
                String uuid = parts[0].trim();
                String type = parts[1].trim();
                relationships.add(new AbstractMap.SimpleEntry<>(uuid, type));
            }
        }

        return relationships;
    }

    /**
     * Collects all nodes in the subtree rooted at {@code root} (including {@code root} itself).
     */
    public static List<NodeModel> collectSubtreeNodes(NodeModel root) {
        List<NodeModel> result = new ArrayList<>();
        collectSubtreeNodesRecursive(root, result);
        return result;
    }

    private static void collectSubtreeNodesRecursive(NodeModel node, List<NodeModel> result) {
        if (node == null) {
            return;
        }
        result.add(node);
        for (NodeModel child : node.getChildren()) {
            collectSubtreeNodesRecursive(child, result);
        }
    }
}
