#!/bin/bash
# Entrypoint: fix Docker named-volume permissions, then drop to spring user.
set -e

STORAGE_DIR="/data/storage"
if [ -d "$STORAGE_DIR" ]; then
  # The volume root can retain spring ownership while nested bucket directories
  # are created by an earlier root process. Repair the full tree before dropping
  # privileges so every storage bucket remains writable.
  echo "[entrypoint] Ensuring storage ownership for $STORAGE_DIR"
  chown -R spring:spring "$STORAGE_DIR" || true
fi

# Drop from root → spring and exec the JVM
exec runuser -u spring -- env "HOME=/home/spring" java -jar app.jar
