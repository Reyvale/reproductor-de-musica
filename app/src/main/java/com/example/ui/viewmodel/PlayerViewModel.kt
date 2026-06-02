package com.example.ui.viewmodel

import android.app.Application
import android.net.Uri
import android.provider.MediaStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.api.GeminiClient
import com.example.data.database.EqualizerPresetEntity
import com.example.data.database.PlaybackStateEntity
import com.example.data.database.PlaylistEntity
import com.example.data.model.Track
import com.example.data.model.TrackList
import com.example.data.repository.MusicRepository
import com.example.service.PlaybackManager
import com.example.ui.theme.CustomTheme
import com.example.ui.theme.MusicThemes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.random.Random

class PlayerViewModel(application: Application) : AndroidViewModel(application) {
    
    private val repository = MusicRepository(application)

    // Current lists loaded from database
    private val _presets = MutableStateFlow<List<EqualizerPresetEntity>>(emptyList())
    val presets: StateFlow<List<EqualizerPresetEntity>> = _presets.asStateFlow()

        private val _playlists = MutableStateFlow<List<PlaylistEntity>>(emptyList())
    val playlists: StateFlow<List<PlaylistEntity>> = _playlists.asStateFlow()

    // Scanned local tracks & built-in playlist tracks
    private val _playlistTracks = MutableStateFlow<List<Track>>(emptyList())
    val playlistTracks: StateFlow<List<Track>> = _playlistTracks.asStateFlow()

    private val _scanStatus = MutableStateFlow<String>("Listo para escaneo")
    val scanStatus: StateFlow<String> = _scanStatus.asStateFlow()

    private val placeholderTrack = Track(
        id = -99,
        title = "Sin Pistas",
        artist = "Usa el botón Escanear Música",
        album = "Almacenamiento vacío",
        genre = "Ninguno",
        format = "NONE",
        bitrate = "0 kbps",
        sampleRate = "0 kHz",
        bitDepth = "0-bit",
        fileSize = "0 MB",
        durationMs = 0L,
        baseSpectrum = List(15) { 0.05f }
    )

    // Active track & Playback controls
    private val _currentTrack = MutableStateFlow<Track>(placeholderTrack)
    val currentTrack: StateFlow<Track> = _currentTrack.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _isShuffleEnabled = MutableStateFlow(false)
    val isShuffleEnabled: StateFlow<Boolean> = _isShuffleEnabled.asStateFlow()

    fun toggleShuffle() {
        PlaybackManager.toggleShuffle(getApplication())
        saveCurrentState()
    }

    private val _isRepeatEnabled = MutableStateFlow(false)
    val isRepeatEnabled: StateFlow<Boolean> = _isRepeatEnabled.asStateFlow()

    fun toggleRepeat() {
        _isRepeatEnabled.value = !_isRepeatEnabled.value
    }

    private val _playPositionMs = MutableStateFlow(0L)
    val playPositionMs: StateFlow<Long> = _playPositionMs.asStateFlow()

    // Real-time Audio Spectrum Visualization values (15 frequency bands)
    private val _spectrumValues = MutableStateFlow<List<Float>>(List(15) { 0.05f })
    val spectrumValues: StateFlow<List<Float>> = _spectrumValues.asStateFlow()

    // Equalizer Configuration State
    private val _activePresetName = MutableStateFlow("Flat (Hi-Res Default)")
    val activePresetName: StateFlow<String> = _activePresetName.asStateFlow()

    private val _preamp = MutableStateFlow(0f) // -12f to 12f
    val preamp: StateFlow<Float> = _preamp.asStateFlow()

    private val _bassBoost = MutableStateFlow(0f) // 0f to 100f
    val bassBoost: StateFlow<Float> = _bassBoost.asStateFlow()

    private val _isBassBoosterEnabled = MutableStateFlow(false)
    val isBassBoosterEnabled: StateFlow<Boolean> = _isBassBoosterEnabled.asStateFlow()

    fun toggleBassBooster() {
        PlaybackManager.toggleBassBooster(getApplication())
        saveCurrentState()
    }

    private val _trebleBoost = MutableStateFlow(0f) // 0f to 100f
    val trebleBoost: StateFlow<Float> = _trebleBoost.asStateFlow()

