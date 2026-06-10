module FreeplaneGrpcClient
  class Error < StandardError; end

  class ConnectionError < Error
    def initialize(message = "Connection to Freeplane gRPC server failed")
      super(message)
    end
  end

  class OperationError < Error
    def initialize(message = "Freeplane operation failed")
      super(message)
    end
  end

  class NodeNotFoundError < OperationError
    def initialize(message = "Node not found")
      super(message)
    end
  end

  class MindMapError < OperationError
    def initialize(message = "Mind map operation failed")
      super(message)
    end
  end
end
