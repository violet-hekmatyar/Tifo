#!/usr/bin/env bash
set -euo pipefail

APP_JAR="${APP_JAR:-/opt/south-stand/backend/south-stand-server.jar}"
LOG_DIR="${LOG_DIR:-/opt/south-stand/logs/backend}"
PID_FILE="${PID_FILE:-/opt/south-stand/backend/south-stand-server.pid}"

mkdir -p "$LOG_DIR"

export SPRING_PROFILES_ACTIVE="${SPRING_PROFILES_ACTIVE:-prod}"
export MYSQL_HOST="${MYSQL_HOST:-127.0.0.1}"
export REDIS_HOST="${REDIS_HOST:-127.0.0.1}"

if [[ ! -f "$APP_JAR" ]]; then
  echo "Jar not found: $APP_JAR" >&2
  exit 1
fi

if [[ -f "$PID_FILE" ]] && kill -0 "$(cat "$PID_FILE")" 2>/dev/null; then
  echo "south-stand-server is already running, PID $(cat "$PID_FILE")"
  exit 0
fi

nohup java ${JAVA_OPTS:-} -jar "$APP_JAR" > "$LOG_DIR/app.log" 2>&1 &
echo $! > "$PID_FILE"
echo "south-stand-server started, PID $(cat "$PID_FILE")"
