# Audit Repository Eksternal — LiveCreator Studio

Tanggal pemeriksaan: 16 November 2026 (pembaruan Media3 1.11.1)  
Repository target: https://github.com/adebojel/LiveCreatorStudio  
Branch pengembangan: `main`

## Prinsip seleksi

1. Kecocokan Android/Kotlin dan arsitektur saat ini.
2. Lisensi eksplisit dan kewajiban atribusi.
3. Status pemeliharaan dan kualitas dokumentasi.
4. Ukuran native binary, kebutuhan RAM/storage, dan kompatibilitas Android.
5. Risiko keamanan, privasi, dan ketentuan platform.
6. Tidak memasukkan kode atau aset sebelum lisensi dan asal-usulnya jelas.

## Kandidat dan hasil penilaian

### 1. Llamatik — kandidat AI lokal jangka menengah

Repository: https://github.com/llamatik/Llamatik  
Lisensi: MIT untuk kode inti; repository menyatakan aset merek tertentu memiliki pengecualian lisensi terpisah.  
Teknologi: Kotlin Multiplatform, llama.cpp, whisper.cpp, Stable Diffusion native bindings, SDK/agent, memory, tools, workflow.

**Nilai guna:** kandidat kuat untuk modul AI lokal opsional dan arsitektur assistant/agent.  
**Risiko:** native CMake build, ukuran binary dan model, variasi ABI/perangkat, konsumsi RAM/thermal, dan kompleksitas dependency graph. README menyatakan modul inti multiplatform; itu tidak berarti semua modul siap ditambahkan langsung ke aplikasi Android ini.  
**Keputusan:** belum ditambahkan sebagai dependency. Lakukan proof-of-concept terpisah, ukur APK size, ABI, cold start, RAM, inference speed, dan izin model sebelum integrasi.

### 2. Offline AI Assistant — referensi AI Android offline

Repository: https://github.com/its-hazratbilal/offline-ai-assistant  
Lisensi: MIT.  
Teknologi yang didokumentasikan: Kotlin, Compose, MVVM/Clean Architecture, Room, DataStore, model GGUF, llama.cpp/JNI, SpeechRecognizer, TextToSpeech.

**Nilai guna:** referensi struktur modul AI, pengelolaan model, percakapan, dan pemisahan data/domain/UI.  
**Risiko:** klaim performa dan dukungan model perlu diverifikasi pada perangkat target; jangan menyalin keseluruhan proyek atau mengasumsikan semua fitur siap dipakai.  
**Keputusan:** gunakan sebagai referensi desain dan daftar evaluasi; bukan dependency langsung.

### 3. TikTokLiveJava — referensi event LIVE tidak resmi

Repository: https://github.com/jwdeveloper/TikTokLiveJava  
Teknologi: Java client yang mengakses internal WebCast push service dan mendeskripsikan event comment, gift, follow, share, dan lainnya.  
Lisensi: file LICENSE tidak ditemukan pada path yang diperiksa. README menyatakan proyek ini bukan API resmi dan merupakan reverse-engineering.

**Nilai guna:** memahami bentuk event dan model event-driven untuk adapter eksperimental.  
**Risiko:** endpoint internal dapat berubah, tidak ada jaminan dukungan platform, dan lisensi kode belum terverifikasi.  
**Keputusan:** tidak menyalin kode atau menambahkan dependency. Tidak boleh menjadi jalur produksi tanpa tinjauan legal/ketentuan platform dan validasi teknis.

### 4. TikTok-Live-Connector — referensi event Node.js

Repository: https://github.com/zerodytrash/TikTok-Live-Connector  
Teknologi: Node.js client untuk event chat, gift, like, follow, share, viewer, dan battle melalui internal Webcast push service.  
README menyebut proyek tidak resmi, berbasis reverse engineering, dan bukan API production-ready; merekomendasikan WebSocket API provider untuk penggunaan produksi.  
Lisensi: belum diverifikasi dalam pemeriksaan ini.

**Nilai guna:** referensi skema event dan ide adapter server-side.  
**Risiko:** bukan library Android/Kotlin, bergantung endpoint internal/layanan signing pihak ketiga, dan membawa risiko privasi bila sesi autentikasi dikirim ke layanan eksternal.  
**Keputusan:** tidak dimasukkan ke aplikasi Android dan tidak menerima cookie/password pengguna.

