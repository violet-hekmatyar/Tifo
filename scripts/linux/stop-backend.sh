#!/usr/bin/env bash
set -euo pipefail

PID_FILE="${PID_FILE:-/opt/south-stand/backend/south-stand-server.pid}"

if [[ ! -f "$PID_FILE" ]]; then
  echo "PID file not found: $PID_FILE"
  exit 0
fi

PID="$(cat "$PID_FILE")"
if kill -0 "$PID" 2>/dev/null; then
  kill "$PID"
  rm -f "$PID_FILE"
  echo "south-stand-server stopped, PID $PID"
else
  rm -f "$PID_FILE"
  echo "stale PID file removed"
fi
