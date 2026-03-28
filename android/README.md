# Android module (Room + sync)

Open the **`android/`** folder in Android Studio as the Gradle project root.

## Stack

- **Room** `2.6.1` with **KSP** (Kotlin Symbol Processing), matching [current Android guidance](https://developer.android.com/training/data-storage/room).
- **OkHttp** for `GET` of the full JSON snapshot.
- **kotlinx.serialization** for parsing the wrapper `{ "businesses": [ ... ] }` payload from GitHub Pages.

## Wiring (example)

```kotlin
val db = AppDatabase.getInstance(applicationContext)
val http = OkHttpClient()
val repository = BusinessRepository(
    database = db,
    businessDao = db.businessDao(),
    syncMetaDao = db.syncMetaDao(),
    httpClient = http,
)

// e.g. from a ViewModel scope
viewModelScope.launch {
    repository.syncFromRemote("https://zavscom.github.io/adoni-directory-backend/data/adoni_full.json")
}
```

`BusinessRepository` takes **`AppDatabase`** in addition to the DAOs so `syncFromRemote` can use `withTransaction { }` and atomically clear, insert, and update sync metadata.

## Gradle wrapper

If `gradlew` / `gradlew.bat` is missing, generate it once (from `android/`):

```bash
gradle wrapper --gradle-version 8.10.2
```

Or use **File → New → Import** and let Android Studio create/sync the wrapper.

## Versioning and archived APKs

On any Gradle invocation that runs **`assemble*`** or **`bundle*`**, `defaultConfig` gets a **new** `versionCode` (UTC epoch seconds) and **`versionName`** `1.0.<yyyyMMdd-HHmmss>` (UTC), so each packaged build is unique.

After **`assembleDebug`** / **`assembleRelease`**, a task **`archive…Apk`** runs and copies every APK from `app/build/outputs/apk/<variant>/` into **`android/apk-archive/`** with a name like:

`AdoniDirectory-b7-v1.0.20260328-123456-release-app-release-20260328-123501.apk`

- **`b7`** — persistent increment from **`android/archive-counter.txt`** (commit this file if you want the counter shared across machines).
- **`v…`** — `versionName` with unsafe characters replaced.
- **Trailing UTC stamp** — time when the archive step ran (may differ by a second from `versionName`).

`apk-archive/` is listed in **`android/.gitignore`** so binaries are not committed by default.

## Background sync (WorkManager)

- **`DirectoryApp`** (`android:name` in the manifest) initializes **`DirectoryAppServices`** (tiny service locator for **`BusinessRepository`**), then schedules:
  - **Periodic** sync every **24 hours** (`directory_sync`, `ExistingPeriodicWorkPolicy.KEEP`) with **network required**.
  - **One-time** seed on first process start (`directory_sync_seed`) so data loads soon after install.
- **`DirectorySyncWorker`** (`CoroutineWorker`) calls **`syncFromRemote(FULL_URL)`**; **`IOException`** or **`SerializationException`** → **`Result.retry()`**.
- URL constant: **`sync/SyncConstants.kt`** → **`FULL_URL`** (set to your GitHub Pages JSON).

## Build note: `stripDebugDebugSymbols` / `libandroidx.graphics.path.so`

If you see **“Unable to strip … libandroidx.graphics.path.so, packaging them as they are”**, that is **normal** on many Windows setups: the NDK strip tool skips that AndroidX native library and the APK still contains it unchanged. The `jniLibs.keepDebugSymbols` entry in `app/build.gradle.kts` tells AGP to keep that `.so` unstripped. You can ignore the message if the build succeeds.

## Compose UI (MVVM)

- **`DirectoryViewModel`** + **`DirectoryUiState`**: categories (`All` + distinct from Room), search, category filter, businesses list, **`lastUpdated`** from **`observeLastSyncAt()`**.
- **`DirectoryScreen`**, **`BusinessDetailScreen`**, **`TownDirectoryTheme`**, **`MainActivity`** + **Navigation** (`directory` / `detail/{businessId}`).
- **`BusinessDetailViewModel`** loads the row by id via **`observeBusinessById`**.
