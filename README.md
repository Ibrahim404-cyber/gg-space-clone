# GG Space Clone

A personal-use, clean-room **game space** app built with Kotlin + Jetpack Compose (Material 3). Inspired by
the feature set of GG Game Space (`com.zjx.gamebox`), but all code, UI and assets are written from scratch —
no code or resources were copied from the original APK.

## Features

### Game space
- **Game library** — scan installed apps, add/remove, search, favourites, launch, recently played
- **Game booster** — kills background processes and reports reclaimed RAM
- **Modern Material 3 UI** — dark neon theme, adaptive icon, edge-to-edge

### Floating tool menu (draggable ball → expandable grid)
Boost, brightness ±, DND, volume ±, screenshot, 30-second screen recording, virtual crosshair,
Vision Booster magnifier, device-info monitor, rotation lock/auto, screen off, Home/Back/Recents,
kill apps, play macro, open Macro/Keymap.

- **Virtual crosshair** — dot / cross / circle / circle-cross / square styles, colour, size, transparency
- **Vision Booster** — MediaProjection magnifier with hunting-mode colour filters (Night, Night Vision,
  Hawkeye, Pure)
- **Device info monitor** — FPS, CPU usage, CPU temperature, battery, network speed, current mode, time

### Macro & Keymap (the "Macro" tab)
- **Macro recorder** — records taps/swipes with timings into a floating capture layer, then saves them
- **Macro playback** — from the Macro tab or the floating "Play macro" button
- **Keymap** — press any key, bind it to a screen point or to a saved macro; a custom AccessibilityService
  dispatches the gestures. Enable **GG Space Macro & Keymap** in Android accessibility settings first.

### Activation (the "Activate" tab)
- **Root** — runs privileged commands through `su` (Magisk / KernelSU / APatch)
- **Shizuku** — runs shell commands through Shizuku without root (install Shizuku, start service, grant permission)
- **Limited mode** — no activation; unprivileged features still work

Screenshot, screen recording and keyevent tools require Root or Shizuku activation.

## Build

### GitHub Actions
Push to `main`/`master`, or open the **Actions** tab and run **"Build GG Space APK"**. The workflow
uploads `gg-space-debug` and `gg-space-release` artifacts.

### Locally
JDK 17 + Android SDK platform 35 / build-tools 35:

```sh
./gradlew assembleDebug
```

## Notes

For personal use only. Not affiliated with the original GG Game Space app. The CI release APK is unsigned;
sign it locally before installing over an existing signed build.
