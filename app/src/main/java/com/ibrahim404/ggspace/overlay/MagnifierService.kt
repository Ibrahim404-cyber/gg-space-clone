package com.ibrahim404.ggspace.overlay

import android.annotation.SuppressLint
import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.os.Looper
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.app.NotificationCompat
import com.ibrahim404.ggspace.GGApplication
import com.ibrahim404.ggspace.MainActivity
import com.ibrahim404.ggspace.R
import com.ibrahim404.ggspace.util.Prefs

/** Vision Booster: screen magnifier with hunting-mode colour filters. */
class MagnifierService : Service() {

    private lateinit var windowManager: WindowManager
    private var projection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var reader: ImageReader? = null
    private var overlayView: View? = null
    private var preview: ImageView? = null

    private val mainHandler = Handler(Looper.getMainLooper())
    private lateinit var worker: HandlerThread
    private var workerHandler: Handler? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        startAsForeground()
        worker = HandlerThread("gg-magnifier").apply { start() }
        workerHandler = Handler(worker.looper)

        val code = getIntent().getIntExtra("capture_code", -1)
        @Suppress("DEPRECATION")
        val data: Intent? = getIntent().getParcelableExtra("capture_data")
        if (code == -1 || data == null) {
            stopSelf()
            return
        }
        val manager = getSystemService(MediaProjectionManager::class.java)
        projection = runCatching { manager.getMediaProjection(code, data) }.getOrElse {
            stopSelf()
            return
        }

        val metrics = resources.displayMetrics
        val scale = minOf(1f, 1280f / metrics.widthPixels.coerceAtLeast(1))
        val width = (metrics.widthPixels * scale).toInt().coerceAtLeast(1)
        val height = (metrics.heightPixels * scale).toInt().coerceAtLeast(1)

        reader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)
        reader?.setOnImageAvailableListener({ r ->
            r.acquireLatestImage()?.let { process(it) }
        }, workerHandler)

        virtualDisplay = projection?.createVirtualDisplay(
            "GGSpaceMagnifier",
            width, height, metrics.densityDpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            reader?.surface, null, workerHandler
        )

        buildOverlay()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_NOT_STICKY

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
            .setContentTitle("Vision Booster is active")
            .setContentText("Magnifier overlay is running")
            .setContentIntent(pending)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(1002, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION)
        } else {
            startForeground(1002, notification)
        }
    }

    @SuppressLint("SetTextI18n")
    private fun buildOverlay() {
        val density = resources.displayMetrics.density
        fun dp(value: Int) = (value * density).toInt()

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = android.graphics.drawable.GradientDrawable().apply {
                setColor(android.graphics.Color.parseColor("#E8141420"))
                cornerRadius = 20f
            }
            setPadding(dp(10), dp(8), dp(10), dp(10))
        }

        val header = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val title = TextView(this).apply {
            text = "VISION BOOSTER"
            setTextColor(android.graphics.Color.WHITE)
            textSize = 12f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        }
        val spacer = View(this).apply {
            layoutParams = LinearLayout.LayoutParams(0, 1).apply { weight = 1f }
        }
        val close = TextView(this).apply {
            text = "✕"
            setTextColor(android.graphics.Color.parseColor("#00E5FF"))
            textSize = 16f
            setPadding(dp(12), 0, 0, 0)
            setOnClickListener { stopSelf() }
        }
        header.addView(title)
        header.addView(spacer)
        header.addView(close)

        preview = ImageView(this).apply {
            adjustViewBounds = true
            scaleType = ImageView.ScaleType.FIT_CENTER
            setBackgroundColor(android.graphics.Color.parseColor("#05050A"))
        }

        val hint = TextView(this).apply {
            text = "Drag to move · tap ✕ to stop"
            setTextColor(android.graphics.Color.parseColor("#8E8EA6"))
            textSize = 10f
        }

        root.addView(header)
        root.addView(preview)
        root.addView(hint)

        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = dp(16)
            y = dp(100)
        }

        var startX = 0
        var startY = 0
        var touchX = 0f
        var touchY = 0f
        header.setOnTouchListener { target, event ->
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
        overlayView = root
        applyHuntingMode()
    }

    private fun applyHuntingMode() {
        val mode = Prefs.getString(this, Prefs.HUNTING_MODE, "none")
        val matrix = ColorMatrix()
        when (mode) {
            "night" -> matrix.set(floatArrayOf(
                1.6f, 0f, 0f, 0f, 12f,
                0f, 1.6f, 0f, 0f, 12f,
                0f, 0f, 1.6f, 0f, 12f,
                0f, 0f, 0f, 1f, 0f
            ))

            "night_vision" -> matrix.set(floatArrayOf(
                0.3f, 0.6f, 0.1f, 0f, 0f,
                0.3f, 1.4f, 0.1f, 0f, 0f,
                0.2f, 0.6f, 0.2f, 0f, 0f,
                0f, 0f, 0f, 1f, 0f
            ))

            "hawkeye" -> matrix.set(floatArrayOf(
                1.5f, -0.2f, -0.2f, 0f, 0f,
                -0.2f, 1.5f, -0.2f, 0f, 0f,
                -0.2f, -0.2f, 1.5f, 0f, 0f,
                0f, 0f, 0f, 1f, 0f
            ))

            "pure" -> matrix.set(floatArrayOf(
                1.1f, 0f, 0f, 0f, 4f,
                0f, 1.1f, 0f, 0f, 4f,
                0f, 0f, 1.1f, 0f, 4f,
                0f, 0f, 0f, 1f, 0f
            ))
        }
        preview?.colorFilter = ColorMatrixColorFilter(matrix)
    }

    private fun process(image: Image) {
        try {
            val plane = image.planes[0]
            val width = image.width
            val height = image.height
            val pixels = IntArray(width * height)
            plane.buffer.asIntBuffer().get(pixels)

            val full = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            full.setPixels(pixels, 0, width, 0, 0, width, height)

            val zoom = Prefs.getInt(this, Prefs.MAGNIFICATION, 3).coerceIn(1, 8)
            val cropW = (width / zoom).coerceAtLeast(1)
            val cropH = (height / zoom).coerceAtLeast(1)
            val left = ((width - cropW) / 2).coerceAtLeast(0)
            val top = ((height - cropH) / 2).coerceAtLeast(0)
            val cropped = Bitmap.createBitmap(full, left, top, cropW, cropH)

            val outWidth = 260
            val outHeight = (outWidth.toLong() * cropH / cropW).toInt().coerceAtLeast(1)
            val scaled = Bitmap.createScaledBitmap(cropped, outWidth, outHeight, true)

            mainHandler.post { preview?.setImageBitmap(scaled) }
            full.recycle()
            cropped.recycle()
        } catch (e: Exception) {
            // A dropped frame is harmless; the next one will be drawn.
        } finally {
            runCatching { image.close() }
        }
    }

    override fun onDestroy() {
        overlayView?.let { runCatching { windowManager.removeView(it) } }
        overlayView = null
        workerHandler?.removeCallbacksAndMessages(null)
        virtualDisplay?.release()
        reader?.close()
        runCatching { projection?.stop() }
        worker.quitSafely()
        super.onDestroy()
    }
}
