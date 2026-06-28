//! FreeplaneClient — the main entry point for interacting with Freeplane via gRPC.
//!
//! Provides a trait-based abstraction (`FreeplaneStub`) for mockability,
//! a concrete `FreeplaneClient` that wraps the tonic-generated client,
//! and high-level operations matching the Python/Node.js clients.

use async_trait::async_trait;
use std::future::Future;
use thiserror::Error;
use tonic::transport::Channel;

use crate::error::{
    is_connection_error, status_to_connection_error, status_to_error, FreeplaneConnectionError,
    FreeplaneOperationError, NodeNotFoundError,
};
use crate::generated::freeplane::{
    freeplane_client::FreeplaneClient as TonicClient,
    CreateChildRequest, CreateChildResponse, DeleteChildRequest, DeleteChildResponse,
    FocusNodeRequest, FocusNodeResponse, GetCurrentNodeRequest, GetCurrentNodeResponse,
    GroovyRequest, GroovyResponse, MindMapFromJsonRequest, MindMapFromJsonResponse,
    MindMapToJsonRequest, MindMapToJsonResponse, NodeAddIconRequest, NodeAddIconResponse,
    NodeAttributeAddRequest, NodeAttributeAddResponse, NodeBackgroundColorSetRequest,
    NodeBackgroundColorSetResponse, NodeColorSetRequest, NodeColorSetResponse, NodeConnectRequest,
    NodeConnectResponse, NodeDetailsSetRequest, NodeDetailsSetResponse, NodeLinkSetRequest,
    NodeLinkSetResponse, NodeNoteSetRequest, NodeNoteSetResponse, NodeTagAddRequest,
    NodeTagAddResponse, NodeTagSetRequest, NodeTagSetResponse, OpenMapRequest, OpenMapResponse,
    StatusInfoSetRequest, StatusInfoSetResponse, TextFsmRequest, TextFsmResponse,
};

// ─── Node inspection / manipulation request/response types ──────────────────

use crate::generated::freeplane::{
    GetNodeTextRequest, GetNodeTextResponse, GetParentNodeRequest, GetParentNodeResponse,
    ListChildNodesRequest, ListChildNodesResponse, GetNodeNoteRequest, GetNodeNoteResponse,
    GetNodeLinkRequest, GetNodeLinkResponse, SetNodeTextRequest, SetNodeTextResponse,
    MoveNodeRequest, MoveNodeResponse,
};

// ─── Trait abstraction for mockability ──────────────────────────────────────

