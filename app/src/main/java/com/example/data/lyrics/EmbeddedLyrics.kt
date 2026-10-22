package com.example.data.lyrics

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder

object EmbeddedLyrics {

    suspend fun read(
        context: Context,
        localFilePathOrUri: String?,
        trackIdKey: String? = null,
    ): List<LyricLine>? = withContext(Dispatchers.IO) {
        if (localFilePathOrUri.isNullOrBlank() && trackIdKey.isNullOrBlank()) return@withContext null

        // 1. Check sidecar .lrc in offline_downloads / offline_lyrics / alongside audio file
        val candidateFiles = ArrayList<File>()
        if (!localFilePathOrUri.isNullOrBlank() && !localFilePathOrUri.startsWith("content://")) {
            val audioFile = File(localFilePathOrUri.removePrefix("file://"))
            if (audioFile.exists()) {
                val parent = audioFile.parentFile
                if (parent != null) {
                    candidateFiles.add(File(parent, "${audioFile.nameWithoutExtension}.lrc"))
                    candidateFiles.add(File(parent, "lyrics.lrc"))
                }
            }
        }

        if (!trackIdKey.isNullOrBlank()) {
            val safeKey = trackIdKey.replace(Regex("[^a-zA-Z0-9_-]"), "_")
            val dlDir = File(context.filesDir, "offline_downloads")
            val lrcDir = File(context.filesDir, "offline_lyrics")
            candidateFiles.add(File(lrcDir, "$safeKey.lrc"))
            candidateFiles.add(File(dlDir, "$safeKey.lrc"))
        }

        for (lrcFile in candidateFiles) {
            if (lrcFile.exists() && lrcFile.isFile && lrcFile.length() > 8L) {
                val raw = runCatching { lrcFile.readText(Charsets.UTF_8) }.getOrNull()
                if (!raw.isNullOrBlank()) {
                    ProviderLyrics.parse(raw)?.takeIf { it.isNotEmpty() }?.let { return@withContext it }
                }
            }
        }

        // 2. Check container tags if file exists on disk
        val audioFile = localFilePathOrUri
            ?.takeIf { !it.startsWith("content://") }
            ?.let { File(it.removePrefix("file://")) }
            ?.takeIf { it.exists() && it.isFile && it.length() > 128L }

        if (audioFile != null) {
            readContainerLyrics(audioFile)?.let { raw ->
                ProviderLyrics.parse(raw)?.takeIf { it.isNotEmpty() }?.let { return@withContext it }
            }
        }

        // 3. Fallback: Check ID3 USLT/SYLT or MediaMetadataRetriever if content:// or MP3
        if (!localFilePathOrUri.isNullOrBlank()) {
            readId3OrRetrieverLyrics(context, localFilePathOrUri)?.let { raw ->
                ProviderLyrics.parse(raw)?.takeIf { it.isNotEmpty() }?.let { return@withContext it }
            }
        }

        null
    }

