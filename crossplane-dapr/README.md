# 🚀 Cloud-Agnostic Event-Driven Microservices Stack

Welcome to a fully local and prod-like, cloud-agnostic microservices environment.
This project demonstrates how to build highly decoupled, event-driven applications using **Kotlin (Ktor)**, **Go (Gin)**, and **Java (Spring Boot)** with **Dapr** and **Crossplane** on Kubernetes, plus first-class **OpenTelemetry observability** and a **Shift-Left testing strategy**.

## 🌟 The Core Philosophy: Why is this stack powerful?

Traditional microservices often couple application code directly to cloud SDKs. That creates migration friction, slows down developer autonomy, and makes local validation expensive.

This stack addresses those problems:

1. **Zero app-level vendor lock-in:** producer/consumer implementations do not call Google Cloud SDKs directly. They talk to local Dapr sidecars over HTTP, and Dapr handles broker-specific details.
2. **Infrastructure as software:** Crossplane runs inside Kubernetes and reconciles cloud resources from Kubernetes manifests.
3. **Developer self-service:** App teams request a high-level `MessageBus` claim and platform composition translates it into concrete managed resources.
4. **Free local development:** Local mode uses a Pub/Sub emulator, so full event flow can run without real cloud credentials.
5. **Production-grade feedback loops:** You get traces, metrics, dashboards, contract tests, and quality gates from day one.

---

## 🏗️ Architecture Breakdown

### 1. Applications (Ktor, Gin, Spring Boot pairs)

- **Purpose:** business logic only.
- `producer-ktor`, `producer-gin`, and `producer-springboot` expose `POST /publish` and emit `OrderCreatedV1`.
- `consumer-ktor`, `consumer-gin`, and `consumer-springboot` expose `GET /dapr/subscribe` and receive events on `/orders`.

### 2. Dapr (Distributed Application Runtime)

- **Purpose:** sidecar abstraction for pub/sub.
- Producer apps send publish calls to Dapr, and Dapr maps them to the configured component.
- Consumer apps declare subscriptions via `/dapr/subscribe`; Dapr pushes matching messages to each route.

### 3. Crossplane

- **Purpose:** Kubernetes-native control plane for infrastructure.
- An app-facing `MessageBus` claim is reconciled into a GCP Pub/Sub `Topic` managed resource using composition.

### 4. GCP Pub/Sub Emulator (local overlay)

- **Purpose:** local broker behavior without cloud spend.
- Crossplane and Dapr are configured to point to emulator endpoints in local mode.

### 5. Observability Stack

- **OpenTelemetry Collector** receives OTLP telemetry from apps.
- **Tempo** stores traces.
- **Prometheus** scrapes app and collector metrics.
- **Loki** stores logs.
- **Promtail** collects Kubernetes container logs and enriches them with pod labels/metadata.
- **Grafana** visualizes dashboards (including Golden Signals).

```text
+------------------------------------------------------------------------------+
|                              KUBERNETES CLUSTER                              |
|                                                                              |
| [APP NAMESPACE]                                                              |
| producer-* (ktor/gin/springboot): POST /publish                              |
| consumer-* (ktor/gin/springboot): GET /dapr/subscribe, POST /orders          |
|                                                                              |
| producer app -> daprd -> order-pubsub(topic=orders) -> daprd -> consumer app |
|                                                                              |
| local backend: gcp-emulator.crossplane-system.svc.cluster.local:8085         |
| prod backend:  GCP Pub/Sub                                                   |
|                                                                              |
| [OBSERVABILITY]                                                              |
| apps + daprd -> OTLP -> otel-collector -> tempo / prometheus -> grafana      |
| kubernetes logs -> promtail -> loki -> grafana                               |
|                                                                              |
| [CROSSPLANE-SYSTEM]                                                          |
| MessageBus claim -> XRD/Composition -> managed Topic                         |
+------------------------------------------------------------------------------+
```

---

## 🚀 Getting Started

### Prerequisites

