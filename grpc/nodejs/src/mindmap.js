/**
 * MindMap - high-level representation of a Freeplane mind map.
 *
 * Provides map-level operations such as getting the root node,
 * searching nodes, and exporting/importing the map.
 */

const { FreeplaneOperationError, NodeNotFoundError } = require('./exceptions');
const Node = require('./node');

/**
 * Represents a Freeplane mind map.
 */
class MindMap {
  /**
   * Create a new MindMap instance.
   * @param {FreeplaneClient} client - The FreeplaneClient to use for gRPC calls.
   * @param {string} [mapId] - The ID of the mind map on the server.
   * @param {string} [nodeId] - The ID of the currently focused/selected node.
   */
  constructor(client, mapId, nodeId) {
    this._client = client;
    this._mapId = mapId || '';
    this._nodeId = nodeId || '';
  }

  get client() {
    return this._client;
  }

  get mapId() {
    return this._mapId;
  }

  get nodeId() {
    return this._nodeId;
  }

  // ---- navigation ----

  /**
   * Get the root node of this mind map.
   * @returns {Promise<Node>}
   */
  async root() {
    let currentId = this._nodeId;
    if (!currentId) {
      const resp = await this._client._call(
        this._client._getStub().GetCurrentNode, {}
      );
      if (resp.success) {
        currentId = resp.nodeId;
      } else {
        throw new FreeplaneOperationError('No map currently open to get root from');
      }
    }

    // Traverse up to find root
    while (currentId) {
      const parentResp = await this._client.getParentNode(currentId);
      if (!parentResp.success || !parentResp.parentNodeId) {
        return new Node(this._client, currentId, this);
      }
      currentId = parentResp.parentNodeId;
    }

    return new Node(this._client, this._nodeId, this);
  }

  /**
   * Get the currently selected/focused node.
   * @returns {Promise<Node>}
   */
  async selectedNode() {
    const resp = await this._client._call(
      this._client._getStub().GetCurrentNode, {}
    );
    if (!resp.success || !resp.nodeId) {
      throw new FreeplaneOperationError('No node currently selected');
    }
    return new Node(this._client, resp.nodeId, this);
  }

  /**
   * Find all nodes whose text contains the given pattern.
   * @param {string} pattern - Text pattern to search for (case-insensitive).
   * @returns {Promise<Node[]>}
   */
  async findNodes(pattern) {
    const rootNode = await this.root();
    const matches = [];
    await this._walkAndCollect(rootNode, pattern, matches);
    return matches;
  }

  /**
   * Recursively walk the tree and collect matching nodes.
   * @private
   */
  async _walkAndCollect(node, pattern, matches) {
    try {
      const text = await node.getText();
      if (text.toLowerCase().includes(pattern.toLowerCase())) {
        matches.push(node);
      }
    } catch (_) {
      // Skip nodes we can't read
    }

    try {
      const children = await node.children();
      for (const child of children) {
        await this._walkAndCollect(child, pattern, matches);
      }
    } catch (_) {
      // No children or error accessing them
    }
  }

  // ---- metadata ----

  /**
   * Get basic information about the current map.
   * @returns {object}
   */
  info() {
    return {
      mapId: this._mapId,
      nodeId: this._nodeId,
    };
  }

  /**
   * Estimate the number of nodes in the mind map.
   * @returns {Promise<number>}
   */
  async size() {
    const rootNode = await this.root();
    return this._countNodes(rootNode);
  }

  /**
   * Recursively count nodes.
   * @private
   */
  async _countNodes(node) {
    let count = 1;
    try {
      const children = await node.children();
      for (const child of children) {
        count += await this._countNodes(child);
      }
    } catch (_) {
      // ignore
    }
    return count;
  }

  // ---- file operations ----

  /**
   * Save the current mind map.
   * @param {string} [path] - Optional path to save to.
   * @returns {Promise<boolean>}
   */
  async save(path) {
    if (path) {
      await this._client.groovy(
        `model.getMap().getFile().setFile(new File("${path}"));` +
        `model.getMap().getController().getUndoManager().undoableChanges(model.getMap());` +
        `model.getMap().getController().getMapView().updateFileHistory(model.getMap());`
      );
    }
    return true;
  }

  /**
   * Export the current mind map to a file.
   * @param {string} outputPath - Output file path.
   * @param {string} [format='png'] - Export format.
   * @returns {Promise<boolean>}
   */
  async export(outputPath, format) {
    const fmt = format || 'png';
    const result = await this._client.groovy(
      `def controller = model.getMap().getController();` +
      `def view = controller.getMapView();` +
      `view.exportMap(new File('${outputPath}'), '${fmt}');`
    );
    return !result.includes('Error');
  }

  /**
   * Import a mind map from a file.
   * @param {string} filePath - Path to the map file.
   * @returns {Promise<MindMap>}
   */
  async importMap(filePath) {
    return this._client.openMap(filePath);
  }

  // ---- node creation ----

  /**
   * Create a new node in the mind map.
   * @param {string} text - The node text.
   * @param {string} [parentId] - ID of the parent node. If empty, creates under root.
   * @returns {Promise<Node>}
   */
  async createNode(text, parentId) {
    if (!parentId) {
      const rootNode = await this.root();
      parentId = rootNode.nodeId;
    }
    const resp = await this._client.createChild(text, parentId);
    return new Node(this._client, resp.nodeId, this);
  }

  /**
   * Create a child node under an existing node.
   * @param {Node} parent - The parent Node instance.
   * @param {string} text - The node text.
   * @returns {Promise<Node>}
   */
  async createChild(parent, text) {
    return this.createNode(text, parent.nodeId);
  }

  // ---- convenience ----

  toString() {
    return `MindMap(mapId=${this._mapId}, nodeId=${this._nodeId})`;
  }
}

module.exports = MindMap;
