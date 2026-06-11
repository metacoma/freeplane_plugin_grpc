#!/usr/bin/env bash
# run-e2e-tests.sh
#
# Unified end-to-end test orchestration script for the Freeplane gRPC plugin.
#
# This script:
#   1. Checks/clones Freeplane source to /tmp/freeplane
#   2. Integrates the plugin into Freeplane's settings.gradle
#   3. Builds Freeplane (gradle dist -x test for speed)
#   4. Starts Xvfb + window manager
#   5. Starts Freeplane with a blank map
#   6. Waits for gRPC readiness (poll nc -z 127.0.0.1 50051)
#   7. Waits for MindMap mode activation (poll via _check_grpc_map.py)
#   8. Runs Python smoke test (modify_mindmap_example.py)
#   9. Runs Python JSON round-trip test (test_json_roundtrip.py)
#  10. Runs Ruby integration smoke test (run-freeplane-ruby-integration.rb)
#  11. Collects results, prints summary
#  12. Shuts down Freeplane, Xvfb
#  13. Cleans up Freeplane clone
#
# Exit code: 0 if all tests pass, non-zero otherwise.
#
# Usage:
#   bash misc/scripts/run-e2e-tests.sh
#
# Prerequisites (for local runs):
#   - Java 17+
#   - Gradle 8.x
#   - Python 3.10+ with grpcio + protobuf
#   - Ruby 3.2+ with grpc gem
#   - Xvfb, openbox/fluxbox/twm
#   - netcat (nc)
#
# For Docker-based runs, use:
#   make e2e-test

set -euo pipefail

# ---- Configuration --------------------------------------------------------
PLUGIN_REPO="$(cd "$(dirname "$0")/.." && pwd)"
FREEPLANE_SRC="/tmp/freeplane"
FREEPLANE_REPO="https://github.com/freeplane/freeplane"
RUNTIME_DIR="/tmp/freeplane-xvfb"
GRPC_HOST="127.0.0.1"
GRPC_PORT="50051"

# Test scripts
PYTHON_EXAMPLE="${PLUGIN_REPO}/grpc/python/examples/modify_mindmap_example.py"
PYTHON_ROUNDTRIP_TEST="${PLUGIN_REPO}/grpc/python/examples/test_json_roundtrip.py"
RUBY_INTEGRATION="${PLUGIN_REPO}/misc/scripts/run-freeplane-ruby-integration.rb"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# ---- Logging helpers ------------------------------------------------------
log_info()  { echo -e "${GREEN}[INFO]${NC} $*"; }
log_warn()  { echo -e "${YELLOW}[WARN]${NC} $*"; }
log_error() { echo -e "${RED}[ERROR]${NC} $*"; }
log_step()  { echo -e "${BLUE}[STEP]${NC} $*"; }

# ---- State tracking -------------------------------------------------------
PYTHON_EXIT=0
ROUNDTRIP_EXIT=0
RUBY_EXIT=0
ALL_OK=true

# ---- Step 0: Pre-flight check for existing Freeplane process --------------
echo ""
log_step "Step 0: Pre-flight check"

