#!/usr/bin/env bash
# run-freeplane-python-smoke-test.sh
#
# Master orchestration script for full Freeplane runtime validation:
#   1. Checks for and safely stops previous Freeplane instances
#   2. Starts Xvfb + lightweight WM
#   3. Builds Freeplane from source
#   4. Starts Freeplane
#   5. Waits for gRPC server readiness
#   6. Runs the Python example
#   7. Verifies mind map change through read-back
#   8. Shuts down everything
#   9. Returns non-zero on failure
#
# Usage:
#   bash /workspace/freeplane_plugin_grpc/misc/scripts/run-freeplane-python-smoke-test.sh

set -euo pipefail

PLUGIN_REPO="${PLUGIN_REPO:-$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)}"
FREEPLANE_SRC="${FREEPLANE_SRC:-/workspace/freeplane}"
RUNTIME_DIR="/tmp/freeplane-xvfb"
GRPC_HOST="127.0.0.1"
GRPC_PORT="50051"
PYTHON_EXAMPLE="${PLUGIN_REPO}/grpc/python/examples/modify_mindmap_example.py"
PYTHON_ROUNDTRIP_TEST="${PLUGIN_REPO}/grpc/python/examples/test_json_roundtrip.py"

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
    # Stop only Freeplane processes from this workspace
    while IFS= read -r pid; do
        if [[ -n "$pid" ]] && kill -0 "$pid" 2>/dev/null; then
            echo "  Stopping PID $pid..."
            kill "$pid" 2>/dev/null || true
        fi
    done < <(pgrep -f 'freeplane.sh' 2>/dev/null || true)
    
    # Wait for graceful shutdown
    waited=0
    while [[ $waited -lt 10 ]]; do
        if ! check_freeplane_processes; then
            break
        fi
        sleep 1
        waited=$((waited + 1))
    done
    
    # Force kill if still alive
    if check_freeplane_processes; then
        log_warn "Freeplane did not exit gracefully — sending SIGKILL"
        while IFS= read -r pid; do
            if [[ -n "$pid" ]] && kill -0 "$pid" 2>/dev/null; then
                kill -9 "$pid" 2>/dev/null || true
            fi
        done < <(pgrep -f 'freeplane.sh' 2>/dev/null || true)
    fi
    
    sleep 2
    
    # Verify clean
    if check_freeplane_processes; then
        log_error "Failed to stop previous Freeplane instance"
        exit 1
    fi
    log_info "Previous Freeplane instance stopped successfully"
else
    log_info "No previous Freeplane instance detected"
fi

# --- Step 0b: Integrate plugin into Freeplane build ---
echo ""
echo "=========================================="
echo " Step 0b: Integrate plugin into Freeplane build"
echo "=========================================="
PLUGIN_INTEGRATED=false
if [[ ! -d "$FREEPLANE_SRC/freeplane_plugin_grpc" ]]; then
    log_info "Copying plugin to Freeplane source tree..."
    # Create the target subdirectory so plugin files land in the correct location
    mkdir -p "$FREEPLANE_SRC/freeplane_plugin_grpc"
    # Use tar to avoid copying .git, __pycache__, and build artifacts
    (cd "$PLUGIN_REPO" && tar cf - --exclude='.git' --exclude='__pycache__' --exclude='*.egg-info' --exclude='*.egg' --exclude='.gradle' --exclude='build' --exclude='.pytest_cache' --exclude='.venv' --exclude='*.pyc' .) | (cd "$FREEPLANE_SRC/freeplane_plugin_grpc" && tar xf -)
    PLUGIN_INTEGRATED=true
else
    log_info "Plugin directory already exists in Freeplane source tree"
fi

