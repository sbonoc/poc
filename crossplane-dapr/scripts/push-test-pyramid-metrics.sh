#!/usr/bin/env bash
set -euo pipefail

usage() {
  cat <<EOF
Usage: ./scripts/push-test-pyramid-metrics.sh [PUSHGATEWAY_URL] [LATEST_JOB_PREFIX] [LATEST_INSTANCE]

Arguments:
  PUSHGATEWAY_URL  Pushgateway base URL (default: http://localhost:9091)
  LATEST_JOB_PREFIX Pushgateway job prefix for latest metrics (default: test-pyramid-latest)
  LATEST_INSTANCE  Pushgateway instance label for latest metrics (default: latest)

Environment variables:
  STACKS                 Space-delimited stack targets to push (default: value from ./scripts/test-pyramid-targets.sh stacks)
  SERVICES               Space-delimited service targets to push (default: value from ./scripts/test-pyramid-targets.sh services)
  PUSH_STACKS            Set to "false" to skip stack-level push (default: true)
  PUSH_SERVICES          Set to "false" to skip service-level push (default: true)
  HISTORY_JOB_PREFIX     Pushgateway job prefix for run history (default: test-pyramid-history)
  RUN_ID                 Unique run id used for history instance label (default: UTC timestamp)
  PUSH_HISTORY           Set to "true" to also write per-run history metrics (default: false)
  SKIP_TEST_EXECUTION    Set to "true" to skip running metric collection tasks (default: false)
EOF
}

if [[ "${1:-}" =~ ^(--help|-h|help)$ ]]; then
  usage
  exit 0
fi

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
TARGETS_SCRIPT="${SCRIPT_DIR}/test-pyramid-targets.sh"
if [[ ! -x "${TARGETS_SCRIPT}" ]]; then
  echo "Required helper is missing or not executable: ${TARGETS_SCRIPT}" >&2
  exit 1
fi

PUSHGATEWAY_URL="${1:-http://localhost:9091}"
LATEST_JOB_PREFIX="${2:-test-pyramid-latest}"
LATEST_INSTANCE="${3:-latest}"
STACKS="${STACKS:-$("${TARGETS_SCRIPT}" stacks)}"
SERVICES="${SERVICES:-$("${TARGETS_SCRIPT}" services)}"
PUSH_STACKS="${PUSH_STACKS:-true}"
PUSH_SERVICES="${PUSH_SERVICES:-true}"
HISTORY_JOB_PREFIX="${HISTORY_JOB_PREFIX:-test-pyramid-history}"
PUSH_HISTORY="${PUSH_HISTORY:-false}"
SKIP_TEST_EXECUTION="${SKIP_TEST_EXECUTION:-false}"

if [[ "${SKIP_TEST_EXECUTION}" != "true" ]]; then
  ./gradlew testPyramidMetricsJvm
  ./scripts/gin/collect-test-pyramid.sh
fi

push_latest_metrics() {
  local target="$1"
  local metrics_file="build/reports/test-pyramid/${target}/test-pyramid.prom"
  local latest_job="${LATEST_JOB_PREFIX}-${target}"

  if [[ ! -f "${metrics_file}" ]]; then
    echo "Metrics file not found for target ${target}: ${metrics_file}"
    exit 1
  fi

  curl -fsS --data-binary @"${metrics_file}" \
    "${PUSHGATEWAY_URL%/}/metrics/job/${latest_job}/instance/${LATEST_INSTANCE}"

  echo "Pushed latest test pyramid metrics to ${PUSHGATEWAY_URL} (target=${target}, job=${latest_job}, instance=${LATEST_INSTANCE})."
}

if [[ "${PUSH_STACKS}" == "true" ]]; then
  for stack in ${STACKS}; do
    push_latest_metrics "${stack}"
  done
fi

if [[ "${PUSH_SERVICES}" == "true" ]]; then
  for service in ${SERVICES}; do
    push_latest_metrics "${service}"
  done
fi

if [[ "${PUSH_HISTORY}" == "true" ]]; then
  default_run_id="$(date -u +%Y%m%dT%H%M%SZ)"
  raw_run_id="${RUN_ID:-${default_run_id}}"
  history_instance="$(echo "${raw_run_id}" | tr -cs '[:alnum:]_.-' '-' | sed -E 's/^-+//; s/-+$//; s/-+/-/g')"

  push_history_metrics() {
    local target="$1"
    local metrics_file="build/reports/test-pyramid/${target}/test-pyramid.prom"
    local history_job="${HISTORY_JOB_PREFIX}-${target}"

    if [[ ! -f "${metrics_file}" ]]; then
      echo "Metrics file not found for target ${target}: ${metrics_file}"
      exit 1
    fi

    curl -fsS --data-binary @"${metrics_file}" \
      "${PUSHGATEWAY_URL%/}/metrics/job/${history_job}/instance/${history_instance}"
    echo "Pushed historical test pyramid metrics to ${PUSHGATEWAY_URL} (target=${target}, job=${history_job}, run_id=${raw_run_id})."
  }

  if [[ "${PUSH_STACKS}" == "true" ]]; then
    for stack in ${STACKS}; do
      push_history_metrics "${stack}"
    done
  fi

  if [[ "${PUSH_SERVICES}" == "true" ]]; then
    for service in ${SERVICES}; do
      push_history_metrics "${service}"
    done
  fi
fi
