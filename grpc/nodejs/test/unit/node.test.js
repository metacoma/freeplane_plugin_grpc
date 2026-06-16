/**
 * Unit tests for Node class.
 * All tests are mocked — no Freeplane server required.
 */

const Node = require('../../src/node');
const { NodeNotFoundError } = require('../../src/exceptions');

describe('Node', () => {
  let mockClient;
  let mockMindmap;

  beforeEach(() => {
    mockClient = {
      getNodeText: jest.fn(),
      setNodeText: jest.fn(),
      createChild: jest.fn(),
      listChildNodes: jest.fn(),
      getParentNode: jest.fn(),
      deleteChild: jest.fn(),
      moveNode: jest.fn(),
      groovy: jest.fn(),
      nodeColorSet: jest.fn(),
      nodeBackgroundColorSet: jest.fn(),
      getNodeNote: jest.fn(),
      nodeNoteSet: jest.fn(),
      nodeAttributeAdd: jest.fn(),
      getNodeLink: jest.fn(),
      nodeLinkSet: jest.fn(),
      nodeTagSet: jest.fn(),
      nodeTagAdd: jest.fn(),
      nodeAddIcon: jest.fn(),
      focusNode: jest.fn(),
    };
    mockMindmap = { _nodeId: 'root1' };
  });

  describe('constructor and properties', () => {
    test('stores client, nodeId, and mindmap', () => {
      const node = new Node(mockClient, 'node1', mockMindmap);
      expect(node.client).toBe(mockClient);
      expect(node.nodeId).toBe('node1');
      expect(node.mindmap).toBe(mockMindmap);
    });

    test('mindmap can be undefined', () => {
      const node = new Node(mockClient, 'node1');
      expect(node.mindmap).toBeUndefined();
    });
  });

  describe('getText', () => {
    test('returns text from client response', async () => {
      mockClient.getNodeText.mockResolvedValue({ text: 'Hello World' });
      const node = new Node(mockClient, 'node1');
      const text = await node.getText();
      expect(text).toBe('Hello World');
      expect(mockClient.getNodeText).toHaveBeenCalledWith('node1');
    });
  });

  describe('setText', () => {
    test('calls client.setNodeText with nodeId and text', async () => {
      mockClient.setNodeText.mockResolvedValue({ success: true });
      const node = new Node(mockClient, 'node1');
      await node.setText('New Text');
      expect(mockClient.setNodeText).toHaveBeenCalledWith('node1', 'New Text');
    });
  });

  describe('addChild', () => {
    test('returns a new Node with the created child id', async () => {
      mockClient.createChild.mockResolvedValue({ nodeId: 'child1', nodeText: 'Child' });
      const node = new Node(mockClient, 'node1', mockMindmap);
      const child = await node.addChild('Child');
      expect(child.nodeId).toBe('child1');
      expect(child.client).toBe(mockClient);
      expect(child.mindmap).toBe(mockMindmap);
      expect(mockClient.createChild).toHaveBeenCalledWith('Child', 'node1');
    });

    test('updates mindmap nodeId when mindmap exists', async () => {
      mockClient.createChild.mockResolvedValue({ nodeId: 'child1', nodeText: 'Child' });
      const node = new Node(mockClient, 'node1', mockMindmap);
      await node.addChild('Child');
      expect(mockMindmap._nodeId).toBe('child1');
    });
  });

  describe('children', () => {
    test('returns array of Node instances', async () => {
      mockClient.listChildNodes.mockResolvedValue({
        success: true,
        children: [
          { nodeId: 'c1', text: 'Child 1' },
          { nodeId: 'c2', text: 'Child 2' },
        ],
      });
      const node = new Node(mockClient, 'node1', mockMindmap);
      const children = await node.children();
      expect(children).toHaveLength(2);
      expect(children[0].nodeId).toBe('c1');
      expect(children[1].nodeId).toBe('c2');
    });

    test('returns empty array when no children', async () => {
      mockClient.listChildNodes.mockResolvedValue({ success: true, children: [] });
      const node = new Node(mockClient, 'node1');
      const children = await node.children();
      expect(children).toHaveLength(0);
    });
  });

  describe('parent', () => {
    test('returns parent Node', async () => {
      mockClient.getParentNode.mockResolvedValue({
        success: true,
        parentNodeId: 'parent1',
        parentNodeText: 'Parent',
      });
      const node = new Node(mockClient, 'node1', mockMindmap);
      const parent = await node.parent();
      expect(parent.nodeId).toBe('parent1');
    });

    test('throws NodeNotFoundError when no parent', async () => {
      mockClient.getParentNode.mockResolvedValue({
        success: true,
        parentNodeId: '',
      });
      const node = new Node(mockClient, 'node1');
      await expect(node.parent()).rejects.toThrow(NodeNotFoundError);
    });
  });

  describe('delete', () => {
    test('returns success from client', async () => {
      mockClient.deleteChild.mockResolvedValue({ success: true });
      const node = new Node(mockClient, 'node1');
      const result = await node.delete();
      expect(result).toBe(true);
      expect(mockClient.deleteChild).toHaveBeenCalledWith('node1');
    });
  });

  describe('move', () => {
    test('returns success from client', async () => {
      mockClient.moveNode.mockResolvedValue({ success: true });
      const node = new Node(mockClient, 'node1');
      const result = await node.move('newParent');
      expect(result).toBe(true);
      expect(mockClient.moveNode).toHaveBeenCalledWith('node1', 'newParent');
    });
  });

  describe('setStyle', () => {
    test('calls groovy and returns true on success', async () => {
      mockClient.groovy.mockResolvedValue('');
      const node = new Node(mockClient, 'node1');
      const result = await node.setStyle('bubble');
      expect(result).toBe(true);
      expect(mockClient.groovy).toHaveBeenCalled();
    });

    test('returns false when groovy returns error', async () => {
      mockClient.groovy.mockResolvedValue('Error: something went wrong');
      const node = new Node(mockClient, 'node1');
      const result = await node.setStyle('bubble');
      expect(result).toBe(false);
    });
  });

  describe('setColor', () => {
    test('calls client.nodeColorSet', async () => {
      mockClient.nodeColorSet.mockResolvedValue({ success: true });
      const node = new Node(mockClient, 'node1');
      const result = await node.setColor(255, 0, 0, 255);
      expect(result).toBe(true);
      expect(mockClient.nodeColorSet).toHaveBeenCalledWith('node1', 255, 0, 0, 255);
    });
  });

  describe('setBackgroundColor', () => {
    test('calls client.nodeBackgroundColorSet', async () => {
      mockClient.nodeBackgroundColorSet.mockResolvedValue({ success: true });
      const node = new Node(mockClient, 'node1');
      const result = await node.setBackgroundColor(0, 255, 0);
      expect(result).toBe(true);
    });
  });

  describe('getNote', () => {
    test('returns note from client response', async () => {
      mockClient.getNodeNote.mockResolvedValue({ note: 'My note', hasNote: true });
      const node = new Node(mockClient, 'node1');
      const note = await node.getNote();
      expect(note).toBe('My note');
    });
  });

  describe('setNote', () => {
    test('returns success from client', async () => {
      mockClient.nodeNoteSet.mockResolvedValue({ success: true });
      const node = new Node(mockClient, 'node1');
      const result = await node.setNote('My note');
      expect(result).toBe(true);
    });
  });

  describe('setAttribute', () => {
    test('returns success from client', async () => {
      mockClient.nodeAttributeAdd.mockResolvedValue({ success: true });
      const node = new Node(mockClient, 'node1');
      const result = await node.setAttribute('key', 'value');
      expect(result).toBe(true);
    });
  });

  describe('setAttributes', () => {
    test('sets multiple attributes', async () => {
      mockClient.nodeAttributeAdd.mockResolvedValue({ success: true });
      const node = new Node(mockClient, 'node1');
      const result = await node.setAttributes({ key1: 'val1', key2: 'val2' });
      expect(result).toBe(true);
      expect(mockClient.nodeAttributeAdd).toHaveBeenCalledTimes(2);
    });
  });

  describe('getLinks', () => {
    test('returns array with link when hasLink is true', async () => {
      mockClient.getNodeLink.mockResolvedValue({ link: 'http://example.com', hasLink: true });
      const node = new Node(mockClient, 'node1');
      const links = await node.getLinks();
      expect(links).toEqual(['http://example.com']);
    });

    test('returns empty array when hasLink is false', async () => {
      mockClient.getNodeLink.mockResolvedValue({ link: '', hasLink: false });
      const node = new Node(mockClient, 'node1');
      const links = await node.getLinks();
      expect(links).toEqual([]);
    });
  });

  describe('setTags', () => {
    test('returns success from client', async () => {
      mockClient.nodeTagSet.mockResolvedValue({ success: true });
      const node = new Node(mockClient, 'node1');
      const result = await node.setTags(['tag1', 'tag2']);
      expect(result).toBe(true);
    });
  });

  describe('addTags', () => {
    test('returns success from client', async () => {
      mockClient.nodeTagAdd.mockResolvedValue({ success: true });
      const node = new Node(mockClient, 'node1');
      const result = await node.addTags(['tag1']);
      expect(result).toBe(true);
    });
  });

  describe('addIcon', () => {
    test('returns success from client', async () => {
      mockClient.nodeAddIcon.mockResolvedValue({ success: true });
      const node = new Node(mockClient, 'node1');
      const result = await node.addIcon('flag');
      expect(result).toBe(true);
    });
  });

  describe('select', () => {
    test('delegates to client.focusNode', async () => {
      mockClient.focusNode.mockResolvedValue(true);
      const node = new Node(mockClient, 'node1');
      const result = await node.select();
      expect(result).toBe(true);
      expect(mockClient.focusNode).toHaveBeenCalledWith('node1');
    });
  });

  describe('center', () => {
    test('delegates to client.focusNode', async () => {
      mockClient.focusNode.mockResolvedValue(true);
      const node = new Node(mockClient, 'node1');
      const result = await node.center();
      expect(result).toBe(true);
    });
  });

  describe('refresh', () => {
    test('calls getText to reload state', async () => {
      mockClient.getNodeText.mockResolvedValue({ text: 'updated' });
      const node = new Node(mockClient, 'node1');
      await node.refresh();
      expect(mockClient.getNodeText).toHaveBeenCalledWith('node1');
    });
  });

  describe('toString', () => {
    test('returns formatted string', () => {
      const node = new Node(mockClient, 'node1');
      expect(node.toString()).toBe('Node(id=node1)');
    });
  });
});
