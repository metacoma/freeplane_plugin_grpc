package freeplane_grpc

import (
	"context"
	"errors"
	"testing"

	"google.golang.org/grpc"
	"google.golang.org/grpc/codes"
	"google.golang.org/grpc/status"

	"github.com/metacoma/freeplane_plugin_grpc/grpc/golang/freeplane"
)

// mockFreeplaneClient implements freeplane.FreeplaneClient for testing.
type mockFreeplaneClient struct {
	createChildFn            func(ctx context.Context, in *freeplane.CreateChildRequest, opts ...grpc.CallOption) (*freeplane.CreateChildResponse, error)
	deleteChildFn            func(ctx context.Context, in *freeplane.DeleteChildRequest, opts ...grpc.CallOption) (*freeplane.DeleteChildResponse, error)
	getCurrentNodeFn         func(ctx context.Context, in *freeplane.GetCurrentNodeRequest, opts ...grpc.CallOption) (*freeplane.GetCurrentNodeResponse, error)
	openMapFn                func(ctx context.Context, in *freeplane.OpenMapRequest, opts ...grpc.CallOption) (*freeplane.OpenMapResponse, error)
	getNodeTextFn            func(ctx context.Context, in *freeplane.GetNodeTextRequest, opts ...grpc.CallOption) (*freeplane.GetNodeTextResponse, error)
	getParentNodeFn          func(ctx context.Context, in *freeplane.GetParentNodeRequest, opts ...grpc.CallOption) (*freeplane.GetParentNodeResponse, error)
	listChildNodesFn         func(ctx context.Context, in *freeplane.ListChildNodesRequest, opts ...grpc.CallOption) (*freeplane.ListChildNodesResponse, error)
	setNodeTextFn            func(ctx context.Context, in *freeplane.SetNodeTextRequest, opts ...grpc.CallOption) (*freeplane.SetNodeTextResponse, error)
	moveNodeFn               func(ctx context.Context, in *freeplane.MoveNodeRequest, opts ...grpc.CallOption) (*freeplane.MoveNodeResponse, error)
	mindMapToJSONFn          func(ctx context.Context, in *freeplane.MindMapToJSONRequest, opts ...grpc.CallOption) (*freeplane.MindMapToJSONResponse, error)
	mindMapFromJSONFn        func(ctx context.Context, in *freeplane.MindMapFromJSONRequest, opts ...grpc.CallOption) (*freeplane.MindMapFromJSONResponse, error)
	groovyFn                 func(ctx context.Context, in *freeplane.GroovyRequest, opts ...grpc.CallOption) (*freeplane.GroovyResponse, error)
	focusNodeFn              func(ctx context.Context, in *freeplane.FocusNodeRequest, opts ...grpc.CallOption) (*freeplane.FocusNodeResponse, error)
	statusInfoSetFn          func(ctx context.Context, in *freeplane.StatusInfoSetRequest, opts ...grpc.CallOption) (*freeplane.StatusInfoSetResponse, error)
	nodeNoteSetFn            func(ctx context.Context, in *freeplane.NodeNoteSetRequest, opts ...grpc.CallOption) (*freeplane.NodeNoteSetResponse, error)
	getNodeNoteFn            func(ctx context.Context, in *freeplane.GetNodeNoteRequest, opts ...grpc.CallOption) (*freeplane.GetNodeNoteResponse, error)
	nodeTagSetFn             func(ctx context.Context, in *freeplane.NodeTagSetRequest, opts ...grpc.CallOption) (*freeplane.NodeTagSetResponse, error)
	nodeTagAddFn             func(ctx context.Context, in *freeplane.NodeTagAddRequest, opts ...grpc.CallOption) (*freeplane.NodeTagAddResponse, error)
	nodeLinkSetFn            func(ctx context.Context, in *freeplane.NodeLinkSetRequest, opts ...grpc.CallOption) (*freeplane.NodeLinkSetResponse, error)
	getNodeLinkFn            func(ctx context.Context, in *freeplane.GetNodeLinkRequest, opts ...grpc.CallOption) (*freeplane.GetNodeLinkResponse, error)
	nodeAttributeAddFn       func(ctx context.Context, in *freeplane.NodeAttributeAddRequest, opts ...grpc.CallOption) (*freeplane.NodeAttributeAddResponse, error)
	nodeDetailsSetFn         func(ctx context.Context, in *freeplane.NodeDetailsSetRequest, opts ...grpc.CallOption) (*freeplane.NodeDetailsSetResponse, error)
	nodeColorSetFn           func(ctx context.Context, in *freeplane.NodeColorSetRequest, opts ...grpc.CallOption) (*freeplane.NodeColorSetResponse, error)
	nodeBackgroundColorSetFn func(ctx context.Context, in *freeplane.NodeBackgroundColorSetRequest, opts ...grpc.CallOption) (*freeplane.NodeBackgroundColorSetResponse, error)
	nodeConnectFn            func(ctx context.Context, in *freeplane.NodeConnectRequest, opts ...grpc.CallOption) (*freeplane.NodeConnectResponse, error)
	nodeAddIconFn            func(ctx context.Context, in *freeplane.NodeAddIconRequest, opts ...grpc.CallOption) (*freeplane.NodeAddIconResponse, error)
	textFSMFn                func(ctx context.Context, in *freeplane.TextFSMRequest, opts ...grpc.CallOption) (*freeplane.TextFSMResponse, error)
}

