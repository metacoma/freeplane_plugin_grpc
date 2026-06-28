//! Unit tests for the Freeplane client using mocked stubs.

use async_trait::async_trait;
use freeplane_grpc::client::{
    ClientError, FreeplaneClient, FreeplaneStub,
};
use freeplane_grpc::error::{
    FreeplaneConnectionError, FreeplaneOperationError, NodeNotFoundError,
};
use freeplane_grpc::generated::freeplane::{
    ChildNodeInfo, CreateChildRequest, CreateChildResponse, DeleteChildRequest, DeleteChildResponse,
    FocusNodeRequest, FocusNodeResponse, GetCurrentNodeRequest, GetCurrentNodeResponse,
    GroovyRequest, GroovyResponse, MindMapFromJsonRequest, MindMapFromJsonResponse,
    MindMapToJsonRequest, MindMapToJsonResponse, NodeAddIconRequest, NodeAddIconResponse,
    NodeAttributeAddRequest, NodeAttributeAddResponse, NodeBackgroundColorSetRequest,
    NodeBackgroundColorSetResponse, NodeColorSetRequest, NodeColorSetResponse, NodeConnectRequest,
    NodeConnectResponse, NodeDetailsSetRequest, NodeDetailsSetResponse, NodeLinkSetRequest,
    NodeLinkSetResponse, NodeNoteSetRequest, NodeNoteSetResponse, NodeTagAddRequest,
    NodeTagAddResponse, NodeTagSetRequest, NodeTagSetResponse, OpenMapRequest, OpenMapResponse,
    StatusInfoSetRequest, StatusInfoSetResponse, TextFsmRequest, TextFsmResponse,
    GetNodeTextRequest, GetNodeTextResponse, GetParentNodeRequest, GetParentNodeResponse,
    ListChildNodesRequest, ListChildNodesResponse, GetNodeNoteRequest, GetNodeNoteResponse,
    GetNodeLinkRequest, GetNodeLinkResponse, SetNodeTextRequest, SetNodeTextResponse,
    MoveNodeRequest, MoveNodeResponse,
};
use mockall::mock;
use tonic::Status;

// Create a mock for FreeplaneStub using mockall
mock! {
    pub FreeplaneStub {}

    #[async_trait]
    impl FreeplaneStub for FreeplaneStub {
        async fn create_child(&self, req: CreateChildRequest) -> Result<CreateChildResponse, Status>;
        async fn delete_child(&self, req: DeleteChildRequest) -> Result<DeleteChildResponse, Status>;
        async fn node_attribute_add(&self, req: NodeAttributeAddRequest) -> Result<NodeAttributeAddResponse, Status>;
        async fn node_link_set(&self, req: NodeLinkSetRequest) -> Result<NodeLinkSetResponse, Status>;
        async fn node_details_set(&self, req: NodeDetailsSetRequest) -> Result<NodeDetailsSetResponse, Status>;
        async fn node_note_set(&self, req: NodeNoteSetRequest) -> Result<NodeNoteSetResponse, Status>;
        async fn node_tag_set(&self, req: NodeTagSetRequest) -> Result<NodeTagSetResponse, Status>;
        async fn node_tag_add(&self, req: NodeTagAddRequest) -> Result<NodeTagAddResponse, Status>;
        async fn node_connect(&self, req: NodeConnectRequest) -> Result<NodeConnectResponse, Status>;
        async fn node_add_icon(&self, req: NodeAddIconRequest) -> Result<NodeAddIconResponse, Status>;
        async fn groovy(&self, req: GroovyRequest) -> Result<GroovyResponse, Status>;
        async fn node_color_set(&self, req: NodeColorSetRequest) -> Result<NodeColorSetResponse, Status>;
        async fn node_background_color_set(&self, req: NodeBackgroundColorSetRequest) -> Result<NodeBackgroundColorSetResponse, Status>;
        async fn status_info_set(&self, req: StatusInfoSetRequest) -> Result<StatusInfoSetResponse, Status>;
        async fn text_fsm(&self, req: TextFsmRequest) -> Result<TextFsmResponse, Status>;
        async fn mind_map_from_json(&self, req: MindMapFromJsonRequest) -> Result<MindMapFromJsonResponse, Status>;
        async fn mind_map_to_json(&self, req: MindMapToJsonRequest) -> Result<MindMapToJsonResponse, Status>;
        async fn get_current_node(&self, req: GetCurrentNodeRequest) -> Result<GetCurrentNodeResponse, Status>;
        async fn open_map(&self, req: OpenMapRequest) -> Result<OpenMapResponse, Status>;
        async fn focus_node(&self, req: FocusNodeRequest) -> Result<FocusNodeResponse, Status>;
        async fn get_node_text(&self, req: GetNodeTextRequest) -> Result<GetNodeTextResponse, Status>;
        async fn get_parent_node(&self, req: GetParentNodeRequest) -> Result<GetParentNodeResponse, Status>;
        async fn list_child_nodes(&self, req: ListChildNodesRequest) -> Result<ListChildNodesResponse, Status>;
        async fn get_node_note(&self, req: GetNodeNoteRequest) -> Result<GetNodeNoteResponse, Status>;
        async fn get_node_link(&self, req: GetNodeLinkRequest) -> Result<GetNodeLinkResponse, Status>;
        async fn set_node_text(&self, req: SetNodeTextRequest) -> Result<SetNodeTextResponse, Status>;
        async fn move_node(&self, req: MoveNodeRequest) -> Result<MoveNodeResponse, Status>;
    }
}

