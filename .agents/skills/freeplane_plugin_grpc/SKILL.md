---
name: freeplane_plugin_grpc
trigger: ["freeplane_grpc", "grpc", "freeplane gRPC", "gRPC plugin"]
---

# Freeplane gRPC Plugin Skill

Automate installing the Freeplane gRPC plugin's client libraries (Python/Ruby), starting a Freeplane instance with the gRPC plugin loaded, verifying the connection, using the client libraries in Python/Ruby/shell, and regenerating gRPC stubs from the proto file.

## Prerequisites

This skill requires a Linux container environment with `apt-get` access. Neither Java nor Gradle are pre-installed.

## Step 1: Environment Setup

### 1.1 Install Xvfb and openbox (headless display)

```bash
sudo apt-get update
sudo apt-get install -y xvfb openbox
```

### 1.2 Start Xvfb display server

```bash
Xvfb :99 -screen 0 1024x768x24 &
sleep 2
export DISPLAY=:99
```

Verify:
```bash
ps aux | grep Xvfb | grep -v grep
echo $DISPLAY
# Expected: :99
```

### 1.3 Install Java 21 (OpenJDK)

The plugin targets Java 8 bytecode (`sourceCompatibility = 1.8 / targetCompatibility = 1.8`) but requires Java 21 JDK to build (same as the freeplane monorepo CI).

```bash
sudo apt-get install -y openjdk-21-jdk-headless
```

Verify:
```bash
java -version
# Expected: openjdk version "21.x.x"
```

### 1.4 Install Gradle 8.x

The freeplane monorepo uses system Gradle (no wrapper). The Debian package provides only Gradle 4.4.1 which is too old. Download Gradle 8.14 directly:

```bash
cd /tmp
wget -q https://services.gradle.org/distributions/gradle-8.14-bin.zip -O gradle-8.14-bin.zip
sudo unzip -q gradle-8.14-bin.zip -d /opt/
sudo ln -sf /opt/gradle-8.14/bin/gradle /usr/local/bin/gradle
```

Verify:
```bash
gradle --version
# Expected: Gradle 8.14
```

## Step 2: Repository Verification

### 2.1 Clone or verify the freeplane monorepo

```bash
FREEPLANE_DIR=/workspace/git/freeplane

if [ ! -d "$FREEPLANE_DIR/.git" ]; then
    git clone https://github.com/freeplane/freeplane.git "$FREEPLANE_DIR"
fi

# Verify remote
git -C "$FREEPLANE_DIR" remote -v | grep origin
# Expected: https://github.com/freeplane/freeplane.git
```

### 2.2 Clone or verify the plugin repository

```bash
PLUGIN_DIR=/workspace/git/freeplane_plugin_grpc

if [ ! -d "$PLUGIN_DIR/.git" ]; then
    git clone https://github.com/metacoma/freeplane_plugin_grpc.git "$PLUGIN_DIR"
fi

# Verify remote
git -C "$PLUGIN_DIR" remote -v | grep origin
# Expected: https://github.com/metacoma/freeplane_plugin_grpc.git
```

## Step 3: Build & Install Plugin

### 3.1 Add plugin to freeplane monorepo settings.gradle

The plugin is built as part of the freeplane monorepo. Add `freeplane_grpc` to the include list in `settings.gradle`:

```bash
# Check if freeplane_grpc is already in the include list
grep -q "freeplane_grpc" "$FREEPLANE_DIR/settings.gradle" || \
    sed -i "/include 'freeplane_plugin_ai',/a\\        'freeplane_grpc'," "$FREEPLANE_DIR/settings.gradle"
```

Verify:
```bash
grep 'freeplane_grpc' "$FREEPLANE_DIR/settings.gradle"
# Expected: 'freeplane_grpc',
```

### 3.2 Build the plugin

```bash
cd "$PLUGIN_DIR"
gradle build --no-daemon
# Expected: BUILD SUCCESSFUL
```

### 3.3 Verify the built JAR

```bash
ls -la "$PLUGIN_DIR/build/libs/freeplane_plugin_grpc-*.jar"
# Expected: freeplane_plugin_grpc-1.13.3.jar (or similar version)
```

### 3.4 Copy JAR to Freeplane plugin directory

```bash
mkdir -p "$FREEPLANE_DIR/BIN/plugins"
cp "$PLUGIN_DIR/build/libs/freeplane_plugin_grpc-*.jar" "$FREEPLANE_DIR/BIN/plugins/"
```

## Step 4: Install Client Libraries

### 4.1 Install Python client library

The Python client library is at `grpc/python/`. It requires Python 3.10+ and depends on `grpcio>=1.60.0` and `protobuf>=4.25.0`.

```bash
cd "$PLUGIN_DIR/grpc/python"
pip install -e .
```

Verify:
```bash
python3 -c "import freeplane_grpc; print('freeplane_grpc imported successfully')"
# Expected: freeplane_grpc imported successfully
```

For stub generation, also install the dev dependency:
```bash
pip install grpcio-tools
```

### 4.2 Install Ruby client library

