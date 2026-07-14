#!/usr/bin/env bash
# Progressive canary rollout via replica ratios (DIY, no extra tools).
# Runs the NEW image on a growing share of pods behind the shared "shelter"
# Service, health-checking between steps. Rolls back on failure, promotes on success.
#
# Usage: scripts/canary.sh <namespace> <new-image-tag>
#   e.g. scripts/canary.sh staging v2
#   tune: TOTAL=4 STEPS="1 2 3 4" PAUSE=15 scripts/canary.sh staging v2
set -euo pipefail

NS="${1:?usage: canary.sh <namespace> <new-image-tag>}"
TAG="${2:?usage: canary.sh <namespace> <new-image-tag>}"

STABLE=shelter
CANARY=shelter-canary
TOTAL="${TOTAL:-4}"            # total app pods across stable + canary
STEPS="${STEPS:-1 2 3 4}"     # canary pod counts to step through (out of TOTAL)
PAUSE="${PAUSE:-15}"          # seconds to observe between steps

ROOT="$(cd "$(dirname "$0")/.." && pwd)"

echo "▶ Canary rollout of shelter:$TAG in '$NS' (total=$TOTAL, steps: $STEPS)"

# Make sure the canary Deployment exists and runs the NEW image.
kubectl apply -n "$NS" -f "$ROOT/k8s/canary.yaml"
kubectl set image -n "$NS" deployment/$CANARY shelter="shelter:$TAG"

# Hit the shared Service 10x; ALL must return 2xx (some land on canary, some on stable).
health_ok() {
  kubectl port-forward -n "$NS" svc/shelter 18080:8080 >/tmp/pf-canary.log 2>&1 &
  local pf=$! ok=1
  for i in $(seq 1 10); do
    curl -fsS --retry 5 --retry-connrefused --retry-delay 1 --max-time 5 \
      http://localhost:18080/api/animals >/dev/null || { ok=0; break; }
  done
  kill $pf 2>/dev/null || true
  return $(( 1 - ok ))
}

rollback() {
  echo "✗ health check FAILED — rolling back to 100% stable"
  kubectl scale -n "$NS" deployment/$CANARY --replicas=0
  kubectl scale -n "$NS" deployment/$STABLE --replicas="$TOTAL"
  exit 1
}

# Step through the ratios: grow canary, shrink stable, verify each time.
for c in $STEPS; do
  s=$(( TOTAL - c ))
  echo "▶ step: canary=$c / stable=$s  (~$(( 100 * c / TOTAL ))% on new)"
  kubectl scale -n "$NS" deployment/$CANARY --replicas="$c"
  kubectl scale -n "$NS" deployment/$STABLE --replicas="$s"
  kubectl rollout status -n "$NS" deployment/$CANARY --timeout=120s
  [ "$s" -gt 0 ] && kubectl rollout status -n "$NS" deployment/$STABLE --timeout=120s
  health_ok || rollback
  echo "✓ step healthy"
  sleep "$PAUSE"
done

# Success → bake the new image into stable, retire the canary.
echo "▶ promoting shelter:$TAG to stable"
kubectl set image -n "$NS" deployment/$STABLE shelter="shelter:$TAG"
kubectl scale -n "$NS" deployment/$STABLE --replicas="$TOTAL"
kubectl rollout status -n "$NS" deployment/$STABLE --timeout=180s
kubectl scale -n "$NS" deployment/$CANARY --replicas=0
echo "✓ canary complete — shelter:$TAG is now stable in $NS"