// Manually implement Clone for the mock since mockall doesn't derive it.
// Each MockFreeplaneStub is stateless by default (no expectations set),
// so cloning is trivial.
impl Clone for MockFreeplaneStub {
    fn clone(&self) -> Self {
        MockFreeplaneStub::new()
    }
}

// ─── Client construction tests ──────────────────────────────────────────────

#[test]
fn test_client_new_mock() {
    let mock_stub = MockFreeplaneStub::new();
    let client = FreeplaneClient::new_mock(mock_stub);
    assert_eq!(client.host(), "127.0.0.1");
    assert_eq!(client.port(), 50051);
}

#[test]
fn test_client_from_stub() {
    let mock_stub = MockFreeplaneStub::new();
    let client = FreeplaneClient::from_stub(mock_stub, "example.com", 9999);
    assert_eq!(client.host(), "example.com");
    assert_eq!(client.port(), 9999);
}

#[tokio::test]
async fn test_client_clone() {
    let mock_stub = MockFreeplaneStub::new();
    let client = FreeplaneClient::new_mock(mock_stub);
    let cloned = client.clone();
    assert_eq!(client.host(), cloned.host());
    assert_eq!(client.port(), cloned.port());
}

// ─── Error type tests ───────────────────────────────────────────────────────

#[test]
fn test_error_connection_variant() {
    let err = ClientError::Connection(FreeplaneConnectionError("connection failed".to_string()));
    assert!(matches!(err, ClientError::Connection(_)));
    assert!(err.to_string().contains("connection failed"));
}

#[test]
fn test_error_operation_variant() {
    let err = ClientError::Operation(FreeplaneOperationError("operation failed".to_string()));
    assert!(matches!(err, ClientError::Operation(_)));
    assert!(err.to_string().contains("operation failed"));
}

#[test]
fn test_error_node_not_found_variant() {
    let err = ClientError::NodeNotFound(NodeNotFoundError("node not found".to_string()));
    assert!(matches!(err, ClientError::NodeNotFound(_)));
    assert!(err.to_string().contains("node not found"));
}

// ─── RPC wrapper tests (mocked) ─────────────────────────────────────────────

#[tokio::test]
async fn test_create_child_success() {
    let mut mock_stub = MockFreeplaneStub::new();
    mock_stub
        .expect_create_child()
        .times(1)
        .returning(|req| {
            Ok(CreateChildResponse {
                node_id: format!("created_{}", req.parent_node_id),
                node_text: req.name.clone(),
            })
        });

    let client = FreeplaneClient::new_mock(mock_stub);
    let result = client.create_child("Test Node", "parent123").await;

    assert!(result.is_ok());
    let resp = result.unwrap();
    assert_eq!(resp.node_id, "created_parent123");
    assert_eq!(resp.node_text, "Test Node");
}

#[tokio::test]
async fn test_delete_child_success() {
    let mut mock_stub = MockFreeplaneStub::new();
    mock_stub
        .expect_delete_child()
        .times(1)
        .returning(|_| Ok(DeleteChildResponse { success: true }));

    let client = FreeplaneClient::new_mock(mock_stub);
    let result = client.delete_child("node123").await;

    assert!(result.is_ok());
    assert!(result.unwrap());
}

#[tokio::test]
async fn test_get_node_text_success() {
    let mut mock_stub = MockFreeplaneStub::new();
    mock_stub
        .expect_get_node_text()
        .times(1)
        .returning(|req| {
            Ok(GetNodeTextResponse {
                success: true,
                node_id: req.node_id.clone(),
                text: "Hello world".to_string(),
                error_message: String::new(),
            })
        });

    let client = FreeplaneClient::new_mock(mock_stub);
    let result = client.get_node_text("node123").await;

    assert!(result.is_ok());
    let resp = result.unwrap();
    assert_eq!(resp.text, "Hello world");
}

