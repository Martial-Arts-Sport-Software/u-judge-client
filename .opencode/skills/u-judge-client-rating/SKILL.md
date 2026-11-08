---
name: u-judge-client-rating
description: Use when changing U'Judge combat buttons, technical criteria, ratings, Save, Send, score feedback, or discipline screens.
---

# U'Judge Client Rating

Read `docs/REQUIREMENTS.md` sections 5-6, `docs/PROJECT.md` and the server `docs/FHR-RULES-2024.md` before changing
scoring UI or models.

## Rules

- The server is the source of official score and quorum calculation. Client combat buttons only create typed events.
- Kerugi and Tanbon event values: `HEAD = 2`, `BODY = 1`; Tanbon `CROSS` is neutral and audit-only.
- Technical criteria accept only `0.1..1.0` in `0.1` increments. Preserve raw criteria, extra points, and calculated total.
- Tanbon is a product discipline absent from FHR rules; it is judged through server ADR-011 with `tanbon_score_command`.
- Hosinsool and Pair use 4 technical criteria for Juniors and 6 for Adults; Group and Weapon use 6. Presentation uses 4 criteria.
  Online, the server snapshot's `criteria.technique` decides the count.
- Totals are integer tenths, `max(0, ΣT + ΣP − D)`, exactly as the server; test against the server fixture
  `technical_test_vectors`, never a `Float` sum.

## Save and Send

- `Save` writes only a local draft and must work offline. It never sends a network request.
- `Send` is available only for a seated judge of the running server performance of the screen's discipline, needs explicit
  confirmation, and creates one immutable `technical_score_send_command` with the raw criteria, deduction and local total.
- A final payload survives disconnect in the outbox. After ACK, make that rating read-only.
- Clearly render pending, accepted, rejected, and offline states without color alone.

## Tests

Add boundary tests for criteria range, rounding, Junior/Adult structures, extra points, and all total calculations. Add contract tests before binding a new UI action to transport.
