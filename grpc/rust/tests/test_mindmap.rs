//! Unit tests for the MindMap class using mocked stubs.

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
use std::collections::HashMap;
use std::sync::Arc;
use tokio::sync::Mutex;
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

// Manually implement Clone for the mock (creates a fresh mock)
impl Clone for MockFreeplaneStub {
    fn clone(&self) -> Self {
        MockFreeplaneStub::new()
    }
}

/// Wrapper type that shares a mockall mock across clones using Arc<Mutex<>>.
/// This allows multiple clones of FreeplaneClient/MindMap/Node to share the same mock expectations.
#[derive(Clone)]
pub struct SharedMock(Arc<Mutex<MockFreeplaneStub>>);

impl SharedMock {
    pub fn new() -> Self {
        SharedMock(Arc::new(Mutex::new(MockFreeplaneStub::new())))
    }

    pub fn inner(&self) -> Arc<Mutex<MockFreeplaneStub>> {
        Arc::clone(&self.0)
    }
}

// Implement FreeplaneStub for SharedMock by delegating to the inner mock
#[async_trait]
impl FreeplaneStub for SharedMock {
    async fn create_child(&self, req: CreateChildRequest) -> Result<CreateChildResponse, Status> {
        let stub = self.0.lock().await;
        stub.create_child(req).await
    }
    async fn delete_child(&self, req: DeleteChildRequest) -> Result<DeleteChildResponse, Status> {
        let stub = self.0.lock().await;
        stub.delete_child(req).await
    }
    async fn node_attribute_add(&self, req: NodeAttributeAddRequest) -> Result<NodeAttributeAddResponse, Status> {
        let stub = self.0.lock().await;
        stub.node_attribute_add(req).await
    }
    async fn node_link_set(&self, req: NodeLinkSetRequest) -> Result<NodeLinkSetResponse, Status> {
        let stub = self.0.lock().await;
        stub.node_link_set(req).await
    }
    async fn node_details_set(&self, req: NodeDetailsSetRequest) -> Result<NodeDetailsSetResponse, Status> {
        let stub = self.0.lock().await;
        stub.node_details_set(req).await
    }
    async fn node_note_set(&self, req: NodeNoteSetRequest) -> Result<NodeNoteSetResponse, Status> {
        let stub = self.0.lock().await;
        stub.node_note_set(req).await
    }
    async fn node_tag_set(&self, req: NodeTagSetRequest) -> Result<NodeTagSetResponse, Status> {
        let stub = self.0.lock().await;
        stub.node_tag_set(req).await
    }
    async fn node_tag_add(&self, req: NodeTagAddRequest) -> Result<NodeTagAddResponse, Status> {
        let stub = self.0.lock().await;
        stub.node_tag_add(req).await
    }
    async fn node_connect(&self, req: NodeConnectRequest) -> Result<NodeConnectResponse, Status> {
        let stub = self.0.lock().await;
        stub.node_connect(req).await
    }
    async fn node_add_icon(&self, req: NodeAddIconRequest) -> Result<NodeAddIconResponse, Status> {
        let stub = self.0.lock().await;
        stub.node_add_icon(req).await
    }
    async fn groovy(&self, req: GroovyRequest) -> Result<GroovyResponse, Status> {
        let stub = self.0.lock().await;
        stub.groovy(req).await
    }
    async fn node_color_set(&self, req: NodeColorSetRequest) -> Result<NodeColorSetResponse, Status> {
        let stub = self.0.lock().await;
        stub.node_color_set(req).await
    }
    async fn node_background_color_set(&self, req: NodeBackgroundColorSetRequest) -> Result<NodeBackgroundColorSetResponse, Status> {
        let stub = self.0.lock().await;
        stub.node_background_color_set(req).await
    }
    async fn status_info_set(&self, req: StatusInfoSetRequest) -> Result<StatusInfoSetResponse, Status> {
        let stub = self.0.lock().await;
        stub.status_info_set(req).await
    }
    async fn text_fsm(&self, req: TextFsmRequest) -> Result<TextFsmResponse, Status> {
        let stub = self.0.lock().await;
        stub.text_fsm(req).await
    }
    async fn mind_map_from_json(&self, req: MindMapFromJsonRequest) -> Result<MindMapFromJsonResponse, Status> {
        let stub = self.0.lock().await;
        stub.mind_map_from_json(req).await
    }
    async fn mind_map_to_json(&self, req: MindMapToJsonRequest) -> Result<MindMapToJsonResponse, Status> {
        let stub = self.0.lock().await;
        stub.mind_map_to_json(req).await
    }
    async fn get_current_node(&self, req: GetCurrentNodeRequest) -> Result<GetCurrentNodeResponse, Status> {
        let stub = self.0.lock().await;
        stub.get_current_node(req).await
    }
    async fn open_map(&self, req: OpenMapRequest) -> Result<OpenMapResponse, Status> {
        let stub = self.0.lock().await;
        stub.open_map(req).await
    }
    async fn focus_node(&self, req: FocusNodeRequest) -> Result<FocusNodeResponse, Status> {
        let stub = self.0.lock().await;
        stub.focus_node(req).await
    }
    async fn get_node_text(&self, req: GetNodeTextRequest) -> Result<GetNodeTextResponse, Status> {
        let stub = self.0.lock().await;
        stub.get_node_text(req).await
    }
    async fn get_parent_node(&self, req: GetParentNodeRequest) -> Result<GetParentNodeResponse, Status> {
        let stub = self.0.lock().await;
        stub.get_parent_node(req).await
    }
    async fn list_child_nodes(&self, req: ListChildNodesRequest) -> Result<ListChildNodesResponse, Status> {
        let stub = self.0.lock().await;
        stub.list_child_nodes(req).await
    }
    async fn get_node_note(&self, req: GetNodeNoteRequest) -> Result<GetNodeNoteResponse, Status> {
        let stub = self.0.lock().await;
        stub.get_node_note(req).await
    }
    async fn get_node_link(&self, req: GetNodeLinkRequest) -> Result<GetNodeLinkResponse, Status> {
        let stub = self.0.lock().await;
        stub.get_node_link(req).await
    }
    async fn set_node_text(&self, req: SetNodeTextRequest) -> Result<SetNodeTextResponse, Status> {
        let stub = self.0.lock().await;
        stub.set_node_text(req).await
    }
    async fn move_node(&self, req: MoveNodeRequest) -> Result<MoveNodeResponse, Status> {
        let stub = self.0.lock().await;
        stub.move_node(req).await
    }
}