- Docker Desktop / OrbStack / Colima
- Local Kubernetes cluster
- `kubectl`
- `helm`
- Dapr CLI (`dapr`)
- Java 21 (for local Gradle execution and Spring Boot services)
- Go 1.23+ (if you want to run Gin services outside Docker)

### Service matrix

- `producer-ktor` / `consumer-ktor`: Kotlin + Ktor (fully integrated with current Gradle test pyramid and Pact setup)
- `producer-gin` / `consumer-gin`: Go + Gin
- `producer-springboot` / `consumer-springboot`: Java + Spring Boot (built with the same root `./gradlew` wrapper)

### Shared code strategy

- `common-ktor` contains Kotlin-only shared code (event models, serialization helpers, OTel factory) used by Ktor modules.
- Best-practice for cross-language sharing is to keep contracts language-neutral (for example AsyncAPI/JSON Schema/Proto in a dedicated `contracts/` folder) instead of sharing runtime libraries across stacks.

### Automation layout by stack

- JVM/Kotlin and Spring Boot test orchestration is implemented as Gradle tasks (`./gradlew ...`).
- Gin test orchestration is isolated under `scripts/gin/`:
  - `scripts/gin/run-suite.sh`
  - `scripts/gin/quality-check.sh`
  - `scripts/gin/collect-test-pyramid.sh`
  - `scripts/gin/ensure-pact-cli.sh`
  - `scripts/gin/README.md` documents ownership and placement rules for Gin tooling.
- `scripts/test-pyramid-targets.sh` is the single source of truth for pyramid stack/service targets consumed by Makefile, CI, and push scripts.
- Cross-stack orchestration stays at root-level scripts (for example `scripts/smoke-test-all.sh`, `scripts/push-test-pyramid-metrics.sh`).

### 1. Discover available commands

```bash
make help
./deploy.sh --help
./teardown.sh --help
```

### 2. Deploy local environment

```bash
./deploy.sh
```

Equivalent:

```bash
make deploy-local
```

Optional namespace override:

```bash
APP_NAMESPACE=agnostic-local make deploy-local
```

`make deploy-local` now renders manifests to `build/rendered/local/infra` using `env/local.env` defaults plus any shell overrides (for example `APP_NAMESPACE=agnostic-local`).
This keeps deployment declarative and avoids imperative `kubectl patch` steps.

You can render manifests explicitly:

```bash
make render-local
make render-prod
```

Promtail stays cluster-wide and the ServiceAccount namespace in `ClusterRoleBinding` is rendered from `${APP_NAMESPACE}`.

### 3. Expose all services, Loki, and Grafana locally

```bash
make port-forward-local
```

This exposes all app services simultaneously with different local ports:

```bash
producer-ktor       -> http://localhost:8080
consumer-ktor       -> http://localhost:8081
producer-gin        -> http://localhost:8082
consumer-gin        -> http://localhost:8083
producer-springboot -> http://localhost:8084
consumer-springboot -> http://localhost:8085
loki                -> http://localhost:3100
grafana             -> http://localhost:3000
```

Or deploy and port-forward in one step:

```bash
./deploy.sh --port-forward
```

### 4. Drive traffic and verify event flow

Open one terminal and follow consumer logs (example for all three consumers):

```bash
kubectl logs -f -l app=consumer-ktor -c consumer-ktor
kubectl logs -f -l app=consumer-gin -c consumer-gin
kubectl logs -f -l app=consumer-springboot -c consumer-springboot
```

In another terminal:

```bash
make smoke-test
```

`make smoke-test` runs burst traffic against all three producers (`producer-ktor`, `producer-gin`, `producer-springboot`). If the producer endpoints are not already exposed on localhost, it opens temporary port-forwards automatically.

### 5. Open Grafana and inspect metrics/traces/logs

- URL: `http://localhost:3000`
- Local overlay enables anonymous admin for convenience.
- Dashboards are provisioned automatically:
  - `Crossplane Dapr - Service Overview`
  - `Crossplane Dapr - Event Flow`
  - `Crossplane Dapr - Golden Signals`
  - `Crossplane Dapr - Test Pyramid`
