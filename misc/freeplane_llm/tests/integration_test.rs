//! Integration tests for freeplane_llm.
//!
//! These tests use a fake `axum` LLM server and a real Freeplane instance.
//! Set the `FREEPLANE_HOST` and `FREEPLANE_PORT` environment variables to enable them:
//!
//! ```bash
//! FREEPLANE_HOST=127.0.0.1 FREEPLANE_PORT=50051 \
//!   cargo test --test integration_test -- --ignored --test-threads=1
//! ```

use async_openai::types::chat::{
    ChatCompletionRequestSystemMessage, ChatCompletionRequestUserMessage,
    ChatCompletionResponseMessage, CreateChatCompletionRequestArgs, Role,
};
use axum::extract::Json;
use axum::http::StatusCode;
use axum::response::IntoResponse;
use axum::routing::post;
use axum::Router;
use freeplane_grpc::FreeplaneClient;
use serde::{Deserialize, Serialize};
use serde_json::json;
use std::net::SocketAddr;
use std::time::Duration;
use tokio::net::TcpListener;
use tokio::time::timeout;

/// Fake LLM chat completion request (we only inspect messages).
#[derive(Debug, Deserialize)]
struct FakeChatRequest {
    messages: Vec<serde_json::Value>,
    model: Option<String>,
    response_format: Option<serde_json::Value>,
}

/// Fake LLM chat completion response.
#[derive(Debug, Serialize)]
struct FakeChatResponse {
    choices: Vec<FakeChoice>,
}

#[derive(Debug, Serialize)]
struct FakeChoice {
    index: u32,
    message: ChatCompletionResponseMessage,
}

/// The mind map node structure we return from the fake LLM.
fn fake_mindmap_response() -> serde_json::Value {
    json!({
        "text": "Fake LLM Mind Map",
        "children": [
            {
                "text": "Branch 1",
                "detail": "Auto-generated detail for branch 1"
            },
            {
                "text": "Branch 2",
                "children": [
                    {
                        "text": "Sub-branch 2.1",
                        "tags": ["auto", "generated"]
                    }
                ]
            }
        ]
    })
}

/// Start a fake LLM server on a random available port.
/// Returns the `SocketAddr` and the `Router` (which must be kept alive).
async fn start_fake_llm_server() -> (SocketAddr, Router) {
    let mindmap = fake_mindmap_response();

    let app = Router::new().route(
        "/v1/chat/completions",
        post(move |body: Json<FakeChatRequest>| {
            let mindmap = mindmap.clone();
            async move {
                let response = FakeChatResponse {
                    choices: vec![FakeChoice {
                        index: 0,
                        message: ChatCompletionResponseMessage {
                            content: Some(serde_json::to_string(&mindmap).unwrap()),
                            refusal: None,
                            role: Role::Assistant,
                            tool_calls: None,
                            annotations: None,
                            function_call: None,
                            audio: None,
                        },
                    }],
                };
                (StatusCode::OK, Json(response)).into_response()
            }
        }),
    );

    let listener = TcpListener::bind("127.0.0.1:0").await.unwrap();
    let addr = listener.local_addr().unwrap();
    let server_app = app.clone();

    tokio::spawn(async move {
        let _ = axum::serve(listener, server_app).await;
    });

    (addr, app)
}

/// Connect to a real Freeplane gRPC server.
async fn connect_to_freeplane() -> FreeplaneClient<
    freeplane_grpc::generated::freeplane::FreeplaneClient<tonic::transport::Channel>,
> {
    let host = std::env::var("FREEPLANE_HOST").expect("FREEPLANE_HOST not set");
    let port = std::env::var("FREEPLANE_PORT")
        .ok()
        .and_then(|p| p.parse().ok())
        .unwrap_or(50051);
    FreeplaneClient::connect(&host, port).await.unwrap()
}

/// Run test logic with a per-test timeout to prevent indefinite hangs.
async fn with_timeout<F: std::future::Future<Output = T>, T>(fut: F) -> Result<T, String> {
    timeout(Duration::from_secs(90), fut)
        .await
        .map_err(|_| "Test timed out after 90 seconds".to_string())
}

// ─── Integration tests ──────────────────────────────────────────────────────

/// Test that the fake LLM server returns a valid mind map response.
#[tokio::test]
#[ignore = "requires FREEPLANE_HOST"]
async fn test_fake_llm_server_returns_valid_response() {
    let result = with_timeout(async {
        let (addr, _server) = start_fake_llm_server().await;
        let port = addr.port();

        // Use async-openai client to call the fake server
        let config = async_openai::config::OpenAIConfig::new()
            .with_api_base(format!("http://127.0.0.1:{}", port))
            .with_api_key("fake-key");
        let client = async_openai::Client::with_config(config);

        let request = CreateChatCompletionRequestArgs::default()
            .model("gpt-4o")
            .messages([
                ChatCompletionRequestSystemMessage::from("You are a mind map generator.").into(),
                ChatCompletionRequestUserMessage::from("Create a mind map about Rust programming.").into(),
            ])
            .max_tokens(512u32)
            .build()
            .unwrap();

        let response = client.chat().create(request).await.unwrap();
        let choice = response.choices.first().expect("Expected at least one choice");
        let content = choice.message.content.as_ref().expect("Expected content");

        // Parse and validate the response
        let value: serde_json::Value = serde_json::from_str(content).unwrap();
        assert_eq!(value["text"], "Fake LLM Mind Map");
        assert_eq!(value["children"].as_array().unwrap().len(), 2);
    })
    .await;
    assert!(result.is_ok(), "{}", result.unwrap_err());
}

