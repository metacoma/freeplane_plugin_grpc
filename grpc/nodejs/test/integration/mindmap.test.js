/**
 * Integration tests for MindMap class.
 * Skipped when FREEPLANE_HOST is not set.
 */

const { getRealClient, uniqueName, shouldRunIntegration } = require('./integration-helper');

const describeInt = shouldRunIntegration() ? describe : describe.skip;

describeInt('MindMap Integration', () => {
  let client;

  beforeAll(async () => {
    client = getRealClient();
    await client.connect();
  }, 30000);

  afterAll(async () => {
    if (client) {
      await client.close();
    }
  }, 10000);

  test('currentMap returns a MindMap', async () => {
    const map = await client.currentMap();
    expect(map).toBeDefined();
    expect(map.mapId).toBeDefined();
  });

  test('root returns the root node', async () => {
    const map = await client.currentMap();
    const root = await map.root();
    expect(root.nodeId).toBeDefined();
  });

  test('selectedNode returns the selected node', async () => {
    const map = await client.currentMap();
    const node = await map.selectedNode();
    expect(node.nodeId).toBeDefined();
  });

  test('createNode creates a node under root', async () => {
    const map = await client.currentMap();
    const name = uniqueName('MM');
    const node = await map.createNode(name);
    expect(node.nodeId).toBeDefined();
    const text = await node.getText();
    expect(text).toBe(name);
    await node.delete();
  });

  test('findNodes finds matching nodes', async () => {
    const map = await client.currentMap();
    const name = uniqueName('FindMe');
    const root = await map.root();
    const node = await root.addChild(name);

    const results = await map.findNodes('FindMe');
    expect(results.length).toBeGreaterThanOrEqual(1);

    await node.delete();
  });

  test('info returns map metadata', async () => {
    const map = await client.currentMap();
    const info = map.info();
    expect(info).toHaveProperty('mapId');
    expect(info).toHaveProperty('nodeId');
  });

  test('toString returns formatted string', async () => {
    const map = await client.currentMap();
    expect(map.toString()).toContain('MindMap(');
  });
});
