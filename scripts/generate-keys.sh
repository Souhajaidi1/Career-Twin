#!/usr/bin/env sh
# Prints a fresh RSA-2048 key pair as the two single-line values the services expect:
#   JWT_PRIVATE_KEY  base64 PKCS#8 DER   (identity-service only)
#   JWT_PUBLIC_KEY   base64 X.509 DER    (every service)
# Append the output to .env. Never commit it.
set -eu
tmp="$(mktemp -d)"
trap 'rm -rf "$tmp"' EXIT
openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -out "$tmp/key.pem" 2>/dev/null
printf 'JWT_PRIVATE_KEY=%s\n' "$(openssl pkcs8 -topk8 -nocrypt -in "$tmp/key.pem" -outform DER 2>/dev/null | base64 | tr -d '\n')"
printf 'JWT_PUBLIC_KEY=%s\n' "$(openssl pkey -in "$tmp/key.pem" -pubout -outform DER 2>/dev/null | base64 | tr -d '\n')"
