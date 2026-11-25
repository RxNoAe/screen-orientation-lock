# Screen Orientation Lock

> A root-free Android utility that locks the screen orientation **globally** — including for apps that declare their own orientation: car launchers, many video/music players, and some games.

[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
[![Platform](https://img.shields.io/badge/Android-8.0%2B-3ddc84.svg)](https://developer.android.com)
[![minSdk](https://img.shields.io/badge/minSdk-26-orange.svg)](app/build.gradle.kts)
[![Release](https://img.shields.io/github/v/release/RxNoAe/screen-orientation-lock)](../../releases/latest)

<p align="center">
  <b>Chinese docs → <a href="README.zh.md">README.zh.md</a></b>
</p>

---

## What's new in 1.0.2

- **False auto-landscape fixed.** The old rule treated any device with a non-zero keyboard type as a keyboard. Phones report `KEYBOARD_TYPE_NON_ALPHABETIC` for volume keys, navigation and headset remotes, so a phone with nothing plugged in flipped to landscape. Auto-landscape now requires a real alphabetic keyboard.
- **Live input status.** The new **Input devices** section shows what Android actually reports: `Keyboard + Mouse connected`, or `No keyboard or mouse connected`.
- **Clearer settings.** Cards are grouped under **Behaviour** and **Input devices** headings.
- **1.7 MB signed release.** The previous CI artifact was an unminified debug build at 11.7 MB. Releases now use R8, resource shrinking and a stable signing key.

---

## What makes this different

Most root-free orientation lockers only write the orientation value into `Settings.System`. That is **completely ineffective against apps that declare their own orientation**, because Android's precedence is:

```
app-declared orientation  >  global system setting
```

Car launchers (e.g. Qing Launcher, which declares `sensorLandscape`), many video and music apps, and some games all fall into that category. They snap the screen back, and you cannot lock them.

**This project instead adds a 0×0 overlay window and declares the orientation in its window parameters.** Overlay windows sit above ordinary application windows in the window hierarchy, so WindowManager takes their orientation request into account when computing the display rotation — and the app's own declaration is overridden.

It needs only the "Display over other apps" permission. **No root, no Shizuku, no ADB.**

### Verified on a real device

With a car launcher declaring `sensorLandscape` in the foreground:

| Action | Display |
|---|---|
| Lock to portrait | `1920x1080` → **`1080x1920`** |
| Hold for 20 seconds | **stays portrait, not a single flip** |
| Kill this app to drop the overlay | the launcher **immediately reclaims landscape** |

That last step is the control experiment — it rules out coincidence and proves the overlay is what's holding the orientation.

---

## Features

- **6 orientation modes**: portrait, reverse portrait, landscape, reverse landscape, current orientation, auto (follow sensor)
- **Persistent notification**: portrait / landscape / unlock / reverse buttons — switch without opening the app
- **Auto-start on boot**: restores the last lock after a reboot
- **Guard mode**: re-applies the lock if something else changes the orientation
- **Keyboard / mouse auto-landscape** (opt-in): switch to landscape when a peripheral connects, and optionally restore your previous mode when it is unplugged. Only a real alphabetic keyboard triggers it, so built-in key devices are ignored.
- **Live detection status**: the settings screen shows whether Android currently sees a keyboard, a mouse/touchpad, both, or neither
- **Dark UI**: per-orientation gradient colours, a rotatable phone illustration

---

## Install

1. Download `OrientLock-1.0.2.apk` from the [latest release](../../releases/latest)
2. Open it — an amber permission card appears at the top
3. Tap **Enable overlay** and turn the switch on in the system settings page
4. Go back and you're set

Upgrading from 1.0.1? Uninstall it first. The signing key changed, so Android refuses to install over the old build; app settings reset once.

### Two permission paths

| Permission | Capability | Recommendation |
|---|---|---|
| **Display over other apps** | Forces the display orientation via an overlay — **works on apps that declare their own orientation** | **Recommended**; this alone is enough |
| Modify system settings | Writes the system orientation setting — only affects apps that follow the system | Optional supplement |

**Either one is enough to lock.** With only "Modify system settings", car launchers and similar apps cannot be locked.

---

## Known limitations

**Orientations fixed by the system itself cannot be locked** — for example some car head units' boot splash, or a lock screen pinned by a particular ROM. That is outside what any app can reach; Android exposes no entry point for it.

Also, the overlay requires the app process to be alive. This app holds it in a foreground service, with boot auto-start and a guard. If the system or an OEM's background manager kills it, the lock goes with it. In that case, allow this app to auto-start in the system's "Auto-start management" and remove background restrictions — the in-app "System adaptation" card deep-links to those pages.

---

## Build

### Requirements

- JDK 21 (Android Studio's bundled JBR works well)
- Android SDK, `compileSdk 36`
- Gradle 8.12 (the wrapper is committed)

### Debug APK

```bash
git clone <repo>
cd screen-orientation-lock
./gradlew assembleDebug
```

Output: `app/build/outputs/apk/debug/app-debug.apk`

Debug builds are not minified, so the APK is much larger than a release build.

### One gotcha worth knowing

`gradle.properties` hardcodes

```properties
org.gradle.java.home=C:/Program Files/Android/Android Studio/jbr
```

because the original development machine's default JDK was Java 26, which is incompatible with AGP 8.9.3 — without this line the build fails with `Unsupported class file major version`. **On another machine, change this line to your local JDK 21 path**, or delete it and set `JAVA_HOME`.

Gradle only reads this property from `gradle.properties` or `JAVA_HOME` — putting it in `local.properties` has no effect.

### Run the tests

```bash
./gradlew testDebugUnitTest
```

120 unit tests, all running on the JVM. No emulator, no Robolectric.

### Build a signed release

The repository does not contain a signing key. To build a signed release, create `keystore.properties` at the repository root (it is in `.gitignore`):

```properties
storeFile=keystore/release.jks
storePassword=your-password
keyAlias=your-alias
keyPassword=your-password
```

Then run:

```bash
./gradlew assembleRelease
```

Without this file the release build degrades to unsigned — **the build does not fail**. An open-source project should build straight after `git clone`.

For CI, `.github/workflows/build.yml` restores the key from four repository secrets:

| Secret | Contents |
|---|---|
| `ANDROID_KEYSTORE_B64` | `base64 -w0 keystore/release.jks` |
| `ANDROID_KEYSTORE_PASSWORD` | keystore password |
| `ANDROID_KEY_ALIAS` | key alias |
| `ANDROID_KEY_PASSWORD` | key password |

Without them the workflow falls back to a debug build instead of failing.

---

## Code structure

```
app/src/main/java/com/orientlock/
├── OrientLockApp.kt        Application: notification channel, fallback service start
├── MainActivity.kt         Single activity
├── domain/                 Pure Kotlin, zero Android dependencies, JVM-unit-testable
│   ├── NaturalOrientation.kt    Device natural orientation + pure detection (incl. DisplayRotation)
│   ├── OrientationMode.kt       6 modes × English label / storage name / system value mapping
│   ├── OrientationGuard.kt      Guard drift decision
│   ├── InputAutoLandscape.kt    Keyboard/mouse device rules + trigger decision
│   ├── AppSettings.kt           Settings data model
│   └── OrientationRepository.kt Data-access interface (dependency inversion, for testability)
├── data/
│   ├── SystemOrientationAccess.kt  Platform reads/writes (the only place touching Settings.System)
│   ├── SystemOrientationWriter.kt  Write policy (probe mutex, double sampling, validate-then-write)
│   ├── SettingsOrientationRepository.kt  DataStore + guard heartbeat
│   └── Preferences.kt              DataStore delegate
├── system/
│   ├── OverlayOrientationController.kt  ★ Overlay-based orientation control (the key trick)
│   ├── OverlayOrientation.kt            Mode → overlay orientation value mapping
│   ├── OrientationService.kt            Foreground service: notification + guard + holds the overlay
│   ├── InputDeviceWatcher.kt            InputManager scan + hotplug callbacks
│   ├── BootReceiver.kt                  Start after boot / app update
│   ├── NotificationHelper.kt            Notification channel and building
│   ├── PermissionChecker.kt             Permission query interface + Android implementation
│   ├── PermissionIntents.kt             System settings page intents
│   └── ServiceGateway.kt                Service start/stop interface + Android implementation
└── ui/
    ├── theme/                  Colours / typography / theme / mode gradients (single source)
    ├── MainViewModel.kt        UI state and user intents
    ├── MainScreen.kt           Main screen
    ├── AppRoot.kt              ViewModel ↔ theme wiring
    └── components/             PhonePreview / ModeCard / StatusPill / InputStatusRow / SectionHeader / …
```

**Layering principle**: the `domain` layer imports nothing from `android.*`, so every unit test runs on the JVM. All `Settings.System` access is confined to a single class, `SystemOrientationAccess` — the highest-risk surface in the app, and therefore the one with dedicated tests.

---

## Compatibility

- Minimum Android 8.0 (API 26)
- `targetSdk` stays at 35. Recent Android releases ignore app-declared orientation on large screens (tablets, foldables) when the app targets the newest SDK. Targeting 35 keeps the lock effective on those devices.

### Always enable every signature scheme when packaging

These three lines in `build.gradle.kts` **must not be removed**:

```kotlin
enableV1Signing = true
enableV2Signing = true
enableV3Signing = true
```

With `minSdk >= 24`, AGP signs **v2 only** by default. Some OEM installers still validate via the v1 (JAR signing) path, and reject a v2-only package with *"App not installed: package appears to be invalid"*.

v1.0.0 of this project hit exactly that — it would not install on a Lenovo Xiaoxin Pad Plus 2023. v1.0.1 fixes it.

---

## Development notes

- [`docs/development-log.md`](docs/development-log.md) — full development log and pitfall catalogue, focused on the bugs that **compiled fine, passed all unit tests, and still broke on a real device** (overlay-based orientation control, APK signature schemes, CI configuration, and more). Written in Chinese.
- [`docs/device-test-checklist.md`](docs/device-test-checklist.md) — device test checklist. Written in Chinese.

---

## License

[MIT](LICENSE)
