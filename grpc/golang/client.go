package freeplane

import (
	"context"
	"fmt"
	"sync"
	"time"

	pb "github.com/metacoma/freeplane_plugin_grpc/grpc/golang/pb"
	"google.golang.org/grpc"
	"google.golang.org/grpc/credentials/insecure"
	"google.golang.org/grpc/status"
	"google.golang.org/protobuf/proto"
)

const (
	// DefaultHost is the default host for Freeplane gRPC server.
	DefaultHost = "127.0.0.1"
	// DefaultPort is the default port for Freeplane gRPC server.
	DefaultPort = 50051
	// DefaultTimeout is the default timeout for gRPC calls.
	DefaultTimeout = 30 * time.Second
)

// FreeplaneClientInterface defines the interface for all Freeplane gRPC methods.
// This interface is used for mocking in tests.
type FreeplaneClientInterface interface {
	CreateChild(ctx context.Context, in *pb.CreateChildRequest, opts ...grpc.CallOption) (*pb.CreateChildResponse, error)
	DeleteChild(ctx context.Context, in *pb.DeleteChildRequest, opts ...grpc.CallOption) (*pb.DeleteChildResponse, error)
	NodeAttributeAdd(ctx context.Context, in *pb.NodeAttributeAddRequest, opts ...grpc.CallOption) (*pb.NodeAttributeAddResponse, error)
	NodeLinkSet(ctx context.Context, in *pb.NodeLinkSetRequest, opts ...grpc.CallOption) (*pb.NodeLinkSetResponse, error)
	NodeDetailsSet(ctx context.Context, in *pb.NodeDetailsSetRequest, opts ...grpc.CallOption) (*pb.NodeDetailsSetResponse, error)
	NodeNoteSet(ctx context.Context, in *pb.NodeNoteSetRequest, opts ...grpc.CallOption) (*pb.NodeNoteSetResponse, error)
	NodeTagSet(ctx context.Context, in *pb.NodeTagSetRequest, opts ...grpc.CallOption) (*pb.NodeTagSetResponse, error)
	NodeTagAdd(ctx context.Context, in *pb.NodeTagAddRequest, opts ...grpc.CallOption) (*pb.NodeTagAddResponse, error)
	NodeConnect(ctx context.Context, in *pb.NodeConnectRequest, opts ...grpc.CallOption) (*pb.NodeConnectResponse, error)
	NodeAddIcon(ctx context.Context, in *pb.NodeAddIconRequest, opts ...grpc.CallOption) (*pb.NodeAddIconResponse, error)
	Groovy(ctx context.Context, in *pb.GroovyRequest, opts ...grpc.CallOption) (*pb.GroovyResponse, error)
	NodeColorSet(ctx context.Context, in *pb.NodeColorSetRequest, opts ...grpc.CallOption) (*pb.NodeColorSetResponse, error)
	NodeBackgroundColorSet(ctx context.Context, in *pb.NodeBackgroundColorSetRequest, opts ...grpc.CallOption) (*pb.NodeBackgroundColorSetResponse, error)
	StatusInfoSet(ctx context.Context, in *pb.StatusInfoSetRequest, opts ...grpc.CallOption) (*pb.StatusInfoSetResponse, error)
	TextFSM(ctx context.Context, in *pb.TextFSMRequest, opts ...grpc.CallOption) (*pb.TextFSMResponse, error)
	MindMapFromJSON(ctx context.Context, in *pb.MindMapFromJSONRequest, opts ...grpc.CallOption) (*pb.MindMapFromJSONResponse, error)
	MindMapToJSON(ctx context.Context, in *pb.MindMapToJSONRequest, opts ...grpc.CallOption) (*pb.MindMapToJSONResponse, error)
	GetCurrentNode(ctx context.Context, in *pb.GetCurrentNodeRequest, opts ...grpc.CallOption) (*pb.GetCurrentNodeResponse, error)
	OpenMap(ctx context.Context, in *pb.OpenMapRequest, opts ...grpc.CallOption) (*pb.OpenMapResponse, error)
	FocusNode(ctx context.Context, in *pb.FocusNodeRequest, opts ...grpc.CallOption) (*pb.FocusNodeResponse, error)
	GetNodeText(ctx context.Context, in *pb.GetNodeTextRequest, opts ...grpc.CallOption) (*pb.GetNodeTextResponse, error)
	GetParentNode(ctx context.Context, in *pb.GetParentNodeRequest, opts ...grpc.CallOption) (*pb.GetParentNodeResponse, error)
	ListChildNodes(ctx context.Context, in *pb.ListChildNodesRequest, opts ...grpc.CallOption) (*pb.ListChildNodesResponse, error)
	GetNodeNote(ctx context.Context, in *pb.GetNodeNoteRequest, opts ...grpc.CallOption) (*pb.GetNodeNoteResponse, error)
	GetNodeLink(ctx context.Context, in *pb.GetNodeLinkRequest, opts ...grpc.CallOption) (*pb.GetNodeLinkResponse, error)
	SetNodeText(ctx context.Context, in *pb.SetNodeTextRequest, opts ...grpc.CallOption) (*pb.SetNodeTextResponse, error)
	MoveNode(ctx context.Context, in *pb.MoveNodeRequest, opts ...grpc.CallOption) (*pb.MoveNodeResponse, error)
}

