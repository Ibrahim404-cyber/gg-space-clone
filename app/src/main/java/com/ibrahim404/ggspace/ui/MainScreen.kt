package com.ibrahim404.ggspace.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Launch
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ibrahim404.ggspace.data.Game
import com.ibrahim404.ggspace.data.GameRepository
import com.ibrahim404.ggspace.ui.theme.AppBackground
import com.ibrahim404.ggspace.ui.theme.CardSurface
import com.ibrahim404.ggspace.ui.theme.CardSurfaceAlt
import com.ibrahim404.ggspace.ui.theme.NeonCyan
import com.ibrahim404.ggspace.ui.theme.NeonPink
import com.ibrahim404.ggspace.ui.theme.NeonPurple
import com.ibrahim404.ggspace.ui.theme.TextMuted

private data class NavItem(val label: String, val icon: ImageVector)

@Composable
fun MainScreen(
    vm: MainViewModel,
    onToggleOverlay: (Boolean) -> Unit,
    onRequestOverlay: () -> Unit,
    onRequestWriteSettings: () -> Unit,
    onRequestDnd: () -> Unit,
    canOverlay: () -> Boolean,
    canWriteSettings: () -> Boolean,
    canControlDnd: () -> Boolean
) {
    var tab by remember { mutableStateOf(0) }
    val items = listOf(
        NavItem("Home", Icons.Filled.Home),
        NavItem("Library", Icons.Filled.SportsEsports),
        NavItem("Tools", Icons.Filled.Build),
        NavItem("Settings", Icons.Filled.Settings)
    )

    Scaffold(
        containerColor = AppBackground,
        bottomBar = {
            NavigationBar(
                containerColor = CardSurface,
                contentColor = Color.White,
                windowInsets = NavigationBarDefaults.windowInsets
            ) {
                items.forEachIndexed { index, item ->
                    NavigationBarItem(
                        selected = tab == index,
                        onClick = { tab = index },
                        icon = { Icon(item.icon, contentDescription = null) },
                        label = { Text(item.label, fontSize = 10.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = NeonPurple,
                            selectedTextColor = NeonPurple,
                            indicatorColor = NeonPurple.copy(alpha = 0.16f),
                            unselectedIconColor = TextMuted,
                            unselectedTextColor = TextMuted
                        )
                    )
                }
            }
        }
    ) { padding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .background(
                    Brush.radialGradient(
                        colors = listOf(NeonPurple.copy(alpha = 0.10f), Color.Transparent),
                        radius = 720f
                    )
                )
        ) {
            when (tab) {
                0 -> HomeScreen(vm = vm, onGoLibrary = { tab = 1 })
                1 -> LibraryScreen(vm = vm)
                2 -> ToolsScreen(
                    vm = vm,
                    onToggleOverlay = onToggleOverlay,
                    onRequestOverlay = onRequestOverlay,
                    onRequestWriteSettings = onRequestWriteSettings,
                    onRequestDnd = onRequestDnd,
                    canOverlay = canOverlay
                )
                3 -> SettingsScreen(
                    vm = vm,
                    canOverlay = canOverlay,
                    canWriteSettings = canWriteSettings,
                    canControlDnd = canControlDnd,
                    onRequestOverlay = onRequestOverlay,
                    onRequestWriteSettings = onRequestWriteSettings,
                    onRequestDnd = onRequestDnd
                )
            }
        }
    }
}

