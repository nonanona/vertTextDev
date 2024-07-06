package com.nona.verticallayout.graphics

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

class VerticalFontMetrics(val left: Float, val right: Float) {
    fun width(): Float {
        return left + right
    }
}

sealed class VerticalLayoutRun(
    val text: CharSequence,
    val start: Int,
    val end: Int
) {
    abstract val height: Float
    abstract fun verticalMetrics(paint: Paint): VerticalFontMetrics

    abstract fun draw(canvas: Canvas, x: Float, y: Float, paint: Paint, vMetrics: VerticalFontMetrics)

    abstract fun split(height: Float): Pair<VerticalLayoutRun, VerticalLayoutRun>?
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

    override fun draw(canvas: Canvas, x: Float, y: Float, paint: Paint, vMetrics: VerticalFontMetrics) {
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

    // Don't break line inside Ruby.
    override fun split(height: Float): Pair<VerticalLayoutRun, VerticalLayoutRun>? = null

    override fun verticalMetrics(paint: Paint): VerticalFontMetrics =
        VerticalFontMetrics(paint.textSize * 0.5f, paint.textSize)
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

    override fun draw(canvas: Canvas, x: Float, y: Float, paint: Paint, vMetrics: VerticalFontMetrics) {
        var x = x - vMetrics.left
        var y = y - metrics.ascent
        if (width < paint.textSize * 1.1f) {
            val w = vMetrics.left + vMetrics.right
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
                canvas.drawRect(x, y, x + 1.1f * paint.textSize, y + metrics.ascent,
                    DebugPaints.ascentPaint
                )
                canvas.drawRect(x, y, x + 1.1f * paint.textSize, y + metrics.descent,
                    DebugPaints.descentPaint
                )
                canvas.drawCircle(x, y, 5f, DebugPaints.drawOffsetPaint)
                canvas.drawLine(x, y, x + 1.1f * paint.textSize, y, DebugPaints.baselinePaint)
            }
            canvas.drawText(text, start, end, x, y, paint)
            paint.textScaleX = originalScaleX
        }
    }

    // Don't break line inside TateChuYoko.
    override fun split(height: Float): Pair<VerticalLayoutRun, VerticalLayoutRun>? = null

    override fun verticalMetrics(paint: Paint): VerticalFontMetrics =
        VerticalFontMetrics(paint.textSize * 0.55f, paint.textSize * 0.55f)
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

    override fun draw(canvas: Canvas, x: Float, y: Float, paint: Paint, vMetrics: VerticalFontMetrics) {
        val metrics = Paint.FontMetrics()
        paint.getFontMetrics(metrics)
        val width = metrics.descent - metrics.ascent
        val shift = metrics.ascent + width * 0.5f
        var y = y - shift
        canvas.save()
        try {
            canvas.rotate(90f, x, y + shift)
            if (DebugPaints.DEBUG) {
                canvas.drawRect(x, y, x + hCharAdvances.sum(), y + metrics.ascent,
                    DebugPaints.ascentPaint
                )
                canvas.drawRect(x, y, x + hCharAdvances.sum(), y + metrics.descent,
                    DebugPaints.descentPaint
                )

                canvas.drawLine(x, y, x + hCharAdvances.sum(), y, DebugPaints.baselinePaint)
                canvas.drawCircle(x, y, 5f, DebugPaints.drawOffsetPaint)
            }
            canvas.drawText(text, start, end, x, y, paint)
        } finally {
            canvas.restore()
        }
    }

    override fun split(height: Float): Pair<VerticalLayoutRun, VerticalLayoutRun>? {
        // TODO: Implement line break for Rotate run.
        return null
    }

    override fun verticalMetrics(paint: Paint): VerticalFontMetrics =
        VerticalFontMetrics(paint.textSize * 0.5f, paint.textSize * 0.5f)
}

