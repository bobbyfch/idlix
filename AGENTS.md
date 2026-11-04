# Maintainer rules · IDLIX

- **Branch:** use `main` for ordinary changes; no temporary branches, forks, or PR-only workflow.
- **Scope:** one native app for Android TV, phone, and tablet. Keep `com.idlix.tv`, min SDK 26, compile/target SDK 35, and JDK 21.
- **UI:** Android Material 3/Compose with Light, Dark, and OLED (IDLIX red). Material You is opt-in. Mobile may use bounded optional glass blur; TV keeps high-contrast remote/D-pad focus and performant surfaces. Avoid copying native iOS-only APIs.
- **Navigation:** preserve TV focus/Back, mobile touch/insets, and tablet adaptive behavior. Never assume phone-only changes are harmless on TV.
- **Metadata curation:** Cinemeta is discovery metadata only, not a stream source. Never navigate by external IMDb ID. Verify exact title/year/type against IDLIX search results; keep external requests lazy, bounded, cached, and cancellable.
- **Collections:** maintain web IDLIX collection slugs separately from Cinemeta editorial collections. Saved collection slugs are local SharedPreferences only. Do not invent community collection APIs, labels, or availability.
- **Data:** retain bounded 5-minute first-page cache, 10-minute collection-detail cache, and paged Explore. Do not claim complete year/country/network filtering where backend data is sampled. Never cache failed/empty responses as valid catalog.
- **Local state:** SharedPreferences + StateFlow. Do not add Room for nonexistent storage requirements.
- **Build/release:** every push to `main` and manual run from `main` builds only the signed release APK and publishes a new GitHub Release. No debug APK, separate publisher workflow, or non-main release. `workflow_dispatch` intentionally creates the next patch release.
- **Signing:** keep the fresh `IDLIX_SIGNING_BUNDLE` only in GitHub Actions secrets and the matching local key outside the repository. Do not commit keystores, passwords, local.properties, or secret bundles. Keep the same signing identity for every later release.
- **Versions:** use semantic release versions and monotonically increasing version codes. Signed artifact != published Release.
- **Tests:** the Android build workflow runs JVM tests and assembles debug APK; publishing also requires release signature and package/version verification. Device D-pad, phone touch, live playback, and install-over-old need hands-on QA.
- **Docs:** README is canonical for user-facing current status; `docs/ARCHITECTURE.md` for implementation and `QA_ANDROID_TV.md` for test gates. Do not add historical audit snapshots as current specifications.
- **Cleanup:** before deleting Android resources, verify references in Kotlin, manifest, XML, tests, and CI.
