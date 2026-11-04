# Signing dan rilis IDLIX

## Siapkan satu kali

1. Di laptop Windows, pasang JDK 21 dan unduh ZIP dari [repo IDLIX](https://github.com/bobbyfch/idlix).
2. Ekstrak ZIP, lalu jalankan `RUN-IDLIX-SIGNING.bat`.
3. Ketik `CREATE-FRESH` saat diminta. Skrip membuat kunci rilis di `%USERPROFILE%\.idlix` dan menyiapkan konfigurasi lokal dalam `local.properties`.
4. Saat halaman GitHub terbuka, tambahkan secret bernama `IDLIX_SIGNING_BUNDLE`, tempel nilai yang sudah disalin skrip, lalu simpan. Kembali ke jendela skrip dan ketik `SECRET-SAVED`.

GitHub CLI (`gh`) bersifat opsional. Bila tersedia dan sudah login, skrip dapat menyimpan secret langsung; bila tidak, skrip membuka halaman Secrets dan menyalin nilainya agar ditempel sendiri. Nilai secret dan password tidak dimasukkan ke repository.

Simpan cadangan file `release-signing.jks` dan password dengan aman di luar repository. Kunci ini harus tetap sama untuk setiap rilis selanjutnya. Jangan mengunggahnya ke repo atau membagikannya di chat.

## Build dan terbitkan

- Perubahan kode Android atau konfigurasi build yang dikirim ke `main` memulai build Actions otomatis.
- Untuk membangun ulang setelah secret baru ditambahkan tanpa perubahan kode, buka **Actions → IDLIX · Android QA / Release → Run workflow**.
- Setelah build selesai dengan status berhasil dan menghasilkan APK signed, jalankan **Actions → IDLIX · Publish verified APK → Run workflow** untuk menerbitkannya di GitHub Releases.
- Workflow rilis memakai izin bawaan GitHub Actions. Secret `IDLIX_RELEASE_TOKEN` tidak diperlukan.

Laptop Windows dapat membangun dan menerbitkan rilis dengan `scripts/release-local.ps1` setelah GitHub CLI login. Kunci lokal yang sama dan konfigurasi Android SDK harus tersedia. Jangan menerbitkan build debug sebagai APK rilis.

## Pemasangan pertama versi 1.0.0

Versi 1.0.0 di repository baru memakai kunci rilis baru. Android tidak dapat memasangnya di atas aplikasi lama: hapus IDLIX versi lama lebih dulu. Riwayat dan pengaturan yang tersimpan di instalasi lama tidak ikut terbawa.
