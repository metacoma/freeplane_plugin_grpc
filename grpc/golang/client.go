package freeplane_grpc

import (
	"context"
	"fmt"
	"os"

	"google.golang.org/grpc"
	"google.golang.org/grpc/credentials/insecure"
	"google.golang.org/grpc/status"

	"github.com/metacoma/freeplane_plugin_grpc/grpc/golang/freeplane"
)

// FreeplaneClient is the main entry point for interacting with Freeplane via gRPC.
type FreeplaneClient struct {
	host string
	port int
	conn *grpc.ClientConn
	stub freeplane.FreeplaneClient
}

// NewClient creates a new FreeplaneClient with the given host and port.
// If host is empty, it defaults to "127.0.0.1".
// If port is 0, it defaults to 50051.
// Environment variables FREEPLANE_HOST and FREEPLANE_PORT can override defaults.
func NewClient(host string, port int) (*FreeplaneClient, error) {
	// Apply environment variable overrides
	if h := os.Getenv("FREEPLANE_HOST"); h != "" {
		host = h
	}
	if p := os.Getenv("FREEPLANE_PORT"); p != "" {
		if _, err := fmt.Sscanf(p, "%d", &port); err != nil {
			return nil, NewConnectionError("invalid FREEPLANE_PORT: %s", p)
		}
	}

	if host == "" {
		host = "127.0.0.1"
	}
	if port == 0 {
		port = 50051
	}

	return &FreeplaneClient{
		host: host,
		port: port,
	}, nil
}

// Connect opens a gRPC channel to the server using grpc.NewClient and insecure.NewCredentials().
func (c *FreeplaneClient) Connect() error {
	addr := fmt.Sprintf("%s:%d", c.host, c.port)
	conn, err := grpc.NewClient(addr, grpc.WithTransportCredentials(insecure.NewCredentials()))
	if err != nil {
		return NewConnectionError("failed to create client connection to %s: %v", addr, err)
	}
	c.conn = conn
	c.stub = freeplane.NewFreeplaneClient(conn)
	return nil
}

// Close closes the gRPC connection.
func (c *FreeplaneClient) Close() error {
	if c.conn != nil {
		err := c.conn.Close()
		c.conn = nil
		c.stub = nil
		return err
	}
	return nil
}

// call is an internal helper that wraps a gRPC call and maps errors to domain errors.
func (c *FreeplaneClient) call(ctx context.Context, fn func(context.Context) (interface{}, error)) (interface{}, error) {
	resp, err := fn(ctx)
	if err != nil {
		if s, ok := status.FromError(err); ok {
			return nil, statusToError(s)
		}
		if isConnectionError(err) {
			return nil, NewConnectionError("gRPC call failed: %v", err)
		}
		return nil, NewOperationError("gRPC call failed: %v", err)
	}
	return resp, nil
}

