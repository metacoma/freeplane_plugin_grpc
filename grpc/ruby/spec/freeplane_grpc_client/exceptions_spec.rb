require "spec_helper"
require "freeplane_grpc_client/exceptions"

RSpec.describe FreeplaneGrpcClient do
  describe "exception hierarchy" do
    it "FreeplaneGrpcError is a StandardError" do
      expect(described_class::FreeplaneGrpcError.ancestors).to include(StandardError)
    end

    it "FreeplaneConnectionError inherits from FreeplaneGrpcError" do
      expect(described_class::FreeplaneConnectionError.ancestors).to include(
        described_class::FreeplaneGrpcError
      )
    end

    it "FreeplaneOperationError inherits from FreeplaneGrpcError" do
      expect(described_class::FreeplaneOperationError.ancestors).to include(
        described_class::FreeplaneGrpcError
      )
    end

    it "NodeNotFoundError inherits from FreeplaneOperationError" do
      expect(described_class::NodeNotFoundError.ancestors).to include(
        described_class::FreeplaneOperationError
      )
    end

    it "MindMapError inherits from FreeplaneOperationError" do
      expect(described_class::MindMapError.ancestors).to include(
        described_class::FreeplaneOperationError
      )
    end
  end

  describe "FreeplaneGrpcClient::FreeplaneConnectionError" do
    it "has a default message" do
      e = described_class::FreeplaneConnectionError.new
      expect(e.message).to eq("Connection to Freeplane gRPC server failed")
    end

    it "accepts a custom message" do
      e = described_class::FreeplaneConnectionError.new("custom")
      expect(e.message).to eq("custom")
    end
  end

  describe "FreeplaneGrpcClient::FreeplaneOperationError" do
    it "has a default message" do
      e = described_class::FreeplaneOperationError.new
      expect(e.message).to eq("Freeplane operation failed")
    end

    it "accepts a custom message" do
      e = described_class::FreeplaneOperationError.new("custom")
      expect(e.message).to eq("custom")
    end
  end

  describe "FreeplaneGrpcClient::NodeNotFoundError" do
    it "has a default message" do
      e = described_class::NodeNotFoundError.new
      expect(e.message).to eq("Node not found")
    end
  end

  describe "FreeplaneGrpcClient::MindMapError" do
    it "has a default message" do
      e = described_class::MindMapError.new
      expect(e.message).to eq("Mind map operation failed")
    end
  end
end