The Ruby client library is at `grpc/ruby/`. It requires Ruby >= 3.2 and depends on `grpc` and `google-protobuf < 4.0` (the pre-committed stubs use the old DescriptorPool API).

```bash
cd "$PLUGIN_DIR/grpc/ruby"
gem install grpc
bundle install
```

Verify:
```bash
ruby -e 'require "freeplane_grpc_client"; puts "freeplane_grpc_client loaded"'
# Expected: freeplane_grpc_client loaded
```

## Step 5: Start Freeplane & Verify Connection

### 5.1 Launch Freeplane with the gRPC plugin

```bash
# Ensure Xvfb is running
export DISPLAY=:99

# Start Freeplane in the background
xvfb-run "$FREEPLANE_DIR/BIN/freeplane.sh" &
sleep 10
```

### 5.2 Verify the gRPC server is listening

Wait for the log line `Freeplane grpc plugin loaded and listen on 0.0.0.0:50051` in the Freeplane console output.

Alternative: check the port directly:
```bash
ss -tlnp | grep 50051
# Expected: LISTEN on port 50051
```

### 5.3 Verify connection with grpcurl

Install grpcurl if not available:
```bash
# Option 1: Docker
alias grpcurl="docker run --rm -v /workspace/git/freeplane_plugin_grpc:/host -w /host --network=host fullstorydev/grpcurl"

# Option 2: Download binary from https://github.com/fullstorydev/grpcurl/releases
```

List available RPC services:
```bash
grpcurl -plaintext 127.0.0.1:50051 list
# Expected: output includes "freeplane.Freeplane"
```

List methods on the Freeplane service:
```bash
grpcurl -plaintext 127.0.0.1:50051 list freeplane.Freeplane
# Expected: output includes all 27 RPC methods (CreateChild, GetNodeText, SetNodeText, etc.)
```

### 5.4 Verify connection with a Python client

```bash
cd "$PLUGIN_DIR/grpc/python"
python3 -c "
from freeplane_grpc import FreeplaneClient
with FreeplaneClient('127.0.0.1', 50051) as client:
    print('Connected to Freeplane gRPC server')
"
# Expected: Connected to Freeplane gRPC server
```

## Step 6: Usage Examples

### 6.1 Python Example

From `grpc/python/examples/basic_usage.py`:

```python
import os, sys
sys.path.insert(0, os.path.join(os.path.dirname(__file__), ".."))
from freeplane_grpc import FreeplaneClient, FreeplaneOperationError

host = os.environ.get("FREEPLANE_HOST", "127.0.0.1")
port = int(os.environ.get("FREEPLANE_PORT", "50051"))

try:
    with FreeplaneClient(host=host, port=port) as client:
        # Get the currently open mind map
        mindmap = client.current_map()
        info = mindmap.info()
        print(f"Map ID: {info['map_id']}, Current node: {info['node_id']}")

        # Get the root node
        root = mindmap.root()
        print(f"Root text: {root.get_text()}")

        # Create a child node under root
        child = root.add_child("Hello from Python!")
        print(f"Created child: {child.node_id}")

        # Modify the child node
        child.set_text("Updated text via Python")
        print(f"Updated text: {child.get_text()}")

        # Add attributes, notes, tags
        grandchild = child.add_child("Grandchild node")
        grandchild.set_attribute("type", "example")
        grandchild.set_notes("This is a note for the grandchild node.")
        grandchild.add_tags(["python", "example"])

        # List children of root
        for c in root.children():
            print(f"  [{c.node_id}] {c.get_text()}")

        # Set colors
        child.set_color(255, 0, 0)           # foreground red
        child.set_background_color(255, 255, 200)  # background light yellow

        # Export map as JSON
        json_data = client.get_map_to_json()
        print(f"JSON length: {len(json_data)} characters")

except FreeplaneOperationError as exc:
    print(f"Operation failed: {exc}", file=sys.stderr)
    sys.exit(1)
except ConnectionRefusedError:
    print(f"Cannot connect to Freeplane at {host}:{port}. "
          f"Is Freeplane running with the gRPC plugin?", file=sys.stderr)
    sys.exit(1)
```

### 6.2 Ruby Example

From `grpc/ruby/examples/basic_usage.rb`:

