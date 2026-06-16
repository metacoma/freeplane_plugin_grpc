#!/usr/bin/env bash
# run-freeplane-nodejs-smoke-test.sh
#
# Master orchestration script for Node.js Freeplane runtime validation:
#   1. Checks for and safely stops previous Freeplane instances
#   2. Downloads Freeplane binary and installs plugin
#   3. Starts Xvfb + lightweight WM
#   4. Starts Freeplane
#   5. Waits for gRPC server readiness
#   6. Runs the Node.js smoke test
#   7. Shuts down everything
#   8. Returns non-zero on failure
#
# Usage:
#   bash misc/scripts/run-freeplane-nodejs-smoke-test.sh

set -euo pipefail

PLUGIN_REPO="${PLUGIN_REPO:-$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)}"
FREEPLANE_VERSION="${FREEPLANE_VERSION:-1.12.18}"
PLUGIN_VERSION="${PLUGIN_VERSION:-0.0.9}"
FREEPLANE_DIR="${FREEPLANE_DIR:-/tmp/freeplane-${FREEPLANE_VERSION}}"
RUNTIME_DIR="/tmp/freeplane-xvfb"
GRPC_HOST="127.0.0.1"
GRPC_PORT="50051"
NODEJS_SMOKE="${PLUGIN_REPO}/grpc/nodejs/examples/smoke_test.js"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

log_info()  { echo -e "${GREEN}[INFO]${NC} $*"; }
log_warn()  { echo -e "${YELLOW}[WARN]${NC} $*"; }
log_error() { echo -e "${RED}[ERROR]${NC} $*"; }

# --- Step 0: Pre-flight check for existing Freeplane process ---
echo ""
echo "=========================================="
echo " Step 0: Pre-flight check"
echo "=========================================="
check_freeplane_processes() {
    local pids
    pids=$(pgrep -f 'freeplane.sh' 2>/dev/null || true)
    if [[ -n "$pids" ]]; then
        echo "Found existing Freeplane processes:"
        echo "$pids" | while read -r pid; do
            echo "  PID $pid: $(ps -p "$pid" -o cmd= 2>/dev/null || echo 'unknown')"
        done
        return 0
    fi
    return 1
}

if check_freeplane_processes; then
    log_warn "Previous Freeplane instance detected. Stopping it..."
    while IFS= read -r pid; do
        if [[ -n "$pid" ]] && kill -0 "$pid" 2>/dev/null; then
            echo "  Stopping PID $pid..."
            kill "$pid" 2>/dev/null || true
        fi
    done < <(pgrep -f 'freeplane.sh' 2>/dev/null || true)

    waited=0
    while [[ $waited -lt 10 ]]; do
        if ! check_freeplane_processes; then
            break
        fi
        sleep 1
        waited=$((waited + 1))
    done

    if check_freeplane_processes; then
        log_warn "Freeplane did not exit gracefully — sending SIGKILL"
        while IFS= read -r pid; do
            if [[ -n "$pid" ]] && kill -0 "$pid" 2>/dev/null; then
                kill -9 "$pid" 2>/dev/null || true
            fi
        done < <(pgrep -f 'freeplane.sh' 2>/dev/null || true)
    fi

    sleep 2

    if check_freeplane_processes; then
        log_error "Failed to stop previous Freeplane instance"
        exit 1
    fi
    log_info "Previous Freeplane instance stopped successfully"
else
    log_info "No previous Freeplane instance detected"
fi

# --- Step 1: Download and setup Freeplane binary ---
echo ""
echo "=========================================="
echo " Step 1: Download and setup Freeplane binary"
echo "=========================================="

if [[ ! -d "$FREEPLANE_DIR" ]]; then
    log_info "Downloading Freeplane ${FREEPLANE_VERSION} binary..."
    curl -sL "https://github.com/freeplane/freeplane/releases/download/release-${FREEPLANE_VERSION}/freeplane_bin-${FREEPLANE_VERSION}.zip" -o /tmp/freeplane.zip
    mkdir -p "$(dirname "$FREEPLANE_DIR")"
    unzip -q /tmp/freeplane.zip -d "$(dirname "$FREEPLANE_DIR")"
    rm /tmp/freeplane.zip
    log_info "Freeplane ${FREEPLANE_VERSION} extracted to $FREEPLANE_DIR"
