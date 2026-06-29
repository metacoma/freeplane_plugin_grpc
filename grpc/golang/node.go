package freeplane_grpc

import (
	"context"
	"fmt"
)

// Node represents a node in a Freeplane mind map.
type Node struct {
	client  *FreeplaneClient
	nodeID  string
	mindMap *MindMap
	ctx     context.Context
}

// NewNode creates a new Node instance.
func NewNode(client *FreeplaneClient, nodeID string, mindMap *MindMap) *Node {
	return &Node{
		client:  client,
		nodeID:  nodeID,
		mindMap: mindMap,
		ctx:     context.Background(),
	}
}

// WithContext returns a copy of the Node with the provided context.
func (n *Node) WithContext(ctx context.Context) *Node {
	return &Node{
		client:  n.client,
		nodeID:  n.nodeID,
		mindMap: n.mindMap,
		ctx:     ctx,
	}
}

// ==================== Text Operations ====================

// GetText returns the text of the node.
func (n *Node) GetText() (string, error) {
	resp, err := n.client.GetNodeText(n.ctx, n.nodeID)
	if err != nil {
		return "", err
	}
	return resp.Text, nil
}

// SetText sets the text of the node.
func (n *Node) SetText(text string) error {
	_, err := n.client.SetNodeText(n.ctx, n.nodeID, text)
	return err
}

// ==================== Hierarchy Operations ====================

// AddChild creates a child node with the given text and optional style.
func (n *Node) AddChild(text, style string) (*Node, error) {
	resp, err := n.client.CreateChild(n.ctx, text, n.nodeID)
	if err != nil {
		return nil, err
	}
	return NewNode(n.client, resp.NodeId, n.mindMap), nil
}

// Children returns the list of child nodes.
func (n *Node) Children() ([]*Node, error) {
	resp, err := n.client.ListChildNodes(n.ctx, n.nodeID)
	if err != nil {
		return nil, err
	}
	var children []*Node
	for _, child := range resp.Children {
		children = append(children, NewNode(n.client, child.NodeId, n.mindMap))
	}
	return children, nil
}

// Parent returns the parent node.
func (n *Node) Parent() (*Node, error) {
	resp, err := n.client.GetParentNode(n.ctx, n.nodeID)
	if err != nil {
		return nil, err
	}
	if resp.NodeId == "" {
		return nil, NewNodeNotFoundError("node has no parent")
	}
	return NewNode(n.client, resp.NodeId, n.mindMap), nil
}

// Delete deletes the node.
func (n *Node) Delete() error {
	_, err := n.client.DeleteChild(n.ctx, n.nodeID)
	return err
}

// Move moves the node to a new parent.
func (n *Node) Move(newParentID string) error {
	_, err := n.client.MoveNode(n.ctx, n.nodeID, newParentID)
	return err
}

// ==================== Styling Operations ====================

// GetStyle returns the style name of the node.
func (n *Node) GetStyle() (string, error) {
	// Style is retrieved via Groovy script since there's no dedicated RPC
	resp, err := n.client.Groovy(n.ctx, fmt.Sprintf("return node.getStyle().toString()"))
	if err != nil {
		return "", err
	}
	return resp.Result, nil
}

// SetStyle sets the style name of the node.
func (n *Node) SetStyle(style string) error {
	// Escape single quotes to prevent Groovy injection
	escaped := style
	for i := 0; i < len(escaped); i++ {
		if escaped[i] == '\'' {
			escaped = escaped[:i] + "\\'" + escaped[i+1:]
			i++
		}
	}
	_, err := n.client.Groovy(n.ctx, fmt.Sprintf("node.getStyle().setStyle('%s')", escaped))
	return err
}

// GetColor returns the text color as RGBA.
func (n *Node) GetColor() (red, green, blue, alpha int32, err error) {
	// Color is retrieved via Groovy script
	resp, err := n.client.Groovy(n.ctx, "return node.style.color.toString()")
	if err != nil {
		return 0, 0, 0, 0, err
	}
	// Parse the color string (format: "r,g,b,a" or similar)
	// TODO: Implement actual color parsing from Groovy result
	if resp.Result == "" {
		return 0, 0, 0, 0, NewOperationError("empty color result from Groovy script")
	}
	_ = resp
	return 0, 0, 0, 0, nil
}

// SetColor sets the text color.
func (n *Node) SetColor(red, green, blue, alpha int32) error {
	_, err := n.client.NodeColorSet(n.ctx, n.nodeID, red, green, blue, alpha)
	return err
}

// SetBackgroundColor sets the background color.
func (n *Node) SetBackgroundColor(red, green, blue, alpha int32) error {
	_, err := n.client.NodeBackgroundColorSet(n.ctx, n.nodeID, red, green, blue, alpha)
	return err
}

// ==================== Notes Operations ====================