func (m *mockFreeplaneClient) CreateChild(ctx context.Context, in *freeplane.CreateChildRequest, opts ...grpc.CallOption) (*freeplane.CreateChildResponse, error) {
	if m.createChildFn != nil {
		return m.createChildFn(ctx, in)
	}
	return &freeplane.CreateChildResponse{NodeId: "mock-node-id", NodeText: "mock text"}, nil
}

func (m *mockFreeplaneClient) DeleteChild(ctx context.Context, in *freeplane.DeleteChildRequest, opts ...grpc.CallOption) (*freeplane.DeleteChildResponse, error) {
	if m.deleteChildFn != nil {
		return m.deleteChildFn(ctx, in)
	}
	return &freeplane.DeleteChildResponse{Success: true}, nil
}

func (m *mockFreeplaneClient) GetCurrentNode(ctx context.Context, in *freeplane.GetCurrentNodeRequest, opts ...grpc.CallOption) (*freeplane.GetCurrentNodeResponse, error) {
	if m.getCurrentNodeFn != nil {
		return m.getCurrentNodeFn(ctx, in)
	}
	return &freeplane.GetCurrentNodeResponse{MapId: "mock-map-id", NodeId: "mock-node-id", Success: true}, nil
}

func (m *mockFreeplaneClient) OpenMap(ctx context.Context, in *freeplane.OpenMapRequest, opts ...grpc.CallOption) (*freeplane.OpenMapResponse, error) {
	if m.openMapFn != nil {
		return m.openMapFn(ctx, in)
	}
	return &freeplane.OpenMapResponse{Success: true}, nil
}

func (m *mockFreeplaneClient) GetNodeText(ctx context.Context, in *freeplane.GetNodeTextRequest, opts ...grpc.CallOption) (*freeplane.GetNodeTextResponse, error) {
	if m.getNodeTextFn != nil {
		return m.getNodeTextFn(ctx, in)
	}
	return &freeplane.GetNodeTextResponse{Success: true, NodeId: in.NodeId, Text: "mock text"}, nil
}

func (m *mockFreeplaneClient) GetParentNode(ctx context.Context, in *freeplane.GetParentNodeRequest, opts ...grpc.CallOption) (*freeplane.GetParentNodeResponse, error) {
	if m.getParentNodeFn != nil {
		return m.getParentNodeFn(ctx, in)
	}
	return &freeplane.GetParentNodeResponse{Success: true, NodeId: in.NodeId, ParentNodeId: "mock-parent-id", ParentNodeText: "parent text"}, nil
}