- Service dashboards include a `Service` variable at the top, so you can filter dynamically by jobs such as `producer-ktor`, `consumer-ktor`, `producer-gin`, `consumer-gin`, `producer-springboot`, and `consumer-springboot`.

To find traces in Tempo:

1. Open Grafana `Explore`.
2. Select `Tempo` datasource.
3. Query `{ resource.service.name="producer-ktor" }` or `{ resource.service.name="consumer-ktor" }` (Ktor traces are enabled by default).
4. If empty, generate traffic with `make smoke-test` and query last 30 minutes.

To inspect logs in Loki:

1. Open Grafana `Explore`.
2. Select `Loki` datasource.
3. Query by labels, for example: `{service="producer-gin"}` or `{service=~"consumer-.*", stack="springboot"}`.

For trace correlation from logs, Gin and Spring Boot request logs include:
- `trace_id`
- `span_id`
- `http_method`
- `http_path`

Low-cardinality context (`service`, `stack`, `role`, `namespace`, `env`) is added by Promtail from Kubernetes metadata/labels. High-cardinality values (`trace_id`, `span_id`, `orderId`) stay in log payload for query-time filtering.

Kubernetes contextual values are injected in existing Deployment manifests (`infra/base/apps/*.yaml`) via Downward API env vars (`POD_NAMESPACE`, `POD_NAME`, `NODE_NAME`) and app identity vars (`APP_SERVICE`, `APP_STACK`, `APP_ROLE`, `APP_VERSION`).  
`DEPLOYMENT_ENV` is derived from pod label `app.kubernetes.io/environment`, which overlays set per environment (`local`/`prod`) with `includeTemplates: true`.

### 6. Teardown

```bash
./teardown.sh
```

Optional full cleanup (remove control planes too):

```bash
./teardown.sh --all
```

---

## 🧠 Deep Dive: Configuration Files

### 🏗️ Crossplane Infrastructure Files

- **`infra/base/crossplane/xrd-messagebus.yaml`**
  Defines the platform API (`MessageBus` claim / `XMessageBus` composite) with parameters such as `topicName`, optional `projectId`, and `providerConfigName`.

- **`infra/base/crossplane/composition-messagebus.yaml`**
  Composition pipeline that maps a `MessageBus` claim into a concrete `pubsub.gcp.upbound.io/v1beta1 Topic` managed resource.

- **`infra/base/crossplane/provider.yaml`** and **`infra/base/crossplane/function-patch-and-transform.yaml`**
  Install shared Crossplane provider/function packages for both local and prod overlays.

- **`infra/overlays/local/crossplane-runtime-config.yaml`**
  Defines the runtime configuration used in local mode (`PUBSUB_EMULATOR_HOST`).

- **`infra/overlays/local/crossplane/kustomization.yaml`** and **`infra/overlays/local/crossplane/provider-runtime-config-patch.yaml`**
  Declaratively attach `runtimeConfigRef=gcp-emulator-config` to `provider-gcp-pubsub` (no imperative `kubectl patch`).

- **`env/local.env`**, **`env/prod.env`**, and **`scripts/render-overlay.sh`**
  Environment-parameterized render layer used before apply. It substitutes `${APP_NAMESPACE}`, `${GCP_PROJECT_ID}`, `${PROVIDER_CONFIG_NAME}`, and `${GCP_SECRET_NAME}` into rendered manifests under `build/rendered/<env>/infra`.

- **`infra/overlays/common/kustomization.yaml`**
  Shared overlay layer consumed by local and prod overlays (`ProviderConfig` + `MessageBus` claim).

- **`infra/overlays/common/provider-config.yaml`** and **`infra/overlays/common/bus-claim.yaml`**
  Shared Crossplane runtime resources rendered per environment from `env/*.env` (`ProviderConfig` + developer-facing `MessageBus` claim).

- **`scripts/create-local-gcp-emulator-secret.sh`**
  Generates emulator-only service account JSON and creates the secret in `crossplane-system`.

