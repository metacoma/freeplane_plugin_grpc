//! Unit tests for the error hierarchy.
//!
//! Mirrors the Python test_exceptions.py and Node.js exceptions.test.js patterns.

use freeplane_grpc::error::{
    FreeplaneGrpcError,
    FreeplaneConnectionError,
    FreeplaneOperationError,
    NodeNotFoundError,
    MindMapError,
    is_connection_error,
    status_to_error,
    status_to_connection_error,
};

// ─── Error type hierarchy tests ─────────────────────────────────────────────

#[test]
fn test_freeplane_grpc_error_is_error() {
    let err = FreeplaneGrpcError("test message".to_string());
    // Verify it implements Display (via thiserror)
    let msg = format!("{}", err);
    assert!(msg.contains("test message"));
}

#[test]
fn test_freeplane_connection_error_is_grpc_error() {
    let err = FreeplaneConnectionError("connection failed".to_string());
    let msg = format!("{}", err);
    assert!(msg.contains("connection failed"));
    assert!(msg.contains("connection error"));
}

#[test]
fn test_freeplane_operation_error_is_grpc_error() {
    let err = FreeplaneOperationError("operation failed".to_string());
    let msg = format!("{}", err);
    assert!(msg.contains("operation failed"));
    assert!(msg.contains("operation error"));
}

#[test]
fn test_node_not_found_error() {
    let err = NodeNotFoundError("node abc123 not found".to_string());
    let msg = format!("{}", err);
    assert!(msg.contains("node abc123 not found"));
    assert!(msg.contains("Node not found"));
}

#[test]
fn test_mind_map_error() {
    let err = MindMapError("map is corrupted".to_string());
    let msg = format!("{}", err);
    assert!(msg.contains("map is corrupted"));
    assert!(msg.contains("MindMap error"));
}

// ─── Error message tests ────────────────────────────────────────────────────

#[test]
fn test_base_exception_message() {
    let err = FreeplaneGrpcError("Something went wrong".to_string());
    assert!(err.0.contains("Something went wrong"));
}

#[test]
fn test_connection_error_message() {
    let err = FreeplaneConnectionError("Cannot reach server".to_string());
    assert!(err.0.contains("Cannot reach server"));
}

#[test]
fn test_operation_error_message() {
    let err = FreeplaneOperationError("Operation failed".to_string());
    assert!(err.0.contains("Operation failed"));
}

#[test]
fn test_node_not_found_error_message() {
    let err = NodeNotFoundError("Node abc123 not found".to_string());
    assert!(err.0.contains("abc123"));
}

#[test]
fn test_mind_map_error_message() {
    let err = MindMapError("Map is corrupted".to_string());
    assert!(err.0.contains("corrupted"));
}

#[test]
fn test_exception_no_raw_stack_trace() {
    let err = FreeplaneOperationError("Failed".to_string());
    let msg = format!("{}", err);
    assert!(!msg.contains("Traceback"));
    assert!(!msg.contains("File "));
}

// ─── Status conversion tests ────────────────────────────────────────────────

#[test]
fn test_status_to_error_creates_operation_error() {
    let status = tonic::Status::new(tonic::Code::Internal, "something broke");
    let err = status_to_error(&status);
    assert!(err.0.contains("Internal"));
    assert!(err.0.contains("something broke"));
    assert!(err.0.contains("gRPC call failed"));
}

#[test]
fn test_status_to_connection_error_creates_connection_error() {
    let status = tonic::Status::new(tonic::Code::Unavailable, "server down");
    let err = status_to_connection_error(&status);
    // The format is "gRPC call failed (CODE): message"
    assert!(err.0.contains("server down"));
    assert!(err.0.contains("gRPC call failed"));
}

// ─── Connection error detection tests ───────────────────────────────────────

#[test]
fn test_is_connection_error_unavailable() {
    let status = tonic::Status::new(tonic::Code::Unavailable, "server down");
    assert!(is_connection_error(&status));
}

