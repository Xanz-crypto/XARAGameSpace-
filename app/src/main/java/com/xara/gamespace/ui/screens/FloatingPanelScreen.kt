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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xara.gamespace.domain.model.FloatingPositionPreset
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FloatingPanelScreen(viewModel: XaraMainViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val settings by viewModel.userSettings.collectAsStateWithLifecycle()
    val isRunning by viewModel.isFloatingPanelRunning.collectAsStateWithLifecycle()
    val xaraColors = LocalXaraColors.current

    var hasOverlayPerm by remember { mutableStateOf(PermissionHelper.canDrawOverlays(context)) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasOverlayPerm = PermissionHelper.canDrawOverlays(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        XaraTopBar(title = "Floating Game Panel", subtitle = "Draggable in-game HUD overlay", onBack = onBack)
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                XaraGlassCard(accentBorder = true) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Overlay Permission", style = MaterialTheme.typography.titleMedium, color = XaraTextPrimary)
                            Text("Required to display XARA HUD over games", style = MaterialTheme.typography.labelSmall, color = XaraTextSecondary)
                        }
                        StatusPillBadge(
                            text = if (hasOverlayPerm) "GRANTED" else "REQUIRED",
                            color = if (hasOverlayPerm) xaraColors.thermalCool else xaraColors.thermalHot
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    if (!hasOverlayPerm) {
                        Button(
                            onClick = { PermissionHelper.openOverlaySettings(context) },
                            colors = ButtonDefaults.buttonColors(containerColor = xaraColors.accentPrimary, contentColor = XaraObsidian),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Filled.OpenInNew, contentDescription = "Grant Overlay Permission")
                            Spacer(Modifier.width(8.dp))
                            Text("GRANT OVERLAY PERMISSION", style = MaterialTheme.typography.labelLarge)
                        }
                    } else {
                        Button(
                            onClick = {
                                if (isRunning) viewModel.stopFloatingPanel() else viewModel.startFloatingPanel()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isRunning) xaraColors.thermalHot else xaraColors.accentPrimary,
                                contentColor = if (isRunning) XaraTextPrimary else XaraObsidian
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = if (isRunning) Icons.Filled.Close else Icons.Filled.Layers,
                                contentDescription = if (isRunning) "Stop Floating Panel" else "Launch Floating Panel"
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = if (isRunning) "CLOSE FLOATING PANEL" else "LAUNCH FLOATING PANEL",
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                }
            }

            item {
                XaraGlassCard {
                    XaraSectionHeader(title = "Panel Customization")
                    Spacer(Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Transparent Mode", style = MaterialTheme.typography.titleMedium, color = XaraTextPrimary)
                            Text("Lowers overlay opacity to keep game HUD visible", style = MaterialTheme.typography.labelSmall, color = XaraTextSecondary)
                        }
                        Switch(
                            checked = settings.floatingTransparentMode,
                            onCheckedChange = { viewModel.updateFloatingTransparency(it) },
                            colors = SwitchDefaults.colors(checkedThumbColor = xaraColors.accentPrimary)
                        )
                    }

                    Spacer(Modifier.height(12.dp))
                    Text("Initial Position Preset", style = MaterialTheme.typography.labelLarge, color = XaraTextSecondary)
                    Spacer(Modifier.height(6.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FloatingPositionPreset.entries.forEach { preset ->
                            FilterChip(
                                selected = settings.floatingPositionPreset == preset,
                                onClick = { viewModel.updateFloatingPreset(preset) },
                                label = { Text(preset.displayName) }
                            )
                        }
                    }
                }
            }

            item {
                XaraGlassCard {
                    XaraSectionHeader(title = "Included Floating HUD Modules")
                    Spacer(Modifier.height(8.dp))
                    val items = listOf(
                        "XARA BOOST (One-tap background cleanup)",
                        "RAM Monitor (Live MB & percentage)",
                        "CPU Monitor (Real load or N/A)",
                        "FPS & Refresh Rate Monitor",
                        "Battery Temperature (°C)",
                        "Ping Monitor (Live RTT latency)",
                        "Graphics, Sensitivity & Macro Shortcuts",
                        "Draggable Orb, Expand/Minimize & Close Panel"
                    )
                    items.forEach { line ->
                        Text("• $line", style = MaterialTheme.typography.bodyMedium, color = XaraTextPrimary)
                    }
                }
            }
        }
    }
}
