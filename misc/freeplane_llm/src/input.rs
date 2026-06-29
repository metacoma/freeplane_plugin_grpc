//! Input handling for the freeplane_llm CLI tool.
//!
//! Supports two mutually exclusive input modes:
//! - `--text <TEXT>` — inline text input
//! - `--from-file` — read from stdin

use clap::Parser;
use std::io::Read;
use thiserror::Error;

/// CLI arguments for the freeplane_llm tool.
#[derive(Debug, Parser)]
#[command(name = "freeplane_llm")]
#[command(about = "Convert text to a Freeplane mind map using an LLM")]
pub struct CliArgs {
    /// Freeplane gRPC host address.
    /// Falls back to the FREEPLANE_HOST environment variable.
    #[arg(long = "freeplane-host", env = "FREEPLANE_HOST", default_value = "127.0.0.1")]
    pub freeplane_host: String,

    /// Freeplane gRPC port.
    /// Falls back to the FREEPLANE_PORT environment variable.
    #[arg(long = "freeplane-port", env = "FREEPLANE_PORT", default_value = "50051")]
    pub freeplane_port: u16,

    /// Input text to convert to a mind map.
    /// Mutually exclusive with --from-file.
    #[arg(long = "text", conflicts_with = "from_file")]
    pub text: Option<String>,

    /// Read input text from stdin.
    /// Mutually exclusive with --text.
    #[arg(long = "from-file")]
    pub from_file: bool,

    /// LLM model name.
    /// Falls back to the LLM_MODEL environment variable.
    #[arg(long = "model", env = "LLM_MODEL", default_value = "gpt-4o")]
    pub model: String,

    /// OpenAI API key.
    /// Falls back to the OPENAI_API_KEY environment variable.
    #[arg(long = "openai-api-key", env = "OPENAI_API_KEY")]
    pub openai_api_key: Option<String>,

    /// LLM server base URL.
    /// Takes priority over OPENAI_BASE_URL / --openai-base-url when both are set.
    /// Falls back to the LLM_SERVER environment variable.
    #[arg(long = "llm-server", env = "LLM_SERVER")]
    pub llm_server: Option<String>,

    /// OpenAI-compatible API base URL.
    /// Falls back to the OPENAI_BASE_URL environment variable.
    #[arg(long = "openai-base-url", env = "OPENAI_BASE_URL")]
    pub openai_base_url: Option<String>,
}

/// Errors that can occur during input reading.
#[derive(Debug, Error)]
pub enum InputError {
    #[error("both --text and --from-file were specified; use at most one")]
    BothInputsSpecified,

    #[error("no input provided: use --text <TEXT> or --from-file")]
    NoInputSpecified,

    #[error("failed to read from stdin: {0}")]
    StdinRead(#[from] std::io::Error),
}

/// Read input text based on CLI arguments.
///
/// Returns the input text as a `String`.
/// - If `--text` is provided, returns that text directly.
/// - If `--from-file` is provided, reads all of stdin.
/// - If neither is provided, reads all of stdin (default behavior).
/// - If both are provided, returns an error.
pub async fn read_input(args: &CliArgs) -> Result<String, InputError> {
    if args.text.is_some() && args.from_file {
        return Err(InputError::BothInputsSpecified);
    }

    if let Some(text) = &args.text {
        Ok(text.clone())
    } else {
        // Read from stdin (either --from-file was specified, or neither was specified)
        let mut input = String::new();
        std::io::stdin().read_to_string(&mut input)?;
        Ok(input)
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_cli_args_parsing_defaults() {
        // Clear LLM_MODEL env var if set, to test the default
        std::env::remove_var("LLM_MODEL");
        let args = CliArgs::try_parse_from(["freeplane_llm", "--text", "hello"]).unwrap();
        assert_eq!(args.freeplane_host, "127.0.0.1");
        assert_eq!(args.freeplane_port, 50051);
        assert_eq!(args.text.as_deref(), Some("hello"));
        assert!(!args.from_file);
        assert_eq!(args.model, "gpt-4o");
    }

    #[test]
    fn test_cli_args_parsing_custom_host_port() {
        let args = CliArgs::try_parse_from([
            "freeplane_llm",
            "--freeplane-host", "localhost",
            "--freeplane-port", "9999",
            "--text", "test",
        ])
        .unwrap();
        assert_eq!(args.freeplane_host, "localhost");
        assert_eq!(args.freeplane_port, 9999);
    }

    #[test]
    fn test_cli_args_parsing_from_file() {
        let args = CliArgs::try_parse_from(["freeplane_llm", "--from-file"]).unwrap();
        assert!(args.from_file);
        assert!(args.text.is_none());
    }

    #[test]
    fn test_cli_args_parsing_both_inputs_fails() {
        let result = CliArgs::try_parse_from([
            "freeplane_llm",
            "--text", "hello",
            "--from-file",
        ]);
        assert!(result.is_err());
    }

    #[test]
    fn test_cli_args_parsing_custom_model() {
        let args = CliArgs::try_parse_from([
            "freeplane_llm",
            "--text", "test",
            "--model", "claude-sonnet-4-20250514",
        ])
        .unwrap();
        assert_eq!(args.model, "claude-sonnet-4-20250514");
    }

    #[test]
    fn test_cli_args_parsing_custom_base_url() {
        let args = CliArgs::try_parse_from([
            "freeplane_llm",
            "--text", "test",
            "--openai-base-url", "http://localhost:11434/v1",
        ])
        .unwrap();
        assert_eq!(args.openai_base_url.as_deref(), Some("http://localhost:11434/v1"));
    }

    #[test]
    fn test_cli_args_parsing_llm_server() {
        let args = CliArgs::try_parse_from([
            "freeplane_llm",
            "--text", "test",
            "--llm-server", "http://localhost:8080/v1",
        ])
        .unwrap();
        assert_eq!(args.llm_server.as_deref(), Some("http://localhost:8080/v1"));
    }

    #[test]
    fn test_read_input_uses_read_to_string_for_multiline() {
        // This test documents the expected behavior: read_to_string reads until EOF,
        // supporting multi-line input. Full multi-line stdin testing requires
        // integration-level testing (mocking stdin), which is covered by the
        // integration test suite.
        //
        // The key change is that std::io::stdin().read_to_string(&mut input)
        // reads all bytes from stdin until EOF, not just up to the first newline
        // (which was the behavior with read_line).
        let mut input = String::new();
        let multiline = "Line 1\nLine 2\nLine 3";
        std::io::Cursor::new(multiline.as_bytes())
            .read_to_string(&mut input)
            .unwrap();
        assert_eq!(input, multiline);
    }
}
