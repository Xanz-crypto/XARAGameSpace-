package com.xara.gamespace.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xara.gamespace.data.local.MacroConfigEntity
import com.xara.gamespace.domain.model.TouchZoneMode
import com.xara.gamespace.ui.XaraMainViewModel
import com.xara.gamespace.ui.components.StatusPillBadge
import com.xara.gamespace.ui.components.XaraGlassCard
import com.xara.gamespace.ui.components.XaraSectionHeader
import com.xara.gamespace.ui.components.XaraTopBar
import com.xara.gamespace.ui.theme.LocalXaraColors
import com.xara.gamespace.ui.theme.XaraGraphiteDark
import com.xara.gamespace.ui.theme.XaraObsidian
import com.xara.gamespace.ui.theme.XaraTextPrimary
import com.xara.gamespace.ui.theme.XaraTextSecondary
import java.util.Locale
import kotlin.math.hypot

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TouchOptimizerScreen(viewModel: XaraMainViewModel, onBack: () -> Unit) {
    val profiles by viewModel.touchProfiles.collectAsStateWithLifecycle()
    val apps by viewModel.installedApps.collectAsStateWithLifecycle()
    val xaraColors = LocalXaraColors.current

    var profileName by remember { mutableStateOf("My Custom Aim") }
    var selectedGamePkg by remember { mutableStateOf<String?>(null) }
    var sensX by remember { mutableFloatStateOf(1.0f) }
    var sensY by remember { mutableFloatStateOf(1.0f) }
    var touchArea by remember { mutableFloatStateOf(1.0f) }
    var zoneMode by remember { mutableStateOf(TouchZoneMode.FULL_SCREEN) }

    val spaceGames = apps.filter { it.isAddedToSpace }.take(8)

    Column(modifier = Modifier.fillMaxSize()) {
        XaraTopBar(title = "Touch & Sensitivity", subtitle = "Per-game calibration profiles & presets", onBack = onBack)
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                XaraGlassCard(accentBorder = true) {
                    StatusPillBadge(
                        text = "STATUS: PROFILE SAVED (NON-ROOT ANDROID)",
                        color = xaraColors.thermalWarm
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "Android does not allow third-party apps to override kernel touchscreen sensitivity without OEM Game Turbo or root. XARAGameSpace saves your calibrated X/Y, touch area, and zone profiles per game as reference & HUD configs.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = XaraTextSecondary
                    )
                }
            }

            item {
                XaraGlassCard {
                    XaraSectionHeader(title = "Presets & Saved Profiles")
                    Spacer(Modifier.height(8.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        profiles.forEach { p ->
                            FilterChip(
                                selected = false,
                                onClick = {
                                    profileName = p.name
                                    sensX = p.sensitivityX
                                    sensY = p.sensitivityY
                                    touchArea = p.touchAreaScale
                                    zoneMode = runCatching { TouchZoneMode.valueOf(p.touchZoneMode) }
                                        .getOrDefault(TouchZoneMode.FULL_SCREEN)
                                    selectedGamePkg = p.gamePackage
                                },
                                label = { Text(p.name) }
                            )
                        }
                    }
                }
            }

            item {
                XaraGlassCard {
                    XaraSectionHeader(title = "Calibration Sliders")
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = profileName,
                        onValueChange = { profileName = it },
                        label = { Text("Profile Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(String.format(Locale.US, "Sensitivity X: %.2fx", sensX), style = MaterialTheme.typography.labelMedium, color = xaraColors.accentPrimary)
                    Slider(
                        value = sensX,
                        onValueChange = { sensX = it },
                        valueRange = 0.5f..2.0f,
                        colors = SliderDefaults.colors(thumbColor = xaraColors.accentPrimary, activeTrackColor = xaraColors.accentPrimary)
                    )
                    Text(String.format(Locale.US, "Sensitivity Y: %.2fx", sensY), style = MaterialTheme.typography.labelMedium, color = xaraColors.accentSecondary)
                    Slider(
                        value = sensY,
                        onValueChange = { sensY = it },
                        valueRange = 0.5f..2.0f,
                        colors = SliderDefaults.colors(thumbColor = xaraColors.accentSecondary, activeTrackColor = xaraColors.accentSecondary)
                    )
                    Text(String.format(Locale.US, "Touch Area Scale: %.0f%%", touchArea * 100f), style = MaterialTheme.typography.labelMedium, color = XaraTextPrimary)
                    Slider(
                        value = touchArea,
                        onValueChange = { touchArea = it },
                        valueRange = 0.6f..1.4f,
                        colors = SliderDefaults.colors(thumbColor = xaraColors.accentPrimary, activeTrackColor = xaraColors.accentPrimary)
                    )

                    Spacer(Modifier.height(8.dp))
                    Text("Touch Zone", style = MaterialTheme.typography.labelLarge, color = XaraTextSecondary)
                    Spacer(Modifier.height(6.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TouchZoneMode.entries.forEach { z ->
                            FilterChip(
                                selected = zoneMode == z,
                                onClick = { zoneMode = z },
                                label = { Text(z.displayName) }
                            )
                        }
                    }

                    if (spaceGames.isNotEmpty()) {
                        Spacer(Modifier.height(10.dp))
                        Text("Bind to Game (Optional)", style = MaterialTheme.typography.labelLarge, color = XaraTextSecondary)
                        Spacer(Modifier.height(6.dp))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = selectedGamePkg == null,
                                onClick = { selectedGamePkg = null },
                                label = { Text("Global") }
                            )
                            spaceGames.forEach { g ->
                                FilterChip(
                                    selected = selectedGamePkg == g.packageName,
                                    onClick = { selectedGamePkg = g.packageName },
                                    label = { Text(g.appName) }
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(14.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(
                            onClick = {
                                sensX = 1.0f
                                sensY = 1.0f
                                touchArea = 1.0f
                                zoneMode = TouchZoneMode.FULL_SCREEN
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Filled.RestartAlt, contentDescription = "Reset Sensitivity")
                            Spacer(Modifier.width(6.dp))
                            Text("RESET", style = MaterialTheme.typography.labelLarge)
                        }
                        Button(
                            onClick = {
                                viewModel.saveTouchProfile(
                                    name = profileName,
                                    gamePackage = selectedGamePkg,
                                    sensX = sensX,
                                    sensY = sensY,
                                    touchArea = touchArea,
                                    zoneMode = zoneMode
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = xaraColors.accentPrimary, contentColor = XaraObsidian),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1.4f)
                        ) {
                            Icon(Icons.Filled.Save, contentDescription = "Save Profile")
                            Spacer(Modifier.width(6.dp))
                            Text("SAVE PROFILE", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }

            val customProfiles = profiles.filter { !it.isPreset }
            if (customProfiles.isNotEmpty()) {
                item { XaraSectionHeader(title = "Saved Custom Profiles (${customProfiles.size})") }
                items(customProfiles, key = { it.id }) { p ->
                    XaraGlassCard(cornerRadius = 14.dp, contentPadding = 12.dp) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(p.name, style = MaterialTheme.typography.titleMedium, color = XaraTextPrimary)
                                Text(
                                    String.format(Locale.US, "X: %.2fx • Y: %.2fx • Area %.0f%% • %s", p.sensitivityX, p.sensitivityY, p.touchAreaScale * 100f, p.touchZoneMode),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = xaraColors.accentPrimary
                                )
                            }
                            IconButton(onClick = { viewModel.deleteTouchProfile(p.id) }) {
                                Icon(Icons.Filled.Delete, contentDescription = "Delete profile", tint = xaraColors.thermalHot)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MacroConfigScreen(viewModel: XaraMainViewModel, onBack: () -> Unit) {
    val configs by viewModel.macroConfigs.collectAsStateWithLifecycle()
    val xaraColors = LocalXaraColors.current

    var macroName by remember { mutableStateOf("Quick Combo Node") }
    var triggerX by remember { mutableFloatStateOf(0.28f) }
    var triggerY by remember { mutableFloatStateOf(0.50f) }
    var targetX by remember { mutableFloatStateOf(0.72f) }
    var targetY by remember { mutableFloatStateOf(0.50f) }
    var triggerSizeDp by remember { mutableFloatStateOf(46f) }
    var repeatIntervalMs by remember { mutableIntStateOf(150) }

    Column(modifier = Modifier.fillMaxSize()) {
        XaraTopBar(title = "Macro Configuration", subtitle = "Visual trigger & target layout editor", onBack = onBack)
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                XaraGlassCard(accentBorder = true) {
                    Text(
                        text = "Legitimate Configuration Editor: Layouts and intervals are saved locally as user-initiated Accessibility/HUD overlay references. No anti-cheat bypass or unauthorized injection is included.",
                        style = MaterialTheme.typography.labelSmall,
                        color = XaraTextSecondary
                    )
                }
            }

            item {
                XaraGlassCard {
                    XaraSectionHeader(title = "Interactive Preview Area", subtitle = "Drag Trigger (Cyan) or Target (Purple) nodes")
                    Spacer(Modifier.height(10.dp))
                    val density = LocalDensity.current
                    BoxWithConstraints(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(XaraGraphiteDark)
                            .border(1.dp, xaraColors.glassBorder, RoundedCornerShape(16.dp))
                    ) {
                        val widthPx = with(density) { maxWidth.toPx() }
                        val heightPx = with(density) { maxHeight.toPx() }
                        var draggingNode by remember { mutableStateOf<String?>(null) }

                        Canvas(
                            modifier = Modifier
                                .fillMaxSize()
                                .pointerInput(Unit) {
                                    detectDragGestures(
                                        onDragStart = { offset ->
                                            val tPos = Offset(triggerX * widthPx, triggerY * heightPx)
                                            val gPos = Offset(targetX * widthPx, targetY * heightPx)
                                            val dTrig = hypot(offset.x - tPos.x, offset.y - tPos.y)
                                            val dTarg = hypot(offset.x - gPos.x, offset.y - gPos.y)
                                            draggingNode = if (dTrig <= dTarg) "TRIGGER" else "TARGET"
                                        },
                                        onDragEnd = { draggingNode = null },
                                        onDragCancel = { draggingNode = null },
                                        onDrag = { change, dragAmount ->
                                            change.consume()
                                            if (draggingNode == "TRIGGER") {
                                                triggerX = (triggerX + dragAmount.x / widthPx).coerceIn(0.08f, 0.92f)
                                                triggerY = (triggerY + dragAmount.y / heightPx).coerceIn(0.10f, 0.90f)
                                            } else if (draggingNode == "TARGET") {
                                                targetX = (targetX + dragAmount.x / widthPx).coerceIn(0.08f, 0.92f)
                                                targetY = (targetY + dragAmount.y / heightPx).coerceIn(0.10f, 0.90f)
                                            }
                                        }
                                    )
                                }
                        ) {
                            val tCenter = Offset(triggerX * size.width, triggerY * size.height)
                            val gCenter = Offset(targetX * size.width, targetY * size.height)
                            val trigR = (triggerSizeDp * 0.45f).dp.toPx()

                            drawLine(
                                color = xaraColors.accentPrimary.copy(alpha = 0.7f),
                                start = tCenter,
                                end = gCenter,
                                strokeWidth = 3.dp.toPx(),
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)
                            )

                            drawCircle(
                                color = xaraColors.accentPrimary.copy(alpha = 0.25f),
                                radius = trigR,
                                center = tCenter
                            )
                            drawCircle(
                                color = xaraColors.accentPrimary,
                                radius = trigR,
                                center = tCenter,
                                style = Stroke(width = 2.5.dp.toPx())
                            )

                            drawCircle(
                                color = xaraColors.accentSecondary.copy(alpha = 0.28f),
                                radius = 20.dp.toPx(),
                                center = gCenter
                            )
                            drawCircle(
                                color = xaraColors.accentSecondary,
                                radius = 20.dp.toPx(),
                                center = gCenter,
                                style = Stroke(width = 2.5.dp.toPx())
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = String.format(
                            Locale.US,
                            "Trigger: (%.0f%%, %.0f%%)  →  Target: (%.0f%%, %.0f%%)",
                            triggerX * 100f, triggerY * 100f, targetX * 100f, targetY * 100f
                        ),
                        style = MaterialTheme.typography.labelMedium,
                        color = xaraColors.accentPrimary
                    )
                }
            }

            item {
                XaraGlassCard {
                    XaraSectionHeader(title = "Position Controller & Timing")
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = macroName,
                        onValueChange = { macroName = it },
                        label = { Text("Configuration Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(10.dp))
                    Text("Trigger Position X: ${(triggerX * 100).toInt()}%", style = MaterialTheme.typography.labelSmall, color = xaraColors.accentPrimary)
                    Slider(value = triggerX, onValueChange = { triggerX = it }, valueRange = 0.08f..0.92f)
                    Text("Trigger Position Y: ${(triggerY * 100).toInt()}%", style = MaterialTheme.typography.labelSmall, color = xaraColors.accentPrimary)
                    Slider(value = triggerY, onValueChange = { triggerY = it }, valueRange = 0.10f..0.90f)
                    Text("Target Position X: ${(targetX * 100).toInt()}%", style = MaterialTheme.typography.labelSmall, color = xaraColors.accentSecondary)
                    Slider(value = targetX, onValueChange = { targetX = it }, valueRange = 0.08f..0.92f)
                    Text("Target Position Y: ${(targetY * 100).toInt()}%", style = MaterialTheme.typography.labelSmall, color = xaraColors.accentSecondary)
                    Slider(value = targetY, onValueChange = { targetY = it }, valueRange = 0.10f..0.90f)
                    Text("Trigger Size: ${triggerSizeDp.toInt()} dp", style = MaterialTheme.typography.labelSmall, color = XaraTextPrimary)
                    Slider(value = triggerSizeDp, onValueChange = { triggerSizeDp = it }, valueRange = 28f..76f)
                    Text("Repeat Interval: $repeatIntervalMs ms", style = MaterialTheme.typography.labelSmall, color = XaraTextPrimary)
                    Slider(
                        value = repeatIntervalMs.toFloat(),
                        onValueChange = { repeatIntervalMs = it.toInt() },
                        valueRange = 50f..2000f
                    )

                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = {
                            viewModel.saveMacroConfig(
                                MacroConfigEntity(
                                    name = macroName.ifBlank { "Macro Preset" },
                                    gamePackage = null,
                                    triggerX = triggerX,
                                    triggerY = triggerY,
                                    targetX = targetX,
                                    targetY = targetY,
                                    triggerSizeDp = triggerSizeDp,
                                    repeatIntervalMs = repeatIntervalMs
                                )
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = xaraColors.accentPrimary, contentColor = XaraObsidian),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.Save, contentDescription = "Save Macro")
                        Spacer(Modifier.width(8.dp))
                        Text("SAVE MACRO CONFIGURATION", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }

            if (configs.isNotEmpty()) {
                item { XaraSectionHeader(title = "Saved Macro Configurations (${configs.size})") }
                items(configs, key = { it.id }) { c ->
                    XaraGlassCard(
                        cornerRadius = 14.dp,
                        contentPadding = 12.dp,
                        onClick = {
                            macroName = c.name
                            triggerX = c.triggerX
                            triggerY = c.triggerY
                            targetX = c.targetX
                            targetY = c.targetY
                            triggerSizeDp = c.triggerSizeDp
                            repeatIntervalMs = c.repeatIntervalMs
                        }
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(c.name, style = MaterialTheme.typography.titleMedium, color = XaraTextPrimary)
                                Text(
                                    "Size ${c.triggerSizeDp.toInt()}dp • Interval ${c.repeatIntervalMs}ms",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = xaraColors.accentPrimary
                                )
                            }
                            IconButton(onClick = { viewModel.deleteMacroConfig(c.id) }) {
                                Icon(Icons.Filled.Delete, contentDescription = "Delete macro", tint = xaraColors.thermalHot)
                            }
                        }
                    }
                }
            }
        }
    }
}
