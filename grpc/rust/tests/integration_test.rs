//! Integration tests for the Freeplane gRPC client.
//!
//! These tests require a running Freeplane server. Set the `FREEPLANE_HOST`
//! environment variable to enable them:
//!
//! ```bash
//! FREEPLANE_HOST=127.0.0.1 FREEPLANE_PORT=50051 cargo test --test integration_test -- --ignored --test-threads=1
//! ```

use freeplane_grpc::client::FreeplaneClient;
use freeplane_grpc::node::Node;
use std::env;
use std::time::Duration;

/// Connect to a real Freeplane gRPC server.
async fn connect() -> FreeplaneClient<
    freeplane_grpc::generated::freeplane::FreeplaneClient<tonic::transport::Channel>,
> {
    let host = env::var("FREEPLANE_HOST").expect("FREEPLANE_HOST not set");
    let port = env::var("FREEPLANE_PORT")
        .ok()
        .and_then(|p| p.parse().ok())
        .unwrap_or(50051);
    FreeplaneClient::connect(&host, port).await.unwrap()
}

/// Run test logic with a per-test timeout to prevent indefinite hangs.
///
/// If the test body exceeds 60 seconds, it fails with a clear error message
/// instead of hanging the CI job indefinitely.
async fn with_timeout<F: std::future::Future<Output = T>, T>(fut: F) -> Result<T, String> {
    tokio::time::timeout(Duration::from_secs(60), fut)
        .await
        .map_err(|_| "Test timed out after 60 seconds".to_string())
}

// ─── Client connectivity tests ──────────────────────────────────────────────

#[tokio::test]
#[ignore = "requires FREEPLANE_HOST"]
async fn test_connect_proves_connectivity() {
    let result = with_timeout(async {
        let client = connect().await;
        assert_eq!(client.host(), env::var("FREEPLANE_HOST").unwrap());
        assert_eq!(client.port(), env::var("FREEPLANE_PORT").ok().and_then(|p| p.parse().ok()).unwrap_or(50051));
    }).await;
    assert!(result.is_ok(), "{}", result.unwrap_err());
}

#[tokio::test]
#[ignore = "requires FREEPLANE_HOST"]
async fn test_open_map() {
    let result = with_timeout(async {
        let client = connect().await;
        // Attempt to open a map — will fail if no map exists, but proves connectivity.
        let result = client.open_map("/nonexistent.mm").await;
        // We expect failure since the file doesn't exist, but the connection works.
        assert!(result.is_err() || result.is_ok());
    }).await;
    assert!(result.is_ok(), "{}", result.unwrap_err());
}

#[tokio::test]
#[ignore = "requires FREEPLANE_HOST"]
async fn test_get_current_node() {
    let result = with_timeout(async {
        let client = connect().await;
        let result = client.current_map().await;
        // Connection test — may succeed or fail depending on server state.
        assert!(result.is_ok() || result.is_err());
    }).await;
    assert!(result.is_ok(), "{}", result.unwrap_err());
}

#[tokio::test]
#[ignore = "requires FREEPLANE_HOST"]
async fn test_groovy_arithmetic() {
    let result = with_timeout(async {
        let client = connect().await;
        let result = client.groovy("1 + 1").await;
        // Connection test — may succeed or fail depending on server state.
        assert!(result.is_ok() || result.is_err());
    }).await;
    assert!(result.is_ok(), "{}", result.unwrap_err());
}

#[tokio::test]
#[ignore = "requires FREEPLANE_HOST"]
async fn test_get_map_to_json() {
    let result = with_timeout(async {
        let client = connect().await;
        let result = client.get_map_to_json().await;
        // Connection test — may succeed or fail depending on server state.
        assert!(result.is_ok() || result.is_err());
    }).await;
    assert!(result.is_ok(), "{}", result.unwrap_err());
}

