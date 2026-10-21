package com.example.data.local

import com.example.data.model.MusicItem

data class FavoriteTrackEntity(
    val id: String,
    val title: String,
    val subtitle: String,
    val thumbnailUrl: String,
    val videoId: String?,
    val browseId: String?,
    val duration: String?,
    val likedAt: Long = System.currentTimeMillis()
) {
    fun toMusicItem(): MusicItem = MusicItem(
        id = id,
        title = title,
        subtitle = subtitle,
        thumbnailUrl = thumbnailUrl,
        videoId = videoId ?: id,
        browseId = browseId,
        type = "song",
        duration = duration
    )

    companion object {
        fun fromMusicItem(item: MusicItem): FavoriteTrackEntity {
            val effectiveId = item.videoId ?: item.id
            return FavoriteTrackEntity(
                id = effectiveId,
                title = item.title,
                subtitle = item.subtitle,
                thumbnailUrl = item.highResThumbnail,
                videoId = item.videoId,
                browseId = item.browseId,
                duration = item.duration,
                likedAt = System.currentTimeMillis()
            )
        }
    }
}
