package com.ibrahim404.ggspace.booster

import android.app.ActivityManager
import android.content.Context
import com.ibrahim404.ggspace.util.DeviceInfo

class Booster(private val context: Context) {

    /** Kills background processes and returns the amount of RAM reclaimed, in MB. */
    fun boost(): Long {
        val before = DeviceInfo.memory(context).first
        val manager = context.getSystemService(ActivityManager::class.java)

        backgroundPackages().forEach { pkg ->
            runCatching { manager.killBackgroundProcesses(pkg) }
        }

        // Give the system a moment to update its memory info.
        Thread.sleep(350)
        val after = DeviceInfo.memory(context).first
        return (after - before).coerceAtLeast(0)
    }

    private fun backgroundPackages(): List<String> {
        val manager = context.getSystemService(ActivityManager::class.java)
        return runCatching {
            manager.runningAppProcesses.orEmpty()
                .filter {
                    it.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_BACKGROUND
                }
                .flatMap { it.pkgList?.toList().orEmpty() }
                .filterNot { it == context.packageName }
                .distinct()
        }.getOrDefault(emptyList())
    }
}