### 5. MusicPlayer-JetpackCompose — referensi UI/audio

Repository: https://github.com/UmairOye/MusicPlayer-JetpackCompose  
Teknologi yang didokumentasikan: Kotlin, Jetpack Compose, playlists, favorites, Room, navigasi dan animasi.  
Lisensi: belum diverifikasi dalam pemeriksaan ini.

**Nilai guna:** inspirasi pola navigasi dan pengelolaan pustaka audio.  
**Keputusan:** referensi saja. Tidak menyalin source, screenshot, atau aset tanpa lisensi yang sesuai.

### 6. Referensi tambahan yang ditemukan

- https://github.com/dhruva-b-dev/TxtVoice — hasil pencarian terkait text-to-voice; README tidak ditemukan pada path default yang diperiksa, sehingga belum lolos evaluasi.
- https://github.com/UmairOye/MusicPlayer-JetpackCompose — referensi Compose audio library.
- https://github.com/llamatik/Llamatik — SDK lokal Kotlin.
- https://github.com/its-hazratbilal/offline-ai-assistant — referensi offline AI.

Hasil pencarian repository bukan bukti kualitas, keamanan, kompatibilitas, atau hak penggunaan. Setiap kandidat harus ditinjau pada level file, dependency, lisensi, dan build sebelum adopsi.

## Peta integrasi yang direkomendasikan

| Prioritas | Komponen | Rencana |
|---|---|---|
| P0 | Build reliability | CI compile + unit test + APK publication |
| P1 | DataStore | Preferensi terstruktur, migrasi aman |
| P1 | Room | Pustaka soundboard, kategori, metadata, riwayat event |
| P1 | Audio engine | Evaluasi AndroidX Media3 untuk kontrol playback dan audio focus |
| P2 | Event domain | Event envelope, timestamp, source ID, deduplication, bounded processing |
| P2 | Provider adapter | Interface provider terpisah; aktif hanya dengan sumber data sah |
| P3 | Local AI | Proof-of-concept Llamatik/llama.cpp sebagai modul opsional |
| P3 | Voice input | SpeechRecognizer dengan status permission/network/offline yang eksplisit |

## Rancangan event domain (belum diimplementasikan)

Event LIVE masa depan sebaiknya masuk melalui kontrak netral provider, bukan langsung dari UI:

```kotlin
data class CreatorEvent(
    val id: String,
    val source: String,
    val type: EventType,
    val occurredAtEpochMillis: Long,
    val userId: String?,
    val displayName: String?,
    val message: String?,
    val quantity: Long?,
    val rawPayload: String?
)

enum class EventType {
    COMMENT, GIFT, FOLLOW, SHARE, LIKE, JOIN, SUBSCRIPTION, UNKNOWN
}

interface LiveEventProvider {
    val events: kotlinx.coroutines.flow.Flow<CreatorEvent>
    suspend fun connect()
    suspend fun disconnect()
}
```

Kontrak ini hanya usulan desain; belum menjadi implementasi dan tidak menghasilkan event sendiri. Setiap provider wajib mengurus otorisasi, koneksi, reconnect, rate limit, deduplication, dan kebijakan penyimpanan.

## Kebijakan lisensi dan atribusi

- Llamatik: MIT untuk kode inti, tetapi periksa `LICENSE-assets` dan `NOTICE` sebelum memakai aset atau logo.
- Offline AI Assistant: MIT; sertakan copyright/license jika source yang dilisensikan diadopsi.
- TikTokLiveJava: jangan salin sampai lisensi eksplisit dan izin penggunaan dapat diverifikasi.
- TikTok-Live-Connector: jangan salin sampai lisensi dan risiko reverse-engineering dapat ditinjau.
- MusicPlayer-JetpackCompose: jangan salin sampai lisensi diverifikasi.
- Proyek target sendiri belum memiliki lisensi utama yang ditetapkan.

## Kesimpulan

