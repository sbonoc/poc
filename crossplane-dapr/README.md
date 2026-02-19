# 🚀 Cloud-Agnostic Event-Driven Microservices Stack

Welcome to a fully local and prod-like, cloud-agnostic microservices environment! This project demonstrates how to build highly decoupled, event-driven applications using **Kotlin (Ktor)**, **Dapr**, and **Crossplane** running entirely on local Kubernetes.

## 🌟 The Core Philosophy: Why is this stack so powerful?

Traditional microservices often suffer from heavy vendor lock-in. If you write code directly against the AWS or GCP SDKs, migrating clouds requires a complete rewrite. Furthermore, developers often have to wait on Ops teams to provision infrastructure. 

This stack solves both problems:

1. **Zero App-Level Vendor Lock-in:** The Ktor applications **do not** contain any Google Cloud SDKs. They communicate via simple HTTP to a local Dapr sidecar. Dapr handles the complex translation to GCP Pub/Sub. If we move to AWS tomorrow, the application code *does not change*.
2. **Infrastructure as Software:** Instead of running Terraform manually, **Crossplane** runs inside Kubernetes. It constantly monitors our cluster and automatically provisions the required cloud resources (or local emulators) based on simple YAML files.
3. **Developer Self-Service:** A developer just writes a generic "Claim" (`bus-claim.yaml`) saying, *"I need a message bus."* Crossplane reads this and builds the underlying infrastructure automatically based on rules set by Platform Engineers.
4. **100% Free Local Development:** By using the GCP Pub/Sub Emulator, we can test full cloud-native event flows locally without spending a dime or needing cloud credentials.

---

## 🏗️ Architecture Breakdown

Here is the step-by-step purpose of each component in this repository:

### 1. The Applications (Ktor Producer & Consumer)
* **Purpose:** The actual business logic. The `producer` generates events (e.g., a new order), and the `consumer` processes them.
* **Why Ktor?** It's a lightweight, fully asynchronous Kotlin framework built on coroutines, making it incredibly fast and efficient for microservices.

### 2. Dapr (Distributed Application Runtime)
* **Purpose:** The sidecar that sits next to your application container.
* **How it works:** The Producer sends a generic HTTP POST to its local Dapr sidecar. Dapr wraps it in a standard CloudEvent and sends it to the message broker. The Consumer's Dapr sidecar listens to the broker and POSTs the message to the Consumer's `/orders` HTTP endpoint.

### 3. Crossplane
* **Purpose:** The Kubernetes-native infrastructure control plane.
* **How it works:** We installed the Crossplane GCP Provider and a "Composition" pipeline. When we apply our `bus-claim.yaml`, Crossplane detects it, runs it through a patching function (`function-patch-and-transform`), and dynamically provisions the required Pub/Sub topics.



### 4. GCP Emulator
* **Purpose:** A local container mimicking Google Cloud Pub/Sub. 
* **How it works:** Crossplane provisions topics *inside* this emulator instead of the real Google Cloud, and Dapr routes messages through it.

```plaintext
+-------------------------------------------------------------------------+
|                        LOCAL KUBERNETES CLUSTER                         |
|                                                                         |
|   +-------------------+                        +-------------------+    |
|   |   PRODUCER POD    |                        |   CONSUMER POD    |    |
|   |                   |                        |                   |    |
|   | +---------------+ |                        | +---------------+ |    |
|   | |   Ktor App    | |                        | |   Ktor App    | |    |
|   | | (POST /publish) |                        | | (POST /orders)| |    |
|   | +-------+-------+ |                        | +-------^-------+ |    |
|   |         | HTTP    |                        |         | HTTP    |    |
|   | +-------v-------+ |                        | +-------+-------+ |    |
|   | | Dapr Sidecar  | |                        | | Dapr Sidecar  | |    |
|   | | (daprd)       | |                        | | (daprd)       | |    |
|   | +-------+-------+ |                        | +-------^-------+ |    |
|   +---------|---------+                        +---------|---------+    |
|             |                                            |              |
|             | Publishes Event             Subscribes & Pulls Event      |
|             | (CloudEvent format)                        |              |
|   +---------v--------------------------------------------+---------+    |
|   |                                                                |    |
|   |                 GCP PUB/SUB EMULATOR (Broker)                  |    |
|   |                     [Topic: 'orders']                          |    |
|   |                                                                |    |
|   +----------------------------------------------------------------+    |
|                                 ^                                       |
|                                 | Provisions & Configures Topic         |
|   +-----------------------------+----------------------------------+    |
|   |                        CROSSPLANE                              |    |
|   |  (Reads 'bus-claim.yaml' -> Executes 'patch-and-transform')    |    |
|   +----------------------------------------------------------------+    |
+-------------------------------------------------------------------------+
```

