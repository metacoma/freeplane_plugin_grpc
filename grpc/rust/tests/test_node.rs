//! Unit tests for the Node class using mocked stubs.

use async_trait::async_trait;
use freeplane_grpc::client::{
    ClientError, FreeplaneClient, FreeplaneStub,
};

use freeplane_grpc::generated::freeplane::{
    ChildNodeInfo, CreateChildRequest, CreateChildResponse, DeleteChildRequest, DeleteChildResponse,
    FocusNodeRequest, FocusNodeResponse, GetCurrentNodeRequest, GetCurrentNodeResponse,
    GroovyRequest, GroovyResponse, GetNodeTextRequest, GetNodeTextResponse,
    GetParentNodeRequest, GetParentNodeResponse, ListChildNodesRequest, ListChildNodesResponse,
    GetNodeNoteRequest, GetNodeNoteResponse, GetNodeLinkRequest, GetNodeLinkResponse,
    SetNodeTextRequest, SetNodeTextResponse, MoveNodeRequest, MoveNodeResponse,
    NodeColorSetRequest, NodeColorSetResponse, NodeBackgroundColorSetRequest,
    NodeBackgroundColorSetResponse, NodeNoteSetRequest, NodeNoteSetResponse,
    NodeAttributeAddRequest, NodeAttributeAddResponse, NodeTagSetRequest, NodeTagSetResponse,
    NodeTagAddRequest, NodeTagAddResponse, NodeAddIconRequest, NodeAddIconResponse,
    NodeLinkSetRequest, NodeLinkSetResponse, NodeDetailsSetRequest, NodeDetailsSetResponse,
    NodeConnectRequest, NodeConnectResponse, StatusInfoSetRequest, StatusInfoSetResponse,
    TextFsmRequest, TextFsmResponse, MindMapFromJsonRequest, MindMapFromJsonResponse,
    MindMapToJsonRequest, MindMapToJsonResponse, OpenMapRequest, OpenMapResponse,
};
use freeplane_grpc::node::{Node, MindMap};
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

// Manually implement Clone for the mock
impl Clone for MockFreeplaneStub {
    fn clone(&self) -> Self {
        MockFreeplaneStub::new()
    }
}

// ─── Node initialization tests ──────────────────────────────────────────────

#[test]
fn test_node_new() {
    let mock_stub = MockFreeplaneStub::new();
    let client = FreeplaneClient::new_mock(mock_stub);
    let node = Node::new(client.clone(), "node123", None);

    assert_eq!(node.node_id(), "node123");
}

#[test]
fn test_node_with_mindmap() {
    let mock_stub = MockFreeplaneStub::new();
    let client = FreeplaneClient::new_mock(mock_stub);
    let mindmap = MindMap::new(client.clone(), "map1", "node1");
    let node = Node::new(client.clone(), "node123", Some(mindmap));

    assert_eq!(node.node_id(), "node123");
}

#[test]
fn test_node_client_ref() {
    let mock_stub = MockFreeplaneStub::new();
    let client = FreeplaneClient::new_mock(mock_stub);
    let node = Node::new(client.clone(), "node123", None);

    assert_eq!(node.client().host(), "127.0.0.1");
    assert_eq!(node.client().port(), 50051);
}

// ─── Node text tests ────────────────────────────────────────────────────────

#[tokio::test]
async fn test_node_get_text() {
    let mut mock_stub = MockFreeplaneStub::new();
    mock_stub
        .expect_get_node_text()
        .times(1)
        .returning(|_| {
            Ok(GetNodeTextResponse {
                success: true,
                node_id: "node123".to_string(),
                text: "Hello World".to_string(),
                error_message: String::new(),
            })
        });

    let client = FreeplaneClient::new_mock(mock_stub);
    let node = Node::new(client, "node123", None);
    let text = node.get_text().await.unwrap();

    assert_eq!(text, "Hello World");
}

#[tokio::test]
async fn test_node_set_text() {
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
    let node = Node::new(client, "node123", None);
    node.set_text("New text").await.unwrap();
}

// ─── Node hierarchy tests ───────────────────────────────────────────────────

#[tokio::test]
async fn test_node_add_child() {
    let mut mock_stub = MockFreeplaneStub::new();
    // Groovy script returns a node ID
    mock_stub
        .expect_groovy()
        .times(1)
        .returning(|req| {
            Ok(GroovyResponse {
                success: true,
                result: "ID_child123".to_string(),
                error_message: String::new(),
            })
        });

    let client = FreeplaneClient::new_mock(mock_stub);
    let node = Node::new(client, "parent123", None);
    let child = node.add_child("Child text", "").await.unwrap();

    assert!(child.node_id().starts_with("ID_"));
}

