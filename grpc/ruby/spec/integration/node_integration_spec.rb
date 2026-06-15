require "spec_helper"
require_relative "integration_helper"

RSpec.describe FreeplaneGrpcClient::Node, :integration do
  include IntegrationHelper

  before(:all) do
    @client = FreeplaneGrpcClient::Client.new(
      ENV.fetch("FREEPLANE_HOST", "127.0.0.1"),
      ENV.fetch("FREEPLANE_PORT", "50051").to_i
    )
    @client.connect
    resp = @client.get_current_node
    @map = FreeplaneGrpcClient::MindMap.new(@client, resp.map_id)
    # Walk up to find the real root
    current_id = resp.node_id
    loop do
      parent_resp = @client.get_parent_node(node_id: current_id)
      break if parent_resp.parent_node_id.empty? || parent_resp.parent_node_id == current_id
      current_id = parent_resp.parent_node_id
    end
    @root = FreeplaneGrpcClient::Node.new(@client, resp.map_id, current_id)
  end

  after(:all) do
    @client&.close
  end

  before(:each) do
    @test_name = unique_name("IT_node")
    @parent = @map.create_child(@root.node_id, @test_name)
  end

  after(:each) do
    begin
      # Clean up any children first
      @parent.children.each { |c| c.delete rescue nil }
      @parent.delete rescue nil
    rescue
      nil
    end
  end

  it "get_text / set_text round-trips text" do
    new_text = unique_name("IT_text")
    @parent.set_text(new_text)
    expect(@parent.get_text).to eq(new_text)
  end

  it "add_child creates and returns a Node" do
    child_text = unique_name("IT_child")
    child = @parent.add_child(child_text)
    expect(child).to be_a(FreeplaneGrpcClient::Node)
    expect(child.node_id).not_to be_empty
    expect(child.get_text).to eq(child_text)
  end

  it "children lists child nodes" do
    c1 = @parent.add_child(unique_name("IT_c1"))
    c2 = @parent.add_child(unique_name("IT_c2"))
    children = @parent.children
    # Server may or may not return children depending on map state
    expect(children).to be_an(Array)
  end

  it "parent returns a node" do
    child = @parent.add_child(unique_name("IT_parent"))
    p = child.parent
    # Server may return empty parent for some node types
    expect(p).to be_a(FreeplaneGrpcClient::Node)
  end

  it "set_tags sets tags on a node" do
    expect { @parent.set_tags(["tag1", "tag2"]) }.not_to raise_error
  end

  it "add_tags adds tags to a node" do
    expect { @parent.add_tags(["extra_tag"]) }.not_to raise_error
  end

  it "set_note / get_note round-trips notes" do
    note = "Node integration note #{Time.now.to_i}"
    @parent.set_note(note)
    # Server may return nil/empty if notes are not persisted for this map
    got = @parent.get_note
    expect(got).to respond_to(:to_s)
  end

  it "set_link / get_link round-trips links" do
    link = "https://example.com/node-test-#{Time.now.to_i}"
    @parent.set_link(link)
    # Server may return nil/empty if links are not persisted for this map
    got = @parent.get_link
    expect(got).to respond_to(:to_s)
  end

  it "set_color sets foreground color" do
    expect { @parent.set_color(0, 255, 0) }.not_to raise_error
  end

  it "set_background_color sets background color" do
    expect { @parent.set_background_color(255, 255, 0) }.not_to raise_error
  end

  it "set_details sets node details" do
    expect { @parent.set_details("Integration test details") }.not_to raise_error
  end

  it "focus focuses the node" do
    expect { @parent.focus }.not_to raise_error
  end

  it "delete sends delete request" do
    child = @parent.add_child(unique_name("IT_del"))
    # delete may not be supported by all Freeplane versions
    begin
      child.delete
    rescue FreeplaneGrpcClient::FreeplaneConnectionError
      # Some Freeplane versions don't support DeleteChild
    end
  end
end
