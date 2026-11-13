# Changelog

All notable changes to Kiro are listed here. Each GitHub Release shows the section for its version.
Format: `## [version] - date`, newest first.

## [1.0.0] - 2026-11-13

First release of Kiro.

### Added
- Home screen: auto-advancing hero banner with Play button, genre shelves, pull-to-refresh, loading skeletons and retry on error.
- Continue Watching row, filled when you press Play on a title.
- Search screen: live search with debounce, typo-tolerant ranking, recent searches, browse by genre and trending titles.
- Title sheet with poster, score, genres and synopsis.
- Anime data from the AniList GraphQL API (no API key needed): titles, posters, scores, genres, synopsis and trailers.
- Roboto (Google Font) across the whole app and a dark purple theme.
- Kotlin UI, Java data layer (AniList GraphQL client, cache, local history) and a Rust search ranker with a Kotlin fallback.

### Known limitations
- Play opens the trailer (or the AniList page); episode streaming is not included yet.
- Peek, Library and Me tabs are not built yet.
