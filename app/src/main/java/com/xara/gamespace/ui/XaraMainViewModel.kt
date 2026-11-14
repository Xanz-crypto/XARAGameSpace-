package com.xara.gamespace.ui

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.xara.gamespace.XaraApplication
import com.xara.gamespace.data.local.BoostHistoryEntity
import com.xara.gamespace.data.local.GameProfileEntity
import com.xara.gamespace.data.local.GameSessionEntity
import com.xara.gamespace.data.local.MacroConfigEntity
import com.xara.gamespace.data.local.TouchProfileEntity
import com.xara.gamespace.domain.model.ActiveSessionState
import com.xara.gamespace.domain.model.BoostMode
import com.xara.gamespace.domain.model.BoostResult
import com.xara.gamespace.domain.model.CompilationResult
import com.xara.gamespace.domain.model.CompileMode
import com.xara.gamespace.domain.model.DeviceTelemetry
import com.xara.gamespace.domain.model.FloatingPositionPreset
import com.xara.gamespace.domain.model.GraphicsCompatibility
import com.xara.gamespace.domain.model.GraphicsDriverOption
import com.xara.gamespace.domain.model.InstalledGameItem
import com.xara.gamespace.domain.model.ShizukuStatus
import com.xara.gamespace.domain.model.TextureOptMode
import com.xara.gamespace.domain.model.ThemeAccent
import com.xara.gamespace.domain.model.TouchZoneMode
import com.xara.gamespace.domain.model.XaraUserSettings
import com.xara.gamespace.permissions.PermissionHelper
import com.xara.gamespace.services.FloatingPanelService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class XaraMainViewModel(application: Application) : AndroidViewModel(application) {

    private val container = XaraApplication.from(application).container
    private val repository = container.repository
    private val telemetryMonitor = container.telemetryMonitor
    private val appsScanner = container.appsScanner
    private val boostEngine = container.boostEngine
    private val shizukuEngine = container.shizukuEngine
    private val graphicsEngine = container.graphicsEngine
    private val compilerEngine = container.compilerEngine
    private val sessionManager = container.sessionManager

    val telemetry: StateFlow<DeviceTelemetry> = telemetryMonitor.observeTelemetry(1500L)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), telemetryMonitor.collectSnapshotSync())

    val userSettings: StateFlow<XaraUserSettings> = repository.settingsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), XaraUserSettings())

    val shizukuStatus: StateFlow<ShizukuStatus> = shizukuEngine.status
    val sessionState: StateFlow<ActiveSessionState> = sessionManager.sessionState
    val isFloatingPanelRunning: StateFlow<Boolean> = container.isFloatingPanelRunning.asStateFlow()
    val pendingDeepLinkRoute: StateFlow<String?> = container.pendingDeepLinkRoute.asStateFlow()

    val boostHistory: StateFlow<List<BoostHistoryEntity>> = repository.boostHistoryFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val gameSessions: StateFlow<List<GameSessionEntity>> = repository.gameSessionsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val touchProfiles: StateFlow<List<TouchProfileEntity>> = repository.touchProfilesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val macroConfigs: StateFlow<List<MacroConfigEntity>> = repository.macroConfigsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val rawInstalledApps = MutableStateFlow<List<InstalledGameItem>>(emptyList())
    val installedApps: StateFlow<List<InstalledGameItem>> = combine(
        rawInstalledApps,
        repository.gameProfilesFlow
    ) { apps, profiles ->
        val map = profiles.associateBy { it.packageName }
        apps.map { app ->
            val saved = map[app.packageName]
            if (saved != null) {
                app.copy(
                    isAddedToSpace = saved.isAddedToSpace,
                    isFavorite = saved.isFavorite,
                    lastPlayedTimestamp = saved.lastPlayedTimestamp,
                    totalPlaytimeSeconds = saved.totalPlaytimeSeconds,
                    boostModeOverride = saved.boostModeOverride?.let {
                        runCatching { BoostMode.valueOf(it) }.getOrNull()
                    },
                    graphicsDriver = runCatching { GraphicsDriverOption.valueOf(saved.graphicsDriver) }
                        .getOrDefault(GraphicsDriverOption.DEFAULT),
                    textureOptMode = runCatching { TextureOptMode.valueOf(saved.textureOptMode) }
                        .getOrDefault(TextureOptMode.SHARP_NATIVE),
                    sensitivityX = saved.sensitivityX,
                    sensitivityY = saved.sensitivityY,
                    compileStatus = saved.compileStatus,
                    lastCompiledTimestamp = saved.lastCompiledTimestamp
                )
            } else {
                app
            }
        }.sortedWith(
            compareByDescending<InstalledGameItem> { it.isFavorite }
                .thenByDescending { it.isAddedToSpace }
                .thenByDescending { it.lastPlayedTimestamp }
                .thenBy { it.appName.lowercase() }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isScanningApps = MutableStateFlow(true)
    val isScanningApps: StateFlow<Boolean> = _isScanningApps.asStateFlow()

    private val _boostProgress = MutableStateFlow<Pair<Float, String>?>(null)
    val boostProgress: StateFlow<Pair<Float, String>?> = _boostProgress.asStateFlow()

    private val _lastBoostResult = MutableStateFlow<BoostResult?>(null)
    val lastBoostResult: StateFlow<BoostResult?> = _lastBoostResult.asStateFlow()

    private val _graphicsStatusMessage = MutableStateFlow<String?>(null)
    val graphicsStatusMessage: StateFlow<String?> = _graphicsStatusMessage.asStateFlow()

    private val _compileProgressPackage = MutableStateFlow<String?>(null)
    val compileProgressPackage: StateFlow<String?> = _compileProgressPackage.asStateFlow()

    private val _lastCompilationResult = MutableStateFlow<CompilationResult?>(null)
    val lastCompilationResult: StateFlow<CompilationResult?> = _lastCompilationResult.asStateFlow()

    private val _networkPingHistory = MutableStateFlow<List<Int>>(emptyList())
    val networkPingHistory: StateFlow<List<Int>> = _networkPingHistory.asStateFlow()

    private val _statusBanner = MutableStateFlow<String?>(null)
    val statusBanner: StateFlow<String?> = _statusBanner.asStateFlow()

    val graphicsCompatibility: GraphicsCompatibility = graphicsEngine.inspectCompatibility()

    init {
        viewModelScope.launch {
            repository.seedInitialPresetsIfEmpty()
            rescanInstalledApps()
        }
    }

    fun consumeDeepLinkRoute() {
        container.pendingDeepLinkRoute.value = null
    }

    fun clearStatusBanner() {
        _statusBanner.value = null
    }

    fun setFpsTrackingActive(active: Boolean) {
        if (active) telemetryMonitor.startFpsTracking() else telemetryMonitor.stopFpsTracking()
    }

    fun getAppIconBitmap(packageName: String) = appsScanner.getAppIconBitmap(packageName)

    fun rescanInstalledApps() {
        viewModelScope.launch {
            _isScanningApps.value = true
            val savedMap = repository.gameProfilesFlow.first().associateBy { it.packageName }
            rawInstalledApps.value = appsScanner.scanInstalledApps(savedMap)
            _isScanningApps.value = false
        }
    }

    fun toggleGameInSpace(item: InstalledGameItem) {
        viewModelScope.launch {
            val newAdded = !item.isAddedToSpace
            val existing = repository.getGameProfile(item.packageName)
            repository.upsertGameProfile(
                (existing ?: GameProfileEntity(item.packageName, item.appName)).copy(
                    isAddedToSpace = newAdded
                )
            )
            _statusBanner.value = if (newAdded) {
                "${item.appName} added to XARAGameSpace"
            } else {
                "${item.appName} removed from XARAGameSpace"
            }
        }
    }

    fun toggleFavoriteGame(item: InstalledGameItem) {
        viewModelScope.launch {
            val existing = repository.getGameProfile(item.packageName)
            val newFav = !item.isFavorite
            repository.upsertGameProfile(
                (existing ?: GameProfileEntity(item.packageName, item.appName, isAddedToSpace = true)).copy(
                    isAddedToSpace = if (newFav) true else (existing?.isAddedToSpace ?: item.isAddedToSpace),
                    isFavorite = newFav
                )
            )
        }
    }

    fun launchGame(item: InstalledGameItem) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val existing = repository.getGameProfile(item.packageName)
            repository.upsertGameProfile(
                (existing ?: GameProfileEntity(item.packageName, item.appName, isAddedToSpace = true)).copy(
                    isAddedToSpace = true,
                    lastPlayedTimestamp = now
                )
            )
            val launched = appsScanner.launchApp(item.packageName)
            if (launched) {
                val settings = userSettings.value
                if (!sessionState.value.isRunning) {
                    sessionManager.startSession(
                        gamePackage = item.packageName,
                        gameName = item.appName,
                        mode = item.boostModeOverride ?: settings.boostMode,
                        detectActiveApp = settings.activeGameDetectionEnabled,
                        notificationsEnabled = settings.notificationsEnabled
                    )
                }
            } else {
                _statusBanner.value = "Unable to launch ${item.appName}"
            }
        }
    }

    fun updateGameSettings(
        item: InstalledGameItem,
        boostModeOverride: BoostMode?,
        driver: GraphicsDriverOption,
        textureMode: TextureOptMode,
        sensX: Float,
        sensY: Float
    ) {
        viewModelScope.launch {
            val existing = repository.getGameProfile(item.packageName)
            repository.upsertGameProfile(
                (existing ?: GameProfileEntity(item.packageName, item.appName, isAddedToSpace = true)).copy(
                    isAddedToSpace = true,
                    boostModeOverride = boostModeOverride?.name,
                    graphicsDriver = driver.name,
                    textureOptMode = textureMode.name,
                    sensitivityX = sensX,
                    sensitivityY = sensY
                )
            )
            _statusBanner.value = "Saved ${item.appName} profile"
        }
    }

    fun setBoostMode(mode: BoostMode) {
        viewModelScope.launch { repository.setBoostMode(mode) }
    }

    fun runBoost(mode: BoostMode = userSettings.value.boostMode) {
        if (_boostProgress.value != null) return
        viewModelScope.launch {
            repository.setBoostMode(mode)
            val result = boostEngine.runBoost(
                mode = mode,
                performCleanup = userSettings.value.autoCleanupOnBoost
            ) { progress, msg ->
                _boostProgress.value = progress to msg
            }
            _lastBoostResult.value = result
            _boostProgress.value = null
        }
    }

    fun applyGraphicsSettings(
        driver: GraphicsDriverOption,
        profile: String,
        textureMode: TextureOptMode,
        targetPackage: String? = null
    ) {
        viewModelScope.launch {
            repository.setGraphicsSettings(driver, profile, textureMode)
            val (_, message) = graphicsEngine.applyDriverSettingIfPermitted(driver, targetPackage)
            _graphicsStatusMessage.value = message
        }
    }

    fun resetGraphicsSettings() {
        viewModelScope.launch {
            repository.setGraphicsSettings(
                GraphicsDriverOption.DEFAULT,
                "Balanced",
                TextureOptMode.SHARP_NATIVE
            )
            _graphicsStatusMessage.value = "Graphics profile reset to Default Driver & Sharp Native."
        }
    }

    fun saveTouchProfile(
        id: Long = 0L,
        name: String,
        gamePackage: String?,
        sensX: Float,
        sensY: Float,
        touchArea: Float,
        zoneMode: TouchZoneMode
    ) {
        viewModelScope.launch {
            repository.saveTouchProfile(
                TouchProfileEntity(
                    id = id,
                    name = name.ifBlank { "Custom Profile" },
                    gamePackage = gamePackage,
                    sensitivityX = sensX,
                    sensitivityY = sensY,
                    touchAreaScale = touchArea,
                    touchZoneMode = zoneMode.name,
                    isPreset = false,
                    updatedAt = System.currentTimeMillis()
                )
            )
            if (gamePackage != null) {
                val existing = repository.getGameProfile(gamePackage)
                if (existing != null) {
                    repository.upsertGameProfile(
                        existing.copy(sensitivityX = sensX, sensitivityY = sensY)
                    )
                }
            }
            _statusBanner.value = "Touch profile \"$name\" saved locally"
        }
    }

    fun deleteTouchProfile(id: Long) {
        viewModelScope.launch { repository.deleteTouchProfile(id) }
    }

    fun saveMacroConfig(config: MacroConfigEntity) {
        viewModelScope.launch {
            repository.saveMacroConfig(config.copy(updatedAt = System.currentTimeMillis()))
            _statusBanner.value = "Macro \"${config.name}\" saved"
        }
    }

    fun deleteMacroConfig(id: Long) {
        viewModelScope.launch { repository.deleteMacroConfig(id) }
    }

    fun compileGame(game: InstalledGameItem, mode: CompileMode) {
        if (_compileProgressPackage.value != null) return
        viewModelScope.launch {
            _compileProgressPackage.value = game.packageName
            val result = compilerEngine.compilePackage(game, mode)
            _lastCompilationResult.value = result
            _compileProgressPackage.value = null
        }
    }

    fun connectShizuku() = shizukuEngine.connectOrRequestPermission()
    fun reconnectShizuku() = shizukuEngine.reconnect()
    fun disconnectShizuku() = shizukuEngine.disconnect()
    fun openShizukuApp() = shizukuEngine.openShizukuApp()
    fun refreshShizukuStatus() = shizukuEngine.refreshStatus()

    fun startSession(gamePackage: String? = null, gameName: String = "Manual Gaming Session") {
        val s = userSettings.value
        sessionManager.startSession(
            gamePackage = gamePackage,
            gameName = gameName,
            mode = s.boostMode,
            detectActiveApp = s.activeGameDetectionEnabled,
            notificationsEnabled = s.notificationsEnabled
        )
    }

    fun stopSession(restoreTemp: Boolean = true) {
        viewModelScope.launch {
            sessionManager.stopSession(restoreTemp)
            _statusBanner.value = "Gaming session saved & temporary settings restored"
        }
    }

    fun restoreTemporarySettings() {
        _statusBanner.value = sessionManager.restoreTemporarySettings()
    }

    fun startFloatingPanel(): Boolean {
        val ctx = getApplication<Application>()
        if (!PermissionHelper.canDrawOverlays(ctx)) {
            PermissionHelper.openOverlaySettings(ctx)
            return false
        }
        val settings = userSettings.value
        val intent = Intent(ctx, FloatingPanelService::class.java).apply {
            putExtra(FloatingPanelService.EXTRA_TRANSPARENT, settings.floatingTransparentMode)
            putExtra(FloatingPanelService.EXTRA_POSITION_PRESET, settings.floatingPositionPreset.name)
        }
        ctx.startForegroundService(intent)
        return true
    }

    fun stopFloatingPanel() {
        val ctx = getApplication<Application>()
        ctx.stopService(Intent(ctx, FloatingPanelService::class.java))
    }

    fun updateFloatingTransparency(transparent: Boolean) {
        viewModelScope.launch {
            repository.setFloatingTransparentMode(transparent)
            if (isFloatingPanelRunning.value) {
                val ctx = getApplication<Application>()
                val intent = Intent(ctx, FloatingPanelService::class.java).apply {
                    putExtra(FloatingPanelService.EXTRA_TRANSPARENT, transparent)
                }
                ctx.startService(intent)
            }
        }
    }

    fun updateFloatingPreset(preset: FloatingPositionPreset) {
        viewModelScope.launch {
            repository.setFloatingPositionPreset(preset)
            if (isFloatingPanelRunning.value) {
                val ctx = getApplication<Application>()
                val intent = Intent(ctx, FloatingPanelService::class.java).apply {
                    putExtra(FloatingPanelService.EXTRA_POSITION_PRESET, preset.name)
                }
                ctx.startService(intent)
            }
        }
    }

    fun runPingTest() {
        viewModelScope.launch {
            val ms = telemetryMonitor.measureNetworkLatencyMs()
            if (ms != null) {
                _networkPingHistory.value = (_networkPingHistory.value + ms).takeLast(12)
            } else {
                _statusBanner.value = "Network offline or unreachable"
            }
        }
    }

    fun setGamerTag(tag: String) = viewModelScope.launch { repository.setGamerTag(tag) }
    fun setThemeAccent(accent: ThemeAccent) = viewModelScope.launch { repository.setThemeAccent(accent) }
    fun setAnimationsEnabled(enabled: Boolean) = viewModelScope.launch { repository.setAnimationsEnabled(enabled) }
    fun setNotificationsEnabled(enabled: Boolean) = viewModelScope.launch { repository.setNotificationsEnabled(enabled) }
    fun setAutoCleanupOnBoost(enabled: Boolean) = viewModelScope.launch { repository.setAutoCleanupOnBoost(enabled) }
    fun setActiveGameDetection(enabled: Boolean) = viewModelScope.launch { repository.setActiveGameDetection(enabled) }

    fun resetAllSettings() {
        viewModelScope.launch {
            repository.resetAllSettings()
            repository.seedInitialPresetsIfEmpty()
            rescanInstalledApps()
            _lastBoostResult.value = null
            _lastCompilationResult.value = null
            _graphicsStatusMessage.value = null
            _statusBanner.value = "All XARAGameSpace settings & history reset"
        }
    }
}