// ─── MindMap initialization tests ───────────────────────────────────────────

#[test]
fn test_mindmap_new() {
    let mock_stub = MockFreeplaneStub::new();
    let client = FreeplaneClient::new_mock(mock_stub);
    let mm = MindMap::new(client, "map123", "node456");

    assert_eq!(mm.map_id(), "map123");
    assert_eq!(mm.node_id(), "node456");
}

#[test]
fn test_mindmap_new_empty_ids() {
    let mock_stub = MockFreeplaneStub::new();
    let client = FreeplaneClient::new_mock(mock_stub);
    let mm = MindMap::new(client, "", "");

    assert_eq!(mm.map_id(), "");
    assert_eq!(mm.node_id(), "");
}

#[test]
fn test_mindmap_client_ref() {
    let mock_stub = MockFreeplaneStub::new();
    let client = FreeplaneClient::new_mock(mock_stub);
    let mm = MindMap::new(client, "map123", "node456");

    assert_eq!(mm.client().host(), "127.0.0.1");
    assert_eq!(mm.client().port(), 50051);
}

#[test]
fn test_mindmap_clone() {
    let mock_stub = MockFreeplaneStub::new();
    let client = FreeplaneClient::new_mock(mock_stub);
    let mm = MindMap::new(client, "map123", "node456");
    let cloned = mm.clone();

    assert_eq!(mm.map_id(), cloned.map_id());
    assert_eq!(mm.node_id(), cloned.node_id());
}

// ─── MindMap info tests ─────────────────────────────────────────────────────

#[test]
fn test_mindmap_info() {
    let mock_stub = MockFreeplaneStub::new();
    let client = FreeplaneClient::new_mock(mock_stub);
    let mm = MindMap::new(client, "map123", "node456");
    let info = mm.info();

    assert_eq!(info.get("map_id").unwrap(), "map123");
    assert_eq!(info.get("node_id").unwrap(), "node456");
}

