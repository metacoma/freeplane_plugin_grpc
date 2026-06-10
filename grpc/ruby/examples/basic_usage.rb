#!/usr/bin/env ruby
# frozen_string_literal: true

# Basic usage example for the Freeplane gRPC Ruby client.
#
# Mirrors the Python example at grpc/python/examples/basic_usage.py.
#
# Usage:
#   ruby examples/basic_usage.rb
#
# Requires a running Freeplane instance with the gRPC plugin on port 50051.
# Override host/port via environment variables:
#   FREEPLANE_HOST=127.0.0.1 FREEPLANE_PORT=50051 ruby examples/basic_usage.rb

require "freeplane_grpc_client"

host = ENV.fetch("FREEPLANE_HOST", "127.0.0.1")
port = ENV.fetch("FREEPLANE_PORT", "50051").to_i

client = FreeplaneGrpcClient::Client.new(host, port)

begin
  client.connect
  puts "Connected to #{host}:#{port}"

  # Get the current map
  map = client.current_map
  puts "Current map ID: #{map.map_id}"

  # Get the root node
  root = map.root
  puts "Root node: #{root.node_id} (text: #{root.get_text.inspect})"

  # Create a child node
  child = map.create_child(root.node_id, "New Child Node")
  puts "Created child: #{child.node_id}"

  # Set text on the child
  child.set_text("Modified Child")
  puts "Child text now: #{child.get_text}"

  # List children of root
  children = map.root.children
  puts "Root has #{children.length} child(ren)"

  # Set tags on the child
  child.set_tags(["important", "review"])
  puts "Tags set on child"

  # Add a tag
  child.add_tags(["new-tag"])
  puts "Added tag to child"

  # Set a note on the child
  child.set_note("This is a note")
  puts "Note set: #{child.get_note.inspect}"

  # Set a link on the child
  child.set_link("http://example.com")
  puts "Link set: #{child.get_link.inspect}"

  # Set colors
  child.set_color(255, 0, 0)
  child.set_background_color(0, 255, 0)
  puts "Colors set"

  # Set a detail on the child
  child.set_details("Some details about this node")
  puts "Details set"

  # Focus the child
  child.focus
  puts "Focused child node"

  # Export map to JSON
  json = map.to_json
  puts "Map JSON length: #{json.length} characters"

  # Find nodes by text
  found = map.find_nodes("Modified")
  puts "Found #{found.length} node(s) matching 'Modified'"

  # Set status info
  client.set_status_info("Ruby client example complete")
  puts "Status bar updated"

  puts "\nExample completed successfully!"

rescue FreeplaneGrpcClient::ConnectionError => e
  $stderr.puts "Connection error: #{e.message}"
  exit 1
rescue FreeplaneGrpcClient::OperationError => e
  $stderr.puts "Operation error: #{e.message}"
  exit 1
ensure
  client.close
  puts "Connection closed"
end
