#!/usr/bin/env bash
set -euo pipefail

usage() {
  cat <<EOF
Usage: ./teardown.sh [--all] [--help|help]

Options:
  --all            Also remove Dapr/Crossplane control planes
  --help, help     Show this help
EOF
}

destroy_control_planes=false

while [[ $# -gt 0 ]]; do
  case "$1" in
    --all)
      destroy_control_planes=true
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

make teardown-local

if [[ "${destroy_control_planes}" == "true" ]]; then
  make destroy-control-planes
fi
