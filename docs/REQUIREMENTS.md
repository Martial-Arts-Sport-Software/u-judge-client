# U'Judge Client: функциональные требования

Статус документа: требования мобильного приложения для U'Judge v1 Pilot.

Дата фиксации: 30 августа 2026 года.

Канонические общесистемные требования находятся в [
`u-judge-server/docs/REQUIREMENTS.md`](https://github.com/Martial-Arts-Sport-Software/u-judge-server/blob/main/docs/REQUIREMENTS.md).
Этот документ детализирует клиентские требования `CLI-*`.

## 1. Приоритеты и статусы

| Приоритет | Значение                                           |
|-----------|----------------------------------------------------|
| Must      | Обязательно для полевого pilot                     |
| Should    | Ожидается, но может быть перенесено явным решением |
| Could     | Улучшение после прохождения Must-гейтов            |

| Статус      | Значение                                        |
|-------------|-------------------------------------------------|
| Implemented | Реализовано и проверено                         |
| Partial     | Есть UI/model baseline, но сценарий не закончен |
| Planned     | Требуется для v1 Pilot                          |
| Blocked     | Нужны внешние данные/решение                    |

## 2. Запуск и режимы

| ID      | Приоритет | Статус  | Требование                                                | Критерий приёмки                                                                   |
|---------|-----------|---------|-----------------------------------------------------------|------------------------------------------------------------------------------------|
| CLI-001 | Must      | Partial | Клиент запускается на Android и iPhone в landscape        | Critical-flow smoke test проходит на физических устройствах pilot                  |
| CLI-002 | Must      | Partial | Судья вводит фамилию до выбора режима                     | Пустое/пробельное значение не позволяет продолжить                                 |
| CLI-003 | Must      | Partial | Клиент поддерживает online и offline                      | Выбранный режим явно виден и не меняется из-за навигации                           |
| CLI-004 | Must      | Partial | Offline разрешает только технические дисциплины           | Kerugi/Tanbon заблокированы с объяснением причины                                  |
| CLI-005 | Must      | Partial | Shared realtime handshake переводит pairing-pending state в connected только после typed server acceptance; credential storage and UI wiring pending | Простого выбора mDNS service недостаточно для connected state                      |
| CLI-006 | Should    | Planned | Post-v1 интерфейс переключается между RU/EN               | Все строки текущего flow локализованы; v1 Pilot требует русский интерфейс          |
| CLI-007 | Should    | Planned | Фамилия, язык и локальные черновики переживают перезапуск | После restart значения восстановлены из локального persistence                     |

## 3. Discovery и pairing

| ID      | Приоритет | Статус  | Требование                                                     | Критерий приёмки                                                                      |
|---------|-----------|---------|----------------------------------------------------------------|---------------------------------------------------------------------------------------|
| CLI-010 | Must      | Partial | Клиент обнаруживает `_u-judge._tcp.local.`                     | Server появляется без ручного IP в общей LAN                                          |
| CLI-011 | Must      | Partial | Найденные servers дедуплицируются и удаляются при mDNS removed | В списке нет дублей и заведомо недоступных записей                                    |
| CLI-012 | Must      | Planned | Повторный scan отменяет или переиспользует предыдущую job      | Многократное нажатие Search не создаёт несколько collectors                           |
| CLI-013 | Must      | Implemented | Судья выбирает площадку по понятному имени                     | UI показывает имя, адрес и статус; resolving площадка недоступна для выбора           |
| CLI-014 | Must      | Partial | Resolved mDNS endpoint проходит shared HTTP metadata validation; TLS/manual fallback pending | Несовместимый server отклоняется с локализованной причиной                            |
| CLI-015 | Must      | Partial | Выбор validated mDNS server отправляет pairing request с device identity, нормализованной фамилией судьи и platform; TLS pending | Server видит pending device и фамилию                                                 |
| CLI-016 | Must      | Partial | UI показывает pending и локальные pairing errors; shared realtime handshake decodes typed accepted/rejected responses, UI status wiring pending | Судья не попадает на рабочий экран до accepted                                        |
| CLI-017 | Must      | Partial | Shared client accepts a reconnect credential through the realtime handshake; secure persistence and reconnect lifecycle pending | Подтверждённый клиент не требует ручного pairing после каждого packet loss            |
| CLI-018 | Must      | Planned | Отзыв server немедленно блокирует новые события                | Client показывает disconnected/rejected и сохраняет только допустимые pending records |
| CLI-019 | Must      | Planned | Client поддерживает manual host/IP fallback после mDNS         | Endpoint проходит metadata, protocol и TLS validation до online state                |

## 4. Выбор дисциплины и сессии

| ID      | Приоритет | Статус  | Требование                                                     | Критерий приёмки                                                            |
|---------|-----------|---------|----------------------------------------------------------------|-----------------------------------------------------------------------------|
| CLI-020 | Must      | Partial | Клиент показывает девять дисциплинарных режимов                 | Восемь дисциплин PDF 1 и продуктовая дисциплина Tanbon доступны по правилам режима |
| CLI-021 | Must      | Partial | Hosinsool и Pair позволяют выбрать Juniors/Adults              | Выбор формирует корректное число технических критериев                      |
| CLI-022 | Must      | Planned | Online-доступность проверяется server session                  | Судья не отправляет событие в дисциплину, не совпадающую с текущей сессией  |
| CLI-023 | Must      | Planned | Client получает ID и номер текущей сессии                      | На экране нет hardcoded bout number                                         |
| CLI-024 | Must      | Planned | Client показывает participant labels/colors от server          | Событие однозначно относится к blue/red участнику текущей сессии            |
| CLI-025 | Must      | Planned | Client показывает `prepared`, `running`, `paused`, `completed` | События разрешены только в допустимом состоянии                             |
| CLI-026 | Must      | Planned | Client сообщает готовность судьи                               | Оператор видит ready для конкретной сессии и judge slot                     |

## 5. Kerugi и Tanbon

| ID      | Приоритет | Статус  | Требование                                              | Критерий приёмки                                                       |
|---------|-----------|---------|---------------------------------------------------------|------------------------------------------------------------------------|
| CLI-030 | Must      | Partial | Kerugi имеет `HEAD`/`BODY` для blue/red                 | Каждая кнопка создаёт правильный typed event                           |
| CLI-031 | Must      | Partial | Tanbon имеет `HEAD`/`BODY` для blue/red и `CROSS`       | Пять кнопок создают правильные typed events                            |
| CLI-032 | Must      | Partial | Shared outbox сохраняет caller-provided unique event ID; generation in combat controls pending | Два намеренных нажатия различаются, transport retry сохраняет ID       |
| CLI-033 | Must      | Partial | Shared outbox сохраняет client sequence и timestamp; typed transport send pending | Server может восстановить локальный порядок и clock offset             |
| CLI-034 | Must      | Planned | Client не вычисляет кворум и итоговый балл              | UI не объявляет score accepted до server ACK/state update; server разрешает конфликт score candidates по минимальной оценке |
| CLI-035 | Must      | Partial | Realtime command client journals the complete command before socket send; physical tap wiring and app-kill proof pending | App kill до ACK не теряет событие                                      |
| CLI-036 | Must      | Partial | Shared command transport applies terminal ACK/rejection to outbox; UI feedback pending | Состояние каждого недавнего события понятно судье                      |
| CLI-037 | Must      | Planned | Warning/attention отправляется отдельно от scoring      | Server получает judge, device, session и timestamp; счёт не меняется   |
| CLI-038 | Must      | Planned | Offline Kerugi/Tanbon не генерирует официальные события | Боевые кнопки недоступны без server session                            |
| CLI-039 | Should    | Planned | У кнопок есть настраиваемый haptic feedback             | Feedback на tap не выдаётся за ACK; accessibility settings учитываются |

## 6. Технические дисциплины

| ID      | Приоритет | Статус  | Требование                                                         | Критерий приёмки                                                         |
|---------|-----------|---------|--------------------------------------------------------------------|--------------------------------------------------------------------------|
| CLI-040 | Must      | Partial | Клиент поддерживает Hosinsool, Pair, Group, Sword, Pole, Paired Nunchaku и Paired Fans | Набор критериев соответствует дисциплине и категории |
| CLI-041 | Must      | Partial | Обязательный критерий вводится в `0.1..1.0`, шаг `0.1`             | UI не создаёт `0.0`, NaN или значение вне диапазона                      |
| CLI-042 | Must      | Partial | Hosinsool/Pair содержит 4 или 6 технических критериев              | Juniors даёт 4, Adults даёт 6                                            |
| CLI-043 | Must      | Partial | Group/Weapon содержит 6 технических критериев                      | Все значения входят в payload                                            |
| CLI-044 | Must      | Partial | Презентация содержит 4 критерия                                    | Все значения входят в payload                                            |
| CLI-045 | Must      | Partial | Client показывает technique, presentation, extra points и total    | Total округлён до `0.1` и не меньше нуля                                 |
| CLI-046 | Must      | Planned | `Save` сохраняет локальный черновик                                | Черновик переживает navigation/restart и не вызывает network request     |
| CLI-047 | Must      | Planned | `Save` доступна offline                                            | Технический калькулятор выполняет своё автономное назначение             |
| CLI-048 | Must      | Planned | `Send` доступна только для активной online-сессии                  | Offline/неподтверждённый client не может отправить оценку                |
| CLI-049 | Must      | Planned | `Send` требует подтверждения                                       | Диалог сообщает, что после отправки оценка неизменяема                   |
| CLI-050 | Must      | Planned | Payload содержит исходные критерии, extra points и локальный total | Server может независимо проверить расчёт                                 |
| CLI-051 | Must      | Planned | После ACK оценка блокируется                                       | Судья не может отредактировать или отправить вторую оценку той же сессии |
| CLI-052 | Must      | Planned | Send во время disconnect сохраняется как final pending             | После reconnect отправляется тот же payload и ID                         |
| CLI-053 | Must      | Planned | Rejected rating показывает причину и не теряет payload             | Судья/оператор может диагностировать несовместимую сессию или schema     |

## 7. Outbox, reconnect и синхронизация

| ID      | Приоритет | Статус  | Требование                                                 | Критерий приёмки                                                       |
|---------|-----------|---------|------------------------------------------------------------|------------------------------------------------------------------------|
| CLI-060 | Must      | Partial | Shared JSON journal сохраняется в Android SharedPreferences/iOS NSUserDefaults; event wiring and app-kill integration proof pending | Process death не очищает неподтверждённые events                       |
| CLI-061 | Must      | Partial | Realtime command retry reuses the durable event ID; reconnect/server integration evidence pending | Server применяет событие не более одного раза                          |
| CLI-062 | Must      | Partial | Shared outbox uses ordered bounded exponential backoff; transport fault injection pending | Client не создаёт request storm при недоступном server                 |
| CLI-063 | Must      | Planned | WebSocket heartbeat обнаруживает разрыв                    | UI переходит в reconnecting в ограниченное protocol timeout время      |
| CLI-064 | Must      | Planned | После reconnect client отправляет cursor и получает resync | Session state сходится до разрешения новых действий                    |
| CLI-065 | Must      | Partial | Shared command ACK removes only its matching durable event; transport reorder test pending | Поздний ACK не удаляет более новое pending event                       |
| CLI-066 | Must      | Partial | Terminal command rejection is persisted and excluded from retry; UI feedback pending | Outbox отмечает final rejection и показывает действие пользователю     |
| CLI-067 | Must      | Planned | Logout/смена server не удаляет pending events молча        | Требуется успешная доставка или явное подтверждённое discard с аудитом |
| CLI-068 | Must      | Partial | Shared `ClockSyncClient` выполняет один typed four-timestamp exchange через authenticated realtime socket; handshake/reconnect wiring pending | Four-timestamp exchange оценивает offset/round-trip; combat timestamp не полагается только на device wall clock |

## 8. Состояние и навигация

| ID      | Приоритет | Статус  | Требование                                                              | Критерий приёмки                                                         |
|---------|-----------|---------|-------------------------------------------------------------------------|--------------------------------------------------------------------------|
| CLI-070 | Must      | Implemented | Connection представлено state machine, а не boolean                  | Невозможны одновременно offline/connected или paired/no-server состояния |
| CLI-071 | Must      | Planned | Session state отделено от navigation state                              | Возврат назад не завершает серверную сессию неявно                       |
| CLI-072 | Must      | Planned | Rating draft имеет ID дисциплины, категории и сессии                    | Черновик другой сессии не отправляется случайно                          |
| CLI-073 | Must      | Partial | Metadata validation, pairing request и realtime handshake имеют типизированные локализуемые errors; UI transport feedback and remaining protocol errors pending | UI различает discovery, pairing, transport, validation и protocol errors |
| CLI-074 | Must      | Planned | Loading/action jobs отменяются по lifecycle                             | Уход с экрана не оставляет лишние scans или sends                        |
| CLI-075 | Must      | Planned | Значимый state восстанавливается после configuration/process recreation | Android recreation и iOS lifecycle не сбрасывают active flow             |

## 9. Локализация, правила и доступность

| ID      | Приоритет | Статус  | Требование                                                      | Критерий приёмки                                                                     |
|---------|-----------|---------|-----------------------------------------------------------------|--------------------------------------------------------------------------------------|
| CLI-080 | Must      | Partial | Все пользовательские строки находятся в resources               | Search по screens не находит hardcoded UI RU/EN, кроме имён языков при необходимости |
| CLI-081 | Must      | Partial | Информационный popup открывает правила текущей дисциплины/языка | Ресурс существует и соответствует выбранной дисциплине                               |
| CLI-082 | Could     | Planned | Английские PDF действительно англоязычные                       | Post-v1 контент проверен владельцем продукта и не является копией RU                 |
| CLI-083 | Must      | Planned | Combat controls имеют semantic labels                           | Accessibility tree сообщает participant и event type                                 |
| CLI-084 | Must      | Planned | Статус не кодируется только цветом                              | Pending/accepted/rejected имеют текст/иконку/форму                                   |
| CLI-085 | Must      | Planned | Все критические элементы доступны при pilot screen sizes        | Physical-device smoke test не обнаруживает clipping/недоступных кнопок               |
| CLI-086 | Should    | Planned | Dynamic font не скрывает критические действия                   | Поддерживаемый accessibility scale проходит layout test                              |

## 10. Безопасность и приватность

| ID      | Приоритет | Статус  | Требование                                                                | Критерий приёмки                                          |
|---------|-----------|---------|---------------------------------------------------------------------------|-----------------------------------------------------------|
| CLI-090 | Must      | Planned | Client не отправляет события до pairing                                   | Anonymous write отклоняется и локально, и server-side     |
| CLI-091 | Must      | Planned | Competition/session credentials хранятся в platform secure storage        | Секреты отсутствуют в plain preferences/logs              |
| CLI-092 | Must      | Planned | Логи не содержат полный rating payload и персональные данные по умолчанию | Production pilot log использует IDs и error codes         |
| CLI-093 | Must      | Partial | Protocol/capability validation готова как shared domain boundary; HTTP endpoint/TLS validation pending | Подключение к сервису неверного типа/protocol отклоняется |
| CLI-094 | Must      | Planned | Локальные черновики удаляются явным действием или после retention policy  | Судья понимает, какие данные остались на устройстве       |

## 11. Качество и выпуск

| ID      | Приоритет | Статус  | Требование                                  | Критерий приёмки                                                                  |
|---------|-----------|---------|---------------------------------------------|-----------------------------------------------------------------------------------|
| CLI-100 | Must      | Implemented | Формулы rating покрыты unit tests        | Есть boundary cases для всех criteria models и rounding                           |
| CLI-101 | Must      | Planned | Discovery/pairing покрыты integration tests | Проверены discovered/resolved/removed, accept/reject и version mismatch           |
| CLI-102 | Must      | Planned | Outbox покрыт fault-injection tests         | Drop, duplicate, reorder, app kill и reconnect не теряют/не дублируют events      |
| CLI-103 | Must      | Planned | DTO совместимы с server contract tests      | Несовпадение protocol/schema блокирует CI                                         |
| CLI-104 | Must      | Planned | Critical UI flow покрыт smoke tests         | Entry -> connect/offline -> discipline -> event/save/send проходит на Android/iOS |
| CLI-105 | Must      | Planned | APK устанавливается без developer tooling   | Pilot Android devices запускают release build                                     |
| CLI-106 | Must      | Planned | TestFlight build устанавливается на iPhone  | Local Network permission и discovery работают после чистой установки              |
| CLI-107 | Must      | Implemented | Известные ограничения опубликованы       | Release notes называют версию pilot, а не production `1.0`                        |

## 12. Не входит в клиент v1 Pilot

- управление турнирными сетками;
- управление server timer и операторскими штрафами;
- вычисление кворума и общего результата;
- Kerugi/Tanbon без server;
- Korean localization;
- публичная публикация в Google Play/App Store;
- автоматическое восстановление offline-черновика как официальной оценки завершённой сессии.

## 13. Связанные требования server

| Клиентская область   | Системные IDs      |
|----------------------|--------------------|
| Pairing и устройства | `DEV-001..DEV-010` |
| Kerugi               | `KER-001..KER-015` |
| Tanbon               | `TAN-001..TAN-005` |
| Технические оценки   | `TEC-001..TEC-014` |
| Reconnect            | `NET-001..NET-006` |
| Качество и release   | `NFR-*`, `REL-*`   |

## 14. Связанные документы

- [Описание клиента](PROJECT.md)
- [Клиентский roadmap](ROADMAP.md)
- [Принятые protocol decisions](PROTOCOL-DECISIONS.md)
- [Системные требования](https://github.com/Martial-Arts-Sport-Software/u-judge-server/blob/main/docs/REQUIREMENTS.md)
