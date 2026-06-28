package tests

import (
	"context"
	"testing"
	"time"

	freeplane "github.com/metacoma/freeplane_plugin_grpc/grpc/golang"
	"go.uber.org/mock/gomock"
	mockpb "github.com/metacoma/freeplane_plugin_grpc/grpc/golang/internal/mock"
	pb "github.com/metacoma/freeplane_plugin_grpc/grpc/golang/pb"
)

func TestNewFreeplaneClient_Defaults(t *testing.T) {
	c := freeplane.NewFreeplaneClient("", 0)
	if c.Host() != "127.0.0.1" {
		t.Errorf("expected default host 127.0.0.1, got %s", c.Host())
	}
	if c.Port() != 50051 {
		t.Errorf("expected default port 50051, got %d", c.Port())
	}
}

func TestNewFreeplaneClient_Custom(t *testing.T) {
	c := freeplane.NewFreeplaneClient("10.0.0.1", 9999)
	if c.Host() != "10.0.0.1" {
		t.Errorf("expected host 10.0.0.1, got %s", c.Host())
	}
	if c.Port() != 9999 {
		t.Errorf("expected port 9999, got %d", c.Port())
	}
}

func TestCheckSuccess_Success(t *testing.T) {
	resp := &pb.DeleteChildResponse{Success: true}
	err := freeplane.CheckSuccess(resp)
	if err != nil {
		t.Errorf("expected no error for success=true, got %v", err)
	}
}

func TestCheckSuccess_Failure(t *testing.T) {
	resp := &pb.GroovyResponse{Success: false, ErrorMessage: "node not found"}
	err := freeplane.CheckSuccess(resp)
	if err == nil {
		t.Fatal("expected error for success=false")
	}
	if !freeplane.IsOperationError(err) {
		t.Errorf("expected OperationError, got %T", err)
	}
}

func TestCheckSuccess_CreateChild_NoID(t *testing.T) {
	resp := &pb.CreateChildResponse{NodeId: "", NodeText: ""}
	err := freeplane.CheckSuccess(resp)
	if err == nil {
		t.Fatal("expected error for empty node_id")
	}
	if !freeplane.IsOperationError(err) {
		t.Errorf("expected OperationError, got %T", err)
	}
}

func TestDo(t *testing.T) {
	c := freeplane.NewFreeplaneClient("127.0.0.1", 50051)
	err := c.Do(func(client *freeplane.FreeplaneClient) error {
		// Do doesn't auto-connect, just calls the function
		return nil
	})
	if err != nil {
		t.Errorf("expected no error from Do, got %v", err)
	}
}

func TestMockClient_CreateChild_Success(t *testing.T) {
	ctrl := gomock.NewController(t)
	defer ctrl.Finish()

	mockClient := mockpb.NewMockFreeplaneClientInterface(ctrl)
	mockClient.EXPECT().CreateChild(
		gomock.Any(),
		gomock.Any(),
	).Return(&pb.CreateChildResponse{
		NodeId:   "node-123",
		NodeText: "Hello",
	}, nil)

	// We can't directly test the FreeplaneClient.call with a mock because
	// FreeplaneClient holds a concrete stub. Instead, we verify the mock
	// interface works correctly.
	resp, err := mockClient.CreateChild(context.Background(), &pb.CreateChildRequest{
		Name:         "Hello",
		ParentNodeId: "",
	})
	if err != nil {
		t.Fatalf("unexpected error: %v", err)
	}
	if resp.GetNodeId() != "node-123" {
		t.Errorf("expected node_id node-123, got %s", resp.GetNodeId())
	}
}

func TestMockClient_GetCurrentNode_Success(t *testing.T) {
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

	resp, err := mockClient.GetCurrentNode(context.Background(), &pb.GetCurrentNodeRequest{})
	if err != nil {
		t.Fatalf("unexpected error: %v", err)
	}
	if !resp.GetSuccess() {
		t.Error("expected success=true")
	}
}

func TestMockClient_GetNodeText_Success(t *testing.T) {
	ctrl := gomock.NewController(t)
	defer ctrl.Finish()

	mockClient := mockpb.NewMockFreeplaneClientInterface(ctrl)
	mockClient.EXPECT().GetNodeText(
		gomock.Any(),
		gomock.Any(),
	).Return(&pb.GetNodeTextResponse{
		Success: true,
		NodeId:  "node-1",
		Text:    "Root node",
	}, nil)

	resp, err := mockClient.GetNodeText(context.Background(), &pb.GetNodeTextRequest{
		NodeId: "node-1",
	})
	if err != nil {
		t.Fatalf("unexpected error: %v", err)
	}
	if resp.GetText() != "Root node" {
		t.Errorf("expected text 'Root node', got %s", resp.GetText())
	}
}

