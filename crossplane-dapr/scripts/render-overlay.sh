#!/usr/bin/env bash
set -euo pipefail

usage() {
  cat <<EOF
Usage: ./scripts/render-overlay.sh <local|prod>

Renders infra manifests with environment-specific substitutions into:
  build/rendered/<environment>/infra

Inputs:
  env/<environment>.env

Substitutions:
  \${APP_NAMESPACE}
  \${GCP_PROJECT_ID}
  \${PROVIDER_CONFIG_NAME}
  \${GCP_SECRET_NAME}
EOF
}

if [[ "${1:-}" =~ ^(--help|-h|help)$ ]]; then
  usage
  exit 0
fi

ENVIRONMENT="${1:-}"
if [[ "${ENVIRONMENT}" != "local" && "${ENVIRONMENT}" != "prod" ]]; then
  usage >&2
  exit 1
fi

if ! command -v envsubst >/dev/null 2>&1; then
  echo "envsubst is required (install gettext)." >&2
  exit 1
fi

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"
ENV_FILE="${ROOT_DIR}/env/${ENVIRONMENT}.env"
OUT_DIR="${ROOT_DIR}/build/rendered/${ENVIRONMENT}"

if [[ ! -f "${ENV_FILE}" ]]; then
  echo "Missing environment file: ${ENV_FILE}" >&2
  exit 1
fi

rm -rf "${OUT_DIR}"
mkdir -p "${OUT_DIR}"
cp -R "${ROOT_DIR}/infra" "${OUT_DIR}/infra"

set -a
# shellcheck source=/dev/null
source "${ENV_FILE}"
set +a

SUBST_VARS='${APP_NAMESPACE} ${GCP_PROJECT_ID} ${PROVIDER_CONFIG_NAME} ${GCP_SECRET_NAME}'

find "${OUT_DIR}/infra" -type f \( -name '*.yaml' -o -name '*.yml' \) -print0 | while IFS= read -r -d '' file; do
  tmp="${file}.tmp"
  envsubst "${SUBST_VARS}" < "${file}" > "${tmp}"
  mv "${tmp}" "${file}"
done

if rg -n '\$\{(APP_NAMESPACE|GCP_PROJECT_ID|PROVIDER_CONFIG_NAME|GCP_SECRET_NAME)\}' "${OUT_DIR}/infra" >/dev/null; then
  echo "Unresolved placeholders found after rendering." >&2
  exit 1
fi

echo "Rendered manifests to ${OUT_DIR}/infra"
