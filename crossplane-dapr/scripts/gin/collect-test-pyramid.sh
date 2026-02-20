#!/usr/bin/env bash
set -euo pipefail

usage() {
  cat <<USAGE
Usage: ./scripts/gin/collect-test-pyramid.sh [--help|help]

Environment variables:
  SKIP_TEST_EXECUTION  Set to "true" to skip running go test suites (default: false)
  INCLUDE_E2E          Set to "true" to include e2e suite execution in collection (default: false)
  GOCACHE              Go build cache directory (default: /tmp/go-build)
  GOMODCACHE           Go module cache directory (default: /tmp/go-mod)
  PACT_CLI_VERSION     Pact CLI version for gin contract tests (default: v2.0.7)
  PACT_CLI_DIR         Pact CLI install directory (default: ./.tools/pact-cli)
USAGE
}

if [[ "${1:-}" =~ ^(--help|-h|help)$ ]]; then
  usage
  exit 0
fi

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "${SCRIPT_DIR}/../.." && pwd)"
REPORT_ROOT="${ROOT_DIR}/build/reports/test-pyramid"
STACK_REPORT_DIR="${REPORT_ROOT}/gin"
RAW_DIR="${STACK_REPORT_DIR}/raw"

SKIP_TEST_EXECUTION="${SKIP_TEST_EXECUTION:-false}"
INCLUDE_E2E="${INCLUDE_E2E:-false}"
GOCACHE="${GOCACHE:-/tmp/go-build}"
GOMODCACHE="${GOMODCACHE:-/tmp/go-mod}"

modules=(producer-gin consumer-gin)
kind_order=(unit integration contract e2e)
suite_order=(test integrationTest contractTest e2eTest)
run_failed=0

mkdir -p "${RAW_DIR}"

fmt4() {
  perl -e '
    my $n = shift // 0;
    $n = 0 + $n;
    printf "%.4f", $n;
  ' "$1"
}