    private fun readContainerLyrics(file: File): String? {
        return try {
            RandomAccessFile(file, "r").use { raf ->
                if (raf.length() < 16L) return null
                val header = ByteArray(12)
                raf.readFully(header)
                val magicStr = String(header, 0, 4, Charsets.ISO_8859_1)
                val ftypStr = String(header, 4, 4, Charsets.ISO_8859_1)
                when {
                    magicStr == "fLaC" -> readFlacVorbisLyrics(raf)
                    ftypStr == "ftyp" || magicStr == "moov" -> readMp4Lyrics(raf)
                    magicStr == "ID3\u0003" || magicStr.startsWith("ID3") -> readId3v2Lyrics(raf)
                    header[0] == 0x1A.toByte() &&
                        header[1] == 0x45.toByte() &&
                        header[2] == 0xDF.toByte() &&
                        header[3] == 0xA3.toByte() -> readMatroskaLyrics(raf)
                    else -> null
                }
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun readFlacVorbisLyrics(raf: RandomAccessFile): String? {
        raf.seek(4L)
        var enhanced: String? = null
        var plain: String? = null
        while (raf.filePointer + 4L <= raf.length()) {
            val blockHeader = raf.readUnsignedByte()
            val isLast = (blockHeader and 0x80) != 0
            val blockType = blockHeader and 0x7F
            val length = ((raf.readUnsignedByte() shl 16) or (raf.readUnsignedByte() shl 8) or raf.readUnsignedByte())
            val nextPos = raf.filePointer + length
            if (blockType == 4 && length in 8..2_000_000) {
                val buf = ByteArray(length)
                raf.readFully(buf)
                val bb = ByteBuffer.wrap(buf).order(ByteOrder.LITTLE_ENDIAN)
                val vendorLen = bb.int
                if (vendorLen in 0..(bb.remaining() - 4)) {
                    bb.position(bb.position() + vendorLen)
                    val count = bb.int
                    for (i in 0 until count.coerceAtMost(512)) {
                        if (bb.remaining() < 4) break
                        val cLen = bb.int
                        if (cLen < 0 || cLen > bb.remaining()) break
                        val bytes = ByteArray(cLen)
                        bb.get(bytes)
                        val comment = String(bytes, Charsets.UTF_8)
                        val eq = comment.indexOf('=')
                        if (eq > 0) {
                            val key = comment.substring(0, eq).trim().uppercase()
                            val value = comment.substring(eq + 1).trim()
                            if (key == WORD_LYRICS_FIELD && value.isNotEmpty()) {
                                enhanced = value
                            } else if ((key == "LYRICS" || key == "UNSYNCEDLYRICS") && value.isNotEmpty() && plain == null) {
                                plain = value
                            }
                        }
                    }
                }
            }
            if (isLast || nextPos >= raf.length()) break
            raf.seek(nextPos)
        }
        return enhanced ?: plain
    }

    private fun readMp4Lyrics(raf: RandomAccessFile): String? {
        val maxRead = raf.length().coerceAtMost(4_000_000L).toInt()
        raf.seek(0L)
        val data = ByteArray(maxRead)
        raf.readFully(data)
        val content = String(data, Charsets.ISO_8859_1)

        val customMarker = WORD_LYRICS_FIELD
        val customIdx = content.indexOf(customMarker, ignoreCase = true)
        if (customIdx >= 0) {
            val dataIdx = content.indexOf("data", customIdx)
            if (dataIdx in customIdx..(customIdx + 128)) {
                val lenPos = dataIdx - 4
                if (lenPos >= 0 && dataIdx + 12 <= data.size) {
                    val atomSize = ByteBuffer.wrap(data, lenPos, 4).order(ByteOrder.BIG_ENDIAN).int
                    val payloadStart = dataIdx + 12
                    val payloadLen = (atomSize - 16).coerceIn(0, data.size - payloadStart)
                    if (payloadLen > 0) {
                        val str = String(data, payloadStart, payloadLen, Charsets.UTF_8).trim()
                        if (str.isNotEmpty()) return str
                    }
                }
            }
        }

        val lyrMarker = "\u00A9lyr"
        val lyrIdx = content.indexOf(lyrMarker)
        if (lyrIdx >= 0) {
            val dataIdx = content.indexOf("data", lyrIdx)
            if (dataIdx in lyrIdx..(lyrIdx + 64)) {
                val lenPos = dataIdx - 4
                if (lenPos >= 0 && dataIdx + 12 <= data.size) {
                    val atomSize = ByteBuffer.wrap(data, lenPos, 4).order(ByteOrder.BIG_ENDIAN).int
                    val payloadStart = dataIdx + 12
                    val payloadLen = (atomSize - 16).coerceIn(0, data.size - payloadStart)
                    if (payloadLen > 0) {
                        val str = String(data, payloadStart, payloadLen, Charsets.UTF_8).trim()
                        if (str.isNotEmpty()) return str
                    }
                }
            }
        }
        return null
    }

    private fun readId3v2Lyrics(raf: RandomAccessFile): String? {
        raf.seek(0L)
        val header = ByteArray(10)
        raf.readFully(header)
        val version = header[3].toInt() and 0xFF
        val tagSize = ((header[6].toInt() and 0x7F) shl 21) or
            ((header[7].toInt() and 0x7F) shl 14) or
            ((header[8].toInt() and 0x7F) shl 7) or
            (header[9].toInt() and 0x7F)
        val limit = (10 + tagSize).toLong().coerceAtMost(raf.length()).coerceAtMost(2_000_000L)
        var enhanced: String? = null
        var plain: String? = null

        while (raf.filePointer + 10L <= limit) {
            val fHeader = ByteArray(10)
            raf.readFully(fHeader)
            val frameId = String(fHeader, 0, 4, Charsets.ISO_8859_1)
            if (!frameId.all { it in 'A'..'Z' || it in '0'..'9' }) break
            val frameSize = if (version == 4) {
                ((fHeader[4].toInt() and 0x7F) shl 21) or
                    ((fHeader[5].toInt() and 0x7F) shl 14) or
                    ((fHeader[6].toInt() and 0x7F) shl 7) or
                    (fHeader[7].toInt() and 0x7F)
            } else {
                ByteBuffer.wrap(fHeader, 4, 4).order(ByteOrder.BIG_ENDIAN).int
            }
            if (frameSize <= 0 || raf.filePointer + frameSize > limit) break
            val body = ByteArray(frameSize)
            raf.readFully(body)

            if (frameId == "TXXX" || frameId == "USLT") {
                val decoded = decodeId3TextPayload(body)
                if (frameId == "TXXX" && decoded.startsWith(WORD_LYRICS_FIELD, ignoreCase = true)) {
                    val after = decoded.substringAfter(WORD_LYRICS_FIELD).trimStart('\u0000', '=', ' ')
                    if (after.isNotEmpty()) enhanced = after
                } else if (frameId == "USLT" && decoded.isNotBlank() && plain == null) {
                    plain = decoded
                }
            }
        }
        return enhanced ?: plain
    }

    private fun decodeId3TextPayload(body: ByteArray): String {
        if (body.isEmpty()) return ""
        val enc = body[0].toInt() and 0xFF
        val charset = when (enc) {
            1 -> Charsets.UTF_16
            2 -> Charsets.UTF_16BE
            3 -> Charsets.UTF_8
            else -> Charsets.ISO_8859_1
        }
        return String(body, 1, body.size - 1, charset)
            .replace("\u0000", " ")
            .trim()
    }

    private fun readMatroskaLyrics(raf: RandomAccessFile): String? {
        val len = raf.length().coerceAtMost(2_000_000L).toInt()
        raf.seek(0L)
        val bytes = ByteArray(len)
        raf.readFully(bytes)
        val raw = String(bytes, Charsets.ISO_8859_1)
        for (tag in listOf(WORD_LYRICS_FIELD, "LYRICS")) {
            val idx = raw.indexOf(tag)
            if (idx >= 0) {
                val windowEnd = (idx + 65536).coerceAtMost(bytes.size)
                val slice = String(bytes, idx + tag.length, windowEnd - (idx + tag.length), Charsets.UTF_8)
                val match = Regex("""(\[\d{1,2}:\d{2}(?:[.,]\d{1,3})?][^\u0000-\u0008\u000B-\u001F]+)""").find(slice)
                if (match != null) return match.value.trim()
            }
        }
        return null
    }

    private fun readId3OrRetrieverLyrics(context: Context, uriOrPath: String): String? {
        val retriever = MediaMetadataRetriever()
        return try {
            if (uriOrPath.startsWith("content://")) {
                retriever.setDataSource(context, Uri.parse(uriOrPath))
            } else {
                retriever.setDataSource(uriOrPath.removePrefix("file://"))
            }
            // MediaMetadataRetriever constant 1000 is sometimes used for lyrics on some OEMs
            retriever.extractMetadata(1000)?.takeIf { it.isNotBlank() }
        } catch (_: Exception) {
            null
        } finally {
            runCatching { retriever.release() }
        }
    }
}
