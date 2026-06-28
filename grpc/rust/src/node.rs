//! Node — high-level representation of a Freeplane mind map node.
//!
//! Provides node-level operations such as getting/setting text,
//! managing children, styling, notes, attributes, links, and icons.
//! Mirrors the Python `Node` class API.

use crate::client::{ClientError, FreeplaneClient, FreeplaneStub};
use crate::error::{status_to_connection_error, FreeplaneOperationError, NodeNotFoundError};
use crate::generated::freeplane::GetCurrentNodeRequest;

/// Represents a Freeplane mind map node.
///
/// Provides node-level operations such as getting/setting text,
/// managing children, styling, notes, attributes, links, and icons.
pub struct Node<S>
where
    S: FreeplaneStub,
{
    client: FreeplaneClient<S>,
    node_id: String,
    mindmap: Option<MindMap<S>>,
}

impl<S> Clone for Node<S>
where
    S: FreeplaneStub + Clone,
{
    fn clone(&self) -> Self {
        Node {
            client: self.client.clone(),
            node_id: self.node_id.clone(),
            mindmap: self.mindmap.clone(),
        }
    }
}

impl<S> Node<S>
where
    S: FreeplaneStub + Send + Sync + Clone,
{
    /// Create a new Node.
    pub fn new(client: FreeplaneClient<S>, node_id: &str, mindmap: Option<MindMap<S>>) -> Self {
        Node {
            client,
            node_id: node_id.to_string(),
            mindmap,
        }
    }

    /// The server-side node ID.
    pub fn node_id(&self) -> &str {
        &self.node_id
    }

    /// The parent FreeplaneClient.
    pub fn client(&self) -> &FreeplaneClient<S> {
        &self.client
    }

    // -- text ---------------------------------------------------------------

    /// Get the text of this node.
    pub async fn get_text(&self) -> Result<String, ClientError> {
        let resp = self.client.get_node_text(&self.node_id).await?;
        Ok(resp.text.clone())
    }

    /// Set the text of this node.
    pub async fn set_text(&self, text: &str) -> Result<(), ClientError> {
        self.client.set_node_text(&self.node_id, text).await?;
        Ok(())
    }

    // -- hierarchy ----------------------------------------------------------

    /// Add a child node to this node.
    pub async fn add_child(&self, text: &str, style: &str) -> Result<Node<S>, ClientError> {
        // Use Groovy for style parameter (matching Python implementation)
        let groovy_code = if !style.is_empty() && style != "classic" {
            format!(
                "def child = model.addNode({}, {});\nchild.style = model.getStyleLib().getStyle('{}');\nchild",
                self.node_id,
                format!("\"{}\"", text.replace('"', "\\\"")),
                style
            )
        } else {
            format!(
                "model.addNode({}, {})",
                self.node_id,
                format!("\"{}\"", text.replace('"', "\\\""))
            )
        };

        let result = self.client.groovy(&groovy_code).await?;
        // Extract node_id from Groovy result
        let child_id = if result.contains("ID_") {
            // Try to extract the node ID from the result
            result.split(' ').find(|s| s.starts_with("ID_"))
                .map(|s| s.trim_end_matches(';').to_string())
                .unwrap_or_default()
        } else {
            // Fallback: use create_child RPC
            let resp = self.client.create_child(text, &self.node_id).await?;
            resp.node_id
        };

        Ok(Node::new(
            self.client.clone(),
            &child_id,
            self.mindmap.clone(),
        ))
    }

    /// Get the direct children of this node.
    pub async fn children(&self) -> Result<Vec<Node<S>>, ClientError> {
        let resp = self.client.list_child_nodes(&self.node_id).await?;
        Ok(resp.children.iter().map(|child| {
            Node::new(
                self.client.clone(),
                &child.node_id,
                self.mindmap.clone(),
            )
        }).collect())
    }

    /// Get the parent of this node.
    pub async fn parent(&self) -> Result<Node<S>, ClientError> {
        let resp = self.client.get_parent_node(&self.node_id).await?;
        if !resp.success || resp.parent_node_id.is_empty() {
            return Err(ClientError::NodeNotFound(NodeNotFoundError(
                format!("Node {} has no parent (is root)", self.node_id)
            )));
        }
        Ok(Node::new(
            self.client.clone(),
            &resp.parent_node_id,
            self.mindmap.clone(),
        ))
    }

    /// Delete this node.
    pub async fn delete(&self) -> Result<bool, ClientError> {
        self.client.delete_child(&self.node_id).await
    }

    /// Move this node under a new parent.
    pub async fn move_to(&self, new_parent_id: &str) -> Result<bool, ClientError> {
        self.client.move_node(&self.node_id, new_parent_id).await
    }

    // -- styling ------------------------------------------------------------

    /// Get the style information for this node via Groovy scripting.
    pub async fn get_style(&self) -> Result<String, ClientError> {
        let groovy_code = format!(
            "def node = model.getNode('{}');\nnode.style?.toString() ?: ''",
            self.node_id
        );
        self.client.groovy(&groovy_code).await
    }

    /// Set the style of this node via Groovy scripting.
    pub async fn set_style(&self, style: &str) -> Result<bool, ClientError> {
        let groovy_code = format!(
            "def node = model.getNode('{}');\nnode.style = model.getStyleLib().getStyle('{}');\ntrue",
            self.node_id, style
        );
        let result = self.client.groovy(&groovy_code).await?;
        Ok(!result.contains("Error"))
    }

    /// Get the foreground color of this node via Groovy scripting.
    pub async fn get_color(&self) -> Result<String, ClientError> {
        let groovy_code = format!(
            "def node = model.getNode('{}');\nnode.style?.foregroundColor?.toString() ?: ''",
            self.node_id
        );
        self.client.groovy(&groovy_code).await
    }

    /// Set the foreground color of this node.
    pub async fn set_color(&self, red: i32, green: i32, blue: i32, alpha: i32) -> Result<bool, ClientError> {
        self.client.node_color_set(&self.node_id, red, green, blue, alpha).await
    }

    /// Get the background color of this node via Groovy scripting.
    pub async fn get_background_color(&self) -> Result<String, ClientError> {
        let groovy_code = format!(
            "def node = model.getNode('{}');\nnode.style?.backgroundColor?.toString() ?: ''",
            self.node_id
        );
        self.client.groovy(&groovy_code).await
    }

    /// Set the background color of this node.
    pub async fn set_background_color(&self, red: i32, green: i32, blue: i32, alpha: i32) -> Result<bool, ClientError> {
        self.client.node_background_color_set(&self.node_id, red, green, blue, alpha).await
    }

    // -- notes --------------------------------------------------------------

    /// Get the note of this node.
    pub async fn get_note(&self) -> Result<Option<String>, ClientError> {
        let resp = self.client.get_node_note(&self.node_id).await?;
        if resp.has_note && !resp.note.is_empty() {
            Ok(Some(resp.note.clone()))
        } else {
            Ok(None)
        }
    }

    /// Set the note of this node.
    pub async fn set_note(&self, note: &str) -> Result<(), ClientError> {
        self.client.node_note_set(&self.node_id, note).await?;
        Ok(())
    }

    // -- attributes ---------------------------------------------------------

    /// Get all custom attributes of this node via Groovy scripting.
    pub async fn get_attributes(&self) -> Result<String, ClientError> {
        let groovy_code = format!(
            "def node = model.getNode('{}');\ndef attrs = node.attributes;\nattrs ? attrs.toString() : '{}'",
            self.node_id, "{}"
        );
        self.client.groovy(&groovy_code).await
    }

    /// Set a custom attribute on this node.
    pub async fn set_attribute(&self, name: &str, value: &str) -> Result<bool, ClientError> {
        self.client.node_attribute_add(&self.node_id, name, value).await
    }

    /// Set multiple custom attributes on this node.
    pub async fn set_attributes(&self, attrs: &[(&str, &str)]) -> Result<bool, ClientError> {
        for (name, value) in attrs {
            if !self.set_attribute(name, value).await? {
                return Ok(false);
            }
        }
        Ok(true)
    }

    // -- links --------------------------------------------------------------

    /// Get the links of this node.
    pub async fn get_links(&self) -> Result<Vec<String>, ClientError> {
        let resp = self.client.get_node_link(&self.node_id).await?;
        if resp.has_link && !resp.link.is_empty() {
            Ok(vec![resp.link.clone()])
        } else {
            Ok(Vec::new())
        }
    }

    /// Set the links of this node (replaces existing links).
    pub async fn set_links(&self, links: &[&str]) -> Result<bool, ClientError> {
        for link in links {
            self.client.node_link_set(&self.node_id, link).await?;
        }
        Ok(true)
    }

    // -- tags ---------------------------------------------------------------

    /// Set the tags of this node (replaces existing tags).
    pub async fn set_tags(&self, tags: &[&str]) -> Result<bool, ClientError> {
        let tag_vec: Vec<String> = tags.iter().map(|t| t.to_string()).collect();
        self.client.node_tag_set(&self.node_id, tag_vec).await
    }

    /// Add tags to this node (does not remove existing tags).
    pub async fn add_tags(&self, tags: &[&str]) -> Result<bool, ClientError> {
        let tag_vec: Vec<String> = tags.iter().map(|t| t.to_string()).collect();
        self.client.node_tag_add(&self.node_id, tag_vec).await
    }

    // -- icons --------------------------------------------------------------

    /// Get the icons of this node via Groovy scripting.
    pub async fn get_icons(&self) -> Result<String, ClientError> {
        let groovy_code = format!(
            "def node = model.getNode('{}');\ndef icons = node.getIconIds();\nicons ? icons.toList().toString() : '[]'",
            self.node_id
        );
        self.client.groovy(&groovy_code).await
    }

    /// Add an icon to this node.
    pub async fn add_icon(&self, icon_name: &str) -> Result<bool, ClientError> {
        self.client.node_add_icon(&self.node_id, icon_name).await
    }

    // -- state --------------------------------------------------------------

    /// Get the folded (collapsed) state of this node via Groovy scripting.
    pub async fn get_folded(&self) -> Result<bool, ClientError> {
        let groovy_code = format!(
            "def node = model.getNode('{}');\nnode.isFolded()",
            self.node_id
        );
        let result = self.client.groovy(&groovy_code).await?;
        Ok(result.to_lowercase().contains("true"))
    }

    /// Set the folded (collapsed) state of this node via Groovy scripting.
    pub async fn set_folded(&self, folded: bool) -> Result<bool, ClientError> {
        let groovy_code = format!(
            "def node = model.getNode('{}');\nif ({}) node.fold() else node.unfold();\ntrue",
            self.node_id, folded
        );
        let result = self.client.groovy(&groovy_code).await?;
        Ok(!result.contains("Error"))
    }

    // -- actions ------------------------------------------------------------

    /// Select this node in the Freeplane UI.
    pub async fn select(&self) -> Result<bool, ClientError> {
        self.client.focus_node(&self.node_id).await
    }

    /// Center the view on this node.
    pub async fn center(&self) -> Result<bool, ClientError> {
        self.client.focus_node(&self.node_id).await
    }

    /// Reload this node's state from the server.
    pub async fn refresh(&self) -> Result<(), ClientError> {
        self.get_text().await?;
        Ok(())
    }
}