### 🛜 Dapr Runtime Files

- **`infra/base/dapr/dapr-config.yaml`**
  Dapr configuration with tracing enabled and OTLP export to collector (`otel-collector:4317`).

- **`infra/base/dapr/dapr-pubsub.yaml`**
  Base `order-pubsub` component (GCP pub/sub type) where `projectId` is rendered from `${GCP_PROJECT_ID}`.

- **`infra/overlays/local/dapr-pubsub-endpoint-patch.yaml`**
  Local-only patch that adds emulator endpoint metadata.

### 🧩 Application Deployment Files

- **`infra/base/apps/producer-ktor.yaml`**, **`infra/base/apps/producer-gin.yaml`**, **`infra/base/apps/producer-springboot.yaml`**
  Producer deployments/services for each stack with Dapr sidecar annotations, OTEL env vars, probes, and resource limits.

- **`infra/base/apps/consumer-ktor.yaml`**, **`infra/base/apps/consumer-gin.yaml`**, **`infra/base/apps/consumer-springboot.yaml`**
  Consumer deployments/services with Dapr annotations and subscription route config through app env vars.

- **`infra/base/apps/common-deployment-hardening-patch.yaml`**
  Shared hardening patch applied to all app Deployments (pod/container security context, read-only root filesystem, and `/tmp` emptyDir mount).

- **`producer-ktor/src/main/kotlin/com/agnostic/producer/routes/ProducerRoutes.kt`**
  `POST /publish` endpoint, request validation, and publish path instrumentation/logging.

- **`consumer-ktor/src/main/kotlin/com/agnostic/consumer/routes/ConsumerRoutes.kt`**
  `GET /dapr/subscribe` and `POST /orders` handlers with parsing, processing, metrics, and logs.

### 📈 Observability Files

- **`infra/base/observability/otel-collector-config.yaml`**
  OTLP receiver, trace export to Tempo, metric export for Prometheus scrape.

- **`infra/base/observability/tempo-config.yaml`**
  Tempo local storage and OTLP receiver config.

- **`infra/base/observability/loki-config.yaml`** and **`infra/base/observability/loki.yaml`**
  Loki single-binary deployment/service for centralized log storage.

- **`infra/base/observability/promtail-config.yaml`** and **`infra/base/observability/promtail.yaml`**
  Promtail daemonset + RBAC that tails Kubernetes container logs and adds labels such as `service`, `stack`, `role`, `namespace`, and `env`.

- **`infra/base/observability/prometheus-config.yaml`**
  Scrape jobs for `otel-collector`, all producer/consumer implementations, and `pushgateway` (for test metrics).

- **`infra/base/observability/pushgateway.yaml`**
  Pushgateway deployment/service used to ingest both latest-run and historical test-pyramid metrics from local and CI.

- **`infra/base/observability/grafana-datasources.yaml`**
  Provisioned datasources for Prometheus, Tempo, and Loki, including trace-id derived fields from logs to Tempo.

- **`infra/base/observability/grafana-dashboards.yaml`**
  Provisioned dashboards including Golden Signals (traffic, latency, errors, saturation) for both services.

### 🤝 Contract Testing (Pact)

- **`consumer-ktor/src/contractTest/kotlin/com/agnostic/consumer/contract/ConsumerPactContractTest.kt`**
  Consumer test defines expected async message contract and generates pact file.

- **`consumer-ktor/pacts/order-event-consumer-order-event-producer.json`**
  Generated pact artifact, committed to source control.

- **`producer-ktor/src/contractTest/kotlin/com/agnostic/producer/contract/ProducerPactVerificationTest.kt`**
  Provider verification test loads pact from `../consumer-ktor/pacts` and validates produced message contract.

- **`consumer-springboot/src/contractTest/java/com/agnostic/consumerspringboot/ConsumerPactContractTest.java`**
  Pact JVM consumer test generates message contract for the Spring Boot pair.

