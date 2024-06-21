package com.example.verticallayouttest.graphics

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.fonts.Font
import android.util.Log

enum class DrawOrientation {
    Upright,
    Rotate,
    TateChuYoko,
}

class OrientationRun(
    val text: CharSequence,
    val start: Int,
    val end: Int,
    val glyphIds: IntArray?,
    val fonts: Array<Font>?,
    val charAdvances: FloatArray,
    val drawOrientation: DrawOrientation
) {
    val height: Float by lazy { charAdvances.sum() }

    fun draw(canvas: Canvas, x: Float, y: Float, start: Int, end: Int, paint: Paint) {
        when (drawOrientation) {
            DrawOrientation.Upright -> drawUpright(canvas, x, y, start, end, paint)
            DrawOrientation.Rotate -> drawRotate(canvas, x, y, start, end, paint)
            DrawOrientation.TateChuYoko -> TODO()
        }
    }

    private fun drawUpright(canvas: Canvas, x: Float, y: Float, start: Int, end: Int, paint: Paint) {
        val glyphIds = this.glyphIds!!
        val fonts = this.fonts!!

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
            if (prevFont !== curFont) {
                Log.e("Debug", "font = ${prevFont.file}, glyphs = ${java.util.Arrays.toString(drawGlyphs)}, positions = ${java.util.Arrays.toString(drawPositions)}")
                canvas.drawGlyphs(drawGlyphs, 0,
                    drawPositions, 0,
                    glyphIndex,
                    prevFont,
                    paint)
                prevFont = curFont
                glyphIndex = 0
            }

            x += 0f
            y += charAdvances[charIndex++]

            while (charIndex < charAdvances.size && charAdvances[charIndex] == 0f) {
                charIndex++
            }

            drawPositions[glyphIndex * 2] = x
            drawPositions[glyphIndex * 2 + 1] = y
            drawGlyphs[glyphIndex] = glyphIds[i]
            glyphIndex++
        }

        Log.e("Debug", "font = ${prevFont.file}, glyphs = ${java.util.Arrays.toString(drawGlyphs)}, positions = ${java.util.Arrays.toString(drawPositions)}")
        canvas.drawGlyphs(drawGlyphs, 0,
            drawPositions, 0,
            glyphIndex,
            prevFont,
            paint)
    }

    private fun drawRotate(canvas: Canvas, x: Float, y: Float, start: Int, end: Int, paint: Paint) {
        val fontMetrics = Paint.FontMetricsInt()
        paint.getFontMetricsInt(fontMetrics)
        canvas.save()
        try {
            canvas.rotate(90f, x, y)
            canvas.drawText(text, start, end, x, y - fontMetrics.descent, paint)
        } finally {
            canvas.restore()
        }
    }
}

class IntrinsicVerticalLayout(
    val text: CharSequence,
    val openType: OpenType,
    val runs: List<OrientationRun>
) {
    override fun toString(): String {
        var out = ""
        runs.forEachIndexed { i, run ->
            out += "Orientation[$i]: ${run.drawOrientation} \n"

            if (run.glyphIds != null ) {
                for (i in 0 until run.glyphIds.size) {
                    out += "  { ${run.glyphIds[i]}, ${run.fonts!![i].file} }\n"
                }
            }
            run.charAdvances.forEachIndexed { j, advance ->
                out += " $j: $advance, "
                if (j % 8 == 7) {
                    out += "\n"
                }
            }
            out += "\n"
        }
        return out
    }


    val baselinePaint = Paint().apply {
        color = Color.GREEN
        strokeWidth = 5f
    }


    fun draw(canvas: Canvas, x: Float, y: Float, start: Int, end: Int, paint: Paint) {

        canvas.drawLine(x, 0f, x, canvas.height.toFloat(), baselinePaint)

        var x = x
        var y = y
        runs.forEach { run ->
            run.draw(canvas, x, y, run.start, run.end, paint)

            x += 0f
            y += run.height
        }
    }

    val start: Int
        get() = runs.first().start
    val end: Int
        get() = runs.last().end
}