package com.xara.gamespace.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.DisplaySettings
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.NetworkPing
import androidx.compose.material.icons.filled.PictureInPictureAlt
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.xara.gamespace.domain.model.InstalledGameItem
import com.xara.gamespace.ui.XaraMainViewModel
import com.xara.gamespace.ui.components.AppIconImage
import com.xara.gamespace.ui.components.BoostCoreButton
import com.xara.gamespace.ui.components.PerformanceGauge
import com.xara.gamespace.ui.components.StatusPillBadge
import com.xara.gamespace.ui.components.TelemetryMiniTile
import com.xara.gamespace.ui.components.XaraGlassCard
import com.xara.gamespace.ui.components.XaraLogoCrest
import com.xara.gamespace.ui.components.XaraSectionHeader
import com.xara.gamespace.ui.theme.LocalXaraColors
import com.xara.gamespace.ui.theme.XaraObsidian
import com.xara.gamespace.ui.theme.XaraTextPrimary
import com.xara.gamespace.ui.theme.XaraTextSecondary
import com.xara.gamespace.utils.FormatUtils
import java.util.Locale

@Composable
fun HomeDashboardScreen(
    viewModel: XaraMainViewModel,
    onOpenShizuku: () -> Unit,
    onOpenRam: () -> Unit,
    onOpenCpu: () -> Unit,
    onOpenBattery: () -> Unit,
    onOpenNetwork: () -> Unit,
    onOpenHud: () -> Unit,
    onOpenGraphics: () -> Unit,
    onOpenTouch: () -> Unit,
    onOpenMacro: () -> Unit,
    onOpenFloatingPanel: () -> Unit,
    onOpenCompiler: () -> Unit,
    onOpenSession: () -> Unit,
    onOpenGamesTab: () -> Unit,
    onOpenGameDetail: (String) -> Unit
) {
    val telemetry by viewModel.telemetry.collectAsStateWithLifecycle()
    val settings by viewModel.userSettings.collectAsStateWithLifecycle()
    val shizuku by viewModel.shizukuStatus.collectAsStateWithLifecycle()
    val session by viewModel.sessionState.collectAsStateWithLifecycle()
    val apps by viewModel.installedApps.collectAsStateWithLifecycle()
    val boostProgress by viewModel.boostProgress.collectAsStateWithLifecycle()
    val lastBoost by viewModel.lastBoostResult.collectAsStateWithLifecycle()

    val xaraColors = LocalXaraColors.current
    val spaceGames = apps.filter { it.isAddedToSpace }
    val recentGames = spaceGames.sortedByDescending { it.lastPlayedTimestamp }.take(8)

    val tempColor = when {
        telemetry.batteryTempCelsius >= 42f -> xaraColors.thermalHot
        telemetry.batteryTempCelsius >= 38f -> xaraColors.thermalWarm
        else -> xaraColors.thermalCool
    }

    val shizukuColor = when {
        shizuku.isBinderAlive && shizuku.isPermissionGranted -> xaraColors.thermalCool
        shizuku.isBinderAlive -> xaraColors.thermalWarm
        else -> xaraColors.thermalHot
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Futuristic Cockpit Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    XaraLogoCrest(size = 42.dp)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "WELCOME, ${settings.gamerTag}",
                            style = MaterialTheme.typography.labelLarge,
                            color = xaraColors.accentPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "XARA GAMING COCKPIT",
                            style = MaterialTheme.typography.headlineMedium,
                            color = XaraTextPrimary
                        )
                    }
                }
                StatusPillBadge(
                    text = if (shizuku.isBinderAlive && shizuku.isPermissionGranted) "SHIZUKU ON" else "SHIZUKU",
                    color = shizukuColor,
                    onClick = onOpenShizuku,
                    modifier = Modifier.testTag("home_shizuku_pill")
                )
            }
        }

        // 2. Gaming Status + Device Banner Strip
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatusPillBadge(
                    text = if (session.isRunning) {
                        "ACTIVE: ${FormatUtils.formatDuration(session.elapsedSeconds)}"
                    } else {
                        "GAMING STANDBY"
                    },
                    color = if (session.isRunning) xaraColors.accentPrimary else XaraTextSecondary,
                    onClick = onOpenSession,
                    modifier = Modifier.weight(1f)
                )
                StatusPillBadge(
                    text = "${telemetry.manufacturer} ${telemetry.deviceModel}",
                    color = xaraColors.accentSecondary,
                    onClick = onOpenHud
                )
            }
        }

        // 3. Asymmetric Core Reactor Card (Score Gauge + Big Animated BOOST Button)
        item {
            XaraGlassCard(
                accentBorder = true,
                cornerRadius = 24.dp,
                contentPadding = 0.dp
            ) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    Image(
                        painter = painterResource(id = R.drawable.img_hero_bg),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .matchParentSize()
                            .alpha(0.24f)
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        Color.Transparent,
                                        XaraObsidian.copy(alpha = 0.65f)
                                    )
                                )
                            )
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                PerformanceGauge(
                                    score = telemetry.performanceScore,
                                    size = 134.dp,
                                    label = "PERF SCORE"
                                )
                                Text(
                                    text = "${telemetry.refreshRateHz.toInt()} Hz • ${telemetry.socHardware}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = XaraTextSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            BoostCoreButton(
                                isBoosting = boostProgress != null,
                                progress = boostProgress?.first ?: 0f,
                                animationsEnabled = settings.animationsEnabled,
                                modeLabel = settings.boostMode.shortLabel,
                                onClick = { viewModel.runBoost() }
                            )
                        }

                        AnimatedVisibility(visible = boostProgress != null) {
                            Text(
                                text = boostProgress?.second.orEmpty(),
                                style = MaterialTheme.typography.labelMedium,
                                color = xaraColors.accentPrimary,
                                modifier = Modifier.padding(top = 10.dp)
                            )
                        }

                        AnimatedVisibility(visible = boostProgress == null && lastBoost != null) {
                            lastBoost?.let { res ->
                                Spacer(Modifier.height(10.dp))
                                Text(
                                    text = "Last Boost (${res.mode.shortLabel}): ${res.freedRamMb} MB freed • ${res.backgroundProcessesCleared} bg cleared",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = xaraColors.thermalCool
                                )
                            }
                        }

                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = telemetry.performanceScoreBreakdown,
                            style = MaterialTheme.typography.labelSmall,
                            color = XaraTextSecondary
                        )
                    }
                }
            }
        }

        // 4. Live Hardware Telemetry Grid
        item {
            XaraSectionHeader(
                title = "Live Device Telemetry",
                subtitle = "100% real Android sensor & proc metrics"
            )
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                TelemetryMiniTile(
                    title = "RAM MONITOR",
                    value = "${telemetry.ramUsagePercent}%",
                    subtitle = "${FormatUtils.formatMemoryMb(telemetry.usedRamMb)} / ${FormatUtils.formatMemoryMb(telemetry.totalRamMb)}",
                    icon = Icons.Filled.Memory,
                    accentColor = xaraColors.accentPrimary,
                    modifier = Modifier.weight(1f),
                    onClick = onOpenRam
                )
                TelemetryMiniTile(
                    title = "CPU MONITOR",
                    value = telemetry.cpuUsagePercent?.let { String.format(Locale.US, "%.0f%%", it) } ?: "N/A",
                    subtitle = telemetry.cpuCurrentFreqMhz?.let { "$it MHz • ${telemetry.cpuCores}C" }
                        ?: "${telemetry.cpuCores} Cores • ${telemetry.cpuArchitecture}",
                    icon = Icons.Filled.Speed,
                    accentColor = xaraColors.accentSecondary,
                    modifier = Modifier.weight(1f),
                    onClick = onOpenCpu
                )
            }
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                TelemetryMiniTile(
                    title = "BATTERY & TEMP",
                    value = String.format(Locale.US, "%.1f°C", telemetry.batteryTempCelsius),
                    subtitle = "${telemetry.batteryPercent}% • ${telemetry.chargingStatus}",
                    icon = Icons.Filled.Thermostat,
                    accentColor = tempColor,
                    modifier = Modifier.weight(1f),
                    onClick = onOpenBattery
                )
                TelemetryMiniTile(
                    title = "DISPLAY & PING",
                    value = "${telemetry.refreshRateHz.toInt()} Hz",
                    subtitle = telemetry.networkLatencyMs?.let { "$it ms • ${telemetry.networkType}" }
                        ?: "Ping N/A • ${telemetry.networkType}",
                    icon = Icons.Filled.NetworkPing,
                    accentColor = xaraColors.accentPrimary,
                    modifier = Modifier.weight(1f),
                    onClick = onOpenNetwork
                )
            }
        }

        // 5. Recent & Added Games Carousel
        item {
            XaraSectionHeader(
                title = "Game Space (${spaceGames.size} Games)",
                subtitle = "Tap to launch or configure per-game profile",
                trailing = {
                    Text(
                        text = "VIEW ALL",
                        style = MaterialTheme.typography.labelLarge,
                        color = xaraColors.accentPrimary,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(onClick = onOpenGamesTab)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            )
            Spacer(Modifier.height(10.dp))
            if (recentGames.isEmpty()) {
                XaraGlassCard(onClick = onOpenGamesTab) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.SportsEsports,
                            contentDescription = "Add games",
                            tint = xaraColors.accentPrimary
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = "No games pinned yet. Tap to scan & add installed games to XARAGameSpace.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = XaraTextSecondary
                        )
                    }
                }
            } else {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(recentGames, key = { it.packageName }) { game ->
                        RecentGameCard(
                            game = game,
                            viewModel = viewModel,
                            onLaunch = { viewModel.launchGame(game) },
                            onDetail = { onOpenGameDetail(game.packageName) }
                        )
                    }
                }
            }
        }

        // 6. Quick Access Tools
        item {
            XaraSectionHeader(
                title = "Quick Access Tools",
                subtitle = "Direct launch to specialized XARA engines"
            )
            Spacer(Modifier.height(10.dp))
            val quickTools = listOf(
                Triple("Floating HUD", Icons.Filled.PictureInPictureAlt, onOpenFloatingPanel),
                Triple("Real-Time HUD", Icons.Filled.Speed, onOpenHud),
                Triple("Graphics Engine", Icons.Filled.DisplaySettings, onOpenGraphics),
                Triple("Touch Optimizer", Icons.Filled.TouchApp, onOpenTouch),
                Triple("Macro Editor", Icons.Filled.Tune, onOpenMacro),
                Triple("Shizuku Engine", Icons.Filled.Terminal, onOpenShizuku),
                Triple("Game Compiler", Icons.Filled.Build, onOpenCompiler),
                Triple("Battery Engine", Icons.Filled.BatteryChargingFull, onOpenBattery)
            )
            quickTools.chunked(2).forEach { pair ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    pair.forEach { (label, icon, action) ->
                        QuickToolChip(
                            label = label,
                            icon = icon,
                            onClick = action,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RecentGameCard(
    game: InstalledGameItem,
    viewModel: XaraMainViewModel,
    onLaunch: () -> Unit,
    onDetail: () -> Unit
) {
    val xaraColors = LocalXaraColors.current
    XaraGlassCard(
        modifier = Modifier.width(172.dp),
        cornerRadius = 18.dp,
        contentPadding = 12.dp,
        onClick = onDetail
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AppIconImage(
                bitmap = viewModel.getAppIconBitmap(game.packageName),
                appName = game.appName,
                size = 44.dp
            )
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = game.appName,
                    style = MaterialTheme.typography.titleMedium,
                    color = XaraTextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = game.boostModeOverride?.shortLabel ?: "GLOBAL",
                    style = MaterialTheme.typography.labelSmall,
                    color = xaraColors.accentPrimary
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        Button(
            onClick = onLaunch,
            colors = ButtonDefaults.buttonColors(
                containerColor = xaraColors.accentPrimary.copy(alpha = 0.18f),
                contentColor = xaraColors.accentPrimary
            ),
            shape = RoundedCornerShape(10.dp),
            contentPadding = PaddingValues(vertical = 6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Filled.PlayArrow, contentDescription = "Launch ${game.appName}", modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            Text("PLAY", style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun QuickToolChip(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val xaraColors = LocalXaraColors.current
    XaraGlassCard(
        modifier = modifier,
        cornerRadius = 14.dp,
        contentPadding = 12.dp,
        onClick = onClick
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(xaraColors.accentPrimary.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = xaraColors.accentPrimary,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(Modifier.width(10.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                color = XaraTextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
