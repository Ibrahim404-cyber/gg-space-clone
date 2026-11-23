# GG Space Clone

A personal-use, clean-room **game space** app built with Kotlin + Jetpack Compose (Material 3). It is
inspired by the feature set and layout of GG Game Space (`com.zjx.gamebox`) but the code, UI and assets
here are written from scratch — no code or resources were copied from the original APK.

## Features

- **Game library** — scan installed apps, add/remove favourites, search, launch games
- **Recently played** tracking
- **Game booster** — kills background processes and reports reclaimed RAM
- **Floating overlay** — draggable in-game panel with Boost, Brightness, Do Not Disturb and Home
- **Live device stats** — free RAM, battery, CPU cores
- **Modern Material 3 UI** — dark neon theme, adaptive launcher icon, edge-to-edge layout
- Settings screen with permission status and source link

## Build

### GitHub Actions (recommended)

Push to `main`/`master`, or open the **Actions** tab and run **"Build GG Space APK"** manually.
The workflow builds both debug and release APKs and uploads them as artifacts:

- `gg-space-debug` → `app-debug.apk`
- `gg-space-release` → `app-release-unsigned.apk`

### Locally

You need JDK 17, Android SDK platform 35 + build-tools 35, and (optionally) `ANDROID_HOME` set.

```sh
./gradlew assembleDebug
# output: app/build/outputs/apk/debug/app-debug.apk
```

## Permissions used

`SYSTEM_ALERT_WINDOW` (floating tools), `FOREGROUND_SERVICE` / `FOREGROUND_SERVICE_SPECIAL_USE`
(overlay service), `POST_NOTIFICATIONS`, `KILL_BACKGROUND_PROCESSES` (booster), `WRITE_SETTINGS`
(brightness), `ACCESS_NOTIFICATION_POLICY` (DND). All are optional except the foreground service
needed by the overlay; features degrade gracefully when a permission is denied.

## Notes

For personal use only. This project is not affiliated with, endorsed by, or connected to the original
GG Game Space app. The release APK from CI is unsigned; sign it locally if you want to install it as
an update over an existing signed build.