else
    log_info "Freeplane ${FREEPLANE_VERSION} already available at $FREEPLANE_DIR"
fi

# Install gRPC plugin
PLUGIN_TAR="/tmp/org.freplane.plugin.grpc.tar.gz"
if [[ ! -f "$PLUGIN_TAR" ]]; then
    log_info "Downloading gRPC plugin ${PLUGIN_VERSION}..."
    curl -sL "https://github.com/metacoma/freeplane_plugin_grpc/releases/download/${PLUGIN_VERSION}/org.freplane.plugin.grpc.tar.gz" -o "$PLUGIN_TAR"
fi

log_info "Installing plugin into Freeplane..."
tar xzf "$PLUGIN_TAR" -C "$FREEPLANE_DIR/plugins/"

# Verify
if [[ ! -f "$FREEPLANE_DIR/freeplane.sh" ]]; then
    log_error "Freeplane launcher not found: $FREEPLANE_DIR/freeplane.sh"
    exit 1
fi
if [[ ! -d "$FREEPLANE_DIR/plugins/org.freeplane.plugin.grpc/lib" ]]; then
    log_error "Plugin not installed correctly"
    exit 1
fi
log_info "Freeplane binary and plugin ready"

# --- Step 2: Start Xvfb + WM ---
echo ""
echo "=========================================="
echo " Step 2: Start Xvfb + window manager"
echo "=========================================="
if [[ ! -f "${PLUGIN_REPO}/misc/scripts/start-xvfb-freeplane-env.sh" ]]; then
    log_error "Xvfb start script not found"
    exit 1
fi

source "${PLUGIN_REPO}/misc/scripts/start-xvfb-freeplane-env.sh"
export DISPLAY=":$DISPLAY_NUM"
export _JAVA_AWT_WM_NONREPARENTING=1
log_info "Xvfb + WM started on display :$DISPLAY_NUM"

# --- Step 3: Start Freeplane ---
echo ""
echo "=========================================="
echo " Step 3: Start Freeplane"
echo "=========================================="
FREEPLANE_LAUNCHER="${FREEPLANE_DIR}/freeplane.sh"
if [[ ! -f "$FREEPLANE_LAUNCHER" ]]; then
    log_error "Freeplane launcher not found: $FREEPLANE_LAUNCHER"
    exit 1
fi

mkdir -p "$RUNTIME_DIR"
FREEPLANE_LOG="$RUNTIME_DIR/freeplane.log"

MINDMAP_FILE="$RUNTIME_DIR/smoke_test_map.mm"
if [[ -f "${PLUGIN_REPO}/grpc/python/examples/test_blank_map.mm" ]]; then
    cp "${PLUGIN_REPO}/grpc/python/examples/test_blank_map.mm" "$MINDMAP_FILE"
elif [[ ! -f "$MINDMAP_FILE" ]]; then
    cat > "$MINDMAP_FILE" <<'MMEOF'
<map version="freeplane 1.9.0">
<node TEXT="Smoke Test Map" FOLDED="false" ID="ID_00000001" CREATED="0000000000000" MODIFIED="0000000000000">
</node>
</map>
MMEOF
fi

log_info "Starting Freeplane from $FREEPLANE_LAUNCHER ..."
$FREEPLANE_LAUNCHER "$MINDMAP_FILE" > "$FREEPLANE_LOG" 2>&1 &
FREEPLANE_PID=$!
echo "$FREEPLANE_PID" > "$RUNTIME_DIR/freeplane.pid"
log_info "Freeplane started (PID $FREEPLANE_PID)"

# --- Step 4: Wait for gRPC readiness ---
echo ""
echo "=========================================="
echo " Step 4: Wait for gRPC server readiness"
echo "=========================================="
GRPC_READY=false
for i in $(seq 1 60); do
    if nc -z "$GRPC_HOST" "$GRPC_PORT" 2>/dev/null; then
        GRPC_READY=true
        log_info "gRPC server ready after $((i * 2)) seconds"
        break
    fi
    sleep 2
done

if ! $GRPC_READY; then
    log_error "gRPC server did not become ready within 120 seconds"
    tail -50 "$FREEPLANE_LOG" 2>/dev/null || true
    exit 1
fi

