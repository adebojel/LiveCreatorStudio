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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
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
    var sourceFilter by remember { mutableStateOf("Semua") }
    var soundCategoryFilter by remember { mutableStateOf("Semua") }
    var packCategoryFilter by remember { mutableStateOf("Semua") }
    var showPackCatalog by remember { mutableStateOf(false) }
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
                message = "File berhasil disalin ke penyimpanan aplikasi."
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
        Text("Audio siap pakai, tersusun rapi, dan bisa diputar offline setelah terpasang.", color = Color.LightGray, fontSize = 12.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(color = Color(0x5538CDE0), shape = RoundedCornerShape(50)) {
                Text("${sounds.size} suara", color = Color.White, modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp), fontSize = 11.sp)
            }
            Surface(color = Color(0x5553DDF2), shape = RoundedCornerShape(50)) {
                Text("${SoundPackCatalog.packs.size} paket CC0", color = Color.White, modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp), fontSize = 11.sp)
            }
        }
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

        OutlinedButton(onClick = { showPackCatalog = true }, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.LibraryMusic, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Katalog paket • pilih kategori & unduh")
        }
        if (showPackCatalog) {
            Dialog(onDismissRequest = { showPackCatalog = false }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
                Surface(
                    modifier = Modifier.fillMaxWidth(0.94f).fillMaxHeight(0.82f),
                    color = Color(0xFF101D32), shape = RoundedCornerShape(24.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF42D9F2))
                ) {
                    Column(Modifier.fillMaxSize().padding(14.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("Katalog Soundboard", color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                                Text("Pilih kategori, tes suara, lalu unduh", color = Color.LightGray, fontSize = 11.sp)
                            }
                            TextButton(onClick = { showPackCatalog = false }) { Text("Tutup") }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            listOf("Semua", "Lucu", "Game", "Film", "UI", "Voice").forEach { category ->
                                FilterChip(selected = packCategoryFilter == category, onClick = { packCategoryFilter = category }, label = { Text(category, fontSize = 10.sp) })
                            }
                        }
                        OutlinedButton(
                            onClick = { scope.launch { SoundPackInstaller.installAll(context) } },
                            enabled = packState !is SoundPackInstallState.Downloading,
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("⬇  Unduh semua paket") }
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(7.dp), modifier = Modifier.weight(1f)) {
                            val categorizedPacks = SoundPackCatalog.packs.filter { pack ->
                                packCategoryFilter == "Semua" || packCategory(pack) == packCategoryFilter
                            }
                            items(categorizedPacks, key = { it.id }) { pack ->
                                val installed = SoundPackInstaller.isInstalled(context, pack)
                                Surface(color = Color(0xCC1B2B43), shape = RoundedCornerShape(14.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x5547CDE8))) {
                                    Row(Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Column(Modifier.weight(1f)) {
                                            Text("${packEmoji(pack)}  ${pack.title}", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                            Text(if (installed) "Terpasang • offline" else "${packCategory(pack)} • ~${pack.assetCount} aset sumber",
                                                color = if (installed) Color(0xFF79D7A5) else Color.LightGray, fontSize = 10.sp)
                                        }
                                        TextButton(onClick = { scope.launch { SoundPackInstaller.install(context, pack.id) } },
                                            enabled = !installed && packState !is SoundPackInstallState.Downloading) {
                                            Text(if (installed) "Siap" else "Unduh")
                                        }
                                    }
                                }
                            }
                        }
                        Text("Jumlah aktual mengikuti file audio yang didukung dan berhasil diekstrak.", color = Color.LightGray, fontSize = 10.sp)
                    }
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("Semua", "Paket CC0", "Impor").forEach { filter ->
                FilterChip(
                    selected = sourceFilter == filter,
                    onClick = { sourceFilter = filter },
                    label = { Text(filter, fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0x553A9CB8),
                        selectedLabelColor = Color.White,
                        labelColor = Color.LightGray
                    )
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            listOf("Semua", "Lucu", "Meme", "Game", "UI", "Alam", "Lainnya").forEach { category ->
                FilterChip(selected = soundCategoryFilter == category, onClick = { soundCategoryFilter = category }, label = { Text(category, fontSize = 10.sp) })
            }
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
        )
        Button(onClick = { picker.launch(arrayOf("audio/*")) }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF285F83), contentColor = Color.White)) {
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
                    Icon(Icons.Default.LibraryMusic, contentDescription = null, tint = Color(0xFF53DDF2))
                    Text("Pustaka masih kosong", color = Color.White, fontWeight = FontWeight.SemiBold)
                    Text("Belum ada file audio. Impor MP3/WAV atau gunakan tes speaker untuk memeriksa keluaran suara.", color = Color.LightGray, fontSize = 11.sp)
                }
            }
        } else {
            val visibleSounds = sounds.filter { sound ->
                val matchesSearch = sound.title.contains(searchQuery, ignoreCase = true)
                val isPackSound = isPackSoundUri(sound.uri)
                val matchesSource = when (sourceFilter) {
                    "Paket CC0" -> isPackSound
                    "Impor" -> !isPackSound
                    else -> true
                }
                matchesSearch && matchesSource && (soundCategoryFilter == "Semua" || soundCategory(sound.title) == soundCategoryFilter)
            }
            if (visibleSounds.isEmpty()) {
                Text("Tidak ada suara yang cocok dengan pencarian atau filter ini.", color = Color.LightGray, fontSize = 12.sp)
            }
            LazyColumn(modifier = Modifier.weight(1f, fill = true), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(visibleSounds, key = { it.uri }) { sound ->
                    Surface(color = Color(0xAA1A2940), shape = RoundedCornerShape(16.dp), border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x3347CDE8)), modifier = Modifier.fillMaxWidth()) {
                        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.LibraryMusic, contentDescription = "Audio", tint = Color(0xFF66DDF1))
                            Text(sound.title, color = Color.White, modifier = Modifier.weight(1f), fontSize = 12.sp, maxLines = 2)
                            IconButton(
                                onClick = { playSound(sound) },
                                enabled = activeUri != sound.uri && !isPreparing,
                                modifier = Modifier.semantics { contentDescription = "Putar ${sound.title}" }
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color(0xFF72DDEB))
                            }
                            IconButton(
                                onClick = {
                                    if (activeUri == sound.uri) stopPlayback()
                                    scope.launch {
                                        runCatching {
                                            SoundboardStore.save(context, sounds.filterNot { it.uri == sound.uri })
                                            SoundboardFileImport.delete(context, Uri.parse(sound.uri))
                                        }.onSuccess {
                                            message = "Suara dihapus dari pustaka."
                                        }.onFailure { error ->
                                            message = "Suara belum dapat dihapus: ${error.message ?: "kesalahan penyimpanan"}"
                                        }
                                    }
                                },
                                modifier = Modifier.semantics { contentDescription = "Hapus ${sound.title} dari pustaka" }
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = null, tint = Color(0xFFFF8D9B))
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


