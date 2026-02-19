#!/bin/bash
set -e

# --- CONFIGURATION ---
XRD_NAME="xpubsubtopics.agnostic.systems"
NAMESPACE_DAPR="dapr-system"
NAMESPACE_XP="crossplane-system"

echo "Removing previous application deployments..."
kubectl delete -f producer/k8s-deployment.yaml --ignore-not-found=true || true
kubectl delete -f consumer/k8s-deployment.yaml --ignore-not-found=true || true

echo "Removing infrastructure claims and emulator..."
kubectl delete -k infra/overlays/local/phase-2-logic  -ignore-not-found=true || true
kubectl delete -k infra/overlays/local/phase-1-plumbing/ -ignore-not-found=true || true

echo "Cleaning up Crossplane XRD (this may take a moment)..."
kubectl delete -k infra/base/runtime/ --ignore-not-found=true || true
kubectl delete -k infra/base/crds/ --ignore-not-found=true || true

# Wait for the XRD to be fully gone to avoid naming conflicts
until ! kubectl get xrd $XRD_NAME > /dev/null 2>&1; do
  echo "   ...waiting for XRD to finalize deletion..."
  sleep 2
done

echo "✅ Cleanup Complete. Starting Fresh Deployment."
echo "--------------------------------------------------"

echo "🏁 Starting All-in-One Deployment..."

# --- 1. HOST TOOLING CHECK & INSTALL ---
echo "🔍 Checking host dependencies..."

if ! command -v brew &> /dev/null; then
    echo "❌ Error: Homebrew is not installed. Please install it first from https://brew.sh"
    exit 1
fi

if ! command -v helm &> /dev/null; then
    echo "📦 Installing Helm..."
    brew install helm
else
    echo "✅ Helm is already installed."
fi

if ! command -v dapr &> /dev/null; then
    echo "📦 Installing Dapr CLI..."
    # For M1 Macs, we ensure the correct tap and architecture
    brew install dapr/tap/dapr-cli
else
    echo "✅ Dapr CLI is already installed."
fi

# --- 2. KUBERNETES CONTROL PLANE INSTALLATION ---
# Check for Dapr in-cluster
if ! kubectl get ns $NAMESPACE_DAPR > /dev/null 2>&1; then
    echo "🚀 Initializing Dapr in Kubernetes..."
    dapr init -k
else
    echo "✅ Dapr control plane already exists."
fi

# Check for Crossplane in-cluster
if ! kubectl get ns $NAMESPACE_XP > /dev/null 2>&1; then
    echo "🏗️ Installing Crossplane via Helm..."
    helm repo add crossplane-stable https://charts.crossplane.io/stable --force-update
    helm install crossplane crossplane-stable/crossplane --namespace $NAMESPACE_XP --create-namespace
else
    echo "✅ Crossplane control plane already exists."
fi

echo "⏳ Waiting for control planes to initialize (max 2 mins)..."
kubectl wait --for=condition=Ready pods --all -n $NAMESPACE_DAPR --timeout=120s
kubectl wait --for=condition=Ready pods --all -n $NAMESPACE_XP --timeout=120s

echo "🔧 Installing Crossplane Logic Functions..."
kubectl apply -f - <<EOF
apiVersion: pkg.crossplane.io/v1
kind: Function
metadata:
  name: function-patch-and-transform
spec:
  package: xpkg.upbound.io/crossplane-contrib/function-patch-and-transform:v0.7.0
EOF

# --- 3. INFRASTRUCTURE CONTRACT (XRD) ---
echo "📜 Applying Crossplane XRD..."
kubectl apply -k infra/base/crds/

echo "⏳ Waiting for API Registration ($XRD_NAME)..."
# This prevents the 'resource mapping not found' error by ensuring the API is live
until kubectl get xrd $XRD_NAME > /dev/null 2>&1; do 
    echo "   ...waiting for API server to register XRD..."
    sleep 3
done
kubectl wait --for=condition=Established xrd/$XRD_NAME --timeout=60s

echo "🏗️  Applying Kustomized Infrastructure (Runtime)..."
kubectl apply -k infra/base/runtime/

# --- 4. DOCKER BUILDS (M1 ARM64) ---
echo "🔨 Building Application Images..."
# Context is '.' so Docker finds the gradle/ folder for the TOML catalog
docker build -t agnostic-producer:local -f producer/Dockerfile ./producer
docker build -t agnostic-consumer:local -f consumer/Dockerfile ./consumer

# Tag them for the ephemeral registry (valid for 2 hours)
docker tag agnostic-producer:local ttl.sh/agnostic-producer-user99:2h
docker tag agnostic-consumer:local ttl.sh/agnostic-consumer-user99:2h

# Push them out to the internet
docker push ttl.sh/agnostic-producer-user99:2h
docker push ttl.sh/agnostic-consumer-user99:2h

# --- 5. LOGIC & CLAIMS (KUSTOMIZE) ---
# --- 1. BOOTSTRAP PLUMBING ---
echo "🏗️  Applying Phase 1: Plumbing..."
kubectl apply -k infra/overlays/local/phase-1-plumbing/

# --- 2. THE CRITICAL HANDSHAKE ---
echo "⏳ Waiting for GCP Provider to register ProviderConfig API..."
# We poll the API server until it recognizes the new Resource type
MAX_RETRIES=24
COUNT=0
until kubectl api-resources | grep -q "pubsub.gcp.upbound.io"; do
  echo "   ...waiting for Provider registration ($COUNT/$MAX_RETRIES)..."
  ((COUNT++))
  if [ $COUNT -ge $MAX_RETRIES ]; then
    echo "❌ Timeout: Provider failed to start. Check: kubectl get pods -n crossplane-system"
    exit 1
  fi
  sleep 5
done

# --- 3. APPLY LOGIC ---
echo "📜 Applying Phase 2: Infrastructure Logic & Claims..."
# We use -k here. Since the APIs are now registered, this will succeed.
kubectl apply -k infra/overlays/local/phase-2-logic/

# --- 6. APPLICATION DEPLOYMENT ---
echo "🚀 Deploying Service Folders..."
kubectl apply -f producer/k8s-deployment.yaml
kubectl apply -f consumer/k8s-deployment.yaml

# --- 7. VERIFICATION ---
echo "⏳ Waiting for Consumer Pod to be Ready..."
kubectl wait --for=condition=ready pod -l app=consumer --timeout=90s

echo "--------------------------------------------------"
echo "✨ ALL SYSTEMS GO!"
echo "Producer: http://localhost:8080/publish"
echo "Test the flow: ./burst-test.sh"
echo "--------------------------------------------------"

echo "✅ SUCCESS. Tunneling to Producer..."
kubectl port-forward deployment/producer 8080:8080