    // The 10 equalizer bands (Index 0 to 9)
    private val _eqBands = MutableStateFlow(List(10) { 0f }) // -12f to 12f
    val eqBands: StateFlow<List<Float>> = _eqBands.asStateFlow()

    // Customization & Themes State
    private val _currentTheme = MutableStateFlow<CustomTheme>(MusicThemes.themes[0])
    val currentTheme: StateFlow<CustomTheme> = _currentTheme.asStateFlow()

    // Resizable Widget Sandbox State
    private val _widgetSize = MutableStateFlow("4x2 Standard") // "4x1 Minimal", "4x2 Standard", "4x4 Immersive"
    val widgetSize: StateFlow<String> = _widgetSize.asStateFlow()

    private val _widgetOpacity = MutableStateFlow(85f) // 0f to 100f
    val widgetOpacity: StateFlow<Float> = _widgetOpacity.asStateFlow()

    private val _widgetAccentColor = MutableStateFlow("#14DFD0")
    val widgetAccentColor: StateFlow<String> = _widgetAccentColor.asStateFlow()

    // Cloud Sync State
    private val _cloudUserEmail = MutableStateFlow("17240547@uicslp.edu.mx")
    val cloudUserEmail: StateFlow<String> = _cloudUserEmail.asStateFlow()

    private val _selectedFolder = MutableStateFlow<String?>(null)
    val selectedFolder: StateFlow<String?> = _selectedFolder.asStateFlow()

    private val _syncStatus = MutableStateFlow("No Sincronizado") // "Sincronizando...", "¡Éxito!", "No Sincronizado"
    val syncStatus: StateFlow<String> = _syncStatus.asStateFlow()

    private val _lastSyncTime = MutableStateFlow("")
    val lastSyncTime: StateFlow<String> = _lastSyncTime.asStateFlow()

    // Gemini Audio Analysis
    private val _geminiResult = MutableStateFlow("")
    val geminiResult: StateFlow<String> = _geminiResult.asStateFlow()

    private val _isGeminiLoading = MutableStateFlow(false)
    val isGeminiLoading: StateFlow<Boolean> = _isGeminiLoading.asStateFlow()

    // Hi-res configuration
    private val _isHiResDirect = MutableStateFlow(true)
    val isHiResDirect: StateFlow<Boolean> = _isHiResDirect.asStateFlow()

    private val _resamplingRate = MutableStateFlow("192 kHz")
    val resamplingRate: StateFlow<String> = _resamplingRate.asStateFlow()

    private val _dacFilter = MutableStateFlow("Minimum Phase Fast")
    val dacFilter: StateFlow<String> = _dacFilter.asStateFlow()

    // Jobs for simulated clock & equalizer spectrum pulse
    private var playbackJob: Job? = null
    private var spectrumJob: Job? = null

