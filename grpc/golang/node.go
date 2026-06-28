package freeplane

import (
	"context"
	"fmt"
	"strings"
)

// Node represents a Freeplane mind map node.
// It provides node-level operations such as getting/setting text,
// managing children, styling, notes, attributes, links, and icons.
type Node struct {
	client  *FreeplaneClient
	nodeID  string
	mindMap *MindMap
}

// NewNode creates a new Node instance.
func NewNode(client *FreeplaneClient, nodeID string, mindMap *MindMap) *Node {
	return &Node{
		client:  client,
		nodeID:  nodeID,
		mindMap: mindMap,
	}
}

// Client returns the FreeplaneClient used for gRPC calls.
func (n *Node) Client() *FreeplaneClient {
	return n.client
}

// NodeID returns the server-side node ID.
func (n *Node) NodeID() string {
	return n.nodeID
}

// MindMap returns the parent MindMap, if available.
func (n *Node) MindMap() *MindMap {
	return n.mindMap
}

// GetText gets the text of this node.
func (n *Node) GetText(ctx context.Context) (string, error) {
	text, err := n.client.GetNodeText(ctx, n.nodeID)
	if err != nil {
		return "", err
	}
	return text, nil
}

// SetText sets the text of this node.
func (n *Node) SetText(ctx context.Context, text string) error {
	return n.client.SetNodeText(ctx, n.nodeID, text)
}

// AddChild adds a child node to this node.
func (n *Node) AddChild(ctx context.Context, text, style string) (*Node, error) {
	resp, err := n.client.CreateChild(ctx, text, n.nodeID)
	if err != nil {
		return nil, err
	}
	var mm *MindMap = n.mindMap
	if mm != nil {
		mm.nodeID = resp.GetNodeId()
	}
	return NewNode(n.client, resp.GetNodeId(), mm), nil
}

// Children gets the direct children of this node.
func (n *Node) Children(ctx context.Context) ([]*Node, error) {
	children, err := n.client.ListChildNodes(ctx, n.nodeID)
	if err != nil {
		return nil, err
	}
	result := make([]*Node, 0, len(children))
	for _, child := range children {
		result = append(result, NewNode(n.client, child.GetNodeId(), n.mindMap))
	}
	return result, nil
}

// Parent gets the parent of this node.
func (n *Node) Parent(ctx context.Context) (*Node, error) {
	resp, err := n.client.GetParentNode(ctx, n.nodeID)
	if err != nil {
		return nil, err
	}
	if !resp.GetSuccess() || resp.GetParentNodeId() == "" {
		return nil, NewFreeplaneError(ErrorNodeNotFound,
			fmt.Sprintf("Node %s has no parent (is root)", n.nodeID), nil)
	}
	return NewNode(n.client, resp.GetParentNodeId(), n.mindMap), nil
}

// Delete deletes this node.
func (n *Node) Delete(ctx context.Context) (bool, error) {
	return n.client.DeleteChild(ctx, n.nodeID)
}

// Move moves this node under a new parent.
func (n *Node) Move(ctx context.Context, newParentID string) (bool, error) {
	return n.client.MoveNode(ctx, n.nodeID, newParentID)
}

// GetStyle gets the style information for this node via Groovy.
func (n *Node) GetStyle(ctx context.Context) (string, error) {
	groovyCode := fmt.Sprintf(
		"def node = model.getNode('%s');"+
			"def style = node.style;"+
			"def result = [:];"+
			"if (style != null) {"+
			"  result = style.getProperties().collectEntries { k, v ->"+
			"    [k.toString(), v.toString()]"+"  }"+
			"}",
		n.nodeID,
	)
	result, err := n.client.Groovy(ctx, groovyCode)
	if err != nil {
		return "", err
	}
	return result, nil
}

// SetStyle sets the style of this node via Groovy.
func (n *Node) SetStyle(ctx context.Context, style string) (bool, error) {
	groovyCode := fmt.Sprintf(
		"def node = model.getNode('%s');"+
			"node.style = model.getStyleLib().getStyle('%s');",
		n.nodeID, style,
	)
	result, err := n.client.Groovy(ctx, groovyCode)
	if err != nil {
		return false, err
	}
	return !strings.Contains(result, "Error"), nil
}

