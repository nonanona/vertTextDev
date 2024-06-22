package com.example.verticallayouttest.graphics

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Paint.FontMetrics
import android.graphics.fonts.Font
import android.util.Log
import kotlin.math.max

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

enum class DrawOrientation {
    Upright,
    Rotate,
    TateChuYoko,
    Ruby,
}

sealed class VerticalLayoutRun(
    val text: CharSequence,
    val start: Int,
    val end: Int
) {
    abstract val height: Float

    abstract fun draw(canvas: Canvas, x: Float, y: Float, paint: Paint, vMetrics: FontMetrics)
}

class RubyVerticalLayoutRun(
    text: CharSequence,
    start: Int,
    end: Int,
    val contentsRun: IntrinsicVerticalLayout,
    val rubySpan: RubySpan,
    val rubyRun: IntrinsicVerticalLayout
): VerticalLayoutRun(text, start, end) {
    override val height: Float by lazy {
        max(contentsRun.height, rubyRun.height)
    }

    override fun draw(canvas: Canvas, x: Float, y: Float, paint: Paint, vMetrics: FontMetrics) {
        val contentHeight = contentsRun.height
        val rubyHeight = rubyRun.height

        var contentY = y
        var rubyY = y
        if (contentHeight > rubyHeight) {
            rubyY += (contentHeight - rubyHeight) / 2
        } else {
            contentY += (rubyHeight - contentHeight) / 2
        }

        contentsRun.draw(canvas, x, contentY, paint)

        val originalTextSize = paint.textSize
        paint.textSize *= rubySpan.textScale
        rubyRun.draw(canvas, x - contentsRun.vMetrics.ascent + rubyRun.vMetrics.descent, rubyY, paint)
        paint.textSize = originalTextSize

    }
}

class TateChuYokoVerticalLayoutRun(
    text: CharSequence,
    start: Int,
    end: Int,
    val metrics: FontMetrics,
    val width: Float,
) : VerticalLayoutRun(text, start, end) {
    override val height: Float by lazy {
        metrics.descent - metrics.ascent
    }

    override fun draw(canvas: Canvas, x: Float, y: Float, paint: Paint, vMetrics: FontMetrics) {
        var x = x - vMetrics.descent
        var y = y - metrics.ascent
        if (width < paint.textSize * 1.1f) {
            val w = vMetrics.descent - vMetrics.ascent
            x += (w - width) / 2
            if (DebugPaints.DEBUG) {
                canvas.drawRect(x, y, x + width, y + metrics.ascent, DebugPaints.ascentPaint)
                canvas.drawRect(x, y, x + width, y + metrics.descent, DebugPaints.descentPaint)
                canvas.drawCircle(x, y, 5f, DebugPaints.drawOffsetPaint)
                canvas.drawLine(x, y, x + width, y, DebugPaints.baselinePaint)
            }
            canvas.drawText(text, start, end, x, y, paint)
        } else {
            x -= paint.textSize * 0.05f
            val originalScaleX = paint.textScaleX
            paint.textScaleX = 1.1f * paint.textSize / width
            if (DebugPaints.DEBUG) {
                canvas.drawRect(x, y, x + 1.1f * paint.textSize, y + metrics.ascent, DebugPaints.ascentPaint)
                canvas.drawRect(x, y, x + 1.1f * paint.textSize, y + metrics.descent, DebugPaints.descentPaint)
                canvas.drawCircle(x, y, 5f, DebugPaints.drawOffsetPaint)
                canvas.drawLine(x, y, x + 1.1f * paint.textSize, y, DebugPaints.baselinePaint)
            }
            canvas.drawText(text, start, end, x, y, paint)
            paint.textScaleX = originalScaleX
        }
    }
}

class RotateVerticalLayoutRun(
    text: CharSequence,
    start: Int,
    end: Int,
    val hCharAdvances: FloatArray
) : VerticalLayoutRun(text, start, end) {
    override val height: Float by lazy {
        hCharAdvances.sum()
    }

    override fun draw(canvas: Canvas, x: Float, y: Float, paint: Paint, vMetrics: FontMetrics) {
        val metrics = PaintCompat.getFontMetricsInt(paint, text, start, end)
        val width = metrics.descent - metrics.ascent
        val shift = metrics.ascent + width * 0.5f
        var y = y - shift
        canvas.save()
        try {
            canvas.rotate(90f, x, y + shift)
            if (DebugPaints.DEBUG) {
                canvas.drawRect(x, y, x + hCharAdvances.sum(), y + metrics.ascent, DebugPaints.ascentPaint)
                canvas.drawRect(x, y, x + hCharAdvances.sum(), y + metrics.descent, DebugPaints.descentPaint)

                canvas.drawLine(x, y, x + hCharAdvances.sum(), y, DebugPaints.baselinePaint)
                canvas.drawCircle(x, y, 5f, DebugPaints.drawOffsetPaint)
            }
            canvas.drawText(text, start, end, x, y, paint)
        } finally {
            canvas.restore()
        }
    }
}

class UprightVerticalLayoutRun(
    text: CharSequence,
    start: Int,
    end: Int,
    val glyphIds: IntArray,
    val fonts: Array<Font>,
    val vCharAdvances: FloatArray,
    val vCharTsb: FloatArray,
    val hCharAdvances: FloatArray,
) : VerticalLayoutRun(text, start, end) {
    override val height: Float by lazy {
        vCharAdvances.sum()
    }

    override fun draw(canvas: Canvas, x: Float, y: Float, paint: Paint, vMetrics: FontMetrics) {
        if (glyphIds.isEmpty()) {
            return
        }

        if (DebugPaints.DEBUG) {
            canvas.drawRect(x, y, x - vMetrics.ascent, y + height, DebugPaints.ascentPaint)
            canvas.drawRect(x, y, x - vMetrics.descent, y + height, DebugPaints.descentPaint)
            canvas.drawLine(x, y, x, y + height, DebugPaints.baselinePaint)
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

            val tsb = vCharTsb[charIndex]
            val w = hCharAdvances[charIndex]

            x += 0f
            y += vCharAdvances[charIndex]

            if (DebugPaints.DEBUG) {
                canvas.drawRect(
                    x - w / 2,
                    y,
                    x + w / 2,
                    y - vCharAdvances[charIndex],
                    DebugPaints.bboxPaint
                )
                canvas.drawCircle(x - w / 2, y, 5f, DebugPaints.drawOffsetPaint)
            }

            drawPositions[glyphIndex * 2] = x - w / 2
            drawPositions[glyphIndex * 2 + 1] = y  - tsb
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
    val runs: List<VerticalLayoutRun>
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

    val height: Float
        get() = runs.fold(0f) { acc, run -> acc + run.height }
}