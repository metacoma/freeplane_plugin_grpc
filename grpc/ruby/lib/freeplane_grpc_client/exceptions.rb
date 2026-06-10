module FreeplaneGrpcClient
  # Base exception for all Freeplane gRPC client errors.
  class FreeplaneGrpcError < StandardError; end

  # Connection/gRPC-level failure (network errors, unavailable server, etc.).
  class FreeplaneConnectionError < FreeplaneGrpcError
    def initialize(message = "Connection to Freeplane gRPC server failed")
      super(message)
    end
  end

  # Server-reported operation failure (success: false in response).
  class FreeplaneOperationError < FreeplaneGrpcError
    def initialize(message = "Freeplane operation failed")
      super(message)
    end
  end

  # Requested node was not found.
  class NodeNotFoundError < FreeplaneOperationError
    def initialize(message = "Node not found")
      super(message)
    end
  end

  # Mind map-level operation failure.
  class MindMapError < FreeplaneOperationError
    def initialize(message = "Mind map operation failed")
      super(message)
    end
  end
end
