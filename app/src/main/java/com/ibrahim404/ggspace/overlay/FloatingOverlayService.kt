package com.ibrahim404.ggspace.overlay

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
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
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.ibrahim404.ggspace.GGApplication
import com.ibrahim404.ggspace.MainActivity
import com.ibrahim404.ggspace.R
import com.ibrahim404.ggspace.booster.Booster
import com.ibrahim404.ggspace.util.DeviceInfo
import com.ibrahim404.ggspace.util.Permissions

class FloatingOverlayService : Service() {

    private lateinit var windowManager: WindowManager
    private var overlayView: View? = null
    private var statsText: TextView? = null
    private var brightValue = 140
    private var dndOn = false

    private val handler = Handler(Looper.getMainLooper())
    private val refreshTask = object : Runnable {
        override fun run() {
            updateStats()
            handler.postDelayed(this, 2000)
        }
    }

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        startAsForeground()
        if (Permissions.canDrawOverlays(this)) buildOverlay()
        handler.post(refreshTask)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startAsForeground() {
        val openApp = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pending = PendingIntent.getActivity(
            this, 0, openApp,
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
            startForeground(
                1001, notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(1001, notification)
        }
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private fun rounded(color: Int, radius: Float): GradientDrawable = GradientDrawable().apply {
        setColor(color)
        cornerRadius = radius
    }

    private fun overlayParams(): WindowManager.LayoutParams {
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }
        return WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = dp(16)
            y = dp(180)
        }
    }

    private fun buildOverlay() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = rounded(Color.parseColor("#E8141420"), 22f)
            setPadding(dp(14), dp(10), dp(14), dp(12))
        }

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val handle = TextView(this).apply {
            text = "⠿  GG SPACE"
            setTextColor(Color.WHITE)
            textSize = 13f
            typeface = Typeface.DEFAULT_BOLD
        }
        val spacer = View(this).apply {
            layoutParams = LinearLayout.LayoutParams(0, 1).apply { weight = 1f }
        }
        val close = TextView(this).apply {
            text = "✕"
            setTextColor(Color.parseColor("#00E5FF"))
            textSize = 17f
            setPadding(dp(12), 0, 0, 0)
            setOnClickListener { stopSelf() }
        }
        header.addView(handle)
        header.addView(spacer)
        header.addView(close)

        statsText = TextView(this).apply {
            text = "Loading stats…"
            setTextColor(Color.parseColor("#B9B9CC"))
            textSize = 11f
            setPadding(0, dp(6), 0, dp(10))
        }

        val buttons = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }
        buttons.addView(toolButton("⚡ BOOST") {
            val freed = Booster(this@FloatingOverlayService).boost()
            toast("Boosted · +${freed} MB free")
        })
        buttons.addView(toolButton("☀ LIGHT") { toggleBrightness() })
        buttons.addView(toolButton("🔕 DND") { toggleDnd() })
        buttons.addView(toolButton("⌂ HOME") { goHome() })

        root.addView(header)
        root.addView(statsText)
        root.addView(buttons)

        val params = overlayParams()
        attachDrag(handle, params)
        runCatching { windowManager.addView(root, params) }
        overlayView = root
        updateStats()
    }

    private fun toolButton(label: String, click: () -> Unit): Button {
        return Button(this).apply {
            text = label
            isAllCaps = false
            textSize = 10f
            setTextColor(Color.WHITE)
            background = rounded(Color.parseColor("#7C5CFF"), 14f)
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            lp.setMargins(0, 0, dp(6), 0)
            layoutParams = lp
            minHeight = 0
            minimumHeight = 0
            setPadding(dp(10), dp(4), dp(10), dp(4))
            setOnClickListener { click() }
        }
    }

    private fun attachDrag(handle: View, params: WindowManager.LayoutParams) {
        var startX = 0
        var startY = 0
        var touchX = 0f
        var touchY = 0f
        handle.setOnTouchListener { view, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    startX = params.x
                    startY = params.y
                    touchX = event.rawX
                    touchY = event.rawY
                    true
                }

                MotionEvent.ACTION_MOVE -> {
                    params.x = startX + (event.rawX - touchX).toInt()
                    params.y = startY + (event.rawY - touchY).toInt()
                    runCatching { windowManager.updateViewLayout(view.rootView, params) }
                    true
                }

                else -> false
            }
        }
    }

    private fun updateStats() {
        val (free, total) = DeviceInfo.memory(this)
        val battery = DeviceInfo.batteryPercent(this)
        val percent = if (total > 0) ((free * 100) / total) else 0
        statsText?.text = "RAM ${free}MB free · $percent% · BAT ${battery}% · ${DeviceInfo.cpuCores()} cores"
    }

    private fun toggleBrightness() {
        if (!Permissions.canWriteSettings(this)) {
            Permissions.requestWriteSettings(this)
            toast("Grant \"Modify system settings\" first")
            return
        }
        brightValue = if (brightValue > 120) 80 else 220
        runCatching {
            Settings.System.putInt(
                contentResolver,
                Settings.System.SCREEN_BRIGHTNESS_MODE,
                Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL
            )
            Settings.System.putInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS, brightValue)
        }
        toast("Brightness → $brightValue")
    }

    private fun toggleDnd() {
        val manager = getSystemService(android.app.NotificationManager::class.java)
        if (!manager.isNotificationPolicyAccessGranted) {
            Permissions.requestDndAccess(this)
            toast("Grant Do Not Disturb access first")
            return
        }
        dndOn = !dndOn
        runCatching {
            manager.setInterruptionFilter(
                if (dndOn) android.app.NotificationManager.INTERRUPTION_FILTER_NONE
                else android.app.NotificationManager.INTERRUPTION_FILTER_ALL
            )
        }
        toast("Do Not Disturb ${if (dndOn) "ON" else "OFF"}")
    }

    private fun goHome() {
        val home = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { startActivity(home) }
    }

    private fun toast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroy() {
        handler.removeCallbacks(refreshTask)
        overlayView?.let { runCatching { windowManager.removeView(it) } }
        overlayView = null
        super.onDestroy()
    }
}
