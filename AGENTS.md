# freeplane_plugin_grpc — OpenHands Agent Context

## 1. Repository Purpose

This repository provides a **Freeplane plugin** that exposes mind-map operations over **gRPC**.
It allows external clients (Python, Ruby, shell/grpcurl) to programmatically create, modify, and
query Freeplane mind maps via a gRPC server running inside Freeplane on port **50051**.

It is part of the [MindWM](https://github.com/metacoma/mindwm) project.

## 2. Project Structure

| Path | Purpose |
|---|---|
| `src/main/java/org/freeplane/plugin/grpc/` | Java plugin source (5 files) |
| `src/main/java/org/freeplane/plugin/grpc/Activator.java` | OSGi BundleActivator — starts gRPC server on bundle start |
| `src/main/java/org/freeplane/plugin/grpc/GrpcRegistration.java` | gRPC Netty server bootstrap; listens on configurable addr:port |
| `src/main/java/org/freeplane/plugin/grpc/FreeplaneGrpcService.java` | gRPC service implementation (all RPC handlers) |
| `src/main/java/org/freeplane/plugin/grpc/JsonHelper.java` | JSON ↔ mindmap node conversion (canonical + legacy formats) |
| `src/main/java/org/freeplane/plugin/grpc/NodeUtils.java` | Static utility methods for node operations |
| `src/main/proto/freeplane.proto` | Protocol Buffers definition (Freeplane service, 26+ RPCs) |
| `src/main/proto/generate.sh` | Docker-based stub generation script (Ruby + Python) |
| `grpc/python/` | Python client package (`freeplane_grpc/`) + pre-built gRPC stubs |
| `grpc/python/freeplane_grpc/client.py` | `FreeplaneClient` — high-level gRPC client |
| `grpc/python/freeplane_grpc/mindmap.py` | `MindMap` — map-level operations |
| `grpc/python/freeplane_grpc/node.py` | `Node` — node-level operations |
| `grpc/python/freeplane_grpc/exceptions.py` | Custom exceptions |
| `grpc/python/freeplane_grpc/_stub.py` | Stub import shim |
| `grpc/python/freeplane_pb2.py` | Generated protobuf message classes |
| `grpc/python/freeplane_pb2_grpc.py` | Generated gRPC stub classes |
| `grpc/python/generate_stubs.sh` | Python stub regeneration script (no Docker) |
| `grpc/python/pyproject.toml` | Python package metadata (requires-python >= 3.10) |
| `grpc/python/requirements.txt` | Legacy deps (grpc) |
| `grpc/python/examples/` | Python examples + smoke tests |
| `grpc/python/examples/test_json_roundtrip.py` | JSON round-trip smoke test (requires running Freeplane) |
| `grpc/python/examples/modify_mindmap_example.py` | Mind-map modification example |
| `grpc/python/examples/test_blank_map.mm` | Blank test map fixture |
| `grpc/python/ec2_list_instances.json` | Sample EC2 data for freeplane_ec2.py |
| `grpc/python/freeplane_ec2.py` | EC2 → mindmap example |
| `grpc/ruby/` | Ruby gRPC stubs + examples |
| `grpc/shell/` | Shell scripts using grpcurl |
| `misc/scripts/run-freeplane-python-smoke-test.sh` | Full runtime smoke test (Xvfb → Freeplane → Python) |
| `misc/scripts/start-xvfb-freeplane-env.sh` | Xvfb + window manager starter |
| `misc/scripts/stop-xvfb-freeplane-env.sh` | Xvfb + WM stopper |
| `misc/chatgpt/` | ChatGPT integration scripts |
| `misc/nvim/` | Neovim integration |
| `misc/*.d2`, `misc/*.png` | Architecture diagrams |
| `build.gradle` | Gradle build config (protobuf plugin, Java 8, gRPC 1.52.0) |
| `README.md` | Project documentation |

## 3. Technology Stack

- **Java 8** (sourceCompatibility = 1.8, targetCompatibility = 1.8) — `build.gradle`
- **Gradle** (no wrapper; build requires system `gradle`)
- **Freeplane plugin API** (OSGi bundle, depends on `:freeplane`, `:freeplane_plugin_script`)
- **gRPC Java 1.52.0** (`grpc-protobuf`, `grpc-stub`, `grpc-netty-shaded`, `grpc-netty`)
- **Protocol Buffers 3.21.12**
- **Protoc plugin** `com.google.protobuf:protoc-gen-grpc-java`
- **Python ≥ 3.10** (pyproject.toml `requires-python`)
- **grpcio ≥ 1.60.0**, **protobuf ≥ 4.25.0** (Python client deps)
- **pytest ≥ 8.0** (Python dev deps)
- **Ruby gRPC** (pre-built stubs in `grpc/ruby/`)
- **grpcurl** (shell examples)
- **GitHub Actions** — no CI workflows detected in this repo

## 4. Development Environment

### Java / Gradle
- **Java version**: 8 (source/target compatibility). **Confirmed** from `build.gradle` line 6-7.
- **Gradle version**: Not detected. No `gradlew` wrapper. **Unknown**.
- The plugin must be built **inside the Freeplane source tree** (`settings.gradle` must include
  `freeplane_plugin_grpc`). See README "Build plugin" section.
- `build.gradle` references tasks from the Freeplane parent project (`:freeplane:createEmojiList`).

### Python
- **Python version**: ≥ 3.10 (pyproject.toml `requires-python = ">=3.10"`).
- Install Python client: `pip install -e grpc/python/` (or `pip install grpcio protobuf`).
- Regenerate Python stubs: `bash grpc/python/generate_stubs.sh`

### Freeplane Runtime
- **gRPC server address**: `0.0.0.0:50051` (default; configurable via `GRPC_LISTEN_ADDR`, `GRPC_LISTEN_PORT` env vars).
- **Clients connect to**: `127.0.0.1:50051` (default).
- **Freeplane source checkout required**: Yes — the plugin is built as part of the Freeplane Gradle project.
  Clone Freeplane to `/workspace/freeplane` (per smoke test script).
- **Xvfb / display manager**: Required for Freeplane GUI. Use `misc/scripts/start-xvfb-freeplane-env.sh`.
- **Docker**: NOT assumed available inside OpenHands sandbox. The `generate.sh` script uses Docker
  for Ruby stub generation, but `generate_stubs.sh` (Python) does not.

## 5. Build Commands

### Plugin build (requires Freeplane source tree)
```bash
# From Freeplane root (after cloning freeplane_plugin_grpc into it)
cd /workspace/freeplane
# Ensure settings.gradle includes: include 'freeplane_plugin_grpc'
gradle :freeplane_plugin_grpc:build --no-daemon
# Or full build:
gradle build --no-daemon
# Or dist (skip tests):
gradle dist -x test -x check_translation --no-daemon
```

### Python stub regeneration
```bash
cd grpc/python
bash generate_stubs.sh          # in-place
bash generate_stubs.sh /output  # to specific directory
```

### Python client install
```bash
cd grpc/python
pip install -e .
# or for dev:
pip install -e ".[dev]"
```

### Python unit tests
```bash
cd grpc/python
pytest
```

## 6. Test and Validation Commands

### Java / Gradle
- `gradle :freeplane_plugin_grpc:build --no-daemon` — compile + test
- **No Gradle wrapper** — requires system `gradle`.
- **Unknown**: exact Gradle version required.

### Python unit tests
- `cd grpc/python && pytest` — runs `test_client.py`, `test_exceptions.py`, `test_mindmap.py`, `test_node.py`

### Python smoke test (requires running Freeplane)
- `bash misc/scripts/run-freeplane-python-smoke-test.sh`
  - Full runtime validation: Xvfb → Freeplane build → gRPC readiness → Python example → JSON round-trip
  - Requires: Freeplane source at `/workspace/freeplane`, Xvfb, Java, Gradle

### JSON round-trip test (requires running Freeplane)
- `python3 grpc/python/examples/test_json_roundtrip.py`
  - Connects to `127.0.0.1:50051`
  - Validates canonical JSON import/export consistency

### gRPC / shell smoke test
- `grpcurl -plaintext -proto src/main/proto/freeplane.proto -d '{}' 127.0.0.1:50051 freeplane.Freeplane/GetCurrentNode`
  - Requires `grpcurl` binary

### Formatting / linting
- **Not detected**. No formatter/linter config found.

### CI workflows
- **None detected**. No `.github/workflows/` or similar CI files.

## 7. Freeplane Runtime Notes

- **Launch Freeplane**: `BIN/freeplane.sh <map_file>` from Freeplane build output.
- **Plugin installation**: Copy `build/libs/org.freeplane.plugin.grpc*.jar` to
  `Freeplane/BIN/plugins/org.freeplane.plugin.grpc/` (built via Freeplane Gradle).
- **Xvfb required**: Freeplane is a Java GUI app. Use `misc/scripts/start-xvfb-freeplane-env.sh`
  (source it to get `DISPLAY` env var).
- **gRPC readiness polling**: Wait for port 50051 to be available (`nc -z 127.0.0.1 50051`).
- **MindMap mode activation**: Poll gRPC `GetCurrentNode` RPC to verify map is loaded.
- **Smoke test map**: Use `grpc/python/examples/test_blank_map.mm` as a clean starting point.
- **Environment variables**: `GRPC_LISTEN_ADDR`, `GRPC_LISTEN_PORT` override default binding.

## 8. gRPC / Protobuf Notes

### Proto file locations
- **Canonical source**: `src/main/proto/freeplane.proto` (primary, authoritative)
- **Copies**: `grpc/shell/freeplane.proto`, `misc/nvim/freeplane/freeplane.proto` (mirrored)

### Generated code
- **Java**: Generated at build time by `com.google.protobuf` Gradle plugin into `build/generated/`.
- **Python**: `grpc/python/freeplane_pb2.py`, `grpc/python/freeplane_pb2.pyi`, `grpc/python/freeplane_pb2_grpc.py`
  (pre-committed stubs).
- **Ruby**: `grpc/ruby/lib/freeplane_pb.rb`, `grpc/ruby/lib/freeplane_services_pb.rb` (pre-committed).

### Regeneration
```bash
# Python (no Docker needed):
bash grpc/python/generate_stubs.sh

# Ruby (requires Docker):
bash src/main/proto/generate.sh   # uses grpc_ruby_docker_image

# Manual protoc (any language):
protoc -Isrc/main/proto --python_out=grpc/python --pyi_out=grpc/python --grpc_python_out=grpc/python src/main/proto/freeplane.proto
```

### Compatibility
- Java gRPC server: **1.52.0** (build.gradle)
- Python gRPC client: **≥ 1.60.0** (pyproject.toml) — **potential version mismatch risk**
- Python protobuf: **≥ 4.25.0** — **may differ from Java protobuf 3.21.12**
- **Important**: After editing `.proto` files, regenerate both Java and Python stubs.

## 9. Agent Workflow

```
scout → architect → coder → reviewer → publisher
```

Each role must **verify critical assumptions independently** from repository files.
Previous role output is **useful context but not a source of truth**.
**Repository files are the source of truth.**

## 10. Role-Specific Instructions

### Scout

The scout must:
- Inspect only. Never modify files.
- Identify relevant files and components.
- Detect Java, Gradle, Python, gRPC, protobuf, and Freeplane-related structure.
- Detect exact build/test commands from repository files.
- Detect required versions from CI configs, build files, pyproject.toml, etc.
- Detect CI workflow commands (none detected in this repo).
- Identify integration-test requirements (Freeplane runtime, Xvfb, display).
- Identify risks and unknowns.
- Recommend architect focus areas.

**Output must include**: repository summary, important files/directories, detected commands,
detected versions, Freeplane runtime assumptions, gRPC/protobuf notes, risks, unknowns.

### Architect

The architect must:
- Read scout output critically. Verify key assumptions from repository files.
- Produce a concrete implementation plan with **exact files to modify**.
- Specify exact validation commands.
- Consider Java/8 compatibility (source/target = 1.8).
- Consider Freeplane plugin compatibility (OSGi, parent project dependency).
- Consider protobuf/gRPC compatibility (Java 1.52.0 vs Python ≥ 1.60.0 version gap).
- Consider Python client compatibility (≥ 3.10, grpcio ≥ 1.60.0).
- Consider integration-test constraints (Freeplane build, Xvfb, display).
- Avoid vague plans. Never modify files.

**Output must include**: implementation plan, files to change, validation plan,
compatibility risks, rollback notes, unresolved unknowns.

### Coder

The coder must:
- Implement only the requested change.
- Follow the architect plan unless repository evidence shows it is wrong.
- Prefer minimal, reviewable changes.
- Avoid unrelated refactors.
- Avoid editing generated files unless regeneration workflow is understood.
- Keep Java server, protobuf definitions, and Python client compatible.
- Install only minimal missing tools if needed.
- Run relevant validation commands.
- Commit final changes if the pipeline expects committed code.
- **Never push. Never create PRs.**

**Output must include**: files changed, implementation summary, validation commands run,
validation result, commit hash if committed, skipped validation and why, known limitations.

### Reviewer

The reviewer must:
- Remain read-only.
- Review diff between current branch and base branch (`main`).
- Verify the implementation satisfies the original task.
- Check Java/Gradle changes (source/target = 1.8).
- Check Python client changes (≥ 3.10, grpcio ≥ 1.60.0).
- Check protobuf/gRPC compatibility.
- Check Freeplane runtime assumptions.
- Run relevant validation commands.
- Install minimal validation tools if needed.
- Report **ACTION: PASS** or **ACTION: BLOCKER**.
- Include risk level: **RISK: LOW**, **RISK: MEDIUM**, or **RISK: HIGH**.
- **Never modify files. Never commit. Never push. Never create PRs.**

**Output must include**: verdict, risk level, reviewed diff summary, validation commands run,
findings, required fixes if blocked.

### Publisher

The publisher must:
- Inspect repository state.
- Provide exact user instructions for publishing.
- **Never push. Never create PRs. Never modify files.**

**Output must include**: current branch, base branch if detectable, remotes, upstream status,
working tree status, latest commit summary, recommended git push command,
recommended gh pr create command, manual notes.

## 11. Repository-Specific Rules

| Rule | Value | Source |
|---|---|---|
| Java source compatibility | 1.8 | `build.gradle` line 6 |
| Java target compatibility | 1.8 | `build.gradle` line 7 |
| gRPC Java version | 1.52.0 | `build.gradle` line 10 |
| Protobuf Java version | 3.21.12 | `build.gradle` line 11 |
| Python requires-python | ≥ 3.10 | `grpc/python/pyproject.toml` |
| Python grpcio | ≥ 1.60.0 | `grpc/python/pyproject.toml` |
| Python protobuf | ≥ 4.25.0 | `grpc/python/pyproject.toml` |
| Default gRPC port | 50051 | `GrpcRegistration.java` line 24 |
| Default gRPC bind | 0.0.0.0 | `GrpcRegistration.java` line 41 |
| Configurable via env | `GRPC_LISTEN_ADDR`, `GRPC_LISTEN_PORT` | `GrpcRegistration.java` line 35-36 |
| Freeplane source path | `/workspace/freeplane` (convention) | `run-freeplane-python-smoke-test.sh` |
| Plugin build method | Gradle inside Freeplane source tree | `README.md` section 5.2 |
| No Gradle wrapper | Confirmed — no `gradlew` | File system inspection |
| No CI workflows | Confirmed — no `.github/workflows/` | File system inspection |
| Generated proto files must not be edited manually | Standard protobuf practice | Confirmed by `generate.sh` and `generate_stubs.sh` |
| Python client stubs must be regenerated after .proto changes | Required for consistency | `generate_stubs.sh` |
| Integration tests require GUI/Xvfb | Confirmed | `start-xvfb-freeplane-env.sh`, `run-freeplane-python-smoke-test.sh` |
| Docker not assumed in OpenHands sandbox | Safety constraint | This role's rules |

## 12. Safety Constraints

All agents must:
- Never expose secrets.
- Never print tokens.
- Never run destructive commands unless explicitly requested.
- Never assume Docker is available inside OpenHands sandbox.
- Never push unless explicitly instructed by the user.
- Never create PRs unless explicitly instructed by the user.
- Prefer small diffs.
- Report blockers honestly.
- Explain skipped validation.
- Avoid broad rewrites.
- Avoid deleting files unless required by the task.

## 13. Known Unknowns

- **Gradle version**: Not detected. No `gradlew` or version file found.
- **Java runtime version**: Source/target = 1.8, but runtime Java version not specified.
- **Freeplane version compatibility**: Not specified in any file.
- **Exact Freeplane build command**: Varies — may need `gradle build`, `gradle dist`, or both.
- **Integration test command**: Only the smoke test script is available; no JUnit test suite detected.
- **Protobuf regeneration for Java**: Done automatically by Gradle plugin; no manual command detected.
- **Release process**: Not documented.
- **Base branch**: `main` (inferred from remote tracking).
- **CI/CD**: None detected.

## 14. Maintenance Notes

Update this file when:
- Build commands change.
- Gradle/Java versions change.
- Python client layout or dependencies change.
- Protobuf generation changes.
- Freeplane runtime setup changes.
- CI workflows are added/changed.
- Agent workflow changes.
