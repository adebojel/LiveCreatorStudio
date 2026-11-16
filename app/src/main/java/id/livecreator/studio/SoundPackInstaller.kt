package id.livecreator.studio

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import java.util.zip.ZipInputStream

internal sealed interface SoundPackInstallState {
    data object Idle : SoundPackInstallState
    data class Downloading(val packTitle: String) : SoundPackInstallState
    data class Installed(val packTitle: String, val soundCount: Int) : SoundPackInstallState
    data class Failed(val packTitle: String, val reason: String) : SoundPackInstallState
}

/**
 * Downloads and safely extracts the curated CC0 packs into app-private storage.
 * The installer never downloads arbitrary URLs: only reviewed catalog entries on kenney.nl
 * and opengameart.org are accepted. Starter packs are retried on the next app launch if offline.
 */
internal object SoundPackInstaller {
    private const val MAX_ARCHIVE_BYTES = 120L * 1024L * 1024L
    private const val MAX_EXTRACTED_BYTES = 240L * 1024L * 1024L
    private const val MAX_ENTRIES = 1_500
    private const val MAX_AUDIO_FILE_BYTES = 24L * 1024L * 1024L
    private val supportedExtensions = setOf("ogg", "wav", "mp3", "flac", "m4a", "aac", "opus", "aiff", "aif", "weba")
    private val mutex = Mutex()

    private val mutableState = MutableStateFlow<SoundPackInstallState>(SoundPackInstallState.Idle)
    val state: StateFlow<SoundPackInstallState> = mutableState

    suspend fun ensureStarterPacks(context: Context) {
        mutex.withLock {
            var firstFailure: Pair<String, String>? = null
            for (pack in SoundPackCatalog.starterPacks) {
                if (!isInstalled(context, pack)) {
                    runCatching { installPackLocked(context, pack) }
                        .onFailure { error ->
                            if (firstFailure == null) {
                                firstFailure = pack.title to (error.message ?: "Unduhan belum berhasil")
                            }
                        }
                }
            }
            firstFailure?.let { (title, reason) ->
                mutableState.value = SoundPackInstallState.Failed(title, reason)
            }
        }
    }

    suspend fun installAll(context: Context) {
        mutex.withLock {
            var firstFailure: Pair<String, String>? = null
            for (pack in SoundPackCatalog.packs) {
                if (!isInstalled(context, pack)) {
                    runCatching { installPackLocked(context, pack) }
                        .onFailure { error ->
                            if (firstFailure == null) {
                                firstFailure = pack.title to (error.message ?: "Unduhan belum berhasil")
                            }
                        }
                }
            }
            val installedCount = SoundPackCatalog.packs
                .filter { isInstalled(context, it) }
                .sumOf { audioFiles(packDirectory(context, it)).size }
            firstFailure?.let { (title, reason) ->
                mutableState.value = SoundPackInstallState.Failed(title, reason)
            } ?: run {
                mutableState.value = SoundPackInstallState.Installed("Semua paket", installedCount)
            }
        }
    }

    suspend fun install(context: Context, packId: String) {
        val pack = SoundPackCatalog.packs.firstOrNull { it.id == packId } ?: return
        mutex.withLock {
            if (isInstalled(context, pack)) {
                mutableState.value = SoundPackInstallState.Installed(
                    pack.title, audioFiles(packDirectory(context, pack)).size
                )
                return
            }
            runCatching { installPackLocked(context, pack) }
                .onFailure {
                    mutableState.value = SoundPackInstallState.Failed(
                        pack.title, it.message ?: "Unduhan belum berhasil"
                    )
                }
        }
    }

    fun isInstalled(context: Context, pack: SoundPack): Boolean =
        audioFiles(packDirectory(context, pack)).isNotEmpty()

    private suspend fun installPackLocked(context: Context, pack: SoundPack) = withContext(Dispatchers.IO) {
        mutableState.value = SoundPackInstallState.Downloading(pack.title)
        val archive = File(context.cacheDir, "soundpack_${pack.id}.zip")
        val root = File(context.filesDir, "soundboard/packs")
        if (!root.exists() && !root.mkdirs()) error("Folder pustaka suara tidak dapat dibuat.")
        val staging = File(root, ".${pack.id}.staging")
        val destination = packDirectory(context, pack)
        val backup = File(root, ".${pack.id}.backup")
        archive.delete()
        staging.deleteRecursively()
        backup.deleteRecursively()

        try {
            downloadArchive(pack, archive)
            if (!staging.mkdirs()) error("Folder sementara paket tidak dapat dibuat.")
            val sourceExtension = pack.archiveUrl.substringBefore('?').substringAfterLast('.', "").lowercase(Locale.ROOT)
            val extracted = if (sourceExtension in supportedExtensions) {
                val directAudio = File(staging, "1_${pack.id}.${sourceExtension}")
                archive.copyTo(directAudio, overwrite = true)
                listOf(directAudio)
            } else {
                extractAudio(archive, staging)
            }
            if (extracted.isEmpty()) error("Arsip berhasil dibuka, tetapi tidak ditemukan audio OGG, WAV, MP3, FLAC, M4A, AAC, OPUS, AIFF, atau WEBA. Periksa arsip sumber paket.")

            File(staging, "LICENSES.txt").writeText(
                "${pack.title}\nCreator: ${pack.creator}\n" +
                    "Source: ${pack.sourceUrl}\nLicense: Creative Commons Zero 1.0 (CC0)\n" +
                    "License URL: https://creativecommons.org/publicdomain/zero/1.0/\n" +
                    "Original pack license is included in the upstream archive when provided.\n"
            )

            if (destination.exists() && !destination.renameTo(backup)) {
                error("Pustaka paket lama tidak dapat diamankan.")
            }
            if (!staging.renameTo(destination)) {
                if (backup.exists()) backup.renameTo(destination)
                error("Paket audio tidak dapat dipasang.")
            }
            backup.deleteRecursively()

            val items = extracted.map { stagedFile ->
                // The staging directory has been renamed; rebuild each URI against its final location.
                val installedFile = File(destination, stagedFile.name)
                val label = installedFile.nameWithoutExtension
                    .replace('_', ' ')
                    .replace('-', ' ')
                    .replace(Regex("\\s+"), " ")
                    .trim()
                SoundItem(
                    Uri.fromFile(installedFile).toString(),
                    "${pack.title} • ${label.ifBlank { installedFile.nameWithoutExtension }}"
                )
            }
            val existing = SoundboardStore.observe(context).first()
            SoundboardStore.save(context, (existing + items).distinctBy { it.uri })
            mutableState.value = SoundPackInstallState.Installed(pack.title, items.size)
        } catch (error: Exception) {
            staging.deleteRecursively()
            if (backup.exists() && !destination.exists()) backup.renameTo(destination)
            throw error
        } finally {
            archive.delete()
        }
    }

