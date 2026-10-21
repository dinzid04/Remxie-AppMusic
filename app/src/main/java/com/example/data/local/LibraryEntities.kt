package com.example.data.local

import com.example.data.model.DownloadedTrackItem
import com.example.data.model.MusicItem
import com.example.data.model.PlaylistPrivacy
import com.example.data.model.PlaylistSourceType
import com.example.data.model.SubscribedArtistItem
import com.example.data.model.UserLibraryPlaylist

data class LibraryPlaylistEntity(
    val id: String,
    val title: String,
    val subtitle: String,
    val thumbnailUrl: String,
    val privacy: String = PlaylistPrivacy.PRIVATE.name,
    val isPinned: Boolean = false,
    val sourceType: String = PlaylistSourceType.CUSTOM.name,
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toDomain(tracks: List<MusicItem>): UserLibraryPlaylist {
        val parsedPrivacy = try {
            PlaylistPrivacy.valueOf(privacy)
        } catch (_: Exception) {
            PlaylistPrivacy.PRIVATE
        }
        val parsedSource = try {
            PlaylistSourceType.valueOf(sourceType)
        } catch (_: Exception) {
            PlaylistSourceType.CUSTOM
        }
        return UserLibraryPlaylist(
            id = id,
            title = title,
            subtitle = subtitle,
            thumbnailUrl = thumbnailUrl,
            privacy = parsedPrivacy,
            isPinned = isPinned,
            sourceType = parsedSource,
            createdAt = createdAt,
            tracks = tracks
        )
    }
}

data class PlaylistTrackEntity(
    val entryKey: String,
    val playlistId: String,
    val trackId: String,
    val title: String,
    val subtitle: String,
    val thumbnailUrl: String,
    val videoId: String?,
    val duration: String?,
    val addedAt: Long = System.currentTimeMillis()
) {
    fun toMusicItem(): MusicItem = MusicItem(
        id = trackId,
        title = title,
        subtitle = subtitle,
        thumbnailUrl = thumbnailUrl,
        videoId = videoId ?: trackId,
        playlistId = playlistId,
        type = "song",
        duration = duration
    )

    companion object {
        fun fromMusicItem(playlistId: String, item: MusicItem, orderOffset: Long = 0L): PlaylistTrackEntity {
            val effectiveTrackId = item.videoId ?: item.id
            return PlaylistTrackEntity(
                entryKey = "${playlistId}_$effectiveTrackId",
                playlistId = playlistId,
                trackId = effectiveTrackId,
                title = item.title,
                subtitle = item.subtitle,
                thumbnailUrl = item.highResThumbnail,
                videoId = item.videoId ?: item.id,
                duration = item.duration,
                addedAt = System.currentTimeMillis() + orderOffset
            )
        }
    }
}

data class DownloadedTrackEntity(
    val id: String,
    val videoId: String?,
    val title: String,
    val subtitle: String,
    val thumbnailUrl: String,
    val duration: String?,
    val localFilePath: String,
    val mediaStoreUri: String? = null,
    val fileSizeBytes: Long = 0L,
    val downloadedAt: Long = System.currentTimeMillis()
) {
    fun toDomain(): DownloadedTrackItem = DownloadedTrackItem(
        id = id,
        videoId = videoId,
        title = title,
        subtitle = subtitle,
        thumbnailUrl = thumbnailUrl,
        duration = duration,
        localFilePath = localFilePath,
        mediaStoreUri = mediaStoreUri,
        fileSizeBytes = fileSizeBytes,
        downloadedAt = downloadedAt
    )
}

data class SubscribedArtistEntity(
    val id: String,
    val name: String,
    val avatarUrl: String,
    val subscribersText: String = "Artis",
    val subscribedAt: Long = System.currentTimeMillis()
) {
    fun toDomain(): SubscribedArtistItem = SubscribedArtistItem(
        id = id,
        name = name,
        avatarUrl = avatarUrl,
        subscribersText = subscribersText,
        subscribedAt = subscribedAt
    )

    companion object {
        fun fromDomain(item: SubscribedArtistItem): SubscribedArtistEntity = SubscribedArtistEntity(
            id = item.id,
            name = item.name,
            avatarUrl = item.avatarUrl,
            subscribersText = item.subscribersText,
            subscribedAt = item.subscribedAt
        )
    }
}
