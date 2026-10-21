#!/usr/bin/env bash
# Northstar CRM - one-time setup (macOS / Linux)
# Everything (Angular, Spring Boot, PostgreSQL, Kafka) runs in Docker, so a machine only needs Docker + Git.
# This script checks/installs them, creates .env, and downloads/builds the containers.
set -euo pipefail
cd "$(dirname "$0")/../.."          # repo root
step() { printf '\n==> %s\n' "$1"; }
has()  { command -v "$1" >/dev/null 2>&1; }

step "Checking Git"
if has git; then git --version
elif [ "$(uname)" = "Darwin" ] && has brew; then brew install git
else echo "Please install Git (https://git-scm.com/downloads) and re-run."; exit 1; fi

step "Checking Docker"
if ! has docker; then
  if [ "$(uname)" = "Darwin" ] && has brew; then
    brew install --cask docker
    echo "Docker Desktop installed. Open it once from Applications, accept the terms, then re-run this script."; exit 0
  else
    echo "Please install Docker:"
    echo "  macOS: https://docs.docker.com/desktop/setup/install/mac-install/"
    echo "  Linux: https://docs.docker.com/engine/install/  (include the 'docker compose' plugin)"
    exit 1
  fi
fi
if ! docker info >/dev/null 2>&1; then
  [ "$(uname)" = "Darwin" ] && open -a Docker
  echo "Waiting for Docker to start ..."
  for _ in $(seq 1 60); do docker info >/dev/null 2>&1 && break; sleep 3; done
  docker info >/dev/null 2>&1 || { echo "Docker is not running. Start it and re-run."; exit 1; }
fi
echo "Docker is running."

step "Creating .env (local settings)"
if [ -f .env ]; then echo ".env already exists - keeping it."; else cp .env.example .env; echo "Created .env from .env.example."; fi

step "Downloading and building containers (first time takes a few minutes)"
docker compose pull postgres kafka
docker compose build

printf '\nSetup complete. Next: ./scripts/mac-linux/start.sh\n'
