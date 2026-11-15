package id.livecreator.studio

import android.os.Bundle
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

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
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(horizontal = 18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                contentPadding = PaddingValues(top = 28.dp, bottom = 28.dp)
            ) {
                item {
                    Text("LIVE CREATOR", color = Cyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text(selected, color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                    Text("Creator control center", color = Color.LightGray)
                }
                item { StatusCard() }
                item {
                    Text("Quick tools", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        QuickCard("Text to Speech", "Voice queue", Icons.Default.RecordVoiceOver, Modifier.weight(1f))
                        QuickCard("Sound alerts", "Event sounds", Icons.Default.Notifications, Modifier.weight(1f))
                    }
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        QuickCard("AI assistant", "Response tools", Icons.Default.AutoAwesome, Modifier.weight(1f))
                        QuickCard("Live events", "Connection status", Icons.Default.Sensors, Modifier.weight(1f))
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
    }
}

@Composable
private fun StatusCard() {
    Surface(color = Panel, shape = RoundedCornerShape(22.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(9.dp).background(Cyan, RoundedCornerShape(50)))
                Spacer(Modifier.width(8.dp))
                Text("SYSTEM READY", color = Cyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            Text("Your live workspace", color = Color.White, fontSize = 21.sp, fontWeight = FontWeight.Bold)
            Text("Configure your audio, alerts and integrations from one place.",
                color = Color.LightGray, fontSize = 13.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AssistChip(onClick = {}, label = { Text("Audio setup") })
                AssistChip(onClick = {}, label = { Text("Connect provider") })
            }
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
