package com.ibrahim404.ggspace.shell

import android.content.pm.PackageManager
import rikka.shizuku.Shizuku
import java.util.concurrent.TimeUnit

/**
 * Single entry point for privileged commands. Activation mode is chosen from the
 * "Activate" screen: ROOT (su), SHIZUKU (Shizuku-backed shell), or NONE (plain sh).
 */
object Shell {

    enum class Mode { NONE, SHIZUKU, ROOT }

    data class Result(val exitCode: Int, val output: String) {
        val ok: Boolean get() = exitCode == 0
    }

    @Volatile
    var mode: Mode = Mode.NONE

    fun isPrivileged(): Boolean = mode == Mode.ROOT || (mode == Mode.SHIZUKU && shizukuReady())

    fun shizukuInstalled(): Boolean = runCatching { Shizuku.getBinder() != null }.getOrDefault(false)

    fun shizukuReady(): Boolean = runCatching {
        shizukuInstalled() && Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
    }.getOrDefault(false)

    fun rootAvailable(): Boolean = runRoot("id").output.contains("uid=0")

    fun run(command: String): Result = when (mode) {
        Mode.ROOT -> runRoot(command)
        Mode.SHIZUKU -> runShizuku(command)
        Mode.NONE -> runPlain(command)
    }

    private fun runRoot(command: String): Result = execute(arrayOf("su", "-c", command))

    private fun runPlain(command: String): Result = execute(arrayOf("sh", "-c", command))

    private fun runShizuku(command: String): Result = runCatching {
        collect(Shizuku.newProcess(arrayOf("sh", "-c", command), null, null))
    }.getOrDefault(Result(-1, "Shizuku unavailable"))

    private fun execute(command: Array<String>): Result = runCatching {
        collect(Runtime.getRuntime().exec(command))
    }.getOrDefault(Result(-1, "Unable to start shell"))

    private fun collect(process: Process): Result {
        val output = process.inputStream.bufferedReader().use { it.readText() }
        val error = process.errorStream.bufferedReader().use { it.readText() }
        val finished = process.waitFor(8, TimeUnit.SECONDS)
        if (!finished) {
            process.destroyForcibly()
            return Result(-1, "Command timed out")
        }
        val text = if (output.isNotBlank()) output else error
        return Result(process.exitValue(), text)
    }
}
