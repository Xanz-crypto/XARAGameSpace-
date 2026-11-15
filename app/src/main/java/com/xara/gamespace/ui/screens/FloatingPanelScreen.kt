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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
    var hasUsageAccess by remember { mutableStateOf(PermissionHelper.hasUsageStatsPermission(context)) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasOverlayPerm = PermissionHelper.canDrawOverlays(context)
                hasUsageAccess = PermissionHelper.hasUsageStatsPermission(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        XaraTopBar(
            title = "Floating Game Panel",
            subtitle = "Panel sisi kanan dengan tombol stik gamepad",
            onBack = onBack
        )
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
                            Text("Izin Overlay (Tampil di Atas Aplikasi)", style = MaterialTheme.typography.titleMedium, color = XaraTextPrimary)
                            Text("Wajib aktif agar tombol stik & panel tampil saat bermain game", style = MaterialTheme.typography.bodyMedium, color = XaraTextSecondary)
                        }
                        Spacer(Modifier.width(8.dp))
                        StatusPillBadge(
                            text = if (hasOverlayPerm) "AKTIF" else "PERLU IZIN",
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
                            Text("Izinkan Tampil di Atas Aplikasi", style = MaterialTheme.typography.labelLarge)
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
                                contentDescription = if (isRunning) "Tutup Floating Panel" else "Buka Floating Panel"
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = if (isRunning) "Tutup Floating Panel" else "Aktifkan Floating Panel Sekarang",
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                }
            }

            item {
                XaraGlassCard {
                    XaraSectionHeader(
                        title = "Otomatis Saat Buka Game",
                        subtitle = "Munculkan tombol stik & panel secara otomatis"
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Auto-Open Floating Panel", style = MaterialTheme.typography.titleMedium, color = XaraTextPrimary)
                            Text(
                                "Otomatis aktif saat game dibuka dari XARAGameSpace atau beranda HP",
                                style = MaterialTheme.typography.bodyMedium,
                                color = XaraTextSecondary
                            )
                        }
                        Switch(
                            checked = settings.autoFloatingPanelOnGameLaunch,
                            onCheckedChange = { viewModel.setAutoFloatingOnGameLaunch(it) },
                            colors = SwitchDefaults.colors(checkedThumbColor = xaraColors.accentPrimary)
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Izin Usage Access (Deteksi Game Luar)", style = MaterialTheme.typography.bodyMedium, color = XaraTextSecondary)
                        StatusPillBadge(
                            text = if (hasUsageAccess) "SIAP" else "OPSIONAL",
                            color = if (hasUsageAccess) xaraColors.thermalCool else xaraColors.thermalWarm
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = { viewModel.startBackgroundGameWatcher() },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.AutoAwesome, contentDescription = "Aktifkan Auto Watcher")
                        Spacer(Modifier.width(8.dp))
                        Text(
                            if (hasUsageAccess) "Aktifkan Background Game Watcher" else "Berikan Izin Usage Access",
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }

            item {
                XaraGlassCard {
                    XaraSectionHeader(title = "Tampilan & Posisi Panel")
                    Spacer(Modifier.height(10.dp))
                    Text("Posisi Dock Panel & Tombol Stik", style = MaterialTheme.typography.titleSmall, color = XaraTextSecondary)
                    Spacer(Modifier.height(6.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = settings.floatingSideRight,
                            onClick = { viewModel.updateFloatingSideRight(true) },
                            label = { Text("Sisi Kanan (Rekomendasi)") }
                        )
                        FilterChip(
                            selected = !settings.floatingSideRight,
                            onClick = { viewModel.updateFloatingSideRight(false) },
                            label = { Text("Sisi Kiri") }
                        )
                    }

                    Spacer(Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Mini Telemetry Bar Atas", style = MaterialTheme.typography.titleMedium, color = XaraTextPrimary)
                            Text("Tampilkan FPS, CPU & Suhu ringkas di atas layar", style = MaterialTheme.typography.bodyMedium, color = XaraTextSecondary)
                        }
                        Switch(
                            checked = settings.floatingMiniHudEnabled,
                            onCheckedChange = { viewModel.updateFloatingMiniHud(it) },
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
                            Text("Mode Transparan", style = MaterialTheme.typography.titleMedium, color = XaraTextPrimary)
                            Text("Membuat panel lebih tembus pandang saat dibuka", style = MaterialTheme.typography.bodyMedium, color = XaraTextSecondary)
                        }
                        Switch(
                            checked = settings.floatingTransparentMode,
                            onCheckedChange = { viewModel.updateFloatingTransparency(it) },
                            colors = SwitchDefaults.colors(checkedThumbColor = xaraColors.accentPrimary)
                        )
                    }
                }
            }

            item {
                XaraGlassCard {
                    XaraSectionHeader(title = "Menu di Dalam Floating Panel")
                    Spacer(Modifier.height(8.dp))
                    val items = listOf(
                        "Monitor — Live FPS/Hz, CPU, RAM, Suhu Baterai & Ping",
                        "Boost — Pembersihan RAM latar cepat tanpa menutup game",
                        "Sensi — Kalibrasi X/Y, lock skala & pilihan area sentuh",
                        "Crosshair — Overlay bidik Titik / Plus / T dengan warna & ukuran",
                        "Macro — Pengaturan trigger size, interval & lama tekan",
                        "Burik — Profil tekstur Off, Soft, Pixel & Balanced",
                        "Kontrol — Volume, layar tetap menyala, pindah sisi & tutup sesi"
                    )
                    items.forEach { line ->
                        Text(
                            text = "• $line",
                            style = MaterialTheme.typography.bodyMedium,
                            color = XaraTextPrimary
                        )
                        Spacer(Modifier.height(4.dp))
                    }
                }
            }
        }
    }
}
