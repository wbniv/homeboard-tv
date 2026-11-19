#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "${BASH_SOURCE[0]}")/.."
cleanup_state="${HOMEBOARD_CLEANUP_DATA:-$HOME/.local/state/homeboard-cleanup-standalone}"
umask 077
mkdir -p "$cleanup_state"
chmod 700 "$cleanup_state"
exec docker run --rm -it --network host \
  --user "$(id -u):$(id -g)" \
  -v "$PWD:/workspace:ro" -v "$cleanup_state:/data" \
  -w /workspace "${BUILD_IMAGE:-homeboard-tv-build:local}" \
  python3 dev/cleanup_companion.py --transport standalone --data /data \
  --adb /opt/android-sdk/platform-tools/adb --no-open "$@"
