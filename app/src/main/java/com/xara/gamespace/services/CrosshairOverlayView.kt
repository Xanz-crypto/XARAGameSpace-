package com.xara.gamespace.services

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.View

enum class CrosshairShape(val label: String) {
    DOT("Titik"),
    PLUS("Plus"),
    T_SHAPE("T")
}

data class CrosshairConfig(
    val enabled: Boolean = false,
    val shape: CrosshairShape = CrosshairShape.PLUS,
    val colorInt: Int = Color.parseColor("#00F0FF"),
    val sizeDp: Int = 18,
    val thicknessDp: Int = 2,
    val opacityPercent: Int = 80,
    val offsetX: Int = 0,
    val offsetY: Int = 0
)

class CrosshairOverlayView(context: Context) : View(context) {

    var config: CrosshairConfig = CrosshairConfig()
        set(value) {
            field = value
            invalidate()
        }

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        strokeCap = Paint.Cap.ROUND
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (!config.enabled) return

        val density = resources.displayMetrics.density
        val cx = width / 2f + config.offsetX * density
        val cy = height / 2f + config.offsetY * density
        val arm = config.sizeDp * density
        val stroke = (config.thicknessDp.coerceAtLeast(1)) * density
        val gap = (arm * 0.25f).coerceAtLeast(3f * density)

        paint.color = config.colorInt
        paint.alpha = ((config.opacityPercent.coerceIn(10, 100) / 100f) * 255).toInt()
        paint.strokeWidth = stroke
        paint.style = Paint.Style.FILL

        when (config.shape) {
            CrosshairShape.DOT -> {
                canvas.drawCircle(cx, cy, (arm * 0.28f).coerceAtLeast(3f * density), paint)
            }
            CrosshairShape.PLUS -> {
                paint.style = Paint.Style.STROKE
                canvas.drawLine(cx - arm, cy, cx - gap, cy, paint)
                canvas.drawLine(cx + gap, cy, cx + arm, cy, paint)
                canvas.drawLine(cx, cy - arm, cx, cy - gap, paint)
                canvas.drawLine(cx, cy + gap, cx, cy + arm, paint)
                paint.style = Paint.Style.FILL
                canvas.drawCircle(cx, cy, stroke * 0.85f, paint)
            }
            CrosshairShape.T_SHAPE -> {
                paint.style = Paint.Style.STROKE
                canvas.drawLine(cx - arm, cy, cx - gap, cy, paint)
                canvas.drawLine(cx + gap, cy, cx + arm, cy, paint)
                canvas.drawLine(cx, cy + gap, cx, cy + arm, paint)
                paint.style = Paint.Style.FILL
                canvas.drawCircle(cx, cy, stroke * 0.85f, paint)
            }
        }
    }
}
