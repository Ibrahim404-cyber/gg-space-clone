package com.ibrahim404.ggspace.util

import android.content.Context

object Prefs {
    private const val NAME = "ggspace_ui"

    private fun prefs(context: Context) = context.getSharedPreferences(NAME, Context.MODE_PRIVATE)

    fun getString(context: Context, key: String, default: String): String =
        prefs(context).getString(key, default) ?: default

    fun putString(context: Context, key: String, value: String) {
        prefs(context).edit().putString(key, value).apply()
    }

    fun getBool(context: Context, key: String, default: Boolean): Boolean =
        prefs(context).getBoolean(key, default)

    fun putBool(context: Context, key: String, value: Boolean) {
        prefs(context).edit().putBoolean(key, value).apply()
    }

    fun getInt(context: Context, key: String, default: Int): Int =
        prefs(context).getInt(key, default)

    fun putInt(context: Context, key: String, value: Int) {
        prefs(context).edit().putInt(key, value).apply()
    }

    const val SHELL_MODE = "shell_mode"
    const val CROSSHAIR_ENABLED = "crosshair_enabled"
    const val CROSSHAIR_STYLE = "crosshair_style"
    const val CROSSHAIR_COLOR = "crosshair_color"
    const val CROSSHAIR_SIZE = "crosshair_size"
    const val CROSSHAIR_ALPHA = "crosshair_alpha"
    const val MONITOR_ENABLED = "monitor_enabled"
    const val HUNTING_MODE = "hunting_mode"
    const val MAGNIFICATION = "magnification"
    const val KEY_CAPTURE_ACTIVE = "key_capture_active"
    const val CAPTURED_KEYCODE = "captured_keycode"
    const val PICKED_X = "picked_x"
    const val PICKED_Y = "picked_y"
}
