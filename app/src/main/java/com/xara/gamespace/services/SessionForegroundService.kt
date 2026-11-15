package com.xara.gamespace.services

import android.app.PendingIntent
import android.app.Service
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.provider.Settings
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.xara.gamespace.XaraApplication
import com.xara.gamespace.permissions.PermissionHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class SessionForegroundService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var watcherJob: Job? = null
    private var lastAutoTriggeredPkg: String? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val gameName = intent?.getStringExtra(EXTRA_GAME_NAME) ?: "Game Auto-Watcher"
        val watchGames = intent?.getBooleanExtra(EXTRA_WATCH_GAMES, false) ?: false

        val openIntent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            100,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val contentText = if (watchGames) {
            "Otomatis membuka Floating Panel saat game dijalankan"
        } else {
            "Monitoring $gameName telemetry & thermal headroom"
        }

        val notification = NotificationCompat.Builder(this, XaraApplication.CHANNEL_SESSION_HUD)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("XARAGameSpace Active")
            .setContentText(contentText)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        startForeground(NOTIFICATION_ID, notification)

        if (watchGames || watcherJob == null) {
            startForegroundGameWatcher()
        }
        return START_STICKY
    }

    private fun startForegroundGameWatcher() {
        watcherJob?.cancel()
        watcherJob = serviceScope.launch {
            val container = XaraApplication.from(this@SessionForegroundService).container
            while (isActive) {
                delay(2000L)
                val settings = runCatching { container.repository.settingsFlow.first() }.getOrNull()
                    ?: continue
                if (!settings.autoFloatingPanelOnGameLaunch) continue
                if (!Settings.canDrawOverlays(this@SessionForegroundService)) continue
                if (!PermissionHelper.hasUsageStatsPermission(this@SessionForegroundService)) continue

                val fgPkg = detectForegroundPackage() ?: continue
                if (fgPkg == packageName) {
                    lastAutoTriggeredPkg = null
                    continue
                }

                val spacePackages = runCatching {
                    container.repository.gameProfilesFlow.first()
                        .filter { it.isAddedToSpace }
                        .associateBy { it.packageName }
                }.getOrDefault(emptyMap())

                val matchedGame = spacePackages[fgPkg]
                if (matchedGame != null &&
                    !container.isFloatingPanelRunning.value &&
                    lastAutoTriggeredPkg != fgPkg
                ) {
                    lastAutoTriggeredPkg = fgPkg
                    val panelIntent = Intent(
                        this@SessionForegroundService,
                        FloatingPanelService::class.java
                    ).apply {
                        putExtra(FloatingPanelService.EXTRA_TRANSPARENT, settings.floatingTransparentMode)
                        putExtra(FloatingPanelService.EXTRA_SIDE_RIGHT, settings.floatingSideRight)
                        putExtra(FloatingPanelService.EXTRA_MINI_HUD, settings.floatingMiniHudEnabled)
                        putExtra(FloatingPanelService.EXTRA_GAME_NAME, matchedGame.appName)
                    }
                    startForegroundService(panelIntent)
                }
            }
        }
    }

    private fun detectForegroundPackage(): String? {
        return runCatching {
            val usm = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
            val end = System.currentTimeMillis()
            val begin = end - 8_000L
            val events = usm.queryEvents(begin, end)
            val event = UsageEvents.Event()
            var lastPkg: String? = null
            while (events.hasNextEvent()) {
                events.getNextEvent(event)
                if (event.eventType == UsageEvents.Event.ACTIVITY_RESUMED) {
                    lastPkg = event.packageName
                }
            }
            lastPkg
        }.getOrNull()
    }

    override fun onDestroy() {
        super.onDestroy()
        watcherJob?.cancel()
        serviceScope.cancel()
    }

    companion object {
        const val EXTRA_GAME_NAME = "extra_game_name"
        const val EXTRA_WATCH_GAMES = "extra_watch_games"
        private const val NOTIFICATION_ID = 2001
    }
}
