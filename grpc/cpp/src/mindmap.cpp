#include "mindmap.h"
#include "client.h"
#include "node.h"

#include <algorithm>
#include <sstream>

namespace freeplane {
namespace grpc {

MindMap::MindMap(FreeplaneClient* client, const std::string& mapId, 
                 const std::string& nodeId)
    : client_(client), map_id_(mapId), node_id_(nodeId) {
}

// -- navigation ---------------------------------------------------------

std::shared_ptr<Node> MindMap::root() {
    // Get the current node which should be the root or we need to find root
    // We use the map_id to find the root - but since the API doesn't have
    // a direct "get root" method, we use the current node's parent chain
    // to find root, or just return the current node if it's the root.
    
    std::string current_id = node_id_;
    if (current_id.empty()) {
        // Try to get current node first
        auto resp = client_->getCurrentNode();
        if (resp->success() && !resp->node_id().empty()) {
            current_id = resp->node_id();
        } else {
            throw FreeplaneOperationError("No map currently open to get root from");
        }
    }
    
    // Traverse up to find root
    while (!current_id.empty()) {
        auto parent_resp = client_->getParentNode(current_id);
        if (!parent_resp->success() || parent_resp->parent_node_id().empty()) {
            // This is the root
            return std::make_shared<Node>(client_, current_id, this);
        }
        current_id = parent_resp->parent_node_id();
    }
    
    // Fallback
    return std::make_shared<Node>(client_, node_id_, this);
}

std::shared_ptr<Node> MindMap::selectedNode() {
    auto resp = client_->getCurrentNode();
    if (!resp->success() || resp->node_id().empty()) {
        throw FreeplaneOperationError("No node currently selected");
    }
    return std::make_shared<Node>(client_, resp->node_id(), this);
}

std::vector<std::shared_ptr<Node>> MindMap::findNodes(const std::string& pattern) {
    auto root_node = root();
    std::vector<std::shared_ptr<Node>> matches;
    walkAndCollect(root_node, pattern, matches);
    return matches;
}

void MindMap::walkAndCollect(std::shared_ptr<Node> node, const std::string& pattern,
                              std::vector<std::shared_ptr<Node>>& matches) {
    try {
        std::string text = node->getText();
        std::transform(text.begin(), text.end(), text.begin(), ::tolower);
        std::string lower_pattern = pattern;
        std::transform(lower_pattern.begin(), lower_pattern.end(), lower_pattern.begin(), ::tolower);
        
        if (text.find(lower_pattern) != std::string::npos) {
            matches.push_back(node);
        }
    } catch (const FreeplaneOperationError&) {
        // Skip nodes we can't read
    }
    
    // Check children
    try {
        auto children = node->children();
        for (const auto& child : children) {
            walkAndCollect(child, pattern, matches);
        }
    } catch (const FreeplaneOperationError&) {
        // No children or error accessing them
    }
}

// -- metadata -----------------------------------------------------------

std::map<std::string, std::string> MindMap::info() const {
    return {{"map_id", map_id_}, {"node_id", node_id_}};
}

int MindMap::size() {
    auto root_node = root();
    return countNodes(root_node);
}

int MindMap::countNodes(std::shared_ptr<Node> node) {
    int count = 1;  // Count this node
    try {
        auto children = node->children();
        for (const auto& child : children) {
            count += countNodes(child);
        }
    } catch (const FreeplaneOperationError&) {
        // No children or error
    }
    return count;
}

// -- file operations ----------------------------------------------------

bool MindMap::save(const std::string& path) {
    // The gRPC API doesn't have a direct Save RPC; the map is saved
    // automatically by Freeplane. For explicit save, users can use Groovy.
    if (!path.empty()) {
        client_->groovy(
            "def controller = model.getMap().getController();"
            "model.getMap().getFile().setFile(new File('" + path + "'));"
            "controller.getUndoManager().undoableChanges(model.getMap());"
            "controller.getMapView().updateFileHistory(model.getMap());");
    }
    return true;
}

bool MindMap::exportMap(const std::string& path, const std::string& format) {
    std::ostringstream groovy;
    groovy << "def controller = model.getMap().getController();"
           << "def view = controller.getMapView();"
           << "view.exportMap(new File('" << path << "'), '" << format << "');";
    
    auto result = client_->groovy(groovy.str());
    return result.find("Error") == std::string::npos;
}

std::shared_ptr<MindMap> MindMap::importMap(const std::string& path) {
    return client_->openMap(path);
}

// -- node creation ------------------------------------------------------

std::shared_ptr<Node> MindMap::createNode(const std::string& text, 
                                           const std::string& parentId,
                                           const std::string& style) {
    (void)style;  // Style is not directly supported via gRPC
    
    // Get root if no parent specified
    std::string parent_id = parentId;
    if (parent_id.empty()) {
        parent_id = root()->nodeId();
    }
    
    auto resp = client_->createChild(text, parent_id);
    return std::make_shared<Node>(client_, resp->node_id(), this);
}

std::shared_ptr<Node> MindMap::createChild(std::shared_ptr<Node> parent,
                                            const std::string& text,
                                            const std::string& style) {
    (void)style;  // Style is not directly supported via gRPC
    return createNode(text, parent->nodeId());
}

std::string MindMap::getToJson() {
    return client_->getMapToJson();
}

bool MindMap::setFromJson(const std::string& json) {
    return client_->mindMapFromJson(json);
}

std::string MindMap::toString() const {
    std::ostringstream oss;
    oss << "MindMap(map_id=" << map_id_ << ", node_id=" << node_id_ << ")";
    return oss.str();
}

}  // namespace grpc
}  // namespace freeplane