    init {
        // Collect PlaybackManager state changes to update local ViewModel flows for UI reactive binding
        viewModelScope.launch { PlaybackManager.playlistTracks.collect { _playlistTracks.value = it } }
        viewModelScope.launch { PlaybackManager.currentTrack.collect { _currentTrack.value = it } }
        viewModelScope.launch { PlaybackManager.isPlaying.collect { _isPlaying.value = it } }
        viewModelScope.launch { PlaybackManager.isShuffleEnabled.collect { _isShuffleEnabled.value = it } }
        viewModelScope.launch { PlaybackManager.isRepeatEnabled.collect { _isRepeatEnabled.value = it } }
        viewModelScope.launch { PlaybackManager.isBassBoosterEnabled.collect { _isBassBoosterEnabled.value = it } }
        viewModelScope.launch { PlaybackManager.playPositionMs.collect { _playPositionMs.value = it } }
        viewModelScope.launch { PlaybackManager.spectrumValues.collect { _spectrumValues.value = it } }
        viewModelScope.launch { PlaybackManager.preamp.collect { _preamp.value = it } }
        viewModelScope.launch { PlaybackManager.bassBoost.collect { _bassBoost.value = it } }
        viewModelScope.launch { PlaybackManager.trebleBoost.collect { _trebleBoost.value = it } }
        viewModelScope.launch { PlaybackManager.eqBands.collect { _eqBands.value = it } }
        viewModelScope.launch { PlaybackManager.activePresetName.collect { _activePresetName.value = it } }
        viewModelScope.launch { PlaybackManager.selectedFolder.collect { _selectedFolder.value = it } }

        viewModelScope.launch(Dispatchers.IO) {
            repository.checkPrepopulate()
            
            // Collect presets and playtlists directly to remain updated
            launch {
                repository.equalizerPresets.collect { list ->
                    _presets.value = list
                }
            }
            launch {
                repository.playlists.collect { list ->
                    _playlists.value = list
                }
            }
            launch {
                repository.localTracks.collect { entities ->
                    val domainTracks = entities.map { entity ->
                        val specList = try {
                            entity.spectrumString.split(",").mapNotNull { it.toFloatOrNull() }
                        } catch (e: Exception) {
                            emptyList()
                        }
                        Track(
                            id = entity.id,
                            title = entity.title,
                            artist = entity.artist,
                            album = entity.album,
                            genre = entity.genre,
                            format = entity.format,
                            bitrate = entity.bitrate,
                            sampleRate = entity.sampleRate,
                            bitDepth = entity.bitDepth,
                            fileSize = entity.fileSize,
                            durationMs = entity.durationMs,
                            baseSpectrum = if (specList.isEmpty()) List(15) { 0.5f } else specList,
                            uriString = entity.uriString
                        )
                    }
                    _playlistTracks.value = domainTracks
                    PlaybackManager._playlistTracks.value = domainTracks
                    
                    val stored = repository.getPlaybackState()
                    val activeTrack = domainTracks.find { it.id == stored.currentTrackId }
                        ?: domainTracks.firstOrNull()
                    
                    if (activeTrack != null) {
                        _currentTrack.value = activeTrack
                        PlaybackManager._currentTrack.value = activeTrack
                    } else {
                        _currentTrack.value = placeholderTrack
                        PlaybackManager._currentTrack.value = placeholderTrack
                    }
                }
            }

            // Load last stored playback state details
            val stored = repository.getPlaybackState()
            _playPositionMs.value = stored.currentPositionMs
            PlaybackManager._playPositionMs.value = stored.currentPositionMs
            
            // Themes and settings
            val theme = MusicThemes.getThemeById(stored.themeName)
            _currentTheme.value = theme
            _isHiResDirect.value = stored.isHiResDirect
            _resamplingRate.value = stored.resamplingRate
            _dacFilter.value = stored.dacFilterType
            
            // Equalizer Setup from stored active EQ template ID
            launch {
                repository.equalizerPresets.collect { list ->
                    val match = list.find { it.id == stored.activePresetId }
                    if (match != null) {
                        val bands = listOf(
                            match.b31, match.b62, match.b125, match.b250, match.b500,
                            match.b1k, match.b2k, match.b4k, match.b8k, match.b16k
                        )
                        _activePresetName.value = match.name
                        _preamp.value = match.preamp
                        _bassBoost.value = match.bBoost
                        _trebleBoost.value = match.tBoost
                        _eqBands.value = bands

                        PlaybackManager._activePresetName.value = match.name
                        PlaybackManager._preamp.value = match.preamp
                        PlaybackManager._bassBoost.value = match.bBoost
                        PlaybackManager._trebleBoost.value = match.tBoost
                        PlaybackManager._eqBands.value = bands
                        PlaybackManager.applyNativeAudioEffects()
                    }
                }
            }

            if (stored.cloudSynced) {
                _syncStatus.value = "Sincronizado"
                val sdf = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
                _lastSyncTime.value = sdf.format(Date(stored.cloudSyncTime))
            }
        }
    }

    fun togglePlayPause() {
        PlaybackManager.togglePlayPause(getApplication())
        saveCurrentState()
    }

    fun nextTrack() {
        PlaybackManager.nextTrack(getApplication())
        saveCurrentState()
    }

    fun prevTrack() {
        PlaybackManager.prevTrack(getApplication())
        saveCurrentState()
    }

    fun selectTrack(trackId: Int) {
        val match = _playlistTracks.value.find { it.id == trackId }
        if (match != null) {
            PlaybackManager._selectedFolder.value = match.genre
            PlaybackManager._playPositionMs.value = 0L
            PlaybackManager.initAndPlayMediaPlayer(getApplication(), match, autoStart = true)
            saveCurrentState()
        }
    }

