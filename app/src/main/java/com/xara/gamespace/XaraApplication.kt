package com.xara.gamespace

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.example.R
import com.xara.gamespace.data.local.AppDatabase
import com.xara.gamespace.data.preferences.XaraPreferences
import com.xara.gamespace.data.repository.XaraRepository
import com.xara.gamespace.engine.GameCompilerEngine
import com.xara.gamespace.engine.GraphicsEngine
import com.xara.gamespace.engine.SessionManager
import com.xara.gamespace.engine.ShizukuEngine
import com.xara.gamespace.engine.XaraBoostEngine
import com.xara.gamespace.games.InstalledAppsScanner
import com.xara.gamespace.monitor.DeviceTelemetryMonitor
import kotlinx.coroutines.flow.MutableStateFlow

class XaraAppContainer(context: Context) {
    private val database = AppDatabase.getInstance(context)
    private val preferences = XaraPreferences(context)
    val repository = XaraRepository(database.xaraDao(), preferences)
    val telemetryMonitor = DeviceTelemetryMonitor(context)
    val appsScanner = InstalledAppsScanner(context)
    val shizukuEngine = ShizukuEngine(context)
    val boostEngine = XaraBoostEngine(context, telemetryMonitor, repository)
    val graphicsEngine = GraphicsEngine(context, shizukuEngine)
    val compilerEngine = GameCompilerEngine(shizukuEngine, repository)
    val sessionManager = SessionManager(context, telemetryMonitor, repository)

    val isFloatingPanelRunning = MutableStateFlow(false)
    val pendingDeepLinkRoute = MutableStateFlow<String?>(null)
}

class XaraApplication : Application() {

    lateinit var container: XaraAppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = XaraAppContainer(this)
        container.shizukuEngine.initialize()
        createNotificationChannels()
    }

    override fun onTerminate() {
        super.onTerminate()
        container.shizukuEngine.destroy()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java) ?: return
            val channel = NotificationChannel(
                CHANNEL_SESSION_HUD,
                getString(R.string.notification_channel_session),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.notification_channel_session_desc)
                setShowBadge(false)
            }
            manager.createNotificationChannel(channel)
        }
    }

    companion object {
        const val CHANNEL_SESSION_HUD = "xara_session_hud"

        fun from(context: Context): XaraApplication {
            return context.applicationContext as XaraApplication
        }
    }
}
