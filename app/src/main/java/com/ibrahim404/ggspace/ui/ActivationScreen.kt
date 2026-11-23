package com.ibrahim404.ggspace.ui

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ibrahim404.ggspace.shell.Shell
import com.ibrahim404.ggspace.ui.theme.CardSurface
import com.ibrahim404.ggspace.ui.theme.NeonCyan
import com.ibrahim404.ggspace.ui.theme.NeonPink
import com.ibrahim404.ggspace.ui.theme.NeonPurple
import com.ibrahim404.ggspace.ui.theme.TextMuted
import com.ibrahim404.ggspace.util.Prefs
import rikka.shizuku.Shizuku

private const val SHIZUKU_REQUEST = 2024

@Composable
fun ActivationScreen() {
    val context = LocalContext.current
    var tick by remember { mutableIntStateOf(0) }

    val rootAvailable = remember(tick) { Shell.rootAvailable() }
    val shizukuInstalled = remember(tick) { Shell.shizukuInstalled() }
    val shizukuReady = remember(tick) { Shell.shizukuReady() }
    val currentMode = remember(tick) { Prefs.getString(context, Prefs.SHELL_MODE, "none") }

    DisposableEffect(Unit) {
        val listener = Shizuku.OnRequestPermissionResultListener { _, _ -> tick++ }
        runCatching { Shizuku.addRequestPermissionResultListener(listener) }
        onDispose {
            runCatching { Shizuku.removeRequestPermissionResultListener(listener) }
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 18.dp)
    ) {
        Text("Activation", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Text("Choose how GG Space runs privileged commands", color = TextMuted, fontSize = 12.sp)
        Spacer(Modifier.height(18.dp))

        Card(
            title = "Root",
            description = "Uses 'su' from Magisk / KernelSU / APatch. Full access to screen capture, keyevents and settings.",
            status = if (rootAvailable) "Root detected" else "Root not detected",
            statusOk = rootAvailable,
            actionLabel = "Activate root",
            active = currentMode == "root",
            onAction = {
                if (!rootAvailable) {
                    Toast.makeText(context, "No root access found", Toast.LENGTH_SHORT).show()
                } else {
                    activate(context, "root")
                    tick++
                }
            }
        )

        Spacer(Modifier.height(12.dp))
        Card(
            title = "Shizuku",
            description = "Works without root. Install Shizuku, start its service, then grant permission here.",
            status = when {
                !shizukuInstalled -> "Shizuku not installed / not running"
                !shizukuReady -> "Permission not granted"
                else -> "Ready"
            },
            statusOk = shizukuReady,
            actionLabel = if (shizukuReady) "Activate Shizuku" else "Grant permission",
            active = currentMode == "shizuku",
            onAction = {
                when {
                    !shizukuInstalled -> openShizuku(context)
                    !shizukuReady -> runCatching { Shizuku.requestPermission(SHIZUKU_REQUEST) }
                    else -> { activate(context, "shizuku"); tick++ }
                }
            },
            secondaryLabel = if (shizukuInstalled) null else "Open Shizuku",
            onSecondary = { openShizuku(context) }
        )

        Spacer(Modifier.height(12.dp))
        Card(
            title = "No activation",
            description = "Only unprivileged actions work: boost, overlay, brightness (with permission), volume, crosshair, macro & keymap.",
            status = "Limited mode",
            statusOk = currentMode == "none",
            actionLabel = "Use limited mode",
            active = currentMode == "none",
            onAction = { activate(context, "none"); tick++ }
        )

        Spacer(Modifier.height(16.dp))
        Text(
            "Screen record and screenshot need Root/Shizuku. Macro and keymap need the GG Space accessibility service (enable it from the Macro tab).",
            color = Color(0xFF6E6E85),
            fontSize = 11.sp
        )
        Spacer(Modifier.height(26.dp))
    }
}

private fun openShizuku(context: Context) {
    val intent = context.packageManager.getLaunchIntentForPackage("moe.shizuku.privileged.api")
    if (intent != null) {
        context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } else {
        Toast.makeText(context, "Install Shizuku from Play Store first", Toast.LENGTH_SHORT).show()
    }
}

private fun activate(context: Context, mode: String) {
    Prefs.putString(context, Prefs.SHELL_MODE, mode)
    Shell.mode = when (mode) {
        "root" -> Shell.Mode.ROOT
        "shizuku" -> Shell.Mode.SHIZUKU
        else -> Shell.Mode.NONE
    }
    Toast.makeText(context, "Activation mode saved", Toast.LENGTH_SHORT).show()
}

@Composable
private fun Card(
    title: String,
    description: String,
    status: String,
    statusOk: Boolean,
    actionLabel: String,
    active: Boolean,
    onAction: () -> Unit,
    secondaryLabel: String? = null,
    onSecondary: (() -> Unit)? = null
) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(CardSurface)
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(title, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
            if (active) {
                Text("ACTIVE", color = NeonCyan, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.height(7.dp))
        Text(description, color = TextMuted, fontSize = 11.sp)
        Spacer(Modifier.height(9.dp))
        Text(status, color = if (statusOk) NeonCyan else NeonPink, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(13.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Button(
                onClick = onAction,
                colors = ButtonDefaults.buttonColors(containerColor = NeonPurple)
            ) {
                Text(actionLabel, fontSize = 11.sp)
            }
            if (secondaryLabel != null && onSecondary != null) {
                Spacer(Modifier.width(8.dp))
                OutlinedButton(onClick = onSecondary) {
                    Text(secondaryLabel, fontSize = 11.sp)
                }
            }
        }
    }
}
