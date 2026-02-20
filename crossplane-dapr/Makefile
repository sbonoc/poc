SHELL := /bin/bash

NAMESPACE_DAPR ?= dapr-system
NAMESPACE_XP ?= crossplane-system
APP_NAMESPACE ?= default
DAPR_RUNTIME_VERSION ?= 1.16.9
CROSSPLANE_VERSION ?= 2.2.0
PORT_FORWARD_NAMESPACE ?= $(APP_NAMESPACE)
PRODUCER_KTOR_LOCAL_PORT ?= 8080
CONSUMER_KTOR_LOCAL_PORT ?= 8081
PRODUCER_GIN_LOCAL_PORT ?= 8082
CONSUMER_GIN_LOCAL_PORT ?= 8083
PRODUCER_SPRINGBOOT_LOCAL_PORT ?= 8084
CONSUMER_SPRINGBOOT_LOCAL_PORT ?= 8085
PRODUCER_LOCAL_PORT ?= $(PRODUCER_KTOR_LOCAL_PORT)
APP_SERVICE_PORT ?= 8080
GRAFANA_LOCAL_PORT ?= 3000
GRAFANA_SERVICE_PORT ?= 3000
PUSHGATEWAY_URL ?= http://localhost:9091
PUSHGATEWAY_JOB_PREFIX ?= test-pyramid-latest
PUSHGATEWAY_INSTANCE ?= latest
PUSHGATEWAY_HISTORY_JOB_PREFIX ?= test-pyramid-history
TEST_PYRAMID_STACKS ?= $(shell ./scripts/test-pyramid-targets.sh stacks)
TEST_PYRAMID_SERVICES ?= $(shell ./scripts/test-pyramid-targets.sh services)
PUSH_HISTORY ?= false
RUN_ID ?=
PUSHGATEWAY_LOCAL_PORT ?= 9091

.PHONY: preflight bootstrap-control-planes build-images deploy-local wait-local teardown-local destroy-control-planes smoke-test
.PHONY: unit-test integration-test contract-test e2e-test quality verify format run-producer-local run-consumer-local
.PHONY: unit-test-ktor unit-test-springboot unit-test-gin integration-test-ktor integration-test-springboot integration-test-gin
.PHONY: contract-test-ktor contract-test-springboot contract-test-gin e2e-test-ktor e2e-test-springboot e2e-test-gin
.PHONY: pact-regenerate test-pyramid-metrics push-test-pyramid-metrics push-test-pyramid-history port-forward-local help

