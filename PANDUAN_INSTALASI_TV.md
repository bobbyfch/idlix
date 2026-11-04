# Install IDLIX · Android TV & Phone

Gunakan **APK produksi signed** untuk memasang maupun memperbarui `com.idlix.tv`. Debug APK adalah aplikasi lain dan tidak menggantikan instalasi resmi.

## Pilih APK yang benar

- **Public Release:** [GitHub Releases](https://github.com/bobbyfch/idlix/releases/latest). APK di sini dipakai oleh fitur **Check for Update**.
- **Build v1.0.17 (3 Nov 2026):** [Actions #34](https://github.com/bobbyfch/idlix/actions/runs/41679453395) → **Artifacts** → `IDLIX-1.0.17-signed-34` → ekstrak `app-release.apk`. Ini hasil build signed terverifikasi tetapi **belum** diterbitkan sebagai public Release. Artifact GitHub memiliki masa simpan terbatas; login GitHub mungkin diperlukan.
- **Public Release terakhir yang terverifikasi saat panduan diperbarui:** v1.0.15. Selalu periksa halaman release untuk keadaan paling baru.

## Android TV / Google TV

1. Unduh APK signed di HP atau PC. Untuk TV, pindahkan via *Send Files to TV* di Wi-Fi yang sama, flashdisk, atau aplikasi downloader tepercaya.
2. Di TV buka pengelola file, temukan APK, dan pilih **Install**.
3. Bila Android meminta izin, buka **Settings → Apps → Special app access → Install unknown apps** (nama menu bisa berbeda per merek), lalu izinkan **aplikasi pengelola file yang sedang digunakan**. Tidak perlu mengaktifkan Developer Options hanya untuk sideload pada sebagian besar perangkat.
4. Instal lalu buka IDLIX. Saat sudah terpasang versi signed, lakukan update dengan APK **signed oleh sertifikat yang sama**, tanpa uninstall agar riwayat/pengaturan tetap ada.

## Android phone

Unduh APK produksi, buka lewat Files/Downloads, izinkan pemasangan dari aplikasi sumber ketika diminta, lalu instal. Saat memperbarui, pilih *Update* dan jangan menghapus aplikasi lama.

## Troubleshooting

| Gejala | Pemeriksaan |
| :-- | :-- |
| `App not installed` / `Package conflicts` | Pastikan package `com.idlix.tv`, versi baru lebih tinggi, dan sertifikat sama dengan versi terpasang. |
| Debug sudah terpasang | Debug menggunakan `com.idlix.tv.debug`; riwayatnya tidak otomatis pindah ke produksi. |
| Check for Update belum menemukan v1.0.17 | Pemeriksaan mengambil **GitHub Releases**, bukan Actions artifact. |
| File tidak bisa dipasang dari file manager | Berikan izin *Install unknown apps* pada **file manager** yang membuka APK. |
| Peringatan Play Protect | Verifikasi asal APK; jangan abaikan peringatan keamanan serius. |
| Film/subtitle tidak muncul | Periksa koneksi dan status sumber eksternal, coba ulangi; metadata dan streaming tidak dijamin selalu tersedia. |

Jangan memasang APK dari tautan acak, mengubah signing certificate, atau membagikan keystore.
