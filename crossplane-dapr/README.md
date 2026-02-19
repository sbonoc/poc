# crossplane-dapr

Cloud-agnostic, event-driven microservices PoC using:
- `Ktor` for `producer` and `consumer`
- `Dapr` for publish/subscribe abstraction
- `Crossplane` for infrastructure provisioning
- `OpenTelemetry + Tempo + Prometheus + Grafana` for observability

The project is structured and hardened as a production-style codebase while staying runnable locally.

## Architecture

`producer` sends `OrderCreatedV1` events to Dapr pub/sub component `order-pubsub`.

`consumer` subscribes via `/dapr/subscribe` and consumes events through `/orders`.

`Crossplane` provisions the underlying Pub/Sub topic from a high-level `MessageBus` claim.

`Dapr` routes events to local emulator (local overlay) or real cloud service (prod overlay) without changing app code.

## Modules

- `common`: shared contracts (versioned events, CloudEvent envelope, serialization helpers)
- `producer`: HTTP API for event publication, Dapr publisher adapter
- `consumer`: Dapr subscription endpoint and event processing pipeline

## Repository Layout

- `infra/base`: reusable manifests (Crossplane API/composition, Dapr runtime, apps, observability)
- `infra/overlays/local`: local environment (emulator + local provider config + local patches)
- `infra/overlays/prod`: production-oriented overlay templates
- `Makefile`: repeatable local ops entry points

## Local Prerequisites

- Docker
- Kubernetes cluster (`kubectl` configured)
- Helm
- Dapr CLI

## Run Locally

```bash
make deploy-local
```

Optional namespace override:

```bash
make deploy-local APP_NAMESPACE=agnostic-local
```

Useful local access:

```bash
make port-forward-local
```

Send test traffic:

```bash
./burst-test.sh
```

Tear down:

```bash
make teardown-local
```

Optional heavy cleanup (control planes):

```bash
make destroy-control-planes
```

## Shift-Left Test Strategy

Test pyramid is codified in Gradle source sets/tasks:

- `test`: unit tests (most)
- `integrationTest`: component integration tests (fewer)
- `contractTest`: contract verification (Pact)
- `e2eTest`: smoke tests only (least)

Run suites:

```bash
make unit-test
make integration-test
make contract-test
make e2e-test
```

Quality gates:

```bash
make quality
make verify
```

## Pact Contract

Consumer-driven contract flow:

1. `consumer:contractTest` generates the pact file using Pact JUnit5.
2. Generated pact is written to and committed from:
   - `consumer/pacts/order-event-consumer-order-event-producer.json`
3. `producer:contractTest` verifies provider messages against that generated pact.

Provider verification uses Pact folder loading from `consumer/pacts`.

## Observability

Local overlay deploys:

- OpenTelemetry Collector (`otel-collector`)
- Tempo (`tempo`)
- Prometheus (`prometheus`)
- Grafana (`grafana`)

Apps export telemetry with OTLP env vars and expose `/metrics` for scrape/debug.

## Production Path

Use `infra/overlays/prod` as baseline:

1. Configure real image registry names/tags.
2. Provide real GCP credentials secret for Crossplane `ProviderConfig`.
3. Set real GCP project ID in prod patches.
4. Create `grafana-admin-credentials` secret (keys: `username`, `password`) in your target namespace.
5. Keep app code unchanged.

## Local JVM Mode (Optional)

When you want to run apps directly on JVM instead of Kubernetes:

1. expose an OTLP endpoint at `localhost:4317` (port-forward from cluster or run local collector)
2. run services:

```bash
make run-producer-local
make run-consumer-local
```