/// Trait abstracting all 27 gRPC RPC methods.
///
/// This trait enables unit testing via `mockall` — tests can create a
/// `MockFreeplaneStub` and set expectations on individual methods.
#[async_trait]
pub trait FreeplaneStub: Send + Sync {
    async fn create_child(&self, req: CreateChildRequest) -> Result<CreateChildResponse, tonic::Status>;
    async fn delete_child(&self, req: DeleteChildRequest) -> Result<DeleteChildResponse, tonic::Status>;
    async fn node_attribute_add(&self, req: NodeAttributeAddRequest) -> Result<NodeAttributeAddResponse, tonic::Status>;
    async fn node_link_set(&self, req: NodeLinkSetRequest) -> Result<NodeLinkSetResponse, tonic::Status>;
    async fn node_details_set(&self, req: NodeDetailsSetRequest) -> Result<NodeDetailsSetResponse, tonic::Status>;
    async fn node_note_set(&self, req: NodeNoteSetRequest) -> Result<NodeNoteSetResponse, tonic::Status>;
    async fn node_tag_set(&self, req: NodeTagSetRequest) -> Result<NodeTagSetResponse, tonic::Status>;
    async fn node_tag_add(&self, req: NodeTagAddRequest) -> Result<NodeTagAddResponse, tonic::Status>;
    async fn node_connect(&self, req: NodeConnectRequest) -> Result<NodeConnectResponse, tonic::Status>;
    async fn node_add_icon(&self, req: NodeAddIconRequest) -> Result<NodeAddIconResponse, tonic::Status>;
    async fn groovy(&self, req: GroovyRequest) -> Result<GroovyResponse, tonic::Status>;
    async fn node_color_set(&self, req: NodeColorSetRequest) -> Result<NodeColorSetResponse, tonic::Status>;
    async fn node_background_color_set(&self, req: NodeBackgroundColorSetRequest) -> Result<NodeBackgroundColorSetResponse, tonic::Status>;
    async fn status_info_set(&self, req: StatusInfoSetRequest) -> Result<StatusInfoSetResponse, tonic::Status>;
    async fn text_fsm(&self, req: TextFsmRequest) -> Result<TextFsmResponse, tonic::Status>;
    async fn mind_map_from_json(&self, req: MindMapFromJsonRequest) -> Result<MindMapFromJsonResponse, tonic::Status>;
    async fn mind_map_to_json(&self, req: MindMapToJsonRequest) -> Result<MindMapToJsonResponse, tonic::Status>;
    async fn get_current_node(&self, req: GetCurrentNodeRequest) -> Result<GetCurrentNodeResponse, tonic::Status>;
    async fn open_map(&self, req: OpenMapRequest) -> Result<OpenMapResponse, tonic::Status>;
    async fn focus_node(&self, req: FocusNodeRequest) -> Result<FocusNodeResponse, tonic::Status>;
    async fn get_node_text(&self, req: GetNodeTextRequest) -> Result<GetNodeTextResponse, tonic::Status>;
    async fn get_parent_node(&self, req: GetParentNodeRequest) -> Result<GetParentNodeResponse, tonic::Status>;
    async fn list_child_nodes(&self, req: ListChildNodesRequest) -> Result<ListChildNodesResponse, tonic::Status>;
    async fn get_node_note(&self, req: GetNodeNoteRequest) -> Result<GetNodeNoteResponse, tonic::Status>;
    async fn get_node_link(&self, req: GetNodeLinkRequest) -> Result<GetNodeLinkResponse, tonic::Status>;
    async fn set_node_text(&self, req: SetNodeTextRequest) -> Result<SetNodeTextResponse, tonic::Status>;
    async fn move_node(&self, req: MoveNodeRequest) -> Result<MoveNodeResponse, tonic::Status>;
}

// ─── Implementation for the real tonic client ───────────────────────────────

