package com.xara.gamespace.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.DisplaySettings
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.NetworkPing
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PictureInPictureAlt
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xara.gamespace.ui.XaraMainViewModel
import com.xara.gamespace.ui.components.StatusPillBadge
import com.xara.gamespace.ui.components.XaraGlassCard
import com.xara.gamespace.ui.components.XaraSectionHeader
import com.xara.gamespace.ui.theme.LocalXaraColors
import com.xara.gamespace.ui.theme.XaraTextPrimary
import com.xara.gamespace.ui.theme.XaraTextSecondary

private data class ToolEntry(
    val title: String,
    val subtitle: String,
    val badge: String,
    val icon: ImageVector,
    val onClick: () -> Unit
)

@Composable
fun ToolsCenterScreen(
    viewModel: XaraMainViewModel,
    onOpenRam: () -> Unit,
    onOpenCpu: () -> Unit,
    onOpenNetwork: () -> Unit,
    onOpenGraphics: () -> Unit,
    onOpenTouch: () -> Unit,
    onOpenMacro: () -> Unit,
    onOpenFloatingPanel: () -> Unit,
    onOpenRealTimeHud: () -> Unit,
    onOpenShizuku: () -> Unit,
    onOpenCompiler: () -> Unit,
    onOpenBattery: () -> Unit,
    onOpenSession: () -> Unit,
    onOpenDeviceInfo: () -> Unit
) {
    val telemetry by viewModel.telemetry.collectAsStateWithLifecycle()
    val shizuku by viewModel.shizukuStatus.collectAsStateWithLifecycle()
    val isFloating by viewModel.isFloatingPanelRunning.collectAsStateWithLifecycle()
    val session by viewModel.sessionState.collectAsStateWithLifecycle()
    val xaraColors = LocalXaraColors.current

    val tools = listOf(
        ToolEntry("RAM Monitor", "Live memory usage & background process cleanup", "${telemetry.ramUsagePercent}% Used", Icons.Filled.Memory, onOpenRam),
        ToolEntry("CPU Monitor", "Per-core frequencies, architecture & load", telemetry.cpuUsagePercent?.let { "${it.toInt()}% Load" } ?: "${telemetry.cpuCores} Cores", Icons.Filled.Speed, onOpenCpu),
        ToolEntry("Network Monitor", "Active transport & real TCP RTT latency test", telemetry.networkLatencyMs?.let { "$it ms" } ?: telemetry.networkType, Icons.Filled.NetworkPing, onOpenNetwork),
        ToolEntry("Real-Time Performance HUD", "Full telemetry HUD with Choreographer FPS & Hz", "${telemetry.refreshRateHz.toInt()} Hz", Icons.Filled.Speed, onOpenRealTimeHud),
        ToolEntry("Floating Game Panel", "Draggable overlay HUD for in-game monitoring", if (isFloating) "ACTIVE" else "STANDBY", Icons.Filled.PictureInPictureAlt, onOpenFloatingPanel),
        ToolEntry("Graphics Engine", "Driver selector, Vulkan/ANGLE check & Texture Opt", "Profiles", Icons.Filled.DisplaySettings, onOpenGraphics),
        ToolEntry("Touch & Sensitivity Optimizer", "X/Y sensitivity, touch area, zones & game presets", "Profiles", Icons.Filled.TouchApp, onOpenTouch),
        ToolEntry("Macro Configuration", "Visual trigger/target node editor & timing intervals", "Editor", Icons.Filled.Tune, onOpenMacro),
        ToolEntry("Shizuku Engine", "Wireless Debugging & elevated shell IPC manager", if (shizuku.isPermissionGranted) "READY" else "SETUP", Icons.Filled.Terminal, onOpenShizuku),
        ToolEntry("Game Compilation", "ART package compiler (speed-profile / speed)", "Shizuku", Icons.Filled.Build, onOpenCompiler),
        ToolEntry("Gaming Session", "Playtime timer, foreground detection & summary", if (session.isRunning) "RUNNING" else "IDLE", Icons.Filled.Timer, onOpenSession),
        ToolEntry("Battery Engine", "Thermal readout, voltage, health & power guide", "${telemetry.batteryTempCelsius}°C", Icons.Filled.BatteryChargingFull, onOpenBattery),
        ToolEntry("Device Information", "SoC, display modes, build & security patch", telemetry.deviceModel, Icons.Filled.PhoneAndroid, onOpenDeviceInfo)
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            XaraSectionHeader(
                title = "Tools Center",
                subtitle = "13 dedicated XARA monitoring & optimization modules"
            )
            Spacer(Modifier.height(4.dp))
        }

        items(tools) { tool ->
            XaraGlassCard(
                cornerRadius = 16.dp,
                contentPadding = 14.dp,
                onClick = tool.onClick
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(xaraColors.accentPrimary.copy(alpha = 0.14f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = tool.icon,
                            contentDescription = tool.title,
                            tint = xaraColors.accentPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(tool.title, style = MaterialTheme.typography.titleMedium, color = XaraTextPrimary)
                        Text(tool.subtitle, style = MaterialTheme.typography.labelSmall, color = XaraTextSecondary)
                    }
                    StatusPillBadge(text = tool.badge, color = xaraColors.accentPrimary)
                    Spacer(Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "Open ${tool.title}",
                        tint = XaraTextSecondary
                    )
                }
            }
        }
    }
}