#[tokio::test]
async fn test_node_children() {
    let mut mock_stub = MockFreeplaneStub::new();
    mock_stub
        .expect_list_child_nodes()
        .times(1)
        .returning(|_| {
            Ok(ListChildNodesResponse {
                success: true,
                error_message: String::new(),
                children: vec![
                    ChildNodeInfo {
                        node_id: "child1".to_string(),
                        text: "Child 1".to_string(),
                    },
                    ChildNodeInfo {
                        node_id: "child2".to_string(),
                        text: "Child 2".to_string(),
                    },
                ],
            })
        });

    let client = FreeplaneClient::new_mock(mock_stub);
    let node = Node::new(client, "parent123", None);
    let children = node.children().await.unwrap();

    assert_eq!(children.len(), 2);
    assert_eq!(children[0].node_id(), "child1");
    assert_eq!(children[1].node_id(), "child2");
}

#[tokio::test]
async fn test_node_children_empty() {
    let mut mock_stub = MockFreeplaneStub::new();
    mock_stub
        .expect_list_child_nodes()
        .times(1)
        .returning(|_| {
            Ok(ListChildNodesResponse {
                success: true,
                error_message: String::new(),
                children: vec![],
            })
        });

    let client = FreeplaneClient::new_mock(mock_stub);
    let node = Node::new(client, "parent123", None);
    let children = node.children().await.unwrap();

    assert!(children.is_empty());
}

#[tokio::test]
async fn test_node_parent() {
    let mut mock_stub = MockFreeplaneStub::new();
    mock_stub
        .expect_get_parent_node()
        .times(1)
        .returning(|_| {
            Ok(GetParentNodeResponse {
                success: true,
                node_id: "child123".to_string(),
                parent_node_id: "parent123".to_string(),
                parent_node_text: "Parent".to_string(),
                error_message: String::new(),
            })
        });

    let client = FreeplaneClient::new_mock(mock_stub);
    let node = Node::new(client, "child123", None);
    let parent = node.parent().await.unwrap();

    assert_eq!(parent.node_id(), "parent123");
}

#[tokio::test]
async fn test_node_parent_root_raises() {
    let mut mock_stub = MockFreeplaneStub::new();
    mock_stub
        .expect_get_parent_node()
        .times(1)
        .returning(|_| {
            Ok(GetParentNodeResponse {
                success: true,
                node_id: "root123".to_string(),
                parent_node_id: String::new(),
                parent_node_text: String::new(),
                error_message: "No parent".to_string(),
            })
        });

    let client = FreeplaneClient::new_mock(mock_stub);
    let node = Node::new(client, "root123", None);
    let result = node.parent().await;

    assert!(result.is_err());
    match result {
        Err(ClientError::NodeNotFound(_)) => {},
        Err(ClientError::Connection(_)) => {},
        Err(ClientError::Operation(_)) => {},
        Ok(_) => panic!("Expected error, got Ok"),
    }
}

#[tokio::test]
async fn test_node_delete() {
    let mut mock_stub = MockFreeplaneStub::new();
    mock_stub
        .expect_delete_child()
        .times(1)
        .returning(|_| Ok(DeleteChildResponse { success: true }));

    let client = FreeplaneClient::new_mock(mock_stub);
    let node = Node::new(client, "node123", None);
    let result = node.delete().await.unwrap();

    assert!(result);
}

#[tokio::test]
async fn test_node_move() {
    let mut mock_stub = MockFreeplaneStub::new();
    mock_stub
        .expect_move_node()
        .times(1)
        .returning(|_| Ok(MoveNodeResponse {
            success: true,
            error_message: String::new(),
        }));

    let client = FreeplaneClient::new_mock(mock_stub);
    let node = Node::new(client, "node123", None);
    let result = node.move_to("new_parent").await.unwrap();

    assert!(result);
}

// ─── Node styling tests ─────────────────────────────────────────────────────

#[tokio::test]
async fn test_node_get_style() {
    let mut mock_stub = MockFreeplaneStub::new();
    mock_stub
        .expect_groovy()
        .times(1)
        .returning(|_| {
            Ok(GroovyResponse {
                success: true,
                result: "classic".to_string(),
                error_message: String::new(),
            })
        });

    let client = FreeplaneClient::new_mock(mock_stub);
    let node = Node::new(client, "node123", None);
    let style = node.get_style().await.unwrap();

    assert_eq!(style, "classic");
}

