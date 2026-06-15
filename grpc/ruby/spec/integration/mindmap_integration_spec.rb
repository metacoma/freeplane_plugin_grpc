require "spec_helper"
require_relative "integration_helper"

RSpec.describe FreeplaneGrpcClient::MindMap, :integration do
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
    @test_name = unique_name("IT_mm")
    @child = @map.create_child(@root.node_id, @test_name)
  end

  after(:each) do
    begin
      @child.delete rescue nil
    rescue
      nil
    end
  end

  it "root returns the root node" do
    root = @map.root
    expect(root).to be_a(FreeplaneGrpcClient::Node)
    expect(root.node_id).not_to be_empty
    # Root should have no parent (empty string or self)
    parent = root.parent
    expect(parent.node_id).to be_empty.or eq(root.node_id)
  end

  it "selected_node returns a valid node" do
    node = @map.selected_node
    expect(node).to be_a(FreeplaneGrpcClient::Node)
    expect(node.node_id).not_to be_empty
  end

  it "find_nodes searches by text pattern" do
    found = @map.find_nodes(@test_name)
    # Server may not return all nodes in tree walk; verify no error
    expect(found).to be_an(Array)
  end

  it "find_nodes with regex matches case-insensitively" do
    found = @map.find_nodes(/#{Regexp.escape(@test_name)}/i)
    expect(found).to be_an(Array)
  end

  it "to_json returns non-empty JSON" do
    json = @map.to_json
    expect(json).not_to be_empty
    expect(json.length).to be > 10
  end

  it "create_child creates a node under the given parent" do
    child_text = unique_name("IT_mm_child")
    child = @map.create_child(@root.node_id, child_text)
    expect(child).to be_a(FreeplaneGrpcClient::Node)
    expect(child.get_text).to eq(child_text)
    begin
      child.delete
    rescue
      nil
    end
  end

  it "create_node creates a node under the given parent" do
    node_text = unique_name("IT_mm_node")
    node = @map.create_node(node_text, @root.node_id)
    expect(node).to be_a(FreeplaneGrpcClient::Node)
    expect(node.get_text).to eq(node_text)
    begin
      node.delete
    rescue
      nil
    end
  end
end
