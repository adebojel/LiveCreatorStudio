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
