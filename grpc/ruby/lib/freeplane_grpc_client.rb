# Compatibility shim for grpc >= 1.66 where GRPC::InsecureChannel was removed.
# Provides GRPC::InsecureChannel as an alias to GRPC::Core::Channel.
require "grpc"
unless defined?(GRPC::InsecureChannel)
  module GRPC
    class InsecureChannel
      def initialize(target)
        @channel = GRPC::Core::Channel.new(target, {}, :this_channel_is_insecure)
        @closed = false
      end

      def finished?
        false
      end

      def closed?
        @closed
      end

      def close
        @closed = true
      end

      def method_missing(method, *args, &block)
        @channel.send(method, *args, &block)
      end

      def respond_to_missing?(method, include_private = false)
        @channel.respond_to?(method, include_private)
      end
    end
  end
end

require "freeplane_grpc_client/version"
require "freeplane_grpc_client/exceptions"
require "freeplane_grpc_client/client"
require "freeplane_grpc_client/node"
require "freeplane_grpc_client/mindmap"
