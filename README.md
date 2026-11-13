# Kiro

Anime streaming app (Home + Search) in the style of your screenshots.

| Layer | Language | Where |
|---|---|---|
| UI (Jetpack Compose, Roboto) | Kotlin | `app/src/main/java/com/kiro/app/ui` |
| Data: AniList GraphQL client, cache, local history | Java | `app/src/main/java/com/kiro/app/data` |
| Search ranking engine (JNI) | Rust | `native/` |

## Run it
1. Open the `Kiro` folder in Android Studio (Ladybug or newer) and let Gradle sync.
   Roboto is bundled in `app/src/main/res/font`, so nothing extra is downloaded.
2. Press Run on a device or emulator (Android 8.0+).

## Rust search engine (optional)
Without it the app uses an identical Kotlin ranker, so everything still works.
To use Rust: install rustup + the Android NDK (`ANDROID_NDK_HOME`), then `./build-rust.sh`
(or `./gradlew assembleDebug -PbuildRust`). Logcat tag `Kiro` tells you which ranker is active.
Rust tests: `cd native && cargo test`.

## What works
- Home: auto-advancing hero, Continue Watching, genre shelves, pull-to-refresh, retry on error, loading skeletons
- Search: debounced live search, results re-ranked by Rust, typo tolerance, recent searches, browse by genre, trending
- Title sheet: poster, score, genres, synopsis, Play

## Streaming source
Titles, posters and trailers come from the free AniList GraphQL API (https://graphql.anilist.co, no API key). It has no episode video,
so Play opens the trailer (or the AniList page). To stream real episodes you need a licensed video source.

## Releases and changelog
Download the APK from the Releases page. To ship a new version:
1. Bump `versionName` (and `versionCode`) in `app/build.gradle.kts`.
2. Add a `## [x.y.z] - date` section to `CHANGELOG.md` describing what's new.
3. Commit and push, then open the Actions tab, choose "Release" and press Run workflow
   (or push a tag: `git tag vx.y.z && git push origin vx.y.z`).
GitHub Actions builds the APK and publishes the release with that changelog section as the notes.

## Credits
- Roboto: Google Fonts (Apache License 2.0 / SIL OFL)
- Feather icons: https://feathericons.com (MIT)
- Anime data: AniList (https://anilist.co)
