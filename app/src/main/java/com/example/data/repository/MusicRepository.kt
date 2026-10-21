package com.example.data.repository

import com.example.data.local.FavoriteTrackEntity
import com.example.data.local.LibraryPlaylistEntity
import com.example.data.local.PlaylistTrackEntity
import com.example.data.local.RecentTrackDao
import com.example.data.local.RecentTrackEntity
import com.example.data.local.SubscribedArtistEntity
import com.example.data.model.ArtistProfileDetail
import com.example.data.model.AudioStreamInfo
import com.example.data.model.DownloadedTrackItem
import com.example.data.model.HomeSection
import com.example.data.model.IntegrationPlatform
import com.example.data.model.LyricsData
import com.example.data.model.LyricsProvider
import com.example.data.model.MusicItem
import com.example.data.model.PlaylistDetail
import com.example.data.model.PlaylistPrivacy
import com.example.data.model.PlaylistSourceType
import com.example.data.model.SearchCategorySection
import com.example.data.model.SearchSuggestionPayload
import com.example.data.model.SpotifyImportProgressState
import com.example.data.model.SubscribedArtistItem
import com.example.data.model.UserLibraryPlaylist
import com.example.data.remote.CuratedHomeFallback
import com.example.data.remote.LrcRedService
import com.example.data.remote.PlaylistIntegrationService
import com.example.data.remote.YtMusicScraper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class MusicRepository(
    private val recentTrackDao: RecentTrackDao,
    private val scraper: YtMusicScraper = YtMusicScraper(),
    private val lrcRedService: LrcRedService = LrcRedService(),
    private val integrationService: PlaylistIntegrationService = PlaylistIntegrationService(scraper)
) {

    val recentTracksFlow: Flow<List<MusicItem>> = recentTrackDao.observeRecentTracks()
        .map { entities -> entities.map { it.toMusicItem() } }

    val favoriteIdsFlow: Flow<Set<String>> = recentTrackDao.observeFavoriteIds()
        .map { list -> list.toSet() }

    val favoriteTracksFlow: Flow<List<MusicItem>> = recentTrackDao.observeFavoriteTracks()
        .map { list -> list.map { it.toMusicItem() } }

    val libraryPlaylistsFlow: Flow<List<UserLibraryPlaylist>> = combine(
        recentTrackDao.observeLibraryPlaylists(),
        recentTrackDao.observeAllPlaylistTracks()
    ) { playlists, allTracks ->
        val tracksByPlaylist = allTracks.groupBy { it.playlistId }
        playlists.map { entity ->
            val tracks = tracksByPlaylist[entity.id]?.map { it.toMusicItem() } ?: emptyList()
            entity.toDomain(tracks)
        }
    }

    val downloadedTracksFlow: Flow<List<DownloadedTrackItem>> = recentTrackDao.observeDownloadedTracks()
        .map { list -> list.map { it.toDomain() } }

    val downloadedTrackIdsFlow: Flow<Set<String>> = downloadedTracksFlow
        .map { list ->
            buildSet {
                list.forEach { item ->
                    add(item.id)
                    if (!item.videoId.isNullOrBlank()) {
                        add(item.videoId)
                    }
                }
            }
        }

    val subscribedArtistsFlow: Flow<List<SubscribedArtistItem>> = recentTrackDao.observeSubscribedArtists()
        .map { list -> list.map { it.toDomain() } }

    suspend fun upsertSubscribedArtist(artist: SubscribedArtistItem) {
        recentTrackDao.upsertSubscribedArtist(SubscribedArtistEntity.fromDomain(artist))
    }

    suspend fun deleteSubscribedArtist(browseId: String) {
        recentTrackDao.deleteSubscribedArtistById(browseId)
    }

    suspend fun createCustomPlaylist(
        title: String,
        privacy: PlaylistPrivacy = PlaylistPrivacy.PRIVATE,
        initialTrack: MusicItem? = null,
        ownerUsername: String = "user"
    ): UserLibraryPlaylist {
        val cleanTitle = title.trim().ifBlank { "Playlist Baru" }
        val id = "custom_pl_${System.currentTimeMillis()}"
        val subtitle = ownerUsername.ifBlank { "user" }
        val thumb = initialTrack?.highResThumbnail.orEmpty()
        val entity = LibraryPlaylistEntity(
            id = id,
            title = cleanTitle,
            subtitle = subtitle,
            thumbnailUrl = thumb,
            privacy = privacy.name,
            isPinned = false,
            sourceType = PlaylistSourceType.USER_CREATED.name,
            createdAt = System.currentTimeMillis()
        )
        recentTrackDao.upsertLibraryPlaylist(entity)
        val initialTracks = if (initialTrack != null) {
            val trackEntity = PlaylistTrackEntity.fromMusicItem(id, initialTrack)
            recentTrackDao.insertPlaylistTrack(trackEntity)
            listOf(initialTrack)
        } else {
            emptyList()
        }
        return entity.toDomain(initialTracks)
    }

    suspend fun savePlaylistToLibrary(
        playlistDetail: PlaylistDetail,
        sourceType: PlaylistSourceType,
        platformBadge: String = ""
    ) {
        val playlistId = playlistDetail.id.ifBlank { playlistDetail.browseId }
        val effectiveThumb = playlistDetail.thumbnailUrl.ifBlank {
            playlistDetail.tracks.firstOrNull()?.highResThumbnail.orEmpty()
        }
        val effectiveSource = when {
            platformBadge.contains("Spotify", ignoreCase = true) -> PlaylistSourceType.SPOTIFY_IMPORT
            platformBadge.contains("YouTube", ignoreCase = true) -> PlaylistSourceType.YOUTUBE_IMPORT
            else -> sourceType
        }
        val entity = LibraryPlaylistEntity(
            id = playlistId,
            title = playlistDetail.title,
            subtitle = playlistDetail.subtitle.ifBlank { "user" },
            thumbnailUrl = effectiveThumb,
            privacy = PlaylistPrivacy.PUBLIC.name,
            isPinned = false,
            sourceType = effectiveSource.name,
            createdAt = System.currentTimeMillis()
        )
        recentTrackDao.upsertLibraryPlaylist(entity)
        if (playlistDetail.tracks.isNotEmpty()) {
            val trackEntities = playlistDetail.tracks.mapIndexed { idx, track ->
                PlaylistTrackEntity.fromMusicItem(playlistId, track, orderOffset = idx.toLong())
            }
            recentTrackDao.insertPlaylistTracks(trackEntities)
        }
    }

    suspend fun saveOrImportPlaylistToLibrary(
        item: MusicItem,
        sourceType: PlaylistSourceType,
        tracks: List<MusicItem> = item.previewTracks,
        ownerUsername: String = "user"
    ) {
        val playlistId = item.browseId?.takeIf { it.isNotBlank() } ?: item.id
        val effectiveThumb = item.highResThumbnail.ifBlank {
            tracks.firstOrNull()?.highResThumbnail.orEmpty()
        }
        val subtitle = when (sourceType) {
            PlaylistSourceType.SPOTIFY_IMPORT -> "Spotify • $ownerUsername"
            PlaylistSourceType.YOUTUBE_IMPORT -> "YouTube Music • $ownerUsername"
            PlaylistSourceType.SAVED_COMMUNITY -> item.cleanArtist.ifBlank { ownerUsername }
            else -> ownerUsername
        }
        val entity = LibraryPlaylistEntity(
            id = playlistId,
            title = item.title,
            subtitle = subtitle,
            thumbnailUrl = effectiveThumb,
            privacy = PlaylistPrivacy.PUBLIC.name,
            isPinned = false,
            sourceType = sourceType.name,
            createdAt = System.currentTimeMillis()
        )
        recentTrackDao.upsertLibraryPlaylist(entity)
        if (tracks.isNotEmpty()) {
            val trackEntities = tracks.mapIndexed { idx, track ->
                PlaylistTrackEntity.fromMusicItem(playlistId, track, orderOffset = idx.toLong())
            }
            recentTrackDao.insertPlaylistTracks(trackEntities)
        }
    }

    suspend fun togglePinPlaylist(playlistId: String, currentPinned: Boolean) {
        recentTrackDao.updatePlaylistPin(playlistId, !currentPinned)
    }

    suspend fun setPlaylistPinned(playlistId: String, isPinned: Boolean) {
        recentTrackDao.updatePlaylistPin(playlistId, isPinned)
    }

    suspend fun renamePlaylist(playlistId: String, newTitle: String) {
        val clean = newTitle.trim()
        if (clean.isNotBlank()) {
            recentTrackDao.renameLibraryPlaylist(playlistId, clean)
        }
    }

    suspend fun renameLibraryPlaylist(playlistId: String, newTitle: String) {
        renamePlaylist(playlistId, newTitle)
    }

    suspend fun deletePlaylist(playlistId: String) {
        recentTrackDao.deleteAllTracksFromPlaylist(playlistId)
        recentTrackDao.deleteLibraryPlaylist(playlistId)
    }

    suspend fun deleteLibraryPlaylist(playlistId: String) {
        deletePlaylist(playlistId)
    }

    suspend fun reorderPlaylistTracks(playlistId: String, orderedTracks: List<MusicItem>) {
        val entities = orderedTracks.mapIndexed { idx, track ->
            PlaylistTrackEntity.fromMusicItem(
                playlistId = playlistId,
                item = track,
                orderOffset = idx.toLong()
            )
        }
        recentTrackDao.replacePlaylistTracks(playlistId, entities)
        orderedTracks.firstOrNull()?.highResThumbnail?.takeIf { it.isNotBlank() }?.let { firstThumb ->
            recentTrackDao.updatePlaylistThumbnail(playlistId, firstThumb)
        }
    }

    suspend fun addTrackToPlaylist(playlistId: String, track: MusicItem) {
        val existingTracks = recentTrackDao.getTracksForPlaylist(playlistId)
        val entity = PlaylistTrackEntity.fromMusicItem(
            playlistId = playlistId,
            item = track,
            orderOffset = existingTracks.size.toLong()
        )
        recentTrackDao.insertPlaylistTrack(entity)
        if (existingTracks.isEmpty() && track.highResThumbnail.isNotBlank()) {
            recentTrackDao.updatePlaylistThumbnail(playlistId, track.highResThumbnail)
        }
    }

    suspend fun addTrackToLibraryPlaylist(playlistId: String, track: MusicItem) {
        addTrackToPlaylist(playlistId, track)
    }

    suspend fun removeTrackFromPlaylist(playlistId: String, trackId: String) {
        recentTrackDao.removeTrackFromPlaylist(playlistId, trackId)
    }

    suspend fun removeTrackFromLibraryPlaylist(playlistId: String, trackId: String) {
        removeTrackFromPlaylist(playlistId, trackId)
    }

    suspend fun fetchHomeSections(): List<HomeSection> {
        return try {
            scraper.getHome(hl = "id", gl = "ID", depth = 5)
        } catch (_: Exception) {
            CuratedHomeFallback.getRequiredHomeSectionsFallback()
        }
    }

    fun getQuickPicksDefault(): List<MusicItem> {
        return CuratedHomeFallback.getQuickPicksFallback()
    }

    private fun ensureTracksHaveThumbnails(tracks: List<MusicItem>, fallbackCover: String): List<MusicItem> {
        return tracks.map { t ->
            val upgraded = MusicItem.upgradeThumbnailUrl(t.thumbnailUrl, t.videoId, MusicItem.Quality.HIGH)
            val validThumb = when {
                upgraded.isNotBlank() -> upgraded
                !t.videoId.isNullOrBlank() && !t.videoId.startsWith("UC") && !t.videoId.startsWith("VL") && !t.videoId.startsWith("MPRE") ->
                    "https://i.ytimg.com/vi/${t.videoId}/hqdefault.jpg"
                else -> fallbackCover
            }
            if (validThumb != t.thumbnailUrl && validThumb.isNotBlank()) {
                t.copy(thumbnailUrl = validThumb)
            } else {
                t
            }
        }
    }

    suspend fun fetchPlaylistOrAlbumDetail(item: MusicItem): PlaylistDetail {
        val targetId = item.browseId?.takeIf { it.isNotBlank() } ?: item.id
        val localTracks = recentTrackDao.getTracksForPlaylist(targetId).map { it.toMusicItem() }
        if (localTracks.isNotEmpty() || targetId.startsWith("custom_pl_")) {
            val rawEffectiveTracks = localTracks.ifEmpty { item.previewTracks }
            val cover = item.ultraResThumbnail.ifBlank {
                rawEffectiveTracks.firstOrNull()?.ultraResThumbnail.orEmpty()
            }
            val effectiveTracks = ensureTracksHaveThumbnails(rawEffectiveTracks, cover)
            return PlaylistDetail(
                browseId = targetId,
                title = item.title,
                authorSubtitle = item.subtitle.substringBefore("•").trim().ifBlank { "user" },
                metaSubtitle = "PLAYLIST • ${effectiveTracks.size} TRACKS",
                description = "Disimpan di Perpustakaan Anda",
                thumbnailUrl = cover,
                totalTracks = effectiveTracks.size,
                tracks = effectiveTracks
            )
        }
        if (item.previewTracks.isNotEmpty() && (
                item.id.startsWith("spotify_") ||
                item.id.startsWith("ytm_sync_") ||
                item.id.startsWith("feat_pl_") ||
                item.id.startsWith("artist_radio_")
            )
        ) {
            val cover = item.ultraResThumbnail.ifBlank {
                item.previewTracks.firstOrNull()?.ultraResThumbnail.orEmpty()
            }
            val enrichedPreview = ensureTracksHaveThumbnails(item.previewTracks, cover)
            return CuratedHomeFallback.getPlaylistFallback(item).copy(
                title = item.title,
                authorSubtitle = item.subtitle.ifBlank { "user" },
                metaSubtitle = "PLAYLIST • ${enrichedPreview.size} TRACKS",
                thumbnailUrl = cover,
                totalTracks = enrichedPreview.size,
                tracks = enrichedPreview
            )
        }
        return try {
            val detail = scraper.getPlaylistOrAlbumDetail(item, hl = "id", gl = "ID")
            val baseCover = detail.thumbnailUrl.ifBlank { item.ultraResThumbnail }
            if (detail.tracks.isEmpty() && item.previewTracks.isNotEmpty()) {
                val enriched = ensureTracksHaveThumbnails(item.previewTracks, baseCover)
                detail.copy(
                    thumbnailUrl = baseCover,
                    tracks = enriched,
                    totalTracks = enriched.size
                )
            } else {
                val enriched = ensureTracksHaveThumbnails(detail.tracks, baseCover)
                detail.copy(
                    thumbnailUrl = baseCover.ifBlank { enriched.firstOrNull()?.highResThumbnail.orEmpty() },
                    tracks = enriched
                )
            }
        } catch (_: Exception) {
            val fb = CuratedHomeFallback.getPlaylistFallback(item)
            val baseCover = fb.thumbnailUrl.ifBlank { item.ultraResThumbnail }
            fb.copy(
                thumbnailUrl = baseCover,
                tracks = ensureTracksHaveThumbnails(fb.tracks, baseCover)
            )
        }
    }

    suspend fun fetchArtistProfile(seedItem: MusicItem): ArtistProfileDetail {
        val rawName = if (seedItem.type == "artist" || seedItem.browseId?.startsWith("UC") == true) {
            seedItem.title
        } else {
            seedItem.cleanArtist.ifBlank { seedItem.title }
        }
        return try {
            scraper.getArtistProfile(seedItem, hl = "id", gl = "ID")
        } catch (_: Exception) {
            CuratedHomeFallback.getArtistProfileFallback(rawName, seedItem)
        }
    }

    suspend fun resolveArtistAvatar(artistName: String, browseId: String? = null): String? {
        return scraper.fetchArtistAvatarUrl(artistName, browseId)
    }

    suspend fun connectSpotifyAndImport(
        accountInput: String,
        tokenOrUrl: String,
        onProgress: ((SpotifyImportProgressState) -> Unit)? = null
    ): PlaylistIntegrationService.IntegrationSyncResult {
        val res = integrationService.connectAndImportSpotify(accountInput, tokenOrUrl, onProgress)
        res.importedPlaylists.forEach { pl ->
            saveOrImportPlaylistToLibrary(pl, PlaylistSourceType.SPOTIFY_IMPORT, pl.previewTracks)
        }
        return res
    }

    suspend fun connectYouTubeMusicAndImport(
        googleEmail: String,
        tokenOrUrl: String,
        onProgress: ((SpotifyImportProgressState) -> Unit)? = null
    ): PlaylistIntegrationService.IntegrationSyncResult {
        val res = integrationService.connectAndImportYouTubeMusic(googleEmail, tokenOrUrl, onProgress)
        res.importedPlaylists.forEach { pl ->
            saveOrImportPlaylistToLibrary(pl, PlaylistSourceType.YOUTUBE_IMPORT, pl.previewTracks)
        }
        return res
    }

    suspend fun importPlaylistByUrl(
        platform: IntegrationPlatform,
        urlOrId: String,
        onProgress: ((SpotifyImportProgressState) -> Unit)? = null
    ): MusicItem? {
        val imported = integrationService.importPlaylistByUrl(platform, urlOrId, onProgress)
        if (imported != null) {
            val src = if (platform == IntegrationPlatform.SPOTIFY) {
                PlaylistSourceType.SPOTIFY_IMPORT
            } else {
                PlaylistSourceType.YOUTUBE_IMPORT
            }
            saveOrImportPlaylistToLibrary(imported, src, imported.previewTracks)
        }
        return imported
    }

    suspend fun fetchSearchSuggestions(query: String): SearchSuggestionPayload {
        return try {
            scraper.getSearchSuggestions(query, hl = "id", gl = "ID")
        } catch (_: Exception) {
            SearchSuggestionPayload()
        }
    }

    suspend fun searchAllCategories(query: String): List<SearchCategorySection> {
        return try {
            scraper.searchAll(query, hl = "id", gl = "ID")
        } catch (_: Exception) {
            emptyList()
        }
    }

    suspend fun searchByFilter(query: String, filter: String): List<MusicItem> {
        return try {
            scraper.searchByFilter(query, filter = filter, hl = "id", gl = "ID")
        } catch (_: Exception) {
            emptyList()
        }
    }

    suspend fun recordPlayedTrack(item: MusicItem) {
        recentTrackDao.upsertRecentTrack(RecentTrackEntity.fromMusicItem(item))
    }

    suspend fun removeRecentTrack(id: String) {
        recentTrackDao.deleteRecentById(id)
    }

    suspend fun clearAllRecents() {
        recentTrackDao.clearAllRecents()
    }

    suspend fun toggleFavoriteTrack(item: MusicItem): Boolean {
        val currentIds = recentTrackDao.observeFavoriteIds().first()
        val effectiveId = item.videoId ?: item.id
        return if (currentIds.contains(effectiveId) || currentIds.contains(item.id)) {
            recentTrackDao.deleteFavoriteById(effectiveId)
            recentTrackDao.deleteFavoriteById(item.id)
            false
        } else {
            recentTrackDao.insertFavorite(FavoriteTrackEntity.fromMusicItem(item))
            true
        }
    }

    suspend fun resolveAudioStream(item: MusicItem): AudioStreamInfo? {
        return scraper.getAudioStream(item)
    }

    suspend fun resolveVideoStreamUrl(item: MusicItem): String? {
        return scraper.getVideoStreamUrl(item)
    }

    suspend fun getTrackLyrics(
        item: MusicItem,
        preferredProvider: LyricsProvider = LyricsProvider.LRC_RED
    ): LyricsData {
        val lrcRedData = try {
            lrcRedService.fetchSyncedLyrics(item)
        } catch (_: Exception) {
            null
        }

        if (preferredProvider == LyricsProvider.LRC_RED && lrcRedData != null) {
            return lrcRedData
        }

        val ytMusicData = try {
            scraper.getLyrics(item, hl = "id", gl = "ID")
        } catch (_: Exception) {
            CuratedHomeFallback.getLyricsFallback(item, preferredProvider = preferredProvider)
        }

        if (preferredProvider == LyricsProvider.LRC_RED) {
            val lines = ytMusicData.lines.ifEmpty {
                CuratedHomeFallback.getLyricsFallback(item, LyricsProvider.LRC_RED).lines
            }
            val totalDurSec = parseDurationToSeconds(item.duration)?.toDouble()?.coerceAtLeast(120.0) ?: 210.0
            val startOffset = 8.0
            val usableSpan = (totalDurSec - startOffset - 10.0).coerceAtLeast(lines.size * 3.8)
            val perLineSpan = (usableSpan / lines.size.coerceAtLeast(1)).coerceIn(3.2, 6.0)

            val synced = lines.mapIndexed { idx, lineText ->
                val start = startOffset + idx * perLineSpan
                val end = start + (perLineSpan * 0.92)
                val tokens = Regex("\\S+\\s*").findAll(lineText).map { it.value }.toList()
                val totalChars = tokens.sumOf { it.trim().length.coerceAtLeast(1) }.toDouble().coerceAtLeast(1.0)
                val wordSpan = (end - start - 0.12).coerceAtLeast(0.45)
                var cursor = start
                val words = tokens.map { token ->
                    val dur = (wordSpan * (token.trim().length.coerceAtLeast(1) / totalChars)).coerceAtLeast(0.08)
                    val wStart = cursor
                    val wEnd = (wStart + dur).coerceAtMost(end)
                    cursor = wEnd
                    com.example.data.model.LyricSyllable(
                        text = token,
                        beginSec = wStart,
                        endSec = wEnd
                    )
                }
                com.example.data.model.LyricLine(
                    text = lineText,
                    beginSec = start,
                    endSec = end,
                    words = words
                )
            }
            return LyricsData(
                videoId = item.videoId ?: item.id,
                lines = lines,
                syncedLines = synced,
                isSynced = true,
                source = LyricsProvider.LRC_RED.creditLabel,
                provider = LyricsProvider.LRC_RED,
                hdCoverUrl = lrcRedData?.hdCoverUrl,
                accentColorHex = lrcRedData?.accentColorHex,
                songwriters = lrcRedData?.songwriters ?: emptyList()
            )
        }

        return if (lrcRedData != null) {
            ytMusicData.copy(
                hdCoverUrl = lrcRedData.hdCoverUrl,
                accentColorHex = lrcRedData.accentColorHex,
                songwriters = lrcRedData.songwriters
            )
        } else {
            ytMusicData
        }
    }

    private fun parseDurationToSeconds(durationStr: String?): Int? {
        if (durationStr.isNullOrBlank()) return null
        val parts = durationStr.trim().split(":").mapNotNull { it.trim().toIntOrNull() }
        return when (parts.size) {
            2 -> parts[0] * 60 + parts[1]
            3 -> parts[0] * 3600 + parts[1] * 60 + parts[2]
            else -> null
        }
    }

    suspend fun getTrackQueue(item: MusicItem): List<MusicItem> {
        return try {
            scraper.getQueue(item, hl = "id", gl = "ID")
        } catch (_: Exception) {
            CuratedHomeFallback.getAutoPlayQueueFallback(item)
        }
    }
}
