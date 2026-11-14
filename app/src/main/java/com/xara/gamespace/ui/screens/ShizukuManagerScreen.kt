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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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

@Composable
fun ShizukuManagerScreen(viewModel: XaraMainViewModel, onBack: () -> Unit) {
    val shizuku by viewModel.shizukuStatus.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val xaraColors = LocalXaraColors.current

    val statusColor = when {
        shizuku.isBinderAlive && shizuku.isPermissionGranted -> xaraColors.thermalCool
        shizuku.isBinderAlive -> xaraColors.thermalWarm
        else -> xaraColors.thermalHot
    }

    Column(modifier = Modifier.fillMaxSize()) {
        XaraTopBar(title = "Shizuku Engine", subtitle = "Privileged ADB/Root shell IPC via Rikka Shizuku", onBack = onBack)
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                XaraGlassCard(accentBorder = true) {
                    StatusPillBadge(text = shizuku.privilegeLabel.uppercase(), color = statusColor)
                    Spacer(Modifier.height(10.dp))
                    InfoPairRow("Shizuku App Installed", if (shizuku.isAppInstalled) "Yes (moe.shizuku.privileged.api)" else "No")
                    InfoPairRow("Binder Status", if (shizuku.isBinderAlive) "Alive & Responding" else "Offline")
                    InfoPairRow("Permission Status", if (shizuku.isPermissionGranted) "Granted" else "Not Granted")
                    InfoPairRow("UserService IPC", if (shizuku.isUserServiceBound) "Bound" else "Standby")
                    InfoPairRow("Shizuku API Version", shizuku.shizukuVersion?.toString() ?: "N/A")
                    InfoPairRow("Execution UID", shizuku.uid?.toString() ?: "N/A")

                    shizuku.lastError?.let { err ->
                        Spacer(Modifier.height(8.dp))
                        Text("Diagnostic: $err", style = MaterialTheme.typography.labelSmall, color = xaraColors.thermalWarm)
                    }
                }
            }

            item {
                XaraGlassCard {
                    XaraSectionHeader(title = "Connection Controls")
                    Spacer(Modifier.height(10.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { viewModel.connectShizuku() },
                            colors = ButtonDefaults.buttonColors(containerColor = xaraColors.accentPrimary, contentColor = XaraObsidian),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Filled.Link, contentDescription = "Connect Shizuku")
                            Spacer(Modifier.width(6.dp))
                            Text("CONNECT", style = MaterialTheme.typography.labelLarge)
                        }
                        OutlinedButton(
                            onClick = { viewModel.reconnectShizuku() },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Filled.Refresh, contentDescription = "Reconnect")
                            Spacer(Modifier.width(6.dp))
                            Text("RECONNECT", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { viewModel.disconnectShizuku() },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Filled.LinkOff, contentDescription = "Disconnect")
                            Spacer(Modifier.width(6.dp))
                            Text("DISCONNECT", style = MaterialTheme.typography.labelLarge)
                        }
                        OutlinedButton(
                            onClick = { viewModel.openShizukuApp() },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Filled.OpenInNew, contentDescription = "Open Shizuku")
                            Spacer(Modifier.width(6.dp))
                            Text("OPEN SHIZUKU", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }

            item {
                XaraGlassCard {
                    XaraSectionHeader(
                        title = "Features Requiring Shizuku",
                        subtitle = "Elevated shell permissions explained transparently"
                    )
                    Spacer(Modifier.height(8.dp))
                    Text("• Game Compilation (cmd package compile -m speed-profile -f)", style = MaterialTheme.typography.bodyMedium, color = XaraTextPrimary)
                    Text("• System Graphics Driver Flags (updatable_driver_all_apps / ANGLE)", style = MaterialTheme.typography.bodyMedium, color = XaraTextPrimary)
                }
            }

            item {
                XaraGlassCard {
                    XaraSectionHeader(title = "Wireless Debugging & Pairing Guide")
                    Spacer(Modifier.height(8.dp))
                    val steps = listOf(
                        "1. Install the official Shizuku app and open it.",
                        "2. Enable Developer Options (Settings > About Phone > Tap Build Number 7x).",
                        "3. Turn on Wireless Debugging (Android 11+) while connected to Wi-Fi.",
                        "4. Inside Shizuku, tap 'Pairing' -> 'Developer Options' -> 'Pair device with pairing code'.",
                        "5. Enter the 6-digit pairing code in the Shizuku notification, then tap 'Start' in Shizuku.",
                        "6. Return to XARAGameSpace and tap CONNECT to authorize Shell IPC."
                    )
                    steps.forEach { s ->
                        Text(s, style = MaterialTheme.typography.bodyMedium, color = XaraTextSecondary, modifier = Modifier.height(34.dp))
                    }
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = { PermissionHelper.openDeveloperOptions(context) },
                        colors = ButtonDefaults.buttonColors(containerColor = xaraColors.accentSecondary, contentColor = XaraTextPrimary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.Settings, contentDescription = "Open Developer Options")
                        Spacer(Modifier.width(8.dp))
                        Text("OPEN DEVELOPER OPTIONS", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
    }
}
