package com.xara.gamespace.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xara.gamespace.domain.model.BoostMode
import com.xara.gamespace.domain.model.GraphicsDriverOption
import com.xara.gamespace.domain.model.TextureOptMode
import com.xara.gamespace.ui.XaraMainViewModel
import com.xara.gamespace.ui.components.AppIconImage
import com.xara.gamespace.ui.components.XaraGlassCard
import com.xara.gamespace.ui.components.XaraSectionHeader
import com.xara.gamespace.ui.components.XaraTopBar
import com.xara.gamespace.ui.theme.LocalXaraColors
import com.xara.gamespace.ui.theme.XaraObsidian
import com.xara.gamespace.ui.theme.XaraTextPrimary
import com.xara.gamespace.ui.theme.XaraTextSecondary
import com.xara.gamespace.utils.FormatUtils
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GameDetailScreen(
    packageName: String,
    viewModel: XaraMainViewModel,
    onBack: () -> Unit
) {
    val apps by viewModel.installedApps.collectAsStateWithLifecycle()
    val sessions by viewModel.gameSessions.collectAsStateWithLifecycle()
    val xaraColors = LocalXaraColors.current

    val game = apps.firstOrNull { it.packageName == packageName }
    if (game == null) {
        Column(modifier = Modifier.fillMaxSize()) {
            XaraTopBar(title = "Game Profile", onBack = onBack)
            XaraGlassCard(modifier = Modifier.padding(16.dp)) {
                Text("Package $packageName not found.", color = XaraTextSecondary)
            }
        }
        return
    }

    var selectedBoost by remember(game.packageName) { mutableStateOf(game.boostModeOverride) }
    var selectedDriver by remember(game.packageName) { mutableStateOf(game.graphicsDriver) }
    var selectedTexture by remember(game.packageName) { mutableStateOf(game.textureOptMode) }
    var sensX by remember(game.packageName) { mutableFloatStateOf(game.sensitivityX) }
    var sensY by remember(game.packageName) { mutableFloatStateOf(game.sensitivityY) }

    val gameSessions = sessions.filter { it.gamePackage == packageName }

    Column(modifier = Modifier.fillMaxSize()) {
        XaraTopBar(
            title = game.appName,
            subtitle = game.packageName,
            onBack = onBack
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                XaraGlassCard(accentBorder = true) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AppIconImage(
                            bitmap = viewModel.getAppIconBitmap(game.packageName),
                            appName = game.appName,
                            size = 60.dp
                        )
                        Spacer(Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(game.appName, style = MaterialTheme.typography.titleLarge, color = XaraTextPrimary)
                            Text("Version ${game.versionName} • ${game.compileStatus}", style = MaterialTheme.typography.labelSmall, color = xaraColors.accentPrimary)
                            Text(
                                "Total Playtime: ${FormatUtils.formatDuration(game.totalPlaytimeSeconds)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = XaraTextSecondary
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = { viewModel.launchGame(game) },
                        colors = ButtonDefaults.buttonColors(containerColor = xaraColors.accentPrimary, contentColor = XaraObsidian),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = "Launch game")
                        Spacer(Modifier.width(6.dp))
                        Text("LAUNCH & START SESSION", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }

            item {
                XaraGlassCard {
                    XaraSectionHeader(title = "Per-Game Boost Override")
                    Spacer(Modifier.height(10.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = selectedBoost == null,
                            onClick = { selectedBoost = null },
                            label = { Text("Global Default") }
                        )
                        BoostMode.entries.forEach { mode ->
                            FilterChip(
                                selected = selectedBoost == mode,
                                onClick = { selectedBoost = mode },
                                label = { Text(mode.shortLabel) }
                            )
                        }
                    }

                    Spacer(Modifier.height(14.dp))
                    XaraSectionHeader(title = "Per-Game Graphics Driver")
                    Spacer(Modifier.height(8.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        GraphicsDriverOption.entries.forEach { drv ->
                            FilterChip(
                                selected = selectedDriver == drv,
                                onClick = { selectedDriver = drv },
                                label = { Text(drv.displayName) }
                            )
                        }
                    }

                    Spacer(Modifier.height(14.dp))
                    XaraSectionHeader(title = "Texture Profile")
                    Spacer(Modifier.height(8.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextureOptMode.entries.forEach { tex ->
                            FilterChip(
                                selected = selectedTexture == tex,
                                onClick = { selectedTexture = tex },
                                label = { Text(tex.displayName) }
                            )
                        }
                    }
                }
            }

            item {
                XaraGlassCard {
                    XaraSectionHeader(
                        title = "Per-Game Touch Profile",
                        subtitle = "Reference multipliers saved locally"
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = String.format(Locale.US, "Sensitivity X: %.2fx", sensX),
                        style = MaterialTheme.typography.labelMedium,
                        color = xaraColors.accentPrimary
                    )
                    Slider(
                        value = sensX,
                        onValueChange = { sensX = it },
                        valueRange = 0.5f..2.0f,
                        colors = SliderDefaults.colors(thumbColor = xaraColors.accentPrimary, activeTrackColor = xaraColors.accentPrimary)
                    )
                    Text(
                        text = String.format(Locale.US, "Sensitivity Y: %.2fx", sensY),
                        style = MaterialTheme.typography.labelMedium,
                        color = xaraColors.accentSecondary
                    )
                    Slider(
                        value = sensY,
                        onValueChange = { sensY = it },
                        valueRange = 0.5f..2.0f,
                        colors = SliderDefaults.colors(thumbColor = xaraColors.accentSecondary, activeTrackColor = xaraColors.accentSecondary)
                    )
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = {
                            viewModel.updateGameSettings(game, selectedBoost, selectedDriver, selectedTexture, sensX, sensY)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = xaraColors.accentSecondary, contentColor = XaraTextPrimary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.Save, contentDescription = "Save game profile")
                        Spacer(Modifier.width(6.dp))
                        Text("SAVE PER-GAME CONFIGURATION", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }

            item {
                XaraSectionHeader(title = "Game Session History (${gameSessions.size})")
            }

            if (gameSessions.isEmpty()) {
                item {
                    XaraGlassCard {
                        Text(
                            text = "No recorded sessions for ${game.appName} yet.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = XaraTextSecondary
                        )
                    }
                }
            } else {
                items(gameSessions, key = { it.id }) { s ->
                    XaraGlassCard(cornerRadius = 14.dp, contentPadding = 12.dp) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(FormatUtils.formatTimestamp(s.startTimestamp), style = MaterialTheme.typography.titleMedium, color = XaraTextPrimary)
                                Text("Mode: ${s.boostMode} • Peak ${s.peakTempC}°C • Avg RAM ${s.avgRamPercent}%", style = MaterialTheme.typography.labelSmall, color = XaraTextSecondary)
                            }
                            Text(FormatUtils.formatDuration(s.durationSeconds), style = MaterialTheme.typography.labelLarge, color = xaraColors.accentPrimary)
                        }
                    }
                }
            }
        }
    }
}
