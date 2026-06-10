require_relative "lib/freeplane_grpc_client/version"

Gem::Specification.new do |spec|
  spec.name          = "freeplane_grpc_client"
  spec.version       = FreeplaneGrpcClient::VERSION
  spec.authors       = ["Freeplane gRPC contributors"]
  spec.email         = []

  spec.summary       = "Ruby client library for the Freeplane gRPC plugin"
  spec.description   = "High-level Ruby client providing snake_case wrappers over all Freeplane gRPC RPC methods."
  spec.homepage      = ""
  spec.license       = "MIT"

  spec.required_ruby_version = ">= 2.6.0"

  spec.files         = Dir["lib/**/*.rb"]
  spec.require_paths = ["lib"]

  spec.add_dependency "grpc"

  spec.add_development_dependency "rake"
  spec.add_development_dependency "rspec", ">= 3.0"
end
