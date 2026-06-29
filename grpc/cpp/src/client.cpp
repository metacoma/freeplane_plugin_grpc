#include "client.h"
#include "mindmap.h"

#include <sstream>

namespace freeplane {
namespace grpc {

// =====================================================================
// FreeplaneClient implementation
// =====================================================================

FreeplaneClient::FreeplaneClient(const std::string& host, int port)
    : host_(host), port_(port), connected_(false) {
}

FreeplaneClient::~FreeplaneClient() {
    close();
}

FreeplaneClient::FreeplaneClient(FreeplaneClient&& other) noexcept
    : channel_(std::move(other.channel_)),
      stub_(std::move(other.stub_)),
      host_(std::move(other.host_)),
      port_(other.port_),
      connected_(other.connected_) {
    other.connected_ = false;
    other.port_ = 0;
}

FreeplaneClient& FreeplaneClient::operator=(FreeplaneClient&& other) noexcept {
    if (this != &other) {
        close();
        channel_ = std::move(other.channel_);
        stub_ = std::move(other.stub_);
        host_ = std::move(other.host_);
        port_ = other.port_;
        connected_ = other.connected_;
        other.connected_ = false;
        other.port_ = 0;
    }
    return *this;
}

void FreeplaneClient::connect() {
    if (connected_) {
        return;  // Already connected
    }
    
    channel_ = ::grpc::CreateChannel(host_ + ":" + std::to_string(port_),
                                   ::grpc::InsecureChannelCredentials());
    stub_ = freeplane::Freeplane::NewStub(channel_);
    connected_ = true;
}

void FreeplaneClient::close() {
    if (connected_) {
        channel_ = nullptr;
        stub_ = nullptr;
        connected_ = false;
    }
}

std::shared_ptr<freeplane::Freeplane::Stub> FreeplaneClient::getStub() {
    if (!connected_) {
        throw FreeplaneConnectionError(
            "Not connected. Call connect() or use as a scoped connection.");
    }
    return stub_;
}

// =====================================================================
// High-level operations
// =====================================================================

std::shared_ptr<MindMap> FreeplaneClient::currentMap() {
    auto resp = getCurrentNode();
    if (!resp->success()) {
        throw FreeplaneOperationError(
            "No map currently open");
    }
    return std::make_shared<MindMap>(this, resp->map_id(), resp->node_id());
}

std::shared_ptr<MindMap> FreeplaneClient::selectedMap() {
    return currentMap();
}

std::shared_ptr<MindMap> FreeplaneClient::openMap(const std::string& filePath) {
    auto resp = openMapRpc(filePath);
    if (!resp->success()) {
        throw FreeplaneOperationError("Failed to open map: " + filePath);
    }
    return currentMap();
}

std::string FreeplaneClient::getMapToJson() {
    auto resp = mindMapToJson();
    if (!resp->success()) {
        throw FreeplaneOperationError("Failed to export map to JSON");
    }
    return resp->json();
}

bool FreeplaneClient::mindMapFromJson(const std::string& jsonData) {
    auto resp = mindMapFromJsonRpc(jsonData);
    if (!resp->success()) {
        throw FreeplaneOperationError("Failed to import map from JSON");
    }
    return resp->success();
}

std::string FreeplaneClient::groovy(const std::string& code) {
    auto resp = groovyRpc(code);
    if (!resp->success()) {
        throw FreeplaneOperationError(
            !resp->error_message().empty() ? resp->error_message() : "Groovy execution failed");
    }
    return resp->result();
}

bool FreeplaneClient::focusNode(const std::string& nodeId) {
    auto resp = focusNodeRpc(nodeId);
    if (!resp->success()) {
        throw FreeplaneOperationError("Failed to focus node: " + nodeId);
    }
    return resp->success();
}

bool FreeplaneClient::setStatusInfo(const std::string& info) {
    auto resp = statusInfoSet(info);
    if (!resp->success()) {
        throw FreeplaneOperationError("Failed to set status info");
    }
    return resp->success();
}

// =====================================================================
// Raw RPC wrappers (27 methods)
// =====================================================================

std::shared_ptr<freeplane::CreateChildResponse> FreeplaneClient::createChild(
    const std::string& name, const std::string& parent_node_id) {
    
    freeplane::CreateChildRequest request;
    request.set_name(name);
    request.set_parent_node_id(parent_node_id);
    
    freeplane::CreateChildResponse response;
    auto stub = getStub();
    
    ::grpc::ClientContext context;
    ::grpc::Status status = stub->CreateChild(&context, request, &response);
    
    if (!status.ok()) {
        throw FreeplaneConnectionError(
            "gRPC call failed: " + status.error_message());
    }
    
    auto result = std::make_shared<freeplane::CreateChildResponse>();
    *result = response;
    return result;
}

std::shared_ptr<freeplane::DeleteChildResponse> FreeplaneClient::deleteChild(
    const std::string& node_id) {
    
    freeplane::DeleteChildRequest request;
    request.set_node_id(node_id);
    
    freeplane::DeleteChildResponse response;
    auto stub = getStub();
    
    ::grpc::ClientContext context;
    ::grpc::Status status = stub->DeleteChild(&context, request, &response);
    
    if (!status.ok()) {
        throw FreeplaneConnectionError(
            "gRPC call failed: " + status.error_message());
    }
    
    auto result = std::make_shared<freeplane::DeleteChildResponse>();
    *result = response;
    return result;
}

std::shared_ptr<freeplane::NodeAttributeAddResponse> FreeplaneClient::nodeAttributeAdd(
    const std::string& node_id, const std::string& attribute_name, 
    const std::string& attribute_value) {
    
    freeplane::NodeAttributeAddRequest request;
    request.set_node_id(node_id);
    request.set_attribute_name(attribute_name);
    request.set_attribute_value(attribute_value);
    
    freeplane::NodeAttributeAddResponse response;
    auto stub = getStub();
    
    ::grpc::ClientContext context;
    ::grpc::Status status = stub->NodeAttributeAdd(&context, request, &response);
    
    if (!status.ok()) {
        throw FreeplaneConnectionError(
            "gRPC call failed: " + status.error_message());
    }
    
    auto result = std::make_shared<freeplane::NodeAttributeAddResponse>();
    *result = response;
    return result;
}

std::shared_ptr<freeplane::NodeLinkSetResponse> FreeplaneClient::nodeLinkSet(
    const std::string& node_id, const std::string& link) {
    
    freeplane::NodeLinkSetRequest request;
    request.set_node_id(node_id);
    request.set_link(link);
    
    freeplane::NodeLinkSetResponse response;
    auto stub = getStub();
    
    ::grpc::ClientContext context;
    ::grpc::Status status = stub->NodeLinkSet(&context, request, &response);
    
    if (!status.ok()) {
        throw FreeplaneConnectionError(
            "gRPC call failed: " + status.error_message());
    }
    
    auto result = std::make_shared<freeplane::NodeLinkSetResponse>();
    *result = response;
    return result;
}

std::shared_ptr<freeplane::NodeDetailsSetResponse> FreeplaneClient::nodeDetailsSet(
    const std::string& node_id, const std::string& details) {
    
    freeplane::NodeDetailsSetRequest request;
    request.set_node_id(node_id);
    request.set_details(details);
    
    freeplane::NodeDetailsSetResponse response;
    auto stub = getStub();
    
    ::grpc::ClientContext context;
    ::grpc::Status status = stub->NodeDetailsSet(&context, request, &response);
    
    if (!status.ok()) {
        throw FreeplaneConnectionError(
            "gRPC call failed: " + status.error_message());
    }
    
    auto result = std::make_shared<freeplane::NodeDetailsSetResponse>();
    *result = response;
    return result;
}

std::shared_ptr<freeplane::NodeNoteSetResponse> FreeplaneClient::nodeNoteSet(
    const std::string& node_id, const std::string& note) {
    
    freeplane::NodeNoteSetRequest request;
    request.set_node_id(node_id);
    request.set_note(note);
    
    freeplane::NodeNoteSetResponse response;
    auto stub = getStub();
    
    ::grpc::ClientContext context;
    ::grpc::Status status = stub->NodeNoteSet(&context, request, &response);
    
    if (!status.ok()) {
        throw FreeplaneConnectionError(
            "gRPC call failed: " + status.error_message());
    }
    
    auto result = std::make_shared<freeplane::NodeNoteSetResponse>();
    *result = response;
    return result;
}

std::shared_ptr<freeplane::NodeTagSetResponse> FreeplaneClient::nodeTagSet(
    const std::string& node_id, const std::vector<std::string>& tags) {
    
    freeplane::NodeTagSetRequest request;
    request.set_node_id(node_id);
    for (const auto& tag : tags) {
        request.add_tags(tag);
    }
    
    freeplane::NodeTagSetResponse response;
    auto stub = getStub();
    
    ::grpc::ClientContext context;
    ::grpc::Status status = stub->NodeTagSet(&context, request, &response);
    
    if (!status.ok()) {
        throw FreeplaneConnectionError(
            "gRPC call failed: " + status.error_message());
    }
    
    auto result = std::make_shared<freeplane::NodeTagSetResponse>();
    *result = response;
    return result;
}

std::shared_ptr<freeplane::NodeTagAddResponse> FreeplaneClient::nodeTagAdd(
    const std::string& node_id, const std::vector<std::string>& tags) {
    
    freeplane::NodeTagAddRequest request;
    request.set_node_id(node_id);
    for (const auto& tag : tags) {
        request.add_tags(tag);
    }
    
    freeplane::NodeTagAddResponse response;
    auto stub = getStub();
    
    ::grpc::ClientContext context;
    ::grpc::Status status = stub->NodeTagAdd(&context, request, &response);
    
    if (!status.ok()) {
        throw FreeplaneConnectionError(
            "gRPC call failed: " + status.error_message());
    }
    
    auto result = std::make_shared<freeplane::NodeTagAddResponse>();
    *result = response;
    return result;
}

std::shared_ptr<freeplane::NodeConnectResponse> FreeplaneClient::nodeConnect(
    const std::string& source_node_id, const std::string& target_node_id,
    const std::string& relationship) {
    
    freeplane::NodeConnectRequest request;
    request.set_source_node_id(source_node_id);
    request.set_target_node_id(target_node_id);
    request.set_relationship(relationship);
    
    freeplane::NodeConnectResponse response;
    auto stub = getStub();
    
    ::grpc::ClientContext context;
    ::grpc::Status status = stub->NodeConnect(&context, request, &response);
    
    if (!status.ok()) {
        throw FreeplaneConnectionError(
            "gRPC call failed: " + status.error_message());
    }
    
    auto result = std::make_shared<freeplane::NodeConnectResponse>();
    *result = response;
    return result;
}

std::shared_ptr<freeplane::NodeAddIconResponse> FreeplaneClient::nodeAddIcon(
    const std::string& node_id, const std::string& icon_name) {
    
    freeplane::NodeAddIconRequest request;
    request.set_node_id(node_id);
    request.set_icon_name(icon_name);
    
    freeplane::NodeAddIconResponse response;
    auto stub = getStub();
    
    ::grpc::ClientContext context;
    ::grpc::Status status = stub->NodeAddIcon(&context, request, &response);
    
    if (!status.ok()) {
        throw FreeplaneConnectionError(
            "gRPC call failed: " + status.error_message());
    }
    
    auto result = std::make_shared<freeplane::NodeAddIconResponse>();
    *result = response;
    return result;
}

std::shared_ptr<freeplane::GroovyResponse> FreeplaneClient::groovyRpc(
    const std::string& groovy_code) {
    
    freeplane::GroovyRequest request;
    request.set_groovy_code(groovy_code);
    
    freeplane::GroovyResponse response;
    auto stub = getStub();
    
    ::grpc::ClientContext context;
    ::grpc::Status status = stub->Groovy(&context, request, &response);
    
    if (!status.ok()) {
        throw FreeplaneConnectionError(
            "gRPC call failed: " + status.error_message());
    }
    
    auto result = std::make_shared<freeplane::GroovyResponse>();
    *result = response;
    return result;
}

std::shared_ptr<freeplane::NodeColorSetResponse> FreeplaneClient::nodeColorSet(
    const std::string& node_id, int32_t red, int32_t green, 
    int32_t blue, int32_t alpha) {
    
    freeplane::NodeColorSetRequest request;
    request.set_node_id(node_id);
    request.set_red(red);
    request.set_green(green);
    request.set_blue(blue);
    request.set_alpha(alpha);
    
    freeplane::NodeColorSetResponse response;
    auto stub = getStub();
    
    ::grpc::ClientContext context;
    ::grpc::Status status = stub->NodeColorSet(&context, request, &response);
    
    if (!status.ok()) {
        throw FreeplaneConnectionError(
            "gRPC call failed: " + status.error_message());
    }
    
    auto result = std::make_shared<freeplane::NodeColorSetResponse>();
    *result = response;
    return result;
}

std::shared_ptr<freeplane::NodeBackgroundColorSetResponse> FreeplaneClient::nodeBackgroundColorSet(
    const std::string& node_id, int32_t red, int32_t green, 
    int32_t blue, int32_t alpha) {
    
    freeplane::NodeBackgroundColorSetRequest request;
    request.set_node_id(node_id);
    request.set_red(red);
    request.set_green(green);
    request.set_blue(blue);
    request.set_alpha(alpha);
    
    freeplane::NodeBackgroundColorSetResponse response;
    auto stub = getStub();
    
    ::grpc::ClientContext context;
    ::grpc::Status status = stub->NodeBackgroundColorSet(&context, request, &response);
    
    if (!status.ok()) {
        throw FreeplaneConnectionError(
            "gRPC call failed: " + status.error_message());
    }
    
    auto result = std::make_shared<freeplane::NodeBackgroundColorSetResponse>();
    *result = response;
    return result;
}

std::shared_ptr<freeplane::StatusInfoSetResponse> FreeplaneClient::statusInfoSet(
    const std::string& status_info) {
    
    freeplane::StatusInfoSetRequest request;
    request.set_statusinfo(status_info);
    
    freeplane::StatusInfoSetResponse response;
    auto stub = getStub();
    
    ::grpc::ClientContext context;
    ::grpc::Status status = stub->StatusInfoSet(&context, request, &response);
    
    if (!status.ok()) {
        throw FreeplaneConnectionError(
            "gRPC call failed: " + status.error_message());
    }
    
    auto result = std::make_shared<freeplane::StatusInfoSetResponse>();
    *result = response;
    return result;
}

std::shared_ptr<freeplane::TextFSMResponse> FreeplaneClient::textFSM(const std::string& json) {
    
    freeplane::TextFSMRequest request;
    request.set_json(json);
    
    freeplane::TextFSMResponse response;
    auto stub = getStub();
    
    ::grpc::ClientContext context;
    ::grpc::Status status = stub->TextFSM(&context, request, &response);
    
    if (!status.ok()) {
        throw FreeplaneConnectionError(
            "gRPC call failed: " + status.error_message());
    }
    
    auto result = std::make_shared<freeplane::TextFSMResponse>();
    *result = response;
    return result;
}

std::shared_ptr<freeplane::MindMapFromJSONResponse> FreeplaneClient::mindMapFromJsonRpc(
    const std::string& json) {
    
    freeplane::MindMapFromJSONRequest request;
    request.set_json(json);
    
    freeplane::MindMapFromJSONResponse response;
    auto stub = getStub();
    
    ::grpc::ClientContext context;
    ::grpc::Status status = stub->MindMapFromJSON(&context, request, &response);
    
    if (!status.ok()) {
        throw FreeplaneConnectionError(
            "gRPC call failed: " + status.error_message());
    }
    
    auto result = std::make_shared<freeplane::MindMapFromJSONResponse>();
    *result = response;
    return result;
}

std::shared_ptr<freeplane::MindMapToJSONResponse> FreeplaneClient::mindMapToJson() {
    
    freeplane::MindMapToJSONRequest request;
    
    freeplane::MindMapToJSONResponse response;
    auto stub = getStub();
    
    ::grpc::ClientContext context;
    ::grpc::Status status = stub->MindMapToJSON(&context, request, &response);
    
    if (!status.ok()) {
        throw FreeplaneConnectionError(
            "gRPC call failed: " + status.error_message());
    }
    
    auto result = std::make_shared<freeplane::MindMapToJSONResponse>();
    *result = response;
    return result;
}

std::shared_ptr<freeplane::GetCurrentNodeResponse> FreeplaneClient::getCurrentNode() {
    
    freeplane::GetCurrentNodeRequest request;
    
    freeplane::GetCurrentNodeResponse response;
    auto stub = getStub();
    
    ::grpc::ClientContext context;
    ::grpc::Status status = stub->GetCurrentNode(&context, request, &response);
    
    if (!status.ok()) {
        throw FreeplaneConnectionError(
            "gRPC call failed: " + status.error_message());
    }
    
    auto result = std::make_shared<freeplane::GetCurrentNodeResponse>();
    *result = response;
    return result;
}

std::shared_ptr<freeplane::OpenMapResponse> FreeplaneClient::openMapRpc(
    const std::string& file_path) {
    
    freeplane::OpenMapRequest request;
    request.set_file_path(file_path);
    
    freeplane::OpenMapResponse response;
    auto stub = getStub();
    
    ::grpc::ClientContext context;
    ::grpc::Status status = stub->OpenMap(&context, request, &response);
    
    if (!status.ok()) {
        throw FreeplaneConnectionError(
            "gRPC call failed: " + status.error_message());
    }
    
    auto result = std::make_shared<freeplane::OpenMapResponse>();
    *result = response;
    return result;
}

std::shared_ptr<freeplane::FocusNodeResponse> FreeplaneClient::focusNodeRpc(
    const std::string& node_id) {
    
    freeplane::FocusNodeRequest request;
    request.set_node_id(node_id);
    
    freeplane::FocusNodeResponse response;
    auto stub = getStub();
    
    ::grpc::ClientContext context;
    ::grpc::Status status = stub->FocusNode(&context, request, &response);
    
    if (!status.ok()) {
        throw FreeplaneConnectionError(
            "gRPC call failed: " + status.error_message());
    }
    
    auto result = std::make_shared<freeplane::FocusNodeResponse>();
    *result = response;
    return result;
}

std::shared_ptr<freeplane::GetNodeTextResponse> FreeplaneClient::getNodeText(
    const std::string& node_id) {
    
    freeplane::GetNodeTextRequest request;
    request.set_node_id(node_id);
    
    freeplane::GetNodeTextResponse response;
    auto stub = getStub();
    
    ::grpc::ClientContext context;
    ::grpc::Status status = stub->GetNodeText(&context, request, &response);
    
    if (!status.ok()) {
        throw FreeplaneConnectionError(
            "gRPC call failed: " + status.error_message());
    }
    
    auto result = std::make_shared<freeplane::GetNodeTextResponse>();
    *result = response;
    return result;
}

std::shared_ptr<freeplane::GetParentNodeResponse> FreeplaneClient::getParentNode(
    const std::string& node_id) {
    
    freeplane::GetParentNodeRequest request;
    request.set_node_id(node_id);
    
    freeplane::GetParentNodeResponse response;
    auto stub = getStub();
    
    ::grpc::ClientContext context;
    ::grpc::Status status = stub->GetParentNode(&context, request, &response);
    
    if (!status.ok()) {
        throw FreeplaneConnectionError(
            "gRPC call failed: " + status.error_message());
    }
    
    auto result = std::make_shared<freeplane::GetParentNodeResponse>();
    *result = response;
    return result;
}

std::shared_ptr<freeplane::ListChildNodesResponse> FreeplaneClient::listChildNodes(
    const std::string& node_id) {
    
    freeplane::ListChildNodesRequest request;
    request.set_node_id(node_id);
    
    freeplane::ListChildNodesResponse response;
    auto stub = getStub();
    
    ::grpc::ClientContext context;
    ::grpc::Status status = stub->ListChildNodes(&context, request, &response);
    
    if (!status.ok()) {
        throw FreeplaneConnectionError(
            "gRPC call failed: " + status.error_message());
    }
    
    auto result = std::make_shared<freeplane::ListChildNodesResponse>();
    *result = response;
    return result;
}

std::shared_ptr<freeplane::GetNodeNoteResponse> FreeplaneClient::getNodeNote(
    const std::string& node_id) {
    
    freeplane::GetNodeNoteRequest request;
    request.set_node_id(node_id);
    
    freeplane::GetNodeNoteResponse response;
    auto stub = getStub();
    
    ::grpc::ClientContext context;
    ::grpc::Status status = stub->GetNodeNote(&context, request, &response);
    
    if (!status.ok()) {
        throw FreeplaneConnectionError(
            "gRPC call failed: " + status.error_message());
    }
    
    auto result = std::make_shared<freeplane::GetNodeNoteResponse>();
    *result = response;
    return result;
}

std::shared_ptr<freeplane::GetNodeLinkResponse> FreeplaneClient::getNodeLink(
    const std::string& node_id) {
    
    freeplane::GetNodeLinkRequest request;
    request.set_node_id(node_id);
    
    freeplane::GetNodeLinkResponse response;
    auto stub = getStub();
    
    ::grpc::ClientContext context;
    ::grpc::Status status = stub->GetNodeLink(&context, request, &response);
    
    if (!status.ok()) {
        throw FreeplaneConnectionError(
            "gRPC call failed: " + status.error_message());
    }
    
    auto result = std::make_shared<freeplane::GetNodeLinkResponse>();
    *result = response;
    return result;
}

std::shared_ptr<freeplane::SetNodeTextResponse> FreeplaneClient::setNodeText(
    const std::string& node_id, const std::string& text) {
    
    freeplane::SetNodeTextRequest request;
    request.set_node_id(node_id);
    request.set_text(text);
    
    freeplane::SetNodeTextResponse response;
    auto stub = getStub();
    
    ::grpc::ClientContext context;
    ::grpc::Status status = stub->SetNodeText(&context, request, &response);
    
    if (!status.ok()) {
        throw FreeplaneConnectionError(
            "gRPC call failed: " + status.error_message());
    }
    
    auto result = std::make_shared<freeplane::SetNodeTextResponse>();
    *result = response;
    return result;
}

std::shared_ptr<freeplane::MoveNodeResponse> FreeplaneClient::moveNode(
    const std::string& node_id, const std::string& new_parent_node_id) {
    
    freeplane::MoveNodeRequest request;
    request.set_node_id(node_id);
    request.set_new_parent_node_id(new_parent_node_id);
    
    freeplane::MoveNodeResponse response;
    auto stub = getStub();
    
    ::grpc::ClientContext context;
    ::grpc::Status status = stub->MoveNode(&context, request, &response);
    
    if (!status.ok()) {
        throw FreeplaneConnectionError(
            "gRPC call failed: " + status.error_message());
    }
    
    auto result = std::make_shared<freeplane::MoveNodeResponse>();
    *result = response;
    return result;
}

}  // namespace grpc
}  // namespace freeplane
