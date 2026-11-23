package com.ibrahim404.ggspace.overlay

import android.annotation.SuppressLint
import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.ComponentName
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.media.AudioManager
import android.media.MediaScannerConnection
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.GridLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.ibrahim404.ggspace.GGApplication
import com.ibrahim404.ggspace.MainActivity
import com.ibrahim404.ggspace.R
import com.ibrahim404.ggspace.a11y.GGAccessibilityService
import com.ibrahim404.ggspace.booster.Booster
import com.ibrahim404.ggspace.device.DeviceMonitor
import com.ibrahim404.ggspace.macro.MacroStore
import com.ibrahim404.ggspace.shell.Shell
import com.ibrahim404.ggspace.util.DeviceInfo
import com.ibrahim404.ggspace.util.Permissions
import com.ibrahim404.ggspace.util.Prefs
import java.io.File

class FloatingOverlayService : Service() {

    private lateinit var windowManager: WindowManager
    private var ballView: View? = null
    private var panelView: View? = null
    private var monitorView: View? = null
    private var statsText: TextView? = null
    private var monitorText: TextView? = null

    private lateinit var crosshair: CrosshairOverlay
    private lateinit var monitor: DeviceMonitor

    private val handler = Handler(Looper.getMainLooper())
    private var recording = false

    private val statsTask = object : Runnable {
        override fun run() {
            val (free, total) = DeviceInfo.memory(this@FloatingOverlayService)
            val battery = DeviceInfo.batteryPercent(this@FloatingOverlayService)
            statsText?.text = "RAM ${free}MB / ${total}MB · BAT ${battery}%"
            handler.postDelayed(this, 2000)
        }
    }

    private val monitorTask = object : Runnable {
        override fun run() {
            val m = monitor.metrics.value
            monitorText?.text = buildString {
                append("FPS ${m.fps}  CPU ${m.cpuUsage}%\n")
                append("TEMP ${"%.1f".format(m.cpuTemp)}°C  BAT ${m.battery}%\n")
                append("NET ↓${m.rxKBps}KB/s ↑${m.txKBps}KB/s\n")
                append("MODE ${Shell.mode.name}  ${m.time}")
            }
            handler.postDelayed(this, 1000)
        }
    }

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        crosshair = CrosshairOverlay(this)
        monitor = DeviceMonitor(this)
        startAsForeground()

