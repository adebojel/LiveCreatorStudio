package id.livecreator.studio

import android.app.Service
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.VelocityTracker
import android.view.View
import android.view.WindowManager
import android.view.animation.DecelerateInterpolator
import android.animation.ValueAnimator
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.abs
import kotlin.math.max

/** Compact glass quick-actions overlay. Only controls LiveCreator Studio features. */
class FloatingControlService : Service() {
    private lateinit var windowManager: WindowManager
    private var panel: LinearLayout? = null
    private var bubble: TextView? = null
    private var expanded = false
    private var dockedRight = false
    private var player: MediaPlayer? = null
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var dockAnimator: ValueAnimator? = null

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
            background = glassBackground(20, opacity)
            elevation = dp(18).toFloat()
            setPadding(dp(8), dp(8), dp(8), dp(8))
        }
        val mark = TextView(this).apply {
            text = "◉"
            textSize = 25f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.argb((opacity * 215).toInt(), 18, 76, 105))
                setStroke(dp(1), Color.rgb(112, 242, 255))
            }
            alpha = opacity
            elevation = dp(8).toFloat()
            contentDescription = "LiveCreator Studio. Ketuk untuk membuka pintasan, geser untuk memindahkan."
            isFocusable = true
        }
        bubble = mark
        root.addView(mark, LinearLayout.LayoutParams(dp(size), dp(size)).apply { gravity = Gravity.CENTER })

        if (expanded) {
            addLabel(root, "LIVE CREATOR  •  QUICK")
            addEffectRow(root, listOf(
                Effect("😂", "Ketawa", listOf("laugh", "laughter", "haha", "giggle")),
                Effect("😘", "Cium", listOf("kiss", "smooch")),
                Effect("⚡", "Petir", listOf("electric", "lightning", "thunder", "zap"))
            ))
            addEffectRow(root, listOf(
                Effect("👏", "Tepuk", listOf("clap", "applause")),
                Effect("💥", "Impact", listOf("impact", "hit", "punch", "boom")),
                Effect("🎮", "Game", listOf("retro", "game", "8-bit", "arcade"))
            ))
            addDivider(root)
            addActionRow(root, listOf(
                Action("▶", "Audio", "Mulai audio") { sendAudioAction(LiveBackgroundService.ACTION_START) },
                Action("■", "Stop", "Matikan audio") { stopService(Intent(this, LiveBackgroundService::class.java)) },
                Action("⌂", "App", "Buka aplikasi") {
                    packageManager.getLaunchIntentForPackage(packageName)?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)?.let(::startActivity)
                }
            ))
            addActionRow(root, listOf(
                Action("×", "Tutup", "Tutup panel") { expanded = false; removePanel(); showPanel() },
                Action("◌", "Sembunyi", "Sembunyikan bola") { expanded = false; removePanel() },
                Action("⏻", "Off", "Nonaktifkan bola") {
                    prefs.edit().putBoolean("enabled", false).apply()
                    removePanel()
                    stopSelf()
                }
            ))
            TextView(this).apply {
                text = "Ketuk area luar untuk menutup"
                textSize = 9f
                setTextColor(Color.rgb(145, 185, 205))
                gravity = Gravity.CENTER
                setPadding(0, dp(5), 0, 0)
            }.also(root::addView)
        }

        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY else WindowManager.LayoutParams.TYPE_PHONE
        val displayWidth = resources.displayMetrics.widthPixels
        val width = if (expanded) dp(218) else dp(size + 8)
        val height = WindowManager.LayoutParams.WRAP_CONTENT
        val savedRight = prefs.getBoolean("right", false)
        val savedX = if (expanded) {
            if (savedRight) displayWidth - width else 0
        } else prefs.getInt("x", dp(8))
        val lp = WindowManager.LayoutParams(
            width, height, type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = savedX.coerceIn(-width + dp(18), displayWidth - dp(18))
            y = max(dp(48), prefs.getInt("y", dp(190)))
        }
        dockedRight = savedRight

        // Android sends ACTION_OUTSIDE to this non-modal overlay while allowing the tapped app underneath to receive the tap.
        root.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_OUTSIDE && expanded) {
                expanded = false
                removePanel()
                showPanel()
                true
            } else false
        }

        var downX = 0f
        var downY = 0f
        var startX = 0
        var startY = 0
        var moved = false
        var velocity: VelocityTracker? = null
        mark.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    dockAnimator?.cancel()
                    velocity = VelocityTracker.obtain().also { it.addMovement(event) }
                    downX = event.rawX; downY = event.rawY
                    startX = lp.x; startY = lp.y; moved = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    velocity?.addMovement(event)
                    val dx = (event.rawX - downX).toInt()
                    val dy = (event.rawY - downY).toInt()
                    if (abs(dx) > dp(4) || abs(dy) > dp(4)) moved = true
                    if (moved) {
                        val targetX = (startX + dx).coerceIn(0, max(0, displayWidth - width))
                        val targetY = (startY + dy).coerceIn(dp(48), resources.displayMetrics.heightPixels - dp(100))
                        // A light low-pass filter removes the stiff, jittery feel without adding drag lag.
                        lp.x = ((lp.x * 0.28f) + (targetX * 0.72f)).toInt()
                        lp.y = ((lp.y * 0.28f) + (targetY * 0.72f)).toInt()
                        runCatching { windowManager.updateViewLayout(root, lp) }
                    }
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    velocity?.recycle(); velocity = null
                    if (moved && !expanded) {
                        dockedRight = lp.x + width / 2 >= displayWidth / 2
                        val grip = dp(18)
                        val targetX = if (dockedRight) displayWidth - grip else -width + grip
                        val targetY = lp.y.coerceIn(dp(48), resources.displayMetrics.heightPixels - dp(100))
                        animateDock(root, lp, targetX, targetY)
                        prefs.edit().putBoolean("right", dockedRight).putInt("y", targetY).apply()
                    } else if (!moved) {
                        expanded = !expanded
                        removePanel()
                        showPanel()
                        return@setOnTouchListener true
                    }
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

    private fun glassBackground(radius: Int, opacity: Float) = GradientDrawable(
        GradientDrawable.Orientation.TL_BR,
        intArrayOf(
            Color.argb((opacity * 245).toInt(), 12, 30, 55),
            Color.argb((opacity * 232).toInt(), 18, 45, 72),
            Color.argb((opacity * 238).toInt(), 31, 22, 65)
        )
    ).apply {
        cornerRadius = dp(radius).toFloat()
        setStroke(dp(1), Color.rgb(45, 220, 255))
    }

    private fun animateDock(root: View, lp: WindowManager.LayoutParams, targetX: Int, targetY: Int) {
        dockAnimator?.cancel()
        val fromX = lp.x
        val fromY = lp.y
        dockAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 190
            interpolator = DecelerateInterpolator(1.7f)
            addUpdateListener { animator ->
                val fraction = animator.animatedValue as Float
                lp.x = (fromX + (targetX - fromX) * fraction).toInt()
                lp.y = (fromY + (targetY - fromY) * fraction).toInt()
                runCatching { windowManager.updateViewLayout(root, lp) }
            }
            start()
        }
        getSharedPreferences(PREFS, MODE_PRIVATE).edit().putInt("x", targetX).putInt("y", targetY).apply()
    }

    private fun addLabel(parent: LinearLayout, label: String) {
        parent.addView(TextView(this).apply {
            text = label; textSize = 10f; setTextColor(Color.rgb(75, 230, 255)); gravity = Gravity.CENTER
            setPadding(0, dp(5), 0, dp(8))
        })
    }

    private data class Effect(val icon: String, val label: String, val keywords: List<String>)
    private data class Action(val icon: String, val label: String, val description: String, val run: () -> Unit)

    private fun addEffectRow(parent: LinearLayout, effects: List<Effect>) {
        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER }
        effects.forEach { effect ->
            val button = TextView(this).apply {
                text = effect.icon
                textSize = 23f
                gravity = Gravity.CENTER
                contentDescription = effect.label
                background = glassBackground(14, 0.82f)
                setPadding(dp(5), dp(7), dp(5), dp(2))
                setOnClickListener { playEffect(effect) }
            }
            row.addView(button, LinearLayout.LayoutParams(0, dp(54), 1f).apply {
                setMargins(dp(3), dp(2), dp(3), dp(2))
            })
        }
        parent.addView(row)
        val labels = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        effects.forEach { effect ->
            labels.addView(TextView(this).apply {
                text = effect.label; textSize = 9f; setTextColor(Color.LTGRAY); gravity = Gravity.CENTER
            }, LinearLayout.LayoutParams(0, dp(17), 1f))
        }
        parent.addView(labels)
    }

    private fun addActionRow(parent: LinearLayout, actions: List<Action>) {
        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER }
        actions.forEach { action ->
            val button = TextView(this).apply {
                text = action.icon + "\n" + action.label
                textSize = 11f
                setTextColor(Color.WHITE)
                gravity = Gravity.CENTER
                contentDescription = action.description
                background = getDrawable(android.R.drawable.list_selector_background)
                minHeight = dp(48)
                setOnClickListener { action.run() }
            }
            row.addView(button, LinearLayout.LayoutParams(0, dp(48), 1f))
        }
        parent.addView(row)
    }

    private fun addDivider(parent: LinearLayout) {
        parent.addView(View(this).apply { setBackgroundColor(Color.argb(80, 65, 190, 220)) },
            LinearLayout.LayoutParams(-1, dp(1)).apply { setMargins(0, dp(6), 0, dp(4)) })
    }

    private fun playEffect(effect: Effect) {
        serviceScope.launch {
            val match = withContext(Dispatchers.IO) {
                val sounds = SoundboardStore.observe(applicationContext).first()
                sounds.firstOrNull { sound ->
                    val title = sound.title.lowercase()
                    effect.keywords.any { keyword -> title.contains(keyword) }
                }
            }
            if (match == null) {
                Toast.makeText(this@FloatingControlService,
                    "Efek belum tersedia. Unduh paket suara terkait di Soundboard.", Toast.LENGTH_SHORT).show()
                return@launch
            }
            runCatching {
                player?.release()
                player = MediaPlayer().apply {
                    setDataSource(this@FloatingControlService, Uri.parse(match.uri))
                    setOnCompletionListener { it.release(); if (player === it) player = null }
                    setOnErrorListener { mp, _, _ -> mp.release(); if (player === mp) player = null; true }
                    prepare()
                    start()
                }
            }.onFailure {
                Toast.makeText(this@FloatingControlService, "Efek suara tidak dapat diputar.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun sendAudioAction(action: String) {
        val serviceIntent = Intent(this, LiveBackgroundService::class.java).setAction(action)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(serviceIntent) else startService(serviceIntent)
    }

    private fun removePanel() {
        dockAnimator?.cancel()
        panel?.let { view -> runCatching { windowManager.removeView(view) } }
        panel = null
        bubble = null
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
    override fun onDestroy() {
        removePanel()
        player?.release(); player = null
        serviceScope.cancel()
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
