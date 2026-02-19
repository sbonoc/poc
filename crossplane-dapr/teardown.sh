#!/bin/bash

echo "🛑 Initiating graceful teardown of the Agnostic environment..."

echo "1️⃣ Removing Ktor Applications..."
kubectl delete -f producer/k8s-deployment.yaml --ignore-not-found=true
kubectl delete -f consumer/k8s-deployment.yaml --ignore-not-found=true

echo "2️⃣ Removing Infrastructure Claims (The Graceful Way)..."
# We delete the claim explicitly first so Crossplane can talk to the emulator to delete the topic
kubectl delete -f infra/overlays/local/phase-2-logic/bus-claim.yaml --ignore-not-found=true

echo "⏳ Waiting for Crossplane to cleanly unbind and destroy the topic..."
kubectl wait --for=delete pubsubtopic.agnostic.systems/orders-bus --timeout=60s || true

echo "3️⃣ Removing Emulators, Compositions, and Dapr Components..."
# Now it is safe to kill the emulator and the routing rules
kubectl delete -k infra/overlays/local/phase-2-logic/ --ignore-not-found=true

echo "4️⃣ Removing Crossplane Plumbing (Providers & Functions)..."
# Optional: This removes the heavy Crossplane provider pod and functions
kubectl delete -k infra/overlays/local/phase-1-plumbing/ --ignore-not-found=true

echo "5️⃣ Removing the Infrastructure Runtime..."
kubectl delete -k infra/base/runtime/ --ignore-not-found=true

echo "6️⃣ Removing the Infrastructure Contract (XRDs)..."
kubectl delete -k infra/base/crds/ --ignore-not-found=true

echo "7️⃣ Uninstalling Crossplane Engine & Scrubbing CRDs..."
# Uninstall via Helm
helm uninstall crossplane --namespace crossplane-system 2>/dev/null || true
# Delete the namespace
kubectl delete namespace crossplane-system --ignore-not-found=true || true
# Scrub leftover Crossplane CRDs
echo "🧹 Scrubbing Crossplane CRDs..."
kubectl get crds -o name | grep "\.crossplane\.io" | xargs -n 1 kubectl delete 2>/dev/null || true
kubectl get crds -o name | grep "\.upbound\.io" | xargs -n 1 kubectl delete 2>/dev/null || true

echo "8️⃣ Uninstalling Dapr Control Plane..."
# The Dapr CLI has a built-in clean uninstall flag for Kubernetes
if command -v dapr &> /dev/null; then
    dapr uninstall -k --all
else
    echo "⚠️ Dapr CLI not found, skipping Dapr uninstall."
fi
# Ensure the namespace is wiped
kubectl delete namespace dapr-system --ignore-not-found=true || true

echo "--------------------------------------------------"
echo "✅ TEARDOWN COMPLETE. Your Mac's RAM and battery are safe! 🔋"
echo "--------------------------------------------------"