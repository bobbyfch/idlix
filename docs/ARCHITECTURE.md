# Architecture · IDLIX native Android

One package, `com.idlix.tv`, with Compose UIs tailored to Android TV, phone and tablet. `main` is the canonical source branch.

## Code map

| Location | Responsibility |
| :-- | :-- |
| `data/api` | Retrofit / OkHttp, backend URL and endpoints |
| `data/model` | Serialized catalog, playback and metadata models |
| `data/repository` | Catalog/collection loading, filters, artwork, subtitle and playback sources |
| `data/local` | Display preferences and watch history via SharedPreferences |
| `data/update` | GitHub Release checks, integrity validation and installer flow |
| `ui/theme` | Light, Dark, OLED and opt-in Material You |
| `ui/screens`, `ui/components` | TV screens, cards, D-pad/focus navigation |
| `ui/mobile` | Phone/tablet touch navigation, settings, player |
| `ui/navigation` | Routes, layout selection and navigation state |
| `app/src/test`, `app/src/androidTest` | JVM and device-oriented regression coverage |
| `scripts` | Local debug / signed release helpers |

## External metadata & collection matching

- **Cinemeta** menyediakan 11 daftar metadata publik (top, IMDb rating, genre) via katalog Stremio yang sudah di-whitelist. Tidak ada endpoint stream Stremio yang dipakai.
- Koleksi Cinemeta dimuat **hanya saat dibuka**, maksimum 18 judul, cache positif 30 menit, maksimum tiga pencarian IDLIX paralel per batch. Respons kosong/gagal tidak disimpan sebagai hasil final.
- `CinemetaCollectionSource.matchesIdlixTitle` menolak hasil dengan **judul, tipe film/serial, atau tahun rilis yang tidak sama**. Hanya `SearchResultItem` asli dari API IDLIX yang dikonversi menjadi `MovieItem` dan boleh dinavigasi; ID IMDb bukan slug IDLIX.
- Kategori yang tidak ditemukan di IDLIX tetap kosong. Tidak ada klaim ketersediaan judul, kesetaraan metadata 100%, maupun indeks lengkap.
- Koleksi web IDLIX tetap bersumber dari `/collection` dan detailnya, bukan homepage editorial strip; daftar komunitas di web dapat memerlukan JS/pagination yang belum tersedia sebagai API stabil.
- `SavedCollectionsStore` menyimpan slug pilihan lokal di SharedPreferences (tanpa akun/sinkronisasi). Bookmark diprioritaskan pada grid Search.
- Pilihan Network berbasis 32 label web. Pencocokan tetap menuntut metadata network nyata dalam halaman katalog atau daftar seed yang sudah diberi batasan.

## Runtime invariants

1. `MainActivity` selects TV or touch UI from device/layout preference. Both consume the same typed repository layer.
2. Home/Film/Serial share a **5-minute bounded first-page cache** per source/query with per-key concurrency; failed/empty responses must not masquerade as valid results.
3. Explore starts with **four fetched pages**, loading deeper samples on demand. Country/year/network filtering depends on available metadata, **not** complete server-side indexing.
4. Collection index comes from `/collection` with a known fallback; collection detail responses are cached for **10 minutes**. Never substitute arbitrary franchise-name searches for confirmed collection membership.
5. Playback uses Media3 / ExoPlayer; history/preference storage stays local. Keep cancellation, seeking bounds and per-episode identity safe.
6. TV UI prioritizes focus contrast and D-pad support. Mobile can use optional limited blur; do not assume Android can render Apple's native UI materials.
7. Preserve `com.idlix.tv` and the original production signing certificate across upgrades. `.debug` builds have a separate application ID.

## Build, CI and release

- **Development:** JDK 21, Android SDK 35; `scripts/build-local.bat` or `./scripts/build-local.sh` runs JVM tests + assembleDebug.
- **CI:** [Android QA / Release](../.github/workflows/android-apk.yml) is **manual only**. Ordinary pushes do not spend Actions minutes. Signed builds require `IDLIX_SIGNING_BUNDLE` and are verified for signer, package and version.
- **Publish:** `publish_release` is opt-in and may fail independently due to GitHub permissions. Actions #32 successfully built/uploaded **v1.0.16** but did not publish a public Release. Local scripts can publish when properly authenticated.
- **Acceptance:** [QA_ANDROID_TV.md](../QA_ANDROID_TV.md) tracks what requires physical TV/phone testing; green CI is not a device certification.

Current user-facing release status is maintained in [README](../README.md). Historical audit reports were removed because they referred to old repository names, obsolete versions and superseded UI decisions.
