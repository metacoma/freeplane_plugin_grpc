/**
 * Node - high-level representation of a Freeplane mind map node.
 *
 * Provides node-level operations such as getting/setting text,
 * managing children, styling, notes, attributes, links, and icons.
 */

const { FreeplaneOperationError, NodeNotFoundError } = require('./exceptions');

/**
 * Represents a Freeplane mind map node.
 */
class Node {
  /**
   * Create a new Node instance.
   * @param {FreeplaneClient} client - The FreeplaneClient to use for gRPC calls.
   * @param {string} nodeId - The server-side node ID.
   * @param {MindMap} [mindmap] - Optional parent MindMap reference.
   */
  constructor(client, nodeId, mindmap) {
    this._client = client;
    this._nodeId = nodeId;
    this._mindmap = mindmap;
  }

  get client() {
    return this._client;
  }

  get nodeId() {
    return this._nodeId;
  }

  get mindmap() {
    return this._mindmap;
  }

  // ---- text ----

  /**
   * Get the text of this node.
   * @returns {Promise<string>}
   */
  async getText() {
    const resp = await this._client.getNodeText(this._nodeId);
    return resp.text;
  }

  /**
   * Set the text of this node.
   * @param {string} text - The new node text.
   * @returns {Promise<void>}
   */
  async setText(text) {
    await this._client.setNodeText(this._nodeId, text);
  }

  // ---- hierarchy ----

  /**
   * Add a child node to this node.
   * @param {string} text - The child node text.
   * @returns {Promise<Node>} The newly created child node.
   */
  async addChild(text) {
    const resp = await this._client.createChild(text, this._nodeId);
    const child = new Node(this._client, resp.nodeId, this._mindmap);
    if (this._mindmap) {
      this._mindmap._nodeId = resp.nodeId;
    }
    return child;
  }

  /**
   * Get the direct children of this node.
   * @returns {Promise<Node[]>}
   */
  async children() {
    const resp = await this._client.listChildNodes(this._nodeId);
    return (resp.children || []).map(child =>
      new Node(this._client, child.nodeId, this._mindmap)
    );
  }

  /**
   * Get the parent of this node.
   * @returns {Promise<Node>}
   * @throws {NodeNotFoundError} If this node has no parent (is root).
   */
  async parent() {
    const resp = await this._client.getParentNode(this._nodeId);
    if (!resp.success || !resp.parentNodeId) {
      throw new NodeNotFoundError(
        `Node ${this._nodeId} has no parent (is root)`
      );
    }
    return new Node(this._client, resp.parentNodeId, this._mindmap);
  }

  /**
   * Delete this node.
   * @returns {Promise<boolean>}
   */
  async delete() {
    const resp = await this._client.deleteChild(this._nodeId);
    return resp.success;
  }

  /**
   * Move this node under a new parent.
   * @param {string} newParentId - ID of the new parent node.
   * @returns {Promise<boolean>}
   */
  async move(newParentId) {
    const resp = await this._client.moveNode(this._nodeId, newParentId);
    return resp.success;
  }

  // ---- styling ----

  /**
   * Get the style information for this node.
   * @returns {Promise<object>}
   */
  async getStyle() {
    const groovyCode = [
      `def node = model.getNode('${this._nodeId}');`,
      `def style = node.style;`,
      `def result = [:];`,
      `if (style != null) {`,
      `  result = style.getProperties().collectEntries { k, v -> [k.toString(), v.toString()] }`,
      `}`,
    ].join('');
    const result = await this._client.groovy(groovyCode);
    return { style: result };
  }

  /**
   * Set the style of this node.
   * @param {string} style - Style name (e.g., "classic", "bubble", "flag").
   * @returns {Promise<boolean>}
   */
  async setStyle(style) {
    const groovyCode = [
      `def node = model.getNode('${this._nodeId}');`,
      `node.style = model.getStyleLib().getStyle('${style}');`,
    ].join('');
    const result = await this._client.groovy(groovyCode);
    return !result.includes('Error');
  }

  /**
   * Get the foreground color of this node.
   * @returns {Promise<object>}
   */
  async getColor() {
    const groovyCode = [
      `def node = model.getNode('${this._nodeId}');`,
      `def color = node.style.foregroundColor;`,
      `color ? [red:color.red, green:color.green, blue:color.blue, alpha:color.alpha] : [:]`,
    ].join('');
    const result = await this._client.groovy(groovyCode);
    return { color: result };
  }

  /**
   * Set the foreground color of this node.
   * @param {number} red
   * @param {number} green
   * @param {number} blue
   * @param {number} [alpha=255]
   * @returns {Promise<boolean>}
   */
  async setColor(red, green, blue, alpha) {
    const resp = await this._client.nodeColorSet(this._nodeId, red, green, blue, alpha);
    return resp.success;
  }

  /**
   * Get the background color of this node.
   * @returns {Promise<object>}
   */
  async getBackgroundColor() {
    const groovyCode = [
      `def node = model.getNode('${this._nodeId}');`,
      `def color = node.style.backgroundColor;`,
      `color ? [red:color.red, green:color.green, blue:color.blue, alpha:color.alpha] : [:]`,
    ].join('');
    const result = await this._client.groovy(groovyCode);
    return { backgroundColor: result };
  }

