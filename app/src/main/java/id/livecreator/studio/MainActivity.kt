package id.livecreator.studio

import android.Manifest
import android.app.Activity
import android.app.PictureInPictureParams
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.content.res.Configuration
import android.provider.Settings
import android.util.Rational
import android.speech.tts.TextToSpeech
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import java.util.Locale

private val Ink = Color(0xFF0B1020)
private val Panel = Color(0xCC171F35)
private val Violet = Color(0xFF9B7BFF)
private val Cyan = Color(0xFF54D8E8)

class MainActivity : ComponentActivity() {
    private var pipMode by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 4101)
        }
        setContent {
            LiveCreatorApp(
                onStartBackground = { startBackgroundAudio() },
                onStopBackground = { stopBackgroundAudio() },
                onOpenBatterySettings = { openBatterySettings() },
                onEnterPip = { enterCompactMode() },
                onSpeakInBackground = { speakInBackground(it) },
                inPipMode = pipMode
            )
        }
    }

    private fun startBackgroundAudio() {
        val intent = Intent(this, LiveBackgroundService::class.java).setAction(LiveBackgroundService.ACTION_START)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(intent) else startService(intent)
    }

    private fun stopBackgroundAudio() {
        startService(Intent(this, LiveBackgroundService::class.java).setAction(LiveBackgroundService.ACTION_STOP))
    }

    private fun speakInBackground(text: String) {
        val intent = Intent(this, LiveBackgroundService::class.java)
            .setAction(LiveBackgroundService.ACTION_SPEAK)
            .putExtra(LiveBackgroundService.EXTRA_TEXT, text)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(intent) else startService(intent)
    }

    private fun openBatterySettings() {
        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
        } else Intent(Settings.ACTION_SETTINGS)
        runCatching { startActivity(intent) }
    }

    private fun enterCompactMode() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val params = PictureInPictureParams.Builder()
                .setAspectRatio(Rational(16, 9))
                .build()
            enterPictureInPictureMode(params)
        }
    }

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean, newConfig: Configuration) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        pipMode = isInPictureInPictureMode
    }

}

@Composable
fun LiveCreatorApp(
    onStartBackground: () -> Unit,
    onStopBackground: () -> Unit,
    onOpenBatterySettings: () -> Unit,
    onEnterPip: () -> Unit,
    onSpeakInBackground: (String) -> Unit,
    inPipMode: Boolean
) {
    val context = LocalContext.current
    var selected by remember { mutableStateOf("Dashboard") }
    var backgroundEnabled by remember { mutableStateOf(false) }
    var autoStartOnLaunch by remember {
        mutableStateOf(context.getSharedPreferences("livecreator_prefs", Activity.MODE_PRIVATE)
            .getBoolean("auto_start_on_launch", false))
    }
    var resumeReminder by remember {
        mutableStateOf(context.getSharedPreferences("livecreator_prefs", Activity.MODE_PRIVATE)
            .getBoolean("resume_reminder_enabled", true))
    }
    LaunchedEffect(Unit) {
        if (autoStartOnLaunch) {
            onStartBackground()
            backgroundEnabled = true
        }
    }

    MaterialTheme(colorScheme = darkColorScheme(
        background = Ink, surface = Panel, primary = Violet, secondary = Cyan
    )) {
        if (inPipMode) {
            PipMiniScreen(backgroundEnabled)
        } else Row(Modifier.fillMaxSize().background(Ink)) {
            NavigationRail(containerColor = Panel) {
                listOf("Dashboard", "TTS", "Alerts", "AI", "Settings").forEach { item ->
                    NavigationRailItem(
                        selected = selected == item,
                        onClick = { selected = item },
                        icon = {
                            Icon(when(item) {
                                "Dashboard" -> Icons.Default.Dashboard
                                "TTS" -> Icons.Default.RecordVoiceOver
                                "Alerts" -> Icons.Default.NotificationsActive
                                "AI" -> Icons.Default.AutoAwesome
                                else -> Icons.Default.Settings
                            }, contentDescription = item,
                                modifier = Modifier.semantics { contentDescription = "Buka menu $item" })
                        },
                        label = { Text(item, fontSize = 9.sp) }
                    )
                }
            }
            when (selected) {
                "TTS" -> TtsScreen(Modifier.fillMaxSize(), onSpeakInBackground)
                "Alerts" -> InfoScreen("Sound alerts", "Pustaka suara lokal dan perutean event akan dikembangkan berikutnya. Pemicu LIVE belum terhubung.")
                "AI" -> InfoScreen("AI assistant", "Provider AI belum dikonfigurasi. Build ini belum mengirim pesan ke layanan AI.")
                "Settings" -> SettingsScreen(
                    autoStartOnLaunch = autoStartOnLaunch,
                    onAutoStartChange = {
                        autoStartOnLaunch = it
                        context.getSharedPreferences("livecreator_prefs", Activity.MODE_PRIVATE)
                            .edit().putBoolean("auto_start_on_launch", it).apply()
                    },
                    resumeReminder = resumeReminder,
                    onResumeReminderChange = {
                        resumeReminder = it
                        context.getSharedPreferences("livecreator_prefs", Activity.MODE_PRIVATE)
                            .edit().putBoolean("resume_reminder_enabled", it).apply()
                    },
                    onOpenBatterySettings = onOpenBatterySettings
                )
                else -> DashboardScreen(
                    modifier = Modifier.fillMaxSize(),
                    backgroundEnabled = backgroundEnabled,
                    onStartBackground = {
                        onStartBackground()
                        backgroundEnabled = true
                    },
                    onStopBackground = {
                        onStopBackground()
                        backgroundEnabled = false
                    },
                    onEnterPip = onEnterPip
                )
            }
        }
    }
}