#[test]
fn test_mindmap_info_is_hashmap() {
    let mock_stub = MockFreeplaneStub::new();
    let client = FreeplaneClient::new_mock(mock_stub);
    let mm = MindMap::new(client, "map123", "node456");
    let info: HashMap<String, String> = mm.info();

    // Verify it's a proper HashMap
    assert!(info.contains_key("map_id"));
    assert!(info.contains_key("node_id"));
}

// ─── MindMap root tests ─────────────────────────────────────────────────────

#[tokio::test]
async fn test_mindmap_root_with_node_id() {
    let mut mock_stub = MockFreeplaneStub::new();
    // First call: node1 has parent parent1
    // Second call: parent1 has no parent (is root)
    mock_stub
        .expect_get_parent_node()
        .times(2)
        .returning(move |req| {
            if req.node_id == "node1" {
                Ok(GetParentNodeResponse {
                node_id: String::new(),
                    success: true,
                    parent_node_id: "parent1".to_string(),
                    parent_node_text: "Parent".to_string(),
                    error_message: String::new(),
                })
            } else {
                Ok(GetParentNodeResponse {
                node_id: String::new(),
                    success: true,
                    parent_node_id: String::new(),
                    parent_node_text: String::new(),
                    error_message: "No parent".to_string(),
                })
            }
        });

    let client = FreeplaneClient::new_mock(mock_stub);
    let mm = MindMap::new(client, "map1", "node1");
    let root = mm.root().await.unwrap();

    assert_eq!(root.node_id(), "parent1");
}

#[tokio::test]
async fn test_mindmap_root_already_root() {
    let mut mock_stub = MockFreeplaneStub::new();
    mock_stub
        .expect_get_parent_node()
        .times(1)
        .returning(|_| {
            Ok(GetParentNodeResponse {
                node_id: String::new(),
                success: true,
                parent_node_id: String::new(),
                parent_node_text: String::new(),
                error_message: "No parent".to_string(),
            })
        });

    let client = FreeplaneClient::new_mock(mock_stub);
    let mm = MindMap::new(client, "map1", "rootNode");
    let root = mm.root().await.unwrap();

    assert_eq!(root.node_id(), "rootNode");
}

#[tokio::test]
async fn test_mindmap_root_empty_node_id_fetches_current() {
    let mut mock_stub = MockFreeplaneStub::new();
    mock_stub
        .expect_get_current_node()
        .times(1)
        .returning(|_| {
            Ok(GetCurrentNodeResponse {
                map_id: "map1".to_string(),
                node_id: "current1".to_string(),
                success: true,
            })
        });
    mock_stub
        .expect_get_parent_node()
        .times(1)
        .returning(|_| {
            Ok(GetParentNodeResponse {
                node_id: String::new(),
                success: true,
                parent_node_id: String::new(),
                parent_node_text: String::new(),
                error_message: "No parent".to_string(),
            })
        });

    let client = FreeplaneClient::new_mock(mock_stub);
    let mm = MindMap::new(client, "map1", "");
    let root = mm.root().await.unwrap();

    assert_eq!(root.node_id(), "current1");
}

// ─── MindMap selected_node tests ────────────────────────────────────────────

#[tokio::test]
async fn test_mindmap_selected_node() {
    let mut mock_stub = MockFreeplaneStub::new();
    mock_stub
        .expect_get_current_node()
        .times(1)
        .returning(|_| {
            Ok(GetCurrentNodeResponse {
                map_id: "map1".to_string(),
                node_id: "selected1".to_string(),
                success: true,
            })
        });

    let client = FreeplaneClient::new_mock(mock_stub);
    let mm = MindMap::new(client, "map1", "");
    let node = mm.selected_node().await.unwrap();

    assert_eq!(node.node_id(), "selected1");
}

