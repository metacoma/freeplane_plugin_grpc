//! LLM interaction module.
//!
//! Handles building prompts, calling the OpenAI-compatible API with structured outputs,
//! and parsing the JSON response.

use async_openai::types::chat::{
    ChatCompletionRequestSystemMessage, ChatCompletionRequestUserMessage,
    CreateChatCompletionRequestArgs, ResponseFormat, ResponseFormatJsonSchema,
};
use async_openai::Client;
use thiserror::Error;

use crate::schema::MindMapNode;

/// Errors that can occur during LLM interaction.
#[derive(Debug, Error)]
pub enum LlmError {
    #[error("API key not configured: set OPENAI_API_KEY environment variable")]
    ApiKeyNotConfigured,

    #[error("LLM API call failed: {0}")]
    ApiCallFailed(String),

    #[error("Failed to parse LLM response as JSON: {0}")]
    ParseError(#[from] serde_json::Error),

    #[error("LLM response does not contain a valid mind map node (missing 'text' field)")]
    InvalidResponse,

    #[error("No content in LLM response")]
    NoContent,

    #[error("Failed to build request: {0}")]
    BuildError(String),
}

/// Build the system prompt that includes the JSON Schema and instructions.
pub fn build_system_prompt() -> String {
    format!(
        "You are a mind map generation assistant. Your task is to analyze input text and create \
        a hierarchical mind map representation.\n\n\
        Return a JSON object matching this schema (the root node of the mind map):\n\
        {}\n\n\
        Instructions:\n\
        1. Analyze the input text and identify key concepts and their relationships.\n\
        2. Create a hierarchical mind map with a root node and child nodes.\n\
        3. Use only the fields defined in the schema above.\n\
        4. Do NOT include 'id' or 'relationships' fields — these are managed by the importing system.\n\
        5. Limit the mind map depth to 3-4 levels.\n\
        6. The 'text' field is required for every node.\n\
        7. Use the 'detail' field to add explanatory text when helpful.\n\
        8. Use the 'note' field for additional notes.\n\
        9. Use the 'link' field for relevant URLs.\n\
        10. Use the 'tags' field for categorization.\n\
        11. Return ONLY valid JSON — no markdown code fences, no explanation, no backticks.\n\
        12. The root node should represent the main topic of the input text.",
        MindMapNode::json_schema()
    )
}

/// Build the user message containing the input text.
pub fn build_user_prompt(text: &str) -> String {
    format!(
        "Create a mind map for the following text:\n\n{}\n\nReturn only the JSON mind map.",
        text
    )
}

/// Call the LLM API with structured output and return the parsed JSON response.
///
/// The response is expected to be a single `MindMapNode` (the root of the mind map).
pub async fn call_llm(
    client: &Client<async_openai::config::OpenAIConfig>,
    model: &str,
    text: &str,
) -> Result<serde_json::Value, LlmError> {
    let system_prompt = build_system_prompt();
    let user_prompt = build_user_prompt(text);

    // Generate the JSON schema for structured output
    let schema: serde_json::Value = serde_json::from_str(&MindMapNode::json_schema())
        .map_err(|e| LlmError::BuildError(format!("Failed to parse schema: {}", e)))?;

    let response_format = ResponseFormat::JsonSchema {
        json_schema: ResponseFormatJsonSchema {
            name: "mind_map_node".into(),
            description: Some("A single node in a Freeplane-compatible mind map".to_string()),
            schema: Some(schema),
            strict: Some(true),
        },
    };

    let request = CreateChatCompletionRequestArgs::default()
        .model(model)
        .messages([
            ChatCompletionRequestSystemMessage::from(system_prompt).into(),
            ChatCompletionRequestUserMessage::from(user_prompt).into(),
        ])
        .response_format(response_format)
        .max_tokens(4096u32)
        .build()
        .map_err(|e| LlmError::BuildError(format!("Failed to build request: {}", e)))?;

    let response = client.chat().create(request).await.map_err(|e| {
        LlmError::ApiCallFailed(format!("OpenAI API error: {}", e))
    })?;

    let choice = response.choices.first().ok_or(LlmError::NoContent)?;
    let content = choice.message.content.as_ref().ok_or(LlmError::NoContent)?;

    parse_llm_response(content)
}

/// Parse the LLM response content as JSON and validate it contains a mind map node.
pub fn parse_llm_response(content: &str) -> Result<serde_json::Value, LlmError> {
    // Strip markdown code fences if present (some LLMs add them despite instructions)
    let cleaned = content
        .trim()
        .strip_prefix("```json")
        .or_else(|| content.trim().strip_prefix("```"))
        .unwrap_or(content)
        .strip_suffix("```")
        .unwrap_or(content)
        .trim();

    let value: serde_json::Value = serde_json::from_str(cleaned)?;

    // Validate that the response has a 'text' field (required by MindMapNode)
    if let Some(obj) = value.as_object() {
        if !obj.contains_key("text") {
            return Err(LlmError::InvalidResponse);
        }
    } else {
        return Err(LlmError::InvalidResponse);
    }

    Ok(value)
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_system_prompt_contains_schema() {
        let prompt = build_system_prompt();
        assert!(prompt.contains("MindMapNode"));
        assert!(prompt.contains("text"));
        assert!(prompt.contains("children"));
    }

    #[test]
    fn test_user_prompt_contains_text() {
        let text = "Hello world";
        let prompt = build_user_prompt(text);
        assert!(prompt.contains("Hello world"));
    }

    #[test]
    fn test_parse_llm_response_valid() {
        let json = r#"{"text": "Root", "children": [{"text": "Child"}]}"#;
        let result = parse_llm_response(json);
        assert!(result.is_ok());
        let value = result.unwrap();
        assert_eq!(value["text"], "Root");
        assert_eq!(value["children"].as_array().unwrap().len(), 1);
    }

    #[test]
    fn test_parse_llm_response_with_code_fences() {
        let json = r#"```json
{"text": "Root", "children": []}
```"#;
        let result = parse_llm_response(json);
        assert!(result.is_ok());
        let value = result.unwrap();
        assert_eq!(value["text"], "Root");
    }

    #[test]
    fn test_parse_llm_response_missing_text_field() {
        let json = r#"{"title": "Root"}"#;
        let result = parse_llm_response(json);
        assert!(result.is_err());
        match result.unwrap_err() {
            LlmError::InvalidResponse => {},
            other => panic!("Expected InvalidResponse, got {:?}", other),
        }
    }

    #[test]
    fn test_parse_llm_response_not_an_object() {
        let json = r#"[{"text": "Root"}]"#;
        let result = parse_llm_response(json);
        assert!(result.is_err());
    }

    #[test]
    fn test_parse_llm_response_invalid_json() {
        let json = r#"not json at all"#;
        let result = parse_llm_response(json);
        assert!(result.is_err());
    }
}