#[tokio::test]
async fn test_node_set_style_success() {
    let mut mock_stub = MockFreeplaneStub::new();
    mock_stub
        .expect_groovy()
        .times(1)
        .returning(|_| {
            Ok(GroovyResponse {
                success: true,
                result: "true".to_string(),
                error_message: String::new(),
            })
        });

    let client = FreeplaneClient::new_mock(mock_stub);
    let node = Node::new(client, "node123", None);
    let result = node.set_style("bubble").await.unwrap();

    assert!(result);
}

#[tokio::test]
async fn test_node_set_style_failure() {
    let mut mock_stub = MockFreeplaneStub::new();
    mock_stub
        .expect_groovy()
        .times(1)
        .returning(|_| {
            Ok(GroovyResponse {
                success: true,
                result: "Error: style not found".to_string(),
                error_message: String::new(),
            })
        });

    let client = FreeplaneClient::new_mock(mock_stub);
    let node = Node::new(client, "node123", None);
    let result = node.set_style("invalid_style").await.unwrap();

    assert!(!result);
}

#[tokio::test]
async fn test_node_get_color() {
    let mut mock_stub = MockFreeplaneStub::new();
    mock_stub
        .expect_groovy()
        .times(1)
        .returning(|_| {
            Ok(GroovyResponse {
                success: true,
                result: "java.awt.Color[r=255,g=0,b=0]".to_string(),
                error_message: String::new(),
            })
        });

    let client = FreeplaneClient::new_mock(mock_stub);
    let node = Node::new(client, "node123", None);
    let color = node.get_color().await.unwrap();

    assert!(color.contains("255"));
}

#[tokio::test]
async fn test_node_set_color() {
    let mut mock_stub = MockFreeplaneStub::new();
    mock_stub
        .expect_node_color_set()
        .times(1)
        .returning(|_| Ok(NodeColorSetResponse { success: true }));

    let client = FreeplaneClient::new_mock(mock_stub);
    let node = Node::new(client, "node123", None);
    let result = node.set_color(255, 0, 0, 255).await.unwrap();

    assert!(result);
}

#[tokio::test]
async fn test_node_get_background_color() {
    let mut mock_stub = MockFreeplaneStub::new();
    mock_stub
        .expect_groovy()
        .times(1)
        .returning(|_| {
            Ok(GroovyResponse {
                success: true,
                result: "java.awt.Color[r=255,g=255,b=200]".to_string(),
                error_message: String::new(),
            })
        });

    let client = FreeplaneClient::new_mock(mock_stub);
    let node = Node::new(client, "node123", None);
    let bg_color = node.get_background_color().await.unwrap();

    assert!(bg_color.contains("255"));
}

#[tokio::test]
async fn test_node_set_background_color() {
    let mut mock_stub = MockFreeplaneStub::new();
    mock_stub
        .expect_node_background_color_set()
        .times(1)
        .returning(|_| Ok(NodeBackgroundColorSetResponse { success: true }));

    let client = FreeplaneClient::new_mock(mock_stub);
    let node = Node::new(client, "node123", None);
    let result = node.set_background_color(0, 255, 0, 255).await.unwrap();

    assert!(result);
}

// ─── Node notes tests ───────────────────────────────────────────────────────

#[tokio::test]
async fn test_node_get_note_exists() {
    let mut mock_stub = MockFreeplaneStub::new();
    mock_stub
        .expect_get_node_note()
        .times(1)
        .returning(|_| {
            Ok(GetNodeNoteResponse {
                success: true,
                node_id: "node123".to_string(),
                has_note: true,
                note: "<html><body>Note content</body></html>".to_string(),
                error_message: String::new(),
            })
        });

    let client = FreeplaneClient::new_mock(mock_stub);
    let node = Node::new(client, "node123", None);
    let note = node.get_note().await.unwrap();

    assert!(note.is_some());
    assert!(note.unwrap().contains("Note content"));
}

#[tokio::test]
async fn test_node_get_note_empty() {
    let mut mock_stub = MockFreeplaneStub::new();
    mock_stub
        .expect_get_node_note()
        .times(1)
        .returning(|_| {
            Ok(GetNodeNoteResponse {
                success: true,
                node_id: "node123".to_string(),
                has_note: false,
                note: String::new(),
                error_message: String::new(),
            })
        });

    let client = FreeplaneClient::new_mock(mock_stub);
    let node = Node::new(client, "node123", None);
    let note = node.get_note().await.unwrap();

    assert!(note.is_none());
}

