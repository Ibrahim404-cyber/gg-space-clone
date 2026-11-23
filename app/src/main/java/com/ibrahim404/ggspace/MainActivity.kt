package com.ibrahim404.ggspace

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.ibrahim404.ggspace.overlay.FloatingOverlayService
import com.ibrahim404.ggspace.ui.MainScreen
import com.ibrahim404.ggspace.ui.MainViewModel
import com.ibrahim404.ggspace.ui.theme.GGSpaceTheme
import com.ibrahim404.ggspace.util.Permissions

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

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
                    canControlDnd = { Permissions.canControlDnd(this) }
                )
            }
        }
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