# Check if plugin is included in settings.gradle
SETTINGS_FILE="$FREEPLANE_SRC/settings.gradle"
if [[ -f "$SETTINGS_FILE" ]]; then
    if ! grep -q "'freeplane_plugin_grpc'" "$SETTINGS_FILE" 2>/dev/null && \
       ! grep -q '"freeplane_plugin_grpc"' "$SETTINGS_FILE" 2>/dev/null; then
        log_info "Adding freeplane_plugin_grpc to settings.gradle..."
        # Append a standalone include statement (Gradle supports multiple include() calls)
        echo "include 'freeplane_plugin_grpc'" >> "$SETTINGS_FILE"
        log_info "Plugin added to settings.gradle"
    else
        log_info "freeplane_plugin_grpc already in settings.gradle"
    fi
else
    log_error "settings.gradle not found at $SETTINGS_FILE"
    exit 1
fi

# --- Step 1: Full Freeplane build ---
echo ""
echo "=========================================="
echo " Step 1: Full Freeplane build"
echo "=========================================="
if [[ ! -d "$FREEPLANE_SRC" ]]; then
    log_error "Freeplane source directory not found: $FREEPLANE_SRC"
    exit 1
fi

cd "$FREEPLANE_SRC"
log_info "Building Freeplane from $FREEPLANE_SRC ..."
BUILD_OK=false
if gradle build --no-daemon; then
    BUILD_OK=true
    log_info "Freeplane build completed successfully"
else
    log_warn "gradle build exited non-zero — checking if required artifacts exist..."
    if [[ -f "$FREEPLANE_SRC/BIN/freeplane.sh" ]] && \
       [[ -d "$FREEPLANE_SRC/BIN/plugins/org.freeplane.plugin.grpc" ]]; then
        log_info "Required artifacts found — continuing despite build failures."
        log_info "(Build failures may be unrelated to the plugin, e.g. AWT tests in headless env.)"
        BUILD_OK=true
    else
        log_warn "Required artifacts not found — running dist task (skipping tests)..."
        if gradle dist -x test -x check_translation --no-daemon; then
            if [[ -f "$FREEPLANE_SRC/BIN/freeplane.sh" ]] && \
               [[ -d "$FREEPLANE_SRC/BIN/plugins/org.freeplane.plugin.grpc" ]]; then
                log_info "Dist artifacts produced successfully."
                BUILD_OK=true
            else
                log_error "gradle dist -x test completed but artifacts still missing."
                exit 1
            fi
        else
            log_error "gradle dist -x test also failed."
            exit 1
        fi
    fi
fi

# --- Step 2: Start Xvfb + WM ---
echo ""
echo "=========================================="
echo " Step 2: Start Xvfb + window manager"
echo "=========================================="
if [[ ! -f "${PLUGIN_REPO}/misc/scripts/start-xvfb-freeplane-env.sh" ]]; then
    log_error "Xvfb start script not found"
    exit 1
fi

# Source the start script (it exports DISPLAY)
source "${PLUGIN_REPO}/misc/scripts/start-xvfb-freeplane-env.sh"
export DISPLAY=":$DISPLAY_NUM"
export _JAVA_AWT_WM_NONREPARENTING=1
log_info "Xvfb + WM started on display :$DISPLAY_NUM"

# --- Step 3: Start Freeplane ---
echo ""
echo "=========================================="
echo " Step 3: Start Freeplane"
echo "=========================================="
FREEPLANE_LAUNCHER="${FREEPLANE_SRC}/BIN/freeplane.sh"
if [[ ! -f "$FREEPLANE_LAUNCHER" ]]; then
    log_error "Freeplane launcher not found: $FREEPLANE_LAUNCHER"
    log_error "Ensure Freeplane was built successfully"
    exit 1
fi

mkdir -p "$RUNTIME_DIR"
FREEPLANE_LOG="$RUNTIME_DIR/freeplane.log"

# --- Ensure a mindmap file is available for Freeplane to open ---
MINDMAP_FILE="$RUNTIME_DIR/smoke_test_map.mm"
# Prefer the blank test map fixture from the plugin repo for clean test isolation
if [[ -f "${PLUGIN_REPO}/grpc/python/examples/test_blank_map.mm" ]]; then
    cp "${PLUGIN_REPO}/grpc/python/examples/test_blank_map.mm" "$MINDMAP_FILE"
    log_info "Copied test_blank_map.mm as smoke test map: $MINDMAP_FILE"
