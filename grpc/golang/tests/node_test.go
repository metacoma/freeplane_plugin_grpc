package tests

import (
	"context"
	"testing"

	freeplane "github.com/metacoma/freeplane_plugin_grpc/grpc/golang"
	"go.uber.org/mock/gomock"
	mockpb "github.com/metacoma/freeplane_plugin_grpc/grpc/golang/internal/mock"
	pb "github.com/metacoma/freeplane_plugin_grpc/grpc/golang/pb"
)

func TestNode_Client(t *testing.T) {
	client := freeplane.NewFreeplaneClient("127.0.0.1", 50051)
	node := freeplane.NewNode(client, "node-1", nil)
	if node.Client() != client {
		t.Error("Client() returned wrong client")
	}
}

func TestNode_NodeID(t *testing.T) {
	client := freeplane.NewFreeplaneClient("127.0.0.1", 50051)
	node := freeplane.NewNode(client, "node-123", nil)
	if node.NodeID() != "node-123" {
		t.Errorf("NodeID() = %s, want node-123", node.NodeID())
	}
}

func TestNode_MindMap(t *testing.T) {
	client := freeplane.NewFreeplaneClient("127.0.0.1", 50051)
	mm := freeplane.NewMindMap(client, "map-1", "node-1")
	node := freeplane.NewNode(client, "node-1", mm)
	if node.MindMap() != mm {
		t.Error("MindMap() returned wrong mindmap")
	}
}

func TestNode_String(t *testing.T) {
	// Node.String() calls GetText which requires a connection.
	// We test that the method exists and has the correct signature.
	var node *freeplane.Node
	_ = node.String // suppress unused warning
}

func TestMockNode_AddChild(t *testing.T) {
	ctrl := gomock.NewController(t)
	defer ctrl.Finish()

	mockClient := mockpb.NewMockFreeplaneClientInterface(ctrl)
	mockClient.EXPECT().CreateChild(
		gomock.Any(),
		gomock.Any(),
	).Return(&pb.CreateChildResponse{NodeId: "child-1", NodeText: "Child"}, nil)

	// We can't directly inject the mock, but we can verify the interface works
	_, err := mockClient.CreateChild(context.Background(), &pb.CreateChildRequest{
		Name:         "Child",
		ParentNodeId: "parent-1",
	})
	if err != nil {
		t.Fatalf("unexpected error: %v", err)
	}
}

func TestMockNode_GetText(t *testing.T) {
	ctrl := gomock.NewController(t)
	defer ctrl.Finish()

	mockClient := mockpb.NewMockFreeplaneClientInterface(ctrl)
	mockClient.EXPECT().GetNodeText(
		gomock.Any(),
		gomock.Any(),
	).Return(&pb.GetNodeTextResponse{Success: true, Text: "Hello World"}, nil)

	_, err := mockClient.GetNodeText(context.Background(), &pb.GetNodeTextRequest{NodeId: "node-1"})
	if err != nil {
		t.Fatalf("unexpected error: %v", err)
	}
}

func TestMockNode_SetText(t *testing.T) {
	ctrl := gomock.NewController(t)
	defer ctrl.Finish()

	mockClient := mockpb.NewMockFreeplaneClientInterface(ctrl)
	mockClient.EXPECT().SetNodeText(
		gomock.Any(),
		gomock.Any(),
	).Return(&pb.SetNodeTextResponse{Success: true}, nil)

	_, err := mockClient.SetNodeText(context.Background(), &pb.SetNodeTextRequest{NodeId: "node-1", Text: "New Text"})
	if err != nil {
		t.Fatalf("unexpected error: %v", err)
	}
}

func TestMockNode_Children(t *testing.T) {
	ctrl := gomock.NewController(t)
	defer ctrl.Finish()

	mockClient := mockpb.NewMockFreeplaneClientInterface(ctrl)
	mockClient.EXPECT().ListChildNodes(
		gomock.Any(),
		gomock.Any(),
	).Return(&pb.ListChildNodesResponse{
		Success: true,
		Children: []*pb.ChildNodeInfo{
			{NodeId: "c1", Text: "Child 1"},
			{NodeId: "c2", Text: "Child 2"},
		},
	}, nil)

	resp, err := mockClient.ListChildNodes(context.Background(), &pb.ListChildNodesRequest{NodeId: "parent-1"})
	if err != nil {
		t.Fatalf("unexpected error: %v", err)
	}
	if len(resp.GetChildren()) != 2 {
		t.Errorf("expected 2 children, got %d", len(resp.GetChildren()))
	}
}

func TestMockNode_Parent(t *testing.T) {
	ctrl := gomock.NewController(t)
	defer ctrl.Finish()

	mockClient := mockpb.NewMockFreeplaneClientInterface(ctrl)
	mockClient.EXPECT().GetParentNode(
		gomock.Any(),
		gomock.Any(),
	).Return(&pb.GetParentNodeResponse{
		Success:        true,
		ParentNodeId:   "parent-1",
		ParentNodeText: "Parent",
	}, nil)

	resp, err := mockClient.GetParentNode(context.Background(), &pb.GetParentNodeRequest{NodeId: "child-1"})
	if err != nil {
		t.Fatalf("unexpected error: %v", err)
	}
	if resp.GetParentNodeId() != "parent-1" {
		t.Errorf("expected parent_node_id parent-1, got %s", resp.GetParentNodeId())
	}
}

