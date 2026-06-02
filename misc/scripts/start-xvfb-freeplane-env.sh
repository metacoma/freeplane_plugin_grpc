#!/usr/bin/env bash
set -euo pipefail

# start-xvfb-freeplane-env.sh
# Installs dependencies, starts Xvfb and a lightweight window manager,
# and writes environment variables and PID files to a runtime directory.
# Safe to run more than once — will skip if already running.

RUNTIME_DIR="/tmp/freeplane-xvfb"
DISPLAY_NUM=99

# --- Ensure runtime directory exists ---
mkdir -p "$RUNTIME_DIR"

# --- Check if environment is already running ---
if [[ -f "$RUNTIME_DIR/xvfb.pid" ]] && [[ -f "$RUNTIME_DIR/wm.pid" ]]; then
    OLD_XVFB_PID="$(cat "$RUNTIME_DIR/xvfb.pid")"
    OLD_WM_PID="$(cat "$RUNTIME_DIR/wm.pid")"
    IS_XVFB_RUNNING=false
    IS_WM_RUNNING=false
    kill -0 "$OLD_XVFB_PID" 2>/dev/null && IS_XVFB_RUNNING=true
    kill -0 "$OLD_WM_PID" 2>/dev/null && IS_WM_RUNNING=true
    if $IS_XVFB_RUNNING && $IS_WM_RUNNING; then
        echo "Xvfb (PID $OLD_XVFB_PID) and window manager (PID $OLD_WM_PID) are already running."
        echo "To restart, run: misc/scripts/stop-xvfb-freeplane-env.sh"
        exit 0
    fi
    # Clean up stale PIDs if processes are dead
    echo "Detected stale PID files — cleaning up."
    rm -f "$RUNTIME_DIR/xvfb.pid" "$RUNTIME_DIR/wm.pid" "$RUNTIME_DIR/display" "$RUNTIME_DIR/env"
fi

# --- Install required OS packages ---
install_packages() {
    local pkgs=("$@")
    local need_install=()
    for pkg in "${pkgs[@]}"; do
        if ! dpkg -l | grep -q "^ii  $pkg " 2>/dev/null; then
            need_install+=("$pkg")
        fi
    done
    if [[ ${#need_install[@]} -gt 0 ]]; then
        echo "Installing packages: ${need_install[*]}"
        sudo apt-get update -qq
        sudo apt-get install -y "${need_install[@]}"
    else
        echo "All required packages are already installed."
    fi
}

install_packages xvfb xauth openbox procps

# --- Find a free display number starting from 99 ---
find_free_display() {
    local start="$1"
    local num
    for num in $(seq "$start" 120); do
        # Check if X lock file exists for this display
        if [[ ! -f "/tmp/.X${num}-lock" ]]; then
            echo "$num"
            return 0
        fi
        # Also check if any Xvfb is already on this display
        if ! pgrep -f "Xvfb.*:.*${num}" > /dev/null 2>&1; then
            echo "$num"
            return 0
        fi
    done
    echo "ERROR: No free display found between :99 and :120" >&2
    return 1
}

DISPLAY_NUM="$(find_free_display "$DISPLAY_NUM")"
echo "Using display :$DISPLAY_NUM"

# --- Start Xvfb ---
XVFB_LOG="$RUNTIME_DIR/xvfb.log"
echo "Starting Xvfb on :$DISPLAY_NUM ..."
Xvfb ":$DISPLAY_NUM" -screen 0 1024x768x24 -ac +extension GLX +render -noreset &> "$XVFB_LOG" &
XVFB_PID=$!
echo "$XVFB_PID" > "$RUNTIME_DIR/xvfb.pid"
echo "Xvfb started (PID $XVFB_PID)."

# Give Xvfb a moment to initialize
sleep 1

# --- Start window manager ---
WM_LOG="$RUNTIME_DIR/wm.log"
# Export DISPLAY so child processes (including the WM) can see it
export DISPLAY=":$DISPLAY_NUM"
start_window_manager() {
    local wm="$1"
    echo "Starting $wm ..."
    $wm &> "$WM_LOG" &
    local wm_pid=$!
    echo "$wm_pid" > "$RUNTIME_DIR/wm.pid"
    echo "Window manager ($wm) started (PID $wm_pid)."
}

# Try openbox first, then fluxbox, then twm
if command -v openbox &>/dev/null; then
    start_window_manager openbox
elif command -v fluxbox &>/dev/null; then
    start_window_manager fluxbox
elif command -v twm &>/dev/null; then
    start_window_manager twm
else
    echo "ERROR: No lightweight window manager (openbox, fluxbox, or twm) found." >&2
    exit 1
fi

# --- Write environment file ---
ENV_FILE="$RUNTIME_DIR/env"
cat > "$ENV_FILE" <<EOF
export DISPLAY=:$DISPLAY_NUM
export _JAVA_AWT_WM_NONREPARENTING=1
EOF

# --- Write display number ---
echo "$DISPLAY_NUM" > "$RUNTIME_DIR/display"

# --- Print instructions ---
echo ""
echo "=========================================="
echo " Xvfb environment is ready!"
echo "=========================================="
echo "To use, run:"
echo "  . $ENV_FILE"
echo ""
echo "Display :$DISPLAY_NUM (PID $XVFB_PID)"
echo "Window manager PID $(cat "$RUNTIME_DIR/wm.pid")"
echo "=========================================="