help:
	@echo "Usage: make <target> [VAR=value]"
	@echo
	@echo "Core targets:"
	@echo "  help                  Show this help"
	@echo "  deploy-local          Build images and deploy full local stack"
	@echo "  wait-local            Wait for app/observability deployments to be available"
	@echo "  port-forward-local    Port-forward all services and grafana locally"
	@echo "  smoke-test            Run burst publish smoke test on all producers"
	@echo "  teardown-local        Delete local stack resources"
	@echo "  destroy-control-planes Remove Dapr/Crossplane control planes"
	@echo
	@echo "Verification targets:"
	@echo "  unit-test integration-test contract-test e2e-test"
	@echo "  unit-test-ktor|springboot|gin (same split exists for integration/contract/e2e)"
	@echo "  test-pyramid-metrics  Generate segregated pyramid metrics for stacks and services"
	@echo "  push-test-pyramid-metrics Push generated metrics to Pushgateway"
	@echo "  push-test-pyramid-history Push generated metrics as per-run history snapshot"
	@echo "  pact-regenerate       Regenerate consumer pact and verify provider against it"
	@echo "  quality verify format"
	@echo
	@echo "Script helpers:"
	@echo "  ./deploy.sh --help"
	@echo "  ./teardown.sh --help"
	@echo "  ./scripts/smoke-test-all.sh --help"
	@echo "  ./scripts/gin/run-suite.sh --help"
	@echo "  ./scripts/gin/collect-test-pyramid.sh --help"
	@echo "  ./scripts/gin/ensure-pact-cli.sh --help"
	@echo "  ./scripts/test-pyramid-targets.sh --help"
	@echo "  ./scripts/create-local-gcp-emulator-secret.sh --help"
	@echo "  ./scripts/push-test-pyramid-metrics.sh --help"
	@echo
	@echo "Useful variables:"
	@echo "  APP_NAMESPACE=$(APP_NAMESPACE)"
	@echo "  PORT_FORWARD_NAMESPACE=$(PORT_FORWARD_NAMESPACE)"
	@echo "  PRODUCER_KTOR_LOCAL_PORT=$(PRODUCER_KTOR_LOCAL_PORT)"
	@echo "  CONSUMER_KTOR_LOCAL_PORT=$(CONSUMER_KTOR_LOCAL_PORT)"
	@echo "  PRODUCER_GIN_LOCAL_PORT=$(PRODUCER_GIN_LOCAL_PORT)"
	@echo "  CONSUMER_GIN_LOCAL_PORT=$(CONSUMER_GIN_LOCAL_PORT)"
	@echo "  PRODUCER_SPRINGBOOT_LOCAL_PORT=$(PRODUCER_SPRINGBOOT_LOCAL_PORT)"
	@echo "  CONSUMER_SPRINGBOOT_LOCAL_PORT=$(CONSUMER_SPRINGBOOT_LOCAL_PORT)"
	@echo "  GRAFANA_LOCAL_PORT=$(GRAFANA_LOCAL_PORT)"
	@echo "  PUSHGATEWAY_URL=$(PUSHGATEWAY_URL)"
	@echo "  PUSHGATEWAY_JOB_PREFIX=$(PUSHGATEWAY_JOB_PREFIX)"
	@echo "  PUSHGATEWAY_INSTANCE=$(PUSHGATEWAY_INSTANCE)"
	@echo "  PUSHGATEWAY_HISTORY_JOB_PREFIX=$(PUSHGATEWAY_HISTORY_JOB_PREFIX)"
	@echo "  TEST_PYRAMID_STACKS=$(TEST_PYRAMID_STACKS)"
	@echo "  TEST_PYRAMID_SERVICES=$(TEST_PYRAMID_SERVICES)"
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
	docker build -t agnostic-producer-ktor:local -f producer-ktor/Dockerfile .
	docker build -t agnostic-consumer-ktor:local -f consumer-ktor/Dockerfile .
	docker build -t agnostic-producer-gin:local -f producer-gin/Dockerfile .
	docker build -t agnostic-consumer-gin:local -f consumer-gin/Dockerfile .
	docker build -t agnostic-producer-springboot:local -f producer-springboot/Dockerfile .
	docker build -t agnostic-consumer-springboot:local -f consumer-springboot/Dockerfile .

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
	@echo "Restarting app deployments to pick up rebuilt :local images"
	@for deployment in producer-ktor consumer-ktor producer-gin consumer-gin producer-springboot consumer-springboot; do \
		kubectl -n $(APP_NAMESPACE) rollout restart deployment/$$deployment >/dev/null 2>&1 || true; \
	done
	$(MAKE) wait-local
	@echo "Local deployment is ready in namespace $(APP_NAMESPACE)."
	@echo "Run 'make port-forward-local' to expose all apps and grafana locally."