#[async_trait]
impl FreeplaneStub for TonicClient<Channel> {
    async fn create_child(&self, req: CreateChildRequest) -> Result<CreateChildResponse, tonic::Status> {
        let client = self.clone();
        client.create_child(req).await
    }
    async fn delete_child(&self, req: DeleteChildRequest) -> Result<DeleteChildResponse, tonic::Status> {
        let client = self.clone();
        client.delete_child(req).await
    }
    async fn node_attribute_add(&self, req: NodeAttributeAddRequest) -> Result<NodeAttributeAddResponse, tonic::Status> {
        let client = self.clone();
        client.node_attribute_add(req).await
    }
    async fn node_link_set(&self, req: NodeLinkSetRequest) -> Result<NodeLinkSetResponse, tonic::Status> {
        let client = self.clone();
        client.node_link_set(req).await
    }
    async fn node_details_set(&self, req: NodeDetailsSetRequest) -> Result<NodeDetailsSetResponse, tonic::Status> {
        let client = self.clone();
        client.node_details_set(req).await
    }
    async fn node_note_set(&self, req: NodeNoteSetRequest) -> Result<NodeNoteSetResponse, tonic::Status> {
        let client = self.clone();
        client.node_note_set(req).await
    }
    async fn node_tag_set(&self, req: NodeTagSetRequest) -> Result<NodeTagSetResponse, tonic::Status> {
        let client = self.clone();
        client.node_tag_set(req).await
    }
    async fn node_tag_add(&self, req: NodeTagAddRequest) -> Result<NodeTagAddResponse, tonic::Status> {
        let client = self.clone();
        client.node_tag_add(req).await
    }
    async fn node_connect(&self, req: NodeConnectRequest) -> Result<NodeConnectResponse, tonic::Status> {
        let client = self.clone();
        client.node_connect(req).await
    }
    async fn node_add_icon(&self, req: NodeAddIconRequest) -> Result<NodeAddIconResponse, tonic::Status> {
        let client = self.clone();
        client.node_add_icon(req).await
    }
    async fn groovy(&self, req: GroovyRequest) -> Result<GroovyResponse, tonic::Status> {
        let client = self.clone();
        client.groovy(req).await
    }
    async fn node_color_set(&self, req: NodeColorSetRequest) -> Result<NodeColorSetResponse, tonic::Status> {
        let client = self.clone();
        client.node_color_set(req).await
    }
    async fn node_background_color_set(&self, req: NodeBackgroundColorSetRequest) -> Result<NodeBackgroundColorSetResponse, tonic::Status> {
        let client = self.clone();
        client.node_background_color_set(req).await
    }
    async fn status_info_set(&self, req: StatusInfoSetRequest) -> Result<StatusInfoSetResponse, tonic::Status> {
        let client = self.clone();
        client.status_info_set(req).await
    }
    async fn text_fsm(&self, req: TextFsmRequest) -> Result<TextFsmResponse, tonic::Status> {
        let client = self.clone();
        client.text_fsm(req).await
    }
    async fn mind_map_from_json(&self, req: MindMapFromJsonRequest) -> Result<MindMapFromJsonResponse, tonic::Status> {
        let client = self.clone();
        client.mind_map_from_json(req).await
    }
    async fn mind_map_to_json(&self, req: MindMapToJsonRequest) -> Result<MindMapToJsonResponse, tonic::Status> {
        let client = self.clone();
        client.mind_map_to_json(req).await
    }
    async fn get_current_node(&self, req: GetCurrentNodeRequest) -> Result<GetCurrentNodeResponse, tonic::Status> {
        let client = self.clone();
        client.get_current_node(req).await
    }
    async fn open_map(&self, req: OpenMapRequest) -> Result<OpenMapResponse, tonic::Status> {
        let client = self.clone();
        client.open_map(req).await
    }
    async fn focus_node(&self, req: FocusNodeRequest) -> Result<FocusNodeResponse, tonic::Status> {
        let client = self.clone();
        client.focus_node(req).await
    }
    async fn get_node_text(&self, req: GetNodeTextRequest) -> Result<GetNodeTextResponse, tonic::Status> {
        let client = self.clone();
        client.get_node_text(req).await
    }
    async fn get_parent_node(&self, req: GetParentNodeRequest) -> Result<GetParentNodeResponse, tonic::Status> {
        let client = self.clone();
        client.get_parent_node(req).await
    }
    async fn list_child_nodes(&self, req: ListChildNodesRequest) -> Result<ListChildNodesResponse, tonic::Status> {
        let client = self.clone();
        client.list_child_nodes(req).await
    }
    async fn get_node_note(&self, req: GetNodeNoteRequest) -> Result<GetNodeNoteResponse, tonic::Status> {
        let client = self.clone();
        client.get_node_note(req).await
    }
    async fn get_node_link(&self, req: GetNodeLinkRequest) -> Result<GetNodeLinkResponse, tonic::Status> {
        let client = self.clone();
        client.get_node_link(req).await
    }
    async fn set_node_text(&self, req: SetNodeTextRequest) -> Result<SetNodeTextResponse, tonic::Status> {
        let client = self.clone();
        client.set_node_text(req).await
    }
    async fn move_node(&self, req: MoveNodeRequest) -> Result<MoveNodeResponse, tonic::Status> {
        let client = self.clone();
        client.move_node(req).await
    }
}

// ─── High-level client ──────────────────────────────────────────────────────

/// Client for interacting with a Freeplane gRPC server.
///
/// Provides a high-level interface to the Freeplane mind-map application
/// through its gRPC plugin. Supports async context-manager-like usage
/// via `connect()` / `close()`.
///
/// Typical usage:
/// ```ignore
/// let mut client = FreeplaneClient::new("127.0.0.1", 50051).await?;
/// let mindmap = client.current_map().await?;
/// let root = mindmap.root().await?;
/// println!("{}", root.get_text().await?);
/// client.close().await;
/// ```
pub struct FreeplaneClient<S>
where
    S: FreeplaneStub,
{
    pub(crate) stub: S,
    host: String,
    port: u32,
}

impl<S> Clone for FreeplaneClient<S>
where
    S: FreeplaneStub + Clone,
{
    fn clone(&self) -> Self {
        FreeplaneClient {
            stub: self.stub.clone(),
            host: self.host.clone(),
            port: self.port,
        }
    }
}

