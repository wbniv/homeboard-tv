#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "${BASH_SOURCE[0]}")/.."
# Rootless container UID 0 maps to the invoking host user, not host root.
if docker info --format '{{json .SecurityOptions}}' | grep -q '"name=rootless"'; then
  export BUILD_UID=${BUILD_UID:-0} BUILD_GID=${BUILD_GID:-0}
else
  export BUILD_UID=${BUILD_UID:-$(id -u)} BUILD_GID=${BUILD_GID:-$(id -g)}
fi
case "${1:-}" in
  image) exec docker compose build build ;;
  # Every worktree uses the same named Gradle cache. Separate container PID
  # namespaces cannot negotiate Gradle's lock holder, so serialize cache users.
  run) shift; exec flock /tmp/homeboard-tv-gradle-cache.lock docker compose run --rm --no-deps build "$@" ;;
  release)
    shift
    export HOMEBOARD_SIGNING_DIR=${HOMEBOARD_SIGNING_DIR:-$HOME/.local/share/homeboard-tv/signing}
    test -f "$HOMEBOARD_SIGNING_DIR/release.json" || { echo 'Run task signing:init first.' >&2; exit 2; }
    exec flock /tmp/homeboard-tv-gradle-cache.lock docker compose -f docker-compose.yaml -f docker-compose.signing.yaml run --rm --no-deps build "$@" ;;
  *) echo 'Usage: dev/container.sh image | run COMMAND [ARG...]' >&2; exit 2 ;;
esac