percentage() {
  local part="$1"
  local total="$2"
  perl -e '
    my ($p, $t) = @ARGV;
    $p = 0 + ($p // 0);
    $t = 0 + ($t // 0);
    if ($t <= 0) {
      print "0.0000";
    } else {
      printf "%.4f", ($p / $t) * 100;
    }
  ' "${part}" "${total}"
}

run_suite() {
  local suite_name="$1"
  local suite_tag="$2"

  local suite_modules=("${modules[@]}")
  if [[ "${suite_name}" == "contractTest" ]]; then
    suite_modules=(consumer-gin producer-gin)
  fi

  for module in "${suite_modules[@]}"; do
    local output_file="${RAW_DIR}/${module}-${suite_name}.json"
    local command=(go test -json ./...)
    if [[ -n "${suite_tag}" ]]; then
      command=(go test -json -tags="${suite_tag}" ./...)
    fi

    set +e
    (cd "${ROOT_DIR}/${module}" && GOCACHE="${GOCACHE}" GOMODCACHE="${GOMODCACHE}" "${command[@]}") >"${output_file}"
    local exit_code=$?
    set -e

    if [[ ${exit_code} -ne 0 ]]; then
      run_failed=1
      echo "Suite ${suite_name} failed for ${module}" >&2
    fi
  done
}

sum_suite_metric() {
  local suite_name="$1"
  local jq_filter="$2"
  shift 2
  local selected_modules=("$@")

  local files=()
  local existing_files=()
  for module in "${selected_modules[@]}"; do
    files+=("${RAW_DIR}/${module}-${suite_name}.json")
  done

  for file in "${files[@]}"; do
    if [[ -f "${file}" ]]; then
      existing_files+=("${file}")
    fi
  done

  if [[ ${#existing_files[@]} -eq 0 ]]; then
    echo "0"
    return
  fi

  cat "${existing_files[@]}" | jq -R -s "split(\"\n\") | map(fromjson? | select(. != null)) | (${jq_filter})"
}

write_report() {
  local report_dir="$1"
  local report_label="$2"
  shift 2
  local selected_modules=("$@")
  local summary_file="${report_dir}/summary.json"
  local metrics_file="${report_dir}/test-pyramid.prom"

  mkdir -p "${report_dir}"

  local -a tests_by_kind=(0 0 0 0)
  local -a failures_by_kind=(0 0 0 0)
  local -a skipped_by_kind=(0 0 0 0)
  local -a duration_by_kind=(0 0 0 0)

  for i in "${!suite_order[@]}"; do
    local suite="${suite_order[${i}]}"
    tests_by_kind[${i}]="$(sum_suite_metric "${suite}" '[.[] | select(.Test != null and (.Action == "pass" or .Action == "fail" or .Action == "skip"))] | length' "${selected_modules[@]}")"
    failures_by_kind[${i}]="$(sum_suite_metric "${suite}" '[.[] | select(.Test != null and .Action == "fail")] | length' "${selected_modules[@]}")"
    skipped_by_kind[${i}]="$(sum_suite_metric "${suite}" '[.[] | select(.Test != null and .Action == "skip")] | length' "${selected_modules[@]}")"
    duration_by_kind[${i}]="$(sum_suite_metric "${suite}" '[.[] | select(.Test != null and (.Action == "pass" or .Action == "fail" or .Action == "skip")) | (.Elapsed // 0)] | add // 0' "${selected_modules[@]}")"
  done

  local total_tests=0
  local total_duration=0
  for i in "${!kind_order[@]}"; do
    total_tests=$((total_tests + tests_by_kind[${i}]))
    total_duration=$(
      perl -e '
        my ($a, $b) = @ARGV;
        $a = 0 + ($a // 0);
        $b = 0 + ($b // 0);
        printf "%.6f", $a + $b;
      ' "${total_duration}" "${duration_by_kind[${i}]}"
    )
  done

  if [[ ${total_tests} -eq 0 ]]; then
    echo "No Go tests were collected for ${report_label}." >&2
    return 1
  fi

  {
    echo "{"
    echo "  \"generatedAt\": \"${generated_at}\","
    echo "  \"totals\": {"
    echo "    \"tests\": ${total_tests},"
    echo "    \"durationSeconds\": $(fmt4 "${total_duration}")"
    echo "  },"
    echo "  \"suites\": ["

    for i in "${!kind_order[@]}"; do
      local kind="${kind_order[${i}]}"
      local tests="${tests_by_kind[${i}]}"
      local failures="${failures_by_kind[${i}]}"
      local skipped="${skipped_by_kind[${i}]}"
      local duration="${duration_by_kind[${i}]}"
      local tests_pct
      local duration_pct
      tests_pct="$(percentage "${tests}" "${total_tests}")"
      duration_pct="$(percentage "${duration}" "${total_duration}")"
      local suffix=","
      if [[ "${i}" -eq $((${#kind_order[@]} - 1)) ]]; then
        suffix=""
      fi

      cat <<ROW
    {
      "kind": "${kind}",
      "tests": ${tests},
      "testsPercentage": ${tests_pct},
      "failures": ${failures},
      "skipped": ${skipped},
      "durationSeconds": $(fmt4 "${duration}"),
      "durationPercentage": ${duration_pct}
    }${suffix}
ROW
    done

    echo "  ]"
    echo "}"
  } >"${summary_file}"

  {
    echo "# HELP test_pyramid_tests_count Executed tests grouped by test pyramid kind."
    echo "# TYPE test_pyramid_tests_count gauge"
    for i in "${!kind_order[@]}"; do
      local kind="${kind_order[${i}]}"
      echo "test_pyramid_tests_count{kind=\"${kind}\"} ${tests_by_kind[${i}]}"
    done
    echo

    echo "# HELP test_pyramid_tests_percentage Percentage of tests grouped by test pyramid kind."
    echo "# TYPE test_pyramid_tests_percentage gauge"
    for i in "${!kind_order[@]}"; do
      local kind="${kind_order[${i}]}"
      echo "test_pyramid_tests_percentage{kind=\"${kind}\"} $(percentage "${tests_by_kind[${i}]}" "${total_tests}")"
    done
    echo

    echo "# HELP test_pyramid_duration_seconds Total test execution time grouped by test pyramid kind."
    echo "# TYPE test_pyramid_duration_seconds gauge"
    for i in "${!kind_order[@]}"; do
      local kind="${kind_order[${i}]}"
      echo "test_pyramid_duration_seconds{kind=\"${kind}\"} $(fmt4 "${duration_by_kind[${i}]}")"
    done
    echo

    echo "# HELP test_pyramid_duration_percentage Percentage of execution time grouped by test pyramid kind."
    echo "# TYPE test_pyramid_duration_percentage gauge"
    for i in "${!kind_order[@]}"; do
      local kind="${kind_order[${i}]}"
      echo "test_pyramid_duration_percentage{kind=\"${kind}\"} $(percentage "${duration_by_kind[${i}]}" "${total_duration}")"
    done
    echo

    echo "# HELP test_pyramid_failures_count Failed tests grouped by test pyramid kind."
    echo "# TYPE test_pyramid_failures_count gauge"
    for i in "${!kind_order[@]}"; do
      local kind="${kind_order[${i}]}"
      echo "test_pyramid_failures_count{kind=\"${kind}\"} ${failures_by_kind[${i}]}"
    done
    echo

    echo "# HELP test_pyramid_skipped_count Skipped tests grouped by test pyramid kind."
    echo "# TYPE test_pyramid_skipped_count gauge"
    for i in "${!kind_order[@]}"; do
      local kind="${kind_order[${i}]}"
      echo "test_pyramid_skipped_count{kind=\"${kind}\"} ${skipped_by_kind[${i}]}"
    done
    echo

    echo "# HELP test_pyramid_total_tests Total number of executed tests."
    echo "# TYPE test_pyramid_total_tests gauge"
    echo "test_pyramid_total_tests ${total_tests}"
    echo

    echo "# HELP test_pyramid_total_duration_seconds Total execution time across all tests."
    echo "# TYPE test_pyramid_total_duration_seconds gauge"
    echo "test_pyramid_total_duration_seconds $(fmt4 "${total_duration}")"
    echo

    echo "# HELP test_pyramid_last_update_unix_seconds Unix epoch time for latest metrics generation."
    echo "# TYPE test_pyramid_last_update_unix_seconds gauge"
    echo "test_pyramid_last_update_unix_seconds ${generated_at_unix}"
  } >"${metrics_file}"

  echo "Wrote gin test pyramid summary to ${summary_file}"
  echo "Wrote gin test pyramid metrics to ${metrics_file}"
}

if [[ "${SKIP_TEST_EXECUTION}" != "true" ]]; then
  run_suite "test" ""
  run_suite "integrationTest" "integration"

  PACT_CLI_BIN_DIR="${ROOT_DIR}/scripts/gin/ensure-pact-cli.sh"
  PACT_CLI_BIN_DIR="$("${PACT_CLI_BIN_DIR}")"
  export PATH="${PACT_CLI_BIN_DIR}:${PATH}"
  run_suite "contractTest" "contract"

  if [[ "${INCLUDE_E2E}" == "true" || "${RUN_E2E:-false}" == "true" ]]; then
    run_suite "e2eTest" "e2e"
  fi
fi

generated_at="$(date -u +%Y-%m-%dT%H:%M:%SZ)"
generated_at_unix="$(date -u +%s)"

write_report "${STACK_REPORT_DIR}" "gin stack" "${modules[@]}"

for module in "${modules[@]}"; do
  write_report "${REPORT_ROOT}/${module}" "${module}" "${module}"
done

if [[ ${run_failed} -ne 0 ]]; then
  echo "One or more gin suites failed while collecting metrics." >&2
  exit 1
fi
