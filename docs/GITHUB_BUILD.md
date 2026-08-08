# Сборка через GitHub Actions

CI лежит в [`.github/workflows/android.yml`](../.github/workflows/android.yml).

## Что делает workflow

На каждый push/PR в `main`/`master` и по кнопке **Run workflow**:

1. Поднимает JDK 17 + Android SDK
2. Собирает `assembleDebug` и `assembleRelease` (unsigned)
3. Кладёт APK в Artifacts → `pokolenie-apk`

На тег `v*` дополнительно публикует GitHub Release с APK.

## Первый пуш

```bash
cd C:\Users\leonj\Projects\Pokolenie
git init
git add .
git commit -m "Initial Pokolenie Android client"
gh auth login
gh repo create Pokolenie --private --source=. --remote=origin --push
```

Или создай репозиторий на сайте и:

```bash
git remote add origin https://github.com/<user>/Pokolenie.git
git branch -M main
git push -u origin main
```

## Где скачать APK

1. GitHub → вкладка **Actions** → последний успешный run **Android CI**
2. Внизу **Artifacts** → `pokolenie-apk`
3. Либо Release, если запушил тег: `git tag v1.0.0 && git push origin v1.0.0`

## Подпись release (опционально)

По умолчанию release APK **unsigned** (для тестов ставь debug).  
Чтобы подписывать в CI, добавь secrets репозитория:

- `KEYSTORE_BASE64`
- `KEYSTORE_PASSWORD`
- `KEY_ALIAS`
- `KEY_PASSWORD`

и расширь workflow шагом `signing` (можно добавить позже).

## libbox

Без `app/libs/libbox.aar` CI соберёт stub-сборку (UI + подписки + Warp-конфиги).  
Полноценное ядро — положи AAR в репозиторий (осторожно с лицензией/размером) или скачивай его в workflow отдельным шагом.