---

## 🚀 Getting Started

### Prerequisites
* Docker Desktop (or OrbStack/Colima)
* A local Kubernetes cluster (Docker Desktop built-in, or KinD/Minikube)
* `kubectl` and `helm` installed on your machine.

### 1. Deploy the Environment
We have scripted the entire bootstrapping process. This script builds your local Docker images, applies the Crossplane infrastructure definitions, and deploys your Ktor apps.

```bash
./deploy.sh
```

_Note: The first time you run this, it may take a few minutes for Crossplane to download its providers and establish the emulators._

### 2. Test the Event Flow

Once the pods are running (`kubectl get pods`), you can trigger an event by port-forwarding the producer and sending a curl request.

Open a new terminal and watch the consumer logs:
```bash
kubectl logs -f -l app=consumer -c consumer
```

### 3. Clean Up (Save Your Battery! 🔋)

Running multiple control planes locally is heavy. When you are done developing, run the teardown script to gracefully destroy the infrastructure, apps, and Crossplane/Dapr control planes.

```bash
./teardown.sh
```

## 🧠 Deep Dive: The Configuration Files

This stack relies on a strict separation of concerns. Developers only care about their application code and a simple "Claim", while Platform Engineers configure the underlying Dapr components and Crossplane compositions. 

Here is exactly what each file does.

### 🏗️ Crossplane Infrastructure Files

Crossplane turns Kubernetes into a universal control plane. We split this into "Plumbing" (Platform Setup) and "Logic" (Developer Usage).

* **`infra/base/crds/definition.yaml` (The XRD)**
    * **What it is:** The Composite Resource Definition.
    * **Why we need it:** This is the API contract. It defines a custom Kubernetes resource (e.g., `MessageBus`). It tells Kubernetes: *"Allow developers to request a MessageBus and give them fields like `topicName`."*

* **`infra/overlays/local/phase-1-plumbing/provider.yaml`**
    * **What it is:** Installs the `provider-gcp-pubsub` and configures it via `DeploymentRuntimeConfig`.
    * **Why we need it:** The Provider contains the actual code to talk to GCP. The `DeploymentRuntimeConfig` is crucial here: it injects the `PUBSUB_EMULATOR_HOST` environment variable into the Provider pod. This tricks Crossplane into building infrastructure inside our local emulator instead of reaching out to the real Google Cloud!

* **`infra/overlays/local/phase-2-logic/composition.yaml`**
    * **What it is:** The translation engine.
    * **Why we need it:** When a developer asks for a `MessageBus`, this file tells Crossplane *how* to build it. It uses a pipeline function (`function-patch-and-transform`) to take the developer's requested `topicName` and map it to a concrete GCP `PubSubTopic` managed resource.

* **`infra/overlays/local/phase-2-logic/bus-claim.yaml`**
    * **What it is:** The Developer's Request.
    * **Why we need it:** This is the only infrastructure file an app developer touches. It simply says, *"I want an instance of a MessageBus named 'orders-bus'."* Crossplane detects this and triggers the `composition.yaml`.

---

### 🛜 Dapr Configuration Files

Dapr abstracts away the message broker so our Ktor apps don't need Google Cloud SDKs.

