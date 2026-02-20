#!/usr/bin/env bash
set -euo pipefail

usage() {
  cat <<EOF
Usage: ./scripts/test-pyramid-targets.sh <stacks|services|service_regex>

Outputs canonical test-pyramid target definitions used by Makefile, CI, and scripts.
EOF
}

STACKS=(ktor springboot gin)
SERVICES=(
  producer-ktor
  consumer-ktor
  producer-springboot
  consumer-springboot
  producer-gin
  consumer-gin
)

case "${1:-}" in
  stacks)
    printf '%s\n' "${STACKS[*]}"
    ;;
  services)
    printf '%s\n' "${SERVICES[*]}"
    ;;
  service_regex)
    printf '%s\n' "(producer|consumer)-.+"
    ;;
  --help|-h|help)
    usage
    ;;
  *)
    usage >&2
    exit 1
    ;;
esac
