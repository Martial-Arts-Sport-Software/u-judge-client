---
name: u-judge-client-transport
description: Use when implementing U'Judge mDNS discovery, HTTP handshake, pairing, WebSocket transport, ACK, reconnect, or durable outbox.
---

# U'Judge Client Transport

Read `docs/REQUIREMENTS.md` sections 3, 5, 7, 8, and 10, the increment plan and known server divergences in
`docs/ROADMAP.md`, and server ADR-004 before implementation.

## Connection lifecycle

- Selecting an mDNS service never means connected. Online state is reached only after metadata validation, protocol/capability negotiation, handshake, and pairing acceptance.
- Model connection as an explicit state machine. Keep discovery, transport, pairing, session, navigation, and rating-draft state separate.
- Run one discovery job at a time. Re-scan must cancel or reuse the existing job, deduplicate services, and remove unavailable services.
- Include a stable device identity and nonblank judge surname in pairing. Do not send judging events before pairing is accepted.

## Server contract

- Message types and fields must match the server contract (ADR-004 and server contract tests in `u-judge-server`).
  A client-only message shape is a defect even when fake-server tests pass.
- Scoring commands use the server's typed command (for Kerugi, `kerugi_score_command`) with the audit context from the
  server session snapshot. A generic `command` is acknowledged by the server without scoring.
- Handle server publications (`session_state_updated`, `kerugi_score_updated`, `resync_response`) as the only source of
  session and score truth.

## Event delivery

- Create a unique event ID, client sequence, and timestamp for each physical action.
- Persist an outgoing event before sending it. Retry uses the exact same event ID.
- Remove an event from the outbox only after terminal ACK; terminal rejection is retained with a localized reason and is not retried forever.
- Use bounded exponential backoff, heartbeat, reconnect resync, and clock offset negotiation.
- Never show an action as accepted until the server ACK or confirmed state update arrives.

## Validation

Run the increment scenario against a server started with `./gradlew :desktop:run` from `u-judge-server`. Test duplicate delivery, delayed ACK, disconnect during send, restart with pending outbox, version mismatch, pairing reject, and reconnect resync on physical Android and iPhone devices.
