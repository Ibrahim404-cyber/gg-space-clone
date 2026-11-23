package com.ibrahim404.ggspace.a11y

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.accessibilityservice.GestureDescription
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Path
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import androidx.core.content.ContextCompat
import com.ibrahim404.ggspace.macro.MacroStore
import com.ibrahim404.ggspace.util.Prefs

class GGAccessibilityService : AccessibilityService() {

    private val handler = Handler(Looper.getMainLooper())

    private val tapAction: (Int, Int) -> Unit = { x, y -> tap(x, y) }
    private val swipeAction: (Int, Int, Int, Int, Long) -> Unit = { x1, y1, x2, y2, d ->
        swipe(x1, y1, x2, y2, d)
    }

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action != ACTION_PLAY_MACRO) return
            val id = intent.getStringExtra("id") ?: return
            val macro = MacroStore.get(context, id) ?: return
            MacroStore.play(context, macro, tapAction, swipeAction)
        }
    }

    override fun onCreate() {
        super.onCreate()
        ContextCompat.registerReceiver(
            this, receiver, IntentFilter(ACTION_PLAY_MACRO),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        runCatching {
            serviceInfo = serviceInfo.apply {
                flags = flags or AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS
            }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit

    override fun onInterrupt() = Unit

    override fun onKeyEvent(event: KeyEvent): Boolean {
        if (event.action != KeyEvent.ACTION_DOWN || event.repeatCount > 0) {
            return super.onKeyEvent(event)
        }

        if (Prefs.getBool(this, Prefs.KEY_CAPTURE_ACTIVE, false)) {
            Prefs.putInt(this, Prefs.CAPTURED_KEYCODE, event.keyCode)
            Prefs.putBool(this, Prefs.KEY_CAPTURE_ACTIVE, false)
            val broadcast = Intent(ACTION_KEY_CAPTURED).setPackage(packageName)
            sendBroadcast(broadcast)
            return true
        }

        val binding = MacroStore.binding(this, event.keyCode) ?: return super.onKeyEvent(event)
        when (binding.actionType) {
            "point" -> tap(binding.x, binding.y)
            "macro" -> MacroStore.get(this, binding.macroId)?.let {
                MacroStore.play(this, it, tapAction, swipeAction)
            }
        }
        return true
    }

    fun tap(x: Int, y: Int, duration: Long = 80L) {
        dispatchGesture(Path().apply { moveTo(x.toFloat(), y.toFloat()) }, duration)
    }

    fun swipe(x1: Int, y1: Int, x2: Int, y2: Int, duration: Long = 300L) {
        dispatchGesture(
            Path().apply {
                moveTo(x1.toFloat(), y1.toFloat())
                lineTo(x2.toFloat(), y2.toFloat())
            },
            duration
        )
    }

    private fun dispatchGesture(path: Path, duration: Long) {
        val stroke = GestureDescription.StrokeDescription(path, 0, duration)
        val gesture = GestureDescription.Builder().addStroke(stroke).build()
        dispatchGesture(gesture, null, handler)
    }

    override fun onDestroy() {
        runCatching { unregisterReceiver(receiver) }
        super.onDestroy()
    }

    companion object {
        const val ACTION_PLAY_MACRO = "com.ibrahim404.ggspace.PLAY_MACRO"
        const val ACTION_KEY_CAPTURED = "com.ibrahim404.ggspace.KEY_CAPTURED"
    }
}
