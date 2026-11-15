package com.xara.gamespace.ui.screens

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.xara.gamespace.domain.model.InstalledGameItem
import com.xara.gamespace.ui.XaraMainViewModel
import com.xara.gamespace.ui.components.AppIconImage
import com.xara.gamespace.ui.components.XaraGlassCard
import com.xara.gamespace.ui.components.XaraSectionHeader
import com.xara.gamespace.ui.theme.LocalXaraColors
import com.xara.gamespace.ui.theme.XaraObsidian
import com.xara.gamespace.ui.theme.XaraTextPrimary
import com.xara.gamespace.ui.theme.XaraTextSecondary
import com.xara.gamespace.utils.FormatUtils

private enum class GameFilter(val label: String) {
    IN_SPACE("Game Space"),
    DETECTED_GAMES("Detected Games"),
    FAVORITES("Favorites"),
    ALL_APPS("All Installed")
}

@Composable
fun GamesManagerScreen(
    viewModel: XaraMainViewModel,
    onOpenGameDetail: (String) -> Unit
) {
    val apps by viewModel.installedApps.collectAsStateWithLifecycle()
    val isScanning by viewModel.isScanningApps.collectAsStateWithLifecycle()
    val sessions by viewModel.gameSessions.collectAsStateWithLifecycle()
    val xaraColors = LocalXaraColors.current

    var query by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf(GameFilter.IN_SPACE) }
    var showHistory by remember { mutableStateOf(false) }

    val filtered = remember(apps, query, filter) {
        apps.filter { item ->
            val matchesFilter = when (filter) {
                GameFilter.IN_SPACE -> item.isAddedToSpace
                GameFilter.DETECTED_GAMES -> item.isSystemGame
                GameFilter.FAVORITES -> item.isFavorite
                GameFilter.ALL_APPS -> true
            }
            val matchesQuery = query.isBlank() ||
                item.appName.contains(query, ignoreCase = true) ||
                item.packageName.contains(query, ignoreCase = true)
            matchesFilter && matchesQuery
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            XaraGlassCard(accentBorder = true, contentPadding = 0.dp) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    Image(
                        painter = painterResource(id = R.drawable.img_gaming_banner),
                        contentDescription = "XARA Game Library Banner",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .matchParentSize()
                            .alpha(0.32f)
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.horizontalGradient(
                                    listOf(XaraObsidian.copy(alpha = 0.85f), Color.Transparent)
                                )
                            )
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "GAME MANAGER",
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = XaraTextPrimary
                                )
                                Text(
                                    text = "${apps.count { it.isAddedToSpace }} in Space • ${apps.size} Launchable Apps",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = xaraColors.accentPrimary
                                )
                            }
                            Row {
                                IconButton(onClick = { showHistory = !showHistory }) {
                                    Icon(
                                        imageVector = Icons.Filled.History,
                                        contentDescription = "Session History",
                                        tint = if (showHistory) xaraColors.accentPrimary else XaraTextSecondary
                                    )
                                }
                                IconButton(
                                    onClick = { viewModel.rescanInstalledApps() },
                                    modifier = Modifier.testTag("rescan_games_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Refresh,
                                        contentDescription = "Scan installed applications",
                                        tint = xaraColors.accentPrimary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        if (showHistory) {
            item {
                XaraSectionHeader(
                    title = "Game Session History",
                    subtitle = "Recent recorded sessions across titles"
                )
            }
            if (sessions.isEmpty()) {
                item {
                    XaraGlassCard {
                        Text(
                            text = "No game sessions recorded yet. Launch a game or start a session in Tools.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = XaraTextSecondary
                        )
                    }
                }
            } else {
                items(sessions.take(15), key = { it.id }) { s ->
                    XaraGlassCard(cornerRadius = 14.dp, contentPadding = 12.dp) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(s.gameName, style = MaterialTheme.typography.titleMedium, color = XaraTextPrimary)
                                Text(
                                    text = "${FormatUtils.formatTimestamp(s.startTimestamp)} • Peak ${s.peakTempC}°C",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = XaraTextSecondary
                                )
                            }
                            Text(
                                text = FormatUtils.formatDuration(s.durationSeconds),
                                style = MaterialTheme.typography.labelLarge,
                                color = xaraColors.accentPrimary
                            )
                        }
                    }
                }
            }
        }

        item {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("Search installed games or packages...") },
                leadingIcon = {
                    Icon(Icons.Filled.Search, contentDescription = "Search", tint = xaraColors.accentPrimary)
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = xaraColors.accentPrimary,
                    unfocusedBorderColor = xaraColors.glassBorder,
                    focusedTextColor = XaraTextPrimary,
                    unfocusedTextColor = XaraTextPrimary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("games_search_input")
            )
        }

        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(GameFilter.entries) { option ->
                    FilterChip(
                        selected = filter == option,
                        onClick = { filter = option },
                        label = { Text(option.label, style = MaterialTheme.typography.labelSmall) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = xaraColors.accentPrimary.copy(alpha = 0.2f),
                            selectedLabelColor = xaraColors.accentPrimary,
                            labelColor = XaraTextSecondary
                        )
                    )
                }
            }
        }

        if (isScanning) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = xaraColors.accentPrimary)
                }
            }
        } else if (filtered.isEmpty()) {
            item {
                XaraGlassCard {
                    Text(
                        text = "No apps matched this filter. Switch to \"All Installed\" to add any app to XARAGameSpace.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = XaraTextSecondary
                    )
                }
            }
        } else {
            items(filtered, key = { it.packageName }) { item ->
                GameRowCard(
                    item = item,
                    viewModel = viewModel,
                    onLaunch = { viewModel.launchGame(item) },
                    onToggleSpace = { viewModel.toggleGameInSpace(item) },
                    onToggleFav = { viewModel.toggleFavoriteGame(item) },
                    onOpenDetail = { onOpenGameDetail(item.packageName) }
                )
            }
        }
    }
}

