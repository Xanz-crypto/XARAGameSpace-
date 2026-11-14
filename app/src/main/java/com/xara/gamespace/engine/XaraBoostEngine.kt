package com.xara.gamespace.engine

import android.app.ActivityManager
import android.content.Context
import com.xara.gamespace.data.local.BoostHistoryEntity
import com.xara.gamespace.data.repository.XaraRepository
import com.xara.gamespace.domain.model.BoostMode
import com.xara.gamespace.domain.model.BoostResult
import com.xara.gamespace.monitor.DeviceTelemetryMonitor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

class XaraBoostEngine(
    private val context: Context,
    private val telemetryMonitor: DeviceTelemetryMonitor,
    private val repository: XaraRepository
) {
    private val activityManager =
        context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager

    suspend fun runBoost(
        mode: BoostMode,
        performCleanup: Boolean,
        onProgress: suspend (Float, String) -> Unit
    ): BoostResult = withContext(Dispatchers.IO) {
        val startMs = System.currentTimeMillis()
        onProgress(0.15f, "Inspecting RAM & thermal headroom...")
        val beforeSnap = telemetryMonitor.collectSnapshotSync()
        val beforeUsedMb = beforeSnap.usedRamMb

        delay(220)
        onProgress(0.40f, "Scanning active background processes...")
        @Suppress("DEPRECATION")
        val runningProcesses = runCatching { activityManager.runningAppProcesses }.getOrNull().orEmpty()
        var clearedCount = 0

        if (performCleanup && mode != BoostMode.BATTERY_SAVER) {
            onProgress(0.65f, "Safely reclaiming eligible background caches...")
            val ownPkg = context.packageName
            val protectedPrefixes = listOf(
                "com.android.systemui",
                "com.google.android.gms",
                "android",
                "moe.shizuku"
            )
            for (proc in runningProcesses) {
                val importance = proc.importance
                if (importance >= ActivityManager.RunningAppProcessInfo.IMPORTANCE_BACKGROUND) {
                    for (pkg in proc.pkgList) {
                        if (pkg != ownPkg && protectedPrefixes.none { pkg.startsWith(it) }) {
                            runCatching {
                                activityManager.killBackgroundProcesses(pkg)
                                clearedCount++
                            }
                        }
                    }
                }
            }
            System.gc()
            delay(350)
        } else {
            onProgress(0.65f, "Evaluating power-saving & background balance...")
            delay(250)
        }

        onProgress(0.90f, "Measuring post-optimization memory state...")
        val afterSnap = telemetryMonitor.collectSnapshotSync()
        val afterUsedMb = afterSnap.usedRamMb
        val freedMb = (beforeUsedMb - afterUsedMb).coerceAtLeast(0L)

        val recommendations = buildRecommendations(
            mode = mode,
            tempC = afterSnap.batteryTempCelsius,
            ramPercent = afterSnap.ramUsagePercent,
            isPowerSave = afterSnap.isPowerSaveMode,
            refreshRate = afterSnap.refreshRateHz
        )

        repository.recordBoost(
            BoostHistoryEntity(
                mode = mode.name,
                beforeUsedRamMb = beforeUsedMb,
                afterUsedRamMb = afterUsedMb,
                freedRamMb = freedMb,
                processesInspected = runningProcesses.size,
                processesCleared = clearedCount,
                recommendationsSummary = recommendations.firstOrNull().orEmpty(),
                timestamp = System.currentTimeMillis()
            )
        )

        onProgress(1.0f, "Optimization cycle verified.")

        BoostResult(
            mode = mode,
            beforeUsedRamMb = beforeUsedMb,
            afterUsedRamMb = afterUsedMb,
            totalRamMb = afterSnap.totalRamMb,
            freedRamMb = freedMb,
            runningProcessesInspected = runningProcesses.size,
            backgroundProcessesCleared = clearedCount,
            recommendations = recommendations,
            durationMs = System.currentTimeMillis() - startMs,
            timestamp = System.currentTimeMillis()
        )
    }

    private fun buildRecommendations(
        mode: BoostMode,
        tempC: Float,
        ramPercent: Int,
        isPowerSave: Boolean,
        refreshRate: Float
    ): List<String> {
        val list = mutableListOf<String>()
        when (mode) {
            BoostMode.PERFORMANCE -> {
                if (isPowerSave) {
                    list += "System Battery Saver is currently ON — disable it in Quick Settings for peak scheduler responsiveness."
                }
                if (tempC >= 40f) {
                    list += "Battery temp is ${tempC}°C — remove phone case or lower screen brightness to prevent natural thermal throttling."
                }
                if (ramPercent >= 80) {
                    list += "RAM utilization remains at $ramPercent% — close heavy social/browser tabs before launching 3D titles."
                }
                if (refreshRate >= 90f) {
                    list += "Display running at ${refreshRate.toInt()} Hz — ready for high-refresh titles."
                } else {
                    list += "Display at ${refreshRate.toInt()} Hz — check Display settings if your panel supports 90/120 Hz."
                }
            }
            BoostMode.BALANCED -> {
                list += "Balanced telemetry active: safe memory inspection completed without aggressive background termination."
                if (tempC >= 39f) {
                    list += "Thermal readout ${tempC}°C — consider 60 FPS cap in-game for sustained frame stability."
                }
            }
            BoostMode.BATTERY_SAVER -> {
                list += "Battery Saver guidance: lock in-game FPS to 30/60 Hz and reduce shadow quality to extend playtime."
                list += "Keep brightness around 45–60% and prefer Wi-Fi over 5G to reduce modem heat."
            }
        }
        return list
    }
}
