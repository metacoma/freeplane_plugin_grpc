# freeplane_llm

A CLI tool that converts text to a Freeplane mind map using an LLM (OpenAI-compatible API).

## Overview

`freeplane_llm` sends text to an OpenAI-compatible LLM (e.g., OpenAI, Ollama, vLLM) with a structured output schema, then imports the generated mind map into Freeplane via the gRPC plugin.

## Prerequisites

- **Freeplane** running with the gRPC plugin installed
- **Rust toolchain** (edition 2021)
- An **OpenAI API key** (or any OpenAI-compatible API)

## Installation

```bash
cd misc/freeplane_llm
cargo build --release
```

The binary will be available at `target/release/freeplane_llm`.

## Usage

### Basic Usage

```bash
# With inline text
freeplane_llm --text "Rust is a systems programming language focused on safety and performance"

# With stdin input
echo "Rust is a systems programming language" | freeplane_llm --from-file

# Pipe from another command
cat notes.txt | freeplane_llm --from-file
```

### Configuration

| Flag | Environment Variable | Default | Description |
|------|---------------------|---------|-------------|
| `--freeplane-host` | `FREEPLANE_HOST` | `127.0.0.1` | Freeplane gRPC host address |
| `--freeplane-port` | `FREEPLANE_PORT` | `50051` | Freeplane gRPC port |
| `--text <TEXT>` | — | — | Input text (mutually exclusive with `--from-file`) |
| `--from-file` | — | — | Read input from stdin |
| `--model <MODEL>` | `LLM_MODEL` | `gpt-4o` | LLM model name |
| `--openai-api-key <KEY>` | `OPENAI_API_KEY` | — | OpenAI API key (required) |
| `--llm-server <URL>` | `LLM_SERVER` | — | LLM server base URL (takes priority over `--openai-base-url`) |
| `--openai-base-url <URL>` | `OPENAI_BASE_URL` | — | Custom API base URL (e.g., `http://localhost:11434/v1` for Ollama; fallback when `--llm-server` is not set) |

### Examples

#### Using OpenAI

```bash
export OPENAI_API_KEY="sk-..."
freeplane_llm --text "Artificial Intelligence: Machine Learning, Deep Learning, Neural Networks"
```

#### Using Ollama (local)

```bash
freeplane_llm \
  --llm-server "http://localhost:11434/v1" \
  --openai-api-key "ollama" \
  --model "llama3.2" \
  --text "Project planning: requirements, design, implementation, testing"
```

#### Using a custom API

```bash
freeplane_llm \
  --openai-base-url "https://api.example.com/v1" \
  --openai-api-key "your-api-key" \
  --model "custom-model" \
  --text "Meeting notes: agenda, discussion, action items"
```

## How It Works

1. **Input**: Text is provided via `--text` or read from stdin via `--from-file`
2. **LLM Call**: The text is sent to the configured LLM with a system prompt containing a JSON Schema for mind map nodes
3. **Structured Output**: The LLM returns a JSON mind map using the `response_format` structured output feature
4. **Import**: The JSON is wrapped in Freeplane's import format and sent via gRPC to Freeplane
5. **Result**: The mind map appears in Freeplane

## Mind Map Schema

The LLM generates mind maps using the following node structure:

```json
{
  "text": "Node display text (required)",
  "children": [
    {
      "text": "Child node",
      "detail": "Optional detail text",
      "note": "Optional note",
      "link": "https://example.com",
      "tags": ["tag1", "tag2"],
      "icons": ["star"],
      "attributes": {"key": "value"},
      "background_color": "rgba(255,0,0,128)",
      "folded": false
    }
  ]
}
```

## Running Tests

### Unit Tests

```bash
cargo test
```

### Integration Tests

Integration tests require a running Freeplane instance with the gRPC plugin:

```bash
# Start Freeplane with the gRPC plugin first, then:
FREEPLANE_HOST=127.0.0.1 FREEPLANE_PORT=50051 \
  cargo test --test integration_test -- --ignored --test-threads=1
```

## Troubleshooting

- **"OPENAI_API_KEY environment variable is not set"**: Set the `OPENAI_API_KEY` environment variable or pass `--openai-api-key`
- **"Failed to connect to Freeplane"**: Ensure Freeplane is running with the gRPC plugin and the host/port are correct
- **"LLM response does not contain a valid mind map node"**: The LLM may have returned unexpected output. Try a different model or check the API URL
