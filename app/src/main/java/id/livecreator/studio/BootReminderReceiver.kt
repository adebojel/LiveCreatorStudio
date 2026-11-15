package id.livecreator.studio

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.os.Build

/**
 * Android 15+ restricts starting some foreground-service types from BOOT_COMPLETED.
 * We therefore post a user-visible resume reminder instead of silently forcing startup.
 */
class BootReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != Intent.ACTION_BOOT_COMPLETED) return
        val prefs = context.getSharedPreferences("livecreator_prefs", Context.MODE_PRIVATE)
        if (!prefs.getBoolean("resume_reminder_enabled", true)) return

        val channelId = "livecreator_resume"
        val manager = context.getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(channelId, "Pengingat LiveCreator", NotificationManager.IMPORTANCE_DEFAULT)
            )
        }
        val openApp = PendingIntent.getActivity(
            context, 9, Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(context, channelId)
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(context)
        }
        manager.notify(
            4108,
            builder.setSmallIcon(android.R.drawable.ic_media_play)
                .setContentTitle("LiveCreator Studio siap")
                .setContentText("Ketuk untuk mengaktifkan kembali layanan latar belakang.")
                .setContentIntent(openApp)
                .setAutoCancel(true)
                .build()
        )
    }
}
