/**
 * Unit tests for MindMap class.
 * All tests are mocked — no Freeplane server required.
 */

const MindMap = require('../../src/mindmap');
const Node = require('../../src/node');
const { FreeplaneOperationError } = require('../../src/exceptions');

describe('MindMap', () => {
  let mockClient;

  beforeEach(() => {
    mockClient = {
      getCurrentNode: jest.fn(),
      getParentNode: jest.fn(),
      createChild: jest.fn(),
      groovy: jest.fn(),
      openMap: jest.fn(),
      _getStub: jest.fn(),
      _call: jest.fn(),
    };
  });

  describe('constructor and properties', () => {
    test('stores client, mapId, and nodeId', () => {
      const mm = new MindMap(mockClient, 'map1', 'node1');
      expect(mm.client).toBe(mockClient);
      expect(mm.mapId).toBe('map1');
      expect(mm.nodeId).toBe('node1');
    });

    test('defaults to empty strings when not provided', () => {
      const mm = new MindMap(mockClient);
      expect(mm.mapId).toBe('');
      expect(mm.nodeId).toBe('');
    });
  });

  describe('root', () => {
    test('traverses up to find root node', async () => {
      // First call: node1 has parent parent1
      // Second call: parent1 has no parent (is root)
      mockClient.getParentNode
        .mockResolvedValueOnce({ success: true, parentNodeId: 'parent1' })
        .mockResolvedValueOnce({ success: true, parentNodeId: '' });

      const mm = new MindMap(mockClient, 'map1', 'node1');
      const root = await mm.root();
      expect(root.nodeId).toBe('parent1');
      expect(mockClient.getParentNode).toHaveBeenCalledTimes(2);
    });

    test('uses current nodeId when nodeId is set', async () => {
      mockClient.getParentNode.mockResolvedValue({ success: true, parentNodeId: '' });
      const mm = new MindMap(mockClient, 'map1', 'rootNode');
      const root = await mm.root();
      expect(root.nodeId).toBe('rootNode');
    });

    test('fetches current node when nodeId is empty', async () => {
      mockClient._getStub.mockReturnValue({ GetCurrentNode: jest.fn() });
      mockClient._call.mockResolvedValue({ success: true, nodeId: 'current1' });
      mockClient.getParentNode.mockResolvedValue({ success: true, parentNodeId: '' });

      const mm = new MindMap(mockClient, 'map1', '');
      const root = await mm.root();
      expect(root.nodeId).toBe('current1');
    });

    test('throws FreeplaneOperationError when no map is open', async () => {
      mockClient._getStub.mockReturnValue({ GetCurrentNode: jest.fn() });
      mockClient._call.mockResolvedValue({ success: false });

      const mm = new MindMap(mockClient, 'map1', '');
      await expect(mm.root()).rejects.toThrow(FreeplaneOperationError);
    });
  });

  describe('selectedNode', () => {
    test('returns the currently selected node', async () => {
      mockClient._getStub.mockReturnValue({ GetCurrentNode: jest.fn() });
      mockClient._call.mockResolvedValue({ success: true, nodeId: 'selected1' });

      const mm = new MindMap(mockClient, 'map1');
      const node = await mm.selectedNode();
      expect(node.nodeId).toBe('selected1');
    });

    test('throws when no node is selected', async () => {
      mockClient._getStub.mockReturnValue({ GetCurrentNode: jest.fn() });
      mockClient._call.mockResolvedValue({ success: false });

      const mm = new MindMap(mockClient, 'map1');
      await expect(mm.selectedNode()).rejects.toThrow(FreeplaneOperationError);
    });
  });

  describe('findNodes', () => {
    test('finds nodes matching the pattern', async () => {
      const testClient = {
        getParentNode: jest.fn().mockResolvedValue({ success: true, parentNodeId: '' }),
        getNodeText: jest.fn().mockResolvedValue({ text: 'Hello World' }),
        listChildNodes: jest.fn().mockResolvedValue({ success: true, children: [] }),
      };

      const mm = new MindMap(testClient, 'map1', 'root1');
      const results = await mm.findNodes('hello');
      expect(results).toHaveLength(1);
      expect(results[0].nodeId).toBe('root1');
    });

    test('returns empty array when no matches', async () => {
      const testClient = {
        getParentNode: jest.fn().mockResolvedValue({ success: true, parentNodeId: '' }),
        getNodeText: jest.fn().mockResolvedValue({ text: 'Hello World' }),
        listChildNodes: jest.fn().mockResolvedValue({ success: true, children: [] }),
      };

      const mm = new MindMap(testClient, 'map1', 'root1');
      const results = await mm.findNodes('xyz');
      expect(results).toHaveLength(0);
    });
  });

  describe('info', () => {
    test('returns map metadata', () => {
      const mm = new MindMap(mockClient, 'map1', 'node1');
      const info = mm.info();
      expect(info).toEqual({ mapId: 'map1', nodeId: 'node1' });
    });
  });

  describe('createNode', () => {
    test('creates node under root when no parentId given', async () => {
      mockClient.getParentNode.mockResolvedValue({ success: true, parentNodeId: '' });
      mockClient.createChild.mockResolvedValue({ nodeId: 'new1', nodeText: 'New Node' });

      const mm = new MindMap(mockClient, 'map1', 'root1');
      const node = await mm.createNode('New Node');
      expect(node.nodeId).toBe('new1');
      expect(mockClient.createChild).toHaveBeenCalledWith('New Node', 'root1');
    });

    test('uses provided parentId', async () => {
      mockClient.createChild.mockResolvedValue({ nodeId: 'new1', nodeText: 'New Node' });

      const mm = new MindMap(mockClient, 'map1', 'root1');
      const node = await mm.createNode('New Node', 'parent2');
      expect(node.nodeId).toBe('new1');
      expect(mockClient.createChild).toHaveBeenCalledWith('New Node', 'parent2');
    });
  });

  describe('createChild', () => {
    test('creates node under the given parent', async () => {
      mockClient.createChild.mockResolvedValue({ nodeId: 'child1', nodeText: 'Child' });
      const parentNode = new Node(mockClient, 'parent1');

      const mm = new MindMap(mockClient, 'map1');
      const child = await mm.createChild(parentNode, 'Child');
      expect(child.nodeId).toBe('child1');
      expect(mockClient.createChild).toHaveBeenCalledWith('Child', 'parent1');
    });
  });

  describe('save', () => {
    test('returns true without path', async () => {
      const mm = new MindMap(mockClient, 'map1');
      const result = await mm.save();
      expect(result).toBe(true);
    });

    test('calls groovy when path is provided', async () => {
      mockClient.groovy.mockResolvedValue('');
      const mm = new MindMap(mockClient, 'map1');
      await mm.save('/tmp/test.mm');
      expect(mockClient.groovy).toHaveBeenCalled();
    });
  });

  describe('export', () => {
    test('calls groovy for export', async () => {
      mockClient.groovy.mockResolvedValue('');
      const mm = new MindMap(mockClient, 'map1');
      const result = await mm.export('/tmp/test.png', 'png');
      expect(result).toBe(true);
    });
  });

  describe('importMap', () => {
    test('delegates to client.openMap', async () => {
      const mockMm = { mapId: 'imported', nodeId: 'n1' };
      mockClient.openMap.mockResolvedValue(mockMm);
      const mm = new MindMap(mockClient, 'map1');
      const result = await mm.importMap('/tmp/test.mm');
      expect(mockClient.openMap).toHaveBeenCalledWith('/tmp/test.mm');
    });
  });

  describe('toString', () => {
    test('returns formatted string', () => {
      const mm = new MindMap(mockClient, 'map1', 'node1');
      expect(mm.toString()).toBe('MindMap(mapId=map1, nodeId=node1)');
    });
  });
});
