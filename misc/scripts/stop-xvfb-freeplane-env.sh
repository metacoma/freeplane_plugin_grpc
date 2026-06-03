#!/usr/bin/env bash
# stop-xvfb-freeplane-env.sh
#
# Stops the Xvfb and window manager processes started by
# start-xvfb-freeplane-env.sh and cleans up runtime files.
# Safe to run if processes are already stopped.
#
# Usage:
#   /workspace/freeplane_plugin_grpc/misc/scripts/stop-xvfb-freeplane-env.sh

set -euo pipefail

RUNTIME_DIR="/tmp/freeplane-xvfb"

# Ensure runtime directory exists
if [[ ! -d "$RUNTIME_DIR" ]]; then
    echo "Runtime directory $RUNTIME_DIR does not exist. Nothing to stop."
    exit 0
fi

# --- Helper: stop a process by PID file ---
stop_by_pid() {
    local pid_file="$1"
    local service_name="$2"

    if [[ ! -f "$pid_file" ]]; then
        echo "$service_name PID file not found — skipping."
        return 0
    fi

    local pid
    pid="$(cat "$pid_file")"

    if [[ -z "$pid" ]]; then
        echo "$service_name PID file is empty — skipping."
        return 0
    fi

    if kill -0 "$pid" 2>/dev/null; then
        echo "Stopping $service_name (PID $pid) ..."
        kill "$pid" 2>/dev/null || true
        local waited=0
        while [[ $waited -lt 5 ]]; do
            if ! kill -0 "$pid" 2>/dev/null; then
                break
            fi
            sleep 1
            waited=$((waited + 1))
        done
        if kill -0 "$pid" 2>/dev/null; then
            echo "$service_name did not exit gracefully — sending SIGKILL."
            kill -9 "$pid" 2>/dev/null || true
        fi
        echo "$service_name (PID $pid) stopped."
    else
        echo "$service_name (PID $pid) is not running — skipping."
    fi
}

# --- Stop Freeplane first (if tracked) ---
if [[ -f "$RUNTIME_DIR/freeplane.pid" ]]; then
    FREEPLANE_PID="$(cat "$RUNTIME_DIR/freeplane.pid")"
    if kill -0 "$FREEPLANE_PID" 2>/dev/null; then
        echo "Stopping Freeplane (PID $FREEPLANE_PID) ..."
        kill "$FREEPLANE_PID" 2>/dev/null || true
        waited=0
        while [[ $waited -lt 10 ]]; do
            if ! kill -0 "$FREEPLANE_PID" 2>/dev/null; then
                break
            fi
            sleep 1
            waited=$((waited + 1))
        done
        if kill -0 "$FREEPLANE_PID" 2>/dev/null; then
            echo "Freeplane did not exit gracefully — sending SIGKILL."
            kill -9 "$FREEPLANE_PID" 2>/dev/null || true
        fi
        echo "Freeplane (PID $FREEPLANE_PID) stopped."
    else
        echo "Freeplane (PID $FREEPLANE_PID) is not running — skipping."
    fi
    rm -f "$RUNTIME_DIR/freeplane.pid"
fi

# --- Stop window manager first ---
stop_by_pid "$RUNTIME_DIR/wm.pid" "Window manager"

# --- Stop Xvfb second ---
stop_by_pid "$RUNTIME_DIR/xvfb.pid" "Xvfb"

# --- Read display number for cleanup of X lock files ---
DISPLAY_NUM=""
if [[ -f "$RUNTIME_DIR/display" ]]; then
    DISPLAY_NUM="$(cat "$RUNTIME_DIR/display")"
fi

# --- Remove temporary X lock files (only for our display) ---
if [[ -n "$DISPLAY_NUM" ]]; then
    X_LOCK="/tmp/.X${DISPLAY_NUM}-lock"
    if [[ -f "$X_LOCK" ]]; then
        rm -f "$X_LOCK"
        echo "Removed X lock file: $X_LOCK"
    fi

    X11_DIR="/tmp/.X11-unix"
    X11_SOCKET="${X11_DIR}/X${DISPLAY_NUM}"
    if [[ -e "$X11_SOCKET" ]]; then
        rm -f "$X11_SOCKET"
        echo "Removed X11 socket: $X11_SOCKET"
    fi
fi

# --- Clean up runtime files ---
rm -f "$RUNTIME_DIR/xvfb.pid" "$RUNTIME_DIR/wm.pid" "$RUNTIME_DIR/display" "$RUNTIME_DIR/env"
echo "Runtime directory cleaned up: $RUNTIME_DIR"
echo "Xvfb environment stopped successfully."