#[tokio::test]
async fn test_set_node_text_success() {
    let mut mock_stub = MockFreeplaneStub::new();
    mock_stub
        .expect_set_node_text()
        .times(1)
        .returning(|_| {
            Ok(SetNodeTextResponse {
                success: true,
                node_id: "node123".to_string(),
                error_message: String::new(),
            })
        });

    let client = FreeplaneClient::new_mock(mock_stub);
    let result = client.set_node_text("node123", "New text").await;

    assert!(result.is_ok());
    assert!(result.unwrap());
}

#[tokio::test]
async fn test_groovy_success() {
    let mut mock_stub = MockFreeplaneStub::new();
    mock_stub
        .expect_groovy()
        .times(1)
        .returning(|req| {
            Ok(GroovyResponse {
                success: true,
                result: format!("Output: {}", req.groovy_code),
                error_message: String::new(),
            })
        });

    let client = FreeplaneClient::new_mock(mock_stub);
    let result = client.groovy("println 'hello'").await;

    assert!(result.is_ok());
    assert_eq!(result.unwrap(), "Output: println 'hello'");
}

#[tokio::test]
async fn test_open_map_success() {
    let mut mock_stub = MockFreeplaneStub::new();
    mock_stub
        .expect_open_map()
        .times(1)
        .returning(|_| Ok(OpenMapResponse { success: true }));
    mock_stub
        .expect_get_current_node()
        .times(1)
        .returning(|_| {
            Ok(GetCurrentNodeResponse {
                map_id: "map123".to_string(),
                node_id: "root456".to_string(),
                success: true,
            })
        });

    let client = FreeplaneClient::new_mock(mock_stub);
    let result = client.open_map("/path/to/file.mm").await;

    assert!(result.is_ok());
    let resp = result.unwrap();
    assert_eq!(resp.map_id, "map123");
}

#[tokio::test]
async fn test_focus_node_success() {
    let mut mock_stub = MockFreeplaneStub::new();
    mock_stub
        .expect_focus_node()
        .times(1)
        .returning(|_| Ok(FocusNodeResponse { success: true }));

    let client = FreeplaneClient::new_mock(mock_stub);
    let result = client.focus_node("node123").await;

    assert!(result.is_ok());
    assert!(result.unwrap());
}

#[tokio::test]
async fn test_status_info_set_success() {
    let mut mock_stub = MockFreeplaneStub::new();
    mock_stub
        .expect_status_info_set()
        .times(1)
        .returning(|_| Ok(StatusInfoSetResponse { success: true }));

    let client = FreeplaneClient::new_mock(mock_stub);
    let result = client.set_status_info("Test status").await;

    assert!(result.is_ok());
    assert!(result.unwrap());
}

#[tokio::test]
async fn test_list_child_nodes_success() {
    let mut mock_stub = MockFreeplaneStub::new();
    mock_stub
        .expect_list_child_nodes()
        .times(1)
        .returning(|_| {
            Ok(ListChildNodesResponse {
                success: true,
                children: vec![
                    ChildNodeInfo {
                        node_id: "child1".to_string(),
                        text: "Child 1".to_string(),
                        ..Default::default()
                    },
                    ChildNodeInfo {
                        node_id: "child2".to_string(),
                        text: "Child 2".to_string(),
                        ..Default::default()
                    },
                ],
                error_message: String::new(),
            })
        });

    let client = FreeplaneClient::new_mock(mock_stub);
    let result = client.list_child_nodes("parent123").await;

    assert!(result.is_ok());
    let resp = result.unwrap();
    assert_eq!(resp.children.len(), 2);
    assert_eq!(resp.children[0].node_id, "child1");
    assert_eq!(resp.children[1].text, "Child 2");
}

#[tokio::test]
async fn test_get_parent_node_success() {
    let mut mock_stub = MockFreeplaneStub::new();
    mock_stub
        .expect_get_parent_node()
        .times(1)
        .returning(|_| {
            Ok(GetParentNodeResponse {
                success: true,
                node_id: "child456".to_string(),
                parent_node_id: "parent123".to_string(),
                parent_node_text: "Parent".to_string(),
                error_message: String::new(),
            })
        });

    let client = FreeplaneClient::new_mock(mock_stub);
    let result = client.get_parent_node("child456").await;

    assert!(result.is_ok());
    let resp = result.unwrap();
    assert_eq!(resp.parent_node_id, "parent123");
}

