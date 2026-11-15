# Audit Repository Eksternal — LiveCreator Studio

Tanggal pemeriksaan: 16 November 2026  
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
