---
name: u-judge-client-rating
description: Use when changing U'Judge combat buttons, technical criteria, ratings, Save, Send, score feedback, or discipline screens.
---

# U'Judge Client Rating

Read `docs/REQUIREMENTS.md` sections 5-6 and `docs/PROJECT.md` before changing scoring UI or models.

## Rules

- The server is the source of official score and quorum calculation. Client combat buttons only create typed events.
- Kerugi and Tanbon event values: `HEAD = 2`, `BODY = 1`; Tanbon `CROSS` is neutral and audit-only.
- Technical criteria accept only `0.1..1.0` in `0.1` increments. Preserve raw criteria, extra points, and calculated total.
- Hosinsool and Pair use 4 technical criteria for Juniors and 6 for Adults; Group and Weapon use 6. Presentation uses 4 criteria.

## Save and Send

- `Save` writes only a local draft and must work offline. It never sends a network request.
- `Send` is available only for a paired active online session, needs explicit confirmation, and creates an immutable final rating payload.
- A final payload survives disconnect in the outbox. After ACK, make that rating read-only.
- Clearly render pending, accepted, rejected, and offline states without color alone.

## Tests

Add boundary tests for criteria range, rounding, Junior/Adult structures, extra points, and all total calculations. Add contract tests before binding a new UI action to transport.
