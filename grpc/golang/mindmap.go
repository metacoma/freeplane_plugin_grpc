package freeplane

import (
	"context"
	"fmt"
)

// MindMap represents a Freeplane mind map.
// It provides map-level operations such as getting the root node,
// searching nodes, and exporting/importing the map.
type MindMap struct {
	client *FreeplaneClient
	mapID  string
	nodeID string
}

// NewMindMap creates a new MindMap instance.
func NewMindMap(client *FreeplaneClient, mapID, nodeID string) *MindMap {
	return &MindMap{
		client: client,
		mapID:  mapID,
		nodeID: nodeID,
	}
}

// Client returns the FreeplaneClient used for gRPC calls.
func (m *MindMap) Client() *FreeplaneClient {
	return m.client
}

// MapID returns the server-side map ID.
func (m *MindMap) MapID() string {
	return m.mapID
}

// NodeID returns the server-side node ID (currently focused node).
func (m *MindMap) NodeID() string {
	return m.nodeID
}

// Root gets the root node of this mind map by traversing up from the current node.
func (m *MindMap) Root(ctx context.Context) (*Node, error) {
	currentID := m.nodeID
	if currentID == "" {
		// Try to get current node first
		resp, err := m.client.CurrentMap(ctx)
		if err != nil {
			return nil, NewFreeplaneError(ErrorOperation, "No map currently open to get root from", err)
		}
		currentID = resp.nodeID
		if currentID == "" {
			return nil, NewFreeplaneError(ErrorOperation, "No map currently open to get root from", nil)
		}
	}

	// Traverse up to find root
	for {
		parentResp, err := m.client.GetParentNode(ctx, currentID)
		if err != nil {
			// If we can't get parent, this might be root
			break
		}
		if !parentResp.GetSuccess() || parentResp.GetParentNodeId() == "" {
			// This is the root
			return NewNode(m.client, currentID, m), nil
		}
		currentID = parentResp.GetParentNodeId()
	}

	// Fallback: return current node as root
	return NewNode(m.client, currentID, m), nil
}

// SelectedNode gets the currently selected/focused node.
func (m *MindMap) SelectedNode(ctx context.Context) (*Node, error) {
	mm, err := m.client.CurrentMap(ctx)
	if err != nil {
		return nil, NewFreeplaneError(ErrorOperation, "No node currently selected", err)
	}
	if mm.nodeID == "" {
		return nil, NewFreeplaneError(ErrorOperation, "No node currently selected", nil)
	}
	return NewNode(m.client, mm.nodeID, m), nil
}

// FindNodes finds all nodes whose text contains the given pattern (case-insensitive).
func (m *MindMap) FindNodes(ctx context.Context, pattern string) ([]*Node, error) {
	root, err := m.Root(ctx)
	if err != nil {
		return nil, err
	}
	return WalkAndCollect(ctx, root, pattern, nil)
}

// Info returns basic information about the current map.
func (m *MindMap) Info() map[string]string {
	return map[string]string{
		"map_id":  m.mapID,
		"node_id": m.nodeID,
	}
}

// Size estimates the number of nodes in the mind map.
func (m *MindMap) Size(ctx context.Context) (int, error) {
	root, err := m.Root(ctx)
	if err != nil {
		return 0, err
	}
	return CountNodes(ctx, root)
}

// Save saves the current mind map.
// If path is provided, it sets the save location via Groovy.
func (m *MindMap) Save(ctx context.Context, path string) (bool, error) {
	if path != "" {
		groovyCode := fmt.Sprintf(
			`model.getMap().getFile().setFile(new File("%s"));`+
				`model.getMap().getController().getUndoManager().undoableChanges(model.getMap());`+
				`model.getMap().getController().getMapView().updateFileHistory(model.getMap());`,
			path,
		)
		_, err := m.client.Groovy(ctx, groovyCode)
		if err != nil {
			return false, err
		}
	}
	return true, nil
}

// Export exports the current mind map to a file using Groovy scripting.
func (m *MindMap) Export(ctx context.Context, path, format string) (bool, error) {
	groovyCode := fmt.Sprintf(
		"def controller = model.getMap().getController();"+
			"def view = controller.getMapView();"+
			`view.exportMap(new File('%s'), '%s');`,
		path, format,
	)
	_, err := m.client.Groovy(ctx, groovyCode)
	if err != nil {
		return false, err
	}
	return true, nil
}

// ImportMap imports a mind map from a file.
func (m *MindMap) ImportMap(ctx context.Context, path string) (*MindMap, error) {
	return m.client.OpenMap(ctx, path)
}

// CreateNode creates a new node in the mind map.
// If parentID is empty, the node is created under the root.
func (m *MindMap) CreateNode(ctx context.Context, text, parentID, style string) (*Node, error) {
	if parentID == "" {
		root, err := m.Root(ctx)
		if err != nil {
			return nil, err
		}
		parentID = root.nodeID
	}
	resp, err := m.client.CreateChild(ctx, text, parentID)
	if err != nil {
		return nil, err
	}
	return NewNode(m.client, resp.GetNodeId(), m), nil
}

// CreateChild creates a child node under an existing node.
func (m *MindMap) CreateChild(ctx context.Context, parent *Node, text, style string) (*Node, error) {
	return m.CreateNode(ctx, text, parent.nodeID, style)
}

// String returns a human-readable representation of the mind map.
func (m *MindMap) String() string {
	return fmt.Sprintf("MindMap(map_id=%s, node_id=%s)", m.mapID, m.nodeID)
}
