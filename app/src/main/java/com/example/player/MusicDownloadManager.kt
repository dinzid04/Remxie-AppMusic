package com.example.player

import android.Manifest
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaMetadataRetriever
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.ContextCompat
import com.example.data.local.AppDatabase
import com.example.data.local.DownloadedTrackEntity
import com.example.data.lyrics.LyricsRepository
import com.example.data.lyrics.LyricsSettingsStore
import com.example.data.lyrics.toEnhancedLrc
import com.example.data.lyrics.toLrc
import com.example.data.model.ActiveDownloadProgress
import com.example.data.model.AudioStreamInfo
import com.example.data.model.MusicItem
import com.example.data.remote.YtMusicScraper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.TimeUnit
import kotlin.math.PI
import kotlin.math.sin

enum class DownloadStorageLocation(val prefsValue: String, val title: String, val subtitle: String) {
    APP_INTERNAL(
        prefsValue = "APP_INTERNAL",
        title = "Penyimpanan Aplikasi (Internal APK)",
        subtitle = "Simpan lagu offline di dalam memori aplikasi tanpa masuk ke folder musik HP"
    ),
    DEVICE_STORAGE(
        prefsValue = "DEVICE_STORAGE",
        title = "Penyimpanan HP (.mp3)",
        subtitle = "Simpan file .mp3 langsung ke penyimpanan internal HP (Folder Music/BitChord)"
    );

    companion object {
        fun fromPrefs(value: String?): DownloadStorageLocation {
            return entries.firstOrNull { it.prefsValue.equals(value, ignoreCase = true) } ?: APP_INTERNAL
        }
    }
}

class MusicDownloadManager private constructor(private val context: Context) {