#[tokio::test]
async fn test_mindmap_selected_node_no_selection() {
    let mut mock_stub = MockFreeplaneStub::new();
    mock_stub
        .expect_get_current_node()
        .times(1)
        .returning(|_| {
            Ok(GetCurrentNodeResponse {
                map_id: String::new(),
                node_id: String::new(),
                success: false,
            })
        });

    let client = FreeplaneClient::new_mock(mock_stub);
    let mm = MindMap::new(client, "map1", "");
    let result = mm.selected_node().await;

    assert!(result.is_err());
    match result {
        Err(ClientError::Operation(err)) => {
            assert!(err.0.contains("No node currently selected"));
        }
        Err(other) => panic!("Expected Operation error, got {:?}", other),
        Ok(_) => panic!("Expected error, got Ok"),
    }
}

// ─── MindMap find_nodes tests ───────────────────────────────────────────────

#[tokio::test]
async fn test_mindmap_find_nodes_matches() {
    let mock_stub = SharedMock::new();
    {
        let mut stub = mock_stub.0.lock().await;
        // get_parent_node for root detection
        stub.expect_get_parent_node()
        .returning(|_| {
            Ok(GetParentNodeResponse {
                node_id: String::new(),
                success: true,
                parent_node_id: String::new(),
                parent_node_text: String::new(),
                error_message: "No parent".to_string(),
            })
        });
        // get_node_text for root
        stub.expect_get_node_text()
        .returning(|_| {
            Ok(GetNodeTextResponse {
                node_id: String::new(),
                success: true,
                text: "Root Node".to_string(),
                error_message: String::new(),
            })
        });
        // list_child_nodes for root - no children
        stub.expect_list_child_nodes()
        .returning(|_| {
            Ok(ListChildNodesResponse {
                success: true,
                error_message: String::new(),
                children: vec![],
            })
        });
    }

    let client = FreeplaneClient::from_stub(mock_stub.clone(), "127.0.0.1", 50051);
    let mm = MindMap::new(client, "map1", "root1");
    let results = mm.find_nodes("root").await.unwrap();

    assert!(!results.is_empty());
}

#[tokio::test]
async fn test_mindmap_find_nodes_no_match() {
    let mock_stub = SharedMock::new();
    {
        let mut stub = mock_stub.0.lock().await;
        stub.expect_get_parent_node()
        .returning(|_| {
            Ok(GetParentNodeResponse {
                node_id: String::new(),
                success: true,
                parent_node_id: String::new(),
                parent_node_text: String::new(),
                error_message: "No parent".to_string(),
            })
        });
        stub.expect_get_node_text()
        .returning(|_| {
            Ok(GetNodeTextResponse {
                node_id: String::new(),
                success: true,
                text: "Hello World".to_string(),
                error_message: String::new(),
            })
        });
        stub.expect_list_child_nodes()
        .returning(|_| {
            Ok(ListChildNodesResponse {
                success: true,
                error_message: String::new(),
                children: vec![],
            })
        });
    }

    let client = FreeplaneClient::from_stub(mock_stub.clone(), "127.0.0.1", 50051);
    let mm = MindMap::new(client, "map1", "root1");
    let results = mm.find_nodes("xyz_nonexistent").await.unwrap();

    assert!(results.is_empty());
}

// ─── MindMap size tests ─────────────────────────────────────────────────────

#[tokio::test]
async fn test_mindmap_size() {
    let mock_stub = SharedMock::new();
    {
        let mut stub = mock_stub.0.lock().await;
        // get_parent_node for root detection (called once)
        stub.expect_get_parent_node()
        .times(1)
        .returning(|_| {
            Ok(GetParentNodeResponse {
                node_id: String::new(),
                success: true,
                parent_node_id: String::new(),
                parent_node_text: String::new(),
                error_message: "No parent".to_string(),
            })
        });
        // list_child_nodes: called 3 times (root, child1, child2)
        // root returns 2 children, child1 and child2 return empty
        stub.expect_list_child_nodes()
        .times(3)
        .returning(|req| {
            if req.node_id == "root1" {
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
            } else {
                Ok(ListChildNodesResponse {
                    success: true,
                    error_message: String::new(),
                    children: vec![],
                })
            }
        });
    }

    let client = FreeplaneClient::from_stub(mock_stub.clone(), "127.0.0.1", 50051);
    let mm = MindMap::new(client, "map1", "root1");
    let size = mm.size().await.unwrap();

    // Root + 2 children = 3
    assert_eq!(size, 3);
}