#[tokio::test]
async fn test_node_set_note() {
    let mut mock_stub = MockFreeplaneStub::new();
    mock_stub
        .expect_node_note_set()
        .times(1)
        .returning(|_| Ok(NodeNoteSetResponse { success: true }));

    let client = FreeplaneClient::new_mock(mock_stub);
    let node = Node::new(client, "node123", None);
    node.set_note("My note").await.unwrap();
}

// ─── Node attributes tests ──────────────────────────────────────────────────

#[tokio::test]
async fn test_node_get_attributes() {
    let mut mock_stub = MockFreeplaneStub::new();
    mock_stub
        .expect_groovy()
        .times(1)
        .returning(|_| {
            Ok(GroovyResponse {
                success: true,
                result: "{key=value}".to_string(),
                error_message: String::new(),
            })
        });

    let client = FreeplaneClient::new_mock(mock_stub);
    let node = Node::new(client, "node123", None);
    let attrs = node.get_attributes().await.unwrap();

    assert!(attrs.contains("key"));
}

#[tokio::test]
async fn test_node_set_attribute() {
    let mut mock_stub = MockFreeplaneStub::new();
    mock_stub
        .expect_node_attribute_add()
        .times(1)
        .returning(|_| Ok(NodeAttributeAddResponse { success: true }));

    let client = FreeplaneClient::new_mock(mock_stub);
    let node = Node::new(client, "node123", None);
    let result = node.set_attribute("key", "value").await.unwrap();

    assert!(result);
}

// ─── Node links tests ───────────────────────────────────────────────────────

#[tokio::test]
async fn test_node_get_links_exists() {
    let mut mock_stub = MockFreeplaneStub::new();
    mock_stub
        .expect_get_node_link()
        .times(1)
        .returning(|_| {
            Ok(GetNodeLinkResponse {
                success: true,
                node_id: "node123".to_string(),
                has_link: true,
                link: "https://example.com".to_string(),
                error_message: String::new(),
            })
        });

    let client = FreeplaneClient::new_mock(mock_stub);
    let node = Node::new(client, "node123", None);
    let links = node.get_links().await.unwrap();

    assert!(!links.is_empty());
    assert_eq!(links[0], "https://example.com");
}

#[tokio::test]
async fn test_node_get_links_empty() {
    let mut mock_stub = MockFreeplaneStub::new();
    mock_stub
        .expect_get_node_link()
        .times(1)
        .returning(|_| {
            Ok(GetNodeLinkResponse {
                success: true,
                node_id: "node123".to_string(),
                has_link: false,
                link: String::new(),
                error_message: String::new(),
            })
        });

    let client = FreeplaneClient::new_mock(mock_stub);
    let node = Node::new(client, "node123", None);
    let links = node.get_links().await.unwrap();

    assert!(links.is_empty());
}

#[tokio::test]
async fn test_node_set_links() {
    let mut mock_stub = MockFreeplaneStub::new();
    mock_stub
        .expect_node_link_set()
        .times(1)
        .returning(|_| Ok(NodeLinkSetResponse { success: true }));

    let client = FreeplaneClient::new_mock(mock_stub);
    let node = Node::new(client, "node123", None);
    let result = node.set_links(&["https://example.com"]).await.unwrap();

    assert!(result);
}

// ─── Node tags tests ────────────────────────────────────────────────────────

#[tokio::test]
async fn test_node_set_tags() {
    let mut mock_stub = MockFreeplaneStub::new();
    mock_stub
        .expect_node_tag_set()
        .times(1)
        .returning(|_| Ok(NodeTagSetResponse { success: true }));

    let client = FreeplaneClient::new_mock(mock_stub);
    let node = Node::new(client, "node123", None);
    let result = node.set_tags(&["tag1", "tag2"]).await.unwrap();

    assert!(result);
}

#[tokio::test]
async fn test_node_add_tags() {
    let mut mock_stub = MockFreeplaneStub::new();
    mock_stub
        .expect_node_tag_add()
        .times(1)
        .returning(|_| Ok(NodeTagAddResponse { success: true }));

    let client = FreeplaneClient::new_mock(mock_stub);
    let node = Node::new(client, "node123", None);
    let result = node.add_tags(&["tag1"]).await.unwrap();

    assert!(result);
}