func (m *mockFreeplaneClient) ListChildNodes(ctx context.Context, in *freeplane.ListChildNodesRequest, opts ...grpc.CallOption) (*freeplane.ListChildNodesResponse, error) {
	if m.listChildNodesFn != nil {
		return m.listChildNodesFn(ctx, in)
	}
	return &freeplane.ListChildNodesResponse{Success: true, Children: []*freeplane.ChildNodeInfo{{NodeId: "child-1", Text: "child 1"}}}, nil
}

func (m *mockFreeplaneClient) SetNodeText(ctx context.Context, in *freeplane.SetNodeTextRequest, opts ...grpc.CallOption) (*freeplane.SetNodeTextResponse, error) {
	if m.setNodeTextFn != nil {
		return m.setNodeTextFn(ctx, in)
	}
	return &freeplane.SetNodeTextResponse{Success: true, NodeId: in.NodeId}, nil
}

func (m *mockFreeplaneClient) MoveNode(ctx context.Context, in *freeplane.MoveNodeRequest, opts ...grpc.CallOption) (*freeplane.MoveNodeResponse, error) {
	if m.moveNodeFn != nil {
		return m.moveNodeFn(ctx, in)
	}
	return &freeplane.MoveNodeResponse{Success: true}, nil
}

func (m *mockFreeplaneClient) MindMapToJSON(ctx context.Context, in *freeplane.MindMapToJSONRequest, opts ...grpc.CallOption) (*freeplane.MindMapToJSONResponse, error) {
	if m.mindMapToJSONFn != nil {
		return m.mindMapToJSONFn(ctx, in)
	}
	return &freeplane.MindMapToJSONResponse{Success: true, Json: `{"nodes":[]}`}, nil
}

func (m *mockFreeplaneClient) MindMapFromJSON(ctx context.Context, in *freeplane.MindMapFromJSONRequest, opts ...grpc.CallOption) (*freeplane.MindMapFromJSONResponse, error) {
	if m.mindMapFromJSONFn != nil {
		return m.mindMapFromJSONFn(ctx, in)
	}
	return &freeplane.MindMapFromJSONResponse{Success: true}, nil
}

func (m *mockFreeplaneClient) Groovy(ctx context.Context, in *freeplane.GroovyRequest, opts ...grpc.CallOption) (*freeplane.GroovyResponse, error) {
	if m.groovyFn != nil {
		return m.groovyFn(ctx, in)
	}
	return &freeplane.GroovyResponse{Success: true, Result: "groovy result"}, nil
}

func (m *mockFreeplaneClient) FocusNode(ctx context.Context, in *freeplane.FocusNodeRequest, opts ...grpc.CallOption) (*freeplane.FocusNodeResponse, error) {
	if m.focusNodeFn != nil {
		return m.focusNodeFn(ctx, in)
	}
	return &freeplane.FocusNodeResponse{Success: true}, nil
}

func (m *mockFreeplaneClient) StatusInfoSet(ctx context.Context, in *freeplane.StatusInfoSetRequest, opts ...grpc.CallOption) (*freeplane.StatusInfoSetResponse, error) {
	if m.statusInfoSetFn != nil {
		return m.statusInfoSetFn(ctx, in)
	}
	return &freeplane.StatusInfoSetResponse{Success: true}, nil
}

func (m *mockFreeplaneClient) NodeNoteSet(ctx context.Context, in *freeplane.NodeNoteSetRequest, opts ...grpc.CallOption) (*freeplane.NodeNoteSetResponse, error) {
	if m.nodeNoteSetFn != nil {
		return m.nodeNoteSetFn(ctx, in)
	}
	return &freeplane.NodeNoteSetResponse{Success: true}, nil
}

func (m *mockFreeplaneClient) GetNodeNote(ctx context.Context, in *freeplane.GetNodeNoteRequest, opts ...grpc.CallOption) (*freeplane.GetNodeNoteResponse, error) {
	if m.getNodeNoteFn != nil {
		return m.getNodeNoteFn(ctx, in)
	}
	return &freeplane.GetNodeNoteResponse{Success: true, NodeId: in.NodeId, Note: "mock note", HasNote: true}, nil
}

