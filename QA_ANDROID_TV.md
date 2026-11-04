# IDLIX · Android TV, Phone & Tablet QA

## Build baseline — 3 November 2026

| Pemeriksaan | Hasil |
| :-- | :-- |
| Source | `main`, build source commit `2de110f` |
| Automated run | [Android Actions #34](https://github.com/bobbyfch/idlix/actions/runs/41679453395) — **success** |
| JVM unit tests + assembleDebug | **Passed** |
| AssembleRelease + signed certificate verification | **Passed** |
| Signed APK artifact | **v1.0.17 / versionCode 1100017 / com.idlix.tv**, uploaded successfully |
| Public GitHub Release v1.0.17 | **Not published** at this baseline; last public release v1.0.15 |
| Physical TV/phone/tablet validation | **Not verified by this CI run** |

Automated signing/compilation is **not** proof of device UX or successful GitHub Release publication.

## New feature QA (built into v1.0.17; manual device verification pending)

- [ ] Search (query kosong) pada HP/TV menampilkan dua kelompok: koleksi IDLIX dan **19 koleksi Cinemeta**.
- [ ] Buka kurasi populer/genre/rating; hanya judul yang lolos validasi **tipe + nama + tahun** dari API IDLIX yang bisa dibuka.
- [ ] Judul metadata eksternal yang tidak tersedia di IDLIX tidak pernah mengarah ke detail palsu, IMDb ID sebagai slug, atau playback.
- [ ] Sumber Cinemeta timeout/offline atau IDLIX search gagal: tidak crash, dapat retry; permintaan external hanya saat kategori dipilih.
- [ ] Simpan/hapus simpan koleksi IDLIX dan Cinemeta; urutan tersimpan di Search diperbarui dan bertahan setelah restart.
- [ ] Pilih network tvN, TVING, BBC, Crunchyroll dan lainnya; hasil tanpa metadata provider tidak dianggap pasti tersedia.
- [ ] Scroll banyak kartu di HP landscape/TV 720p, D-pad fokus tetap terbaca; tidak ada reload berulang atau halaman macet.
- [ ] Cari 18 judul berikutnya: cache reuse, paging ke 100+ judul melalui `skip=100`, tidak duplikat, pagination berhenti pada 108 kandidat per koleksi.
- [ ] JVM test untuk parser metadata dan exact-match; compile debug+release dan verifikasi signing sebelum distribusi.

## Manual acceptance gate

**Install & update**
- [ ] Install signed v1.0.17 over v1.0.15 atau v1.0.16 without uninstall; history, preferences and signing identity persist.
- [ ] Confirm launcher icon/TV banner are not cropped; app opens cold and after process death.
- [ ] Confirm Check for Update sees new versions **only after** the new public Release exists.

**Android TV / remote**
- [ ] Home, Film, Serial, Jelajah, Search, History, Settings navigable by D-pad; focus visible, Back consistent.
- [ ] Launch at 720p, 1080p and 4K; check focus, text scaling, sidebar and OLED pure black.
- [ ] Open movie and series detail, change season/episode quickly, return without stale focus/state.
- [ ] Media3 playback, remote pause/seek, quality, subtitles, aspect ratio, next episode, resume history.

**Phone / tablet**
- [ ] Test portrait + landscape, 5 mobile tabs, safe areas, keyboard, touch targets, floating navigation, no jank.
- [ ] Search empty state shows collection grid; open collection and return; sort/filter and retry handle network failure.
- [ ] Validate Light, Dark, OLED and Material You with system light/dark (including bottom navigation contrast).
- [ ] Playback orientation, subtitle picker/position, ratio changes, seeking, Back, background/resume.

**Data, resilience & limits**
- [ ] Source unavailable, offline, missing poster/cast/subtitle, invalid responses, limited bandwidth → useful errors, no crash.
- [ ] Collection membership/order matches source; fallback clearly distinguishes missing live data.
- [ ] Page-limited year/country/network filters are **not** described as exhaustive.
- [ ] History clear-all and per-episode resume remain stable across app restart.
- [ ] Test low-memory Android TV; watch for catalog/cache duplication, frame drops and player leaks.

## Local commands

Windows: `scripts\build-local.bat` and `scripts\build-local.bat run`.

Linux/macOS: `./scripts/build-local.sh` and `./scripts/build-local.sh run`.

These build/test the **debug** application, not a production signed update. For distribution, use the manual workflow or `scripts/release-local.ps1` / `scripts/release-local.sh` with the **original** release key and verify the release asset appears publicly.