# Wait for MindMap mode
log_info "Waiting for MindMap mode to activate..."
MAP_READY=false
PYTHON_CHECK_MAP="$PLUGIN_REPO/misc/scripts/_check_grpc_map.py"
for i in $(seq 1 60); do
    RESULT=$(GRPC_HOST="$GRPC_HOST" GRPC_PORT="$GRPC_PORT" GRPC_TIMEOUT="3" python3 "$PYTHON_CHECK_MAP" 2>/dev/null || true)
    if [[ "$RESULT" == "OK" ]]; then
        MAP_READY=true
        log_info "MindMap mode activated after $((i * 2)) seconds"
        break
    fi
    sleep 2
done

if ! $MAP_READY; then
    log_error "gRPC server ready but no map available after 120 seconds"
    tail -30 "$FREEPLANE_LOG" 2>/dev/null || true
    exit 1
fi

# Open blank map via gRPC
echo ""
echo "=========================================="
echo " Step 4b: Open blank map via gRPC"
echo "=========================================="
OPEN_MAP_RESULT=$(python3 -c "
import sys, os
sys.path.insert(0, os.path.join('${PLUGIN_REPO}', 'grpc/python'))
from freeplane_pb2 import OpenMapRequest
import freeplane_pb2_grpc, grpc
stub = freeplane_pb2_grpc.FreeplaneStub(grpc.insecure_channel('${GRPC_HOST}:${GRPC_PORT}'))
resp = stub.OpenMap(OpenMapRequest(file_path='${MINDMAP_FILE}'))
print('success' if resp.success else 'failed')
" 2>&1)
if [[ "$OPEN_MAP_RESULT" == *"success"* ]]; then
    log_info "Blank map opened successfully via gRPC"
else
    log_warn "OpenMap call returned: $OPEN_MAP_RESULT"
fi

# --- Step 5: Install Node.js dependencies ---
echo ""
echo "=========================================="
echo " Step 5: Install Node.js dependencies"
echo "=========================================="
cd "$PLUGIN_REPO/grpc/nodejs"
if [[ ! -d "node_modules" ]]; then
    log_info "Installing Node.js dependencies..."
    npm install --no-audit --no-fund
fi
log_info "Node.js dependencies ready"

# --- Step 6: Run Node.js smoke test ---
echo ""
echo "=========================================="
echo " Step 6: Run Node.js smoke test"
echo "=========================================="
if [[ ! -f "$NODEJS_SMOKE" ]]; then
    log_error "Node.js smoke test not found: $NODEJS_SMOKE"
    exit 1
fi

NODEJS_EXIT_CODE=0
FREEPLANE_HOST="$GRPC_HOST" FREEPLANE_PORT="$GRPC_PORT" node "$NODEJS_SMOKE" || NODEJS_EXIT_CODE=$?

# --- Step 7: Shutdown ---
echo ""
echo "=========================================="
echo " Step 7: Shutdown"
echo "=========================================="

if kill -0 "$FREEPLANE_PID" 2>/dev/null; then
    log_info "Stopping Freeplane (PID $FREEPLANE_PID) ..."
    kill "$FREEPLANE_PID" 2>/dev/null || true
    waited=0
    while [[ $waited -lt 15 ]]; do
        if ! kill -0 "$FREEPLANE_PID" 2>/dev/null; then
            break
        fi
        sleep 1
        waited=$((waited + 1))
    done
    if kill -0 "$FREEPLANE_PID" 2>/dev/null; then
        log_warn "Freeplane did not exit gracefully — sending SIGKILL"
        kill -9 "$FREEPLANE_PID" 2>/dev/null || true
    fi
    log_info "Freeplane stopped"
fi

if [[ -f "${PLUGIN_REPO}/misc/scripts/stop-xvfb-freeplane-env.sh" ]]; then
    log_info "Stopping Xvfb + WM ..."
    bash "${PLUGIN_REPO}/misc/scripts/stop-xvfb-freeplane-env.sh"
fi

# --- Final result ---
echo ""
echo "=========================================="
if [[ $NODEJS_EXIT_CODE -eq 0 ]]; then
    log_info "SMOKE TEST: PASSED"
else
    log_error "SMOKE TEST: FAILED (exit code $NODEJS_EXIT_CODE)"
fi
echo "=========================================="

exit $NODEJS_EXIT_CODE
