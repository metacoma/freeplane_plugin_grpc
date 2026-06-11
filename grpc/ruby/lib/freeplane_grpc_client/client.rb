require "grpc"
require "ostruct"
require "google/protobuf"
require "freeplane_pb"
require "freeplane_services_pb"
require "freeplane_grpc_client/exceptions"

module FreeplaneGrpcClient
  # High-level client for the Freeplane gRPC plugin.
  #
  # Provides snake_case wrappers over all 27 RPC methods defined in
  # freeplane.proto, with automatic error mapping and optional timeout support.
  #
  # Example usage:
  #   client = FreeplaneGrpcClient::Client.new("127.0.0.1", 50051)
  #   client.connect
  #   resp = client.create_child(name: "My Node", parent_node_id: "")
  #   puts resp.node_id
  #   client.close
  #
  # Context-manager style:
  #   FreeplaneGrpcClient::Client.new("127.0.0.1", 50051).tap do |client|
  #     client.connect
  #     # ... use client ...
  #   ensure
  #     client.close
  #   end
  class Client
    DEFAULT_HOST = "127.0.0.1"
    DEFAULT_PORT = 50051

    def initialize(host = nil, port = nil)
      @host = host || ENV.fetch("FREEPLANE_HOST", DEFAULT_HOST)
      @port = (port || ENV.fetch("FREEPLANE_PORT", DEFAULT_PORT.to_s)).to_i
      @channel = nil
      @stub = nil
    end

    attr_reader :host, :port

    # -- lifecycle ----------------------------------------------------------

    def connect
      @channel = nil
      @stub = Freeplane::Freeplane::Stub.new("#{@host}:#{@port}", :this_channel_is_insecure)
    end

    def close
      @channel&.close
      @channel = nil
      @stub = nil
    end

    def connected?
      !!(@channel && !@channel.finished? && !@channel.closed?)
    end

    # -- context manager ----------------------------------------------------

    # Yields self as a context manager; ensures close in ensure block.
    #
    # Example:
    #   FreeplaneGrpcClient::Client.new("127.0.0.1", 50051).in_context do |client|
    #     map = client.current_map
    #     puts map.root.get_text
    #   end
    def in_context
      connect
      begin
        yield self
      ensure
        close
      end
    end

    # -- convenience --------------------------------------------------------

    # Returns a MindMap for the currently open map.
    def current_map
      resp = get_current_node
      MindMap.new(self, resp.map_id)
    end

    # -- internal helper ----------------------------------------------------

    # Look up a proto message class by name, falling back to the DescriptorPool
    def self.proto_message_class(name)
      descriptor = Google::Protobuf::DescriptorPool.generated_pool.lookup("freeplane.#{name}")
      descriptor&.msgclass
    end

    # -- internal helper ----------------------------------------------------

    def _call(method, request, timeout: nil)
      opts = {}
      opts[:timeout] = timeout if timeout
      begin
        response = method.call(request, **opts)
      rescue GRPC::BadStatus => e
        status_code = e.status.to_s
        if ["UNAVAILABLE", "DEADLINE_EXCEEDED", "INTERNAL"].include?(status_code.upcase)
          raise FreeplaneConnectionError, "gRPC call failed: #{e.message}"
        end
        raise FreeplaneOperationError, "gRPC call failed (#{e.status}): #{e.message}"
      rescue => e
        raise FreeplaneConnectionError, "gRPC call failed: #{e.message}"
      end

      if response.respond_to?(:success) && !response.success
        error_msg = response.respond_to?(:error_message) ? response.error_message : ""
        raise FreeplaneOperationError, error_msg || "Operation failed"
      end

      response
    end

    # -- RPC wrappers (27 methods matching freeplane.proto) -----------------

    # rpc CreateChild
    def create_child(name:, parent_node_id:, timeout: nil)
      req = proto_class("CreateChildRequest").new(name: name, parent_node_id: parent_node_id)
      _call(@stub.create_child, req, timeout: timeout)
    end

    # rpc DeleteChild
    def delete_child(node_id:, timeout: nil)
      req = proto_class("DeleteChildRequest").new(node_id: node_id)
      _call(@stub.delete_child, req, timeout: timeout)
    end

    # rpc NodeAttributeAdd
    def node_attribute_add(node_id:, attribute_name:, attribute_value:, timeout: nil)
      req = proto_class("NodeAttributeAddRequest").new(
        node_id: node_id,
        attribute_name: attribute_name,
        attribute_value: attribute_value,
      )
      _call(@stub.node_attribute_add, req, timeout: timeout)
    end

    # rpc NodeLinkSet
    def node_link_set(node_id:, link:, timeout: nil)
      req = proto_class("NodeLinkSetRequest").new(node_id: node_id, link: link)
      _call(@stub.node_link_set, req, timeout: timeout)
    end

    # rpc NodeDetailsSet
    def node_details_set(node_id:, details:, timeout: nil)
      req = proto_class("NodeDetailsSetRequest").new(node_id: node_id, details: details)
      _call(@stub.node_details_set, req, timeout: timeout)
    end

    # rpc NodeNoteSet
    def node_note_set(node_id:, note:, timeout: nil)
      req = proto_class("NodeNoteSetRequest").new(node_id: node_id, note: note)
      _call(@stub.node_note_set, req, timeout: timeout)
    end

    # rpc NodeTagSet
    def node_tag_set(node_id:, tags:, timeout: nil)
      req = proto_class("NodeTagSetRequest").new(node_id: node_id, tags: tags)
      _call(@stub.node_tag_set, req, timeout: timeout)
    end

    # rpc NodeTagAdd
    def node_tag_add(node_id:, tags:, timeout: nil)
      req = proto_class("NodeTagAddRequest").new(node_id: node_id, tags: tags)
      _call(@stub.node_tag_add, req, timeout: timeout)
    end

    # rpc NodeConnect
    def node_connect(source_node_id:, target_node_id:, relationship:, timeout: nil)
      req = proto_class("NodeConnectRequest").new(
        source_node_id: source_node_id,
        target_node_id: target_node_id,
        relationship: relationship,
      )
      _call(@stub.node_connect, req, timeout: timeout)
    end

    # rpc NodeAddIcon
    def node_add_icon(node_id:, icon_name:, timeout: nil)
      req = proto_class("NodeAddIconRequest").new(node_id: node_id, icon_name: icon_name)
      _call(@stub.node_add_icon, req, timeout: timeout)
    end

    # rpc Groovy
    def groovy(groovy_code:, timeout: nil)
      req = proto_class("GroovyRequest").new(groovy_code: groovy_code)
      _call(@stub.groovy, req, timeout: timeout)
    end

    # rpc NodeColorSet
    def node_color_set(node_id:, red:, green:, blue:, alpha: 255, timeout: nil)
      req = proto_class("NodeColorSetRequest").new(
        node_id: node_id,
        red: red,
        green: green,
        blue: blue,
        alpha: alpha,
      )
      _call(@stub.node_color_set, req, timeout: timeout)
    end

    # rpc NodeBackgroundColorSet
    def node_background_color_set(node_id:, red:, green:, blue:, alpha: 255, timeout: nil)
      req = proto_class("NodeBackgroundColorSetRequest").new(
        node_id: node_id,
        red: red,
        green: green,
        blue: blue,
        alpha: alpha,
      )
      _call(@stub.node_background_color_set, req, timeout: timeout)
    end

    # rpc StatusInfoSet
    def status_info_set(status_info:, timeout: nil)
      req = proto_class("StatusInfoSetRequest").new(statusInfo: status_info)
      _call(@stub.status_info_set, req, timeout: timeout)
    end

    # rpc TextFSM
    def text_fsm(json:, timeout: nil)
      req = proto_class("TextFSMRequest").new(json: json)
      _call(@stub.text_fsm, req, timeout: timeout)
    end

    # rpc MindMapFromJSON
    def mind_map_from_json(json:, timeout: nil)
      req = proto_class("MindMapFromJSONRequest").new(json: json)
      _call(@stub.mind_map_from_json, req, timeout: timeout)
    end

    # rpc MindMapToJSON
    def mind_map_to_json(timeout: nil)
      req = proto_class("MindMapToJSONRequest").new
      _call(@stub.mind_map_to_json, req, timeout: timeout)
    end

    # rpc GetCurrentNode
    def get_current_node(timeout: nil)
      req = proto_class("GetCurrentNodeRequest").new
      _call(@stub.get_current_node, req, timeout: timeout)
    end

    # rpc OpenMap
    def open_map(file_path:, timeout: nil)
      req = proto_class("OpenMapRequest").new(file_path: file_path)
      _call(@stub.open_map, req, timeout: timeout)
    end

    # rpc FocusNode
    def focus_node(node_id:, timeout: nil)
      req = proto_class("FocusNodeRequest").new(node_id: node_id)
      _call(@stub.focus_node, req, timeout: timeout)
    end

    # rpc GetNodeText
    def get_node_text(node_id:, timeout: nil)
      req = proto_class("GetNodeTextRequest").new(node_id: node_id)
      _call(@stub.get_node_text, req, timeout: timeout)
    end

    # rpc GetParentNode
    def get_parent_node(node_id:, timeout: nil)
      req = proto_class("GetParentNodeRequest").new(node_id: node_id)
      _call(@stub.get_parent_node, req, timeout: timeout)
    end

    # rpc ListChildNodes
    def list_child_nodes(node_id:, timeout: nil)
      req = proto_class("ListChildNodesRequest").new(node_id: node_id)
      _call(@stub.list_child_nodes, req, timeout: timeout)
    end

    # rpc GetNodeNote
    def get_node_note(node_id:, timeout: nil)
      req = proto_class("GetNodeNoteRequest").new(node_id: node_id)
      _call(@stub.get_node_note, req, timeout: timeout)
    end

    # rpc GetNodeLink
    def get_node_link(node_id:, timeout: nil)
      req = proto_class("GetNodeLinkRequest").new(node_id: node_id)
      _call(@stub.get_node_link, req, timeout: timeout)
    end

    # rpc SetNodeText
    def set_node_text(node_id:, text:, timeout: nil)
      req = proto_class("SetNodeTextRequest").new(node_id: node_id, text: text)
      _call(@stub.set_node_text, req, timeout: timeout)
    end

    # rpc MoveNode
    def move_node(node_id:, new_parent_node_id:, timeout: nil)
      req = proto_class("MoveNodeRequest").new(
        node_id: node_id,
        new_parent_node_id: new_parent_node_id,
      )
      _call(@stub.move_node, req, timeout: timeout)
    end

    private

    # Look up a proto message class by name
    def proto_class(name)
      self.class.proto_message_class(name) or
        raise NameError, "Unknown proto message: freeplane.#{name}"
    end
  end
end