class UprightVerticalLayoutRun(
    text: CharSequence,
    start: Int,
    end: Int,
    val glyphIds: IntArray,
    val fonts: Array<Font?>,
    val vCharAdvances: FloatArray,
    val vCharTsb: FloatArray,
    val hCharAdvances: FloatArray,
) : VerticalLayoutRun(text, start, end) {
    override val height: Float by lazy {
        vCharAdvances.sum()
    }

    override fun draw(canvas: Canvas, x: Float, y: Float, paint: Paint, vMetrics: VerticalFontMetrics) {
        if (glyphIds.isEmpty()) {
            return
        }

        if (DebugPaints.DEBUG) {
            canvas.drawRect(x, y, x - vMetrics.left, y + height, DebugPaints.ascentPaint)
            canvas.drawRect(x, y, x + vMetrics.right, y + height, DebugPaints.descentPaint)
            canvas.drawLine(x, y, x, y + height, DebugPaints.baselinePaint)
        }

        val drawPositions = FloatArray(40)
        val drawGlyphs = IntArray(20)

        var x = x
        var y = y
        var prevFont = requireNotNull(fonts[0])
        var glyphIndex = 0
        var charIndex = 0

        for (i in 0 until glyphIds.size) {
            if (glyphIds[i] == -1) {
                continue;
            }
            val curFont = requireNotNull(fonts[i])
            if (glyphIndex == 20 || prevFont !== curFont) {
                canvas.drawGlyphs(drawGlyphs, 0,
                    drawPositions, 0,
                    glyphIndex,
                    prevFont,
                    paint)
                prevFont = curFont
                glyphIndex = 0
            }

            val w = hCharAdvances[charIndex]
            val tsb = vCharTsb[charIndex]

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
            drawPositions[glyphIndex * 2 + 1] = y - 0.12f * paint.textSize
            drawGlyphs[glyphIndex] = glyphIds[i]
            glyphIndex++

            charIndex++
            while (charIndex < vCharAdvances.size && vCharAdvances[charIndex] == 0f) {
                charIndex++
            }

        }

        canvas.drawGlyphs(drawGlyphs, 0,
            drawPositions, 0,
            glyphIndex,
            prevFont,
            paint)
    }

    override fun split(height: Float): Pair<VerticalLayoutRun, VerticalLayoutRun>? {
        var remaining = height
        for (i in 0 until vCharAdvances.size) {
            val vChar = vCharAdvances[i]
            if (remaining < vChar) {
                if (i == 0) {
                    return null // Unable to fit only one char.
                } else {
                    val current = UprightVerticalLayoutRun(
                        text, start, start + i,
                        glyphIds.slice(0 until i).toIntArray(),
                        fonts.slice(0 until i).toTypedArray(),
                        vCharAdvances.slice(0 until i).toFloatArray(),
                        vCharTsb.slice(0 until i).toFloatArray(),
                        hCharAdvances.slice(0 until i).toFloatArray()
                    )
                    val next = UprightVerticalLayoutRun(
                        text, start + i, end,
                        glyphIds.slice(i until glyphIds.size).toIntArray(),
                        fonts.slice(i until glyphIds.size).toTypedArray(),
                        vCharAdvances.slice(i until glyphIds.size).toFloatArray(),
                        vCharTsb.slice(i until glyphIds.size).toFloatArray(),
                        hCharAdvances.slice(i until glyphIds.size).toFloatArray()
                    )
                    return Pair(current, next)
                }
            } else {
                remaining -= vChar
            }
        }
        return null
    }

    override fun verticalMetrics(paint: Paint): VerticalFontMetrics =
        VerticalFontMetrics(paint.textSize * 0.5f, paint.textSize * 0.5f)

    override fun toString(): String {
        return "UprightVerticalLayoutRun(glyphIds=${glyphIds.contentToString()}, fonts=${fonts.contentToString()}, vCharAdvances=${vCharAdvances.contentToString()}, vCharTsb=${vCharTsb.contentToString()}, hCharAdvances=${hCharAdvances.contentToString()}, height=$height)"
    }
}

class IntrinsicVerticalLayout(
    val text: CharSequence,
    val start: Int,
    val end: Int,
    val vMetrics: Paint.FontMetrics,
    val runs: List<VerticalLayoutRun>
) {
    fun draw(canvas: Canvas, x: Float, y: Float, paint: Paint) {
        var x = x
        var y = y
        runs.forEach { run ->
            run.draw(canvas, x, y, paint, run.verticalMetrics(paint))

            x += 0f
            y += run.height
        }
    }

    val height: Float
        get() = runs.fold(0f) { acc, run -> acc + run.height }

    override fun toString(): String {
        return runs.fold("") { acc, it -> "$acc,$it" }
    }
}

class VerticalLine(
    val text: CharSequence,
    val lineStart: Int,
    val lineEnd: Int,
    val runs: List<VerticalLayoutRun>
) {
    fun draw(canvas: Canvas, x: Float, y: Float, paint: Paint) {
        var x = x
        var y = y
        runs.forEach { run ->
            run.draw(canvas, x, y, paint, run.verticalMetrics(paint))

            x += 0f
            y += run.height
        }
    }

    fun verticalMetrics(paint: Paint) : VerticalFontMetrics {
        var left = 0f
        var right = 0f
        runs.forEach { run ->
            val runMetrics = run.verticalMetrics(paint)
            left = max(left, runMetrics.left)
            right = max(right, runMetrics.right)
        }
        return VerticalFontMetrics(left, right)
    }

    companion object {
        fun breakLine(layout: IntrinsicVerticalLayout, height: Float): List<VerticalLine> {
            val result = mutableListOf<VerticalLine>()
            var lineRuns = mutableListOf<VerticalLayoutRun>()
            var lineStart = layout.start

            var curHeight = 0f
            for (i in 0 until layout.runs.size) {
                var run = layout.runs[i]

                while (curHeight + run.height > height) {
                    val lineBreak = run.split(height - curHeight)
                    if (lineBreak == null) {
                        result.add(VerticalLine(layout.text, lineStart, run.start, lineRuns))
                        lineRuns = mutableListOf()
                        lineStart = run.start
                        curHeight = 0f
                        break
                    } else {
                        val next = lineBreak.second

                        lineRuns.add(lineBreak.first)
                        result.add(VerticalLine(layout.text, lineStart, next.start, lineRuns))
                        lineRuns = mutableListOf()
                        curHeight = 0f
                        lineStart = next.start
                        run = next
                    }
                }

                lineRuns.add(run)
                curHeight += run.height
            }
            result.add(VerticalLine(layout.text, lineStart, layout.end, lineRuns))
            return result
        }
    }
}