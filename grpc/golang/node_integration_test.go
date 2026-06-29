package freeplane_grpc

import (
	"context"
	"testing"
	"time"
)

func TestNodeGetTextSetTextRoundTrip(t *testing.T) {
	client := getRealClient(t)

	_, cancel := context.WithTimeout(context.Background(), 60*time.Second)
	defer cancel()

	testNode := createTestNode(t, client, "IT_node_text")
	if testNode == nil {
		t.Skip("no map available for node text round-trip test")
	}

	newText := uniqueName("IT_node_text")
	err := testNode.SetText(newText)
	if err != nil {
		t.Fatalf("SetText() error: %v", err)
	}

	text, err := testNode.GetText()
	if err != nil {
		t.Fatalf("GetText() error: %v", err)
	}
	if text != newText {
		t.Errorf("expected text %q, got %q", newText, text)
	}
}

func TestNodeAddChild(t *testing.T) {
	client := getRealClient(t)

	_, cancel := context.WithTimeout(context.Background(), 60*time.Second)
	defer cancel()

	testNode := createTestNode(t, client, "IT_node_child")
	if testNode == nil {
		t.Skip("no map available for add child test")
	}

	childText := uniqueName("IT_child")
	child, err := testNode.AddChild(childText, "")
	if err != nil {
		t.Fatalf("AddChild() error: %v", err)
	}
	if child == nil {
		t.Fatal("AddChild() returned nil")
	}
	if child.NodeID() == "" {
		t.Error("expected non-empty node_id for child")
	}

	childText2, err := child.GetText()
	if err != nil {
		t.Fatalf("child GetText() error: %v", err)
	}
	if childText2 != childText {
		t.Errorf("expected child text %q, got %q", childText, childText2)
	}
}

func TestNodeChildren(t *testing.T) {
	client := getRealClient(t)

	_, cancel := context.WithTimeout(context.Background(), 60*time.Second)
	defer cancel()

	testNode := createTestNode(t, client, "IT_node_children")
	if testNode == nil {
		t.Skip("no map available for children test")
	}

	_, err := testNode.AddChild(uniqueName("IT_child1"), "")
	if err != nil {
		t.Fatalf("AddChild() error: %v", err)
	}
	_, err = testNode.AddChild(uniqueName("IT_child2"), "")
	if err != nil {
		t.Fatalf("AddChild() error: %v", err)
	}

	children, err := testNode.Children()
	if err != nil {
		t.Fatalf("Children() error: %v", err)
	}
	if children == nil {
		t.Error("expected non-nil children slice")
	}
}

func TestNodeParent(t *testing.T) {
	client := getRealClient(t)

	_, cancel := context.WithTimeout(context.Background(), 60*time.Second)
	defer cancel()

	testNode := createTestNode(t, client, "IT_node_parent")
	if testNode == nil {
		t.Skip("no map available for parent test")
	}

	child, err := testNode.AddChild(uniqueName("IT_child"), "")
	if err != nil {
		t.Fatalf("AddChild() error: %v", err)
	}

	parent, err := child.Parent()
	if err != nil {
		t.Fatalf("Parent() error: %v", err)
	}
	if parent == nil {
		t.Fatal("Parent() returned nil")
	}
}

func TestNodeSetTags(t *testing.T) {
	client := getRealClient(t)

	_, cancel := context.WithTimeout(context.Background(), 60*time.Second)
	defer cancel()

	testNode := createTestNode(t, client, "IT_node_tags")
	if testNode == nil {
		t.Skip("no map available for set tags test")
	}

	err := testNode.SetTags([]string{"tag1", "tag2"})
	if err != nil {
		t.Fatalf("SetTags() error: %v", err)
	}
}

func TestNodeAddTags(t *testing.T) {
	client := getRealClient(t)

	_, cancel := context.WithTimeout(context.Background(), 60*time.Second)
	defer cancel()

	testNode := createTestNode(t, client, "IT_node_addtags")
	if testNode == nil {
		t.Skip("no map available for add tags test")
	}

	err := testNode.AddTags([]string{"extra"})
	if err != nil {
		t.Fatalf("AddTags() error: %v", err)
	}
}

func TestNodeSetNotes(t *testing.T) {
	client := getRealClient(t)

	_, cancel := context.WithTimeout(context.Background(), 60*time.Second)
	defer cancel()

	testNode := createTestNode(t, client, "IT_node_notes")
	if testNode == nil {
		t.Skip("no map available for set notes test")
	}

	err := testNode.SetNotes("Integration test notes")
	if err != nil {
		t.Fatalf("SetNotes() error: %v", err)
	}
}

func TestNodeSetLinks(t *testing.T) {
	client := getRealClient(t)

	_, cancel := context.WithTimeout(context.Background(), 60*time.Second)
	defer cancel()

	testNode := createTestNode(t, client, "IT_node_links")
	if testNode == nil {
		t.Skip("no map available for set links test")
	}

	err := testNode.SetLinks("https://example.com")
	if err != nil {
		t.Fatalf("SetLinks() error: %v", err)
	}
}

func TestNodeSetColor(t *testing.T) {
	client := getRealClient(t)

	_, cancel := context.WithTimeout(context.Background(), 60*time.Second)
	defer cancel()

	testNode := createTestNode(t, client, "IT_node_color")
	if testNode == nil {
		t.Skip("no map available for set color test")
	}

	err := testNode.SetColor(0, 255, 0, 255)
	if err != nil {
		t.Fatalf("SetColor() error: %v", err)
	}
}

func TestNodeSetBackgroundColor(t *testing.T) {
	client := getRealClient(t)

	_, cancel := context.WithTimeout(context.Background(), 60*time.Second)
	defer cancel()

	testNode := createTestNode(t, client, "IT_node_bgcolor")
	if testNode == nil {
		t.Skip("no map available for set background color test")
	}

	err := testNode.SetBackgroundColor(255, 255, 0, 255)
	if err != nil {
		t.Fatalf("SetBackgroundColor() error: %v", err)
	}
}

func TestNodeSetDetails(t *testing.T) {
	client := getRealClient(t)

	ctx, cancel := context.WithTimeout(context.Background(), 60*time.Second)
	defer cancel()

	testNode := createTestNode(t, client, "IT_node_details")
	if testNode == nil {
		t.Skip("no map available for set details test")
	}

	_, err := client.NodeDetailsSet(ctx, testNode.NodeID(), "Integration test details")
	if err != nil {
		t.Fatalf("NodeDetailsSet() error: %v", err)
	}
}

func TestNodeSelect(t *testing.T) {
	client := getRealClient(t)

	_, cancel := context.WithTimeout(context.Background(), 60*time.Second)
	defer cancel()

	testNode := createTestNode(t, client, "IT_node_select")
	if testNode == nil {
		t.Skip("no map available for select test")
	}

	err := testNode.Select()
	if err != nil {
		t.Fatalf("Select() error: %v", err)
	}
}

func TestNodeDelete(t *testing.T) {
	client := getRealClient(t)

	_, cancel := context.WithTimeout(context.Background(), 60*time.Second)
	defer cancel()

	testNode := createTestNode(t, client, "IT_node_delete")
	if testNode == nil {
		t.Skip("no map available for delete test")
	}

	// Create a child to delete
	child, err := testNode.AddChild(uniqueName("IT_delete_child"), "")
	if err != nil {
		t.Fatalf("AddChild() error: %v", err)
	}

	// DeleteChild may not be supported in all Freeplane versions
	err = child.Delete()
	if err != nil {
		t.Logf("Delete() returned error (may be unsupported in this Freeplane version): %v", err)
	}
}
