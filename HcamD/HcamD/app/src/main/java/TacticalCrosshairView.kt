package com.example.hcamd

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View

class TacticalCrosshairView(context: Context, attrs: AttributeSet?) : View(context, attrs) {

    // The faint cyan box that connects the corners
    private val thinBoxPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#4400f0ff")
        style = Paint.Style.STROKE
        strokeWidth = 3f
    }

    // The thick corner brackets
    private val bracketPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 12f
        strokeCap = Paint.Cap.SQUARE
    }

    var isDangerMode = false
        set(value) {
            field = value
            invalidate() // Redraws the view when the state changes
        }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val w = width.toFloat()
        val h = height.toFloat()
        val padding = 6f // Prevents the thick lines from being clipped by the view bounds

        val left = padding
        val top = padding
        val right = w - padding
        val bottom = h - padding

        val cornerLength = 80f // How long the brackets extend

        // 1. Draw the faint inner bounding box
        canvas.drawRect(left, top, right, bottom, thinBoxPaint)

        // 2. Set bracket color (Green for Normal, Red for Danger)
        bracketPaint.color = if (isDangerMode) Color.parseColor("#d30017") else Color.parseColor("#00ff00")

        // 3. Draw Top-Left Bracket
        canvas.drawLine(left, top, left + cornerLength, top, bracketPaint)
        canvas.drawLine(left, top, left, top + cornerLength, bracketPaint)

        // 4. Draw Top-Right Bracket
        canvas.drawLine(right, top, right - cornerLength, top, bracketPaint)
        canvas.drawLine(right, top, right, top + cornerLength, bracketPaint)

        // 5. Draw Bottom-Left Bracket
        canvas.drawLine(left, bottom, left + cornerLength, bottom, bracketPaint)
        canvas.drawLine(left, bottom, left, bottom - cornerLength, bracketPaint)

        // 6. Draw Bottom-Right Bracket
        canvas.drawLine(right, bottom, right - cornerLength, bottom, bracketPaint)
        canvas.drawLine(right, bottom, right, bottom - cornerLength, bracketPaint)
    }
}