        if (Permissions.canDrawOverlays(this)) {
            buildBall()
            buildPanel()
        }
        if (Prefs.getBool(this, Prefs.CROSSHAIR_ENABLED, false)) crosshair.show()
        if (Prefs.getBool(this, Prefs.MONITOR_ENABLED, false)) {
            monitor.start()
            showMonitorView()
        }
        handler.post(statsTask)
        handler.post(monitorTask)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startAsForeground() {
        val open = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pending = PendingIntent.getActivity(
            this, 0, open,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification: Notification = NotificationCompat.Builder(this, GGApplication.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_bolt)
            .setContentTitle(getString(R.string.overlay_notification_title))
            .setContentText(getString(R.string.overlay_notification_text))
            .setContentIntent(pending)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(1001, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(1001, notification)
        }
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private fun rounded(color: String, radius: Float): GradientDrawable =
        GradientDrawable().apply {
            setColor(Color.parseColor(color))
            cornerRadius = radius
        }

    private fun overlayType(): Int = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
    } else {
        @Suppress("DEPRECATION")
        WindowManager.LayoutParams.TYPE_PHONE
    }

    /** Small floating ball, shown while the menu is collapsed. */
    private fun buildBall() {
        val ball = TextView(this).apply {
            text = "GG"
            setTextColor(Color.WHITE)
            textSize = 15f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            background = rounded("#CC7C5CFF", 60f)
        }
        val params = WindowManager.LayoutParams(
            dp(58), dp(58), overlayType(),
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            android.graphics.PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = dp(16)
            y = dp(180)
        }

        var startX = 0
        var startY = 0
        var touchX = 0f
        var touchY = 0f
        var moved = false
        ball.setOnTouchListener { view, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    startX = params.x; startY = params.y
                    touchX = event.rawX; touchY = event.rawY
                    moved = false
                    true
                }

                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - touchX
                    val dy = event.rawY - touchY
                    if (kotlin.math.abs(dx) > 6 || kotlin.math.abs(dy) > 6) moved = true
                    params.x = startX + dx.toInt()
                    params.y = startY + dy.toInt()
                    runCatching { windowManager.updateViewLayout(view, params) }
                    true
                }

                MotionEvent.ACTION_UP -> {
                    if (!moved) showPanel()
                    true
                }

                else -> false
            }
        }
        runCatching { windowManager.addView(ball, params) }
        ballView = ball
    }

    /** Expanded tool grid. */
    @SuppressLint("SetTextI18n")
    private fun buildPanel() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = rounded("#EE12121D", 22f)
            setPadding(dp(12), dp(10), dp(12), dp(12))
        }

        val header = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val handle = TextView(this).apply {
            text = "⠿  GG SPACE TOOLS"
            setTextColor(Color.WHITE)
            textSize = 13f
            typeface = Typeface.DEFAULT_BOLD
        }
        val spacer = View(this).apply {
            layoutParams = LinearLayout.LayoutParams(0, 1).apply { weight = 1f }
        }
        val minimize = TextView(this).apply {
            text = "—"
            setTextColor(Color.parseColor("#00E5FF"))
            textSize = 18f
            setPadding(dp(10), 0, dp(4), 0)
            setOnClickListener { showBall() }
        }
        val close = TextView(this).apply {
            text = "✕"
            setTextColor(Color.parseColor("#FF4D9D"))
            textSize = 16f
            setPadding(dp(6), 0, 0, 0)
            setOnClickListener { stopSelf() }
        }
        header.addView(handle)
        header.addView(spacer)
        header.addView(minimize)
        header.addView(close)

        statsText = TextView(this).apply {
            text = "Loading…"
            setTextColor(Color.parseColor("#A9A9BE"))
            textSize = 11f
            setPadding(0, dp(6), 0, dp(10))
        }

        val grid = GridLayout(this).apply {
            columnCount = 3
            alignmentMode = GridLayout.ALIGN_BOUNDS
        }
        val tools = listOf(
            Triple("⚡", "Boost", { boost() }),
            Triple("☀", "Bright+", { brightness(20) }),
            Triple("☾", "Bright-", { brightness(-20) }),
            Triple("🔕", "DND", { toggleDnd() }),
            Triple("🔊", "Vol+", { volume(AudioManager.ADJUST_RAISE) }),
            Triple("🔉", "Vol-", { volume(AudioManager.ADJUST_LOWER) }),
            Triple("📷", "Shot", { screenshot() }),
            Triple("🎥", "Record", { record() }),
            Triple("⊕", "Crosshair", { toggleCrosshair() }),
            Triple("🔍", "Magnifier", { openMagnifier() }),
            Triple("📊", "Monitor", { toggleMonitor() }),
            Triple("🔒", "Rot lock", { rotation(false) }),
            Triple("🔄", "Rot auto", { rotation(true) }),
            Triple("💤", "Screen off", { key(26) }),
            Triple("⌂", "Home", { goHome() }),
            Triple("◀", "Back", { key(4) }),
            Triple("▣", "Recent", { key(187) }),
            Triple("🧹", "Kill apps", { boost() }),
            Triple("▶", "Play macro", { playFirstMacro() }),
            Triple("🎯", "Macro/Key", { openAutomate() })
        )
        tools.forEach { (symbol, label, action) ->
            grid.addView(toolButton(symbol, label, action))
        }

        root.addView(header)
        root.addView(statsText)
        root.addView(grid)

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            overlayType(),
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            android.graphics.PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = dp(16)
            y = dp(120)
        }

        var startX = 0
        var startY = 0
        var touchX = 0f
        var touchY = 0f
        handle.setOnTouchListener { _, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    startX = params.x; startY = params.y
                    touchX = event.rawX; touchY = event.rawY
                    true
                }

                MotionEvent.ACTION_MOVE -> {
                    params.x = startX + (event.rawX - touchX).toInt()
                    params.y = startY + (event.rawY - touchY).toInt()
                    runCatching { windowManager.updateViewLayout(root, params) }
                    true
                }

                else -> false
            }
        }

        runCatching { windowManager.addView(root, params) }
        panelView = root
        root.visibility = View.GONE
    }

    private fun toolButton(symbol: String, label: String, click: () -> Unit): Button {
        return Button(this).apply {
            text = "$symbol\n$label"
            isAllCaps = false
            textSize = 10f
            setTextColor(Color.WHITE)
            background = rounded("#7C5CFF", 14f)
            val lp = GridLayout.LayoutParams()
            lp.width = dp(74)
            lp.height = dp(58)
            lp.setMargins(3, 3, 3, 3)
            layoutParams = lp
            minHeight = 0
            minimumHeight = 0
            setPadding(2, 2, 2, 2)
            setOnClickListener { click() }
        }
    }

    private fun showBall() {
        panelView?.visibility = View.GONE
        ballView?.visibility = View.VISIBLE
    }

    private fun showPanel() {
        ballView?.visibility = View.GONE
        panelView?.visibility = View.VISIBLE
    }

    private fun showMonitorView() {
        if (monitorView != null) return
        val text = TextView(this).apply {
            text = "Monitor…"
            setTextColor(Color.parseColor("#00E5FF"))
            textSize = 11f
            typeface = Typeface.MONOSPACE
            background = rounded("#CC0A0A12", 14f)
            setPadding(dp(10), dp(8), dp(10), dp(8))
        }
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            overlayType(),
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            android.graphics.PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.END
            x = dp(8)
            y = dp(60)
        }
        runCatching { windowManager.addView(text, params) }
        monitorView = text
        monitorText = text
    }

    private fun hideMonitorView() {
        monitorView?.let { runCatching { windowManager.removeView(it) } }
        monitorView = null
        monitorText = null
    }

    private fun toast(message: String) = Toast.makeText(this, message, Toast.LENGTH_SHORT).show()

    private fun requirePrivileged(action: () -> Unit) {
        if (!Shell.isPrivileged()) {
            toast("Activate Root or Shizuku first")
            val intent = Intent(this, MainActivity::class.java).apply {
                putExtra("open_activation", true)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            }
            runCatching { startActivity(intent) }
            return
        }
        action()
    }

    private fun boost() {
        val freed = Booster(this).boost()
        toast("Boosted · +${freed} MB free")
    }

    private fun brightness(delta: Int) {
        val current = runCatching {
            Settings.System.getInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS, 120)
        }.getOrDefault(120)
        val next = (current + delta).coerceIn(10, 255)
        when {
            Shell.isPrivileged() -> {
                Shell.run("settings put system screen_brightness_mode 0")
                Shell.run("settings put system screen_brightness $next")
            }

            Permissions.canWriteSettings(this) -> {
                Settings.System.putInt(
                    contentResolver,
                    Settings.System.SCREEN_BRIGHTNESS_MODE,
                    Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL
                )
                Settings.System.putInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS, next)
            }

            else -> {
                Permissions.requestWriteSettings(this)
                toast("Grant \"Modify system settings\" first")
                return
            }
        }
        toast("Brightness → $next")
    }

    private fun toggleDnd() {
        val manager = getSystemService(android.app.NotificationManager::class.java)
        if (!manager.isNotificationPolicyAccessGranted) {
            Permissions.requestDndAccess(this)
            toast("Grant Do Not Disturb access first")
            return
        }
        val next = manager.currentInterruptionFilter != android.app.NotificationManager.INTERRUPTION_FILTER_NONE
        runCatching {
            manager.setInterruptionFilter(
                if (next) android.app.NotificationManager.INTERRUPTION_FILTER_NONE
                else android.app.NotificationManager.INTERRUPTION_FILTER_ALL
            )
        }
        toast("Do Not Disturb ${if (next) "ON" else "OFF"}")
    }

    private fun volume(direction: Int) {
        val audio = getSystemService(AudioManager::class.java)
        audio.adjustStreamVolume(AudioManager.STREAM_MUSIC, direction, AudioManager.FLAG_SHOW_UI)
    }

    private fun screenshot() = requirePrivileged {
        val dir = "/sdcard/Pictures/GGSpace"
        Shell.run("mkdir -p $dir")
        val path = "$dir/GG_${System.currentTimeMillis()}.png"
        val result = Shell.run("screencap -p $path")
        if (result.ok) {
            MediaScannerConnection.scanFile(this, arrayOf(path), arrayOf("image/png"), null)
            toast("Saved to Pictures/GGSpace")
        } else {
            toast("Screenshot failed")
        }
    }

    private fun record() = requirePrivileged {
        if (recording) {
            Shell.run("pkill screenrecord")
            recording = false
            toast("Recording stopped")
        } else {
            val dir = "/sdcard/Movies/GGSpace"
            Shell.run("mkdir -p $dir")
            val path = "$dir/GG_${System.currentTimeMillis()}.mp4"
            Shell.run("nohup screenrecord --time-limit 30 $path >/dev/null 2>&1 &")
            recording = true
            toast("Recording 30s → Movies/GGSpace")
        }
    }

    private fun toggleCrosshair() {
        val enabled = !Prefs.getBool(this, Prefs.CROSSHAIR_ENABLED, false)
        Prefs.putBool(this, Prefs.CROSSHAIR_ENABLED, enabled)
        if (enabled) crosshair.show() else crosshair.hide()
        toast("Crosshair ${if (enabled) "ON" else "OFF"}")
    }

    private fun openMagnifier() {
        val intent = Intent(this, MainActivity::class.java).apply {
            putExtra("request_capture", true)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        runCatching { startActivity(intent) }
    }

    private fun toggleMonitor() {
        val enabled = !Prefs.getBool(this, Prefs.MONITOR_ENABLED, false)
        Prefs.putBool(this, Prefs.MONITOR_ENABLED, enabled)
        if (enabled) {
            monitor.start()
            showMonitorView()
        } else {
            monitor.stop()
            hideMonitorView()
        }
        toast("Device monitor ${if (enabled) "ON" else "OFF"}")
    }

    private fun rotation(auto: Boolean) {
        val value = if (auto) 1 else 0
        when {
            Shell.isPrivileged() -> {
                Shell.run("settings put system accelerometer_rotation $value")
                if (!auto) Shell.run("settings put system user_rotation 1")
            }

            Permissions.canWriteSettings(this) -> {
                Settings.System.putInt(contentResolver, Settings.System.ACCELEROMETER_ROTATION, value)
                if (!auto) Settings.System.putInt(contentResolver, Settings.System.USER_ROTATION, 1)
            }

            else -> {
                Permissions.requestWriteSettings(this)
                toast("Grant \"Modify system settings\" first")
                return
            }
        }
        toast("Auto-rotation ${if (auto) "ON" else "OFF"}")
    }

    private fun key(code: Int) = requirePrivileged {
        Shell.run("input keyevent $code")
    }

    private fun goHome() {
        if (Shell.isPrivileged()) {
            Shell.run("input keyevent 3")
        } else {
            val home = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            runCatching { startActivity(home) }
        }
    }

    private fun a11yEnabled(): Boolean {
        val enabled = runCatching {
            Settings.Secure.getString(contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
        }.getOrDefault(null) ?: return false
        val expected = ComponentName(this, GGAccessibilityService::class.java).flattenToString()
        return enabled.split(':').any {
            it.equals(expected, ignoreCase = true) || it.contains(GGAccessibilityService::class.java.name)
        }
    }

    private fun playFirstMacro() {
        val macro = MacroStore.all(this).firstOrNull()
        if (macro == null) {
            toast("No macro saved yet")
            return
        }
        if (!a11yEnabled()) {
            toast("Enable GG Space accessibility service first")
            openAutomate()
            return
        }
        val intent = Intent(GGAccessibilityService.ACTION_PLAY_MACRO)
            .setPackage(packageName)
            .putExtra("id", macro.id)
        sendBroadcast(intent)
        toast("Playing ${macro.name}")
    }

    private fun openAutomate() {
        val intent = Intent(this, MainActivity::class.java).apply {
            putExtra("open_automate", true)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        runCatching { startActivity(intent) }
    }

    override fun onDestroy() {
        handler.removeCallbacks(statsTask)
        handler.removeCallbacks(monitorTask)
        ballView?.let { runCatching { windowManager.removeView(it) } }
        panelView?.let { runCatching { windowManager.removeView(it) } }
        hideMonitorView()
        ballView = null
        panelView = null
        crosshair.destroy()
        monitor.stop()
        if (recording) runCatching { Shell.run("pkill screenrecord") }
        super.onDestroy()
    }
}
