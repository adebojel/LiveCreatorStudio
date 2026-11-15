package id.livecreator.studio

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
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
    private val pendingTexts = ArrayDeque<String>()

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        tts = TextToSpeech(applicationContext, this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                tts?.stop()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_SPEAK -> {
                intent.getStringExtra(EXTRA_TEXT)?.take(4000)?.let { text ->
                    if (pendingTexts.size >= MAX_PENDING_ITEMS) pendingTexts.removeFirst()
                    pendingTexts.addLast(text)
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
            val text = pendingTexts.removeFirst()
            engine.speak(text, TextToSpeech.QUEUE_ADD, null, "livecreator-bg-${System.currentTimeMillis()}")
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
        val stopService = PendingIntent.getService(
            this, 2,
            Intent(this, LiveBackgroundService::class.java).setAction(ACTION_STOP),
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
        private const val CHANNEL_ID = "livecreator_background"
        private const val NOTIFICATION_ID = 4107
        private const val MAX_PENDING_ITEMS = 40
    }
}
