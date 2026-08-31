# U'Judge Client

Мобильный судейский пульт U'Judge System для Android и iPhone, построенный на Kotlin Multiplatform и Compose Multiplatform.

Проект находится в разработке. Текущая версия `0.1.0` является U'Judge v1 Pilot для полевого испытания и не является production-релизом.

## Возможности pilot

- русский и английский интерфейс;
- поиск площадок в локальной сети через mDNS;
- offline-калькулятор технических дисциплин;
- Kerugi, Tanbon, Hosinsool, Pair Freestyle, Group Freestyle и Weapon Freestyle;

## Текущее состояние

UI всех шести дисциплин и локальные модели технической оценки уже существуют. mDNS находит `_u-judge._tcp.local.`; повторный поиск отменяет предыдущий scan, а removed services исчезают из списка. Выбор площадки хранится в явной connection state machine, но не считается pairing или online connection. Настоящий handshake, pairing, HTTP/WebSocket transport, ACK/outbox и отправка оценок ещё не реализованы. Боевые кнопки и `Save`/`Send` пока содержат пустые обработчики.

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
- [Release notes](docs/RELEASE_NOTES.md)
- [Системная документация U'Judge Server](https://github.com/Martial-Arts-Sport-Software/u-judge-server/tree/main/docs)
- [Макеты Figma](https://www.figma.com/design/x5vY9DbXh3a0kv0lBPcNru/Judging-app)

## Лицензия

См. [LICENSE](LICENSE). Исходный код опубликован только для портфолио и демонстрации; копирование и коммерческое использование не разрешены условиями правообладателя.
