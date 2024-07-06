package com.nona.verticallayout.graphics.debug

import android.graphics.Color
import android.graphics.Paint

object DebugPaints {
    const val DEBUG = false
    val drawOffsetPaint = Paint().apply {
        color = Color.BLUE
    }
    val bboxPaint = Paint().apply {
        color = Color.BLACK
        style = Paint.Style.STROKE
        strokeWidth = 3f
    }
    val baselinePaint = Paint().apply {
        color = Color.GREEN
        style = Paint.Style.STROKE
        strokeWidth = 3f
    }
    val ascentPaint = Paint().apply {
        color = Color.valueOf(1f, 0f, 0f, 0.3f).toArgb()
        style = Paint.Style.FILL
    }
    val descentPaint = Paint().apply {
        color = Color.valueOf(0f, 0f, 1f, 0.3f).toArgb()
        style = Paint.Style.FILL
    }
}