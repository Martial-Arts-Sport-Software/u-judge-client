# U'Judge Client: описание проекта

Статус документа: рабочая спецификация U'Judge v1 Pilot.

Дата фиксации: 30 августа 2026 года.

## 1. Назначение

U'Judge Client - мобильный судейский пульт U'Judge System для Android и iPhone. Приложение предоставляет боковому судье специализированный интерфейс для восьми дисциплин из PDF 1 и отдельной продуктовой дисциплины Tanbon и работает в двух режимах:

| Режим   | Назначение                                                                                     |
|---------|------------------------------------------------------------------------------------------------|
| Online  | Подключение к desktop-площадке, получение текущей сессии и надёжная отправка судейских событий |
| Offline | Автономный калькулятор технической оценки без сервера и без публикации результата соревнования |

Системная архитектура, общие требования и канонический roadmap находятся в репозитории [`u-judge-server`](https://github.com/Martial-Arts-Sport-Software/u-judge-server/tree/main/docs).

## 2. Платформы и технологии

- Kotlin Multiplatform;
- Compose Multiplatform UI в `composeApp`;
- Android application wrapper в `androidApp`;
- iOS framework `ComposeApp` и `MainViewController`;
- Compose Resources для строк, изображений и PDF;
- `dns-sd-kt` для mDNS discovery;
- Android `minSdk = 24` в текущей конфигурации; Android 5.1/TZ55 compatibility track deferred beyond v1 Pilot.

Для v1 Pilot минимальная версия iOS — 18; обязательны smoke tests на iOS 18 и iOS 26. Android 5.1/TZ55 не входит в
v1 Pilot и будет поддержан позже отдельной облегчённой версией клиента. Текущая build-конфигурация сама по себе не
считается подтверждением совместимости.

## 3. Пользовательский поток

```text
Ввод фамилии
      |
      +--> Offline --> Выбор дисциплины --> [Категория] --> Техническая оценка
      |
      +--> Online --> Поиск площадки --> Pairing --> Выбор дисциплины
                                                  --> [Категория]
                                                  --> Судейский экран
```

### 3.1. Старт

1. Судья выбирает русский или английский язык.
2. Судья вводит фамилию; пробельное значение недопустимо.
3. Судья выбирает online или offline.

### 3.2. Online

1. Клиент ищет сервисы `_u-judge._tcp.local.` через mDNS.
2. Судья выбирает площадку из списка.
3. Client выполняет handshake с выбранным server и ожидает подтверждение оператора.
4. После pairing судья выбирает доступную дисциплину и категорию, если она нужна.
5. Server сообщает текущий поединок/выступление и состояние сессии.
6. События отправляются с уникальными ID, локальной последовательностью и подтверждаются ACK.

Выбор найденной mDNS-записи ещё не означает успешное подключение. Online-состояние наступает только после сетевого handshake, проверки protocol version и подтверждения server.

### 3.3. Offline

- Kerugi и Tanbon недоступны, потому что зависят от текущего поединка, кворума и серверного scoring.
- Hosinsool, Pair, Group, Sword, Pole, Paired Nunchaku и Paired Fans работают как локальный калькулятор.
- `Save` сохраняет черновик оценки локально на устройстве.
- `Send` недоступна, пока нет подтверждённой online-сессии.
- Offline-оценка не становится официальным результатом и не синхронизируется автоматически как событие уже завершённого выступления.

## 4. Поддерживаемые дисциплины

| Enum               | Название         | Экран                                                  | Категория                   |
|--------------------|------------------|--------------------------------------------------------|-----------------------------|
| `KERUGI`           | Kerugi           | Четыре кнопки: голова/туловище синего и красного       | Определяется текущей сеткой |
| `TANBON`           | Tanbon           | Голова/туловище синего и красного, центральный `CROSS` | Определяется текущей сеткой |
| `HOSINSOOL`        | Hosinsool        | Техника, презентация, результат                        | Juniors/Adults              |
| `FREESTYLE_PAIR`   | Pair Freestyle   | Техника, презентация, результат                        | Juniors/Adults              |
| `FREESTYLE_GROUP`  | Group Freestyle  | 6 технических и 4 презентационных критерия             | Определяется сеткой         |
| `FREESTYLE_SWORD`  | Sword Freestyle  | 6 технических и 4 презентационных критерия             | Определяется сеткой         |
| `FREESTYLE_POLE`   | Pole Freestyle   | 6 технических и 4 презентационных критерия             | Определяется сеткой         |
| `FREESTYLE_NUNCHAKU` | Paired Nunchaku | 6 технических и 4 презентационных критерия             | Определяется сеткой         |
| `FREESTYLE_FANS`   | Paired Fans      | 6 технических и 4 презентационных критерия             | Определяется сеткой         |

Четыре weapon-дисциплины являются отдельными клиентскими режимами в v1 Pilot.

## 5. События боевых дисциплин

### 5.1. Kerugi

Каждое нажатие создаёт самостоятельное событие:

| Участник | Область | Серверная ценность по умолчанию |
|----------|---------|---------------------------------|
| Blue     | `HEAD`  | 2                               |
| Blue     | `BODY`  | 1                               |
| Red      | `HEAD`  | 2                               |
| Red      | `BODY`  | 1                               |

Клиент не объединяет нажатия разных судей и не определяет итоговый балл. Кворум и окно совпадения, по умолчанию `1000 мс`, применяет server. Для одного участника server разрешает все конфликтующие score candidates в одном окне по минимальной оценке; например, кандидаты `1` и `2` дают итоговый `1`.

### 5.2. Tanbon

| Участник | Событие | Серверная ценность         |
|----------|---------|----------------------------|
| Blue/Red | `HEAD`  | 2                          |
| Blue/Red | `BODY`  | 1                          |
| Neutral  | `CROSS` | 0; отдельная запись аудита |

Tanbon является пользовательским правилом U'Judge и требует экспертного подтверждения перед пилотом.

### 5.3. Надёжная доставка

```text
Tap -> local durable outbox -> WebSocket send -> server validation -> ACK
              ^                                          |
              +------------ retry same ID ----------------+
```

- UI немедленно показывает `pending`, но не изображает событие подтверждённым.
- Неподтверждённое событие переживает смену экрана и перезапуск приложения.
- Retry использует исходный event ID и не создаёт повторный балл.
- После ACK состояние становится `accepted` или `rejected`.
- Для coincidence window client передаёт timestamp/sequence, а server учитывает согласованный clock offset.

## 6. Техническая оценка

### 6.1. Общая модель

`TechniqueRating` содержит:

- набор технических критериев;
- набор критериев презентации;
- штрафы/бонусы `extraPoints`;
- итог, округлённый до `0.1` и ограниченный снизу нулём.

Каждый обязательный критерий имеет диапазон `0.1..1.0` и шаг `0.1`. Client валидирует ввод, но server повторно проверяет payload и рассчитывает официальное значение независимо.

### 6.2. Hosinsool и Pair Freestyle

- Juniors: 4 технических критерия.
- Adults: 6 технических критериев.
- Презентация: 4 критерия.

### 6.3. Group Freestyle

Технические критерии:

1. Атака и защита.
2. Разбивание предметов.
3. Удары ногами.
4. Навыки владения оружием.
5. Динамика движения.
6. Акробатика.

Презентация: креативность, сила, баланс и хореография.

### 6.4. Sword, Pole, Paired Nunchaku и Paired Fans

Технические критерии:

1. Техника владения оружием.
2. Удары ногами в прыжке.
3. Удары ногами с вращением.
4. Манипуляция оружием.
5. Движение.
6. Акробатика.

Презентация: креативность, сила, баланс и хореография.

### 6.5. Save и Send

| Действие | Поведение                                                                                         |
|----------|---------------------------------------------------------------------------------------------------|
| `Save`   | Сохраняет локальный черновик для текущей дисциплины/категории; не отправляет server               |
| `Send`   | Показывает подтверждение, отправляет исходные критерии и итог, после ACK блокирует редактирование |

Если `Send` выполнена во время краткого разрыва, payload остаётся в outbox и считается окончательным. Судья не может создать вторую оценку для той же сессии.

## 7. Сетевой контракт клиента

### 7.1. Discovery

- Service type: `_u-judge._tcp.local.`.
- Список дедуплицируется по стабильному service key.
- Removed service исчезает или помечается недоступным.
- Повторный scan отменяет предыдущий collector и не запускает бесконечно несколько discovery jobs.

### 7.2. HTTP

Целевые операции:

- pairing request/status;
- получение server/protocol metadata;
- initial snapshot текущей сессии;
- команды, для которых realtime не требуется.

### 7.3. WebSocket

Целевые сообщения:

- session state и bout number;
- judge readiness;
- combat event и ACK;
- technical rating и ACK;
- warning/attention;
- heartbeat, reconnect и resync cursor.

Каждый изменяющий payload содержит competition, peer/court, session, judge/device, event ID, client sequence, timestamp и protocol version.

## 8. Состояния приложения

Целевая модель заменяет набор независимых глобальных boolean-флагов:

```text
Offline
Discovering
ServerSelected
PairingPending
ConnectedIdle
SessionPrepared
SessionRunning
Reconnecting
Rejected
```

Состояние UI выводится из connection/session state, а не устанавливается экраном напрямую. Навигация не должна создавать или уничтожать transport session неявно.

## 9. Текущее состояние

Исходной точкой является локальная ветка `feat/server-connection`.

### 9.1. Реализовано

- общая Compose UI-кодовая база для Android/iOS;
- стартовый экран, фамилия, выбор online/offline и русский язык;
- навигация по восьми дисциплинам;
- выбор Juniors/Adults для Hosinsool и Pair Freestyle;
- UI Kerugi и Tanbon;
- экраны критериев, презентации и результата технических дисциплин;
- локальные модели критериев и расчёт индивидуальной суммы;
- unit tests граничных значений и формул `TechniqueCriteria`, `PresentationCriteria` и `TechniqueRating`;
- информационные PDF и popup;
- mDNS discovery `_u-judge._tcp.local.` с дедупликацией по service key, удалением unavailable service и единственной отменяемой scan job;
- локализованный список площадок с понятными именем, адресом и статусом доступности; площадка в состоянии resolving недоступна для выбора.
- явная connection state machine: выбор mDNS-площадки не даёт paired/online access.
- shared durable outbox: platform-backed journal сохраняет event ID, client sequence, timestamp, payload, retry metadata и terminal rejection; unit tests покрывают recreation, matching ACK и ordered bounded retry.

### 9.2. Частично или не реализовано

- Выбор resolved mDNS-площадки строит endpoint из адреса/порта, проверяет metadata и отправляет `POST /v1/pairing-requests` с device identity, нормализованной фамилией судьи и platform. Shared Ktor client выполняет versioned `/v1/realtime` handshake, принимает typed accepted/rejected response и authenticated clock sync до перехода в `ConnectedIdle`; secure credential storage, UI/lifecycle wiring, pairing-status push и TLS/manual fallback pending;
- server pairing approval, protocol version и текущий bout не получаются;
- кнопки Kerugi/Tanbon имеют пустые `onclick`;
- `Save` и `Send` имеют пустые `onclick` и сейчас обе выключены offline;
- typed combat/rating events ещё не wired in UI to the durable outbox; shared WebSocket command ACK/rejection handling exists, while transport retry lifecycle, heartbeat and resync remain pending;
- глобальный singleton `State` всё ещё хранит navigation/UI/domain state; connection lifecycle выделен в отдельную state machine;
- v1 minimum iOS version is 18; iOS 18 and iOS 26 require physical-device smoke coverage.
- Android 5.1/TZ55 compatibility is deferred to a separate lightweight client track after v1.

## 10. Ресурсы правил

PDF находятся в `composeApp/src/commonMain/composeResources/files/` и открываются из информационного popup. Русские материалы ФХР 2023 используются как нормативный источник. Файлы в английских каталогах должны быть проверены: копия русского PDF не является английской локализацией.

## 11. UI и доступность

- Основной режим использования мобильного пульта - landscape.
- Кнопки боевых событий должны быть крупными и различимыми не только цветом.
- Семантические подписи должны содержать участника и тип события.
- Pending/rejected/connected должны различаться текстом или формой, а не только цветом.
- Вибро/аудио feedback не может подтверждать server acceptance до ACK.
- Случайное двойное нажатие создаёт два разных физических события только если это явно разрешено; transport retry сохраняет один ID.
- `Send` требует явного подтверждения необратимости.

## 12. Ограничения v1 Pilot

- Установка через APK и TestFlight.
- Публичная публикация в магазинах не требуется.
- Korean localization не входит в pilot.
- Kerugi/Tanbon offline недоступны.
- Клиент не вычисляет общий результат нескольких судей.
- Client не редактирует сетки и не управляет таймером.

## 13. Связанные документы

- [Клиентские требования](REQUIREMENTS.md)
- [Клиентский roadmap](ROADMAP.md)
- [Системное описание](https://github.com/Martial-Arts-Sport-Software/u-judge-server/blob/main/docs/PROJECT.md)
- [Системные требования](https://github.com/Martial-Arts-Sport-Software/u-judge-server/blob/main/docs/REQUIREMENTS.md)
