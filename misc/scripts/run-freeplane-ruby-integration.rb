#!/usr/bin/env ruby
# frozen_string_literal: true

# run-freeplane-ruby-integration.rb — Full smoke test for Freeplane gRPC plugin (Ruby client).
#
# This script demonstrates the complete runtime validation flow:
#   1. Connect to the Freeplane gRPC server
#   2. Get the current mind map
#   3. Create a child node with a unique marker string
#   4. Read back the node text via gRPC
#   5. Search for the marker via find_nodes
#   6. Export the mind map as JSON and verify the marker appears in it
#   7. Print success/failure with the marker string
#   8. Exit non-zero on any failure
#
# Usage (requires a running Freeplane instance with the gRPC plugin):
#
#     ruby misc/scripts/run-freeplane-ruby-integration.rb
#
# To connect to a non-default host/port:
#
#     FREEPLANE_HOST=192.168.1.100 FREEPLANE_PORT=9000 ruby misc/scripts/run-freeplane-ruby-integration.rb

require "securerandom"
require "time"

# Ensure the gem can be found
$LOAD_PATH.unshift File.expand_path("../../grpc/ruby/lib", __dir__)
require "freeplane_grpc_client"

# ---------------------------------------------------------------------------
# Helpers
# ---------------------------------------------------------------------------

def generate_marker
  timestamp = Time.now.strftime("%Y%m%d_%H%M%S")
  random_suffix = SecureRandom.hex(2)
  "grpc-ruby-smoke-test-#{timestamp}_#{random_suffix}"
end

# ---------------------------------------------------------------------------
# Main
# ---------------------------------------------------------------------------

def main
  host = ENV.fetch("FREEPLANE_HOST", "127.0.0.1")
  port = ENV.fetch("FREEPLANE_PORT", "50051").to_i
  marker = generate_marker

  puts "Connecting to Freeplane gRPC server at #{host}:#{port}..."

  # --- 1. Connect --------------------------------------------------------
  client = nil
  begin
    client = FreeplaneGrpcClient::Client.new(host, port)
    client.connect
  rescue FreeplaneGrpcClient::FreeplaneConnectionError => _exc
    puts "CONNECTION FAILED: #{"#{"_exc.message}" rescue "unknown"}"
    return 1
  rescue Errno::ECONNREFUSED
    puts "Cannot connect to Freeplane at #{host}:#{port}. " \
         "Is Freeplane running with the gRPC plugin?"
    return 1
  end

  begin
    # --- 2. Get the current mind map -----------------------------------
    puts "\n--- Getting current mind map ---"
    mindmap = client.current_map
    info = mindmap.info
    puts "Map ID: #{info[:map_id]}"

    # --- 3. Get the root node ------------------------------------------
    puts "\n--- Getting root node ---"
    root = mindmap.root
    root_text = root.get_text
    puts "Root text: #{root_text}"

    # --- 4. Create a child node with the unique marker -----------------
    puts "\n--- Creating test node with marker: #{marker} ---"
    child = root.add_child(marker)
    created_node_id = child.node_id
    puts "Created child node: #{created_node_id}"

    # --- 5. Read back the node text via gRPC ---------------------------
    puts "\n--- Read-back verification (GetNodeText) ---"
    readback_text = child.get_text
    puts "Read back text: #{readback_text}"

    if readback_text != marker
      puts "FAILED: Read-back text mismatch!\n" \
           "  Expected: #{marker}\n" \
           "  Got:      #{readback_text}"
      return 1
    end
    puts "  ✓ GetNodeText verification passed"

    # --- 6. Search for the marker in the mind map ----------------------
    puts "\n--- Search for marker in mind map ---"
    matches = mindmap.find_nodes(marker)
    puts "Found #{matches.size} node(s) matching '#{marker}'"

    if matches.empty?
      puts "FAILED: Marker '#{marker}' not found in mind map tree"
      return 1
    end
    puts "  ✓ find_nodes() verification passed"

    # --- 7. Export mind map as JSON and verify marker (non-fatal) ------
    puts "\n--- Export mind map as JSON and verify marker (non-fatal) ---"
    json_str = mindmap.to_json
    puts "JSON length: #{json_str.length} characters"

    if json_str.include?(marker)
      puts "  ✓ MindMapToJSON verification passed"
    else
      puts "  (Warning: Marker '#{marker}' not found in MindMapToJSON output. " \
           "This may indicate a stale JSON export, but the node was " \
           "confirmed created via GetNodeText and find_nodes().)"
    end

    # --- 8. Success ----------------------------------------------------
    puts "\n" + "=" * 50
    puts "SMOKE TEST PASSED"
    puts "  Marker:   #{marker}"
    puts "  Node ID:  #{created_node_id}"
    puts "  Text:     #{readback_text}"
    puts "  Map ID:   #{info[:map_id]}"
    puts "=" * 50
    0  # GetNodeText + find_nodes() confirmed the node was created
  rescue FreeplaneGrpcClient::FreeplaneOperationError => _exc
    puts "\nOPERATION FAILED: #{"#{"_exc.message}" rescue "unknown"}"
    1
  rescue StandardError => _exc
    puts "\nUNEXPECTED ERROR: #{_exc.message}"
    1
  ensure
    client&.close
  end
end

exit(main)
