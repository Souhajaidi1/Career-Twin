# ADR 0002: RS256 with a single signer

**Status:** accepted

## Decision
identity-service signs tokens with a private key; every other service verifies with the
public key only. Issuer is `careertwin-identity`; role is the `role` claim.

## Consequences
- (+) A compromised downstream service cannot mint tokens.
- (+) No shared secret to rotate across services.
- (-) Key rotation (JWKS, `kid`) is not implemented. Keys are injected via environment
  variables; `scripts/generate-keys.sh` creates a pair for local use.
- The gateway only routes. Each service enforces authentication itself, so bypassing
  the gateway does not bypass security.
