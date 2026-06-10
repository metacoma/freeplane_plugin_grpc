require "spec_helper"
require "ostruct"
require "freeplane_grpc_client/client"
require "freeplane_grpc_client/node"

RSpec.describe FreeplaneGrpcClient::Node do
  let(:client) { double("FreeplaneGrpcClient::Client") }
  let(:map_id) { "m1" }
  let(:node_id) { "n1" }
  let(:node) { described_class.new(client, map_id, node_id) }

  describe "#initialize" do
    it "stores client, map_id, and node_id" do
      expect(node.client).to eq(client)
      expect(node.map_id).to eq(map_id)
      expect(node.node_id).to eq(node_id)
    end
  end

  describe "#get_text" do
    it "calls get_node_text and returns text" do
      allow(client).to receive(:get_node_text).with(node_id: node_id).and_return(OpenStruct.new(text: "Hello"))
      expect(node.get_text).to eq("Hello")
    end
  end

  describe "#set_text" do
    it "calls set_node_text and returns self" do
      allow(client).to receive(:set_node_text).with(node_id: node_id, text: "World")
      expect(node.set_text("World")).to eq(node)
    end
  end

  describe "#add_child" do
    it "calls create_child and returns a new Node" do
      allow(client).to receive(:create_child).with(name: "Child", parent_node_id: node_id).and_return(OpenStruct.new(node_id: "n2"))
      child = node.add_child("Child")
      expect(child).to be_a(described_class)
      expect(child.node_id).to eq("n2")
    end
  end

  describe "#children" do
    it "calls list_child_nodes and returns Node instances" do
      child1 = OpenStruct.new(node_id: "c1", text: "C1")
      child2 = OpenStruct.new(node_id: "c2", text: "C2")
      allow(client).to receive(:list_child_nodes).with(node_id: node_id).and_return(OpenStruct.new(children: [child1, child2]))
      children = node.children
      expect(children.length).to eq(2)
      expect(children[0]).to be_a(described_class)
      expect(children[1]).to be_a(described_class)
    end
  end

  describe "#parent" do
    it "calls get_parent_node and returns a Node" do
      allow(client).to receive(:get_parent_node).with(node_id: node_id).and_return(OpenStruct.new(parent_node_id: "p1"))
      parent = node.parent
      expect(parent).to be_a(described_class)
      expect(parent.node_id).to eq("p1")
    end
  end

  describe "#delete" do
    it "calls delete_child and returns self" do
      allow(client).to receive(:delete_child).with(node_id: node_id)
      expect(node.delete).to eq(node)
    end
  end

  describe "#move" do
    it "calls move_node and returns self" do
      allow(client).to receive(:move_node).with(node_id: node_id, new_parent_node_id: "p1")
      expect(node.move("p1")).to eq(node)
    end
  end

  describe "#set_color" do
    it "calls node_color_set" do
      allow(client).to receive(:node_color_set)
      expect(node.set_color(255, 0, 0)).to eq(node)
    end
  end

  describe "#set_background_color" do
    it "calls node_background_color_set" do
      allow(client).to receive(:node_background_color_set)
      expect(node.set_background_color(0, 255, 0)).to eq(node)
    end
  end

  describe "#get_note" do
    it "returns note when has_note is true" do
      allow(client).to receive(:get_node_note).and_return(OpenStruct.new(has_note: true, note: "A note"))
      expect(node.get_note).to eq("A note")
    end

    it "returns nil when has_note is false" do
      allow(client).to receive(:get_node_note).and_return(OpenStruct.new(has_note: false))
      expect(node.get_note).to be_nil
    end
  end

  describe "#set_note" do
    it "calls node_note_set and returns self" do
      allow(client).to receive(:node_note_set)
      expect(node.set_note("A note")).to eq(node)
    end
  end

  describe "#set_attribute" do
    it "calls node_attribute_add and returns self" do
      allow(client).to receive(:node_attribute_add)
      expect(node.set_attribute("label", "value")).to eq(node)
    end
  end

  describe "#get_link" do
    it "returns link when has_link is true" do
      allow(client).to receive(:get_node_link).and_return(OpenStruct.new(has_link: true, link: "http://example.com"))
      expect(node.get_link).to eq("http://example.com")
    end

    it "returns nil when has_link is false" do
      allow(client).to receive(:get_node_link).and_return(OpenStruct.new(has_link: false))
      expect(node.get_link).to be_nil
    end
  end

  describe "#set_link" do
    it "calls node_link_set and returns self" do
      allow(client).to receive(:node_link_set)
      expect(node.set_link("http://example.com")).to eq(node)
    end
  end

  describe "#set_tags" do
    it "calls node_tag_set and returns self" do
      allow(client).to receive(:node_tag_set)
      expect(node.set_tags(["tag1", "tag2"])).to eq(node)
    end
  end

  describe "#add_tags" do
    it "calls node_tag_add and returns self" do
      allow(client).to receive(:node_tag_add)
      expect(node.add_tags(["tag3"])).to eq(node)
    end
  end

  describe "#add_icon" do
    it "calls node_add_icon and returns self" do
      allow(client).to receive(:node_add_icon)
      expect(node.add_icon("star")).to eq(node)
    end
  end

  describe "#set_details" do
    it "calls node_details_set and returns self" do
      allow(client).to receive(:node_details_set)
      expect(node.set_details("details")).to eq(node)
    end
  end

  describe "#focus" do
    it "calls focus_node and returns self" do
      allow(client).to receive(:focus_node)
      expect(node.focus).to eq(node)
    end
  end

  describe "#select" do
    it "calls focus and returns self" do
      allow(client).to receive(:focus_node)
      expect(node.select).to eq(node)
    end
  end

  describe "#to_s" do
    it "returns a string representation" do
      allow(client).to receive(:get_node_text).and_return(OpenStruct.new(text: "Hello"))
      expect(node.to_s).to include("n1")
      expect(node.to_s).to include("Hello")
    end
  end

  describe "#==" do
    it "returns true for equal nodes" do
      other = described_class.new(client, map_id, node_id)
      expect(node == other).to be true
    end

    it "returns false for different nodes" do
      other = described_class.new(client, map_id, "n2")
      expect(node == other).to be false
    end
  end

  describe "#hash" do
    it "returns a hash based on map_id and node_id" do
      other = described_class.new(client, map_id, node_id)
      expect(node.hash).to eq(other.hash)
    end
  end
end
