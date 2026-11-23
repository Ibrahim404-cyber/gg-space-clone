package com.ibrahim404.ggspace.ui

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.provider.Settings
import android.view.KeyEvent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
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
import androidx.core.content.ContextCompat
import com.ibrahim404.ggspace.a11y.GGAccessibilityService
import com.ibrahim404.ggspace.macro.KeyBinding
import com.ibrahim404.ggspace.macro.MacroRecordService
import com.ibrahim404.ggspace.macro.MacroStore
import com.ibrahim404.ggspace.ui.theme.CardSurface
import com.ibrahim404.ggspace.ui.theme.CardSurfaceAlt
import com.ibrahim404.ggspace.ui.theme.NeonCyan
import com.ibrahim404.ggspace.ui.theme.NeonPink
import com.ibrahim404.ggspace.ui.theme.NeonPurple
import com.ibrahim404.ggspace.ui.theme.TextMuted
import com.ibrahim404.ggspace.util.Permissions
import com.ibrahim404.ggspace.util.Prefs

@Composable
fun AutomateScreen(initialTab: Int = 0) {
    val context = LocalContext.current
    var tab by remember { mutableIntStateOf(initialTab) }
    var refresh by remember { mutableIntStateOf(0) }
    var pendingKey by remember { mutableStateOf<Int?>(null) }
    var showMacroPicker by remember { mutableStateOf(false) }

    val macros = remember(refresh) { MacroStore.all(context) }
    val bindings = remember(refresh) { MacroStore.bindings(context) }
    val a11yEnabled = remember(refresh) { isAccessibilityEnabled(context) }

    DisposableEffect(Unit) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context, intent: Intent) {
                when (intent.action) {
                    GGAccessibilityService.ACTION_KEY_CAPTURED -> {
                        val code = Prefs.getInt(context, Prefs.CAPTURED_KEYCODE, -1)
                        if (code > 0) pendingKey = code
                    }

                    MacroRecordService.ACTION_POINT_PICKED -> {
                        pendingKey?.let { key ->
                            MacroStore.saveBinding(
                                context,
                                KeyBinding(
                                    keyCode = key,
                                    actionType = "point",
                                    x = Prefs.getInt(context, Prefs.PICKED_X, 0),
                                    y = Prefs.getInt(context, Prefs.PICKED_Y, 0)
                                )
                            )
                            pendingKey = null
                            Toast.makeText(context, "Key bound to screen point", Toast.LENGTH_SHORT).show()
                        }
                        refresh++
                    }
                }
            }
        }
        val filter = IntentFilter().apply {
            addAction(GGAccessibilityService.ACTION_KEY_CAPTURED)
            addAction(MacroRecordService.ACTION_POINT_PICKED)
        }
        ContextCompat.registerReceiver(
            context, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED
        )
        onDispose { runCatching { context.unregisterReceiver(receiver) } }
    }

    Column(Modifier.fillMaxSize()) {
        Text(
            "Macro & Keymap",
            color = Color.White,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp)
        )
        TabRow(selectedTabIndex = tab, containerColor = CardSurface, contentColor = NeonCyan) {
            Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("Macros") })
            Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("Keymap") })
        }

        if (!a11yEnabled) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(CardSurface)
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Accessibility service off", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Text("Required to play macros and map keys", color = TextMuted, fontSize = 11.sp)
                }
                OutlinedButton(onClick = {
                    context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                }) {
                    Text("Enable", fontSize = 11.sp)
                }
            }
        }

        when (tab) {
            0 -> MacroTab(
                context = context,
                macros = macros,
                onRecord = { startRecorder(context, MacroRecordService.MODE_RECORD) },
                onPlay = { macro ->
                    val intent = Intent(GGAccessibilityService.ACTION_PLAY_MACRO)
                        .setPackage(context.packageName)
                        .putExtra("id", macro.id)
                    context.sendBroadcast(intent)
                    Toast.makeText(context, "Playing ${macro.name}", Toast.LENGTH_SHORT).show()
                },
                onDelete = { macro ->
                    MacroStore.delete(context, macro.id)
                    refresh++
                }
            )

            1 -> KeymapTab(
                bindings = bindings,
                macros = macros,
                onAdd = {
                    if (!a11yEnabled) {
                        Toast.makeText(context, "Enable accessibility service first", Toast.LENGTH_SHORT).show()
                        context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                    } else {
                        Prefs.putBool(context, Prefs.KEY_CAPTURE_ACTIVE, true)
                        Toast.makeText(context, "Press any key now…", Toast.LENGTH_SHORT).show()
                    }
                },
                onDelete = { binding ->
                    MacroStore.deleteBinding(context, binding.keyCode)
                    refresh++
                }
            )
        }
    }

    if (pendingKey != null) {
        AlertDialog(
            onDismissRequest = {
                pendingKey = null
                Prefs.putBool(context, Prefs.KEY_CAPTURE_ACTIVE, false)
            },
            title = {
                Text("Key: ${KeyEvent.keyCodeToString(pendingKey ?: 0)}", color = Color.White)
            },
            text = { Text("What should this key do?", color = TextMuted) },
            confirmButton = {
                TextButton(onClick = {
                    startRecorder(context, MacroRecordService.MODE_PICK)
                }) {
                    Text("Pick screen point")
                }
            },
            dismissButton = {
                TextButton(onClick = { showMacroPicker = true }) {
                    Text("Play a macro")
                }
            }
        )
    }

    if (showMacroPicker) {
        AlertDialog(
            onDismissRequest = { showMacroPicker = false },
            title = { Text("Choose a macro", color = Color.White) },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    if (macros.isEmpty()) {
                        Text("No macros saved yet", color = TextMuted)
                    }
                    macros.forEach { macro ->
                        TextButton(onClick = {
                            pendingKey?.let { key ->
                                MacroStore.saveBinding(
                                    context,
                                    KeyBinding(keyCode = key, actionType = "macro", macroId = macro.id)
                                )
                                Toast.makeText(context, "Key bound to ${macro.name}", Toast.LENGTH_SHORT).show()
                            }
                            pendingKey = null
                            showMacroPicker = false
                            Prefs.putBool(context, Prefs.KEY_CAPTURE_ACTIVE, false)
                            refresh++
                        }) {
                            Text(macro.name)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showMacroPicker = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun MacroTab(
    context: Context,
    macros: List<com.ibrahim404.ggspace.macro.Macro>,
    onRecord: () -> Unit,
    onPlay: (com.ibrahim404.ggspace.macro.Macro) -> Unit,
    onDelete: (com.ibrahim404.ggspace.macro.Macro) -> Unit
) {
    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("${macros.size} saved macros", color = TextMuted, fontSize = 12.sp, modifier = Modifier.weight(1f))
            Button(
                onClick = onRecord,
                colors = ButtonDefaults.buttonColors(containerColor = NeonPurple)
            ) {
                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.width(16.dp))
                Text(" Record", fontSize = 11.sp)
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            "Recording opens a touch layer: tap/swipe where you want, then press SAVE.",
            color = Color(0xFF6E6E85),
            fontSize = 11.sp
        )
        Spacer(Modifier.height(12.dp))
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(macros, key = { it.id }) { macro ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(CardSurface)
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(macro.name, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Text("${macro.actions.size} actions", color = TextMuted, fontSize = 11.sp)
                    }
                    OutlinedButton(onClick = { onPlay(macro) }) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.width(15.dp))
                    }
                    Spacer(Modifier.width(6.dp))
                    OutlinedButton(onClick = { onDelete(macro) }) {
                        Icon(Icons.Filled.Delete, contentDescription = null, tint = NeonPink, modifier = Modifier.width(15.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun KeymapTab(
    bindings: List<KeyBinding>,
    macros: List<com.ibrahim404.ggspace.macro.Macro>,
    onAdd: () -> Unit,
    onDelete: (KeyBinding) -> Unit
) {
    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("${bindings.size} bindings", color = TextMuted, fontSize = 12.sp, modifier = Modifier.weight(1f))
            Button(onClick = onAdd, colors = ButtonDefaults.buttonColors(containerColor = NeonPurple)) {
                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.width(16.dp))
                Text(" Add key", fontSize = 11.sp)
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            "Press a key, choose a screen point or macro, then the key will trigger it while the service is on.",
            color = Color(0xFF6E6E85),
            fontSize = 11.sp
        )
        Spacer(Modifier.height(12.dp))
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(bindings, key = { it.keyCode }) { binding ->
                val target = if (binding.actionType == "point") {
                    "tap (${binding.x}, ${binding.y})"
                } else {
                    macros.firstOrNull { it.id == binding.macroId }?.name ?: "macro"
                }
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(CardSurfaceAlt)
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(KeyEvent.keyCodeToString(binding.keyCode), color = NeonCyan, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Text("→ $target", color = TextMuted, fontSize = 11.sp)
                    }
                    OutlinedButton(onClick = { onDelete(binding) }) {
                        Icon(Icons.Filled.Delete, contentDescription = null, tint = NeonPink, modifier = Modifier.width(15.dp))
                    }
                }
            }
        }
    }
}

private fun startRecorder(context: Context, mode: String) {
    if (!Settings.canDrawOverlays(context)) {
        Permissions.requestOverlay(context)
        Toast.makeText(context, "Grant overlay permission first", Toast.LENGTH_SHORT).show()
        return
    }
    val intent = Intent(context, MacroRecordService::class.java).putExtra("mode", mode)
    runCatching { ContextCompat.startForegroundService(context, intent) }
}

private fun isAccessibilityEnabled(context: Context): Boolean {
    val enabled = runCatching {
        Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        )
    }.getOrDefault(null) ?: return false
    val expected = ComponentName(context, GGAccessibilityService::class.java).flattenToString()
    return enabled.split(':').any {
        it.equals(expected, ignoreCase = true) || it.contains(GGAccessibilityService::class.java.name)
    }
}
