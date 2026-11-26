package com.example.data.local

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.model.MediaKind
import com.example.model.MediaSourceType
import com.example.model.MediaTrack
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "play_history")
data class PlayHistoryEntity(
    @PrimaryKey val id: String,
    val title: String,
    val artist: String,
    val album: String,
    val coverUrl: String?,
    val streamUrl: String?,
    val mvId: Long,
    val durationMs: Long,
    val sourceType: String,
    val mediaKind: String,
    val playedAtMs: Long = System.currentTimeMillis()
) {
    fun toDomain(): MediaTrack = MediaTrack(
        id = id,
        title = title,
        artist = artist,
        album = album,
        coverUrl = coverUrl,
        streamUrl = streamUrl,
        mvId = mvId,
        durationMs = durationMs,
        sourceType = runCatching { MediaSourceType.valueOf(sourceType) }.getOrDefault(MediaSourceType.NETEASE),
        mediaKind = runCatching { MediaKind.valueOf(mediaKind) }.getOrDefault(MediaKind.AUDIO)
    )

    companion object {
        fun fromDomain(track: MediaTrack): PlayHistoryEntity = PlayHistoryEntity(
            id = track.id,
            title = track.title,
            artist = track.artist,
            album = track.album,
            coverUrl = track.coverUrl,
            streamUrl = track.streamUrl,
            mvId = track.mvId,
            durationMs = track.durationMs,
            sourceType = track.sourceType.name,
            mediaKind = track.mediaKind.name,
            playedAtMs = System.currentTimeMillis()
        )
    }
}

@Entity(tableName = "local_media")
data class LocalMediaEntity(
    @PrimaryKey val id: String,
    val uriString: String,
    val title: String,
    val artist: String,
    val durationMs: Long,
    val mediaKind: String,
    val addedAtMs: Long = System.currentTimeMillis()
) {
    fun toDomain(): MediaTrack = MediaTrack(
        id = id,
        title = title,
        artist = artist,
        album = if (mediaKind == MediaKind.VIDEO.name) "本地视频" else "本地音频",
        coverUrl = null,
        streamUrl = uriString,
        mvId = 0L,
        durationMs = durationMs,
        sourceType = MediaSourceType.LOCAL,
        mediaKind = runCatching { MediaKind.valueOf(mediaKind) }.getOrDefault(MediaKind.AUDIO)
    )
}

@Entity(tableName = "favorite_tracks")
data class FavoriteTrackEntity(
    @PrimaryKey val id: String,
    val title: String,
    val artist: String,
    val album: String,
    val coverUrl: String?,
    val streamUrl: String?,
    val mvId: Long,
    val durationMs: Long,
    val sourceType: String,
    val mediaKind: String,
    val savedAtMs: Long = System.currentTimeMillis()
) {
    fun toDomain(): MediaTrack = MediaTrack(
        id = id,
        title = title,
        artist = artist,
        album = album,
        coverUrl = coverUrl,
        streamUrl = streamUrl,
        mvId = mvId,
        durationMs = durationMs,
        sourceType = runCatching { MediaSourceType.valueOf(sourceType) }.getOrDefault(MediaSourceType.NETEASE),
        mediaKind = runCatching { MediaKind.valueOf(mediaKind) }.getOrDefault(MediaKind.AUDIO),
        isLiked = true
    )

    companion object {
        fun fromDomain(track: MediaTrack): FavoriteTrackEntity = FavoriteTrackEntity(
            id = track.id,
            title = track.title,
            artist = track.artist,
            album = track.album,
            coverUrl = track.coverUrl,
            streamUrl = track.streamUrl,
            mvId = track.mvId,
            durationMs = track.durationMs,
            sourceType = track.sourceType.name,
            mediaKind = track.mediaKind.name,
            savedAtMs = System.currentTimeMillis()
        )
    }
}

@Entity(tableName = "search_history")
data class SearchHistoryEntity(
    @PrimaryKey val keyword: String,
    val searchedAtMs: Long = System.currentTimeMillis()
)

@Dao
interface AriaDao {
    @Query("SELECT * FROM play_history ORDER BY playedAtMs DESC LIMIT 60")
    fun observePlayHistory(): Flow<List<PlayHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertHistory(entity: PlayHistoryEntity)

    @Query("DELETE FROM play_history")
    suspend fun clearHistory()

    @Query("SELECT * FROM local_media ORDER BY addedAtMs DESC")
    fun observeLocalMedia(): Flow<List<LocalMediaEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertLocalMedia(entity: LocalMediaEntity)

    @Query("DELETE FROM local_media WHERE id = :id")
    suspend fun deleteLocalMedia(id: String)

    @Query("SELECT * FROM favorite_tracks ORDER BY savedAtMs DESC")
    fun observeFavorites(): Flow<List<FavoriteTrackEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM favorite_tracks WHERE id = :id)")
    fun observeIsFavorite(id: String): Flow<Boolean>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertFavorite(entity: FavoriteTrackEntity)

    @Query("DELETE FROM favorite_tracks WHERE id = :id")
    suspend fun removeFavorite(id: String)

    @Query("SELECT * FROM search_history ORDER BY searchedAtMs DESC LIMIT 12")
    fun observeSearchHistory(): Flow<List<SearchHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSearchKeyword(entity: SearchHistoryEntity)

    @Query("DELETE FROM search_history")
    suspend fun clearSearchHistory()
}

@Database(
    entities = [
        PlayHistoryEntity::class,
        LocalMediaEntity::class,
        FavoriteTrackEntity::class,
        SearchHistoryEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AriaDatabase : RoomDatabase() {
    abstract fun ariaDao(): AriaDao

    companion object {
        @Volatile
        private var INSTANCE: AriaDatabase? = null

        fun getInstance(context: Context): AriaDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AriaDatabase::class.java,
                    "aria_player.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
