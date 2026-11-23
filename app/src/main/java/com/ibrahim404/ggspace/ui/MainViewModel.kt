package com.ibrahim404.ggspace.ui

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ibrahim404.ggspace.booster.Booster
import com.ibrahim404.ggspace.data.Game
import com.ibrahim404.ggspace.data.GameRepository
import com.ibrahim404.ggspace.util.DeviceInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DeviceStats(
    val freeMb: Long = 0,
    val totalMb: Long = 0,
    val battery: Int = -1,
    val charging: Boolean = false,
    val cores: Int = 0
)

class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val repository = GameRepository(app)
    private val booster = Booster(app)

    private val _allApps = MutableStateFlow<List<Game>>(emptyList())
    val allApps: StateFlow<List<Game>> = _allApps

    private val _stats = MutableStateFlow(DeviceStats())
    val stats: StateFlow<DeviceStats> = _stats

    private val _boostResult = MutableStateFlow<Long?>(null)
    val boostResult: StateFlow<Long?> = _boostResult

    private val _refreshing = MutableStateFlow(false)
    val refreshing: StateFlow<Boolean> = _refreshing

    val savedPackages = repository.savedPackages.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet()
    )
    val favorites = repository.favorites.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet()
    )
    val recentPairs = repository.recents.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )
    val overlayEnabled = repository.overlayEnabled.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), false
    )
    val autoBoost = repository.autoBoost.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), true
    )
    val haptic = repository.haptic.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), true
    )

    val savedGames: StateFlow<List<Game>> = combine(savedPackages, allApps) { saved, apps ->
        apps.filter { it.packageName in saved }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favoriteGames: StateFlow<List<Game>> = combine(favorites, savedGames) { favs, games ->
        games.filter { it.packageName in favs }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentGames: StateFlow<List<Game>> = combine(recentPairs, savedGames) { pairs, games ->
        val byPackage = games.associateBy { it.packageName }
        pairs.mapNotNull { byPackage[it.first] }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        refreshApps()
        viewModelScope.launch(Dispatchers.IO) {
            while (true) {
                _stats.value = readStats()
                delay(3000)
            }
        }
    }

    private fun readStats(): DeviceStats {
        val context = getApplication<Application>()
        val (free, total) = DeviceInfo.memory(context)
        return DeviceStats(
            freeMb = free,
            totalMb = total,
            battery = DeviceInfo.batteryPercent(context),
            charging = DeviceInfo.isCharging(context),
            cores = DeviceInfo.cpuCores()
        )
    }

    fun refreshApps() {
        viewModelScope.launch(Dispatchers.IO) {
            _refreshing.value = true
            _allApps.value = repository.getInstalledApps()
            _refreshing.value = false
            _stats.value = readStats()
        }
    }

    fun launchGame(pkg: String) {
        val context = getApplication<Application>()
        val launchIntent = context.packageManager.getLaunchIntentForPackage(pkg) ?: return
        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(launchIntent) }
        viewModelScope.launch(Dispatchers.IO) { repository.recordLaunch(pkg) }
    }

    fun boost() {
        viewModelScope.launch(Dispatchers.IO) {
            val freed = booster.boost()
            _boostResult.value = freed
            _stats.value = readStats()
            delay(2800)
            _boostResult.value = null
        }
    }

    fun addGame(pkg: String) {
        viewModelScope.launch(Dispatchers.IO) { repository.addGame(pkg) }
    }

    fun removeGame(pkg: String) {
        viewModelScope.launch(Dispatchers.IO) { repository.removeGame(pkg) }
    }

    fun toggleFavorite(pkg: String) {
        viewModelScope.launch(Dispatchers.IO) { repository.toggleFavorite(pkg) }
    }

    fun setOverlay(enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) { repository.setOverlay(enabled) }
    }

    fun setAutoBoost(enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) { repository.setAutoBoost(enabled) }
    }

    fun setHaptic(enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) { repository.setHaptic(enabled) }
    }
}
