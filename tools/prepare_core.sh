#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
DEST="$ROOT/app/src/main/cpp/upstream"
REV=497aa6cdd26f45f8064b78916b00a773eab4ded7
if [ ! -d "$DEST/.git" ]; then
  git init "$DEST"
  git -C "$DEST" remote add origin https://github.com/melonDS-emu/melonDS.git
fi
git -C "$DEST" fetch --depth 1 origin "$REV"
git -C "$DEST" checkout --detach "$REV"
test "$(git -C "$DEST" rev-parse HEAD)" = "$REV"