// checkSuccess checks the success field of a response and returns an error if false.
func checkSuccess(resp interface{}) error {
	switch r := resp.(type) {
	case *freeplane.DeleteChildResponse:
		if !r.Success {
			return NewOperationError("operation failed")
		}
	case *freeplane.NodeAttributeAddResponse:
		if !r.Success {
			return NewOperationError("operation failed")
		}
	case *freeplane.NodeLinkSetResponse:
		if !r.Success {
			return NewOperationError("operation failed")
		}
	case *freeplane.NodeDetailsSetResponse:
		if !r.Success {
			return NewOperationError("operation failed")
		}
	case *freeplane.NodeNoteSetResponse:
		if !r.Success {
			return NewOperationError("operation failed")
		}
	case *freeplane.NodeTagSetResponse:
		if !r.Success {
			return NewOperationError("operation failed")
		}
	case *freeplane.NodeTagAddResponse:
		if !r.Success {
			return NewOperationError("operation failed")
		}
	case *freeplane.NodeConnectResponse:
		if !r.Success {
			return NewOperationError("operation failed")
		}
	case *freeplane.NodeAddIconResponse:
		if !r.Success {
			return NewOperationError("operation failed")
		}
	case *freeplane.NodeColorSetResponse:
		if !r.Success {
			return NewOperationError("operation failed")
		}
	case *freeplane.NodeBackgroundColorSetResponse:
		if !r.Success {
			return NewOperationError("operation failed")
		}
	case *freeplane.StatusInfoSetResponse:
		if !r.Success {
			return NewOperationError("operation failed")
		}
	case *freeplane.TextFSMResponse:
		if !r.Success {
			return NewOperationError("operation failed")
		}
	case *freeplane.MindMapFromJSONResponse:
		if !r.Success {
			return NewOperationError("operation failed")
		}
	case *freeplane.OpenMapResponse:
		if !r.Success {
			return NewOperationError("operation failed")
		}
	case *freeplane.FocusNodeResponse:
		if !r.Success {
			return NewOperationError("operation failed")
		}
	case *freeplane.SetNodeTextResponse:
		if !r.Success {
			msg := r.ErrorMessage
			if msg == "" {
				msg = "operation failed"
			}
			return NewOperationError("%s", msg)
		}
	case *freeplane.MoveNodeResponse:
		if !r.Success {
			msg := r.ErrorMessage
			if msg == "" {
				msg = "operation failed"
			}
			return NewOperationError("%s", msg)
		}
	}
	return nil
}

// ==================== 27 RPC Wrappers ====================

// CreateChild creates a child node under the specified parent.
func (c *FreeplaneClient) CreateChild(ctx context.Context, name, parentNodeID string) (*freeplane.CreateChildResponse, error) {
	resp, err := c.call(ctx, func(ctx context.Context) (interface{}, error) {
		return c.stub.CreateChild(ctx, &freeplane.CreateChildRequest{
			Name:         name,
			ParentNodeId: parentNodeID,
		})
	})
	if err != nil {
		return nil, err
	}
	return resp.(*freeplane.CreateChildResponse), nil
}

// DeleteChild deletes a node by its ID.
func (c *FreeplaneClient) DeleteChild(ctx context.Context, nodeID string) (*freeplane.DeleteChildResponse, error) {
	resp, err := c.call(ctx, func(ctx context.Context) (interface{}, error) {
		return c.stub.DeleteChild(ctx, &freeplane.DeleteChildRequest{
			NodeId: nodeID,
		})
	})
	if err != nil {
		return nil, err
	}
	if err = checkSuccess(resp); err != nil {
		return nil, err
	}
	return resp.(*freeplane.DeleteChildResponse), nil
}

// NodeAttributeAdd adds an attribute to a node.
func (c *FreeplaneClient) NodeAttributeAdd(ctx context.Context, nodeID, attributeName, attributeValue string) (*freeplane.NodeAttributeAddResponse, error) {
	resp, err := c.call(ctx, func(ctx context.Context) (interface{}, error) {
		return c.stub.NodeAttributeAdd(ctx, &freeplane.NodeAttributeAddRequest{
			NodeId:         nodeID,
			AttributeName:  attributeName,
			AttributeValue: attributeValue,
		})
	})
	if err != nil {
		return nil, err
	}
	if err = checkSuccess(resp); err != nil {
		return nil, err
	}
	return resp.(*freeplane.NodeAttributeAddResponse), nil
}

// NodeLinkSet sets a link on a node.
func (c *FreeplaneClient) NodeLinkSet(ctx context.Context, nodeID, link string) (*freeplane.NodeLinkSetResponse, error) {
	resp, err := c.call(ctx, func(ctx context.Context) (interface{}, error) {
		return c.stub.NodeLinkSet(ctx, &freeplane.NodeLinkSetRequest{
			NodeId: nodeID,
			Link:   link,
		})
	})
	if err != nil {
		return nil, err
	}
	if err = checkSuccess(resp); err != nil {
		return nil, err
	}
	return resp.(*freeplane.NodeLinkSetResponse), nil
}

