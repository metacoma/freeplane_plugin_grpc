# Shared setup/teardown for integration specs.
# Requires a running Freeplane gRPC server.
#
# Environment variables:
#   FREEPLANE_HOST  — server host (default: 127.0.0.1)
#   FREEPLANE_PORT  — server port  (default: 50051)

require "freeplane_grpc_client"

module IntegrationHelper
  def real_client
    @real_client ||= FreeplaneGrpcClient::Client.new(
      ENV.fetch("FREEPLANE_HOST", "127.0.0.1"),
      ENV.fetch("FREEPLANE_PORT", "50051").to_i
    ).tap(&:connect)
  end

  def unique_name(prefix = "IT")
    "#{prefix}_#{Time.now.to_i}_#{rand(10000)}"
  end
end