@Composable
private fun HomeScreen(vm: MainViewModel, onGoLibrary: () -> Unit) {
    val stats by vm.stats.collectAsState()
    val boostResult by vm.boostResult.collectAsState()
    val recents by vm.recentGames.collectAsState()
    val favorites by vm.favoriteGames.collectAsState()
    val saved by vm.savedGames.collectAsState()

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 18.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(NeonPurple, NeonPink))),
                contentAlignment = Alignment.Center
            ) {
                Text("G", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("GG Space", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Text("Your game command center", color = TextMuted, fontSize = 12.sp)
            }
            IconButton(onClick = { vm.refreshApps() }) {
                Icon(Icons.Filled.Refresh, contentDescription = "Refresh", tint = NeonCyan)
            }
        }

        Spacer(Modifier.height(22.dp))
        BoostHero(stats = stats, result = boostResult, onBoost = { vm.boost() })

        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatCard("RAM FREE", "${stats.freeMb} MB", Icons.Filled.Memory, NeonCyan)
            StatCard("BATTERY", if (stats.battery >= 0) "${stats.battery}%" else "--", Icons.Filled.BatteryChargingFull, NeonPink)
            StatCard("CPU", "${stats.cores} cores", Icons.Filled.Bolt, NeonPurple)
        }

        Spacer(Modifier.height(24.dp))
        SectionTitle("Quick tools")
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            QuickTool("Boost", Icons.Filled.Bolt, NeonPink, onClick = { vm.boost() })
            QuickTool("Library", Icons.Filled.SportsEsports, NeonPurple, onClick = onGoLibrary)
            QuickTool("Refresh", Icons.Filled.Refresh, NeonCyan, onClick = { vm.refreshApps() })
        }

        Spacer(Modifier.height(24.dp))
        val showcase = if (recents.isNotEmpty()) recents.take(8) else favorites.take(8)
        SectionTitle(
            title = if (recents.isNotEmpty()) "Recently played" else "Favourites",
            action = if (showcase.isNotEmpty()) "See all" else null,
            onAction = onGoLibrary
        )
        Spacer(Modifier.height(10.dp))
        if (showcase.isEmpty() && saved.isEmpty()) {
            EmptyGameCard(onGoLibrary)
        } else if (showcase.isEmpty()) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(CardSurface)
                    .padding(20.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Launch a game to see it here", color = TextMuted, fontSize = 12.sp)
            }
        } else {
            Row(
                Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                showcase.forEach { game ->
                    RecentTile(game = game, onClick = { vm.launchGame(game.packageName) })
                }
            }
        }
        Spacer(Modifier.height(26.dp))
    }
}

@Composable
private fun BoostHero(stats: DeviceStats, result: Long?, onBoost: () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(154.dp)
            .clip(RoundedCornerShape(26.dp))
            .background(Brush.linearGradient(listOf(NeonPurple, NeonPink)))
            .clickable(onClick = onBoost)
            .padding(18.dp)
    ) {
        Column(Modifier.align(Alignment.CenterStart)) {
            Text(
                "BOOST YOUR GAME",
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(6.dp))
            Text("One tap.\nMore space.", color = Color.White, fontSize = 23.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            AnimatedVisibility(visible = result != null) {
                Text(
                    "+${result ?: 0} MB RAM freed · ${stats.freeMb} MB available",
                    color = Color.White,
                    fontSize = 12.sp
                )
            }
        }
        Box(
            Modifier
                .align(Alignment.CenterEnd)
                .size(72.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.Bolt, contentDescription = null, tint = Color.White, modifier = Modifier.size(36.dp))
        }
    }
}

@Composable
private fun RowScope.StatCard(label: String, value: String, icon: ImageVector, accent: Color) {
    Column(
        Modifier
            .weight(1f)
            .clip(RoundedCornerShape(18.dp))
            .background(CardSurface)
            .padding(13.dp)
    ) {
        Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(18.dp))
        Spacer(Modifier.height(9.dp))
        Text(value, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 1)
        Text(label, color = TextMuted, fontSize = 10.sp)
    }
}

@Composable
private fun SectionTitle(title: String, action: String? = null, onAction: (() -> Unit)? = null) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(title, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.weight(1f))
        if (action != null && onAction != null) {
            Text(
                action,
                color = NeonCyan,
                fontSize = 12.sp,
                modifier = Modifier.clickable(onClick = onAction)
            )
        }
    }
}

@Composable
private fun RowScope.QuickTool(label: String, icon: ImageVector, accent: Color, onClick: () -> Unit) {
    Column(
        Modifier
            .weight(1f)
            .clip(RoundedCornerShape(18.dp))
            .background(CardSurface)
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(accent.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(19.dp))
        }
        Spacer(Modifier.height(8.dp))
        Text(label, color = Color(0xFFD8D8E6), fontSize = 11.sp)
    }
}

private fun drawableToBitmap(drawable: Drawable): ImageBitmap {
    val bitmap = Bitmap.createBitmap(
        drawable.intrinsicWidth.coerceAtLeast(1),
        drawable.intrinsicHeight.coerceAtLeast(1),
        Bitmap.Config.ARGB_8888
    )
    val canvas = android.graphics.Canvas(bitmap)
    drawable.setBounds(0, 0, bitmap.width, bitmap.height)
    drawable.draw(canvas)
    return bitmap.asImageBitmap()
}

