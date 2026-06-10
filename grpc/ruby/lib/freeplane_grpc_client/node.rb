require "freeplane_grpc_client/client"

module FreeplaneGrpcClient
  # High-level abstraction over a single Freeplane node.
  #
  # Provides methods for text, hierarchy, styling, notes, attributes,
  # links, tags, icons, state, and actions — all delegating to the
  # underlying gRPC stub via @client._call.
  #
  # Example:
  #   node = mindmap.root
  #   node.set_text("New Title")
  #   child = node.add_child("Child Node")
  #   child.set_tags(["important", "review"])
  class Node
    attr_reader :client, :map_id, :node_id

    def initialize(client, map_id, node_id)
      @client = client
      @map_id = map_id
      @node_id = node_id
    end

    # -- Text ---------------------------------------------------------------

    def get_text
      resp = @client.get_node_text(node_id: @node_id)
      resp.text
    end

    def set_text(text)
      @client.set_node_text(node_id: @node_id, text: text)
      self
    end

    # -- Hierarchy ----------------------------------------------------------

    def add_child(text, style = nil)
      resp = @client.create_child(name: text, parent_node_id: @node_id)
      Node.new(@client, @map_id, resp.node_id)
    end

    def children
      resp = @client.list_child_nodes(node_id: @node_id)
      resp.children.map { |c| Node.new(@client, @map_id, c.node_id) }
    end

    def parent
      resp = @client.get_parent_node(node_id: @node_id)
      Node.new(@client, @map_id, resp.parent_node_id)
    end

    def delete
      @client.delete_child(node_id: @node_id)
      self
    end

    def move(new_parent_node_id)
      @client.move_node(node_id: @node_id, new_parent_node_id: new_parent_node_id)
      self
    end

    # -- Styling ------------------------------------------------------------

    def set_color(red, green, blue, alpha = 255)
      @client.node_color_set(node_id: @node_id, red: red, green: green, blue: blue, alpha: alpha)
      self
    end

    def set_background_color(red, green, blue, alpha = 255)
      @client.node_background_color_set(
        node_id: @node_id, red: red, green: green, blue: blue, alpha: alpha
      )
      self
    end

    # -- Notes --------------------------------------------------------------

    def get_note
      resp = @client.get_node_note(node_id: @node_id)
      resp.has_note ? resp.note : nil
    end

    def set_note(note)
      @client.node_note_set(node_id: @node_id, note: note)
      self
    end

    # -- Attributes ---------------------------------------------------------

    def set_attribute(name, value)
      @client.node_attribute_add(
        node_id: @node_id,
        attribute_name: name,
        attribute_value: value
      )
      self
    end

    # -- Links --------------------------------------------------------------

    def get_link
      resp = @client.get_node_link(node_id: @node_id)
      resp.has_link ? resp.link : nil
    end

    def set_link(link)
      @client.node_link_set(node_id: @node_id, link: link)
      self
    end

    # -- Tags ---------------------------------------------------------------

    def set_tags(tags)
      @client.node_tag_set(node_id: @node_id, tags: tags)
      self
    end

    def add_tags(tags)
      @client.node_tag_add(node_id: @node_id, tags: tags)
      self
    end

    # -- Icons --------------------------------------------------------------

    def add_icon(icon_name)
      @client.node_add_icon(node_id: @node_id, icon_name: icon_name)
      self
    end

    # -- State --------------------------------------------------------------

    def set_details(details)
      @client.node_details_set(node_id: @node_id, details: details)
      self
    end

    # -- Actions ------------------------------------------------------------

    def focus
      @client.focus_node(node_id: @node_id)
      self
    end

    def select
      focus
    end

    # -- Convenience --------------------------------------------------------

    def to_s
      "#<FreeplaneGrpcClient::Node map=#{@map_id} node=#{@node_id} text=#{get_text.inspect}>"
    end

    def ==(other)
      other.is_a?(Node) && @map_id == other.map_id && @node_id == other.node_id
    end

    def hash
      [@map_id, @node_id].hash
    end
  end
end
