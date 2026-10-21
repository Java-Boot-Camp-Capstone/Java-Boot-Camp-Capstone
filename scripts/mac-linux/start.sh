#!/usr/bin/env bash
# Northstar CRM - start everything (macOS / Linux): containers up, wait until healthy, quick check, open browser.
#   ./scripts/mac-linux/start.sh [--no-browser]
set -euo pipefail
cd "$(dirname "$0")/../.."          # repo root

if ! docker info >/dev/null 2>&1; then
  [ "$(uname)" = "Darwin" ] && open -a Docker
  echo "Waiting for Docker to start ..."
  for _ in $(seq 1 60); do docker info >/dev/null 2>&1 && break; sleep 3; done
  docker info >/dev/null 2>&1 || { echo "Docker is not running. Run ./scripts/mac-linux/install.sh first."; exit 1; }
fi
[ -f .env ] || { cp .env.example .env; echo "Created .env from .env.example."; }
agent_pw=$(grep -E '^APP_DEMO_AGENT_PASSWORD=' .env | cut -d= -f2-)

echo "==> Starting containers (first run builds them - a few minutes)"
docker compose up -d --build --wait --wait-timeout 300 || { docker compose ps; echo "See: docker compose logs backend"; exit 1; }
docker compose ps

echo "==> Quick check"
AGENT_PASSWORD="$agent_pw" ./scripts/smoke-test.sh || echo "A check failed - see above."

echo
echo "Northstar CRM is running."
echo "  App:    http://localhost:4200"
echo "  Login:  agent / $agent_pw   (from your .env file)"
echo "  Stop:   ./scripts/mac-linux/stop.sh"
if [ "${1:-}" != "--no-browser" ]; then
  if command -v open >/dev/null; then open http://localhost:4200; elif command -v xdg-open >/dev/null; then xdg-open http://localhost:4200 >/dev/null 2>&1 || true; fi
fi
