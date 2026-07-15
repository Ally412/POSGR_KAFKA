#!/usr/bin/env bash
# Deploy the shelter stack (Postgres + app) to ONE namespace.
# Usage: scripts/deploy.sh <namespace> [image-tag]
#   e.g. scripts/deploy.sh staging          # uses shelter:local
#        scripts/deploy.sh production v1.2   # uses shelter:v1.2
set -euo pipefail

NS="${1:?usage: deploy.sh <namespace> [image-tag]}"
TAG="${2:-local}"
ROOT="$(cd "$(dirname "$0")/.." && pwd)"   # repo root, regardless of where script is run from

echo "▶ Deploying to '$NS' with image shelter:$TAG"

# 1. namespace exists?
kubectl get ns "$NS" >/dev/null 2>&1 || kubectl create ns "$NS"

# 2. credentials — the PER-NAMESPACE sealed secret (namespace-scoped, remember)
kubectl apply -n "$NS" -f "$ROOT/k8s/sealed-secret-$NS.yaml"

# 3. database
kubectl apply -n "$NS" -f "$ROOT/k8s/postgres.yaml"

# 4. app — apply the manifest, then pin THIS release's image tag
kubectl apply -n "$NS" -f "$ROOT/k8s/app.yaml"
kubectl set image -n "$NS" deployment/shelter shelter="shelter:$TAG"

# 4b. the front door — main Ingress (routes shelter.local → stable Service)
kubectl apply -n "$NS" -f "$ROOT/k8s/ingress.yaml"

# 5. wait until both are actually ready (readiness probes gate this)
kubectl rollout status -n "$NS" deployment/postgres --timeout=120s
kubectl rollout status -n "$NS" deployment/shelter  --timeout=180s

# 6. smoke test — prove the API actually answers before calling it a success
echo "▶ Smoke test against $NS ..."
kubectl port-forward -n "$NS" svc/shelter 18080:8080 >/tmp/pf-$NS.log 2>&1 &
PF=$!
trap 'kill $PF 2>/dev/null' EXIT
if curl -fsS --retry 15 --retry-delay 1 --retry-all-errors http://localhost:18080/api/animals >/dev/null; then
  echo "✓ $NS is healthy (GET /api/animals OK)"
else
  echo "✗ $NS smoke test FAILED"; exit 1
fi
