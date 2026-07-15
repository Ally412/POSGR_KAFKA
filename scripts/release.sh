#!/usr/bin/env bash
# Release pipeline: deploy to STAGING, verify, then (on approval) PRODUCTION.
# Usage: scripts/release.sh [image-tag]
set -euo pipefail

TAG="${1:-local}"
DIR="$(cd "$(dirname "$0")" && pwd)"

# 1. STAGING — full deploy + smoke test (the smoke test lives inside deploy.sh)
echo "==================== STAGING ===================="
"$DIR/deploy.sh" staging "$TAG"

# 2. Manual gate
echo
read -rp "Staging looks good. Promote shelter:$TAG to PRODUCTION? [y/N] " ans
[[ "${ans:-}" == "y" || "${ans:-}" == "Y" ]] || { echo "Aborted — production untouched."; exit 0; }

# 3. PRODUCTION — canary if a version is already live, else first-time baseline deploy
echo "==================== PRODUCTION ===================="
if kubectl get deployment shelter -n production >/dev/null 2>&1; then
  echo "(existing version live → gradual canary)"
  "$DIR/canary.sh" production "$TAG"
else
  echo "(first production release → baseline deploy)"
  "$DIR/deploy.sh" production "$TAG"
fi
echo "✓ Released shelter:$TAG to production"
