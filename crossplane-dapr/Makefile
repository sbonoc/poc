SHELL := /bin/bash

NAMESPACE_DAPR ?= dapr-system
NAMESPACE_XP ?= crossplane-system
APP_NAMESPACE ?= default
DAPR_RUNTIME_VERSION ?= 1.16.9
CROSSPLANE_VERSION ?= 2.2.0
PORT_FORWARD_NAMESPACE ?= $(APP_NAMESPACE)
PRODUCER_LOCAL_PORT ?= 8080
PRODUCER_SERVICE_PORT ?= 8080
GRAFANA_LOCAL_PORT ?= 3000
GRAFANA_SERVICE_PORT ?= 3000
PUSHGATEWAY_URL ?= http://localhost:9091
PUSHGATEWAY_JOB ?= test-pyramid-latest
PUSHGATEWAY_INSTANCE ?= latest
PUSHGATEWAY_HISTORY_JOB ?= test-pyramid-history
PUSH_HISTORY ?= false
RUN_ID ?=
PUSHGATEWAY_LOCAL_PORT ?= 9091

.PHONY: preflight bootstrap-control-planes build-images deploy-local wait-local teardown-local destroy-control-planes smoke-test
.PHONY: unit-test integration-test contract-test e2e-test quality verify format run-producer-local run-consumer-local
.PHONY: pact-regenerate test-pyramid-metrics push-test-pyramid-metrics push-test-pyramid-history port-forward-local help

help:
	@echo "Usage: make <target> [VAR=value]"
	@echo
	@echo "Core targets:"
	@echo "  help                  Show this help"
	@echo "  deploy-local          Build images and deploy full local stack"
	@echo "  wait-local            Wait for app/observability deployments to be available"
	@echo "  port-forward-local    Port-forward producer and grafana locally"
	@echo "  smoke-test            Run burst publish smoke test"
	@echo "  teardown-local        Delete local stack resources"
	@echo "  destroy-control-planes Remove Dapr/Crossplane control planes"
	@echo
	@echo "Verification targets:"
	@echo "  unit-test integration-test contract-test e2e-test"
	@echo "  test-pyramid-metrics  Generate absolute/percentage/duration pyramid metrics"
	@echo "  push-test-pyramid-metrics Push generated metrics to Pushgateway"
	@echo "  push-test-pyramid-history Push generated metrics as per-run history snapshot"
	@echo "  pact-regenerate       Regenerate consumer pact and verify provider against it"
	@echo "  quality verify format"
	@echo
	@echo "Script helpers:"
	@echo "  ./deploy.sh --help"
	@echo "  ./teardown.sh --help"
	@echo "  ./burst-test.sh --help"
	@echo "  ./scripts/create-local-gcp-emulator-secret.sh --help"
	@echo "  ./scripts/push-test-pyramid-metrics.sh --help"
	@echo
	@echo "Useful variables:"
	@echo "  APP_NAMESPACE=$(APP_NAMESPACE)"
	@echo "  PORT_FORWARD_NAMESPACE=$(PORT_FORWARD_NAMESPACE)"
	@echo "  PRODUCER_LOCAL_PORT=$(PRODUCER_LOCAL_PORT)"
	@echo "  GRAFANA_LOCAL_PORT=$(GRAFANA_LOCAL_PORT)"
	@echo "  PUSHGATEWAY_URL=$(PUSHGATEWAY_URL)"
	@echo "  PUSHGATEWAY_JOB=$(PUSHGATEWAY_JOB)"
	@echo "  PUSHGATEWAY_INSTANCE=$(PUSHGATEWAY_INSTANCE)"
	@echo "  PUSHGATEWAY_HISTORY_JOB=$(PUSHGATEWAY_HISTORY_JOB)"
	@echo "  PUSH_HISTORY=$(PUSH_HISTORY)"
	@echo "  RUN_ID=$(RUN_ID)"
	@echo "  PUSHGATEWAY_LOCAL_PORT=$(PUSHGATEWAY_LOCAL_PORT)"

preflight:
	@command -v kubectl >/dev/null || (echo "kubectl is required" && exit 1)
	@command -v helm >/dev/null || (echo "helm is required" && exit 1)
	@command -v dapr >/dev/null || (echo "dapr CLI is required" && exit 1)
	@command -v docker >/dev/null || (echo "docker is required" && exit 1)