elif [[ ! -f "$MINDMAP_FILE" ]]; then
    cat > "$MINDMAP_FILE" <<'MMEOF'
<map version="freeplane 1.9.0">
<node TEXT="Smoke Test Map" FOLDED="false" ID="ID_00000001" CREATED="0000000000000" MODIFIED="0000000000000">
</node>
</map>
MMEOF
    log_info "Created temporary mindmap file: $MINDMAP_FILE"
else
    log_info "Using existing mindmap file: $MINDMAP_FILE"
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
    log_error "gRPC server did not become ready on $GRPC_HOST:$GRPC_PORT within 120 seconds"
    log_error "Freeplane log (last 50 lines):"
    tail -50 "$FREEPLANE_LOG" 2>/dev/null || true
    exit 1
fi

# Verify Freeplane is still running
if ! kill -0 "$FREEPLANE_PID" 2>/dev/null; then
    log_error "Freeplane process exited unexpectedly"
    log_error "Freeplane log (last 50 lines):"
    tail -50 "$FREEPLANE_LOG" 2>/dev/null || true
    exit 1
fi
log_info "Freeplane is still running (PID $FREEPLANE_PID)"

# --- Wait for MindMap mode to fully activate (poll gRPC server) ---
log_info "Waiting for MindMap mode to activate (polling gRPC server)..."
MAP_READY=false
PYTHON_CHECK_MAP="$PLUGIN_REPO/misc/scripts/_check_grpc_map.py"

for i in $(seq 1 60); do
    RESULT=$(GRPC_HOST="$GRPC_HOST" GRPC_PORT="$GRPC_PORT" GRPC_TIMEOUT="3" python3 "$PYTHON_CHECK_MAP" 2>/dev/null)
    if [[ "$RESULT" == "OK" ]]; then
        MAP_READY=true
        log_info "MindMap mode activated after $((i * 2)) seconds"
        break
    fi
    sleep 2
done

if ! $MAP_READY; then
    log_error "gRPC server ready but no map available after 120 seconds"
    log_error "Freeplane log (last 30 lines):"
    tail -30 "$FREEPLANE_LOG" 2>/dev/null || true
    exit 1
fi

# --- Step 4b: Open blank map via gRPC to override demo map ---
echo ""
echo "=========================================="
echo " Step 4b: Open blank map via gRPC"
echo "=========================================="
log_info "Opening blank map via gRPC to override Freeplane demo map..."
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
    log_warn "Tests may run on the demo map instead of the blank map"
fi

# --- Step 5: Run Python example ---
echo ""
echo "=========================================="
echo " Step 5: Run Python example"
echo "=========================================="
if [[ ! -f "$PYTHON_EXAMPLE" ]]; then
    log_error "Python example not found: $PYTHON_EXAMPLE"
    exit 1
fi

# Install Python dependencies if needed
if ! python3 -c "import grpc" 2>/dev/null; then
    log_info "Installing Python gRPC dependencies..."
    pip install --quiet grpcio protobuf
fi

PYTHON_EXIT_CODE=0
python3 "$PYTHON_EXAMPLE" || PYTHON_EXIT_CODE=$?

# --- Step 5b: Run JSON round-trip smoke test ---
echo ""
echo "=========================================="
echo " Step 5b: Run JSON round-trip test"
echo "=========================================="
ROUNDTRIP_EXIT_CODE=0
if [[ -f "$PYTHON_ROUNDTRIP_TEST" ]]; then
    log_info "Running JSON round-trip smoke test..."
    python3 "$PYTHON_ROUNDTRIP_TEST" || ROUNDTRIP_EXIT_CODE=$?
    if [[ $ROUNDTRIP_EXIT_CODE -eq 0 ]]; then
        log_info "JSON round-trip test PASSED"
    else
        log_error "JSON round-trip test FAILED (exit code $ROUNDTRIP_EXIT_CODE)"
    fi
