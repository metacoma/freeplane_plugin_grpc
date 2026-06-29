//! freeplane_llm — Convert text to a Freeplane mind map using an LLM.
//!
//! This CLI tool:
//! 1. Accepts text input via `--text` or `--from-file` (stdin)
//! 2. Sends the text to an OpenAI-compatible LLM with a structured output schema
//! 3. Imports the LLM's JSON response into Freeplane via the gRPC plugin

mod input;
mod llm;
mod schema;

use std::process;

use async_openai::Client;
use clap::Parser;
use freeplane_grpc::FreeplaneClient;
use thiserror::Error;

use crate::input::{CliArgs, InputError, read_input};
use crate::llm::{LlmError, call_llm};
use crate::schema::MindMapNode;

/// Errors that can occur during the main pipeline.
#[derive(Debug, Error)]
enum AppError {
    #[error("Input error: {0}")]
    Input(#[from] InputError),

    #[error("LLM error: {0}")]
    Llm(#[from] LlmError),

    #[error("Connection error: {0}")]
    Connection(String),

    #[error("Import error: {0}")]
    Import(String),

    #[error("Configuration error: {0}")]
    Config(String),
}

#[tokio::main]
async fn main() {
    if let Err(e) = run().await {
        eprintln!("Error: {}", e);
        process::exit(1);
    }
}

async fn run() -> Result<(), AppError> {
    // Parse CLI arguments
    let args = CliArgs::parse();

    // Validate API key
    if args.openai_api_key.is_none() {
        return Err(AppError::Config(
            "OPENAI_API_KEY environment variable is not set. \
             Set it with: export OPENAI_API_KEY='your-key'"
                .to_string(),
        ));
    }

    // Read input text
    let input_text = read_input(&args).await?;

    if input_text.trim().is_empty() {
        return Err(AppError::Input(InputError::NoInputSpecified));
    }

    eprintln!("Sending text to LLM (model: {})...", args.model);

    // Build OpenAI client
    // Priority: llm_server > openai_base_url > default
    let base_url = args
        .llm_server
        .clone()
        .or(args.openai_base_url.clone())
        .unwrap_or_else(|| "https://api.openai.com/v1".to_string());

    let config = async_openai::config::OpenAIConfig::new()
        .with_api_key(args.openai_api_key.as_deref().unwrap_or(""))
        .with_api_base(base_url);
    let client = Client::with_config(config);

    // Call LLM
    let mindmap_node = call_llm(&client, &args.model, &input_text).await?;

    eprintln!("LLM response received. Importing into Freeplane...");

    // Wrap the response in the Freeplane import format
    let import_json = MindMapNode::wrap_for_import(&mindmap_node);

    // Connect to Freeplane
    let mut grpc_client = FreeplaneClient::connect(&args.freeplane_host, args.freeplane_port as u32)
        .await
        .map_err(|e| AppError::Connection(format!("Failed to connect to Freeplane: {}", e)))?;

    // Import the mind map
    let success = grpc_client
        .mind_map_from_json(&import_json)
        .await
        .map_err(|e| AppError::Import(format!("Failed to import mind map: {}", e)))?;

    if success {
        println!(
            "Mind map imported successfully into Freeplane at {}:{}!",
            args.freeplane_host, args.freeplane_port
        );
    } else {
        return Err(AppError::Import("Import returned failure".to_string()));
    }

    grpc_client.close().await;
    Ok(())
}