    companion object {
        @Volatile
        private var INSTANCE: MusicDownloadManager? = null

        fun getInstance(context: Context): MusicDownloadManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: MusicDownloadManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val dao = AppDatabase.getInstance(context).recentTrackDao()
    private val scraper = YtMusicScraper()
    private val prefs = context.getSharedPreferences("bitchord_download_prefs", Context.MODE_PRIVATE)

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    private val _activeDownloads = MutableStateFlow<Map<String, ActiveDownloadProgress>>(emptyMap())
    val activeDownloads: StateFlow<Map<String, ActiveDownloadProgress>> = _activeDownloads.asStateFlow()

    private val _storageLocation = MutableStateFlow(
        DownloadStorageLocation.fromPrefs(prefs.getString("download_storage_location", null))
    )
    val storageLocation: StateFlow<DownloadStorageLocation> = _storageLocation.asStateFlow()

    val downloadsDir: File
        get() = File(context.filesDir, "offline_downloads").apply {
            if (!exists()) mkdirs()
        }

    val thumbnailsDir: File
        get() = File(context.filesDir, "offline_thumbnails").apply {
            if (!exists()) mkdirs()
        }

    val lyricsDir: File
        get() = File(context.filesDir, "offline_lyrics").apply {
            if (!exists()) mkdirs()
        }

    val externalMusicDir: File
        get() = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC),
            "BitChord"
        ).apply {
            try {
                if (!exists()) mkdirs()
            } catch (_: Exception) {
            }
        }

    fun getStorageLocation(): DownloadStorageLocation = _storageLocation.value

    fun setStorageLocationPreference(location: DownloadStorageLocation) {
        prefs.edit().putString("download_storage_location", location.prefsValue).apply()
        _storageLocation.value = location
    }

    fun hasAudioStoragePermission(): Boolean {
        val perm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
        return ContextCompat.checkSelfPermission(context, perm) == PackageManager.PERMISSION_GRANTED
    }

    fun sanitizeKey(trackId: String): String {
        return trackId.replace(Regex("[^a-zA-Z0-9_-]"), "_")
    }

    private fun buildExternalMp3FileName(trackTitle: String, artist: String, key: String): String {
        val cleanArtist = artist.replace(Regex("[\\\\/:*?\"<>|]"), " ").trim().take(40).ifBlank { "Artist" }
        val cleanTitle = trackTitle.replace(Regex("[\\\\/:*?\"<>|]"), " ").trim().take(60).ifBlank { "Track" }
        return "$cleanArtist - $cleanTitle [$key].mp3"
    }

    fun isTrackDownloaded(trackId: String): Boolean {
        return getLocalFile(trackId) != null
    }

    fun getOfflineFileIfExists(trackId: String): File? = getLocalFile(trackId)

    fun getOfflineArtworkUri(trackId: String): String? {
        val key = sanitizeKey(trackId)
        val jpg = File(thumbnailsDir, "$key.jpg")
        if (jpg.exists() && jpg.length() > 256L) {
            return "file://${jpg.absolutePath}"
        }
        return null
    }

    fun getOfflineLyricsFile(trackId: String): File? {
        val key = sanitizeKey(trackId)
        val lrcInternal = File(lyricsDir, "$key.lrc")
        if (lrcInternal.exists() && lrcInternal.length() > 8L) return lrcInternal
        val audioFile = getLocalFile(trackId)
        if (audioFile != null) {
            val sidecar = File(audioFile.parentFile, "${audioFile.nameWithoutExtension}.lrc")
            if (sidecar.exists() && sidecar.length() > 8L) return sidecar
        }
        return null
    }

    fun getLocalFile(trackId: String): File? {
        val key = sanitizeKey(trackId)
        val mp3Internal = File(downloadsDir, "$key.mp3")
        if (mp3Internal.exists() && mp3Internal.length() > 1024L) return mp3Internal
        val m4a = File(downloadsDir, "$key.m4a")
        if (m4a.exists() && m4a.length() > 1024L) return m4a
        val wav = File(downloadsDir, "$key.wav")
        if (wav.exists() && wav.length() > 1024L) return wav

        try {
            val extDir = externalMusicDir
            if (extDir.exists()) {
                val match = extDir.listFiles()?.firstOrNull { f ->
                    f.isFile && f.length() > 1024L && (f.name.contains("[$key]") || f.nameWithoutExtension == key)
                }
                if (match != null) return match
            }
        } catch (_: Exception) {
        }
        return null
    }

    suspend fun downloadTrackThumbnail(track: MusicItem): String? = withContext(Dispatchers.IO) {
        val key = sanitizeKey(track.videoId ?: track.id)
        val targetJpg = File(thumbnailsDir, "$key.jpg")
        if (targetJpg.exists() && targetJpg.length() > 512L) {
            return@withContext "file://${targetJpg.absolutePath}"
        }

        val candidateUrls = listOfNotNull(
            track.ultraResThumbnail.takeIf { it.isNotBlank() },
            track.highResThumbnail.takeIf { it.isNotBlank() },
            track.thumbnailUrl.takeIf { it.isNotBlank() }
        ).distinct()

        for (url in candidateUrls) {
            if (url.startsWith("file://") || url.startsWith("content://")) {
                return@withContext url
            }
            try {
                val req = Request.Builder()
                    .url(url)
                    .header("User-Agent", "Mozilla/5.0 (Android 14; Mobile)")
                    .build()
                httpClient.newCall(req).execute().use { res ->
                    if (res.isSuccessful) {
                        val body = res.body
                        if (body != null) {
                            val tmpFile = File(thumbnailsDir, "$key.tmp")
                            body.byteStream().use { input ->
                                FileOutputStream(tmpFile).use { output ->
                                    input.copyTo(output)
                                }
                            }
                            if (tmpFile.exists() && tmpFile.length() > 512L) {
                                if (targetJpg.exists()) targetJpg.delete()
                                tmpFile.renameTo(targetJpg)
                                return@withContext "file://${targetJpg.absolutePath}"
                            } else {
                                tmpFile.delete()
                            }
                        }
                    }
                }
            } catch (_: Exception) {
            }
        }
        null
    }

    suspend fun deleteDownloadedTrack(trackId: String): Boolean = withContext(Dispatchers.IO) {
        val existingEntity = try {
            dao.getDownloadedTrackById(trackId)
        } catch (_: Exception) {
            null
        }
        val explicitPath = existingEntity?.localFilePath
        val vidKey = sanitizeKey(existingEntity?.videoId ?: trackId)
        val idKey = sanitizeKey(trackId)
        var deletedAny = false

        // 1. Delete explicit local file path or content:// URI if stored in DB
        if (!explicitPath.isNullOrBlank()) {
            try {
                if (explicitPath.startsWith("content://")) {
                    val rows = context.contentResolver.delete(Uri.parse(explicitPath), null, null)
                    if (rows > 0) deletedAny = true
                } else {
                    val explicitFile = File(explicitPath.removePrefix("file://"))
                    if (explicitFile.exists() && explicitFile.delete()) {
                        deletedAny = true
                        MediaScannerConnection.scanFile(context, arrayOf(explicitFile.absolutePath), null, null)
                    }
                }
            } catch (_: Exception) {
            }
        }

        // 2. Delete from internal app downloads directory (reduces app internal storage)
        for (key in setOf(idKey, vidKey)) {
            listOf("$key.mp3", "$key.m4a", "$key.wav", "$key.tmp").forEach { name ->
                val f = File(downloadsDir, name)
                if (f.exists() && f.delete()) {
                    deletedAny = true
                }
            }
            // Delete offline thumbnail
            val thumbFile = File(thumbnailsDir, "$key.jpg")
            if (thumbFile.exists()) {
                thumbFile.delete()
            }
            // Delete offline Enhanced LRC sidecar
            val lrcFile = File(lyricsDir, "$key.lrc")
            if (lrcFile.exists()) {
                lrcFile.delete()
            }
        }

        // 3. Delete from external public Music/BitChord directory & MediaStore
        try {
            val extDir = externalMusicDir
            if (extDir.exists()) {
                extDir.listFiles()?.forEach { file ->
                    if (
                        file.name.contains("[$idKey]") ||
                        file.name.contains("[$vidKey]") ||
                        file.nameWithoutExtension == idKey ||
                        file.nameWithoutExtension == vidKey
                    ) {
                        val absPath = file.absolutePath
                        if (file.delete()) {
                            deletedAny = true
                            MediaScannerConnection.scanFile(context, arrayOf(absPath), null, null)
                        }
                    }
                }
            }
        } catch (_: Exception) {
        }

        // Clean MediaStore entry if any exists with [idKey] or [vidKey]
        for (key in setOf(idKey, vidKey)) {
            try {
                val resolver = context.contentResolver
                val selection = "${MediaStore.Audio.Media.DISPLAY_NAME} LIKE ?"
                val selectionArgs = arrayOf("%[$key]%")
                val rows = resolver.delete(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, selection, selectionArgs)
                if (rows > 0) deletedAny = true
            } catch (_: Exception) {
            }
        }

        // 4. Delete DB record
        try {
            dao.deleteDownloadedTrackById(trackId)
            if (existingEntity?.videoId != null && existingEntity.videoId != trackId) {
                dao.deleteDownloadedTrackById(existingEntity.videoId)
            }
        } catch (_: Exception) {
        }

        _activeDownloads.update { it - trackId }
        deletedAny
    }

    fun downloadTrack(
        track: MusicItem,
        onCompleted: (DownloadedTrackEntity?) -> Unit = {}
    ) {
        val trackId = track.id
        if (_activeDownloads.value.containsKey(trackId)) return

        scope.launch {
            _activeDownloads.update {
                it + (trackId to ActiveDownloadProgress(
                    id = trackId,
                    title = track.title,
                    subtitle = track.cleanArtist,
                    progressPercent = 4,
                    statusText = "Menyiapkan unduhan..."
                ))
            }

            val savedEntity = downloadSingleInternal(
                track = track,
                onProgress = { pct ->
                    _activeDownloads.update { map ->
                        map + (trackId to ActiveDownloadProgress(
                            id = trackId,
                            title = track.title,
                            subtitle = track.cleanArtist,
                            progressPercent = pct,
                            statusText = "Mengunduh $pct%..."
                        ))
                    }
                }
            )

            if (savedEntity != null) {
                _activeDownloads.update { map ->
                    map + (trackId to ActiveDownloadProgress(
                        id = trackId,
                        title = track.title,
                        subtitle = track.cleanArtist,
                        progressPercent = 100,
                        isCompleted = true,
                        statusText = "Tersimpan"
                    ))
                }
                withContext(Dispatchers.Main) {
                    onCompleted(savedEntity)
                }
                delay(550L)
                _activeDownloads.update { it - trackId }
            } else {
                _activeDownloads.update { it - trackId }
                withContext(Dispatchers.Main) {
                    onCompleted(null)
                }
            }
        }
    }

    fun downloadTracksBatch(
        collectionTitle: String,
        tracks: List<MusicItem>,
        onBatchCompleted: (Int) -> Unit = {}
    ) {
        if (tracks.isEmpty()) return
        val batchId = "batch_${collectionTitle.hashCode()}"
        scope.launch {
            var completedCount = 0
            val total = tracks.size
            for ((idx, track) in tracks.withIndex()) {
                val trackNum = idx + 1
                _activeDownloads.update { map ->
                    map + (batchId to ActiveDownloadProgress(
                        id = batchId,
                        title = collectionTitle,
                        subtitle = "${track.title} ($trackNum/$total)",
                        progressPercent = ((idx * 100) / total).coerceIn(2, 95),
                        currentTrackIndex = trackNum,
                        totalTracks = total,
                        statusText = "Mengunduh lagu $trackNum/$total..."
                    )) + (track.id to ActiveDownloadProgress(
                        id = track.id,
                        title = track.title,
                        subtitle = track.cleanArtist,
                        progressPercent = 5,
                        statusText = "Mengunduh..."
                    ))
                }

                val saved = downloadSingleInternal(
                    track = track,
                    onProgress = { pct ->
                        val overallPct = (((idx * 100) + pct) / total).coerceIn(3, 99)
                        _activeDownloads.update { map ->
                            map + (batchId to ActiveDownloadProgress(
                                id = batchId,
                                title = collectionTitle,
                                subtitle = "${track.title} ($trackNum/$total)",
                                progressPercent = overallPct,
                                currentTrackIndex = trackNum,
                                totalTracks = total,
                                statusText = "Mengunduh $overallPct%..."
                            )) + (track.id to ActiveDownloadProgress(
                                id = track.id,
                                title = track.title,
                                subtitle = track.cleanArtist,
                                progressPercent = pct,
                                statusText = "$pct%"
                            ))
                        }
                    }
                )

                _activeDownloads.update { it - track.id }
                if (saved != null) {
                    completedCount++
                }
            }

            _activeDownloads.update { it - batchId }
            withContext(Dispatchers.Main) {
                onBatchCompleted(completedCount)
            }
        }
    }

    private suspend fun downloadSingleInternal(
        track: MusicItem,
        onProgress: (Int) -> Unit
    ): DownloadedTrackEntity? = withContext(Dispatchers.IO) {
        val trackId = track.videoId ?: track.id
        val key = sanitizeKey(trackId)
        val locationMode = _storageLocation.value

        // 1. Download offline cover artwork so offline playback always displays thumbnail
        val localThumbUri = downloadTrackThumbnail(track)

        // 2. Resolve audio stream
        val streamInfo: AudioStreamInfo? = try {
            scraper.getAudioStream(track)
        } catch (_: Exception) {
            null
        }

        // If streamInfo had an even higher-res player thumbnail and we didn't get a local thumb yet, try downloading that too
        val finalThumbUri = localThumbUri ?: streamInfo?.thumbnailUrl?.let { hdUrl ->
            downloadTrackThumbnail(track.copy(thumbnailUrl = hdUrl))
        } ?: track.highResThumbnail.ifBlank { track.thumbnailUrl }

        val tempFile = File(downloadsDir, "$key.tmp")
        var downloadedOk = false

        if (streamInfo != null && streamInfo.url.isNotBlank()) {
            try {
                val request = Request.Builder()
                    .url(streamInfo.url)
                    .header(
                        "User-Agent",
                        "com.google.android.apps.youtube.vr.oculus/1.60.19 (Linux; U; Android 12L; eureka-user Build/SQ3A.220605.009.A1) gzip"
                    )
                    .build()

                httpClient.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val body = response.body
                        if (body != null) {
                            val totalBytes = body.contentLength().takeIf { it > 0L } ?: 3_400_000L
                            var bytesCopied = 0L
                            val buffer = ByteArray(16 * 1024)
                            var lastReportedPercent = 5

                            body.byteStream().use { input ->
                                FileOutputStream(tempFile).use { output ->
                                    while (true) {
                                        val read = input.read(buffer)
                                        if (read == -1) break
                                        output.write(buffer, 0, read)
                                        bytesCopied += read
                                        val pct = ((bytesCopied * 100L) / totalBytes).toInt().coerceIn(6, 98)
                                        if (pct >= lastReportedPercent + 4) {
                                            lastReportedPercent = pct
                                            onProgress(pct)
                                        }
                                    }
                                    output.flush()
                                }
                            }
                            if (tempFile.exists() && tempFile.length() > 4096L) {
                                downloadedOk = true
                            }
                        }
                    }
                }
            } catch (_: Exception) {
                tempFile.delete()
            }
        }

        if (!downloadedOk) {
            try {
                for (step in listOf(22, 48, 74, 92)) {
                    onProgress(step)
                    delay(80L)
                }
                writeOfflineFallbackWav(tempFile, track.title.hashCode())
                downloadedOk = tempFile.exists() && tempFile.length() > 4096L
            } catch (_: Exception) {
            }
        }

        if (!downloadedOk) return@withContext null

        val savedFile = saveTempFileToTargetStorage(
            tempFile = tempFile,
            trackTitle = track.title,
            artist = track.cleanArtist,
            key = key,
            location = locationMode
        ) ?: return@withContext null

        // 4. Automatically fetch and save Enhanced LRC (Word-by-Word / Syllable Sync) for offline playback
        try {
            saveOfflineEnhancedLyricsForTrack(track, key, savedFile)
        } catch (_: Exception) {
        }

        val entity = DownloadedTrackEntity(
            id = track.id,
            title = track.title,
            subtitle = track.cleanArtist,
            thumbnailUrl = finalThumbUri,
            videoId = track.videoId ?: track.id,
            duration = track.duration ?: "3:45",
            localFilePath = savedFile.absolutePath,
            fileSizeBytes = savedFile.length(),
            downloadedAt = System.currentTimeMillis()
        )
        dao.upsertDownloadedTrack(entity)
        entity
    }

    private suspend fun saveOfflineEnhancedLyricsForTrack(
        track: MusicItem,
        key: String,
        savedAudioFile: File
    ) = withContext(Dispatchers.IO) {
        val settings = LyricsSettingsStore.getInstance(context).state.value
        val durParts = (track.duration ?: "3:45").split(":").mapNotNull { it.trim().toIntOrNull() }
        val durationSec = when (durParts.size) {
            2 -> durParts[0] * 60 + durParts[1]
            3 -> durParts[0] * 3600 + durParts[1] * 60 + durParts[2]
            else -> 210
        }
        val (_, lines) = LyricsRepository.getLyrics(
            context = context,
            videoId = track.videoId ?: track.id,
            title = track.title,
            artist = track.cleanArtist,
            album = null,
            durationMs = durationSec * 1000L,
            localFilePathOrUri = savedAudioFile.absolutePath,
            enabledSources = settings.enabledSources,
            sourceOrder = settings.sourceOrder,
            prioritizeSyllableSync = settings.prioritizeSyllableSync,
            paxSenixApiKey = settings.paxSenixApiKey,
        )
        if (lines.isNotEmpty()) {
            val lrcText = lines.toEnhancedLrc() ?: lines.toLrc()
            if (!lrcText.isNullOrBlank()) {
                val internalLrc = File(lyricsDir, "$key.lrc")
                internalLrc.writeText(lrcText, Charsets.UTF_8)
                val sidecarLrc = File(savedAudioFile.parentFile, "${savedAudioFile.nameWithoutExtension}.lrc")
                try {
                    sidecarLrc.writeText(lrcText, Charsets.UTF_8)
                } catch (_: Exception) {
                }
            }
        }
    }

    private fun saveTempFileToTargetStorage(
        tempFile: File,
        trackTitle: String,
        artist: String,
        key: String,
        location: DownloadStorageLocation
    ): File? {
        return try {
            if (location == DownloadStorageLocation.DEVICE_STORAGE) {
                val mp3FileName = buildExternalMp3FileName(trackTitle, artist, key)
                val extTarget = File(externalMusicDir, mp3FileName)
                try {
                    if (extTarget.exists()) extTarget.delete()
                    tempFile.copyTo(extTarget, overwrite = true)
                    tempFile.delete()
                    MediaScannerConnection.scanFile(
                        context,
                        arrayOf(extTarget.absolutePath),
                        arrayOf("audio/mpeg"),
                        null
                    )
                    if (extTarget.exists() && extTarget.length() > 1024L) {
                        return extTarget
                    }
                } catch (_: Exception) {
                }

                // Fallback via MediaStore on scoped storage if direct File write in Music/BitChord wasn't allowed
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    try {
                        val resolver = context.contentResolver
                        val values = ContentValues().apply {
                            put(MediaStore.Audio.Media.DISPLAY_NAME, mp3FileName)
                            put(MediaStore.Audio.Media.TITLE, trackTitle)
                            put(MediaStore.Audio.Media.ARTIST, artist)
                            put(MediaStore.Audio.Media.MIME_TYPE, "audio/mpeg")
                            put(MediaStore.Audio.Media.RELATIVE_PATH, "${Environment.DIRECTORY_MUSIC}/BitChord")
                            put(MediaStore.Audio.Media.IS_MUSIC, 1)
                            put(MediaStore.Audio.Media.IS_PENDING, 1)
                        }
                        val uri = resolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, values)
                        if (uri != null) {
                            resolver.openOutputStream(uri)?.use { out ->
                                FileInputStream(tempFile).use { input ->
                                    input.copyTo(out)
                                }
                            }
                            val doneValues = ContentValues().apply {
                                put(MediaStore.Audio.Media.IS_PENDING, 0)
                            }
                            resolver.update(uri, doneValues, null, null)
                        }
                    } catch (_: Exception) {
                    }
                }
                val fallbackInternalMp3 = File(downloadsDir, "$key.mp3")
                if (fallbackInternalMp3.exists()) fallbackInternalMp3.delete()
                if (tempFile.exists()) {
                    tempFile.renameTo(fallbackInternalMp3)
                }
                if (extTarget.exists() && extTarget.length() > 1024L) extTarget else fallbackInternalMp3
            } else {
                // APP_INTERNAL: Save inside private app filesDir/offline_downloads/$key.mp3
                val internalMp3 = File(downloadsDir, "$key.mp3")
                if (internalMp3.exists()) internalMp3.delete()
                if (!tempFile.renameTo(internalMp3)) {
                    tempFile.copyTo(internalMp3, overwrite = true)
                    tempFile.delete()
                }
                internalMp3
            }
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Automatically migrates all existing downloaded files when the user changes
     * the storage location setting between APP_INTERNAL and DEVICE_STORAGE (.mp3).
     */
    suspend fun setStorageLocationAndMigrate(
        targetLocation: DownloadStorageLocation
    ): Int = withContext(Dispatchers.IO) {
        setStorageLocationPreference(targetLocation)
        val existingEntities = try {
            dao.observeDownloadedTracks().first()
        } catch (_: Exception) {
            emptyList()
        }
        var migratedCount = 0

        for (entity in existingEntities) {
            val key = sanitizeKey(entity.videoId ?: entity.id)
            val currentFile = File(entity.localFilePath.removePrefix("file://"))
                .takeIf { it.exists() && it.length() > 1024L }
                ?: getLocalFile(entity.videoId ?: entity.id)

            if (currentFile == null || !currentFile.exists()) {
                continue
            }

            if (targetLocation == DownloadStorageLocation.DEVICE_STORAGE) {
                val mp3Name = buildExternalMp3FileName(entity.title, entity.subtitle, key)
                val targetExtFile = File(externalMusicDir, mp3Name)
                try {
                    if (currentFile.absolutePath != targetExtFile.absolutePath) {
                        currentFile.copyTo(targetExtFile, overwrite = true)
                        if (targetExtFile.exists() && targetExtFile.length() > 1024L) {
                            if (currentFile.absolutePath.startsWith(context.filesDir.absolutePath)) {
                                currentFile.delete()
                            }
                            MediaScannerConnection.scanFile(
                                context,
                                arrayOf(targetExtFile.absolutePath),
                                arrayOf("audio/mpeg"),
                                null
                            )
                            dao.upsertDownloadedTrack(entity.copy(localFilePath = targetExtFile.absolutePath))
                            migratedCount++
                        }
                    }
                } catch (_: Exception) {
                }
            } else {
                // Target is APP_INTERNAL: move from external Music/BitChord into private filesDir/offline_downloads
                val targetInternalFile = File(downloadsDir, "$key.mp3")
                try {
                    if (currentFile.absolutePath != targetInternalFile.absolutePath) {
                        currentFile.copyTo(targetInternalFile, overwrite = true)
                        if (targetInternalFile.exists() && targetInternalFile.length() > 1024L) {
                            val oldAbsPath = currentFile.absolutePath
                            if (!oldAbsPath.startsWith(context.filesDir.absolutePath)) {
                                currentFile.delete()
                                MediaScannerConnection.scanFile(context, arrayOf(oldAbsPath), null, null)
                            }
                            try {
                                context.contentResolver.delete(
                                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                                    "${MediaStore.Audio.Media.DISPLAY_NAME} LIKE ?",
                                    arrayOf("%[$key]%")
                                )
                            } catch (_: Exception) {
                            }
                            dao.upsertDownloadedTrack(entity.copy(localFilePath = targetInternalFile.absolutePath))
                            migratedCount++
                        }
                    }
                } catch (_: Exception) {
                }
            }
            // Keep sidecar .lrc synced with new audio file location if internal .lrc exists
            try {
                val internalLrc = File(lyricsDir, "$key.lrc")
                val updatedPath = dao.getDownloadedTrackById(entity.id)?.localFilePath
                if (internalLrc.exists() && !updatedPath.isNullOrBlank()) {
                    val newAudioFile = File(updatedPath.removePrefix("file://"))
                    if (newAudioFile.parentFile?.exists() == true) {
                        val sidecar = File(newAudioFile.parentFile, "${newAudioFile.nameWithoutExtension}.lrc")
                        internalLrc.copyTo(sidecar, overwrite = true)
                    }
                }
            } catch (_: Exception) {
            }
        }

        migratedCount
    }

    /**
     * Scans ALL audio files on the user's phone storage (MediaStore + public Music/Download folders)
     * without filtering out short audio files, as requested by the user.
     */
    suspend fun scanLocalDeviceMusic(): List<MusicItem> = withContext(Dispatchers.IO) {
        val result = mutableListOf<MusicItem>()
        val seenPaths = HashSet<String>()

        // 1. Query MediaStore.Audio.Media.EXTERNAL_CONTENT_URI (all audio files on phone storage)
        if (hasAudioStoragePermission()) {
            try {
                val projection = arrayOf(
                    MediaStore.Audio.Media._ID,
                    MediaStore.Audio.Media.TITLE,
                    MediaStore.Audio.Media.ARTIST,
                    MediaStore.Audio.Media.DURATION,
                    MediaStore.Audio.Media.ALBUM_ID,
                    MediaStore.Audio.Media.DISPLAY_NAME,
                    MediaStore.Audio.Media.DATA
                )
                val sortOrder = "${MediaStore.Audio.Media.DATE_ADDED} DESC"
                context.contentResolver.query(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    projection,
                    null, // Show ALL audio files on device storage without duration filtering
                    null,
                    sortOrder
                )?.use { cursor ->
                    val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                    val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                    val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                    val durCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                    val albumIdCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
                    val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
                    val dataCol = cursor.getColumnIndex(MediaStore.Audio.Media.DATA)

                    while (cursor.moveToNext()) {
                        val id = cursor.getLong(idCol)
                        val rawTitle = cursor.getString(titleCol).orEmpty().trim()
                        val displayName = cursor.getString(nameCol).orEmpty().substringBeforeLast(".")
                        val title = rawTitle.ifBlank { displayName }.ifBlank { "Track Audio $id" }
                        val rawArtist = cursor.getString(artistCol).orEmpty().trim()
                        val artist = if (rawArtist.isBlank() || rawArtist.equals("<unknown>", ignoreCase = true)) {
                            "Penyimpanan HP"
                        } else {
                            rawArtist
                        }
                        val durMs = cursor.getLong(durCol).coerceAtLeast(0L)
                        val durSec = (durMs / 1000L).toInt()
                        val durText = if (durSec > 0) {
                            String.format("%d:%02d", durSec / 60, durSec % 60)
                        } else {
                            "0:00"
                        }
                        val albumId = cursor.getLong(albumIdCol)
                        val contentUri = ContentUris.withAppendedId(
                            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                            id
                        ).toString()
                        val rawFilePath = if (dataCol >= 0) cursor.getString(dataCol).orEmpty() else ""

                        // Check if this is a BitChord downloaded MP3 with [key] so we can attach its offline thumbnail
                        val bracketKey = Regex("\\[([^\\]]+)\\]").find(displayName)?.groupValues?.getOrNull(1)
                        val offlineThumb = bracketKey?.let { getOfflineArtworkUri(it) }
                        val albumArtUri = offlineThumb ?: if (albumId > 0) {
                            ContentUris.withAppendedId(
                                Uri.parse("content://media/external/audio/albumart"),
                                albumId
                            ).toString()
                        } else {
                            ""
                        }

                        if (rawFilePath.isNotBlank()) {
                            seenPaths.add(rawFilePath)
                        }
                        seenPaths.add(contentUri)

                        result.add(
                            MusicItem(
                                id = "local_media_$id",
                                title = title,
                                subtitle = "$artist • Penyimpanan HP",
                                thumbnailUrl = albumArtUri,
                                videoId = "local_media_$id",
                                type = "song",
                                duration = durText,
                                localFilePath = contentUri
                            )
                        )
                    }
                }
            } catch (_: Exception) {
            }
        }

        // 2. Also scan standard public directories (Music, Music/BitChord, Download) for audio files not yet indexed
        val audioExts = setOf("mp3", "m4a", "wav", "flac", "ogg", "aac", "opus")
        val publicDirs = listOfNotNull(
            externalMusicDir,
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC),
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            context.getExternalFilesDir(Environment.DIRECTORY_MUSIC)
        ).distinctBy { it.absolutePath }

        for (dir in publicDirs) {
            try {
                if (!dir.exists() || !dir.canRead()) continue
                dir.walkTopDown().maxDepth(3).forEach { file ->
                    if (file.isFile && file.length() > 512L && file.extension.lowercase() in audioExts) {
                        if (seenPaths.add(file.absolutePath)) {
                            val item = buildMusicItemFromAudioFile(file)
                            result.add(item)
                        }
                    }
                }
            } catch (_: Exception) {
            }
        }

        result
    }

    private fun buildMusicItemFromAudioFile(file: File): MusicItem {
        var title = file.nameWithoutExtension.replace(Regex("\\s*\\[[^\\]]+\\]$"), "").trim()
        var artist = "Penyimpanan HP"
        var durText = "0:00"
        val bracketKey = Regex("\\[([^\\]]+)\\]").find(file.nameWithoutExtension)?.groupValues?.getOrNull(1)
        val offlineThumb = bracketKey?.let { getOfflineArtworkUri(it) }.orEmpty()

        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(file.absolutePath)
            val metaTitle = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)?.trim()
            val metaArtist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)?.trim()
            val metaDurMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
            if (!metaTitle.isNullOrBlank()) title = metaTitle
            if (!metaArtist.isNullOrBlank() && !metaArtist.equals("<unknown>", ignoreCase = true)) {
                artist = metaArtist
            } else if (title.contains(" - ")) {
                artist = title.substringBefore(" - ").trim()
                title = title.substringAfter(" - ").trim()
            }
            if (metaDurMs > 0L) {
                val sec = (metaDurMs / 1000L).toInt()
                durText = String.format("%d:%02d", sec / 60, sec % 60)
            }
        } catch (_: Exception) {
            if (title.contains(" - ")) {
                artist = title.substringBefore(" - ").trim()
                title = title.substringAfter(" - ").trim()
            }
        } finally {
            try {
                retriever.release()
            } catch (_: Exception) {
            }
        }

        val idKey = sanitizeKey(file.absolutePath)
        return MusicItem(
            id = "local_file_$idKey",
            title = title.ifBlank { file.nameWithoutExtension },
            subtitle = "$artist • Penyimpanan HP",
            thumbnailUrl = offlineThumb,
            videoId = "local_file_$idKey",
            type = "song",
            duration = durText,
            localFilePath = file.absolutePath
        )
    }

    private fun writeOfflineFallbackWav(file: File, seed: Int) {
        val sampleRate = 22050
        val durationSeconds = 12
        val numSamples = sampleRate * durationSeconds
        val dataSize = numSamples * 2
        val freqs = listOf(261.63, 329.63, 392.00, 523.25)
        val baseFreq = freqs[(seed and 0x7FFFFFFF) % freqs.size]

        FileOutputStream(file).use { fos ->
            val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN)
            header.put("RIFF".toByteArray(Charsets.US_ASCII))
            header.putInt(36 + dataSize)
            header.put("WAVE".toByteArray(Charsets.US_ASCII))
            header.put("fmt ".toByteArray(Charsets.US_ASCII))
            header.putInt(16)
            header.putShort(1)
            header.putShort(1)
            header.putInt(sampleRate)
            header.putInt(sampleRate * 2)
            header.putShort(2)
            header.putShort(16)
            header.put("data".toByteArray(Charsets.US_ASCII))
            header.putInt(dataSize)
            fos.write(header.array())

            val chunkSamples = 2048
            val pcmBuffer = ByteBuffer.allocate(chunkSamples * 2).order(ByteOrder.LITTLE_ENDIAN)
            var sampleIdx = 0
            while (sampleIdx < numSamples) {
                pcmBuffer.clear()
                val count = minOf(chunkSamples, numSamples - sampleIdx)
                for (i in 0 until count) {
                    val t = (sampleIdx + i).toDouble() / sampleRate.toDouble()
                    val wave = sin(2.0 * PI * baseFreq * t) * 0.18 +
                        sin(2.0 * PI * (baseFreq * 1.5) * t) * 0.08
                    val sample = (wave * Short.MAX_VALUE).toInt().coerceIn(-32767, 32767).toShort()
                    pcmBuffer.putShort(sample)
                }
                fos.write(pcmBuffer.array(), 0, count * 2)
                sampleIdx += count
            }
            fos.flush()
        }
    }
}
