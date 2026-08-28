#!/bin/bash
# Entrypoint: fix Docker named-volume permissions, then drop to spring user.
set -e

STORAGE_DIR="/data/storage"
if [ -d "$STORAGE_DIR" ]; then
  CURRENT_UID=$(stat -c %u "$STORAGE_DIR" 2>/dev/null || echo "1001")
  if [ "$CURRENT_UID" != "1001" ]; then
    echo "[entrypoint] Fixing ownership of $STORAGE_DIR (current UID=$CURRENT_UID)"
    chown -R spring:spring "$STORAGE_DIR" || true
  fi
fi

# Ensure Playwright can find its pre-installed browsers
export PLAYWRIGHT_BROWSERS_PATH="/opt/playwright-browsers"

# Drop from root → spring and exec the JVM
exec runuser -u spring -- env "PLAYWRIGHT_BROWSERS_PATH=$PLAYWRIGHT_BROWSERS_PATH" java -jar app.jar
