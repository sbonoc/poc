#!/usr/bin/env bash
set -euo pipefail

usage() {
  cat <<EOF
Usage: ./scripts/create-local-gcp-emulator-secret.sh [NAMESPACE] [SECRET_NAME]

Arguments:
  NAMESPACE    Namespace where the secret will be created (default: crossplane-system)
  SECRET_NAME  Secret name (default: gcp-emulator-credentials)

Environment variables:
  PROJECT_ID   GCP project id to embed in generated credentials (default: local-project)
EOF
}

if [[ "${1:-}" =~ ^(--help|-h|help)$ ]]; then
  usage
  exit 0
fi

NAMESPACE="${1:-crossplane-system}"
SECRET_NAME="${2:-gcp-emulator-credentials}"
PROJECT_ID="${PROJECT_ID:-local-project}"

if ! command -v openssl >/dev/null 2>&1; then
  echo "openssl is required to generate local emulator credentials"
  exit 1
fi

tmp_dir="$(mktemp -d)"
trap 'rm -rf "${tmp_dir}"' EXIT

key_file="${tmp_dir}/private_key.pem"
json_file="${tmp_dir}/creds.json"

openssl genrsa 2048 > "${key_file}" 2>/dev/null
private_key_escaped="$(awk '{printf "%s\\n", $0}' "${key_file}")"

cat > "${json_file}" <<JSON
{
  "type": "service_account",
  "project_id": "${PROJECT_ID}",
  "private_key_id": "local-emulator-key",
  "private_key": "${private_key_escaped}",
  "client_email": "local-emulator@${PROJECT_ID}.iam.gserviceaccount.com",
  "client_id": "1234567890",
  "auth_uri": "https://accounts.google.com/o/oauth2/auth",
  "token_uri": "https://oauth2.googleapis.com/token",
  "auth_provider_x509_cert_url": "https://www.googleapis.com/oauth2/v1/certs",
  "client_x509_cert_url": "https://www.googleapis.com/robot/v1/metadata/x509/local-emulator%40${PROJECT_ID}.iam.gserviceaccount.com"
}
JSON

kubectl create namespace "${NAMESPACE}" --dry-run=client -o yaml | kubectl apply -f - >/dev/null

kubectl -n "${NAMESPACE}" create secret generic "${SECRET_NAME}" \
  --from-file=creds="${json_file}" \
  --dry-run=client -o yaml | kubectl apply -f -

echo "Generated emulator credentials secret ${SECRET_NAME} in namespace ${NAMESPACE}."