    fun startPlayingFolder(folderName: String) {
        val tracksList = _playlistTracks.value
        val folderTracks = tracksList.filter { it.genre == folderName }
        if (folderTracks.isNotEmpty()) {
            PlaybackManager._selectedFolder.value = folderName
            PlaybackManager._isShuffleEnabled.value = false // disable general shuffle initially so it plays sequentially
            PlaybackManager._playPositionMs.value = 0L
            PlaybackManager.initAndPlayMediaPlayer(getApplication(), folderTracks.first(), autoStart = true)
            saveCurrentState()
        }
    }

    fun seekTo(positionMs: Long) {
        PlaybackManager.seekTo(positionMs)
        saveCurrentState()
    }

    // Setters for equalizer values
    fun updatePreamp(value: Float) {
        PlaybackManager._preamp.value = value
        PlaybackManager._activePresetName.value = "Personalizado (EQ)"
        PlaybackManager.applyNativeAudioEffects()
        saveCurrentState()
    }

    fun updateBassBoost(value: Float) {
        PlaybackManager._bassBoost.value = value
        PlaybackManager._activePresetName.value = "Personalizado (EQ)"
        PlaybackManager.applyNativeAudioEffects()
        saveCurrentState()
    }

    fun updateTrebleBoost(value: Float) {
        PlaybackManager._trebleBoost.value = value
        PlaybackManager._activePresetName.value = "Personalizado (EQ)"
        PlaybackManager.applyNativeAudioEffects()
        saveCurrentState()
    }

    fun updateBand(index: Int, dbValue: Float) {
        val list = PlaybackManager._eqBands.value.toMutableList()
        if (index in list.indices) {
            list[index] = dbValue
            PlaybackManager._eqBands.value = list
            PlaybackManager._activePresetName.value = "Personalizado (EQ)"
            PlaybackManager.applyNativeAudioEffects()
            saveCurrentState()
        }
    }

    fun applyPreset(preset: EqualizerPresetEntity) {
        PlaybackManager._activePresetName.value = preset.name
        PlaybackManager._preamp.value = preset.preamp
        PlaybackManager._bassBoost.value = preset.bBoost
        PlaybackManager._trebleBoost.value = preset.tBoost
        PlaybackManager._eqBands.value = listOf(
            preset.b31, preset.b62, preset.b125, preset.b250, preset.b500,
            preset.b1k, preset.b2k, preset.b4k, preset.b8k, preset.b16k
        )
        PlaybackManager.applyNativeAudioEffects()
        saveCurrentState()
    }

    fun resetToDefaultEq() {
        PlaybackManager._activePresetName.value = "Flat (Hi-Res Default)"
        PlaybackManager._preamp.value = 0f
        PlaybackManager._bassBoost.value = 0f
        PlaybackManager._trebleBoost.value = 0f
        PlaybackManager._eqBands.value = listOf(0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f)
        PlaybackManager.applyNativeAudioEffects()
        saveCurrentState()
    }

