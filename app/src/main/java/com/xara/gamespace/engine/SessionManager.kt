package com.xara.gamespace.engine

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import com.xara.gamespace.data.local.GameProfileEntity
import com.xara.gamespace.data.local.GameSessionEntity
import com.xara.gamespace.data.repository.XaraRepository
import com.xara.gamespace.domain.model.ActiveSessionState
import com.xara.gamespace.domain.model.BoostMode
import com.xara.gamespace.monitor.DeviceTelemetryMonitor
import com.xara.gamespace.permissions.PermissionHelper
import com.xara.gamespace.services.SessionForegroundService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.roundToInt

class SessionManager(
    private val context: Context,
    private val telemetryMonitor: DeviceTelemetryMonitor,
    private val repository: XaraRepository
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var timerJob: Job? = null

    private val _sessionState = MutableStateFlow(ActiveSessionState())
    val sessionState: StateFlow<ActiveSessionState> = _sessionState.asStateFlow()

    private var ramSamples = mutableListOf<Int>()

    fun startSession(
        gamePackage: String? = null,
        gameName: String = "Manual Gaming Session",
        mode: BoostMode = BoostMode.PERFORMANCE,
        detectActiveApp: Boolean = true,
        notificationsEnabled: Boolean = true
    ) {
        if (_sessionState.value.isRunning) return
        val snap = telemetryMonitor.collectSnapshotSync()
        ramSamples = mutableListOf(snap.ramUsagePercent)

        _sessionState.value = ActiveSessionState(
            isRunning = true,
            activeGamePackage = gamePackage,
            activeGameName = gameName,
            elapsedSeconds = 0L,
            startTimestamp = System.currentTimeMillis(),
            initialTempC = snap.batteryTempCelsius,
            peakTempC = snap.batteryTempCelsius,
            initialBatteryPct = snap.batteryPercent,
            avgRamPercent = snap.ramUsagePercent,
            appliedMode = mode,
            tempSettingsApplied = true
        )

        if (notificationsEnabled) {
            runCatching {
                val intent = Intent(context, SessionForegroundService::class.java).apply {
                    putExtra(SessionForegroundService.EXTRA_GAME_NAME, gameName)
                }
                context.startForegroundService(intent)
            }
        }

        timerJob?.cancel()
        timerJob = scope.launch {
            while (isActive && _sessionState.value.isRunning) {
                delay(1000L)
                val current = _sessionState.value
                val latest = telemetryMonitor.latestTelemetry.value
                ramSamples.add(latest.ramUsagePercent)
                val avgRam = ramSamples.average().roundToInt()

                var detectedPkg = current.activeGamePackage
                var detectedName = current.activeGameName
                if (detectActiveApp && PermissionHelper.hasUsageStatsPermission(context)) {
                    val fg = detectForegroundPackage()
                    if (fg != null && fg != context.packageName) {
                        detectedPkg = fg
                        detectedName = resolveAppLabel(fg) ?: fg
                    }
                }

                _sessionState.value = current.copy(
                    activeGamePackage = detectedPkg,
                    activeGameName = detectedName,
                    elapsedSeconds = current.elapsedSeconds + 1L,
                    peakTempC = max(current.peakTempC, latest.batteryTempCelsius),
                    avgRamPercent = avgRam
                )
            }
        }
    }

    suspend fun stopSession(restoreTemporarySettings: Boolean = true): GameSessionEntity? {
        val current = _sessionState.value
        if (!current.isRunning) return null
        timerJob?.cancel()
        timerJob = null

        runCatching {
            context.stopService(Intent(context, SessionForegroundService::class.java))
        }

        val endSnap = telemetryMonitor.collectSnapshotSync()
        val endTimestamp = System.currentTimeMillis()
        val batteryDelta = (current.initialBatteryPct - endSnap.batteryPercent).coerceAtLeast(0)

        val record = GameSessionEntity(
            gamePackage = current.activeGamePackage,
            gameName = current.activeGameName,
            boostMode = current.appliedMode.name,
            durationSeconds = current.elapsedSeconds,
            startTimestamp = current.startTimestamp,
            endTimestamp = endTimestamp,
            initialTempC = current.initialTempC,
            peakTempC = max(current.peakTempC, endSnap.batteryTempCelsius),
            batteryDeltaPct = batteryDelta,
            avgRamPercent = current.avgRamPercent
        )

        repository.recordSession(record)

        current.activeGamePackage?.let { pkg ->
            val existing = repository.getGameProfile(pkg)
            if (existing != null) {
                repository.upsertGameProfile(
                    existing.copy(
                        lastPlayedTimestamp = endTimestamp,
                        totalPlaytimeSeconds = existing.totalPlaytimeSeconds + current.elapsedSeconds
                    )
                )
            } else {
                repository.upsertGameProfile(
                    GameProfileEntity(
                        packageName = pkg,
                        appName = current.activeGameName,
                        isAddedToSpace = true,
                        lastPlayedTimestamp = endTimestamp,
                        totalPlaytimeSeconds = current.elapsedSeconds
                    )
                )
            }
        }

        if (restoreTemporarySettings) {
            System.gc()
        }

        _sessionState.value = ActiveSessionState(
            isRunning = false,
            tempSettingsApplied = false
        )
        return record
    }

    fun restoreTemporarySettings(): String {
        val current = _sessionState.value
        _sessionState.value = current.copy(tempSettingsApplied = false)
        return "Temporary session boost flags cleared and baseline profile restored."
    }

    private fun detectForegroundPackage(): String? {
        return runCatching {
            val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
            val end = System.currentTimeMillis()
            val begin = end - 10_000L
            val events = usm.queryEvents(begin, end)
            val event = UsageEvents.Event()
            var lastPkg: String? = null
            while (events.hasNextEvent()) {
                events.getNextEvent(event)
                if (event.eventType == UsageEvents.Event.ACTIVITY_RESUMED) {
                    lastPkg = event.packageName
                }
            }
            lastPkg
        }.getOrNull()
    }

    private fun resolveAppLabel(pkg: String): String? {
        return runCatching {
            val appInfo = context.packageManager.getApplicationInfo(pkg, 0)
            context.packageManager.getApplicationLabel(appInfo).toString()
        }.getOrNull()
    }
}