Tidak ada repository yang aman untuk diimpor secara utuh ke aplikasi ini. Pilihan terbaik adalah mengambil pola arsitektur yang relevan, mengisolasi modul, meninjau lisensi, mengukur biaya runtime, dan menguji setiap integrasi. Prioritas saat ini adalah stabilitas aplikasi Android yang sudah ada; AI lokal dan LIVE event ingestion tetap fase terpisah sampai jalur teknis dan legalnya jelas.


## 7. SwaraPlayer — referensi AndroidX Media3

Repository: https://github.com/kannan-ai/SwaraPlayer  
Teknologi README: Jetpack Compose, AndroidX Media3/ExoPlayer, MediaSessionService, kontrol media latar belakang, audio effects, dan DataStore.  
Status metadata lisensi GitHub: tidak terdeteksi pada pemeriksaan API (README menyebut MIT, tetapi tidak ada metadata lisensi terverifikasi).

**Nilai guna:** referensi teknis untuk migrasi soundboard ke Media3 dan pengelolaan playback melalui MediaSessionService.  
**Risiko:** proyek ini adalah pemutar audio/video lengkap; fitur equalizer/boost/gesture tidak otomatis cocok dengan kebutuhan soundboard dan perlu diuji secara terpisah.  
**Keputusan:** referensi arsitektur saja. Tidak menyalin kode atau mengimpor dependency sampai lisensi diverifikasi dan kebutuhan soundboard dirumuskan.

## 8. Parakeet Android — referensi speech-to-text offline

Repository: https://github.com/meet30997/parakeet-android-on-device-speech-recognition  
Lisensi metadata GitHub: MIT.  
Teknologi: Kotlin, Compose, JNI, NeMo-Speech.cpp/ggml, model Parakeet TDT 0.6B, pemrosesan lokal ARM64.

**Nilai guna:** kandidat riset untuk transkripsi offline jika kebutuhan voice input berkembang dari Android SpeechRecognizer ke model lokal.  
**Risiko:** model yang didokumentasikan sekitar 714 MB, membutuhkan NDK/CMake/JNI, ARM64, memori dan ruang penyimpanan besar. README menyebut inferensi chunked dan bukan streaming native sejati.  
**Keputusan:** jangan tambahkan ke APK utama saat ini. Lakukan benchmark terpisah pada perangkat sasaran dan pastikan pengalaman izin mikrofon, suhu, baterai, serta pengelolaan model layak.

## 9. Sample Compose + Hilt + Room — referensi database

Repository: https://github.com/NicosNicolaou16/SampleComposeWithHiltAndRoom  
Lisensi metadata GitHub: Apache-2.0.  
Teknologi: Compose, Room, MVVM, Repository Pattern, Coroutines, Hilt.

**Nilai guna:** referensi pola pemisahan repository dan database untuk migrasi pustaka soundboard dari SharedPreferences ke Room.  
**Risiko:** sample ini memakai konfigurasi SDK/dependency yang lebih baru; tidak dapat disalin langsung ke proyek yang saat ini compileSdk 35 dan Kotlin/Gradle 8.9.  
**Keputusan:** referensi arsitektur saja; integrasi Room dilakukan bertahap dan diuji dengan migrasi data.

## 10. DataStoreSample — referensi Preferences DataStore

Repository: https://github.com/emineinan/DataStoreSample  
Status metadata lisensi GitHub: tidak terdeteksi; repository terakhir diperbarui pada 2023.  
**Keputusan:** referensi konsep saja. Implementasi aplikasi menggunakan AndroidX Preferences DataStore resmi dan migrasi SharedPreferences bawaan, bukan menyalin source repository tersebut.

## Implementasi dari audit pada versi 0.5.0

- Preferences DataStore menjadi sumber preferensi terpusat untuk auto-start, pengingat reboot, speech rate, dan pitch.
- SharedPreferences lama dimigrasikan sekali ke DataStore agar pengaturan pengguna tidak hilang saat upgrade.
- Sanitasi speech rate/pitch membatasi nilai ke rentang 0.5×–2.0× dan mengembalikan nilai non-finite ke 1.0×.
- Empat unit test baru menguji rentang dan nilai invalid.
- Tidak ada source code pihak ketiga yang disalin ke aplikasi; integrasi ini memakai AndroidX DataStore.


## 11. LocalMind — aplikasi Android AI lokal

