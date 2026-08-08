# Сборка libbox для Pokolenie

Полноценный туннель требует `app/libs/libbox.aar` (sing-box mobile). Без AAR приложение собирается и работает в stub-режиме: UI, подписки, пинг, Warp-конфиги и JSON-конфиг — да; DPI/proxy-ядро — нет.

## Требования

- Go 1.22+
- Android NDK / SDK
- [sagernet gomobile](https://github.com/SagerNet/gomobile)

## Команды

```bash
go install github.com/sagernet/gomobile/cmd/gomobile@v0.1.8
gomobile init

git clone https://github.com/SagerNet/sing-box.git
cd sing-box
git checkout v1.11.15

gomobile bind -v \
  -androidapi 26 \
  -javapkg=io.nekohasekai \
  -libname=box \
  -tags "with_gvisor,with_quic,with_wireguard,with_utls,with_clash_api" \
  ./experimental/libbox

cp libbox.aar /path/to/Pokolenie/app/libs/
```

После копирования Gradle подхватит AAR автоматически (`app/build.gradle.kts`).

## geoip / geosite

Положите актуальные `geoip.dat` и `geosite.dat` в `app/src/main/assets/geo/` (опционально для расширенного routing). Базовый whitelist в MVP работает и на domain_suffix правилах.
