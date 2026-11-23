package com.ibrahim404.ggspace.macro

import android.content.Context
import android.os.Handler
import android.os.Looper

data class MacroAction(
    val type: String,
    val x: Int,
    val y: Int,
    val x2: Int,
    val y2: Int,
    val duration: Long,
    val delay: Long
)

data class Macro(val id: String, val name: String, val actions: List<MacroAction>)

data class KeyBinding(
    val keyCode: Int,
    val actionType: String,
    val x: Int = 0,
    val y: Int = 0,
    val macroId: String = ""
)

object MacroStore {

    private const val NAME = "ggspace_macros"
    private fun prefs(context: Context) = context.getSharedPreferences(NAME, Context.MODE_PRIVATE)

    fun ids(context: Context): List<String> =
        (prefs(context).getString("ids", "") ?: "").split(',').filter { it.isNotEmpty() }

    fun all(context: Context): List<Macro> = ids(context).mapNotNull { get(context, it) }

    fun get(context: Context, id: String): Macro? {
        val name = prefs(context).getString("name_$id", null) ?: return null
        val data = prefs(context).getString("data_$id", "") ?: ""
        val actions = data.lines().filter { it.isNotBlank() }.mapNotNull { line ->
            val parts = line.split("|")
            if (parts.size >= 7) {
                MacroAction(
                    type = parts[0],
                    x = parts[1].toIntOrNull() ?: 0,
                    y = parts[2].toIntOrNull() ?: 0,
                    x2 = parts[3].toIntOrNull() ?: 0,
                    y2 = parts[4].toIntOrNull() ?: 0,
                    duration = parts[5].toLongOrNull() ?: 0,
                    delay = parts[6].toLongOrNull() ?: 0
                )
            } else null
        }
        return Macro(id, name, actions)
    }

    fun save(context: Context, macro: Macro) {
        val encoded = macro.actions.joinToString("\n") { action ->
            listOf(action.type, action.x, action.y, action.x2, action.y2, action.duration, action.delay)
                .joinToString("|")
        }
        prefs(context).edit()
            .putString("name_${macro.id}", macro.name)
            .putString("data_${macro.id}", encoded)
            .putString("ids", (ids(context).toSet() + macro.id).joinToString(","))
            .apply()
    }

    fun delete(context: Context, id: String) {
        prefs(context).edit()
            .remove("name_$id")
            .remove("data_$id")
            .putString("ids", ids(context).filter { it != id }.joinToString(","))
            .apply()
        bindings(context).filter { it.macroId == id }.forEach { deleteBinding(context, it.keyCode) }
    }

    fun binding(context: Context, keyCode: Int): KeyBinding? {
        val raw = prefs(context).getString("key_$keyCode", null) ?: return null
        val parts = raw.split("|")
        return if (parts.size >= 4) {
            KeyBinding(
                keyCode = keyCode,
                actionType = parts[0],
                x = parts[1].toIntOrNull() ?: 0,
                y = parts[2].toIntOrNull() ?: 0,
                macroId = parts[3]
            )
        } else null
    }

    fun saveBinding(context: Context, binding: KeyBinding) {
        prefs(context).edit()
            .putString(
                "key_${binding.keyCode}",
                listOf(binding.actionType, binding.x, binding.y, binding.macroId).joinToString("|")
            )
            .apply()
    }

    fun deleteBinding(context: Context, keyCode: Int) {
        prefs(context).edit().remove("key_$keyCode").apply()
    }

    fun bindings(context: Context): List<KeyBinding> =
        prefs(context).all.keys
            .filter { it.startsWith("key_") }
            .mapNotNull { key -> key.removePrefix("key_").toIntOrNull()?.let { binding(context, it) } }

    fun play(
        context: Context,
        macro: Macro,
        tap: (Int, Int) -> Unit,
        swipe: (Int, Int, Int, Int, Long) -> Unit
    ) {
        val handler = Handler(Looper.getMainLooper())
        var offset = 0L
        macro.actions.forEach { action ->
            offset += action.delay.coerceAtLeast(0)
            val scheduled = offset
            val duration = action.duration.coerceIn(50, 5000)
            handler.postDelayed({
                if (action.type == "tap") tap(action.x, action.y)
                else swipe(action.x, action.y, action.x2, action.y2, duration)
            }, scheduled)
            offset += if (action.type == "tap") 120L else duration
        }
    }
}