wait-local:
	kubectl -n $(NAMESPACE_XP) wait --for=condition=available deployment/gcp-emulator --timeout=180s
	kubectl -n $(APP_NAMESPACE) wait --for=condition=available deployment/producer-ktor --timeout=180s
	kubectl -n $(APP_NAMESPACE) wait --for=condition=available deployment/consumer-ktor --timeout=180s
	kubectl -n $(APP_NAMESPACE) wait --for=condition=available deployment/producer-gin --timeout=180s
	kubectl -n $(APP_NAMESPACE) wait --for=condition=available deployment/consumer-gin --timeout=180s
	kubectl -n $(APP_NAMESPACE) wait --for=condition=available deployment/producer-springboot --timeout=180s
	kubectl -n $(APP_NAMESPACE) wait --for=condition=available deployment/consumer-springboot --timeout=180s
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
	@kubectl -n $(APP_NAMESPACE) wait --for=delete deployment/producer-ktor --timeout=120s >/dev/null 2>&1 || true
	@kubectl -n $(APP_NAMESPACE) wait --for=delete deployment/consumer-ktor --timeout=120s >/dev/null 2>&1 || true
	@kubectl -n $(APP_NAMESPACE) wait --for=delete deployment/producer-gin --timeout=120s >/dev/null 2>&1 || true
	@kubectl -n $(APP_NAMESPACE) wait --for=delete deployment/consumer-gin --timeout=120s >/dev/null 2>&1 || true
	@kubectl -n $(APP_NAMESPACE) wait --for=delete deployment/producer-springboot --timeout=120s >/dev/null 2>&1 || true
	@kubectl -n $(APP_NAMESPACE) wait --for=delete deployment/consumer-springboot --timeout=120s >/dev/null 2>&1 || true
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
	PORT_FORWARD_NAMESPACE=$(PORT_FORWARD_NAMESPACE) \
	APP_SERVICE_PORT=$(APP_SERVICE_PORT) \
	PRODUCER_KTOR_LOCAL_PORT=$(PRODUCER_KTOR_LOCAL_PORT) \
	PRODUCER_GIN_LOCAL_PORT=$(PRODUCER_GIN_LOCAL_PORT) \
	PRODUCER_SPRINGBOOT_LOCAL_PORT=$(PRODUCER_SPRINGBOOT_LOCAL_PORT) \
	./scripts/smoke-test-all.sh

unit-test:
	$(MAKE) unit-test-ktor
	$(MAKE) unit-test-springboot
	$(MAKE) unit-test-gin

unit-test-ktor:
	./gradlew unitTestKtor

unit-test-springboot:
	./gradlew unitTestSpringBoot

unit-test-gin:
	./scripts/gin/run-suite.sh unit

integration-test:
	$(MAKE) integration-test-ktor
	$(MAKE) integration-test-springboot
	$(MAKE) integration-test-gin

integration-test-ktor:
	./gradlew integrationTestKtor

integration-test-springboot:
	./gradlew integrationTestSpringBoot

integration-test-gin:
	./scripts/gin/run-suite.sh integration

contract-test:
	$(MAKE) contract-test-ktor
	$(MAKE) contract-test-springboot
	$(MAKE) contract-test-gin

contract-test-ktor:
	./gradlew contractTestKtor

contract-test-springboot:
	./gradlew contractTestSpringBoot

contract-test-gin:
	./scripts/gin/run-suite.sh contract

pact-regenerate:
	./gradlew :producer-ktor:contractTest :producer-springboot:contractTest --rerun-tasks
	./scripts/gin/run-suite.sh contract

e2e-test:
	$(MAKE) e2e-test-ktor
	$(MAKE) e2e-test-springboot
	$(MAKE) e2e-test-gin

e2e-test-ktor:
	RUN_E2E=true ./gradlew e2eTestKtor

e2e-test-springboot:
	RUN_E2E=true ./gradlew e2eTestSpringBoot

e2e-test-gin:
	RUN_E2E=true ./scripts/gin/run-suite.sh e2e

test-pyramid-metrics:
	rm -f build/reports/test-pyramid/summary.json build/reports/test-pyramid/test-pyramid.prom
	./gradlew testPyramidMetricsJvm
	./scripts/gin/collect-test-pyramid.sh