@Composable
private fun PipMiniScreen(backgroundEnabled: Boolean) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Ink,
        shape = RoundedCornerShape(18.dp)
    ) {
        Row(
            Modifier.fillMaxSize().padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text("LIVE CREATOR", color = Cyan, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                Text("Audio assistant", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Text(if (backgroundEnabled) "Background aktif" else "Siap digunakan",
                    color = Color.LightGray, fontSize = 10.sp)
            }
            Icon(Icons.Default.GraphicEq, contentDescription = "Status audio", tint = Cyan,
                modifier = Modifier.size(28.dp))
        }
    }
}

@Composable
private fun DashboardScreen(
    modifier: Modifier,
    backgroundEnabled: Boolean,
    onStartBackground: () -> Unit,
    onStopBackground: () -> Unit,
    onEnterPip: () -> Unit
) {
    LazyColumn(
        modifier = modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(top = 22.dp, bottom = 28.dp)
    ) {
        item {
            Text("LIVE CREATOR STUDIO", color = Cyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Text("Creator hub", color = Color.White, fontSize = 27.sp, fontWeight = FontWeight.Bold)
            Text("Kontrol audio, layanan latar belakang, dan alat kreator.", color = Color.LightGray, fontSize = 12.sp)
        }
        item {
            Surface(color = Panel, shape = RoundedCornerShape(22.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.GraphicEq, contentDescription = null, tint = Cyan)
                        Spacer(Modifier.width(8.dp))
                        Text("Background audio engine", color = Color.White, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.weight(1f))
                        Text(if (backgroundEnabled) "AKTIF" else "NONAKTIF",
                            color = if (backgroundEnabled) Cyan else Color.LightGray, fontSize = 10.sp)
                    }
                    Text("Layanan foreground dengan notifikasi persisten untuk pekerjaan audio yang dimulai pengguna.",
                        color = Color.LightGray, fontSize = 12.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = onStartBackground, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null)
                            Text("Mulai")
                        }
                        OutlinedButton(onClick = onStopBackground, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.Stop, contentDescription = null)
                            Text("Hentikan")
                        }
                    }
                    Text("Status ini menunjukkan perintah layanan dikirim dari aplikasi; bukan jaminan sistem tidak akan menghentikannya.",
                        color = Color(0xFFFFD18A), fontSize = 10.sp)
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FeatureCard("Picture-in-Picture", "Panel mini saat pindah aplikasi", Icons.Default.PictureInPicture, Modifier.weight(1f), onEnterPip)
                FeatureCard("Aksesibilitas", "Kompatibel dengan TalkBack", Icons.Default.AccessibilityNew, Modifier.weight(1f)) {}
            }
        }
        item { StatusCard() }
        item {
            Text("Quick tools", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FeatureCard("Text to Speech", "Pratinjau suara Indonesia", Icons.Default.RecordVoiceOver, Modifier.weight(1f)) {}
                FeatureCard("Sound alerts", "Pustaka suara lokal", Icons.Default.Notifications, Modifier.weight(1f)) {}
            }
        }
        item {
            Surface(color = Panel, shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("LIVE integration", color = Color.White, fontWeight = FontWeight.Bold)
                    Text("TikTok LIVE belum terhubung. Tidak ada event palsu atau simulasi koneksi.",
                        color = Color.LightGray, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun TtsScreen(modifier: Modifier, onSpeakInBackground: (String) -> Unit) {
    val context = LocalContext.current
    var ready by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("Halo! Selamat datang di LiveCreator Studio.") }
    var status by remember { mutableStateOf("Menyiapkan mesin suara perangkat…") }
    val tts = remember {
        TextToSpeech(context) { result ->
            ready = result == TextToSpeech.SUCCESS
            status = if (ready) "Mesin suara siap. Bahasa Indonesia diperiksa saat pemutaran." else "Mesin TTS gagal dimulai."
        }
    }
    DisposableEffect(tts) {
        onDispose { tts.stop(); tts.shutdown() }
    }
    Column(modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("AUDIO ENGINE", color = Cyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Text("Text to Speech", color = Color.White, fontSize = 25.sp, fontWeight = FontWeight.Bold)
        Text(status, color = Color.LightGray, fontSize = 12.sp)
        OutlinedTextField(
            value = message, onValueChange = { message = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Teks untuk dibacakan") }, minLines = 3
        )
        Button(
            onClick = {
                val language = tts.setLanguage(Locale("id", "ID"))
                if (language == TextToSpeech.LANG_MISSING_DATA || language == TextToSpeech.LANG_NOT_SUPPORTED) {
                    status = "Bahasa Indonesia belum tersedia pada mesin TTS perangkat."
                } else {
                    tts.speak(message, TextToSpeech.QUEUE_FLUSH, null, "livecreator-preview")
                    status = "Pratinjau suara berjalan di aplikasi."
                }
            },
            enabled = ready && message.isNotBlank(),
            modifier = Modifier.fillMaxWidth()
        ) { Icon(Icons.Default.PlayArrow, contentDescription = null); Spacer(Modifier.width(6.dp)); Text("Pratinjau") }
        OutlinedButton(
            onClick = { onSpeakInBackground(message); status = "Permintaan suara latar belakang dikirim ke layanan." },
            enabled = message.isNotBlank(),
            modifier = Modifier.fillMaxWidth()
        ) { Icon(Icons.Default.Headphones, contentDescription = null); Spacer(Modifier.width(6.dp)); Text("Bacakan di latar belakang") }
        Text("Suara latar belakang memakai layanan Android yang dimulai pengguna. Komentar LIVE belum menjadi sumber teks otomatis.",
            color = Color.LightGray, fontSize = 11.sp)
    }
}

@Composable
private fun SettingsScreen(
    autoStartOnLaunch: Boolean,
    onAutoStartChange: (Boolean) -> Unit,
    resumeReminder: Boolean,
    onResumeReminderChange: (Boolean) -> Unit,
    onOpenBatterySettings: () -> Unit
) {
    Column(Modifier.fillMaxSize().padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("PREFERENCES", color = Cyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Text("Pengaturan", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold)
        Surface(color = Panel, shape = RoundedCornerShape(18.dp)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Ketahanan layanan", color = Color.White, fontWeight = FontWeight.Bold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Aktif otomatis saat aplikasi dibuka", color = Color.White)
                        Text("Jika diaktifkan, layanan audio dimulai ketika kamu membuka aplikasi.", color = Color.LightGray, fontSize = 11.sp)
                    }
                    Switch(checked = autoStartOnLaunch, onCheckedChange = onAutoStartChange)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Pengingat setelah restart", color = Color.White)
                        Text("Android dapat membatasi auto-start. Aplikasi memberi notifikasi untuk mengaktifkan ulang layanan.",
                            color = Color.LightGray, fontSize = 11.sp)
                    }
                    Switch(checked = resumeReminder, onCheckedChange = onResumeReminderChange)
                }
                OutlinedButton(onClick = onOpenBatterySettings, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.BatteryChargingFull, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Buka pengaturan optimasi baterai")
                }
                Text("Pengecualian baterai harus dipilih sendiri oleh pengguna. Produsen perangkat dapat menerapkan pembatasan tambahan.",
                    color = Color(0xFFFFD18A), fontSize = 11.sp)
            }
        }
        Surface(color = Panel, shape = RoundedCornerShape(18.dp)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Aksesibilitas", color = Color.White, fontWeight = FontWeight.Bold)
                Text("Tombol dan kontrol menggunakan semantik Compose agar dapat dibaca TalkBack. Aktifkan TalkBack melalui pengaturan Aksesibilitas Android.",
                    color = Color.LightGray, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun FeatureCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        color = Panel,
        shape = RoundedCornerShape(18.dp),
        modifier = modifier.semantics { contentDescription = "$title. $subtitle" }
    ) {
        Column(Modifier.padding(13.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Icon(icon, contentDescription = null, tint = Violet, modifier = Modifier.size(23.dp))
            Text(title, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            Text(subtitle, color = Color.LightGray, fontSize = 10.sp)
        }
    }
}

@Composable
private fun InfoScreen(title: String, description: String) {
    Column(Modifier.fillMaxSize().padding(22.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("LIVE CREATOR", color = Cyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Text(title, color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold)
        Surface(color = Panel, shape = RoundedCornerShape(18.dp)) {
            Text(description, color = Color.LightGray, modifier = Modifier.padding(18.dp))
        }
    }
}

@Composable
private fun StatusCard() {
    Surface(color = Panel, shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(8.dp).background(Cyan, RoundedCornerShape(50)))
                Spacer(Modifier.width(8.dp))
                Text("APP READY", color = Cyan, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
            Text("Creator workspace", color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.Bold)
            Text("Satu tempat untuk mengelola audio dan fitur kreator.", color = Color.LightGray, fontSize = 12.sp)
        }
    }
}
