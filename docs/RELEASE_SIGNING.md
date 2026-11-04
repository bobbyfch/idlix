# Menyiapkan signing key IDLIX

Rilis pertama di repo baru memakai signing key baru. Siapkan secret ini sekali dari laptop Windows sebelum meminta APK release:

1. Pasang JDK 21 dan GitHub CLI (gh), lalu login ke akun yang punya akses admin ke bobbyfch/idlix dengan perintah gh auth login.
2. Di folder hasil clone repo, buka PowerShell dan jalankan:

   ~~~powershell
   .\scripts\create-signing-secret.ps1
   ~~~

3. Skrip membuat key di folder %USERPROFILE%\.idlix, menulis konfigurasi build lokal ke local.properties yang diabaikan Git, lalu mengirim nilai IDLIX_SIGNING_BUNDLE langsung ke Actions secrets repo. Nilai secret dan password tidak ditampilkan atau disimpan sebagai file bundle. Jika pengiriman secret gagal, coba lagi dengan opsi -ReuseExisting agar key yang sama dipakai kembali.
4. Simpan salinan aman release-signing.jks. Key ini diperlukan untuk menandatangani semua rilis berikutnya. Jangan ganti key setelah rilis pertama dipublikasikan.

Skrip meminta konfirmasi CREATE-FRESH karena key baru membuat Android memperlakukan rilis 1.0.0 sebagai instalasi baru. Hapus versi IDLIX lama dari perangkat sebelum memasang rilis baru; data lokal dari aplikasi lama akan terhapus.

## Membuat APK

Setelah secret tersedia, penambahan secret tidak otomatis memulai workflow. Jalankan **IDLIX · Android QA / Release** secara manual dari tab Actions pada branch main. Jika berhasil, jalankan **IDLIX · Publish verified APK** untuk membuat rilis v1.0.0.

Di laptop Windows, konfigurasi lokal yang sama dapat dipakai untuk membuat APK signed dengan:

~~~powershell
.\scripts\build-local.bat release
~~~

Workflow publikasi memakai token bawaan GitHub Actions dengan izin repo yang sudah didefinisikan di workflow; IDLIX_RELEASE_TOKEN tidak diperlukan.
