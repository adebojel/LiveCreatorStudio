package id.livecreator.studio

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.speech.tts.TextToSpeech
import java.util.Locale

/**
 * User-started foreground service for persistent creator audio tasks.
 * Android can still stop/restrict services; START_STICKY is a recovery hint, not a guarantee.
 */
class LiveBackgroundService : Service(), TextToSpeech.OnInitListener {
    private var tts: TextToSpeech? = null
    private var ttsReady = false
    private data class SpeechRequest(val text: String, val rate: Float, val pitch: Float)
    private val pendingTexts = ArrayDeque<SpeechRequest>()

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        tts = TextToSpeech(applicationContext, this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                pendingTexts.clear()
                tts?.stop()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_SPEAK -> {
                intent.getStringExtra(EXTRA_TEXT)?.take(4000)?.takeIf { it.isNotBlank() }?.let { text ->
                    if (pendingTexts.size >= MAX_PENDING_ITEMS) pendingTexts.removeFirst()
                    val rate = intent.getFloatExtra(EXTRA_RATE, 1.0f).coerceIn(0.5f, 2.0f)
                    val pitch = intent.getFloatExtra(EXTRA_PITCH, 1.0f).coerceIn(0.5f, 2.0f)
                    pendingTexts.addLast(SpeechRequest(text, rate, pitch))
                }
            }
        }

        startAsForeground()
        speakPendingIfReady()
        return START_STICKY
    }

    override fun onInit(status: Int) {
        ttsReady = status == TextToSpeech.SUCCESS
        if (ttsReady) {
            tts?.setLanguage(Locale("id", "ID"))
            speakPendingIfReady()
        }
    }

    private fun speakPendingIfReady() {
        val engine = tts ?: return
        if (!ttsReady || pendingTexts.isEmpty()) return
        val result = engine.setLanguage(Locale("id", "ID"))
        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) return
        while (pendingTexts.isNotEmpty()) {
            val request = pendingTexts.removeFirst()
            val utteranceParams = Bundle().apply {
                putFloat(TextToSpeech.Engine.KEY_PARAM_RATE, request.rate)
                putFloat(TextToSpeech.Engine.KEY_PARAM_PITCH, request.pitch)
            }
            engine.speak(request.text, TextToSpeech.QUEUE_ADD, utteranceParams, "livecreator-bg-${System.currentTimeMillis()}")
        }
    }

    private fun startAsForeground() {
        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun buildNotification(): Notification {
        val openApp = PendingIntent.getActivity(
            this, 1,
            Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val stopService = PendingIntent.getBroadcast(
            this, 2,
            Intent(this, StopBackgroundReceiver::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, CHANNEL_ID)
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
        }
        return builder
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle("LiveCreator Studio aktif")
            .setContentText("Layanan audio latar belakang berjalan")
            .setContentIntent(openApp)
            .setOngoing(true)
            .addAction(android.R.drawable.ic_media_pause, "Hentikan", stopService)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    "LiveCreator background audio",
                    NotificationManager.IMPORTANCE_LOW
                ).apply { description = "Status layanan audio latar belakang" }
            )
        }
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        // Keep the user-started foreground service alive when the task is swiped away.
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val ACTION_START = "id.livecreator.studio.action.START"
        const val ACTION_SPEAK = "id.livecreator.studio.action.SPEAK"
        const val ACTION_STOP = "id.livecreator.studio.action.STOP"
        const val EXTRA_TEXT = "extra_text"
        const val EXTRA_RATE = "extra_rate"
        const val EXTRA_PITCH = "extra_pitch"
        private const val CHANNEL_ID = "livecreator_background"
        private const val NOTIFICATION_ID = 4107
        private const val MAX_PENDING_ITEMS = 40
    }
}
