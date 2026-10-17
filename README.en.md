<p align="center">
  <a href="README.md">Türkçe</a> · <b>English</b>
</p>

<p align="center">
  <img src="app/src/main/res/drawable-nodpi/logo_hoset.png" width="140" alt="Hoset logo">
</p>

<h1 align="center">Hoset</h1>

<p align="center">An unofficial Android client for delidehset.com, written with Jetpack Compose.</p>

<p align="center">
  <img src="https://img.shields.io/badge/platform-Android-3DDC84?style=flat-square&logo=android&logoColor=white" alt="platform: Android">
  <img src="https://img.shields.io/badge/license-MIT-3DA639?style=flat-square" alt="license: MIT">
</p>

<p align="center">
  <a href="https://github.com/eyyun/hoset/releases/latest">Download the APK</a> ·
  <a href="LICENSE">MIT license</a>
</p>

Hoset talks to the site's web endpoints and HTML directly. The feed, posts, profiles, communities and the text and voice chat room all run on native Compose screens.

## Features

- **Feed and posts:** popular and top listings, community feeds, effortpost filtering, voting, comments, posting anonymously or with a tripcode, deleting and reporting posts.
- **Chat room (text and voice):** live chat over a small background WebSocket bridge ([ChatBridge](app/src/main/java/com/delidehset/mobile/data/ChatBridge.kt)). The interface, audio and microphone handling are all native.
- **Profile and account settings:** avatar and banner upload with automatic resizing, bio, pronouns, theme, password and e-mail updates, and active session management.
- **Theme store:** eleven looks (Hoset, Mavi Saat, Çamlık, Kızıl Ufuk, Lila, Beyaz Sayfa, Grafit, Derin Sular, Pamuk Şeker, cuteeee, Neon Gece). A kit changes colours, corner radii and title weight together, and the store shows each one as a miniature of the app itself; the choice is kept on the device.
- **Updates itself:** once a day the app checks the GitHub releases; when a newer one is out it downloads the signed APK, verifies its SHA-256 digest and hands it to Android's own confirmation screen. As long as the signing key stays the same, the session and the settings survive the update.

## Build and install

Requirements: **JDK 17** and the **Android SDK** (`compileSdk 35`, `minSdk 26`).

1. Point `local.properties` at your SDK:

   ```properties
   sdk.dir=C:\\Users\\<user>\\android-sdk
   ```

2. Run the tests and build a release APK:

   ```bash
   ./gradlew :app:testDebugUnitTest
   ./gradlew :app:assembleRelease
   adb install -r app/build/outputs/apk/release/app-release.apk
   ```

> If `keystore.properties` exists the release APK is signed with that key, otherwise it falls back to the debug key.

Releases are distributed through GitHub Releases, and the key that signs the APK has to stay the same from one release to the next: otherwise the update cannot be written over an installed app. Once a release is published the app finds it on its own — no store involved.

## Translations

Interface strings are written in Turkish and routed through the components in [AppText.kt](app/src/main/java/com/delidehset/mobile/ui/AppText.kt) into the dictionary in [Translations.kt](app/src/main/java/com/delidehset/mobile/ui/Translations.kt).

- Each entry (`Phrase`) holds the Turkish source string plus its seven translations. A missing translation is a build error.
- Text with dynamic values uses placeholders such as `{0}` and `{1}` (`tf("Seste {0} kişi", voice.count)`). The pattern is translated first, then the values are filled in.
- [AppLangTest](app/src/test/java/com/delidehset/mobile/AppLangTest.kt) scans the source and fails when a Turkish string has no dictionary entry.

## Project layout

```
app/src/main/java/com/delidehset/mobile/
├── data/           HTTP calls, session and cookies, HTML parsing, data layer
├── vm/             ViewModels and UI state
├── ui/             Compose screens, theme and translation components
└── MainActivity.kt Navigation and the main session flow
```

## Known limitations

- Changes to the site's HTML can break the parser.
- Two-factor authentication has to be set up on the website first, since the initial step needs a QR code.

## License

This project is released under the [MIT](LICENSE) license.

## Disclaimer

This is an independent open source project with no official connection to delidehset.com.