- **`producer-springboot/src/contractTest/java/com/agnostic/producerspringboot/ProducerPactVerificationTest.java`**
  Pact JVM provider verification loads pacts from `../consumer-springboot/pacts`.

- **`consumer-gin/internal/consumer/contract_test.go`** and **`producer-gin/internal/producer/contract_test.go`**
  Pact-Go consumer/provider verification for the Gin pair, with automatic Pact CLI bootstrap via `scripts/gin/ensure-pact-cli.sh`.

---

## 🧪 Shift-Left Test Strategy

The test pyramid is codified as separate suites per stack (Gradle for Ktor/Spring Boot, `scripts/gin/run-suite.sh` for Gin):

- Unit tests first (`test`) and most numerous.
- Fewer integration tests (`integrationTest`).
- Contract tests (`contractTest`) for service boundaries.
- Minimal E2E smoke (`e2eTest`, opt-in).

Useful commands:

```bash
make unit-test
make integration-test
make contract-test
make e2e-test
make test-pyramid-metrics
make push-test-pyramid-metrics
make push-test-pyramid-history
make quality
make verify
```

- `make quality` runs JVM static/coverage checks plus Gin `gofmt` validation, `go vet`, and unit tests.
- `make verify` runs full JVM verification plus Gin quality checks, integration, and contract suites.

Publish test metrics to Grafana:

```bash
make push-test-pyramid-metrics
```

`make push-test-pyramid-metrics` waits for Pushgateway, opens a temporary port-forward, runs unit/integration/contract suites, and generates segregated reports by stack and by service:
- `build/reports/test-pyramid/ktor/summary.json`
- `build/reports/test-pyramid/ktor/test-pyramid.prom`
- `build/reports/test-pyramid/springboot/summary.json`
- `build/reports/test-pyramid/springboot/test-pyramid.prom`
- `build/reports/test-pyramid/gin/summary.json`
- `build/reports/test-pyramid/gin/test-pyramid.prom`
- `build/reports/test-pyramid/producer-ktor/summary.json`
- `build/reports/test-pyramid/producer-ktor/test-pyramid.prom`
- `build/reports/test-pyramid/consumer-ktor/summary.json`
- `build/reports/test-pyramid/consumer-ktor/test-pyramid.prom`
- `build/reports/test-pyramid/producer-springboot/summary.json`
- `build/reports/test-pyramid/producer-springboot/test-pyramid.prom`
- `build/reports/test-pyramid/consumer-springboot/summary.json`
- `build/reports/test-pyramid/consumer-springboot/test-pyramid.prom`
- `build/reports/test-pyramid/producer-gin/summary.json`
- `build/reports/test-pyramid/producer-gin/test-pyramid.prom`
- `build/reports/test-pyramid/consumer-gin/summary.json`
- `build/reports/test-pyramid/consumer-gin/test-pyramid.prom`

Then it pushes those metrics to Pushgateway with segregated jobs:
- `job="test-pyramid-latest-ktor", instance="latest"`
- `job="test-pyramid-latest-springboot", instance="latest"`
- `job="test-pyramid-latest-gin", instance="latest"`
- `job="test-pyramid-latest-producer-ktor|consumer-ktor|producer-springboot|consumer-springboot|producer-gin|consumer-gin", instance="latest"`
- Optional per-run snapshots (`PUSH_HISTORY=true`) are pushed to `test-pyramid-history-<target>` with `instance="<run-id>"`.

The `Crossplane Dapr - Test Pyramid` dashboard shows:
- A required single `Service` selector (no `All` option), so each service pyramid is visualized independently (for example `producer-gin` or `consumer-ktor`).
- Absolute test count by kind.
- Percentage split by kind.
- Absolute and percentage execution time by kind.
- Total test count and total execution time.
- Quality gates (green/red):
  - Pyramid ratio: `unit > (integration + contract) > e2e`
  - Total execution time: `< 15 minutes`
  - Overall gate: ratio and time gate combined
- Time-series trends over time (stacked by test kind) for test count and execution time for the selected service.

History snapshot mode:

```bash
make push-test-pyramid-history
```

