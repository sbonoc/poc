# Gin Stack Tooling

This directory contains automation that applies to the **Gin stack as a whole** (`producer-gin` + `consumer-gin`).

## Why these scripts live here

- They orchestrate both services together (not one service only).
- They are stack-specific (not global platform scripts).
- Keeping them in one stack folder avoids coupling either service directory to shared orchestration concerns.

## Scripts

- `run-suite.sh`  
  Runs Gin test suites (`unit`, `integration`, `contract`, `e2e`) across both services.

- `quality-check.sh`
  Runs Gin quality gates comparable to JVM stack checks: `gofmt` validation, `go vet`, and unit tests across both services.

- `collect-test-pyramid.sh`  
  Collects Gin test-pyramid metrics and writes stack/service reports under `build/reports/test-pyramid`.

- `ensure-pact-cli.sh`  
  Ensures required Pact CLI tools are available for Gin contract tests using a pinned, checksum-verified installer script.

## Placement rules

- Put scripts here when they target **both** `producer-gin` and `consumer-gin`.
- Put scripts inside `producer-gin/` or `consumer-gin/` only when they are strictly service-local.
- Create `common-gin` only for shared runtime/library code (Go packages), not for orchestration scripts.
