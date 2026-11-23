package com.ibrahim404.ggspace.device

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.TrafficStats
import android.os.BatteryManager
import android.os.Handler
import android.os.Looper
import android.view.Choreographer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DeviceMonitor(private val context: Context) {

    data class Metrics(
        val fps: Int = 0,
        val cpuTemp: Float = 0f,
        val cpuUsage: Int = 0,
        val rxKBps: Long = 0,
        val txKBps: Long = 0,
        val battery: Int = 0,
        val time: String = "--:--:--"
    )

    private val _metrics = MutableStateFlow(Metrics())
    val metrics: StateFlow<Metrics> = _metrics

    private val handler = Handler(Looper.getMainLooper())
    private var frames = 0
    private var lastFrameNanos = 0L
    private var lastTotal = 0L
    private var lastIdle = 0L
    private var lastRx = 0L
    private var lastTx = 0L
    private var running = false

    private val frameCallback = object : Choreographer.FrameCallback {
        override fun doFrame(frameTimeNanos: Long) {
            if (!running) return
            frames++
            if (lastFrameNanos == 0L) lastFrameNanos = frameTimeNanos
            val elapsed = frameTimeNanos - lastFrameNanos
            if (elapsed >= 1_000_000_000L) {
                _metrics.value = _metrics.value.copy(fps = frames)
                frames = 0
                lastFrameNanos = frameTimeNanos
            }
            Choreographer.getInstance().postFrameCallback(this)
        }
    }

    private val sampler = object : Runnable {
        override fun run() {
            if (!running) return
            val (rx, tx) = networkDelta()
            _metrics.value = _metrics.value.copy(
                cpuTemp = cpuTemperature(),
                cpuUsage = cpuUsage(),
                rxKBps = rx,
                txKBps = tx,
                battery = batteryPercent(),
                time = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
            )
            handler.postDelayed(this, 1000)
        }
    }

    fun start() {
        if (running) return
        running = true
        lastRx = TrafficStats.getTotalRxBytes()
        lastTx = TrafficStats.getTotalTxBytes()
        Choreographer.getInstance().postFrameCallback(frameCallback)
        handler.post(sampler)
    }

    fun stop() {
        running = false
        Choreographer.getInstance().removeFrameCallback(frameCallback)
        handler.removeCallbacks(sampler)
    }

    private fun networkDelta(): Pair<Long, Long> {
        val rx = TrafficStats.getTotalRxBytes()
        val tx = TrafficStats.getTotalTxBytes()
        val result = ((rx - lastRx) / 1024).coerceAtLeast(0) to ((tx - lastTx) / 1024).coerceAtLeast(0)
        lastRx = rx
        lastTx = tx
        return result
    }

    private fun cpuUsage(): Int = runCatching {
        val line = File("/proc/stat").bufferedReader().use { it.readLine() } ?: return 0
        val values = line.split(Regex("\\s+")).drop(1).map { it.toLongOrNull() ?: 0L }
        val idle = values[3] + (values.getOrNull(4) ?: 0L)
        val total = values.sum()
        val deltaTotal = total - lastTotal
        val deltaIdle = idle - lastIdle
        lastTotal = total
        lastIdle = idle
        if (deltaTotal > 0) (((deltaTotal - deltaIdle) * 100) / deltaTotal).toInt().coerceIn(0, 100) else 0
    }.getOrDefault(0)

    private fun cpuTemperature(): Float = runCatching {
        val root = File("/sys/class/thermal")
        val zones = root.listFiles { file ->
            file.name.startsWith("thermal_zone") && File(file, "temp").exists()
        } ?: return 0f
        zones.mapNotNull { File(it, "temp").readText().trim().toFloatOrNull() }
            .maxOrNull()?.div(1000f) ?: 0f
    }.getOrDefault(0f)

    private fun batteryPercent(): Int {
        val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
        return if (level >= 0 && scale > 0) (level * 100) / scale else 0
    }
}
