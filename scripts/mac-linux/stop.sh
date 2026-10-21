#!/usr/bin/env bash
# Northstar CRM - stop everything (macOS / Linux)
#   ./scripts/mac-linux/stop.sh          stop (database data is kept)
#   ./scripts/mac-linux/stop.sh --reset  stop AND delete database data
set -euo pipefail
cd "$(dirname "$0")/../.."
if [ "${1:-}" = "--reset" ]; then docker compose down -v; else docker compose down; fi
echo "Stopped."
