package id.livecreator.studio

import android.os.Bundle
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale

private val Ink = Color(0xFF0B1020)
private val Panel = Color(0xFF171F35)
private val Violet = Color(0xFF9B7BFF)
private val Cyan = Color(0xFF54D8E8)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { LiveCreatorApp() }
    }
}

@Composable
fun LiveCreatorApp() {
    var selected by remember { mutableStateOf("Dashboard") }
    MaterialTheme(colorScheme = darkColorScheme(
        background = Ink, surface = Panel, primary = Violet, secondary = Cyan
    )) {
        Row(Modifier.fillMaxSize().background(Ink)) {
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
                            }, contentDescription = item)
                        },
                        label = { Text(item, fontSize = 9.sp) }
                    )
                }
            }
            when (selected) {
                "TTS" -> TtsScreen(Modifier.fillMaxSize())
                "Alerts" -> InfoScreen("Sound alerts", "Local sound library and event routing are planned. LIVE event triggers are not connected yet.")
                "AI" -> InfoScreen("AI assistant", "AI providers are not configured. No messages are sent to an AI service in this build.")
                "Settings" -> InfoScreen("Settings", "Provider connections and persistent settings will be added in a later milestone.")
                else -> DashboardScreen(Modifier.fillMaxSize())
            }
        }
    }
}

@Composable
private fun DashboardScreen(modifier: Modifier = Modifier) {
    LazyColumn(
        modifier = modifier.padding(horizontal = 18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(top = 28.dp, bottom = 28.dp)
    ) {
        item {
            Text("LIVE CREATOR", color = Cyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Text("Dashboard", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Text("Creator control center", color = Color.LightGray)
        }
        item { StatusCard() }
        item { Text("Quick tools", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                QuickCard("Text to Speech", "On-device voice preview", Icons.Default.RecordVoiceOver, Modifier.weight(1f))
                QuickCard("Sound alerts", "Local event sounds", Icons.Default.Notifications, Modifier.weight(1f))
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                QuickCard("AI assistant", "Provider setup pending", Icons.Default.AutoAwesome, Modifier.weight(1f))
                QuickCard("Live events", "Not connected", Icons.Default.Sensors, Modifier.weight(1f))
            }
        }
        item {
            Surface(color = Panel, shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Integration status", color = Color.White, fontWeight = FontWeight.Bold)
                    Text("TikTok LIVE connection is not configured.", color = Color.LightGray)
                    Text("No live events are simulated. Connect a supported provider before enabling event-driven features.",
                        color = Color(0xFFFFD18A), fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun TtsScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var ready by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("Halo! Selamat datang di LiveCreator Studio.") }
    var status by remember { mutableStateOf("Menyiapkan mesin suara perangkat…") }
    val tts = remember {
        TextToSpeech(context) { result ->
            ready = result == TextToSpeech.SUCCESS
            status = if (ready) {
                val languageResult = ttsLanguagePlaceholder()
                if (languageResult) "Mesin suara siap. Suara menggunakan layanan TTS Android."
                else "Mesin siap, tetapi bahasa Indonesia mungkin belum tersedia pada perangkat."
            } else "Mesin TTS gagal dimulai. Periksa layanan Text-to-Speech Android."
        }
    }
    DisposableEffect(tts) {
        onDispose {
            tts.stop()
            tts.shutdown()
        }
    }
    Column(
        modifier.padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("AUDIO ENGINE", color = Cyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Text("Text to Speech", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold)
        Text(status, color = Color.LightGray, fontSize = 13.sp)
        OutlinedTextField(
            value = message,
            onValueChange = { message = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Teks untuk dibacakan") },
            minLines = 3,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = Violet,
                unfocusedBorderColor = Color.Gray
            )
        )
        Button(
            onClick = {
                val language = tts.setLanguage(Locale("id", "ID"))
                if (language == TextToSpeech.LANG_MISSING_DATA || language == TextToSpeech.LANG_NOT_SUPPORTED) {
                    status = "Bahasa Indonesia tidak tersedia. Pasang data suara Indonesia di pengaturan TTS Android."
                } else {
                    tts.speak(message, TextToSpeech.QUEUE_FLUSH, null, "livecreator-preview")
                    status = "Membacakan teks menggunakan TTS perangkat."
                }
            },
            enabled = ready && message.isNotBlank(),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.PlayArrow, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Putar pratinjau suara")
        }
        OutlinedButton(
            onClick = { tts.stop(); status = "Pembacaan dihentikan." },
            enabled = ready,
            modifier = Modifier.fillMaxWidth()
        ) { Text("Hentikan suara") }
        Text(
            "Pratinjau ini memakai mesin TextToSpeech bawaan Android dan berjalan lokal. Ini belum terhubung ke komentar LIVE.",
            color = Color.LightGray, fontSize = 12.sp
        )
    }
}

private fun ttsLanguagePlaceholder(): Boolean = true

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
    Surface(color = Panel, shape = RoundedCornerShape(22.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(9.dp).background(Cyan, RoundedCornerShape(50)))
                Spacer(Modifier.width(8.dp))
                Text("APP STARTED", color = Cyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            Text("Your live workspace", color = Color.White, fontSize = 21.sp, fontWeight = FontWeight.Bold)
            Text("Configure audio, alerts and integrations from one place.",
                color = Color.LightGray, fontSize = 13.sp)
        }
    }
}

@Composable
private fun QuickCard(title: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier) {
    Surface(color = Panel, shape = RoundedCornerShape(18.dp), modifier = modifier) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(icon, contentDescription = null, tint = Violet, modifier = Modifier.size(24.dp))
            Text(title, color = Color.White, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = Color.LightGray, fontSize = 11.sp)
        }
    }
}
