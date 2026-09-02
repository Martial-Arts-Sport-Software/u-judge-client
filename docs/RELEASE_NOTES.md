# U'Judge Client 0.1.0 Pilot

Статус релиза: pilot для полевого тестирования. Это не production-релиз.

## Ограничения

- mDNS discovery доступен: повторный поиск отменяет предыдущий scan, а удалённые сервисы исчезают из списка. Для fallback после поиска доступен manual host/IP: endpoint проходит HTTP metadata, protocol/capability validation и pairing request, но не получает online state без pairing acceptance и authenticated clock sync. TLS trust UX, secure credential storage и realtime lifecycle ещё не реализованы.
- Kerugi и Tanbon не отправляют официальные события: кнопки пока не имеют обработчиков.
- `Save` и `Send` технических оценок пока не реализованы.
- Durable outbox, ACK, retry, reconnect и session resync отсутствуют.
- Для v1 Pilot минимальная версия iOS — 18; iOS 18 и iOS 26 требуют physical-device smoke tests. Android 5.1/TZ55
  deferred to a separate lightweight client track after v1.
- English UI/rules deferred beyond v1; Russian rules are the Pilot baseline.

Полное текущее состояние и план работ приведены в [PROJECT.md](PROJECT.md) и [ROADMAP.md](ROADMAP.md).
