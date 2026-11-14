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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xara.gamespace.domain.model.CompileMode
import com.xara.gamespace.ui.XaraMainViewModel
import com.xara.gamespace.ui.components.AppIconImage
import com.xara.gamespace.ui.components.StatusPillBadge
import com.xara.gamespace.ui.components.XaraGlassCard
import com.xara.gamespace.ui.components.XaraSectionHeader
import com.xara.gamespace.ui.components.XaraTopBar
import com.xara.gamespace.ui.theme.LocalXaraColors
import com.xara.gamespace.ui.theme.XaraObsidian
import com.xara.gamespace.ui.theme.XaraTextPrimary
import com.xara.gamespace.ui.theme.XaraTextSecondary
import com.xara.gamespace.utils.FormatUtils

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GameCompilerScreen(
    viewModel: XaraMainViewModel,
    onOpenShizuku: () -> Unit,
    onBack: () -> Unit
) {
    val apps by viewModel.installedApps.collectAsStateWithLifecycle()
    val shizuku by viewModel.shizukuStatus.collectAsStateWithLifecycle()
    val compilingPkg by viewModel.compileProgressPackage.collectAsStateWithLifecycle()
    val lastResult by viewModel.lastCompilationResult.collectAsStateWithLifecycle()
    val xaraColors = LocalXaraColors.current

    var selectedMode by remember { mutableStateOf(CompileMode.SPEED_PROFILE) }
    val spaceGames = apps.filter { it.isAddedToSpace }.ifEmpty { apps.take(15) }

    Column(modifier = Modifier.fillMaxSize()) {
        XaraTopBar(title = "Game Compilation", subtitle = "Android ART cmd package compile", onBack = onBack)
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                XaraGlassCard(accentBorder = true) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Shizuku Compilation Requirement", style = MaterialTheme.typography.titleMedium, color = XaraTextPrimary)
                            Text("Executes legitimate Android ART dex2oat optimization via Shizuku.", style = MaterialTheme.typography.labelSmall, color = XaraTextSecondary)
                        }
                        StatusPillBadge(
                            text = if (shizuku.isPermissionGranted) "READY" else "SHIZUKU NEEDED",
                            color = if (shizuku.isPermissionGranted) xaraColors.thermalCool else xaraColors.thermalWarm,
                            onClick = onOpenShizuku
                        )
                    }
                }
            }

            item {
                XaraGlassCard {
                    XaraSectionHeader(title = "Compilation Mode")
                    Spacer(Modifier.height(8.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CompileMode.entries.forEach { mode ->
                            FilterChip(
                                selected = selectedMode == mode,
                                onClick = { selectedMode = mode },
                                label = { Text(mode.flag) }
                            )
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(selectedMode.description, style = MaterialTheme.typography.labelSmall, color = XaraTextSecondary)
                }
            }

            lastResult?.let { res ->
                item {
                    XaraGlassCard(accentBorder = true) {
                        StatusPillBadge(
                            text = if (res.success) "COMPILE SUCCESS" else "COMPILE FAILED",
                            color = if (res.success) xaraColors.thermalCool else xaraColors.thermalHot
                        )
                        Spacer(Modifier.height(6.dp))
                        Text("${res.appName} (${res.mode.flag})", style = MaterialTheme.typography.titleMedium, color = XaraTextPrimary)
                        Text(res.outputMessage, style = MaterialTheme.typography.bodyMedium, color = xaraColors.accentPrimary)
                        res.errorDetails?.let { details ->
                            Spacer(Modifier.height(4.dp))
                            Text(details, style = MaterialTheme.typography.labelSmall, color = XaraTextSecondary)
                        }
                    }
                }
            }

            item {
                XaraSectionHeader(title = "Target Games (${spaceGames.size})")
            }

            items(spaceGames, key = { it.packageName }) { game ->
                val isCompiling = compilingPkg == game.packageName
                XaraGlassCard(cornerRadius = 16.dp, contentPadding = 12.dp) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AppIconImage(
                            bitmap = viewModel.getAppIconBitmap(game.packageName),
                            appName = game.appName,
                            size = 46.dp
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(game.appName, style = MaterialTheme.typography.titleMedium, color = XaraTextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("${game.compileStatus} • ${FormatUtils.formatTimestamp(game.lastCompiledTimestamp)}", style = MaterialTheme.typography.labelSmall, color = xaraColors.accentPrimary)
                        }
                        Button(
                            onClick = { viewModel.compileGame(game, selectedMode) },
                            enabled = compilingPkg == null,
                            colors = ButtonDefaults.buttonColors(containerColor = xaraColors.accentPrimary, contentColor = XaraObsidian),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            if (isCompiling) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = XaraObsidian, strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Filled.Build, contentDescription = "Compile ${game.appName}", modifier = Modifier.size(16.dp))
                            }
                            Spacer(Modifier.width(6.dp))
                            Text("COMPILE", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }
        }
    }
}
