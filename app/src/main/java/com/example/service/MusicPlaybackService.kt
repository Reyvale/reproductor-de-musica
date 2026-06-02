package com.example.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.data.model.Track
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MusicPlaybackService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    companion object {
        const val NOTIFICATION_ID = 404
        const val CHANNEL_ID = "music_playback_channel_v1"

        const val ACTION_START = "com.example.service.action.START"
        const val ACTION_PLAY_PAUSE = "com.example.service.action.PLAY_PAUSE"
        const val ACTION_NEXT = "com.example.service.action.NEXT"
        const val ACTION_PREV = "com.example.service.action.PREV"
        const val ACTION_STOP = "com.example.service.action.STOP"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()

        // Observe playback state changes to dynamically update notification
        serviceScope.launch {
            PlaybackManager.isPlaying.collectLatest { _ ->
                updateNotification()
            }
        }
        serviceScope.launch {
            PlaybackManager.currentTrack.collectLatest { _ ->
                updateNotification()
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        when (action) {
            ACTION_START -> {
                updateNotification()
            }
            ACTION_PLAY_PAUSE -> {
                PlaybackManager.togglePlayPause(this)
                updateNotification()
            }
            ACTION_NEXT -> {
                PlaybackManager.nextTrack(this)
                updateNotification()
            }
            ACTION_PREV -> {
                PlaybackManager.prevTrack(this)
                updateNotification()
            }
            ACTION_STOP -> {
                PlaybackManager.shutdown()
                stopForeground(true)
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    private fun updateNotification() {
        val track = PlaybackManager.currentTrack.value
        val isPlaying = PlaybackManager.isPlaying.value

        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pOpen = PendingIntent.getActivity(
            this, 0, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificationBuilder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentIntent(pOpen)
            .setOngoing(isPlaying)
            .setOnlyAlertOnce(true)

        if (track.id == -99) {
            notificationBuilder
                .setContentTitle("PowerPlayer")
                .setContentText("Listo para reproducir música")
        } else {
            // Notification Action intents
            val prevIntent = Intent(this, MusicPlaybackService::class.java).apply { action = ACTION_PREV }
            val pPrev = PendingIntent.getService(this, 1, prevIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

            val playIntent = Intent(this, MusicPlaybackService::class.java).apply { action = ACTION_PLAY_PAUSE }
            val pPlay = PendingIntent.getService(this, 2, playIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

            val nextIntent = Intent(this, MusicPlaybackService::class.java).apply { action = ACTION_NEXT }
            val pNext = PendingIntent.getService(this, 3, nextIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

            val stopIntent = Intent(this, MusicPlaybackService::class.java).apply { action = ACTION_STOP }
            val pStop = PendingIntent.getService(this, 4, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

            val playIconRes = if (isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play
            val playActionTitle = if (isPlaying) "Pausar" else "Reproducir"

            notificationBuilder
                .setContentTitle(track.title)
                .setContentText("${track.artist} — ${track.album}")
                .setStyle(
                    androidx.media.app.NotificationCompat.MediaStyle()
                        .setShowActionsInCompactView(0, 1, 2)
                        .setShowCancelButton(true)
                        .setCancelButtonIntent(pStop)
                )
                .addAction(android.R.drawable.ic_media_previous, "Atrasar", pPrev)
                .addAction(playIconRes, playActionTitle, pPlay)
                .addAction(android.R.drawable.ic_media_next, "Siguiente", pNext)
                .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Detener", pStop)
        }

        val notification = notificationBuilder.build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Reproductor en Segundo Plano",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Mantiene la reproducción y los controles en la barra de estado."
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}
