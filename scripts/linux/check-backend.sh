#!/usr/bin/env bash
set -euo pipefail

HEALTH_URL="${HEALTH_URL:-http://127.0.0.1:8080/api/public/health}"

echo "Checking $HEALTH_URL"
curl -fsS "$HEALTH_URL"
echo
