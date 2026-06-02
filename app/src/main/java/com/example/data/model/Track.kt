package com.example.data.model

data class Track(
    val id: Int,
    val title: String,
    val artist: String,
    val album: String,
    val genre: String,
    val format: String,      // FLAC, WAV, ALAC, DSD
    val bitrate: String,     // e.g. "4608 kbps"
    val sampleRate: String,  // e.g. "192 kHz"
    val bitDepth: String,    // e.g. "24-bit"
    val fileSize: String,    // e.g. "87 MB"
    val durationMs: Long,    // overall duration
    val baseSpectrum: List<Float>, // sample heights for spectrum visualization
    val uriString: String? = null
)

object TrackList {
    val tracks = emptyList<Track>()
}
