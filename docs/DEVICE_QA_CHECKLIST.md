# Checklist QA perangkat — LiveCreator Studio 0.6.4

Dokumen ini adalah catatan uji manual untuk APK 0.6.4. CI hijau hanya membuktikan unit test dan proses build berhasil; **jangan tandai pengujian perangkat sebagai lulus sebelum benar-benar dijalankan pada ponsel Android**.

## 1. Identitas pengujian

- Versi aplikasi: 0.6.4 (versionCode 28)
- Perangkat / model: ____________________
- Versi Android: ____________________
- Patch keamanan / ROM: ____________________
- Tanggal dan waktu pengujian: ____________________
- Build / tag GitHub Release: ____________________
- Hasil umum: ☐ Lulus  ☐ Lulus dengan catatan  ☐ Gagal

## 2. Verifikasi CI dan instalasi

- [ ] GitHub Actions untuk commit rilis menunjukkan `testDebugUnitTest` sukses.
- [ ] GitHub Actions menunjukkan `assembleDebug` sukses.
- [ ] Artifact `LiveCreatorStudio-debug-apk` tersedia.
- [ ] Release terbaru menampilkan asset `LiveCreatorStudio-debug.apk`.
- [ ] APK diunduh dari halaman Release dan ukuran file tidak nol.
- [ ] Instalasi bersih berhasil pada perangkat Android API 26+.
- [ ] Aplikasi dapat dibuka setelah instalasi dan setelah perangkat direstart.
- [ ] Jika meng-upgrade dari versi lama, data pustaka dan preferensi yang memang dipertahankan tetap tersedia.
- [ ] Catat peringatan Android/Play Protect; jangan menonaktifkan proteksi perangkat untuk melewatinya.

## 3. Dashboard, TTS, dan layanan latar belakang

- [ ] Dashboard terbuka tanpa crash; navigasi ke TTS, Soundboard, dan Settings berfungsi.
- [ ] TTS lokal mengucapkan teks uji dengan suara, bahasa, pitch, dan kecepatan yang dipilih.
- [ ] Tombol stop menghentikan ucapan yang sedang berjalan.
- [ ] Antrean TTS menerima beberapa item; batas antrean dan penghapusan antrean bekerja.
- [ ] Mulai layanan dari Dashboard; notifikasi persisten muncul.
- [ ] Kontrol stop pada notifikasi menghentikan layanan dan status Dashboard ikut berubah.
- [ ] Mulai ulang layanan dari Dashboard; notifikasi kembali muncul.
- [ ] Hapus antrean dari notifikasi; playback/antrean berhenti tanpa membuat status layanan salah.
- [ ] Uji saat layar terkunci, aplikasi di background, dan setelah berpindah aplikasi.
- [ ] Pada Android 13+, pastikan izin notifikasi diberikan dan penolakan izin ditangani dengan jelas.

## 4. Floating Control / overlay

- [ ] Fitur nonaktif secara default dan tidak meminta izin overlay sebelum pengguna mengaktifkannya.
- [ ] Aktifkan fitur; alur penjelasan dan halaman izin **Tampil di atas aplikasi lain** terbuka benar.
- [ ] Setelah izin diberikan dan kembali ke aplikasi, bola kontrol tampil.
- [ ] Geser bola ke berbagai posisi; dock kiri/kanan dan grip tetap dapat dijangkau.
- [ ] Ketuk bola untuk membuka dan menutup panel; ketuk area luar untuk menutup panel.
- [ ] Uji semua shortcut: Audio, Stop, Ketawa, Kiss, Petir, Tepuk, Party, Magic, Alert, Game, App, Tutup, Hide, Off.
- [ ] Uji efek setelah paket terkait diunduh; jika paket belum ada, pesan instruksi tampil tanpa crash.
- [ ] Tekan beberapa efek berurutan; tidak ada playback ganda yang tidak disengaja atau suara menggantung.
- [ ] Tutup layanan overlay ketika efek sedang diputar; tidak ada crash dan controller dibersihkan.
- [ ] Cabut izin overlay dari Settings Android; aplikasi tetap dapat dibuka dan menampilkan instruksi yang benar.
- [ ] Matikan overlay, tutup aplikasi, lalu buka ulang; overlay tidak muncul tanpa tindakan pengguna.

