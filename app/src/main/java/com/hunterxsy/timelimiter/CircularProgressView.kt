package com.hunterxsy.timelimiter

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.LinearGradient
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View

class CircularProgressView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    private var progressPercent: Int = 0
    private var centerText: String = ""
    private var subText: String = ""

    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 22f
        color = Color.parseColor("#2A2645")
        strokeCap = Paint.Cap.ROUND
    }

    private val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 22f
        strokeCap = Paint.Cap.ROUND
    }

    private val centerTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 46f
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
    }

    private val subTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#A0A0C0")
        textSize = 24f
        textAlign = Paint.Align.CENTER
    }

    fun setProgress(percent: Int, center: String, sub: String) {
        progressPercent = percent.coerceIn(0, 100)
        centerText = center
        subText = sub
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val strokeWidth = 22f
        val padding = strokeWidth / 2 + 4f
        val rect = RectF(padding, padding, width - padding, height - padding)

        canvas.drawArc(rect, 0f, 360f, false, bgPaint)

        progressPaint.shader = LinearGradient(
            0f, 0f, width.toFloat(), height.toFloat(),
            Color.parseColor("#2DD9C7"),
            Color.parseColor("#6C3CE9"),
            Shader.TileMode.CLAMP
        )

        val sweepAngle = 360f * (progressPercent / 100f)
        canvas.drawArc(rect, -90f, sweepAngle, false, progressPaint)

        val centerX = width / 2f
        val centerY = height / 2f
        canvas.drawText(centerText, centerX, centerY - 5f, centerTextPaint)
        canvas.drawText(subText, centerX, centerY + 35f, subTextPaint)
    }
}
