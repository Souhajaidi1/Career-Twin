# ADR 0001: Split into services, share two libraries

**Status:** accepted

## Context
Identity, career data and job matching change for different reasons and have different data
ownership. This is a personal engineering project, so the split is partly a deliberate
exercise in service boundaries.

## Decision
Three services behind a gateway, each with its own database. Two shared Maven modules:
`career-engine` (pure logic) and `security-lib` (token verification and error model).

## Consequences
- (+) Clear data ownership; identity can be hardened independently.
- (+) The engine is trivially unit-testable with no framework.
- (-) Shared libraries mean a change in `security-lib` rebuilds every service. Acceptable
  here; a larger system would version and publish them.
- (-) More moving parts than a modular monolith would need. A monolith would be the
  right call for a single team shipping this product; this repo chooses services to
  demonstrate the boundaries.