push-test-pyramid-metrics:
	@norm_url=$$(echo "$(PUSHGATEWAY_URL)" | tr '[:upper:]' '[:lower:]'); \
	if [[ "$$norm_url" == http://localhost* || "$$norm_url" == http://127.0.0.1* ]]; then \
		kubectl -n $(PORT_FORWARD_NAMESPACE) wait --for=condition=available deployment/pushgateway --timeout=120s; \
		mkdir -p build; \
		kubectl -n $(PORT_FORWARD_NAMESPACE) port-forward svc/pushgateway $(PUSHGATEWAY_LOCAL_PORT):9091 >build/pf-pushgateway.log 2>&1 & \
		pushgateway_pid=$$!; \
		trap 'kill $$pushgateway_pid >/dev/null 2>&1 || true' INT TERM EXIT; \
		sleep 2; \
		STACKS="$(TEST_PYRAMID_STACKS)" SERVICES="$(TEST_PYRAMID_SERVICES)" HISTORY_JOB_PREFIX="$(PUSHGATEWAY_HISTORY_JOB_PREFIX)" PUSH_HISTORY="$(PUSH_HISTORY)" RUN_ID="$(RUN_ID)" ./scripts/push-test-pyramid-metrics.sh http://localhost:$(PUSHGATEWAY_LOCAL_PORT) $(PUSHGATEWAY_JOB_PREFIX) $(PUSHGATEWAY_INSTANCE); \
	else \
		STACKS="$(TEST_PYRAMID_STACKS)" SERVICES="$(TEST_PYRAMID_SERVICES)" HISTORY_JOB_PREFIX="$(PUSHGATEWAY_HISTORY_JOB_PREFIX)" PUSH_HISTORY="$(PUSH_HISTORY)" RUN_ID="$(RUN_ID)" ./scripts/push-test-pyramid-metrics.sh $(PUSHGATEWAY_URL) $(PUSHGATEWAY_JOB_PREFIX) $(PUSHGATEWAY_INSTANCE); \
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
	OTEL_EXPORTER_OTLP_ENDPOINT=http://localhost:4317 OTEL_EXPORTER_OTLP_PROTOCOL=grpc ./gradlew :producer-ktor:run

run-consumer-local:
	OTEL_EXPORTER_OTLP_ENDPOINT=http://localhost:4317 OTEL_EXPORTER_OTLP_PROTOCOL=grpc ./gradlew :consumer-ktor:run

port-forward-local:
	@command -v kubectl >/dev/null || (echo "kubectl is required" && exit 1)
	@kubectl -n $(PORT_FORWARD_NAMESPACE) wait --for=condition=available deployment/producer-ktor --timeout=120s
	@kubectl -n $(PORT_FORWARD_NAMESPACE) wait --for=condition=available deployment/consumer-ktor --timeout=120s
	@kubectl -n $(PORT_FORWARD_NAMESPACE) wait --for=condition=available deployment/producer-gin --timeout=120s
	@kubectl -n $(PORT_FORWARD_NAMESPACE) wait --for=condition=available deployment/consumer-gin --timeout=120s
	@kubectl -n $(PORT_FORWARD_NAMESPACE) wait --for=condition=available deployment/producer-springboot --timeout=120s
	@kubectl -n $(PORT_FORWARD_NAMESPACE) wait --for=condition=available deployment/consumer-springboot --timeout=120s
	@kubectl -n $(PORT_FORWARD_NAMESPACE) wait --for=condition=available deployment/grafana --timeout=120s
	@mkdir -p build
	@kubectl -n $(PORT_FORWARD_NAMESPACE) port-forward svc/producer-ktor $(PRODUCER_KTOR_LOCAL_PORT):$(APP_SERVICE_PORT) >build/pf-producer-ktor.log 2>&1 & \
	producer_ktor_pid=$$!; \
	kubectl -n $(PORT_FORWARD_NAMESPACE) port-forward svc/consumer-ktor $(CONSUMER_KTOR_LOCAL_PORT):$(APP_SERVICE_PORT) >build/pf-consumer-ktor.log 2>&1 & \
	consumer_ktor_pid=$$!; \
	kubectl -n $(PORT_FORWARD_NAMESPACE) port-forward svc/producer-gin $(PRODUCER_GIN_LOCAL_PORT):$(APP_SERVICE_PORT) >build/pf-producer-gin.log 2>&1 & \
	producer_gin_pid=$$!; \
	kubectl -n $(PORT_FORWARD_NAMESPACE) port-forward svc/consumer-gin $(CONSUMER_GIN_LOCAL_PORT):$(APP_SERVICE_PORT) >build/pf-consumer-gin.log 2>&1 & \
	consumer_gin_pid=$$!; \
	kubectl -n $(PORT_FORWARD_NAMESPACE) port-forward svc/producer-springboot $(PRODUCER_SPRINGBOOT_LOCAL_PORT):$(APP_SERVICE_PORT) >build/pf-producer-springboot.log 2>&1 & \
	producer_springboot_pid=$$!; \
	kubectl -n $(PORT_FORWARD_NAMESPACE) port-forward svc/consumer-springboot $(CONSUMER_SPRINGBOOT_LOCAL_PORT):$(APP_SERVICE_PORT) >build/pf-consumer-springboot.log 2>&1 & \
	consumer_springboot_pid=$$!; \
	kubectl -n $(PORT_FORWARD_NAMESPACE) port-forward svc/grafana $(GRAFANA_LOCAL_PORT):$(GRAFANA_SERVICE_PORT) & \
	grafana_pid=$$!; \
	trap 'kill $$producer_ktor_pid $$consumer_ktor_pid $$producer_gin_pid $$consumer_gin_pid $$producer_springboot_pid $$consumer_springboot_pid $$grafana_pid >/dev/null 2>&1 || true' INT TERM EXIT; \
	sleep 1; \
	if ! kill -0 $$producer_ktor_pid >/dev/null 2>&1; then echo "producer-ktor port-forward failed to start"; exit 1; fi; \
	if ! kill -0 $$consumer_ktor_pid >/dev/null 2>&1; then echo "consumer-ktor port-forward failed to start"; exit 1; fi; \
	if ! kill -0 $$producer_gin_pid >/dev/null 2>&1; then echo "producer-gin port-forward failed to start"; exit 1; fi; \
	if ! kill -0 $$consumer_gin_pid >/dev/null 2>&1; then echo "consumer-gin port-forward failed to start"; exit 1; fi; \
	if ! kill -0 $$producer_springboot_pid >/dev/null 2>&1; then echo "producer-springboot port-forward failed to start"; exit 1; fi; \
	if ! kill -0 $$consumer_springboot_pid >/dev/null 2>&1; then echo "consumer-springboot port-forward failed to start"; exit 1; fi; \
	if ! kill -0 $$grafana_pid >/dev/null 2>&1; then echo "Grafana port-forward failed to start"; exit 1; fi; \
	echo "producer-ktor available at http://localhost:$(PRODUCER_KTOR_LOCAL_PORT)"; \
	echo "consumer-ktor available at http://localhost:$(CONSUMER_KTOR_LOCAL_PORT)"; \
	echo "producer-gin available at http://localhost:$(PRODUCER_GIN_LOCAL_PORT)"; \
	echo "consumer-gin available at http://localhost:$(CONSUMER_GIN_LOCAL_PORT)"; \
	echo "producer-springboot available at http://localhost:$(PRODUCER_SPRINGBOOT_LOCAL_PORT)"; \
	echo "consumer-springboot available at http://localhost:$(CONSUMER_SPRINGBOOT_LOCAL_PORT)"; \
	echo "Grafana available at http://localhost:$(GRAFANA_LOCAL_PORT)"; \
	echo "Press Ctrl+C to stop all port-forwards"; \
	wait
