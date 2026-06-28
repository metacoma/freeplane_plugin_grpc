//! # freeplane-grpc
//!
//! A Rust gRPC client for the [Freeplane](https://github.com/freeplane/freeplane)
//! mind map application. Provides a high-level API for interacting with Freeplane
//! nodes, mind maps, and the Freeplane server via gRPC.
//!
//! ## Quick Start
//!
//! ```no_run
//! use freeplane_grpc::FreeplaneClient;
//!
//! #[tokio::main]
//! async fn main() -> Result<(), Box<dyn std::error::Error>> {
//!     let mut client = FreeplaneClient::connect("127.0.0.1", 50051).await?;
//!
//!     let mindmap = client.mind_map();
//!     let root = mindmap.root().await?;
//!     println!("Root node: {}", root.get_text().await?);
//!
//!     client.close().await;
//!     Ok(())
//! }
//! ```
//!
//! ## Architecture
//!
//! The client follows the same pattern as the Python/Node.js clients:
//! - `FreeplaneClient` — main entry point, wraps the gRPC stub
//! - `Node` — represents a mind map node with text, hierarchy, styling, etc.
//! - `MindMap` — represents a mind map with root, search, export/import
//!
//! All 27 RPC methods from `freeplane.proto` are wrapped.

pub mod generated;
pub mod error;
pub mod client;
pub mod node;

// Re-export key types at the crate root for convenience.
pub use error::{
    FreeplaneGrpcError,
    FreeplaneConnectionError,
    FreeplaneOperationError,
    NodeNotFoundError,
    MindMapError,
};
pub use client::{FreeplaneClient, FreeplaneStub, ClientError};
pub use node::{Node, MindMap};
