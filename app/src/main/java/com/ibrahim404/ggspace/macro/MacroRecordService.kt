package com.ibrahim404.ggspace.macro

import android.annotation.SuppressLint
import android.app.Notification
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.ibrahim404.ggspace.GGApplication
import com.ibrahim404.ggspace.R
import com.ibrahim404.ggspace.util.Prefs
import kotlin.math.abs

/** Records a tap/swipe sequence, or picks a single screen point for a keymap binding. */
class MacroRecordService : Service() {

    private lateinit var windowManager: WindowManager
    private var touchView: View? = null
    private var controlView: View? = null
    private var mode = MODE_RECORD
    private val actions = mutableListOf<MacroAction>()

    private var downX = 0
    private var downY = 0
    private var downTime = 0L
    private var lastEnd = 0L

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        mode = getIntent().getStringExtra("mode") ?: MODE_RECORD
        startAsForeground()
        buildTouchLayer()
        buildControls()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_NOT_STICKY

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startAsForeground() {
        val notification: Notification = NotificationCompat.Builder(this, GGApplication.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_bolt)
            .setContentTitle(if (mode == MODE_PICK) "Pick a screen point" else "Recording macro")
            .setContentText("Tap the screen · use the floating buttons to finish")
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(1003, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(1003, notification)
        }
    }

    private fun overlayType(): Int = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
    } else {
        @Suppress("DEPRECATION")
        WindowManager.LayoutParams.TYPE_PHONE
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun buildTouchLayer() {
        val view = View(this)
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            overlayType(),
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply { gravity = Gravity.TOP or Gravity.START }

        view.setOnTouchListener { _, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    downX = event.rawX.toInt()
                    downY = event.rawY.toInt()
                    downTime = System.currentTimeMillis()
                    true
                }

                MotionEvent.ACTION_UP -> {
                    val now = System.currentTimeMillis()
                    val dx = abs(event.rawX - downX)
                    val dy = abs(event.rawY - downY)
                    if (mode == MODE_PICK) {
                        Prefs.putInt(this, Prefs.PICKED_X, downX)
                        Prefs.putInt(this, Prefs.PICKED_Y, downY)
                        sendBroadcast(Intent(ACTION_POINT_PICKED).setPackage(packageName))
                        stopSelf()
                        return@setOnTouchListener true
                    }
                    val delay = if (lastEnd == 0L) 0L else (downTime - lastEnd).coerceAtLeast(0)
                    if (dx < 12 && dy < 12) {
                        actions.add(MacroAction("tap", downX, downY, 0, 0, 80, delay))
                    } else {
                        actions.add(
                            MacroAction(
                                "swipe", downX, downY,
                                event.rawX.toInt(), event.rawY.toInt(),
                                (now - downTime).coerceIn(50, 5000), delay
                            )
                        )
                    }
                    lastEnd = now
                    true
                }

                else -> true
            }
        }

        runCatching { windowManager.addView(view, params) }
        touchView = view
    }

    private fun buildControls() {
        val density = resources.displayMetrics.density
        fun dp(value: Int) = (value * density).toInt()

        val bar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#EE12121D"))
                cornerRadius = 18f
            }
            setPadding(dp(10), dp(6), dp(10), dp(6))
        }
        val label = Button(this).apply {
            text = if (mode == MODE_PICK) "PICK POINT" else "● REC"
            isAllCaps = false
            textSize = 11f
            setTextColor(Color.WHITE)
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#FF4D9D"))
                cornerRadius = 12f
            }
            isEnabled = false
            setPadding(dp(10), dp(4), dp(10), dp(4))
        }
        val save = Button(this).apply {
            text = if (mode == MODE_PICK) "CANCEL" else "SAVE"
            isAllCaps = false
            textSize = 11f
            setTextColor(Color.WHITE)
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#7C5CFF"))
                cornerRadius = 12f
            }
            setPadding(dp(10), dp(4), dp(10), dp(4))
            setOnClickListener {
                if (mode == MODE_PICK) {
                    stopSelf()
                } else {
                    saveMacro()
                }
            }
        }
        bar.addView(label)
        bar.addView(save)

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            overlayType(),
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            y = dp(50)
        }
        runCatching { windowManager.addView(bar, params) }
        controlView = bar
    }

    private fun saveMacro() {
        if (actions.isEmpty()) {
            Toast.makeText(this, "No actions recorded", Toast.LENGTH_SHORT).show()
            return
        }
        val id = "m_${System.currentTimeMillis()}"
        val name = "Macro ${MacroStore.ids(this).size + 1}"
        MacroStore.save(this, Macro(id, name, actions.toList()))
        Toast.makeText(this, "$name saved", Toast.LENGTH_SHORT).show()
        stopSelf()
    }

    override fun onDestroy() {
        touchView?.let { runCatching { windowManager.removeView(it) } }
        controlView?.let { runCatching { windowManager.removeView(it) } }
        touchView = null
        controlView = null
        super.onDestroy()
    }

    companion object {
        const val MODE_RECORD = "record"
        const val MODE_PICK = "pick"
        const val ACTION_POINT_PICKED = "com.ibrahim404.ggspace.POINT_PICKED"
    }
}