## 5. Soundboard dan pustaka CC0

- [ ] Dengan jaringan stabil, tiga starter packs dapat diunduh pada peluncuran pertama.
- [ ] Jika offline / unduhan gagal, aplikasi tetap dapat digunakan dan menampilkan status kegagalan yang jelas.
- [ ] Unduh satu ZIP pack opsional; jumlah file aktual sesuai hasil ekstraksi, bukan hanya hitungan katalog.
- [ ] Unduh satu file MP3 langsung dari SFXMint; file dapat diputar dan disimpan di app-private storage.
- [ ] Uji format MP3, OGG, WAV, dan format lain yang memang tersedia pada paket.
- [ ] Pastikan setiap folder paket berisi `LICENSES.txt` dengan creator, source URL, dan lisensi.
- [ ] Uji pencarian, kategori, filter Semua / Paket CC0 / Impor, dan empty state.
- [ ] Impor audio melalui Android document picker; putar setelah aplikasi ditutup dan dibuka kembali.
- [ ] Hapus file impor; pastikan file paket CC0 tidak ikut terhapus.
- [ ] Uji paket yang gagal diunduh, timeout jaringan, ruang penyimpanan rendah, dan retry.
- [ ] Uji unduh semua hanya dengan ruang penyimpanan memadai; catat jumlah paket/file yang berhasil dan yang gagal.
- [ ] Setelah paket terpasang, putar suara saat mode pesawat untuk memastikan playback lokal bekerja offline.

## 6. Media, audio focus, dan koneksi

- [ ] Playback melalui speaker internal.
- [ ] Playback melalui headset kabel / USB-C bila tersedia.
- [ ] Playback melalui Bluetooth; uji tombol headset jika didukung.
- [ ] Kontrol media muncul pada notification shade / lockscreen sesuai versi Android.
- [ ] Uji audio focus saat notifikasi, panggilan, dan aplikasi musik lain aktif.
- [ ] Cabut headset saat audio berjalan; perilaku pause / perpindahan output sesuai ekspektasi.
- [ ] Putar efek pendek berulang kali; tidak ada audio yang terus menyala setelah selesai.

## 7. UI, aksesibilitas, dan stabilitas

- [ ] Periksa layar kecil, layar besar, portrait, dan landscape.
- [ ] Tidak ada tombol atau teks penting yang terpotong / tertutup system bars.
- [ ] TalkBack dapat membaca nama tombol, status, dan kontrol Soundboard.
- [ ] Uji ukuran font / display scaling Android.
- [ ] Pindah antar layar berulang kali; tidak ada crash, UI beku, atau playback ganda.
- [ ] Jalankan sesi audio minimal 15 menit; pantau RAM, baterai, suhu, dan stabilitas.
- [ ] Tutup paksa lalu buka ulang hanya sebagai uji pemulihan; catat apakah status layanan sesuai keadaan sebenarnya.
- [ ] Uji reboot perangkat dan perilaku layanan sesuai kebijakan Android/OEM.

## 8. Format laporan hasil

Salin dan isi bagian ini setelah pengujian:

```text
Versi APK:
Perangkat:
Android:
Tanggal:
Hasil: LULUS / GAGAL / LULUS DENGAN CATATAN

Fitur yang lulus:
Fitur yang gagal:
Langkah untuk mengulang bug:
Pesan error / screenshot:
Apakah suara terdengar:
Apakah overlay/notifikasi berfungsi:
```

Lampirkan screenshot atau rekaman layar jika ada. Jangan sertakan password, cookie, access token, atau data akun.

## Batasan pengujian

- CI dan unit test tidak dapat membuktikan keluaran speaker, izin overlay, perilaku lockscreen, Bluetooth, TalkBack, atau kebijakan background service OEM.
- Android dapat membatasi foreground service karena versi OS, pengaturan baterai, force-stop, atau kebijakan produsen.
- Floating Control adalah overlay opsional; bukan Quick Settings, bukan notification shade, dan tidak mengendalikan aplikasi lain.
- APK pada workflow ini adalah debug build, bukan paket production-signed.
