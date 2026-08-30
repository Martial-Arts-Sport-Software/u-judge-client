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
| CLI-005 | Must      | Planned | Online-функции доступны только после handshake и pairing  | Простого выбора mDNS service недостаточно для connected state                      |
| CLI-006 | Must      | Partial | Интерфейс переключается между RU/EN                       | Все строки текущего flow локализованы; отсутствуют hardcoded mixed-language строки |
| CLI-007 | Should    | Planned | Фамилия, язык и локальные черновики переживают перезапуск | После restart значения восстановлены из локального persistence                     |

## 3. Discovery и pairing

| ID      | Приоритет | Статус  | Требование                                                     | Критерий приёмки                                                                      |
|---------|-----------|---------|----------------------------------------------------------------|---------------------------------------------------------------------------------------|
| CLI-010 | Must      | Partial | Клиент обнаруживает `_u-judge._tcp.local.`                     | Server появляется без ручного IP в общей LAN                                          |
| CLI-011 | Must      | Partial | Найденные servers дедуплицируются и удаляются при mDNS removed | В списке нет дублей и заведомо недоступных записей                                    |
| CLI-012 | Must      | Planned | Повторный scan отменяет или переиспользует предыдущую job      | Многократное нажатие Search не создаёт несколько collectors                           |
| CLI-013 | Must      | Planned | Судья выбирает площадку по понятному имени                     | UI не требует читать raw address list                                                 |
| CLI-014 | Must      | Planned | Client проверяет protocol version/capabilities                 | Несовместимый server отклоняется с локализованной причиной                            |
| CLI-015 | Must      | Planned | Client отправляет pairing request с judge/device identity      | Server видит pending device и фамилию                                                 |
| CLI-016 | Must      | Planned | UI показывает pending, accepted и rejected                     | Судья не попадает на рабочий экран до accepted                                        |
| CLI-017 | Must      | Planned | Pairing session восстанавливается при кратком reconnect        | Подтверждённый клиент не требует ручного pairing после каждого packet loss            |
| CLI-018 | Must      | Planned | Отзыв server немедленно блокирует новые события                | Client показывает disconnected/rejected и сохраняет только допустимые pending records |

## 4. Выбор дисциплины и сессии

| ID      | Приоритет | Статус  | Требование                                                     | Критерий приёмки                                                            |
|---------|-----------|---------|----------------------------------------------------------------|-----------------------------------------------------------------------------|
| CLI-020 | Must      | Partial | Клиент показывает шесть дисциплин                              | Kerugi, Tanbon, Hosinsool, Pair, Group и Weapon доступны по правилам режима |
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
| CLI-032 | Must      | Planned | Каждое физическое нажатие получает уникальный event ID  | Два намеренных нажатия различаются, transport retry сохраняет ID       |
| CLI-033 | Must      | Planned | Client отправляет client sequence и timestamp           | Server может восстановить локальный порядок и clock offset             |
| CLI-034 | Must      | Planned | Client не вычисляет кворум и итоговый балл              | UI не объявляет score accepted до server ACK/state update              |
| CLI-035 | Must      | Planned | Нажатие немедленно попадает в durable outbox            | App kill до ACK не теряет событие                                      |
| CLI-036 | Must      | Planned | Client отображает pending/accepted/rejected             | Состояние каждого недавнего события понятно судье                      |
| CLI-037 | Must      | Planned | Warning/attention отправляется отдельно от scoring      | Server получает judge, device, session и timestamp; счёт не меняется   |
| CLI-038 | Must      | Planned | Offline Kerugi/Tanbon не генерирует официальные события | Боевые кнопки недоступны без server session                            |
| CLI-039 | Should    | Planned | У кнопок есть настраиваемый haptic feedback             | Feedback на tap не выдаётся за ACK; accessibility settings учитываются |

## 6. Технические дисциплины

| ID      | Приоритет | Статус  | Требование                                                         | Критерий приёмки                                                         |
|---------|-----------|---------|--------------------------------------------------------------------|--------------------------------------------------------------------------|
| CLI-040 | Must      | Partial | Клиент поддерживает Hosinsool, Pair, Group и Weapon                | Набор критериев соответствует дисциплине и категории                     |
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
| CLI-060 | Must      | Planned | Outbox хранится в локальном durable storage                | Process death не очищает неподтверждённые events                       |
| CLI-061 | Must      | Planned | Retry использует тот же event ID                           | Server применяет событие не более одного раза                          |
| CLI-062 | Must      | Planned | Retry применяет bounded exponential backoff                | Client не создаёт request storm при недоступном server                 |
| CLI-063 | Must      | Planned | WebSocket heartbeat обнаруживает разрыв                    | UI переходит в reconnecting в ограниченное protocol timeout время      |
| CLI-064 | Must      | Planned | После reconnect client отправляет cursor и получает resync | Session state сходится до разрешения новых действий                    |
| CLI-065 | Must      | Planned | Event ordering сохраняется для одного client               | Поздний ACK не удаляет более новое pending event                       |
| CLI-066 | Must      | Planned | Rejected terminal event не повторяется бесконечно          | Outbox отмечает final rejection и показывает действие пользователю     |
| CLI-067 | Must      | Planned | Logout/смена server не удаляет pending events молча        | Требуется успешная доставка или явное подтверждённое discard с аудитом |
| CLI-068 | Must      | Planned | Clock offset согласуется при handshake/reconnect           | Combat timestamp не полагается только на device wall clock             |

## 8. Состояние и навигация

| ID      | Приоритет | Статус  | Требование                                                              | Критерий приёмки                                                         |
|---------|-----------|---------|-------------------------------------------------------------------------|--------------------------------------------------------------------------|
| CLI-070 | Must      | Planned | Connection представлено state machine, а не boolean                     | Невозможны одновременно offline/connected или paired/no-server состояния |
| CLI-071 | Must      | Planned | Session state отделено от navigation state                              | Возврат назад не завершает серверную сессию неявно                       |
| CLI-072 | Must      | Planned | Rating draft имеет ID дисциплины, категории и сессии                    | Черновик другой сессии не отправляется случайно                          |
| CLI-073 | Must      | Planned | Ошибки типизированы и локализованы                                      | UI различает discovery, pairing, transport, validation и protocol errors |
| CLI-074 | Must      | Planned | Loading/action jobs отменяются по lifecycle                             | Уход с экрана не оставляет лишние scans или sends                        |
| CLI-075 | Must      | Planned | Значимый state восстанавливается после configuration/process recreation | Android recreation и iOS lifecycle не сбрасывают active flow             |

## 9. Локализация, правила и доступность

| ID      | Приоритет | Статус  | Требование                                                      | Критерий приёмки                                                                     |
|---------|-----------|---------|-----------------------------------------------------------------|--------------------------------------------------------------------------------------|
| CLI-080 | Must      | Partial | Все пользовательские строки находятся в resources               | Search по screens не находит hardcoded UI RU/EN, кроме имён языков при необходимости |
| CLI-081 | Must      | Partial | Информационный popup открывает правила текущей дисциплины/языка | Ресурс существует и соответствует выбранной дисциплине                               |
| CLI-082 | Must      | Blocked | Английские PDF действительно англоязычные                       | Контент проверен владельцем продукта, а не является копией RU                        |
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
| CLI-093 | Must      | Planned | Client валидирует endpoint из discovery                                   | Подключение к сервису неверного типа/protocol отклоняется |
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
- [Системные требования](https://github.com/Martial-Arts-Sport-Software/u-judge-server/blob/main/docs/REQUIREMENTS.md)