func TestMockClient_GetParentNode_NoParent(t *testing.T) {
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

	resp, err := mockClient.GetParentNode(context.Background(), &pb.GetParentNodeRequest{
		NodeId: "root-node",
	})
	if err != nil {
		t.Fatalf("unexpected error: %v", err)
	}
	if resp.GetSuccess() {
		t.Error("expected success=false for root node")
	}
}

func TestMockClient_ListChildNodes(t *testing.T) {
	ctrl := gomock.NewController(t)
	defer ctrl.Finish()

	mockClient := mockpb.NewMockFreeplaneClientInterface(ctrl)
	mockClient.EXPECT().ListChildNodes(
		gomock.Any(),
		gomock.Any(),
	).Return(&pb.ListChildNodesResponse{
		Success: true,
		Children: []*pb.ChildNodeInfo{
			{NodeId: "child-1", Text: "Child 1"},
			{NodeId: "child-2", Text: "Child 2"},
		},
	}, nil)

	resp, err := mockClient.ListChildNodes(context.Background(), &pb.ListChildNodesRequest{
		NodeId: "parent-1",
	})
	if err != nil {
		t.Fatalf("unexpected error: %v", err)
	}
	if len(resp.GetChildren()) != 2 {
		t.Errorf("expected 2 children, got %d", len(resp.GetChildren()))
	}
}

func TestMockClient_Groovy_Success(t *testing.T) {
	ctrl := gomock.NewController(t)
	defer ctrl.Finish()

	mockClient := mockpb.NewMockFreeplaneClientInterface(ctrl)
	mockClient.EXPECT().Groovy(
		gomock.Any(),
		gomock.Any(),
	).Return(&pb.GroovyResponse{
		Success: true,
		Result:  "groovy result",
	}, nil)

	resp, err := mockClient.Groovy(context.Background(), &pb.GroovyRequest{
		GroovyCode: "test code",
	})
	if err != nil {
		t.Fatalf("unexpected error: %v", err)
	}
	if resp.GetResult() != "groovy result" {
		t.Errorf("expected result 'groovy result', got %s", resp.GetResult())
	}
}