private fun packCategory(pack: SoundPack): String {
    val id = pack.id.lowercase()
    return when {
        id.contains("weird") || id.contains("laugh") || id.contains("zombie") -> "Lucu"
        id.contains("retro") || id.contains("rpg") || id.contains("battle") || id.contains("arcade") -> "Game"
        id.contains("voice") -> "Voice"
        id.contains("ui") || id.contains("interface") || id.contains("digital") -> "UI"
        id.contains("sci-fi") || id.contains("impact") || id.contains("casino") || id.contains("music") -> "Film"
        else -> "Semua"
    }
}

private fun packEmoji(pack: SoundPack): String = when (packCategory(pack)) {
    "Lucu" -> "😂"
    "Game" -> "🎮"
    "Voice" -> "🎙"
    "UI" -> "✨"
    "Film" -> "🎬"
    else -> "🎧"
}

private fun soundCategory(title: String): String {
    val value = title.lowercase()
    return when {
        listOf("laugh", "funny", "boing", "silly", "bloop", "blung", "plup", "uah", "kiss", "giggle").any { keyword -> value.contains(keyword) } -> "Lucu"
        listOf("meme", "fail", "wow", "bruh", "troll").any(value::contains) -> "Meme"
        listOf("game", "coin", "level", "retro", "arcade", "rpg", "battle").any(value::contains) -> "Game"
        listOf("ui", "button", "click", "interface", "menu").any(value::contains) -> "UI"
        listOf("rain", "wind", "water", "footstep", "bird", "animal", "thunder").any(value::contains) -> "Alam"
        else -> "Lainnya"
    }
}