#[tokio::test]
async fn test_get_node_note_success() {
    let mut mock_stub = MockFreeplaneStub::new();
    mock_stub
        .expect_get_node_note()
        .times(1)
        .returning(|req| {
            Ok(GetNodeNoteResponse {
                success: true,
                node_id: req.node_id.clone(),
                note: "Important note".to_string(),
                has_note: true,
                error_message: String::new(),
            })
        });

    let client = FreeplaneClient::new_mock(mock_stub);
    let result = client.get_node_note("node123").await;

    assert!(result.is_ok());
    let resp = result.unwrap();
    assert_eq!(resp.note, "Important note");
    assert!(resp.has_note);
}

#[tokio::test]
async fn test_get_node_link_success() {
    let mut mock_stub = MockFreeplaneStub::new();
    mock_stub
        .expect_get_node_link()
        .times(1)
        .returning(|req| {
            Ok(GetNodeLinkResponse {
                success: true,
                node_id: req.node_id.clone(),
                link: "https://example.com".to_string(),
                has_link: true,
                error_message: String::new(),
            })
        });

    let client = FreeplaneClient::new_mock(mock_stub);
    let result = client.get_node_link("node123").await;

    assert!(result.is_ok());
    let resp = result.unwrap();
    assert_eq!(resp.link, "https://example.com");
}

#[tokio::test]
async fn test_move_node_success() {
    let mut mock_stub = MockFreeplaneStub::new();
    mock_stub
        .expect_move_node()
        .times(1)
        .returning(|_| Ok(MoveNodeResponse {
            success: true,
            error_message: String::new(),
        }));

    let client = FreeplaneClient::new_mock(mock_stub);
    let result = client.move_node("node123", "new_parent").await;

    assert!(result.is_ok());
    assert!(result.unwrap());
}

#[tokio::test]
async fn test_node_attribute_add_success() {
    let mut mock_stub = MockFreeplaneStub::new();
    mock_stub
        .expect_node_attribute_add()
        .times(1)
        .returning(|_| Ok(NodeAttributeAddResponse { success: true }));

    let client = FreeplaneClient::new_mock(mock_stub);
    let result = client
        .node_attribute_add("node123", "attr_name", "attr_value")
        .await;

    assert!(result.is_ok());
    assert!(result.unwrap());
}

#[tokio::test]
async fn test_node_link_set_success() {
    let mut mock_stub = MockFreeplaneStub::new();
    mock_stub
        .expect_node_link_set()
        .times(1)
        .returning(|_| Ok(NodeLinkSetResponse { success: true }));

    let client = FreeplaneClient::new_mock(mock_stub);
    let result = client.node_link_set("node123", "https://example.com").await;

    assert!(result.is_ok());
    assert!(result.unwrap());
}

#[tokio::test]
async fn test_node_details_set_success() {
    let mut mock_stub = MockFreeplaneStub::new();
    mock_stub
        .expect_node_details_set()
        .times(1)
        .returning(|_| Ok(NodeDetailsSetResponse { success: true }));

    let client = FreeplaneClient::new_mock(mock_stub);
    let result = client.node_details_set("node123", "details").await;

    assert!(result.is_ok());
    assert!(result.unwrap());
}

#[tokio::test]
async fn test_node_note_set_success() {
    let mut mock_stub = MockFreeplaneStub::new();
    mock_stub
        .expect_node_note_set()
        .times(1)
        .returning(|_| Ok(NodeNoteSetResponse { success: true }));

    let client = FreeplaneClient::new_mock(mock_stub);
    let result = client.node_note_set("node123", "note content").await;

    assert!(result.is_ok());
    assert!(result.unwrap());
}

#[tokio::test]
async fn test_node_tag_set_success() {
    let mut mock_stub = MockFreeplaneStub::new();
    mock_stub
        .expect_node_tag_set()
        .times(1)
        .returning(|_| Ok(NodeTagSetResponse { success: true }));

    let client = FreeplaneClient::new_mock(mock_stub);
    let result = client
        .node_tag_set("node123", vec!["tag1".to_string(), "tag2".to_string()])
        .await;

    assert!(result.is_ok());
    assert!(result.unwrap());
}

#[tokio::test]
async fn test_node_tag_add_success() {
    let mut mock_stub = MockFreeplaneStub::new();
    mock_stub
        .expect_node_tag_add()
        .times(1)
        .returning(|_| Ok(NodeTagAddResponse { success: true }));

    let client = FreeplaneClient::new_mock(mock_stub);
    let result = client.node_tag_add("node123", vec!["tag1".to_string()]).await;

    assert!(result.is_ok());
    assert!(result.unwrap());
}

