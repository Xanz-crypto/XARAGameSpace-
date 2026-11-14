package com.xara.gamespace.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.BuildConfig
import com.example.R
import com.xara.gamespace.domain.model.ThemeAccent
import com.xara.gamespace.permissions.PermissionHelper
import com.xara.gamespace.ui.XaraMainViewModel
import com.xara.gamespace.ui.components.StatusPillBadge
import com.xara.gamespace.ui.components.XaraGlassCard
import com.xara.gamespace.ui.components.XaraLogoCrest
import com.xara.gamespace.ui.components.XaraSectionHeader
import com.xara.gamespace.ui.theme.LocalXaraColors
import com.xara.gamespace.ui.theme.XaraObsidian
import com.xara.gamespace.ui.theme.XaraTextPrimary
import com.xara.gamespace.ui.theme.XaraTextSecondary

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProfileSettingsScreen(
    viewModel: XaraMainViewModel,
    onOpenShizuku: () -> Unit,
    onOpenDeviceInfo: () -> Unit
) {
    val settings by viewModel.userSettings.collectAsStateWithLifecycle()
    val telemetry by viewModel.telemetry.collectAsStateWithLifecycle()
    val shizuku by viewModel.shizukuStatus.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val xaraColors = LocalXaraColors.current

    var gamerTagInput by remember(settings.gamerTag) { mutableStateOf(settings.gamerTag) }
    var showPrivacyDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var showResetDialog by remember { mutableStateOf(false) }

    val notificationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        viewModel.setNotificationsEnabled(granted)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            XaraGlassCard(accentBorder = true) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    XaraLogoCrest(size = 54.dp)
                    Spacer(Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(settings.gamerTag, style = MaterialTheme.typography.headlineMedium, color = XaraTextPrimary)
                        Text("${telemetry.manufacturer} ${telemetry.deviceModel} • Android ${telemetry.androidVersion}", style = MaterialTheme.typography.labelSmall, color = xaraColors.accentPrimary)
                    }
                }
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = gamerTagInput,
                        onValueChange = { gamerTagInput = it },
                        label = { Text("Gamer Tag") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    Button(
                        onClick = { viewModel.setGamerTag(gamerTagInput) },
                        colors = ButtonDefaults.buttonColors(containerColor = xaraColors.accentPrimary, contentColor = XaraObsidian),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Filled.Save, contentDescription = "Save Gamer Tag")
                    }
                }
            }
        }

        item {
            XaraGlassCard {
                XaraSectionHeader(title = "Theme & Visuals")
                Spacer(Modifier.height(8.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ThemeAccent.entries.forEach { accent ->
                        FilterChip(
                            selected = settings.themeAccent == accent,
                            onClick = { viewModel.setThemeAccent(accent) },
                            label = { Text(accent.displayName) }
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Reactor & HUD Animations", style = MaterialTheme.typography.titleMedium, color = XaraTextPrimary)
                        Text("Disable to minimize UI GPU draw overhead", style = MaterialTheme.typography.labelSmall, color = XaraTextSecondary)
                    }
                    Switch(
                        checked = settings.animationsEnabled,
                        onCheckedChange = { viewModel.setAnimationsEnabled(it) },
                        colors = SwitchDefaults.colors(checkedThumbColor = xaraColors.accentPrimary)
                    )
                }
            }
        }

        item {
            XaraGlassCard {
                XaraSectionHeader(title = "Permissions & System Integration")
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Session Notifications", style = MaterialTheme.typography.titleMedium, color = XaraTextPrimary)
                        Text("Foreground notification for timer & floating HUD", style = MaterialTheme.typography.labelSmall, color = XaraTextSecondary)
                    }
                    Switch(
                        checked = settings.notificationsEnabled,
                        onCheckedChange = { wantEnable ->
                            if (wantEnable && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                !PermissionHelper.hasNotificationPermission(context)
                            ) {
                                notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                viewModel.setNotificationsEnabled(wantEnable)
                            }
                        },
                        colors = SwitchDefaults.colors(checkedThumbColor = xaraColors.accentPrimary)
                    )
                }
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Overlay Permission", style = MaterialTheme.typography.titleMedium, color = XaraTextPrimary)
                        Text("Required for Floating Game Panel", style = MaterialTheme.typography.labelSmall, color = XaraTextSecondary)
                    }
                    StatusPillBadge(
                        text = if (PermissionHelper.canDrawOverlays(context)) "GRANTED" else "GRANT",
                        color = if (PermissionHelper.canDrawOverlays(context)) xaraColors.thermalCool else xaraColors.thermalWarm,
                        onClick = { PermissionHelper.openOverlaySettings(context) }
                    )
                }
                Spacer(Modifier.height(10.dp))
                OutlinedButton(
                    onClick = onOpenShizuku,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.Terminal, contentDescription = "Shizuku Configuration")
                    Spacer(Modifier.width(8.dp))
                    Text("SHIZUKU CONFIGURATION (${shizuku.privilegeLabel.uppercase()})", style = MaterialTheme.typography.labelLarge)
                }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = onOpenDeviceInfo,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("VIEW FULL DEVICE INFORMATION", style = MaterialTheme.typography.labelLarge)
                }
            }
        }

        item {
            XaraGlassCard {
                XaraSectionHeader(title = "About & Legal")
                Spacer(Modifier.height(8.dp))
                InfoPairRow("Application", stringResource(R.string.app_name))
                InfoPairRow("Developer", stringResource(R.string.brand_name))
                InfoPairRow("App Version", BuildConfig.VERSION_NAME)
                Spacer(Modifier.height(10.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { showAboutDialog = true }, shape = RoundedCornerShape(10.dp), modifier = Modifier.weight(1f)) {
                        Icon(Icons.Filled.Info, contentDescription = "About XARA")
                        Spacer(Modifier.width(4.dp))
                        Text("ABOUT XARA", style = MaterialTheme.typography.labelLarge)
                    }
                    OutlinedButton(onClick = { showPrivacyDialog = true }, shape = RoundedCornerShape(10.dp), modifier = Modifier.weight(1f)) {
                        Icon(Icons.Filled.PrivacyTip, contentDescription = "Privacy Policy")
                        Spacer(Modifier.width(4.dp))
                        Text("PRIVACY", style = MaterialTheme.typography.labelLarge)
                    }
                }
                Spacer(Modifier.height(10.dp))
                Button(
                    onClick = { showResetDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = xaraColors.thermalHot.copy(alpha = 0.2f), contentColor = xaraColors.thermalHot),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.RestartAlt, contentDescription = "Reset Settings")
                    Spacer(Modifier.width(8.dp))
                    Text("RESET ALL SETTINGS", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }

    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            title = { Text("XARAGameSpace v${BuildConfig.VERSION_NAME}") },
            text = {
                Text("Developed by XARA Technology.\n\nTagline: UNLEASH YOUR GAMING POWER.\n\nDesigned for transparent Android hardware telemetry, safe memory optimization, per-game profiles, and Shizuku-assisted ART compilation.")
            },
            confirmButton = {
                TextButton(onClick = { showAboutDialog = false }) { Text("CLOSE") }
            }
        )
    }

    if (showPrivacyDialog) {
        AlertDialog(
            onDismissRequest = { showPrivacyDialog = false },
            title = { Text("Privacy Policy") },
            text = {
                Text("XARAGameSpace operates strictly on-device. Installed app lists, game profiles, boost history, session metrics, touch profiles, and macro layouts are stored locally in Room & DataStore and are never transmitted to external servers.")
            },
            confirmButton = {
                TextButton(onClick = { showPrivacyDialog = false }) { Text("UNDERSTOOD") }
            }
        )
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Reset All Settings?") },
            text = { Text("This clears your saved boost history, session logs, custom touch profiles, macro layouts, and theme preferences.") },
            confirmButton = {
                TextButton(onClick = {
                    showResetDialog = false
                    viewModel.resetAllSettings()
                }) { Text("RESET", color = xaraColors.thermalHot) }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) { Text("CANCEL") }
            }
        )
    }
}
