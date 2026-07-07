package com.example.hcamd

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View

class TacticalGaugeView(context: Context, attrs: AttributeSet?) : View(context, attrs) {
    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#3b494b")
        style = Paint.Style.STROKE
        strokeWidth = 20f
        strokeCap = Paint.Cap.BUTT
    }
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 20f
        strokeCap = Paint.Cap.BUTT
    }
    private val needlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 6f
        strokeCap = Paint.Cap.ROUND
    }
    private val centerDotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    var minValue = 0f
    var maxValue = 100f
    var currentValue = 0f
        set(value) {
            field = value
            postInvalidateOnAnimation() // This forces the gauge to redraw!
        }

    var gaugeColor: Int = Color.parseColor("#00f0ff")
    var needleColor: Int = Color.WHITE
    var isDangerMode = false

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        val padding = 20f

        val rect = RectF(padding, padding, w - padding, (h * 2) - padding)

        // 1. Draw the empty background track
        canvas.drawArc(rect, 180f, 180f, false, trackPaint)

        val clampedValue = currentValue.coerceIn(minValue, maxValue)
        val percentage = (clampedValue - minValue) / (maxValue - minValue)
        val angle = 180f * percentage

        // 2. Draw the fill or danger zone
        if (isDangerMode) {
            // FIX: Move the red danger zone to the right side (near 0 dBm)
            val dangerSweep = 180f * 0.3f // Top 30% of the gauge is danger
            val dangerStart = 360f - dangerSweep // Start drawing from the right side

            fillPaint.color = Color.parseColor("#d30017")
            canvas.drawArc(rect, dangerStart, dangerSweep, false, fillPaint)
        } else {
            // Normal gauge behavior (e.g., Thermal density)
            fillPaint.color = gaugeColor
            canvas.drawArc(rect, 180f, angle, false, fillPaint)
        }

        // 3. Draw the needle
        needlePaint.color = needleColor
        centerDotPaint.color = needleColor

        val cx = w / 2f
        val cy = h - (padding / 2f)
        val radius = (w / 2f) - padding

        val needleAngleRad = Math.toRadians((180f + angle).toDouble())
        val stopX = (cx + radius * Math.cos(needleAngleRad)).toFloat()
        val stopY = (cy + radius * Math.sin(needleAngleRad)).toFloat()

        canvas.drawLine(cx, cy, stopX, stopY, needlePaint)
        canvas.drawCircle(cx, cy, 10f, centerDotPaint)
    }
}