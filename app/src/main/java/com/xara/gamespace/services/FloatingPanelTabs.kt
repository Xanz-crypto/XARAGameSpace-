package com.xara.gamespace.services

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.media.AudioManager
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import com.xara.gamespace.data.local.MacroConfigEntity
import com.xara.gamespace.data.local.TouchProfileEntity
import com.xara.gamespace.domain.model.BoostMode
import com.xara.gamespace.domain.model.GraphicsDriverOption
import com.xara.gamespace.domain.model.TextureOptMode
import com.xara.gamespace.domain.model.TouchZoneMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.util.Locale

class FloatingPanelTabRefs(
    val tvMonitorRam: TextView,
    val tvMonitorCpu: TextView,
    val tvMonitorFps: TextView,
    val tvMonitorTemp: TextView,
    val tvMonitorPing: TextView,
    val tvBoostResult: TextView,
    val btnBoostAction: TextView
)

class FloatingPanelTabsBuilder(
    private val context: Context,
    private val theme: FloatingOverlayTheme,
    private val scope: CoroutineScope,
    private val onMiniHudToggle: (Boolean) -> Unit,
    private val onBoostClick: () -> Unit,
    private val onCrosshairUpdated: (CrosshairConfig) -> Unit,
    private val onMoveSideClick: () -> Unit,
    private val onKeepScreenOnToggle: (Boolean) -> Unit,
    private val onOpenAppRoute: (String?) -> Unit,
    private val onEndSessionClick: () -> Unit
) {
    private var crosshairConfig = CrosshairConfig()
    private var sensEnabled = true
    private var sensLocked = false
    private var sensXPercent = 0
    private var sensYPercent = 0
    private var sensArea = TouchZoneMode.FULL_SCREEN

    private var macroEnabled = false
    private var macroTriggerSize = 8
    private var macroIntervalMs = 150
    private var macroHoldMs = 30

    fun buildMonitorTab(
        initialMiniHud: Boolean
    ): Pair<LinearLayout, FloatingPanelTabRefs> {
        val root = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
        root.addView(theme.titleText("Monitor"))
        root.addView(theme.space(8))

        val (miniRow, _) = theme.switchRow(
            label = "Mini Telemetry Bar",
            subtitle = "Tampilkan bar FPS | CPU | TEMP di atas layar",
            initial = initialMiniHud,
            onToggle = onMiniHudToggle
        )
        root.addView(miniRow)
        root.addView(theme.space(8))

        val metricsCard = theme.cardContainer()
        val tvFps = theme.monoText("FPS / Hz : --", theme.cyan, 12f)
        val tvCpu = theme.monoText("CPU Load : N/A", theme.textPrimary, 12f)
        val tvRam = theme.monoText("RAM Used : --", theme.textPrimary, 12f)
        val tvTemp = theme.monoText("Battery  : --", theme.warm, 12f)
        val tvPing = theme.monoText("Net Ping : N/A", theme.green, 12f)
        listOf(tvFps, tvCpu, tvRam, tvTemp, tvPing).forEachIndexed { idx, tv ->
            tv.setPadding(0, theme.dp(3), 0, theme.dp(3))
            metricsCard.addView(tv)
            if (idx < 4) {
                metricsCard.addView(View(context).apply {
                    setBackgroundColor(Color.parseColor("#1E293B"))
                    layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, theme.dp(1))
                })
            }
        }
        root.addView(metricsCard)
        root.addView(theme.space(8))

        val shortcuts = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL }
        val btnGfx = theme.actionPillButton("Graphics") { onOpenAppRoute("graphics") }.apply {
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        val btnTouch = theme.actionPillButton("Touch") { onOpenAppRoute("touch") }.apply {
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        val btnMacro = theme.actionPillButton("Macro") { onOpenAppRoute("macro") }.apply {
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        shortcuts.addView(btnGfx)
        shortcuts.addView(theme.space(6))
        shortcuts.addView(btnTouch)
        shortcuts.addView(theme.space(6))
        shortcuts.addView(btnMacro)
        root.addView(shortcuts)

        val boostViews = buildBoostPlaceholderRefs()
        val refs = FloatingPanelTabRefs(
            tvMonitorRam = tvRam,
            tvMonitorCpu = tvCpu,
            tvMonitorFps = tvFps,
            tvMonitorTemp = tvTemp,
            tvMonitorPing = tvPing,
            tvBoostResult = boostViews.first,
            btnBoostAction = boostViews.second
        )
        return root to refs
    }

    private fun buildBoostPlaceholderRefs(): Pair<TextView, TextView> {
        val tvResult = theme.bodyText("Siap melakukan optimasi RAM latar.", theme.textSecondary, 11.5f)
        val btnBoost = theme.actionPillButton(
            label = "⚡ Boost Sekarang",
            fillColor = Color.WHITE,
            textColor = Color.parseColor("#0B0E17"),
            strokeColor = null,
            onClick = onBoostClick
        )
        return tvResult to btnBoost
    }

    fun buildBoostTab(refs: FloatingPanelTabRefs): LinearLayout {
        val root = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
        root.addView(theme.titleText("Boost"))
        root.addView(theme.space(6))
        root.addView(
            theme.bodyText(
                "Menutup aplikasi yang berjalan di latar secara aman. Game kamu tetap terbuka.",
                theme.textSecondary,
                12f
            )
        )
        root.addView(theme.space(12))
        root.addView(refs.btnBoostAction)
        root.addView(theme.space(10))
        val resCard = theme.cardContainer()
        resCard.addView(refs.tvBoostResult)
        root.addView(resCard)
        return root
    }

    fun buildSensiTab(onSaveSensi: (Int, Int, TouchZoneMode) -> Unit): LinearLayout {
        val root = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
        root.addView(theme.titleText("Sensi"))
        root.addView(theme.space(8))

        val (swRow, _) = theme.switchRow("Sensitivity", "Profil kalibrasi sentuhan", sensEnabled) {
            sensEnabled = it
        }
        root.addView(swRow)
        root.addView(theme.space(8))

        val summaryCard = theme.cardContainer().apply { orientation = LinearLayout.HORIZONTAL }
        val colX = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        val tvXBig = theme.titleText("0%").apply {
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f)
            setTextColor(theme.cyan)
        }
        colX.addView(theme.bodyText("X AXIS", theme.textSecondary, 10f))
        colX.addView(tvXBig)

        val colY = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        val tvYBig = theme.titleText("0%").apply {
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f)
            setTextColor(theme.violet)
        }
        colY.addView(theme.bodyText("Y AXIS", theme.textSecondary, 10f))
        colY.addView(tvYBig)
        summaryCard.addView(colX)
        summaryCard.addView(colY)
        root.addView(summaryCard)
        root.addView(theme.space(8))

        val (lockRow, _) = theme.switchRow("Samakan X dan Y", null, sensLocked) { sensLocked = it }
        root.addView(lockRow)
        root.addView(theme.space(8))

        val (xCard, _) = theme.stepperSliderCard("Horizontal (X)", 100, sensXPercent, { "$it%" }) { v ->
            sensXPercent = v
            tvXBig.text = "$v%"
            if (sensLocked) {
                sensYPercent = v
                tvYBig.text = "$v%"
            }
        }
        root.addView(xCard)
        root.addView(theme.space(8))

        val (yCard, _) = theme.stepperSliderCard("Vertikal (Y)", 100, sensYPercent, { "$it%" }) { v ->
            sensYPercent = v
            tvYBig.text = "$v%"
            if (sensLocked) {
                sensXPercent = v
                tvXBig.text = "$v%"
            }
        }
        root.addView(yCard)
        root.addView(theme.space(8))

        root.addView(theme.bodyText("Area Sentuh", theme.textSecondary, 11.5f))
        root.addView(theme.space(4))
        val areaRow = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL }
        val areaOptions = listOf(
            "Semua" to TouchZoneMode.FULL_SCREEN,
            "Kiri" to TouchZoneMode.LEFT_MOVE,
            "Kanan" to TouchZoneMode.RIGHT_AIM
        )
        val areaButtons = mutableListOf<Pair<TouchZoneMode, TextView>>()
        areaOptions.forEachIndexed { idx, (label, zone) ->
            val btn = theme.actionPillButton(
                label = label,
                fillColor = if (sensArea == zone) Color.WHITE else theme.bgCard,
                textColor = if (sensArea == zone) Color.parseColor("#0B0E17") else theme.textPrimary
            ) {
                sensArea = zone
                areaButtons.forEach { (z, tv) ->
                    val active = z == sensArea
                    tv.background = theme.roundedBg(
                        if (active) Color.WHITE else theme.bgCard,
                        20,
                        theme.strokeGlass
                    )
                    tv.setTextColor(if (active) Color.parseColor("#0B0E17") else theme.textPrimary)
                }
            }.apply {
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            areaButtons.add(zone to btn)
            areaRow.addView(btn)
            if (idx < areaOptions.lastIndex) areaRow.addView(theme.space(6))
        }
        root.addView(areaRow)
        root.addView(theme.space(8))

        val actionRow = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL }
        val btnReset = theme.actionPillButton("Reset to 0%") {
            sensXPercent = 0
            sensYPercent = 0
            tvXBig.text = "0%"
            tvYBig.text = "0%"
            onSaveSensi(0, 0, sensArea)
        }.apply { layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f) }
        val btnSave = theme.actionPillButton("Simpan Profil", theme.cyan, Color.parseColor("#07090F"), null) {
            onSaveSensi(sensXPercent, sensYPercent, sensArea)
        }.apply { layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f) }
        actionRow.addView(btnReset)
        actionRow.addView(theme.space(6))
        actionRow.addView(btnSave)
        root.addView(actionRow)

        return root
    }

    fun buildCrosshairTab(): LinearLayout {
        val root = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
        root.addView(theme.titleText("Crosshair"))
        root.addView(theme.space(8))

        val (enableRow, _) = theme.switchRow("Tampilkan Crosshair", "Overlay bidik non-sentuh", crosshairConfig.enabled) {
            crosshairConfig = crosshairConfig.copy(enabled = it)
            onCrosshairUpdated(crosshairConfig)
        }
        root.addView(enableRow)
        root.addView(theme.space(8))

        root.addView(theme.bodyText("Bentuk", theme.textSecondary, 11.5f))
        root.addView(theme.space(4))
        val shapeRow = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL }
        val shapeViews = mutableListOf<Pair<CrosshairShape, TextView>>()
        CrosshairShape.entries.forEachIndexed { idx, shape ->
            val btn = theme.actionPillButton(
                label = shape.label,
                fillColor = if (crosshairConfig.shape == shape) theme.cyan else theme.bgCard,
                textColor = if (crosshairConfig.shape == shape) Color.parseColor("#07090F") else theme.textPrimary
            ) {
                crosshairConfig = crosshairConfig.copy(shape = shape)
                onCrosshairUpdated(crosshairConfig)
                shapeViews.forEach { (s, tv) ->
                    val active = s == crosshairConfig.shape
                    tv.background = theme.roundedBg(if (active) theme.cyan else theme.bgCard, 20, theme.strokeGlass)
                    tv.setTextColor(if (active) Color.parseColor("#07090F") else theme.textPrimary)
                }
            }.apply { layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f) }
            shapeViews.add(shape to btn)
            shapeRow.addView(btn)
            if (idx < CrosshairShape.entries.lastIndex) shapeRow.addView(theme.space(6))
        }
        root.addView(shapeRow)
        root.addView(theme.space(8))

        root.addView(theme.bodyText("Warna", theme.textSecondary, 11.5f))
        root.addView(theme.space(4))
        val colorRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val colors = listOf("#00F0FF", "#FF4D6D", "#00E676", "#FFB300", "#FFFFFF", "#A855F7", "#2979FF")
        colors.forEach { hex ->
            val cInt = Color.parseColor(hex)
            val dot = View(context).apply {
                val sz = theme.dp(24)
                layoutParams = LinearLayout.LayoutParams(sz, sz).apply {
                    marginEnd = theme.dp(6)
                }
                background = theme.roundedBg(cInt, 12, Color.WHITE, 1)
                setOnClickListener {
                    crosshairConfig = crosshairConfig.copy(colorInt = cInt)
                    onCrosshairUpdated(crosshairConfig)
                }
            }
            colorRow.addView(dot)
        }
        root.addView(colorRow)
        root.addView(theme.space(8))

        val (sizeCard, _) = theme.stepperSliderCard("Ukuran", 40, crosshairConfig.sizeDp, { "$it dp" }) {
            crosshairConfig = crosshairConfig.copy(sizeDp = it.coerceAtLeast(6))
            onCrosshairUpdated(crosshairConfig)
        }
        root.addView(sizeCard)
        root.addView(theme.space(6))

        val (thickCard, _) = theme.stepperSliderCard("Ketebalan", 8, crosshairConfig.thicknessDp, { "$it dp" }) {
            crosshairConfig = crosshairConfig.copy(thicknessDp = it.coerceAtLeast(1))
            onCrosshairUpdated(crosshairConfig)
        }
        root.addView(thickCard)
        root.addView(theme.space(6))

        val (opCard, _) = theme.stepperSliderCard("Opacity", 100, crosshairConfig.opacityPercent, { "$it%" }) {
            crosshairConfig = crosshairConfig.copy(opacityPercent = it.coerceAtLeast(15))
            onCrosshairUpdated(crosshairConfig)
        }
        root.addView(opCard)
        root.addView(theme.space(6))

        val (offXCard, _) = theme.stepperSliderCard("Geser X", 80, crosshairConfig.offsetX + 40, { "${it - 40}" }) {
            crosshairConfig = crosshairConfig.copy(offsetX = it - 40)
            onCrosshairUpdated(crosshairConfig)
        }
        root.addView(offXCard)
        root.addView(theme.space(6))

        val (offYCard, _) = theme.stepperSliderCard("Geser Y", 80, crosshairConfig.offsetY + 40, { "${it - 40}" }) {
            crosshairConfig = crosshairConfig.copy(offsetY = it - 40)
            onCrosshairUpdated(crosshairConfig)
        }
        root.addView(offYCard)

        return root
    }

    fun buildMacroTab(onSaveMacro: (MacroConfigEntity) -> Unit): LinearLayout {
        val root = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
        root.addView(theme.titleText("Macro"))
        root.addView(theme.space(6))
        root.addView(theme.bodyText("Konfigurasi node & interval tersimpan secara lokal.", theme.textSecondary, 11.5f))
        root.addView(theme.space(8))

        val (swRow, _) = theme.switchRow("Macro Aktif", "Mode referensi konfigurasi", macroEnabled) {
            macroEnabled = it
        }
        root.addView(swRow)
        root.addView(theme.space(8))

        val (trigCard, _) = theme.stepperSliderCard("Ukuran Trigger", 20, macroTriggerSize, { "${it * 4} dp" }) {
            macroTriggerSize = it.coerceAtLeast(4)
        }
        root.addView(trigCard)
        root.addView(theme.space(6))

        val (intCard, _) = theme.stepperSliderCard("Interval", 2000, macroIntervalMs, { "$it ms" }) {
            macroIntervalMs = it.coerceAtLeast(50)
        }
        root.addView(intCard)
        root.addView(theme.space(6))

        val (holdCard, _) = theme.stepperSliderCard("Lama Tekan", 300, macroHoldMs, { "$it ms" }) {
            macroHoldMs = it.coerceAtLeast(10)
        }
        root.addView(holdCard)
        root.addView(theme.space(8))

        val btnSave = theme.actionPillButton("Simpan Macro", theme.cyan, Color.parseColor("#07090F"), null) {
            onSaveMacro(
                MacroConfigEntity(
                    name = "Floating Macro",
                    gamePackage = null,
                    triggerX = 0.25f,
                    triggerY = 0.55f,
                    targetX = 0.72f,
                    targetY = 0.55f,
                    triggerSizeDp = (macroTriggerSize * 4).toFloat(),
                    repeatIntervalMs = macroIntervalMs,
                    enabled = macroEnabled
                )
            )
        }
        root.addView(btnSave)
        root.addView(theme.space(6))
        root.addView(theme.actionPillButton("Buka Editor Visual Macro") { onOpenAppRoute("macro") })
        return root
    }

    fun buildBurikTab(
        initialTexture: TextureOptMode,
        onSelectTexture: (TextureOptMode) -> Unit
    ): LinearLayout {
        val root = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
        root.addView(theme.titleText("Burik"))
        root.addView(theme.space(4))
        root.addView(theme.bodyText("Perubahan profil tekstur disimpan untuk game Anda.", theme.textSecondary, 11.5f))
        root.addView(theme.space(8))

        var selected = initialTexture
        val cardViews = mutableListOf<Pair<TextureOptMode, LinearLayout>>()
        TextureOptMode.entries.forEach { mode ->
            val card = theme.cardContainer().apply {
                background = theme.roundedBg(
                    theme.bgCard,
                    14,
                    if (selected == mode) theme.cyan else theme.strokeGlass,
                    if (selected == mode) 2 else 1
                )
                addView(theme.bodyText("${mode.displayName} (${mode.resolutionScalePercent}%)", theme.textPrimary, 12.5f).apply {
                    setTypeface(theme.spaceGrotesk, Typeface.BOLD)
                })
                addView(theme.bodyText(mode.description, theme.textSecondary, 11f))
                setOnClickListener {
                    selected = mode
                    onSelectTexture(mode)
                    cardViews.forEach { (m, c) ->
                        c.background = theme.roundedBg(
                            theme.bgCard,
                            14,
                            if (m == selected) theme.cyan else theme.strokeGlass,
                            if (m == selected) 2 else 1
                        )
                    }
                }
            }
            cardViews.add(mode to card)
            root.addView(card)
            root.addView(theme.space(6))
        }
        return root
    }

    fun buildKontrolTab(): LinearLayout {
        val root = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
        root.addView(theme.titleText("Kontrol"))
        root.addView(theme.space(8))

        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
        val curVol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        val (volCard, _) = theme.stepperSliderCard("Volume Media", maxVol, curVol, { "${(it * 100 / maxVol)}%" }) { v ->
            runCatching { audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, v, 0) }
        }
        root.addView(volCard)
        root.addView(theme.space(8))

        val (wakeRow, _) = theme.switchRow("Layar Tetap Menyala", "Cegah layar redup saat bermain", true) {
            onKeepScreenOnToggle(it)
        }
        root.addView(wakeRow)
        root.addView(theme.space(8))

        root.addView(theme.actionPillButton("Move button to the other side", onClick = onMoveSideClick))
        root.addView(theme.space(6))
        root.addView(theme.actionPillButton("Turn off sensitivity & macro") {
            sensEnabled = false
            macroEnabled = false
            crosshairConfig = crosshairConfig.copy(enabled = false)
            onCrosshairUpdated(crosshairConfig)
        })
        root.addView(theme.space(6))
        root.addView(theme.actionPillButton("Buka XARAGameSpace", theme.bgRail, theme.cyan, theme.cyan) {
            onOpenAppRoute(null)
        })
        root.addView(theme.space(6))
        root.addView(theme.actionPillButton("End game session", Color.parseColor("#33FF4D6D"), theme.danger, theme.danger) {
            onEndSessionClick()
        })

        return root
    }
}
