package com.example.data.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "equalizer_presets")
data class EqualizerPresetEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val isCustom: Boolean,
    val b31: Float, // value in dB, -12.0 to 12.0
    val b62: Float,
    val b125: Float,
    val b250: Float,
    val b500: Float,
    val b1k: Float,
    val b2k: Float,
    val b4k: Float,
    val b8k: Float,
    val b16k: Float,
    val preamp: Float, // dB, -12.0 to 12.0
    val bBoost: Float,  // 0.0 to 100.0 (bass%)
    val tBoost: Float   // 0.0 to 100.0 (treble%)
)

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val songIds: String // Comma-separated like "1,2,3"
)

@Entity(tableName = "playback_state")
data class PlaybackStateEntity(
    @PrimaryKey val id: Int = 1,
    val currentTrackId: Int = 1,
    val currentPositionMs: Long = 0L,
    val isPlaying: Boolean = false,
    val themeName: String = "Classic Graphite",
    val activePresetId: Int = 1,
    val isHiResDirect: Boolean = true,
    val resamplingRate: String = "192 kHz",
    val dacFilterType: String = "Minimum Phase Fast",
    val cloudSynced: Boolean = false,
    val cloudSyncTime: Long = 0L
)

@Entity(tableName = "local_tracks")
data class LocalTrackEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val artist: String,
    val album: String,
    val genre: String,
    val format: String,
    val bitrate: String,
    val sampleRate: String,
    val bitDepth: String,
    val fileSize: String,
    val durationMs: Long,
    val spectrumString: String, // Comma-separated floats list
    val uriString: String?
)

@Dao
interface AppDao {
    @Query("SELECT * FROM equalizer_presets")
    fun getAllPresetsFlow(): Flow<List<EqualizerPresetEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPreset(preset: EqualizerPresetEntity)

    @Delete
    suspend fun deletePreset(preset: EqualizerPresetEntity)

    @Query("SELECT * FROM playlists")
    fun getAllPlaylistsFlow(): Flow<List<PlaylistEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlist: PlaylistEntity)

    @Query("DELETE FROM playlists WHERE id = :id")
    suspend fun deletePlaylistById(id: Int)

    @Query("SELECT * FROM playback_state WHERE id = 1")
    suspend fun getPlaybackState(): PlaybackStateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaybackState(state: PlaybackStateEntity)

    @Query("SELECT * FROM local_tracks ORDER BY title ASC")
    fun getAllLocalTracksFlow(): Flow<List<LocalTrackEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLocalTracks(tracks: List<LocalTrackEntity>)

    @Query("DELETE FROM local_tracks")
    suspend fun deleteAllLocalTracks()
}

@Database(entities = [EqualizerPresetEntity::class, PlaylistEntity::class, PlaybackStateEntity::class, LocalTrackEntity::class], version = 2, exportSchema = false)
abstract class MusicDatabase : RoomDatabase() {
    abstract fun dao(): AppDao
}
