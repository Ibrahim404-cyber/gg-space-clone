package com.ibrahim404.ggspace

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.ibrahim404.ggspace.shell.Shell
import com.ibrahim404.ggspace.util.Prefs

class GGApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        Shell.mode = when (Prefs.getString(this, Prefs.SHELL_MODE, "none")) {
            "root" -> Shell.Mode.ROOT
            "shizuku" -> Shell.Mode.SHIZUKU
            else -> Shell.Mode.NONE
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.channel_desc)
                setShowBadge(false)
            }
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    companion object {
        const val CHANNEL_ID = "ggspace_tools"
    }
}