func (m *mockFreeplaneClient) NodeTagSet(ctx context.Context, in *freeplane.NodeTagSetRequest, opts ...grpc.CallOption) (*freeplane.NodeTagSetResponse, error) {
	if m.nodeTagSetFn != nil {
		return m.nodeTagSetFn(ctx, in)
	}
	return &freeplane.NodeTagSetResponse{Success: true}, nil
}

func (m *mockFreeplaneClient) NodeTagAdd(ctx context.Context, in *freeplane.NodeTagAddRequest, opts ...grpc.CallOption) (*freeplane.NodeTagAddResponse, error) {
	if m.nodeTagAddFn != nil {
		return m.nodeTagAddFn(ctx, in)
	}
	return &freeplane.NodeTagAddResponse{Success: true}, nil
}

func (m *mockFreeplaneClient) NodeLinkSet(ctx context.Context, in *freeplane.NodeLinkSetRequest, opts ...grpc.CallOption) (*freeplane.NodeLinkSetResponse, error) {
	if m.nodeLinkSetFn != nil {
		return m.nodeLinkSetFn(ctx, in)
	}
	return &freeplane.NodeLinkSetResponse{Success: true}, nil
}

func (m *mockFreeplaneClient) GetNodeLink(ctx context.Context, in *freeplane.GetNodeLinkRequest, opts ...grpc.CallOption) (*freeplane.GetNodeLinkResponse, error) {
	if m.getNodeLinkFn != nil {
		return m.getNodeLinkFn(ctx, in)
	}
	return &freeplane.GetNodeLinkResponse{Success: true, NodeId: in.NodeId, Link: "http://example.com", HasLink: true}, nil
}

func (m *mockFreeplaneClient) NodeAttributeAdd(ctx context.Context, in *freeplane.NodeAttributeAddRequest, opts ...grpc.CallOption) (*freeplane.NodeAttributeAddResponse, error) {
	if m.nodeAttributeAddFn != nil {
		return m.nodeAttributeAddFn(ctx, in)
	}
	return &freeplane.NodeAttributeAddResponse{Success: true}, nil
}

func (m *mockFreeplaneClient) NodeDetailsSet(ctx context.Context, in *freeplane.NodeDetailsSetRequest, opts ...grpc.CallOption) (*freeplane.NodeDetailsSetResponse, error) {
	if m.nodeDetailsSetFn != nil {
		return m.nodeDetailsSetFn(ctx, in)
	}
	return &freeplane.NodeDetailsSetResponse{Success: true}, nil
}

func (m *mockFreeplaneClient) NodeColorSet(ctx context.Context, in *freeplane.NodeColorSetRequest, opts ...grpc.CallOption) (*freeplane.NodeColorSetResponse, error) {
	if m.nodeColorSetFn != nil {
		return m.nodeColorSetFn(ctx, in)
	}
	return &freeplane.NodeColorSetResponse{Success: true}, nil
}

func (m *mockFreeplaneClient) NodeBackgroundColorSet(ctx context.Context, in *freeplane.NodeBackgroundColorSetRequest, opts ...grpc.CallOption) (*freeplane.NodeBackgroundColorSetResponse, error) {
	if m.nodeBackgroundColorSetFn != nil {
		return m.nodeBackgroundColorSetFn(ctx, in)
	}
	return &freeplane.NodeBackgroundColorSetResponse{Success: true}, nil
}

func (m *mockFreeplaneClient) NodeConnect(ctx context.Context, in *freeplane.NodeConnectRequest, opts ...grpc.CallOption) (*freeplane.NodeConnectResponse, error) {
	if m.nodeConnectFn != nil {
		return m.nodeConnectFn(ctx, in)
	}
	return &freeplane.NodeConnectResponse{Success: true}, nil
}

func (m *mockFreeplaneClient) NodeAddIcon(ctx context.Context, in *freeplane.NodeAddIconRequest, opts ...grpc.CallOption) (*freeplane.NodeAddIconResponse, error) {
	if m.nodeAddIconFn != nil {
		return m.nodeAddIconFn(ctx, in)
	}
	return &freeplane.NodeAddIconResponse{Success: true}, nil
}

