package id.livecreator.studio

import android.app.Service
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import kotlin.math.max

/** Optional user-enabled overlay shortcut; the audio foreground service stays independent. */
class FloatingControlService : Service() {
    private lateinit var windowManager: WindowManager
    private var panel: LinearLayout? = null
    private var expanded = false

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> { removePanel(); stopSelf(); return START_NOT_STICKY }
            ACTION_REFRESH -> { if (panel != null) { removePanel(); showPanel() }; return START_STICKY }
            ACTION_HIDE -> { removePanel(); return START_STICKY }
            ACTION_SHOW, null -> showPanel()
        }
        return START_STICKY
    }

    private fun showPanel() {
        if (!Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "Izinkan tampil di atas aplikasi lain terlebih dahulu.", Toast.LENGTH_LONG).show()
            stopSelf()
            return
        }
        if (panel != null) return
        val prefs = getSharedPreferences(PREFS, MODE_PRIVATE)
        val size = prefs.getFloat("size", 58f).toInt().coerceIn(44, 84)
        val opacity = prefs.getFloat("opacity", 0.82f).coerceIn(0.25f, 1f)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            background = GradientDrawable().apply {
                cornerRadius = dp(22).toFloat()
                setColor(Color.argb((opacity * 205).toInt(), 13, 24, 43))
                setStroke(dp(1), Color.argb(220, 0, 217, 255))
            }
            elevation = dp(10).toFloat()
            setPadding(dp(7), dp(7), dp(7), dp(7))
        }
        val bubble = TextView(this).apply {
            text = "LC"
            textSize = 16f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.argb((opacity * 225).toInt(), 0, 105, 145))
                setStroke(dp(1), Color.CYAN)
            }
            alpha = opacity
            contentDescription = "LiveCreator Studio. Bola kontrol mengambang. Ketuk untuk membuka atau menutup panel, geser untuk memindahkan."
            isFocusable = true
            setOnClickListener {
                expanded = !expanded
                removePanel()
                showPanel()
            }
        }
        root.addView(bubble, LinearLayout.LayoutParams(dp(size), dp(size)))
        if (expanded) {
            addLabel(root, "LIVECREATOR • KONTROL")
            addAction(root, "Aktifkan layanan") { sendAudioAction(LiveBackgroundService.ACTION_START) }
            addAction(root, "Matikan layanan") { stopService(Intent(this, LiveBackgroundService::class.java)) }
            addAction(root, "Buka aplikasi") {
                packageManager.getLaunchIntentForPackage(packageName)?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)?.let(::startActivity)
            }
            addAction(root, "Sembunyikan") { expanded = false; removePanel() }
            addAction(root, "Tutup panel") {
                prefs.edit().putBoolean("enabled", false).apply()
                removePanel()
                stopSelf()
            }
        }
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        else WindowManager.LayoutParams.TYPE_PHONE
        val lp = WindowManager.LayoutParams(
            if (expanded) dp(190) else dp(size + 14),
            WindowManager.LayoutParams.WRAP_CONTENT,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = prefs.getInt("x", 0)
            y = max(dp(24), prefs.getInt("y", dp(180)))
        }
        var downX = 0f
        var downY = 0f
        var startX = 0
        var startY = 0
        var moved = false
        bubble.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    downX = event.rawX; downY = event.rawY
                    startX = lp.x; startY = lp.y; moved = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - downX).toInt()
                    val dy = (event.rawY - downY).toInt()
                    if (kotlin.math.abs(dx) > dp(4) || kotlin.math.abs(dy) > dp(4)) moved = true
                    if (moved) {
                        lp.x = startX + dx
                        lp.y = max(dp(24), startY + dy)
                        runCatching { windowManager.updateViewLayout(root, lp) }
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    prefs.edit().putInt("x", lp.x).putInt("y", lp.y).apply()
                    if (!moved) bubble.performClick()
                    true
                }
                else -> true
            }
        }
        runCatching { windowManager.addView(root, lp); panel = root }
            .onFailure {
                Toast.makeText(this, "Panel mengambang tidak dapat ditampilkan.", Toast.LENGTH_SHORT).show()
                stopSelf()
            }
    }

    private fun addLabel(parent: LinearLayout, label: String) {
        parent.addView(TextView(this).apply {
            text = label; textSize = 10f; setTextColor(Color.CYAN); gravity = Gravity.CENTER
            setPadding(0, dp(6), 0, dp(6))
        })
    }

    private fun addAction(parent: LinearLayout, label: String, action: () -> Unit) {
        parent.addView(TextView(this).apply {
            text = label; textSize = 12f; setTextColor(Color.WHITE); gravity = Gravity.CENTER
            setPadding(dp(8), dp(9), dp(8), dp(9))
            minHeight = dp(48)
            isFocusable = true
            contentDescription = label
            background = getDrawable(android.R.drawable.list_selector_background)
            setOnClickListener { action() }
        }, LinearLayout.LayoutParams(-1, -2))
    }

    private fun sendAudioAction(action: String) {
        val serviceIntent = Intent(this, LiveBackgroundService::class.java).setAction(action)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(serviceIntent)
        else startService(serviceIntent)
    }

    private fun removePanel() {
        panel?.let { view -> runCatching { windowManager.removeView(view) } }
        panel = null
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    override fun onDestroy() { removePanel(); super.onDestroy() }
    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val ACTION_SHOW = "id.livecreator.studio.FLOAT_SHOW"
        const val ACTION_STOP = "id.livecreator.studio.FLOAT_STOP"
        const val ACTION_HIDE = "id.livecreator.studio.FLOAT_HIDE"
        const val ACTION_REFRESH = "id.livecreator.studio.FLOAT_REFRESH"
        private const val PREFS = "floating_control"
    }
}
