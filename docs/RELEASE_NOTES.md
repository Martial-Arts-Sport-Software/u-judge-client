# U'Judge Client 0.1.0 Pilot

Статус релиза: pilot для полевого тестирования. Это не production-релиз.

## Ограничения

- mDNS discovery доступен: повторный поиск отменяет предыдущий scan, а удалённые сервисы исчезают из списка. HTTP metadata/pairing request, manual host/IP fallback, one-shot typed pairing-status reader and shared WebSocket handshake готовы, но secure credential storage, UI wiring, status polling/push, TLS trust UX и realtime lifecycle ещё не реализованы.
- Kerugi и Tanbon не отправляют официальные события: кнопки пока не имеют обработчиков.
- `Save` и `Send` технических оценок пока не реализованы.
- Shared durable outbox and terminal WebSocket command ACK/rejection handling готовы; UI event wiring, retry lifecycle, reconnect and session resync ещё не реализованы.
- Для v1 Pilot минимальная версия iOS — 18; iOS 18 и iOS 26 требуют physical-device smoke tests. Android 5.1/TZ55
  deferred to a separate lightweight client track after v1.
- English UI/rules deferred beyond v1; Russian rules are the Pilot baseline.

Полное текущее состояние и план работ приведены в [PROJECT.md](PROJECT.md) и [ROADMAP.md](ROADMAP.md).
