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
import kotlin.math.abs
import kotlin.math.max

/** Compact magnetic edge-dock overlay. It only exposes controls belonging to this app. */
class FloatingControlService : Service() {
    private lateinit var windowManager: WindowManager
    private var panel: LinearLayout? = null
    private var expanded = false
    private var dockedRight = false

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
        val size = sanitizeFloatingBubbleSize(prefs.getFloat("size", 42f)).toInt()
        val opacity = sanitizeFloatingBubbleOpacity(prefs.getFloat("opacity", 0.88f))
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.argb((opacity * 190).toInt(), 9, 24, 43))
                setStroke(dp(1), Color.argb(230, 0, 220, 255))
            }
            elevation = dp(14).toFloat()
            setPadding(dp(3), dp(3), dp(3), dp(3))
        }
        val bubble = TextView(this).apply {
            text = "◈"
            textSize = 27f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.argb((opacity * 215).toInt(), 18, 76, 105))
                setStroke(dp(1), Color.rgb(112, 242, 255))
            }
            alpha = opacity
            elevation = dp(8).toFloat()
            contentDescription = "LiveCreator Studio. Bola kontrol magnetik. Ketuk untuk membuka panel, geser untuk memindahkan."
            isFocusable = true
        }
        root.addView(bubble, LinearLayout.LayoutParams(dp(size), dp(size)))
        if (expanded) {
            root.background = GradientDrawable().apply {
                cornerRadius = dp(20).toFloat()
                setColor(Color.argb(238, 12, 25, 46))
                setStroke(dp(1), Color.argb(220, 0, 217, 255))
            }
            addLabel(root, "LIVE CREATOR")
            addAction(root, "Mulai audio") { sendAudioAction(LiveBackgroundService.ACTION_START) }
            addAction(root, "Matikan audio") { stopService(Intent(this, LiveBackgroundService::class.java)) }
            addAction(root, "Buka aplikasi") {
                packageManager.getLaunchIntentForPackage(packageName)?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)?.let(::startActivity)
            }
            addAction(root, "Sembunyikan") { expanded = false; removePanel() }
            addAction(root, "Nonaktifkan bola") {
                prefs.edit().putBoolean("enabled", false).apply()
                removePanel()
                stopSelf()
            }
        }
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY else WindowManager.LayoutParams.TYPE_PHONE
        val displayWidth = resources.displayMetrics.widthPixels
        val width = if (expanded) dp(184) else dp(size + 6)
        val savedX = if (expanded) {
            if (prefs.getBoolean("right", false)) displayWidth - width else 0
        } else prefs.getInt("x", dp(8))
        val lp = WindowManager.LayoutParams(
            width, WindowManager.LayoutParams.WRAP_CONTENT, type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = savedX.coerceIn(-width + dp(16), displayWidth - dp(16))
            y = max(dp(36), prefs.getInt("y", dp(190)))
        }
        dockedRight = lp.x > displayWidth / 2
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
                    if (abs(dx) > dp(5) || abs(dy) > dp(5)) moved = true
                    if (moved) {
                        lp.x = (startX + dx).coerceIn(0, max(0, displayWidth - width))
                        lp.y = (startY + dy).coerceIn(dp(36), resources.displayMetrics.heightPixels - dp(90))
                        runCatching { windowManager.updateViewLayout(root, lp) }
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (moved && !expanded) {
                        dockedRight = lp.x + width / 2 >= displayWidth / 2
                        // Leave a 16dp grip visible so the user can pull the bubble back.
                        lp.x = if (dockedRight) displayWidth - dp(16) else -width + dp(16)
                        lp.x = lp.x.coerceIn(-width + dp(16), displayWidth - dp(16))
                        runCatching { windowManager.updateViewLayout(root, lp) }
                    } else if (!moved) {
                        expanded = !expanded
                        removePanel()
                        showPanel()
                        return@setOnTouchListener true
                    }
                    prefs.edit().putInt("x", lp.x).putInt("y", lp.y).putBoolean("right", dockedRight).apply()
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
            text = label; textSize = 10f; setTextColor(Color.rgb(75, 230, 255)); gravity = Gravity.CENTER
            setPadding(0, dp(6), 0, dp(6))
        })
    }

    private fun addAction(parent: LinearLayout, label: String, action: () -> Unit) {
        parent.addView(TextView(this).apply {
            text = label; textSize = 12f; setTextColor(Color.WHITE); gravity = Gravity.CENTER
            setPadding(dp(8), dp(8), dp(8), dp(8))
            minHeight = dp(44)
            isFocusable = true
            contentDescription = label
            background = getDrawable(android.R.drawable.list_selector_background)
            setOnClickListener { action() }
        }, LinearLayout.LayoutParams(-1, -2))
    }

    private fun sendAudioAction(action: String) {
        val serviceIntent = Intent(this, LiveBackgroundService::class.java).setAction(action)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(serviceIntent) else startService(serviceIntent)
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