// ─── Node icons tests ───────────────────────────────────────────────────────

#[tokio::test]
async fn test_node_add_icon() {
    let mut mock_stub = MockFreeplaneStub::new();
    mock_stub
        .expect_node_add_icon()
        .times(1)
        .returning(|_| Ok(NodeAddIconResponse { success: true }));

    let client = FreeplaneClient::new_mock(mock_stub);
    let node = Node::new(client, "node123", None);
    let result = node.add_icon("star").await.unwrap();

    assert!(result);
}

// ─── Node actions tests ─────────────────────────────────────────────────────

#[tokio::test]
async fn test_node_select() {
    let mut mock_stub = MockFreeplaneStub::new();
    mock_stub
        .expect_focus_node()
        .times(1)
        .returning(|_| Ok(FocusNodeResponse { success: true }));

    let client = FreeplaneClient::new_mock(mock_stub);
    let node = Node::new(client, "node123", None);
    let result = node.select().await.unwrap();

    assert!(result);
}

#[tokio::test]
async fn test_node_center() {
    let mut mock_stub = MockFreeplaneStub::new();
    mock_stub
        .expect_focus_node()
        .times(1)
        .returning(|_| Ok(FocusNodeResponse { success: true }));

    let client = FreeplaneClient::new_mock(mock_stub);
    let node = Node::new(client, "node123", None);
    let result = node.center().await.unwrap();

    assert!(result);
}

#[tokio::test]
async fn test_node_refresh() {
    let mut mock_stub = MockFreeplaneStub::new();
    mock_stub
        .expect_get_node_text()
        .times(1)
        .returning(|_| {
            Ok(GetNodeTextResponse {
                success: true,
                node_id: "node123".to_string(),
                text: "Hello".to_string(),
                error_message: String::new(),
            })
        });

    let client = FreeplaneClient::new_mock(mock_stub);
    let node = Node::new(client, "node123", None);
    node.refresh().await.unwrap();
}

// ─── Node folded state tests ────────────────────────────────────────────────

#[tokio::test]
async fn test_node_get_folded_true() {
    let mut mock_stub = MockFreeplaneStub::new();
    mock_stub
        .expect_groovy()
        .times(1)
        .returning(|_| {
            Ok(GroovyResponse {
                success: true,
                result: "true".to_string(),
                error_message: String::new(),
            })
        });

    let client = FreeplaneClient::new_mock(mock_stub);
    let node = Node::new(client, "node123", None);
    let folded = node.get_folded().await.unwrap();

    assert!(folded);
}

#[tokio::test]
async fn test_node_get_folded_false() {
    let mut mock_stub = MockFreeplaneStub::new();
    mock_stub
        .expect_groovy()
        .times(1)
        .returning(|_| {
            Ok(GroovyResponse {
                success: true,
                result: "false".to_string(),
                error_message: String::new(),
            })
        });

    let client = FreeplaneClient::new_mock(mock_stub);
    let node = Node::new(client, "node123", None);
    let folded = node.get_folded().await.unwrap();

    assert!(!folded);
}

#[tokio::test]
async fn test_node_set_folded_true() {
    let mut mock_stub = MockFreeplaneStub::new();
    mock_stub
        .expect_groovy()
        .times(1)
        .returning(|_| {
            Ok(GroovyResponse {
                success: true,
                result: "true".to_string(),
                error_message: String::new(),
            })
        });

    let client = FreeplaneClient::new_mock(mock_stub);
    let node = Node::new(client, "node123", None);
    let result = node.set_folded(true).await.unwrap();

    assert!(result);
}

#[tokio::test]
async fn test_node_set_folded_false() {
    let mut mock_stub = MockFreeplaneStub::new();
    mock_stub
        .expect_groovy()
        .times(1)
        .returning(|_| {
            Ok(GroovyResponse {
                success: true,
                result: "true".to_string(),
                error_message: String::new(),
            })
        });

    let client = FreeplaneClient::new_mock(mock_stub);
    let node = Node::new(client, "node123", None);
    let result = node.set_folded(false).await.unwrap();

    assert!(result);
}

// ─── Node Clone tests ───────────────────────────────────────────────────────

#[tokio::test]
async fn test_node_clone() {
    let mock_stub = MockFreeplaneStub::new();
    let client = FreeplaneClient::new_mock(mock_stub);
    let node = Node::new(client, "node123", None);
    let cloned = node.clone();

    assert_eq!(node.node_id(), cloned.node_id());
}