#[tokio::test]
async fn test_node_connect_success() {
    let mut mock_stub = MockFreeplaneStub::new();
    mock_stub
        .expect_node_connect()
        .times(1)
        .returning(|_| Ok(NodeConnectResponse { success: true }));

    let client = FreeplaneClient::new_mock(mock_stub);
    let result = client
        .node_connect("src123", "dst456", "depends_on")
        .await;

    assert!(result.is_ok());
    assert!(result.unwrap());
}

#[tokio::test]
async fn test_node_add_icon_success() {
    let mut mock_stub = MockFreeplaneStub::new();
    mock_stub
        .expect_node_add_icon()
        .times(1)
        .returning(|_| Ok(NodeAddIconResponse { success: true }));

    let client = FreeplaneClient::new_mock(mock_stub);
    let result = client.node_add_icon("node123", "icon_name").await;

    assert!(result.is_ok());
    assert!(result.unwrap());
}

#[tokio::test]
async fn test_node_color_set_success() {
    let mut mock_stub = MockFreeplaneStub::new();
    mock_stub
        .expect_node_color_set()
        .times(1)
        .returning(|_| Ok(NodeColorSetResponse { success: true }));

    let client = FreeplaneClient::new_mock(mock_stub);
    let result = client.node_color_set("node123", 255, 0, 0, 255).await;

    assert!(result.is_ok());
    assert!(result.unwrap());
}

#[tokio::test]
async fn test_node_background_color_set_success() {
    let mut mock_stub = MockFreeplaneStub::new();
    mock_stub
        .expect_node_background_color_set()
        .times(1)
        .returning(|_| Ok(NodeBackgroundColorSetResponse { success: true }));

    let client = FreeplaneClient::new_mock(mock_stub);
    let result = client
        .node_background_color_set("node123", 0, 255, 0, 255)
        .await;

    assert!(result.is_ok());
    assert!(result.unwrap());
}

#[tokio::test]
async fn test_text_fsm_success() {
    let mut mock_stub = MockFreeplaneStub::new();
    mock_stub
        .expect_text_fsm()
        .times(1)
        .returning(|_| Ok(TextFsmResponse { success: true }));

    let client = FreeplaneClient::new_mock(mock_stub);
    let result = client.text_fsm(r#"{"key": "value"}"#).await;

    assert!(result.is_ok());
    assert!(result.unwrap());
}

#[tokio::test]
async fn test_mind_map_to_json_success() {
    let mut mock_stub = MockFreeplaneStub::new();
    mock_stub
        .expect_mind_map_to_json()
        .times(1)
        .returning(|_| {
            Ok(MindMapToJsonResponse {
                success: true,
                json: r#"{"nodes":[]}"#.to_string(),
            })
        });

    let client = FreeplaneClient::new_mock(mock_stub);
    let result = client.get_map_to_json().await;

    assert!(result.is_ok());
    let resp = result.unwrap();
    assert_eq!(resp, r#"{"nodes":[]}"#);
}

#[tokio::test]
async fn test_mind_map_from_json_success() {
    let mut mock_stub = MockFreeplaneStub::new();
    mock_stub
        .expect_mind_map_from_json()
        .times(1)
        .returning(|_| Ok(MindMapFromJsonResponse { success: true }));

    let client = FreeplaneClient::new_mock(mock_stub);
    let result = client.mind_map_from_json(r#"{"nodes":[]}"#).await;

    assert!(result.is_ok());
    assert!(result.unwrap());
}

#[tokio::test]
async fn test_get_current_node_success() {
    let mut mock_stub = MockFreeplaneStub::new();
    mock_stub
        .expect_get_current_node()
        .times(1)
        .returning(|_| {
            Ok(GetCurrentNodeResponse {
                map_id: "map123".to_string(),
                node_id: "current789".to_string(),
                success: true,
            })
        });

    let client = FreeplaneClient::new_mock(mock_stub);
    let result = client.current_map().await;

    assert!(result.is_ok());
    let resp = result.unwrap();
    assert_eq!(resp.node_id, "current789");
    assert_eq!(resp.map_id, "map123");
}

#[tokio::test]
async fn test_error_operation_from_server_failure() {
    let mut mock_stub = MockFreeplaneStub::new();
    mock_stub
        .expect_delete_child()
        .times(1)
        .returning(|_| Ok(DeleteChildResponse { success: false }));

    let client = FreeplaneClient::new_mock(mock_stub);
    let result = client.delete_child("node123").await;

    assert!(result.is_err());
    match result.unwrap_err() {
        ClientError::Operation(err) => {
            assert!(err.0.contains("Operation failed"));
        }
        other => panic!("Expected Operation error, got {:?}", other),
    }
}
