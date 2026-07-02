#!/usr/bin/env bash
set -euo pipefail

HEALTH_URL="${HEALTH_URL:-http://127.0.0.1:8080/api/public/health}"
DB_HEALTH_URL="${DB_HEALTH_URL:-http://127.0.0.1:8080/api/public/health/db}"
REDIS_HEALTH_URL="${REDIS_HEALTH_URL:-http://127.0.0.1:8080/api/public/health/redis}"
ADMIN_HEALTH_URL="${ADMIN_HEALTH_URL:-http://127.0.0.1:8080/api/admin/health}"

echo "Checking $HEALTH_URL"
curl -fsS "$HEALTH_URL"
echo
echo "Checking $DB_HEALTH_URL"
curl -fsS "$DB_HEALTH_URL"
echo
echo "Checking $REDIS_HEALTH_URL"
curl -fsS "$REDIS_HEALTH_URL"
echo
echo "Checking admin health without token should return application code 40101"
curl -fsS "$ADMIN_HEALTH_URL"
echo
