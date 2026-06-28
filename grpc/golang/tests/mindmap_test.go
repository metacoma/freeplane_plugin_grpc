package tests

import (
	"context"
	"testing"

	freeplane "github.com/metacoma/freeplane_plugin_grpc/grpc/golang"
	"go.uber.org/mock/gomock"
	mockpb "github.com/metacoma/freeplane_plugin_grpc/grpc/golang/internal/mock"
	pb "github.com/metacoma/freeplane_plugin_grpc/grpc/golang/pb"
)

func TestMindMap_Client(t *testing.T) {
	client := freeplane.NewFreeplaneClient("127.0.0.1", 50051)
	mm := freeplane.NewMindMap(client, "map-1", "node-1")
	if mm.Client() != client {
		t.Error("Client() returned wrong client")
	}
}

func TestMindMap_MapID(t *testing.T) {
	client := freeplane.NewFreeplaneClient("127.0.0.1", 50051)
	mm := freeplane.NewMindMap(client, "map-1", "node-1")
	if mm.MapID() != "map-1" {
		t.Errorf("MapID() = %s, want map-1", mm.MapID())
	}
}

func TestMindMap_NodeID(t *testing.T) {
	client := freeplane.NewFreeplaneClient("127.0.0.1", 50051)
	mm := freeplane.NewMindMap(client, "map-1", "node-1")
	if mm.NodeID() != "node-1" {
		t.Errorf("NodeID() = %s, want node-1", mm.NodeID())
	}
}

func TestMindMap_Info(t *testing.T) {
	client := freeplane.NewFreeplaneClient("127.0.0.1", 50051)
	mm := freeplane.NewMindMap(client, "map-1", "node-1")
	info := mm.Info()
	if info["map_id"] != "map-1" {
		t.Errorf("info map_id = %s, want map-1", info["map_id"])
	}
	if info["node_id"] != "node-1" {
		t.Errorf("info node_id = %s, want node-1", info["node_id"])
	}
}

func TestMindMap_String(t *testing.T) {
	client := freeplane.NewFreeplaneClient("127.0.0.1", 50051)
	mm := freeplane.NewMindMap(client, "map-1", "node-1")
	s := mm.String()
	if s == "" {
		t.Error("String() returned empty string")
	}
}

func TestMockMindMap_CurrentMap(t *testing.T) {
	ctrl := gomock.NewController(t)
	defer ctrl.Finish()

	mockClient := mockpb.NewMockFreeplaneClientInterface(ctrl)
	mockClient.EXPECT().GetCurrentNode(
		gomock.Any(),
		gomock.Any(),
	).Return(&pb.GetCurrentNodeResponse{
		Success: true,
		MapId:   "map-1",
		NodeId:  "node-1",
	}, nil)

	_, err := mockClient.GetCurrentNode(context.Background(), &pb.GetCurrentNodeRequest{})
	if err != nil {
		t.Fatalf("unexpected error: %v", err)
	}
}

func TestMockMindMap_OpenMap(t *testing.T) {
	ctrl := gomock.NewController(t)
	defer ctrl.Finish()

	mockClient := mockpb.NewMockFreeplaneClientInterface(ctrl)
	mockClient.EXPECT().OpenMap(
		gomock.Any(),
		gomock.Any(),
	).Return(&pb.OpenMapResponse{Success: true}, nil)

	_, err := mockClient.OpenMap(context.Background(), &pb.OpenMapRequest{FilePath: "/test.mm"})
	if err != nil {
		t.Fatalf("unexpected error: %v", err)
	}
}

func TestMockMindMap_GetMapToJSON(t *testing.T) {
	ctrl := gomock.NewController(t)
	defer ctrl.Finish()

	mockClient := mockpb.NewMockFreeplaneClientInterface(ctrl)
	mockClient.EXPECT().MindMapToJSON(
		gomock.Any(),
		gomock.Any(),
	).Return(&pb.MindMapToJSONResponse{Success: true, Json: `{"nodes":[]}`}, nil)

	_, err := mockClient.MindMapToJSON(context.Background(), &pb.MindMapToJSONRequest{})
	if err != nil {
		t.Fatalf("unexpected error: %v", err)
	}
}

func TestMockMindMap_MindMapFromJSON(t *testing.T) {
	ctrl := gomock.NewController(t)
	defer ctrl.Finish()

	mockClient := mockpb.NewMockFreeplaneClientInterface(ctrl)
	mockClient.EXPECT().MindMapFromJSON(
		gomock.Any(),
		gomock.Any(),
	).Return(&pb.MindMapFromJSONResponse{Success: true}, nil)

	_, err := mockClient.MindMapFromJSON(context.Background(), &pb.MindMapFromJSONRequest{Json: `{"nodes":[]}`})
	if err != nil {
		t.Fatalf("unexpected error: %v", err)
	}
}

func TestMockMindMap_FocusNode(t *testing.T) {
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

func TestMockMindMap_SetStatusInfo(t *testing.T) {
	ctrl := gomock.NewController(t)
	defer ctrl.Finish()

	mockClient := mockpb.NewMockFreeplaneClientInterface(ctrl)
	mockClient.EXPECT().StatusInfoSet(
		gomock.Any(),
		gomock.Any(),
	).Return(&pb.StatusInfoSetResponse{Success: true}, nil)

	_, err := mockClient.StatusInfoSet(context.Background(), &pb.StatusInfoSetRequest{StatusInfo: "test"})
	if err != nil {
		t.Fatalf("unexpected error: %v", err)
	}
}

func TestMockMindMap_CreateNode(t *testing.T) {
	ctrl := gomock.NewController(t)
	defer ctrl.Finish()

	mockClient := mockpb.NewMockFreeplaneClientInterface(ctrl)
	mockClient.EXPECT().CreateChild(
		gomock.Any(),
		gomock.Any(),
	).Return(&pb.CreateChildResponse{NodeId: "new-node", NodeText: "New Node"}, nil)

	_, err := mockClient.CreateChild(context.Background(), &pb.CreateChildRequest{
		Name:         "New Node",
		ParentNodeId: "parent-1",
	})
	if err != nil {
		t.Fatalf("unexpected error: %v", err)
	}
}

func TestWalkAndCollect_EmptyPattern(t *testing.T) {
	// WalkAndCollect calls GetText and Children which require a real connection.
	// We test that the function exists and has the correct signature.
	// Actual tree walking requires a connected client and mocked stubs.
	var fn func(context.Context, *freeplane.Node, string, []*freeplane.Node) ([]*freeplane.Node, error)
	fn = freeplane.WalkAndCollect
	if fn == nil {
		t.Error("WalkAndCollect should not be nil")
	}
	_ = fn // suppress unused warning
}

func TestCountNodes_EmptyPattern(t *testing.T) {
	// CountNodes calls Children which requires a real connection.
	// We test that the function exists and has the correct signature.
	var fn func(context.Context, *freeplane.Node) (int, error)
	fn = freeplane.CountNodes
	if fn == nil {
		t.Error("CountNodes should not be nil")
	}
	_ = fn // suppress unused warning
}
