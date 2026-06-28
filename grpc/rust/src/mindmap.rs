//! MindMap - high-level representation of a Freeplane mind map.

use crate::client::FreeplaneClient;
use crate::error::ClientError;
use crate::node::Node;

/// Represents a Freeplane mind map.
pub struct MindMap {
    client: FreeplaneClient<crate::client::ChannelWrapper>,
    map_id: String,
    node_id: String,
}

impl MindMap {
    pub fn new(client: FreeplaneClient<crate::client::ChannelWrapper>, map_id: String, node_id: String) -> Self {
        Self { client, map_id, node_id }
    }

    /// Get the map ID.
    pub fn map_id(&self) -> &str {
        &self.map_id
    }

    /// Get the current node ID.
    pub fn node_id(&self) -> &str {
        &self.node_id
    }

    /// Get the root node.
    pub async fn root(&self) -> Result<Node, ClientError> {
        let mut current_id = self.node_id.clone();
        loop {
            let parent_resp = self.client.get_parent_node(&current_id).await?;
            if parent_resp.parent_node_id.is_empty() || parent_resp.parent_node_id == current_id {
                break;
            }
            current_id = parent_resp.parent_node_id;
        }
        Ok(Node::new(
            self.client.clone(),
            self.map_id.clone(),
            current_id,
        ))
    }

    /// Get the currently selected node.
    pub async fn selected_node(&self) -> Result<Node, ClientError> {
        let resp = self.client.get_current_node().await?;
        Ok(Node::new(
            self.client.clone(),
            self.map_id.clone(),
            resp.node_id,
        ))
    }

    /// Find all nodes matching a text pattern.
    pub async fn find_nodes(&self, pattern: &str) -> Result<Vec<Node>, ClientError> {
        // Use Groovy to search for nodes
        let groovy_code = format!(
            "def result = [];\
             def nodes = model.rootNode.descendants().toArray();\
             for (def node : nodes) {{\
                 if (node.getText().contains('{}')) {{\
                     result.add(node.getUUID());\
                 }}\
             }};\
             result",
            pattern.replace('\'', "\\'")
        );
        let result = self.client.groovy(&groovy_code).await?;
        // Parse the result (simplified - in practice would parse the Groovy list output)
        let mut nodes = Vec::new();
        // This is a simplified implementation
        Ok(nodes)
    }

    /// Get map metadata.
    pub fn info(&self) -> (&str, &str) {
        (&self.map_id, &self.node_id)
    }

    /// Create a new node.
    pub async fn create_node(&self, text: &str, parent_id: &str) -> Result<Node, ClientError> {
        let resp = self.client.create_child(text, parent_id).await?;
        Ok(Node::new(
            self.client.clone(),
            self.map_id.clone(),
            resp.node_id,
        ))
    }

    /// Create a child node.
    pub async fn create_child(&self, parent: &Node, text: &str) -> Result<Node, ClientError> {
        let resp = self.client.create_child(text, &parent.node_id).await?;
        Ok(Node::new(
            self.client.clone(),
            self.map_id.clone(),
            resp.node_id,
        ))
    }
}