// FreeplaneClient is the main entry point for interacting with Freeplane via gRPC.
//
// Typical usage:
//
//	client := freeplane.NewFreeplaneClient("127.0.0.1", 50051)
//	defer client.Close()
//	if err := client.Connect(ctx); err != nil {
//	    log.Fatal(err)
//	}
//	mindmap, err := client.CurrentMap(ctx)
//	...
type FreeplaneClient struct {
	host     string
	port     int
	conn     *grpc.ClientConn
	stub     pb.FreeplaneClient
	mu       sync.Mutex
}

// NewFreeplaneClient creates a new FreeplaneClient with the given host and port.
// Default host is "127.0.0.1" and default port is 50051.
// Pass empty host or port 0 to use defaults.
func NewFreeplaneClient(host string, port int) *FreeplaneClient {
	if host == "" {
		host = DefaultHost
	}
	if port == 0 {
		port = DefaultPort
	}
	return &FreeplaneClient{
		host: host,
		port: port,
	}
}

// Host returns the server hostname.
func (c *FreeplaneClient) Host() string {
	return c.host
}

// Port returns the server port.
func (c *FreeplaneClient) Port() int {
	return c.port
}

// Connect opens the gRPC channel to the server.
func (c *FreeplaneClient) Connect(ctx context.Context) error {
	c.mu.Lock()
	defer c.mu.Unlock()

	addr := fmt.Sprintf("%s:%d", c.host, c.port)
	conn, err := grpc.NewClient(addr, grpc.WithTransportCredentials(insecure.NewCredentials()), grpc.WithBlock())
	if err != nil {
		return NewFreeplaneError(ErrorConnection, fmt.Sprintf("Failed to connect to Freeplane gRPC server at %s: %v", addr, err), err)
	}
	c.conn = conn
	c.stub = pb.NewFreeplaneClient(conn)
	return nil
}

// Close closes the gRPC channel.
func (c *FreeplaneClient) Close() error {
	c.mu.Lock()
	defer c.mu.Unlock()

	if c.conn != nil {
		err := c.conn.Close()
		c.conn = nil
		c.stub = nil
		return err
	}
	return nil
}

// Do executes a function with the client, providing a context-manager-like pattern.
// It is the caller's responsibility to ensure Connect() has been called.
func (c *FreeplaneClient) Do(fn func(*FreeplaneClient) error) error {
	return fn(c)
}

// call invokes a gRPC method and converts failures to *FreeplaneError.
// It checks the Success field on responses that have one.
func (c *FreeplaneClient) call(ctx context.Context, methodName string, fn func(context.Context) (proto.Message, error)) (proto.Message, error) {
	timeoutCtx, cancel := context.WithTimeout(ctx, DefaultTimeout)
	defer cancel()

	resp, err := fn(timeoutCtx)
	if err != nil {
		st := status.Convert(err)
		if isConnectionError(st.Code()) {
			return nil, NewFreeplaneError(ErrorConnection,
				fmt.Sprintf("gRPC call %s failed: %s", methodName, st.Message()), err)
		}
		return nil, NewFreeplaneError(ErrorOperation,
			fmt.Sprintf("gRPC call %s failed (%s): %s", methodName, st.Code(), st.Message()), err)
	}

	// Check success field on responses that have one
	if err := CheckSuccess(resp); err != nil {
		return nil, err
	}

	return resp, nil
}