Repository: https://github.com/tk85457/LocalMind  
Lisensi metadata GitHub: MIT.  
README mendokumentasikan Kotlin, Compose, llama.cpp/JNI, GGUF, pengunduhan model, streaming token, riwayat percakapan lokal, pencarian, dan database Room.

**Nilai guna:** kandidat kuat untuk mempelajari model manager, state generasi, penyimpanan percakapan, dan pemisahan UI/AI runtime.  
**Risiko:** aplikasi penuh memiliki arsitektur, dependency, dan kebutuhan memori yang berbeda; model GGUF dapat berukuran ratusan MB hingga beberapa GB.  
**Keputusan:** referensi arsitektur. Tidak mengimpor keseluruhan aplikasi atau runtime ke APK utama sebelum proof-of-concept terukur.

## 12. Hush AI — Android offline LLM

Repository: https://github.com/hushai-app/hushai-android  
Lisensi metadata GitHub: NOASSERTION; README menyebut Apache 2.0.  
README mendokumentasikan Qwen3 GGUF, JNI bridge C++ langsung ke llama.cpp, model tier 0.6B–4B, dan kebutuhan model 484 MB–2.5 GB.

**Nilai guna:** contoh integrasi llama.cpp native yang lebih baru dan pembacaan kemampuan RAM perangkat.  
**Risiko:** model besar, cold start, CPU-only, native build, dan klaim kompatibilitas yang perlu diuji. Lisensi metadata perlu diperiksa dari file LICENSE sebelum source diadopsi.  
**Keputusan:** referensi riset; tidak ada kode yang disalin.

## 13. ThalzeronLabs — Android on-device LLM

Repository: https://github.com/ThalzeronLabs/android-on-device-llm  
Lisensi metadata GitHub: Apache-2.0.  
README mendokumentasikan Compose, llama.cpp, llama-kotlin, model catalog, Room, mode offline, dan screenshot test.

**Nilai guna:** referensi bagaimana menampilkan model catalog, pengelolaan model, dan screenshot tests.  
**Risiko:** fokus pada ARM64 dan model download; APK demo bukan Play Store release. Demo GIF README menyatakan placeholder pada saat audit, sehingga tidak dianggap bukti runtime.  
**Keputusan:** referensi arsitektur dan pengujian UI, bukan dependency langsung.

## 14. HexaPlayer — referensi pemutar musik

Repository: https://github.com/OxoGhost01/HexaPlayer  
Lisensi metadata GitHub: NOASSERTION.  
README mendokumentasikan playlist, antrean persisten, folder, pencarian, kontrol media, equalizer, dan UI Material/terminal.

**Nilai guna:** inspirasi fitur pustaka dan pengelolaan antrean audio.  
**Risiko:** metadata lisensi belum jelas; beberapa fitur integrasi pihak ketiga bukan kebutuhan soundboard inti.  
**Keputusan:** tidak menyalin kode atau aset.

## 15. PulsePlayer-Android — referensi audio/UI

Repository: https://github.com/Shivaanshe/PulsePlayer-Android  
Lisensi metadata GitHub: GPL-3.0.

**Nilai guna:** inspirasi tampilan glassmorphism, playlist, audio status, dan pola dashboard.  
**Risiko:** lisensi GPL-3.0 membawa kewajiban distribusi yang tidak boleh diabaikan; README juga menguraikan resolver sumber eksternal yang tidak akan diadopsi.  
**Keputusan:** pelajari konsep UI secara umum saja; tidak menyalin source, aset, atau dependency.

## 16. Rekomendasi audio engine resmi

Dokumentasi AndroidX Media3: https://developer.android.com/jetpack/androidx/releases/media3  
Versi stabil yang ditemukan saat audit: **1.11.1** (September 2026); 1.12.0-beta01 tersedia pada November 2026.

**Rencana:** gunakan hanya modul Media3 yang dibutuhkan (ExoPlayer, Session, UI jika perlu) untuk menggantikan MediaPlayer Soundboard setelah desain lifecycle, audio focus, media notification, dan service ownership siap. Jangan mengambil semua modul Media3 atau memakai versi beta pada jalur stabil.

## Status hasil riset repository