func (m *mockFreeplaneClient) TextFSM(ctx context.Context, in *freeplane.TextFSMRequest, opts ...grpc.CallOption) (*freeplane.TextFSMResponse, error) {
	if m.textFSMFn != nil {
		return m.textFSMFn(ctx, in)
	}
	return &freeplane.TextFSMResponse{Success: true}, nil
}

// testClient wraps a mockFreeplaneClient as a FreeplaneClient for testing.
type testClient struct {
	*FreeplaneClient
	mock *mockFreeplaneClient
}

func newTestClient(mock *mockFreeplaneClient) *FreeplaneClient {
	return &FreeplaneClient{
		host: "127.0.0.1",
		port: 50051,
		stub: mock,
	}
}

func TestNewClient(t *testing.T) {
	c, err := NewClient("", 0)
	if err != nil {
		t.Fatalf("NewClient() error: %v", err)
	}
	if c.host != "127.0.0.1" {
		t.Errorf("expected host 127.0.0.1, got %s", c.host)
	}
	if c.port != 50051 {
		t.Errorf("expected port 50051, got %d", c.port)
	}
}

func TestIsConnectionError(t *testing.T) {
	if isConnectionError(nil) {
		t.Error("isConnectionError(nil) should be false")
	}

	connErr := NewConnectionError("connection failed")
	if isConnectionError(connErr) {
		t.Error("isConnectionError(FreeplaneConnectionError) should be false (not a gRPC status)")
	}

	grpcErr := status.Error(codes.Unavailable, "server unavailable")
	if !isConnectionError(grpcErr) {
		t.Error("isConnectionError(Unavailable) should be true")
	}

	deadlineErr := status.Error(codes.DeadlineExceeded, "deadline exceeded")
	if !isConnectionError(deadlineErr) {
		t.Error("isConnectionError(DeadlineExceeded) should be true")
	}

	resourceErr := status.Error(codes.ResourceExhausted, "resource exhausted")
	if !isConnectionError(resourceErr) {
		t.Error("isConnectionError(ResourceExhausted) should be true")
	}

	otherErr := status.Error(codes.NotFound, "not found")
	if isConnectionError(otherErr) {
		t.Error("isConnectionError(NotFound) should be false")
	}
}

func TestStatusToError(t *testing.T) {
	tests := []struct {
		name     string
		s        *status.Status
		expected error
	}{
		{
			name:     "connection error",
			s:        status.New(codes.Unavailable, "unavailable"),
			expected: &FreeplaneConnectionError{},
		},
		{
			name:     "node not found",
			s:        status.New(codes.NotFound, "node not found"),
			expected: &NodeNotFoundError{},
		},
		{
			name:     "operation error",
			s:        status.New(codes.Internal, "internal error"),
			expected: &FreeplaneOperationError{},
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			err := statusToError(tt.s)
			var target interface{}
			switch tt.expected.(type) {
			case *FreeplaneConnectionError:
				target = &FreeplaneConnectionError{}
			case *NodeNotFoundError:
				target = &NodeNotFoundError{}
			case *FreeplaneOperationError:
				target = &FreeplaneOperationError{}
			}
			if !errors.As(err, &target) {
				t.Errorf("statusToError() type mismatch: got %T, want %T", err, target)
			}
		})
	}
}

func TestNewErrorTypes(t *testing.T) {
	tests := []struct {
		name   string
		err    error
		target interface{}
	}{
		{
			name:   "FreeplaneGrpcError",
			err:    NewConnectionError("test"),
			target: &FreeplaneGrpcError{},
		},
		{
			name:   "FreeplaneConnectionError",
			err:    NewConnectionError("test"),
			target: &FreeplaneConnectionError{},
		},
		{
			name:   "FreeplaneOperationError",
			err:    NewOperationError("test"),
			target: &FreeplaneOperationError{},
		},
		{
			name:   "NodeNotFoundError",
			err:    NewNodeNotFoundError("test"),
			target: &NodeNotFoundError{},
		},
		{
			name:   "MindMapError",
			err:    NewMindMapError("test"),
			target: &MindMapError{},
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			ptr := tt.target
			if !errors.As(tt.err, &ptr) {
				t.Errorf("errors.As() failed: got %T, want %T", tt.err, tt.target)
			}
		})
	}
}

