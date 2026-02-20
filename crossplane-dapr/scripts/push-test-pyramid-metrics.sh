#!/usr/bin/env bash
set -euo pipefail

usage() {
  cat <<EOF
Usage: ./scripts/push-test-pyramid-metrics.sh [PUSHGATEWAY_URL] [LATEST_JOB] [LATEST_INSTANCE]

Arguments:
  PUSHGATEWAY_URL  Pushgateway base URL (default: http://localhost:9091)
  LATEST_JOB       Pushgateway job label for "latest run" metrics (default: test-pyramid-latest)
  LATEST_INSTANCE  Pushgateway instance label for latest metrics (default: latest)

Environment variables:
  HISTORY_JOB            Pushgateway job label for run history (default: test-pyramid-history)
  RUN_ID                 Unique run id used for history instance label (default: UTC timestamp)
  PUSH_HISTORY           Set to "true" to also write per-run history metrics (default: false)
  SKIP_TEST_EXECUTION    Set to "true" to skip running ./gradlew testPyramidMetrics (default: false)
EOF
}

if [[ "${1:-}" =~ ^(--help|-h|help)$ ]]; then
  usage
  exit 0
fi

PUSHGATEWAY_URL="${1:-http://localhost:9091}"
LATEST_JOB="${2:-test-pyramid-latest}"
LATEST_INSTANCE="${3:-latest}"
HISTORY_JOB="${HISTORY_JOB:-test-pyramid-history}"
PUSH_HISTORY="${PUSH_HISTORY:-false}"
SKIP_TEST_EXECUTION="${SKIP_TEST_EXECUTION:-false}"
METRICS_FILE="build/reports/test-pyramid/test-pyramid.prom"

if [[ "${SKIP_TEST_EXECUTION}" != "true" ]]; then
  ./gradlew testPyramidMetrics
fi

if [[ ! -f "${METRICS_FILE}" ]]; then
  echo "Metrics file not found: ${METRICS_FILE}"
  exit 1
fi

curl -fsS --data-binary @"${METRICS_FILE}" \
  "${PUSHGATEWAY_URL%/}/metrics/job/${LATEST_JOB}/instance/${LATEST_INSTANCE}"

if [[ "${PUSH_HISTORY}" == "true" ]]; then
  default_run_id="$(date -u +%Y%m%dT%H%M%SZ)"
  raw_run_id="${RUN_ID:-${default_run_id}}"
  history_instance="$(echo "${raw_run_id}" | tr -cs '[:alnum:]_.-' '-' | sed -E 's/^-+//; s/-+$//; s/-+/-/g')"
  curl -fsS --data-binary @"${METRICS_FILE}" \
    "${PUSHGATEWAY_URL%/}/metrics/job/${HISTORY_JOB}/instance/${history_instance}"
fi

echo "Pushed latest test pyramid metrics to ${PUSHGATEWAY_URL} (job=${LATEST_JOB}, instance=${LATEST_INSTANCE})."
if [[ "${PUSH_HISTORY}" == "true" ]]; then
  echo "Pushed historical test pyramid metrics to ${PUSHGATEWAY_URL} (job=${HISTORY_JOB}, run_id=${raw_run_id})."
fi
