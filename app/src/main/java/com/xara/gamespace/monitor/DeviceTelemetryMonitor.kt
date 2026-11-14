package com.xara.gamespace.monitor

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.display.DisplayManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.os.Build
import android.os.PowerManager
import android.util.DisplayMetrics
import android.view.Choreographer
import android.view.Display
import android.view.WindowManager
import com.xara.gamespace.domain.model.DeviceTelemetry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.io.File
import java.net.InetSocketAddress
import java.net.Socket
import kotlin.math.roundToInt

class DeviceTelemetryMonitor(private val context: Context) {

    private val activityManager =
        context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    private val powerManager =
        context.getSystemService(Context.POWER_SERVICE) as PowerManager
    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val displayManager =
        context.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
    private val windowManager =
        context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

    private var prevIdleTime = 0L
    private var prevTotalTime = 0L

    @Volatile
    private var lastFrameNanos = 0L
    @Volatile
    private var currentHudFps: Float? = null
    @Volatile
    private var currentFrameTimeMs: Float? = null

    private var frameCallbackRegistered = false
    private val frameCallback = object : Choreographer.FrameCallback {
        override fun doFrame(frameTimeNanos: Long) {
            if (lastFrameNanos > 0L) {
                val deltaNs = frameTimeNanos - lastFrameNanos
                if (deltaNs in 1_000_000L..500_000_000L) {
                    val frameMs = deltaNs / 1_000_000f
                    val fps = 1_000_000_000f / deltaNs
                    currentFrameTimeMs = frameMs
                    currentHudFps = fps.coerceAtMost(240f)
                }
            }
            lastFrameNanos = frameTimeNanos
            if (frameCallbackRegistered) {
                Choreographer.getInstance().postFrameCallback(this)
            }
        }
    }

    private val _latestTelemetry = MutableStateFlow(collectSnapshotSync(cachedPingMs = null))
    val latestTelemetry: StateFlow<DeviceTelemetry> = _latestTelemetry.asStateFlow()

    fun startFpsTracking() {
        if (!frameCallbackRegistered) {
            frameCallbackRegistered = true
            lastFrameNanos = 0L
            Choreographer.getInstance().postFrameCallback(frameCallback)
        }
    }

    fun stopFpsTracking() {
        frameCallbackRegistered = false
        Choreographer.getInstance().removeFrameCallback(frameCallback)
    }

    fun observeTelemetry(intervalMs: Long = 1500L): Flow<DeviceTelemetry> = flow {
        var tick = 0
        var lastPingMs: Int? = null
        while (currentCoroutineContext().isActive) {
            if (tick % 3 == 0) {
                lastPingMs = measureNetworkLatencyMs()
            }
            val snapshot = collectSnapshotSync(lastPingMs)
            _latestTelemetry.value = snapshot
            emit(snapshot)
            tick++
            delay(intervalMs)
        }
    }.flowOn(Dispatchers.IO)

    fun collectSnapshotSync(cachedPingMs: Int? = _latestTelemetry.value.networkLatencyMs): DeviceTelemetry {
        val memInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memInfo)
        val totalRamMb = (memInfo.totalMem / (1024L * 1024L)).coerceAtLeast(1L)
        val availRamMb = memInfo.availMem / (1024L * 1024L)
        val usedRamMb = (totalRamMb - availRamMb).coerceAtLeast(0L)
        val ramPercent = ((usedRamMb.toDouble() / totalRamMb.toDouble()) * 100.0)
            .roundToInt()
            .coerceIn(0, 100)

        val coreCount = Runtime.getRuntime().availableProcessors().coerceAtLeast(1)
        val coreFreqs = readCpuCoreFrequencies(coreCount)
        val coreMaxFreqs = readCpuCoreMaxFrequencies(coreCount)
        val avgCurFreq = coreFreqs.filterNotNull().takeIf { it.isNotEmpty() }?.average()?.roundToInt()
        val avgMaxFreq = coreMaxFreqs.filterNotNull().takeIf { it.isNotEmpty() }?.average()?.roundToInt()
        val cpuUsage = readCpuUsageFromProcStat() ?: computeFreqLoadPercent(coreFreqs, coreMaxFreqs)