  /**
   * Set the background color of this node.
   * @param {number} red
   * @param {number} green
   * @param {number} blue
   * @param {number} [alpha=255]
   * @returns {Promise<boolean>}
   */
  async setBackgroundColor(red, green, blue, alpha) {
    const resp = await this._client.nodeBackgroundColorSet(this._nodeId, red, green, blue, alpha);
    return resp.success;
  }

  // ---- notes ----

  /**
   * Get the note of this node.
   * @returns {Promise<string>}
   */
  async getNote() {
    const resp = await this._client.getNodeNote(this._nodeId);
    return resp.note;
  }

  /**
   * Set the note of this node.
   * @param {string} note
   * @returns {Promise<boolean>}
   */
  async setNote(note) {
    const resp = await this._client.nodeNoteSet(this._nodeId, note);
    return resp.success;
  }

  // ---- attributes ----

  /**
   * Get the attributes of this node.
   * @returns {Promise<object>}
   */
  async getAttributes() {
    const groovyCode = [
      `def node = model.getNode('${this._nodeId}');`,
      `def attrs = node.attributes;`,
      `attrs ? attrs.collectEntries { k, v -> [k.toString(), v.toString()] } : [:]`,
    ].join('');
    const result = await this._client.groovy(groovyCode);
    return { attributes: result };
  }

  /**
   * Set a custom attribute on this node.
   * @param {string} name - Attribute name.
   * @param {string} value - Attribute value.
   * @returns {Promise<boolean>}
   */
  async setAttribute(name, value) {
    const resp = await this._client.nodeAttributeAdd(this._nodeId, name, value);
    return resp.success;
  }

  /**
   * Set multiple custom attributes on this node.
   * @param {object} attrs - Dictionary of attribute name-value pairs.
   * @returns {Promise<boolean>}
   */
  async setAttributes(attrs) {
    for (const [name, value] of Object.entries(attrs)) {
      const result = await this.setAttribute(name, String(value));
      if (!result) return false;
    }
    return true;
  }

  // ---- links ----

  /**
   * Get the links of this node.
   * @returns {Promise<string[]>}
   */
  async getLinks() {
    const resp = await this._client.getNodeLink(this._nodeId);
    return resp.hasLink ? [resp.link] : [];
  }

  /**
   * Set the links of this node (replaces existing links).
   * @param {string[]} links - List of link URLs.
   * @returns {Promise<boolean>}
   */
  async setLinks(links) {
    for (const link of links) {
      await this._client.nodeLinkSet(this._nodeId, link);
    }
    return true;
  }

  // ---- tags ----

  /**
   * Set the tags of this node (replaces existing tags).
   * @param {string[]} tags - List of tag strings.
   * @returns {Promise<boolean>}
   */
  async setTags(tags) {
    const resp = await this._client.nodeTagSet(this._nodeId, tags);
    return resp.success;
  }

  /**
   * Add tags to this node (does not remove existing tags).
   * @param {string[]} tags - List of tag strings to add.
   * @returns {Promise<boolean>}
   */
  async addTags(tags) {
    const resp = await this._client.nodeTagAdd(this._nodeId, tags);
    return resp.success;
  }

  // ---- icons ----

  /**
   * Get the icons of this node.
   * @returns {Promise<object>}
   */
  async getIcons() {
    const groovyCode = [
      `def node = model.getNode('${this._nodeId}');`,
      `def icons = node.getIconIds();`,
      `icons ? icons.toList() : []`,
    ].join('');
    const result = await this._client.groovy(groovyCode);
    return { icons: result };
  }

  /**
   * Add an icon to this node.
   * @param {string} iconName - Name of the icon to add.
   * @returns {Promise<boolean>}
   */
  async addIcon(iconName) {
    const resp = await this._client.nodeAddIcon(this._nodeId, iconName);
    return resp.success;
  }

  // ---- state ----

  /**
   * Get the folded (collapsed) state of this node.
   * @returns {Promise<boolean>}
   */
  async getFolded() {
    const groovyCode = [
      `def node = model.getNode('${this._nodeId}');`,
      `node.isFolded()`,
    ].join('');
    const result = await this._client.groovy(groovyCode);
    return result.toLowerCase().includes('true');
  }

  /**
   * Set the folded (collapsed) state of this node.
   * @param {boolean} folded - True to fold, false to unfold.
   * @returns {Promise<boolean>}
   */
  async setFolded(folded) {
    const groovyCode = [
      `def node = model.getNode('${this._nodeId}');`,
      `if (${folded}) node.fold() else node.unfold();`,
    ].join('');
    const result = await this._client.groovy(groovyCode);
    return !result.includes('Error');
  }

  // ---- actions ----

  /**
   * Select this node in the Freeplane UI.
   * @returns {Promise<boolean>}
   */
  async select() {
    return this._client.focusNode(this._nodeId);
  }

  /**
   * Center the view on this node.
   * @returns {Promise<boolean>}
   */
  async center() {
    return this._client.focusNode(this._nodeId);
  }

  // ---- refresh ----

  /**
   * Reload this node's state from the server.
   * @returns {Promise<void>}
   */
  async refresh() {
    await this.getText();
  }

  // ---- convenience ----

  toString() {
    return `Node(id=${this._nodeId})`;
  }

  valueOf() {
    return this._nodeId;
  }
}

module.exports = Node;
