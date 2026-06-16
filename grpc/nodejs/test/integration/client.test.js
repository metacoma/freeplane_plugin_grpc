/**
 * Integration tests for FreeplaneClient.
 * Skipped when FREEPLANE_HOST is not set.
 */

const { getRealClient, uniqueName, shouldRunIntegration } = require('./integration-helper');

const describeInt = shouldRunIntegration() ? describe : describe.skip;

describeInt('FreeplaneClient Integration', () => {
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

  test('can get current map', async () => {
    const map = await client.currentMap();
    expect(map).toBeDefined();
    expect(map.mapId).toBeDefined();
  }, 15000);

  test('can create and read a child node', async () => {
    const map = await client.currentMap();
    const root = await map.root();
    const name = uniqueName('IT');
    const child = await root.addChild(name);
    expect(child.nodeId).toBeDefined();

    const text = await child.getText();
    expect(text).toBe(name);

    // Cleanup
    await child.delete();
  }, 15000);

  test('can set and get node text', async () => {
    const map = await client.currentMap();
    const root = await map.root();
    const name = uniqueName('IT');
    const child = await root.addChild(name);

    await child.setText('Modified Text');
    const text = await child.getText();
    expect(text).toBe('Modified Text');

    // Cleanup
    await child.delete();
  }, 15000);

  test('can list children', async () => {
    const map = await client.currentMap();
    const root = await map.root();
    const name1 = uniqueName('IT');
    const name2 = uniqueName('IT');

    const child1 = await root.addChild(name1);
    const child2 = await root.addChild(name2);

    const children = await root.children();
    const childIds = children.map(c => c.nodeId);
    expect(childIds).toContain(child1.nodeId);
    expect(childIds).toContain(child2.nodeId);

    // Cleanup
    await child1.delete();
    await child2.delete();
  }, 15000);

  test('can get parent node', async () => {
    const map = await client.currentMap();
    const root = await map.root();
    const name = uniqueName('IT');
    const child = await root.addChild(name);

    const parent = await child.parent();
    expect(parent.nodeId).toBe(root.nodeId);

    // Cleanup
    await child.delete();
  }, 15000);

  test('can set and get node note', async () => {
    const map = await client.currentMap();
    const root = await map.root();
    const name = uniqueName('IT');
    const child = await root.addChild(name);

    await child.setNote('Integration test note');
    const note = await child.getNote();
    expect(note).toBe('Integration test note');

    // Cleanup
    await child.delete();
  }, 15000);

  test('can set and get node tags', async () => {
    const map = await client.currentMap();
    const root = await map.root();
    const name = uniqueName('IT');
    const child = await root.addChild(name);

    await child.setTags(['tag1', 'tag2']);

    // Cleanup
    await child.delete();
  }, 15000);

  test('can add tags to node', async () => {
    const map = await client.currentMap();
    const root = await map.root();
    const name = uniqueName('IT');
    const child = await root.addChild(name);

    await child.addTags(['extra-tag']);

    // Cleanup
    await child.delete();
  }, 15000);

  test('can set node color', async () => {
    const map = await client.currentMap();
    const root = await map.root();
    const name = uniqueName('IT');
    const child = await root.addChild(name);

    const result = await child.setColor(255, 0, 0, 255);
    expect(result).toBe(true);

    // Cleanup
    await child.delete();
  }, 15000);

  test('can set node attribute', async () => {
    const map = await client.currentMap();
    const root = await map.root();
    const name = uniqueName('IT');
    const child = await root.addChild(name);

    const result = await child.setAttribute('test-key', 'test-value');
    expect(result).toBe(true);

    // Cleanup
    await child.delete();
  }, 15000);

  test('can export and import JSON', async () => {
    const json = await client.getMapToJson();
    expect(json).toBeDefined();
    expect(json.length).toBeGreaterThan(0);

    // Parse to verify it's valid JSON
    const parsed = JSON.parse(json);
    expect(parsed).toBeDefined();
  }, 15000);

  test('can set status info', async () => {
    const result = await client.setStatusInfo('Integration test status');
    expect(result).toBe(true);
  }, 15000);

  test('can focus a node', async () => {
    const map = await client.currentMap();
    const root = await map.root();
    const result = await client.focusNode(root.nodeId);
    expect(result).toBe(true);
  }, 15000);
});
