package com.xara.gamespace.services

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.xara.gamespace.XaraApplication
import com.xara.gamespace.data.local.TouchProfileEntity
import com.xara.gamespace.domain.model.BoostMode
import com.xara.gamespace.domain.model.GraphicsDriverOption
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Locale

class FloatingPanelService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var theme: FloatingOverlayTheme
    private lateinit var triggerParams: WindowManager.LayoutParams
    private lateinit var panelParams: WindowManager.LayoutParams
    private lateinit var miniHudParams: WindowManager.LayoutParams
    private lateinit var crosshairParams: WindowManager.LayoutParams

    private lateinit var triggerButton: FrameLayout
    private lateinit var drawerContainer: LinearLayout
    private lateinit var miniHudPill: TextView
    private lateinit var crosshairView: CrosshairOverlayView
    private lateinit var tabRefs: FloatingPanelTabRefs

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var telemetryJob: Job? = null

    private var isDrawerOpen = false
    private var isTransparentMode = false
    private var isRightSide = true
    private var isMiniHudEnabled = true
    private var keepScreenOn = true
    private var currentTabIndex = 0

    private val sidebarTabs = mutableListOf<LinearLayout>()
    private val tabContentViews = mutableListOf<View>()

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        if (!Settings.canDrawOverlays(this)) {
            stopSelf()
            return
        }
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        theme = FloatingOverlayTheme(this)
        startAsForeground()
        buildAllOverlays()
        observeLiveTelemetry()
        XaraApplication.from(this).container.isFloatingPanelRunning.value = true
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!Settings.canDrawOverlays(this)) {
            stopSelf()
            return START_NOT_STICKY
        }
        intent?.let {
            if (it.hasExtra(EXTRA_TRANSPARENT)) {
                isTransparentMode = it.getBooleanExtra(EXTRA_TRANSPARENT, false)
                applyTransparency()
            }
            if (it.hasExtra(EXTRA_SIDE_RIGHT)) {
                isRightSide = it.getBooleanExtra(EXTRA_SIDE_RIGHT, true)
                applySideAlignment()
            }
            if (it.hasExtra(EXTRA_MINI_HUD)) {
                isMiniHudEnabled = it.getBooleanExtra(EXTRA_MINI_HUD, true)
                if (::miniHudPill.isInitialized) {
                    miniHudPill.visibility = if (isMiniHudEnabled) View.VISIBLE else View.GONE
                }
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        telemetryJob?.cancel()
        serviceScope.cancel()
        listOf(
            if (::triggerButton.isInitialized) triggerButton else null,
            if (::drawerContainer.isInitialized) drawerContainer else null,
            if (::miniHudPill.isInitialized) miniHudPill else null,
            if (::crosshairView.isInitialized) crosshairView else null
        ).forEach { v ->
            if (v != null && v.isAttachedToWindow) {
                runCatching { windowManager.removeView(v) }
            }
        }
        XaraApplication.from(this).container.isFloatingPanelRunning.value = false
    }

    private fun startAsForeground() {
        val openIntent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            101,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(this, XaraApplication.CHANNEL_SESSION_HUD)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("XARA Floating Game Panel")
            .setContentText("Ketuk ikon stik untuk membuka panel • XARAGameSpace")
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
        startForeground(NOTIFICATION_ID, notification)
    }

    private fun overlayWindowType(): Int = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
    } else {
        @Suppress("DEPRECATION")
        WindowManager.LayoutParams.TYPE_PHONE
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun buildAllOverlays() {
        val type = overlayWindowType()
        val dm = resources.displayMetrics
        val panelWidth = (dm.widthPixels * 0.78f).toInt().coerceIn(theme.dp(290), theme.dp(410))
        val panelHeight = (dm.heightPixels * 0.76f).toInt().coerceIn(theme.dp(300), theme.dp(480))

        // 1. Crosshair Overlay (Full-screen non-touchable)
        crosshairParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        )
        crosshairView = CrosshairOverlayView(this)
        windowManager.addView(crosshairView, crosshairParams)

        // 2. Top-Center Mini Telemetry Pill (FPS | CPU | TEMP)
        miniHudParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            y = theme.dp(14)
        }
        miniHudPill = TextView(this).apply {
            text = "FPS --  |  CPU --  |  TEMP --"
            typeface = theme.jetBrainsMono
            setTextColor(theme.textPrimary)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 10.5f)
            setPadding(theme.dp(12), theme.dp(5), theme.dp(12), theme.dp(5))
            background = theme.roundedBg(theme.bgPill, 14, theme.strokeGlass)
            visibility = if (isMiniHudEnabled) View.VISIBLE else View.GONE
        }
        attachDrag(miniHudPill, miniHudParams) {}
        windowManager.addView(miniHudPill, miniHudParams)

        // 3. Gamepad Stick Trigger Button (Docked on Right Edge by default)
        triggerParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = (if (isRightSide) Gravity.END else Gravity.START) or Gravity.CENTER_VERTICAL
            x = theme.dp(8)
            y = 0
        }
        triggerButton = FrameLayout(this).apply {
            val size = theme.dp(48)
            layoutParams = WindowManager.LayoutParams(size, size)
            background = theme.roundedBg(theme.bgPanel, 24, theme.strokeAccent, 2)
            setPadding(theme.dp(10), theme.dp(10), theme.dp(10), theme.dp(10))
            val icon = ImageView(this@FloatingPanelService).apply {
                setImageResource(R.drawable.ic_gamepad_stick)
                contentDescription = "Buka XARA Floating Panel"
            }
            addView(icon, FrameLayout.LayoutParams(theme.dp(28), theme.dp(28), Gravity.CENTER))
        }
        attachDrag(triggerButton, triggerParams) { setDrawerOpen(!isDrawerOpen) }
        windowManager.addView(triggerButton, triggerParams)

        // 4. Right-Side Docked 7-Tab Panel
        panelParams = WindowManager.LayoutParams(
            panelWidth,
            panelHeight,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = (if (isRightSide) Gravity.END else Gravity.START) or Gravity.CENTER_VERTICAL
            x = theme.dp(8)
            y = 0
        }

        drawerContainer = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            visibility = View.GONE
            background = theme.roundedBg(theme.bgPanel, 20, theme.strokeGlass, 1)
            clipToPadding = true
        }

        val tabsBuilder = FloatingPanelTabsBuilder(
            context = this,
            theme = theme,
            scope = serviceScope,
            onMiniHudToggle = { show ->
                isMiniHudEnabled = show
                miniHudPill.visibility = if (show) View.VISIBLE else View.GONE
            },
            onBoostClick = { triggerQuickBoost() },
            onCrosshairUpdated = { cfg -> crosshairView.config = cfg },
            onMoveSideClick = {
                isRightSide = !isRightSide
                applySideAlignment()
            },
            onKeepScreenOnToggle = { keep ->
                keepScreenOn = keep
                panelParams.flags = if (keep) {
                    panelParams.flags or WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                } else {
                    panelParams.flags and WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON.inv()
                }
                runCatching { windowManager.updateViewLayout(drawerContainer, panelParams) }
            },
            onOpenAppRoute = { route -> openAppRoute(route) },
            onEndSessionClick = { endSessionAndClose() }
        )

        val (monitorView, refs) = tabsBuilder.buildMonitorTab(isMiniHudEnabled)
        tabRefs = refs

        val container = XaraApplication.from(this).container
        val boostView = tabsBuilder.buildBoostTab(refs)
        val sensiView = tabsBuilder.buildSensiTab { sx, sy, zone ->
            serviceScope.launch {
                container.repository.saveTouchProfile(
                    TouchProfileEntity(
                        name = "Floating Sensi (${sx}%/${sy}%)",
                        gamePackage = container.sessionManager.sessionState.value.activeGamePackage,
                        sensitivityX = 1.0f + sx / 100f,
                        sensitivityY = 1.0f + sy / 100f,
                        touchAreaScale = 1.0f,
                        touchZoneMode = zone.name,
                        isPreset = false
                    )
                )
            }
        }
        val crosshairTabView = tabsBuilder.buildCrosshairTab()
        val macroView = tabsBuilder.buildMacroTab { macro ->
            serviceScope.launch { container.repository.saveMacroConfig(macro) }
        }
        val burikView = tabsBuilder.buildBurikTab(
            initialTexture = com.xara.gamespace.domain.model.TextureOptMode.SHARP_NATIVE
        ) { mode ->
            serviceScope.launch {
                val cur = container.repository.settingsFlow.first()
                container.repository.setGraphicsSettings(
                    driver = cur.selectedGraphicsDriver,
                    profile = cur.selectedGraphicsProfile,
                    textureMode = mode
                )
            }
        }
        val kontrolView = tabsBuilder.buildKontrolTab()

        tabContentViews.clear()
        tabContentViews.addAll(
            listOf(monitorView, boostView, sensiView, crosshairTabView, macroView, burikView, kontrolView)
        )

        // Left Sidebar Rail inside Panel
        val rail = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = theme.roundedBg(theme.bgRail, 18, null)
            setPadding(theme.dp(6), theme.dp(8), theme.dp(6), theme.dp(8))
            layoutParams = LinearLayout.LayoutParams(theme.dp(92), LinearLayout.LayoutParams.MATCH_PARENT)
        }

        val tabSpecs = listOf(
            "Monitor" to android.R.drawable.ic_menu_info_details,
            "Boost" to android.R.drawable.ic_menu_manage,
            "Sensi" to android.R.drawable.ic_menu_compass,
            "Crosshair" to android.R.drawable.ic_menu_mylocation,
            "Macro" to android.R.drawable.ic_menu_send,
            "Burik" to android.R.drawable.ic_menu_gallery,
            "Kontrol" to android.R.drawable.ic_menu_preferences
        )

        val contentScroll = ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f)
            setPadding(theme.dp(12), theme.dp(12), theme.dp(12), theme.dp(12))
            isVerticalScrollBarEnabled = false
        }
        val contentHost = FrameLayout(this)
        contentScroll.addView(contentHost)

        sidebarTabs.clear()
        tabSpecs.forEachIndexed { index, (label, iconRes) ->
            val item = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(theme.dp(8), theme.dp(9), theme.dp(8), theme.dp(9))
                val lp = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = theme.dp(4) }
                layoutParams = lp

                val iv = ImageView(this@FloatingPanelService).apply {
                    setImageResource(iconRes)
                    setColorFilter(theme.textPrimary)
                    layoutParams = LinearLayout.LayoutParams(theme.dp(16), theme.dp(16)).apply {
                        marginEnd = theme.dp(6)
                    }
                }
                val tv = TextView(this@FloatingPanelService).apply {
                    text = label
                    typeface = theme.spaceGrotesk
                    setTypeface(theme.spaceGrotesk, Typeface.BOLD)
                    setTextColor(theme.textPrimary)
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
                    maxLines = 1
                }
                addView(iv)
                addView(tv)
                setOnClickListener { selectTab(index, contentHost) }
            }
            sidebarTabs.add(item)
            rail.addView(item)
        }

        // Minimize / Close Drawer Footer inside Rail
        rail.addView(View(this).apply {
            layoutParams = LinearLayout.LayoutParams(1, 0, 1f)
        })
        rail.addView(
            theme.actionPillButton("Tutup", theme.bgCard, theme.cyan, theme.strokeGlass) {
                setDrawerOpen(false)
            }
        )

        drawerContainer.addView(rail)
        drawerContainer.addView(contentScroll)
        selectTab(0, contentHost)

        windowManager.addView(drawerContainer, panelParams)
        applyTransparency()
    }

    private fun selectTab(index: Int, host: FrameLayout) {
        currentTabIndex = index
        host.removeAllViews()
        host.addView(tabContentViews[index])
        sidebarTabs.forEachIndexed { idx, row ->
            val active = idx == index
            row.background = if (active) {
                theme.roundedBg(Color.parseColor("#2A324B"), 10, theme.cyan, 1)
            } else {
                null
            }
        }
    }

    private fun setDrawerOpen(open: Boolean) {
        isDrawerOpen = open
        if (open) {
            triggerButton.visibility = View.GONE
            drawerContainer.visibility = View.VISIBLE
            drawerContainer.alpha = 0f
            drawerContainer.animate().alpha(if (isTransparentMode) 0.72f else 0.98f).setDuration(180).start()
        } else {
            drawerContainer.visibility = View.GONE
            triggerButton.visibility = View.VISIBLE
        }
    }

    private fun applySideAlignment() {
        val sideGravity = (if (isRightSide) Gravity.END else Gravity.START) or Gravity.CENTER_VERTICAL
        if (::triggerParams.isInitialized) {
            triggerParams.gravity = sideGravity
            triggerParams.x = theme.dp(8)
            runCatching { windowManager.updateViewLayout(triggerButton, triggerParams) }
        }
        if (::panelParams.isInitialized) {
            panelParams.gravity = sideGravity
            panelParams.x = theme.dp(8)
            runCatching { windowManager.updateViewLayout(drawerContainer, panelParams) }
        }
    }

    private fun applyTransparency() {
        val alpha = if (isTransparentMode) 0.70f else 0.97f
        if (::drawerContainer.isInitialized && isDrawerOpen) {
            drawerContainer.animate().alpha(alpha).setDuration(160).start()
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun attachDrag(
        view: View,
        params: WindowManager.LayoutParams,
        onTap: () -> Unit
    ) {
        var initX = 0
        var initY = 0
        var touchX = 0f
        var touchY = 0f
        var moved = false

        view.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initX = params.x
                    initY = params.y
                    touchX = event.rawX
                    touchY = event.rawY
                    moved = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - touchX).toInt()
                    val dy = (event.rawY - touchY).toInt()
                    if (kotlin.math.abs(dx) > 10 || kotlin.math.abs(dy) > 10) moved = true
                    val xSign = if ((params.gravity and Gravity.END) == Gravity.END) -1 else 1
                    params.x = (initX + dx * xSign).coerceAtLeast(0)
                    params.y = initY + dy
                    runCatching { windowManager.updateViewLayout(view, params) }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (!moved) onTap()
                    true
                }
                else -> false
            }
        }
    }

    private fun observeLiveTelemetry() {
        val container = XaraApplication.from(this).container
        telemetryJob = serviceScope.launch {
            container.telemetryMonitor.observeTelemetry(1500L).collect { t ->
                val fpsTxt = t.hudFps?.let { "${it.toInt()}" } ?: "${t.refreshRateHz.toInt()}Hz"
                val cpuTxt = t.cpuUsagePercent?.let { "${it.toInt()}%" } ?: "N/A"
                val tempTxt = String.format(Locale.US, "%.0f°C", t.batteryTempCelsius)

                if (::miniHudPill.isInitialized) {
                    miniHudPill.text = "FPS $fpsTxt  |  CPU $cpuTxt  |  TEMP $tempTxt  |  RAM ${t.ramUsagePercent}%"
                }
                if (::tabRefs.isInitialized) {
                    tabRefs.tvMonitorFps.text = "FPS / Hz : $fpsTxt (${t.refreshRateHz.toInt()} Hz)"
                    tabRefs.tvMonitorCpu.text = "CPU Load : $cpuTxt (${t.cpuCores} Cores)"
                    tabRefs.tvMonitorRam.text = "RAM Used : ${t.usedRamMb}/${t.totalRamMb} MB (${t.ramUsagePercent}%)"
                    tabRefs.tvMonitorTemp.text = String.format(Locale.US, "Battery  : %.1f°C • %d%%", t.batteryTempCelsius, t.batteryPercent)
                    tabRefs.tvMonitorPing.text = t.networkLatencyMs?.let { "Net Ping : $it ms (${t.networkType})" }
                        ?: "Net Ping : N/A (${t.networkType})"
                }
            }
        }
    }

    private fun triggerQuickBoost() {
        val container = XaraApplication.from(this).container
        tabRefs.btnBoostAction.text = "Mengoptimalkan..."
        tabRefs.btnBoostAction.isEnabled = false
        serviceScope.launch {
            val mode = container.repository.settingsFlow.first().boostMode
            val result = container.boostEngine.runBoost(
                mode = mode.takeIf { it != BoostMode.BATTERY_SAVER } ?: BoostMode.PERFORMANCE,
                performCleanup = true
            ) { _, step ->
                tabRefs.tvBoostResult.text = step
            }
            tabRefs.tvBoostResult.text =
                "Selesai! ${result.backgroundProcessesCleared} aplikasi latar dibersihkan • RAM dibebaskan: ${result.freedRamMb} MB (${result.beforeUsedRamMb} → ${result.afterUsedRamMb} MB)"
            tabRefs.btnBoostAction.text = "⚡ Boost Sekarang"
            tabRefs.btnBoostAction.isEnabled = true
        }
    }

    private fun openAppRoute(route: String?) {
        val container = XaraApplication.from(this).container
        if (route != null) {
            container.pendingDeepLinkRoute.value = route
        }
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            if (route != null) putExtra(EXTRA_DEEP_LINK_ROUTE, route)
        }
        startActivity(intent)
        setDrawerOpen(false)
    }

    private fun endSessionAndClose() {
        val container = XaraApplication.from(this).container
        serviceScope.launch {
            if (container.sessionManager.sessionState.value.isRunning) {
                container.sessionManager.stopSession(true)
            }
            stopSelf()
        }
    }

    companion object {
        const val EXTRA_TRANSPARENT = "extra_transparent"
        const val EXTRA_SIDE_RIGHT = "extra_side_right"
        const val EXTRA_MINI_HUD = "extra_mini_hud"
        const val EXTRA_GAME_NAME = "extra_game_name"
        const val EXTRA_DEEP_LINK_ROUTE = "extra_deep_link_route"
        private const val NOTIFICATION_ID = 2002
    }
}