#[test]
fn test_is_connection_error_deadline_exceeded() {
    let status = tonic::Status::new(tonic::Code::DeadlineExceeded, "timeout");
    assert!(is_connection_error(&status));
}

#[test]
fn test_is_connection_error_resource_exhausted() {
    let status = tonic::Status::new(tonic::Code::ResourceExhausted, "rate limited");
    assert!(is_connection_error(&status));
}

#[test]
fn test_is_connection_error_not_found_is_not_connection() {
    let status = tonic::Status::new(tonic::Code::NotFound, "not found");
    assert!(!is_connection_error(&status));
}

#[test]
fn test_is_connection_error_invalid_argument_is_not_connection() {
    let status = tonic::Status::new(tonic::Code::InvalidArgument, "bad input");
    assert!(!is_connection_error(&status));
}

#[test]
fn test_is_connection_error_internal_is_not_connection() {
    let status = tonic::Status::new(tonic::Code::Internal, "internal error");
    assert!(!is_connection_error(&status));
}

#[test]
fn test_is_connection_error_cancelled_is_not_connection() {
    let status = tonic::Status::new(tonic::Code::Cancelled, "cancelled");
    assert!(!is_connection_error(&status));
}

// ─── Error Debug trait tests ────────────────────────────────────────────────

#[test]
fn test_error_debug_format() {
    let err = FreeplaneGrpcError("test".to_string());
    let debug_str = format!("{:?}", err);
    assert!(debug_str.contains("FreeplaneGrpcError"));
}

#[test]
fn test_connection_error_debug_format() {
    let err = FreeplaneConnectionError("test".to_string());
    let debug_str = format!("{:?}", err);
    assert!(debug_str.contains("FreeplaneConnectionError"));
}

#[test]
fn test_operation_error_debug_format() {
    let err = FreeplaneOperationError("test".to_string());
    let debug_str = format!("{:?}", err);
    assert!(debug_str.contains("FreeplaneOperationError"));
}

#[test]
fn test_node_not_found_error_debug_format() {
    let err = NodeNotFoundError("test".to_string());
    let debug_str = format!("{:?}", err);
    assert!(debug_str.contains("NodeNotFoundError"));
}

#[test]
fn test_mind_map_error_debug_format() {
    let err = MindMapError("test".to_string());
    let debug_str = format!("{:?}", err);
    assert!(debug_str.contains("MindMapError"));
}

// ─── Error From implementation tests (thiserror derives From<tonic::Status>) ─

#[test]
fn test_error_from_status() {
    let status = tonic::Status::new(tonic::Code::Internal, "test error");
    let err: FreeplaneOperationError = status_to_error(&status);
    assert!(err.0.contains("test error"));
}

// ─── Empty message tests ────────────────────────────────────────────────────

#[test]
fn test_empty_message_errors() {
    let err1 = FreeplaneGrpcError(String::new());
    assert!(err1.0.is_empty());

    let err2 = FreeplaneConnectionError(String::new());
    assert!(err2.0.is_empty());

    let err3 = FreeplaneOperationError(String::new());
    assert!(err3.0.is_empty());

    let err4 = NodeNotFoundError(String::new());
    assert!(err4.0.is_empty());

    let err5 = MindMapError(String::new());
    assert!(err5.0.is_empty());
}

// ─── Long message tests ─────────────────────────────────────────────────────

#[test]
fn test_long_error_message() {
    let long_msg = "a".repeat(1000);
    let err = FreeplaneGrpcError(long_msg.clone());
    assert_eq!(err.0.len(), 1000);
}

#[test]
fn test_unicode_error_message() {
    let err = FreeplaneGrpcError("Ошибка: что-то пошло не так".to_string());
    assert!(err.0.contains("Ошибка"));

    let err2 = NodeNotFoundError("ノードが見つかりません".to_string());
    assert!(err2.0.contains("ノード"));
}
