#!/usr/bin/env node
/**
 * Smoke test for the freeplane-grpc-client.
 *
 * Performs a minimal end-to-end test:
 *   1. Connect to Freeplane
 *   2. Get current map
 *   3. Create a node
 *   4. Read back the node text
 *   5. Export JSON and verify
 *   6. Delete the test node
 *
 * Exit code 0 = pass, 1 = fail.
 *
 * Usage:
 *   node examples/smoke_test.js
 */

const { FreeplaneClient, FreeplaneConnectionError } = require('../src/index');

async function main() {
  const client = new FreeplaneClient();
  let childNode = null;

  try {
    console.log('[1/6] Connecting to Freeplane...');
    await client.connect();
    console.log('  OK: Connected');

    console.log('[2/6] Getting current map...');
    const map = await client.currentMap();
    console.log(`  OK: Map ${map.mapId}`);

    console.log('[3/6] Creating test node...');
    const root = await map.root();
    const smokeName = `SmokeTest_${Date.now()}`;
    childNode = await root.addChild(smokeName);
    console.log(`  OK: Created node ${childNode.nodeId}`);

    console.log('[4/6] Reading back node text...');
    const text = await childNode.getText();
    if (text !== smokeName) {
      throw new Error(`Text mismatch: expected "${smokeName}", got "${text}"`);
    }
    console.log(`  OK: Text matches "${text}"`);

    console.log('[5/6] Exporting JSON...');
    const json = await client.getMapToJson();
    const parsed = JSON.parse(json);
    if (!parsed) {
      throw new Error('JSON export returned empty/invalid data');
    }
    console.log(`  OK: JSON exported (${json.length} chars)`);

    console.log('[6/6] Cleaning up...');
    if (childNode) {
      await childNode.delete();
    }
    console.log('  OK: Test node deleted');

    console.log('\nSMOKE TEST: PASSED');
    process.exitCode = 0;
  } catch (err) {
    console.error(`\nSMOKE TEST: FAILED`);
    console.error(`  Error: ${err.message}`);

    // Try to cleanup on failure
    if (childNode) {
      try {
        await childNode.delete();
      } catch (_) {
        // ignore
      }
    }
    process.exitCode = 1;
  } finally {
    try {
      await client.close();
    } catch (_) {
      // ignore
    }
  }
}

main();
