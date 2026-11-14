package com.xara.gamespace.ui.screens

import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.NetworkPing
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xara.gamespace.domain.model.BoostMode
import com.xara.gamespace.permissions.PermissionHelper
import com.xara.gamespace.ui.XaraMainViewModel
import com.xara.gamespace.ui.components.XaraGlassCard
import com.xara.gamespace.ui.components.XaraSectionHeader
import com.xara.gamespace.ui.components.XaraTopBar
import com.xara.gamespace.ui.theme.LocalXaraColors
import com.xara.gamespace.ui.theme.XaraObsidian
import com.xara.gamespace.ui.theme.XaraTextPrimary
import com.xara.gamespace.ui.theme.XaraTextSecondary
import com.xara.gamespace.utils.FormatUtils
import java.util.Locale

@Composable
fun RamMonitorScreen(viewModel: XaraMainViewModel, onBack: () -> Unit) {
    val telemetry by viewModel.telemetry.collectAsStateWithLifecycle()
    val boostProgress by viewModel.boostProgress.collectAsStateWithLifecycle()
    val xaraColors = LocalXaraColors.current

    Column(modifier = Modifier.fillMaxSize()) {
        XaraTopBar(title = "RAM Monitor", subtitle = "Live Android ActivityManager.MemoryInfo", onBack = onBack)
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                XaraGlassCard(accentBorder = true) {
                    Text("RAM UTILIZATION", style = MaterialTheme.typography.labelLarge, color = xaraColors.accentPrimary)
                    Spacer(Modifier.height(8.dp))
                    Text("${telemetry.ramUsagePercent}%", style = MaterialTheme.typography.displayLarge, color = XaraTextPrimary)
                    Spacer(Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { telemetry.ramUsagePercent / 100f },
                        modifier = Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(8.dp)),
                        color = xaraColors.accentPrimary,
                        trackColor = xaraColors.glassBorder
                    )
                    Spacer(Modifier.height(12.dp))
                    InfoPairRow("Used RAM", FormatUtils.formatMemoryMb(telemetry.usedRamMb))
                    InfoPairRow("Available RAM", FormatUtils.formatMemoryMb(telemetry.availableRamMb))
                    InfoPairRow("Total Physical RAM", FormatUtils.formatMemoryMb(telemetry.totalRamMb))
                    InfoPairRow("Low Memory State", if (telemetry.isLowMemory) "YES (Critical)" else "Normal")
                }
            }
            item {
                Button(
                    onClick = { viewModel.runBoost(BoostMode.PERFORMANCE) },
                    enabled = boostProgress == null,
                    colors = ButtonDefaults.buttonColors(containerColor = xaraColors.accentPrimary, contentColor = XaraObsidian),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                ) {
                    Icon(Icons.Filled.Bolt, contentDescription = "Reclaim RAM")
                    Spacer(Modifier.width(8.dp))
                    Text(if (boostProgress != null) "CLEANING..." else "SAFE MEMORY CLEANUP", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

@Composable
fun CpuMonitorScreen(viewModel: XaraMainViewModel, onBack: () -> Unit) {
    val telemetry by viewModel.telemetry.collectAsStateWithLifecycle()
    val xaraColors = LocalXaraColors.current

    Column(modifier = Modifier.fillMaxSize()) {
        XaraTopBar(title = "CPU Monitor", subtitle = "Cores, frequencies & architecture", onBack = onBack)
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                XaraGlassCard(accentBorder = true) {
                    InfoPairRow("CPU Load", telemetry.cpuUsagePercent?.let { String.format(Locale.US, "%.1f%%", it) } ?: "N/A (Kernel restricted)")
                    InfoPairRow("Processor Cores", "${telemetry.cpuCores} Logical Cores")
                    InfoPairRow("Architecture", telemetry.cpuArchitecture)
                    InfoPairRow("SoC / Hardware", telemetry.socHardware)
                    InfoPairRow("Avg Current Freq", telemetry.cpuCurrentFreqMhz?.let { "$it MHz" } ?: "N/A")
                    InfoPairRow("Avg Max Freq", telemetry.cpuMaxFreqMhz?.let { "$it MHz" } ?: "N/A")
                }
            }
            item {
                XaraSectionHeader(
                    title = "Per-Core Frequency Readout",
                    subtitle = "Reads /sys/devices/system/cpu/cpu*/cpufreq when permitted"
                )
            }
            itemsIndexed(telemetry.cpuCoreFreqsMhz) { index, curFreq ->
                val maxFreq = telemetry.cpuCoreMaxFreqsMhz.getOrNull(index)
                val ratio = if (curFreq != null && maxFreq != null && maxFreq > 0) {
                    (curFreq.toFloat() / maxFreq.toFloat()).coerceIn(0f, 1f)
                } else {
                    0f
                }
                XaraGlassCard(cornerRadius = 14.dp, contentPadding = 12.dp) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("CORE $index", style = MaterialTheme.typography.labelLarge, color = XaraTextPrimary)
                        Text(
                            text = if (curFreq != null && maxFreq != null) "$curFreq / $maxFreq MHz"
                            else if (curFreq != null) "$curFreq MHz"
                            else "N/A (SELinux Protected)",
                            style = MaterialTheme.typography.labelMedium,
                            color = xaraColors.accentPrimary
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { ratio },
                        modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(6.dp)),
                        color = xaraColors.accentPrimary,
                        trackColor = xaraColors.glassBorder
                    )
                }
            }
        }
    }
}

