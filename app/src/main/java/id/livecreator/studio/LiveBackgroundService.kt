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
import android.speech.tts.UtteranceProgressListener
import java.util.Locale

/**
 * User-started foreground service for persistent creator audio tasks.
 * Android can still stop/restrict services; START_STICKY is a recovery hint, not a guarantee.
 */
class LiveBackgroundService : Service(), TextToSpeech.OnInitListener {
    private var tts: TextToSpeech? = null
    private var ttsReady = false
    private var speaking = false
    private var notificationText = "Layanan audio latar belakang aktif."
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
                speaking = false
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
        if (!ttsReady) {
            pendingTexts.clear()
            updateNotification("Mesin TTS perangkat gagal diinisialisasi.")
            return
        }
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) = Unit
            override fun onDone(utteranceId: String?) {
                speaking = false
                if (pendingTexts.isEmpty()) updateNotification("Pembacaan teks selesai.")
                speakPendingIfReady()
            }
            override fun onError(utteranceId: String?) {
                speaking = false
                updateNotification("Terjadi kesalahan saat membacakan teks.")
                speakPendingIfReady()
            }
        })
        speakPendingIfReady()
    }

    private fun speakPendingIfReady() {
        val engine = tts ?: return
        if (!ttsReady || speaking || pendingTexts.isEmpty()) return
        val result = engine.setLanguage(Locale("id", "ID"))
        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            pendingTexts.clear()
            updateNotification("Bahasa Indonesia tidak tersedia pada mesin TTS perangkat.")
            return
        }

        val request = pendingTexts.removeFirst()
        engine.setSpeechRate(request.rate)
        engine.setPitch(request.pitch)
        speaking = true
        updateNotification("Sedang membacakan teks.")
        val utteranceId = "livecreator-bg-${System.currentTimeMillis()}"
        val resultCode = engine.speak(request.text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
        if (resultCode == TextToSpeech.ERROR) {
            speaking = false
            updateNotification("Mesin TTS menolak permintaan pembacaan.")
            speakPendingIfReady()
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
        publishServiceState(true)
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
            .setContentText(notificationText)
            .setContentIntent(openApp)
            .setOngoing(true)
            .addAction(android.R.drawable.ic_media_pause, "Hentikan", stopService)
            .build()
    }

    private fun updateNotification(text: String) {
        notificationText = text
        getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, buildNotification())
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
        publishServiceState(false)
        tts?.stop()
        tts?.shutdown()
        tts = null
        super.onDestroy()
    }

    private fun publishServiceState(running: Boolean) {
        sendBroadcast(
            Intent(ACTION_STATUS)
                .setPackage(packageName)
                .putExtra(EXTRA_RUNNING, running)
                .putExtra(EXTRA_STATUS_TEXT, notificationText)
        )
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val ACTION_START = "id.livecreator.studio.action.START"
        const val ACTION_SPEAK = "id.livecreator.studio.action.SPEAK"
        const val ACTION_STOP = "id.livecreator.studio.action.STOP"
        const val ACTION_STATUS = "id.livecreator.studio.action.STATUS"
        const val EXTRA_RUNNING = "extra_running"
        const val EXTRA_STATUS_TEXT = "extra_status_text"
        const val EXTRA_TEXT = "extra_text"
        const val EXTRA_RATE = "extra_rate"
        const val EXTRA_PITCH = "extra_pitch"
        private const val CHANNEL_ID = "livecreator_background"
        private const val NOTIFICATION_ID = 4107
        private const val MAX_PENDING_ITEMS = 40
    }
}
