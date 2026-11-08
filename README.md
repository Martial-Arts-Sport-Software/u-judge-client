# U'Judge Client

Мобильный судейский пульт U'Judge System для Android и iPhone, построенный на Kotlin Multiplatform и Compose Multiplatform.

Проект находится в разработке. Текущая версия `0.1.0` является U'Judge v1 Pilot для полевого испытания и не является production-релизом.

## Возможности pilot

- русский и английский интерфейс;
- поиск площадок в локальной сети через mDNS;
- offline-калькулятор технических дисциплин с локальным сохранением черновика;
- Kerugi, Tanbon, Hosinsool, Pair Freestyle, Group Freestyle, Sword, Pole, Paired Nunchaku и Paired Fans;

## Текущее состояние

UI всех девяти дисциплин и локальные модели технической оценки уже существуют. Sword, Pole, Paired Nunchaku и Paired Fans являются отдельными offline-режимами и хранят независимые черновики. mDNS находит `_u-judge._tcp.local.`; повторный поиск отменяет предыдущий scan, а removed services исчезают из списка. Shared HTTP metadata/pairing, WebSocket handshake с authenticated four-timestamp clock sync, typed heartbeat lifecycle и typed command/terminal ACK outbox реализованы. Server session state изолирован от navigation и следует Kerugi bout contract server ADR-009: после каждого connect/reconnect client воспроизводит due durable commands в исходном порядке, затем запрашивает `resync_request` и применяет pushes по cursor. Экран Kerugi показывает номер поединка, категорию, синего и красного участника, состояние и счёт от server; удары и сигнал оператору уходят как `kerugi_score_command`/`judge_attention_command` с audit context snapshot, ввод разрешён только назначенному судье в `running`, а feedback меняется только по ответу server на свой event ID. По server ADR-010 экран показывает период и оставшееся время боя, кнопку «Готов» до начала поединка и переходит к следующему поединку сетки без перезапуска; нажатие по устаревшему поединку ничего не отправляет, а черновики `Save` привязаны к сессии. Tanbon поддерживает blue/red `HEAD`/`BODY` и neutral `CROSS`, но server Tanbon появится в I5. Прогон против настоящего server и physical reconnect proof ещё не выполнены. Reconnect credential, endpoint и SPKI pin server хранятся в Android Keystore-backed storage и iOS Keychain; подключение идёт только по HTTPS/WSS с pinned ключом и кодом сверки (server ADR-006), а после перезапуска приложение переподключается к paired server без нового pairing. `Save` локально сохраняет и восстанавливает черновик поддерживаемых технических экранов без network request; `Send` пока недоступна.

Server на `main` ещё не реализует ADR-009; это и остальные расхождения закрывают общие с server инкременты из
[roadmap](docs/ROADMAP.md#инкременты-поставки).
Подробное разделение текущего и целевого состояния находится в [описании проекта](docs/PROJECT.md).

## Модули

| Путь | Назначение |
| --- | --- |
| `composeApp/` | Общая Compose UI, модели, ресурсы и platform-specific Kotlin code |
| `androidApp/` | Android application wrapper |
| `iosApp/` | Xcode-проект iOS application |
| `docs/` | Описание, требования и roadmap |

## Запуск

Требуется JDK 21. Для iOS также нужны macOS и Xcode.

Сборка Android debug APK:

```shell
./gradlew :androidApp:assembleDebug
```

Проверка перед изменением или pull request (JDK 21):

```shell
./gradlew :composeApp:testAndroidHostTest :androidApp:assembleDebug \
  :composeApp:compileKotlinIosArm64 :composeApp:compileKotlinIosSimulatorArm64
```

GitHub Actions запускает эти проверки для каждого push и pull request: Android tests/APK на Ubuntu,
iOS framework compilation на macOS и сверку contract fixtures с `u-judge-server` на commit из `contract/server-ref`.
После изменения server contract обновите ref и выполните:

```shell
scripts/sync-server-contract.sh ../u-judge-server
```

Android transport против запущенного server (TLS, metadata, pairing request):

```shell
UJUDGE_SERVER_URL=https://127.0.0.1:8443 ./gradlew :composeApp:testAndroidHostTest --tests '*RealServerSmokeTest'
```

Проверка не заменяет smoke test на физических pilot devices.

Запуск Android выполняется из Android Studio. Для iOS откройте `iosApp/iosApp.xcodeproj` в Xcode и запустите схему `iosApp`.

## Документация

- [Описание U'Judge Client](docs/PROJECT.md)
- [Клиентские требования](docs/REQUIREMENTS.md)
- [Roadmap клиента](docs/ROADMAP.md)
- [Release notes](docs/RELEASE_NOTES.md)
- [Системная документация U'Judge Server](https://github.com/Martial-Arts-Sport-Software/u-judge-server/tree/main/docs)
- [Макеты Figma](https://www.figma.com/design/x5vY9DbXh3a0kv0lBPcNru/Judging-app)

## Лицензия

См. [LICENSE](LICENSE). Исходный код опубликован только для портфолио и демонстрации; копирование и коммерческое использование не разрешены условиями правообладателя.
