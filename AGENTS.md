# Maintainer rules · IDLIX

- **Branch:** use `main` for ordinary changes; no temporary branches, forks or PR-only workflow.
- **Scope:** one native APK for Android TV, phone and tablet. Keep `com.idlix.tv`, min SDK 26, compile/target SDK 35 and JDK 21.
- **UI:** Android Material 3/Compose with Light, Dark and OLED (IDLIX red). Material You is opt-in. Mobile may use bounded optional glass blur; TV keeps high-contrast remote/D-pad focus and performant surfaces. Avoid copying native iOS-only APIs.
- **Navigation:** preserve TV focus/Back, mobile touch/insets and tablet adaptive behavior. Never assume phone-only changes are harmless on TV.
- **Metadata curation:** Cinemeta is discovery metadata only, not a stream source. Never navigate by external IMDb ID. Verify exact title/year/type against IDLIX search results; keep external requests lazy, bounded, cached and cancellable.
- **Collections:** maintain web IDLIX collection slugs separately from Cinemeta editorial collections. Saved collection slugs are local SharedPreferences only. Do not invent community collection APIs, labels or availability.
- **Data:** retain bounded 5-minute first-page cache, 10-minute collection-detail cache and paged Explore. Do not claim complete year/country/network filtering where backend data is sampled. Never cache failed/empty responses as valid catalog.
- **Local state:** SharedPreferences + StateFlow. Do not add Room for nonexistent storage requirements.
- **Release:** normal pushes must not run GitHub Actions; trigger QA/release manually to protect minutes. Keep original signing certificate, semantic versions and monotonically increasing versionCode. Signed artifact != published Release.
- **Signing:** `IDLIX_SIGNING_BUNDLE` only as a GitHub secret; never commit keystores, passwords or `local.properties`. Debug package `com.idlix.tv.debug` is not a production upgrade.
- **Tests:** run JVM tests + assembleDebug, plus release signing verification when publishing. Device D-pad, phone touch, live HLS and install-over-old require hands-on QA.
- **Docs:** README is canonical for user-facing current status; `docs/ARCHITECTURE.md` for implementation and `QA_ANDROID_TV.md` for test gates. Do not add historical audit snapshots as current specifications.
- **Cleanup:** before deleting Android resources, verify references in Kotlin, manifest, XML, tests and CI.