func TestCheckSuccess(t *testing.T) {
	t.Run("success true", func(t *testing.T) {
		resp := &freeplane.DeleteChildResponse{Success: true}
		err := checkSuccess(resp)
		if err != nil {
			t.Errorf("expected nil error, got %v", err)
		}
	})

	t.Run("success false", func(t *testing.T) {
		resp := &freeplane.DeleteChildResponse{Success: false}
		err := checkSuccess(resp)
		if err == nil {
			t.Error("expected error, got nil")
		}
	})
}

func TestCreateChild(t *testing.T) {
	mock := &mockFreeplaneClient{}
	client := newTestClient(mock)

	resp, err := client.CreateChild(context.Background(), "test child", "parent-id")
	if err != nil {
		t.Fatalf("CreateChild() error: %v", err)
	}
	if resp.NodeId != "mock-node-id" {
		t.Errorf("expected node-id mock-node-id, got %s", resp.NodeId)
	}
}

func TestDeleteChild(t *testing.T) {
	mock := &mockFreeplaneClient{}
	client := newTestClient(mock)

	resp, err := client.DeleteChild(context.Background(), "node-id")
	if err != nil {
		t.Fatalf("DeleteChild() error: %v", err)
	}
	if !resp.Success {
		t.Error("expected success true")
	}
}

func TestGetCurrentNode(t *testing.T) {
	mock := &mockFreeplaneClient{}
	client := newTestClient(mock)

	resp, err := client.GetCurrentNode(context.Background())
	if err != nil {
		t.Fatalf("GetCurrentNode() error: %v", err)
	}
	if !resp.Success {
		t.Error("expected success true")
	}
}

func TestOpenMap(t *testing.T) {
	mock := &mockFreeplaneClient{}
	client := newTestClient(mock)

	resp, err := client.OpenMap(context.Background(), "/path/to/map.mm")
	if err != nil {
		t.Fatalf("OpenMap() error: %v", err)
	}
	if !resp.Success {
		t.Error("expected success true")
	}
}

func TestGetNodeText(t *testing.T) {
	mock := &mockFreeplaneClient{}
	client := newTestClient(mock)

	resp, err := client.GetNodeText(context.Background(), "node-id")
	if err != nil {
		t.Fatalf("GetNodeText() error: %v", err)
	}
	if resp.Text != "mock text" {
		t.Errorf("expected text 'mock text', got %s", resp.Text)
	}
}

func TestSetNodeText(t *testing.T) {
	mock := &mockFreeplaneClient{}
	client := newTestClient(mock)

	resp, err := client.SetNodeText(context.Background(), "node-id", "new text")
	if err != nil {
		t.Fatalf("SetNodeText() error: %v", err)
	}
	if !resp.Success {
		t.Error("expected success true")
	}
}

func TestMoveNode(t *testing.T) {
	mock := &mockFreeplaneClient{}
	client := newTestClient(mock)

	resp, err := client.MoveNode(context.Background(), "node-id", "new-parent-id")
	if err != nil {
		t.Fatalf("MoveNode() error: %v", err)
	}
	if !resp.Success {
		t.Error("expected success true")
	}
}

func TestListChildNodes(t *testing.T) {
	mock := &mockFreeplaneClient{}
	client := newTestClient(mock)

	resp, err := client.ListChildNodes(context.Background(), "node-id")
	if err != nil {
		t.Fatalf("ListChildNodes() error: %v", err)
	}
	if len(resp.Children) != 1 {
		t.Errorf("expected 1 child, got %d", len(resp.Children))
	}
}

func TestGetParentNode(t *testing.T) {
	mock := &mockFreeplaneClient{}
	client := newTestClient(mock)

	resp, err := client.GetParentNode(context.Background(), "node-id")
	if err != nil {
		t.Fatalf("GetParentNode() error: %v", err)
	}
	if resp.ParentNodeId != "mock-parent-id" {
		t.Errorf("expected parent-node-id mock-parent-id, got %s", resp.ParentNodeId)
	}
}

