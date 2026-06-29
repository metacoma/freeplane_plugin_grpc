#ifndef FREEPLANE_GRPC_MINDMAP_H
#define FREEPLANE_GRPC_MINDMAP_H

#include <memory>
#include <string>
#include <vector>
#include <functional>
#include <map>

namespace freeplane {
namespace grpc {

class FreeplaneClient;
class Node;

/**
 * Represents a Freeplane mind map.
 *
 * Provides map-level operations such as getting the root node,
 * searching nodes, and exporting/importing the map.
 */
class MindMap {
public:
    /**
     * Create a MindMap.
     * @param client The FreeplaneClient to use for gRPC calls.
     * @param mapId The ID of the mind map on the server.
     * @param nodeId The ID of the currently focused/selected node.
     */
    MindMap(FreeplaneClient* client, const std::string& mapId = "", 
            const std::string& nodeId = "");
    
    /**
     * Get the FreeplaneClient used for gRPC calls.
     */
    FreeplaneClient* client() const { return client_; }
    
    /**
     * Get the server-side map ID.
     */
    const std::string& mapId() const { return map_id_; }
    
    /**
     * Get the server-side node ID (currently focused node).
     */
    const std::string& nodeId() const { return node_id_; }
    
    // -- navigation ---------------------------------------------------------
    
    /**
     * Get the root node of this mind map.
     * Traverses up via GetParentNode until no parent exists.
     */
    std::shared_ptr<Node> root();
    
    /**
     * Get the currently selected/focused node.
     */
    std::shared_ptr<Node> selectedNode();
    
    /**
     * Find all nodes whose text contains the given pattern.
     * This method walks the entire tree starting from the root node
     * and collects nodes matching the pattern (case-insensitive).
     * @param pattern Text pattern to search for.
     */
    std::vector<std::shared_ptr<Node>> findNodes(const std::string& pattern);
    
    // -- metadata -----------------------------------------------------------
    
    /**
     * Get basic information about the current map.
     */
    std::map<std::string, std::string> info() const;
    
    /**
     * Estimate the number of nodes in the mind map.
     */
    int size();
    
    // -- file operations ----------------------------------------------------
    
    /**
     * Save the current mind map.
     * @param path Optional path to save to. If empty, saves to the
     *             current file location.
     */
    bool save(const std::string& path = "");
    
    /**
     * Export the current mind map to a file.
     * Uses Groovy scripting to trigger Freeplane's export functionality.
     * @param path Output file path.
     * @param format Export format (e.g., "png", "svg", "pdf", "html").
     */
    bool exportMap(const std::string& path, const std::string& format = "png");
    
    /**
     * Import a mind map from a file.
     * @param path Path to the map file (.mm, .xml, etc.).
     */
    std::shared_ptr<MindMap> importMap(const std::string& path);
    
    // -- node creation ------------------------------------------------------
    
    /**
     * Create a new node in the mind map.
     * @param text The node text.
     * @param parentId ID of the parent node. If empty, creates under root.
     * @param style Node style (e.g., "classic", "bubble", "flag").
     */
    std::shared_ptr<Node> createNode(const std::string& text, 
                                      const std::string& parentId = "",
                                      const std::string& style = "classic");
    
    /**
     * Create a child node under an existing node.
     * @param parent The parent Node instance.
     * @param text The node text.
     * @param style Node style.
     */
    std::shared_ptr<Node> createChild(std::shared_ptr<Node> parent,
                                       const std::string& text,
                                       const std::string& style = "classic");
    
    /**
     * Get the current mind map as JSON.
     */
    std::string getToJson();
    
    /**
     * Import a mind map from JSON.
     */
    bool setFromJson(const std::string& json);
    
    /**
     * Get a string representation of this mind map.
     */
    std::string toString() const;

private:
    void walkAndCollect(std::shared_ptr<Node> node, const std::string& pattern,
                        std::vector<std::shared_ptr<Node>>& matches);
    
    int countNodes(std::shared_ptr<Node> node);
    
    FreeplaneClient* client_;
    std::string map_id_;
    std::string node_id_;
};

}  // namespace grpc
}  // namespace freeplane

#endif  // FREEPLANE_GRPC_MINDMAP_H
