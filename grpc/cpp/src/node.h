#ifndef FREEPLANE_GRPC_NODE_H
#define FREEPLANE_GRPC_NODE_H

#include <memory>
#include <string>
#include <vector>

namespace freeplane {
namespace grpc {

class FreeplaneClient;
class MindMap;

/**
 * Represents a Freeplane mind map node.
 *
 * Provides node-level operations such as getting/setting text,
 * managing children, styling, notes, attributes, links, and icons.
 */
class Node {
public:
    /**
     * Create a Node.
     * @param client The FreeplaneClient to use for gRPC calls.
     * @param nodeId The server-side node ID.
     * @param mindmap Optional parent MindMap reference.
     */
    Node(FreeplaneClient* client, const std::string& nodeId, 
         MindMap* mindmap = nullptr);
    
    /**
     * Get the FreeplaneClient used for gRPC calls.
     */
    FreeplaneClient* client() const { return client_; }
    
    /**
     * Get the server-side node ID.
     */
    const std::string& nodeId() const { return node_id_; }
    
    /**
     * Get the parent MindMap, if available.
     */
    MindMap* mindmap() const { return mindmap_; }
    
    // -- text ----------------------------------------------------------------
    
    /**
     * Get the text of this node.
     */
    std::string getText() const;
    
    /**
     * Set the text of this node.
     */
    void setText(const std::string& text);
    
    // -- hierarchy -----------------------------------------------------------
    
    /**
     * Add a child node to this node.
     * @param text The child node text.
     * @param style Node style (e.g., "classic", "bubble", "flag").
     * @return A Node instance for the newly created child node.
     */
    std::shared_ptr<Node> addChild(const std::string& text, 
                                   const std::string& style = "classic");
    
    /**
     * Get the direct children of this node.
     */
    std::vector<std::shared_ptr<Node>> children();
    
    /**
     * Get the parent of this node.
     * @throws NodeNotFoundError if this node has no parent (is root).
     */
    std::shared_ptr<Node> parent();
    
    /**
     * Delete this node.
     */
    bool deleteNode();
    
    /**
     * Move this node under a new parent.
     * @param newParentId ID of the new parent node.
     */
    bool move(const std::string& newParentId);
    
    // -- styling -------------------------------------------------------------
    
    /**
     * Set the style of this node.
     * @param style Style name (e.g., "classic", "bubble", "flag", "diamond").
     */
    bool setStyle(const std::string& style);
    
    /**
     * Set the foreground color of this node.
     * @param red Red component (0-255).
     * @param green Green component (0-255).
     * @param blue Blue component (0-255).
     * @param alpha Alpha component (0-255).
     */
    bool setColor(int32_t red, int32_t green, int32_t blue, int32_t alpha = 255);
    
    /**
     * Set the background color of this node.
     * @param red Red component (0-255).
     * @param green Green component (0-255).
     * @param blue Blue component (0-255).
     * @param alpha Alpha component (0-255).
     */
    bool setBackgroundColor(int32_t red, int32_t green, int32_t blue, int32_t alpha = 255);
    
    // -- notes ---------------------------------------------------------------
    
    /**
     * Get the note of this node.
     * @return Pair of (has_note, note_text).
     */
    std::pair<bool, std::string> getNote();
    
    /**
     * Set the note of this node.
     */
    void setNote(const std::string& note);
    
    // -- attributes ----------------------------------------------------------
    
    /**
     * Set a custom attribute on this node.
     */
    bool setAttribute(const std::string& name, const std::string& value);
    
    /**
     * Set multiple custom attributes on this node.
     */
    bool setAttributes(const std::vector<std::pair<std::string, std::string>>& attrs);
    
    // -- links ---------------------------------------------------------------
    
    /**
     * Get the links of this node.
     */
    std::vector<std::string> getLinks();
    
    /**
     * Set the links of this node (replaces existing links).
     */
    void setLinks(const std::vector<std::string>& links);
    
    // -- tags ----------------------------------------------------------------
    
    /**
     * Set the tags of this node (replaces existing tags).
     */
    bool setTags(const std::vector<std::string>& tags);
    
    /**
     * Add tags to this node (does not remove existing tags).
     */
    bool addTags(const std::vector<std::string>& tags);
    
    // -- icons ---------------------------------------------------------------
    
    /**
     * Add an icon to this node.
     */
    bool addIcon(const std::string& iconName);
    
    // -- state ---------------------------------------------------------------
    
    /**
     * Get the folded (collapsed) state of this node.
     */
    bool getFolded();
    
    /**
     * Set the folded (collapsed) state of this node.
     */
    bool setFolded(bool folded);
    
    // -- actions -------------------------------------------------------------
    
    /**
     * Select this node in the Freeplane UI.
     */
    bool select();
    
    /**
     * Center the view on this node.
     */
    bool center();
    
    /**
     * Reload this node's state from the server.
     */
    void refresh();
    
    /**
     * Get a string representation of this node.
     */
    std::string toString() const;

private:
    FreeplaneClient* client_;
    std::string node_id_;
    MindMap* mindmap_;
};

}  // namespace grpc
}  // namespace freeplane

#endif  // FREEPLANE_GRPC_NODE_H
