#!/usr/bin/env bash
set -euo pipefail

usage() {
  cat <<USAGE
Usage: ./scripts/gin/quality-check.sh

Runs Gin stack quality checks for producer-gin + consumer-gin:
  1) gofmt check (fails if files are not formatted)
  2) go vet
  3) unit tests

Environment:
  GOCACHE      Go build cache directory (default: /tmp/go-build)
  GOMODCACHE   Go module cache directory (default: /tmp/go-mod)
USAGE
}

if [[ "${1:-}" =~ ^(--help|-h|help)$ ]]; then
  usage
  exit 0
fi

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "${SCRIPT_DIR}/../.." && pwd)"
GOCACHE="${GOCACHE:-/tmp/go-build}"
GOMODCACHE="${GOMODCACHE:-/tmp/go-mod}"

GO_FILES="$(find "${ROOT_DIR}/producer-gin" "${ROOT_DIR}/consumer-gin" -type f -name '*.go' | sort)"
if [[ -z "${GO_FILES}" ]]; then
  echo "No Go files found under producer-gin/consumer-gin."
  exit 1
fi

echo "Checking Go formatting (gofmt)"
UNFORMATTED="$(gofmt -l ${GO_FILES})"
if [[ -n "${UNFORMATTED}" ]]; then
  echo "Go files are not formatted. Run: gofmt -w <files>" >&2
  echo "${UNFORMATTED}" >&2
  exit 1
fi

run_module_vet() {
  local module="$1"
  echo "Running go vet for ${module}"
  (cd "${ROOT_DIR}/${module}" && GOCACHE="${GOCACHE}" GOMODCACHE="${GOMODCACHE}" go vet ./...)
}

run_module_unit_test() {
  local module="$1"
  echo "Running unit tests for ${module}"
  (cd "${ROOT_DIR}/${module}" && GOCACHE="${GOCACHE}" GOMODCACHE="${GOMODCACHE}" go test ./...)
}

run_module_vet "producer-gin"
run_module_vet "consumer-gin"
run_module_unit_test "producer-gin"
run_module_unit_test "consumer-gin"

echo "Gin quality checks passed."
