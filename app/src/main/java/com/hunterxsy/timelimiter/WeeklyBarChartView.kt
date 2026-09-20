package com.hunterxsy.timelimiter

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View

class WeeklyBarChartView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    private var values: List<Int> = listOf(0, 0, 0, 0, 0, 0, 0)
    private var labels: List<String> = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

    private val barPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#A0A0C0")
        textSize = 26f
        textAlign = Paint.Align.CENTER
    }

    fun setData(newValues: List<Int>, newLabels: List<String>) {
        values = newValues
        labels = newLabels
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (values.isEmpty()) return

        val maxValue = (values.maxOrNull() ?: 1).coerceAtLeast(1)
        val barCount = values.size
        val spacing = width / (barCount * 2f)
        val barWidth = spacing * 0.9f
        val chartHeight = height - 60f

        barPaint.shader = LinearGradient(
            0f, 0f, 0f, chartHeight,
            Color.parseColor("#2DD9C7"), Color.parseColor("#6C3CE9"),
            Shader.TileMode.CLAMP
        )

        for (i in values.indices) {
            val barHeight = (values[i].toFloat() / maxValue) * chartHeight
            val left = spacing * (2 * i + 0.5f)
            val right = left + barWidth
            val top = chartHeight - barHeight
            val bottom = chartHeight

            canvas.drawRoundRect(left, top, right, bottom, 8f, 8f, barPaint)
            canvas.drawText(labels[i], left + barWidth / 2, height.toFloat() - 10f, textPaint)
        }
    }
}
