package id.livecreator.studio

import android.content.ComponentName
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.media.AudioManager
import android.media.ToneGenerator
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.GraphicEq
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
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import kotlinx.coroutines.launch
import java.util.concurrent.Executor

@Composable
fun SoundboardScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current.applicationContext
    val sounds by SoundboardStore.observe(context).collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    var controller by remember { mutableStateOf<MediaController?>(null) }
    var activeUri by remember { mutableStateOf<String?>(null) }
    var isPreparing by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("Pustaka suara CC0 sedang disiapkan otomatis pada penggunaan pertama.") }
    val packState by SoundPackInstaller.state.collectAsState()

    val playerListener = remember {
        object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                when (playbackState) {
                    Player.STATE_BUFFERING -> {
                        isPreparing = true
                        message = "Menyiapkan audio."
                    }
                    Player.STATE_READY -> {
                        isPreparing = false
                        if (controller?.isPlaying == true) message = "Audio sedang diputar."
                    }
                    Player.STATE_ENDED -> {
                        activeUri = null
                        isPreparing = false
                        message = "Pemutaran selesai."
                    }
                    Player.STATE_IDLE -> if (controller?.mediaItemCount == 0) {
                        activeUri = null
                        isPreparing = false
                    }
                }
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                activeUri = mediaItem?.localConfiguration?.uri?.toString()
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (isPlaying) {
                    isPreparing = false
                    message = "Audio sedang diputar."
                } else if (controller?.playbackState == Player.STATE_READY) {
                    message = "Pemutaran dijeda."
                }
            }

            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                activeUri = null
                isPreparing = false
                message = "Audio gagal diputar: ${error.errorCodeName}. Periksa file atau formatnya."
            }
        }
    }

    DisposableEffect(context, playerListener) {
        val token = SessionToken(context, ComponentName(context, SoundboardPlaybackService::class.java))
        val future = MediaController.Builder(context, token).buildAsync()
        var disposed = false
        var connected: MediaController? = null
        future.addListener({
            if (disposed) {
                runCatching { future.get().release() }
            } else {
                runCatching { future.get() }
                    .onSuccess { mediaController ->
                        connected = mediaController
                        mediaController.addListener(playerListener)
                        controller = mediaController
                        message = "Soundboard siap. Playback dapat berlanjut saat halaman ditutup."
                    }
                    .onFailure {
                        message = "Layanan playback belum dapat dihubungkan. Buka ulang Soundboard."
                    }
            }
        }, Executor { command -> Handler(Looper.getMainLooper()).post(command) })

        onDispose {
            disposed = true
            connected?.let {
                it.removeListener(playerListener)
                it.release()
            } ?: run {
                if (!future.isDone) future.cancel(false)
            }
            if (controller === connected) controller = null
        }
    }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val importResult = runCatching {
            SoundboardFileImport.import(context, uri)
        }
        importResult.onSuccess { item ->
            if (sounds.none { it.uri == item.uri }) {
                scope.launch { SoundboardStore.save(context, (sounds + item).sortedBy { it.title.lowercase() }) }
                message = "File berhasil diimpor dan izin aksesnya disimpan."
            } else message = "File tersebut sudah ada di pustaka."
        }.onFailure { error ->
            message = "Impor gagal: ${error.message ?: "akses file tidak tersedia"}"
        }
    }

    fun stopPlayback() {
        controller?.run {
            stop()
            clearMediaItems()
        }
        activeUri = null
        isPreparing = false
    }

    fun playSound(sound: SoundItem) {
        val mediaController = controller
        if (mediaController == null) {
            message = "Layanan playback sedang disiapkan. Coba kembali sebentar lagi."
            return
        }
        try {
            val item = MediaItem.Builder()
                .setUri(Uri.parse(sound.uri))
                .setMediaMetadata(MediaMetadata.Builder().setTitle(sound.title).build())
                .build()
            mediaController.setMediaItem(item)
            mediaController.prepare()
            mediaController.play()
            activeUri = sound.uri
            isPreparing = true
            message = "Membuka audio: ${sound.title}"
        } catch (error: Exception) {
            stopPlayback()
            message = "Tidak dapat membuka audio: ${error.message ?: "file tidak tersedia"}"
        }
    }

    Column(modifier.fillMaxSize().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("CREATOR TOOLKIT", color = Color(0xFF54D8E8), fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Text("Soundboard", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold)
        Text("Paket suara CC0 diunduh otomatis saat aplikasi pertama kali dibuka. Impor manual tetap tersedia sebagai pilihan.", color = Color.LightGray, fontSize = 12.sp)
        OutlinedButton(
            onClick = { scope.launch { SoundPackInstaller.installAll(context) } },
            enabled = packState !is SoundPackInstallState.Downloading,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.LibraryMusic, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Unduh semua paket suara (${SoundPackCatalog.packs.size} paket)")
        }
        when (val state = packState) {
            is SoundPackInstallState.Downloading -> {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                Text("Mengunduh paket ${state.packTitle}…", color = Color(0xFF54D8E8), fontSize = 11.sp)
            }
            is SoundPackInstallState.Installed -> Text(
                "${state.packTitle}: ${state.soundCount} suara siap digunakan.",
                color = Color(0xFF79D7A5), fontSize = 11.sp
            )
            is SoundPackInstallState.Failed -> {
                Text("Unduhan ${state.packTitle} tertunda: ${state.reason}", color = Color(0xFFFFC078), fontSize = 11.sp)
                TextButton(onClick = { scope.launch { SoundPackInstaller.ensureStarterPacks(context) } }) {
                    Text("Coba unduh ulang paket awal")
                }
            }
            SoundPackInstallState.Idle -> Text(
                "Paket awal: 210 suara. Tersedia ${SoundPackCatalog.packs.size} paket dengan sekitar 1.444 aset CC0.",
                color = Color.LightGray, fontSize = 11.sp
            )
        }
        OutlinedTextField(
            value = searchQuery, onValueChange = { searchQuery = it },
            modifier = Modifier.fillMaxWidth(), singleLine = true,
            placeholder = { Text("Cari suara…", color = Color(0xFF8290A8)) },
            leadingIcon = { Icon(Icons.Default.GraphicEq, contentDescription = "Cari suara", tint = Color(0xFF72DDEB)) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                focusedBorderColor = Color(0xFF59DDEB), unfocusedBorderColor = Color(0x554E6A86),
                cursorColor = Color(0xFF59DDEB), focusedContainerColor = Color(0x331B2940),
                unfocusedContainerColor = Color(0x221B2940)
            ), shape = RoundedCornerShape(16.dp)
        )\n        Button(onClick = { picker.launch(arrayOf("audio/*")) }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF285F83), contentColor = Color.White)) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Tambah file audio")
        }
        OutlinedButton(
            onClick = {
                val tone = ToneGenerator(AudioManager.STREAM_MUSIC, 75)
                val started = tone.startTone(ToneGenerator.TONE_PROP_ACK, 450)
                message = if (started) "Nada tes singkat diputar. Jika tidak terdengar, periksa volume media perangkat."
                    else "Nada tes tidak dapat dimulai pada perangkat ini."
                Handler(Looper.getMainLooper()).postDelayed({ tone.release() }, 650)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.GraphicEq, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Tes speaker tanpa file audio")
        }
        Surface(color = Color(0x99192339), shape = RoundedCornerShape(16.dp), border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x4449C9E4)), modifier = Modifier.fillMaxWidth()) {
            Text(message, color = Color.LightGray, modifier = Modifier.padding(14.dp).semantics { contentDescription = message }, fontSize = 12.sp)
        }
        if (sounds.isEmpty()) {
            Surface(color = Color(0x99192339), shape = RoundedCornerShape(18.dp), border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x554E74A2)), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.LibraryMusic, contentDescription = null, tint = Color(0xFF9B7BFF))
                    Text("Pustaka masih kosong", color = Color.White, fontWeight = FontWeight.SemiBold)
                    Text("Belum ada file audio. Impor MP3/WAV atau gunakan tes speaker untuk memeriksa keluaran suara.", color = Color.LightGray, fontSize = 11.sp)
                }
            }
        } else {
            val visibleSounds = sounds.filter { it.title.contains(searchQuery, ignoreCase = true) }
            LazyColumn(modifier = Modifier.weight(1f, fill = true), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(visibleSounds, key = { it.uri }) { sound ->
                    Surface(color = Color(0xAA1A2940), shape = RoundedCornerShape(16.dp), border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x3347CDE8)), modifier = Modifier.fillMaxWidth()) {
                        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.LibraryMusic, contentDescription = "Audio", tint = Color(0xFF66DDF1))
                            Text(sound.title, color = Color.White, modifier = Modifier.weight(1f), fontSize = 12.sp, maxLines = 2)
                            OutlinedButton(onClick = { playSound(sound) }, enabled = activeUri != sound.uri && !isPreparing,
                                modifier = Modifier.semantics { contentDescription = "Putar ${sound.title}" }) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null)
                            }
                            OutlinedButton(onClick = {
                                if (activeUri == sound.uri) stopPlayback()
                                scope.launch { SoundboardStore.save(context, sounds.filterNot { it.uri == sound.uri }) }
                                SoundboardFileImport.delete(context, Uri.parse(sound.uri))
                                message = "File dihapus dari daftar aplikasi dan izin dokumennya dilepas."
                            }, modifier = Modifier.semantics { contentDescription = "Hapus ${sound.title} dari pustaka" }) {
                                Icon(Icons.Default.Delete, contentDescription = null)
                            }
                        }
                    }
                }
            }
        }
        if (activeUri != null) {
            OutlinedButton(onClick = {
                stopPlayback()
                message = "Pemutaran dihentikan."
            }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Stop, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(if (isPreparing) "Batalkan pemuatan audio" else "Hentikan suara")
            }
        }
        Text("Playback berjalan pada layanan media terpisah. Gunakan notifikasi media, lockscreen, headset, atau Bluetooth untuk kontrol saat aplikasi berada di latar belakang.", color = Color.LightGray, fontSize = 10.sp)
    }
}

