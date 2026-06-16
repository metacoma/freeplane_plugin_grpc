/**
 * FreeplaneClient - the main entry point for interacting with Freeplane via gRPC.
 *
 * Uses @grpc/grpc-js with runtime proto-loader for zero-codegen operation.
 * All public methods are async/await-friendly.
 */

const grpc = require('@grpc/grpc-js');
const protoLoader = require('@grpc/proto-loader');
const path = require('path');

const {
  FreeplaneConnectionError,
  FreeplaneOperationError,
  NodeNotFoundError,
  MindMapError,
} = require('./exceptions');
const MindMap = require('./mindmap');

const DEFAULT_HOST = '127.0.0.1';
const DEFAULT_PORT = 50051;

/**
 * Load the proto file and return the package definition.
 * @returns {object} The loaded proto package definition.
 */
function loadProto() {
  const protoPath = path.join(__dirname, '..', '..', '..', 'src', 'main', 'proto', 'freeplane.proto');
  const packageDefinition = protoLoader.loadSync(protoPath, {
    keepCase: true,
    longs: String,
    enums: String,
    defaults: true,
    oneofs: true,
  });
  return grpc.loadPackageDefinition(packageDefinition);
}

/**
 * Client for interacting with a Freeplane gRPC server.
 *
 * Typical usage:
 *   const client = new FreeplaneClient();
 *   await client.connect();
 *   try {
 *     const mindmap = await client.currentMap();
 *     const root = await mindmap.root();
 *     console.log(await root.getText());
 *   } finally {
 *     await client.close();
 *   }
 */
class FreeplaneClient {
  /**
   * Create a new FreeplaneClient.
   * @param {object} [options]
   * @param {string} [options.host] - Server hostname (default: FREEPLANE_HOST env or 127.0.0.1)
   * @param {number} [options.port] - Server port (default: FREEPLANE_PORT env or 50051)
   */
  constructor(options = {}) {
    this._host = options.host || process.env.FREEPLANE_HOST || DEFAULT_HOST;
    this._port = options.port || parseInt(process.env.FREEPLANE_PORT, 10) || DEFAULT_PORT;
    this._channel = null;
    this._stub = null;
    this._protoLoaded = false;
  }

  get host() {
    return this._host;
  }

  get port() {
    return this._port;
  }

  /**
   * Open (or re-open) the gRPC channel to the server.
   * @returns {Promise<void>}
   * @throws {FreeplaneConnectionError}
   */
  async connect() {
    try {
      this._channel = new grpc.Channel(
        `${this._host}:${this._port}`,
        grpc.credentials.createInsecure()
      );

      // Load proto and create stub
      const freeplanePkg = loadProto();
      this._stub = new freeplanePkg.freeplane.Freeplane(
        this._channel,
        grpc.credentials.createInsecure()
      );
      this._protoLoaded = true;

      // Verify connectivity with a timeout
      await new Promise((resolve, reject) => {
        const deadline = Date.now() + 5000;
        const checkConnectivity = () => {
          const state = this._channel.getConnectivityState();
          if (state === grpc.connectivityState.READY || state === grpc.connectivityState.IDLE) {
            resolve();
          } else if (Date.now() >= deadline) {
            reject(new Error('timeout'));
          } else {
            setTimeout(checkConnectivity, 100);
          }
        };
        this._channel.watchConnectivityState(grpc.connectivityState.IDLE, deadline, (err) => {
          if (err) {
            reject(err);
          } else {
            checkConnectivity();
          }
        });
      });
    } catch (err) {
      if (this._channel) {
        try { this._channel.close(); } catch (_) { /* ignore */ }
      }
      this._channel = null;
      this._stub = null;
      throw new FreeplaneConnectionError(
        `Failed to connect to Freeplane gRPC server at ${this._host}:${this._port}: ${err.message}`
      );
    }
  }