impl FreeplaneClient<TonicClient<Channel>> {
    /// Create a new FreeplaneClient and connect to the server.
    pub async fn connect(host: &str, port: u32) -> Result<Self, FreeplaneConnectionError> {
        let addr = format!("{}:{}", host, port);
        let channel = Channel::from_shared(format!("http://{}", addr))
            .map_err(|e| FreeplaneConnectionError(format!("Invalid address '{}': {}", addr, e)))?
            .connect_timeout(std::time::Duration::from_secs(5))
            .connect()
            .await
            .map_err(|e| FreeplaneConnectionError(format!("Failed to connect to Freeplane gRPC server at {}: {}", addr, e)))?;

        let tonic_client = TonicClient::new(channel);
        Ok(FreeplaneClient {
            stub: tonic_client,
            host: host.to_string(),
            port,
        })
    }
}

impl<S> FreeplaneClient<S>
where
    S: FreeplaneStub,
{
    /// Create a FreeplaneClient from an existing stub (useful for testing).
    pub fn from_stub(stub: S, host: &str, port: u32) -> Self {
        FreeplaneClient {
            stub,
            host: host.to_string(),
            port,
        }
    }

    /// Create a FreeplaneClient from a mock stub with default host/port.
    pub fn new_mock(stub: S) -> Self {
        FreeplaneClient {
            stub,
            host: "127.0.0.1".to_string(),
            port: 50051,
        }
    }

    /// Server hostname.
    pub fn host(&self) -> &str {
        &self.host
    }

    /// Server port.
    pub fn port(&self) -> u32 {
        self.port
    }

    /// Close the connection (no-op for mocked clients).
    pub async fn close(&mut self) {
        // For the real tonic client, dropping the stub closes the channel.
        // For mocked clients, this is a no-op.
    }

    /// Call a gRPC method and convert failures to domain errors.
    ///
    /// Connection-level errors (UNAVAILABLE, DEADLINE_EXCEEDED, RESOURCE_EXHAUSTED)
    /// are mapped to `FreeplaneConnectionError`. Other errors and server-reported
    /// failures are mapped to `FreeplaneOperationError`.
    async fn _call<F, R>(&self, method: F) -> Result<R, ClientError>
    where
        F: Future<Output = Result<R, tonic::Status>>,
        R: HasSuccess,
    {
        let response = method.await.map_err(|status| {
            if is_connection_error(&status) {
                ClientError::Connection(status_to_connection_error(&status))
            } else {
                ClientError::Operation(status_to_error(&status))
            }
        })?;

        if !response.has_success() {
            let error_msg = response.error_message().unwrap_or("Operation failed");
            return Err(ClientError::Operation(FreeplaneOperationError(
                format!("Operation failed: {}", error_msg)
            )));
        }

        Ok(response)
    }

    // ─── High-level operations ──────────────────────────────────────────

    /// Get the currently open / active mind map.
    pub async fn current_map(&self) -> Result<GetCurrentNodeResponse, ClientError> {
        self._call(
            self.stub.get_current_node(GetCurrentNodeRequest::default()),
        ).await
    }

    /// Alias for `current_map()`.
    pub async fn selected_map(&self) -> Result<GetCurrentNodeResponse, ClientError> {
        self.current_map().await
    }

    /// Open a mind map file on the Freeplane server.
    pub async fn open_map(&self, file_path: &str) -> Result<GetCurrentNodeResponse, ClientError> {
        self._call(
            self.stub.open_map(OpenMapRequest { file_path: file_path.to_string() }),
        ).await?;
        self.current_map().await
    }

    /// Export the current mind map as JSON.
    pub async fn get_map_to_json(&self) -> Result<String, ClientError> {
        let resp = self._call(
            self.stub.mind_map_to_json(MindMapToJsonRequest::default()),
        ).await?;
        Ok(resp.json.clone())
    }

    /// Import a mind map from JSON data.
    pub async fn mind_map_from_json(&self, json_data: &str) -> Result<bool, ClientError> {
        let resp = self._call(
            self.stub.mind_map_from_json(MindMapFromJsonRequest { json: json_data.to_string() }),
        ).await?;
        Ok(resp.success)
    }

    /// Execute Groovy code on the Freeplane server.
    pub async fn groovy(&self, code: &str) -> Result<String, ClientError> {
        let resp = self._call(
            self.stub.groovy(GroovyRequest { groovy_code: code.to_string() }),
        ).await?;
        Ok(resp.result.clone())
    }

    /// Focus (select) a node in the Freeplane UI.
    pub async fn focus_node(&self, node_id: &str) -> Result<bool, ClientError> {
        let resp = self._call(
            self.stub.focus_node(FocusNodeRequest { node_id: node_id.to_string() }),
        ).await?;
        Ok(resp.success)
    }

    /// Set the status bar info in Freeplane.
    pub async fn set_status_info(&self, info: &str) -> Result<bool, ClientError> {
        let resp = self._call(
            self.stub.status_info_set(StatusInfoSetRequest { status_info: info.to_string() }),
        ).await?;
        Ok(resp.success)
    }

    // ─── Direct RPC wrappers (27 methods matching freeplane.proto) ──────

    pub async fn create_child(&self, name: &str, parent_node_id: &str) -> Result<CreateChildResponse, ClientError> {
        self._call(
            self.stub.create_child(CreateChildRequest {
                name: name.to_string(),
                parent_node_id: parent_node_id.to_string(),
            }),
        ).await
    }

    pub async fn delete_child(&self, node_id: &str) -> Result<bool, ClientError> {
        let resp = self._call(
            self.stub.delete_child(DeleteChildRequest { node_id: node_id.to_string() }),
        ).await?;
        Ok(resp.success)
    }

    pub async fn node_attribute_add(&self, node_id: &str, attribute_name: &str, attribute_value: &str) -> Result<bool, ClientError> {
        let resp = self._call(
            self.stub.node_attribute_add(NodeAttributeAddRequest {
                node_id: node_id.to_string(),
                attribute_name: attribute_name.to_string(),
                attribute_value: attribute_value.to_string(),
            }),
        ).await?;
        Ok(resp.success)
    }

    pub async fn node_link_set(&self, node_id: &str, link: &str) -> Result<bool, ClientError> {
        let resp = self._call(
            self.stub.node_link_set(NodeLinkSetRequest {
                node_id: node_id.to_string(),
                link: link.to_string(),
            }),
        ).await?;
        Ok(resp.success)
    }

    pub async fn node_details_set(&self, node_id: &str, details: &str) -> Result<bool, ClientError> {
        let resp = self._call(
            self.stub.node_details_set(NodeDetailsSetRequest {
                node_id: node_id.to_string(),
                details: details.to_string(),
            }),
        ).await?;
        Ok(resp.success)
    }

    pub async fn node_note_set(&self, node_id: &str, note: &str) -> Result<bool, ClientError> {
        let resp = self._call(
            self.stub.node_note_set(NodeNoteSetRequest {
                node_id: node_id.to_string(),
                note: note.to_string(),
            }),
        ).await?;
        Ok(resp.success)
    }

    pub async fn node_tag_set(&self, node_id: &str, tags: Vec<String>) -> Result<bool, ClientError> {
        let resp = self._call(
            self.stub.node_tag_set(NodeTagSetRequest {
                node_id: node_id.to_string(),
                tags,
            }),
        ).await?;
        Ok(resp.success)
    }

    pub async fn node_tag_add(&self, node_id: &str, tags: Vec<String>) -> Result<bool, ClientError> {
        let resp = self._call(
            self.stub.node_tag_add(NodeTagAddRequest {
                node_id: node_id.to_string(),
                tags,
            }),
        ).await?;
        Ok(resp.success)
    }

    pub async fn node_connect(&self, source_node_id: &str, target_node_id: &str, relationship: &str) -> Result<bool, ClientError> {
        let resp = self._call(
            self.stub.node_connect(NodeConnectRequest {
                source_node_id: source_node_id.to_string(),
                target_node_id: target_node_id.to_string(),
                relationship: relationship.to_string(),
            }),
        ).await?;
        Ok(resp.success)
    }

    pub async fn node_add_icon(&self, node_id: &str, icon_name: &str) -> Result<bool, ClientError> {
        let resp = self._call(
            self.stub.node_add_icon(NodeAddIconRequest {
                node_id: node_id.to_string(),
                icon_name: icon_name.to_string(),
            }),
        ).await?;
        Ok(resp.success)
    }

    pub async fn node_color_set(&self, node_id: &str, red: i32, green: i32, blue: i32, alpha: i32) -> Result<bool, ClientError> {
        let resp = self._call(
            self.stub.node_color_set(NodeColorSetRequest {
                node_id: node_id.to_string(),
                red, green, blue, alpha: alpha.max(0).min(255),
            }),
        ).await?;
        Ok(resp.success)
    }

    pub async fn node_background_color_set(&self, node_id: &str, red: i32, green: i32, blue: i32, alpha: i32) -> Result<bool, ClientError> {
        let resp = self._call(
            self.stub.node_background_color_set(NodeBackgroundColorSetRequest {
                node_id: node_id.to_string(),
                red, green, blue, alpha: alpha.max(0).min(255),
            }),
        ).await?;
        Ok(resp.success)
    }

    pub async fn text_fsm(&self, json: &str) -> Result<bool, ClientError> {
        let resp = self._call(
            self.stub.text_fsm(TextFsmRequest { json: json.to_string() }),
        ).await?;
        Ok(resp.success)
    }

    pub async fn get_node_text(&self, node_id: &str) -> Result<GetNodeTextResponse, ClientError> {
        self._call(
            self.stub.get_node_text(GetNodeTextRequest { node_id: node_id.to_string() }),
        ).await
    }

    pub async fn get_parent_node(&self, node_id: &str) -> Result<GetParentNodeResponse, ClientError> {
        self._call(
            self.stub.get_parent_node(GetParentNodeRequest { node_id: node_id.to_string() }),
        ).await
    }

    pub async fn list_child_nodes(&self, node_id: &str) -> Result<ListChildNodesResponse, ClientError> {
        self._call(
            self.stub.list_child_nodes(ListChildNodesRequest { node_id: node_id.to_string() }),
        ).await
    }

    pub async fn get_node_note(&self, node_id: &str) -> Result<GetNodeNoteResponse, ClientError> {
        self._call(
            self.stub.get_node_note(GetNodeNoteRequest { node_id: node_id.to_string() }),
        ).await
    }

    pub async fn get_node_link(&self, node_id: &str) -> Result<GetNodeLinkResponse, ClientError> {
        self._call(
            self.stub.get_node_link(GetNodeLinkRequest { node_id: node_id.to_string() }),
        ).await
    }

    pub async fn set_node_text(&self, node_id: &str, text: &str) -> Result<bool, ClientError> {
        let resp = self._call(
            self.stub.set_node_text(SetNodeTextRequest {
                node_id: node_id.to_string(),
                text: text.to_string(),
            }),
        ).await?;
        Ok(resp.success)
    }

    pub async fn move_node(&self, node_id: &str, new_parent_node_id: &str) -> Result<bool, ClientError> {
        let resp = self._call(
            self.stub.move_node(MoveNodeRequest {
                node_id: node_id.to_string(),
                new_parent_node_id: new_parent_node_id.to_string(),
            }),
        ).await?;
        Ok(resp.success)
    }
}

