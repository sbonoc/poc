# 🚀 Cloud-Agnostic Event-Driven Microservices Stack

Welcome to a fully local and prod-like, cloud-agnostic microservices environment.
This project demonstrates how to build highly decoupled, event-driven applications using **Kotlin (Ktor)**, **Dapr**, and **Crossplane** on Kubernetes, with first-class **OpenTelemetry observability** and a **Shift-Left testing strategy**.

## 🌟 The Core Philosophy: Why is this stack powerful?

Traditional microservices often couple application code directly to cloud SDKs. That creates migration friction, slows down developer autonomy, and makes local validation expensive.

This stack addresses those problems:

1. **Zero app-level vendor lock-in:** `producer` and `consumer` do not call Google Cloud SDKs directly. They talk to local Dapr sidecars over HTTP, and Dapr handles broker-specific details.
2. **Infrastructure as software:** Crossplane runs inside Kubernetes and reconciles cloud resources from Kubernetes manifests.
3. **Developer self-service:** App teams request a high-level `MessageBus` claim and platform composition translates it into concrete managed resources.
4. **Free local development:** Local mode uses a Pub/Sub emulator, so full event flow can run without real cloud credentials.
5. **Production-grade feedback loops:** You get traces, metrics, dashboards, contract tests, and quality gates from day one.

---

## 🏗️ Architecture Breakdown

### 1. Applications (`producer` and `consumer`)

- **Purpose:** business logic only.
- `producer` exposes `POST /publish` and emits `OrderCreatedV1`.
- `consumer` exposes `GET /dapr/subscribe` and receives events on `/orders`.

### 2. Dapr (Distributed Application Runtime)

- **Purpose:** sidecar abstraction for pub/sub.
- `producer` sends a publish call to Dapr, Dapr maps it to the configured component.
- `consumer` declares subscriptions via `/dapr/subscribe`; Dapr pushes matching messages to the route.

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
- **Grafana** visualizes dashboards (including Golden Signals).

```text
+--------------------------------------------------------------------------+
|                        KUBERNETES CLUSTER                                |
|                                                                          |
| [APP NAMESPACE]                                                          |
| producer Pod                     consumer Pod                            |
| +---------------------------+    +---------------------------+           |
| | Ktor producer             |    | Ktor consumer             |           |
| | POST /publish             |    | GET /dapr/subscribe       |           |
| +------------+--------------+    | POST /orders              |           |
|              | localhost:3500    +------------^--------------+           |
| +------------v--------------+                 | localhost:3500            |
| | daprd (producer)          |                 |                           |
| +------------+--------------+    +------------+--------------+           |
|              | publish evt       | daprd (consumer)          |           |
|              +------------------>| subscribe + push          |           |
|                                  +------------^--------------+           |
|                                               |                          |
|            +----------------------------------+------------------------+ |
|            | Dapr component: order-pubsub (topic=orders)             | |
|            +----------------------------------+------------------------+ |
|                                               |                          |
|                                               v                          |
|     local: gcp-emulator...:8085  |  prod: GCP Pub/Sub                  |
|                                                                          |
| [OBSERVABILITY]                                                          |
| apps + daprd -> OTLP -> otel-collector -> tempo                         |
|                                 -> prometheus -> grafana                |
|                                                                          |
| [CROSSPLANE-SYSTEM]                                                      |
| MessageBus claim -> XRD/Composition -> managed Topic                    |
+--------------------------------------------------------------------------+
```

---

## 🚀 Getting Started

### Prerequisites

- Docker Desktop / OrbStack / Colima
- Local Kubernetes cluster
- `kubectl`
- `helm`
- Dapr CLI (`dapr`)
- Java 21 (for local Gradle execution)

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

### 3. Expose producer and Grafana locally

```bash
make port-forward-local
```

Or deploy and port-forward in one step:

```bash
./deploy.sh --port-forward
```

### 4. Drive traffic and verify event flow

Open one terminal and follow consumer logs:

```bash
kubectl logs -f -l app=consumer -c consumer
```

In another terminal:

```bash
make smoke-test
```

### 5. Open Grafana and inspect metrics/traces

- URL: `http://localhost:3000`
- Local overlay enables anonymous admin for convenience.
- Dashboards are provisioned automatically:
  - `Crossplane Dapr - Service Overview`
  - `Crossplane Dapr - Event Flow`
  - `Crossplane Dapr - Golden Signals`

To find traces in Tempo:

1. Open Grafana `Explore`.
2. Select `Tempo` datasource.
3. Query `{ resource.service.name="producer" }` or `{ resource.service.name="consumer" }`.
4. If empty, generate traffic with `make smoke-test` and query last 30 minutes.

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

