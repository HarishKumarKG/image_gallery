# Harish — Gallery for Android

A modern, offline photo & video gallery for Android built with **Kotlin**, **Jetpack Compose** and a
**multi-module clean architecture** (the same structure Google uses in *Now in Android*).
It shows every photo and video on your phone in a date timeline, groups them into albums, and lets you
view, zoom, edit, search, share, organise, trash and lock them — with polished light & dark themes.

![CI](https://github.com/HarishKumarKG/image_gallery/actions/workflows/ci.yml/badge.svg)
![Platform](https://img.shields.io/badge/platform-Android-3DDC84?logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-2.4.20-7F52FF?logo=kotlin&logoColor=white)
![Compose](https://img.shields.io/badge/Jetpack%20Compose-BOM%202026.09.00-4285F4?logo=jetpackcompose&logoColor=white)
![Modules](https://img.shields.io/badge/modules-13-orange)
![minSdk](https://img.shields.io/badge/minSdk-30-blue)
![targetSdk](https://img.shields.io/badge/targetSdk-37-blue)

---

## Features

### Photos (timeline)
- **Every photo and video**, newest first, under **date headings** — *Today*, *Yesterday*, *Mon, 14 Oct*, *December 2025*
- **Pinch to resize the grid** — 2 to 6 photos per row, remembered
- Group by **day or month** (Settings)
- Video thumbnails show their **length**; favourites show a small ♥
- Updates **by itself** when you take a photo or another app adds / deletes one

### Albums
- One album per folder (*Camera*, *Screenshots*, *WhatsApp Images*…) with cover and count
- **Collections**: Favourites · Videos · Locked folder · Trash
- Sort by *most recent*, *name* or *most items*
- **Hide from Photos** — long-press an album (e.g. Screenshots) to keep it out of the timeline; it stays in Albums

### Viewer
- **Swipe** through the list you opened it from (an album, a search, favourites…)
- **Pinch** or **double-tap** to zoom (up to 5×), drag to move around
- **Videos play** right in the viewer (Media3 ExoPlayer)
- Tap once for **true full screen** (system bars hide)
- **Info**: date, resolution, size, folder, camera details (aperture, shutter, ISO, focal length) and **location** with *Open in Maps*
- **Share**, **Edit**, **Favourite**, **Delete**, **Rename**, **Move / Copy to album**, **Move to Locked folder**, **Use as** (wallpaper, contact photo)
- **Slideshow** (1–10 s per photo)

### Editor
- **Crop** — free or 1:1, 4:3, 3:4, 16:9, 9:16, with corner handles and a rule-of-thirds grid
- **Rotate** and **mirror**
- **8 filters** — Vivid, Warm, Cool, Mono, Noir, Sepia, Fade
- **Adjust** — brightness, contrast, saturation, warmth
- Always **saves a copy** (`IMG_1_edited.jpg`); the original is never touched

### Manage
- **Multi-select**: long-press, then tap more — or tap a day's circle to select the whole day
- **Delete to Trash** (30 days), **Restore**, **Delete for good**, **Empty trash** (days left on each item)
- **Favourites** use Android's own favourite flag, so other gallery apps see them too
- **Move / Copy** to any album or a **new album** (created in *Pictures/*)
- **Rename** files (extension kept, clashes checked)

### Search
- Type words — they all have to match: **names, albums, months, years, "video", "favourite"**
- Examples: `beach` · `october 2024` · `goa video` · `screenshots`
- Quick-pick chips: Videos, Favourites, years you have photos from, your biggest albums

### Locked folder
- Moves photos into the app's **private storage** — invisible to other apps, the system gallery and USB
- Opens only with your **fingerprint, face or screen lock**; locks again when you leave the app
- **No screenshots** or recent-apps preview while open
- **Move out** puts items back in their original folder

### Settings
| Setting | Default | What it does |
|---|---|---|
| Theme | System | System / Light / Dark (Material You colours on Android 12+) |
| Group timeline by | Day | Day or month headings |
| Photos per row | 4 | 2–6 (or pinch the grid) |
| Slideshow speed | 3 s | Seconds per photo |
| Hidden from Photos | — | Albums kept out of the timeline; tap *Show* to bring one back |
| Skip confirmations | Off | Android 12+ *Media management*: trash / move / favourite without a popup each time |
| Photo locations | Asked at start | Lets the info sheet show where a photo was taken |

---

## Tech stack

| Area | Library / tool |
|---|---|
| Language | Kotlin 2.4.20 |
| UI | Jetpack Compose (BOM 2026.09.00), Material 3, Material You dynamic colour |
| Architecture | Multi-module clean architecture, MVVM, unidirectional data flow |
| Build | Gradle 9.8, AGP 9.4.1 (built-in Kotlin), **convention plugins** in `build-logic/`, version catalog |
| Navigation | Navigation Compose 2.10.2 |
| Dependency injection | Hilt 2.60.1 (KSP 2.3.11) |
| Async | Kotlin Coroutines & Flow |
| Media | Android MediaStore, `ContentResolver.loadThumbnail`, `ImageDecoder`, Media3 1.9.0 (video) |
| Metadata | AndroidX ExifInterface (camera details, GPS) |
| Security | Framework `BiometricPrompt` (fingerprint / face / PIN), `FLAG_SECURE` |
| Tests | JUnit 4 — pure-Kotlin domain tests run in milliseconds |

**SDK:** `minSdk 30` (Android 11) · `targetSdk / compileSdk 37` · Java 17
No image-loading library: thumbnails come from Android itself and are cached in memory.

---

## Architecture

```
                         ┌───────────────────────────── :app ─────────────────────────────┐
                         │  MainActivity · permission gate · bottom tabs · NavHost          │
                         │  (the only module that knows every feature)                      │
                         └───┬────────┬─────────┬─────────┬─────────┬─────────┬────────┬───┘
                             │        │         │         │         │         │        │
 feature modules  →    :photos  :albums  :viewer  :editor  :search  :locked  :settings
 (screens + ViewModels; never depend on each other — they only call lambdas like onOpenMedia())
                             │
                             ▼
 core modules     →   :core:ui          shared UI: photo grid (pinch, selection), thumbnails,
                         │               zoomable image, video player, selection bar, dialogs
                      :core:designsystem theme, colours, type, icons, empty states
                         │
                      :core:data        MediaStore repository, actions (trash, favourite, move,
                         │               copy, rename), Locked folder store, editor saver, EXIF
                         ▼               ── implements the interfaces below (dependency inversion)
                      :core:domain      PURE KOTLIN: use cases, repository interfaces, timeline &
                         │               album grouping, search, rename rules, edit maths
                         ▼
                      :core:model       PURE KOTLIN: MediaItem, Album, MediaSource, settings…
```

**Rules that keep it clean**
- `:core:model` and `:core:domain` have **no Android code at all** → their unit tests run instantly on your computer.
- `:core:data` depends on `:core:domain` (not the other way round): the domain declares `MediaRepository`, data implements it with MediaStore. Swap MediaStore for a fake in tests without touching anything else.
- **Features never depend on each other.** The viewer doesn't know the albums screen exists; `:app` wires `onOpenMedia = navController::navigateToViewer`.
- **Convention plugins** (`build-logic/`) set up each kind of module in one line, e.g. a feature's whole build file is:
  ```kotlin
  plugins { alias(libs.plugins.harish.android.feature) }
  android { namespace = "com.harish.gallery.feature.photos" }
  ```

**How a delete works (with Android's permission popup)**
```
Grid ──long-press──▶ PhotosViewModel.moveToTrash(selected)
                         │
                         ▼
                   MediaActions.moveToTrash()  ──▶ ActionResult.NeedsConsent(popup)
                         │                                   │
                   ActionController (in core:ui) ◀───────────┘
                         │ shows Android's "Allow Gallery to move 3 items to trash?"
                         ▼
                   user taps Allow ──▶ onGranted() ──▶ MediaStore updates ──▶ ContentObserver
                                                                                  │
                   Snackbar "3 items moved to Trash" ◀── timeline re-groups ◀─────┘
```

**Module map**
```
build-logic/convention     harish.android.application · .library · .library.compose · .feature · harish.hilt · harish.jvm.library
core/model                 MediaItem, Album, MediaSource, TimelineSection, GallerySettings, LockedItem, MediaDetails
core/domain                repository/, usecase/, timeline/, album/, search/, rename/, edit/
core/data                  media/ (MediaStore + EXIF), actions/ (trash, favourite, move, copy, rename, save edits),
                           locked/ (Locked folder), settings/, di/ (Hilt)
core/designsystem          theme/, component/, GalleryIcons, 40 Material icons
core/ui                    MediaGrid, Thumbnails, Zoomable, VideoPlayer, ActionController, Selection*, Dialogs
feature/photos | albums | viewer | editor | search | locked | settings
app                        GalleryApp (Hilt), MainActivity, ui/GalleryRoot (tabs + NavHost), permissions
```

---

## Getting started

### Requirements
- **Android Studio** (latest stable) with **Android SDK Platform 37**
- **JDK 17+** (Android Studio's bundled JDK works)
- A device or emulator running **Android 11 (API 30) or newer** with some photos on it

### Build & run
```bash
git clone https://github.com/HarishKumarKG/image_gallery.git
cd image_gallery
./gradlew assembleDebug          # or press ▶ Run in Android Studio
./gradlew installDebug           # install on a connected phone
```

### Run the unit tests
```bash
./gradlew :core:domain:test testDebugUnitTest
```
Tests cover timeline grouping, albums, trash days, search words, rename rules, editor colour maths and
crop box, use cases with fake repositories, and selection helpers.

---

## Continuous integration

| Workflow | Runs when | What it does |
|---|---|---|
| `ci.yml` | Any push, or a pull request to `main` | Runs all unit tests, builds a debug APK (downloadable from the run's *Artifacts*) |
| `release.yml` | A `v*` tag is pushed / a GitHub Release is published | Runs tests, builds a **signed** APK + AAB and attaches them to the release |

Release signing uses four repository secrets: `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`.

## Permissions

| Permission | Why |
|---|---|
| `READ_MEDIA_IMAGES`, `READ_MEDIA_VIDEO` (Android 13+) / `READ_EXTERNAL_STORAGE` (11–12) | Show your photos and videos |
| `READ_MEDIA_VISUAL_USER_SELECTED` (Android 14+) | Works with "Allow limited access" (a banner offers *Allow all*) |
| `ACCESS_MEDIA_LOCATION` | Show where a photo was taken |
| `MANAGE_MEDIA` (optional, Android 12+) | Skip the confirmation popup for trash / move / favourite |
| `USE_BIOMETRIC` | Unlock the Locked folder with fingerprint / face |

No internet permission — the app works fully offline and nothing leaves the device.
Android shows its own confirmation before changing photos made by other apps (camera, WhatsApp…); that's
a system rule since Android 11.

---

## Data stored on the device

| Where | Contents |
|---|---|
| `gallery_settings` (SharedPreferences) | Theme, grid size, grouping, hidden albums, slideshow speed |
| `files/locked/` | Locked folder photos + `index.json` (never backed up) |
| Memory only | Thumbnail cache (1/8 of the app's memory) |

Uninstalling the app deletes the Locked folder too — move items out first.

---

## Roadmap ideas
- Drag-to-select across many photos, and a date scrubber for fast scrolling
- "Open with" from other apps (view intent)
- Places search (reverse-geocoded locations) and people / things (ML Kit)
- Motion photos, GIF animation, RAW support
- Backup to Google Drive

---

## Author

**Harish** — Mobile App Developer (Android · iOS · Flutter · KMP · .NET MAUI)
GitHub: [@HarishKumarKG](https://github.com/HarishKumarKG)

## License

No license has been chosen yet, so all rights are reserved by default.
Add a `LICENSE` file (for example MIT or Apache 2.0) if you want others to use the code.
