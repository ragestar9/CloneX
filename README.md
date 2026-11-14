# CloneX

No-root Android app cloner in Kotlin + Jetpack Compose, built on the
open-source NewBlackbox virtualization engine (Apache 2.0).

## What it does

- **Clone installed apps** — run a second copy of any app with its own
  login and data, so two accounts work side by side.
- **Numbered home-screen icons** — clones appear as their own icons,
  "WhatsApp 1", "WhatsApp 2", with a green number badge.
- **No root, no work profile** — clones run in a private virtual space
  inside CloneX, not in an Android work/personal profile.
- **Split-APK support** — modern App Bundle apps clone correctly
  (ported from the MaxCloner fork).

## How it works

Cloned apps run inside stub processes of the host app. The engine hooks
system services, redirects storage per clone, and fakes device info, so
each clone believes it is a normally installed app. See `Docs.md` and
`MAXCLONER.md` for the engine details.

## Building

```bash
./gradlew assembleDebug
```

Requirements: JDK 21, Android SDK 35, NDK 29.0.13846066, Gradle 8.14.5.
The debug APK is also built by GitHub Actions on every push to `main`
(see `.github/workflows/build-debug.yml`).

## Limits (honest)

- Apps with strong integrity checks (banking, payments) can detect the
  sandbox and refuse to run. Most social and messaging apps clone fine.
- Cannot go on Google Play: the engine pins `targetSdk 28`. Sideload only.

## Credits

- Engine: [ALEX5402/NewBlackbox](https://github.com/ALEX5402/NewBlackbox)
  (Apache 2.0), with split-APK support from
  [MaxCloner](https://github.com/ashiquesheriefofficial-max/MaxCloner).
