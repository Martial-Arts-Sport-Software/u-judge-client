# U'Judge Client 0.1.0 Pilot

Статус релиза: pilot для полевого тестирования. Это не production-релиз.

## Ограничения

- mDNS discovery доступен: повторный поиск отменяет предыдущий scan, а удалённые сервисы исчезают из списка. HTTP/WebSocket handshake и pairing ещё не реализованы.
- Kerugi и Tanbon не отправляют официальные события: кнопки пока не имеют обработчиков.
- `Save` и `Send` технических оценок пока не реализованы.
- Durable outbox, ACK, retry, reconnect и session resync отсутствуют.
- Минимальная версия iOS и compatibility matrix pilot devices ещё не зафиксированы.

Полное текущее состояние и план работ приведены в [PROJECT.md](PROJECT.md) и [ROADMAP.md](ROADMAP.md).
