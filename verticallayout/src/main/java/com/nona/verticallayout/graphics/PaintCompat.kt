package com.nona.verticallayout.graphics

import android.graphics.Paint
import android.graphics.Paint.FontMetrics
import android.graphics.Paint.FontMetricsInt
import android.os.Build
import android.text.StaticLayout
import android.text.TextPaint

object PaintCompat {
    fun getFontMetricsInt(paint: Paint, text: CharSequence, start: Int, end: Int) : FontMetrics {
        val fmi = FontMetricsInt()
        if (Build.VERSION.SDK_INT > 32) {
            paint.getFontMetricsInt(text, start, end, start, end, false, fmi)
            return FontMetrics().apply {
                top = fmi.top.toFloat()
                bottom = fmi.bottom.toFloat()
                ascent = fmi.ascent.toFloat()
                descent = fmi.descent.toFloat()
            }
        } else {
            val tp = TextPaint().apply { set(paint) }
            val l = StaticLayout.Builder.obtain(text, start, end, tp, Int.MAX_VALUE)
                .setUseLineSpacingFromFallbacks(true)
                .build()
            return FontMetrics().apply {
                top = l.getLineAscent(0).toFloat()
                ascent = l.getLineAscent(0).toFloat()
                bottom = l.getLineDescent(0).toFloat()
                descent = l.getLineDescent(0).toFloat()
            }
        }
    }
}