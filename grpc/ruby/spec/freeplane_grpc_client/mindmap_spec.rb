require "spec_helper"
require "ostruct"
require "freeplane_grpc_client/client"
require "freeplane_grpc_client/node"
require "freeplane_grpc_client/mindmap"

RSpec.describe FreeplaneGrpcClient::MindMap do
  let(:client) { double("FreeplaneGrpcClient::Client") }
  let(:map_id) { "m1" }
  let(:mindmap) { described_class.new(client, map_id) }

  describe "#initialize" do
    it "stores client and map_id" do
      expect(mindmap.client).to eq(client)
      expect(mindmap.map_id).to eq(map_id)
    end
  end

  describe "#root" do
    it "traverses up to find the root node" do
      child_node = double(FreeplaneGrpcClient::Node, node_id: "n1")
      parent_node = double(FreeplaneGrpcClient::Node, node_id: "p1", parent_node_id: "root")
      root_node = double(FreeplaneGrpcClient::Node, node_id: "root", parent_node_id: "root")

      allow(client).to receive(:get_current_node).and_return(OpenStruct.new(node_id: "n1"))
      allow(client).to receive(:get_parent_node).with(node_id: "n1").and_return(OpenStruct.new(parent_node_id: "p1"))
      allow(client).to receive(:get_parent_node).with(node_id: "p1").and_return(OpenStruct.new(parent_node_id: "root"))
      allow(client).to receive(:get_parent_node).with(node_id: "root").and_return(OpenStruct.new(parent_node_id: "root"))

      # Build the chain
      allow(FreeplaneGrpcClient::Node).to receive(:new).with(client, map_id, "n1").and_return(child_node)
      allow(FreeplaneGrpcClient::Node).to receive(:new).with(client, map_id, "p1").and_return(parent_node)
      allow(FreeplaneGrpcClient::Node).to receive(:new).with(client, map_id, "root").and_return(root_node)

      result = mindmap.root
      expect(result).to eq(root_node)
    end
  end

  describe "#selected_node" do
    it "calls get_current_node and returns a Node" do
      allow(client).to receive(:get_current_node).and_return(OpenStruct.new(node_id: "sel"))
      allow(FreeplaneGrpcClient::Node).to receive(:new).with(client, map_id, "sel")

      result = mindmap.selected_node
      expect(result).to be_a(FreeplaneGrpcClient::Node)
    end
  end

  describe "#find_nodes" do
    it "finds nodes by string pattern" do
      root_node = double(FreeplaneGrpcClient::Node, get_text: "Root", node_id: "root")
      child_node = double(FreeplaneGrpcClient::Node, get_text: "Hello World", node_id: "c1")

      allow(mindmap).to receive(:root).and_return(root_node)
      allow(root_node).to receive(:children).and_return([child_node])
      allow(child_node).to receive(:children).and_return([])

      results = mindmap.find_nodes("Hello")
      expect(results.length).to eq(1)
      expect(results[0].node_id).to eq("c1")
    end

    it "finds nodes by regex pattern" do
      root_node = double(FreeplaneGrpcClient::Node, get_text: "Root", node_id: "root")
      child_node = double(FreeplaneGrpcClient::Node, get_text: "Hello World", node_id: "c1")

      allow(mindmap).to receive(:root).and_return(root_node)
      allow(root_node).to receive(:children).and_return([child_node])
      allow(child_node).to receive(:children).and_return([])

      results = mindmap.find_nodes(/world/i)
      expect(results.length).to eq(1)
    end
  end

  describe "#info" do
    it "returns a hash with map_id" do
      expect(mindmap.info).to eq({ map_id: map_id })
    end
  end

  describe "#to_json" do
    it "calls mind_map_to_json and returns json string" do
      allow(client).to receive(:mind_map_to_json).and_return(OpenStruct.new(json: '{"root":{}}'))
      expect(mindmap.to_json).to eq('{"root":{}}')
    end
  end

  describe "#save" do
    it "returns true (no-op)" do
      expect(mindmap.save).to be true
    end
  end

  describe "#import_map" do
    it "calls open_map and returns self" do
      allow(client).to receive(:open_map)
      expect(mindmap.import_map("/path/to/map.mm")).to eq(mindmap)
    end
  end

  describe "#create_node" do
    it "calls create_child and returns a Node" do
      allow(client).to receive(:create_child).and_return(OpenStruct.new(node_id: "new"))
      allow(FreeplaneGrpcClient::Node).to receive(:new).with(client, map_id, "new")

      result = mindmap.create_node("New Node", "parent")
      expect(result).to be_a(FreeplaneGrpcClient::Node)
    end

    it "uses map_id as parent when no parent_id given" do
      allow(client).to receive(:create_child).and_return(OpenStruct.new(node_id: "new"))
      allow(FreeplaneGrpcClient::Node).to receive(:new).with(client, map_id, "new")

      mindmap.create_node("New Node")
      expect(client).to have_received(:create_child).with(name: "New Node", parent_node_id: map_id)
    end
  end

  describe "#create_child" do
    it "delegates to create_node" do
      allow(mindmap).to receive(:create_node).and_return(double(FreeplaneGrpcClient::Node))
      mindmap.create_child("parent", "Child")
      expect(mindmap).to have_received(:create_node).with("Child", "parent")
    end
  end
end