- Repository eksternal yang diperiksa pada audit ini: lebih dari 12 kandidat lintas AI lokal, speech-to-text, TTS, media player, database, dan TikTok LIVE.
- Tidak ada repository pihak ketiga yang dikloning atau disalin utuh.
- Komponen yang sudah benar-benar diadopsi pada versi 0.5.0 adalah AndroidX Preferences DataStore resmi, bukan source dari sample pihak ketiga.
- Lisensi yang tidak eksplisit/metadata NOASSERTION diperlakukan sebagai **tidak lolos adopsi kode**.
- Semua integrasi native AI, LIVE event, dan persistent soundboard tetap memerlukan proof-of-concept dan pengujian perangkat sebelum dinyatakan aktif.


## 17. sherpa-onnx — speech toolkit offline (prioritas riset tinggi)

Repository: https://github.com/k2-fsa/sherpa-onnx  
Lisensi metadata GitHub: Apache-2.0.  
Repository aktif dan README mendokumentasikan Android, Kotlin/Java, ARM64, ASR streaming dan non-streaming, TTS lokal, VAD, keyword spotting, speaker diarization, speech enhancement, dan source separation.

**Nilai guna:** kandidat paling lengkap untuk membangun lapisan suara offline terpadu: keyword spotting, speech-to-text streaming, dan neural TTS tanpa harus bergantung pada cloud.  
**Risiko:** runtime native dan model terpisah; lisensi model/voice berbeda-beda dan wajib diverifikasi satu per satu. Ukuran model, RAM, latency, ABI, konsumsi baterai, serta dukungan Bahasa Indonesia harus diuji pada perangkat target.  
**Keputusan:** kandidat utama untuk proof-of-concept terpisah. Belum ditambahkan ke APK utama agar ukuran APK, stabilitas build, dan konsumsi RAM tidak membengkak sebelum model Bahasa Indonesia yang sesuai dipilih.

## 18. Vosk Android Demo — referensi ASR offline

Repository: https://github.com/alphacep/vosk-android-demo  
Lisensi metadata GitHub: Apache-2.0.  
README menyebut pengenalan suara offline dan speaker identification untuk Android melalui Kaldi/Vosk.

**Nilai guna:** alternatif ASR offline yang dapat dibandingkan dengan sherpa-onnx dari sisi ukuran model, latensi, akurasi Bahasa Indonesia, dan konsumsi memori.  
**Risiko:** demo bukan aplikasi produksi siap tempel; kualitas bergantung pada model bahasa yang tersedia.  
**Keputusan:** jadikan baseline pembanding, jangan integrasikan sebelum benchmark Bahasa Indonesia dilakukan.

## 19. HayaiTTS — neural TTS Android

Repository: https://github.com/HayaiApp/HayaiTTS  
Lisensi metadata GitHub: NOASSERTION; README dan badge menyatakan GPL-3.0.  
README mendokumentasikan engine TTS Android sistem berbasis sherpa-onnx, model offline multi-voice, streaming synthesis, dan katalog suara.

**Nilai guna:** referensi teknis untuk neural TTS sistem dan pengelolaan model suara.  
**Risiko:** GPL-3.0 dapat memengaruhi kewajiban distribusi jika kode/binary digabung; model suara memiliki lisensi masing-masing.  
**Keputusan:** tidak menyalin source atau binary ke aplikasi. Gunakan hanya sebagai referensi arsitektur; implementasi terpisah harus mempertimbangkan lisensi dan kompatibilitas.

## 20. AndroidX Media3 — audio playback resmi

Repository: https://github.com/androidx/media  
Lisensi metadata GitHub: Apache-2.0.  
Repository ExoPlayer lama (google/ExoPlayer) sudah deprecated; AndroidX Media3 adalah jalur pengembangan resmi.

**Implementasi 0.5.1:** Soundboard berpindah dari Android MediaPlayer ke AndroidX Media3 ExoPlayer 1.9.4. Versi ini dipilih agar tetap kompatibel dengan compileSdk 35 dan Android Gradle Plugin 8.7.3; versi Media3 1.11.1 mensyaratkan compileSdk 36. Playback menggunakan URI dari Storage Access Framework, audio attributes, pengelolaan audio focus, penanganan audio-becoming-noisy, status buffering/playing/completed/error, serta pelepasan player pada dispose.

