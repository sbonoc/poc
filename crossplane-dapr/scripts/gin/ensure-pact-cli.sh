#!/usr/bin/env bash
set -euo pipefail

usage() {
  cat <<EOF
Usage: ./scripts/gin/ensure-pact-cli.sh [--help|help]

Prints the Pact CLI bin directory after ensuring required binaries are installed.

Environment variables:
  PACT_CLI_VERSION  Pact CLI version to install/check (default: v2.0.7)
  PACT_CLI_DIR      Install directory (default: ./.tools/pact-cli)
  PACT_INSTALLER_REF     Immutable git ref for pact-standalone install.sh
  PACT_INSTALLER_SHA256  SHA-256 for pinned install.sh content
EOF
}

if [[ "${1:-}" =~ ^(--help|-h|help)$ ]]; then
  usage
  exit 0
fi

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "${SCRIPT_DIR}/../.." && pwd)"

PACT_CLI_VERSION="${PACT_CLI_VERSION:-v2.0.7}"
PACT_CLI_DIR="${PACT_CLI_DIR:-${ROOT_DIR}/.tools/pact-cli}"
PACT_CLI_BIN_DIR="${PACT_CLI_DIR}/pact/bin"
VERSION_MARKER="${PACT_CLI_DIR}/.version"
PACT_INSTALLER_REF="${PACT_INSTALLER_REF:-f2bef2fbf90a0ca93faf7c08b4748d33bb8663cc}"
PACT_INSTALLER_SHA256="${PACT_INSTALLER_SHA256:-042f8734c02bc2a694290ba141d2738ae9255ed376960f629ed483d11e0dc2f2}"
PACT_INSTALLER_URL="https://raw.githubusercontent.com/pact-foundation/pact-standalone/${PACT_INSTALLER_REF}/install.sh"

has_expected_cli() {
  [[ -x "${PACT_CLI_BIN_DIR}/pact-provider-verifier" ]] &&
    [[ -x "${PACT_CLI_BIN_DIR}/pact-mock-service" ]] &&
    [[ -x "${PACT_CLI_BIN_DIR}/pact-broker" ]] &&
    [[ -f "${VERSION_MARKER}" ]] &&
    [[ "$(cat "${VERSION_MARKER}")" == "${PACT_CLI_VERSION}" ]]
}

install_cli() {
  if ! command -v curl >/dev/null 2>&1; then
    echo "curl is required to install Pact CLI tools for Gin contract tests." >&2
    exit 1
  fi

  local -a checksum_cmd=()
  if command -v sha256sum >/dev/null 2>&1; then
    checksum_cmd=(sha256sum)
  elif command -v shasum >/dev/null 2>&1; then
    checksum_cmd=(shasum -a 256)
  else
    echo "sha256sum or shasum is required to verify the Pact installer checksum." >&2
    exit 1
  fi

  local tmp_dir
  tmp_dir="$(mktemp -d)"
  trap 'rm -rf "${tmp_dir:-}"' EXIT

  local installer_file
  installer_file="${tmp_dir}/install.sh"
  curl -fsSL "${PACT_INSTALLER_URL}" -o "${installer_file}"

  local actual_checksum
  actual_checksum="$("${checksum_cmd[@]}" "${installer_file}" | awk '{print $1}')"
  if [[ "${actual_checksum}" != "${PACT_INSTALLER_SHA256}" ]]; then
    echo "Pact installer checksum mismatch." >&2
    echo "Expected: ${PACT_INSTALLER_SHA256}" >&2
    echo "Actual:   ${actual_checksum}" >&2
    exit 1
  fi

  (
    cd "${tmp_dir}"
    PACT_CLI_VERSION="${PACT_CLI_VERSION}" bash "${installer_file}"
  )

  rm -rf "${PACT_CLI_DIR}"
  mkdir -p "${PACT_CLI_DIR}"
  mv "${tmp_dir}/pact" "${PACT_CLI_DIR}/pact"
  printf '%s\n' "${PACT_CLI_VERSION}" >"${VERSION_MARKER}"
}

if ! has_expected_cli; then
  install_cli
fi

printf '%s\n' "${PACT_CLI_BIN_DIR}"