- **`infra/overlays/local/crossplane-runtime-config.yaml`**
  Injects `PUBSUB_EMULATOR_HOST` into the Crossplane provider runtime so reconciliation targets the emulator.

- **`infra/overlays/local/crossplane-provider.yaml`**
  Installs the GCP Pub/Sub provider package with the local runtime config.

- **`infra/overlays/local/provider-config.yaml`**
  Local `ProviderConfig` that points to emulator credentials secret.

- **`infra/overlays/local/bus-claim.yaml`**
  Developer-facing claim (`orders-bus`) requesting topic `orders`.

- **`scripts/create-local-gcp-emulator-secret.sh`**
  Generates emulator-only service account JSON and creates the secret in `crossplane-system`.

### 🛜 Dapr Runtime Files

- **`infra/base/runtime/dapr-config.yaml`**
  Dapr configuration with tracing enabled and OTLP export to collector (`otel-collector:4317`).

- **`infra/base/runtime/dapr-pubsub.yaml`**
  Base `order-pubsub` component (GCP pub/sub type).

- **`infra/overlays/local/dapr-pubsub-patch.yaml`**
  Local patch sets `projectId=local-project` and emulator endpoint.

- **`infra/overlays/prod/dapr-pubsub-patch.yaml`**
  Production patch sets real project id and removes emulator dependency.

### 🧩 Application Deployment Files

- **`infra/base/apps/producer.yaml`**
  Producer deployment/service with Dapr sidecar annotations, OTEL env vars, probes, security context, and resource limits.

- **`infra/base/apps/consumer.yaml`**
  Consumer deployment/service with Dapr annotations and subscription route config through app env vars.

- **`producer/src/main/kotlin/com/agnostic/producer/routes/ProducerRoutes.kt`**
  `POST /publish` endpoint, request validation, and publish path instrumentation/logging.

- **`consumer/src/main/kotlin/com/agnostic/consumer/routes/ConsumerRoutes.kt`**
  `GET /dapr/subscribe` and `POST /orders` handlers with parsing, processing, metrics, and logs.

### 📈 Observability Files

- **`infra/base/observability/otel-collector-config.yaml`**
  OTLP receiver, trace export to Tempo, metric export for Prometheus scrape.

- **`infra/base/observability/tempo-config.yaml`**
  Tempo local storage and OTLP receiver config.

- **`infra/base/observability/prometheus-config.yaml`**
  Scrape jobs for `otel-collector`, `producer`, and `consumer`.

- **`infra/base/observability/grafana-datasources.yaml`**
  Provisioned datasources for Prometheus and Tempo.

- **`infra/base/observability/grafana-dashboards.yaml`**
  Provisioned dashboards including Golden Signals (traffic, latency, errors, saturation) for both services.

### 🤝 Contract Testing (Pact)

- **`consumer/src/contractTest/kotlin/com/agnostic/consumer/contract/ConsumerPactContractTest.kt`**
  Consumer test defines expected async message contract and generates pact file.

- **`consumer/pacts/order-event-consumer-order-event-producer.json`**
  Generated pact artifact, committed to source control.

- **`producer/src/contractTest/kotlin/com/agnostic/producer/contract/ProducerPactVerificationTest.kt`**
  Provider verification test loads pact from `../consumer/pacts` and validates produced message contract.

---

## 🧪 Shift-Left Test Strategy

The test pyramid is codified as separate Gradle suites in each module:

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
make quality
make verify
```

Pact workflow:

```bash
make pact-regenerate
```

`pact-regenerate` executes provider contract verification and re-runs consumer pact generation as a dependency, keeping producer validation tied to the latest generated contract.

CI pipeline is in **`.github/workflows/ci.yml`**, enforcing Shift-Left gates (unit, static checks, integration, contract, coverage) with E2E smoke only on manual dispatch.

---

## 🌍 Path to Production: Moving to Real GCP

The application code remains unchanged. Migration is mostly manifest and secret changes.

### 1. Application code

- **No changes required** in `producer`, `consumer`, or shared contracts.

### 2. Crossplane configuration

- Use `infra/overlays/prod/provider-config.yaml` with real project id.
- Create GCP credentials secret in `crossplane-system`.

```bash
kubectl create secret generic gcp-secret -n crossplane-system \
  --from-file=creds=./gcp-credentials.json
```

### 3. Dapr pub/sub configuration

- Use `infra/overlays/prod/dapr-pubsub-patch.yaml` to point component metadata to real project id.
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
4. Check consumer logs for `Consumed order event`
5. Open Grafana at `http://localhost:3000` and inspect Golden Signals + Tempo traces
6. `./teardown.sh` when done