* **`infra/base/runtime/dapr-pubsub.yaml` (The Component)**
    * **What it is:** A Dapr `Component` Custom Resource named `order-pubsub`.
    * **Why we need it:** When the Ktor producer calls `dapr.publishEvent("order-pubsub", ...)`, it has no idea *what* `order-pubsub` actually is. Dapr intercepts that call, looks up this YAML file, and sees `type: pubsub.gcp.pubsub`.
    * **The Emulator Magic:** The `metadata` section is the secret sauce for local development. By hardcoding the `endpoint` to `"gcp-emulator:8085"`, we explicitly tell the Dapr sidecar to ignore the real internet and route all traffic to our local Crossplane-managed emulator. The `projectId` is just a dummy value to satisfy the GCP SDK's requirements!

* **`consumer/k8s-deployment.yaml` & `producer/k8s-deployment.yaml` (The Annotations)**
    * **What it is:** Standard Kubernetes deployments, injected with Dapr magic.
    * **Why we need it:** * `dapr.io/enabled: "true"` tells the Dapr operator to inject the sidecar container (`daprd`) into our pod.
        * `dapr.io/app-id` gives our app a unique identity on the Dapr network.
        * `dapr.io/app-port: "8080"` (Crucial for the Consumer!) tells the Dapr sidecar which local port the Ktor application is listening on, so it knows exactly where to POST incoming Pub/Sub messages.

* **The Ktor `/dapr/subscribe` Route (Programmatic Subscription)**
    * **What it is:** Instead of writing another YAML file, our Ktor consumer has a dedicated HTTP endpoint.
    * **Why we need it:** When the consumer's Dapr sidecar boots up, it calls this endpoint to ask the app: *"What topics do you care about?"* The app replies with JSON linking the `orders` topic on the `order-pubsub` component to the local `/orders` HTTP route.

## 🌍 Path to Production: Moving to Real GCP (or any other)

The true power of this Agnostic Architecture reveals itself when it's time to deploy to a real production environment (like Google Kubernetes Engine - GKE). 

Because we decoupled the application code from the infrastructure, **you do not need to change a single line of Kotlin code to run in production.** You only update the YAML configurations to point to real Google Cloud services instead of local emulators.

Here is the exact checklist for production migration:

### 1. The Application Code (Ktor)
* **What changes:** Absolutely nothing.
* **Why:** The app only knows how to speak HTTP to `localhost:3500` (the Dapr sidecar). It is blissfully unaware whether Dapr is talking to an emulator, real GCP, or AWS.

### 2. Crossplane Configuration (Infrastructure)
To make Crossplane provision real topics in Google Cloud, you must remove the emulator hacks and provide real credentials.

* **Remove the Emulator Injection:** Delete `DeploymentRuntimeConfig` from Phase 1. The GCP Provider pod no longer needs the `PUBSUB_EMULATOR_HOST` environment variable.
* **Create a GCP Service Account:** Create a Service Account in GCP with the `Pub/Sub Admin` role and download its JSON key.
* **Create a Kubernetes Secret:** Store that JSON key securely in your cluster:

  ```bash
  kubectl create secret generic gcp-secret -n crossplane-system --from-file=creds=./gcp-credentials.json
  ```
* **Update the `ProviderConfig`:** Tell Crossplane to use that secret to authenticate against the real GCP API.

```yaml
apiVersion: gcp.upbound.io/v1beta1
kind: ProviderConfig
metadata:
  name: default
spec:
  projectID: your-real-gcp-project-id
  credentials:
    source: Secret
    secretRef:
      namespace: crossplane-system
      name: gcp-secret
      key: creds
```

### 3. Dapr Configuration (Runtime)

Dapr also needs to know to stop routing messages to the emulator and start routing them to the real GCP Pub/Sub.

* **Update** `infra/base/runtime/dapr-pubsub.yaml`:

    * **Remove** the `endpoint: "gcp-emulator:8085"` metadata field entirely.
    * **Update** `projectId` to your actual GCP Project ID.
    * **Add Authentication:** Provide Dapr with the means to authenticate. On GKE, the best practice is to use Workload Identity (binding a Kubernetes Service Account to a GCP Service Account), or by passing a secret similar to Crossplane.