// NodeDetailsSet sets the details (body) of a node.
func (c *FreeplaneClient) NodeDetailsSet(ctx context.Context, nodeID, details string) (*freeplane.NodeDetailsSetResponse, error) {
	resp, err := c.call(ctx, func(ctx context.Context) (interface{}, error) {
		return c.stub.NodeDetailsSet(ctx, &freeplane.NodeDetailsSetRequest{
			NodeId:  nodeID,
			Details: details,
		})
	})
	if err != nil {
		return nil, err
	}
	if err = checkSuccess(resp); err != nil {
		return nil, err
	}
	return resp.(*freeplane.NodeDetailsSetResponse), nil
}

// NodeNoteSet sets the note of a node.
func (c *FreeplaneClient) NodeNoteSet(ctx context.Context, nodeID, note string) (*freeplane.NodeNoteSetResponse, error) {
	resp, err := c.call(ctx, func(ctx context.Context) (interface{}, error) {
		return c.stub.NodeNoteSet(ctx, &freeplane.NodeNoteSetRequest{
			NodeId: nodeID,
			Note:   note,
		})
	})
	if err != nil {
		return nil, err
	}
	if err = checkSuccess(resp); err != nil {
		return nil, err
	}
	return resp.(*freeplane.NodeNoteSetResponse), nil
}

// NodeTagSet sets tags on a node (replaces existing tags).
func (c *FreeplaneClient) NodeTagSet(ctx context.Context, nodeID string, tags []string) (*freeplane.NodeTagSetResponse, error) {
	resp, err := c.call(ctx, func(ctx context.Context) (interface{}, error) {
		return c.stub.NodeTagSet(ctx, &freeplane.NodeTagSetRequest{
			NodeId: nodeID,
			Tags:   tags,
		})
	})
	if err != nil {
		return nil, err
	}
	if err = checkSuccess(resp); err != nil {
		return nil, err
	}
	return resp.(*freeplane.NodeTagSetResponse), nil
}

// NodeTagAdd adds tags to a node (appends to existing tags).
func (c *FreeplaneClient) NodeTagAdd(ctx context.Context, nodeID string, tags []string) (*freeplane.NodeTagAddResponse, error) {
	resp, err := c.call(ctx, func(ctx context.Context) (interface{}, error) {
		return c.stub.NodeTagAdd(ctx, &freeplane.NodeTagAddRequest{
			NodeId: nodeID,
			Tags:   tags,
		})
	})
	if err != nil {
		return nil, err
	}
	if err = checkSuccess(resp); err != nil {
		return nil, err
	}
	return resp.(*freeplane.NodeTagAddResponse), nil
}

// NodeConnect creates a connection between two nodes.
func (c *FreeplaneClient) NodeConnect(ctx context.Context, sourceNodeID, targetNodeID, relationship string) (*freeplane.NodeConnectResponse, error) {
	resp, err := c.call(ctx, func(ctx context.Context) (interface{}, error) {
		return c.stub.NodeConnect(ctx, &freeplane.NodeConnectRequest{
			SourceNodeId: sourceNodeID,
			TargetNodeId: targetNodeID,
			Relationship: relationship,
		})
	})
	if err != nil {
		return nil, err
	}
	if err = checkSuccess(resp); err != nil {
		return nil, err
	}
	return resp.(*freeplane.NodeConnectResponse), nil
}

// NodeAddIcon adds an icon to a node.
func (c *FreeplaneClient) NodeAddIcon(ctx context.Context, nodeID, iconName string) (*freeplane.NodeAddIconResponse, error) {
	resp, err := c.call(ctx, func(ctx context.Context) (interface{}, error) {
		return c.stub.NodeAddIcon(ctx, &freeplane.NodeAddIconRequest{
			NodeId:   nodeID,
			IconName: iconName,
		})
	})
	if err != nil {
		return nil, err
	}
	if err = checkSuccess(resp); err != nil {
		return nil, err
	}
	return resp.(*freeplane.NodeAddIconResponse), nil
}

