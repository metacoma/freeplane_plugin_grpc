require "freeplane_grpc_client/node"

module FreeplaneGrpcClient
  # High-level abstraction over a Freeplane mind map.
  #
  # Provides navigation, metadata, file operations, and node creation methods.
  #
  # Example:
  #   map = client.current_map
  #   root = map.root
  #   child = map.create_child(root.node_id, "New Child")
  #   json = map.to_json
  class MindMap
    attr_reader :client, :map_id

    def initialize(client, map_id)
      @client = client
      @map_id = map_id
    end

    # -- Navigation ---------------------------------------------------------

    def root
      # Start from the current node and walk up to find the root
      resp = @client.get_current_node
      current_id = resp.node_id
      loop do
        parent_resp = @client.get_parent_node(node_id: current_id)
        break if parent_resp.parent_node_id.empty? || parent_resp.parent_node_id == current_id
        current_id = parent_resp.parent_node_id
      end
      Node.new(@client, @map_id, current_id)
    end

    def selected_node
      resp = @client.get_current_node
      Node.new(@client, @map_id, resp.node_id)
    end

    def find_nodes(pattern)
      # Walk the tree from root and match text against pattern (regex or string)
      regex = pattern.is_a?(Regexp) ? pattern : Regexp.new(Regexp.escape(pattern), Regexp::IGNORECASE)
      results = []
      queue = [root]
      while (n = queue.shift)
        results << n if regex.match?(n.get_text)
        n.children.each { |c| queue << c }
      end
      results
    end

    # -- Metadata -----------------------------------------------------------

    def info
      { map_id: @map_id }
    end

    # -- File operations ----------------------------------------------------

    def to_json
      resp = @client.mind_map_to_json
      resp.json
    end

    def save(_path = nil)
      # Freeplane saves automatically; this is a no-op placeholder.
      true
    end

    def import_map(file_path)
      @client.open_map(file_path: file_path)
      self
    end

    # -- Node creation ------------------------------------------------------

    def create_node(text, parent_id = nil, _style = nil)
      parent = parent_id || @map_id
      resp = @client.create_child(name: text, parent_node_id: parent)
      Node.new(@client, @map_id, resp.node_id)
    end

    def create_child(parent, text, _style = nil)
      create_node(text, parent)
    end
  end
end
