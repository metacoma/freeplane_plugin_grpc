# freeplane-grpc-client (Node.js)

Node.js gRPC client for the [Freeplane](https://www.freeplane.org/) mind-map application.

## Requirements

- Node.js >= 18.0.0 (LTS)
- Freeplane with the gRPC plugin enabled (default port: 50051)

## Installation

```bash
cd grpc/nodejs
npm install
```

## Usage

```javascript
const { FreeplaneClient } = require('./src/index');

async function main() {
  const client = new FreeplaneClient();
  await client.connect();

  try {
    const map = await client.currentMap();
    const root = await map.root();
    console.log('Root:', await root.getText());

    const child = await root.addChild('Hello from Node.js');
    await child.setText('Modified text');
    await child.setNote('A note');
    await child.setTags(['tag1', 'tag2']);

    const json = await client.getMapToJson();
    console.log('Map JSON length:', json.length);

    await child.delete();
  } finally {
    await client.close();
  }
}

main().catch(console.error);
```

## Configuration

| Environment Variable | Constructor Option | Default     | Description              |
|---------------------|-------------------|-------------|--------------------------|
| `FREEPLANE_HOST`    | `options.host`    | `127.0.0.1` | Freeplane gRPC server host |
| `FREEPLANE_PORT`    | `options.port`    | `50051`     | Freeplane gRPC server port |

Constructor options take precedence over environment variables.

## API

### FreeplaneClient

- `connect()` — Open gRPC channel
- `close()` — Close gRPC channel
- `currentMap()` — Get the current mind map
- `openMap(filePath)` — Open a .mm file
- `getMapToJson()` — Export current map as JSON
- `mindMapFromJson(json)` — Import map from JSON
- `groovy(code)` — Execute Groovy code on the server
- `focusNode(nodeId)` — Focus a node in the UI
- `setStatusInfo(info)` — Set status bar text

### Node

- `getText()` / `setText(text)`
- `addChild(text)` / `children()` / `parent()` / `delete()` / `move(newParentId)`
- `getNote()` / `setNote(note)`
- `getAttribute(name)` / `setAttribute(name, value)` / `setAttributes(attrs)`
- `getLinks()` / `setLinks(links)`
- `setTags(tags)` / `addTags(tags)`
- `addIcon(iconName)`
- `setColor(r, g, b, a)` / `setBackgroundColor(r, g, b, a)`
- `select()` / `center()` / `refresh()`

### MindMap

- `root()` — Get the root node
- `selectedNode()` — Get the currently selected node
- `findNodes(pattern)` — Search nodes by text
- `createNode(text, parentId)` / `createChild(parent, text)`
- `info()` / `size()`
- `save(path)` / `export(path, format)` / `importMap(path)`

### Exceptions

- `FreeplaneGrpcError` — Base exception
- `FreeplaneConnectionError` — Connection/network failures
- `FreeplaneOperationError` — Server-reported operation failures
- `NodeNotFoundError` — Requested node does not exist
- `MindMapError` — Map-level operation failures

## Testing

```bash
# Unit tests (mocked, no server required)
npm test

# Integration tests (requires FREEPLANE_HOST)
FREEPLANE_HOST=127.0.0.1 npm run test:integration

# All tests
npm run test:all
```

## Examples

See `examples/basic_usage.js` and `examples/smoke_test.js`.
