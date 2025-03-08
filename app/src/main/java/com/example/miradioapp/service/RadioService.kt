// Archivo: app/src/main/java/com/example/miradioapp/service/RadioService.kt
package com.example.miradioapp.service

import android.app.PendingIntent
import android.content.Intent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.IntentFilter
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.net.Uri
import android.os.Binder
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.os.PowerManager
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.core.app.NotificationCompat
import androidx.media.MediaBrowserServiceCompat
import androidx.media.session.MediaButtonReceiver
import com.example.miradioapp.MainActivity
import com.example.miradioapp.R
import com.example.miradioapp.RadioApplication
import com.example.miradioapp.data.model.AppConfig
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ext.mediasession.MediaSessionConnector
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RadioService : MediaBrowserServiceCompat() {

    private lateinit var mediaSession: MediaSessionCompat
    private lateinit var player: ExoPlayer
    private lateinit var mediaSessionConnector: MediaSessionConnector
    private lateinit var notificationManager: RadioNotificationManager
    private lateinit var appConfig: AppConfig

    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)

    private val audioManager by lazy { getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    private val audioFocusRequest by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setAcceptsDelayedFocusGain(true)
                .setOnAudioFocusChangeListener(audioFocusChangeListener)
                .build()
        } else {
            null
        }
    }

    private val audioFocusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        when (focusChange) {
            AudioManager.AUDIOFOCUS_LOSS -> pause()
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> pause()
            AudioManager.AUDIOFOCUS_GAIN -> play()
        }
    }

    inner class LocalBinder : Binder() {
        fun getService(): RadioService = this@RadioService
    }

    private val binder = LocalBinder()

    override fun onCreate() {
        super.onCreate()

        serviceScope.launch {
            appConfig = (application as RadioApplication).configRepository.getAppConfig()

            // Inicializar ExoPlayer
            initializePlayer()

            // Inicializar MediaSession
            initializeMediaSession()

            // Inicializar NotificationManager
            notificationManager = RadioNotificationManager(
                this@RadioService,
                mediaSession.sessionToken
            )
        }
    }

    private fun initializePlayer() {
        player = ExoPlayer.Builder(this).build()
        player.setWakeMode(PowerManager.PARTIAL_WAKE_LOCK)
        player.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                when (playbackState) {
                    Player.STATE_READY -> {
                        updatePlaybackState(PlaybackStateCompat.STATE_PLAYING)
                    }
                    Player.STATE_BUFFERING -> {
                        updatePlaybackState(PlaybackStateCompat.STATE_BUFFERING)
                    }
                    Player.STATE_ENDED -> {
                        updatePlaybackState(PlaybackStateCompat.STATE_STOPPED)
                        // Intentar reconectar si está configurado
                        if (appConfig.reconnect_on_disconnect) {
                            player.prepare()
                            player.play()
                        }
                    }
                    Player.STATE_IDLE -> {
                        updatePlaybackState(PlaybackStateCompat.STATE_STOPPED)
                    }
                }
            }
        })
    }

    private fun initializeMediaSession() {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_IMMUTABLE
        )

        mediaSession = MediaSessionCompat(this, "RadioService").apply {
            setSessionActivity(pendingIntent)
            isActive = true
        }

        sessionToken = mediaSession.sessionToken

        mediaSessionConnector = MediaSessionConnector(mediaSession).apply {
            setPlayer(player)
        }

        // Actualizar metadatos
        val metadataBuilder = MediaMetadataCompat.Builder()
            .putString(MediaMetadataCompat.METADATA_KEY_TITLE, appConfig.app_name)
            .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, "Radio Stream")
            .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, "Live Streaming")

        mediaSession.setMetadata(metadataBuilder.build())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        MediaButtonReceiver.handleIntent(mediaSession, intent)
        return START_STICKY
    }

    override fun onBind(intent: Intent): IBinder {
        return binder
    }

    override fun onGetRoot(
        clientPackageName: String,
        clientUid: Int,
        rootHints: Bundle?
    ): BrowserRoot {
        return BrowserRoot("root", null)
    }

    override fun onLoadChildren(
        parentId: String,
        result: Result<MutableList<MediaBrowserCompat.MediaItem>>
    ) {
        result.sendResult(mutableListOf())
    }

    fun play() {
        if (requestAudioFocus()) {
            if (player.playbackState == Player.STATE_IDLE || player.currentMediaItem == null) {
                val mediaItem = MediaItem.fromUri(Uri.parse(appConfig.stream_url))
                player.setMediaItem(mediaItem)
                player.prepare()
            }
            player.play()
            startForeground()
        }
    }

    fun pause() {
        player.pause()
        abandonAudioFocus()
        stopForeground(false)
        notificationManager.updateNotification(false)
    }

    fun isPlaying(): Boolean {
        return player.isPlaying
    }

    private fun requestAudioFocus(): Boolean {
        val result = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioManager.requestAudioFocus(audioFocusRequest!!)
        } else {
            @Suppress("DEPRECATION")
            audioManager.requestAudioFocus(
                audioFocusChangeListener,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN
            )
        }
        return result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
    }

    private fun abandonAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioManager.abandonAudioFocusRequest(audioFocusRequest!!)
        } else {
            @Suppress("DEPRECATION")
            audioManager.abandonAudioFocus(audioFocusChangeListener)
        }
    }

    private fun startForeground() {
        if (appConfig.enable_notifications) {
            val notification = notificationManager.getNotification(true)
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun updatePlaybackState(state: Int) {
        val playbackStateBuilder = PlaybackStateCompat.Builder()
            .setActions(
                PlaybackStateCompat.ACTION_PLAY or
                        PlaybackStateCompat.ACTION_PAUSE or
                        PlaybackStateCompat.ACTION_STOP
            )
            .setState(state, 0, 1.0f)

        mediaSession.setPlaybackState(playbackStateBuilder.build())

        // Actualizar notificación
        if (state == PlaybackStateCompat.STATE_PLAYING) {
            notificationManager.updateNotification(true)
        } else {
            notificationManager.updateNotification(false)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaSession.release()
        player.release()
        serviceJob.cancel()
    }

    companion object {
        private const val NOTIFICATION_ID = 1
    }
}

// Archivo: app/src/main/java/com/example/miradioapp/service/RadioNotificationManager.kt
package com.example.miradioapp.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.core.app.NotificationCompat
import androidx.media.session.MediaButtonReceiver
import com.example.miradioapp.MainActivity
import com.example.miradioapp.R

class RadioNotificationManager(
    private val context: Context,
    private val token: MediaSessionCompat.Token
) {

    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Radio Playback",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Controles de reproducción para la radio en streaming"
                enableLights(true)
                lightColor = Color.BLUE
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun getNotification(isPlaying: Boolean): Notification {
        val playPauseAction = if (isPlaying) {
            NotificationCompat.Action(
                R.drawable.ic_pause,
                "Pause",
                MediaButtonReceiver.buildMediaButtonPendingIntent(
                    context,
                    PlaybackStateCompat.ACTION_PAUSE
                )
            )
        } else {
            NotificationCompat.Action(
                R.drawable.ic_play,
                "Play",
                MediaButtonReceiver.buildMediaButtonPendingIntent(
                    context,
                    PlaybackStateCompat.ACTION_PLAY
                )
            )
        }

        val mainActivityIntent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context, 0, mainActivityIntent, PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("Radio en streaming")
            .setContentText("Reproduciendo")
            .setSmallIcon(R.drawable.ic_radio)
            .setContentIntent(pendingIntent)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .addAction(playPauseAction)
            .setStyle(
                androidx.media.app.NotificationCompat.MediaStyle()
                    .setMediaSession(token)
                    .setShowActionsInCompactView(0)
                    .setShowCancelButton(true)
                    .setCancelButtonIntent(
                        MediaButtonReceiver.buildMediaButtonPendingIntent(
                            context,
                            PlaybackStateCompat.ACTION_STOP
                        )
                    )
            )
            .setDeleteIntent(
                MediaButtonReceiver.buildMediaButtonPendingIntent(
                    context,
                    PlaybackStateCompat.ACTION_STOP
                )
            )
            .build()
    }

    fun updateNotification(isPlaying: Boolean) {
        notificationManager.notify(NOTIFICATION_ID, getNotification(isPlaying))
    }

    companion object {
        private const val CHANNEL_ID = "radio_channel"
        private const val NOTIFICATION_ID = 1
    }
}