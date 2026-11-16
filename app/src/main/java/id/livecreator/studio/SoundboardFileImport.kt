package id.livecreator.studio

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import java.io.File
import java.io.FileOutputStream

/**
 * Copies selected audio into app-private storage. This avoids broken library entries when
 * a removable/cloud document provider revokes or moves the original document.
 */
internal object SoundboardFileImport {
    const val MAX_AUDIO_BYTES = 50L * 1024L * 1024L

    private val supportedExtensions = setOf(
        "mp3", "wav", "m4a", "aac", "ogg", "oga", "opus", "flac", "3gp", "amr", "mid", "midi"
    )

    internal fun isSupportedAudio(mimeType: String?, displayName: String): Boolean {
        val mime = mimeType?.substringBefore(';')?.trim()?.lowercase()
        val extension = displayName.substringAfterLast('.', "").lowercase()
        return mime?.startsWith("audio/") == true || extension in supportedExtensions
    }

    internal fun extensionFor(mimeType: String?, displayName: String): String {
        val extension = displayName.substringAfterLast('.', "").lowercase()
        if (extension in supportedExtensions) return extension
        return when (mimeType?.substringBefore(';')?.trim()?.lowercase()) {
            "audio/wav", "audio/x-wav", "audio/wave" -> "wav"
            "audio/mpeg" -> "mp3"
            "audio/mp4", "audio/x-m4a" -> "m4a"
            "audio/aac" -> "aac"
            "audio/ogg", "application/ogg" -> "ogg"
            "audio/flac", "audio/x-flac" -> "flac"
            "audio/3gpp" -> "3gp"
            "audio/amr" -> "amr"
            "audio/midi", "audio/x-midi" -> "mid"
            else -> "audio"
        }
    }

    fun import(context: Context, source: Uri): SoundItem {
        val resolver = context.contentResolver
        val displayName = resolver.query(
            source, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null
        )?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0) else null
        }?.takeIf { it.isNotBlank() } ?: "audio"

        val mimeType = resolver.getType(source)
        require(isSupportedAudio(mimeType, displayName)) {
            "Pilih file audio MP3, WAV, M4A, AAC, OGG, OPUS, FLAC, atau format audio yang didukung."
        }

        val directory = File(context.filesDir, "soundboard")
        if (!directory.exists() && !directory.mkdirs()) {
            error("Folder pustaka audio tidak dapat dibuat.")
        }

        val safeStem = displayName.substringBeforeLast('.', displayName)
            .replace(Regex("[^A-Za-z0-9._-]"), "_")
            .trim('.', '_')
            .take(72)
            .ifBlank { "audio" }
        val extension = extensionFor(mimeType, displayName)
        val destination = File(directory, "${System.currentTimeMillis()}_$safeStem.$extension")
        val temporary = File(directory, destination.name + ".part")

        try {
            val input = resolver.openInputStream(source) ?: error("File tidak dapat dibaca.")
            var copied = 0L
            input.use { stream ->
                FileOutputStream(temporary).use { output ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    while (true) {
                        val count = stream.read(buffer)
                        if (count < 0) break
                        copied += count
                        require(copied <= MAX_AUDIO_BYTES) {
                            "Ukuran audio melebihi batas 50 MB."
                        }
                        output.write(buffer, 0, count)
                    }
                }
            }
            require(copied > 0L) { "File audio kosong." }
            if (!temporary.renameTo(destination)) {
                temporary.copyTo(destination, overwrite = true)
                temporary.delete()
            }
            return SoundItem(Uri.fromFile(destination).toString(), displayName.take(120))
        } catch (error: Exception) {
            temporary.delete()
            destination.delete()
            throw error
        }
    }

    fun delete(context: Context, uri: Uri) {
        if (uri.scheme == "file") {
            runCatching {
                val library = File(context.filesDir, "soundboard").canonicalFile
                val target = File(uri.path ?: return).canonicalFile
                if (target.parentFile == library) target.delete()
            }
        } else if (uri.scheme == "content") {
            runCatching {
                context.contentResolver.releasePersistableUriPermission(
                    uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
        }
    }
}