func TestMindMapToJSON(t *testing.T) {
	mock := &mockFreeplaneClient{}
	client := newTestClient(mock)

	resp, err := client.MindMapToJSON(context.Background())
	if err != nil {
		t.Fatalf("MindMapToJSON() error: %v", err)
	}
	if resp.Json != `{"nodes":[]}` {
		t.Errorf("expected json {'nodes':[]}, got %s", resp.Json)
	}
}

func TestMindMapFromJSON(t *testing.T) {
	mock := &mockFreeplaneClient{}
	client := newTestClient(mock)

	resp, err := client.MindMapFromJSON(context.Background(), `{"nodes":[]}`)
	if err != nil {
		t.Fatalf("MindMapFromJSON() error: %v", err)
	}
	if !resp.Success {
		t.Error("expected success true")
	}
}

func TestGroovy(t *testing.T) {
	mock := &mockFreeplaneClient{}
	client := newTestClient(mock)

	resp, err := client.Groovy(context.Background(), "return 'hello'")
	if err != nil {
		t.Fatalf("Groovy() error: %v", err)
	}
	if resp.Result != "groovy result" {
		t.Errorf("expected result 'groovy result', got %s", resp.Result)
	}
}

func TestFocusNode(t *testing.T) {
	mock := &mockFreeplaneClient{}
	client := newTestClient(mock)

	resp, err := client.FocusNode(context.Background(), "node-id")
	if err != nil {
		t.Fatalf("FocusNode() error: %v", err)
	}
	if !resp.Success {
		t.Error("expected success true")
	}
}

func TestStatusInfoSet(t *testing.T) {
	mock := &mockFreeplaneClient{}
	client := newTestClient(mock)

	resp, err := client.StatusInfoSet(context.Background(), "test status")
	if err != nil {
		t.Fatalf("StatusInfoSet() error: %v", err)
	}
	if !resp.Success {
		t.Error("expected success true")
	}
}

func TestNodeNoteSet(t *testing.T) {
	mock := &mockFreeplaneClient{}
	client := newTestClient(mock)

	resp, err := client.NodeNoteSet(context.Background(), "node-id", "test note")
	if err != nil {
		t.Fatalf("NodeNoteSet() error: %v", err)
	}
	if !resp.Success {
		t.Error("expected success true")
	}
}

func TestGetNodeNote(t *testing.T) {
	mock := &mockFreeplaneClient{}
	client := newTestClient(mock)

	resp, err := client.GetNodeNote(context.Background(), "node-id")
	if err != nil {
		t.Fatalf("GetNodeNote() error: %v", err)
	}
	if !resp.HasNote {
		t.Error("expected has_note true")
	}
}

func TestNodeTagSet(t *testing.T) {
	mock := &mockFreeplaneClient{}
	client := newTestClient(mock)

	resp, err := client.NodeTagSet(context.Background(), "node-id", []string{"tag1", "tag2"})
	if err != nil {
		t.Fatalf("NodeTagSet() error: %v", err)
	}
	if !resp.Success {
		t.Error("expected success true")
	}
}

func TestNodeTagAdd(t *testing.T) {
	mock := &mockFreeplaneClient{}
	client := newTestClient(mock)

	resp, err := client.NodeTagAdd(context.Background(), "node-id", []string{"tag1"})
	if err != nil {
		t.Fatalf("NodeTagAdd() error: %v", err)
	}
	if !resp.Success {
		t.Error("expected success true")
	}
}

func TestNodeLinkSet(t *testing.T) {
	mock := &mockFreeplaneClient{}
	client := newTestClient(mock)

	resp, err := client.NodeLinkSet(context.Background(), "node-id", "http://example.com")
	if err != nil {
		t.Fatalf("NodeLinkSet() error: %v", err)
	}
	if !resp.Success {
		t.Error("expected success true")
	}
}

