package com.example.verticallayouttest.graphics

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Paint.FontMetrics
import android.graphics.fonts.Font
import android.util.Log

enum class DrawOrientation {
    Upright,
    Rotate,
    TateChuYoko,
}

sealed class OrientationRun(
    val text: CharSequence,
    val start: Int,
    val end: Int
) {
    abstract val height: Float

    abstract fun draw(canvas: Canvas, x: Float, y: Float, paint: Paint, vMetrics: FontMetrics)
}

class TateChuYokoOrientationRun(
    text: CharSequence,
    start: Int,
    end: Int,
    val metrics: FontMetrics,
    val width: Float,
) : OrientationRun(text, start, end) {
    override val height: Float by lazy {
        metrics.descent - metrics.ascent
    }

    override fun draw(canvas: Canvas, x: Float, y: Float, paint: Paint, vMetrics: FontMetrics) {
        var x = x - vMetrics.descent
        var y = y - metrics.ascent
        if (width < paint.textSize * 1.1f) {
            val w = vMetrics.descent - vMetrics.ascent
            x += (w - width) / 2
            canvas.drawText(text, start, end, x, y, paint)
        } else {
            x -= paint.textSize * 0.05f
            val originalScaleX = paint.textScaleX
            paint.textScaleX = 1.1f * paint.textSize / width
            canvas.drawText(text, start, end, x, y, paint)
            paint.textScaleX = originalScaleX
        }
    }

}

class RotateOrientationRun(
    text: CharSequence,
    start: Int,
    end: Int,
    val hCharAdvances: FloatArray
) : OrientationRun(text, start, end) {
    override val height: Float by lazy {
        hCharAdvances.sum()
    }

    override fun draw(canvas: Canvas, x: Float, y: Float, paint: Paint, vMetrics: FontMetrics) {
        val fontMetrics = Paint.FontMetricsInt()
        paint.getFontMetricsInt(fontMetrics)
        val width = fontMetrics.descent - fontMetrics.ascent
        val shift = fontMetrics.ascent + width * 0.5f
        canvas.save()
        try {
            canvas.rotate(90f, x, y)
            canvas.drawText(text, start, end, x, y - shift, paint)
        } finally {
            canvas.restore()
        }
    }
}

class UprightOrientationRun(
    text: CharSequence,
    start: Int,
    end: Int,
    val glyphIds: IntArray,
    val fonts: Array<Font>,
    val vCharAdvances: FloatArray,
    val vCharTsb: FloatArray,
    val hCharAdvances: FloatArray,
) : OrientationRun(text, start, end) {
    override val height: Float by lazy {
        vCharAdvances.sum()
    }

    override fun draw(canvas: Canvas, x: Float, y: Float, paint: Paint, vMetrics: FontMetrics) {
        if (glyphIds.isEmpty()) {
            return
        }

        val drawPositions = FloatArray(40)
        val drawGlyphs = IntArray(20)

        var x = x
        var y = y
        var prevFont = fonts[0]
        var glyphIndex = 0
        var charIndex = 0

        for (i in 0 until glyphIds.size) {
            val curFont = fonts[i]
            if (glyphIndex == 20 || prevFont !== curFont) {
                Log.e("Debug", "font = ${prevFont.file}, glyphs = ${java.util.Arrays.toString(drawGlyphs)}, positions = ${java.util.Arrays.toString(drawPositions)}")
                canvas.drawGlyphs(drawGlyphs, 0,
                    drawPositions, 0,
                    glyphIndex,
                    prevFont,
                    paint)
                prevFont = curFont
                glyphIndex = 0
            }

            /*
            canvas.drawRect(
                x - hCharAdvances[charIndex] / 2,
                y,
                x + hCharAdvances[charIndex] / 2,
                y + vCharAdvances[charIndex],
                linePaint)

             */

            x += 0f
            y += vCharAdvances[charIndex]
            val tsb = vCharTsb[charIndex]
            val w = hCharAdvances[charIndex]

            drawPositions[glyphIndex * 2] = x - w / 2
            drawPositions[glyphIndex * 2 + 1] = y
            drawGlyphs[glyphIndex] = glyphIds[i]
            glyphIndex++

            charIndex++
            while (charIndex < vCharAdvances.size && vCharAdvances[charIndex] == 0f) {
                charIndex++
            }

        }

        Log.e("Debug", "font = ${prevFont.file}, glyphs = ${java.util.Arrays.toString(drawGlyphs)}, positions = ${java.util.Arrays.toString(drawPositions)}")
        canvas.drawGlyphs(drawGlyphs, 0,
            drawPositions, 0,
            glyphIndex,
            prevFont,
            paint)
    }
}

class IntrinsicVerticalLayout(
    val text: CharSequence,
    val openType: OpenType,
    val pant: Paint,
    val vMetrics: Paint.FontMetrics,
    val runs: List<OrientationRun>
) {
    val linePaint = Paint().apply {
        color = Color.GREEN
        strokeWidth = 5f
    }

    fun draw(canvas: Canvas, x: Float, y: Float, paint: Paint) {

        /*
        val cH = canvas.height.toFloat()
        canvas.drawLine(x, 0f, x, cH, linePaint.apply { color = Color.GREEN })
        canvas.drawLine(x + vMetrics.descent, 0f, x + vMetrics.descent, cH, linePaint.apply { color = Color.BLUE })
        canvas.drawLine(x + vMetrics.ascent, 0f, x + vMetrics.ascent, cH, linePaint.apply { color = Color.RED })

         */

        var x = x
        var y = y
        runs.forEach { run ->
            run.draw(canvas, x, y, paint, vMetrics)

            x += 0f
            y += run.height
        }
    }

    val start: Int
        get() = runs.first().start
    val end: Int
        get() = runs.last().end
}