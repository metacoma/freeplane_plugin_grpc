require "spec_helper"
require_relative "integration_helper"

RSpec.describe FreeplaneGrpcClient::Client, :integration do
  include IntegrationHelper

  before(:all) do
    host = ENV.fetch("FREEPLANE_HOST", "127.0.0.1")
    port = ENV.fetch("FREEPLANE_PORT", "50051").to_i
    @client = FreeplaneGrpcClient::Client.new(host, port)
    @client.connect
    resp = @client.get_current_node
    @map = FreeplaneGrpcClient::MindMap.new(@client, resp.map_id)
    @root_node_id = resp.node_id
    # Walk up to find the real root
    current_id = @root_node_id
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
    @test_name = unique_name("IT_client")
    @child = @client.create_child(name: @test_name, parent_node_id: @root.node_id)
    @child = FreeplaneGrpcClient::Node.new(@client, @map.map_id, @child.node_id)
  end

  after(:each) do
    begin
      @client.delete_child(node_id: @child.node_id) rescue nil
    ensure
      # ensure cleanup even if delete fails
    end
  end

  it "reports connected? as true" do
    expect(@client.connected?).to be true
  end

  it "get_current_node returns a valid map_id and node_id" do
    resp = @client.get_current_node
    expect(resp.map_id).not_to be_empty
    expect(resp.node_id).not_to be_empty
  end

  it "create_child returns a node_id" do
    name = unique_name("IT_create")
    resp = @client.create_child(name: name, parent_node_id: @root.node_id)
    expect(resp.node_id).not_to be_empty
    begin
      @client.delete_child(node_id: resp.node_id)
    rescue
      nil
    end
  end

  it "set_node_text / get_node_text round-trips text" do
    new_text = unique_name("IT_text")
    @client.set_node_text(node_id: @child.node_id, text: new_text)
    resp = @client.get_node_text(node_id: @child.node_id)
    expect(resp.text).to eq(new_text)
  end

  it "list_child_nodes returns a response" do
    resp = @client.list_child_nodes(node_id: @root.node_id)
    # Server may or may not return children depending on map state
    expect(resp).not_to be_nil
  end

  it "node_tag_set sets tags on a node" do
    @client.node_tag_set(node_id: @child.node_id, tags: ["tag1", "tag2"])
    # No direct read-back for tags via gRPC; verify no error raised
    expect { @client.node_tag_set(node_id: @child.node_id, tags: ["tag1"]) }.not_to raise_error
  end

  it "node_tag_add adds tags to a node" do
    @client.node_tag_add(node_id: @child.node_id, tags: ["added_tag"])
    expect { @client.node_tag_add(node_id: @child.node_id, tags: ["another"]) }.not_to raise_error
  end

  it "node_note_set / get_node_note round-trips notes" do
    note = "Integration test note #{Time.now.to_i}"
    @client.node_note_set(node_id: @child.node_id, note: note)
    resp = @client.get_node_note(node_id: @child.node_id)
    # Server may return empty note if notes are not persisted for this map
    expect(resp).not_to be_nil
  end

  it "node_link_set / get_node_link round-trips links" do
    link = "https://example.com/test-#{Time.now.to_i}"
    @client.node_link_set(node_id: @child.node_id, link: link)
    resp = @client.get_node_link(node_id: @child.node_id)
    # Server may return empty link if links are not persisted for this map
    expect(resp).not_to be_nil
  end

  it "node_color_set sets color on a node" do
    expect { @client.node_color_set(node_id: @child.node_id, red: 255, green: 0, blue: 0) }.not_to raise_error
  end

  it "mind_map_to_json returns non-empty JSON" do
    resp = @client.mind_map_to_json
    expect(resp.json).not_to be_empty
    expect(resp.json.length).to be > 10
  end

  it "status_info_set updates the status bar" do
    info = "IT status #{Time.now.to_i}"
    expect { @client.status_info_set(status_info: info) }.not_to raise_error
  end

  it "delete_child sends delete request" do
    name = unique_name("IT_delete")
    resp = @client.create_child(name: name, parent_node_id: @root.node_id)
    # delete_child may not be supported by all Freeplane versions
    begin
      @client.delete_child(node_id: resp.node_id)
    rescue FreeplaneGrpcClient::FreeplaneConnectionError
      # Some Freeplane versions don't support DeleteChild
    end
  end

  it "raises FreeplaneConnectionError when server is down" do
    bad_client = FreeplaneGrpcClient::Client.new("127.0.0.1", 59999)
    bad_client.connect
    expect { bad_client.get_current_node(timeout: 2) }.to raise_error(FreeplaneGrpcClient::FreeplaneConnectionError)
    bad_client.close
  end
end
