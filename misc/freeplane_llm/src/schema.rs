//! MindMapNode schema definition for Freeplane-compatible mind map generation.
//!
//! This module defines the `MindMapNode` struct with `serde` and `schemars` derives,
//! producing a JSON Schema compatible with Freeplane's canonical import format.
//! The import wrapper format is `{"_fp_import_root_node": "root", "mindmap": <node>}`.

use schemars::JsonSchema;
use serde::{Deserialize, Serialize};
use std::collections::HashMap;

/// A single node in a Freeplane-compatible mind map.
///
/// Fields that are `None` are omitted from serialized output.
/// The `id` and `relationships` fields are intentionally excluded —
/// Freeplane auto-generates node IDs and relationships require
/// pre-existing node IDs which the LLM cannot know.
#[derive(Debug, Clone, Serialize, Deserialize, JsonSchema)]
pub struct MindMapNode {
    /// The display text of the mind map node (required).
    pub text: String,

    /// Child nodes (recursive).
    #[serde(skip_serializing_if = "Vec::is_empty", default)]
    pub children: Vec<MindMapNode>,

    /// Key-value attributes attached to the node.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub attributes: Option<HashMap<String, String>>,

    /// Detailed/deep text for the node.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub detail: Option<String>,

    /// Note text attached to the node.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub note: Option<String>,

    /// Hyperlink URI attached to the node.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub link: Option<String>,

    /// Tags attached to the node.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub tags: Option<Vec<String>>,

    /// Icon names (e.g., "star", "flag", "priority1").
    #[serde(skip_serializing_if = "Option::is_none")]
    pub icons: Option<Vec<String>>,

    /// Background color (rgba, rgb, hex, or named color).
    #[serde(skip_serializing_if = "Option::is_none")]
    pub background_color: Option<String>,

    /// Whether the node is folded (collapsed).
    #[serde(skip_serializing_if = "Option::is_none")]
    pub folded: Option<bool>,
}

impl MindMapNode {
    /// Return the JSON Schema for this struct as a formatted JSON string.
    pub fn json_schema() -> String {
        let schema = schemars::schema_for!(MindMapNode);
        serde_json::to_string_pretty(&schema).unwrap_or_default()
    }

    /// Wrap a LLM-generated mind map node in the Freeplane import format.
    ///
    /// The import format is:
    /// ```json
    /// {
    ///   "_fp_import_root_node": "root",
    ///   "mindmap": <LLM_OUTPUT>
    /// }
    /// ```
    pub fn wrap_for_import(node: &serde_json::Value) -> String {
        let wrapper = serde_json::json!({
            "_fp_import_root_node": "root",
            "mindmap": node
        });
        serde_json::to_string_pretty(&wrapper).unwrap_or_default()
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_json_schema_is_valid_json() {
        let schema = MindMapNode::json_schema();
        let parsed: serde_json::Value = serde_json::from_str(&schema)
            .expect("JSON Schema should be valid JSON");
        assert_eq!(parsed["type"], "object");
        assert_eq!(parsed["properties"]["text"]["type"], "string");
    }

    #[test]
    fn test_wrap_for_import() {
        let node = serde_json::json!({
            "text": "Root",
            "children": [{"text": "Child"}]
        });
        let wrapped = MindMapNode::wrap_for_import(&node);
        let parsed: serde_json::Value = serde_json::from_str(&wrapped)
            .expect("Wrapped import should be valid JSON");
        assert_eq!(
            parsed["_fp_import_root_node"],
            serde_json::Value::String("root".to_string())
        );
        assert!(parsed["mindmap"].is_object());
    }

    #[test]
    fn test_mindmap_node_serialization_skips_empty() {
        let node = MindMapNode {
            text: "Test".to_string(),
            children: vec![],
            attributes: None,
            detail: None,
            note: None,
            link: None,
            tags: None,
            icons: None,
            background_color: None,
            folded: None,
        };
        let json = serde_json::to_string(&node).unwrap();
        assert!(json.contains("\"text\":\"Test\""));
        assert!(!json.contains("children"));
        assert!(!json.contains("attributes"));
        assert!(!json.contains("note"));
    }

    #[test]
    fn test_mindmap_node_serialization_includes_fields() {
        let mut attrs = HashMap::new();
        attrs.insert("key".to_string(), "value".to_string());
        let node = MindMapNode {
            text: "Test".to_string(),
            children: vec![MindMapNode {
                text: "Child".to_string(),
                children: vec![],
                attributes: None,
                detail: Some("detail text".to_string()),
                note: Some("note text".to_string()),
                link: Some("https://example.com".to_string()),
                tags: Some(vec!["tag1".to_string(), "tag2".to_string()]),
                icons: Some(vec!["star".to_string()]),
                background_color: Some("rgba(255,0,0,128)".to_string()),
                folded: Some(true),
            }],
            attributes: Some(attrs),
            detail: None,
            note: None,
            link: None,
            tags: None,
            icons: None,
            background_color: None,
            folded: None,
        };
        let json = serde_json::to_string(&node).unwrap();
        let parsed: serde_json::Value = serde_json::from_str(&json).unwrap();
        assert_eq!(parsed["text"], "Test");
        assert_eq!(parsed["children"].as_array().unwrap().len(), 1);
        assert_eq!(parsed["children"][0]["detail"], "detail text");
        assert_eq!(parsed["children"][0]["note"], "note text");
        assert_eq!(parsed["children"][0]["link"], "https://example.com");
        assert_eq!(parsed["children"][0]["tags"].as_array().unwrap().len(), 2);
        assert_eq!(parsed["children"][0]["icons"].as_array().unwrap().len(), 1);
        assert_eq!(parsed["children"][0]["background_color"], "rgba(255,0,0,128)");
        assert_eq!(parsed["children"][0]["folded"], true);
    }
}
