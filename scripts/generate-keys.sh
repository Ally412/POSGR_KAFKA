#!/usr/bin/env bash
# Generate the RSA keypair used to sign/verify JWTs, if it doesn't already exist.
# Needed by local dev (bootRun/tests) and CI — privateKey.pem is gitignored, so a
# fresh clone / CI runner won't have it. Idempotent: skips if the key is already there.
# The keys are throwaway for dev/CI; production uses the sealed secret instead.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
CERTS="$ROOT/src/main/resources/certs"
PRIVATE="$CERTS/privateKey.pem"
PUBLIC="$CERTS/publicKey.pem"

if [ -f "$PRIVATE" ]; then
  echo "✓ RSA keypair already present ($PRIVATE) — skipping"
  exit 0
fi

mkdir -p "$CERTS"
openssl genpkey -algorithm RSA -out "$PRIVATE" -pkeyopt rsa_keygen_bits:2048   # PKCS#8 private
openssl rsa -in "$PRIVATE" -pubout -out "$PUBLIC"                              # X.509 public
echo "✓ generated RSA keypair in $CERTS"
