#!/usr/bin/env bash
# One-time cluster setup for the shelter project.
# Idempotent — safe to re-run (e.g. after `minikube delete` wipes everything).
#   1. installs the Sealed Secrets controller (if missing)
#   2. creates the staging + production namespaces
#   3. seals per-namespace DB credentials into k8s/sealed-secret-<ns>.yaml
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

# 2 + 3. Each namespace and its own sealed secret.
for NS in "${NAMESPACES[@]}"; do
  kubectl get ns "$NS" >/dev/null 2>&1 || kubectl create ns "$NS"
  kubectl create secret generic postgres-secret \
    --from-literal=POSTGRES_DB="$DB_NAME" \
    --from-literal=POSTGRES_USER="$DB_USER" \
    --from-literal=POSTGRES_PASSWORD="$DB_PASSWORD" \
    --namespace="$NS" --dry-run=client -o yaml \
    | kubeseal --format yaml > "$ROOT/k8s/sealed-secret-$NS.yaml"
  echo "✓ namespace '$NS' ready + k8s/sealed-secret-$NS.yaml sealed"
done

echo "✓ bootstrap complete"
