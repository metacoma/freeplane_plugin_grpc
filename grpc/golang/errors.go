package freeplane_grpc

import (
	"fmt"

	"google.golang.org/grpc/codes"
	"google.golang.org/grpc/status"
)

// FreeplaneGrpcError is the base error type for all Freeplane gRPC errors.
type FreeplaneGrpcError struct {
	Message string
}

func (e *FreeplaneGrpcError) Error() string {
	return e.Message
}

// FreeplaneConnectionError indicates a failure to connect or communicate with the server.
type FreeplaneConnectionError struct {
	FreeplaneGrpcError
}

func NewConnectionError(format string, args ...interface{}) *FreeplaneConnectionError {
	return &FreeplaneConnectionError{
		FreeplaneGrpcError: FreeplaneGrpcError{
			Message: fmt.Sprintf(format, args...),
		},
	}
}

// FreeplaneOperationError indicates a failure reported by the server during an operation.
type FreeplaneOperationError struct {
	FreeplaneGrpcError
}

func NewOperationError(format string, args ...interface{}) *FreeplaneOperationError {
	return &FreeplaneOperationError{
		FreeplaneGrpcError: FreeplaneGrpcError{
			Message: fmt.Sprintf(format, args...),
		},
	}
}

// NodeNotFoundError indicates that a requested node was not found.
type NodeNotFoundError struct {
	FreeplaneOperationError
}

func NewNodeNotFoundError(format string, args ...interface{}) *NodeNotFoundError {
	return &NodeNotFoundError{
		FreeplaneOperationError: FreeplaneOperationError{
			FreeplaneGrpcError: FreeplaneGrpcError{
				Message: fmt.Sprintf(format, args...),
			},
		},
	}
}

// MindMapError indicates a failure at the mind map level.
type MindMapError struct {
	FreeplaneOperationError
}

func NewMindMapError(format string, args ...interface{}) *MindMapError {
	return &MindMapError{
		FreeplaneOperationError: FreeplaneOperationError{
			FreeplaneGrpcError: FreeplaneGrpcError{
				Message: fmt.Sprintf(format, args...),
			},
		},
	}
}

// isConnectionError checks if the error is a connection-level error.
func isConnectionError(err error) bool {
	if err == nil {
		return false
	}
	s, ok := status.FromError(err)
	if !ok {
		return false
	}
	code := s.Code()
	return code == codes.Unavailable || code == codes.DeadlineExceeded || code == codes.ResourceExhausted
}

// statusToError converts a gRPC status to a domain error.
func statusToError(s *status.Status) error {
	code := s.Code()
	switch code {
	case codes.Unavailable, codes.DeadlineExceeded, codes.ResourceExhausted:
		return NewConnectionError("connection error: %s", s.Message())
	case codes.NotFound:
		return NewNodeNotFoundError("node not found: %s", s.Message())
	default:
		return NewOperationError("operation failed: %s", s.Message())
	}
}
