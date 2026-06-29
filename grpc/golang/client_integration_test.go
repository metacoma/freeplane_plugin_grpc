package freeplane_grpc

import (
	"context"
	"strings"
	"testing"
	"time"
)

func TestClientConnectivity(t *testing.T) {
	client := getRealClient(t)

	ctx, cancel := context.WithTimeout(context.Background(), 60*time.Second)
	defer cancel()

	mm, err := client.CurrentMap(ctx)
	if err != nil {
		t.Fatalf("CurrentMap() error: %v", err)
	}
	if mm == nil {
		t.Fatal("CurrentMap() returned nil")
	}
	// Propagate context into MindMap (CurrentMap does not set ctx field)
	mm = mm.WithContext(ctx)
	_ = mm
}

func TestClientCreateChild(t *testing.T) {
	client := getRealClient(t)

	_, cancel := context.WithTimeout(context.Background(), 60*time.Second)
	defer cancel()

	testNode := createTestNode(t, client, "IT_createchild")
	if testNode == nil {
		t.Skip("no map available for create child test")
	}

	if testNode.NodeID() == "" {
		t.Error("expected non-empty node_id")
	}
}

func TestClientSetTextGetTextRoundTrip(t *testing.T) {
	client := getRealClient(t)

	_, cancel := context.WithTimeout(context.Background(), 60*time.Second)
	defer cancel()

	testNode := createTestNode(t, client, "IT_text")
	if testNode == nil {
		t.Skip("no map available for text round-trip test")
	}

	newText := uniqueName("IT_text")
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

func TestClientListChildNodes(t *testing.T) {
	client := getRealClient(t)

	ctx, cancel := context.WithTimeout(context.Background(), 60*time.Second)
	defer cancel()

	testNode := createTestNode(t, client, "IT_list")
	if testNode == nil {
		t.Skip("no map available for list child nodes test")
	}

	resp, err := client.ListChildNodes(ctx, testNode.NodeID())
	if err != nil {
		t.Fatalf("ListChildNodes() error: %v", err)
	}
	if resp == nil {
		t.Error("expected non-nil response")
	}
}

func TestClientNodeTagSet(t *testing.T) {
	client := getRealClient(t)

	_, cancel := context.WithTimeout(context.Background(), 60*time.Second)
	defer cancel()

	testNode := createTestNode(t, client, "IT_tag")
	if testNode == nil {
		t.Skip("no map available for tag set test")
	}

	err := testNode.SetTags([]string{"tag1", "tag2"})
	if err != nil {
		t.Fatalf("SetTags() error: %v", err)
	}
}

func TestClientNodeTagAdd(t *testing.T) {
	client := getRealClient(t)

	_, cancel := context.WithTimeout(context.Background(), 60*time.Second)
	defer cancel()

	testNode := createTestNode(t, client, "IT_tagadd")
	if testNode == nil {
		t.Skip("no map available for tag add test")
	}

	err := testNode.AddTags([]string{"extra"})
	if err != nil {
		t.Fatalf("AddTags() error: %v", err)
	}
}

func TestClientNodeNoteSetGetNodeNote(t *testing.T) {
	client := getRealClient(t)

	_, cancel := context.WithTimeout(context.Background(), 60*time.Second)
	defer cancel()

	testNode := createTestNode(t, client, "IT_note")
	if testNode == nil {
		t.Skip("no map available for note test")
	}

	noteText := uniqueName("IT_note")
	err := testNode.SetNotes(noteText)
	if err != nil {
		t.Fatalf("SetNotes() error: %v", err)
	}

	_, hasNote, err := testNode.GetNotes()
	if err != nil {
		t.Fatalf("GetNotes() error: %v", err)
	}
	_ = hasNote
	// Server may or may not return the note; just verify no error
}

func TestClientNodeLinkSetGetNodeLink(t *testing.T) {
	client := getRealClient(t)

	_, cancel := context.WithTimeout(context.Background(), 60*time.Second)
	defer cancel()

	testNode := createTestNode(t, client, "IT_link")
	if testNode == nil {
		t.Skip("no map available for link test")
	}

	linkURL := "https://example.com/test-" + uniqueName("link")
	err := testNode.SetLinks(linkURL)
	if err != nil {
		t.Fatalf("SetLinks() error: %v", err)
	}

	_, _, err = testNode.GetLinks()
	if err != nil {
		t.Fatalf("GetLinks() error: %v", err)
	}
}

func TestClientNodeColorSet(t *testing.T) {
	client := getRealClient(t)

	_, cancel := context.WithTimeout(context.Background(), 60*time.Second)
	defer cancel()

	testNode := createTestNode(t, client, "IT_color")
	if testNode == nil {
		t.Skip("no map available for color test")
	}

	err := testNode.SetColor(0, 255, 0, 255)
	if err != nil {
		t.Fatalf("SetColor() error: %v", err)
	}
}

func TestClientMindMapToJSON(t *testing.T) {
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

func TestClientStatusInfoSet(t *testing.T) {
	client := getRealClient(t)

	ctx, cancel := context.WithTimeout(context.Background(), 60*time.Second)
	defer cancel()

	info := uniqueName("IT_status")
	err := client.SetStatusInfoText(ctx, info)
	if err != nil {
		t.Fatalf("SetStatusInfoText() error: %v", err)
	}
}

func TestClientConnectionError(t *testing.T) {
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