// Groovy executes Groovy code on the Freeplane server.
func (c *FreeplaneClient) Groovy(ctx context.Context, groovyCode string) (*freeplane.GroovyResponse, error) {
	resp, err := c.call(ctx, func(ctx context.Context) (interface{}, error) {
		return c.stub.Groovy(ctx, &freeplane.GroovyRequest{
			GroovyCode: groovyCode,
		})
	})
	if err != nil {
		return nil, err
	}
	return resp.(*freeplane.GroovyResponse), nil
}

// NodeColorSet sets the text color of a node.
func (c *FreeplaneClient) NodeColorSet(ctx context.Context, nodeID string, red, green, blue, alpha int32) (*freeplane.NodeColorSetResponse, error) {
	resp, err := c.call(ctx, func(ctx context.Context) (interface{}, error) {
		return c.stub.NodeColorSet(ctx, &freeplane.NodeColorSetRequest{
			NodeId: nodeID,
			Red:    red,
			Green:  green,
			Blue:   blue,
			Alpha:  alpha,
		})
	})
	if err != nil {
		return nil, err
	}
	if err = checkSuccess(resp); err != nil {
		return nil, err
	}
	return resp.(*freeplane.NodeColorSetResponse), nil
}

// NodeBackgroundColorSet sets the background color of a node.
func (c *FreeplaneClient) NodeBackgroundColorSet(ctx context.Context, nodeID string, red, green, blue, alpha int32) (*freeplane.NodeBackgroundColorSetResponse, error) {
	resp, err := c.call(ctx, func(ctx context.Context) (interface{}, error) {
		return c.stub.NodeBackgroundColorSet(ctx, &freeplane.NodeBackgroundColorSetRequest{
			NodeId: nodeID,
			Red:    red,
			Green:  green,
			Blue:   blue,
			Alpha:  alpha,
		})
	})
	if err != nil {
		return nil, err
	}
	if err = checkSuccess(resp); err != nil {
		return nil, err
	}
	return resp.(*freeplane.NodeBackgroundColorSetResponse), nil
}

// StatusInfoSet sets the status bar info in Freeplane.
func (c *FreeplaneClient) StatusInfoSet(ctx context.Context, statusInfo string) (*freeplane.StatusInfoSetResponse, error) {
	resp, err := c.call(ctx, func(ctx context.Context) (interface{}, error) {
		return c.stub.StatusInfoSet(ctx, &freeplane.StatusInfoSetRequest{
			StatusInfo: statusInfo,
		})
	})
	if err != nil {
		return nil, err
	}
	if err = checkSuccess(resp); err != nil {
		return nil, err
	}
	return resp.(*freeplane.StatusInfoSetResponse), nil
}

// TextFSM executes a TextFSM template.
func (c *FreeplaneClient) TextFSM(ctx context.Context, json string) (*freeplane.TextFSMResponse, error) {
	resp, err := c.call(ctx, func(ctx context.Context) (interface{}, error) {
		return c.stub.TextFSM(ctx, &freeplane.TextFSMRequest{
			Json: json,
		})
	})
	if err != nil {
		return nil, err
	}
	if err = checkSuccess(resp); err != nil {
		return nil, err
	}
	return resp.(*freeplane.TextFSMResponse), nil
}

// MindMapFromJSON imports a mind map from JSON.
func (c *FreeplaneClient) MindMapFromJSON(ctx context.Context, json string) (*freeplane.MindMapFromJSONResponse, error) {
	resp, err := c.call(ctx, func(ctx context.Context) (interface{}, error) {
		return c.stub.MindMapFromJSON(ctx, &freeplane.MindMapFromJSONRequest{
			Json: json,
		})
	})
	if err != nil {
		return nil, err
	}
	if err = checkSuccess(resp); err != nil {
		return nil, err
	}
	return resp.(*freeplane.MindMapFromJSONResponse), nil
}

// MindMapToJSON exports the current mind map to JSON.
func (c *FreeplaneClient) MindMapToJSON(ctx context.Context) (*freeplane.MindMapToJSONResponse, error) {
	resp, err := c.call(ctx, func(ctx context.Context) (interface{}, error) {
		return c.stub.MindMapToJSON(ctx, &freeplane.MindMapToJSONRequest{})
	})
	if err != nil {
		return nil, err
	}
	return resp.(*freeplane.MindMapToJSONResponse), nil
}