**Batasan:** sesi soundboard masih terikat pada halaman UI dan dihentikan saat halaman ditutup. MediaSessionService, playback lintas halaman, mixing beberapa suara, dan automasi event belum diimplementasikan.


## 21. OfflineVoiceAI-Android — facade STT/TTS offline

Repository: https://github.com/edgeaitechnologies/offline-voice-ai
README mendokumentasikan Kotlin API coroutine-friendly yang membungkus Vosk untuk STT dan sherpa-onnx untuk TTS, termasuk chunked/streamed synthesis dan auto-stop berbasis silence timeout.

**Nilai guna:** kandidat untuk merancang satu interface suara yang dapat diganti backend-nya, tanpa mengikat UI ke engine tertentu.
**Risiko:** lisensi dan artefak rilis harus diverifikasi pada commit yang akan dipakai; model bahasa dan voice model harus ditinjau lisensinya secara terpisah.
**Keputusan:** kandidat proof-of-concept; belum ditambahkan sebagai dependency.

## 22. LocalSLM — Android LLM berbasis llama.cpp/JNI

Repository: https://github.com/rushi-gulum/LocalSLM
README mendokumentasikan Kotlin + Compose, JNI/C++, llama.cpp CPU/ARM NEON, Room, DataStore, WorkManager, model manager, GGUF validation, dan export percakapan.

**Nilai guna:** contoh end-to-end untuk manajemen model dan inferensi lokal di Android.
**Risiko:** memerlukan NDK/native build dan model eksternal; kompatibilitas AGP/Kotlin dan lisensi pada commit spesifik harus diverifikasi.
**Keputusan:** referensi arsitektur untuk proof-of-concept, tidak menyalin source secara langsung.

## 23. OffHand — offline-first voice agent dengan persetujuan tindakan

Repository: https://github.com/MRDarshan7/OffHand
README mendokumentasikan LLM lokal, grammar-constrained decoding, Vosk, OCR offline, Room, WorkManager outbox, serta konfirmasi manusia sebelum tindakan sensitif.

**Nilai guna:** pola keselamatan yang relevan untuk agent kreator: model boleh menyusun tindakan, tetapi tindakan eksternal harus melalui preview, izin, dan persetujuan pengguna.
**Risiko:** contoh email/laptop bridge dan konfigurasi SMTP tidak boleh diaktifkan tanpa threat model, secret handling, dan pembatasan tindakan.
**Keputusan:** adopsi prinsip desain human-in-the-loop; tidak mengimpor keseluruhan aplikasi.

## 24. OpenDroid — autonomous Android agent

Repository: https://github.com/JMAN730/opendroid
README mendeskripsikan agen Android berbasis LLM lokal/remote dan otomasi UI melalui Accessibility.

**Nilai guna:** referensi perencanaan multi-langkah, tool registry, dan eksekusi tindakan berbasis izin.
**Risiko:** otomasi UI dapat melakukan tindakan yang tidak diinginkan, AccessibilityService sangat sensitif, dan klaim "production-ready" dari pemilik repo bukan hasil audit independen.
**Keputusan:** riset desain saja. Tidak menambahkan AccessibilityService atau kemampuan mengendalikan aplikasi lain tanpa fitur eksplisit, disclosure, kontrol pengguna, dan pengujian keamanan.

## 25. AVA Android Assistant — referensi multi-provider

Repository: https://github.com/crftsmnd/ava-android-assistant
README mendokumentasikan wake word, Android SpeechRecognizer, TTS, beberapa backend AI, shortcut aplikasi, overlay, dan AccessibilityService. README menyatakan lisensi MIT.

**Nilai guna:** referensi adapter AI provider dan command registry.
**Risiko:** beberapa kemampuan system-control dibatasi Android modern dan memerlukan izin khusus atau konfirmasi; klaim kemampuan repo tidak berarti dapat digunakan tanpa batas di semua perangkat.
**Keputusan:** referensi arsitektur, bukan salin-tempel.

## 26. Sanna — voice-first agent dan automasi

Repository: https://github.com/sannabotdev/sannabotapp
README mendeskripsikan assistant voice-first, rangkaian tindakan multi-langkah, Accessibility-based app control, notifikasi, dan scheduled tasks.