// ─── MindMap save tests ─────────────────────────────────────────────────────

#[tokio::test]
async fn test_mindmap_save() {
    let mock_stub = MockFreeplaneStub::new();
    let client = FreeplaneClient::new_mock(mock_stub);
    let mm = MindMap::new(client, "map1", "node1");
    let result = mm.save("/tmp/test.mm").await.unwrap();

    assert!(result);
}

// ─── MindMap export tests ───────────────────────────────────────────────────

#[tokio::test]
async fn test_mindmap_export() {
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
    let mm = MindMap::new(client, "map1", "node1");
    let result = mm.export("/tmp/test.png", "png").await.unwrap();

    assert!(result);
}

#[tokio::test]
async fn test_mindmap_export_failure() {
    let mut mock_stub = MockFreeplaneStub::new();
    mock_stub
        .expect_groovy()
        .times(1)
        .returning(|_| {
            Ok(GroovyResponse {
                success: true,
                result: "Error: export failed".to_string(),
                error_message: String::new(),
            })
        });

    let client = FreeplaneClient::new_mock(mock_stub);
    let mm = MindMap::new(client, "map1", "node1");
    let result = mm.export("/tmp/test.png", "png").await.unwrap();

    assert!(!result);
}

// ─── MindMap import_map tests ───────────────────────────────────────────────

#[tokio::test]
async fn test_mindmap_import_map() {
    let mut mock_stub = MockFreeplaneStub::new();
    mock_stub
        .expect_open_map()
        .times(1)
        .returning(|_| {
            Ok(OpenMapResponse { success: true })
        });
    mock_stub
        .expect_get_current_node()
        .times(1)
        .returning(|_| {
            Ok(GetCurrentNodeResponse {
                map_id: "imported_map".to_string(),
                node_id: "imported_node".to_string(),
                success: true,
            })
        });

    let client = FreeplaneClient::new_mock(mock_stub);
    let mm = MindMap::new(client, "map1", "node1");
    let imported = mm.import_map("/tmp/test.mm").await.unwrap();

    assert_eq!(imported.map_id(), "imported_map");
    assert_eq!(imported.node_id(), "imported_node");
}

// ─── MindMap create_node tests ──────────────────────────────────────────────

#[tokio::test]
async fn test_mindmap_create_node_under_root() {
    let mut mock_stub = MockFreeplaneStub::new();
    // get_parent_node for root detection
    mock_stub
        .expect_get_parent_node()
        .times(1)
        .returning(|_| {
            Ok(GetParentNodeResponse {
                node_id: String::new(),
                success: true,
                parent_node_id: String::new(),
                parent_node_text: String::new(),
                error_message: "No parent".to_string(),
            })
        });
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
    let mm = MindMap::new(client, "map1", "root1");
    let node = mm.create_node("New Node", "", "").await.unwrap();

    assert!(node.node_id().starts_with("created_root1"));
}

#[tokio::test]
async fn test_mindmap_create_node_under_parent() {
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
    let mm = MindMap::new(client, "map1", "root1");
    let node = mm.create_node("New Node", "parent123", "").await.unwrap();

    assert!(node.node_id().starts_with("created_parent123"));
}

// ─── MindMap create_child tests ─────────────────────────────────────────────

#[tokio::test]
async fn test_mindmap_create_child() {
    let mock_stub = SharedMock::new();
    {
        let mut stub = mock_stub.0.lock().await;
        stub.expect_create_child()
        .returning(|req| {
            Ok(CreateChildResponse {
                node_id: format!("created_{}", req.parent_node_id),
                node_text: req.name.clone(),
            })
        });
    }

    let client = FreeplaneClient::from_stub(mock_stub.clone(), "127.0.0.1", 50051);
    let mm = MindMap::new(client.clone(), "map1", "root1");
    let parent = Node::new(client.clone(), "parent123", Some(mm.clone()));
    let child = mm.create_child(&parent, "Child", "").await.unwrap();

    assert!(child.node_id().starts_with("created_parent123"));
}