// CheckSuccess checks if a proto response has a Success field and if it is false.
// Returns a *FreeplaneError if the operation failed, nil otherwise.
func CheckSuccess(resp proto.Message) error {
	switch r := resp.(type) {
	case interface{ GetSuccess() bool }:
		if !r.GetSuccess() {
			if msg := getErrorMessage(resp); msg != "" {
				return NewFreeplaneError(ErrorOperation, msg, nil)
			}
			return NewFreeplaneError(ErrorOperation, "Operation failed", nil)
		}
	case *pb.CreateChildResponse:
		if r.GetNodeId() == "" {
			return NewFreeplaneError(ErrorOperation, "Failed to create child node", nil)
		}
	}
	return nil
}

// getErrorMessage extracts the error_message field from a proto response if present.
func getErrorMessage(resp proto.Message) string {
	switch r := resp.(type) {
	case interface{ GetErrorMessage() string }:
		return r.GetErrorMessage()
	case interface{ ErrorMessage() string }:
		return r.ErrorMessage()
	}
	return ""
}

// --- High-level operations ---

// CurrentMap gets the currently open/active mind map.
func (c *FreeplaneClient) CurrentMap(ctx context.Context) (*MindMap, error) {
	resp, err := c.call(ctx, "GetCurrentNode", func(ctx context.Context) (proto.Message, error) {
		return c.stub.GetCurrentNode(ctx, &pb.GetCurrentNodeRequest{})
	})
	if err != nil {
		return nil, err
	}
	r := resp.(*pb.GetCurrentNodeResponse)
	if !r.GetSuccess() {
		return nil, NewFreeplaneError(ErrorOperation, "No map currently open", nil)
	}
	return &MindMap{client: c, mapID: r.GetMapId(), nodeID: r.GetNodeId()}, nil
}

// SelectedMap gets the current mind map as a context rooted at the selected node.
func (c *FreeplaneClient) SelectedMap(ctx context.Context) (*MindMap, error) {
	return c.CurrentMap(ctx)
}

// OpenMap opens a mind map file on the Freeplane server.
func (c *FreeplaneClient) OpenMap(ctx context.Context, filePath string) (*MindMap, error) {
	_, err := c.call(ctx, "OpenMap", func(ctx context.Context) (proto.Message, error) {
		return c.stub.OpenMap(ctx, &pb.OpenMapRequest{FilePath: filePath})
	})
	if err != nil {
		return nil, err
	}
	return c.CurrentMap(ctx)
}

// GetMapToJSON exports the current mind map as JSON.
func (c *FreeplaneClient) GetMapToJSON(ctx context.Context) (string, error) {
	resp, err := c.call(ctx, "MindMapToJSON", func(ctx context.Context) (proto.Message, error) {
		return c.stub.MindMapToJSON(ctx, &pb.MindMapToJSONRequest{})
	})
	if err != nil {
		return "", err
	}
	return resp.(*pb.MindMapToJSONResponse).GetJson(), nil
}

// MindMapFromJSON imports a mind map from JSON data.
func (c *FreeplaneClient) MindMapFromJSON(ctx context.Context, jsonData string) (bool, error) {
	resp, err := c.call(ctx, "MindMapFromJSON", func(ctx context.Context) (proto.Message, error) {
		return c.stub.MindMapFromJSON(ctx, &pb.MindMapFromJSONRequest{Json: jsonData})
	})
	if err != nil {
		return false, err
	}
	return resp.(*pb.MindMapFromJSONResponse).GetSuccess(), nil
}

// Groovy executes Groovy code on the Freeplane server.
func (c *FreeplaneClient) Groovy(ctx context.Context, code string) (string, error) {
	resp, err := c.call(ctx, "Groovy", func(ctx context.Context) (proto.Message, error) {
		return c.stub.Groovy(ctx, &pb.GroovyRequest{GroovyCode: code})
	})
	if err != nil {
		return "", err
	}
	return resp.(*pb.GroovyResponse).GetResult(), nil
}

