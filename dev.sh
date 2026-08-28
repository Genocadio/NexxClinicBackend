#!/usr/bin/env bash
# ── NexxClinic Backend Dev Server ──────────────────────────────────────────────
# Builds a jar, then runs it inside a screen session.
# Postgres and Meilisearch stay in Docker.
#
# Usage:
#   ./dev.sh          — restart backend
#   ./dev.sh stop     — stop backend
#   ./dev.sh logs     — tail logs
#   ./dev.sh attach   — attach to screen session (Ctrl-A D to detach)
# ──────────────────────────────────────────────────────────────────────────────
set -euo pipefail
cd "$(dirname "$0")"

PORT=8080
SESSION="nexxclinic-backend"
LOG_FILE="/tmp/nexxclinic-backend.log"
JAR_FILE="build/libs/nexxclinic-0.0.1-SNAPSHOT.jar"

# ── env ──────────────────────────────────────────────────────────────────────
export DB_URL="${DB_URL:-jdbc:postgresql://localhost:5432/nexxclinic}"
export DB_USER="${DB_USER:-nexxclinic_user}"
export DB_PASSWORD="${DB_PASSWORD:-nexxclinic_pass}"
export MEILI_URL="${MEILI_URL:-http://localhost:7700}"
export MEILI_MASTER_KEY="${MEILI_MASTER_KEY:-nexxclinic_meili_master_key}"
export MEILI_ENABLED="${MEILI_ENABLED:-true}"
export STORAGE_TYPE="${STORAGE_TYPE:-LOCAL}"
export LOCAL_STORAGE_PATH="${LOCAL_STORAGE_PATH:-./storage}"
export JWT_SECRET="${JWT_SECRET:-nexxclinic-local-dev-jwt-secret-change-in-production-32b}"
export JWT_EXPIRATION_MINUTES="${JWT_EXPIRATION_MINUTES:-480}"
export JWT_REFRESH_EXPIRATION_DAYS="${JWT_REFRESH_EXPIRATION_DAYS:-30}"

ensure_playwright() {
  # Use a project-local path so we don't need sudo.
  export PLAYWRIGHT_BROWSERS_PATH="$HOME/.cache/ms-playwright"
  mkdir -p "$PLAYWRIGHT_BROWSERS_PATH"

  # Check if any Chromium binary already exists
  if ls "$PLAYWRIGHT_BROWSERS_PATH/chromium-"*/chrome-linux/chrome >/dev/null 2>&1 || \
     ls "$PLAYWRIGHT_BROWSERS_PATH/chromium-"*/chrome-linux/chromium >/dev/null 2>&1; then
    echo "✅ Playwright Chromium already present"
  else
    echo "Installing Playwright Chromium (~85 MiB, one-time)..."
    # Use Node.js playwright CLI to install just chromium (not Firefox/WebKit)
    npx -y playwright@1.44.0 install chromium 2>&1 | tail -5
    echo "✅ Playwright Chromium installed"
  fi
}

stop_backend() {
  # Kill by port first
  local pids
  pids=$(lsof -ti:$PORT 2>/dev/null || true)
  if [ -n "$pids" ]; then
    echo "Killing process(es) on port $PORT..."
    echo "$pids" | xargs kill -9 2>/dev/null || true
    sleep 1
  fi
  # Kill screen session
  if screen -list 2>/dev/null | grep -q "$SESSION"; then
    echo "Killing screen session '$SESSION'..."
    screen -S "$SESSION" -X quit 2>/dev/null || true
    sleep 1
  fi
}

build() {
  echo "Building backend jar..."
  ./gradlew bootJar -x test --quiet 2>&1
  if [ ! -f "$JAR_FILE" ]; then
    echo "❌ Build failed — $JAR_FILE not found"
    exit 1
  fi
  echo "✅ Build complete: $JAR_FILE"
}

start() {
  echo "Starting NexxClinic backend in screen session '$SESSION'..."
  # Pass the Playwright browsers path to the JVM
  export PLAYWRIGHT_BROWSERS_PATH="${PLAYWRIGHT_BROWSERS_PATH:-$HOME/.cache/ms-playwright}"
  screen -dmS "$SESSION" bash -c "
    export PLAYWRIGHT_BROWSERS_PATH='$PLAYWRIGHT_BROWSERS_PATH'
    java -jar $JAR_FILE > $LOG_FILE 2>&1
    echo 'Java process exited. Press Enter to close.' >> $LOG_FILE
  "
  echo "Logs: tail -f $LOG_FILE"

  # Wait for startup
  for i in $(seq 1 40); do
    if curl -sf http://localhost:$PORT/actuator/health >/dev/null 2>&1; then
      echo "✅ Backend healthy — http://localhost:$PORT/graphql"
      return 0
    fi
    sleep 2
  done
  echo "⚠️  Backend may still be starting. Check: tail -f $LOG_FILE"
}

case "${1:-restart}" in
  stop)
    stop_backend
    echo "✅ Backend stopped"
    ;;
  logs)
    tail -f "$LOG_FILE"
    ;;
  attach)
    screen -r "$SESSION"
    ;;
  restart|"")
    stop_backend
    ensure_playwright
    build
    start
    ;;
  *)
    echo "Usage: $0 [restart|stop|logs|attach]"
    exit 1
    ;;
esac