- This pushes additional per-run snapshots for all stacks and services (`job="test-pyramid-history-<target>", instance="<run-id>"`).
- Optional custom run id:

```bash
RUN_ID=my-release-2026-02-20 make push-test-pyramid-history
```

CI publishing:

- In GitHub Actions, set repository secret `PUSHGATEWAY_URL` to enable publishing stack and service test-pyramid metrics from `../.github/workflows/crossplane-dapr.yml` (repository root).
- CI publishes latest metrics only (`test-pyramid-latest-<stack-or-service>`).
- GitHub Actions step summary and uploaded artifacts are split into two sections: by stack and by service.
- If the secret is not set, CI still runs all tests, but metrics are not pushed.

Pact workflow:

```bash
make pact-regenerate
```

`pact-regenerate` executes provider contract verification and re-runs consumer pact generation as a dependency, keeping producer validation tied to the latest generated contract.
It validates CDC independently per pair:
- `consumer-ktor` -> `producer-ktor` (`consumer-ktor/pacts/*.json`)
- `consumer-springboot` -> `producer-springboot` (`consumer-springboot/pacts/*.json`)
- `consumer-gin` -> `producer-gin` (`consumer-gin/pacts/*.json`)

CI pipeline is in **`../.github/workflows/crossplane-dapr.yml`** (repository root), enforcing Shift-Left gates (unit, static checks, integration, contract, coverage) with E2E smoke only on manual dispatch.

---

## 🌍 Path to Production: Moving to Real GCP

The application code remains unchanged. Migration is mostly manifest and secret changes.

### 1. Application code

- **No changes required** in service implementations (`producer-*`, `consumer-*`) or shared contracts.

### 2. Crossplane configuration

- Set `GCP_PROJECT_ID`, `PROVIDER_CONFIG_NAME`, and `GCP_SECRET_NAME` in `env/prod.env` (used to render shared `ProviderConfig`/claim manifests).
- Create GCP credentials secret in `crossplane-system`.

```bash
kubectl create secret generic gcp-secret -n crossplane-system \
  --from-file=creds=./gcp-credentials.json
```

### 3. Dapr pub/sub configuration

- `projectId` is injected into `infra/base/dapr/dapr-pubsub.yaml` during render from `env/prod.env`.
- Ensure workload identity or equivalent auth strategy is configured in your target cluster.

### 4. Grafana security

- Create admin credentials secret expected by `infra/overlays/prod/grafana-security-patch.yaml`.

```bash
kubectl create secret generic grafana-admin-credentials -n <app-namespace> \
  --from-literal=username=admin \
  --from-literal=password='change-me'
```

### 5. Container images

- Update `infra/overlays/prod/kustomization.yaml` image overrides to your real registry and immutable tags.

---

## ☎️ So What?! Tradeoffs to Be Aware Of

### 1. Feature parity tradeoff

Dapr intentionally exposes portable primitives. Some cloud-specific broker capabilities may be harder to use or unavailable through the generic API surface.

### 2. Extra network hop

Path is `App -> Dapr sidecar -> Broker`. For most microservices this is acceptable, but ultra-low-latency workloads may prefer direct SDK access.

### 3. Delivery semantics and tuning

Dapr pushes to app routes and your service must handle throughput safely. Tune Dapr component metadata and app server concurrency as load grows.

### 4. Event envelope interoperability

CloudEvents are great for standardization, but mixed ecosystems sometimes require raw payload handling and explicit parser behavior.

### 5. Operational overhead

You operate app + sidecar + control planes. That raises observability and platform requirements, but this repository already includes those foundations.

---

## ✅ Quick Checklist

For a healthy local run:

1. `make deploy-local`
2. `make port-forward-local`
3. `make smoke-test`
4. Check consumer logs (for example `consumer-ktor`) for `Consumed order event`
5. `make push-test-pyramid-metrics`
6. Open Grafana at `http://localhost:3000` and inspect Golden Signals + Tempo traces + Test Pyramid
7. `./teardown.sh` when done