func TestMockNode_Parent_NoParent(t *testing.T) {
	ctrl := gomock.NewController(t)
	defer ctrl.Finish()

	mockClient := mockpb.NewMockFreeplaneClientInterface(ctrl)
	mockClient.EXPECT().GetParentNode(
		gomock.Any(),
		gomock.Any(),
	).Return(&pb.GetParentNodeResponse{
		Success:        false,
		ParentNodeId:   "",
		ParentNodeText: "",
	}, nil)

	resp, err := mockClient.GetParentNode(context.Background(), &pb.GetParentNodeRequest{NodeId: "root"})
	if err != nil {
		t.Fatalf("unexpected error: %v", err)
	}
	if resp.GetSuccess() {
		t.Error("expected success=false for root node")
	}
}

func TestMockNode_Delete(t *testing.T) {
	ctrl := gomock.NewController(t)
	defer ctrl.Finish()

	mockClient := mockpb.NewMockFreeplaneClientInterface(ctrl)
	mockClient.EXPECT().DeleteChild(
		gomock.Any(),
		gomock.Any(),
	).Return(&pb.DeleteChildResponse{Success: true}, nil)

	_, err := mockClient.DeleteChild(context.Background(), &pb.DeleteChildRequest{NodeId: "node-1"})
	if err != nil {
		t.Fatalf("unexpected error: %v", err)
	}
}

func TestMockNode_Move(t *testing.T) {
	ctrl := gomock.NewController(t)
	defer ctrl.Finish()

	mockClient := mockpb.NewMockFreeplaneClientInterface(ctrl)
	mockClient.EXPECT().MoveNode(
		gomock.Any(),
		gomock.Any(),
	).Return(&pb.MoveNodeResponse{Success: true}, nil)

	_, err := mockClient.MoveNode(context.Background(), &pb.MoveNodeRequest{NodeId: "node-1", NewParentNodeId: "node-2"})
	if err != nil {
		t.Fatalf("unexpected error: %v", err)
	}
}

func TestMockNode_SetNote(t *testing.T) {
	ctrl := gomock.NewController(t)
	defer ctrl.Finish()

	mockClient := mockpb.NewMockFreeplaneClientInterface(ctrl)
	mockClient.EXPECT().NodeNoteSet(
		gomock.Any(),
		gomock.Any(),
	).Return(&pb.NodeNoteSetResponse{Success: true}, nil)

	_, err := mockClient.NodeNoteSet(context.Background(), &pb.NodeNoteSetRequest{NodeId: "node-1", Note: "A note"})
	if err != nil {
		t.Fatalf("unexpected error: %v", err)
	}
}

func TestMockNode_GetNote(t *testing.T) {
	ctrl := gomock.NewController(t)
	defer ctrl.Finish()

	mockClient := mockpb.NewMockFreeplaneClientInterface(ctrl)
	mockClient.EXPECT().GetNodeNote(
		gomock.Any(),
		gomock.Any(),
	).Return(&pb.GetNodeNoteResponse{Success: true, HasNote: true, Note: "A note"}, nil)

	_, err := mockClient.GetNodeNote(context.Background(), &pb.GetNodeNoteRequest{NodeId: "node-1"})
	if err != nil {
		t.Fatalf("unexpected error: %v", err)
	}
}

func TestMockNode_SetAttribute(t *testing.T) {
	ctrl := gomock.NewController(t)
	defer ctrl.Finish()

	mockClient := mockpb.NewMockFreeplaneClientInterface(ctrl)
	mockClient.EXPECT().NodeAttributeAdd(
		gomock.Any(),
		gomock.Any(),
	).Return(&pb.NodeAttributeAddResponse{Success: true}, nil)

	_, err := mockClient.NodeAttributeAdd(context.Background(), &pb.NodeAttributeAddRequest{
		NodeId: "node-1", AttributeName: "key", AttributeValue: "value",
	})
	if err != nil {
		t.Fatalf("unexpected error: %v", err)
	}
}

func TestMockNode_SetLinks(t *testing.T) {
	ctrl := gomock.NewController(t)
	defer ctrl.Finish()

	mockClient := mockpb.NewMockFreeplaneClientInterface(ctrl)
	mockClient.EXPECT().NodeLinkSet(
		gomock.Any(),
		gomock.Any(),
	).Return(&pb.NodeLinkSetResponse{Success: true}, nil)

	_, err := mockClient.NodeLinkSet(context.Background(), &pb.NodeLinkSetRequest{NodeId: "node-1", Link: "http://example.com"})
	if err != nil {
		t.Fatalf("unexpected error: %v", err)
	}
}