    private fun downloadArchive(pack: SoundPack, destination: File) {
        val source = URL(pack.archiveUrl)
        require(source.protocol == "https" &&
            (source.host.equals("kenney.nl", ignoreCase = true) ||
                source.host.equals("opengameart.org", ignoreCase = true) ||
                source.host.equals("sfxmint.com", ignoreCase = true))) {
            "Sumber paket tidak diizinkan."
        }
        val connection = (source.openConnection() as HttpURLConnection).apply {
            connectTimeout = 20_000
            readTimeout = 45_000
            instanceFollowRedirects = true
            requestMethod = "GET"
            setRequestProperty("User-Agent", "LiveCreatorStudio-Android/0.6.3")
            setRequestProperty("Accept", "application/zip, application/octet-stream, */*")
            setRequestProperty("Referer", pack.sourceUrl)
        }
        try {
            val status = connection.responseCode
            require(status in 200..299) { "Server paket mengembalikan HTTP $status." }
            require(connection.url.protocol == "https" &&
                (connection.url.host.equals("kenney.nl", ignoreCase = true) ||
                    connection.url.host.endsWith(".kenney.nl", ignoreCase = true) ||
                    connection.url.host.equals("opengameart.org", ignoreCase = true) ||
                    connection.url.host.endsWith(".opengameart.org", ignoreCase = true) ||
                    connection.url.host.equals("sfxmint.com", ignoreCase = true) ||
                    connection.url.host.endsWith(".sfxmint.com", ignoreCase = true))) {
                "Pengalihan ke host unduhan yang tidak diizinkan."
            }
            val expected = connection.contentLengthLong
            require(expected <= MAX_ARCHIVE_BYTES) { "Ukuran arsip melewati batas 120 MB." }
            connection.inputStream.use { input ->
                FileOutputStream(destination).use { output ->
                    val buffer = ByteArray(16 * 1024)
                    var total = 0L
                    while (true) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        total += count
                        require(total <= MAX_ARCHIVE_BYTES) { "Unduhan melewati batas 120 MB." }
                        output.write(buffer, 0, count)
                    }
                    require(total > 0L && (expected < 0L || total == expected)) {
                        "Arsip unduhan tidak lengkap."
                    }
                }
            }
        } finally {
            connection.disconnect()
        }
    }

    private fun extractAudio(archive: File, destination: File): List<File> {
        val extracted = mutableListOf<File>()
        var entryCount = 0
        var totalBytes = 0L
        ZipInputStream(BufferedInputStream(archive.inputStream())).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                entryCount++
                require(entryCount <= MAX_ENTRIES) { "Paket berisi terlalu banyak file." }
                if (!entry.isDirectory) {
                    val extension = entry.name.substringAfterLast('.', "").lowercase(Locale.ROOT)
                    if (extension in supportedExtensions) {
                        val rawName = entry.name.substringAfterLast('/').substringAfterLast('\\')
                        val safeName = rawName.replace(Regex("[^A-Za-z0-9._-]"), "_")
                            .take(100).ifBlank { "sound_$entryCount.$extension" }
                        val output = File(destination, "${entryCount}_$safeName")
                        val canonicalRoot = destination.canonicalPath + File.separator
                        require(output.canonicalPath.startsWith(canonicalRoot)) {
                            "Arsip memiliki jalur file yang tidak aman."
                        }
                        var fileBytes = 0L
                        FileOutputStream(output).use { stream ->
                            val buffer = ByteArray(8 * 1024)
                            while (true) {
                                val count = zip.read(buffer)
                                if (count < 0) break
                                fileBytes += count
                                totalBytes += count
                                require(fileBytes <= MAX_AUDIO_FILE_BYTES) {
                                    "Salah satu file audio terlalu besar."
                                }
                                require(totalBytes <= MAX_EXTRACTED_BYTES) {
                                    "Ukuran hasil ekstraksi melewati batas."
                                }
                                stream.write(buffer, 0, count)
                            }
                        }
                        if (fileBytes > 0L) extracted += output else output.delete()
                    }
                }
                zip.closeEntry()
            }
        }
        return extracted
    }

    private fun packDirectory(context: Context, pack: SoundPack): File =
        File(File(context.filesDir, "soundboard/packs"), pack.id)

    private fun audioFiles(directory: File): List<File> =
        directory.listFiles()?.filter {
            it.isFile && it.extension.lowercase(Locale.ROOT) in supportedExtensions
        }.orEmpty()
}