```ruby
require "freeplane_grpc_client"

host = ENV.fetch("FREEPLANE_HOST", "127.0.0.1")
port = ENV.fetch("FREEPLANE_PORT", "50051").to_i

client = FreeplaneGrpcClient::Client.new(host, port)

begin
  client.connect
  puts "Connected to #{host}:#{port}"

  # Get the current map
  map = client.current_map
  puts "Current map ID: #{map.map_id}"

  # Get the root node
  root = map.root
  puts "Root node: #{root.node_id} (text: #{root.get_text.inspect})"

  # Create a child node
  child = map.create_child(root.node_id, "New Child Node")
  puts "Created child: #{child.node_id}"

  # Set text on the child
  child.set_text("Modified Child")
  puts "Child text now: #{child.get_text}"

  # List children of root
  children = map.root.children
  puts "Root has #{children.length} child(ren)"

  # Set tags, note, link, colors
  child.set_tags(["important", "review"])
  child.add_tags(["new-tag"])
  child.set_note("This is a note")
  child.set_link("http://example.com")
  child.set_color(255, 0, 0)
  child.set_background_color(0, 255, 0)

  # Focus the child node
  child.focus

  # Export map to JSON
  json = map.to_json
  puts "Map JSON length: #{json.length} characters"

  # Find nodes by text
  found = map.find_nodes("Modified")
  puts "Found #{found.length} node(s) matching 'Modified'"

  # Set status bar message
  client.status_info_set(status_info: "Ruby client example complete")

  puts "\nExample completed successfully!"
rescue FreeplaneGrpcClient::FreeplaneConnectionError => e
  $stderr.puts "Connection error: #{e.message}"
  exit 1
rescue FreeplaneGrpcClient::FreeplaneOperationError => e
  $stderr.puts "Operation error: #{e.message}"
  exit 1
ensure
  client.close
  puts "Connection closed"
end
```

### 6.3 Shell Example (grpcurl)

From `grpc/shell/getNodeText.sh` and `grpc/shell/setNodeText.sh`:

Get text of a node by ID:
```bash
cd "$PLUGIN_DIR/grpc/shell"

NODE_ID="your_node_id_here"
cat <<EOF | grpcurl -plaintext -proto ./freeplane.proto -d @ 127.0.0.1:50051 freeplane.Freeplane/GetNodeText
{
  "node_id": "${NODE_ID}"
}
EOF
```

Set text of a node:
```bash
NODE_ID="your_node_id_here"
cat <<EOF | grpcurl -plaintext -proto ./freeplane.proto -d @ 127.0.0.1:50051 freeplane.Freeplane/SetNodeText
{
  "node_id": "${NODE_ID}",
  "text": "New text via grpcurl"
}
EOF
```

Create a child node:
```bash
cat <<EOF | grpcurl -plaintext -proto ./freeplane.proto -d @ 127.0.0.1:50051 freeplane.Freeplane/CreateChild
{
  "name": "New Node",
  "parent_node_id": ""
}
EOF
```

List all RPC methods:
```bash
grpcurl -plaintext -proto "$PLUGIN_DIR/src/main/proto/freeplane.proto" list freeplane.Freeplane
```

## Step 7: Generate gRPC Stubs

### 7.1 Generate Python stubs

From `grpc/python/generate_stubs.sh`:

```bash
cd "$PLUGIN_DIR/grpc/python"

# Requires: grpcio-tools and protobuf installed
# pip install grpcio-tools protobuf

./generate_stubs.sh
# Generates freeplane_pb2.py, freeplane_pb2.pyi, freeplane_pb2_grpc.py in-place

# Or generate to a custom directory:
./generate_stubs.sh /path/to/output
```

Verify generated files:
```bash
ls -1 freeplane_pb2*.py
# Expected: freeplane_pb2.py, freeplane_pb2.pyi, freeplane_pb2_grpc.py
```

### 7.2 Generate Ruby stubs

From `grpc/ruby/generate_stubs.sh`:

```bash
cd "$PLUGIN_DIR/grpc/ruby"

# Requires: protoc installed and on PATH
# Requires: grpc and protobuf Ruby gems

./generate_stubs.sh
# Generates freeplane_services_pb.rb and freeplane_pb.rb under lib/

# Or generate to a custom directory:
./generate_stubs.sh /path/to/output
```

Verify generated files:
```bash
ls -1 lib/freeplane*pb.rb
# Expected: freeplane_services_pb.rb, freeplane_pb.rb
```

### 7.3 Manual protoc command

For Python:
```bash
protoc \
    -I"$PLUGIN_DIR/src/main/proto" \
    --python_out=grpc/python \
    --pyi_out=grpc/python \
    --grpc_python_out=grpc/python \
    "$PLUGIN_DIR/src/main/proto/freeplane.proto"
```

For Ruby:
```bash
protoc \
    -I"$PLUGIN_DIR/src/main/proto" \
    --ruby_out=grpc/ruby/lib \
    --grpc_ruby_out=grpc/ruby/lib \
    "$PLUGIN_DIR/src/main/proto/freeplane.proto"
```

## Notes

- The gRPC server listens on port 50051 by default, configurable via `GRPC_LISTEN_PORT` and `GRPC_LISTEN_ADDR` environment variables
- The proto file is at `src/main/proto/freeplane.proto` relative to the plugin repo root
- The plugin provides 27 RPC methods for mind map operations (create/delete nodes, set text/notes/tags/links/colors, export/import JSON, etc.)
- Pre-committed stubs are included in `grpc/python/` and `grpc/ruby/lib/` — regeneration is only needed after proto file changes
- The plugin is built as part of the freeplane monorepo; it must be added to `settings.gradle` include list
- Freeplane monorepo uses Gradle 8.14 + Java 21 JDK; the plugin compiles to Java 8 bytecode
- This plugin has been tested on Linux; other OSes may work but are not guaranteed
