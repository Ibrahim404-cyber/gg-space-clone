package com.ibrahim404.ggspace

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import com.ibrahim404.ggspace.overlay.FloatingOverlayService
import com.ibrahim404.ggspace.overlay.MagnifierService
import com.ibrahim404.ggspace.ui.MainScreen
import com.ibrahim404.ggspace.ui.MainViewModel
import com.ibrahim404.ggspace.ui.theme.GGSpaceTheme
import com.ibrahim404.ggspace.util.Permissions

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()
    private var initialTab by mutableIntStateOf(0)

    private val captureLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK && result.data != null) {
                val intent = Intent(this, MagnifierService::class.java).apply {
                    putExtra("capture_code", result.resultCode)
                    putExtra("capture_data", result.data)
                }
                runCatching { ContextCompat.startForegroundService(this, intent) }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleIntent(intent)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1001)
        }

        setContent {
            GGSpaceTheme {
                MainScreen(
                    vm = viewModel,
                    onToggleOverlay = { enabled ->
                        if (enabled) startOverlay() else stopOverlay()
                    },
                    onRequestOverlay = { Permissions.requestOverlay(this) },
                    onRequestWriteSettings = { Permissions.requestWriteSettings(this) },
                    onRequestDnd = { Permissions.requestDndAccess(this) },
                    canOverlay = { Permissions.canDrawOverlays(this) },
                    canWriteSettings = { Permissions.canWriteSettings(this) },
                    canControlDnd = { Permissions.canControlDnd(this) },
                    initialTab = initialTab
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent == null) return
        if (intent.getBooleanExtra("request_capture", false)) {
            intent.removeExtra("request_capture")
            requestCapture()
        }
        if (intent.getBooleanExtra("open_activation", false)) {
            intent.removeExtra("open_activation")
            initialTab = 4
        }
        if (intent.getBooleanExtra("open_automate", false)) {
            intent.removeExtra("open_automate")
            initialTab = 3
        }
    }

    private fun requestCapture() {
        val manager = getSystemService(MediaProjectionManager::class.java)
        runCatching { captureLauncher.launch(manager.createScreenCaptureIntent()) }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshApps()
    }

    private fun startOverlay() {
        if (!Permissions.canDrawOverlays(this)) {
            Permissions.requestOverlay(this)
            return
        }
        val intent = Intent(this, FloatingOverlayService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }

    private fun stopOverlay() {
        stopService(Intent(this, FloatingOverlayService::class.java))
    }
}
