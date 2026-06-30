#include "node.h"
#include "client.h"
#include "mindmap.h"

#include <sstream>

namespace freeplane {
namespace grpc {

Node::Node(FreeplaneClient* client, const std::string& nodeId, MindMap* mindmap)
    : client_(client), node_id_(nodeId), mindmap_(mindmap) {
}

// -- text ----------------------------------------------------------------

std::string Node::getText() const {
    auto resp = client_->getNodeText(node_id_);
    if (!resp->success()) {
        throw FreeplaneOperationError(
            !resp->error_message().empty() ? resp->error_message() : "Failed to get node text");
    }
    return resp->text();
}

void Node::setText(const std::string& text) {
    auto resp = client_->setNodeText(node_id_, text);
    if (!resp->success()) {
        throw FreeplaneOperationError(
            !resp->error_message().empty() ? resp->error_message() : "Failed to set node text");
    }
}

// -- hierarchy -----------------------------------------------------------

std::shared_ptr<Node> Node::addChild(const std::string& text, const std::string& style) {
    (void)style;  // Style is not directly supported via gRPC, uses default
    
    auto resp = client_->createChild(text, node_id_);
    auto node = std::make_shared<Node>(client_, resp->node_id(), mindmap_);
    
    // Update the mindmap's node_id to point to the new child
    if (mindmap_) {
        // MindMap doesn't expose mutable node_id, but we can note this
    }
    
    return node;
}

std::vector<std::shared_ptr<Node>> Node::children() {
    auto resp = client_->listChildNodes(node_id_);
    if (!resp->success()) {
        throw FreeplaneOperationError(
            !resp->error_message().empty() ? resp->error_message() : "Failed to list children");
    }
    
    std::vector<std::shared_ptr<Node>> result;
    for (const auto& child : resp->children()) {
        result.push_back(std::make_shared<Node>(client_, child.node_id(), mindmap_));
    }
    return result;
}

std::shared_ptr<Node> Node::parent() {
    auto resp = client_->getParentNode(node_id_);
    if (!resp->success() || resp->parent_node_id().empty()) {
        throw NodeNotFoundError("Node " + node_id_ + " has no parent (is root)");
    }
    return std::make_shared<Node>(client_, resp->parent_node_id(), mindmap_);
}

bool Node::deleteNode() {
    auto resp = client_->deleteChild(node_id_);
    return resp->success();
}

bool Node::move(const std::string& newParentId) {
    auto resp = client_->moveNode(node_id_, newParentId);
    if (!resp->success()) {
        throw FreeplaneOperationError(
            !resp->error_message().empty() ? resp->error_message() : "Failed to move node");
    }
    return resp->success();
}

// -- styling -------------------------------------------------------------

bool Node::setStyle(const std::string& style) {
    // Use Groovy to set style
    std::ostringstream groovy;
    groovy << "def node = model.getNode('" << node_id_ << "');"
           << "node.style = model.getStyleLib().getStyle('" << style << "');";
    
    auto result = client_->groovy(groovy.str());
    return result.find("Error") == std::string::npos;
}

bool Node::setColor(int32_t red, int32_t green, int32_t blue, int32_t alpha) {
    auto resp = client_->nodeColorSet(node_id_, red, green, blue, alpha);
    return resp->success();
}

bool Node::setBackgroundColor(int32_t red, int32_t green, int32_t blue, int32_t alpha) {
    auto resp = client_->nodeBackgroundColorSet(node_id_, red, green, blue, alpha);
    return resp->success();
}

// -- notes ---------------------------------------------------------------

std::pair<bool, std::string> Node::getNote() {
    auto resp = client_->getNodeNote(node_id_);
    if (!resp->success()) {
        throw FreeplaneOperationError(
            !resp->error_message().empty() ? resp->error_message() : "Failed to get node note");
    }
    return {resp->has_note(), resp->note()};
}

void Node::setNote(const std::string& note) {
    auto resp = client_->nodeNoteSet(node_id_, note);
    if (!resp->success()) {
        throw FreeplaneOperationError("Failed to set node note");
    }
}

// -- attributes ----------------------------------------------------------

bool Node::setAttribute(const std::string& name, const std::string& value) {
    auto resp = client_->nodeAttributeAdd(node_id_, name, value);
    return resp->success();
}

bool Node::setAttributes(const std::vector<std::pair<std::string, std::string>>& attrs) {
    for (const auto& attr : attrs) {
        if (!setAttribute(attr.first, attr.second)) {
            return false;
        }
    }
    return true;
}

// -- links ---------------------------------------------------------------

std::vector<std::string> Node::getLinks() {
    auto resp = client_->getNodeLink(node_id_);
    if (!resp->success()) {
        throw FreeplaneOperationError(
            !resp->error_message().empty() ? resp->error_message() : "Failed to get node link");
    }
    
    std::vector<std::string> result;
    if (resp->has_link()) {
        result.push_back(resp->link());
    }
    return result;
}

void Node::setLinks(const std::vector<std::string>& links) {
    for (const auto& link : links) {
        client_->nodeLinkSet(node_id_, link);
    }
}

// -- tags ----------------------------------------------------------------

bool Node::setTags(const std::vector<std::string>& tags) {
    auto resp = client_->nodeTagSet(node_id_, tags);
    return resp->success();
}

bool Node::addTags(const std::vector<std::string>& tags) {
    auto resp = client_->nodeTagAdd(node_id_, tags);
    return resp->success();
}

// -- icons ---------------------------------------------------------------

bool Node::addIcon(const std::string& iconName) {
    auto resp = client_->nodeAddIcon(node_id_, iconName);
    return resp->success();
}

// -- state ---------------------------------------------------------------

bool Node::getFolded() {
    // Use Groovy to get folded state
    std::ostringstream groovy;
    groovy << "def node = model.getNode('" << node_id_ << "');"
           << "node.isFolded()";
    
    auto result = client_->groovy(groovy.str());
    return result.find("true") != std::string::npos ||
           result.find("True") != std::string::npos ||
           result.find("1") != std::string::npos;
}

bool Node::setFolded(bool folded) {
    // Use Groovy to set folded state
    std::ostringstream groovy;
    groovy << "def node = model.getNode('" << node_id_ << "');"
           << (folded ? "node.fold()" : "node.unfold()");
    
    auto result = client_->groovy(groovy.str());
    return result.find("Error") == std::string::npos;
}

// -- actions -------------------------------------------------------------

bool Node::select() {
    return client_->focusNode(node_id_);
}

bool Node::center() {
    return client_->focusNode(node_id_);
}

void Node::refresh() {
    // Reload by fetching the text
    getText();
}

std::string Node::toString() const {
    std::ostringstream oss;
    oss << "Node(id=" << node_id_ << ")";
    try {
        oss << ", text=" << getText();
    } catch (const FreeplaneOperationError&) {
        oss << ", text=<unreadable>";
    }
    return oss.str();
}

}  // namespace grpc
}  // namespace freeplane
