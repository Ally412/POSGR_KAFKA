#!/usr/bin/env bash
# One-time cluster setup for the shelter project.
# Idempotent — safe to re-run (e.g. after `minikube delete` wipes everything).
#   1. installs the Sealed Secrets controller (if missing)
#   2. enables the nginx ingress controller
#   3. creates the staging + production namespaces
#   4. seals per-namespace DB credentials + RSA signing keys into k8s/sealed-secret-*.yaml
#   5. applies the LoadBalancer that fronts the ingress controller
#
# Usage:  scripts/bootstrap.sh
#         DB_PASSWORD='s3cr3t' scripts/bootstrap.sh   # override the default creds
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
NAMESPACES=(staging production)
SEALED_SECRETS_VERSION="v0.38.4"

# Credentials — override via env for real secrets; defaults are the local throwaway.
DB_NAME="${DB_NAME:-shelter}"
DB_USER="${DB_USER:-shelter}"
DB_PASSWORD="${DB_PASSWORD:-shelter}"

# Fail early with a clear message if the CLI isn't installed.
command -v kubeseal >/dev/null || { echo "✗ kubeseal not on PATH (we installed it to ~/.local/bin)"; exit 1; }

# 1. Sealed Secrets controller — install once if it's not already there.
if ! kubectl get deployment sealed-secrets-controller -n kube-system >/dev/null 2>&1; then
  echo "▶ Installing Sealed Secrets controller $SEALED_SECRETS_VERSION"
  kubectl apply -f "https://github.com/bitnami-labs/sealed-secrets/releases/download/$SEALED_SECRETS_VERSION/controller.yaml"
  kubectl rollout status deployment/sealed-secrets-controller -n kube-system --timeout=120s
else
  echo "✓ Sealed Secrets controller already installed"
fi

# 2. nginx ingress controller — the L7 front door (minikube ships it as an addon;
#    on a real cluster you'd install ingress-nginx via helm/manifest instead).
if ! kubectl get deployment ingress-nginx-controller -n ingress-nginx >/dev/null 2>&1; then
  echo "▶ Enabling ingress addon"
  minikube addons enable ingress
else
  echo "✓ ingress controller already installed"
fi

# Ensure the RSA signing keys exist locally so we can seal them into the cluster.
bash "$ROOT/scripts/generate-keys.sh"

# 3 + 4. Each namespace with its own sealed secrets (DB creds + RSA signing keys).
for NS in "${NAMESPACES[@]}"; do
  kubectl get ns "$NS" >/dev/null 2>&1 || kubectl create ns "$NS"
  kubectl create secret generic postgres-secret \
    --from-literal=POSTGRES_DB="$DB_NAME" \
    --from-literal=POSTGRES_USER="$DB_USER" \
    --from-literal=POSTGRES_PASSWORD="$DB_PASSWORD" \
    --namespace="$NS" --dry-run=client -o yaml \
    | kubeseal --format yaml > "$ROOT/k8s/sealed-secret-$NS.yaml"
  kubectl create secret generic rsa-keys \
    --from-file=privateKey.pem="$ROOT/src/main/resources/certs/privateKey.pem" \
    --from-file=publicKey.pem="$ROOT/src/main/resources/certs/publicKey.pem" \
    --namespace="$NS" --dry-run=client -o yaml \
    | kubeseal --format yaml > "$ROOT/k8s/sealed-secret-rsa-$NS.yaml"
  echo "✓ namespace '$NS' ready + DB & RSA sealed secrets"
done

# 5. External LoadBalancer fronting the ingress controller.
kubectl apply -f "$ROOT/k8s/lb.yaml"
echo "✓ LoadBalancer applied (run 'minikube tunnel' to get its EXTERNAL-IP)"

echo "✓ bootstrap complete"
