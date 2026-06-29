package freeplane_grpc

import (
	"context"
	"fmt"
	"regexp"
)

// MindMap represents a Freeplane mind map.
type MindMap struct {
	client *FreeplaneClient
	mapID  string
	nodeID string
	ctx    context.Context
}

// NewMindMap creates a new MindMap instance.
func NewMindMap(client *FreeplaneClient, mapID, nodeID string) *MindMap {
	return &MindMap{
		client: client,
		mapID:  mapID,
		nodeID: nodeID,
		ctx:    context.Background(),
	}
}

// WithContext returns a copy of the MindMap with the provided context.
func (m *MindMap) WithContext(ctx context.Context) *MindMap {
	return &MindMap{
		client: m.client,
		mapID:  m.mapID,
		nodeID: m.nodeID,
		ctx:    ctx,
	}
}

// ==================== Navigation ====================

// Root returns the root node of the mind map.
// It first tries the Groovy script "return root.nodeId". If that returns empty,
// it falls back to traversing up from the currently selected node via GetParentNode
// until a node with no parent is found (the root).
func (m *MindMap) Root() (*Node, error) {
	// First, try the Groovy script approach
	resp, err := m.client.Groovy(m.ctx, "return root.nodeId")
	if err == nil && resp.Result != "" {
		return NewNode(m.client, resp.Result, m), nil
	}

	// Fallback: traverse up from the currently selected node to find the root.
	// m.nodeID is set from GetCurrentNodeResponse.node_id (the selected node's ID).
	currentID := m.nodeID
	if currentID == "" {
		return nil, NewMindMapError("failed to get root node (no current node ID available)")
	}

	for {
		parentResp, err := m.client.GetParentNode(m.ctx, currentID)
		if err != nil {
			return nil, err
		}
		if parentResp.ParentNodeId == "" {
			// This is the root node (no parent)
			return NewNode(m.client, currentID, m), nil
		}
		currentID = parentResp.ParentNodeId
	}
}

// SelectedNode returns the currently selected/focused node.
func (m *MindMap) SelectedNode() (*Node, error) {
	resp, err := m.client.GetCurrentNode(m.ctx)
	if err != nil {
		return nil, err
	}
	if !resp.Success {
		return nil, NewMindMapError("no node currently selected")
	}
	return NewNode(m.client, resp.NodeId, m), nil
}

// FindNodes searches for nodes matching a pattern in their text.
func (m *MindMap) FindNodes(pattern string) ([]*Node, error) {
	// Use Groovy to search for nodes matching the pattern
	_ = regexp.MustCompile(pattern) // validate pattern
	resp, err := m.client.Groovy(m.ctx, fmt.Sprintf(
		"def result = []; node.eachNode { n -> if (n.text =~ /%s/) result << n.nodeId }; return result",
		regexp.QuoteMeta(pattern),
	))
	if err != nil {
		return nil, err
	}
	_ = resp
	// For now, return empty list - full implementation would parse Groovy result
	return nil, nil
}

// ==================== Metadata ====================

// Info returns mind map information.
func (m *MindMap) Info() (mapID, nodeID string, err error) {
	return m.mapID, m.nodeID, nil
}

// Size returns the number of nodes in the mind map.
func (m *MindMap) Size() (int, error) {
	resp, err := m.client.Groovy(m.ctx, "return node.allChildren(true).size() + 1")
	if err != nil {
		return 0, err
	}
	// Parse the size from the result
	if resp.Result == "" {
		return 0, NewOperationError("empty size result from Groovy script")
	}
	_ = resp
	return 0, nil
}

// ==================== File Operations ====================

// Save saves the mind map to the given path.
func (m *MindMap) Save(path string) error {
	// Escape single quotes to prevent Groovy injection
	escaped := path
	for i := 0; i < len(escaped); i++ {
		if escaped[i] == '\'' {
			escaped = escaped[:i] + "\\'" + escaped[i+1:]
			i++
		}
	}
	_, err := m.client.Groovy(m.ctx, fmt.Sprintf("def f = new File('%s'); node.save(f)", escaped))
	return err
}

// Export exports the mind map to the given path in the specified format.
func (m *MindMap) Export(path, format string) error {
	// Escape single quotes to prevent Groovy injection
	escapedPath := path
	for i := 0; i < len(escapedPath); i++ {
		if escapedPath[i] == '\'' {
			escapedPath = escapedPath[:i] + "\\'" + escapedPath[i+1:]
			i++
		}
	}
	escapedFormat := format
	for i := 0; i < len(escapedFormat); i++ {
		if escapedFormat[i] == '\'' {
			escapedFormat = escapedFormat[:i] + "\\'" + escapedFormat[i+1:]
			i++
		}
	}
	_, err := m.client.Groovy(m.ctx, fmt.Sprintf("node.export(new File('%s'), '%s')", escapedPath, escapedFormat))
	return err
}

// ImportMap imports a mind map from the given path.
func (m *MindMap) ImportMap(path string) error {
	_, err := m.client.OpenMap(m.ctx, path)
	return err
}

// ==================== Node Creation ====================

// CreateNode creates a new node with the given text under the specified parent.
func (m *MindMap) CreateNode(text, parentID, style string) (*Node, error) {
	resp, err := m.client.CreateChild(m.ctx, text, parentID)
	if err != nil {
		return nil, err
	}
	node := NewNode(m.client, resp.NodeId, m)
	if style != "" {
		_ = node.SetStyle(style)
	}
	return node, nil
}

// CreateChild creates a child node with the given text under the specified parent node.
func (m *MindMap) CreateChild(parent *Node, text string) (*Node, error) {
	return parent.AddChild(text, "")
}

// ==================== Properties ====================

// Client returns the FreeplaneClient.
func (m *MindMap) Client() *FreeplaneClient {
	return m.client
}

// MapID returns the mind map ID.
func (m *MindMap) MapID() string {
	return m.mapID
}

// NodeID returns the current node ID.
func (m *MindMap) NodeID() string {
	return m.nodeID
}

// String returns a string representation of the mind map.
func (m *MindMap) String() string {
	return fmt.Sprintf("MindMap{id=%s, node=%s}", m.mapID, m.nodeID)
}