impl<S> FreeplaneClient<S>
where
    S: FreeplaneStub + Clone,
{
    /// Create a `MindMap` instance bound to this client.
    ///
    /// The returned `MindMap` can be used to access the root node,
    /// search nodes, and perform map-level operations.
    ///
    /// Note: This creates a `MindMap` with empty map/node IDs.
    /// For a fully initialized `MindMap`, use `current_map()` or `open_map()`
    /// and then construct via `mind_map()`.
    pub fn mind_map(&self) -> crate::node::MindMap<S> {
        crate::node::MindMap::new(self.clone(), "", "")
    }

    /// Create a `Node` instance for the given node ID.
    ///
    /// The returned `Node` is bound to this client and can be used
    /// to access node-level operations.
    pub fn node(&self, node_id: &str) -> crate::node::Node<S> {
        crate::node::Node::new(self.clone(), node_id, None)
    }
}

/// Trait for response types that have a `success` field and optional `error_message`.
pub trait HasSuccess {
    fn has_success(&self) -> bool;
    fn error_message(&self) -> Option<&str>;
}

impl HasSuccess for CreateChildResponse {
    fn has_success(&self) -> bool { !self.node_id.is_empty() }
    fn error_message(&self) -> Option<&str> { None }
}
impl HasSuccess for DeleteChildResponse {
    fn has_success(&self) -> bool { self.success }
    fn error_message(&self) -> Option<&str> { None }
}
impl HasSuccess for NodeAttributeAddResponse {
    fn has_success(&self) -> bool { self.success }
    fn error_message(&self) -> Option<&str> { None }
}
impl HasSuccess for NodeLinkSetResponse {
    fn has_success(&self) -> bool { self.success }
    fn error_message(&self) -> Option<&str> { None }
}
impl HasSuccess for NodeDetailsSetResponse {
    fn has_success(&self) -> bool { self.success }
    fn error_message(&self) -> Option<&str> { None }
}
impl HasSuccess for NodeNoteSetResponse {
    fn has_success(&self) -> bool { self.success }
    fn error_message(&self) -> Option<&str> { None }
}
impl HasSuccess for NodeTagSetResponse {
    fn has_success(&self) -> bool { self.success }
    fn error_message(&self) -> Option<&str> { None }
}
impl HasSuccess for NodeTagAddResponse {
    fn has_success(&self) -> bool { self.success }
    fn error_message(&self) -> Option<&str> { None }
}
impl HasSuccess for NodeConnectResponse {
    fn has_success(&self) -> bool { self.success }
    fn error_message(&self) -> Option<&str> { None }
}
impl HasSuccess for NodeAddIconResponse {
    fn has_success(&self) -> bool { self.success }
    fn error_message(&self) -> Option<&str> { None }
}
impl HasSuccess for GroovyResponse {
    fn has_success(&self) -> bool { self.success }
    fn error_message(&self) -> Option<&str> { Some(&self.error_message) }
}
impl HasSuccess for NodeColorSetResponse {
    fn has_success(&self) -> bool { self.success }
    fn error_message(&self) -> Option<&str> { None }
}
impl HasSuccess for NodeBackgroundColorSetResponse {
    fn has_success(&self) -> bool { self.success }
    fn error_message(&self) -> Option<&str> { None }
}
impl HasSuccess for StatusInfoSetResponse {
    fn has_success(&self) -> bool { self.success }
    fn error_message(&self) -> Option<&str> { None }
}
impl HasSuccess for TextFsmResponse {
    fn has_success(&self) -> bool { self.success }
    fn error_message(&self) -> Option<&str> { None }
}
impl HasSuccess for MindMapFromJsonResponse {
    fn has_success(&self) -> bool { self.success }
    fn error_message(&self) -> Option<&str> { None }
}
impl HasSuccess for MindMapToJsonResponse {
    fn has_success(&self) -> bool { self.success }
    fn error_message(&self) -> Option<&str> { None }
}
impl HasSuccess for GetCurrentNodeResponse {
    fn has_success(&self) -> bool { self.success }
    fn error_message(&self) -> Option<&str> { if self.success { None } else { Some("No map currently open") } }
}
impl HasSuccess for OpenMapResponse {
    fn has_success(&self) -> bool { self.success }
    fn error_message(&self) -> Option<&str> { None }
}
impl HasSuccess for FocusNodeResponse {
    fn has_success(&self) -> bool { self.success }
    fn error_message(&self) -> Option<&str> { None }
}
impl HasSuccess for GetNodeTextResponse {
    fn has_success(&self) -> bool { self.success }
    fn error_message(&self) -> Option<&str> { if self.success { None } else { Some(&self.error_message) } }
}
impl HasSuccess for GetParentNodeResponse {
    fn has_success(&self) -> bool { self.success }
    fn error_message(&self) -> Option<&str> { if self.success { None } else { Some(&self.error_message) } }
}
impl HasSuccess for ListChildNodesResponse {
    fn has_success(&self) -> bool { self.success }
    fn error_message(&self) -> Option<&str> { if self.success { None } else { Some(&self.error_message) } }
}
impl HasSuccess for GetNodeNoteResponse {
    fn has_success(&self) -> bool { self.success }
    fn error_message(&self) -> Option<&str> { if self.success { None } else { Some(&self.error_message) } }
}
impl HasSuccess for GetNodeLinkResponse {
    fn has_success(&self) -> bool { self.success }
    fn error_message(&self) -> Option<&str> { if self.success { None } else { Some(&self.error_message) } }
}
impl HasSuccess for SetNodeTextResponse {
    fn has_success(&self) -> bool { self.success }
    fn error_message(&self) -> Option<&str> { if self.success { None } else { Some(&self.error_message) } }
}
impl HasSuccess for MoveNodeResponse {
    fn has_success(&self) -> bool { self.success }
    fn error_message(&self) -> Option<&str> { if self.success { None } else { Some(&self.error_message) } }
}

/// Unified error type for client operations.
#[derive(Debug, Error)]
pub enum ClientError {
    #[error("{0}")]
    Connection(FreeplaneConnectionError),
    #[error("{0}")]
    Operation(FreeplaneOperationError),
    #[error("{0}")]
    NodeNotFound(NodeNotFoundError),
}