// FocusNode focuses (selects) a node in the Freeplane UI.
func (c *FreeplaneClient) FocusNode(ctx context.Context, nodeID string) (bool, error) {
	resp, err := c.call(ctx, "FocusNode", func(ctx context.Context) (proto.Message, error) {
		return c.stub.FocusNode(ctx, &pb.FocusNodeRequest{NodeId: nodeID})
	})
	if err != nil {
		return false, err
	}
	return resp.(*pb.FocusNodeResponse).GetSuccess(), nil
}

// SetStatusInfo sets the status bar info in Freeplane.
func (c *FreeplaneClient) SetStatusInfo(ctx context.Context, info string) (bool, error) {
	resp, err := c.call(ctx, "StatusInfoSet", func(ctx context.Context) (proto.Message, error) {
		return c.stub.StatusInfoSet(ctx, &pb.StatusInfoSetRequest{StatusInfo: info})
	})
	if err != nil {
		return false, err
	}
	return resp.(*pb.StatusInfoSetResponse).GetSuccess(), nil
}

// --- Raw RPC wrappers (all 27 methods) ---

// CreateChild creates a child node under a parent node.
func (c *FreeplaneClient) CreateChild(ctx context.Context, name, parentNodeID string) (*pb.CreateChildResponse, error) {
	resp, err := c.call(ctx, "CreateChild", func(ctx context.Context) (proto.Message, error) {
		return c.stub.CreateChild(ctx, &pb.CreateChildRequest{Name: name, ParentNodeId: parentNodeID})
	})
	if err != nil {
		return nil, err
	}
	return resp.(*pb.CreateChildResponse), nil
}

// DeleteChild deletes a node.
func (c *FreeplaneClient) DeleteChild(ctx context.Context, nodeID string) (bool, error) {
	resp, err := c.call(ctx, "DeleteChild", func(ctx context.Context) (proto.Message, error) {
		return c.stub.DeleteChild(ctx, &pb.DeleteChildRequest{NodeId: nodeID})
	})
	if err != nil {
		return false, err
	}
	return resp.(*pb.DeleteChildResponse).GetSuccess(), nil
}

// NodeAttributeAdd adds a custom attribute to a node.
func (c *FreeplaneClient) NodeAttributeAdd(ctx context.Context, nodeID, name, value string) (bool, error) {
	resp, err := c.call(ctx, "NodeAttributeAdd", func(ctx context.Context) (proto.Message, error) {
		return c.stub.NodeAttributeAdd(ctx, &pb.NodeAttributeAddRequest{NodeId: nodeID, AttributeName: name, AttributeValue: value})
	})
	if err != nil {
		return false, err
	}
	return resp.(*pb.NodeAttributeAddResponse).GetSuccess(), nil
}

// NodeLinkSet sets a link on a node.
func (c *FreeplaneClient) NodeLinkSet(ctx context.Context, nodeID, link string) (bool, error) {
	resp, err := c.call(ctx, "NodeLinkSet", func(ctx context.Context) (proto.Message, error) {
		return c.stub.NodeLinkSet(ctx, &pb.NodeLinkSetRequest{NodeId: nodeID, Link: link})
	})
	if err != nil {
		return false, err
	}
	return resp.(*pb.NodeLinkSetResponse).GetSuccess(), nil
}

// NodeDetailsSet sets node details.
func (c *FreeplaneClient) NodeDetailsSet(ctx context.Context, nodeID, details string) (bool, error) {
	resp, err := c.call(ctx, "NodeDetailsSet", func(ctx context.Context) (proto.Message, error) {
		return c.stub.NodeDetailsSet(ctx, &pb.NodeDetailsSetRequest{NodeId: nodeID, Details: details})
	})
	if err != nil {
		return false, err
	}
	return resp.(*pb.NodeDetailsSetResponse).GetSuccess(), nil
}