**Nilai guna:** referensi orchestration dan interaksi berbasis suara.
**Risiko:** lisensi, keamanan aksi lintas aplikasi, dan batasan platform perlu diaudit sebelum adopsi.
**Keputusan:** tidak mengambil source atau binary sampai lisensi dan izin tiap kemampuan diverifikasi.

## 27. VoicePing Android Offline Transcribe — multi-engine ASR

Repository: https://github.com/voiceping-ai/android-offline-transcribe
README mendokumentasikan beberapa backend ASR (sherpa-onnx, whisper.cpp, Qwen ASR, Android SpeechRecognizer), VAD, chunking, metrik sistem, dan pengujian.

**Nilai guna:** referensi benchmark yang lebih kaya untuk memilih ASR Bahasa Indonesia berdasarkan akurasi, latency, RAM, baterai, dan dukungan streaming.
**Risiko:** banyak backend berarti dependency/native footprint besar; setup membutuhkan NDK/CMake dan model.
**Keputusan:** gunakan sebagai daftar kandidat benchmark, bukan memasukkan semua engine ke APK.

## 28. Media3 + MediaSession audio player — referensi lifecycle

Repository: https://github.com/skaftab-in/android-music-player
README mendokumentasikan Media3 ExoPlayer, MediaSessionService, Room playlists, foreground media notification, Bluetooth/lock-screen controls, dan audio focus. Lisensi repository perlu diperiksa pada file LICENSE sebelum source digunakan.

**Nilai guna:** rujukan langsung untuk tahap berikutnya: memindahkan kepemilikan player dari Composable ke service dan memisahkan playback lifecycle dari navigasi UI.
**Keputusan:** gunakan API resmi AndroidX Media3; jangan menyalin kode repository sebelum lisensi dan dependency diverifikasi.

## 29. Media playback service modern — referensi tambahan

Repository: https://github.com/alsaeeddev/android-video-music-player
README mendokumentasikan Media3, MediaSession, Room, DataStore, MediaStore, dan service playback latar belakang.

**Nilai guna:** contoh pemisahan PlayerController dari layar dan state Flow untuk playback.
**Risiko:** kebutuhan aplikasi ini lebih kecil daripada pemutar musik/video penuh; izin media library yang tidak diperlukan tidak boleh ditambahkan.
**Keputusan:** ambil prinsip arsitektur saja, gunakan Storage Access Framework yang sudah dipakai aplikasi.

## 30. Prioritas implementasi berdasarkan riset 21–29

1. **Sudah diadopsi pada source 0.5.2:** metadata soundboard dipindahkan ke Preferences DataStore dengan migrasi SharedPreferences dan kompatibilitas format record lama.
2. **Berikutnya:** MediaSessionService resmi untuk soundboard; gunakan media session, bukan service buatan dari repo eksternal.
3. **Berikutnya:** voice pipeline modular dengan adapter STT/TTS dan benchmark Bahasa Indonesia; permission mikrofon baru ditambahkan ketika fitur rekam/pengenalan suara benar-benar diimplementasikan.
4. **Berikutnya:** proof-of-concept local LLM dengan model download terpisah, validasi checksum, batas storage/RAM, dan tombol stop.
5. **Selanjutnya:** event adapter LIVE yang sah; setiap tindakan otomatis harus melewati rule engine, batas rate, audit log, dan konfigurasi persetujuan pengguna.

Tidak ada repository yang diunduh atau digabungkan secara utuh. Integrasi source eksternal memerlukan lisensi eksplisit, atribusi, pemeriksaan dependency/transitive dependency, dan review keamanan.


## Pembaruan dependensi media — 16 November 2026

AndroidX Media3 diperbarui secara terkoordinasi dari 1.9.4 ke **1.11.1**, rilis stabil terbaru yang tercantum pada AndroidX release index saat pembaruan ini. Versi 1.12.0-beta01 tidak dipakai karena masih prarilis. Modul `media3-exoplayer` dan `media3-session` memakai versi yang sama. Kompatibilitas diuji melalui Android CI (unit tests dan assembleDebug); interaksi notifikasi sistem, lockscreen, headset, dan Bluetooth tetap memerlukan uji perangkat.

Rujukan resmi: https://developer.android.com/jetpack/androidx/releases/media3
