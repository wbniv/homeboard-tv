#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
signing_dir=${HOMEBOARD_SIGNING_DIR:-$HOME/.local/share/homeboard-tv/signing}
backup_dir=${HOMEBOARD_SIGNING_BACKUP_DIR:-$HOME/.local/share/homeboard-tv/signing-backup}
umask 077
test ! -e "$backup_dir"
mkdir -p "$signing_dir"
docker run --rm -v "$PWD:/workspace:ro" -v "$signing_dir:/signing" "${BUILD_IMAGE:-homeboard-tv-build:local}" python3 /workspace/dev/init-signing.py
mkdir -p "$backup_dir"
cp -p "$signing_dir/release.jks" "$signing_dir/release.json" "$backup_dir/"
echo 'Local backup created. Keep an additional protected off-machine backup before relying on public updates.'
