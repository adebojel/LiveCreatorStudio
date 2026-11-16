package id.livecreator.studio

import android.Manifest
import android.app.PictureInPictureParams
import android.content.Intent
import android.content.BroadcastReceiver
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.app.ActivityManager
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale

private val Ink = Color(0xFF0B1020)
private val Panel = Color(0xCC171F35)
private val Violet = Color(0xFF53DDF2)
private val Cyan = Color(0xFF54D8E8)

class MainActivity : ComponentActivity() {
    private var pipMode by mutableStateOf(false)
    private var backgroundServiceRunning by mutableStateOf(false)
    private var serviceReceiverRegistered = false
    private val serviceStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: android.content.Context?, intent: Intent?) {
            if (intent?.action == LiveBackgroundService.ACTION_STATUS) {
                backgroundServiceRunning = intent.getBooleanExtra(LiveBackgroundService.EXTRA_RUNNING, false)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val serviceFilter = IntentFilter(LiveBackgroundService.ACTION_STATUS)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(serviceStateReceiver, serviceFilter, android.content.Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("DEPRECATION")
            registerReceiver(serviceStateReceiver, serviceFilter)
        }
        serviceReceiverRegistered = true
        backgroundServiceRunning = isBackgroundServiceRunning()
        restoreFloatingControlIfAllowed()
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
                onOpenAppSettings = { openAppSettings() },
                onOpenNotificationSettings = { openNotificationSettings() },
                onEnterPip = { enterCompactMode() },
                onSpeakInBackground = { text, rate, pitch -> speakInBackground(text, rate, pitch) },
                inPipMode = pipMode,
                backgroundServiceRunning = backgroundServiceRunning
            )
        }
    }

    @Suppress("DEPRECATION")
    private fun isBackgroundServiceRunning(): Boolean {
        return getSystemService(ActivityManager::class.java)
            .getRunningServices(Int.MAX_VALUE)
            .any { it.service.className == LiveBackgroundService::class.java.name }
    }

    override fun onResume() {
        super.onResume()
        // Reconcile UI state after notification shade actions or returning from Settings.
        backgroundServiceRunning = isBackgroundServiceRunning()
        restoreFloatingControlIfAllowed()
    }

    private fun restoreFloatingControlIfAllowed() {
        val enabled = getSharedPreferences("floating_control", MODE_PRIVATE).getBoolean("enabled", false)
        if (enabled && Settings.canDrawOverlays(this)) {
            startService(Intent(this, FloatingControlService::class.java).setAction(FloatingControlService.ACTION_SHOW))
        }
    }

    override fun onDestroy() {
        if (serviceReceiverRegistered) {
            runCatching { unregisterReceiver(serviceStateReceiver) }
            serviceReceiverRegistered = false
        }
        super.onDestroy()
    }

    private fun startBackgroundAudio() {
        val intent = Intent(this, LiveBackgroundService::class.java).setAction(LiveBackgroundService.ACTION_START)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(intent) else startService(intent)
    }

    private fun stopBackgroundAudio() {
        stopService(Intent(this, LiveBackgroundService::class.java))
        backgroundServiceRunning = false
    }

    private fun speakInBackground(text: String, rate: Float, pitch: Float) {
        val intent = Intent(this, LiveBackgroundService::class.java)
            .setAction(LiveBackgroundService.ACTION_SPEAK)
            .putExtra(LiveBackgroundService.EXTRA_TEXT, text)
            .putExtra(LiveBackgroundService.EXTRA_RATE, rate)
            .putExtra(LiveBackgroundService.EXTRA_PITCH, pitch)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(intent) else startService(intent)
    }

    private fun openBatterySettings() {
        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
        } else Intent(Settings.ACTION_SETTINGS)
        runCatching { startActivity(intent) }
    }

    private fun openAppSettings() {
        runCatching {
            startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).setData(Uri.parse("package:$packageName")))
        }
    }

    private fun openNotificationSettings() {
        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
        } else {
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).setData(Uri.parse("package:$packageName"))
        }
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
    onOpenAppSettings: () -> Unit,
    onOpenNotificationSettings: () -> Unit,
    onEnterPip: () -> Unit,
    onSpeakInBackground: (String, Float, Float) -> Unit,
    inPipMode: Boolean,
    backgroundServiceRunning: Boolean
) {
    val context = LocalContext.current
    var selected by remember { mutableStateOf("Dashboard") }
    val settings by context.liveCreatorSettings.data.collectAsState(initial = emptyPreferences())
    val settingsScope = rememberCoroutineScope()
    val autoStartOnLaunch = settings[AppSettingKeys.AUTO_START_ON_LAUNCH] ?: false
    val resumeReminder = settings[AppSettingKeys.RESUME_REMINDER_ENABLED] ?: true
    LaunchedEffect(autoStartOnLaunch, backgroundServiceRunning) {
        if (autoStartOnLaunch && !backgroundServiceRunning) {
            onStartBackground()
        }
    }

    MaterialTheme(colorScheme = darkColorScheme(
        background = Ink, surface = Panel, primary = Cyan, secondary = Color(0xFFB7F7FF)
    )) {
        if (inPipMode) {
            PipMiniScreen(backgroundServiceRunning)
        } else Row(
            Modifier.fillMaxSize()
                .background(Brush.verticalGradient(listOf(Color(0xFF0A1020), Color(0xFF101A31), Color(0xFF111329))))
                .windowInsetsPadding(WindowInsets.safeDrawing)
        ) {
            NavigationRail(containerColor = Color(0x99131D33)) {
                listOf("Dashboard", "TTS", "Soundboard", "Settings").forEach { item ->
                    NavigationRailItem(
                        selected = selected == item,
                        onClick = { selected = item },
                        icon = {
                            Icon(when(item) {
                                "Dashboard" -> Icons.Default.Dashboard
                                "TTS" -> Icons.Default.RecordVoiceOver
                                "Soundboard" -> Icons.Default.LibraryMusic
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
                "Soundboard" -> SoundboardScreen(Modifier.fillMaxSize())
                "Settings" -> SettingsScreen(
                    autoStartOnLaunch = autoStartOnLaunch,
                    onAutoStartChange = { enabled ->
                        settingsScope.launch {
                            context.liveCreatorSettings.edit { preferences ->
                                preferences[AppSettingKeys.AUTO_START_ON_LAUNCH] = enabled
                            }
                        }
                    },
                    resumeReminder = resumeReminder,
                    onResumeReminderChange = { enabled ->
                        settingsScope.launch {
                            context.liveCreatorSettings.edit { preferences ->
                                preferences[AppSettingKeys.RESUME_REMINDER_ENABLED] = enabled
                            }
                        }
                    },
                    onOpenBatterySettings = onOpenBatterySettings,
                        onOpenAppSettings = onOpenAppSettings,
                    onOpenNotificationSettings = onOpenNotificationSettings
                )
                else -> DashboardScreen(
                    modifier = Modifier.fillMaxSize(),
                    backgroundEnabled = backgroundServiceRunning,
                    onStartBackground = onStartBackground,
                    onStopBackground = onStopBackground,
                    onEnterPip = onEnterPip,
                    onNavigate = { selected = it }
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
    onEnterPip: () -> Unit,
    onNavigate: (String) -> Unit
) {
    LazyColumn(
        modifier = modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 28.dp)
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
                        Button(
                            onClick = onStartBackground,
                            enabled = !backgroundEnabled,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null)
                            Spacer(Modifier.width(5.dp))
                            Text(if (backgroundEnabled) "Aktif" else "Mulai")
                        }
                        OutlinedButton(
                            onClick = onStopBackground,
                            enabled = backgroundEnabled,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Stop, contentDescription = null)
                            Spacer(Modifier.width(5.dp))
                            Text("Matikan")
                        }
                    }
                    Text("Status ini menunjukkan perintah layanan dikirim dari aplikasi; bukan jaminan sistem tidak akan menghentikannya.",
                        color = Color(0xFFFFD18A), fontSize = 10.sp)
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FeatureCard("Pengaturan Kreator", "Notifikasi, baterai, dan layanan audio", Icons.Default.Tune, Modifier.weight(1f)) { onNavigate("Settings") }
            }
        }
        item {
            Text("Quick tools", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FeatureCard("Text to Speech", "Pratinjau suara Indonesia", Icons.Default.RecordVoiceOver, Modifier.weight(1f)) { onNavigate("TTS") }
                FeatureCard("Soundboard", "Pustaka audio perangkat", Icons.Default.LibraryMusic, Modifier.weight(1f)) { onNavigate("Soundboard") }
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
private fun TtsScreen(modifier: Modifier, onSpeakInBackground: (String, Float, Float) -> Unit) {
    val context = LocalContext.current
    var ready by remember { mutableStateOf(false) }
    val appSettings by context.liveCreatorSettings.data.collectAsState(initial = emptyPreferences())
    val settingsScope = rememberCoroutineScope()
    var message by remember { mutableStateOf("") }
    var speechRate by remember { mutableFloatStateOf(1.0f) }
    var speechPitch by remember { mutableFloatStateOf(1.0f) }
    LaunchedEffect(appSettings) {
        speechRate = sanitizeSpeechRate(appSettings[AppSettingKeys.SPEECH_RATE] ?: 1.0f)
        speechPitch = sanitizeSpeechPitch(appSettings[AppSettingKeys.SPEECH_PITCH] ?: 1.0f)
    }
    var status by remember { mutableStateOf("Menyiapkan mesin suara perangkat…") }
    val tts = remember {
        TextToSpeech(context) { result ->
            ready = result == TextToSpeech.SUCCESS
            status = if (result == TextToSpeech.SUCCESS) {
                "Mesin suara siap. Bahasa Indonesia diperiksa saat pemutaran."
            } else {
                "Mesin TTS gagal dimulai."
            }
        }
    }
    LaunchedEffect(ready, tts) {
        if (ready) {
            tts.setOnUtteranceProgressListener(object : android.speech.tts.UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    status = "Sedang membacakan pratinjau."
                }
                override fun onDone(utteranceId: String?) {
                    status = "Pratinjau selesai."
                }
                override fun onError(utteranceId: String?) {
                    status = "Pratinjau gagal diputar oleh mesin TTS."
                }
            })
        }
    }
    DisposableEffect(tts) {
        onDispose {
            tts.stop()
            tts.shutdown()
        }
    }
    Column(
        modifier.verticalScroll(rememberScrollState()).padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("AUDIO ENGINE", color = Cyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Text("Text to Speech", color = Color.White, fontSize = 25.sp, fontWeight = FontWeight.Bold)
        Text(status, color = Color.LightGray, fontSize = 12.sp)
        OutlinedTextField(
            value = message, onValueChange = { message = it.take(4000) },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Teks untuk dibacakan (maks. 4.000 karakter)") }, minLines = 3
        )
        Text("Kecepatan bicara: ${"%.2f".format(speechRate)}×", color = Color.White, fontSize = 12.sp)
        Slider(
            value = speechRate,
            onValueChange = {
                speechRate = it
                settingsScope.launch {
                    context.liveCreatorSettings.edit { preferences ->
                        preferences[AppSettingKeys.SPEECH_RATE] = sanitizeSpeechRate(it)
                    }
                }
            },
            valueRange = 0.5f..2.0f
        )
        Text("Nada suara: ${"%.2f".format(speechPitch)}×", color = Color.White, fontSize = 12.sp)
        Slider(
            value = speechPitch,
            onValueChange = {
                speechPitch = it
                settingsScope.launch {
                    context.liveCreatorSettings.edit { preferences ->
                        preferences[AppSettingKeys.SPEECH_PITCH] = sanitizeSpeechPitch(it)
                    }
                }
            },
            valueRange = 0.5f..2.0f
        )
        OutlinedButton(
            onClick = {
                speechRate = 1.0f
                speechPitch = 1.0f
                settingsScope.launch {
                    context.liveCreatorSettings.edit { preferences ->
                        preferences[AppSettingKeys.SPEECH_RATE] = 1.0f
                        preferences[AppSettingKeys.SPEECH_PITCH] = 1.0f
                    }
                }
                status = "Pengaturan suara dikembalikan ke normal."
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Reset ke normal") }
        Button(
            onClick = {
                val language = tts.setLanguage(Locale("id", "ID"))
                if (language == TextToSpeech.LANG_MISSING_DATA || language == TextToSpeech.LANG_NOT_SUPPORTED) {
                    status = "Bahasa Indonesia belum tersedia pada mesin TTS perangkat."
                } else {
                    tts.setSpeechRate(speechRate)
                    tts.setPitch(speechPitch)
                    val utteranceId = "livecreator-preview-${System.currentTimeMillis()}"
                    val result = tts.speak(message.trim(), TextToSpeech.QUEUE_FLUSH, null, utteranceId)
                    if (result == TextToSpeech.ERROR) {
                        status = "Mesin TTS menolak permintaan. Periksa mesin suara perangkat."
                    }
                }
            },
            enabled = ready && message.isNotBlank(),
            modifier = Modifier.fillMaxWidth()
        ) { Icon(Icons.Default.PlayArrow, contentDescription = null); Spacer(Modifier.width(6.dp)); Text("Pratinjau") }
        OutlinedButton(
            onClick = {
                tts.stop()
                status = "Pratinjau dihentikan."
            },
            enabled = ready,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Stop, contentDescription = null)
            Spacer(Modifier.width(6.dp))
            Text("Hentikan pratinjau")
        }
        OutlinedButton(
            onClick = {
                onSpeakInBackground(message, speechRate, speechPitch)
                status = "Permintaan suara latar belakang dikirim dengan kecepatan dan nada pilihan."
            },
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
    onOpenBatterySettings: () -> Unit,
    onOpenAppSettings: () -> Unit,
    onOpenNotificationSettings: () -> Unit
) {
    val context = LocalContext.current
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("PREFERENCES", color = Cyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Text("Pengaturan", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold)
        Surface(color = Panel, shape = RoundedCornerShape(18.dp)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Kontrol mengambang", color = Color.White, fontWeight = FontWeight.Bold)
                val overlayPrefs = context.getSharedPreferences("floating_control", android.content.Context.MODE_PRIVATE)
                var overlayEnabled by remember { mutableStateOf(overlayPrefs.getBoolean("enabled", false)) }
                var bubbleSize by remember { mutableFloatStateOf(sanitizeFloatingBubbleSize(overlayPrefs.getFloat("size", 42f))) }
                var bubbleOpacity by remember { mutableFloatStateOf(sanitizeFloatingBubbleOpacity(overlayPrefs.getFloat("opacity", 0.82f))) }
                val overlayAllowed = Settings.canDrawOverlays(context)
                Text("Bola kontrol glass RGB berukuran ringkas. Geser ke kiri atau kanan untuk menempel otomatis di tepi layar; tarik kembali untuk memunculkannya.", color = Color.LightGray, fontSize = 11.sp)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Aktifkan Floating Control", color = Color.White)
                        Text(if (overlayAllowed) "Izin overlay tersedia" else "Izin overlay belum diberikan", color = if (overlayAllowed) Cyan else Color(0xFFFFD18A), fontSize = 11.sp)
                    }
                    Switch(checked = overlayEnabled, onCheckedChange = { enabled ->
                        if (enabled && !Settings.canDrawOverlays(context)) {
                            overlayEnabled = true
                            overlayPrefs.edit().putBoolean("enabled", true).apply()
                            runCatching {
                                (context as? android.app.Activity)?.startActivity(
                                    Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}"))
                                )
                            }
                        } else {
                            overlayEnabled = enabled
                            overlayPrefs.edit().putBoolean("enabled", enabled).apply()
                            val serviceIntent = Intent(context, FloatingControlService::class.java)
                                .setAction(if (enabled) FloatingControlService.ACTION_SHOW else FloatingControlService.ACTION_STOP)
                            if (enabled) context.startService(serviceIntent) else context.stopService(serviceIntent)
                        }
                    })
                }
                Text("Ukuran bola: ${bubbleSize.toInt()} dp", color = Color.White, fontSize = 12.sp)
                Slider(value = bubbleSize, onValueChange = { value ->
                    bubbleSize = sanitizeFloatingBubbleSize(value)
                    overlayPrefs.edit().putFloat("size", bubbleSize).apply()
                    context.startService(Intent(context, FloatingControlService::class.java).setAction(FloatingControlService.ACTION_REFRESH))
                }, valueRange = 36f..64f)
                Text("Transparansi: ${(bubbleOpacity * 100).toInt()}%", color = Color.White, fontSize = 12.sp)
                Slider(value = bubbleOpacity, onValueChange = { value ->
                    bubbleOpacity = sanitizeFloatingBubbleOpacity(value)
                    overlayPrefs.edit().putFloat("opacity", bubbleOpacity).apply()
                    context.startService(Intent(context, FloatingControlService::class.java).setAction(FloatingControlService.ACTION_REFRESH))
                }, valueRange = 0.25f..1f)
                OutlinedButton(onClick = {
                    overlayPrefs.edit().putBoolean("enabled", true).apply()
                    overlayEnabled = true
                    if (Settings.canDrawOverlays(context)) {
                        context.startService(Intent(context, FloatingControlService::class.java).setAction(FloatingControlService.ACTION_SHOW))
                    } else {
                        runCatching {
                            (context as? android.app.Activity)?.startActivity(
                                Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}"))
                            )
                        }
                    }
                }, modifier = Modifier.fillMaxWidth()) { Text("Tampilkan / munculkan kembali bola") }
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
                OutlinedButton(onClick = onOpenNotificationSettings, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.NotificationsActive, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Atur izin dan notifikasi aplikasi")
                }
                OutlinedButton(onClick = onOpenAppSettings, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Settings, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Buka info aplikasi dan semua izin")
                }
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
            Icon(icon, contentDescription = null, tint = Cyan, modifier = Modifier.size(23.dp))
            Text(title, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            Text(subtitle, color = Color.LightGray, fontSize = 10.sp)
        }
    }
}

