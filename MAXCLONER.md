# MaxCloner

An Android app-cloning / multi-space app. Runs multiple independent copies of the
same app on one device, sandboxed in a virtual container, without root.

Free and unrestricted by design: no ads, no subscription, no clone limit.

## Relationship to upstream

This is a fork of [ALEX5402/NewBlackbox](https://github.com/ALEX5402/NewBlackbox)
(Apache License 2.0), itself a maintained continuation of the dissolved
FBlackBox/BlackBox virtual-engine project.

- `main` branch — pristine upstream, never modified. Pull upstream fixes here.
- `maxcloner` branch — this fork's changes. Rebase onto `main` after pulling.

## Why there is no paywall to remove

Commercial cloners built on this same engine class gate multi-cloning behind a
subscription. That gate is added by those vendors; it does not exist upstream:

- `BUserManagerService.createUser()` has no cap on clone spaces.
- `BPackageManagerService.installPackageAsUser()` auto-creates a clone space on
  demand if it does not exist.
- No ad SDK is present (verified: AdMob, AppLovin, Unity, IronSource, Vungle,
  StartApp, AppsFlyer, Adjust — zero references).
- No billing SDK or entitlement flag is present.

The `com.android.vending.BILLING` permission in `Bcore/src/main/AndroidManifest.xml`
is **not** monetisation of this app. It is declared so that *cloned* apps which use
Google Play in-app billing keep working inside the sandbox. Leave it.

## Changes made in this fork

| File | Change |
|---|---|
| `app/build.gradle` | `applicationId` → `com.maxcloner.app`; APK output name → `MaxCloner_*.apk` |
| `app/src/main/res/values/strings.xml` | `app_name` → MaxCloner |
| `Bcore/src/main/cpp/Utils/AntiDetection.cpp` | added `/data/data/com.maxcloner.app` to the hidden-paths list |
| `gradle.properties` | Gradle JVM heap 2048m → 4096m |

### Split APK (App Bundle) support

Upstream only ever cloned `base.apk`. Modern Play Store apps ship as App Bundles,
where the base APK contains **no native code at all** — every `.so` lives in
`split_config.<abi>.apk`, and high-density resources in `split_config.<dpi>.apk`.

The result was that cloned bundle apps got zero native libraries and no split
resources. Symptom seen in the wild: eFootball 2026 launched, reached its
"downloading additional data" screen, then failed with a generic "check your
storage and network" error — the game's catch-all message for a failed file
operation.

Upstream's `DexCrashPrevention` treats `split_config.*.apk` as "problematic" and
falls back to the base APK. That is the workaround that caused the bug, not a fix.
It only logs, so it does not interfere with the real fix below.

| File | Change |
|---|---|
| `BEnvironment.java` | `splits/` dir per package + `getInstalledSplitPaths()` |
| `installer/CopyExecutor.java` | copies every split; extracts native libs from **each** split, not just base |
| `pm/PackageManagerCompat.java` | populates `ApplicationInfo.splitSourceDirs` / `splitPublicSourceDirs` |
| `app/BActivityThread.java` | class loader dexPath = base + all splits; also now passes `nativeLibraryDir`, which was previously omitted entirely |

Split paths are not persisted in package settings — they are re-derived by listing
the `splits/` directory, so no migration of existing installs is needed.

**The fix applies at install time.** Clones created before this change stay broken;
they must be deleted and re-cloned.

### Why AntiDetection.cpp had to change

That file holds a blocklist of filesystem paths hidden from apps running inside the
sandbox, so they cannot detect that they are being virtualised. It hardcoded the
*upstream* host package's data directory. Renaming the app without adding the new
data directory would leave `/data/data/com.maxcloner.app` visible to every cloned
app, making MaxCloner trivially detectable.

### What must NOT be renamed

The internal Java package `top.niunaijun.blackbox` must stay exactly as is:

- Native code resolves it by string — `BoxCore.h` defines
  `VMCORE_CLASS "top/niunaijun/blackbox/core/NativeCore"`, and JNI `FindClass`
  calls use the same literal paths.
- `Bcore/src/main/AndroidManifest.xml` names ~100 proxy components by that package.

Renaming it breaks the engine at runtime with no compile-time error.

Renaming `applicationId` *is* safe: all 53 content-provider authorities in the Bcore
manifest are declared with the `${applicationId}` placeholder and rewrite themselves.

## Toolchain

The upstream README says JDK 17. That is wrong — the build sets
`sourceCompatibility`/`targetCompatibility` to `VERSION_21` and `jvmTarget = '21'`,
so JDK 17 cannot compile it.

| Component | Version | Location on this machine |
|---|---|---|
| JDK | Temurin 21.0.12 | `C:\Users\ashiq\Android\jdk\jdk-21.0.12+8` |
| Android SDK | platform 35, build-tools 35.0.0 | `C:\Users\ashiq\Android\Sdk` |
| NDK | 29.0.13846066 | `C:\Users\ashiq\Android\Sdk\ndk` |
| Gradle | 8.14.5 (via wrapper) | downloaded by `gradlew` |

`local.properties` points at the SDK and is intentionally not committed.

## Building

```bash
./gradlew assembleDebug
```

Output lands in `app/build/outputs/apk/debug/`. The build produces per-ABI splits
(`arm64-v8a`, `armeabi-v7a`) plus a universal APK. Install the one matching your
device, or the universal one if unsure.

```bash
./gradlew assembleRelease
```

## Signing

Release builds are signed with `maxcloner-release.jks` (4096-bit RSA, valid ~30
years), located one directory above the repo. Credentials live in
`keystore.properties`, which is gitignored along with `*.jks`.

If `keystore.properties` is absent the build falls back to the debug key rather
than failing, so a fresh clone still builds.

**Back up the keystore.** Android identifies an app by its signing key. Lose it and
you can never update an installed MaxCloner — users would have to uninstall, losing
every clone and all data inside it.

## Environment gotchas on this machine

Two things that silently break a headless setup, both already worked around:

1. **SDK licences.** `sdkmanager --licenses` reads acceptance from the console. With
   no TTY it accepts nothing, skips every package, and *still exits 0*. The licence
   hash files in `$ANDROID_HOME/licenses/` were written directly instead.
2. **Java IPv6 stall.** Java attempts IPv6 first and hangs connecting to
   `services.gradle.org`, even though the host resolves fine from PowerShell. This
   breaks both the wrapper download and Maven/Google dependency resolution. Fixed
   with `-Djava.net.preferIPv4Stack=true` in `gradle.properties` and a persistent
   `GRADLE_OPTS`. The wrapper's `networkTimeout` was also raised from 10s to 120s.

## Known limitations

These are properties of the virtualization approach, not bugs in this fork, and they
affect commercial cloners equally:

- Apps that check Play Integrity / SafetyNet (banking, payment, some messaging) will
  detect the sandbox and refuse to run. No userspace engine reliably defeats this.
- Android 15 support is recent and not battle-tested; individual apps may crash.
- `targetSdkVersion` is deliberately pinned to 28 to keep hidden-API access working.
  Raising it will break the engine. It also means this cannot be published on Google
  Play, which requires a recent target SDK. Sideload only.