  /**
   * Close the gRPC channel if it is open.
   * @returns {Promise<void>}
   */
  async close() {
    if (this._channel) {
      try {
        this._channel.close();
      } catch (_) {
        // ignore close errors
      }
      this._channel = null;
    }
    this._stub = null;
    this._protoLoaded = false;
  }

  /**
   * Get the gRPC stub, ensuring the client is connected.
   * @returns {*} The gRPC stub.
   * @throws {FreeplaneConnectionError}
   * @private
   */
  _getStub() {
    if (!this._stub) {
      throw new FreeplaneConnectionError(
        'No active connection. Call connect() first.'
      );
    }
    return this._stub;
  }

  /**
   * Call a gRPC method and convert failures to domain exceptions.
   * Wraps the callback-based gRPC API in a Promise.
   *
   * @param {Function} method - The gRPC stub method to invoke.
   * @param {object} request - The request message object.
   * @param {number} [timeout] - Optional timeout in milliseconds.
   * @returns {Promise<object>} The response object.
   * @throws {FreeplaneConnectionError|FreeplaneOperationError}
   * @private
   */
  _call(method, request, timeout) {
    return new Promise((resolve, reject) => {
      const options = {};
      if (timeout) {
        options.deadline = new Date(Date.now() + timeout);
      }

      method(request, options, (err, response) => {
        if (err) {
          const code = err.code;
          // Map gRPC status codes to domain exceptions
          if (code === grpc.status.UNAVAILABLE ||
              code === grpc.status.DEADLINE_EXCEEDED ||
              code === grpc.status.RESOURCE_EXHAUSTED ||
              code === grpc.status.CANCELLED) {
            reject(new FreeplaneConnectionError(`gRPC call failed: ${err.details}`));
          } else {
            reject(new FreeplaneOperationError(
              `gRPC call failed (${grpc.status[code] || code}): ${err.details}`
            ));
          }
          return;
        }

        // Check the 'success' field common to most responses
        if (response.success !== undefined && response.success !== null && !response.success) {
          const errorMsg = response.errorMessage || response.error_message || '';
          const message = errorMsg ? `Operation failed: ${errorMsg}` : 'Operation failed';
          reject(new FreeplaneOperationError(message));
          return;
        }

        resolve(response);
      });
    });
  }

  // ---- High-level operations ----

  /**
   * Get the currently open / active mind map.
   * @returns {Promise<MindMap>}
   */
  async currentMap() {
    const stub = this._getStub();
    const resp = await this._call(stub.GetCurrentNode, {});
    if (!resp.success) {
      throw new FreeplaneOperationError(
        resp.errorMessage || 'No map currently open'
      );
    }
    return new MindMap(this, resp.mapId, resp.nodeId);
  }

  /**
   * Alias for currentMap().
   * @returns {Promise<MindMap>}
   */
  async selectedMap() {
    return this.currentMap();
  }

  /**
   * Open a mind map file on the Freeplane server.
   * @param {string} filePath - Path to the .mm file to open.
   * @returns {Promise<MindMap>}
   */
  async openMap(filePath) {
    const stub = this._getStub();
    await this._call(stub.OpenMap, { filePath });
    return this.currentMap();
  }

  /**
   * Export the current mind map as JSON.
   * @returns {Promise<string>} JSON string.
   */
  async getMapToJson() {
    const stub = this._getStub();
    const resp = await this._call(stub.MindMapToJSON, {});
    return resp.json;
  }

  /**
   * Import a mind map from JSON data.
   * @param {string} jsonData - JSON string representing a mind map.
   * @returns {Promise<boolean>}
   */
  async mindMapFromJson(jsonData) {
    const stub = this._getStub();
    const resp = await this._call(stub.MindMapFromJSON, { json: jsonData });
    return resp.success;
  }

  /**
   * Execute Groovy code on the Freeplane server.
   * @param {string} code - Groovy script to execute.
   * @returns {Promise<string>} The result output.
   */
  async groovy(code) {
    const stub = this._getStub();
    const resp = await this._call(stub.Groovy, { groovyCode: code });
    return resp.result;
  }