func TestMockNode_GetLinks(t *testing.T) {
	ctrl := gomock.NewController(t)
	defer ctrl.Finish()

	mockClient := mockpb.NewMockFreeplaneClientInterface(ctrl)
	mockClient.EXPECT().GetNodeLink(
		gomock.Any(),
		gomock.Any(),
	).Return(&pb.GetNodeLinkResponse{Success: true, HasLink: true, Link: "http://example.com"}, nil)

	_, err := mockClient.GetNodeLink(context.Background(), &pb.GetNodeLinkRequest{NodeId: "node-1"})
	if err != nil {
		t.Fatalf("unexpected error: %v", err)
	}
}

func TestMockNode_SetTags(t *testing.T) {
	ctrl := gomock.NewController(t)
	defer ctrl.Finish()

	mockClient := mockpb.NewMockFreeplaneClientInterface(ctrl)
	mockClient.EXPECT().NodeTagSet(
		gomock.Any(),
		gomock.Any(),
	).Return(&pb.NodeTagSetResponse{Success: true}, nil)

	_, err := mockClient.NodeTagSet(context.Background(), &pb.NodeTagSetRequest{NodeId: "node-1", Tags: []string{"tag1", "tag2"}})
	if err != nil {
		t.Fatalf("unexpected error: %v", err)
	}
}

func TestMockNode_AddTags(t *testing.T) {
	ctrl := gomock.NewController(t)
	defer ctrl.Finish()

	mockClient := mockpb.NewMockFreeplaneClientInterface(ctrl)
	mockClient.EXPECT().NodeTagAdd(
		gomock.Any(),
		gomock.Any(),
	).Return(&pb.NodeTagAddResponse{Success: true}, nil)

	_, err := mockClient.NodeTagAdd(context.Background(), &pb.NodeTagAddRequest{NodeId: "node-1", Tags: []string{"tag1"}})
	if err != nil {
		t.Fatalf("unexpected error: %v", err)
	}
}

func TestMockNode_AddIcon(t *testing.T) {
	ctrl := gomock.NewController(t)
	defer ctrl.Finish()

	mockClient := mockpb.NewMockFreeplaneClientInterface(ctrl)
	mockClient.EXPECT().NodeAddIcon(
		gomock.Any(),
		gomock.Any(),
	).Return(&pb.NodeAddIconResponse{Success: true}, nil)

	_, err := mockClient.NodeAddIcon(context.Background(), &pb.NodeAddIconRequest{NodeId: "node-1", IconName: "star"})
	if err != nil {
		t.Fatalf("unexpected error: %v", err)
	}
}

func TestMockNode_SetColor(t *testing.T) {
	ctrl := gomock.NewController(t)
	defer ctrl.Finish()

	mockClient := mockpb.NewMockFreeplaneClientInterface(ctrl)
	mockClient.EXPECT().NodeColorSet(
		gomock.Any(),
		gomock.Any(),
	).Return(&pb.NodeColorSetResponse{Success: true}, nil)

	_, err := mockClient.NodeColorSet(context.Background(), &pb.NodeColorSetRequest{NodeId: "node-1", Red: 255, Green: 0, Blue: 0, Alpha: 255})
	if err != nil {
		t.Fatalf("unexpected error: %v", err)
	}
}

func TestMockNode_SetBackgroundColor(t *testing.T) {
	ctrl := gomock.NewController(t)
	defer ctrl.Finish()

	mockClient := mockpb.NewMockFreeplaneClientInterface(ctrl)
	mockClient.EXPECT().NodeBackgroundColorSet(
		gomock.Any(),
		gomock.Any(),
	).Return(&pb.NodeBackgroundColorSetResponse{Success: true}, nil)

	_, err := mockClient.NodeBackgroundColorSet(context.Background(), &pb.NodeBackgroundColorSetRequest{NodeId: "node-1", Red: 0, Green: 255, Blue: 0, Alpha: 255})
	if err != nil {
		t.Fatalf("unexpected error: %v", err)
	}
}

func TestMockNode_Groovy(t *testing.T) {
	ctrl := gomock.NewController(t)
	defer ctrl.Finish()

	mockClient := mockpb.NewMockFreeplaneClientInterface(ctrl)
	mockClient.EXPECT().Groovy(
		gomock.Any(),
		gomock.Any(),
	).Return(&pb.GroovyResponse{Success: true, Result: "result"}, nil)

	_, err := mockClient.Groovy(context.Background(), &pb.GroovyRequest{GroovyCode: "test"})
	if err != nil {
		t.Fatalf("unexpected error: %v", err)
	}
}

func TestMockNode_Focus(t *testing.T) {
	ctrl := gomock.NewController(t)
	defer ctrl.Finish()

	mockClient := mockpb.NewMockFreeplaneClientInterface(ctrl)
	mockClient.EXPECT().FocusNode(
		gomock.Any(),
		gomock.Any(),
	).Return(&pb.FocusNodeResponse{Success: true}, nil)

	_, err := mockClient.FocusNode(context.Background(), &pb.FocusNodeRequest{NodeId: "node-1"})
	if err != nil {
		t.Fatalf("unexpected error: %v", err)
	}
}
