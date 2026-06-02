package com.example.data.repository

import android.content.Context
import androidx.room.Room
import com.example.data.database.AppDao
import com.example.data.database.EqualizerPresetEntity
import com.example.data.database.MusicDatabase
import com.example.data.database.PlaybackStateEntity
import com.example.data.database.PlaylistEntity
import com.example.data.database.LocalTrackEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class MusicRepository(private val context: Context) {
    
    val database: MusicDatabase by lazy {
        Room.databaseBuilder(
            context.applicationContext,
            MusicDatabase::class.java,
            "music_player_db"
        )
        .fallbackToDestructiveMigration()
        .build()
    }

    val dao: AppDao by lazy { database.dao() }

    val equalizerPresets: Flow<List<EqualizerPresetEntity>> = dao.getAllPresetsFlow()
    val playlists: Flow<List<PlaylistEntity>> = dao.getAllPlaylistsFlow()
    val localTracks: Flow<List<LocalTrackEntity>> = dao.getAllLocalTracksFlow()

    suspend fun getPlaybackState(): PlaybackStateEntity {
        return dao.getPlaybackState() ?: PlaybackStateEntity()
    }

    suspend fun savePlaybackState(state: PlaybackStateEntity) {
        dao.insertPlaybackState(state)
    }

    suspend fun insertPreset(preset: EqualizerPresetEntity) {
        dao.insertPreset(preset)
    }

    suspend fun deletePreset(preset: EqualizerPresetEntity) {
        dao.deletePreset(preset)
    }

    suspend fun insertPlaylist(playlist: PlaylistEntity) {
        dao.insertPlaylist(playlist)
    }

    suspend fun deletePlaylist(playlistId: Int) {
        dao.deletePlaylistById(playlistId)
    }

    suspend fun insertLocalTracks(tracks: List<LocalTrackEntity>) {
        dao.insertLocalTracks(tracks)
    }

    suspend fun clearLocalTracks() {
        dao.deleteAllLocalTracks()
    }

    suspend fun checkPrepopulate() {
        val currentPresets = dao.getAllPresetsFlow().first()
        if (currentPresets.isEmpty()) {
            val builtIn = listOf(
                EqualizerPresetEntity(
                    id = 1, name = "Flat (Hi-Res Default)", isCustom = false,
                    b31=0f, b62=0f, b125=0f, b250=0f, b500=0f, b1k=0f, b2k=0f, b4k=0f, b8k=0f, b16k=0f,
                    preamp = 0f, bBoost = 0f, tBoost = 0f
                ),
                EqualizerPresetEntity(
                    id = 2, name = "Poweramp Extreme Bass", isCustom = false,
                    b31=8f, b62=6.5f, b125=4f, b250=1f, b500=0f, b1k=0f, b2k=0f, b4k=1f, b8k=3f, b16k=5f,
                    preamp = -2f, bBoost = 50f, tBoost = 35f
                ),
                EqualizerPresetEntity(
                    id = 3, name = "Cosmic Metal & Rock", isCustom = false,
                    b31=4f, b62=3f, b125=-1f, b250=-2f, b500=0f, b1k=2f, b2k=3f, b4k=1f, b8k=4f, b16k=5f,
                    preamp = 0f, bBoost = 30f, tBoost = 25f
                ),
                EqualizerPresetEntity(
                    id = 4, name = "Acoustic Vocal Clarity", isCustom = false,
                    b31=-3f, b62=-2f, b125=0f, b250=1f, b500=3f, b1k=4f, b2k=4f, b4k=3f, b8k=2f, b16k=1f,
                    preamp = 1f, bBoost = 10f, tBoost = 15f
                ),
                EqualizerPresetEntity(
                    id = 5, name = "Electronic Sub-Woofer", isCustom = false,
                    b31=9f, b62=8f, b125=6f, b250=1f, b500=-1f, b1k=-2f, b2k=1f, b4k=2f, b8k=4f, b16k=6f,
                    preamp = -3f, bBoost = 75f, tBoost = 40f
                )
            )
            for (p in builtIn) {
                dao.insertPreset(p)
            }
        }
    }
}
