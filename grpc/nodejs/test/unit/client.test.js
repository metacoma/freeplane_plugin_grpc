/**
 * Unit tests for FreeplaneClient.
 * All tests are mocked — no Freeplane server required.
 */

const FreeplaneClient = require('../../src/client');
const {
  FreeplaneConnectionError,
  FreeplaneOperationError,
} = require('../../src/exceptions');

describe('FreeplaneClient', () => {
  describe('constructor', () => {
    test('uses default host and port', () => {
      const client = new FreeplaneClient();
      expect(client.host).toBe('127.0.0.1');
      expect(client.port).toBe(50051);
    });

    test('accepts custom host and port', () => {
      const client = new FreeplaneClient({ host: '10.0.0.1', port: 9999 });
      expect(client.host).toBe('10.0.0.1');
      expect(client.port).toBe(9999);
    });

    test('reads FREEPLANE_HOST from environment', () => {
      const origHost = process.env.FREEPLANE_HOST;
      process.env.FREEPLANE_HOST = 'env-host';
      const client = new FreeplaneClient();
      expect(client.host).toBe('env-host');
      if (origHost === undefined) {
        delete process.env.FREEPLANE_HOST;
      } else {
        process.env.FREEPLANE_HOST = origHost;
      }
    });

    test('reads FREEPLANE_PORT from environment', () => {
      const origPort = process.env.FREEPLANE_PORT;
      process.env.FREEPLANE_PORT = '7777';
      const client = new FreeplaneClient();
      expect(client.port).toBe(7777);
      if (origPort === undefined) {
        delete process.env.FREEPLANE_PORT;
      } else {
        process.env.FREEPLANE_PORT = origPort;
      }
    });

    test('constructor options override environment variables', () => {
      const origHost = process.env.FREEPLANE_HOST;
      process.env.FREEPLANE_HOST = 'env-host';
      const client = new FreeplaneClient({ host: 'opt-host' });
      expect(client.host).toBe('opt-host');
      if (origHost === undefined) {
        delete process.env.FREEPLANE_HOST;
      } else {
        process.env.FREEPLANE_HOST = origHost;
      }
    });
  });

  describe('close', () => {
    test('does not throw when channel is null', async () => {
      const client = new FreeplaneClient();
      await expect(client.close()).resolves.toBeUndefined();
    });

    test('clears stub reference', async () => {
      const client = new FreeplaneClient();
      client._stub = { some: 'stub' };
      await client.close();
      expect(client._stub).toBeNull();
    });
  });

  describe('_getStub', () => {
    test('throws FreeplaneConnectionError when not connected', () => {
      const client = new FreeplaneClient();
      expect(() => client._getStub()).toThrow(FreeplaneConnectionError);
    });

    test('returns stub when connected', () => {
      const client = new FreeplaneClient();
      client._stub = { test: true };
      expect(client._getStub()).toEqual({ test: true });
    });
  });

  describe('_call', () => {
    test('resolves with response on success', async () => {
      const client = new FreeplaneClient();
      const mockMethod = jest.fn((req, opts, callback) => {
        callback(null, { success: true, data: 'ok' });
      });
      const result = await client._call(mockMethod, { test: true });
      expect(result).toEqual({ success: true, data: 'ok' });
      expect(mockMethod).toHaveBeenCalledWith({ test: true }, {}, expect.any(Function));
    });

    test('rejects with FreeplaneOperationError when success is false', async () => {
      const client = new FreeplaneClient();
      const mockMethod = jest.fn((req, opts, callback) => {
        callback(null, { success: false, errorMessage: 'bad thing' });
      });
      await expect(client._call(mockMethod, {})).rejects.toThrow(FreeplaneOperationError);
    });

    test('rejects with FreeplaneConnectionError on UNAVAILABLE', async () => {
      const client = new FreeplaneClient();
      const mockErr = { code: 14, details: 'Server unavailable' }; // UNAVAILABLE
      const mockMethod = jest.fn((req, opts, callback) => {
        callback(mockErr);
      });
      await expect(client._call(mockMethod, {})).rejects.toThrow(FreeplaneConnectionError);
    });

    test('rejects with FreeplaneConnectionError on DEADLINE_EXCEEDED', async () => {
      const client = new FreeplaneClient();
      const mockErr = { code: 4, details: 'Deadline exceeded' };
      const mockMethod = jest.fn((req, opts, callback) => {
        callback(mockErr);
      });
      await expect(client._call(mockMethod, {})).rejects.toThrow(FreeplaneConnectionError);
    });

    test('rejects with FreeplaneOperationError on OTHER errors', async () => {
      const client = new FreeplaneClient();
      const mockErr = { code: 2, details: 'Not found' }; // NOT_FOUND
      const mockMethod = jest.fn((req, opts, callback) => {
        callback(mockErr);
      });
      await expect(client._call(mockMethod, {})).rejects.toThrow(FreeplaneOperationError);
    });

    test('passes timeout as deadline option', async () => {
      const client = new FreeplaneClient();
      const mockMethod = jest.fn((req, opts, callback) => {
        callback(null, { success: true });
      });
      await client._call(mockMethod, {}, 5000);
      expect(mockMethod).toHaveBeenCalledWith({}, expect.objectContaining({ deadline: expect.any(Date) }), expect.any(Function));
    });

    test('does not set deadline when no timeout provided', async () => {
      const client = new FreeplaneClient();
      const mockMethod = jest.fn((req, opts, callback) => {
        callback(null, { success: true });
      });
      await client._call(mockMethod, {});
      const callOpts = mockMethod.mock.calls[0][1];
      expect(callOpts).toEqual({});
    });
  });

  describe('RPC wrappers', () => {
    let client;
    let mockStub;

    beforeEach(() => {
      client = new FreeplaneClient();
      mockStub = {};
      client._stub = mockStub;
    });

    const rpcMethods = [
      { name: 'createChild', stubMethod: 'CreateChild', args: ['child', 'parent1'] },
      { name: 'deleteChild', stubMethod: 'DeleteChild', args: ['node1'] },
      { name: 'nodeAttributeAdd', stubMethod: 'NodeAttributeAdd', args: ['node1', 'key', 'val'] },
      { name: 'nodeLinkSet', stubMethod: 'NodeLinkSet', args: ['node1', 'http://example.com'] },
      { name: 'nodeDetailsSet', stubMethod: 'NodeDetailsSet', args: ['node1', 'details'] },
      { name: 'nodeNoteSet', stubMethod: 'NodeNoteSet', args: ['node1', 'note'] },
      { name: 'nodeTagSet', stubMethod: 'NodeTagSet', args: ['node1', ['tag1']] },
      { name: 'nodeTagAdd', stubMethod: 'NodeTagAdd', args: ['node1', ['tag1']] },
      { name: 'nodeConnect', stubMethod: 'NodeConnect', args: ['src', 'tgt', 'rel'] },
      { name: 'nodeAddIcon', stubMethod: 'NodeAddIcon', args: ['node1', 'flag'] },
      { name: 'nodeColorSet', stubMethod: 'NodeColorSet', args: ['node1', 255, 0, 0] },
      { name: 'nodeBackgroundColorSet', stubMethod: 'NodeBackgroundColorSet', args: ['node1', 0, 255, 0] },
      { name: 'textFSM', stubMethod: 'TextFSM', args: ['{}'] },
      { name: 'getNodeText', stubMethod: 'GetNodeText', args: ['node1'] },
      { name: 'getParentNode', stubMethod: 'GetParentNode', args: ['node1'] },
      { name: 'listChildNodes', stubMethod: 'ListChildNodes', args: ['node1'] },
      { name: 'getNodeNote', stubMethod: 'GetNodeNote', args: ['node1'] },
      { name: 'getNodeLink', stubMethod: 'GetNodeLink', args: ['node1'] },
      { name: 'setNodeText', stubMethod: 'SetNodeText', args: ['node1', 'text'] },
      { name: 'moveNode', stubMethod: 'MoveNode', args: ['node1', 'parent1'] },
    ];

    rpcMethods.forEach(({ name, stubMethod, args }) => {
      test(`${name} calls the correct stub method`, async () => {
        const mockFn = jest.fn((req, opts, cb) => cb(null, { success: true }));
        mockStub[stubMethod] = mockFn;

        await client[name](...args);
        expect(mockFn).toHaveBeenCalled();
      });
    });

    test('groovy calls the Groovy stub method', async () => {
      const mockFn = jest.fn((req, opts, cb) => cb(null, { success: true, result: 'output' }));
      mockStub.Groovy = mockFn;

      const result = await client.groovy('println "hello"');
      expect(result).toBe('output');
      expect(mockFn).toHaveBeenCalledWith(
        { groovyCode: 'println "hello"' },
        {},
        expect.any(Function)
      );
    });

    test('focusNode calls the FocusNode stub method', async () => {
      const mockFn = jest.fn((req, opts, cb) => cb(null, { success: true }));
      mockStub.FocusNode = mockFn;

      const result = await client.focusNode('node1');
      expect(result).toBe(true);
    });

    test('setStatusInfo calls the StatusInfoSet stub method', async () => {
      const mockFn = jest.fn((req, opts, cb) => cb(null, { success: true }));
      mockStub.StatusInfoSet = mockFn;

      const result = await client.setStatusInfo('hello');
      expect(result).toBe(true);
    });
  });
});