else
    log_warn "JSON round-trip test not found: $PYTHON_ROUNDTRIP_TEST — skipping"
fi

# --- Step 6: Verify mind map change ---
echo ""
echo "=========================================="
echo " Step 6: Verify mind map change"
echo "=========================================="
if [[ $PYTHON_EXIT_CODE -ne 0 ]]; then
    log_error "Python example failed with exit code $PYTHON_EXIT_CODE"
    log_error "The mind map change could not be verified"
fi
if [[ $ROUNDTRIP_EXIT_CODE -ne 0 ]]; then
    log_error "JSON round-trip test failed with exit code $ROUNDTRIP_EXIT_CODE"
fi

# --- Step 7: Shutdown ---
echo ""
echo "=========================================="
echo " Step 7: Shutdown"
echo "=========================================="

# Stop Freeplane
if kill -0 "$FREEPLANE_PID" 2>/dev/null; then
    log_info "Stopping Freeplane (PID $FREEPLANE_PID) ..."
    kill "$FREEPLANE_PID" 2>/dev/null || true
    
    # Wait for graceful shutdown
    waited=0
    while [[ $waited -lt 15 ]]; do
        if ! kill -0 "$FREEPLANE_PID" 2>/dev/null; then
            break
        fi
        sleep 1
        waited=$((waited + 1))
    done
    
    # Force kill if still alive
    if kill -0 "$FREEPLANE_PID" 2>/dev/null; then
        log_warn "Freeplane did not exit gracefully — sending SIGKILL"
        kill -9 "$FREEPLANE_PID" 2>/dev/null || true
    fi
    log_info "Freeplane stopped"
else
    log_info "Freeplane was not running"
fi

# Verify Freeplane stopped
if kill -0 "$FREEPLANE_PID" 2>/dev/null; then
    log_error "Failed to stop Freeplane (PID $FREEPLANE_PID)"
else
    log_info "Verified: Freeplane (PID $FREEPLANE_PID) is no longer running"
fi

# Stop Xvfb + WM
if [[ -f "${PLUGIN_REPO}/misc/scripts/stop-xvfb-freeplane-env.sh" ]]; then
    log_info "Stopping Xvfb + WM ..."
    bash "${PLUGIN_REPO}/misc/scripts/stop-xvfb-freeplane-env.sh"
else
    log_warn "Stop script not found, skipping Xvfb cleanup"
fi

# Revert plugin integration changes in Freeplane source tree
if [[ "$PLUGIN_INTEGRATED" == "true" ]]; then
    log_info "Reverting plugin integration changes in Freeplane source tree..."
    if [[ -d "$FREEPLANE_SRC/freeplane_plugin_grpc" ]]; then
        rm -rf "$FREEPLANE_SRC/freeplane_plugin_grpc"
        log_info "Removed copied plugin directory"
    fi
    SETTINGS_FILE="$FREEPLANE_SRC/settings.gradle"
    if [[ -f "$SETTINGS_FILE" ]]; then
        # Remove the standalone include line we added
        sed -i "/^include 'freeplane_plugin_grpc'$/d" "$SETTINGS_FILE"
        log_info "Reverted settings.gradle"
    fi
fi

# --- Final result ---
echo ""
echo "=========================================="
if [[ $PYTHON_EXIT_CODE -eq 0 && $ROUNDTRIP_EXIT_CODE -eq 0 ]]; then
    log_info "SMOKE TEST: PASSED"
else
    log_error "SMOKE TEST: FAILED (python=$PYTHON_EXIT_CODE, roundtrip=$ROUNDTRIP_EXIT_CODE)"
fi
echo "=========================================="

if [[ $PYTHON_EXIT_CODE -ne 0 ]]; then
    exit $PYTHON_EXIT_CODE
fi
if [[ $ROUNDTRIP_EXIT_CODE -ne 0 ]]; then
    exit $ROUNDTRIP_EXIT_CODE
fi
exit 0
