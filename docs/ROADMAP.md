# U'Judge Client v1 Pilot: roadmap

Статус документа: клиентский поток канонического 12-недельного плана U'Judge v1 Pilot.

Дата фиксации: 30 августа 2026 года.

Общий порядок гейтов и release scope определены в [
`u-judge-server/docs/ROADMAP.md`](https://github.com/Martial-Arts-Sport-Software/u-judge-server/blob/main/docs/ROADMAP.md).
Недели ниже синхронизированы с server roadmap и не являются отдельным сроком поверх него.

## 1. Цель

Превратить текущий Compose Multiplatform UI-прототип в надёжный мобильный судейский пульт, который:

- устанавливается на Android/iPhone pilot devices;
- обнаруживает площадку без ручного IP;
- проходит подтверждённый pairing;
- не теряет и не дублирует нажатия при reconnect;
- отправляет окончательные технические оценки;
- работает как offline-калькулятор технических дисциплин;
- ясно различает локальное действие и server ACK.

## Статус выполнения на `main`

Статус сверяется только с влитыми в `main` изменениями и их тестами. Частично выполненная неделя не закрывает gate.

- [x] Gate C0: baseline готов. GitHub Actions `Verify` на `main` подтверждает Android build/tests и iOS framework compilation; physical-device smoke относится к release/pilot acceptance, а не к C0.
- [ ] Gate C1: discovery lifecycle готов частично; handshake, pairing и reconnect не готовы.
- [ ] Gate C2: не готов.
- [ ] Gate C3: не готов.
- [ ] Gate C4: не готов.
- [ ] Gate C5: не готов.
- [ ] Gate C6: не готов.
- [ ] Gate C7: не готов.
- [ ] Gate C8: не готов.

## Принятые protocol decisions

- [x] Требования client/server metadata, pairing, TLS, WebSocket, durable ACK, resync и clock synchronization зафиксированы в [PROTOCOL-DECISIONS.md](PROTOCOL-DECISIONS.md); implementation evidence ещё не готово.
- [x] Kerugi conflict resolution использует `1000 мс` coincidence window и minimum-score policy на server; client не вычисляет итоговый score. Implementation evidence ещё не готово.
- [x] v1 device/language scope определён: iOS 18 minimum с тестом на iOS 26; Android 5.1/TZ55 и English UI deferred beyond v1. Implementation evidence ещё не готово.

## 2. Неделя 1: baseline и тестовая основа

- [x] Зафиксировать актуальную ветку `feat/server-connection` как исходную точку.
- [ ] Согласовать protocol DTO и requirement IDs с server.
- [x] Добавлен CI для Android/shared tests и iOS framework compilation (#17).
- [x] Закреплены Java 21 и воспроизводимые Gradle-команды (#17).
- [x] Добавлены unit tests существующих `TechniqueCriteria`, `PresentationCriteria` и `TechniqueRating` (#6).
- [x] Release naming использует `0.1.0` для pilot, а не production-ready `1.0` (#8).
- [x] Зафиксировать v1 device baseline: iOS 18 minimum, iOS 26 compatibility smoke scope; Android 5.1/TZ55 deferred. Physical-device evidence требуется для release/pilot acceptance.

### Gate C0

Shared/Android/iOS targets собираются в CI, формулы текущих моделей имеют test baseline, а локальные
незавершённые изменения сохранены.

## 3. Недели 1-2: discovery и realtime spike

- [x] Управлять единственной mDNS discovery job и её lifecycle.
- [x] Показывать понятные имя площадки, адрес и статус (`CLI-013`; resolved и resolving состояния покрыты shared unit tests).
- [ ] Реализовать HTTP metadata/handshake, protocol version/capability check и manual host/IP fallback (resolved mDNS вызывает shared HTTP client и validation domain model; TLS/manual fallback pending).
- [ ] Реализовать WebSocket connect, heartbeat и typed envelope (shared Ktor handshake, typed command envelope и terminal ACK/rejection outbox handling готовы; credential storage, lifecycle wiring, heartbeat and reconnect pending).
- [ ] Получать pairing pending/accepted/rejected (UI показывает pending и локальные ошибки после shared HTTP request; operator accepted/rejected realtime status pending).
- [ ] Согласовать clock offset (shared `ClockSyncClient` оценивает offset/round-trip по одному typed exchange и отклоняет invalid/rejected responses; handshake/reconnect wiring pending).
- [ ] Отправить событие, получить ACK, разорвать сеть и повторить тот же ID (shared command/ACK contract сохраняет stable ID and terminal outcome; disconnect/reconnect proof pending).
- [ ] Проверить iOS Local Network permission и mDNS на TestFlight-like build.

### Gate C1

Физические Android и iPhone обнаруживают server, проходят pairing и доставляют событие ровно один
раз после искусственного reconnect.

## 4. Недели 2-3: state и durable outbox

- [x] Заменить `State.isConnectedToServer` connection state machine (`CLI-070`; переходы покрыты shared unit tests).
- Отделить navigation, pairing, session и rating draft state.
- [ ] Ввести локальное durable storage для identity, settings, drafts и outbox (shared outbox journal уже сохраняет pending/rejected records в platform storage; wiring domain events, drafts и settings pending).
- [ ] Добавить event ID, client sequence, timestamp и retry metadata (shared outbox model и retry metadata готовы; typed transport payload pending).
- [ ] Реализовать bounded exponential backoff и terminal rejection (shared ordered retry и terminal rejection готовы; transport integration pending).
- Восстанавливать active connection/session после lifecycle events.
- Локализовать типизированные transport/protocol errors.

### Gate C2

Неподтверждённое событие переживает app kill, отправляется с исходным ID после запуска и исчезает из
outbox только после terminal ACK.

## 5. Недели 4-5: Kerugi vertical slice

- Подключить четыре текущие combat buttons к typed events.
- Получать current bout, blue/red labels и session state от server.
- Блокировать ввод вне `running`.
- Показывать pending/accepted/rejected feedback без ложного подтверждения.
- Реализовать warning/attention event.
- Добавить semantics и distinct non-color statuses.
- Провести double tap, delayed ACK, duplicate, reorder и clock-offset tests.

### Gate C3

Kerugi end-to-end проходит на Android/iPhone, включая disconnect в момент серии нажатий. Server
audit содержит каждый physical tap один раз.

## 6. Недели 5-7: интеграция с сетками и сессиями

- Получать discipline/category/current-next bout snapshot.
- Ограничивать доступность дисциплин server assignment.
- Сообщать judge readiness и submission state.
- Обрабатывать переходы prepared/running/paused/completed.
- Обновлять экран после peer/server state resync.
- Защищать от отправки draft/event в устаревшую session ID.

### Gate C4

Судья переключается между последовательными сессиями без перезапуска, а событие невозможно применить
к предыдущей или чужой сетке.

## 7. Недели 7-9: Tanbon и технические дисциплины

### Tanbon

- Подключить пять текущих buttons.
- Отправлять `HEAD`, `BODY` и neutral `CROSS`.
- Переиспользовать outbox/feedback Kerugi.

### Технические дисциплины

- Проверить наборы критериев по нормативным test vectors.
- Реализовать локальный `Save` и восстановление черновика.
- Разрешить `Save` в offline.
- Реализовать confirmation и final `Send`.
- Передавать исходные критерии, extra points и calculated total.
- Блокировать редактирование после ACK.
- Сохранять final pending payload при disconnect.
- Показывать submitted/pending/rejected.

### Gate C5

Все восемь дисциплин PDF 1 и Tanbon проходят client/server contract tests; offline Save переживает restart, а online
Send применяется один раз и становится read-only.

## 8. Недели 9-10: локализация и UX hardening

- Убрать hardcoded строки из screens и debug `println`.
- Проверить полноту русских resources.
- Проверить соответствие PDF выбранной дисциплине; English resources и rules deferred beyond v1.
- English PDF, rules and UI deferred beyond v1.
- Проверить landscape на минимальном и максимальном pilot screens.
- Добавить accessible names для icons/combat buttons.
- Проверить text scaling, contrast и touch target sizes.
- Добавить подтверждения необратимого Send/discard pending data.

### Gate C6

Русские critical flows не содержат смешанных строк; pilot devices не имеют clipped controls; blind
semantics audit различает все критические действия.

## 9. Недели 10-11: release hardening

- Собрать minified Android release APK.
- Собрать iOS archive и TestFlight build.
- Проверить clean install, upgrade и app data migration.
- Проверить Android network permissions и iOS Local Network permission.
- Выполнить soak test с reconnect и накоплением outbox.
- Проверить отсутствие credentials и rating payload в логах.
- Прогнать совместимость с release server installer, не IDE run target.

### Gate C7

APK и TestFlight устанавливаются на всех инвентаризированных устройствах, discovery/pairing работает
в реальной pilot LAN, P0/P1 client defects отсутствуют.

## 10. Неделя 12: полевой pilot

- Подключить 5-7 судей к одному peer через выделенный Wi-Fi роутер площадки.
- Провести Kerugi и Tanbon с контролируемым mobile disconnect.
- Провести Hosinsool, Pair, Group, Sword, Pole, Paired Nunchaku и Paired Fans.
- Сравнить client feedback, server audit и ручной протокол.
- Собрать anonymized diagnostics и UX observations.
- Зафиксировать battery/network/device-specific issues.

### Gate C8

Client не потерял и не продублировал подтверждённые события, final ratings совпали с server audit, а
все отклонения классифицированы до решения о production work.

## 11. Definition of Done клиента

- `CLI-* Must` закрыты доказательством приёмки.
- Реальные Android/iPhone прошли clean install и full flow.
- Online начинается только после server handshake/pairing.
- Durable outbox доказан app-kill и reconnect tests.
- Combat event применяется server не более одного раза.
- `Save` локален и доступен offline.
- `Send` окончателен, подтверждается и блокирует изменение после ACK.
- Все восемь дисциплин PDF 1 и Tanbon проверены соответствующими test vectors.
- Русский critical flow завершён; English localization deferred beyond v1.
- Accessibility labels присутствуют у критических controls.
- Release notes явно называют сборку pilot.

## 12. Риски

| Риск                                             | Влияние     | Снижение риска                                                      |
|--------------------------------------------------|-------------|---------------------------------------------------------------------|
| iOS mDNS/Local Network отличается от simulator   | Высокое     | Physical iPhone spike на неделе 1-2                                 |
| Глобальный `State` создаёт противоречивые режимы | Высокое     | State machine до подключения discipline UI                          |
| Outbox реализован слишком поздно                 | Критическое | Ввести до Kerugi vertical slice                                     |
| Текущие empty handlers выглядят готовыми         | Высокое     | Не считать UI completion функциональной готовностью                 |
| `Save` сейчас отключена offline                  | Среднее     | Разделить Save/Send semantics и добавить persistence                |
| Physical-device compatibility ещё не проверена   | Среднее     | Зафиксированы iOS 18 minimum и smoke test на iOS 26                 |
| Английские PDF не локализованы                   | Низкое      | English rules явно deferred beyond v1                                |
| Один разработчик и два приложения                | Высокое     | Shared contract tests, минимальная architecture, вертикальные gates |

## 13. После pilot

- Исправить P0/P1 и провести повторный field test.
- Рассмотреть public store distribution.
- Добавить production identity/security hardening.
- Расширить accessibility и device matrix.
- Рассмотреть Korean localization.
- Добавить telemetry только после privacy design и без обязательного WAN.

## 14. Связанные документы

- [Описание клиента](PROJECT.md)
- [Клиентские требования](REQUIREMENTS.md)
- [Принятые protocol decisions](PROTOCOL-DECISIONS.md)
- [Системный roadmap](https://github.com/Martial-Arts-Sport-Software/u-judge-server/blob/main/docs/ROADMAP.md)
