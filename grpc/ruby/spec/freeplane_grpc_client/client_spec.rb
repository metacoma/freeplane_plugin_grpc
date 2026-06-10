require "spec_helper"
require "ostruct"
require "grpc"
require "google/protobuf"
require "freeplane_pb"
require "freeplane_services_pb"
require "freeplane_grpc_client/client"
require "freeplane_grpc_client/exceptions"

# Helper to look up proto message classes dynamically
def proto_message_class(name)
  Google::Protobuf::DescriptorPool.generated_pool.lookup("freeplane.#{name}").msgclass
end

RSpec.describe FreeplaneGrpcClient::Client do
  let(:host) { "127.0.0.1" }
  let(:port) { 50051 }
  let(:client) { described_class.new(host, port) }

  describe "#initialize" do
    it "sets default host and port" do
      c = described_class.new
      expect(c.host).to eq("127.0.0.1")
      expect(c.port).to eq(50051)
    end

    it "accepts custom host and port" do
      c = described_class.new("10.0.0.1", 9999)
      expect(c.host).to eq("10.0.0.1")
      expect(c.port).to eq(9999)
    end
  end

  describe "#connected?" do
    it "returns false before connect" do
      expect(client.connected?).to be false
    end
  end

  describe "#close" do
    it "clears channel and stub" do
      client.close
      expect(client.instance_variable_get(:@channel)).to be_nil
      expect(client.instance_variable_get(:@stub)).to be_nil
    end
  end

  # -- helper to build a mock stub ----------------------------------------

  def mock_stub(response_hash = {})
    OpenStruct.new(success: true, **response_hash)
  end

  def set_up_stub(method_name, response)
    stub = double("GRPC::ClientStub")
    allow(stub).to receive(method_name).and_return(->(*_args, **_kwargs) { response })
    stub
  end

  # -- helper to mock a proto message class -------------------------------

  def mock_proto_class(name)
    klass = Class.new do
      define_singleton_method(:new) { |**kwargs| OpenStruct.new(**kwargs) }
    end
    allow(described_class).to receive(:proto_message_class).with(name).and_return(klass)
  end

  # -- RPC wrapper tests (one per method) ---------------------------------

  describe "#create_child" do
    it "calls the stub and returns response" do
      resp = OpenStruct.new(success: true, node_id: "n1", node_text: "Child")
      stub = set_up_stub(:create_child, resp)

      client.instance_variable_set(:@channel, double("Channel"))
      client.instance_variable_set(:@stub, stub)

      result = client.create_child(name: "Child", parent_node_id: "parent")
      expect(result.node_id).to eq("n1")
      expect(result.node_text).to eq("Child")
    end

    it "raises OperationError on server failure" do
      resp = OpenStruct.new(success: false, error_message: "Parent not found")
      stub = set_up_stub(:create_child, resp)

      client.instance_variable_set(:@channel, double("Channel"))
      client.instance_variable_set(:@stub, stub)

      expect {
        client.create_child(name: "Child", parent_node_id: "bad")
      }.to raise_error(FreeplaneGrpcClient::OperationError, /Parent not found/)
    end
  end

  describe "#delete_child" do
    it "calls the stub and returns response" do
      resp = OpenStruct.new(success: true)
      stub = set_up_stub(:delete_child, resp)

      client.instance_variable_set(:@channel, double("Channel"))
      client.instance_variable_set(:@stub, stub)

      expect(client.delete_child(node_id: "n1").success).to be true
    end
  end

  describe "#node_attribute_add" do
    it "calls the stub and returns response" do
      resp = OpenStruct.new(success: true)
      stub = set_up_stub(:node_attribute_add, resp)

      client.instance_variable_set(:@channel, double("Channel"))
      client.instance_variable_set(:@stub, stub)

      expect(client.node_attribute_add(
        node_id: "n1", attribute_name: "label", attribute_value: "test"
      ).success).to be true
    end
  end

  describe "#node_link_set" do
    it "calls the stub and returns response" do
      resp = OpenStruct.new(success: true)
      stub = set_up_stub(:node_link_set, resp)

      client.instance_variable_set(:@channel, double("Channel"))
      client.instance_variable_set(:@stub, stub)

      expect(client.node_link_set(node_id: "n1", link: "http://example.com").success).to be true
    end
  end

  describe "#node_details_set" do
    it "calls the stub and returns response" do
      resp = OpenStruct.new(success: true)
      stub = set_up_stub(:node_details_set, resp)

      client.instance_variable_set(:@channel, double("Channel"))
      client.instance_variable_set(:@stub, stub)

      expect(client.node_details_set(node_id: "n1", details: "details").success).to be true
    end
  end

  describe "#node_note_set" do
    before { mock_proto_class("NodeNoteSetRequest") }
    it "calls the stub and returns response" do
      resp = OpenStruct.new(success: true)
      stub = set_up_stub(:node_note_set, resp)

      client.instance_variable_set(:@channel, double("Channel"))
      client.instance_variable_set(:@stub, stub)

      expect(client.node_note_set(node_id: "n1", note: "note text").success).to be true
    end
  end

  describe "#node_tag_set" do
    before { mock_proto_class("NodeTagSetRequest") }
    it "calls the stub and returns response" do
      resp = OpenStruct.new(success: true)
      stub = set_up_stub(:node_tag_set, resp)

      client.instance_variable_set(:@channel, double("Channel"))
      client.instance_variable_set(:@stub, stub)

      expect(client.node_tag_set(node_id: "n1", tags: ["tag1", "tag2"]).success).to be true
    end
  end

  describe "#node_tag_add" do
    before { mock_proto_class("NodeTagAddRequest") }
    it "calls the stub and returns response" do
      resp = OpenStruct.new(success: true)
      stub = set_up_stub(:node_tag_add, resp)

      client.instance_variable_set(:@channel, double("Channel"))
      client.instance_variable_set(:@stub, stub)

      expect(client.node_tag_add(node_id: "n1", tags: ["new_tag"]).success).to be true
    end
  end

  describe "#node_connect" do
    before { mock_proto_class("NodeConnectRequest") }
    it "calls the stub and returns response" do
      resp = OpenStruct.new(success: true)
      stub = set_up_stub(:node_connect, resp)

      client.instance_variable_set(:@channel, double("Channel"))
      client.instance_variable_set(:@stub, stub)

      expect(client.node_connect(
        source_node_id: "s1", target_node_id: "t1", relationship: "child"
      ).success).to be true
    end
  end

  describe "#node_add_icon" do
    before { mock_proto_class("NodeAddIconRequest") }
    it "calls the stub and returns response" do
      resp = OpenStruct.new(success: true)
      stub = set_up_stub(:node_add_icon, resp)

      client.instance_variable_set(:@channel, double("Channel"))
      client.instance_variable_set(:@stub, stub)

      expect(client.node_add_icon(node_id: "n1", icon_name: "star").success).to be true
    end
  end

  describe "#groovy" do
    it "calls the stub and returns response" do
      resp = OpenStruct.new(success: true, result: "42", error_message: "")
      stub = set_up_stub(:groovy, resp)

      client.instance_variable_set(:@channel, double("Channel"))
      client.instance_variable_set(:@stub, stub)

      result = client.groovy(groovy_code: "1 + 1")
      expect(result.success).to be true
      expect(result.result).to eq("42")
    end
  end

  describe "#node_color_set" do
    it "calls the stub and returns response" do
      resp = OpenStruct.new(success: true)
      stub = set_up_stub(:node_color_set, resp)

      client.instance_variable_set(:@channel, double("Channel"))
      client.instance_variable_set(:@stub, stub)

      expect(client.node_color_set(
        node_id: "n1", red: 255, green: 0, blue: 0
      ).success).to be true
    end

    it "uses default alpha of 255" do
      resp = OpenStruct.new(success: true)
      stub = set_up_stub(:node_color_set, resp)

      client.instance_variable_set(:@channel, double("Channel"))
      client.instance_variable_set(:@stub, stub)

      expect(client.node_color_set(
        node_id: "n1", red: 255, green: 0, blue: 0
      ).success).to be true
    end
  end

  describe "#node_background_color_set" do
    it "calls the stub and returns response" do
      resp = OpenStruct.new(success: true)
      stub = set_up_stub(:node_background_color_set, resp)

      client.instance_variable_set(:@channel, double("Channel"))
      client.instance_variable_set(:@stub, stub)

      expect(client.node_background_color_set(
        node_id: "n1", red: 0, green: 255, blue: 0
      ).success).to be true
    end
  end

  describe "#status_info_set" do
    it "calls the stub and returns response" do
      resp = OpenStruct.new(success: true)
      stub = set_up_stub(:status_info_set, resp)

      client.instance_variable_set(:@channel, double("Channel"))
      client.instance_variable_set(:@stub, stub)

      expect(client.status_info_set(status_info: "hello").success).to be true
    end
  end

  describe "#text_fsm" do
    it "calls the stub and returns response" do
      resp = OpenStruct.new(success: true)
      stub = set_up_stub(:text_fsm, resp)

      client.instance_variable_set(:@channel, double("Channel"))
      client.instance_variable_set(:@stub, stub)

      expect(client.text_fsm(json: '{"template": "test"}').success).to be true
    end
  end

  describe "#mind_map_from_json" do
    it "calls the stub and returns response" do
      resp = OpenStruct.new(success: true)
      stub = set_up_stub(:mind_map_from_json, resp)

      client.instance_variable_set(:@channel, double("Channel"))
      client.instance_variable_set(:@stub, stub)

      expect(client.mind_map_from_json(json: '{"root":{}}').success).to be true
    end
  end

  describe "#mind_map_to_json" do
    it "calls the stub and returns response" do
      resp = OpenStruct.new(success: true, json: '{"root":{}}')
      stub = set_up_stub(:mind_map_to_json, resp)

      client.instance_variable_set(:@channel, double("Channel"))
      client.instance_variable_set(:@stub, stub)

      result = client.mind_map_to_json
      expect(result.success).to be true
      expect(result.json).to eq('{"root":{}}')
    end
  end

  describe "#get_current_node" do
    before { mock_proto_class("GetCurrentNodeRequest") }
    it "calls the stub and returns response" do
      resp = OpenStruct.new(success: true, map_id: "m1", node_id: "n1")
      stub = set_up_stub(:get_current_node, resp)

      client.instance_variable_set(:@channel, double("Channel"))
      client.instance_variable_set(:@stub, stub)

      result = client.get_current_node
      expect(result.success).to be true
      expect(result.map_id).to eq("m1")
    end
  end

  describe "#open_map" do
    before { mock_proto_class("OpenMapRequest") }
    it "calls the stub and returns response" do
      resp = OpenStruct.new(success: true)
      stub = set_up_stub(:open_map, resp)

      client.instance_variable_set(:@channel, double("Channel"))
      client.instance_variable_set(:@stub, stub)

      expect(client.open_map(file_path: "/path/to/map.mm").success).to be true
    end
  end

  describe "#focus_node" do
    before { mock_proto_class("FocusNodeRequest") }
    it "calls the stub and returns response" do
      resp = OpenStruct.new(success: true)
      stub = set_up_stub(:focus_node, resp)

      client.instance_variable_set(:@channel, double("Channel"))
      client.instance_variable_set(:@stub, stub)

      expect(client.focus_node(node_id: "n1").success).to be true
    end
  end

  describe "#get_node_text" do
    before { mock_proto_class("GetNodeTextRequest") }
    it "calls the stub and returns response" do
      resp = OpenStruct.new(success: true, node_id: "n1", text: "Hello")
      stub = set_up_stub(:get_node_text, resp)

      client.instance_variable_set(:@channel, double("Channel"))
      client.instance_variable_set(:@stub, stub)

      result = client.get_node_text(node_id: "n1")
      expect(result.text).to eq("Hello")
    end
  end

  describe "#get_parent_node" do
    before { mock_proto_class("GetParentNodeRequest") }
    it "calls the stub and returns response" do
      resp = OpenStruct.new(
        success: true, node_id: "n1",
        parent_node_id: "p1", parent_node_text: "Parent"
      )
      stub = set_up_stub(:get_parent_node, resp)

      client.instance_variable_set(:@channel, double("Channel"))
      client.instance_variable_set(:@stub, stub)

      result = client.get_parent_node(node_id: "n1")
      expect(result.parent_node_id).to eq("p1")
    end
  end

  describe "#list_child_nodes" do
    before { mock_proto_class("ListChildNodesRequest") }
    it "calls the stub and returns response" do
      child1 = OpenStruct.new(node_id: "c1", text: "Child 1")
      child2 = OpenStruct.new(node_id: "c2", text: "Child 2")
      resp = OpenStruct.new(success: true, children: [child1, child2])
      stub = set_up_stub(:list_child_nodes, resp)

      client.instance_variable_set(:@channel, double("Channel"))
      client.instance_variable_set(:@stub, stub)

      result = client.list_child_nodes(node_id: "p1")
      expect(result.children.length).to eq(2)
    end
  end

  describe "#get_node_note" do
    before { mock_proto_class("GetNodeNoteRequest") }
    it "calls the stub and returns response" do
      resp = OpenStruct.new(success: true, node_id: "n1", note: "Note text", has_note: true)
      stub = set_up_stub(:get_node_note, resp)

      client.instance_variable_set(:@channel, double("Channel"))
      client.instance_variable_set(:@stub, stub)

      result = client.get_node_note(node_id: "n1")
      expect(result.note).to eq("Note text")
    end
  end

  describe "#get_node_link" do
    before { mock_proto_class("GetNodeLinkRequest") }
    it "calls the stub and returns response" do
      resp = OpenStruct.new(success: true, node_id: "n1", link: "http://example.com", has_link: true)
      stub = set_up_stub(:get_node_link, resp)

      client.instance_variable_set(:@channel, double("Channel"))
      client.instance_variable_set(:@stub, stub)

      result = client.get_node_link(node_id: "n1")
      expect(result.link).to eq("http://example.com")
    end
  end

  describe "#set_node_text" do
    before { mock_proto_class("SetNodeTextRequest") }
    it "calls the stub and returns response" do
      resp = OpenStruct.new(success: true, node_id: "n1")
      stub = set_up_stub(:set_node_text, resp)

      client.instance_variable_set(:@channel, double("Channel"))
      client.instance_variable_set(:@stub, stub)

      result = client.set_node_text(node_id: "n1", text: "New text")
      expect(result.success).to be true
    end
  end

  describe "#move_node" do
    before { mock_proto_class("MoveNodeRequest") }
    it "calls the stub and returns response" do
      resp = OpenStruct.new(success: true)
      stub = set_up_stub(:move_node, resp)

      client.instance_variable_set(:@channel, double("Channel"))
      client.instance_variable_set(:@stub, stub)

      result = client.move_node(node_id: "n1", new_parent_node_id: "p1")
      expect(result.success).to be true
    end
  end

  # -- timeout support ----------------------------------------------------

  describe "timeout support" do
    it "passes timeout to the stub method" do
      resp = OpenStruct.new(success: true)
      stub = set_up_stub(:create_child, resp)

      client.instance_variable_set(:@channel, double("Channel"))
      client.instance_variable_set(:@stub, stub)

      client.create_child(name: "Child", parent_node_id: "", timeout: 5.0)

      expect(stub).to have_received(:create_child).with(
        hash_including, timeout: 5.0
      ).at_least(:once)
    end
  end

  # -- error handling -----------------------------------------------------

  describe "error handling" do
    it "raises ConnectionError on UNAVAILABLE gRPC error" do
      stub = double("GRPC::ClientStub")
      grpc_error = GRPC::BadStatus.new(:unavailable, "Server unavailable")
      allow(stub).to receive(:create_child).and_raise(grpc_error)

      client.instance_variable_set(:@channel, double("Channel"))
      client.instance_variable_set(:@stub, stub)

      expect {
        client.create_child(name: "Child", parent_node_id: "")
      }.to raise_error(FreeplaneGrpcClient::ConnectionError, /gRPC call failed/)
    end

    it "raises OperationError on non-connection gRPC error" do
      stub = double("GRPC::ClientStub")
      grpc_error = GRPC::BadStatus.new(:not_found, "Resource not found")
      allow(stub).to receive(:create_child).and_raise(grpc_error)

      client.instance_variable_set(:@channel, double("Channel"))
      client.instance_variable_set(:@stub, stub)

      expect {
        client.create_child(name: "Child", parent_node_id: "")
      }.to raise_error(FreeplaneGrpcClient::OperationError)
    end

    it "raises OperationError on generic error" do
      stub = double("GRPC::ClientStub")
      generic_error = StandardError.new("Something went wrong")
      allow(stub).to receive(:create_child).and_raise(generic_error)

      client.instance_variable_set(:@channel, double("Channel"))
      client.instance_variable_set(:@stub, stub)

      expect {
        client.create_child(name: "Child", parent_node_id: "")
      }.to raise_error(FreeplaneGrpcClient::ConnectionError)
    end

    it "raises OperationError when success is false with no error_message" do
      resp = OpenStruct.new(success: false)
      stub = set_up_stub(:delete_child, resp)

      client.instance_variable_set(:@channel, double("Channel"))
      client.instance_variable_set(:@stub, stub)

      expect {
        client.delete_child(node_id: "n1")
      }.to raise_error(FreeplaneGrpcClient::OperationError, /Operation failed/)
    end
  end
end