bootstrap-control-planes: preflight
	@if ! kubectl get ns $(NAMESPACE_DAPR) >/dev/null 2>&1; then \
		echo "Initializing Dapr in Kubernetes"; \
		dapr init -k --runtime-version $(DAPR_RUNTIME_VERSION); \
	else \
		echo "Dapr control plane already exists"; \
	fi
	@if ! kubectl get ns $(NAMESPACE_XP) >/dev/null 2>&1; then \
		echo "Installing Crossplane"; \
		helm repo add crossplane-stable https://charts.crossplane.io/stable --force-update; \
		helm install crossplane crossplane-stable/crossplane --namespace $(NAMESPACE_XP) --create-namespace --version $(CROSSPLANE_VERSION); \
	else \
		echo "Crossplane control plane already exists"; \
	fi
	kubectl wait --for=condition=Ready pods --all -n $(NAMESPACE_DAPR) --timeout=180s
	kubectl wait --for=condition=Ready pods --all -n $(NAMESPACE_XP) --timeout=180s

build-images:
	docker build -t agnostic-producer:local -f producer/Dockerfile .
	docker build -t agnostic-consumer:local -f consumer/Dockerfile .

deploy-local: bootstrap-control-planes build-images
	kubectl create namespace $(APP_NAMESPACE) --dry-run=client -o yaml | kubectl apply -f -
	kubectl apply -k infra/base/crossplane
	kubectl wait --for=condition=Established xrd/xmessagebuses.agnostic.systems --timeout=120s
	kubectl apply -f infra/overlays/local/crossplane-runtime-config.yaml
	kubectl apply -f infra/overlays/local/crossplane-provider.yaml
	kubectl apply -f infra/overlays/local/crossplane-function.yaml
	kubectl apply -f infra/overlays/local/emulator.yaml
	./scripts/create-local-gcp-emulator-secret.sh $(NAMESPACE_XP)
	@echo "Waiting for pubsub provider API registration"
	@for i in {1..36}; do \
		if kubectl api-resources | grep -q "pubsub.gcp.upbound.io"; then \
			echo "Provider API is available"; \
			break; \
		fi; \
		echo "...still waiting ($$i/36)"; \
		sleep 5; \
		if [ $$i -eq 36 ]; then echo "Provider API registration timeout" && exit 1; fi; \
	done
	kubectl apply -k infra/overlays/local -n $(APP_NAMESPACE)
	$(MAKE) wait-local
	@echo "Local deployment is ready in namespace $(APP_NAMESPACE)."
	@echo "Run 'make port-forward-local' to expose producer/grafana locally."

wait-local:
	kubectl -n $(NAMESPACE_XP) wait --for=condition=available deployment/gcp-emulator --timeout=180s
	kubectl -n $(APP_NAMESPACE) wait --for=condition=available deployment/producer --timeout=180s
	kubectl -n $(APP_NAMESPACE) wait --for=condition=available deployment/consumer --timeout=180s
	kubectl -n $(APP_NAMESPACE) wait --for=condition=available deployment/otel-collector --timeout=180s
	kubectl -n $(APP_NAMESPACE) wait --for=condition=available deployment/prometheus --timeout=180s
	kubectl -n $(APP_NAMESPACE) wait --for=condition=available deployment/pushgateway --timeout=180s
	kubectl -n $(APP_NAMESPACE) wait --for=condition=available deployment/tempo --timeout=180s
	kubectl -n $(APP_NAMESPACE) wait --for=condition=available deployment/grafana --timeout=180s

teardown-local:
	kubectl delete -k infra/overlays/local -n $(APP_NAMESPACE) --ignore-not-found=true
	kubectl delete -f infra/overlays/local/emulator.yaml --ignore-not-found=true
	kubectl delete secret gcp-emulator-credentials -n $(NAMESPACE_XP) --ignore-not-found=true
	kubectl delete -k infra/base/crossplane --ignore-not-found=true
	@kubectl -n $(APP_NAMESPACE) wait --for=delete deployment/producer --timeout=120s >/dev/null 2>&1 || true
	@kubectl -n $(APP_NAMESPACE) wait --for=delete deployment/consumer --timeout=120s >/dev/null 2>&1 || true
	@kubectl -n $(APP_NAMESPACE) wait --for=delete deployment/otel-collector --timeout=120s >/dev/null 2>&1 || true
	@kubectl -n $(APP_NAMESPACE) wait --for=delete deployment/prometheus --timeout=120s >/dev/null 2>&1 || true
	@kubectl -n $(APP_NAMESPACE) wait --for=delete deployment/pushgateway --timeout=120s >/dev/null 2>&1 || true
	@kubectl -n $(APP_NAMESPACE) wait --for=delete deployment/tempo --timeout=120s >/dev/null 2>&1 || true
	@kubectl -n $(APP_NAMESPACE) wait --for=delete deployment/grafana --timeout=120s >/dev/null 2>&1 || true

