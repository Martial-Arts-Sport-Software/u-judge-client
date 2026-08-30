# U'Judge Client

Мобильный судейский пульт U'Judge System для Android и iPhone, построенный на Kotlin Multiplatform и Compose Multiplatform.

Проект находится в разработке. Целевая версия U'Judge v1 Pilot предназначена для полевого испытания и не является production-релизом.

## Возможности pilot

- русский и английский интерфейс;
- online-подключение к площадке через mDNS, pairing и WebSocket;
- offline-калькулятор технических дисциплин;
- Kerugi, Tanbon, Hosinsool, Pair Freestyle, Group Freestyle и Weapon Freestyle;
- надёжная доставка событий через durable outbox, ACK и reconnect;
- локальное сохранение технической оценки через `Save`;
- окончательная отправка оценки через `Send`.

## Текущее состояние

UI всех шести дисциплин и локальные модели технической оценки уже существуют. mDNS находит `_u-judge._tcp.local.`, но настоящий handshake, pairing, HTTP/WebSocket transport, ACK/outbox и отправка оценок ещё не реализованы. Боевые кнопки и `Save`/`Send` пока содержат пустые обработчики.

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

Запуск Android выполняется из Android Studio. Для iOS откройте `iosApp/iosApp.xcodeproj` в Xcode и запустите схему `iosApp`.

## Документация

- [Описание U'Judge Client](docs/PROJECT.md)
- [Клиентские требования](docs/REQUIREMENTS.md)
- [Roadmap клиента](docs/ROADMAP.md)
- [Системная документация U'Judge Server](https://github.com/Martial-Arts-Sport-Software/u-judge-server/tree/main/docs)
- [Макеты Figma](https://www.figma.com/design/x5vY9DbXh3a0kv0lBPcNru/Judging-app)

## Лицензия

См. [LICENSE](LICENSE). Исходный код опубликован только для портфолио и демонстрации; копирование и коммерческое использование не разрешены условиями правообладателя.