// GetColor gets the foreground color of this node via Groovy.
func (n *Node) GetColor(ctx context.Context) (string, error) {
	groovyCode := fmt.Sprintf(
		"def node = model.getNode('%s');"+
			"def color = node.color;"+
			"color ? color.toString() : 'null'",
		n.nodeID,
	)
	result, err := n.client.Groovy(ctx, groovyCode)
	if err != nil {
		return "", err
	}
	return result, nil
}

// SetColor sets the foreground color of this node.
func (n *Node) SetColor(ctx context.Context, r, g, b, a int32) (bool, error) {
	return n.client.NodeColorSet(ctx, n.nodeID, r, g, b, a)
}

// GetBackgroundColor gets the background color of this node via Groovy.
func (n *Node) GetBackgroundColor(ctx context.Context) (string, error) {
	groovyCode := fmt.Sprintf(
		"def node = model.getNode('%s');"+
			"def bg = node.background;"+
			"bg ? bg.toString() : 'null'",
		n.nodeID,
	)
	result, err := n.client.Groovy(ctx, groovyCode)
	if err != nil {
		return "", err
	}
	return result, nil
}

// SetBackgroundColor sets the background color of this node.
func (n *Node) SetBackgroundColor(ctx context.Context, r, g, b, a int32) (bool, error) {
	return n.client.NodeBackgroundColorSet(ctx, n.nodeID, r, g, b, a)
}

// GetNote gets the note of this node.
func (n *Node) GetNote(ctx context.Context) (string, error) {
	note, hasNote, err := n.client.GetNodeNote(ctx, n.nodeID)
	if err != nil {
		return "", err
	}
	if !hasNote {
		return "", nil
	}
	return note, nil
}

// SetNote sets a note on this node.
func (n *Node) SetNote(ctx context.Context, note string) error {
	_, err := n.client.NodeNoteSet(ctx, n.nodeID, note)
	return err
}

// GetAttributes gets the attributes of this node via Groovy.
func (n *Node) GetAttributes(ctx context.Context) (string, error) {
	groovyCode := fmt.Sprintf(
		"def node = model.getNode('%s');"+
			"def attrs = node.attributes;"+
			"attrs ? attrs.collectEntries { k, v -> [k.toString(), v.toString()] } : [:]",
		n.nodeID,
	)
	result, err := n.client.Groovy(ctx, groovyCode)
	if err != nil {
		return "", err
	}
	return result, nil
}

// SetAttribute sets a custom attribute on this node.
func (n *Node) SetAttribute(ctx context.Context, name, value string) (bool, error) {
	return n.client.NodeAttributeAdd(ctx, n.nodeID, name, value)
}

// SetAttributes sets multiple custom attributes on this node.
func (n *Node) SetAttributes(ctx context.Context, attrs map[string]string) (bool, error) {
	for name, value := range attrs {
		ok, err := n.client.NodeAttributeAdd(ctx, n.nodeID, name, value)
		if err != nil {
			return false, err
		}
		if !ok {
			return false, nil
		}
	}
	return true, nil
}

// GetLinks gets the links of this node.
func (n *Node) GetLinks(ctx context.Context) ([]string, error) {
	link, hasLink, err := n.client.GetNodeLink(ctx, n.nodeID)
	if err != nil {
		return nil, err
	}
	if !hasLink {
		return []string{}, nil
	}
	return []string{link}, nil
}

// SetLinks sets the links of this node (replaces existing links).
func (n *Node) SetLinks(ctx context.Context, links []string) (bool, error) {
	for _, link := range links {
		ok, err := n.client.NodeLinkSet(ctx, n.nodeID, link)
		if err != nil {
			return false, err
		}
		if !ok {
			return false, nil
		}
	}
	return true, nil
}

