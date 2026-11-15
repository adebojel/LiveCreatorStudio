package id.livecreator.studio

import android.content.Context
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
    var message by remember { mutableStateOf("Impor audio milikmu untuk membuat soundboard lokal.") }
    var player by remember { mutableStateOf<MediaPlayer?>(null) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val item = SoundItem(uri.toString(), displayName(context, uri))
            if (sounds.none { it.uri == item.uri }) {
                sounds = sounds + item
                saveSounds(prefs, sounds)
                message = "Audio berhasil ditambahkan."
            } else message = "Audio tersebut sudah ada di pustaka."
        }
    }

    fun stopAudio() {
        runCatching { player?.stop() }
        runCatching { player?.release() }
        player = null
        activeUri = null
    }

    DisposableEffect(Unit) { onDispose { stopAudio() } }

    Column(modifier.fillMaxSize().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("CREATOR TOOLKIT", color = Color(0xFF54D8E8), fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Text("Soundboard", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold)
        Text("Pustaka efek suara lokal. Pilih file audio dari perangkat, simpan ke daftar, lalu putar atau hentikan kapan saja.", color = Color.LightGray, fontSize = 12.sp)
        Button(onClick = { picker.launch(arrayOf("audio/*")) }, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Tambah file audio")
        }
        Surface(color = Color(0xCC171F35), shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
            Text(message, color = Color.LightGray, modifier = Modifier.padding(14.dp), fontSize = 12.sp)
        }
        if (sounds.isEmpty()) {
            Surface(color = Color(0xCC171F35), shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.LibraryMusic, contentDescription = null, tint = Color(0xFF9B7BFF))
                    Text("Belum ada suara", color = Color.White, fontWeight = FontWeight.SemiBold)
                    Text("Tambahkan file audio dari perangkat. Tidak ada audio contoh atau simulasi.", color = Color.LightGray, fontSize = 11.sp)
                }
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(sounds, key = { it.uri }) { sound ->
                    Surface(color = Color(0xCC171F35), shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.LibraryMusic, contentDescription = null, tint = Color(0xFF54D8E8))
                            Text(sound.title, color = Color.White, modifier = Modifier.weight(1f), fontSize = 12.sp, maxLines = 2)
                            OutlinedButton(onClick = {
                                stopAudio()
                                runCatching {
                                    val next = MediaPlayer.create(context, Uri.parse(sound.uri)) ?: error("Format audio tidak dapat dibuka.")
                                    next.setOnCompletionListener {
                                        runCatching { it.release() }
                                        if (activeUri == sound.uri) {
                                            player = null
                                            activeUri = null
                                            message = "Pemutaran selesai."
                                        }
                                    }
                                    player = next
                                    activeUri = sound.uri
                                    next.start()
                                    message = "Memutar: " + sound.title
                                }.onFailure {
                                    stopAudio()
                                    message = "Gagal memutar audio: " + (it.message ?: "format tidak didukung")
                                }
                            }) { Icon(Icons.Default.PlayArrow, contentDescription = "Putar " + sound.title) }
                            OutlinedButton(onClick = {
                                if (activeUri == sound.uri) stopAudio()
                                sounds = sounds.filterNot { it.uri == sound.uri }
                                saveSounds(prefs, sounds)
                                message = "Audio dihapus dari pustaka."
                            }) { Icon(Icons.Default.Delete, contentDescription = "Hapus " + sound.title) }
                        }
                    }
                }
            }
        }
        if (activeUri != null) {
            OutlinedButton(onClick = { stopAudio(); message = "Pemutaran dihentikan." }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Stop, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Hentikan suara")
            }
        }
        Text("Pustaka menyimpan referensi file yang dipilih. Jika file asal dipindah atau dihapus, pilih ulang file tersebut. Pemutaran berlangsung selama layar aplikasi aktif.", color = Color.LightGray, fontSize = 10.sp)
    }
}

private fun saveSounds(prefs: android.content.SharedPreferences, sounds: List<SoundItem>) {
    prefs.edit().putStringSet("items", sounds.map { it.uri + "|" + it.title.replace("|", " ") }.toSet()).apply()
}

private fun loadSounds(context: Context, prefs: android.content.SharedPreferences): List<SoundItem> {
    return prefs.getStringSet("items", emptySet()).orEmpty().mapNotNull { entry ->
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
}

private fun displayName(context: Context, uri: Uri): String {
    val name = runCatching {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0) else null
        }
    }.getOrNull()
    return name?.takeIf { it.isNotBlank() } ?: ("Audio " + (uri.lastPathSegment ?: "tanpa nama"))
}
