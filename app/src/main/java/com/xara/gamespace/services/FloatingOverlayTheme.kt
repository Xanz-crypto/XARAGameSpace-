package com.xara.gamespace.services

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.Switch
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import com.example.R

class FloatingOverlayTheme(private val context: Context) {

    val orbitron: Typeface = ResourcesCompat.getFont(context, R.font.orbitron) ?: Typeface.DEFAULT_BOLD
    val spaceGrotesk: Typeface = ResourcesCompat.getFont(context, R.font.space_grotesk) ?: Typeface.SANS_SERIF
    val jetBrainsMono: Typeface = ResourcesCompat.getFont(context, R.font.jetbrains_mono) ?: Typeface.MONOSPACE

    val bgPanel = Color.parseColor("#E60D111C")
    val bgRail = Color.parseColor("#EE090C14")
    val bgCard = Color.parseColor("#CC151B2B")
    val bgPill = Color.parseColor("#D9101522")
    val strokeGlass = Color.parseColor("#2E3B5E")
    val strokeAccent = Color.parseColor("#00F0FF")
    val cyan = Color.parseColor("#00F0FF")
    val violet = Color.parseColor("#A855F7")
    val green = Color.parseColor("#00E676")
    val warm = Color.parseColor("#FFB300")
    val danger = Color.parseColor("#FF4D6D")
    val textPrimary = Color.parseColor("#F3F6FC")
    val textSecondary = Color.parseColor("#94A3B8")

    fun dp(value: Int): Int = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        value.toFloat(),
        context.resources.displayMetrics
    ).toInt()

    fun roundedBg(
        fillColor: Int,
        radiusDp: Int,
        strokeColor: Int? = null,
        strokeWidthDp: Int = 1
    ): GradientDrawable = GradientDrawable().apply {
        setColor(fillColor)
        cornerRadius = dp(radiusDp).toFloat()
        if (strokeColor != null) {
            setStroke(dp(strokeWidthDp), strokeColor)
        }
    }

    fun titleText(text: String): TextView = TextView(context).apply {
        this.text = text
        typeface = orbitron
        setTextColor(textPrimary)
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
        letterSpacing = 0.04f
    }

    fun bodyText(text: String, color: Int = textSecondary, sizeSp: Float = 12f): TextView = TextView(context).apply {
        this.text = text
        typeface = spaceGrotesk
        setTextColor(color)
        setTextSize(TypedValue.COMPLEX_UNIT_SP, sizeSp)
        setLineSpacing(dp(2).toFloat(), 1f)
    }

    fun monoText(text: String, color: Int = cyan, sizeSp: Float = 12f): TextView = TextView(context).apply {
        this.text = text
        typeface = jetBrainsMono
        setTextColor(color)
        setTextSize(TypedValue.COMPLEX_UNIT_SP, sizeSp)
    }

    fun cardContainer(): LinearLayout = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        background = roundedBg(bgCard, 14, strokeGlass)
        setPadding(dp(12), dp(10), dp(12), dp(10))
    }

    fun space(heightDp: Int): View = View(context).apply {
        layoutParams = LinearLayout.LayoutParams(dp(heightDp), dp(heightDp))
    }

    fun actionPillButton(
        label: String,
        fillColor: Int = bgCard,
        textColor: Int = textPrimary,
        strokeColor: Int? = strokeGlass,
        onClick: () -> Unit
    ): TextView = TextView(context).apply {
        text = label
        typeface = spaceGrotesk
        setTypeface(spaceGrotesk, Typeface.BOLD)
        setTextColor(textColor)
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f)
        gravity = Gravity.CENTER
        setPadding(dp(14), dp(10), dp(14), dp(10))
        background = roundedBg(fillColor, 20, strokeColor)
        setOnClickListener { onClick() }
    }

    fun switchRow(
        label: String,
        subtitle: String? = null,
        initial: Boolean,
        onToggle: (Boolean) -> Unit
    ): Pair<LinearLayout, Switch> {
        val row = cardContainer().apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val textCol = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        textCol.addView(bodyText(label, textPrimary, 12.5f).apply { setTypeface(spaceGrotesk, Typeface.BOLD) })
        if (subtitle != null) {
            textCol.addView(bodyText(subtitle, textSecondary, 10.5f))
        }
        @Suppress("UseSwitchCompatOrMaterialCode")
        val sw = Switch(context).apply {
            isChecked = initial
            setOnCheckedChangeListener { _, checked -> onToggle(checked) }
        }
        row.addView(textCol)
        row.addView(sw)
        return row to sw
    }

    fun stepperSliderCard(
        label: String,
        max: Int,
        initialProgress: Int,
        valueFormatter: (Int) -> String,
        onChanged: (Int) -> Unit
    ): Pair<LinearLayout, TextView> {
        val card = cardContainer()
        val header = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val lbl = bodyText(label, textPrimary, 12f).apply {
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        val valView = monoText(valueFormatter(initialProgress), cyan, 12f)
        header.addView(lbl)
        header.addView(valView)

        val controls = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(6), 0, 0)
        }
        val seekBar = SeekBar(context).apply {
            this.max = max
            progress = initialProgress.coerceIn(0, max)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        val btnMinus = stepperChip("−") {
            val next = (seekBar.progress - 1).coerceAtLeast(0)
            seekBar.progress = next
        }
        val btnPlus = stepperChip("+") {
            val next = (seekBar.progress + 1).coerceAtMost(max)
            seekBar.progress = next
        }
        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, p: Int, fromUser: Boolean) {
                valView.text = valueFormatter(p)
                onChanged(p)
            }
            override fun onStartTrackingTouch(sb: SeekBar?) = Unit
            override fun onStopTrackingTouch(sb: SeekBar?) = Unit
        })

        controls.addView(btnMinus)
        controls.addView(seekBar)
        controls.addView(btnPlus)

        card.addView(header)
        card.addView(controls)
        return card to valView
    }

    private fun stepperChip(symbol: String, onClick: () -> Unit): TextView = TextView(context).apply {
        text = symbol
        typeface = spaceGrotesk
        setTypeface(spaceGrotesk, Typeface.BOLD)
        setTextColor(textPrimary)
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
        gravity = Gravity.CENTER
        val s = dp(30)
        layoutParams = LinearLayout.LayoutParams(s, s)
        background = roundedBg(bgRail, 15, strokeGlass)
        setOnClickListener { onClick() }
    }
}
