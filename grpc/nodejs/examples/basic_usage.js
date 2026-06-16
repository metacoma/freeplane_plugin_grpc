#!/usr/bin/env node
/**
 * Basic usage example for the freeplane-grpc-client.
 *
 * Demonstrates connecting to Freeplane, navigating the mind map,
 * creating nodes, and modifying content.
 *
 * Requires a running Freeplane instance with the gRPC plugin enabled.
 *
 * Usage:
 *   node examples/basic_usage.js
 *   FREEPLANE_HOST=10.0.0.1 FREEPLANE_PORT=50051 node examples/basic_usage.js
 */

const { FreeplaneClient } = require('../src/index');

async function main() {
  const client = new FreeplaneClient();

  try {
    console.log(`Connecting to Freeplane at ${client.host}:${client.port}...`);
    await client.connect();
    console.log('Connected!');

    // Get the current mind map
    const map = await client.currentMap();
    console.log(`Current map: ${map.toString()}`);

    // Get the root node
    const root = await map.root();
    console.log(`Root node: ${root.nodeId} - "${await root.getText()}"`);

    // List root children
    const children = await root.children();
    console.log(`Root has ${children.length} children`);

    // Create a new child node
    const timestamp = new Date().toISOString();
    const child = await root.addChild(`Node.js Test ${timestamp}`);
    console.log(`Created child: ${child.nodeId}`);

    // Modify the child node
    await child.setText('Modified by Node.js client');
    await child.setNote('This note was added by the Node.js gRPC client');
    await child.setTags(['test', 'nodejs']);
    await child.setColor(255, 0, 0, 255);

    // Verify changes
    const text = await child.getText();
    console.log(`Child text: "${text}"`);

    // Export map to JSON
    const json = await client.getMapToJson();
    console.log(`Map JSON length: ${json.length} characters`);

    // Set status info
    await client.setStatusInfo('Node.js client connected and working!');

    // Cleanup: delete the test node
    await child.delete();
    console.log('Test node deleted');

    console.log('\nBasic usage example completed successfully!');
  } catch (err) {
    console.error('Error:', err.message);
    process.exitCode = 1;
  } finally {
    await client.close();
    console.log('Connection closed');
  }
}

main();
