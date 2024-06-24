package com.example.verticallayouttest.graphics

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.fonts.Font
import android.graphics.fonts.SystemFonts
import android.icu.lang.UCharacter
import android.icu.lang.UProperty
import android.os.LocaleList
import java.util.Locale

enum class TextOrientation {
    Mixed,
    Upright,
    Sideways,
}

class TextCombineUprightSpan
class RubySpan(
    val text: CharSequence,
    val orientation: TextOrientation = TextOrientation.Mixed,
    val textScale: Float = 0.5f
)

class VerticalLayout(
    val text: CharSequence,
    val start: Int,
    val end: Int,
    val lines: List<VerticalLine>
) {

    fun draw(canvas: Canvas, x: Float, y: Float, paint: Paint) {
        var x = x
        lines.forEach { it ->
            val verticalMetrics = it.verticalMetrics(paint)

            x -= verticalMetrics.right

            it.draw(canvas, x, y, paint)

            x -= verticalMetrics.left
        }
    }

    companion object {
        fun build(text: CharSequence, start: Int, end: Int, orientation: TextOrientation, paint: Paint, height: Float) : VerticalLayout {
            val lines = mutableListOf<VerticalLine>()

            val measure = VerticalTextMeasure(Locale.JAPANESE)

            var paraStart = 0
            while (paraStart < end) {
                val paraEnd = text.indexOf('\n', paraStart + 1).let { if (it == -1) end else it }

                val intrinsic = measure.layoutText(text, paraStart, paraEnd, orientation, paint)
                android.util.Log.e("Debug", "$intrinsic")
                lines.addAll(VerticalLine.breakLine(intrinsic, height))

                paraStart = paraEnd
            }
            return VerticalLayout(text, start, end, lines)
        }
    }
}