// NodeNoteSet sets a note on a node.
func (c *FreeplaneClient) NodeNoteSet(ctx context.Context, nodeID, note string) (bool, error) {
	resp, err := c.call(ctx, "NodeNoteSet", func(ctx context.Context) (proto.Message, error) {
		return c.stub.NodeNoteSet(ctx, &pb.NodeNoteSetRequest{NodeId: nodeID, Note: note})
	})
	if err != nil {
		return false, err
	}
	return resp.(*pb.NodeNoteSetResponse).GetSuccess(), nil
}

// NodeTagSet sets tags on a node (replaces existing).
func (c *FreeplaneClient) NodeTagSet(ctx context.Context, nodeID string, tags []string) (bool, error) {
	resp, err := c.call(ctx, "NodeTagSet", func(ctx context.Context) (proto.Message, error) {
		return c.stub.NodeTagSet(ctx, &pb.NodeTagSetRequest{NodeId: nodeID, Tags: tags})
	})
	if err != nil {
		return false, err
	}
	return resp.(*pb.NodeTagSetResponse).GetSuccess(), nil
}

// NodeTagAdd adds tags to a node (does not remove existing).
func (c *FreeplaneClient) NodeTagAdd(ctx context.Context, nodeID string, tags []string) (bool, error) {
	resp, err := c.call(ctx, "NodeTagAdd", func(ctx context.Context) (proto.Message, error) {
		return c.stub.NodeTagAdd(ctx, &pb.NodeTagAddRequest{NodeId: nodeID, Tags: tags})
	})
	if err != nil {
		return false, err
	}
	return resp.(*pb.NodeTagAddResponse).GetSuccess(), nil
}

// NodeConnect creates a connection between two nodes.
func (c *FreeplaneClient) NodeConnect(ctx context.Context, sourceNodeID, targetNodeID, relationship string) (bool, error) {
	resp, err := c.call(ctx, "NodeConnect", func(ctx context.Context) (proto.Message, error) {
		return c.stub.NodeConnect(ctx, &pb.NodeConnectRequest{SourceNodeId: sourceNodeID, TargetNodeId: targetNodeID, Relationship: relationship})
	})
	if err != nil {
		return false, err
	}
	return resp.(*pb.NodeConnectResponse).GetSuccess(), nil
}

// NodeAddIcon adds an icon to a node.
func (c *FreeplaneClient) NodeAddIcon(ctx context.Context, nodeID, iconName string) (bool, error) {
	resp, err := c.call(ctx, "NodeAddIcon", func(ctx context.Context) (proto.Message, error) {
		return c.stub.NodeAddIcon(ctx, &pb.NodeAddIconRequest{NodeId: nodeID, IconName: iconName})
	})
	if err != nil {
		return false, err
	}
	return resp.(*pb.NodeAddIconResponse).GetSuccess(), nil
}

// NodeColorSet sets the foreground color of a node.
func (c *FreeplaneClient) NodeColorSet(ctx context.Context, nodeID string, r, g, b, a int32) (bool, error) {
	resp, err := c.call(ctx, "NodeColorSet", func(ctx context.Context) (proto.Message, error) {
		return c.stub.NodeColorSet(ctx, &pb.NodeColorSetRequest{NodeId: nodeID, Red: r, Green: g, Blue: b, Alpha: a})
	})
	if err != nil {
		return false, err
	}
	return resp.(*pb.NodeColorSetResponse).GetSuccess(), nil
}

// NodeBackgroundColorSet sets the background color of a node.
func (c *FreeplaneClient) NodeBackgroundColorSet(ctx context.Context, nodeID string, r, g, b, a int32) (bool, error) {
	resp, err := c.call(ctx, "NodeBackgroundColorSet", func(ctx context.Context) (proto.Message, error) {
		return c.stub.NodeBackgroundColorSet(ctx, &pb.NodeBackgroundColorSetRequest{NodeId: nodeID, Red: r, Green: g, Blue: b, Alpha: a})
	})
	if err != nil {
		return false, err
	}
	return resp.(*pb.NodeBackgroundColorSetResponse).GetSuccess(), nil
}

// TextFSM processes text with a TextFSM template.
func (c *FreeplaneClient) TextFSM(ctx context.Context, json string) (bool, error) {
	resp, err := c.call(ctx, "TextFSM", func(ctx context.Context) (proto.Message, error) {
		return c.stub.TextFSM(ctx, &pb.TextFSMRequest{Json: json})
	})
	if err != nil {
		return false, err
	}
	return resp.(*pb.TextFSMResponse).GetSuccess(), nil
}

