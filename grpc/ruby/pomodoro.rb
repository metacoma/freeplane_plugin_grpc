#!/usr/bin/env ruby

this_dir = File.expand_path(File.dirname(__FILE__))
lib_dir = File.join(this_dir, 'lib')
$LOAD_PATH.unshift(lib_dir) unless $LOAD_PATH.include?(lib_dir)

require 'json'
require 'time'
require 'grpc'
require 'freeplane_services_pb'

def pomo_time(t)
  return Time.parse(t).strftime "%H:%M"
end


def main
  pomo_data =  JSON.parse(ARGF.read)
  stub = Freeplane::Freeplane::Stub.new('localhost:50051', :this_channel_is_insecure)
  pomodoro = stub.create_child(Freeplane::CreateChildRequest.new(name: "pomodoro", parent_node_id: ""))
  stub.node_attribute_add(Freeplane::NodeAttributeAddRequest.new(node_id: pomodoro["node_id"], attribute_name: pomo_time(pomo_data["start"]), attribute_value: pomo_time(pomo_data["end"])))


  session_n = 1
  pomo_index = 1

  pomo_session = stub.create_child(Freeplane::CreateChildRequest.new(name: "session#{session_n}", parent_node_id: pomodoro["node_id"]))

  for segment in pomo_data['segments'] do
    if (segment["type"] == "pomodoro") then
      segment_node = stub.create_child(Freeplane::CreateChildRequest.new(name: "pomodoro#{pomo_index}", parent_node_id: pomo_session["node_id"]))
      stub.node_attribute_add(Freeplane::NodeAttributeAddRequest.new(node_id: segment_node["node_id"], attribute_name: pomo_time(segment["start"]), attribute_value: pomo_time(segment["end"])))
      pomo_index = pomo_index + 1

      stub.node_background_color_set(Freeplane::NodeBackgroundColorSetRequest.new(node_id: segment_node["node_id"], red: 255, green: 120, blue: 120, alpha: 255))
    end
    if (segment["type"] == "long-break") then
      pomo_inde = 1
      session_n = session_n + 1
      pomo_session = stub.create_child(Freeplane::CreateChildRequest.new(name: "session#{session_n}", parent_node_id: pomodoro["node_id"]))
    end
  end
    
end

main