  /**
   * Focus (select) a node in the Freeplane UI.
   * @param {string} nodeId - ID of the node to focus.
   * @returns {Promise<boolean>}
   */
  async focusNode(nodeId) {
    const stub = this._getStub();
    const resp = await this._call(stub.FocusNode, { nodeId });
    return resp.success;
  }

  /**
   * Set the status bar info in Freeplane.
   * @param {string} info - Status text to display.
   * @returns {Promise<boolean>}
   */
  async setStatusInfo(info) {
    const stub = this._getStub();
    const resp = await this._call(stub.StatusInfoSet, { statusInfo: info });
    return resp.success;
  }

  // ---- Direct RPC wrappers (27 methods matching freeplane.proto) ----

  async createChild(name, parentNodeId) {
    const stub = this._getStub();
    return this._call(stub.CreateChild, { name, parentNodeId });
  }

  async deleteChild(nodeId) {
    const stub = this._getStub();
    return this._call(stub.DeleteChild, { nodeId });
  }

  async nodeAttributeAdd(nodeId, attributeName, attributeValue) {
    const stub = this._getStub();
    return this._call(stub.NodeAttributeAdd, { nodeId, attributeName, attributeValue });
  }

  async nodeLinkSet(nodeId, link) {
    const stub = this._getStub();
    return this._call(stub.NodeLinkSet, { nodeId, link });
  }

  async nodeDetailsSet(nodeId, details) {
    const stub = this._getStub();
    return this._call(stub.NodeDetailsSet, { nodeId, details });
  }

  async nodeNoteSet(nodeId, note) {
    const stub = this._getStub();
    return this._call(stub.NodeNoteSet, { nodeId, note });
  }

  async nodeTagSet(nodeId, tags) {
    const stub = this._getStub();
    return this._call(stub.NodeTagSet, { nodeId, tags });
  }

  async nodeTagAdd(nodeId, tags) {
    const stub = this._getStub();
    return this._call(stub.NodeTagAdd, { nodeId, tags });
  }

  async nodeConnect(sourceNodeId, targetNodeId, relationship) {
    const stub = this._getStub();
    return this._call(stub.NodeConnect, { sourceNodeId, targetNodeId, relationship });
  }

  async nodeAddIcon(nodeId, iconName) {
    const stub = this._getStub();
    return this._call(stub.NodeAddIcon, { nodeId, iconName });
  }

  async nodeColorSet(nodeId, red, green, blue, alpha) {
    const stub = this._getStub();
    return this._call(stub.NodeColorSet, { nodeId, red, green, blue, alpha: alpha || 255 });
  }

  async nodeBackgroundColorSet(nodeId, red, green, blue, alpha) {
    const stub = this._getStub();
    return this._call(stub.NodeBackgroundColorSet, { nodeId, red, green, blue, alpha: alpha || 255 });
  }

  async textFSM(json) {
    const stub = this._getStub();
    return this._call(stub.TextFSM, { json });
  }

  async getNodeText(nodeId) {
    const stub = this._getStub();
    return this._call(stub.GetNodeText, { nodeId });
  }

  async getParentNode(nodeId) {
    const stub = this._getStub();
    return this._call(stub.GetParentNode, { nodeId });
  }

  async listChildNodes(nodeId) {
    const stub = this._getStub();
    return this._call(stub.ListChildNodes, { nodeId });
  }

  async getNodeNote(nodeId) {
    const stub = this._getStub();
    return this._call(stub.GetNodeNote, { nodeId });
  }

  async getNodeLink(nodeId) {
    const stub = this._getStub();
    return this._call(stub.GetNodeLink, { nodeId });
  }

  async setNodeText(nodeId, text) {
    const stub = this._getStub();
    return this._call(stub.SetNodeText, { nodeId, text });
  }

  async moveNode(nodeId, newParentNodeId) {
    const stub = this._getStub();
    return this._call(stub.MoveNode, { nodeId, newParentNodeId });
  }
}

module.exports = FreeplaneClient;