// SetTags sets the tags of this node (replaces existing tags).
func (n *Node) SetTags(ctx context.Context, tags []string) (bool, error) {
	return n.client.NodeTagSet(ctx, n.nodeID, tags)
}

// AddTags adds tags to this node (does not remove existing tags).
func (n *Node) AddTags(ctx context.Context, tags []string) (bool, error) {
	return n.client.NodeTagAdd(ctx, n.nodeID, tags)
}

// GetIcons gets the icons of this node via Groovy.
func (n *Node) GetIcons(ctx context.Context) (string, error) {
	groovyCode := fmt.Sprintf(
		"def node = model.getNode('%s');"+
			"def icons = node.getIconIds();"+
			"icons ? icons.toList() : []",
		n.nodeID,
	)
	result, err := n.client.Groovy(ctx, groovyCode)
	if err != nil {
		return "", err
	}
	return result, nil
}

// AddIcon adds an icon to this node.
func (n *Node) AddIcon(ctx context.Context, iconName string) (bool, error) {
	return n.client.NodeAddIcon(ctx, n.nodeID, iconName)
}

// GetFolded gets the folded (collapsed) state of this node via Groovy.
func (n *Node) GetFolded(ctx context.Context) (bool, error) {
	groovyCode := fmt.Sprintf(
		"def node = model.getNode('%s');"+
			"node.isFolded()",
		n.nodeID,
	)
	result, err := n.client.Groovy(ctx, groovyCode)
	if err != nil {
		return false, err
	}
	return strings.Contains(strings.ToLower(result), "true"), nil
}

// SetFolded sets the folded (collapsed) state of this node via Groovy.
func (n *Node) SetFolded(ctx context.Context, folded bool) (bool, error) {
	foldedStr := "false"
	if folded {
		foldedStr = "true"
	}
	groovyCode := fmt.Sprintf(
		"def node = model.getNode('%s');"+
			"if (%s) node.fold() else node.unfold();",
		n.nodeID, foldedStr,
	)
	result, err := n.client.Groovy(ctx, groovyCode)
	if err != nil {
		return false, err
	}
	return !strings.Contains(result, "Error"), nil
}

// Select selects this node in the Freeplane UI.
func (n *Node) Select(ctx context.Context) (bool, error) {
	return n.client.FocusNode(ctx, n.nodeID)
}

// Center centers the view on this node.
func (n *Node) Center(ctx context.Context) (bool, error) {
	return n.client.FocusNode(ctx, n.nodeID)
}

// Refresh reloads this node's state from the server.
func (n *Node) Refresh(ctx context.Context) error {
	_, err := n.GetText(ctx)
	return err
}

// String returns a human-readable representation of the node.
func (n *Node) String() string {
	text := "<unreadable>"
	if t, err := n.GetText(context.Background()); err == nil {
		if len(t) > 50 {
			text = t[:50]
		} else {
			text = t
		}
	}
	return fmt.Sprintf("Node(id=%s, text=%s)", n.nodeID, text)
}

// --- Helper: walk tree and collect matching nodes ---

// WalkAndCollect recursively walks the tree and collects nodes whose text contains the pattern.
func WalkAndCollect(ctx context.Context, node *Node, pattern string, matches []*Node) ([]*Node, error) {
	text, err := node.GetText(ctx)
	if err == nil && strings.Contains(strings.ToLower(text), strings.ToLower(pattern)) {
		matches = append(matches, node)
	}

	children, err := node.Children(ctx)
	if err != nil {
		return matches, nil
	}
	for _, child := range children {
		matches, err = WalkAndCollect(ctx, child, pattern, matches)
		if err != nil {
			return matches, nil
		}
	}
	return matches, nil
}

// CountNodes recursively counts nodes in the subtree.
func CountNodes(ctx context.Context, node *Node) (int, error) {
	count := 1
	children, err := node.Children(ctx)
	if err != nil {
		return count, nil
	}
	for _, child := range children {
		c, err := CountNodes(ctx, child)
		if err != nil {
			return count, nil
		}
		count += c
	}
	return count, nil
}
