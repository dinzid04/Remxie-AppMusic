package com.example.data.local

import kotlinx.coroutines.flow.Flow

interface RecentTrackDao {
    fun observeRecentTracks(): Flow<List<RecentTrackEntity>>

    suspend fun upsertRecentTrack(track: RecentTrackEntity)

    suspend fun deleteRecentById(trackId: String)

    suspend fun clearAllRecents()

    fun observeFavoriteTracks(): Flow<List<FavoriteTrackEntity>>

    fun observeFavoriteIds(): Flow<List<String>>

    suspend fun insertFavorite(track: FavoriteTrackEntity)

    suspend fun deleteFavoriteById(trackId: String)

    // Library Playlists
    fun observeLibraryPlaylists(): Flow<List<LibraryPlaylistEntity>>

    fun observeAllPlaylistTracks(): Flow<List<PlaylistTrackEntity>>

    suspend fun getTracksForPlaylist(playlistId: String): List<PlaylistTrackEntity>

    suspend fun upsertLibraryPlaylist(playlist: LibraryPlaylistEntity)

    suspend fun updatePlaylistPin(playlistId: String, isPinned: Boolean)

    suspend fun renameLibraryPlaylist(playlistId: String, newTitle: String)

    suspend fun updatePlaylistThumbnail(playlistId: String, thumbnailUrl: String)

    suspend fun deleteLibraryPlaylist(playlistId: String)

    suspend fun insertPlaylistTrack(track: PlaylistTrackEntity)

    suspend fun insertPlaylistTracks(tracks: List<PlaylistTrackEntity>)

    suspend fun replacePlaylistTracks(playlistId: String, tracks: List<PlaylistTrackEntity>)

    suspend fun removeTrackFromPlaylist(playlistId: String, trackId: String)

    suspend fun deleteAllTracksFromPlaylist(playlistId: String)

    // Offline Downloaded Tracks
    fun observeDownloadedTracks(): Flow<List<DownloadedTrackEntity>>

    suspend fun getDownloadedTrackById(trackId: String): DownloadedTrackEntity?

    suspend fun upsertDownloadedTrack(track: DownloadedTrackEntity)

    suspend fun deleteDownloadedTrackById(trackId: String)

    // Subscribed Artists
    fun observeSubscribedArtists(): Flow<List<SubscribedArtistEntity>>

    suspend fun upsertSubscribedArtist(artist: SubscribedArtistEntity)

    suspend fun deleteSubscribedArtistById(artistId: String)
}
