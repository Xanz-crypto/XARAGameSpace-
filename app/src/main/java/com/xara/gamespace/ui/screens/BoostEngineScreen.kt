package com.xara.gamespace.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatterySaver
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xara.gamespace.domain.model.BoostMode
import com.xara.gamespace.ui.XaraMainViewModel
import com.xara.gamespace.ui.components.XaraGlassCard
import com.xara.gamespace.ui.components.XaraSectionHeader
import com.xara.gamespace.ui.theme.LocalXaraColors
import com.xara.gamespace.ui.theme.XaraObsidian
import com.xara.gamespace.ui.theme.XaraTextPrimary
import com.xara.gamespace.ui.theme.XaraTextSecondary
import com.xara.gamespace.utils.FormatUtils

@Composable
fun BoostEngineScreen(viewModel: XaraMainViewModel) {
    val settings by viewModel.userSettings.collectAsStateWithLifecycle()
    val telemetry by viewModel.telemetry.collectAsStateWithLifecycle()
    val boostProgress by viewModel.boostProgress.collectAsStateWithLifecycle()
    val lastResult by viewModel.lastBoostResult.collectAsStateWithLifecycle()
    val history by viewModel.boostHistory.collectAsStateWithLifecycle()
    val xaraColors = LocalXaraColors.current

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            XaraSectionHeader(
                title = "XARA Boost Engine",
                subtitle = "Honest RAM inspection, safe background cleanup & telemetry"
            )
        }

        // Mode Cards
        items(BoostMode.entries) { mode ->
            val selected = settings.boostMode == mode
            val icon = when (mode) {
                BoostMode.PERFORMANCE -> Icons.Filled.Speed
                BoostMode.BALANCED -> Icons.Filled.Tune
                BoostMode.BATTERY_SAVER -> Icons.Filled.BatterySaver
            }
            XaraGlassCard(
                accentBorder = selected,
                onClick = { viewModel.setBoostMode(mode) }
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = mode.displayName,
                        tint = if (selected) xaraColors.accentPrimary else XaraTextSecondary
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = mode.displayName,
                            style = MaterialTheme.typography.titleMedium,
                            color = if (selected) xaraColors.accentPrimary else XaraTextPrimary
                        )
                        Text(
                            text = mode.subtitle,
                            style = MaterialTheme.typography.bodyMedium,
                            color = XaraTextSecondary
                        )
                    }
                    if (selected) {
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = "Selected",
                            tint = xaraColors.accentPrimary
                        )
                    }
                }
            }
        }

        // Safe Auto-Cleanup & One-Tap Boost
        item {
            XaraGlassCard(accentBorder = true) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Safe Automatic Cleanup", style = MaterialTheme.typography.titleMedium, color = XaraTextPrimary)
                        Text(
                            "Reclaim eligible background app caches via Android ActivityManager",
                            style = MaterialTheme.typography.labelSmall,
                            color = XaraTextSecondary
                        )
                    }
                    Switch(
                        checked = settings.autoCleanupOnBoost,
                        onCheckedChange = { viewModel.setAutoCleanupOnBoost(it) },
                        colors = SwitchDefaults.colors(checkedThumbColor = xaraColors.accentPrimary)
                    )
                }

                Spacer(Modifier.height(14.dp))

                Button(
                    onClick = { viewModel.runBoost(settings.boostMode) },
                    enabled = boostProgress == null,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = xaraColors.accentPrimary,
                        contentColor = XaraObsidian
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("one_tap_boost_button")
                ) {
                    Icon(Icons.Filled.Bolt, contentDescription = "One Tap Boost")
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (boostProgress != null) "OPTIMIZING..." else "ONE TAP BOOST (${settings.boostMode.shortLabel})",
                        style = MaterialTheme.typography.labelLarge
                    )
                }

                AnimatedVisibility(visible = boostProgress != null) {
                    Column(modifier = Modifier.padding(top = 12.dp)) {
                        LinearProgressIndicator(
                            progress = { boostProgress?.first ?: 0f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(6.dp)),
                            color = xaraColors.accentPrimary,
                            trackColor = xaraColors.glassBorder
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = boostProgress?.second.orEmpty(),
                            style = MaterialTheme.typography.labelMedium,
                            color = xaraColors.accentPrimary
                        )
                    }
                }
            }
        }

        // Before / After RAM Result Card
        lastResult?.let { res ->
            item {
                XaraGlassCard(accentBorder = true) {
                    XaraSectionHeader(
                        title = "Boost Verification Result",
                        subtitle = "${res.mode.displayName} • Completed in ${res.durationMs} ms"
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("BEFORE RAM", style = MaterialTheme.typography.labelSmall, color = XaraTextSecondary)
                            Text("${res.beforeUsedRamMb} MB", style = MaterialTheme.typography.titleLarge, color = XaraTextPrimary)
                        }
                        Column {
                            Text("AFTER RAM", style = MaterialTheme.typography.labelSmall, color = XaraTextSecondary)
                            Text("${res.afterUsedRamMb} MB", style = MaterialTheme.typography.titleLarge, color = xaraColors.accentPrimary)
                        }
                        Column {
                            Text("FREED", style = MaterialTheme.typography.labelSmall, color = XaraTextSecondary)
                            Text("${res.freedRamMb} MB", style = MaterialTheme.typography.titleLarge, color = xaraColors.thermalCool)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Inspected ${res.runningProcessesInspected} active processes • Cleared ${res.backgroundProcessesCleared} background entries",
                        style = MaterialTheme.typography.labelSmall,
                        color = XaraTextSecondary
                    )
                    Spacer(Modifier.height(8.dp))
                    res.recommendations.forEach { rec ->
                        Text(
                            text = "• $rec",
                            style = MaterialTheme.typography.bodyMedium,
                            color = XaraTextPrimary,
                            modifier = Modifier.padding(top = 3.dp)
                        )
                    }
                }
            }
        }

        // Live Memory Baseline
        item {
            XaraGlassCard {
                Text(
                    text = "Current RAM: ${telemetry.usedRamMb} MB / ${telemetry.totalRamMb} MB (${telemetry.ramUsagePercent}%) • Available: ${telemetry.availableRamMb} MB",
                    style = MaterialTheme.typography.labelMedium,
                    color = xaraColors.accentPrimary
                )
            }
        }

        // History
        item {
            XaraSectionHeader(title = "Boost History (${history.size})")
        }

        if (history.isEmpty()) {
            item {
                XaraGlassCard {
                    Text(
                        text = "Run One Tap Boost to log real before/after memory snapshots.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = XaraTextSecondary
                    )
                }
            }
        } else {
            items(history, key = { it.id }) { h ->
                XaraGlassCard(cornerRadius = 14.dp, contentPadding = 12.dp) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("${h.mode} • ${h.beforeUsedRamMb} → ${h.afterUsedRamMb} MB", style = MaterialTheme.typography.titleMedium, color = XaraTextPrimary)
                            Text(
                                text = "${FormatUtils.formatTimestamp(h.timestamp)} • Cleared ${h.processesCleared} bg",
                                style = MaterialTheme.typography.labelSmall,
                                color = XaraTextSecondary
                            )
                        }
                        Text(
                            text = "+${h.freedRamMb} MB",
                            style = MaterialTheme.typography.labelLarge,
                            color = xaraColors.thermalCool
                        )
                    }
                }
            }
        }
    }
}