```yaml
apiVersion: dapr.io/v1alpha1
kind: Component
metadata:
  name: order-pubsub
  namespace: default
spec:
  type: pubsub.gcp.pubsub
  version: v1
  metadata:
    - name: projectId
      value: "your-real-gcp-project-id"
    # The endpoint is removed. Dapr defaults to the real GCP API.
    # Authentication is securely handled by GKE Workload Identity automatically!
```

Once these YAMLs are updated, running `./deploy.sh` will cause Crossplane to reach out to Google Cloud, physically build the Pub/Sub topic in your GCP console, and wire Dapr directly to it!

## ☎️ So What?!

### 📉 1. The "Lowest Common Denominator" Trap (Feature Parity)

Because Dapr is designed to work with GCP, AWS SNS/SQS, Kafka, RabbitMQ, and Redis, its API must be generic. It only exposes features that all of those brokers share.

If you use the native GCP SDK, you get instant access to Google's specialized features. With Dapr, you lose (or struggle to implement) things like:

Exactly-Once Delivery: GCP supports strict exactly-once delivery guarantees, but Dapr's generic API is built around "at-least-once" delivery.

Schema Validation: GCP Pub/Sub can enforce Avro or Protocol Buffer schemas at the topic level. Integrating this with Dapr's CloudEvent wrappers is clunky.

Message Ordering: GCP allows you to guarantee the order of messages using "Ordering Keys." While Dapr recently added some metadata support for routing keys, it is not as robust or native as the GCP SDK.

### ⏱️ 2. The Latency Penalty (The Extra Hop)

When you use the GCP SDK, your Kotlin application opens a highly optimized, multiplexed gRPC connection directly to Google's servers.

With Dapr, you introduce an extra network hop:
`Ktor App -> (HTTP/gRPC) -> Local Dapr Sidecar -> (gRPC) -> Google Cloud`

While the hop from your app to the sidecar over `localhost` is extremely fast (usually ~1-2 milliseconds), it is not zero. For 99% of microservices, this latency is invisible. But if you are building an ultra-low-latency system (like high-frequency trading or real-time gaming), the native SDK will always be faster.

### 📥 3. Push vs. Streaming Pull Mechanics

Under the hood, Dapr connects to GCP Pub/Sub, pulls the messages, and then pushes them to your Ktor app via an HTTP POST request (e.g., POSTing to your `/orders` route).

The Dapr Way: Your Ktor app acts like a web server receiving a flood of HTTP requests. You have to tune Ktor's connection limits and Dapr's `maxConcurrentMessages` metadata so Dapr doesn't accidentally DDoS your own app during traffic spikes.

The Native SDK Way: The GCP SDK uses Streaming Pull. Your app opens a persistent connection and silently consumes messages at its own optimal pace. It is generally more resource-efficient for massive batch processing.

### 📦 4. The CloudEvents Wrapper (Interoperability)

Dapr wraps all outgoing messages in the standard CloudEvents JSON envelope.

If your entire company uses Dapr, this is great! But if you have a legacy Node.js or Python application that reads directly from GCP Pub/Sub using the native SDK, it is suddenly going to receive a CloudEvent JSON wrapper instead of the raw data it expects. You can configure Dapr to use rawPayload: true to bypass this, but it adds configuration friction.

### ⚙️ 5. Operational Overhead

A native SDK is just a .jar file compiled into your application. If it crashes, you get a standard Java stack trace.

Dapr requires you to run and monitor a completely separate distributed system:

You have to allocate CPU and RAM to the daprd sidecar container.

You have to manage the lifecycle (e.g., ensuring Ktor doesn't try to send a message before the sidecar has finished booting).

Debugging requires looking at two sets of logs (App + Sidecar) instead of one.

### ⚖️ The Verdict: When to use which?

**Use Dapr if:** You want to prevent vendor lock-in, you want to test locally without cloud credentials, your team uses multiple languages (Kotlin, Go, Python) and wants a unified messaging API, and you are building standard asynchronous microservices.

**Use the Native GCP SDK if:** Your application processes millions of messages per second where microseconds matter, you heavily rely on GCP-specific features like strict ordering/schemas, or you are building a data-streaming pipeline (like Apache Beam/Dataflow) rather than a web microservice.