#!/usr/bin/env ruby
# frozen_string_literal: true

# Minimal example — sets the Freeplane status bar text via gRPC.
#
# Usage:
#   ruby setStatusSet.rb
#
# Requires a running Freeplane instance with the gRPC plugin on port 50051.
# Override host/port via environment variables:
#   FREEPLANE_HOST=127.0.0.1 FREEPLANE_PORT=50051 ruby setStatusSet.rb

require "freeplane_grpc_client"

host = ENV.fetch("FREEPLANE_HOST", "127.0.0.1")
port = ENV.fetch("FREEPLANE_PORT", "50051").to_i

client = FreeplaneGrpcClient::Client.new(host, port)

begin
  client.connect
  client.status_info_set(status_info: "hello from ruby")
  puts "Status bar set on #{host}:#{port}"
rescue FreeplaneGrpcClient::FreeplaneConnectionError => e
  $stderr.puts "Connection error: #{e.message}"
  exit 1
ensure
  client.close
end
