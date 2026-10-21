package com.example.data.local

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class AppDatabase private constructor(context: Context) {

    private val sqliteHelper = BitChordSqliteHelper(context.applicationContext)
    private val daoInstance: RecentTrackDao = SqliteRecentTrackDao(sqliteHelper)

    fun recentTrackDao(): RecentTrackDao = daoInstance

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AppDatabase(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}

private class BitChordSqliteHelper(context: Context) : SQLiteOpenHelper(
    context,
    "ytm_listen_now.db",
    null,
    4
) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS recent_tracks (
                id TEXT PRIMARY KEY NOT NULL,
                title TEXT NOT NULL,
                subtitle TEXT NOT NULL,
                thumbnailUrl TEXT NOT NULL,
                videoId TEXT,
                browseId TEXT,
                type TEXT NOT NULL,
                playedAt INTEGER NOT NULL,
                playCount INTEGER NOT NULL DEFAULT 1
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS favorite_tracks (
                id TEXT PRIMARY KEY NOT NULL,
                title TEXT NOT NULL,
                subtitle TEXT NOT NULL,
                thumbnailUrl TEXT NOT NULL,
                videoId TEXT,
                browseId TEXT,
                type TEXT NOT NULL,
                duration TEXT,
                favoritedAt INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS library_playlists (
                id TEXT PRIMARY KEY NOT NULL,
                title TEXT NOT NULL,
                subtitle TEXT NOT NULL,
                thumbnailUrl TEXT NOT NULL,
                privacy TEXT NOT NULL,
                isPinned INTEGER NOT NULL,
                sourceType TEXT NOT NULL,
                createdAt INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS playlist_tracks (
                entryKey TEXT PRIMARY KEY NOT NULL,
                playlistId TEXT NOT NULL,
                trackId TEXT NOT NULL,
                title TEXT NOT NULL,
                subtitle TEXT NOT NULL,
                thumbnailUrl TEXT NOT NULL,
                videoId TEXT,
                duration TEXT,
                addedAt INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS downloaded_tracks (
                id TEXT PRIMARY KEY NOT NULL,
                videoId TEXT,
                title TEXT NOT NULL,
                subtitle TEXT NOT NULL,
                thumbnailUrl TEXT NOT NULL,
                duration TEXT,
                localFilePath TEXT NOT NULL,
                mediaStoreUri TEXT,
                fileSizeBytes INTEGER NOT NULL,
                downloadedAt INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS subscribed_artists (
                id TEXT PRIMARY KEY NOT NULL,
                name TEXT NOT NULL,
                avatarUrl TEXT NOT NULL,
                subscribersText TEXT NOT NULL,
                subscribedAt INTEGER NOT NULL
            )
            """.trimIndent()
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        onCreate(db)
        try {
            db.execSQL("ALTER TABLE recent_tracks ADD COLUMN playCount INTEGER NOT NULL DEFAULT 1")
        } catch (_: Exception) {
        }
    }

    override fun onDowngrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        onCreate(db)
    }
}

private class SqliteRecentTrackDao(
    private val helper: BitChordSqliteHelper
) : RecentTrackDao {

    private val writeMutex = Mutex()

    private val recentTracksState = MutableStateFlow<List<RecentTrackEntity>>(emptyList())
    private val favoriteTracksState = MutableStateFlow<List<FavoriteTrackEntity>>(emptyList())
    private val libraryPlaylistsState = MutableStateFlow<List<LibraryPlaylistEntity>>(emptyList())
    private val playlistTracksState = MutableStateFlow<List<PlaylistTrackEntity>>(emptyList())
    private val downloadedTracksState = MutableStateFlow<List<DownloadedTrackEntity>>(emptyList())
    private val subscribedArtistsState = MutableStateFlow<List<SubscribedArtistEntity>>(emptyList())

    init {
        refreshAllSync()
    }

    private fun refreshAllSync() {
        try {
            val db = helper.writableDatabase
            helper.onCreate(db)
            recentTracksState.value = queryRecentTracks(db)
            favoriteTracksState.value = queryFavoriteTracks(db)
            libraryPlaylistsState.value = queryLibraryPlaylists(db)
            playlistTracksState.value = queryAllPlaylistTracks(db)
            downloadedTracksState.value = queryDownloadedTracks(db)
            subscribedArtistsState.value = querySubscribedArtists(db)
        } catch (_: Exception) {
        }
    }

    private fun queryRecentTracks(db: SQLiteDatabase): List<RecentTrackEntity> {
        val list = mutableListOf<RecentTrackEntity>()
        db.rawQuery(
            "SELECT id, title, subtitle, thumbnailUrl, videoId, browseId, type, playedAt, playCount FROM recent_tracks ORDER BY playedAt DESC LIMIT 50",
            null
        ).use { c ->
            val playCountIdx = c.getColumnIndex("playCount")
            while (c.moveToNext()) {
                val playCountVal = if (playCountIdx >= 0 && !c.isNull(playCountIdx)) {
                    c.getInt(playCountIdx).coerceAtLeast(1)
                } else {
                    1
                }
                list.add(
                    RecentTrackEntity(
                        id = c.getString(0),
                        title = c.getString(1),
                        subtitle = c.getString(2),
                        thumbnailUrl = c.getString(3),
                        videoId = if (c.isNull(4)) null else c.getString(4),
                        browseId = if (c.isNull(5)) null else c.getString(5),
                        type = c.getString(6),
                        playedAt = c.getLong(7),
                        playCount = playCountVal
                    )
                )
            }
        }
        return list
    }

    private fun queryFavoriteTracks(db: SQLiteDatabase): List<FavoriteTrackEntity> {
        val list = mutableListOf<FavoriteTrackEntity>()
        db.rawQuery(
            "SELECT id, title, subtitle, thumbnailUrl, videoId, browseId, type, duration, favoritedAt FROM favorite_tracks ORDER BY favoritedAt DESC",
            null
        ).use { c ->
            while (c.moveToNext()) {
                list.add(
                    FavoriteTrackEntity(
                        id = c.getString(0),
                        title = c.getString(1),
                        subtitle = c.getString(2),
                        thumbnailUrl = c.getString(3),
                        videoId = if (c.isNull(4)) null else c.getString(4),
                        browseId = if (c.isNull(5)) null else c.getString(5),
                        duration = if (c.isNull(7)) null else c.getString(7),
                        likedAt = c.getLong(8)
                    )
                )
            }
        }
        return list
    }

    private fun queryLibraryPlaylists(db: SQLiteDatabase): List<LibraryPlaylistEntity> {
        val list = mutableListOf<LibraryPlaylistEntity>()
        db.rawQuery(
            "SELECT id, title, subtitle, thumbnailUrl, privacy, isPinned, sourceType, createdAt FROM library_playlists ORDER BY isPinned DESC, createdAt DESC",
            null
        ).use { c ->
            while (c.moveToNext()) {
                list.add(
                    LibraryPlaylistEntity(
                        id = c.getString(0),
                        title = c.getString(1),
                        subtitle = c.getString(2),
                        thumbnailUrl = c.getString(3),
                        privacy = c.getString(4),
                        isPinned = c.getInt(5) != 0,
                        sourceType = c.getString(6),
                        createdAt = c.getLong(7)
                    )
                )
            }
        }
        return list
    }

    private fun queryAllPlaylistTracks(db: SQLiteDatabase): List<PlaylistTrackEntity> {
        val list = mutableListOf<PlaylistTrackEntity>()
        db.rawQuery(
            "SELECT entryKey, playlistId, trackId, title, subtitle, thumbnailUrl, videoId, duration, addedAt FROM playlist_tracks ORDER BY addedAt ASC",
            null
        ).use { c ->
            while (c.moveToNext()) {
                list.add(
                    PlaylistTrackEntity(
                        entryKey = c.getString(0),
                        playlistId = c.getString(1),
                        trackId = c.getString(2),
                        title = c.getString(3),
                        subtitle = c.getString(4),
                        thumbnailUrl = c.getString(5),
                        videoId = if (c.isNull(6)) null else c.getString(6),
                        duration = if (c.isNull(7)) null else c.getString(7),
                        addedAt = c.getLong(8)
                    )
                )
            }
        }
        return list
    }

    private fun queryDownloadedTracks(db: SQLiteDatabase): List<DownloadedTrackEntity> {
        val list = mutableListOf<DownloadedTrackEntity>()
        db.rawQuery(
            "SELECT id, videoId, title, subtitle, thumbnailUrl, duration, localFilePath, mediaStoreUri, fileSizeBytes, downloadedAt FROM downloaded_tracks ORDER BY downloadedAt DESC",
            null
        ).use { c ->
            while (c.moveToNext()) {
                list.add(
                    DownloadedTrackEntity(
                        id = c.getString(0),
                        videoId = if (c.isNull(1)) null else c.getString(1),
                        title = c.getString(2),
                        subtitle = c.getString(3),
                        thumbnailUrl = c.getString(4),
                        duration = if (c.isNull(5)) null else c.getString(5),
                        localFilePath = c.getString(6),
                        mediaStoreUri = if (c.isNull(7)) null else c.getString(7),
                        fileSizeBytes = c.getLong(8),
                        downloadedAt = c.getLong(9)
                    )
                )
            }
        }
        return list
    }

    private fun querySubscribedArtists(db: SQLiteDatabase): List<SubscribedArtistEntity> {
        val list = mutableListOf<SubscribedArtistEntity>()
        db.rawQuery(
            "SELECT id, name, avatarUrl, subscribersText, subscribedAt FROM subscribed_artists ORDER BY subscribedAt DESC",
            null
        ).use { c ->
            while (c.moveToNext()) {
                list.add(
                    SubscribedArtistEntity(
                        id = c.getString(0),
                        name = c.getString(1),
                        avatarUrl = c.getString(2),
                        subscribersText = c.getString(3),
                        subscribedAt = c.getLong(4)
                    )
                )
            }
        }
        return list
    }

    override fun observeRecentTracks(): Flow<List<RecentTrackEntity>> = recentTracksState.asStateFlow()

    override suspend fun upsertRecentTrack(track: RecentTrackEntity) = withContext(Dispatchers.IO) {
        writeMutex.withLock {
            val db = helper.writableDatabase
            var existingPlayCount = 0
            db.rawQuery(
                "SELECT playCount FROM recent_tracks WHERE id = ? OR (videoId IS NOT NULL AND videoId = ?)",
                arrayOf(track.id, track.videoId ?: track.id)
            ).use { cursor ->
                if (cursor.moveToFirst()) {
                    existingPlayCount = cursor.getInt(0).coerceAtLeast(0)
                }
            }
            val nextPlayCount = existingPlayCount + 1
            val cv = ContentValues().apply {
                put("id", track.id)
                put("title", track.title)
                put("subtitle", track.subtitle)
                put("thumbnailUrl", track.thumbnailUrl)
                put("videoId", track.videoId)
                put("browseId", track.browseId)
                put("type", track.type)
                put("playedAt", track.playedAt)
                put("playCount", nextPlayCount)
            }
            db.insertWithOnConflict("recent_tracks", null, cv, SQLiteDatabase.CONFLICT_REPLACE)
            recentTracksState.value = queryRecentTracks(db)
        }
    }

    override suspend fun deleteRecentById(trackId: String) = withContext(Dispatchers.IO) {
        writeMutex.withLock {
            val db = helper.writableDatabase
            db.delete("recent_tracks", "id = ?", arrayOf(trackId))
            recentTracksState.value = queryRecentTracks(db)
        }
    }

    override suspend fun clearAllRecents() = withContext(Dispatchers.IO) {
        writeMutex.withLock {
            val db = helper.writableDatabase
            db.delete("recent_tracks", null, null)
            recentTracksState.value = emptyList()
        }
    }

    override fun observeFavoriteTracks(): Flow<List<FavoriteTrackEntity>> = favoriteTracksState.asStateFlow()

    override fun observeFavoriteIds(): Flow<List<String>> =
        favoriteTracksState.map { list -> list.map { it.id } }

    override suspend fun insertFavorite(track: FavoriteTrackEntity) = withContext(Dispatchers.IO) {
        writeMutex.withLock {
            val db = helper.writableDatabase
            val cv = ContentValues().apply {
                put("id", track.id)
                put("title", track.title)
                put("subtitle", track.subtitle)
                put("thumbnailUrl", track.thumbnailUrl)
                put("videoId", track.videoId)
                put("browseId", track.browseId)
                put("type", "song")
                put("duration", track.duration)
                put("favoritedAt", track.likedAt)
            }
            db.insertWithOnConflict("favorite_tracks", null, cv, SQLiteDatabase.CONFLICT_REPLACE)
            favoriteTracksState.value = queryFavoriteTracks(db)
        }
    }

    override suspend fun deleteFavoriteById(trackId: String) = withContext(Dispatchers.IO) {
        writeMutex.withLock {
            val db = helper.writableDatabase
            db.delete("favorite_tracks", "id = ?", arrayOf(trackId))
            favoriteTracksState.value = queryFavoriteTracks(db)
        }
    }

    override fun observeLibraryPlaylists(): Flow<List<LibraryPlaylistEntity>> = libraryPlaylistsState.asStateFlow()

    override fun observeAllPlaylistTracks(): Flow<List<PlaylistTrackEntity>> = playlistTracksState.asStateFlow()

    override suspend fun getTracksForPlaylist(playlistId: String): List<PlaylistTrackEntity> =
        withContext(Dispatchers.IO) {
            playlistTracksState.value.filter { it.playlistId == playlistId }
        }

    override suspend fun upsertLibraryPlaylist(playlist: LibraryPlaylistEntity) = withContext(Dispatchers.IO) {
        writeMutex.withLock {
            val db = helper.writableDatabase
            val cv = ContentValues().apply {
                put("id", playlist.id)
                put("title", playlist.title)
                put("subtitle", playlist.subtitle)
                put("thumbnailUrl", playlist.thumbnailUrl)
                put("privacy", playlist.privacy)
                put("isPinned", if (playlist.isPinned) 1 else 0)
                put("sourceType", playlist.sourceType)
                put("createdAt", playlist.createdAt)
            }
            db.insertWithOnConflict("library_playlists", null, cv, SQLiteDatabase.CONFLICT_REPLACE)
            libraryPlaylistsState.value = queryLibraryPlaylists(db)
        }
    }

    override suspend fun updatePlaylistPin(playlistId: String, isPinned: Boolean) = withContext(Dispatchers.IO) {
        writeMutex.withLock {
            val db = helper.writableDatabase
            val cv = ContentValues().apply {
                put("isPinned", if (isPinned) 1 else 0)
            }
            db.update("library_playlists", cv, "id = ?", arrayOf(playlistId))
            libraryPlaylistsState.value = queryLibraryPlaylists(db)
        }
    }

    override suspend fun renameLibraryPlaylist(playlistId: String, newTitle: String) = withContext(Dispatchers.IO) {
        writeMutex.withLock {
            val db = helper.writableDatabase
            val cv = ContentValues().apply {
                put("title", newTitle)
            }
            db.update("library_playlists", cv, "id = ?", arrayOf(playlistId))
            libraryPlaylistsState.value = queryLibraryPlaylists(db)
        }
    }

    override suspend fun updatePlaylistThumbnail(playlistId: String, thumbnailUrl: String) =
        withContext(Dispatchers.IO) {
            writeMutex.withLock {
                val db = helper.writableDatabase
                val cv = ContentValues().apply {
                    put("thumbnailUrl", thumbnailUrl)
                }
                db.update("library_playlists", cv, "id = ?", arrayOf(playlistId))
                libraryPlaylistsState.value = queryLibraryPlaylists(db)
            }
        }

    override suspend fun deleteLibraryPlaylist(playlistId: String) = withContext(Dispatchers.IO) {
        writeMutex.withLock {
            val db = helper.writableDatabase
            db.delete("library_playlists", "id = ?", arrayOf(playlistId))
            libraryPlaylistsState.value = queryLibraryPlaylists(db)
        }
    }

    override suspend fun insertPlaylistTrack(track: PlaylistTrackEntity) = withContext(Dispatchers.IO) {
        writeMutex.withLock {
            val db = helper.writableDatabase
            val cv = ContentValues().apply {
                put("entryKey", track.entryKey)
                put("playlistId", track.playlistId)
                put("trackId", track.trackId)
                put("title", track.title)
                put("subtitle", track.subtitle)
                put("thumbnailUrl", track.thumbnailUrl)
                put("videoId", track.videoId)
                put("duration", track.duration)
                put("addedAt", track.addedAt)
            }
            db.insertWithOnConflict("playlist_tracks", null, cv, SQLiteDatabase.CONFLICT_REPLACE)
            playlistTracksState.value = queryAllPlaylistTracks(db)
        }
    }

    override suspend fun insertPlaylistTracks(tracks: List<PlaylistTrackEntity>) = withContext(Dispatchers.IO) {
        writeMutex.withLock {
            val db = helper.writableDatabase
            db.beginTransaction()
            try {
                for (track in tracks) {
                    val cv = ContentValues().apply {
                        put("entryKey", track.entryKey)
                        put("playlistId", track.playlistId)
                        put("trackId", track.trackId)
                        put("title", track.title)
                        put("subtitle", track.subtitle)
                        put("thumbnailUrl", track.thumbnailUrl)
                        put("videoId", track.videoId)
                        put("duration", track.duration)
                        put("addedAt", track.addedAt)
                    }
                    db.insertWithOnConflict("playlist_tracks", null, cv, SQLiteDatabase.CONFLICT_REPLACE)
                }
                db.setTransactionSuccessful()
            } finally {
                db.endTransaction()
            }
            playlistTracksState.value = queryAllPlaylistTracks(db)
        }
    }

    override suspend fun replacePlaylistTracks(playlistId: String, tracks: List<PlaylistTrackEntity>) =
        withContext(Dispatchers.IO) {
            writeMutex.withLock {
                val db = helper.writableDatabase
                db.beginTransaction()
                try {
                    db.delete("playlist_tracks", "playlistId = ?", arrayOf(playlistId))
                    for (track in tracks) {
                        val cv = ContentValues().apply {
                            put("entryKey", track.entryKey)
                            put("playlistId", track.playlistId)
                            put("trackId", track.trackId)
                            put("title", track.title)
                            put("subtitle", track.subtitle)
                            put("thumbnailUrl", track.thumbnailUrl)
                            put("videoId", track.videoId)
                            put("duration", track.duration)
                            put("addedAt", track.addedAt)
                        }
                        db.insertWithOnConflict("playlist_tracks", null, cv, SQLiteDatabase.CONFLICT_REPLACE)
                    }
                    db.setTransactionSuccessful()
                } finally {
                    db.endTransaction()
                }
                playlistTracksState.value = queryAllPlaylistTracks(db)
            }
        }

    override suspend fun removeTrackFromPlaylist(playlistId: String, trackId: String) =
        withContext(Dispatchers.IO) {
            writeMutex.withLock {
                val db = helper.writableDatabase
                db.delete("playlist_tracks", "playlistId = ? AND trackId = ?", arrayOf(playlistId, trackId))
                playlistTracksState.value = queryAllPlaylistTracks(db)
            }
        }

    override suspend fun deleteAllTracksFromPlaylist(playlistId: String) = withContext(Dispatchers.IO) {
        writeMutex.withLock {
            val db = helper.writableDatabase
            db.delete("playlist_tracks", "playlistId = ?", arrayOf(playlistId))
            playlistTracksState.value = queryAllPlaylistTracks(db)
        }
    }

    override fun observeDownloadedTracks(): Flow<List<DownloadedTrackEntity>> = downloadedTracksState.asStateFlow()

    override suspend fun getDownloadedTrackById(trackId: String): DownloadedTrackEntity? =
        withContext(Dispatchers.IO) {
            downloadedTracksState.value.firstOrNull { it.id == trackId }
        }

    override suspend fun upsertDownloadedTrack(track: DownloadedTrackEntity) = withContext(Dispatchers.IO) {
        writeMutex.withLock {
            val db = helper.writableDatabase
            val cv = ContentValues().apply {
                put("id", track.id)
                put("videoId", track.videoId)
                put("title", track.title)
                put("subtitle", track.subtitle)
                put("thumbnailUrl", track.thumbnailUrl)
                put("duration", track.duration)
                put("localFilePath", track.localFilePath)
                put("mediaStoreUri", track.mediaStoreUri)
                put("fileSizeBytes", track.fileSizeBytes)
                put("downloadedAt", track.downloadedAt)
            }
            db.insertWithOnConflict("downloaded_tracks", null, cv, SQLiteDatabase.CONFLICT_REPLACE)
            downloadedTracksState.value = queryDownloadedTracks(db)
        }
    }

    override suspend fun deleteDownloadedTrackById(trackId: String) = withContext(Dispatchers.IO) {
        writeMutex.withLock {
            val db = helper.writableDatabase
            db.delete("downloaded_tracks", "id = ?", arrayOf(trackId))
            downloadedTracksState.value = queryDownloadedTracks(db)
        }
    }

    override fun observeSubscribedArtists(): Flow<List<SubscribedArtistEntity>> =
        subscribedArtistsState.asStateFlow()

    override suspend fun upsertSubscribedArtist(artist: SubscribedArtistEntity) = withContext(Dispatchers.IO) {
        writeMutex.withLock {
            val db = helper.writableDatabase
            val cv = ContentValues().apply {
                put("id", artist.id)
                put("name", artist.name)
                put("avatarUrl", artist.avatarUrl)
                put("subscribersText", artist.subscribersText)
                put("subscribedAt", artist.subscribedAt)
            }
            db.insertWithOnConflict("subscribed_artists", null, cv, SQLiteDatabase.CONFLICT_REPLACE)
            subscribedArtistsState.value = querySubscribedArtists(db)
        }
    }

    override suspend fun deleteSubscribedArtistById(artistId: String) = withContext(Dispatchers.IO) {
        writeMutex.withLock {
            val db = helper.writableDatabase
            db.delete("subscribed_artists", "id = ? OR LOWER(name) = LOWER(?)", arrayOf(artistId, artistId))
            subscribedArtistsState.value = querySubscribedArtists(db)
        }
    }
}
