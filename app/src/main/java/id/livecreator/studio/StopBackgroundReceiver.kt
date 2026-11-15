package id.livecreator.studio

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Stops the app's own foreground audio service from its notification action. */
class StopBackgroundReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != LiveBackgroundService.ACTION_STOP) return
        context.stopService(Intent(context, LiveBackgroundService::class.java))
    }
}