        val batteryIntent = context.registerReceiver(
            null,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        )
        val level = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
        val batteryPct = if (level >= 0 && scale > 0) ((level * 100f) / scale).roundToInt() else 0
        val tempTenths = batteryIntent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0
        val tempC = tempTenths / 10f
        val voltageMv = batteryIntent?.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0) ?: 0
        val healthCode = batteryIntent?.getIntExtra(BatteryManager.EXTRA_HEALTH, BatteryManager.BATTERY_HEALTH_UNKNOWN)
            ?: BatteryManager.BATTERY_HEALTH_UNKNOWN
        val statusCode = batteryIntent?.getIntExtra(BatteryManager.EXTRA_STATUS, BatteryManager.BATTERY_STATUS_UNKNOWN)
            ?: BatteryManager.BATTERY_STATUS_UNKNOWN
        val pluggedCode = batteryIntent?.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) ?: 0

        val batteryHealth = when (healthCode) {
            BatteryManager.BATTERY_HEALTH_GOOD -> "Good"
            BatteryManager.BATTERY_HEALTH_OVERHEAT -> "Overheat"
            BatteryManager.BATTERY_HEALTH_DEAD -> "Dead"
            BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "Over Voltage"
            BatteryManager.BATTERY_HEALTH_COLD -> "Cold"
            else -> "Unknown"
        }

        val chargingStatus = when (statusCode) {
            BatteryManager.BATTERY_STATUS_CHARGING -> "Charging"
            BatteryManager.BATTERY_STATUS_DISCHARGING -> "Discharging"
            BatteryManager.BATTERY_STATUS_FULL -> "Full"
            BatteryManager.BATTERY_STATUS_NOT_CHARGING -> "Not Charging"
            else -> "Unknown"
        }

        val pluggedSource = when (pluggedCode) {
            BatteryManager.BATTERY_PLUGGED_AC -> "AC Adapter"
            BatteryManager.BATTERY_PLUGGED_USB -> "USB"
            BatteryManager.BATTERY_PLUGGED_WIRELESS -> "Wireless"
            else -> "Battery"
        }

        val display = displayManager.getDisplay(Display.DEFAULT_DISPLAY)
        val refreshRate = display?.refreshRate ?: 60f
        val supportedRates = if (display != null) {
            display.supportedModes.map { it.refreshRate }.distinct().sorted()
        } else {
            listOf(60f)
        }

        val (widthPx, heightPx, dpi) = readScreenMetrics()
        val netType = readNetworkType()
        val powerSave = powerManager.isPowerSaveMode

        val (score, breakdown) = calculatePerformanceScore(
            ramAvailablePercent = 100 - ramPercent,
            tempC = tempC,
            refreshRate = refreshRate,
            batteryPct = batteryPct,
            isPowerSave = powerSave,
            isLowMem = memInfo.lowMemory
        )

        return DeviceTelemetry(
            totalRamMb = totalRamMb,
            usedRamMb = usedRamMb,
            availableRamMb = availRamMb,
            ramUsagePercent = ramPercent,
            isLowMemory = memInfo.lowMemory,
            cpuUsagePercent = cpuUsage,
            cpuCores = coreCount,
            cpuCurrentFreqMhz = avgCurFreq,
            cpuMaxFreqMhz = avgMaxFreq,
            cpuCoreFreqsMhz = coreFreqs,
            cpuCoreMaxFreqsMhz = coreMaxFreqs,
            cpuArchitecture = Build.SUPPORTED_ABIS.firstOrNull() ?: System.getProperty("os.arch") ?: "ARM",
            batteryPercent = batteryPct,
            batteryTempCelsius = tempC,
            batteryVoltageMv = voltageMv,
            batteryHealth = batteryHealth,
            chargingStatus = chargingStatus,
            pluggedSource = pluggedSource,
            isPowerSaveMode = powerSave,
            refreshRateHz = refreshRate,
            supportedRefreshRates = supportedRates,
            hudFps = currentHudFps,
            hudFrameTimeMs = currentFrameTimeMs,
            networkLatencyMs = cachedPingMs,
            networkType = netType,
            deviceModel = Build.MODEL,
            manufacturer = Build.MANUFACTURER.replaceFirstChar { it.uppercase() },
            androidVersion = Build.VERSION.RELEASE,
            apiLevel = Build.VERSION.SDK_INT,
            socHardware = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                "${Build.SOC_MANUFACTURER} ${Build.SOC_MODEL}".trim().ifBlank { Build.HARDWARE }
            } else {
                Build.HARDWARE
            },
            boardName = Build.BOARD,
            securityPatch = Build.VERSION.SECURITY_PATCH,
            screenResolution = "${widthPx}x${heightPx}",
            screenDpi = dpi,
            performanceScore = score,
            performanceScoreBreakdown = breakdown
        )
    }

    suspend fun measureNetworkLatencyMs(): Int? = withContext(Dispatchers.IO) {
        if (readNetworkType() == "Offline") return@withContext null
        val targets = listOf("1.1.1.1" to 443, "8.8.8.8" to 443)
        for ((host, port) in targets) {
            val result = runCatching {
                val startNs = System.nanoTime()
                Socket().use { socket ->
                    socket.connect(InetSocketAddress(host, port), 1800)
                }
                ((System.nanoTime() - startNs) / 1_000_000L).toInt().coerceAtLeast(1)
            }.getOrNull()
            if (result != null) return@withContext result
        }
        null
    }

    private fun readCpuUsageFromProcStat(): Float? {
        return runCatching {
            val line = File("/proc/stat").useLines { lines ->
                lines.firstOrNull { it.startsWith("cpu ") }
            } ?: return@runCatching null
            val parts = line.trim().split(Regex("\\s+")).drop(1).mapNotNull { it.toLongOrNull() }
            if (parts.size < 4) return@runCatching null
            val idle = parts[3] + (parts.getOrNull(4) ?: 0L)
            val total = parts.sum()
            val diffTotal = total - prevTotalTime
            val diffIdle = idle - prevIdleTime
            prevTotalTime = total
            prevIdleTime = idle
            if (diffTotal <= 0L) return@runCatching null
            val usage = ((diffTotal - diffIdle).toFloat() / diffTotal.toFloat()) * 100f
            usage.coerceIn(0f, 100f)
        }.getOrNull()
    }

    private fun readCpuCoreFrequencies(cores: Int): List<Int?> {
        return (0 until cores).map { idx ->
            runCatching {
                val raw = File("/sys/devices/system/cpu/cpu$idx/cpufreq/scaling_cur_freq")
                    .takeIf { it.canRead() }
                    ?.readText()
                    ?.trim()
                    ?.toLongOrNull()
                raw?.div(1000L)?.toInt()
            }.getOrNull()
        }
    }

    private fun readCpuCoreMaxFrequencies(cores: Int): List<Int?> {
        return (0 until cores).map { idx ->
            runCatching {
                val raw = File("/sys/devices/system/cpu/cpu$idx/cpufreq/cpuinfo_max_freq")
                    .takeIf { it.canRead() }
                    ?.readText()
                    ?.trim()
                    ?.toLongOrNull()
                raw?.div(1000L)?.toInt()
            }.getOrNull()
        }
    }

    private fun computeFreqLoadPercent(cur: List<Int?>, max: List<Int?>): Float? {
        val ratios = cur.zip(max).mapNotNull { (c, m) ->
            if (c != null && m != null && m > 0) (c.toFloat() / m.toFloat()) * 100f else null
        }
        return if (ratios.isNotEmpty()) ratios.average().toFloat().coerceIn(0f, 100f) else null
    }

    private fun readNetworkType(): String {
        val network = connectivityManager.activeNetwork ?: return "Offline"
        val caps = connectivityManager.getNetworkCapabilities(network) ?: return "Offline"
        return when {
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "Wi-Fi"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Ethernet"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Cellular"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN) -> "VPN"
            else -> "Connected"
        }
    }

    private fun readScreenMetrics(): Triple<Int, Int, Int> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val bounds = windowManager.currentWindowMetrics.bounds
            val dpi = context.resources.displayMetrics.densityDpi
            Triple(bounds.width(), bounds.height(), dpi)
        } else {
            val metrics = DisplayMetrics()
            @Suppress("DEPRECATION")
            windowManager.defaultDisplay.getRealMetrics(metrics)
            Triple(metrics.widthPixels, metrics.heightPixels, metrics.densityDpi)
        }
    }

    private fun calculatePerformanceScore(
        ramAvailablePercent: Int,
        tempC: Float,
        refreshRate: Float,
        batteryPct: Int,
        isPowerSave: Boolean,
        isLowMem: Boolean
    ): Pair<Int, String> {
        val ramScore = ((ramAvailablePercent.coerceIn(10, 75) - 10) / 65f * 35f).roundToInt()
        val thermalScore = when {
            tempC <= 0f -> 20
            tempC <= 34f -> 25
            tempC <= 38f -> 20
            tempC <= 42f -> 14
            else -> 7
        }
        val refreshScore = when {
            refreshRate >= 120f -> 20
            refreshRate >= 90f -> 16
            else -> 12
        }
        val powerScore = when {
            isPowerSave -> 8
            batteryPct >= 40 -> 20
            batteryPct >= 20 -> 14
            else -> 8
        }
        val penalty = if (isLowMem) 10 else 0
        val total = (ramScore + thermalScore + refreshScore + powerScore - penalty).coerceIn(15, 100)
        val breakdown = "RAM $ramScore/35 • Thermal $thermalScore/25 • Display $refreshScore/20 • Power $powerScore/20"
        return total to breakdown
    }
}
