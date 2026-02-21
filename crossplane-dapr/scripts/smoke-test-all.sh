#!/usr/bin/env bash
set -euo pipefail

usage() {
  cat <<EOF
Usage: ./scripts/smoke-test-all.sh [--help|help]

Environment variables:
  PORT_FORWARD_NAMESPACE       Namespace used for temporary port-forward (default: default)
  APP_SERVICE_PORT             Service port in Kubernetes (default: 8080)
  PRODUCER_KTOR_LOCAL_PORT     Local producer-ktor port (default: 8080)
  PRODUCER_GIN_LOCAL_PORT      Local producer-gin port (default: 8082)
  PRODUCER_SPRINGBOOT_LOCAL_PORT Local producer-springboot port (default: 8084)
  TOTAL_REQUESTS              Number of requests to send per producer (default: 50)
  BATCH_SIZE                  Parallel batch size per producer (default: 10)
  EXPECTED_STATUS             Expected HTTP status per request (default: 202)
EOF
}

if [[ "${1:-}" =~ ^(--help|-h|help)$ ]]; then
  usage
  exit 0
fi

PORT_FORWARD_NAMESPACE="${PORT_FORWARD_NAMESPACE:-default}"
APP_SERVICE_PORT="${APP_SERVICE_PORT:-8080}"
PRODUCER_KTOR_LOCAL_PORT="${PRODUCER_KTOR_LOCAL_PORT:-8080}"
PRODUCER_GIN_LOCAL_PORT="${PRODUCER_GIN_LOCAL_PORT:-8082}"
PRODUCER_SPRINGBOOT_LOCAL_PORT="${PRODUCER_SPRINGBOOT_LOCAL_PORT:-8084}"
TOTAL_REQUESTS="${TOTAL_REQUESTS:-50}"
BATCH_SIZE="${BATCH_SIZE:-10}"
EXPECTED_STATUS="${EXPECTED_STATUS:-202}"

if ! [[ "${TOTAL_REQUESTS}" =~ ^[1-9][0-9]*$ ]]; then
  echo "TOTAL_REQUESTS must be a positive integer (got: ${TOTAL_REQUESTS})" >&2
  exit 1
fi

if ! [[ "${BATCH_SIZE}" =~ ^[1-9][0-9]*$ ]]; then
  echo "BATCH_SIZE must be a positive integer (got: ${BATCH_SIZE})" >&2
  exit 1
fi

services=(
  "producer-ktor:${PRODUCER_KTOR_LOCAL_PORT}"
  "producer-gin:${PRODUCER_GIN_LOCAL_PORT}"
  "producer-springboot:${PRODUCER_SPRINGBOOT_LOCAL_PORT}"
)

started_pids=()
needs_port_forward=()

cleanup() {
  for pid in "${started_pids[@]:-}"; do
    kill "${pid}" >/dev/null 2>&1 || true
  done
}
trap cleanup EXIT INT TERM

is_ready() {
  local port="$1"
  local path
  for path in "/health/ready" "/health/readiness" "/actuator/health/readiness"; do
    if curl -fsS "http://localhost:${port}${path}" >/dev/null 2>&1; then
      return 0
    fi
  done
  return 1
}

wait_for_available_deployment() {
  local service="$1"
  local timeout_seconds="${2:-120}"

  if ! kubectl -n "${PORT_FORWARD_NAMESPACE}" get deployment "${service}" >/dev/null 2>&1; then
    echo "Deployment ${service} not found in namespace ${PORT_FORWARD_NAMESPACE}."
    echo "If you deployed to another namespace, run smoke-test with PORT_FORWARD_NAMESPACE=<namespace>."
    exit 1
  fi

  if ! kubectl -n "${PORT_FORWARD_NAMESPACE}" wait --for=condition=available "deployment/${service}" --timeout="${timeout_seconds}s"; then
    echo "Deployment ${service} is not available in namespace ${PORT_FORWARD_NAMESPACE}."
    kubectl -n "${PORT_FORWARD_NAMESPACE}" get deployment "${service}" -o wide || true
    kubectl -n "${PORT_FORWARD_NAMESPACE}" get pods -l "app=${service}" -o wide || true
    kubectl -n "${PORT_FORWARD_NAMESPACE}" logs "deployment/${service}" --tail=60 || true
    exit 1
  fi
}

run_burst_test() {
  local service="$1"
  local url="$2"
  local failures=0
  local request_pids=()

  printf 'Running burst test for %s at %s with %s requests\n' "${service}" "${url}" "${TOTAL_REQUESTS}"

  for i in $(seq 1 "${TOTAL_REQUESTS}"); do
    (
      code="$(
        curl -sS -o /dev/null -w '%{http_code}' -X POST "${url}" \
          -H "Content-Type: application/json" \
          -d "{\"id\":\"ORD-${service}-${i}\",\"amount\":$((RANDOM % 100 + 1)).0}" || true
      )"

      if [[ "${code}" != "${EXPECTED_STATUS}" ]]; then
        echo "${service}: request ${i} failed: expected ${EXPECTED_STATUS}, got ${code:-000}" >&2
        exit 1
      fi
    ) &
    request_pids+=("$!")

    if (( i % BATCH_SIZE == 0 )); then
      if (( ${#request_pids[@]} > 0 )); then
        for pid in "${request_pids[@]}"; do
          if ! wait "${pid}"; then
            failures=$((failures + 1))
          fi
        done
      fi
      request_pids=()
      printf '%s: sent %s requests\n' "${service}" "${i}"
    fi
  done

  if (( ${#request_pids[@]} > 0 )); then
    for pid in "${request_pids[@]}"; do
      if ! wait "${pid}"; then
        failures=$((failures + 1))
      fi
    done
  fi

  if (( failures > 0 )); then
    echo "${service}: burst test failed with ${failures} request batch error(s)." >&2
    return 1
  fi

  printf '%s: burst test completed\n' "${service}"
}

for service_entry in "${services[@]}"; do
  service="${service_entry%%:*}"
  port="${service_entry##*:}"
  if is_ready "${port}"; then
    echo "Using existing endpoint for ${service} at http://localhost:${port}"
  else
    needs_port_forward+=("${service_entry}")
  fi
done

if [[ "${#needs_port_forward[@]}" -gt 0 ]]; then
  command -v kubectl >/dev/null || (echo "kubectl is required to auto port-forward producers" && exit 1)
  mkdir -p build

  for service_entry in "${needs_port_forward[@]}"; do
    service="${service_entry%%:*}"
    port="${service_entry##*:}"
    log_file="build/pf-${service}-smoke.log"

    wait_for_available_deployment "${service}" 120
    kubectl -n "${PORT_FORWARD_NAMESPACE}" port-forward "svc/${service}" "${port}:${APP_SERVICE_PORT}" >"${log_file}" 2>&1 &
    pid="$!"

    started_pids+=("${pid}")
  done

  sleep 2

  for service_entry in "${needs_port_forward[@]}"; do
    service="${service_entry%%:*}"
    port="${service_entry##*:}"
    log_file="build/pf-${service}-smoke.log"
    if ! is_ready "${port}"; then
      echo "Cannot reach ${service} at http://localhost:${port} after auto port-forward."
      echo "Last lines from ${log_file}:"
      tail -n 20 "${log_file}" || true
      exit 1
    fi
  done
fi

for service_entry in "${services[@]}"; do
  service="${service_entry%%:*}"
  port="${service_entry##*:}"
  run_burst_test "${service}" "http://localhost:${port}/publish"
done
