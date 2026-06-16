/**
 * Shared setup/teardown for integration tests.
 * Requires a running Freeplane gRPC server.
 *
 * Environment variables:
 *   FREEPLANE_HOST  — server host (default: 127.0.0.1)
 *   FREEPLANE_PORT  — server port  (default: 50051)
 */

const FreeplaneClient = require('../../src/client');

let sharedClient = null;

/**
 * Get a shared client instance for integration tests.
 * @returns {FreeplaneClient}
 */
function getRealClient() {
  if (!sharedClient) {
    sharedClient = new FreeplaneClient({
      host: process.env.FREEPLANE_HOST || '127.0.0.1',
      port: parseInt(process.env.FREEPLANE_PORT, 10) || 50051,
    });
  }
  return sharedClient;
}

/**
 * Generate a unique name for test nodes.
 * @param {string} [prefix='IT']
 * @returns {string}
 */
function uniqueName(prefix) {
  const p = prefix || 'IT';
  return `${p}_${Date.now()}_${Math.floor(Math.random() * 10000)}`;
}

/**
 * Check if integration tests should run.
 * @returns {boolean}
 */
function shouldRunIntegration() {
  return !!process.env.FREEPLANE_HOST;
}

module.exports = {
  getRealClient,
  uniqueName,
  shouldRunIntegration,
};
