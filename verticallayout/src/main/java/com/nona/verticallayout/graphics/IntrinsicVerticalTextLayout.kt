package com.nona.verticallayout.graphics

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Paint.FontMetrics
import android.graphics.fonts.Font
import android.util.Log
import com.nona.verticallayout.graphics.debug.DebugPaints
import kotlin.math.max
import kotlin.math.min

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

    abstract fun split(startOuter: Int, height: Float): Pair<VerticalLayoutRun, VerticalLayoutRun>?
    abstract fun split(startOuter: Int, endOuter: Int): VerticalLayoutRun?
    abstract fun breakAt(startOuter: Int, height: Float): Pair<Int, Float>  // break offset, consumed height
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
    override fun split(startOuter: Int, height: Float): Pair<VerticalLayoutRun, VerticalLayoutRun>? = null
    override fun split(startOuter: Int, endOuter: Int): VerticalLayoutRun? {
        if (startOuter == start && endOuter == end) {
            return this
        }
        return null
    }
    override fun breakAt(start: Int, height: Float): Pair<Int, Float> = Pair(-1, 0f)

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
    override fun split(startOuter: Int, height: Float): Pair<VerticalLayoutRun, VerticalLayoutRun>? = null
    override fun split(startOuter: Int, endOuter: Int): VerticalLayoutRun? {
        if (startOuter == start && endOuter == end) {
            return this
        }
        return null
    }
    override fun breakAt(start: Int, height: Float): Pair<Int, Float> = Pair(-1, 0f)

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

    override fun split(startOuter: Int, height: Float): Pair<VerticalLayoutRun, VerticalLayoutRun>? {
        // TODO: Implement line break for Rotate run.
        return null
    }
    override fun split(startOuter: Int, endOuter: Int): VerticalLayoutRun? {
        if (startOuter == start && endOuter == end) {
            return this
        }
        return null
    }
    override fun breakAt(start: Int, height: Float): Pair<Int, Float> = Pair(-1, 0f)

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

    override fun split(startOuter: Int, height: Float): Pair<VerticalLayoutRun, VerticalLayoutRun>? {
        val startInner = startOuter - start
        val (breakIndexOuter, _) = breakAt(startOuter, height)
        if (breakIndexOuter == -1) {
            return null
        }
        val breakIndexInner = breakIndexOuter - start
        val current = UprightVerticalLayoutRun(
            text, startOuter, breakIndexOuter,
            glyphIds.slice(startInner until breakIndexInner).toIntArray(),
            fonts.slice(startInner until breakIndexInner).toTypedArray(),
            vCharAdvances.slice(startInner until breakIndexInner).toFloatArray(),
            hCharAdvances.slice(startInner until breakIndexInner).toFloatArray()
        )
        val next = UprightVerticalLayoutRun(
            text, start + breakIndexInner, end,
            glyphIds.slice(breakIndexInner until glyphIds.size).toIntArray(),
            fonts.slice(breakIndexInner until glyphIds.size).toTypedArray(),
            vCharAdvances.slice(breakIndexInner until glyphIds.size).toFloatArray(),
            hCharAdvances.slice(breakIndexInner until glyphIds.size).toFloatArray()
        )
        return Pair(current, next)
    }
    override fun split(startOuter: Int, endOuter: Int): VerticalLayoutRun? {
        val startInner = startOuter - start
        val endInner = endOuter - start
        return UprightVerticalLayoutRun(
            text, startOuter, endInner,
            glyphIds.slice(startInner until endInner).toIntArray(),
            fonts.slice(startInner until endInner).toTypedArray(),
            vCharAdvances.slice(startInner until endInner).toFloatArray(),
            hCharAdvances.slice(startInner until endInner).toFloatArray()
        )
    }

    override fun breakAt(startOuter: Int, height: Float): Pair<Int, Float> {
        var remaining = height
        val startInner = startOuter - start
        for (i in startInner until vCharAdvances.size) {
            val vChar = vCharAdvances[i]
            if (remaining < vChar) {
                return if (i == 0) {
                    Pair(-1, 0f) // Unable to fit only one char.
                } else {
                    Pair(start + i, height - remaining)
                }
            } else {
                remaining -= vChar
            }
        }
        return Pair(-1, 0f)
    }

    override fun verticalMetrics(paint: Paint): VerticalFontMetrics =
        VerticalFontMetrics(paint.textSize * 0.5f, paint.textSize * 0.5f)

    override fun toString(): String {
        return "UprightVerticalLayoutRun(glyphIds=${glyphIds.contentToString()}, fonts=${fonts.contentToString()}, vCharAdvances=${vCharAdvances.contentToString()}, hCharAdvances=${hCharAdvances.contentToString()}, height=$height)"
    }
}

class IntrinsicVerticalLayout(
    val text: CharSequence,
    val start: Int,
    val end: Int,
    val vMetrics: Paint.FontMetrics,
    val runs: List<VerticalLayoutRun>,
    val paint: Paint,
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

class VerticalLineMetrics(
    val offset: Int,
    val left: Float,
    val right: Float,
    val baseline: Float,
)

class VerticalLine(
    val text: CharSequence,
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

        fun breakLine(layout: IntrinsicVerticalLayout, height: Float, x: Float): List<VerticalLineMetrics> {
            val result = mutableListOf<VerticalLineMetrics>()
            var lineStart = layout.start
            var baseline = x
            var left = 0f
            var right = 0f

            var curHeight = 0f
            for (i in 0 until layout.runs.size) {
                val run = layout.runs[i]
                var runStart = run.start
                var runHeight = run.height
                val vRunMetrics = run.verticalMetrics(layout.paint)
                left = max(left, vRunMetrics.left)
                right = max(right, vRunMetrics.right)

                while (curHeight + runHeight > height) {
                    val (breakIndex, consumed) = run.breakAt(runStart, height - curHeight)
                    if (breakIndex == -1) {
                        baseline -= right
                        result.add(VerticalLineMetrics(lineStart, left, right, baseline))
                        baseline -= left
                        lineStart = run.start
                        curHeight = 0f
                        left = vRunMetrics.left
                        right = vRunMetrics.right
                        break
                    } else {
                        baseline -= right
                        result.add(VerticalLineMetrics(lineStart, left, right, baseline))
                        baseline -= left
                        lineStart = breakIndex
                        runStart = breakIndex
                        curHeight = 0f
                        runHeight -= consumed
                        left = vRunMetrics.left
                        right = vRunMetrics.right

                    }
                }
                curHeight += runHeight
            }
            baseline -= right
            result.add(VerticalLineMetrics(lineStart, left, right, baseline))
            return result
        }

        fun layoutLine(start: Int, end: Int, layout: IntrinsicVerticalLayout): VerticalLine {
            val lineRuns = mutableListOf<VerticalLayoutRun>()

            for (i in 0 until layout.runs.size) {
                val run = layout.runs[i]
                if (end < run.start) {
                    break
                }

                val intersectStart = max(start, run.start)
                val intersectEnd = min(end, run.end)
                if (intersectStart < intersectEnd) {
                    lineRuns.add(run.split(intersectStart, intersectEnd)!!)
                }
            }

            return VerticalLine(layout.text, lineRuns)
        }
    }
}