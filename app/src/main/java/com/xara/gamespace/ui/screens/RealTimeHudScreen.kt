package com.xara.gamespace.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DisplaySettings
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.NetworkPing
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xara.gamespace.ui.XaraMainViewModel
import com.xara.gamespace.ui.components.TelemetryMiniTile
import com.xara.gamespace.ui.components.XaraGlassCard
import com.xara.gamespace.ui.components.XaraTopBar
import com.xara.gamespace.ui.theme.LocalXaraColors
import com.xara.gamespace.ui.theme.XaraTextSecondary
import com.xara.gamespace.utils.FormatUtils
import java.util.Locale

@Composable
fun RealTimeHudScreen(viewModel: XaraMainViewModel, onBack: () -> Unit) {
    val telemetry by viewModel.telemetry.collectAsStateWithLifecycle()
    val xaraColors = LocalXaraColors.current

    DisposableEffect(Unit) {
        viewModel.setFpsTrackingActive(true)
        onDispose { viewModel.setFpsTrackingActive(false) }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        XaraTopBar(
            title = "Real-Time Performance HUD",
            subtitle = "Choreographer frame timing & live hardware telemetry",
            onBack = onBack
        )
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    TelemetryMiniTile(
                        title = "HUD FPS / FRAME TIME",
                        value = telemetry.hudFps?.let { "${it.toInt()} FPS" } ?: "N/A",
                        subtitle = telemetry.hudFrameTimeMs?.let { String.format(Locale.US, "%.1f ms frame time", it) } ?: "Calibrating...",
                        icon = Icons.Filled.Speed,
                        accentColor = xaraColors.accentPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    TelemetryMiniTile(
                        title = "REFRESH RATE",
                        value = "${telemetry.refreshRateHz.toInt()} Hz",
                        subtitle = "Panel Display Mode",
                        icon = Icons.Filled.DisplaySettings,
                        accentColor = xaraColors.accentSecondary,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    TelemetryMiniTile(
                        title = "CPU USAGE",
                        value = telemetry.cpuUsagePercent?.let { String.format(Locale.US, "%.0f%%", it) } ?: "N/A",
                        subtitle = telemetry.cpuCurrentFreqMhz?.let { "$it MHz avg" } ?: "${telemetry.cpuCores} cores",
                        icon = Icons.Filled.Speed,
                        accentColor = xaraColors.accentSecondary,
                        modifier = Modifier.weight(1f)
                    )
                    TelemetryMiniTile(
                        title = "RAM USAGE",
                        value = "${telemetry.ramUsagePercent}%",
                        subtitle = "${FormatUtils.formatMemoryMb(telemetry.usedRamMb)} used",
                        icon = Icons.Filled.Memory,
                        accentColor = xaraColors.accentPrimary,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    TelemetryMiniTile(
                        title = "BATTERY TEMP",
                        value = String.format(Locale.US, "%.1f°C", telemetry.batteryTempCelsius),
                        subtitle = "${telemetry.batteryPercent}% • ${telemetry.batteryHealth}",
                        icon = Icons.Filled.Thermostat,
                        accentColor = if (telemetry.batteryTempCelsius >= 41f) xaraColors.thermalHot else xaraColors.thermalCool,
                        modifier = Modifier.weight(1f)
                    )
                    TelemetryMiniTile(
                        title = "NETWORK LATENCY",
                        value = telemetry.networkLatencyMs?.let { "$it ms" } ?: "N/A",
                        subtitle = telemetry.networkType,
                        icon = Icons.Filled.NetworkPing,
                        accentColor = xaraColors.accentPrimary,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            item {
                XaraGlassCard {
                    Text("Telemetry Accuracy Notice", style = MaterialTheme.typography.titleMedium, color = xaraColors.accentPrimary)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Every readout above comes directly from Android system APIs, Choreographer callbacks, /proc or cpufreq nodes, and live TCP RTT checks. Any metric blocked by SELinux or offline status displays N/A honestly.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = XaraTextSecondary
                    )
                }
            }
        }
    }
}
