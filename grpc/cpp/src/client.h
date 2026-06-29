#ifndef FREEPLANE_GRPC_CLIENT_H
#define FREEPLANE_GRPC_CLIENT_H

#include <memory>
#include <string>
#include <functional>

#include <grpcpp/grpcpp.h>

#include "freeplane/freeplane.pb.h"
#include "freeplane/freeplane.grpc.pb.h"
#include "error.h"

namespace freeplane {
namespace grpc {

class MindMap;

/**
 * Client for interacting with a Freeplane gRPC server.
 *
 * Provides a high-level interface to the Freeplane mind-map application
 * through its gRPC plugin.
 *
 * Typical usage:
 * @code
 * freeplane::grpc::FreeplaneClient client("127.0.0.1", 50051);
 * client.connect();
 * auto mindmap = client.currentMap();
 * auto root = mindmap->root();
 * std::cout << root->getText() << std::endl;
 * client.close();
 * @endcode
 */
class FreeplaneClient {
public:
    /**
     * Create a FreeplaneClient.
     * @param host Hostname or IP address of the Freeplane gRPC server.
     * @param port Port of the Freeplane gRPC server.
     */
    FreeplaneClient(const std::string& host = "127.0.0.1", int port = 50051);
    
    ~FreeplaneClient();
    
    // Non-copyable
    FreeplaneClient(const FreeplaneClient&) = delete;
    FreeplaneClient& operator=(const FreeplaneClient&) = delete;
    
    // Movable
    FreeplaneClient(FreeplaneClient&& other) noexcept;
    FreeplaneClient& operator=(FreeplaneClient&& other) noexcept;
    
    /**
     * Connect to the Freeplane gRPC server.
     * @throws FreeplaneConnectionError if connection fails.
     */
    void connect();
    
    /**
     * Close the connection to the Freeplane gRPC server.
     */
    void close();
    
    /**
     * Get the server hostname.
     */
    const std::string& host() const { return host_; }
    
    /**
     * Get the server port.
     */
    int port() const { return port_; }
    
    // =====================================================================
    // High-level operations (7 methods matching other clients)
    // =====================================================================
    
    /**
     * Get the currently open/active mind map.
     */
    std::shared_ptr<MindMap> currentMap();
    
    /**
     * Get the current mind map as a context rooted at the selected node.
     * Convenience wrapper around currentMap().
     */
    std::shared_ptr<MindMap> selectedMap();
    
    /**
     * Open a mind map file on the Freeplane server.
     * @param filePath Path to the .mm file to open.
     */
    std::shared_ptr<MindMap> openMap(const std::string& filePath);
    
    /**
     * Export the current mind map as JSON.
     */
    std::string getMapToJson();
    
    /**
     * Import a mind map from JSON data.
     * @param jsonData JSON string representing a mind map.
     */
    bool mindMapFromJson(const std::string& jsonData);
    
    /**
     * Execute Groovy code on the Freeplane server.
     * @param code Groovy script to execute.
     */
    std::string groovy(const std::string& code);
    
    /**
     * Focus (select) a node in the Freeplane UI.
     * @param nodeId ID of the node to focus.
     */
    bool focusNode(const std::string& nodeId);
    
    /**
     * Set the status bar info in Freeplane.
     * @param info Status text to display.
     */
    bool setStatusInfo(const std::string& info);
    
    // =====================================================================
    // Raw RPC wrappers (27 methods matching the proto service)
    // =====================================================================
    
    std::shared_ptr<freeplane::CreateChildResponse> createChild(
        const std::string& name, const std::string& parent_node_id);
    
    std::shared_ptr<freeplane::DeleteChildResponse> deleteChild(
        const std::string& node_id);
    
    std::shared_ptr<freeplane::NodeAttributeAddResponse> nodeAttributeAdd(
        const std::string& node_id, const std::string& attribute_name, 
        const std::string& attribute_value);
    
    std::shared_ptr<freeplane::NodeLinkSetResponse> nodeLinkSet(
        const std::string& node_id, const std::string& link);
    
    std::shared_ptr<freeplane::NodeDetailsSetResponse> nodeDetailsSet(
        const std::string& node_id, const std::string& details);
    
    std::shared_ptr<freeplane::NodeNoteSetResponse> nodeNoteSet(
        const std::string& node_id, const std::string& note);
    
    std::shared_ptr<freeplane::NodeTagSetResponse> nodeTagSet(
        const std::string& node_id, const std::vector<std::string>& tags);
    
    std::shared_ptr<freeplane::NodeTagAddResponse> nodeTagAdd(
        const std::string& node_id, const std::vector<std::string>& tags);
    
    std::shared_ptr<freeplane::NodeConnectResponse> nodeConnect(
        const std::string& source_node_id, const std::string& target_node_id,
        const std::string& relationship);
    
    std::shared_ptr<freeplane::NodeAddIconResponse> nodeAddIcon(
        const std::string& node_id, const std::string& icon_name);
    
    std::shared_ptr<freeplane::GroovyResponse> groovyRpc(
        const std::string& groovy_code);
    
    std::shared_ptr<freeplane::NodeColorSetResponse> nodeColorSet(
        const std::string& node_id, int32_t red, int32_t green, 
        int32_t blue, int32_t alpha);
    
    std::shared_ptr<freeplane::NodeBackgroundColorSetResponse> nodeBackgroundColorSet(
        const std::string& node_id, int32_t red, int32_t green, 
        int32_t blue, int32_t alpha);
    
    std::shared_ptr<freeplane::StatusInfoSetResponse> statusInfoSet(
        const std::string& status_info);
    
    std::shared_ptr<freeplane::TextFSMResponse> textFSM(const std::string& json);
    
    std::shared_ptr<freeplane::MindMapFromJSONResponse> mindMapFromJsonRpc(
        const std::string& json);
    
    std::shared_ptr<freeplane::MindMapToJSONResponse> mindMapToJson();
    
    std::shared_ptr<freeplane::GetCurrentNodeResponse> getCurrentNode();
    
    std::shared_ptr<freeplane::OpenMapResponse> openMapRpc(const std::string& file_path);
    
    std::shared_ptr<freeplane::FocusNodeResponse> focusNodeRpc(
        const std::string& node_id);
    
    std::shared_ptr<freeplane::GetNodeTextResponse> getNodeText(
        const std::string& node_id);
    
    std::shared_ptr<freeplane::GetParentNodeResponse> getParentNode(
        const std::string& node_id);
    
    std::shared_ptr<freeplane::ListChildNodesResponse> listChildNodes(
        const std::string& node_id);
    
    std::shared_ptr<freeplane::GetNodeNoteResponse> getNodeNote(
        const std::string& node_id);
    
    std::shared_ptr<freeplane::GetNodeLinkResponse> getNodeLink(
        const std::string& node_id);
    
    std::shared_ptr<freeplane::SetNodeTextResponse> setNodeText(
        const std::string& node_id, const std::string& text);
    
    std::shared_ptr<freeplane::MoveNodeResponse> moveNode(
        const std::string& node_id, const std::string& new_parent_node_id);

private:
    std::shared_ptr<freeplane::Freeplane::Stub> getStub();
    
    std::shared_ptr<::grpc::Channel> channel_;
    std::shared_ptr<freeplane::Freeplane::Stub> stub_;
    
    std::string host_;
    int port_;
    bool connected_;
};

}  // namespace grpc
}  // namespace freeplane

#endif  // FREEPLANE_GRPC_CLIENT_H
