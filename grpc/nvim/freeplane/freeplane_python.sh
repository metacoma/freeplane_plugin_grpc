#!/usr/bin/env bash
set -euo pipefail

# 1. Get script working directory (resolved absolute path)
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# 2. Activate local .venv
if [[ -f "$SCRIPT_DIR/.venv/bin/activate" ]]; then
    # shellcheck disable=SC1091
    source "$SCRIPT_DIR/.venv/bin/activate"
else
    echo "ERROR: .venv not found in $SCRIPT_DIR"
    exit 1
fi

# 3. Run everything passed as arguments
# "$@" preserves exact quoting
cd ${SCRIPT_DIR}
"$@"