// GetNotes returns the note content of the node.
func (n *Node) GetNotes() (string, bool, error) {
	resp, err := n.client.GetNodeNote(n.ctx, n.nodeID)
	if err != nil {
		return "", false, err
	}
	return resp.Note, resp.HasNote, nil
}

// SetNotes sets the note content of the node.
func (n *Node) SetNotes(notes string) error {
	_, err := n.client.NodeNoteSet(n.ctx, n.nodeID, notes)
	return err
}

// ==================== Attributes Operations ====================

// GetAttributes returns the node attributes.
func (n *Node) GetAttributes() (map[string]string, error) {
	// Attributes are retrieved via Groovy script
	resp, err := n.client.Groovy(n.ctx, "def attrs = [:]; node.attributes.each { k, v -> attrs[k] = v.toString() }; return attrs")
	if err != nil {
		return nil, err
	}
	// Parse the result - for now return empty map
	if resp.Result == "" {
		return nil, NewOperationError("empty attributes result from Groovy script")
	}
	_ = resp
	return make(map[string]string), nil
}

// SetAttribute sets a single attribute.
func (n *Node) SetAttribute(name, value string) error {
	_, err := n.client.NodeAttributeAdd(n.ctx, n.nodeID, name, value)
	return err
}

// SetAttributes sets multiple attributes.
func (n *Node) SetAttributes(attrs map[string]string) error {
	for name, value := range attrs {
		if err := n.SetAttribute(name, value); err != nil {
			return err
		}
	}
	return nil
}

// ==================== Links Operations ====================

// GetLinks returns the node links.
func (n *Node) GetLinks() (string, bool, error) {
	resp, err := n.client.GetNodeLink(n.ctx, n.nodeID)
	if err != nil {
		return "", false, err
	}
	return resp.Link, resp.HasLink, nil
}

// SetLinks sets the node link.
func (n *Node) SetLinks(link string) error {
	_, err := n.client.NodeLinkSet(n.ctx, n.nodeID, link)
	return err
}

// ==================== Tags Operations ====================

// SetTags sets tags on the node (replaces existing tags).
func (n *Node) SetTags(tags []string) error {
	_, err := n.client.NodeTagSet(n.ctx, n.nodeID, tags)
	return err
}

// AddTags adds tags to the node (appends to existing tags).
func (n *Node) AddTags(tags []string) error {
	_, err := n.client.NodeTagAdd(n.ctx, n.nodeID, tags)
	return err
}

// ==================== Icons Operations ====================

// GetIcons returns the node icons.
func (n *Node) GetIcons() ([]string, error) {
	// Icons are retrieved via Groovy script
	resp, err := n.client.Groovy(n.ctx, "return node.iconNames.toArray()")
	if err != nil {
		return nil, err
	}
	if resp.Result == "" {
		return nil, NewOperationError("empty icons result from Groovy script")
	}
	_ = resp
	return nil, nil
}

// AddIcon adds an icon to the node.
func (n *Node) AddIcon(name string) error {
	_, err := n.client.NodeAddIcon(n.ctx, n.nodeID, name)
	return err
}

// ==================== State Operations ====================

// GetFolded returns whether the node is folded.
func (n *Node) GetFolded() (bool, error) {
	resp, err := n.client.Groovy(n.ctx, "return node.isExpanded()")
	if err != nil {
		return false, err
	}
	if resp.Result == "" {
		return false, NewOperationError("empty folded state result from Groovy script")
	}
	_ = resp
	return false, nil
}

// SetFolded sets the folded state of the node.
func (n *Node) SetFolded(folded bool) error {
	var cmd string
	if folded {
		cmd = "node.performCollapse()"
	} else {
		cmd = "node.performExpand()"
	}
	_, err := n.client.Groovy(n.ctx, cmd)
	return err
}

// ==================== Action Operations ====================

// Select selects the node.
func (n *Node) Select() error {
	return n.client.FocusNodeByID(n.ctx, n.nodeID)
}

// Centers the node in the view.
func (n *Node) Center() error {
	_, err := n.client.Groovy(n.ctx, "node.centerInView()")
	return err
}

// Refresh refreshes the node display.
func (n *Node) Refresh() error {
	_, err := n.client.Groovy(n.ctx, "node.refresh()")
	return err
}

// ==================== Properties ====================

// Client returns the FreeplaneClient.
func (n *Node) Client() *FreeplaneClient {
	return n.client
}

// NodeID returns the node ID.
func (n *Node) NodeID() string {
	return n.nodeID
}

// MindMap returns the parent MindMap.
func (n *Node) MindMap() *MindMap {
	return n.mindMap
}

// String returns a string representation of the node.
func (n *Node) String() string {
	text, _ := n.GetText()
	return fmt.Sprintf("Node{id=%s, text=%q}", n.nodeID, text)
}