@Composable
private fun GameAvatar(pkg: String, label: String, size: Int = 48) {
    val context = LocalContext.current
    val drawable = remember(pkg) {
        runCatching { context.packageManager.getApplicationIcon(pkg) }.getOrNull()
    }
    val shape = RoundedCornerShape((size / 4).dp)
    if (drawable != null) {
        Image(
            bitmap = remember(drawable) { drawableToBitmap(drawable) },
            contentDescription = label,
            modifier = Modifier.size(size.dp).clip(shape)
        )
    } else {
        Box(
            Modifier.size(size.dp).clip(shape).background(CardSurfaceAlt),
            contentAlignment = Alignment.Center
        ) {
            Text(label.take(1), color = NeonPurple, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun RecentTile(game: Game, onClick: () -> Unit) {
    Column(
        Modifier
            .width(78.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(CardSurface)
            .clickable(onClick = onClick)
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        GameAvatar(game.packageName, game.label, 46)
        Spacer(Modifier.height(8.dp))
        Text(
            game.label,
            color = Color.White,
            fontSize = 11.sp,
            maxLines = 2,
            textAlign = TextAlign.Center,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun EmptyGameCard(onGoLibrary: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(CardSurface)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Filled.SportsEsports, contentDescription = null, tint = NeonPurple, modifier = Modifier.size(32.dp))
        Spacer(Modifier.height(10.dp))
        Text("No games yet", color = Color.White, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        Text(
            "Add your favourite games from the library",
            color = TextMuted,
            fontSize = 12.sp,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(14.dp))
        Button(
            onClick = onGoLibrary,
            colors = ButtonDefaults.buttonColors(containerColor = NeonPurple)
        ) {
            Text("Browse games")
        }
    }
}

@Composable
fun LibraryScreen(vm: MainViewModel) {
    val saved by vm.savedGames.collectAsState()
    val favorites by vm.favorites.collectAsState()
    val allApps by vm.allApps.collectAsState()
    val refreshing by vm.refreshing.collectAsState()
    var query by remember { mutableStateOf("") }
    var onlyFavorites by remember { mutableStateOf(false) }
    var showAdd by remember { mutableStateOf(false) }

    val shown = saved.filter { game ->
        game.label.contains(query, ignoreCase = true) &&
            (!onlyFavorites || game.packageName in favorites)
    }

    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 18.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Game Library", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Text("${saved.size} games in your space", color = TextMuted, fontSize = 12.sp)
            }
            IconButton(onClick = { vm.refreshApps() }) {
                Icon(Icons.Filled.Refresh, contentDescription = "Refresh", tint = NeonCyan)
            }
        }

        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            singleLine = true,
            placeholder = { Text("Search games", color = TextMuted) },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = TextMuted) },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = NeonPurple,
                unfocusedBorderColor = CardSurfaceAlt,
                focusedContainerColor = CardSurface,
                unfocusedContainerColor = CardSurface,
                cursorColor = NeonCyan
            )
        )

        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            FilterChip(
                selected = !onlyFavorites,
                onClick = { onlyFavorites = false },
                label = { Text("All") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = NeonPurple.copy(alpha = 0.22f),
                    selectedLabelColor = NeonCyan
                )
            )
            Spacer(Modifier.width(8.dp))
            FilterChip(
                selected = onlyFavorites,
                onClick = { onlyFavorites = true },
                label = { Text("Favourites") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = NeonPink.copy(alpha = 0.22f),
                    selectedLabelColor = NeonPink
                )
            )
            Spacer(Modifier.weight(1f))
            Button(
                onClick = { showAdd = true },
                colors = ButtonDefaults.buttonColors(containerColor = NeonPurple)
            ) {
                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("Add")
            }
        }

        Spacer(Modifier.height(14.dp))
        if (shown.isEmpty()) {
            Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                Text("No games found", color = TextMuted, fontSize = 13.sp)
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(100.dp),
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(shown, key = { it.packageName }) { game ->
                    GameGridTile(
                        game = game,
                        isFavorite = game.packageName in favorites,
                        onLaunch = { vm.launchGame(game.packageName) },
                        onFavorite = { vm.toggleFavorite(game.packageName) },
                        onRemove = { vm.removeGame(game.packageName) }
                    )
                }
            }
        }
    }

    if (showAdd) {
        AddGameSheet(
            allApps = allApps,
            savedPackages = saved.map { it.packageName }.toSet(),
            refreshing = refreshing,
            onAdd = { vm.addGame(it) },
            onRefresh = { vm.refreshApps() },
            onDismiss = { showAdd = false }
        )
    }
}