func TestMockClient_All27Methods(t *testing.T) {
	// Verify that all 27 RPC methods are available on the interface
	ctrl := gomock.NewController(t)
	defer ctrl.Finish()

	mockClient := mockpb.NewMockFreeplaneClientInterface(ctrl)
	ctx := context.Background()

	checkBoolResp := func(name string, resp interface{ GetSuccess() bool }) {
		if !resp.GetSuccess() {
			t.Error(name, "failed")
		}
	}

	// 1. CreateChild
	mockClient.EXPECT().CreateChild(ctx, gomock.Any()).Return(&pb.CreateChildResponse{NodeId: "1"}, nil)
	r1, _ := mockClient.CreateChild(ctx, &pb.CreateChildRequest{Name: "test", ParentNodeId: ""})
	if r1.GetNodeId() != "1" {
		t.Error("CreateChild failed")
	}

	// 2. DeleteChild
	mockClient.EXPECT().DeleteChild(ctx, gomock.Any()).Return(&pb.DeleteChildResponse{Success: true}, nil)
	r2, _ := mockClient.DeleteChild(ctx, &pb.DeleteChildRequest{NodeId: "1"})
	checkBoolResp("DeleteChild", r2)

	// 3. NodeAttributeAdd
	mockClient.EXPECT().NodeAttributeAdd(ctx, gomock.Any()).Return(&pb.NodeAttributeAddResponse{Success: true}, nil)
	r3, _ := mockClient.NodeAttributeAdd(ctx, &pb.NodeAttributeAddRequest{NodeId: "1", AttributeName: "k", AttributeValue: "v"})
	checkBoolResp("NodeAttributeAdd", r3)

	// 4. NodeLinkSet
	mockClient.EXPECT().NodeLinkSet(ctx, gomock.Any()).Return(&pb.NodeLinkSetResponse{Success: true}, nil)
	r4, _ := mockClient.NodeLinkSet(ctx, &pb.NodeLinkSetRequest{NodeId: "1", Link: "http://example.com"})
	checkBoolResp("NodeLinkSet", r4)

	// 5. NodeDetailsSet
	mockClient.EXPECT().NodeDetailsSet(ctx, gomock.Any()).Return(&pb.NodeDetailsSetResponse{Success: true}, nil)
	r5, _ := mockClient.NodeDetailsSet(ctx, &pb.NodeDetailsSetRequest{NodeId: "1", Details: "details"})
	checkBoolResp("NodeDetailsSet", r5)

	// 6. NodeNoteSet
	mockClient.EXPECT().NodeNoteSet(ctx, gomock.Any()).Return(&pb.NodeNoteSetResponse{Success: true}, nil)
	r6, _ := mockClient.NodeNoteSet(ctx, &pb.NodeNoteSetRequest{NodeId: "1", Note: "note"})
	checkBoolResp("NodeNoteSet", r6)

	// 7. NodeTagSet
	mockClient.EXPECT().NodeTagSet(ctx, gomock.Any()).Return(&pb.NodeTagSetResponse{Success: true}, nil)
	r7, _ := mockClient.NodeTagSet(ctx, &pb.NodeTagSetRequest{NodeId: "1", Tags: []string{"tag1"}})
	checkBoolResp("NodeTagSet", r7)

	// 8. NodeTagAdd
	mockClient.EXPECT().NodeTagAdd(ctx, gomock.Any()).Return(&pb.NodeTagAddResponse{Success: true}, nil)
	r8, _ := mockClient.NodeTagAdd(ctx, &pb.NodeTagAddRequest{NodeId: "1", Tags: []string{"tag1"}})
	checkBoolResp("NodeTagAdd", r8)

	// 9. NodeConnect
	mockClient.EXPECT().NodeConnect(ctx, gomock.Any()).Return(&pb.NodeConnectResponse{Success: true}, nil)
	r9, _ := mockClient.NodeConnect(ctx, &pb.NodeConnectRequest{SourceNodeId: "1", TargetNodeId: "2", Relationship: "rel"})
	checkBoolResp("NodeConnect", r9)

	// 10. NodeAddIcon
	mockClient.EXPECT().NodeAddIcon(ctx, gomock.Any()).Return(&pb.NodeAddIconResponse{Success: true}, nil)
	r10, _ := mockClient.NodeAddIcon(ctx, &pb.NodeAddIconRequest{NodeId: "1", IconName: "icon"})
	checkBoolResp("NodeAddIcon", r10)

	// 11. Groovy
	mockClient.EXPECT().Groovy(ctx, gomock.Any()).Return(&pb.GroovyResponse{Success: true, Result: "ok"}, nil)
	r11, _ := mockClient.Groovy(ctx, &pb.GroovyRequest{GroovyCode: "code"})
	checkBoolResp("Groovy", r11)

	// 12. NodeColorSet
	mockClient.EXPECT().NodeColorSet(ctx, gomock.Any()).Return(&pb.NodeColorSetResponse{Success: true}, nil)
	r12, _ := mockClient.NodeColorSet(ctx, &pb.NodeColorSetRequest{NodeId: "1", Red: 255, Green: 0, Blue: 0, Alpha: 255})
	checkBoolResp("NodeColorSet", r12)

	// 13. NodeBackgroundColorSet
	mockClient.EXPECT().NodeBackgroundColorSet(ctx, gomock.Any()).Return(&pb.NodeBackgroundColorSetResponse{Success: true}, nil)
	r13, _ := mockClient.NodeBackgroundColorSet(ctx, &pb.NodeBackgroundColorSetRequest{NodeId: "1", Red: 0, Green: 255, Blue: 0, Alpha: 255})
	checkBoolResp("NodeBackgroundColorSet", r13)

	// 14. StatusInfoSet
	mockClient.EXPECT().StatusInfoSet(ctx, gomock.Any()).Return(&pb.StatusInfoSetResponse{Success: true}, nil)
	r14, _ := mockClient.StatusInfoSet(ctx, &pb.StatusInfoSetRequest{StatusInfo: "info"})
	checkBoolResp("StatusInfoSet", r14)

	// 15. TextFSM
	mockClient.EXPECT().TextFSM(ctx, gomock.Any()).Return(&pb.TextFSMResponse{Success: true}, nil)
	r15, _ := mockClient.TextFSM(ctx, &pb.TextFSMRequest{Json: "{}"})
	checkBoolResp("TextFSM", r15)

	// 16. MindMapFromJSON
	mockClient.EXPECT().MindMapFromJSON(ctx, gomock.Any()).Return(&pb.MindMapFromJSONResponse{Success: true}, nil)
	r16, _ := mockClient.MindMapFromJSON(ctx, &pb.MindMapFromJSONRequest{Json: "{}"})
	checkBoolResp("MindMapFromJSON", r16)

	// 17. MindMapToJSON
	mockClient.EXPECT().MindMapToJSON(ctx, gomock.Any()).Return(&pb.MindMapToJSONResponse{Success: true, Json: "{}"}, nil)
	r17, _ := mockClient.MindMapToJSON(ctx, &pb.MindMapToJSONRequest{})
	checkBoolResp("MindMapToJSON", r17)

	// 18. GetCurrentNode
	mockClient.EXPECT().GetCurrentNode(ctx, gomock.Any()).Return(&pb.GetCurrentNodeResponse{Success: true, MapId: "m", NodeId: "n"}, nil)
	r18, _ := mockClient.GetCurrentNode(ctx, &pb.GetCurrentNodeRequest{})
	checkBoolResp("GetCurrentNode", r18)

	// 19. OpenMap
	mockClient.EXPECT().OpenMap(ctx, gomock.Any()).Return(&pb.OpenMapResponse{Success: true}, nil)
	r19, _ := mockClient.OpenMap(ctx, &pb.OpenMapRequest{FilePath: "/test.mm"})
	checkBoolResp("OpenMap", r19)

	// 20. FocusNode
	mockClient.EXPECT().FocusNode(ctx, gomock.Any()).Return(&pb.FocusNodeResponse{Success: true}, nil)
	r20, _ := mockClient.FocusNode(ctx, &pb.FocusNodeRequest{NodeId: "1"})
	checkBoolResp("FocusNode", r20)

	// 21. GetNodeText
	mockClient.EXPECT().GetNodeText(ctx, gomock.Any()).Return(&pb.GetNodeTextResponse{Success: true, Text: "text"}, nil)
	r21, _ := mockClient.GetNodeText(ctx, &pb.GetNodeTextRequest{NodeId: "1"})
	checkBoolResp("GetNodeText", r21)

	// 22. GetParentNode
	mockClient.EXPECT().GetParentNode(ctx, gomock.Any()).Return(&pb.GetParentNodeResponse{Success: true, ParentNodeId: "p"}, nil)
	r22, _ := mockClient.GetParentNode(ctx, &pb.GetParentNodeRequest{NodeId: "1"})
	checkBoolResp("GetParentNode", r22)

	// 23. ListChildNodes
	mockClient.EXPECT().ListChildNodes(ctx, gomock.Any()).Return(&pb.ListChildNodesResponse{Success: true}, nil)
	r23, _ := mockClient.ListChildNodes(ctx, &pb.ListChildNodesRequest{NodeId: "1"})
	checkBoolResp("ListChildNodes", r23)

	// 24. GetNodeNote
	mockClient.EXPECT().GetNodeNote(ctx, gomock.Any()).Return(&pb.GetNodeNoteResponse{Success: true, HasNote: true, Note: "note"}, nil)
	r24, _ := mockClient.GetNodeNote(ctx, &pb.GetNodeNoteRequest{NodeId: "1"})
	checkBoolResp("GetNodeNote", r24)

	// 25. GetNodeLink
	mockClient.EXPECT().GetNodeLink(ctx, gomock.Any()).Return(&pb.GetNodeLinkResponse{Success: true, HasLink: true, Link: "link"}, nil)
	r25, _ := mockClient.GetNodeLink(ctx, &pb.GetNodeLinkRequest{NodeId: "1"})
	checkBoolResp("GetNodeLink", r25)

	// 26. SetNodeText
	mockClient.EXPECT().SetNodeText(ctx, gomock.Any()).Return(&pb.SetNodeTextResponse{Success: true}, nil)
	r26, _ := mockClient.SetNodeText(ctx, &pb.SetNodeTextRequest{NodeId: "1", Text: "text"})
	checkBoolResp("SetNodeText", r26)

	// 27. MoveNode
	mockClient.EXPECT().MoveNode(ctx, gomock.Any()).Return(&pb.MoveNodeResponse{Success: true}, nil)
	r27, _ := mockClient.MoveNode(ctx, &pb.MoveNodeRequest{NodeId: "1", NewParentNodeId: "2"})
	checkBoolResp("MoveNode", r27)
}

func TestDefaultTimeout(t *testing.T) {
	// Verify the default timeout constant is 30 seconds
	if freeplane.DefaultTimeout != 30*time.Second {
		t.Errorf("expected default timeout 30s, got %v", freeplane.DefaultTimeout)
	}
}