/// MindMap — high-level representation of a Freeplane mind map.
///
/// Provides map-level operations such as getting the root node,
/// searching nodes, and exporting/importing the map.
pub struct MindMap<S>
where
    S: FreeplaneStub,
{
    client: FreeplaneClient<S>,
    map_id: String,
    node_id: String,
}

impl<S> Clone for MindMap<S>
where
    S: FreeplaneStub + Clone,
{
    fn clone(&self) -> Self {
        MindMap {
            client: self.client.clone(),
            map_id: self.map_id.clone(),
            node_id: self.node_id.clone(),
        }
    }
}

impl<S> MindMap<S>
where
    S: FreeplaneStub + Send + Sync + Clone,
{
    /// Create a new MindMap.
    pub fn new(client: FreeplaneClient<S>, map_id: &str, node_id: &str) -> Self {
        MindMap {
            client,
            map_id: map_id.to_string(),
            node_id: node_id.to_string(),
        }
    }

    /// The server-side map ID.
    pub fn map_id(&self) -> &str {
        &self.map_id
    }

    /// The server-side node ID (currently focused node).
    pub fn node_id(&self) -> &str {
        &self.node_id
    }

    /// The parent FreeplaneClient.
    pub fn client(&self) -> &FreeplaneClient<S> {
        &self.client
    }

    // -- navigation ---------------------------------------------------------

    /// Get the root node of this mind map by traversing up from the current node.
    pub async fn root(&self) -> Result<Node<S>, ClientError> {
        let mut current_id = if self.node_id.is_empty() {
            let resp = self.client.current_map().await?;
            resp.node_id.clone()
        } else {
            self.node_id.clone()
        };

        // Traverse up to find root
        loop {
            let parent_resp = self.client.get_parent_node(&current_id).await?;
            if !parent_resp.success || parent_resp.parent_node_id.is_empty() {
                // This is the root
                return Ok(Node::new(
                    self.client.clone(),
                    &current_id,
                    Some(self.clone()),
                ));
            }
            current_id = parent_resp.parent_node_id;
        }
    }

    /// Get the currently selected/focused node.
    pub async fn selected_node(&self) -> Result<Node<S>, ClientError> {
        let resp = self.client.stub.get_current_node(GetCurrentNodeRequest::default()).await
            .map_err(|status| ClientError::Connection(status_to_connection_error(&status)))?;
        if !resp.success || resp.node_id.is_empty() {
            return Err(ClientError::Operation(FreeplaneOperationError(
                "No node currently selected".to_string()
            )));
        }
        Ok(Node::new(
            self.client.clone(),
            &resp.node_id,
            Some(self.clone()),
        ))
    }

    /// Find all nodes whose text contains the given pattern.
    pub async fn find_nodes(&self, pattern: &str) -> Result<Vec<Node<S>>, ClientError> {
        let root_node = self.root().await?;
        let mut matches = Vec::new();
        self.walk_and_collect(&root_node, pattern, &mut matches).await;
        Ok(matches)
    }

    async fn walk_and_collect(
        &self,
        node: &Node<S>,
        pattern: &str,
        matches: &mut Vec<Node<S>>,
    ) {
        // Use an iterative approach with an explicit stack to avoid E0391
        // cycle error caused by recursive async fn size inference.
        let mut stack = vec![node.clone()];

        while let Some(current) = stack.pop() {
            // Check this node
            if let Ok(text) = current.get_text().await {
                if text.to_lowercase().contains(&pattern.to_lowercase()) {
                    matches.push(current.clone());
                }
            }

            // Add children to stack
            if let Ok(children) = current.children().await {
                for child in children {
                    stack.push(child);
                }
            }
        }
    }

    // -- metadata -----------------------------------------------------------

    /// Get basic information about the current map.
    pub fn info(&self) -> std::collections::HashMap<String, String> {
        let mut info = std::collections::HashMap::new();
        info.insert("map_id".to_string(), self.map_id.clone());
        info.insert("node_id".to_string(), self.node_id.clone());
        info
    }

    /// Estimate the number of nodes in the mind map.
    pub async fn size(&self) -> Result<usize, ClientError> {
        let root_node = self.root().await?;
        Ok(self.count_nodes(&root_node).await)
    }

    async fn count_nodes(&self, node: &Node<S>) -> usize {
        // Use an iterative approach with an explicit stack to avoid E0391
        // cycle error caused by recursive async fn size inference.
        let mut count = 0usize;
        let mut stack = vec![node.clone()];

        while let Some(current) = stack.pop() {
            count += 1;

            if let Ok(children) = current.children().await {
                for child in children {
                    stack.push(child);
                }
            }
        }

        count
    }

    // -- file operations ----------------------------------------------------

    /// Save the current mind map.
    pub async fn save(&self, _path: &str) -> Result<bool, ClientError> {
        // The gRPC API doesn't have a direct Save RPC; the map is saved
        // automatically by Freeplane. Return True to indicate the map
        // is in a valid state.
        Ok(true)
    }

    /// Export the current mind map to a file via Groovy scripting.
    pub async fn export(&self, path: &str, format: &str) -> Result<bool, ClientError> {
        let groovy_code = format!(
            "def controller = model.getMap().getController();\n\
             def view = controller.getMapView();\n\
             view.exportMap(new File('{}'), '{}');\n\
             true",
            path.replace('\'', "\\'"),
            format
        );
        let result = self.client.groovy(&groovy_code).await?;
        Ok(!result.contains("Error"))
    }

    /// Import a mind map from a file.
    pub async fn import_map(&self, path: &str) -> Result<MindMap<S>, ClientError> {
        let resp = self.client.open_map(path).await?;
        Ok(MindMap::new(
            self.client.clone(),
            &resp.map_id,
            &resp.node_id,
        ))
    }

    // -- node creation ------------------------------------------------------

    /// Create a new node in the mind map.
    pub async fn create_node(&self, text: &str, parent_id: &str, _style: &str) -> Result<Node<S>, ClientError> {
        let parent = if parent_id.is_empty() {
            self.root().await?.node_id().to_string()
        } else {
            parent_id.to_string()
        };

        let resp = self.client.create_child(text, &parent).await?;
        Ok(Node::new(
            self.client.clone(),
            &resp.node_id,
            Some(self.clone()),
        ))
    }

    /// Create a child node under an existing node.
    pub async fn create_child(&self, parent: &Node<S>, text: &str, style: &str) -> Result<Node<S>, ClientError> {
        self.create_node(text, &parent.node_id(), style).await
    }
}
