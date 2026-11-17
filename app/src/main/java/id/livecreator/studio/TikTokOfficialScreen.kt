package id.livecreator.studio

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun TikTokOfficialScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    Column(
        modifier = modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("TikTok • Integrasi Resmi", style = MaterialTheme.typography.headlineSmall,
            color = Color.White, fontWeight = FontWeight.Bold)
        Text("Pusat status izin dan kesiapan integrasi. Aplikasi tidak menggunakan konektor komunitas atau endpoint tidak resmi.",
            color = Color(0xFFB8C8DD), style = MaterialTheme.typography.bodyMedium)

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xCC17352F)),
            shape = RoundedCornerShape(18.dp)
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    androidx.compose.material3.Icon(Icons.Default.Security, contentDescription = null,
                        tint = Color(0xFF65E6B1))
                    Text("Mode API resmi saja", color = Color(0xFF8CF0C2), fontWeight = FontWeight.Bold)
                }
                Text("Tidak ada scraping, cookie sesi, reverse-engineered WebSocket, atau login TikTok di dalam aplikasi.",
                    color = Color.White)
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xCC202B43)),
            shape = RoundedCornerShape(18.dp)
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Status LIVE real-time", color = Color.White, fontWeight = FontWeight.Bold)
                Text("Belum tersedia melalui produk API publik TikTok yang terdokumentasi. Komentar LIVE, gift, like, follow, share, jumlah penonton, dan event PK tidak akan ditampilkan sebagai aktif tanpa produk/izin resmi yang secara eksplisit menyediakan data tersebut.",
                    color = Color(0xFFFFD18A))
                Text("Kontrak event internal aplikasi hanya fondasi pemrosesan; bukan koneksi TikTok.",
                    color = Color(0xFFB8C8DD), style = MaterialTheme.typography.bodySmall)
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xCC202B43)),
            shape = RoundedCornerShape(18.dp)
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Produk resmi yang dapat ditinjau", color = Color.White, fontWeight = FontWeight.Bold)
                Text("Login Kit / OAuth: autentikasi dan izin pengguna yang disetujui. Display API: data profil/video yang dicakup izin. Webhooks: hanya notifikasi event yang tercantum di dokumentasi; daftar publik saat ini tidak mencakup komentar atau gift LIVE.",
                    color = Color(0xFFB8C8DD))
                Text("Tidak ada tombol Hubungkan palsu. OAuth baru akan diaktifkan setelah Client Key, redirect URI, backend tepercaya, produk, dan scope yang benar-benar disetujui tersedia.",
                    color = Color(0xFFB8C8DD), style = MaterialTheme.typography.bodySmall)
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xCC3A2C1D)),
            shape = RoundedCornerShape(18.dp)
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    androidx.compose.material3.Icon(Icons.Default.Warning, contentDescription = null,
                        tint = Color(0xFFFFC477))
                    Text("Menunggu konfigurasi dan persetujuan", color = Color(0xFFFFD18A),
                        fontWeight = FontWeight.Bold)
                }
                Text("Status aplikasi Developer masih perlu diverifikasi di portal. Persetujuan scope saja tidak otomatis memberi akses; setiap pengguna juga harus memberikan otorisasi.",
                    color = Color.White)
            }
        }

        Spacer(Modifier.height(2.dp))
        Button(
            onClick = {
                runCatching {
                    context.startActivity(Intent(Intent.ACTION_VIEW,
                        Uri.parse("https://developers.tiktok.com/")))
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Buka TikTok for Developers")
            Spacer(Modifier.padding(start = 4.dp))
            androidx.compose.material3.Icon(Icons.Default.OpenInNew, contentDescription = null)
        }
        OutlinedButton(
            onClick = {
                runCatching {
                    context.startActivity(Intent(Intent.ACTION_VIEW,
                        Uri.parse("https://developers.tiktok.com/docs/en/scopes-overview")))
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Lihat dokumentasi scope resmi") }
    }
}
