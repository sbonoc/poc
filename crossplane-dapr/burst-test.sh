#!/usr/bin/env bash
set -euo pipefail

usage() {
  cat <<EOF
Usage: ./burst-test.sh [--help|help]

Environment variables:
  URL              Target publish endpoint (default: http://localhost:8080/publish)
  TOTAL_REQUESTS   Number of requests to send (default: 50)
  BATCH_SIZE       Parallel batch size (default: 10)
  EXPECTED_STATUS  Expected HTTP status per request (default: 202)
EOF
}

if [[ "${1:-}" =~ ^(--help|-h|help)$ ]]; then
  usage
  exit 0
fi

URL="${URL:-http://localhost:8080/publish}"
TOTAL_REQUESTS="${TOTAL_REQUESTS:-50}"
BATCH_SIZE="${BATCH_SIZE:-10}"
EXPECTED_STATUS="${EXPECTED_STATUS:-202}"

base_url="${URL%/publish}"

if ! curl -sSf "${base_url}/health/ready" >/dev/null; then
  echo "Cannot reach producer at ${base_url}."
  echo "If running in Kubernetes, expose it first:"
  echo "  kubectl port-forward svc/producer 8080:8080"
  exit 1
fi

printf 'Running burst test against %s with %s requests\n' "$URL" "$TOTAL_REQUESTS"

failures=0

for i in $(seq 1 "$TOTAL_REQUESTS"); do
  (
    code="$(
      curl -sS -o /dev/null -w '%{http_code}' -X POST "$URL" \
        -H "Content-Type: application/json" \
        -d "{\"id\":\"ORD-$i\",\"amount\":$((RANDOM % 100 + 1)).0}" || true
    )"

    if [[ "${code}" != "${EXPECTED_STATUS}" ]]; then
      echo "Request ${i} failed: expected ${EXPECTED_STATUS}, got ${code:-000}" >&2
      exit 1
    fi
  ) &

  if (( i % BATCH_SIZE == 0 )); then
    for pid in $(jobs -p); do
      if ! wait "$pid"; then
        failures=$((failures + 1))
      fi
    done
    printf 'Sent %s requests\n' "$i"
  fi
done

for pid in $(jobs -p); do
  if ! wait "$pid"; then
    failures=$((failures + 1))
  fi
done

if (( failures > 0 )); then
  echo "Burst test failed with ${failures} request batch error(s)."
  exit 1
fi

printf 'Burst test completed\n'
