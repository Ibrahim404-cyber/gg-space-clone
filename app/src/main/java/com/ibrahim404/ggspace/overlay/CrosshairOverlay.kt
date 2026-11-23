package com.ibrahim404.ggspace.overlay

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PixelFormat
import android.os.Build
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import com.ibrahim404.ggspace.util.Prefs

/** A draggable virtual crosshair drawn with a plain View so it works inside a service. */
class CrosshairOverlay(private val context: Context) {

    private val windowManager = context.getSystemService(WindowManager::class.java)
    private var view: CrossView? = null
    private var params: WindowManager.LayoutParams? = null

    val isShowing: Boolean get() = view != null

    @SuppressLint("InflateParams")
    fun show() {
        if (isShowing) return
        val cross = CrossView(context)
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }
        val layoutParams = WindowManager.LayoutParams(
            DP, DP, type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER
        }

        var startX = 0
        var startY = 0
        var touchX = 0f
        var touchY = 0f
        cross.setOnTouchListener { target, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    startX = layoutParams.x
                    startY = layoutParams.y
                    touchX = event.rawX
                    touchY = event.rawY
                    true
                }

                MotionEvent.ACTION_MOVE -> {
                    layoutParams.x = startX + (event.rawX - touchX).toInt()
                    layoutParams.y = startY + (event.rawY - touchY).toInt()
                    runCatching { windowManager.updateViewLayout(target, layoutParams) }
                    true
                }

                else -> false
            }
        }

        runCatching { windowManager.addView(cross, layoutParams) }
        view = cross
        params = layoutParams
    }

    fun hide() {
        view?.let { runCatching { windowManager.removeView(it) } }
        view = null
        params = null
    }

    fun refresh() = view?.invalidate()

    fun destroy() {
        hide()
    }

    private companion object {
        const val DP = 220
    }

    private inner class CrossView(context: Context) : View(context) {

        private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { strokeWidth = 3f }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val style = Prefs.getString(context, Prefs.CROSSHAIR_STYLE, "dot")
            val colorHex = Prefs.getString(context, Prefs.CROSSHAIR_COLOR, "#FFFF4D9D")
            val size = Prefs.getInt(context, Prefs.CROSSHAIR_SIZE, 16).coerceIn(4, 60)
            val alpha = Prefs.getInt(context, Prefs.CROSSHAIR_ALPHA, 220).coerceIn(20, 255)

            paint.color = runCatching { Color.parseColor(colorHex) }.getOrDefault(Color.MAGENTA)
            paint.alpha = alpha

            val cx = width / 2f
            val cy = height / 2f
            val half = size / 2f

            when (style) {
                "dot" -> {
                    paint.style = Paint.Style.FILL
                    canvas.drawCircle(cx, cy, half / 2f, paint)
                }

                "cross" -> {
                    paint.style = Paint.Style.STROKE
                    canvas.drawLine(cx - half, cy, cx + half, cy, paint)
                    canvas.drawLine(cx, cy - half, cx, cy + half, paint)
                }

                "circle" -> {
                    paint.style = Paint.Style.STROKE
                    canvas.drawCircle(cx, cy, half, paint)
                }

                "circle_cross" -> {
                    paint.style = Paint.Style.STROKE
                    canvas.drawCircle(cx, cy, half, paint)
                    canvas.drawLine(cx - half, cy, cx + half, cy, paint)
                    canvas.drawLine(cx, cy - half, cx, cy + half, paint)
                }

                else -> {
                    paint.style = Paint.Style.STROKE
                    canvas.drawRect(cx - half, cy - half, cx + half, cy + half, paint)
                    canvas.drawLine(cx - half, cy - half, cx + half, cy + half, paint)
                    canvas.drawLine(cx + half, cy - half, cx - half, cy + half, paint)
                }
            }
        }
    }
}