@Composable
fun NetworkMonitorScreen(viewModel: XaraMainViewModel, onBack: () -> Unit) {
    val telemetry by viewModel.telemetry.collectAsStateWithLifecycle()
    val pingHistory by viewModel.networkPingHistory.collectAsStateWithLifecycle()
    val xaraColors = LocalXaraColors.current

    Column(modifier = Modifier.fillMaxSize()) {
        XaraTopBar(title = "Network Monitor", subtitle = "Real transport & TCP RTT latency", onBack = onBack)
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                XaraGlassCard(accentBorder = true) {
                    InfoPairRow("Active Transport", telemetry.networkType)
                    InfoPairRow("Live Latency (RTT)", telemetry.networkLatencyMs?.let { "$it ms" } ?: "N/A")
                    val avg = pingHistory.takeIf { it.isNotEmpty() }?.average()?.toInt()
                    InfoPairRow("Session Avg Ping", avg?.let { "$it ms" } ?: "N/A")
                    Spacer(Modifier.height(10.dp))
                    Button(
                        onClick = { viewModel.runPingTest() },
                        colors = ButtonDefaults.buttonColors(containerColor = xaraColors.accentPrimary, contentColor = XaraObsidian),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.NetworkPing, contentDescription = "Measure Ping")
                        Spacer(Modifier.width(8.dp))
                        Text("RUN REAL LATENCY PROBE", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
            if (pingHistory.isNotEmpty()) {
                item {
                    XaraGlassCard {
                        XaraSectionHeader(title = "Recent Latency Samples (ms)")
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = pingHistory.joinToString(" ms  •  ") + " ms",
                            style = MaterialTheme.typography.labelMedium,
                            color = xaraColors.accentPrimary
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BatteryEngineScreen(viewModel: XaraMainViewModel, onBack: () -> Unit) {
    val telemetry by viewModel.telemetry.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val xaraColors = LocalXaraColors.current
    val ignoringOpt = PermissionHelper.isIgnoringBatteryOptimizations(context)

    Column(modifier = Modifier.fillMaxSize()) {
        XaraTopBar(title = "Battery Engine", subtitle = "Thermal safety & power telemetry", onBack = onBack)
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                XaraGlassCard(accentBorder = true) {
                    InfoPairRow("Battery Level", "${telemetry.batteryPercent}%")
                    InfoPairRow("Battery Temperature", String.format(Locale.US, "%.1f °C", telemetry.batteryTempCelsius))
                    InfoPairRow("Charging Status", "${telemetry.chargingStatus} (${telemetry.pluggedSource})")
                    InfoPairRow("Battery Health", telemetry.batteryHealth)
                    InfoPairRow("Voltage", if (telemetry.batteryVoltageMv > 0) "${telemetry.batteryVoltageMv} mV" else "N/A")
                    InfoPairRow("System Power Saver", if (telemetry.isPowerSaveMode) "Active" else "Off")
                    InfoPairRow("Background Exempt", if (ignoringOpt) "Unrestricted" else "Standard Optimized")
                }
            }
            item {
                XaraGlassCard {
                    XaraSectionHeader(title = "Battery Optimization Shortcut")
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "XARAGameSpace never disables Android thermal throttling or battery hardware protection.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = XaraTextSecondary
                    )
                    Spacer(Modifier.height(10.dp))
                    Button(
                        onClick = { PermissionHelper.openBatteryOptimizationSettings(context) },
                        colors = ButtonDefaults.buttonColors(containerColor = xaraColors.accentPrimary, contentColor = XaraObsidian),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.BatteryChargingFull, contentDescription = "Battery Settings")
                        Spacer(Modifier.width(8.dp))
                        Text("OPEN BATTERY OPTIMIZATION SETTINGS", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
            item {
                XaraGlassCard {
                    XaraSectionHeader(title = "Safe Power Saving Guide")
                    Spacer(Modifier.height(8.dp))
                    Text("• Keep battery temperature below 41°C to prevent natural SoC thermal throttling.", style = MaterialTheme.typography.bodyMedium, color = XaraTextPrimary)
                    Text("• Avoid fast-charging while running heavy 3D games; bypass/slow charging lowers heat.", style = MaterialTheme.typography.bodyMedium, color = XaraTextPrimary)
                    Text("• Capping FPS to 60 Hz in RPG titles cuts GPU power draw substantially.", style = MaterialTheme.typography.bodyMedium, color = XaraTextPrimary)
                }
            }
        }
    }
}

@Composable
fun DeviceInfoScreen(viewModel: XaraMainViewModel, onBack: () -> Unit) {
    val telemetry by viewModel.telemetry.collectAsStateWithLifecycle()
    val gfx = viewModel.graphicsCompatibility

    Column(modifier = Modifier.fillMaxSize()) {
        XaraTopBar(title = "Device Information", subtitle = "Hardware & Android platform details", onBack = onBack)
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                XaraGlassCard(accentBorder = true) {
                    InfoPairRow("Manufacturer", telemetry.manufacturer)
                    InfoPairRow("Device Model", telemetry.deviceModel)
                    InfoPairRow("SoC / Hardware", telemetry.socHardware)
                    InfoPairRow("Board", telemetry.boardName)
                    InfoPairRow("Android Version", "${telemetry.androidVersion} (API ${telemetry.apiLevel})")
                    InfoPairRow("Security Patch", telemetry.securityPatch)
                    InfoPairRow("ABIs", Build.SUPPORTED_ABIS.joinToString(", "))
                }
            }
            item {
                XaraGlassCard {
                    XaraSectionHeader(title = "Display & Graphics Capabilities")
                    Spacer(Modifier.height(8.dp))
                    InfoPairRow("Resolution", telemetry.screenResolution)
                    InfoPairRow("Density", "${telemetry.screenDpi} dpi")
                    InfoPairRow("Active Refresh Rate", "${telemetry.refreshRateHz.toInt()} Hz")
                    InfoPairRow("Supported Modes", telemetry.supportedRefreshRates.joinToString(" / ") { "${it.toInt()}Hz" })
                    InfoPairRow("Vulkan", gfx.vulkanVersion)
                    InfoPairRow("OpenGL ES", gfx.openGlEsVersion)
                }
            }
        }
    }
}

@Composable
fun InfoPairRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = XaraTextSecondary)
        Text(value, style = MaterialTheme.typography.labelMedium, color = XaraTextPrimary)
    }
}