# Optional heavy cleanup of control planes.
destroy-control-planes:
	helm uninstall crossplane --namespace $(NAMESPACE_XP) || true
	kubectl delete ns $(NAMESPACE_XP) --ignore-not-found=true
	dapr uninstall -k --all || true
	kubectl delete ns $(NAMESPACE_DAPR) --ignore-not-found=true

smoke-test:
	./burst-test.sh

unit-test:
	./gradlew unitTest

integration-test:
	./gradlew integrationTest

contract-test:
	./gradlew contractTest

pact-regenerate:
	./gradlew :producer:contractTest --rerun-tasks

e2e-test:
	RUN_E2E=true ./gradlew e2eTest

test-pyramid-metrics:
	./gradlew testPyramidMetrics

push-test-pyramid-metrics:
	@if [[ "$(PUSHGATEWAY_URL)" == http://localhost* || "$(PUSHGATEWAY_URL)" == http://127.0.0.1* ]]; then \
		kubectl -n $(PORT_FORWARD_NAMESPACE) wait --for=condition=available deployment/pushgateway --timeout=120s; \
		kubectl -n $(PORT_FORWARD_NAMESPACE) port-forward svc/pushgateway $(PUSHGATEWAY_LOCAL_PORT):9091 >/tmp/pf-pushgateway.log 2>&1 & \
		pushgateway_pid=$$!; \
		trap 'kill $$pushgateway_pid >/dev/null 2>&1 || true' INT TERM EXIT; \
		sleep 2; \
		HISTORY_JOB="$(PUSHGATEWAY_HISTORY_JOB)" PUSH_HISTORY="$(PUSH_HISTORY)" RUN_ID="$(RUN_ID)" ./scripts/push-test-pyramid-metrics.sh http://localhost:$(PUSHGATEWAY_LOCAL_PORT) $(PUSHGATEWAY_JOB) $(PUSHGATEWAY_INSTANCE); \
	else \
		HISTORY_JOB="$(PUSHGATEWAY_HISTORY_JOB)" PUSH_HISTORY="$(PUSH_HISTORY)" RUN_ID="$(RUN_ID)" ./scripts/push-test-pyramid-metrics.sh $(PUSHGATEWAY_URL) $(PUSHGATEWAY_JOB) $(PUSHGATEWAY_INSTANCE); \
	fi

push-test-pyramid-history: PUSH_HISTORY=true
push-test-pyramid-history: push-test-pyramid-metrics

quality:
	./gradlew ktlintCheck detekt koverVerify

verify:
	./gradlew clean check koverVerify

format:
	./gradlew ktlintFormat

run-producer-local:
	OTEL_EXPORTER_OTLP_ENDPOINT=http://localhost:4317 OTEL_EXPORTER_OTLP_PROTOCOL=grpc ./gradlew :producer:run

run-consumer-local:
	OTEL_EXPORTER_OTLP_ENDPOINT=http://localhost:4317 OTEL_EXPORTER_OTLP_PROTOCOL=grpc ./gradlew :consumer:run

port-forward-local:
	@command -v kubectl >/dev/null || (echo "kubectl is required" && exit 1)
	@kubectl -n $(PORT_FORWARD_NAMESPACE) wait --for=condition=available deployment/producer --timeout=120s
	@kubectl -n $(PORT_FORWARD_NAMESPACE) wait --for=condition=available deployment/grafana --timeout=120s
	@kubectl -n $(PORT_FORWARD_NAMESPACE) port-forward svc/producer $(PRODUCER_LOCAL_PORT):$(PRODUCER_SERVICE_PORT) & \
	producer_pid=$$!; \
	kubectl -n $(PORT_FORWARD_NAMESPACE) port-forward svc/grafana $(GRAFANA_LOCAL_PORT):$(GRAFANA_SERVICE_PORT) & \
	grafana_pid=$$!; \
	trap 'kill $$producer_pid $$grafana_pid >/dev/null 2>&1 || true' INT TERM EXIT; \
	sleep 1; \
	if ! kill -0 $$producer_pid >/dev/null 2>&1; then echo "Producer port-forward failed to start"; exit 1; fi; \
	if ! kill -0 $$grafana_pid >/dev/null 2>&1; then echo "Grafana port-forward failed to start"; exit 1; fi; \
	echo "Producer available at http://localhost:$(PRODUCER_LOCAL_PORT)"; \
	echo "Grafana available at http://localhost:$(GRAFANA_LOCAL_PORT)"; \
	echo "Press Ctrl+C to stop both port-forwards"; \
	wait
