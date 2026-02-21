#!/usr/bin/env bash
set -euo pipefail

usage() {
  cat <<EOF
Usage: ./deploy.sh [--port-forward] [--help|help]

Options:
  --port-forward   Start all app + grafana port-forwards after deploy (blocks until Ctrl+C)
  --help, help     Show this help

Examples:
  ./deploy.sh
  APP_NAMESPACE=agnostic-local ./deploy.sh --port-forward
EOF
}

with_port_forward=false

while [[ $# -gt 0 ]]; do
  case "$1" in
    --port-forward)
      with_port_forward=true
      shift
      ;;
    --help|-h|help)
      usage
      exit 0
      ;;
    *)
      echo "Unknown argument: $1"
      usage
      exit 1
      ;;
  esac
done

make deploy-local

if [[ "${with_port_forward}" == "true" ]]; then
  exec make port-forward-local
fi

echo "Deployment finished."
echo "Run 'make port-forward-local' to expose all apps, Loki, and Grafana."
