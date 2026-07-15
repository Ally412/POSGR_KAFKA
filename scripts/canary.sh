#!/usr/bin/env bash
# Progressive canary via nginx Ingress traffic WEIGHTS (precise per-request %).
# Ramps the canary-weight annotation up, health-checking each step through the
# ingress. Rolls back on failure, promotes on success.
#
# Usage: scripts/canary.sh <namespace> <new-image-tag>
#   tune: STEPS="10 20 50 100" PAUSE=15 HOST=shelter.local scripts/canary.sh production v2
set -euo pipefail

NS="${1:?usage: canary.sh <namespace> <new-image-tag>}"
TAG="${2:?usage: canary.sh <namespace> <new-image-tag>}"

STABLE=shelter
CANARY=shelter-canary
STEPS="${STEPS:-10 20 50 100}"    # canary TRAFFIC weights (% of requests)
PAUSE="${PAUSE:-15}"             # seconds to observe between steps
HOST="${HOST:-shelter.local}"    # ingress hostname to health-check against
REPLICAS="${REPLICAS:-2}"        # fixed canary pods — WEIGHT controls traffic, not pod count

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
MIP="$(minikube ip)"

echo "▶ Ingress canary of shelter:$TAG in '$NS' (host=$HOST, steps: $STEPS %)"

# Bring up the canary Deployment + Service on the NEW image, and the canary Ingress (weight 0).
kubectl apply -n "$NS" -f "$ROOT/k8s/canary.yaml"
kubectl set image -n "$NS" deployment/$CANARY shelter="shelter:$TAG"
kubectl scale  -n "$NS" deployment/$CANARY --replicas="$REPLICAS"
kubectl apply  -n "$NS" -f "$ROOT/k8s/ingress-canary.yaml"
kubectl rollout status -n "$NS" deployment/$CANARY --timeout=120s

# Set the canary traffic weight (%).
set_weight() {
  kubectl annotate -n "$NS" ingress $CANARY \
    nginx.ingress.kubernetes.io/canary-weight="$1" --overwrite >/dev/null
}

# Hit the ingress 20x; ALL must be 2xx (some requests land on the canary by weight).
health_ok() {
  local ok=1
  for i in $(seq 1 20); do
    curl -fsS --max-time 5 -H "Host: $HOST" "http://$MIP/api/animals" >/dev/null || { ok=0; break; }
  done
  return $(( 1 - ok ))
}

rollback() {
  echo "✗ health check FAILED — rolling back (canary weight 0)"
  set_weight 0
  kubectl scale -n "$NS" deployment/$CANARY --replicas=0
  exit 1
}

# Ramp the weight up, verifying at each level.
for w in $STEPS; do
  echo "▶ shifting ${w}% of traffic to canary"
  set_weight "$w"
  sleep 3                       # let nginx apply the annotation
  health_ok || rollback
  echo "✓ ${w}% healthy"
  sleep "$PAUSE"
done

# Success → bake the new image into stable, then tear down the canary overlay.
echo "▶ promoting shelter:$TAG to stable"
kubectl set image -n "$NS" deployment/$STABLE shelter="shelter:$TAG"
kubectl rollout status -n "$NS" deployment/$STABLE --timeout=180s
set_weight 0
kubectl scale -n "$NS" deployment/$CANARY --replicas=0
echo "✓ canary complete — shelter:$TAG is now stable in $NS"
