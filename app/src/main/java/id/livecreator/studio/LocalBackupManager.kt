package id.livecreator.studio

import android.content.Context
import android.net.Uri
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/** Offline archive saved only to a user-selected Storage Access Framework URI. */
internal object LocalBackupManager {
    const val MAX_ARCHIVE_BYTES = 100L * 1024L * 1024L
    private const val MANIFEST = "backup.json"
    private const val AUDIO_PREFIX = "audio/"
    private const val SCHEMA_VERSION = 1

    suspend fun export(context: Context, destination: Uri) {
        val settings = context.liveCreatorSettings.data.first()
        val sounds = SoundboardStore.observe(context).first()
        val overlay = context.getSharedPreferences("floating_control", Context.MODE_PRIVATE)
        val manifest = JSONObject()
            .put("schemaVersion", SCHEMA_VERSION)
            .put("createdAt", System.currentTimeMillis())
            .put("settings", JSONObject()
                .put("autoStartOnLaunch", settings[AppSettingKeys.AUTO_START_ON_LAUNCH] ?: false)
                .put("resumeReminderEnabled", settings[AppSettingKeys.RESUME_REMINDER_ENABLED] ?: true)
                .put("speechRate", (settings[AppSettingKeys.SPEECH_RATE] ?: 1.0f).toDouble())
                .put("speechPitch", (settings[AppSettingKeys.SPEECH_PITCH] ?: 1.0f).toDouble()))
            .put("floating", JSONObject()
                .put("enabled", overlay.getBoolean("enabled", false))
                .put("size", overlay.getFloat("size", 42f).toDouble())
                .put("opacity", overlay.getFloat("opacity", 0.82f).toDouble())
                .put("right", overlay.getBoolean("right", false))
                .put("y", overlay.getInt("y", 120)))
        val soundArray = JSONArray()
        var totalBytes = 0L
        val audioFiles = mutableListOf<Pair<File, String>>()
        sounds.forEachIndexed { index, sound ->
            val item = JSONObject().put("title", sound.title)
            val file = if (isPackSoundUri(sound.uri)) null else runCatching {
                val parsed = Uri.parse(sound.uri)
                if (parsed.scheme == "file") File(parsed.path ?: "") else null
            }.getOrNull()
            if (file != null && file.isFile &&
                file.canonicalPath.startsWith(File(context.filesDir, "soundboard").canonicalPath + File.separator)) {
                val entryName = "$AUDIO_PREFIX$index-${file.name.replace(Regex("[^A-Za-z0-9._-]"), "_")}"
                totalBytes += file.length()
                require(totalBytes <= MAX_ARCHIVE_BYTES) { "Cadangan audio melebihi batas 100 MB." }
                item.put("entry", entryName)
                audioFiles += file to entryName
            } else if (isPackSoundUri(sound.uri)) {
                item.put("packUri", sound.uri)
            } else {
                item.put("missingAudio", true)
            }
            soundArray.put(item)
        }
        manifest.put("sounds", soundArray)
        val output = context.contentResolver.openOutputStream(destination, "wt")
            ?: error("Lokasi cadangan tidak dapat ditulis.")
        ZipOutputStream(BufferedOutputStream(output)).use { zip ->
            zip.putNextEntry(ZipEntry(MANIFEST))
            zip.write(manifest.toString().toByteArray(Charsets.UTF_8))
            zip.closeEntry()
            audioFiles.forEach { (file, entryName) ->
                zip.putNextEntry(ZipEntry(entryName))
                file.inputStream().buffered().use { it.copyTo(zip) }
                zip.closeEntry()
            }
        }
    }

    suspend fun restore(context: Context, source: Uri) {
        val input = context.contentResolver.openInputStream(source) ?: error("File cadangan tidak dapat dibaca.")
        val entries = linkedMapOf<String, ByteArray>()
        var total = 0L
        ZipInputStream(BufferedInputStream(input)).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                require(!entry.isDirectory && !entry.name.startsWith("/") &&
                    !entry.name.contains("..") && !entry.name.contains("\\")) {
                    "Struktur file cadangan tidak valid."
                }
                require(entry.name == MANIFEST || entry.name.startsWith(AUDIO_PREFIX)) {
                    "Arsip berisi file yang tidak dikenali."
                }
                val bytes = zip.readBytes()
                total += bytes.size
                require(total <= MAX_ARCHIVE_BYTES) { "File cadangan melebihi batas 100 MB." }
                require(entries.put(entry.name, bytes) == null) { "Arsip memiliki nama file duplikat." }
                zip.closeEntry()
            }
        }
        val manifestBytes = entries[MANIFEST] ?: error("Manifest cadangan tidak ditemukan.")
        val manifest = JSONObject(manifestBytes.toString(Charsets.UTF_8))
        require(manifest.optInt("schemaVersion", -1) == SCHEMA_VERSION) { "Versi cadangan tidak didukung." }
        val settings = manifest.getJSONObject("settings")
        val floating = manifest.getJSONObject("floating")
        val restoredSounds = mutableListOf<SoundItem>()
        val audioDir = File(context.filesDir, "soundboard").apply {
            if (!exists() && !mkdirs()) error("Folder soundboard tidak dapat dibuat.")
        }
        val staged = mutableListOf<File>()
        try {
            val soundArray = manifest.optJSONArray("sounds") ?: JSONArray()
            for (i in 0 until soundArray.length()) {
                val item = soundArray.getJSONObject(i)
                val title = item.optString("title", "Audio").take(120)
                val entryName = item.optString("entry")
                if (entryName.isNotBlank()) {
                    require(entryName.startsWith(AUDIO_PREFIX) && entries.containsKey(entryName)) {
                        "Audio dalam cadangan tidak lengkap."
                    }
                    val safeName = entryName.substringAfterLast('/').replace(Regex("[^A-Za-z0-9._-]"), "_")
                    val destination = File(audioDir, "restored_${System.currentTimeMillis()}_$safeName")
                    destination.outputStream().use { it.write(entries.getValue(entryName)) }
                    staged += destination
                    restoredSounds += SoundItem(Uri.fromFile(destination).toString(), title)
                } else {
                    val packUri = item.optString("packUri")
                    if (packUri.startsWith("file:") && isPackSoundUri(packUri)) {
                        restoredSounds += SoundItem(packUri, title)
                    }
                }
            }
            context.liveCreatorSettings.edit { prefs ->
                prefs[AppSettingKeys.AUTO_START_ON_LAUNCH] = settings.optBoolean("autoStartOnLaunch", false)
                prefs[AppSettingKeys.RESUME_REMINDER_ENABLED] = settings.optBoolean("resumeReminderEnabled", true)
                prefs[AppSettingKeys.SPEECH_RATE] = sanitizeSpeechRate(settings.optDouble("speechRate", 1.0).toFloat())
                prefs[AppSettingKeys.SPEECH_PITCH] = sanitizeSpeechPitch(settings.optDouble("speechPitch", 1.0).toFloat())
            }
            context.getSharedPreferences("floating_control", Context.MODE_PRIVATE).edit()
                .putBoolean("enabled", floating.optBoolean("enabled", false))
                .putFloat("size", sanitizeFloatingBubbleSize(floating.optDouble("size", 42.0).toFloat()))
                .putFloat("opacity", sanitizeFloatingBubbleOpacity(floating.optDouble("opacity", 0.82).toFloat()))
                .putBoolean("right", floating.optBoolean("right", false))
                .putInt("y", floating.optInt("y", 120))
                .apply()
            SoundboardStore.save(context, restoredSounds)
        } catch (error: Exception) {
            staged.forEach { it.delete() }
            throw error
        }
    }
}
