package com.ibrahim404.ggspace.data

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.gameDataStore: DataStore<Preferences> by preferencesDataStore(name = "ggspace")

class GameRepository(private val context: Context) {

    private val savedKey = stringSetPreferencesKey("saved_games")
    private val favoritesKey = stringSetPreferencesKey("favorite_games")
    private val recentsKey = stringSetPreferencesKey("recent_games")
    private val overlayKey = booleanPreferencesKey("overlay_enabled")
    private val autoBoostKey = booleanPreferencesKey("auto_boost")
    private val hapticKey = booleanPreferencesKey("haptic")

    val savedPackages: Flow<Set<String>> =
        context.gameDataStore.data.map { it[savedKey] ?: emptySet() }

    val favorites: Flow<Set<String>> =
        context.gameDataStore.data.map { it[favoritesKey] ?: emptySet() }

    val recents: Flow<List<Pair<String, Long>>> =
        context.gameDataStore.data.map { prefs ->
            (prefs[recentsKey] ?: emptySet()).mapNotNull { raw ->
                val split = raw.indexOf('|')
                if (split > 0) {
                    raw.substring(0, split) to (raw.substring(split + 1).toLongOrNull() ?: 0L)
                } else null
            }.sortedByDescending { it.second }
        }

    val overlayEnabled: Flow<Boolean> =
        context.gameDataStore.data.map { it[overlayKey] ?: false }

    val autoBoost: Flow<Boolean> =
        context.gameDataStore.data.map { it[autoBoostKey] ?: true }

    val haptic: Flow<Boolean> =
        context.gameDataStore.data.map { it[hapticKey] ?: true }

    suspend fun addGame(pkg: String) {
        context.gameDataStore.edit { prefs ->
            prefs[savedKey] = (prefs[savedKey] ?: emptySet()) + pkg
        }
    }

    suspend fun removeGame(pkg: String) {
        context.gameDataStore.edit { prefs ->
            prefs[savedKey] = (prefs[savedKey] ?: emptySet()) - pkg
            prefs[favoritesKey] = (prefs[favoritesKey] ?: emptySet()) - pkg
        }
    }

    suspend fun toggleFavorite(pkg: String) {
        context.gameDataStore.edit { prefs ->
            val current = prefs[favoritesKey] ?: emptySet()
            prefs[favoritesKey] = if (pkg in current) current - pkg else current + pkg
        }
    }

    suspend fun recordLaunch(pkg: String) {
        context.gameDataStore.edit { prefs ->
            val stamp = "$pkg|${System.currentTimeMillis()}"
            val current = prefs[recentsKey] ?: emptySet()
            val updated = (current.filterNot { it.startsWith("$pkg|") } + stamp)
                .sortedByDescending { it.substringAfter('|').toLongOrNull() ?: 0L }
                .take(12)
                .toSet()
            prefs[recentsKey] = updated
        }
    }

    suspend fun setOverlay(enabled: Boolean) {
        context.gameDataStore.edit { it[overlayKey] = enabled }
    }

    suspend fun setAutoBoost(enabled: Boolean) {
        context.gameDataStore.edit { it[autoBoostKey] = enabled }
    }

    suspend fun setHaptic(enabled: Boolean) {
        context.gameDataStore.edit { it[hapticKey] = enabled }
    }

    fun getInstalledApps(): List<Game> {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        return pm.queryIntentActivities(intent, 0)
            .mapNotNull { resolve ->
                val pkg = resolve.activityInfo.packageName
                if (pkg == context.packageName) return@mapNotNull null
                val label = runCatching { resolve.loadLabel(pm).toString() }
                    .getOrDefault(pkg.substringAfterLast('.'))
                Game(packageName = pkg, label = label)
            }
            .distinctBy { it.packageName }
            .sortedBy { it.label.lowercase() }
    }

    fun getGameInfo(pkg: String): Game? = try {
        val pm: PackageManager = context.packageManager
        val appInfo = pm.getApplicationInfo(pkg, 0)
        Game(packageName = pkg, label = pm.getApplicationLabel(appInfo).toString())
    } catch (e: PackageManager.NameNotFoundException) {
        null
    }

    companion object {
        private val GAME_HINTS = listOf(
            "game", "gaming", "pubg", "free fire", "freefire", "bgmi", "mobile legends",
            "mlbb", "clash", "brawl", "roblox", "minecraft", "fortnite", "subway",
            "temple", "racing", "asphalt", "nfs", "need for speed", "gta", "call of duty",
            "battleground", "valorant", "wild rift", "arena", "shooter", "solitaire",
            "chess", "puzzle", "ludo", "candy", "fruit", "dragon", "hero", "ninja",
            "sports", "fifa", "pes", "cod", "ff", "ml"
        )

        fun isLikelyGame(label: String, pkg: String): Boolean {
            val text = "$label $pkg".lowercase()
            return GAME_HINTS.any { text.contains(it) }
        }
    }
}
