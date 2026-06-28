package freeplane

import (
	"errors"
	"fmt"

	"google.golang.org/grpc/codes"
	"google.golang.org/grpc/status"
)

// FreeplaneErrorKind represents the category of a Freeplane error.
type FreeplaneErrorKind int

const (
	// ErrorGrpc indicates a generic gRPC error.
	ErrorGrpc FreeplaneErrorKind = iota
	// ErrorConnection indicates a connection-related error (server unavailable, deadline exceeded, etc.).
	ErrorConnection
	// ErrorOperation indicates an operation failed on the server side.
	ErrorOperation
	// ErrorNodeNotFound indicates a requested node was not found.
	ErrorNodeNotFound
	// ErrorMindMap indicates a mind map-level error.
	ErrorMindMap
)

// FreeplaneError is the main error type for all Freeplane gRPC errors.
// It supports errors.Is() and errors.As() for type checking.
type FreeplaneError struct {
	Kind FreeplaneErrorKind
	Msg  string
	Cause error
}

var (
	// ErrConnection is a sentinel error for connection failures.
	ErrConnection = &FreeplaneError{Kind: ErrorConnection}
	// ErrOperation is a sentinel error for operation failures.
	ErrOperation = &FreeplaneError{Kind: ErrorOperation}
	// ErrNodeNotFound is a sentinel error for node-not-found failures.
	ErrNodeNotFound = &FreeplaneError{Kind: ErrorNodeNotFound}
)

// Error implements the error interface.
func (e *FreeplaneError) Error() string {
	if e.Cause != nil {
		return fmt.Sprintf("%s: %s (%v)", e.kindLabel(), e.Msg, e.Cause)
	}
	return fmt.Sprintf("%s: %s", e.kindLabel(), e.Msg)
}

// Unwrap returns the underlying cause error.
func (e *FreeplaneError) Unwrap() error {
	return e.Cause
}

// Is supports errors.Is() matching by kind.
func (e *FreeplaneError) Is(target error) bool {
	t, ok := target.(*FreeplaneError)
	if !ok {
		// Also match sentinel errors by kind
		if t, ok := target.(*FreeplaneError); ok {
			return e.Kind == t.Kind
		}
		return false
	}
	return e.Kind == t.Kind
}

func (e *FreeplaneError) kindLabel() string {
	switch e.Kind {
	case ErrorConnection:
		return "connection error"
	case ErrorOperation:
		return "operation error"
	case ErrorNodeNotFound:
		return "node not found"
	case ErrorMindMap:
		return "mindmap error"
	default:
		return "gRPC error"
	}
}

// MapGRPCStatus maps a gRPC status code to a FreeplaneErrorKind.
func MapGRPCStatus(code codes.Code) FreeplaneErrorKind {
	switch code {
	case codes.Unavailable, codes.DeadlineExceeded, codes.ResourceExhausted:
		return ErrorConnection
	default:
		return ErrorOperation
	}
}

// StatusToError converts a gRPC error to a *FreeplaneError.
func StatusToError(err error) *FreeplaneError {
	st := status.Convert(err)
	kind := MapGRPCStatus(st.Code())
	return &FreeplaneError{
		Kind:  kind,
		Msg:   st.Message(),
		Cause: err,
	}
}

// isConnectionError checks if a gRPC status code indicates a connection problem.
func isConnectionError(code codes.Code) bool {
	return code == codes.Unavailable || code == codes.DeadlineExceeded || code == codes.ResourceExhausted
}

// NewFreeplaneError creates a new FreeplaneError with the given kind and message.
func NewFreeplaneError(kind FreeplaneErrorKind, msg string, cause error) *FreeplaneError {
	return &FreeplaneError{
		Kind:  kind,
		Msg:   msg,
		Cause: cause,
	}
}

// IsConnectionError checks if an error is a connection error.
func IsConnectionError(err error) bool {
	if err == nil {
		return false
	}
	var freeplaneErr *FreeplaneError
	return errors.As(err, &freeplaneErr) && freeplaneErr.Kind == ErrorConnection
}

// IsOperationError checks if an error is an operation error.
func IsOperationError(err error) bool {
	if err == nil {
		return false
	}
	var freeplaneErr *FreeplaneError
	return errors.As(err, &freeplaneErr) && freeplaneErr.Kind == ErrorOperation
}

// IsNodeNotFoundError checks if an error is a node-not-found error.
func IsNodeNotFoundError(err error) bool {
	if err == nil {
		return false
	}
	var freeplaneErr *FreeplaneError
	return errors.As(err, &freeplaneErr) && freeplaneErr.Kind == ErrorNodeNotFound
}
