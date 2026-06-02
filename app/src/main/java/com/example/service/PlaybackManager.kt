package com.example.service

import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.net.Uri
import android.os.Build
import com.example.data.model.Track
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Random

object PlaybackManager {

    private val coroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    val placeholderTrack = Track(
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

    // State flows
    val _playlistTracks = MutableStateFlow<List<Track>>(emptyList())
    val playlistTracks = _playlistTracks.asStateFlow()

    val _currentTrack = MutableStateFlow<Track>(placeholderTrack)
    val currentTrack = _currentTrack.asStateFlow()

    val _isPlaying = MutableStateFlow(false)
    val isPlaying = _isPlaying.asStateFlow()

    val _isShuffleEnabled = MutableStateFlow(false)
    val isShuffleEnabled = _isShuffleEnabled.asStateFlow()

    val _isRepeatEnabled = MutableStateFlow(false)
    val isRepeatEnabled = _isRepeatEnabled.asStateFlow()

    val _selectedFolder = MutableStateFlow<String?>(null)
    val selectedFolder = _selectedFolder.asStateFlow()

    val _isBassBoosterEnabled = MutableStateFlow(false)
    val isBassBoosterEnabled = _isBassBoosterEnabled.asStateFlow()

    val _playPositionMs = MutableStateFlow(0L)
    val playPositionMs = _playPositionMs.asStateFlow()

    val _spectrumValues = MutableStateFlow<List<Float>>(List(15) { 0.2f })
    val spectrumValues = _spectrumValues.asStateFlow()

    // Equalizer properties
    val _preamp = MutableStateFlow(0f)
    val preamp = _preamp.asStateFlow()

    val _bassBoost = MutableStateFlow(0f)
    val bassBoost = _bassBoost.asStateFlow()

    val _trebleBoost = MutableStateFlow(0f)
    val trebleBoost = _trebleBoost.asStateFlow()

    val _eqBands = MutableStateFlow<List<Float>>(List(10) { 0f })
    val eqBands = _eqBands.asStateFlow()

    val _activePresetName = MutableStateFlow("Plano")
    val activePresetName = _activePresetName.asStateFlow()

    // Player and effects
    var mediaPlayer: MediaPlayer? = null
    var isPlayerPreparing = false
    var eqAudioEffect: Equalizer? = null
    var bassAudioEffect: BassBoost? = null

    private var playbackJob: Job? = null
    private var spectrumJob: Job? = null

    fun toggleShuffle(context: Context? = null) {
        val nextShuffle = !_isShuffleEnabled.value
        _isShuffleEnabled.value = nextShuffle
        if (nextShuffle) {
            _selectedFolder.value = null
            // Play a random track from the overall list!
            val tracksList = _playlistTracks.value
            if (tracksList.isNotEmpty() && context != null) {
                val nextTrackItem = tracksList.random()
                _playPositionMs.value = 0L
                initAndPlayMediaPlayer(context, nextTrackItem, autoStart = true)
            }
        }
    }

    fun toggleRepeat() {
        _isRepeatEnabled.value = !_isRepeatEnabled.value
    }

    fun toggleBassBooster(context: Context? = null) {
        _isBassBoosterEnabled.value = !_isBassBoosterEnabled.value
        applyNativeAudioEffects()
        context?.let { startPlaybackService(it) }
    }

    private fun Float.clamp(min: Float, max: Float): Float {
        return if (this < min) min else if (this > max) max else this
    }

    fun applyNativeAudioEffects() {
        val mp = mediaPlayer ?: return
        try {
            val sessionId = mp.audioSessionId
            
            // Equalizer
            val eq = eqAudioEffect ?: Equalizer(0, sessionId).also { 
                eqAudioEffect = it
                it.enabled = true
            }
            val numBands = eq.numberOfBands.toInt()
            val bandsList = _eqBands.value
            val preampVal = _preamp.value
            val bassBoostVal = _bassBoost.value
            val trebleBoostVal = _trebleBoost.value
            val bassBoosterOn = _isBassBoosterEnabled.value

            // Convert bassBoostVal (0..100) to a dB boost (up to +12dB)
            val bassDb = (bassBoostVal / 100f) * 12f
            // Combine with extra heavy Bass Booster (if enabled, add +10dB of pure low-end energy)
            val totalBassDb = bassDb + (if (bassBoosterOn) 10f else 0f)

            // Convert trebleBoostVal (0..100) to a dB boost (up to +12dB)
            val trebleDb = (trebleBoostVal / 100f) * 12f

            for (i in 0 until numBands) {
                val valueIdx = (i * bandsList.size / numBands).coerceIn(0, bandsList.size - 1)
                val baseDb = bandsList[valueIdx]

                // Determine frequency weighting for bass and treble boosting
                val weightBass = when (valueIdx) {
                    0 -> 1.0f  // 31 Hz
                    1 -> 1.0f  // 62 Hz
                    2 -> 0.8f  // 125 Hz
                    3 -> 0.5f  // 250 Hz
                    4 -> 0.2f  // 500 Hz
                    else -> 0.0f
                }

                val weightTreble = when (valueIdx) {
                    9 -> 1.0f  // 16 kHz
                    8 -> 1.0f  // 8 kHz
                    7 -> 0.8f  // 4 kHz
                    6 -> 0.4f  // 2 kHz
                    5 -> 0.1f  // 1 kHz
                    else -> 0.0f
                }

                val finalDb = baseDb + preampVal + (totalBassDb * weightBass) + (trebleDb * weightTreble)
                val level = (finalDb * 100).toInt().coerceIn(-1500, 1500)
                eq.setBandLevel(i.toShort(), level.toShort())
            }
            
            // Bass Boost (Apply native bass boost filter if supported by the Android audio output device)
            val bb = bassAudioEffect ?: BassBoost(0, sessionId).also {
                bassAudioEffect = it
                it.enabled = true
            }
            if (bb.strengthSupported) {
                val boosterExtra = if (bassBoosterOn) 500f else 0f
                val strength = ((bassBoostVal * 10f) + boosterExtra).toInt().coerceIn(0, 1000)
                bb.setStrength(strength.toShort())
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun initAndPlayMediaPlayer(context: Context, track: Track, autoStart: Boolean) {
        playbackJob?.cancel()
        isPlayerPreparing = true
        _currentTrack.value = track

        coroutineScope.launch(Dispatchers.IO) {
            try {
                eqAudioEffect?.release()
                eqAudioEffect = null
                bassAudioEffect?.release()
                bassAudioEffect = null
                mediaPlayer?.stop()
                mediaPlayer?.release()
                mediaPlayer = null
            } catch (e: Exception) {
                e.printStackTrace()
            }

            try {
                val mp = MediaPlayer()
                mediaPlayer = mp

                val sourceUri = if (track.uriString != null) {
                    Uri.parse(track.uriString)
                } else {
                    val url = when(track.id) {
                        1 -> "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3"
                        2 -> "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-2.mp3"
                        3 -> "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-3.mp3"
                        4 -> "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-4.mp3"
                        5 -> "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-5.mp3"
                        else -> "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3"
                    }
                    Uri.parse(url)
                }

                mp.setDataSource(context.applicationContext, sourceUri)
                
                mp.setOnCompletionListener {
                    coroutineScope.launch(Dispatchers.Main) {
                        if (_isRepeatEnabled.value) {
                            _playPositionMs.value = 0L
                            initAndPlayMediaPlayer(context, _currentTrack.value, autoStart = true)
                        } else {
                            nextTrack(context)
                        }
                    }
                }

                mp.setOnPreparedListener { preparedMp ->
                    isPlayerPreparing = false
                    try {
                        val session = preparedMp.audioSessionId
                        eqAudioEffect?.release()
                        eqAudioEffect = Equalizer(0, session).apply { enabled = true }
                        
                        bassAudioEffect?.release()
                        bassAudioEffect = BassBoost(0, session).apply { enabled = true }
                    } catch (ex: Exception) {
                        ex.printStackTrace()
                    }
                    applyNativeAudioEffects()

                    if (_isPlaying.value && autoStart) {
                        try {
                            preparedMp.start()
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                    if (_playPositionMs.value > 0L) {
                        try {
                            preparedMp.seekTo(_playPositionMs.value.toInt())
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                    startPlaybackProgressLoop()
                    startSpectrumProgress()
                }

                mp.prepareAsync()
            } catch (e: Exception) {
                isPlayerPreparing = false
                e.printStackTrace()
            }
        }
        
        startPlaybackService(context)
    }

    private fun startPlaybackProgressLoop() {
        playbackJob?.cancel()
        playbackJob = coroutineScope.launch {
            while (isActive) {
                val mp = mediaPlayer
                if (mp != null && _isPlaying.value && !isPlayerPreparing) {
                    try {
                        _playPositionMs.value = mp.currentPosition.toLong()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                delay(1000)
            }
        }
    }

    private fun startSpectrumProgress() {
        spectrumJob?.cancel()
        spectrumJob = coroutineScope.launch {
            val random = java.util.Random()
            while (isActive) {
                val playing = _isPlaying.value
                val base = _currentTrack.value.baseSpectrum
                val preampMultiplier = 1f + (_preamp.value / 24f) // Boost height based on preamp setting
                val bassMultiplier = if (_isBassBoosterEnabled.value) {
                    1.8f * (1f + (_bassBoost.value / 100f))
                } else {
                    1f + (_bassBoost.value / 100f) // Boost lower elements base index
                }
                val trebleMultiplier = 1f + (_trebleBoost.value / 100f) // Boost higher elements
                
                val currentList = List(15) { idx ->
                    val rawVal = if (idx < base.size) base[idx] else 0.2f
                    val multiplier = if (idx < 5) {
                        preampMultiplier * bassMultiplier
                    } else if (idx >= 10) {
                        preampMultiplier * trebleMultiplier
                    } else {
                        preampMultiplier
                    }
                    
                    if (playing) {
                        // Dynamic oscillation for realistic visual response
                        val offset = random.nextFloat() * 0.25f - 0.12f
                        (rawVal * multiplier + offset).clamp(0.02f, 1.0f)
                    } else {
                        // Flat micro-vibration when paused
                        val offset = random.nextFloat() * 0.04f - 0.02f
                        (rawVal * 0.4f * multiplier + offset).clamp(0.01f, 1.0f)
                    }
                }
                _spectrumValues.value = currentList
                delay(100)
            }
        }
    }

    fun togglePlayPause(context: Context) {
        val nextPlaying = !_isPlaying.value
        _isPlaying.value = nextPlaying

        coroutineScope.launch(Dispatchers.IO) {
            val mp = mediaPlayer
            if (mp != null) {
                try {
                    if (nextPlaying) {
                        mp.start()
                        startPlaybackProgressLoop()
                    } else {
                        mp.pause()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            } else {
                if (nextPlaying && _currentTrack.value.id != -99) {
                    withContext(Dispatchers.Main) {
                        initAndPlayMediaPlayer(context, _currentTrack.value, autoStart = true)
                    }
                }
            }
        }

        startPlaybackService(context)
    }

    fun nextTrack(context: Context) {
        val tracksList = _playlistTracks.value
        if (tracksList.isEmpty()) return

        val activeFolder = _selectedFolder.value
        val filteredList = if (activeFolder != null) {
            tracksList.filter { it.genre == activeFolder }
        } else {
            tracksList
        }

        val effectiveList = if (filteredList.isNotEmpty()) filteredList else tracksList

        val nextTrackItem = if (_isShuffleEnabled.value && effectiveList.size > 1) {
            val otherTracks = effectiveList.filter { it.id != _currentTrack.value.id }
            if (otherTracks.isNotEmpty()) otherTracks.random() else effectiveList.random()
        } else {
            val currentIndex = effectiveList.indexOfFirst { it.id == _currentTrack.value.id }
            val nextIndex = if (currentIndex != -1) (currentIndex + 1) % effectiveList.size else 0
            effectiveList[nextIndex]
        }

        _playPositionMs.value = 0L
        initAndPlayMediaPlayer(context, nextTrackItem, autoStart = _isPlaying.value)
    }

    fun prevTrack(context: Context) {
        val tracksList = _playlistTracks.value
        if (tracksList.isEmpty()) return

        val activeFolder = _selectedFolder.value
        val filteredList = if (activeFolder != null) {
            tracksList.filter { it.genre == activeFolder }
        } else {
            tracksList
        }

        val effectiveList = if (filteredList.isNotEmpty()) filteredList else tracksList

        val prevTrackItem = if (_isShuffleEnabled.value && effectiveList.size > 1) {
            val otherTracks = effectiveList.filter { it.id != _currentTrack.value.id }
            if (otherTracks.isNotEmpty()) otherTracks.random() else effectiveList.random()
        } else {
            val currentIndex = effectiveList.indexOfFirst { it.id == _currentTrack.value.id }
            val prevIndex = if (currentIndex != -1) {
                if (currentIndex - 1 >= 0) currentIndex - 1 else effectiveList.size - 1
            } else {
                0
            }
            effectiveList[prevIndex]
        }

        _playPositionMs.value = 0L
        initAndPlayMediaPlayer(context, prevTrackItem, autoStart = _isPlaying.value)
    }

    fun seekTo(positionMs: Long) {
        val targetPos = positionMs.coerceIn(0L, _currentTrack.value.durationMs)
        _playPositionMs.value = targetPos
        coroutineScope.launch(Dispatchers.IO) {
            try {
                mediaPlayer?.seekTo(targetPos.toInt())
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun startPlaybackService(context: Context) {
        val intent = Intent(context, MusicPlaybackService::class.java).apply {
            action = MusicPlaybackService.ACTION_START
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }

    fun shutdown() {
        playbackJob?.cancel()
        spectrumJob?.cancel()
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null
            eqAudioEffect?.release()
            eqAudioEffect = null
            bassAudioEffect?.release()
            bassAudioEffect = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
