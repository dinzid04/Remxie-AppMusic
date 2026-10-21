package com.example.data.local

import com.example.data.model.MusicItem

data class RecentTrackEntity(
    val id: String,
    val title: String,
    val subtitle: String,
    val thumbnailUrl: String,
    val videoId: String?,
    val browseId: String?,
    val type: String,
    val playedAt: Long = System.currentTimeMillis(),
    val playCount: Int = 1
) {
    fun toMusicItem(): MusicItem = MusicItem(
        id = id,
        title = title,
        subtitle = subtitle,
        thumbnailUrl = thumbnailUrl,
        videoId = videoId,
        browseId = browseId,
        type = type,
        playCount = playCount
    )

    companion object {
        fun fromMusicItem(item: MusicItem): RecentTrackEntity = RecentTrackEntity(
            id = item.videoId ?: item.id,
            title = item.title,
            subtitle = item.subtitle,
            thumbnailUrl = item.thumbnailUrl,
            videoId = item.videoId ?: item.id,
            browseId = item.browseId,
            type = item.type,
            playedAt = System.currentTimeMillis(),
            playCount = item.playCount.coerceAtLeast(1)
        )
    }
}
