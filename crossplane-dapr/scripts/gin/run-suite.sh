#!/usr/bin/env bash
set -euo pipefail

usage() {
  cat <<USAGE
Usage: ./scripts/gin/run-suite.sh <suite>

Suites:
  unit         Run unit tests for producer-gin + consumer-gin
  integration  Run integration tests for producer-gin + consumer-gin
  contract     Run contract tests for producer-gin + consumer-gin
  e2e          Run e2e tests for producer-gin + consumer-gin

Environment:
  GOCACHE      Go build cache directory (default: /tmp/go-build)
  GOMODCACHE   Go module cache directory (default: /tmp/go-mod)
  PACT_CLI_VERSION  Pact CLI version for gin contract tests (default: v2.0.7)
  PACT_CLI_DIR      Pact CLI install directory (default: ./.tools/pact-cli)
USAGE
}

if [[ "${1:-}" =~ ^(--help|-h|help)$ ]]; then
  usage
  exit 0
fi

suite="${1:-}"
if [[ -z "${suite}" ]]; then
  usage
  exit 1
fi

case "${suite}" in
  unit) tags="" ;;
  integration) tags="integration" ;;
  contract) tags="contract" ;;
  e2e) tags="e2e" ;;
  *)
    echo "Unknown suite: ${suite}" >&2
    usage
    exit 1
    ;;
esac

GOCACHE="${GOCACHE:-/tmp/go-build}"
GOMODCACHE="${GOMODCACHE:-/tmp/go-mod}"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "${SCRIPT_DIR}/../.." && pwd)"

run_module() {
  local module="$1"
  if [[ -n "${tags}" ]]; then
    (cd "${ROOT_DIR}/${module}" && GOCACHE="${GOCACHE}" GOMODCACHE="${GOMODCACHE}" go test -tags="${tags}" ./...)
  else
    (cd "${ROOT_DIR}/${module}" && GOCACHE="${GOCACHE}" GOMODCACHE="${GOMODCACHE}" go test ./...)
  fi
}

if [[ "${suite}" == "contract" ]]; then
  PACT_CLI_BIN_DIR="$("${SCRIPT_DIR}/ensure-pact-cli.sh")"
  export PATH="${PACT_CLI_BIN_DIR}:${PATH}"

  echo "Running ${suite} tests for consumer-gin"
  run_module "consumer-gin"

  echo "Running ${suite} tests for producer-gin"
  run_module "producer-gin"
else
  echo "Running ${suite} tests for producer-gin"
  run_module "producer-gin"

  echo "Running ${suite} tests for consumer-gin"
  run_module "consumer-gin"
fi
