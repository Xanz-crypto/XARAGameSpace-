package com.xara.gamespace.services

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.xara.gamespace.XaraApplication
import com.xara.gamespace.domain.model.BoostMode
import com.xara.gamespace.domain.model.FloatingPositionPreset
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
    private lateinit var rootContainer: LinearLayout
    private lateinit var collapsedOrb: TextView
    private lateinit var expandedPanel: LinearLayout
    private lateinit var layoutParams: WindowManager.LayoutParams

    private lateinit var tvRam: TextView
    private lateinit var tvCpu: TextView
    private lateinit var tvFps: TextView
    private lateinit var tvTemp: TextView
    private lateinit var tvPing: TextView
    private lateinit var tvBoostStatus: TextView
    private lateinit var btnBoost: TextView
    private lateinit var btnTransparent: TextView

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var telemetryJob: Job? = null
    private var isExpanded = false
    private var isTransparentMode = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        if (!Settings.canDrawOverlays(this)) {
            stopSelf()
            return
        }
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        startAsForeground()
        buildOverlayView()
        observeLiveTelemetry()
        XaraApplication.from(this).container.isFloatingPanelRunning.value = true
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!Settings.canDrawOverlays(this)) {
            stopSelf()
            return START_NOT_STICKY
        }
        val transparent = intent?.getBooleanExtra(EXTRA_TRANSPARENT, false)
        val presetName = intent?.getStringExtra(EXTRA_POSITION_PRESET)
        if (transparent != null) {
            isTransparentMode = transparent
            applyTransparency()
        }
        if (presetName != null && ::layoutParams.isInitialized) {
            applyPositionPreset(
                runCatching { FloatingPositionPreset.valueOf(presetName) }
                    .getOrDefault(FloatingPositionPreset.TOP_LEFT)
            )
        }
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        telemetryJob?.cancel()
        serviceScope.cancel()
        if (::rootContainer.isInitialized && rootContainer.isAttachedToWindow) {
            runCatching { windowManager.removeView(rootContainer) }
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
            .setContentText("HUD active • Tap to return to XARAGameSpace")
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
        startForeground(NOTIFICATION_ID, notification)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun buildOverlayView() {
        val overlayType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            overlayType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = dp(16)
            y = dp(140)
        }

        rootContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        collapsedOrb = TextView(this).apply {
            text = "XARA"
            setTextColor(Color.parseColor("#00F0FF"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setPadding(dp(14), dp(10), dp(14), dp(10))
            background = createPanelBackground(alpha = 235, radiusDp = 24, strokeColor = "#00F0FF")
        }

        expandedPanel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            visibility = View.GONE
            setPadding(dp(14), dp(12), dp(14), dp(12))
            background = createPanelBackground(alpha = 235, radiusDp = 18, strokeColor = "#7C3AED")
            minimumWidth = dp(240)
        }

        val headerRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val title = TextView(this).apply {
            text = "XARA HUD"
            setTextColor(Color.parseColor("#00F0FF"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        btnTransparent = createChipButton("ALPHA") {
            isTransparentMode = !isTransparentMode
            applyTransparency()
        }
        val btnMin = createChipButton("—") { toggleExpanded(false) }
        val btnClose = createChipButton("✕") { stopSelf() }

        headerRow.addView(title)
        headerRow.addView(btnTransparent)
        headerRow.addView(space(6))
        headerRow.addView(btnMin)
        headerRow.addView(space(6))
        headerRow.addView(btnClose)
        expandedPanel.addView(headerRow)
        expandedPanel.addView(space(8))

        tvRam = createMetricLine("RAM: --")
        tvCpu = createMetricLine("CPU: N/A")
        tvFps = createMetricLine("FPS / Hz: --")
        tvTemp = createMetricLine("TEMP: --")
        tvPing = createMetricLine("PING: N/A")

        expandedPanel.addView(tvRam)
        expandedPanel.addView(tvCpu)
        expandedPanel.addView(tvFps)
        expandedPanel.addView(tvTemp)
        expandedPanel.addView(tvPing)
        expandedPanel.addView(space(8))

        tvBoostStatus = TextView(this).apply {
            text = "Ready for XARA Boost"
            setTextColor(Color.parseColor("#94A3B8"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 10f)
            visibility = View.GONE
        }
        expandedPanel.addView(tvBoostStatus)

        btnBoost = TextView(this).apply {
            text = "⚡ XARA BOOST"
            setTextColor(Color.parseColor("#07090F"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setPadding(dp(12), dp(9), dp(12), dp(9))
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#00F0FF"))
                cornerRadius = dp(12).toFloat()
            }
            setOnClickListener { triggerQuickBoost() }
        }
        expandedPanel.addView(btnBoost)
        expandedPanel.addView(space(8))

        val shortcutsRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }
        shortcutsRow.addView(createShortcutButton("GFX") { openRoute("graphics") })
        shortcutsRow.addView(space(6))
        shortcutsRow.addView(createShortcutButton("TOUCH") { openRoute("touch") })
        shortcutsRow.addView(space(6))
        shortcutsRow.addView(createShortcutButton("MACRO") { openRoute("macro") })
        expandedPanel.addView(shortcutsRow)

        attachDragListener(collapsedOrb) { toggleExpanded(true) }
        attachDragListener(expandedPanel) { }

        rootContainer.addView(collapsedOrb)
        rootContainer.addView(expandedPanel)
        windowManager.addView(rootContainer, layoutParams)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun attachDragListener(view: View, onClick: () -> Unit) {
        var initX = 0
        var initY = 0
        var touchX = 0f
        var touchY = 0f
        var moved = false

        view.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initX = layoutParams.x
                    initY = layoutParams.y
                    touchX = event.rawX
                    touchY = event.rawY
                    moved = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - touchX).toInt()
                    val dy = (event.rawY - touchY).toInt()
                    if (kotlin.math.abs(dx) > 8 || kotlin.math.abs(dy) > 8) {
                        moved = true
                    }
                    layoutParams.x = initX + dx
                    layoutParams.y = initY + dy
                    runCatching { windowManager.updateViewLayout(rootContainer, layoutParams) }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (!moved) onClick()
                    true
                }
                else -> false
            }
        }
    }

    private fun toggleExpanded(expand: Boolean) {
        isExpanded = expand
        collapsedOrb.visibility = if (expand) View.GONE else View.VISIBLE
        expandedPanel.visibility = if (expand) View.VISIBLE else View.GONE
        applyTransparency()
    }

    private fun applyTransparency() {
        val alpha = if (isTransparentMode) 0.58f else 0.95f
        if (::rootContainer.isInitialized) {
            rootContainer.animate().alpha(alpha).setDuration(180).start()
        }
    }

    private fun applyPositionPreset(preset: FloatingPositionPreset) {
        val width = resources.displayMetrics.widthPixels
        when (preset) {
            FloatingPositionPreset.TOP_LEFT -> {
                layoutParams.x = dp(16)
                layoutParams.y = dp(120)
            }
            FloatingPositionPreset.TOP_RIGHT -> {
                layoutParams.x = (width - dp(120)).coerceAtLeast(dp(16))
                layoutParams.y = dp(120)
            }
            FloatingPositionPreset.CENTER_LEFT -> {
                layoutParams.x = dp(16)
                layoutParams.y = resources.displayMetrics.heightPixels / 2 - dp(60)
            }
            FloatingPositionPreset.CUSTOM -> Unit
        }
        runCatching { windowManager.updateViewLayout(rootContainer, layoutParams) }
    }

    private fun observeLiveTelemetry() {
        val container = XaraApplication.from(this).container
        telemetryJob = serviceScope.launch {
            container.telemetryMonitor.observeTelemetry(1500L).collect { t ->
                tvRam.text = "RAM: ${t.usedRamMb}/${t.totalRamMb} MB (${t.ramUsagePercent}%)"
                tvCpu.text = if (t.cpuUsagePercent != null) {
                    String.format(Locale.US, "CPU: %.0f%% (%d cores)", t.cpuUsagePercent, t.cpuCores)
                } else {
                    "CPU: N/A (${t.cpuCores} cores)"
                }
                val fpsPart = t.hudFps?.let { "${it.toInt()} FPS" } ?: "N/A"
                tvFps.text = "FPS/Hz: $fpsPart • ${t.refreshRateHz.toInt()} Hz"
                tvTemp.text = String.format(Locale.US, "TEMP: %.1f°C • %d%%", t.batteryTempCelsius, t.batteryPercent)
                tvPing.text = t.networkLatencyMs?.let { "PING: ${it} ms (${t.networkType})" }
                    ?: "PING: N/A (${t.networkType})"
            }
        }
    }

    private fun triggerQuickBoost() {
        val container = XaraApplication.from(this).container
        btnBoost.text = "BOOSTING..."
        btnBoost.isEnabled = false
        serviceScope.launch {
            val mode = container.repository.settingsFlow.first().boostMode
            val result = container.boostEngine.runBoost(
                mode = mode.takeIf { it != BoostMode.BATTERY_SAVER } ?: BoostMode.PERFORMANCE,
                performCleanup = true
            ) { _, step ->
                tvBoostStatus.visibility = View.VISIBLE
                tvBoostStatus.text = step
            }
            tvBoostStatus.visibility = View.VISIBLE
            tvBoostStatus.text = "Cleared ${result.backgroundProcessesCleared} bg • Freed ${result.freedRamMb} MB"
            btnBoost.text = "⚡ XARA BOOST"
            btnBoost.isEnabled = true
        }
    }

    private fun openRoute(route: String) {
        XaraApplication.from(this).container.pendingDeepLinkRoute.value = route
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            putExtra(EXTRA_DEEP_LINK_ROUTE, route)
        }
        startActivity(intent)
        toggleExpanded(false)
    }

    private fun createMetricLine(initial: String): TextView = TextView(this).apply {
        text = initial
        setTextColor(Color.parseColor("#E2E8F0"))
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
        setPadding(0, dp(2), 0, dp(2))
    }

    private fun createChipButton(label: String, onClick: () -> Unit): TextView = TextView(this).apply {
        text = label
        setTextColor(Color.parseColor("#E2E8F0"))
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 10f)
        setPadding(dp(7), dp(4), dp(7), dp(4))
        background = createPanelBackground(alpha = 200, radiusDp = 8, strokeColor = "#283252")
        setOnClickListener { onClick() }
    }

    private fun createShortcutButton(label: String, onClick: () -> Unit): TextView = TextView(this).apply {
        text = label
        setTextColor(Color.parseColor("#00F0FF"))
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 10f)
        typeface = android.graphics.Typeface.DEFAULT_BOLD
        gravity = Gravity.CENTER
        setPadding(dp(10), dp(6), dp(10), dp(6))
        background = createPanelBackground(alpha = 210, radiusDp = 10, strokeColor = "#283252")
        setOnClickListener { onClick() }
    }

    private fun createPanelBackground(alpha: Int, radiusDp: Int, strokeColor: String): GradientDrawable {
        return GradientDrawable().apply {
            setColor(Color.argb(alpha, 12, 16, 28))
            cornerRadius = dp(radiusDp).toFloat()
            setStroke(dp(1), Color.parseColor(strokeColor))
        }
    }

    private fun space(heightDp: Int): View = View(this).apply {
        layoutParams = LinearLayout.LayoutParams(dp(heightDp), dp(heightDp))
    }

    private fun dp(value: Int): Int =
        TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            value.toFloat(),
            resources.displayMetrics
        ).toInt()

    companion object {
        const val EXTRA_TRANSPARENT = "extra_transparent"
        const val EXTRA_POSITION_PRESET = "extra_position_preset"
        const val EXTRA_DEEP_LINK_ROUTE = "extra_deep_link_route"
        private const val NOTIFICATION_ID = 2002
    }
}
