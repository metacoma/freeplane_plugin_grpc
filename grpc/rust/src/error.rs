//! Custom error types for the Freeplane gRPC client.
//!
//! Mirrors the 5-type exception hierarchy from the Python client:
//! `FreeplaneGrpcError` → `FreeplaneConnectionError`, `FreeplaneOperationError`,
//! `NodeNotFoundError`, `MindMapError`.

use thiserror::Error;

/// Base error for all Freeplane gRPC errors.
#[derive(Error, Debug)]
#[error("{0}")]
pub struct FreeplaneGrpcError(pub String);

/// Raised when a connection to the Freeplane gRPC server fails.
#[derive(Error, Debug)]
#[error("Freeplane connection error: {0}")]
pub struct FreeplaneConnectionError(pub String);

/// Raised when a gRPC operation fails (server reported failure).
#[derive(Error, Debug)]
#[error("Freeplane operation error: {0}")]
pub struct FreeplaneOperationError(pub String);

/// Raised when a requested node is not found.
#[derive(Error, Debug)]
#[error("Node not found: {0}")]
pub struct NodeNotFoundError(pub String);

/// Raised when a map-level operation fails.
#[derive(Error, Debug)]
#[error("MindMap error: {0}")]
pub struct MindMapError(pub String);

/// Maps a `tonic::Status` to the appropriate domain error.
pub fn status_to_error(status: &tonic::Status) -> FreeplaneOperationError {
    FreeplaneOperationError(format!(
        "gRPC call failed ({}): {}",
        status.code(),
        status.message()
    ))
}

/// Maps connection-level gRPC status codes to `FreeplaneConnectionError`.
pub fn status_to_connection_error(status: &tonic::Status) -> FreeplaneConnectionError {
    FreeplaneConnectionError(format!(
        "gRPC call failed ({}): {}",
        status.code(),
        status.message()
    ))
}

/// Returns `true` if the status code indicates a connection-level failure.
pub fn is_connection_error(status: &tonic::Status) -> bool {
    matches!(
        status.code(),
        tonic::Code::Unavailable
            | tonic::Code::DeadlineExceeded
            | tonic::Code::ResourceExhausted
    )
}