// GetNodeText gets the text of a node.
func (c *FreeplaneClient) GetNodeText(ctx context.Context, nodeID string) (string, error) {
	resp, err := c.call(ctx, "GetNodeText", func(ctx context.Context) (proto.Message, error) {
		return c.stub.GetNodeText(ctx, &pb.GetNodeTextRequest{NodeId: nodeID})
	})
	if err != nil {
		return "", err
	}
	return resp.(*pb.GetNodeTextResponse).GetText(), nil
}

// GetParentNode gets the parent of a node.
func (c *FreeplaneClient) GetParentNode(ctx context.Context, nodeID string) (*pb.GetParentNodeResponse, error) {
	resp, err := c.call(ctx, "GetParentNode", func(ctx context.Context) (proto.Message, error) {
		return c.stub.GetParentNode(ctx, &pb.GetParentNodeRequest{NodeId: nodeID})
	})
	if err != nil {
		return nil, err
	}
	return resp.(*pb.GetParentNodeResponse), nil
}

// ListChildNodes gets the direct children of a node.
func (c *FreeplaneClient) ListChildNodes(ctx context.Context, nodeID string) ([]*pb.ChildNodeInfo, error) {
	resp, err := c.call(ctx, "ListChildNodes", func(ctx context.Context) (proto.Message, error) {
		return c.stub.ListChildNodes(ctx, &pb.ListChildNodesRequest{NodeId: nodeID})
	})
	if err != nil {
		return nil, err
	}
	return resp.(*pb.ListChildNodesResponse).GetChildren(), nil
}

// GetNodeNote gets the note of a node.
func (c *FreeplaneClient) GetNodeNote(ctx context.Context, nodeID string) (string, bool, error) {
	resp, err := c.call(ctx, "GetNodeNote", func(ctx context.Context) (proto.Message, error) {
		return c.stub.GetNodeNote(ctx, &pb.GetNodeNoteRequest{NodeId: nodeID})
	})
	if err != nil {
		return "", false, err
	}
	r := resp.(*pb.GetNodeNoteResponse)
	return r.GetNote(), r.GetHasNote(), nil
}

// GetNodeLink gets the link of a node.
func (c *FreeplaneClient) GetNodeLink(ctx context.Context, nodeID string) (string, bool, error) {
	resp, err := c.call(ctx, "GetNodeLink", func(ctx context.Context) (proto.Message, error) {
		return c.stub.GetNodeLink(ctx, &pb.GetNodeLinkRequest{NodeId: nodeID})
	})
	if err != nil {
		return "", false, err
	}
	r := resp.(*pb.GetNodeLinkResponse)
	return r.GetLink(), r.GetHasLink(), nil
}

// SetNodeText sets the text of a node.
func (c *FreeplaneClient) SetNodeText(ctx context.Context, nodeID, text string) error {
	_, err := c.call(ctx, "SetNodeText", func(ctx context.Context) (proto.Message, error) {
		return c.stub.SetNodeText(ctx, &pb.SetNodeTextRequest{NodeId: nodeID, Text: text})
	})
	return err
}

// MoveNode moves a node under a new parent.
func (c *FreeplaneClient) MoveNode(ctx context.Context, nodeID, newParentNodeID string) (bool, error) {
	resp, err := c.call(ctx, "MoveNode", func(ctx context.Context) (proto.Message, error) {
		return c.stub.MoveNode(ctx, &pb.MoveNodeRequest{NodeId: nodeID, NewParentNodeId: newParentNodeID})
	})
	if err != nil {
		return false, err
	}
	return resp.(*pb.MoveNodeResponse).GetSuccess(), nil
}

// --- Mock helper ---

// NewMockFreeplaneClient creates a mock FreeplaneClientInterface using uber-go/mock.
// The generated mock type MockFreeplaneClientInterface is created by mockgen.
// To generate: mockgen -package=freeplane -destination=internal/mock/mock_freeplane.go github.com/metacoma/freeplane_plugin_grpc/grpc/golang FreeplaneClientInterface