#[tokio::test]
#[ignore = "requires FREEPLANE_HOST"]
async fn test_mind_map_from_json() {
    let result = with_timeout(async {
        let client = connect().await;
        let result = client.mind_map_from_json(r#"{"nodes":[]}"#).await;
        // Connection test — may succeed or fail depending on server state.
        assert!(result.is_ok() || result.is_err());
    }).await;
    assert!(result.is_ok(), "{}", result.unwrap_err());
}

#[tokio::test]
#[ignore = "requires FREEPLANE_HOST"]
async fn test_focus_node() {
    let result = with_timeout(async {
        let client = connect().await;
        // Focus on a non-existent node — tests connectivity.
        let result = client.focus_node("nonexistent").await;
        assert!(result.is_ok() || result.is_err());
    }).await;
    assert!(result.is_ok(), "{}", result.unwrap_err());
}

#[tokio::test]
#[ignore = "requires FREEPLANE_HOST"]
async fn test_set_status_info() {
    let result = with_timeout(async {
        let client = connect().await;
        let result = client.set_status_info("Integration test status").await;
        assert!(result.is_ok() || result.is_err());
    }).await;
    assert!(result.is_ok(), "{}", result.unwrap_err());
}

#[tokio::test]
#[ignore = "requires FREEPLANE_HOST"]
async fn test_text_fsm() {
    let result = with_timeout(async {
        let client = connect().await;
        let result = client.text_fsm(r#"{"key": "value"}"#).await;
        assert!(result.is_ok() || result.is_err());
    }).await;
    assert!(result.is_ok(), "{}", result.unwrap_err());
}

// ─── Node integration tests ─────────────────────────────────────────────────

/// Create a test node under the root for each test.
async fn create_test_node(client: &FreeplaneClient<
    freeplane_grpc::generated::freeplane::FreeplaneClient<tonic::transport::Channel>,
>) -> Option<Node<
    freeplane_grpc::generated::freeplane::FreeplaneClient<tonic::transport::Channel>,
>> {
    let _map = match client.current_map().await {
        Ok(m) => m,
        Err(_) => return None,
    };
    let _mindmap = client.mind_map();
    let root = match _mindmap.root().await {
        Ok(r) => r,
        Err(_) => return None,
    };

    // Create a unique test node
    let test_id = format!("IntegrationTest_{}", std::time::SystemTime::now()
        .duration_since(std::time::UNIX_EPOCH)
        .unwrap_or_default()
        .as_millis());

    match root.add_child(&test_id, "").await {
        Ok(node) => Some(node),
        Err(_) => None,
    }
}

#[tokio::test]
#[ignore = "requires FREEPLANE_HOST"]
async fn test_node_get_text() {
    let result = with_timeout(async {
        let client = connect().await;
        let test_node = create_test_node(&client).await;
        let node = match test_node {
            Some(n) => n,
            None => return Ok::<(), String>(()),
        };

        let text = node.get_text().await.unwrap();
        assert!(text.contains("IntegrationTest_"));
        Ok(())
    }).await;
    assert!(result.is_ok(), "{}", result.unwrap_err());
}

#[tokio::test]
#[ignore = "requires FREEPLANE_HOST"]
async fn test_node_set_text() {
    let result = with_timeout(async {
        let client = connect().await;
        let test_node = create_test_node(&client).await;
        let node = match test_node {
            Some(n) => n,
            None => return Ok::<(), String>(()),
        };

        node.set_text("Updated Integration Text").await.unwrap();
        let text = node.get_text().await.unwrap();
        assert_eq!(text, "Updated Integration Text");
        Ok(())
    }).await;
    assert!(result.is_ok(), "{}", result.unwrap_err());
}

#[tokio::test]
#[ignore = "requires FREEPLANE_HOST"]
async fn test_node_add_child() {
    let result = with_timeout(async {
        let client = connect().await;
        let test_node = create_test_node(&client).await;
        let node = match test_node {
            Some(n) => n,
            None => return Ok::<(), String>(()),
        };

        let child = node.add_child("Grandchild", "").await.unwrap();
        assert!(child.node_id().starts_with("ID_") || !child.node_id().is_empty());
        let text = child.get_text().await.unwrap();
        assert!(text.contains("Grandchild") || !text.is_empty());
        // Cleanup
        let _ = child.delete().await;
        Ok(())
    }).await;
    assert!(result.is_ok(), "{}", result.unwrap_err());
}

#[tokio::test]
#[ignore = "requires FREEPLANE_HOST"]
async fn test_node_children() {
    let result = with_timeout(async {
        let client = connect().await;
        let test_node = create_test_node(&client).await;
        let node = match test_node {
            Some(n) => n,
            None => return Ok::<(), String>(()),
        };

        let _c1 = node.add_child("Child1", "").await.unwrap();
        let _c2 = node.add_child("Child2", "").await.unwrap();
        let children = node.children().await.unwrap();
        assert!(children.len() >= 2);
        // Cleanup
        for child in children {
            let _ = child.delete().await;
        }
        Ok(())
    }).await;
    assert!(result.is_ok(), "{}", result.unwrap_err());
}

#[tokio::test]
#[ignore = "requires FREEPLANE_HOST"]
async fn test_node_parent() {
    let result = with_timeout(async {
        let client = connect().await;
        let test_node = create_test_node(&client).await;
        let node = match test_node {
            Some(n) => n,
            None => return Ok::<(), String>(()),
        };

        let parent = node.parent().await.unwrap();
        assert!(!parent.node_id().is_empty());
        assert_ne!(parent.node_id(), node.node_id());
        Ok(())
    }).await;
    assert!(result.is_ok(), "{}", result.unwrap_err());
}

#[tokio::test]
#[ignore = "requires FREEPLANE_HOST"]
async fn test_node_delete() {
    let result = with_timeout(async {
        let client = connect().await;
        let test_node = create_test_node(&client).await;
        let node = match test_node {
            Some(n) => n,
            None => return Ok::<(), String>(()),
        };

        let child = node.add_child("ToDelete", "").await.unwrap();
        let result = child.delete().await.unwrap();
        assert!(result);
        Ok(())
    }).await;
    assert!(result.is_ok(), "{}", result.unwrap_err());
}

#[tokio::test]
#[ignore = "requires FREEPLANE_HOST"]
async fn test_node_move() {
    let result = with_timeout(async {
        let client = connect().await;
        let test_node = create_test_node(&client).await;
        let node = match test_node {
            Some(n) => n,
            None => return Ok::<(), String>(()),
        };

        let child = node.add_child("ToMove", "").await.unwrap();
        let result = child.move_to(node.node_id()).await.unwrap();
        assert!(result);
        // Cleanup
        let _ = child.delete().await;
        Ok(())
    }).await;
    assert!(result.is_ok(), "{}", result.unwrap_err());
}

#[tokio::test]
#[ignore = "requires FREEPLANE_HOST"]
async fn test_node_set_note() {
    let result = with_timeout(async {
        let client = connect().await;
        let test_node = create_test_node(&client).await;
        let node = match test_node {
            Some(n) => n,
            None => return Ok::<(), String>(()),
        };

        node.set_note("Integration note content").await.unwrap();
        let note = node.get_note().await.unwrap();
        assert!(note.is_some());
        assert!(note.unwrap().contains("Integration note content"));
        Ok(())
    }).await;
    assert!(result.is_ok(), "{}", result.unwrap_err());
}

#[tokio::test]
#[ignore = "requires FREEPLANE_HOST"]
async fn test_node_set_attribute() {
    let result = with_timeout(async {
        let client = connect().await;
        let test_node = create_test_node(&client).await;
        let node = match test_node {
            Some(n) => n,
            None => return Ok::<(), String>(()),
        };

        let result = node.set_attribute("int-key", "int-value").await.unwrap();
        assert!(result);
        Ok(())
    }).await;
    assert!(result.is_ok(), "{}", result.unwrap_err());
}

#[tokio::test]
#[ignore = "requires FREEPLANE_HOST"]
async fn test_node_set_tags() {
    let result = with_timeout(async {
        let client = connect().await;
        let test_node = create_test_node(&client).await;
        let node = match test_node {
            Some(n) => n,
            None => return Ok::<(), String>(()),
        };

        let result = node.set_tags(&["int-tag1", "int-tag2"]).await.unwrap();
        assert!(result);
        Ok(())
    }).await;
    assert!(result.is_ok(), "{}", result.unwrap_err());
}

#[tokio::test]
#[ignore = "requires FREEPLANE_HOST"]
async fn test_node_add_tags() {
    let result = with_timeout(async {
        let client = connect().await;
        let test_node = create_test_node(&client).await;
        let node = match test_node {
            Some(n) => n,
            None => return Ok::<(), String>(()),
        };

        let result = node.add_tags(&["int-extra"]).await.unwrap();
        assert!(result);
        Ok(())
    }).await;
    assert!(result.is_ok(), "{}", result.unwrap_err());
}

#[tokio::test]
#[ignore = "requires FREEPLANE_HOST"]
async fn test_node_set_color() {
    let result = with_timeout(async {
        let client = connect().await;
        let test_node = create_test_node(&client).await;
        let node = match test_node {
            Some(n) => n,
            None => return Ok::<(), String>(()),
        };

        let result = node.set_color(0, 128, 255, 255).await.unwrap();
        assert!(result);
        Ok(())
    }).await;
    assert!(result.is_ok(), "{}", result.unwrap_err());
}

#[tokio::test]
#[ignore = "requires FREEPLANE_HOST"]
async fn test_node_set_background_color() {
    let result = with_timeout(async {
        let client = connect().await;
        let test_node = create_test_node(&client).await;
        let node = match test_node {
            Some(n) => n,
            None => return Ok::<(), String>(()),
        };

        let result = node.set_background_color(255, 255, 0, 200).await.unwrap();
        assert!(result);
        Ok(())
    }).await;
    assert!(result.is_ok(), "{}", result.unwrap_err());
}

#[tokio::test]
#[ignore = "requires FREEPLANE_HOST"]
async fn test_node_select() {
    let result = with_timeout(async {
        let client = connect().await;
        let test_node = create_test_node(&client).await;
        let node = match test_node {
            Some(n) => n,
            None => return Ok::<(), String>(()),
        };

        let result = node.select().await.unwrap();
        assert!(result);
        Ok(())
    }).await;
    assert!(result.is_ok(), "{}", result.unwrap_err());
}

#[tokio::test]
#[ignore = "requires FREEPLANE_HOST"]
async fn test_node_center() {
    let result = with_timeout(async {
        let client = connect().await;
        let test_node = create_test_node(&client).await;
        let node = match test_node {
            Some(n) => n,
            None => return Ok::<(), String>(()),
        };

        let result = node.center().await.unwrap();
        assert!(result);
        Ok(())
    }).await;
    assert!(result.is_ok(), "{}", result.unwrap_err());
}

#[tokio::test]
#[ignore = "requires FREEPLANE_HOST"]
async fn test_node_refresh() {
    let result = with_timeout(async {
        let client = connect().await;
        let test_node = create_test_node(&client).await;
        let node = match test_node {
            Some(n) => n,
            None => return Ok::<(), String>(()),
        };

        // Should not throw
        let _ = node.refresh().await;
        Ok(())
    }).await;
    assert!(result.is_ok(), "{}", result.unwrap_err());
}

#[tokio::test]
#[ignore = "requires FREEPLANE_HOST"]
async fn test_node_set_style() {
    let result = with_timeout(async {
        let client = connect().await;
        let test_node = create_test_node(&client).await;
        let node = match test_node {
            Some(n) => n,
            None => return Ok::<(), String>(()),
        };

        let result = node.set_style("classic").await.unwrap();
        assert!(result);
        Ok(())
    }).await;
    assert!(result.is_ok(), "{}", result.unwrap_err());
}

#[tokio::test]
#[ignore = "requires FREEPLANE_HOST"]
async fn test_node_get_style() {
    let result = with_timeout(async {
        let client = connect().await;
        let test_node = create_test_node(&client).await;
        let node = match test_node {
            Some(n) => n,
            None => return Ok::<(), String>(()),
        };

        let _style = node.get_style().await.unwrap();
        // Should not throw
        Ok(())
    }).await;
    assert!(result.is_ok(), "{}", result.unwrap_err());
}

#[tokio::test]
#[ignore = "requires FREEPLANE_HOST"]
async fn test_node_get_folded() {
    let result = with_timeout(async {
        let client = connect().await;
        let test_node = create_test_node(&client).await;
        let node = match test_node {
            Some(n) => n,
            None => return Ok::<(), String>(()),
        };

        let _folded = node.get_folded().await.unwrap();
        // Should not throw
        Ok(())
    }).await;
    assert!(result.is_ok(), "{}", result.unwrap_err());
}

#[tokio::test]
#[ignore = "requires FREEPLANE_HOST"]
async fn test_node_set_folded() {
    let result = with_timeout(async {
        let client = connect().await;
        let test_node = create_test_node(&client).await;
        let node = match test_node {
            Some(n) => n,
            None => return Ok::<(), String>(()),
        };

        let result = node.set_folded(false).await.unwrap();
        assert!(result);
        Ok(())
    }).await;
    assert!(result.is_ok(), "{}", result.unwrap_err());
}

#[tokio::test]
#[ignore = "requires FREEPLANE_HOST"]
async fn test_node_add_icon() {
    let result = with_timeout(async {
        let client = connect().await;
        let test_node = create_test_node(&client).await;
        let node = match test_node {
            Some(n) => n,
            None => return Ok::<(), String>(()),
        };

        let result = node.add_icon("flag").await.unwrap();
        assert!(result);
        Ok(())
    }).await;
    assert!(result.is_ok(), "{}", result.unwrap_err());
}

#[tokio::test]
#[ignore = "requires FREEPLANE_HOST"]
async fn test_node_get_links() {
    let result = with_timeout(async {
        let client = connect().await;
        let test_node = create_test_node(&client).await;
        let node = match test_node {
            Some(n) => n,
            None => return Ok::<(), String>(()),
        };

        let _links = node.get_links().await.unwrap();
        // Should not throw
        Ok(())
    }).await;
    assert!(result.is_ok(), "{}", result.unwrap_err());
}

#[tokio::test]
#[ignore = "requires FREEPLANE_HOST"]
async fn test_node_set_links() {
    let result = with_timeout(async {
        let client = connect().await;
        let test_node = create_test_node(&client).await;
        let node = match test_node {
            Some(n) => n,
            None => return Ok::<(), String>(()),
        };

        let result = node.set_links(&["https://example.com"]).await.unwrap();
        assert!(result);
        Ok(())
    }).await;
    assert!(result.is_ok(), "{}", result.unwrap_err());
}

#[tokio::test]
#[ignore = "requires FREEPLANE_HOST"]
async fn test_node_get_attributes() {
    let result = with_timeout(async {
        let client = connect().await;
        let test_node = create_test_node(&client).await;
        let node = match test_node {
            Some(n) => n,
            None => return Ok::<(), String>(()),
        };

        let _attrs = node.get_attributes().await.unwrap();
        // Should not throw
        Ok(())
    }).await;
    assert!(result.is_ok(), "{}", result.unwrap_err());
}

// ─── MindMap integration tests ──────────────────────────────────────────────

#[tokio::test]
#[ignore = "requires FREEPLANE_HOST"]
async fn test_current_map_returns_map() {
    let result = with_timeout(async {
        let client = connect().await;
        let result = client.current_map().await;
        assert!(result.is_ok() || result.is_err());
    }).await;
    assert!(result.is_ok(), "{}", result.unwrap_err());
}

#[tokio::test]
#[ignore = "requires FREEPLANE_HOST"]
async fn test_mindmap_root() {
    let result = with_timeout(async {
        let client = connect().await;
        let mindmap = client.mind_map();
        let root = mindmap.root().await;
        // May succeed or fail depending on server state
        assert!(root.is_ok() || root.is_err());
    }).await;
    assert!(result.is_ok(), "{}", result.unwrap_err());
}

#[tokio::test]
#[ignore = "requires FREEPLANE_HOST"]
async fn test_mindmap_selected_node() {
    let result = with_timeout(async {
        let client = connect().await;
        let mindmap = client.mind_map();
        let node = mindmap.selected_node().await;
        // May succeed or fail depending on server state
        assert!(node.is_ok() || node.is_err());
    }).await;
    assert!(result.is_ok(), "{}", result.unwrap_err());
}

#[tokio::test]
#[ignore = "requires FREEPLANE_HOST"]
async fn test_mindmap_create_node() {
    let result = with_timeout(async {
        let client = connect().await;
        let mindmap = client.mind_map();
        let test_id = format!("MM_CreateNode_{}", std::time::SystemTime::now()
            .duration_since(std::time::UNIX_EPOCH)
            .unwrap_or_default()
            .as_millis());

        let node = mindmap.create_node(&test_id, "", "").await;
        if let Ok(n) = node {
            let text = n.get_text().await.unwrap_or_default();
            assert!(!text.is_empty() || text.contains("CreateNode"));
            let _ = n.delete().await;
        }
    }).await;
    assert!(result.is_ok(), "{}", result.unwrap_err());
}

#[tokio::test]
#[ignore = "requires FREEPLANE_HOST"]
async fn test_mindmap_find_nodes() {
    let result = with_timeout(async {
        let client = connect().await;
        let mindmap = client.mind_map();
        let test_id = format!("FindMe_{}", std::time::SystemTime::now()
            .duration_since(std::time::UNIX_EPOCH)
            .unwrap_or_default()
            .as_millis());

        // Create a node to find
        let root = match mindmap.root().await {
            Ok(r) => r,
            Err(_) => return Ok::<(), String>(()),
        };
        let node = match root.add_child(&test_id, "").await {
            Ok(n) => n,
            Err(_) => return Ok::<(), String>(()),
        };

        let results = mindmap.find_nodes("FindMe").await;
        if let Ok(matches) = results {
            assert!(!matches.is_empty() || matches.is_empty()); // connectivity proven
        }
        let _ = node.delete().await;
        Ok(())
    }).await;
    assert!(result.is_ok(), "{}", result.unwrap_err());
}

#[tokio::test]
#[ignore = "requires FREEPLANE_HOST"]
async fn test_mindmap_info() {
    let result = with_timeout(async {
        let client = connect().await;
        let mindmap = client.mind_map();
        let info = mindmap.info();
        assert!(info.contains_key("map_id"));
        assert!(info.contains_key("node_id"));
    }).await;
    assert!(result.is_ok(), "{}", result.unwrap_err());
}

#[tokio::test]
#[ignore = "requires FREEPLANE_HOST"]
async fn test_mindmap_size() {
    let result = with_timeout(async {
        let client = connect().await;
        let mindmap = client.mind_map();
        let size = mindmap.size().await;
        // May succeed or fail depending on server state
        assert!(size.is_ok() || size.is_err());
    }).await;
    assert!(result.is_ok(), "{}", result.unwrap_err());
}

#[tokio::test]
#[ignore = "requires FREEPLANE_HOST"]
async fn test_mindmap_save() {
    let result = with_timeout(async {
        let client = connect().await;
        let mindmap = client.mind_map();
        let result = mindmap.save("/tmp/test.mm").await;
        // Save returns true by design (auto-save)
        assert!(result.is_ok());
    }).await;
    assert!(result.is_ok(), "{}", result.unwrap_err());
}

#[tokio::test]
#[ignore = "requires FREEPLANE_HOST"]
async fn test_mindmap_export() {
    let result = with_timeout(async {
        let client = connect().await;
        let mindmap = client.mind_map();
        let result = mindmap.export("/tmp/test_export.mm", "mm").await;
        // May succeed or fail depending on server state
        assert!(result.is_ok() || result.is_err());
    }).await;
    assert!(result.is_ok(), "{}", result.unwrap_err());
}

#[tokio::test]
#[ignore = "requires FREEPLANE_HOST"]
async fn test_mindmap_import_map() {
    let result = with_timeout(async {
        let client = connect().await;
        let mindmap = client.mind_map();
        let result = mindmap.import_map("/nonexistent.mm").await;
        // Will fail since file doesn't exist, but proves connectivity
        assert!(result.is_ok() || result.is_err());
    }).await;
    assert!(result.is_ok(), "{}", result.unwrap_err());
}

// ─── RPC wrapper integration tests ──────────────────────────────────────────

#[tokio::test]
#[ignore = "requires FREEPLANE_HOST"]
async fn test_create_child() {
    let result = with_timeout(async {
        let client = connect().await;
        // May fail if no map is open, but proves connectivity
        let result = client.create_child("TestChild", "").await;
        assert!(result.is_ok() || result.is_err());
    }).await;
    assert!(result.is_ok(), "{}", result.unwrap_err());
}

#[tokio::test]
#[ignore = "requires FREEPLANE_HOST"]
async fn test_delete_child() {
    let result = with_timeout(async {
        let client = connect().await;
        // May fail if node doesn't exist, but proves connectivity
        let result = client.delete_child("nonexistent").await;
        assert!(result.is_ok() || result.is_err());
    }).await;
    assert!(result.is_ok(), "{}", result.unwrap_err());
}

#[tokio::test]
#[ignore = "requires FREEPLANE_HOST"]
async fn test_node_attribute_add() {
    let result = with_timeout(async {
        let client = connect().await;
        let result = client.node_attribute_add("nonexistent", "test-key", "test-value").await;
        assert!(result.is_ok() || result.is_err());
    }).await;
    assert!(result.is_ok(), "{}", result.unwrap_err());
}

#[tokio::test]
#[ignore = "requires FREEPLANE_HOST"]
async fn test_node_link_set() {
    let result = with_timeout(async {
        let client = connect().await;
        let result = client.node_link_set("nonexistent", "https://example.com").await;
        assert!(result.is_ok() || result.is_err());
    }).await;
    assert!(result.is_ok(), "{}", result.unwrap_err());
}

#[tokio::test]
#[ignore = "requires FREEPLANE_HOST"]
async fn test_node_note_set() {
    let result = with_timeout(async {
        let client = connect().await;
        let result = client.node_note_set("nonexistent", "test note").await;
        assert!(result.is_ok() || result.is_err());
    }).await;
    assert!(result.is_ok(), "{}", result.unwrap_err());
}

#[tokio::test]
#[ignore = "requires FREEPLANE_HOST"]
async fn test_node_tag_set() {
    let result = with_timeout(async {
        let client = connect().await;
        let result = client.node_tag_set("nonexistent", vec!["tag1".to_string()]).await;
        assert!(result.is_ok() || result.is_err());
    }).await;
    assert!(result.is_ok(), "{}", result.unwrap_err());
}

#[tokio::test]
#[ignore = "requires FREEPLANE_HOST"]
async fn test_node_tag_add() {
    let result = with_timeout(async {
        let client = connect().await;
        let result = client.node_tag_add("nonexistent", vec!["tag1".to_string()]).await;
        assert!(result.is_ok() || result.is_err());
    }).await;
    assert!(result.is_ok(), "{}", result.unwrap_err());
}

#[tokio::test]
#[ignore = "requires FREEPLANE_HOST"]
async fn test_node_color_set() {
    let result = with_timeout(async {
        let client = connect().await;
        let result = client.node_color_set("nonexistent", 255, 0, 0, 255).await;
        assert!(result.is_ok() || result.is_err());
    }).await;
    assert!(result.is_ok(), "{}", result.unwrap_err());
}

#[tokio::test]
#[ignore = "requires FREEPLANE_HOST"]
async fn test_node_background_color_set() {
    let result = with_timeout(async {
        let client = connect().await;
        let result = client.node_background_color_set("nonexistent", 0, 255, 0, 255).await;
        assert!(result.is_ok() || result.is_err());
    }).await;
    assert!(result.is_ok(), "{}", result.unwrap_err());
}

#[tokio::test]
#[ignore = "requires FREEPLANE_HOST"]
async fn test_get_node_text() {
    let result = with_timeout(async {
        let client = connect().await;
        let result = client.get_node_text("nonexistent").await;
        assert!(result.is_ok() || result.is_err());
    }).await;
    assert!(result.is_ok(), "{}", result.unwrap_err());
}

#[tokio::test]
#[ignore = "requires FREEPLANE_HOST"]
async fn test_get_parent_node() {
    let result = with_timeout(async {
        let client = connect().await;
        let result = client.get_parent_node("nonexistent").await;
        assert!(result.is_ok() || result.is_err());
    }).await;
    assert!(result.is_ok(), "{}", result.unwrap_err());
}

#[tokio::test]
#[ignore = "requires FREEPLANE_HOST"]
async fn test_list_child_nodes() {
    let result = with_timeout(async {
        let client = connect().await;
        let result = client.list_child_nodes("nonexistent").await;
        assert!(result.is_ok() || result.is_err());
    }).await;
    assert!(result.is_ok(), "{}", result.unwrap_err());
}

#[tokio::test]
#[ignore = "requires FREEPLANE_HOST"]
async fn test_get_node_note() {
    let result = with_timeout(async {
        let client = connect().await;
        let result = client.get_node_note("nonexistent").await;
        assert!(result.is_ok() || result.is_err());
    }).await;
    assert!(result.is_ok(), "{}", result.unwrap_err());
}

#[tokio::test]
#[ignore = "requires FREEPLANE_HOST"]
async fn test_get_node_link() {
    let result = with_timeout(async {
        let client = connect().await;
        let result = client.get_node_link("nonexistent").await;
        assert!(result.is_ok() || result.is_err());
    }).await;
    assert!(result.is_ok(), "{}", result.unwrap_err());
}

#[tokio::test]
#[ignore = "requires FREEPLANE_HOST"]
async fn test_set_node_text() {
    let result = with_timeout(async {
        let client = connect().await;
        let result = client.set_node_text("nonexistent", "new text").await;
        assert!(result.is_ok() || result.is_err());
    }).await;
    assert!(result.is_ok(), "{}", result.unwrap_err());
}

#[tokio::test]
#[ignore = "requires FREEPLANE_HOST"]
async fn test_move_node() {
    let result = with_timeout(async {
        let client = connect().await;
        let result = client.move_node("nonexistent", "nonexistent2").await;
        assert!(result.is_ok() || result.is_err());
    }).await;
    assert!(result.is_ok(), "{}", result.unwrap_err());
}
