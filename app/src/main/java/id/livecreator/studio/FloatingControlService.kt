package id.livecreator.studio

import android.app.Service
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.WindowManager
import android.view.animation.DecelerateInterpolator
import android.animation.ValueAnimator
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import kotlin.math.abs
import kotlin.math.max

/** Compact magnetic glass overlay with outside-tap dismissal and quick sound effects. */
class FloatingControlService : Service() {
    private lateinit var windowManager: WindowManager
    private var panel: LinearLayout? = null
    private var expanded = false
    private var dockedRight = false
    private var dockAnimator: ValueAnimator? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private var tone: ToneGenerator? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> { removePanel(); stopSelf(); return START_NOT_STICKY }
            ACTION_REFRESH -> { if (panel != null) { removePanel(); showPanel() }; return START_STICKY }
            ACTION_HIDE -> { expanded = false; removePanel(); return START_STICKY }
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
        val size = sanitizeFloatingBubbleSize(prefs.getFloat("size", 40f)).toInt()
        val opacity = sanitizeFloatingBubbleOpacity(prefs.getFloat("opacity", 0.88f))
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            background = glassBackground(20, 238)
            elevation = dp(18).toFloat()
            setPadding(dp(8), dp(8), dp(8), dp(8))
        }
        val bubble = TextView(this).apply {
            text = "🎙"
            textSize = 23f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.argb((opacity * 220).toInt(), 10, 48, 78))
                setStroke(dp(1), Color.rgb(85, 231, 255))
            }
            alpha = opacity
            elevation = dp(10).toFloat()
            contentDescription = "LiveCreator Studio. Bola kontrol magnetik. Ketuk untuk membuka, ketuk di luar untuk menutup, geser untuk memindahkan."
            isFocusable = true
        }
        root.addView(bubble, LinearLayout.LayoutParams(dp(size), dp(size)))
        if (expanded) {
            root.addView(TextView(this).apply {
                text = "LIVE CREATOR"
                textSize = 10f
                setTextColor(Color.rgb(75, 230, 255))
                gravity = Gravity.CENTER
                setPadding(0, dp(5), 0, dp(7))
            })
            addActionRow(root, listOf(
                Triple("▶", "Audio", { sendAudioAction(LiveBackgroundService.ACTION_START) }),
                Triple("■", "Stop", { stopService(Intent(this, LiveBackgroundService::class.java)) })
            ))
            addActionRow(root, listOf(
                Triple("😂", "Ketawa", { playEffect(Effect.LAUGH) }),
                Triple("💋", "Kiss", { playEffect(Effect.KISS) }),
                Triple("⚡", "Petir", { playEffect(Effect.THUNDER) }),
                Triple("👏", "Tepuk", { playEffect(Effect.CLAP) })
            ))
            addActionRow(root, listOf(
                Triple("🎉", "Party", { playEffect(Effect.PARTY) }),
                Triple("✨", "Magic", { playEffect(Effect.MAGIC) }),
                Triple("🚨", "Alert", { playEffect(Effect.ALERT) }),
                Triple("🎮", "Game", { playEffect(Effect.GAME) })
            ))
            addActionRow(root, listOf(
                Triple("⌂", "App", {
                    packageManager.getLaunchIntentForPackage(packageName)?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)?.let(::startActivity)
                }),
                Triple("⌄", "Tutup", { expanded = false; removePanel(); showPanel() }),
                Triple("◌", "Hide", { expanded = false; removePanel() }),
                Triple("×", "Off", {
                    prefs.edit().putBoolean("enabled", false).apply()
                    removePanel()
                    stopSelf()
                })
            ))
        }

        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY else WindowManager.LayoutParams.TYPE_PHONE
        val displayWidth = resources.displayMetrics.widthPixels
        val width = if (expanded) dp(232) else dp(size + 6)
        val savedX = if (expanded) {
            if (prefs.getBoolean("right", false)) displayWidth - width else 0
        } else prefs.getInt("x", dp(8))
        val flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
            WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
        val lp = WindowManager.LayoutParams(
            width, WindowManager.LayoutParams.WRAP_CONTENT, type, flags, PixelFormat.TRANSLUCENT
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

        root.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_OUTSIDE && expanded) {
                expanded = false
                removePanel()
                showPanel()
                true
            } else false
        }
        bubble.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    dockAnimator?.cancel()
                    downX = event.rawX
                    downY = event.rawY
                    startX = lp.x
                    startY = lp.y
                    moved = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - downX).toInt()
                    val dy = (event.rawY - downY).toInt()
                    if (abs(dx) > dp(4) || abs(dy) > dp(4)) moved = true
                    if (moved && !expanded) {
                        lp.x = (startX + dx).coerceIn(-width + dp(16), max(0, displayWidth - dp(16)))
                        lp.y = (startY + dy).coerceIn(dp(36), resources.displayMetrics.heightPixels - dp(90))
                        runCatching { windowManager.updateViewLayout(root, lp) }
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (moved && !expanded) {
                        dockedRight = lp.x + width / 2 >= displayWidth / 2
                        val targetX = if (dockedRight) displayWidth - dp(16) else -width + dp(16)
                        animateDock(root, lp, targetX)
                    } else if (!moved) {
                        expanded = !expanded
                        removePanel()
                        showPanel()
                        return@setOnTouchListener true
                    }
                    prefs.edit().putInt("y", lp.y).putBoolean("right", dockedRight).apply()
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

    private fun glassBackground(radiusDp: Int, alpha: Int) = GradientDrawable().apply {
        cornerRadius = dp(radiusDp).toFloat()
        setColor(Color.argb(alpha, 10, 24, 46))
        setStroke(dp(1), Color.argb(235, 0, 218, 255))
    }

    private fun addActionRow(parent: LinearLayout, actions: List<Triple<String, String, () -> Unit>>) {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }
        actions.forEach { (icon, label, action) ->
            val cell = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                background = GradientDrawable().apply {
                    cornerRadius = dp(13).toFloat()
                    setColor(Color.argb(105, 31, 75, 111))
                    setStroke(dp(1), Color.argb(125, 74, 213, 255))
                }
                setPadding(dp(3), dp(5), dp(3), dp(4))
                setOnClickListener { action() }
                contentDescription = label
                isClickable = true
                isFocusable = true
            }
            cell.addView(TextView(this).apply {
                text = icon
                textSize = 19f
                gravity = Gravity.CENTER
                setTextColor(Color.WHITE)
            })
            cell.addView(TextView(this).apply {
                text = label
                textSize = 8f
                gravity = Gravity.CENTER
                setTextColor(Color.rgb(195, 224, 244))
            })
            row.addView(cell, LinearLayout.LayoutParams(0, dp(48), 1f).apply {
                setMargins(dp(2), dp(2), dp(2), dp(2))
            })
        }
        parent.addView(row, LinearLayout.LayoutParams(-1, -2))
    }

    private enum class Effect { LAUGH, KISS, THUNDER, CLAP, PARTY, MAGIC, ALERT, GAME }

    /** Short original UI tones; these are cues, not recordings of human laughter or real thunder. */
    private fun playEffect(effect: Effect) {
        runCatching { tone?.release() }
        tone = runCatching { ToneGenerator(AudioManager.STREAM_MUSIC, 82) }.getOrNull() ?: return
        val generator = tone ?: return
        val sequence = when (effect) {
            Effect.LAUGH -> listOf(ToneGenerator.TONE_PROP_BEEP to 90, ToneGenerator.TONE_PROP_ACK to 110, ToneGenerator.TONE_PROP_BEEP2 to 120)
            Effect.KISS -> listOf(ToneGenerator.TONE_DTMF_9 to 115, ToneGenerator.TONE_PROP_ACK to 65)
            Effect.THUNDER -> listOf(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD to 360)
            Effect.CLAP -> listOf(ToneGenerator.TONE_PROP_ACK to 80, ToneGenerator.TONE_PROP_ACK to 80, ToneGenerator.TONE_PROP_ACK to 80)
            Effect.PARTY -> listOf(ToneGenerator.TONE_PROP_BEEP2 to 90, ToneGenerator.TONE_PROP_BEEP to 90, ToneGenerator.TONE_PROP_ACK to 140)
            Effect.MAGIC -> listOf(ToneGenerator.TONE_PROP_PROMPT to 180, ToneGenerator.TONE_PROP_ACK to 120)
            Effect.ALERT -> listOf(ToneGenerator.TONE_SUP_ERROR to 160, ToneGenerator.TONE_SUP_ERROR to 160)
            Effect.GAME -> listOf(ToneGenerator.TONE_DTMF_5 to 100, ToneGenerator.TONE_DTMF_8 to 100)
        }
        var delay = 0L
        sequence.forEach { (toneType, duration) ->
            mainHandler.postDelayed({ runCatching { generator.startTone(toneType, duration) } }, delay)
            delay += duration + 55L
        }
        mainHandler.postDelayed({
            if (tone === generator) {
                runCatching { generator.release() }
                tone = null
            }
        }, delay + 100L)
    }

    private fun animateDock(root: LinearLayout, lp: WindowManager.LayoutParams, targetX: Int) {
        dockAnimator?.cancel()
        dockAnimator = ValueAnimator.ofInt(lp.x, targetX).apply {
            duration = 190L
            interpolator = DecelerateInterpolator(1.7f)
            addUpdateListener { animator ->
                lp.x = animator.animatedValue as Int
                runCatching { windowManager.updateViewLayout(root, lp) }
            }
            start()
        }
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
    override fun onDestroy() {
        dockAnimator?.cancel()
        mainHandler.removeCallbacksAndMessages(null)
        runCatching { tone?.release() }
        tone = null
        removePanel()
        super.onDestroy()
    }
    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val ACTION_SHOW = "id.livecreator.studio.FLOAT_SHOW"
        const val ACTION_STOP = "id.livecreator.studio.FLOAT_STOP"
        const val ACTION_HIDE = "id.livecreator.studio.FLOAT_HIDE"
        const val ACTION_REFRESH = "id.livecreator.studio.FLOAT_REFRESH"
        private const val PREFS = "floating_control"
    }
}
