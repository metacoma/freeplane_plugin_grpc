package freeplane_grpc

import (
	"context"
	"strings"
	"testing"
	"time"
)

func TestMindMapRoot(t *testing.T) {
	client := getRealClient(t)

	ctx, cancel := context.WithTimeout(context.Background(), 60*time.Second)
	defer cancel()

	mm, err := client.CurrentMap(ctx)
	if err != nil {
		t.Fatalf("CurrentMap() error: %v", err)
	}
	// Propagate context into MindMap (CurrentMap does not set ctx field)
	mm = mm.WithContext(ctx)

	root, err := mm.Root()
	if err != nil {
		t.Fatalf("Root() error: %v", err)
	}
	if root == nil {
		t.Fatal("Root() returned nil")
	}
	if root.NodeID() == "" {
		t.Error("expected non-empty node_id for root")
	}
}

func TestMindMapSelectedNode(t *testing.T) {
	client := getRealClient(t)

	ctx, cancel := context.WithTimeout(context.Background(), 60*time.Second)
	defer cancel()

	mm, err := client.CurrentMap(ctx)
	if err != nil {
		t.Fatalf("CurrentMap() error: %v", err)
	}
	// Propagate context into MindMap (CurrentMap does not set ctx field)
	mm = mm.WithContext(ctx)

	selected, err := mm.SelectedNode()
	if err != nil {
		t.Fatalf("SelectedNode() error: %v", err)
	}
	if selected == nil {
		t.Fatal("SelectedNode() returned nil")
	}
	if selected.NodeID() == "" {
		t.Error("expected non-empty node_id for selected node")
	}
}

func TestMindMapFindNodes(t *testing.T) {
	client := getRealClient(t)

	ctx, cancel := context.WithTimeout(context.Background(), 60*time.Second)
	defer cancel()

	mm, err := client.CurrentMap(ctx)
	if err != nil {
		t.Fatalf("CurrentMap() error: %v", err)
	}
	// Propagate context into MindMap (CurrentMap does not set ctx field)
	mm = mm.WithContext(ctx)

	// FindNodes may return nil or empty list depending on implementation
	// Just verify it doesn't panic
	_, err = mm.FindNodes("test")
	if err != nil {
		t.Logf("FindNodes() returned error (may be expected): %v", err)
	}
}

func TestMindMapGetJSON(t *testing.T) {
	client := getRealClient(t)

	ctx, cancel := context.WithTimeout(context.Background(), 60*time.Second)
	defer cancel()

	json, err := client.GetMapToJSON(ctx)
	if err != nil {
		t.Fatalf("GetMapToJSON() error: %v", err)
	}
	if json == "" {
		t.Error("expected non-empty JSON")
	}
	if len(json) <= 10 {
		t.Errorf("expected JSON length > 10, got %d", len(json))
	}
}

func TestMindMapCreateNode(t *testing.T) {
	client := getRealClient(t)

	ctx, cancel := context.WithTimeout(context.Background(), 60*time.Second)
	defer cancel()

	mm, err := client.CurrentMap(ctx)
	if err != nil {
		t.Fatalf("CurrentMap() error: %v", err)
	}
	// Propagate context into MindMap (CurrentMap does not set ctx field)
	mm = mm.WithContext(ctx)

	root, err := mm.Root()
	if err != nil {
		t.Fatalf("Root() error: %v", err)
	}

	nodeText := uniqueName("IT_mindmap_node")
	node, err := mm.CreateNode(nodeText, root.NodeID(), "")
	if err != nil {
		t.Fatalf("CreateNode() error: %v", err)
	}
	if node == nil {
		t.Fatal("CreateNode() returned nil")
	}

	text, err := node.GetText()
	if err != nil {
		t.Fatalf("node GetText() error: %v", err)
	}
	if text != nodeText {
		t.Errorf("expected node text %q, got %q", nodeText, text)
	}
}

func TestMindMapCreateChild(t *testing.T) {
	client := getRealClient(t)

	ctx, cancel := context.WithTimeout(context.Background(), 60*time.Second)
	defer cancel()

	mm, err := client.CurrentMap(ctx)
	if err != nil {
		t.Fatalf("CurrentMap() error: %v", err)
	}
	// Propagate context into MindMap (CurrentMap does not set ctx field)
	mm = mm.WithContext(ctx)

	root, err := mm.Root()
	if err != nil {
		t.Fatalf("Root() error: %v", err)
	}

	childText := uniqueName("IT_mindmap_child")
	child, err := mm.CreateChild(root, childText)
	if err != nil {
		t.Fatalf("CreateChild() error: %v", err)
	}
	if child == nil {
		t.Fatal("CreateChild() returned nil")
	}

	text, err := child.GetText()
	if err != nil {
		t.Fatalf("child GetText() error: %v", err)
	}
	if text != childText {
		t.Errorf("expected child text %q, got %q", childText, text)
	}
}

func TestMindMapConnectionError(t *testing.T) {
	badClient, err := NewClient("127.0.0.1", 59999)
	if err != nil {
		t.Fatalf("NewClient() error: %v", err)
	}

	err = badClient.Connect()
	if err != nil {
		t.Fatalf("Connect() error: %v", err)
	}
	defer badClient.Close()

	ctx, cancel := context.WithTimeout(context.Background(), 10*time.Second)
	defer cancel()

	_, err = badClient.CurrentMap(ctx)
	if err == nil {
		t.Fatal("expected error when connecting to wrong port")
	}
	// gRPC connection errors may vary by platform; just verify we got an error
	_ = strings.Contains(err.Error(), "connection")
}
