package tests

import (
	"errors"
	"testing"

	freeplane "github.com/metacoma/freeplane_plugin_grpc/grpc/golang"
	"google.golang.org/grpc/codes"
	"google.golang.org/grpc/status"
)

func TestFreeplaneError_Error(t *testing.T) {
	tests := []struct {
		name   string
		err    *freeplane.FreeplaneError
		expect string
	}{
		{
			name:   "connection error without cause",
			err:    freeplane.NewFreeplaneError(freeplane.ErrorConnection, "server unavailable", nil),
			expect: "connection error: server unavailable",
		},
		{
			name:   "operation error with cause",
			err:    freeplane.NewFreeplaneError(freeplane.ErrorOperation, "node not found", errors.New("root cause")),
			expect: "operation error: node not found (root cause)",
		},
		{
			name:   "node not found error",
			err:    freeplane.NewFreeplaneError(freeplane.ErrorNodeNotFound, "node abc123 not found", nil),
			expect: "node not found: node abc123 not found",
		},
		{
			name:   "mindmap error",
			err:    freeplane.NewFreeplaneError(freeplane.ErrorMindMap, "map is corrupted", nil),
			expect: "mindmap error: map is corrupted",
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			got := tt.err.Error()
			if got != tt.expect {
				t.Errorf("Error() = %q, want %q", got, tt.expect)
			}
		})
	}
}

func TestFreeplaneError_Is(t *testing.T) {
	connErr := freeplane.NewFreeplaneError(freeplane.ErrorConnection, "test", nil)
	opErr := freeplane.NewFreeplaneError(freeplane.ErrorOperation, "test", nil)
	nodeErr := freeplane.NewFreeplaneError(freeplane.ErrorNodeNotFound, "test", nil)

	// Same kind should match
	if !errors.Is(connErr, freeplane.NewFreeplaneError(freeplane.ErrorConnection, "other", nil)) {
		t.Error("expected errors.Is to match for same kind")
	}

	// Different kinds should not match
	if errors.Is(connErr, opErr) {
		t.Error("expected errors.Is to not match for different kinds")
	}

	// Sentinel errors should match
	if !errors.Is(connErr, freeplane.ErrConnection) {
		t.Error("expected errors.Is to match ErrConnection sentinel")
	}
	if !errors.Is(opErr, freeplane.ErrOperation) {
		t.Error("expected errors.Is to match ErrOperation sentinel")
	}
	if !errors.Is(nodeErr, freeplane.ErrNodeNotFound) {
		t.Error("expected errors.Is to match ErrNodeNotFound sentinel")
	}
}

func TestFreeplaneError_As(t *testing.T) {
	var err error = freeplane.NewFreeplaneError(freeplane.ErrorConnection, "test", nil)

	var freeplaneErr *freeplane.FreeplaneError
	if !errors.As(err, &freeplaneErr) {
		t.Fatal("expected errors.As to succeed")
	}
	if freeplaneErr.Kind != freeplane.ErrorConnection {
		t.Errorf("expected Kind == ErrorConnection, got %v", freeplaneErr.Kind)
	}
}

func TestFreeplaneError_Unwrap(t *testing.T) {
	cause := errors.New("root cause")
	err := freeplane.NewFreeplaneError(freeplane.ErrorOperation, "wrapped", cause)

	if !errors.Is(err, cause) {
		t.Error("expected errors.Is to find wrapped cause")
	}
}

func TestMapGRPCStatus(t *testing.T) {
	tests := []struct {
		code   codes.Code
		expect freeplane.FreeplaneErrorKind
	}{
		{codes.Unavailable, freeplane.ErrorConnection},
		{codes.DeadlineExceeded, freeplane.ErrorConnection},
		{codes.ResourceExhausted, freeplane.ErrorConnection},
		{codes.NotFound, freeplane.ErrorOperation},
		{codes.InvalidArgument, freeplane.ErrorOperation},
		{codes.PermissionDenied, freeplane.ErrorOperation},
		{codes.Unknown, freeplane.ErrorOperation},
	}

	for _, tt := range tests {
		t.Run(tt.code.String(), func(t *testing.T) {
			got := freeplane.MapGRPCStatus(tt.code)
			if got != tt.expect {
				t.Errorf("mapGRPCStatus(%v) = %v, want %v", tt.code, got, tt.expect)
			}
		})
	}
}

func TestStatusToError(t *testing.T) {
	grpcErr := status.Error(codes.Unavailable, "server unavailable")
	err := freeplane.StatusToError(grpcErr)

	if err.Kind != freeplane.ErrorConnection {
		t.Errorf("expected ErrorConnection, got %v", err.Kind)
	}
	if err.Cause != grpcErr {
		t.Error("expected cause to be the original gRPC error")
	}
}

func TestIsConnectionError(t *testing.T) {
	if freeplane.IsConnectionError(nil) {
		t.Error("IsConnectionError(nil) should be false")
	}
	if !freeplane.IsConnectionError(freeplane.ErrConnection) {
		t.Error("IsConnectionError(ErrConnection) should be true")
	}
	if freeplane.IsConnectionError(freeplane.ErrOperation) {
		t.Error("IsConnectionError(ErrOperation) should be false")
	}
}

func TestIsOperationError(t *testing.T) {
	if freeplane.IsOperationError(nil) {
		t.Error("IsOperationError(nil) should be false")
	}
	if !freeplane.IsOperationError(freeplane.ErrOperation) {
		t.Error("IsOperationError(ErrOperation) should be true")
	}
	if freeplane.IsOperationError(freeplane.ErrConnection) {
		t.Error("IsOperationError(ErrConnection) should be false")
	}
}

func TestIsNodeNotFoundError(t *testing.T) {
	if freeplane.IsNodeNotFoundError(nil) {
		t.Error("IsNodeNotFoundError(nil) should be false")
	}
	if !freeplane.IsNodeNotFoundError(freeplane.ErrNodeNotFound) {
		t.Error("IsNodeNotFoundError(ErrNodeNotFound) should be true")
	}
	if freeplane.IsNodeNotFoundError(freeplane.ErrOperation) {
		t.Error("IsNodeNotFoundError(ErrOperation) should be false")
	}
}

func TestErrorHierarchy(t *testing.T) {
	// All error types should be *FreeplaneError
	var err error

	err = freeplane.NewFreeplaneError(freeplane.ErrorGrpc, "grpc error", nil)
	var fe *freeplane.FreeplaneError
	if !errors.As(err, &fe) {
		t.Error("ErrorGrpc should be *FreeplaneError")
	}

	err = freeplane.NewFreeplaneError(freeplane.ErrorConnection, "connection error", nil)
	if !errors.As(err, &fe) {
		t.Error("ErrorConnection should be *FreeplaneError")
	}

	err = freeplane.NewFreeplaneError(freeplane.ErrorOperation, "operation error", nil)
	if !errors.As(err, &fe) {
		t.Error("ErrorOperation should be *FreeplaneError")
	}

	err = freeplane.NewFreeplaneError(freeplane.ErrorNodeNotFound, "node not found", nil)
	if !errors.As(err, &fe) {
		t.Error("ErrorNodeNotFound should be *FreeplaneError")
	}

	err = freeplane.NewFreeplaneError(freeplane.ErrorMindMap, "mindmap error", nil)
	if !errors.As(err, &fe) {
		t.Error("ErrorMindMap should be *FreeplaneError")
	}
}
