# ADR 0003: Scores are computed, never generated

**Status:** accepted

## Decision
All scores come from `career-engine`, a deterministic function of the user's evidence.
No language model produces or adjusts a score.

Each evidence item contributes points by type (capped per type), weighted by recency;
a skill level is `100 * (1 - exp(-sum / 70))`, capped at 99, so extra evidence has
diminishing returns. Readiness blends coverage, experience fit, evidence depth and
practice, scaled by a seniority multiplier.

## Trust rules
- A self-reported claim is worth less than a built project, which is worth less than a
  passed assessment.
- `assessment` and `interview` evidence is reserved: it cannot be created through the
  API by hand, and origin is derived from type on the server, not supplied by the client.
- Every contribution is kept so any number can be explained and replayed.

## Consequences
The weights are judgement calls, not validated against hiring outcomes. The tests pin
relationships (claim < project, recency, monotonicity), not specific numbers.
