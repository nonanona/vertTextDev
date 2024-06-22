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
    val vCharAdvances: FloatArray?,
    val hCharAdvances: FloatArray,
    val drawOrientation: DrawOrientation
) {
    val height: Float by lazy {
        when (drawOrientation) {
            DrawOrientation.Rotate -> hCharAdvances.sum()
            DrawOrientation.Upright -> vCharAdvances!!.sum()
            DrawOrientation.TateChuYoko -> TODO()
        }
    }

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

        val vCharAdvances = vCharAdvances!!

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
            y += vCharAdvances[charIndex]
            val w = hCharAdvances[charIndex]

            charIndex++
            while (charIndex < vCharAdvances.size && vCharAdvances[charIndex] == 0f) {
                charIndex++
            }

            drawPositions[glyphIndex * 2] = x - w / 2
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

class IntrinsicVerticalLayout(
    val text: CharSequence,
    val openType: OpenType,
    val pant: Paint,
    val metrics: Paint.FontMetrics,
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

            run.vCharAdvances?.forEachIndexed { j, advance ->
                out += " $j: $advance, "
                if (j % 8 == 7) {
                    out += "\n"
                }
            }
            out += "\n"
        }
        return out
    }


    val linePaint = Paint().apply {
        color = Color.GREEN
        strokeWidth = 5f
    }

    fun draw(canvas: Canvas, x: Float, y: Float, start: Int, end: Int, paint: Paint) {

        val cH = canvas.height.toFloat()
        canvas.drawLine(x, 0f, x, cH, linePaint.apply { color = Color.GREEN })
        canvas.drawLine(x - metrics.descent, 0f, x - metrics.descent, cH, linePaint.apply { color = Color.BLUE })
        canvas.drawLine(x - metrics.ascent, 0f, x - metrics.ascent, cH, linePaint.apply { color = Color.RED })

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