    fun saveAsNewCustomPreset(name: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val current = _eqBands.value
            val preset = EqualizerPresetEntity(
                name = name,
                isCustom = true,
                b31 = current[0], b62 = current[1], b125 = current[2], b250 = current[3], b500 = current[4],
                b1k = current[5], b2k = current[6], b4k = current[7], b8k = current[8], b16k = current[9],
                preamp = _preamp.value,
                bBoost = _bassBoost.value,
                tBoost = _trebleBoost.value
            )
            repository.insertPreset(preset)
            _activePresetName.value = name
        }
    }

    fun deleteCustomPreset(preset: EqualizerPresetEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deletePreset(preset)
            if (_activePresetName.value == preset.name) {
                _activePresetName.value = "Flat (Hi-Res Default)"
                _preamp.value = 0f
                _bassBoost.value = 0f
                _trebleBoost.value = 0f
                _eqBands.value = List(10) { 0f }
            }
        }
    }

    // Setters for Themes
    fun selectTheme(themeId: String) {
        val theme = MusicThemes.getThemeById(themeId)
        _currentTheme.value = theme
        // Widget preview matches the accent of the picked theme
        _widgetAccentColor.value = "#" + Integer.toHexString(theme.primary.value.toInt()).substring(2).uppercase()
        saveCurrentState()
    }

    // Setters for Widget sandbox customizer
    fun setWidgetSize(size: String) { _widgetSize.value = size }
    fun setWidgetOpacity(opacity: Float) { _widgetOpacity.value = opacity }
    fun setWidgetAccent(hex: String) { _widgetAccentColor.value = hex }

    // Setters for Hi-Res Configuration panel
    fun toggleHiResDirect() {
        _isHiResDirect.value = !_isHiResDirect.value
        saveCurrentState()
    }
    fun setResamplingRate(rate: String) {
        _resamplingRate.value = rate
        saveCurrentState()
    }
    fun setDacFilter(filter: String) {
        _dacFilter.value = filter
        saveCurrentState()
    }

    // Cloud Sync implementation
    fun updateCloudEmail(email: String) { _cloudUserEmail.value = email }

    fun runCloudSync() {
        viewModelScope.launch {
            _syncStatus.value = "Sincronizando..."
            // Simulate deep cloud secure operations (storing presets, theme variables & playlists states)
            delay(2200)
            
            _syncStatus.value = "Sincronizado"
            val sdf = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
            _lastSyncTime.value = sdf.format(Date())
            
            saveCurrentState()
        }
    }

    // Audio Analysis via Gemini API
    fun triggerGeminiAnalysis() {
        val bands = _eqBands.value
        val preset = _activePresetName.value
        val preampVal = _preamp.value
        val bassVal = _bassBoost.value
        val trebleVal = _trebleBoost.value
        val activeTrack = _currentTrack.value

        _isGeminiLoading.value = true
        _geminiResult.value = ""

        viewModelScope.launch(Dispatchers.IO) {
            val response = GeminiClient.analyzeAudioState(
                presetName = preset,
                bands = bands,
                preamp = preampVal,
                bass = bassVal,
                treble = trebleVal,
                format = activeTrack.format,
                bitrate = activeTrack.bitrate,
                sampleRate = activeTrack.sampleRate,
                filter = _dacFilter.value
            )
            _geminiResult.value = response
            _isGeminiLoading.value = false
        }
    }

    fun scanLocalMusic() {
        _scanStatus.value = "Escaneando..."
        viewModelScope.launch(Dispatchers.IO) {
            val context = getApplication<Application>().applicationContext
            val projection = arrayOf(
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.ALBUM,
                MediaStore.Audio.Media.DURATION,
                MediaStore.Audio.Media.SIZE,
                MediaStore.Audio.Media.DATA
            )
            val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
            val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"
            val localTracks = mutableListOf<Track>()

            try {
                context.contentResolver.query(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    projection,
                    selection,
                    null,
                    sortOrder
                )?.use { cursor ->
                    val idColumn = cursor.getColumnIndex(MediaStore.Audio.Media._ID)
                    val titleColumn = cursor.getColumnIndex(MediaStore.Audio.Media.TITLE)
                    val artistColumn = cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST)
                    val albumColumn = cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM)
                    val durationColumn = cursor.getColumnIndex(MediaStore.Audio.Media.DURATION)
                    val sizeColumn = cursor.getColumnIndex(MediaStore.Audio.Media.SIZE)
                    val dataColumn = cursor.getColumnIndex(MediaStore.Audio.Media.DATA)

                    var generatedCount = 100 // Prevent overlapping with mock IDs (1-5)
                    while (cursor.moveToNext()) {
                        if (idColumn == -1 || titleColumn == -1 || artistColumn == -1 ||
                            albumColumn == -1 || durationColumn == -1 || sizeColumn == -1 || dataColumn == -1) {
                            continue
                        }
                        val title = cursor.getString(titleColumn) ?: "Desconocido"
                        val artist = cursor.getString(artistColumn) ?: "Artista Desconocido"
                        val album = cursor.getString(albumColumn) ?: "Álbum Desconocido"
                        val duration = cursor.getLong(durationColumn)
                        val sizeBytes = cursor.getLong(sizeColumn)
                        val filePath = cursor.getString(dataColumn) ?: ""

                        val format = when {
                            filePath.endsWith(".flac", ignoreCase = true) -> "FLAC"
                            filePath.endsWith(".wav", ignoreCase = true) -> "WAV"
                            filePath.endsWith(".alac", ignoreCase = true) -> "ALAC"
                            filePath.endsWith(".dsf", ignoreCase = true) || filePath.endsWith(".dff", ignoreCase = true) -> "DSD"
                            filePath.endsWith(".m4a", ignoreCase = true) -> "AAC"
                            filePath.endsWith(".mp3", ignoreCase = true) -> "MP3"
                            else -> "M4A"
                        }

                        val bitrate = when (format) {
                            "FLAC" -> "4608 kbps"
                            "WAV" -> "1411 kbps"
                            "DSD" -> "11289 kbps"
                            "ALAC" -> "9216 kbps"
                            "MP3" -> "320 kbps"
                            else -> "256 kbps"
                        }

                        val sampleRate = when (format) {
                            "DSD" -> "2.822 MHz"
                            "FLAC" -> "192 kHz"
                            "WAV" -> "44.1 kHz"
                            "ALAC" -> "352.8 kHz"
                            else -> "44.1 kHz"
                        }

                        val bitDepth = when (format) {
                            "FLAC", "ALAC" -> "24-bit"
                            "WAV", "DSD" -> "32-bit"
                            else -> "16-bit"
                        }

                        val fileSizeStr = String.format(Locale.getDefault(), "%.1f MB", sizeBytes / (1024f * 1024f))
                        val specHeights = List(15) { Random.nextFloat() * 0.7f + 0.1f }

                        val mediaId = cursor.getLong(idColumn)
                        val trackUriString = "content://media/external/audio/media/$mediaId"

                        val folderName = try {
                            val f = java.io.File(filePath)
                            f.parentFile?.name ?: "Música Local"
                        } catch (e: Exception) {
                            "Música Local"
                        }

                        localTracks.add(
                            Track(
                                id = generatedCount++,
                                title = title,
                                artist = artist,
                                album = album,
                                genre = folderName,
                                format = format,
                                bitrate = bitrate,
                                sampleRate = sampleRate,
                                bitDepth = bitDepth,
                                fileSize = fileSizeStr,
                                durationMs = if (duration > 0) duration else 180000L,
                                baseSpectrum = specHeights,
                                uriString = trackUriString
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            if (localTracks.isNotEmpty()) {
                val entities = localTracks.map { track ->
                    com.example.data.database.LocalTrackEntity(
                        id = track.id,
                        title = track.title,
                        artist = track.artist,
                        album = track.album,
                        genre = track.genre,
                        format = track.format,
                        bitrate = track.bitrate,
                        sampleRate = track.sampleRate,
                        bitDepth = track.bitDepth,
                        fileSize = track.fileSize,
                        durationMs = track.durationMs,
                        spectrumString = track.baseSpectrum.joinToString(",") { it.toString() },
                        uriString = track.uriString
                    )
                }
                repository.clearLocalTracks()
                repository.insertLocalTracks(entities)
                _scanStatus.value = "Éxito: ${localTracks.size} canciones guardadas"
            } else {
                _scanStatus.value = "No se encontraron audios locales en el almacenamiento"
            }
        }
    }

    // Persist current state in Room database
    private fun saveCurrentState() {
        viewModelScope.launch(Dispatchers.IO) {
            val currentTracks = _playlistTracks.value
            val presetMatch = repository.equalizerPresets.first().find { it.name == _activePresetName.value }
            val presetId = presetMatch?.id ?: 1

            val state = PlaybackStateEntity(
                currentTrackId = _currentTrack.value.id,
                currentPositionMs = _playPositionMs.value,
                isPlaying = _isPlaying.value,
                themeName = _currentTheme.value.id,
                activePresetId = presetId,
                isHiResDirect = _isHiResDirect.value,
                resamplingRate = _resamplingRate.value,
                dacFilterType = _dacFilter.value,
                cloudSynced = _syncStatus.value == "Sincronizado",
                cloudSyncTime = if (_syncStatus.value == "Sincronizado") System.currentTimeMillis() else 0L
            )
            repository.savePlaybackState(state)
        }
    }

    override fun onCleared() {
        playbackJob?.cancel()
        spectrumJob?.cancel()
        super.onCleared()
    }
}
