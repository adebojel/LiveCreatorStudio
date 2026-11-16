package id.livecreator.studio

import android.content.Context
import android.content.Intent
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
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
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SoundboardScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val sounds by SoundboardStore.observe(context).collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    var activeUri by remember { mutableStateOf<String?>(null) }
    var isPreparing by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("Impor file audio dari perangkat untuk membuat pustaka suara.") }
    var player by remember { mutableStateOf<ExoPlayer?>(null) }

    fun releaseCurrent() {
        val current = player
        player = null
        activeUri = null
        isPreparing = false
        if (current != null) {
            runCatching { current.stop() }
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
                scope.launch { SoundboardStore.save(context, (sounds + item).sortedBy { it.title.lowercase() }) }
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
        val next = ExoPlayer.Builder(context).build()
        player = next
        activeUri = sound.uri
        isPreparing = true
        message = "Membuka audio: ${sound.title}"
        try {
            next.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_SONIFICATION)
                    .build(),
                true
            )
            next.setHandleAudioBecomingNoisy(true)
            next.addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (player !== next) return
                    when (playbackState) {
                        Player.STATE_BUFFERING -> {
                            isPreparing = true
                            message = "Menyiapkan audio: ${sound.title}"
                        }
                        Player.STATE_READY -> {
                            isPreparing = false
                            if (next.isPlaying) message = "Sedang memutar: ${sound.title}"
                        }
                        Player.STATE_ENDED -> {
                            releaseCurrent()
                            message = "Pemutaran selesai: ${sound.title}"
                        }
                        Player.STATE_IDLE -> Unit
                    }
                }

                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    if (player === next && isPlaying) {
                        isPreparing = false
                        message = "Sedang memutar: ${sound.title}"
                    }
                }

                override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                    if (player === next) {
                        releaseCurrent()
                        message = "Audio gagal diputar: ${error.errorCodeName}. Periksa file atau formatnya."
                    }
                }
            })
            next.setMediaItem(MediaItem.fromUri(Uri.parse(sound.uri)))
            next.prepare()
            next.play()
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
                                    scope.launch { SoundboardStore.save(context, sounds.filterNot { it.uri == sound.uri }) }
                                    releasePersistedReadPermission(context, Uri.parse(sound.uri))
                                    message = "File dihapus dari daftar aplikasi dan izin dokumennya dilepas."
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

private fun displayName(context: Context, uri: Uri): String {
    val name = runCatching {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0) else null
        }
    }.getOrNull()
    return name?.takeIf { it.isNotBlank() } ?: ("Audio " + (uri.lastPathSegment ?: "tanpa nama"))
}


/**
 * Releases the persisted Storage Access Framework read grant when an item is
 * removed or its source is no longer readable. Android may already have
 * revoked the grant, so cleanup is deliberately best-effort.
 */
private fun releasePersistedReadPermission(context: Context, uri: Uri) {
    runCatching {
        context.contentResolver.releasePersistableUriPermission(
            uri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION
        )
    }
}