// GetCurrentNode gets the current node.
func (c *FreeplaneClient) GetCurrentNode(ctx context.Context) (*freeplane.GetCurrentNodeResponse, error) {
	resp, err := c.call(ctx, func(ctx context.Context) (interface{}, error) {
		return c.stub.GetCurrentNode(ctx, &freeplane.GetCurrentNodeRequest{})
	})
	if err != nil {
		return nil, err
	}
	return resp.(*freeplane.GetCurrentNodeResponse), nil
}

// OpenMap opens a mind map file.
func (c *FreeplaneClient) OpenMap(ctx context.Context, filePath string) (*freeplane.OpenMapResponse, error) {
	resp, err := c.call(ctx, func(ctx context.Context) (interface{}, error) {
		return c.stub.OpenMap(ctx, &freeplane.OpenMapRequest{
			FilePath: filePath,
		})
	})
	if err != nil {
		return nil, err
	}
	if err = checkSuccess(resp); err != nil {
		return nil, err
	}
	return resp.(*freeplane.OpenMapResponse), nil
}

// FocusNode focuses (selects) a node.
func (c *FreeplaneClient) FocusNode(ctx context.Context, nodeID string) (*freeplane.FocusNodeResponse, error) {
	resp, err := c.call(ctx, func(ctx context.Context) (interface{}, error) {
		return c.stub.FocusNode(ctx, &freeplane.FocusNodeRequest{
			NodeId: nodeID,
		})
	})
	if err != nil {
		return nil, err
	}
	if err = checkSuccess(resp); err != nil {
		return nil, err
	}
	return resp.(*freeplane.FocusNodeResponse), nil
}

// GetNodeText gets the text of a node.
func (c *FreeplaneClient) GetNodeText(ctx context.Context, nodeID string) (*freeplane.GetNodeTextResponse, error) {
	resp, err := c.call(ctx, func(ctx context.Context) (interface{}, error) {
		return c.stub.GetNodeText(ctx, &freeplane.GetNodeTextRequest{
			NodeId: nodeID,
		})
	})
	if err != nil {
		return nil, err
	}
	return resp.(*freeplane.GetNodeTextResponse), nil
}

// GetParentNode gets the parent of a node.
func (c *FreeplaneClient) GetParentNode(ctx context.Context, nodeID string) (*freeplane.GetParentNodeResponse, error) {
	resp, err := c.call(ctx, func(ctx context.Context) (interface{}, error) {
		return c.stub.GetParentNode(ctx, &freeplane.GetParentNodeRequest{
			NodeId: nodeID,
		})
	})
	if err != nil {
		return nil, err
	}
	return resp.(*freeplane.GetParentNodeResponse), nil
}

// ListChildNodes lists the children of a node.
func (c *FreeplaneClient) ListChildNodes(ctx context.Context, nodeID string) (*freeplane.ListChildNodesResponse, error) {
	resp, err := c.call(ctx, func(ctx context.Context) (interface{}, error) {
		return c.stub.ListChildNodes(ctx, &freeplane.ListChildNodesRequest{
			NodeId: nodeID,
		})
	})
	if err != nil {
		return nil, err
	}
	return resp.(*freeplane.ListChildNodesResponse), nil
}

// GetNodeNote gets the note of a node.
func (c *FreeplaneClient) GetNodeNote(ctx context.Context, nodeID string) (*freeplane.GetNodeNoteResponse, error) {
	resp, err := c.call(ctx, func(ctx context.Context) (interface{}, error) {
		return c.stub.GetNodeNote(ctx, &freeplane.GetNodeNoteRequest{
			NodeId: nodeID,
		})
	})
	if err != nil {
		return nil, err
	}
	return resp.(*freeplane.GetNodeNoteResponse), nil
}

