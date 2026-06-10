#!/usr/bin/env ruby
# frozen_string_literal: true

# Pomodoro example — creates pomodoro sessions as mind-map nodes.
#
# Usage:
#   cat pomodori_example_data.json | ruby pomodoro.rb
#
# Requires a running Freeplane instance with the gRPC plugin on port 50051.
# Override host/port via environment variables:
#   FREEPLANE_HOST=127.0.0.1 FREEPLANE_PORT=50051 ruby pomodoro.rb

require "freeplane_grpc_client"
require "json"
require "time"

host = ENV.fetch("FREEPLANE_HOST", "127.0.0.1")
port = ENV.fetch("FREEPLANE_PORT", "50051").to_i

def pomo_time(t)
  Time.parse(t).strftime "%H:%M"
end

client = FreeplaneGrpcClient::Client.new(host, port)

begin
  client.connect
  puts "Connected to #{host}:#{port}"

  pomo_data = JSON.parse(ARGF.read)

  # Create the top-level pomodoro node
  pomodoro = client.create_child(name: "pomodoro", parent_node_id: "")
  client.node_attribute_add(
    node_id: pomodoro.node_id,
    attribute_name: pomo_time(pomo_data["start"]),
    attribute_value: pomo_time(pomo_data["end"]),
  )

  session_n = 1
  pomo_index = 1

  pomo_session = client.create_child(
    name: "session#{session_n}",
    parent_node_id: pomodoro.node_id,
  )

  pomo_data["segments"].each do |segment|
    if segment["type"] == "pomodoro"
      segment_node = client.create_child(
        name: "pomodoro#{pomo_index}",
        parent_node_id: pomo_session.node_id,
      )
      client.node_attribute_add(
        node_id: segment_node.node_id,
        attribute_name: pomo_time(segment["start"]),
        attribute_value: pomo_time(segment["end"]),
      )
      pomo_index += 1

      client.node_background_color_set(
        node_id: segment_node.node_id,
        red: 255, green: 120, blue: 120, alpha: 255,
      )
    end
    if segment["type"] == "long-break"
      session_n += 1
      pomo_session = client.create_child(
        name: "session#{session_n}",
        parent_node_id: pomodoro.node_id,
      )
    end
  end

  puts "Pomodoro sessions created successfully"

rescue FreeplaneGrpcClient::FreeplaneConnectionError => e
  $stderr.puts "Connection error: #{e.message}"
  exit 1
rescue FreeplaneGrpcClient::FreeplaneOperationError => e
  $stderr.puts "Operation error: #{e.message}"
  exit 1
ensure
  client.close
  puts "Connection closed"
end
