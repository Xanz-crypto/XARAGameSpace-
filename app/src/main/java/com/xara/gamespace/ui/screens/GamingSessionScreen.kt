package com.xara.gamespace.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xara.gamespace.permissions.PermissionHelper
import com.xara.gamespace.ui.XaraMainViewModel
import com.xara.gamespace.ui.components.StatusPillBadge
import com.xara.gamespace.ui.components.XaraGlassCard
import com.xara.gamespace.ui.components.XaraSectionHeader
import com.xara.gamespace.ui.components.XaraTopBar
import com.xara.gamespace.ui.theme.LocalXaraColors
import com.xara.gamespace.ui.theme.XaraObsidian
import com.xara.gamespace.ui.theme.XaraTextPrimary
import com.xara.gamespace.ui.theme.XaraTextSecondary
import com.xara.gamespace.utils.FormatUtils

@Composable
fun GamingSessionScreen(viewModel: XaraMainViewModel, onBack: () -> Unit) {
    val session by viewModel.sessionState.collectAsStateWithLifecycle()
    val history by viewModel.gameSessions.collectAsStateWithLifecycle()
    val settings by viewModel.userSettings.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val xaraColors = LocalXaraColors.current

    val hasUsageAccess = PermissionHelper.hasUsageStatsPermission(context)
    val totalPlaytimeSec = history.sumOf { it.durationSeconds } + session.elapsedSeconds

    Column(modifier = Modifier.fillMaxSize()) {
        XaraTopBar(title = "Gaming Session", subtitle = "Session timer, playtime & thermal summary", onBack = onBack)
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                XaraGlassCard(accentBorder = true) {
                    StatusPillBadge(
                        text = if (session.isRunning) "SESSION RUNNING" else "SESSION IDLE",
                        color = if (session.isRunning) xaraColors.thermalCool else XaraTextSecondary
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = FormatUtils.formatDuration(session.elapsedSeconds),
                        style = MaterialTheme.typography.displayLarge,
                        color = xaraColors.accentPrimary
                    )
                    Text(
                        text = session.activeGameName,
                        style = MaterialTheme.typography.titleMedium,
                        color = XaraTextPrimary
                    )
                    Spacer(Modifier.height(8.dp))
                    InfoPairRow("Initial / Peak Temp", "${session.initialTempC}°C / ${session.peakTempC}°C")
                    InfoPairRow("Average RAM", "${session.avgRamPercent}%")
                    InfoPairRow("Total Recorded Playtime", FormatUtils.formatDuration(totalPlaytimeSec))

                    Spacer(Modifier.height(12.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                if (session.isRunning) viewModel.stopSession(true)
                                else viewModel.startSession()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (session.isRunning) xaraColors.thermalHot else xaraColors.accentPrimary,
                                contentColor = if (session.isRunning) XaraTextPrimary else XaraObsidian
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = if (session.isRunning) Icons.Filled.Stop else Icons.Filled.PlayArrow,
                                contentDescription = if (session.isRunning) "Stop Session" else "Start Session"
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(if (session.isRunning) "STOP SESSION" else "START SESSION", style = MaterialTheme.typography.labelLarge)
                        }
                        OutlinedButton(
                            onClick = { viewModel.restoreTemporarySettings() },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Filled.Restore, contentDescription = "Restore temporary settings")
                            Spacer(Modifier.width(6.dp))
                            Text("RESTORE", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }

            item {
                XaraGlassCard {
                    XaraSectionHeader(title = "Active Game Detection")
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Detect Foreground Game", style = MaterialTheme.typography.titleMedium, color = XaraTextPrimary)
                            Text(
                                if (hasUsageAccess) "Usage Access Granted — detects active app automatically"
                                else "Requires Android Usage Access permission",
                                style = MaterialTheme.typography.labelSmall,
                                color = XaraTextSecondary
                            )
                        }
                        Switch(
                            checked = settings.activeGameDetectionEnabled,
                            onCheckedChange = { viewModel.setActiveGameDetection(it) },
                            colors = SwitchDefaults.colors(checkedThumbColor = xaraColors.accentPrimary)
                        )
                    }
                    if (!hasUsageAccess) {
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = { PermissionHelper.openUsageAccessSettings(context) },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("GRANT USAGE ACCESS PERMISSION", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }

            item { XaraSectionHeader(title = "Session History (${history.size})") }
            if (history.isEmpty()) {
                item {
                    XaraGlassCard {
                        Text("No completed sessions yet.", style = MaterialTheme.typography.bodyMedium, color = XaraTextSecondary)
                    }
                }
            } else {
                items(history, key = { it.id }) { s ->
                    XaraGlassCard(cornerRadius = 14.dp, contentPadding = 12.dp) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(s.gameName, style = MaterialTheme.typography.titleMedium, color = XaraTextPrimary)
                                Text(
                                    "${FormatUtils.formatTimestamp(s.startTimestamp)} • Peak ${s.peakTempC}°C • Battery -${s.batteryDeltaPct}%",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = XaraTextSecondary
                                )
                            }
                            Text(FormatUtils.formatDuration(s.durationSeconds), style = MaterialTheme.typography.labelLarge, color = xaraColors.accentPrimary)
                        }
                    }
                }
            }
        }
    }
}