@Composable
private fun GameRowCard(
    item: InstalledGameItem,
    viewModel: XaraMainViewModel,
    onLaunch: () -> Unit,
    onToggleSpace: () -> Unit,
    onToggleFav: () -> Unit,
    onOpenDetail: () -> Unit
) {
    val xaraColors = LocalXaraColors.current
    XaraGlassCard(
        cornerRadius = 18.dp,
        contentPadding = 12.dp,
        onClick = onOpenDetail
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AppIconImage(
                bitmap = viewModel.getAppIconBitmap(item.packageName),
                appName = item.appName,
                size = 52.dp
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.appName,
                    style = MaterialTheme.typography.titleMedium,
                    color = XaraTextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = item.packageName,
                    style = MaterialTheme.typography.labelSmall,
                    color = XaraTextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = "v${item.versionName} • ${item.compileStatus} • Playtime ${FormatUtils.formatDuration(item.totalPlaytimeSeconds)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = xaraColors.accentPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            IconButton(onClick = onToggleFav) {
                Icon(
                    imageVector = if (item.isFavorite) Icons.Filled.Star else Icons.Outlined.StarOutline,
                    contentDescription = "Favorite ${item.appName}",
                    tint = if (item.isFavorite) xaraColors.thermalWarm else XaraTextSecondary
                )
            }
            IconButton(onClick = onToggleSpace) {
                Icon(
                    imageVector = if (item.isAddedToSpace) Icons.Filled.Check else Icons.Filled.Add,
                    contentDescription = if (item.isAddedToSpace) "Remove from Game Space" else "Add to Game Space",
                    tint = if (item.isAddedToSpace) xaraColors.thermalCool else xaraColors.accentPrimary
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onOpenDetail,
                colors = ButtonDefaults.buttonColors(
                    containerColor = xaraColors.glassElevated,
                    contentColor = XaraTextPrimary
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Filled.Tune, contentDescription = "Per-game settings", modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("PROFILE", style = MaterialTheme.typography.labelLarge)
            }
            Button(
                onClick = onLaunch,
                colors = ButtonDefaults.buttonColors(
                    containerColor = xaraColors.accentPrimary,
                    contentColor = XaraObsidian
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Filled.PlayArrow, contentDescription = "Launch ${item.appName}", modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("LAUNCH", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}
