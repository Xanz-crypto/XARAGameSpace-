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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xara.gamespace.domain.model.GraphicsDriverOption
import com.xara.gamespace.domain.model.TextureOptMode
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
fun GraphicsEngineScreen(viewModel: XaraMainViewModel, onBack: () -> Unit) {
    val settings by viewModel.userSettings.collectAsStateWithLifecycle()
    val shizuku by viewModel.shizukuStatus.collectAsStateWithLifecycle()
    val statusMessage by viewModel.graphicsStatusMessage.collectAsStateWithLifecycle()
    val compat = viewModel.graphicsCompatibility
    val xaraColors = LocalXaraColors.current

    var selectedDriver by remember(settings.selectedGraphicsDriver) { mutableStateOf(settings.selectedGraphicsDriver) }
    var selectedProfile by remember(settings.selectedGraphicsProfile) { mutableStateOf(settings.selectedGraphicsProfile) }
    var selectedTexture by remember(settings.selectedTextureMode) { mutableStateOf(settings.selectedTextureMode) }

    val profiles = listOf("Competitive", "Ultra Smooth", "Balanced", "Thermal Safe")

    Column(modifier = Modifier.fillMaxSize()) {
        XaraTopBar(
            title = "Graphics Engine",
            subtitle = "Drivers, compatibility & texture optimization",
            onBack = onBack
        )
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                XaraGlassCard(accentBorder = true) {
                    XaraSectionHeader(title = "Device Graphics Compatibility")
                    Spacer(Modifier.height(8.dp))
                    InfoPairRow("Vulkan Support", if (compat.vulkanSupported) compat.vulkanVersion else "Not Supported")
                    InfoPairRow("OpenGL ES", compat.openGlEsVersion)
                    InfoPairRow("ANGLE Layer", if (compat.angleAvailable) "Android 10+ Supported" else "Unsupported (< API 29)")
                    InfoPairRow("Game Driver Flag", if (compat.updatableDriverSupported) "API 29+ (Requires Shizuku/Dev)" else "Unsupported")
                    Spacer(Modifier.height(6.dp))
                    StatusPillBadge(
                        text = if (shizuku.isPermissionGranted) "SHIZUKU FLAG APPLY READY" else "PROFILE MODE (NO SHIZUKU)",
                        color = if (shizuku.isPermissionGranted) xaraColors.thermalCool else xaraColors.thermalWarm
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(compat.shizukuRequiredNote, style = MaterialTheme.typography.labelSmall, color = XaraTextSecondary)
                }
            }

            item {
                XaraGlassCard {
                    XaraSectionHeader(title = "Graphics Driver Selection")
                    Spacer(Modifier.height(10.dp))
                    GraphicsDriverOption.entries.forEach { option ->
                        val isSelected = selectedDriver == option
                        XaraGlassCard(
                            accentBorder = isSelected,
                            cornerRadius = 14.dp,
                            contentPadding = 12.dp,
                            onClick = { selectedDriver = option },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(option.displayName, style = MaterialTheme.typography.titleMedium, color = if (isSelected) xaraColors.accentPrimary else XaraTextPrimary)
                                    Text(option.description, style = MaterialTheme.typography.labelSmall, color = XaraTextSecondary)
                                }
                                if (isSelected) {
                                    Icon(Icons.Filled.CheckCircle, contentDescription = "Selected", tint = xaraColors.accentPrimary)
                                }
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                }
            }

            item {
                XaraGlassCard {
                    XaraSectionHeader(title = "Graphics Profile")
                    Spacer(Modifier.height(8.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        profiles.forEach { p ->
                            FilterChip(
                                selected = selectedProfile == p,
                                onClick = { selectedProfile = p },
                                label = { Text(p) }
                            )
                        }
                    }

                    Spacer(Modifier.height(14.dp))
                    XaraSectionHeader(title = "Texture Optimization", subtitle = "Includes Burik Soft & Burik Pixel profiles")
                    Spacer(Modifier.height(8.dp))
                    TextureOptMode.entries.forEach { tex ->
                        val active = selectedTexture == tex
                        XaraGlassCard(
                            accentBorder = active,
                            cornerRadius = 14.dp,
                            contentPadding = 12.dp,
                            onClick = { selectedTexture = tex },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("${tex.displayName} (${tex.resolutionScalePercent}%)", style = MaterialTheme.typography.titleMedium, color = if (active) xaraColors.accentPrimary else XaraTextPrimary)
                                    Text(tex.description, style = MaterialTheme.typography.labelSmall, color = XaraTextSecondary)
                                }
                                if (active) {
                                    Icon(Icons.Filled.CheckCircle, contentDescription = "Selected", tint = xaraColors.accentPrimary)
                                }
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                }
            }

            statusMessage?.let { msg ->
                item {
                    XaraGlassCard(accentBorder = true) {
                        Text(msg, style = MaterialTheme.typography.bodyMedium, color = xaraColors.accentPrimary)
                    }
                }
            }

            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = {
                            selectedDriver = GraphicsDriverOption.DEFAULT
                            selectedProfile = "Balanced"
                            selectedTexture = TextureOptMode.SHARP_NATIVE
                            viewModel.resetGraphicsSettings()
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Filled.RestartAlt, contentDescription = "Reset Graphics")
                        Spacer(Modifier.width(6.dp))
                        Text("RESET", style = MaterialTheme.typography.labelLarge)
                    }
                    Button(
                        onClick = {
                            viewModel.applyGraphicsSettings(selectedDriver, selectedProfile, selectedTexture)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = xaraColors.accentPrimary, contentColor = XaraObsidian),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1.4f)
                    ) {
                        Icon(Icons.Filled.Save, contentDescription = "Apply Graphics")
                        Spacer(Modifier.width(6.dp))
                        Text("APPLY / SAVE", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
    }
}