/// Test the full pipeline: fake LLM → JSON parsing → Freeplane import.
#[tokio::test]
#[ignore = "requires FREEPLANE_HOST"]
async fn test_full_pipeline_fake_llm_to_freeplane() {
    let result = with_timeout(async {
        let (addr, _server) = start_fake_llm_server().await;
        let port = addr.port();

        // Step 1: Call the fake LLM
        let config = async_openai::config::OpenAIConfig::new()
            .with_api_base(format!("http://127.0.0.1:{}", port))
            .with_api_key("fake-key");
        let client = async_openai::Client::with_config(config);

        let request = CreateChatCompletionRequestArgs::default()
            .model("gpt-4o")
            .messages([
                ChatCompletionRequestSystemMessage::from("You are a mind map generator.").into(),
                ChatCompletionRequestUserMessage::from("Integration test input text.").into(),
            ])
            .max_tokens(512u32)
            .build()
            .unwrap();

        let response = client.chat().create(request).await.unwrap();
        let choice = response.choices.first().expect("Expected at least one choice");
        let content = choice.message.content.as_ref().expect("Expected content");

        // Step 2: Parse the LLM response
        let mindmap_node: serde_json::Value = serde_json::from_str(content).unwrap();
        assert_eq!(mindmap_node["text"], "Fake LLM Mind Map");

        // Step 3: Wrap in import format
        let import_json = serde_json::json!({
            "_fp_import_root_node": "root",
            "mindmap": mindmap_node
        });
        let import_string = serde_json::to_string_pretty(&import_json).unwrap();

        // Step 4: Import into Freeplane
        let freeplane = connect_to_freeplane().await;

        // Clear existing children first for clean state
        let mindmap = freeplane.mind_map();
        if let Ok(root) = mindmap.root().await {
            if let Ok(children) = root.children().await {
                for child in children {
                    let _ = child.delete().await;
                }
            }
        }

        // Import the mind map
        let success = freeplane.mind_map_from_json(&import_string).await.unwrap();
        assert!(success, "Mind map import should succeed");

        // Step 5: Verify the import by reading back
        let mindmap2 = freeplane.mind_map();
        let root = mindmap2.root().await.unwrap();
        let children = root.children().await.unwrap();

        // The import creates a child node under root
        assert!(!children.is_empty(), "Should have at least one child node");

        // Find the imported node by its text
        let imported_node = children
            .iter()
            .find(|n| {
                let text = futures::executor::block_on(n.get_text()).unwrap_or_default();
                text.contains("Fake LLM Mind Map")
            })
            .expect("Should find the imported node");

        let text = futures::executor::block_on(imported_node.get_text()).unwrap();
        assert!(text.contains("Fake LLM Mind Map"));

        // Verify children were imported
        let child_nodes = futures::executor::block_on(imported_node.children()).unwrap();
        assert_eq!(child_nodes.len(), 2, "Should have 2 child branches");
    })
    .await;
    assert!(result.is_ok(), "{}", result.unwrap_err());
}

/// Test that the import JSON format matches the canonical Freeplane format.
#[tokio::test]
#[ignore = "requires FREEPLANE_HOST"]
async fn test_import_format_matches_canonical() {
    let result = with_timeout(async {
        let (addr, _server) = start_fake_llm_server().await;
        let port = addr.port();

        // Get LLM response
        let config = async_openai::config::OpenAIConfig::new()
            .with_api_base(format!("http://127.0.0.1:{}", port))
            .with_api_key("fake-key");
        let client = async_openai::Client::with_config(config);

        let request = CreateChatCompletionRequestArgs::default()
            .model("gpt-4o")
            .messages([
                ChatCompletionRequestSystemMessage::from("You are a mind map generator.").into(),
                ChatCompletionRequestUserMessage::from("Test format.").into(),
            ])
            .max_tokens(512u32)
            .build()
            .unwrap();

        let response = client.chat().create(request).await.unwrap();
        let content = response.choices[0]
            .message
            .content
            .as_ref()
            .expect("Expected content");

        let mindmap_node: serde_json::Value = serde_json::from_str(content).unwrap();

        // Wrap in import format
        let import_json = serde_json::json!({
            "_fp_import_root_node": "root",
            "mindmap": mindmap_node
        });

        // Connect to Freeplane and import
        let freeplane = connect_to_freeplane().await;

        // Clear existing children
        let mindmap = freeplane.mind_map();
        if let Ok(root) = mindmap.root().await {
            if let Ok(children) = root.children().await {
                for child in children {
                    let _ = child.delete().await;
                }
            }
        }

        let success = freeplane.mind_map_from_json(&serde_json::to_string_pretty(&import_json).unwrap()).await.unwrap();
        assert!(success);

        // Export and verify the structure
        let json_export = freeplane.get_map_to_json().await.unwrap();
        let exported: serde_json::Value = serde_json::from_str(&json_export).unwrap();

        // The exported format should have a 'text' field (canonical format)
        assert!(exported.is_object(), "Exported JSON should be an object");
        assert!(
            exported.as_object().unwrap().contains_key("text"),
            "Exported JSON should have 'text' field (canonical format)"
        );
    })
    .await;
    assert!(result.is_ok(), "{}", result.unwrap_err());
}