check_freeplane_processes() {
    local pids
    pids=$(pgrep -f 'freeplane.sh' 2>/dev/null || true)
    if [[ -n "$pids" ]]; then
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

# ---- Step 0b: Clone / integrate plugin into Freeplane build ---------------
echo ""
log_step "Step 0b: Clone/integrate plugin into Freeplane build"

PLUGIN_INTEGRATED=false
if [[ ! -d "$FREEPLANE_SRC" ]]; then
    log_info "Cloning Freeplane source from $FREEPLANE_REPO ..."
    git clone --depth 1 "$FREEPLANE_REPO" "$FREEPLANE_SRC"
    log_info "Freeplane source cloned to $FREEPLANE_SRC"
else
    log_info "Freeplane source already exists at $FREEPLANE_SRC"
fi

if [[ ! -d "$FREEPLANE_SRC/freeplane_plugin_grpc" ]]; then
    log_info "Copying plugin to Freeplane source tree..."
    mkdir -p "$FREEPLANE_SRC/freeplane_plugin_grpc"
    (cd "$PLUGIN_REPO" && tar cf - --exclude='.git' --exclude='__pycache__' --exclude='*.egg-info' --exclude='*.egg' --exclude='.gradle' --exclude='build' --exclude='.pytest_cache' --exclude='.venv' --exclude='*.pyc' --exclude='node_modules' --exclude='Gemfile.lock' --exclude='.bundle' --exclude='vendor' .) \
        | (cd "$FREEPLANE_SRC/freeplane_plugin_grpc" && tar xf -)
    PLUGIN_INTEGRATED=true
else
    log_info "Plugin directory already exists in Freeplane source tree"
fi

SETTINGS_FILE="$FREEPLANE_SRC/settings.gradle"
if [[ -f "$SETTINGS_FILE" ]]; then
    if ! grep -q "'freeplane_plugin_grpc'" "$SETTINGS_FILE" 2>/dev/null && \
       ! grep -q '"freeplane_plugin_grpc"' "$SETTINGS_FILE" 2>/dev/null; then
        log_info "Adding freeplane_plugin_grpc to settings.gradle..."
        echo "include 'freeplane_plugin_grpc'" >> "$SETTINGS_FILE"
        log_info "Plugin added to settings.gradle"
    else
        log_info "freeplane_plugin_grpc already in settings.gradle"
    fi
else
    log_error "settings.gradle not found at $SETTINGS_FILE"
    exit 1
fi

# ---- Step 1: Build Freeplane ----------------------------------------------
echo ""
log_step "Step 1: Build Freeplane"

cd "$FREEPLANE_SRC"
BUILD_OK=false
if gradle build --no-daemon 2>&1 | tail -5; then
    BUILD_OK=true
    log_info "Freeplane build completed successfully"
else
    log_warn "gradle build exited non-zero — checking if required artifacts exist..."
    if [[ -f "$FREEPLANE_SRC/BIN/freeplane.sh" ]] && \
       [[ -d "$FREEPLANE_SRC/BIN/plugins/org.freeplane.plugin.grpc" ]]; then
        log_info "Required artifacts found — continuing despite build warnings."
        BUILD_OK=true
    else
        log_warn "Required artifacts not found — running dist task (skipping tests)..."
        if gradle dist -x test -x check_translation --no-daemon 2>&1 | tail -5; then
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

# ---- Step 2: Start Xvfb + WM ----------------------------------------------
echo ""
log_step "Step 2: Start Xvfb + window manager"

if [[ ! -f "${PLUGIN_REPO}/misc/scripts/start-xvfb-freeplane-env.sh" ]]; then
    log_error "Xvfb start script not found"
    exit 1
fi

source "${PLUGIN_REPO}/misc/scripts/start-xvfb-freeplane-env.sh"
export DISPLAY=":$DISPLAY_NUM"
export _JAVA_AWT_WM_NONREPARENTING=1
log_info "Xvfb + WM started on display :$DISPLAY_NUM"

# ---- Step 3: Start Freeplane ----------------------------------------------
echo ""
log_step "Step 3: Start Freeplane"

FREEPLANE_LAUNCHER="${FREEPLANE_SRC}/BIN/freeplane.sh"
if [[ ! -f "$FREEPLANE_LAUNCHER" ]]; then
    log_error "Freeplane launcher not found: $FREEPLANE_LAUNCHER"
    exit 1
fi

mkdir -p "$RUNTIME_DIR"
FREEPLANE_LOG="$RUNTIME_DIR/freeplane.log"

# Use blank test map for clean test isolation
MINDMAP_FILE="$RUNTIME_DIR/smoke_test_map.mm"
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
fi

log_info "Starting Freeplane from $FREEPLANE_LAUNCHER ..."
$FREEPLANE_LAUNCHER "$MINDMAP_FILE" > "$FREEPLANE_LOG" 2>&1 &
FREEPLANE_PID=$!
echo "$FREEPLANE_PID" > "$RUNTIME_DIR/freeplane.pid"
log_info "Freeplane started (PID $FREEPLANE_PID)"

# ---- Step 4: Wait for gRPC readiness --------------------------------------
echo ""
log_step "Step 4: Wait for gRPC server readiness"

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

if ! kill -0 "$FREEPLANE_PID" 2>/dev/null; then
    log_error "Freeplane process exited unexpectedly"
    log_error "Freeplane log (last 50 lines):"
    tail -50 "$FREEPLANE_LOG" 2>/dev/null || true
    exit 1
fi
log_info "Freeplane is still running (PID $FREEPLANE_PID)"

# Wait for MindMap mode to fully activate
log_info "Waiting for MindMap mode to activate (polling gRPC server)..."
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
    log_error "Freeplane log (last 30 lines):"
    tail -30 "$FREEPLANE_LOG" 2>/dev/null || true
    exit 1
fi

# ---- Step 5: Run Python smoke test ----------------------------------------
echo ""
log_step "Step 5: Run Python smoke test"

if [[ ! -f "$PYTHON_EXAMPLE" ]]; then
    log_error "Python example not found: $PYTHON_EXAMPLE"
    PYTHON_EXIT=1
else
    log_info "Running Python smoke test: $PYTHON_EXAMPLE"
    FREEPLANE_HOST="$GRPC_HOST" FREEPLANE_PORT="$GRPC_PORT" python3 "$PYTHON_EXAMPLE" || PYTHON_EXIT=$?
    if [[ $PYTHON_EXIT -eq 0 ]]; then
        log_info "Python smoke test PASSED"
    else
        log_error "Python smoke test FAILED (exit code $PYTHON_EXIT)"
    fi
fi

# ---- Step 5b: Run Python JSON round-trip test -----------------------------
echo ""
log_step "Step 5b: Run Python JSON round-trip test"

if [[ -f "$PYTHON_ROUNDTRIP_TEST" ]]; then
    log_info "Running Python JSON round-trip test..."
    FREEPLANE_HOST="$GRPC_HOST" FREEPLANE_PORT="$GRPC_PORT" python3 "$PYTHON_ROUNDTRIP_TEST" || ROUNDTRIP_EXIT=$?
    if [[ $ROUNDTRIP_EXIT -eq 0 ]]; then
        log_info "Python JSON round-trip test PASSED"
    else
        log_error "Python JSON round-trip test FAILED (exit code $ROUNDTRIP_EXIT)"
    fi
else
    log_warn "Python JSON round-trip test not found: $PYTHON_ROUNDTRIP_TEST — skipping"
fi

# ---- Step 6: Run Ruby integration smoke test ------------------------------
echo ""
log_step "Step 6: Run Ruby integration smoke test"

if [[ ! -f "$RUBY_INTEGRATION" ]]; then
    log_error "Ruby integration test not found: $RUBY_INTEGRATION"
    RUBY_EXIT=1
else
    # Install Ruby dependencies if needed
    if ! ruby --version 2>/dev/null; then
        log_warn "Ruby not available — skipping Ruby integration test"
        RUBY_EXIT=1
    else
        cd "${PLUGIN_REPO}/grpc/ruby"
        if bundle install --quiet 2>/dev/null; then
            log_info "Ruby dependencies installed"
        else
            log_warn "bundle install failed — attempting gem install..."
            gem install --quiet grpc google-protobuf 2>/dev/null || true
        fi

        log_info "Running Ruby integration smoke test..."
        FREEPLANE_HOST="$GRPC_HOST" FREEPLANE_PORT="$GRPC_PORT" bundle exec ruby "$RUBY_INTEGRATION" || RUBY_EXIT=$?
        if [[ $RUBY_EXIT -eq 0 ]]; then
            log_info "Ruby integration smoke test PASSED"
        else
            log_error "Ruby integration smoke test FAILED (exit code $RUBY_EXIT)"
        fi
    fi
fi

# ---- Step 7: Shutdown -----------------------------------------------------
echo ""
log_step "Step 7: Shutdown"

# Stop Freeplane
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
else
    log_info "Freeplane was not running"
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
    if [[ -f "$SETTINGS_FILE" ]]; then
        sed -i "/^include 'freeplane_plugin_grpc'$/d" "$SETTINGS_FILE"
        log_info "Reverted settings.gradle"
    fi
fi

# ---- Final result ---------------------------------------------------------
echo ""
echo "=========================================="
if [[ $PYTHON_EXIT -eq 0 && $ROUNDTRIP_EXIT -eq 0 && $RUBY_EXIT -eq 0 ]]; then
    log_info "ALL TESTS PASSED"
    echo "=========================================="
    exit 0
else
    log_error "TEST RESULTS:"
    [[ $PYTHON_EXIT -ne 0 ]] && log_error "  Python smoke test: FAILED (exit $PYTHON_EXIT)"
    [[ $ROUNDTRIP_EXIT -ne 0 ]] && log_error "  Python JSON round-trip: FAILED (exit $ROUNDTRIP_EXIT)"
    [[ $RUBY_EXIT -ne 0 ]] && log_error "  Ruby integration test: FAILED (exit $RUBY_EXIT)"
    echo "=========================================="
    exit 1
fi
