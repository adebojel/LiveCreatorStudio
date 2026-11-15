package id.livecreator.studio

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build

/** Handles explicit audio-service actions from the persistent notification. */
class StopBackgroundReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        when (intent?.action) {
            LiveBackgroundService.ACTION_STOP -> {
                context.stopService(Intent(context, LiveBackgroundService::class.java))
            }
            LiveBackgroundService.ACTION_CLEAR_QUEUE -> {
                val serviceIntent = Intent(context, LiveBackgroundService::class.java)
                    .setAction(LiveBackgroundService.ACTION_CLEAR_QUEUE)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(serviceIntent)
                } else {
                    context.startService(serviceIntent)
                }
            }
        }
    }
}
