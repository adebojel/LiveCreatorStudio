package id.livecreator.studio

import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private data class SoundItem(val uri: String, val title: String)

@Composable
fun SoundboardScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("livecreator_soundboard", Context.MODE_PRIVATE) }
    var sounds by remember { mutableStateOf(loadSounds(context, prefs)) }
    var activeUri by remember { mutableStateOf<String?>(null) }
    var isPreparing by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("Impor file audio dari perangkat untuk membuat pustaka suara.") }
    var player by remember { mutableStateOf<MediaPlayer?>(null) }

    fun releaseCurrent() {
        val current = player
        player = null
        activeUri = null
        isPreparing = false
        if (current != null) {
            runCatching { current.setOnCompletionListener(null) }
            runCatching { current.setOnErrorListener(null) }
            runCatching { current.release() }
        }
    }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val importResult = runCatching {
            val mimeType = context.contentResolver.getType(uri)
            require(mimeType?.startsWith("audio/") == true) { "File yang dipilih bukan tipe audio." }
            context.contentResolver.openAssetFileDescriptor(uri, "r")?.use { } 
                ?: error("File tidak dapat dibaca.")
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
            SoundItem(uri.toString(), displayName(context, uri))
        }
        importResult.onSuccess { item ->
            if (sounds.none { it.uri == item.uri }) {
                sounds = (sounds + item).sortedBy { it.title.lowercase() }
                saveSounds(prefs, sounds)
                message = "File berhasil diimpor dan izin aksesnya disimpan."
            } else {
                message = "File tersebut sudah ada di pustaka."
            }
        }.onFailure { error ->
            message = "Impor gagal: ${error.message ?: "akses file tidak tersedia"}"
        }
    }

    DisposableEffect(Unit) {
        onDispose { releaseCurrent() }
    }

    fun playSound(sound: SoundItem) {
        releaseCurrent()
        val next = MediaPlayer()
        player = next
        activeUri = sound.uri
        isPreparing = true
        message = "Membuka audio: ${sound.title}"
        try {
            next.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            next.setOnPreparedListener { prepared ->
                if (player === next) {
                    isPreparing = false
                    runCatching { prepared.start() }.onSuccess {
                        message = "Sedang memutar: ${sound.title}"
                    }.onFailure {
                        releaseCurrent()
                        message = "Pemutaran gagal: ${it.message ?: "kesalahan audio"}"
                    }
                } else {
                    runCatching { prepared.release() }
                }
            }
            next.setOnCompletionListener { completed ->
                runCatching { completed.release() }
                if (player === next) {
                    player = null
                    activeUri = null
                    isPreparing = false
                    message = "Pemutaran selesai: ${sound.title}"
                }
            }
            next.setOnErrorListener { failed, what, extra ->
                runCatching { failed.release() }
                if (player === next) {
                    player = null
                    activeUri = null
                    isPreparing = false
                    message = "Audio gagal diputar (kode $what/$extra). Periksa file atau formatnya."
                }
                true
            }
            next.setDataSource(context, Uri.parse(sound.uri))
            next.prepareAsync()
        } catch (error: Exception) {
            releaseCurrent()
            message = "Tidak dapat membuka audio: ${error.message ?: "file tidak tersedia"}"
        }
    }

    Column(
        modifier.fillMaxSize().padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("CREATOR TOOLKIT", color = Color(0xFF54D8E8), fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Text("Soundboard", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold)
        Text(
            "Impor file audio milikmu, simpan aksesnya, lalu putar atau hentikan dari perangkat.",
            color = Color.LightGray,
            fontSize = 12.sp
        )
        Button(
            onClick = { picker.launch(arrayOf("audio/*")) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Tambah file audio")
        }
        Surface(color = Color(0xCC171F35), shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
            Text(
                message,
                color = Color.LightGray,
                modifier = Modifier.padding(14.dp).semantics { contentDescription = message },
                fontSize = 12.sp
            )
        }
        if (sounds.isEmpty()) {
            Surface(color = Color(0xCC171F35), shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.LibraryMusic, contentDescription = null, tint = Color(0xFF9B7BFF))
                    Text("Pustaka masih kosong", color = Color.White, fontWeight = FontWeight.SemiBold)
                    Text(
                        "Belum ada file audio yang diimpor. Tidak ada audio contoh.",
                        color = Color.LightGray,
                        fontSize = 11.sp
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f, fill = true),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(sounds, key = { it.uri }) { sound ->
                    Surface(color = Color(0xCC171F35), shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                        Row(
                            Modifier.fillMaxWidth().padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.LibraryMusic, contentDescription = null, tint = Color(0xFF54D8E8))
                            Text(
                                sound.title,
                                color = Color.White,
                                modifier = Modifier.weight(1f),
                                fontSize = 12.sp,
                                maxLines = 2
                            )
                            OutlinedButton(
                                onClick = { playSound(sound) },
                                enabled = activeUri != sound.uri && !isPreparing,
                                modifier = Modifier.semantics {
                                    contentDescription = "Putar ${sound.title}"
                                }
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null)
                            }
                            OutlinedButton(
                                onClick = {
                                    if (activeUri == sound.uri) releaseCurrent()
                                    sounds = sounds.filterNot { it.uri == sound.uri }
                                    saveSounds(prefs, sounds)
                                    message = "File dihapus dari daftar aplikasi."
                                },
                                modifier = Modifier.semantics {
                                    contentDescription = "Hapus ${sound.title} dari pustaka"
                                }
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = null)
                            }
                        }
                    }
                }
            }
        }
        if (activeUri != null) {
            OutlinedButton(
                onClick = {
                    releaseCurrent()
                    message = "Pemutaran dihentikan."
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Stop, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(if (isPreparing) "Batalkan pemuatan audio" else "Hentikan suara")
            }
        }
        Text(
            "Akses file disimpan melalui izin dokumen Android. Jika sumber file dihapus atau izinnya dicabut, impor ulang file tersebut. Audio berhenti ketika halaman Soundboard ditutup.",
            color = Color.LightGray,
            fontSize = 10.sp
        )
    }
}

private fun saveSounds(prefs: android.content.SharedPreferences, sounds: List<SoundItem>) {
    val entries = sounds.map { it.uri + "|" + it.title.replace("|", " ") }.toSet()
    prefs.edit().putStringSet("items", entries).apply()
}

private fun loadSounds(context: Context, prefs: android.content.SharedPreferences): List<SoundItem> {
    val stored = prefs.getStringSet("items", emptySet()).orEmpty().toList()
    val valid = stored.mapNotNull { entry ->
        val split = entry.indexOf('|')
        if (split <= 0) null else {
            val uri = entry.substring(0, split)
            val title = entry.substring(split + 1)
            val readable = runCatching {
                context.contentResolver.openAssetFileDescriptor(Uri.parse(uri), "r")?.use { true } ?: false
            }.getOrDefault(false)
            if (readable) SoundItem(uri, title) else null
        }
    }.sortedBy { it.title.lowercase() }
    if (valid.size != stored.size) saveSounds(prefs, valid)
    return valid
}

private fun displayName(context: Context, uri: Uri): String {
    val name = runCatching {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0) else null
        }
    }.getOrNull()
    return name?.takeIf { it.isNotBlank() } ?: ("Audio " + (uri.lastPathSegment ?: "tanpa nama"))
}
