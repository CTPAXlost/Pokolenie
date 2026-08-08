# Pokolenie

Android VPN-клиент со своим UI: whitelist-маршрутизация, мульти-источники VLESS/Trojan с GitHub, пинг с автоочисткой, генерация Warp и раздельное туннелирование приложений.

## Возможности

- Собственный Compose UI (бренд Pokolenie), не форк SagerNet/NekoBox
- Несколько GitHub-источников ключей (VLESS / Trojan), дедуп
- Кнопка **Пинг** / **Пинг всех** — нет ответа → сервер удаляется
- **Whitelist всегда включён** (нельзя выключить)
- Генерация Warp через Cloudflare API + Amnezia-пресет в `.conf`
- MTU, DNS (system / custom / DoH), IPv6, keepalive
- Per-app split tunnel (все / только выбранные / исключить)
- Ядро: sing-box **libbox** (опциональный AAR)

## Сборка через GitHub (рекомендуется)

CI уже настроен: [`.github/workflows/android.yml`](.github/workflows/android.yml).

1. Залей репозиторий на GitHub (`gh repo create` или обычный `git push`).
2. Открой **Actions** → workflow **Android CI** → скачай artifact `pokolenie-apk`.
3. Для релиза: `git tag v1.0.0 && git push origin v1.0.0` — APK попадёт в Releases.

Подробнее: [docs/GITHUB_BUILD.md](docs/GITHUB_BUILD.md).

## Локальная сборка

1. Установите [Android Studio](https://developer.android.com/studio) (JDK 17, SDK 35).
2. Скопируйте `local.properties.example` → `local.properties` и укажите `sdk.dir`.
3. (Рекомендуется) Соберите `libbox.aar` по [docs/BUILD_LIBBOX.md](docs/BUILD_LIBBOX.md) и положите в `app/libs/`.
4. Откройте проект в Android Studio → Run.

```bash
./gradlew :app:assembleDebug
```

Без `libbox.aar` APK соберётся в stub-режиме: UI и конфиги работают, полноценный proxy-engine — после добавления AAR.

## Структура

```
app/          # UI, Room, подписки, пинг, Warp, VpnService
core/         # VpnEngine abstraction
docs/         # libbox и источники
```

## Экраны

1. Главная — Connect / Warp / обновление ключей / пинг
2. Серверы — список, выбор, пинг, удаление
3. Источники — GitHub URL
4. Warp — генерация и экспорт `.conf`
5. Apps — раздельное туннелирование
6. Настройки — MTU / DNS / сеть (whitelist locked)
