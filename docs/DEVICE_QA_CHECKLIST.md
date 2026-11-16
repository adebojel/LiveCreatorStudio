# Checklist QA perangkat — LiveCreator Studio 0.6.0

Checklist ini membedakan pemeriksaan otomatis CI dari pengujian yang wajib dilakukan pada perangkat Android fisik. Jangan menandai langkah perangkat sebagai lulus hanya berdasarkan hasil kompilasi.

## A. CI otomatis

- [ ] `testDebugUnitTest` lulus.
- [ ] `assembleDebug` lulus.
- [ ] APK artifact berhasil diunggah.
- [ ] GitHub Release dibuat dan asset APK dapat diunduh.
- [ ] Tes klasifikasi URI membedakan audio paket lokal (`file:`) dan audio impor SAF (`content:`).

## B. Kontrol layanan latar belakang

- [ ] Mulai layanan dari Dashboard; notifikasi persisten muncul.
- [ ] Tekan **Matikan layanan** pada notifikasi; notifikasi layanan hilang dan status Dashboard menjadi nonaktif.
- [ ] Mulai lagi dari Dashboard; layanan dan notifikasi muncul kembali.
- [ ] Tekan **Hapus antrean**; pembacaan aktif/antrean berhenti, layanan tetap aktif.
- [ ] Lakukan stop dari notifikasi ketika aplikasi berada di background, lalu buka aplikasi; status tetap sinkron.
- [ ] Tutup activity (bukan force-stop aplikasi); periksa kebijakan foreground service Android dan perilaku OEM.
- [ ] Pastikan notifikasi diizinkan pada Android 13+.

## C. Floating Control

- [ ] Aktifkan bola kontrol dari Settings; izin overlay diminta hanya saat fitur diaktifkan.
- [ ] Berikan izin **Tampil di atas aplikasi lain**; bola muncul setelah kembali ke aplikasi.
- [ ] Geser bola ke beberapa sisi layar; pastikan posisi tersimpan dan bola tetap dapat dijangkau.
- [ ] Ketuk bola untuk membuka/menutup panel; uji seluruh aksi layanan, buka aplikasi, sembunyikan, dan tutup panel.
- [ ] Ubah ukuran 44–84 dp dan transparansi 25–100%; perubahan diterapkan tanpa crash.
- [ ] Uji label TalkBack/aksesibilitas pada bola dan tombol panel; target aksi minimal 48 dp.
- [ ] Cabut izin overlay dari Settings Android; pastikan aplikasi tetap dapat dibuka dan bola tidak memaksa tampil.
- [ ] Matikan fitur, tutup aplikasi, lalu buka ulang; bola tidak muncul kembali.

## D. Soundboard dan lisensi

- [ ] Pada jaringan stabil, tiga paket starter dapat diunduh saat pertama kali aplikasi dibuka.
- [ ] Putar beberapa file OGG/WAV/MP3 dari paket yang telah terpasang.
- [ ] Putar file impor dari SAF dan pastikan URI permission tetap tersedia setelah aplikasi dibuka ulang.
- [ ] Filter **Semua**, **Paket CC0**, dan **Impor** menampilkan sumber yang tepat.
- [ ] Pencarian mengembalikan hasil yang sesuai; filter kosong menampilkan empty state.
- [ ] Hentikan pemutaran; uji audio focus ketika ada panggilan/notifikasi atau aplikasi audio lain.
- [ ] Putar audio melalui speaker, headset kabel, Bluetooth, dan lockscreen/media notification.
- [ ] Jalankan unduh semua paket hanya setelah ruang penyimpanan cukup; periksa kegagalan jaringan dan coba ulang.
- [ ] Pastikan file `LICENSES.txt` tersedia pada folder paket yang diekstrak.

## E. UI, aksesibilitas, dan stabilitas

- [ ] Periksa tampilan pada layar kecil dan besar, portrait dan landscape.
- [ ] Periksa tombol utama tidak terpotong dan target sentuh mudah dijangkau.
- [ ] Uji TalkBack pada navigasi, pencarian, tombol putar/hapus, dan status layanan.
- [ ] Uji rotasi layar dan perpindahan Dashboard/TTS/Soundboard/Settings berulang kali.
- [ ] Periksa tidak ada playback ganda atau controller yang tertinggal setelah berpindah halaman.
- [ ] Periksa penggunaan RAM, baterai, dan suhu pada sesi playback panjang.

## Catatan batasan

Android/OEM dapat membatasi foreground service dan playback background. `START_STICKY` bukan jaminan layanan hidup terus-menerus. Pengujian di emulator/CI tidak menggantikan pemeriksaan pada perangkat fisik.
