#!/usr/bin/env ruby

this_dir = File.expand_path(File.dirname(__FILE__))
lib_dir = File.join(this_dir, 'lib')
$LOAD_PATH.unshift(lib_dir) unless $LOAD_PATH.include?(lib_dir)

require 'grpc'
require 'freeplane_services_pb'

stub = Freeplane::Freeplane::Stub.new('localhost:50051', :this_channel_is_insecure)
stub.status_info_set(Freeplane::StatusInfoSetRequest.new(statusInfo: "hello from ruby"));
