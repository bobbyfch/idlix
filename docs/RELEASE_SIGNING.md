# Build dan rilis IDLIX otomatis

## Kunci rilis

Repository menggunakan GitHub Actions secret `IDLIX_SIGNING_BUNDLE` untuk menandatangani semua APK rilis. Kunci lokal yang cocok berada di luar repository. Jangan mengganti atau mengunggah kunci itu; tanpa kunci yang sama, Android tidak dapat memasang pembaruan di atas aplikasi yang sudah ada.

## Dari HP tanpa komputer

Setelah kunci rilis disiapkan satu kali, minta ChatGPT mengubah repository `bobbyfch/idlix` langsung pada branch `main`. Setiap commit ke `main` otomatis memulai build APK release signed dan menerbitkannya ke GitHub Releases setelah verifikasi berhasil.

Tidak ada build APK debug dan tidak ada workflow publikasi kedua. Untuk memulai rilis secara manual, gunakan **Actions → IDLIX · Release APK → Run workflow** dengan branch `main`. Menjalankan secara manual memang membuat rilis patch berikutnya.

## Mengunduh dan memperbarui

Ambil APK terbaru dari [GitHub Releases](https://github.com/bobbyfch/idlix/releases/latest). Rilis setelah 1.0.0 memakai kunci yang sama, sehingga dapat dipasang sebagai pembaruan biasa. Android tetap meminta persetujuan sebelum pemasangan.

Rilis 1.0.0 di repository baru memakai kunci yang berbeda dari aplikasi lama. Untuk pemasangan pertama, hapus IDLIX versi lama terlebih dahulu; data lokal dari instalasi lama tidak ikut terbawa.

## Build lokal

Di laptop Windows, konfigurasi JDK 21, Android SDK, GitHub CLI, dan kunci lokal yang sama diperlukan untuk memakai `scripts/release-local.ps1`. Jangan mengunggah APK debug sebagai rilis.
