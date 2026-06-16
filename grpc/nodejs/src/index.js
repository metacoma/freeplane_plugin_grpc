/**
 * freeplane-grpc-client - Node.js gRPC client for Freeplane
 *
 * Main entry point. Exports the FreeplaneClient, Node, MindMap classes
 * and the exception hierarchy.
 *
 * Usage:
 *   const { FreeplaneClient, Node, MindMap, FreeplaneGrpcError } = require('freeplane-grpc-client');
 */

const FreeplaneClient = require('./client');
const Node = require('./node');
const MindMap = require('./mindmap');

const {
  FreeplaneGrpcError,
  FreeplaneConnectionError,
  FreeplaneOperationError,
  NodeNotFoundError,
  MindMapError,
} = require('./exceptions');

module.exports = {
  FreeplaneClient,
  Node,
  MindMap,
  FreeplaneGrpcError,
  FreeplaneConnectionError,
  FreeplaneOperationError,
  NodeNotFoundError,
  MindMapError,
};