// GetNodeLink gets the link of a node.
func (c *FreeplaneClient) GetNodeLink(ctx context.Context, nodeID string) (*freeplane.GetNodeLinkResponse, error) {
	resp, err := c.call(ctx, func(ctx context.Context) (interface{}, error) {
		return c.stub.GetNodeLink(ctx, &freeplane.GetNodeLinkRequest{
			NodeId: nodeID,
		})
	})
	if err != nil {
		return nil, err
	}
	return resp.(*freeplane.GetNodeLinkResponse), nil
}

// SetNodeText sets the text of a node.
func (c *FreeplaneClient) SetNodeText(ctx context.Context, nodeID, text string) (*freeplane.SetNodeTextResponse, error) {
	resp, err := c.call(ctx, func(ctx context.Context) (interface{}, error) {
		return c.stub.SetNodeText(ctx, &freeplane.SetNodeTextRequest{
			NodeId: nodeID,
			Text:   text,
		})
	})
	if err != nil {
		return nil, err
	}
	if err = checkSuccess(resp); err != nil {
		return nil, err
	}
	return resp.(*freeplane.SetNodeTextResponse), nil
}

// MoveNode moves a node to a new parent.
func (c *FreeplaneClient) MoveNode(ctx context.Context, nodeID, newParentNodeID string) (*freeplane.MoveNodeResponse, error) {
	resp, err := c.call(ctx, func(ctx context.Context) (interface{}, error) {
		return c.stub.MoveNode(ctx, &freeplane.MoveNodeRequest{
			NodeId:          nodeID,
			NewParentNodeId: newParentNodeID,
		})
	})
	if err != nil {
		return nil, err
	}
	if err = checkSuccess(resp); err != nil {
		return nil, err
	}
	return resp.(*freeplane.MoveNodeResponse), nil
}

// ==================== High-Level Operations ====================

// CurrentMap gets the currently open mind map.
// The returned MindMap carries the provided context for all subsequent operations.
func (c *FreeplaneClient) CurrentMap(ctx context.Context) (*MindMap, error) {
	resp, err := c.GetCurrentNode(ctx)
	if err != nil {
		return nil, err
	}
	if !resp.Success {
		return nil, NewOperationError("no map currently open")
	}
	mm := &MindMap{
		client: c,
		mapID:  resp.MapId,
		nodeID: resp.NodeId,
	}
	return mm.WithContext(ctx), nil
}

// SelectedMap gets the current mind map rooted at the selected node.
func (c *FreeplaneClient) SelectedMap(ctx context.Context) (*MindMap, error) {
	return c.CurrentMap(ctx)
}

// OpenMapAndGetCurrent opens a mind map file and returns the current map.
func (c *FreeplaneClient) OpenMapAndGetCurrent(ctx context.Context, filePath string) (*MindMap, error) {
	if _, err := c.OpenMap(ctx, filePath); err != nil {
		return nil, err
	}
	return c.CurrentMap(ctx)
}

// GetMapToJSON exports the current mind map as JSON.
func (c *FreeplaneClient) GetMapToJSON(ctx context.Context) (string, error) {
	resp, err := c.MindMapToJSON(ctx)
	if err != nil {
		return "", err
	}
	return resp.Json, nil
}

// MindMapFromJSONData imports a mind map from JSON data.
func (c *FreeplaneClient) MindMapFromJSONData(ctx context.Context, jsonData string) error {
	_, err := c.MindMapFromJSON(ctx, jsonData)
	return err
}

// GroovyCode executes Groovy code on the Freeplane server and returns the result.
func (c *FreeplaneClient) GroovyCode(ctx context.Context, code string) (string, error) {
	resp, err := c.Groovy(ctx, code)
	if err != nil {
		return "", err
	}
	return resp.Result, nil
}

// FocusNodeByID focuses (selects) a node by its ID.
func (c *FreeplaneClient) FocusNodeByID(ctx context.Context, nodeID string) error {
	_, err := c.FocusNode(ctx, nodeID)
	return err
}

// SetStatusInfoText sets the status bar info in Freeplane.
func (c *FreeplaneClient) SetStatusInfoText(ctx context.Context, info string) error {
	_, err := c.StatusInfoSet(ctx, info)
	return err
}
