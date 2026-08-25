#!/bin/sh
# Entrypoint: fix Docker named-volume permissions, then run as spring user.
# Named volumes are created by Docker daemon (root) on first use, so the
# spring user (UID 1001) may not be able to create files inside them.
set -e

STORAGE_DIR="/data/storage"
if [ -d "$STORAGE_DIR" ]; then
  CURRENT_UID=$(stat -c %u "$STORAGE_DIR" 2>/dev/null || echo "1001")
  if [ "$CURRENT_UID" != "1001" ]; then
    echo "[entrypoint] Fixing ownership of $STORAGE_DIR (current UID=$CURRENT_UID)"
    chown -R spring:spring "$STORAGE_DIR" || true
  fi
fi

# Drop from root → spring and exec the JVM
exec su-exec spring java -jar app.jar