func TestGetNodeLink(t *testing.T) {
	mock := &mockFreeplaneClient{}
	client := newTestClient(mock)

	resp, err := client.GetNodeLink(context.Background(), "node-id")
	if err != nil {
		t.Fatalf("GetNodeLink() error: %v", err)
	}
	if !resp.HasLink {
		t.Error("expected has_link true")
	}
}

func TestNodeAttributeAdd(t *testing.T) {
	mock := &mockFreeplaneClient{}
	client := newTestClient(mock)

	resp, err := client.NodeAttributeAdd(context.Background(), "node-id", "key", "value")
	if err != nil {
		t.Fatalf("NodeAttributeAdd() error: %v", err)
	}
	if !resp.Success {
		t.Error("expected success true")
	}
}

func TestNodeDetailsSet(t *testing.T) {
	mock := &mockFreeplaneClient{}
	client := newTestClient(mock)

	resp, err := client.NodeDetailsSet(context.Background(), "node-id", "details")
	if err != nil {
		t.Fatalf("NodeDetailsSet() error: %v", err)
	}
	if !resp.Success {
		t.Error("expected success true")
	}
}

func TestNodeColorSet(t *testing.T) {
	mock := &mockFreeplaneClient{}
	client := newTestClient(mock)

	resp, err := client.NodeColorSet(context.Background(), "node-id", 255, 0, 0, 255)
	if err != nil {
		t.Fatalf("NodeColorSet() error: %v", err)
	}
	if !resp.Success {
		t.Error("expected success true")
	}
}

func TestNodeBackgroundColorSet(t *testing.T) {
	mock := &mockFreeplaneClient{}
	client := newTestClient(mock)

	resp, err := client.NodeBackgroundColorSet(context.Background(), "node-id", 0, 255, 0, 255)
	if err != nil {
		t.Fatalf("NodeBackgroundColorSet() error: %v", err)
	}
	if !resp.Success {
		t.Error("expected success true")
	}
}

func TestNodeConnect(t *testing.T) {
	mock := &mockFreeplaneClient{}
	client := newTestClient(mock)

	resp, err := client.NodeConnect(context.Background(), "src-id", "tgt-id", "related")
	if err != nil {
		t.Fatalf("NodeConnect() error: %v", err)
	}
	if !resp.Success {
		t.Error("expected success true")
	}
}

func TestNodeAddIcon(t *testing.T) {
	mock := &mockFreeplaneClient{}
	client := newTestClient(mock)

	resp, err := client.NodeAddIcon(context.Background(), "node-id", "icon-name")
	if err != nil {
		t.Fatalf("NodeAddIcon() error: %v", err)
	}
	if !resp.Success {
		t.Error("expected success true")
	}
}

func TestTextFSM(t *testing.T) {
	mock := &mockFreeplaneClient{}
	client := newTestClient(mock)

	resp, err := client.TextFSM(context.Background(), `{"template": "test"}`)
	if err != nil {
		t.Fatalf("TextFSM() error: %v", err)
	}
	if !resp.Success {
		t.Error("expected success true")
	}
}

func TestCurrentMap(t *testing.T) {
	mock := &mockFreeplaneClient{}
	client := newTestClient(mock)

	mm, err := client.CurrentMap(context.Background())
	if err != nil {
		t.Fatalf("CurrentMap() error: %v", err)
	}
	if mm == nil {
		t.Fatal("expected non-nil MindMap")
	}
}

func TestGetMapToJSON(t *testing.T) {
	mock := &mockFreeplaneClient{}
	client := newTestClient(mock)

	json, err := client.GetMapToJSON(context.Background())
	if err != nil {
		t.Fatalf("GetMapToJSON() error: %v", err)
	}
	if json != `{"nodes":[]}` {
		t.Errorf("expected json {'nodes':[]}, got %s", json)
	}
}

func TestGroovyCode(t *testing.T) {
	mock := &mockFreeplaneClient{}
	client := newTestClient(mock)

	result, err := client.GroovyCode(context.Background(), "return 'test'")
	if err != nil {
		t.Fatalf("GroovyCode() error: %v", err)
	}
	if result != "groovy result" {
		t.Errorf("expected result 'groovy result', got %s", result)
	}
}
