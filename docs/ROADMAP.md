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

## Инкременты поставки

Единица планирования и PR - инкремент поставки, общий для обоих репозиториев. ID, недели и gates совпадают с
[server roadmap](https://github.com/Martial-Arts-Sport-Software/u-judge-server/blob/main/docs/ROADMAP.md#инкременты-поставки);
ниже описана client-часть. Правила закреплены в `AGENTS.md` (пункты 9-10, 12) и skill `u-judge-client-increment-planning`.

### Ретроспектива 30.08-20.09.2026

| Наблюдение | Факт на `main` |
|------------|----------------|
| Частота PR | 49 merged PR за 3 недели; медиана 131 строка client code, 75-й перцентиль - 206 |
| Результат | 15 `CLI-*` имеют статус `Implemented`, и все они локальные: offline, discovery, state machine, формулы, Save. Все требования, зависящие от server, остаются `Partial` или `Planned`; Gate C1 не закрыт |
| Интеграция | Client ни разу не прогонялся против настоящего server: все transport tests используют собственные fake responses, а `CLI-103` (совместимость DTO с server contract tests) остаётся `Planned` |
| Расхождение контракта | Kerugi/Tanbon tap отправляется как generic `command` с payload `kerugi_score`/`tanbon_score`. Server отвечает `command_ack` и сохраняет такой command без scoring: счёт считается только из `kerugi_score_command` с полным audit context. Client показывает `accepted` для удара, который не попадает в счёт |
| Документация | В 32 из 86 строк `REQUIREMENTS.md` текст требования был заменён описанием прогресса, поэтому исходный критерий требования перестал быть виден |

Причина та же, что и на server: slice определялся client-слоем (outbox → transport → feedback) и проверялся fake server,
а не сценарием судьи против настоящего desktop server. Cross-repository evidence каждый раз оставалось «pending».

### Правила для client

- Client-часть инкремента заканчивается сценарием на Android emulator или устройстве против server, запущенного
  `./gradlew :desktop:run` из `u-judge-server`, а не только против fake responses.
- Сообщения client совпадают с server contract: имена типов и поля берутся из server contract tests или ADR-004, а
  расхождение исправляется в том же инкременте в обоих репозиториях.
- Cross-repo инкремент имеет по одной issue и одному PR в каждом репозитории; они ссылаются друг на друга, а статусы
  отмечаются после merge обоих PR.
- Локальный сценарий без server (offline Save, формулы, локализация) может быть самостоятельным инкрементом, если он
  переводит требование в `Implemented`.

### План

| Статус | ID | Недели | Gate | Client-сценарий | Requirement IDs |
|--------|----|--------|------|-----------------|-----------------|
| [ ] | I1 | 4-5 | C1 (без physical devices) | Судья на emulator находит desktop server, проходит pairing с подтверждением оператора на desktop по local TLS, получает credential, выдерживает kill приложения и reconnect без повторного pairing; durable outbox против server (`CLI-060`, C2) перенесён в I2a, потому что события создаются только для server session; revoke на desktop блокирует новые события | `CLI-005`, `CLI-014`, `CLI-015`, `CLI-017`-`CLI-019`, `CLI-063`, `CLI-068`, `CLI-073`, `CLI-090`, `CLI-091`, `CLI-093`, `CLI-101`, `CLI-103` |
| [ ] | I4 | 5-6 | - | Client-части нет: server импортирует сетки, по которым идут поединки I2a/I2b | - |
| [ ] | I2a | 6-7 | C2, client-часть C3 | Kerugi: бой по баллам. Два-три emulator-судьи судят поединок импортированной сетки на desktop server: session snapshot с участниками и state, удары как server `kerugi_score_command`, ввод только в `running`, feedback только по ACK, warning, kill/reconnect с outbox и resync; server audit содержит каждый tap один раз | `CLI-022`-`CLI-025`, `CLI-030`, `CLI-032`-`CLI-038`, `CLI-060`, `CLI-061`, `CLI-062`, `CLI-064`-`CLI-066`, `CLI-102` |
| [ ] | I2b | 7-8 | C4 | Kerugi: время боя и ход сетки. Судья видит период боя, после результата переходит к следующему поединку сетки без перезапуска; draft и событие нельзя отправить в устаревшую или чужую сессию | `CLI-026`, `CLI-072`, `CLI-074`, `CLI-075` |
| [ ] | I3 | 8-9 | C1, C3 | Honor 50 Lite и iPhone 15 через роутер площадки: pairing сканированием QR-кода с desktop (адреса, порт и SPKI pin, поправка server ADR-006), запасной путь - ручной адрес или mDNS с вводом 6-значного кода, который client сверяет с ключом сам; Local Network permission, mDNS, pairing, Kerugi-бой с disconnect во время серии нажатий и искусственной задержкой | `CLI-001`, `CLI-010`, `CLI-011`, `CLI-083`-`CLI-085`, `CLI-104` |
| [ ] | I5 | 9-10 | C5 | Tanbon через server; технические дисциплины с подтверждаемым `Send`, final pending при disconnect, read-only после ACK и суммами, совпадающими с server | `CLI-021`, `CLI-031`, `CLI-042`, `CLI-044`, `CLI-045`, `CLI-048`-`CLI-053` |
| [ ] | I6 | 10 | C6 | Русские critical flows без hardcoded строк, доступность и pilot screen sizes | `CLI-007`, `CLI-067`, `CLI-080`, `CLI-081`, `CLI-084`-`CLI-086`, `CLI-092`, `CLI-094` |
| [ ] | I7 | 11 | C7 | Release APK и TestFlight на всех pilot devices против server installer | `CLI-105`, `CLI-106` |
| [ ] | I8 | 12 | C8 | Полевой пилот по разделу 10 | - |

### Известные расхождения с server на `main`

Каждое расхождение закрывается в указанном инкременте в обоих репозиториях.

| Расхождение | Инкремент |
|-------------|-----------|
| Local TLS для credential delivery не реализован ни в одном репозитории, поэтому pairing не может завершиться end-to-end | I1 |
| Kerugi/Tanbon tap отправляется как generic `command`, который server ACK-ит без scoring; `kerugi_score_command` требует competition, bracket, session, judge и device IDs, которых у client нет | I2a |
| Server не публикует session snapshot или assignment (`DEV-008`); client не обрабатывает `session_state_updated`, `kerugi_score_updated` и `resync_response` | I2a |
| Server не поддерживает Tanbon | I5 |

### Накопленное client-only доказательство

Shared unit tests против fake responses. Это partial evidence: server integration и physical devices отсутствуют.

| Область | Подтверждено тестами | Открыто |
|---------|----------------------|---------|
| Discovery | Одна отменяемая mDNS scan job, дедупликация, удаление unavailable services, resolved/resolving статусы | Physical mDNS (I3) |
| Metadata и pairing | Shared HTTP metadata/protocol/capability validation для mDNS и manual host/IP; pairing request с device identity, фамилией и platform; polling pending/accepted/rejected с отменой; delivery proof и credential в Android Keystore-backed storage и iOS Keychain | Local TLS и выдача credential настоящим server (I1) |
| Realtime | Versioned handshake, four-timestamp clock sync, typed heartbeat lifecycle, reconnect со stored credential | Прогон против server (I1), resync (I2a) |
| Outbox | Platform-backed journal с event ID, client sequence, timestamp и retry metadata; ordered bounded backoff; terminal ACK/rejection; replay due commands после connect/reconnect; fault injection | App-kill против server (I1, I2a) |
| Kerugi/Tanbon controls | Durable typed command только в authenticated `running` matching session; feedback только для matching последнего event; semantic labels | Формат server `kerugi_score_command` и session snapshot (I2a); Tanbon на server (I5) |
| Технические дисциплины | Семь offline-режимов, критерии `0.1..1.0` с шагом `0.1`, независимые черновики `Save` без network request | `Send` и server totals (I5) |

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

- [x] Управлять единственной mDNS discovery job и её lifecycle (`CLI-012`; shared rescan/cancellation tests).
- [x] Показывать понятные имя площадки, адрес и статус (`CLI-013`; resolved и resolving состояния покрыты shared unit tests).
- [ ] Реализовать HTTP metadata/handshake, protocol version/capability check и manual host/IP fallback (см. «Накопленное client-only доказательство»).
- [ ] Реализовать WebSocket connect, heartbeat и typed envelope (см. «Накопленное client-only доказательство»).
- [x] Получать pairing pending/accepted/rejected через public HTTP status polling с локальным UI без online access (`CLI-016`; shared contract tests).
- [ ] Согласовать clock offset (см. «Накопленное client-only доказательство»).
- [ ] Отправить событие, получить ACK, разорвать сеть и повторить тот же ID (см. «Накопленное client-only доказательство»).
- [ ] Проверить iOS Local Network permission и mDNS на TestFlight-like build.

### Gate C1

Физические Android и iPhone обнаруживают server, проходят pairing и доставляют событие ровно один
раз после искусственного reconnect.

## 4. Недели 2-3: state и durable outbox

- [x] Заменить `State.isConnectedToServer` connection state machine (`CLI-070`; переходы покрыты shared unit tests).
- [x] Отделить session state от navigation state (`CLI-071`; shared lifecycle/isolation tests); отдельный UI state pairing и rating draft остаётся pending.
- [ ] Ввести локальное durable storage для identity, settings, drafts и outbox (см. «Накопленное client-only доказательство»).
- [ ] Добавить event ID, client sequence, timestamp и retry metadata (см. «Накопленное client-only доказательство»).
- [ ] Реализовать bounded exponential backoff и terminal rejection (см. «Накопленное client-only доказательство»).
- Восстанавливать active connection/session после lifecycle events.
- Локализовать типизированные transport/protocol errors.

### Gate C2

Неподтверждённое событие переживает app kill, отправляется с исходным ID после запуска и исчезает из
outbox только после terminal ACK.

## 5. Недели 4-5: Kerugi vertical slice

- [x] Подключить четыре Kerugi combat buttons к durable typed events только для authenticated `running` Kerugi session (`CLI-030`, `CLI-032`, `CLI-033`, `CLI-035`, `CLI-038`; shared controller tests).
- Получать current bout, blue/red labels и session state от server.
- Блокировать ввод вне `running`.
- [x] Показывать локализованный pending/accepted/rejected feedback без ложного подтверждения только для matching последнего Kerugi event (`CLI-036`, `CLI-065`, `CLI-066`, `CLI-084`; shared ACK/rejection/disconnect, replay и serialized-channel tests).
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

- [x] Подключить пять текущих buttons к durable typed events только для authenticated `running` Tanbon session (`CLI-031`, `CLI-032`, `CLI-033`, `CLI-035`, `CLI-038`; shared controller tests).
- [x] Отправлять `HEAD`, `BODY` и neutral `CROSS` (`CLI-031`; shared payload contract tests).
- [x] Переиспользовать outbox/feedback Kerugi (`CLI-036`, `CLI-066`, `CLI-084`; shared ACK/rejection, replay и matching-feedback tests).

### Технические дисциплины

- Проверить наборы критериев по нормативным test vectors.
- [x] Ограничить обязательные критерии диапазоном `0.1..1.0` с шагом `0.1` на input и model boundary (`CLI-041`; shared boundary tests).
- [x] Реализовать локальный `Save` и восстановление независимого черновика для Hosinsool, Pair, Group, Sword, Pole, Paired Nunchaku и Paired Fans (`CLI-046`; shared persistence tests).
- [x] Разрешить `Save` в offline (`CLI-047`; действие не создаёт network request).
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
| Client и server расходятся в контракте            | Критическое | Общие инкременты I1-I8 и прогон против настоящего desktop server    |

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
