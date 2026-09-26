# U'Judge Client: принятые protocol decisions

Статус: принято для v1 Pilot, 31 августа 2026 года.

Канонические system ADR находятся в server repository:

- [ADR-002: P2P Discovery, Join, and Anti-Entropy](https://github.com/Martial-Arts-Sport-Software/u-judge-server/blob/main/docs/adr/ADR-002-p2p-discovery-join-anti-entropy.md)
- [ADR-003: Managed PostgreSQL Persistence](https://github.com/Martial-Arts-Sport-Software/u-judge-server/blob/main/docs/adr/ADR-003-managed-postgresql.md)
- [ADR-004: HTTP/WebSocket Contract and Version Negotiation](https://github.com/Martial-Arts-Sport-Software/u-judge-server/blob/main/docs/adr/ADR-004-http-websocket-contract.md)
- [ADR-006: Local TLS And Mobile Trust](https://github.com/Martial-Arts-Sport-Software/u-judge-server/blob/main/docs/adr/ADR-006-local-tls-trust.md)

Этот документ определяет только обязанности mobile client. Desktop peer identity, P2P replication и managed PostgreSQL
реализуются server application.

## Подключение и pairing

- Client сначала ищет площадки через mDNS, но может использовать manual host/IP fallback.
- Выбор площадки не даёт online state. Client запрашивает `GET /v1/metadata`, проверяет protocol major version и required
  capabilities, затем начинает pairing.
- Metadata содержит protocol version, capabilities, desktop peer/court ID, server name, pairing policy и server time.
- Каждый новый mobile device требует explicit operator approval. После approval client хранит reconnect credential только в
  platform secure storage, до server-side revocation или credential rotation.
- Client подключается только по HTTPS/WSS (по умолчанию порт `8443`) и доверяет ключу server по SPKI pin (ADR-006): при
  первом контакте ключ запоминается, а экран pairing показывает 6-значный код, который оператор сверяет с desktop до
  подтверждения. После approval pin хранится вместе с endpoint и credential в platform secure storage; другой ключ даёт
  terminal `server_identity_changed` без отправки credential, выход - явное «Забыть сервер» и новый pairing.
- Credential запрашивается с delivery proof в заголовке `X-UJudge-Pairing-Delivery-Proof`.
- После перезапуска приложения client восстанавливает paired server и переподключается без нового pairing, повторяя
  попытки с backoff 1-30 с; `invalid_reconnect_credential` означает отзыв устройства: credential и pairing удаляются.
- Форма сообщений проверяется по contract fixtures server (`contract/server-ref`, `scripts/sync-server-contract.sh`);
  расхождение останавливает CI.

## Realtime contract

- Paired client использует один authenticated WebSocket.
- Typed messages: handshake, pairing status, session snapshot, command/event, ACK, rejection, heartbeat, resync
  request/response и server notice.
- Каждое физическое нажатие получает stable event ID, client sequence, client timestamp, session ID и typed payload до
  durable outbox write.
- `ACK` terminal только после durable journal commit на server. Transport receipt и последующий snapshot не являются ACK.
- После reconnect client передаёт cursor, принимает resync и current active-session snapshot; combat controls остаются
  disabled до завершения этого flow.

## Время и Kerugi

- Shared `ClockSyncClient` отправляет `clock_sync` с ISO-8601 UTC `clientSendTimestamp`, принимает echo с UTC `serverReceiveTimestamp`/`serverSendTimestamp` и вычисляет server/client offset и round-trip по four-timestamp formula. Typed `clock_sync_rejected` возвращает server code; malformed, incomplete или non-matching echo возвращает invalid response. Handshake/reconnect lifecycle wiring остаётся отдельной задачей.
- Client передаёт timestamp, скорректированный по согласованному offset; server, а не порядок WebSocket delivery, применяет
  coincidence window.
- Default Kerugi coincidence window равен `1000 мс`.
- Client никогда не вычисляет quorum или итоговый score. Для одного участника server разрешает все конфликтующие score
  candidates в одном coincidence window по минимальной оценке. Например, кандидаты `1` и `2` дают итоговый `1`.
- Client показывает только `pending`, `accepted` или `rejected` outcome, полученный от server; исходные события и resolution
  остаются доступны server audit.

## Required Evidence

- Incompatible protocol version, capability, endpoint или certificate не переводят client в online state.
- Unapproved/revoked device cannot write an event.
- Retry и reconnect используют исходный event ID; app kill не теряет durable pending event.
- Client-side tests cover delayed ACK, duplicate/reordered transport, resync-before-controls, and clock-offset timestamping.
- Contract tests prove server minimum-score resolution for conflicting Kerugi candidates in the `1000 мс` window.
