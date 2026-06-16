/**
 * Integration tests for Node class.
 * Skipped when FREEPLANE_HOST is not set.
 */

const { getRealClient, uniqueName, shouldRunIntegration } = require('./integration-helper');

const describeInt = shouldRunIntegration() ? describe : describe.skip;

describeInt('Node Integration', () => {
  let client;
  let testNode;

  beforeAll(async () => {
    client = getRealClient();
    await client.connect();
  }, 30000);

  beforeEach(async () => {
    const map = await client.currentMap();
    const root = await map.root();
    testNode = await root.addChild(uniqueName('NodeIT'));
  }, 15000);

  afterEach(async () => {
    if (testNode) {
      try {
        await testNode.delete();
      } catch (_) {
        // ignore cleanup errors
      }
    }
  }, 10000);

  afterAll(async () => {
    if (client) {
      await client.close();
    }
  }, 10000);

  test('getText returns the node text', async () => {
    const text = await testNode.getText();
    expect(text).toContain('NodeIT');
  });

  test('setText updates the node text', async () => {
    await testNode.setText('Updated Integration Text');
    const text = await testNode.getText();
    expect(text).toBe('Updated Integration Text');
  });

  test('addChild creates a child and returns Node', async () => {
    const child = await testNode.addChild('Grandchild');
    expect(child.nodeId).toBeDefined();
    const text = await child.getText();
    expect(text).toBe('Grandchild');
    await child.delete();
  });

  test('children returns child nodes', async () => {
    const c1 = await testNode.addChild('Child1');
    const c2 = await testNode.addChild('Child2');
    const children = await testNode.children();
    expect(children.length).toBeGreaterThanOrEqual(2);
    await c1.delete();
    await c2.delete();
  });

  test('parent returns the parent node', async () => {
    const parent = await testNode.parent();
    expect(parent.nodeId).toBeDefined();
    expect(parent.nodeId).not.toBe(testNode.nodeId);
  });

  test('setNote and getNote work', async () => {
    await testNode.setNote('Integration note content');
    const note = await testNode.getNote();
    expect(note).toBe('Integration note content');
  });

  test('setAttribute works', async () => {
    const result = await testNode.setAttribute('int-key', 'int-value');
    expect(result).toBe(true);
  });

  test('setTags works', async () => {
    const result = await testNode.setTags(['int-tag1', 'int-tag2']);
    expect(result).toBe(true);
  });

  test('addTags works', async () => {
    const result = await testNode.addTags(['int-extra']);
    expect(result).toBe(true);
  });

  test('setColor works', async () => {
    const result = await testNode.setColor(0, 128, 255, 255);
    expect(result).toBe(true);
  });

  test('setBackgroundColor works', async () => {
    const result = await testNode.setBackgroundColor(255, 255, 0, 200);
    expect(result).toBe(true);
  });

  test('select focuses the node', async () => {
    const result = await testNode.select();
    expect(result).toBe(true);
  });

  test('refresh reloads node state', async () => {
    await testNode.refresh();
    // Should not throw
  });

  test('toString returns formatted string', () => {
    expect(testNode.toString()).toContain('Node(id=');
  });
});