@Composable
private fun GameGridTile(
    game: Game,
    isFavorite: Boolean,
    onLaunch: () -> Unit,
    onFavorite: () -> Unit,
    onRemove: () -> Unit
) {
    Box(Modifier.clip(RoundedCornerShape(20.dp)).background(CardSurface)) {
        Column(
            Modifier.clickable(onClick = onLaunch).padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            GameAvatar(game.packageName, game.label, 52)
            Spacer(Modifier.height(9.dp))
            Text(
                game.label,
                color = Color.White,
                fontSize = 11.sp,
                maxLines = 2,
                textAlign = TextAlign.Center,
                overflow = TextOverflow.Ellipsis
            )
        }
        IconButton(
            onClick = onFavorite,
            modifier = Modifier.align(Alignment.TopStart).size(30.dp)
        ) {
            Icon(
                Icons.Filled.Star,
                contentDescription = "Favourite",
                tint = if (isFavorite) Color(0xFFFFC107) else Color(0xFF5A5A70),
                modifier = Modifier.size(16.dp)
            )
        }
        IconButton(
            onClick = onRemove,
            modifier = Modifier.align(Alignment.TopEnd).size(30.dp)
        ) {
            Icon(Icons.Filled.Close, contentDescription = "Remove", tint = Color(0xFF8A8AA0), modifier = Modifier.size(15.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddGameSheet(
    allApps: List<Game>,
    savedPackages: Set<String>,
    refreshing: Boolean,
    onAdd: (String) -> Unit,
    onRefresh: () -> Unit,
    onDismiss: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    val filtered = remember(query, allApps) {
        allApps
            .filter {
                it.label.contains(query, ignoreCase = true) ||
                    it.packageName.contains(query, ignoreCase = true)
            }
            .sortedByDescending { GameRepository.isLikelyGame(it.label, it.packageName) }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = CardSurface,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(Modifier.fillMaxWidth().fillMaxHeight(0.82f).padding(horizontal = 18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Add a game", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onRefresh) {
                    Icon(Icons.Filled.Refresh, contentDescription = "Refresh", tint = NeonCyan)
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Filled.Close, contentDescription = "Close", tint = TextMuted)
                }
            }
            if (refreshing) {
                Text("Scanning apps…", color = NeonCyan, fontSize = 11.sp)
            }
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                singleLine = true,
                placeholder = { Text("Search installed apps", color = TextMuted) },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = TextMuted) },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NeonPurple,
                    unfocusedBorderColor = CardSurfaceAlt,
                    focusedContainerColor = CardSurfaceAlt,
                    unfocusedContainerColor = CardSurfaceAlt
                )
            )
            Spacer(Modifier.height(8.dp))
            LazyColumn(
                Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(filtered, key = { it.packageName }) { app ->
                    val added = app.packageName in savedPackages
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(CardSurfaceAlt)
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        GameAvatar(app.packageName, app.label, 40)
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                app.label,
                                color = Color.White,
                                fontSize = 13.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                app.packageName,
                                color = TextMuted,
                                fontSize = 10.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        if (added) {
                            Text("ADDED", color = NeonCyan, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        } else {
                            Button(
                                onClick = { onAdd(app.packageName) },
                                colors = ButtonDefaults.buttonColors(containerColor = NeonPurple),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                            ) {
                                Text("ADD", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ToolsScreen(
    vm: MainViewModel,
    onToggleOverlay: (Boolean) -> Unit,
    onRequestOverlay: () -> Unit,
    onRequestWriteSettings: () -> Unit,
    onRequestDnd: () -> Unit,
    canOverlay: () -> Boolean
) {
    val overlay by vm.overlayEnabled.collectAsState()
    val autoBoost by vm.autoBoost.collectAsState()
    val haptic by vm.haptic.collectAsState()
    val stats by vm.stats.collectAsState()

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 18.dp)
    ) {
        Text("Tools & Game Mode", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Text("Fine-tune your gaming session", color = TextMuted, fontSize = 12.sp)

        Spacer(Modifier.height(22.dp))
        SwitchCard(
            title = "Floating overlay",
            description = "Quick tools on top of your games",
            icon = Icons.Filled.Launch,
            checked = overlay,
            status = if (canOverlay()) "Permission granted" else "Tap to grant overlay permission",
            onCheckedChange = { enabled ->
                if (enabled && !canOverlay()) {
                    onRequestOverlay()
                } else {
                    vm.setOverlay(enabled)
                    onToggleOverlay(enabled)
                }
            },
            onStatusClick = if (canOverlay()) null else onRequestOverlay
        )

        Spacer(Modifier.height(12.dp))
        SwitchCard(
            title = "Auto boost",
            description = "Free memory when a game launches",
            icon = Icons.Filled.Bolt,
            checked = autoBoost,
            status = "",
            onCheckedChange = { vm.setAutoBoost(it) }
        )

        Spacer(Modifier.height(12.dp))
        SwitchCard(
            title = "Haptic feedback",
            description = "Vibration on tool actions",
            icon = Icons.Filled.SportsEsports,
            checked = haptic,
            status = "",
            onCheckedChange = { vm.setHaptic(it) }
        )

        Spacer(Modifier.height(20.dp))
        SectionTitle("System controls")
        Spacer(Modifier.height(10.dp))
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(CardSurface)
                .padding(16.dp)
        ) {
            Text("Brightness & Do Not Disturb", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(5.dp))
            Text(
                "Toggle them from the floating panel during a game, or open system settings here.",
                color = TextMuted,
                fontSize = 11.sp
            )
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = onRequestWriteSettings) { Text("Brightness", fontSize = 11.sp) }
                OutlinedButton(onClick = onRequestDnd) { Text("Do Not Disturb", fontSize = 11.sp) }
            }
        }

        Spacer(Modifier.height(12.dp))
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(CardSurface)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text("Memory", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text("${stats.freeMb} MB free of ${stats.totalMb} MB", color = TextMuted, fontSize = 11.sp)
            }
            Button(
                onClick = { vm.boost() },
                colors = ButtonDefaults.buttonColors(containerColor = NeonPink)
            ) {
                Text("Boost", fontSize = 12.sp)
            }
        }
        Spacer(Modifier.height(26.dp))
    }
}

@Composable
private fun SwitchCard(
    title: String,
    description: String,
    icon: ImageVector,
    checked: Boolean,
    status: String,
    onCheckedChange: (Boolean) -> Unit,
    onStatusClick: (() -> Unit)? = null
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(CardSurface)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(NeonPurple.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = NeonPurple, modifier = Modifier.size(21.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(3.dp))
            Text(description, color = TextMuted, fontSize = 11.sp)
            if (status.isNotEmpty()) {
                Spacer(Modifier.height(5.dp))
                Text(
                    status,
                    color = if (onStatusClick != null) NeonPink else NeonCyan,
                    fontSize = 10.sp,
                    modifier = if (onStatusClick != null) Modifier.clickable(onClick = onStatusClick) else Modifier
                )
            }
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
fun SettingsScreen(
    vm: MainViewModel,
    canOverlay: () -> Boolean,
    canWriteSettings: () -> Boolean,
    canControlDnd: () -> Boolean,
    onRequestOverlay: () -> Unit,
    onRequestWriteSettings: () -> Unit,
    onRequestDnd: () -> Unit
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val version = runCatching {
        @Suppress("DEPRECATION")
        context.packageManager.getPackageInfo(context.packageName, 0).versionName
    }.getOrDefault("1.0.0")

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 18.dp)
    ) {
        Text("Settings", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(18.dp))

        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(CardSurface)
                .padding(16.dp)
        ) {
            Text("About", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text(
                "GG Space is a personal-use, clean-room game space inspired by the original GG Game Space. Built with Jetpack Compose and Material 3.",
                color = TextMuted,
                fontSize = 12.sp
            )
            Spacer(Modifier.height(8.dp))
            Text("Version $version · ${context.packageName}", color = NeonCyan, fontSize = 11.sp)
            Text("Not affiliated with the original app.", color = Color(0xFF6E6E85), fontSize = 10.sp)
        }

        Spacer(Modifier.height(14.dp))
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(CardSurface)
                .padding(16.dp)
        ) {
            Text("Permissions", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            PermissionRow("Display over other apps", canOverlay(), onRequestOverlay)
            PermissionRow("Modify system settings", canWriteSettings(), onRequestWriteSettings)
            PermissionRow("Do Not Disturb access", canControlDnd(), onRequestDnd)
        }

        Spacer(Modifier.height(14.dp))
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(CardSurface)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text("Installed games", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Text("Re-scan apps on your device", color = TextMuted, fontSize = 11.sp)
            }
            OutlinedButton(onClick = { vm.refreshApps() }) { Text("Re-scan", fontSize = 11.sp) }
        }

        Spacer(Modifier.height(14.dp))
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(CardSurface)
                .clickable {
                    runCatching {
                        uriHandler.openUri("https://github.com/Ibrahim404-cyber/gg-space-clone")
                    }
                }
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.Launch, contentDescription = null, tint = NeonPurple)
            Spacer(Modifier.width(12.dp))
            Column {
                Text("Source on GitHub", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Text("Ibrahim404-cyber/gg-space-clone", color = TextMuted, fontSize = 11.sp)
            }
        }
        Spacer(Modifier.height(26.dp))
    }
}

@Composable
private fun PermissionRow(label: String, granted: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = Color.White, fontSize = 13.sp, modifier = Modifier.weight(1f))
        Text(
            if (granted) "GRANTED" else "OPEN SETTINGS",
            color = if (granted) NeonCyan else NeonPink,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
