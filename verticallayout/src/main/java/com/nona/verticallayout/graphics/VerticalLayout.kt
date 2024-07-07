package com.nona.verticallayout.graphics

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.fonts.Font
import android.graphics.fonts.SystemFonts
import android.icu.lang.UCharacter
import android.icu.lang.UProperty
import android.os.LocaleList
import android.util.Log
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

class VerticalLayout private constructor(
    val text: CharSequence,
    val lines: List<VerticalLineMetrics>,
    val lineLayouts: List<VerticalLine>
) {

    val width: Float
        get() = -lines.last().let { it.baseline - it.left}

    fun draw(canvas: Canvas, x: Float, y: Float, paint: Paint) {
        if (lineLayouts.isEmpty()) {
            Log.e("Debug", "Need to calculate full layout for drawing")
        }
        for (i in lines.indices) {
            lineLayouts[i].draw(canvas, x + lines[i].baseline, y, paint)
        }
    }

    companion object {
        fun build(text: CharSequence,
                  start: Int,
                  end: Int,
                  orientation: TextOrientation,
                  paint: Paint,
                  height: Float,
                  lineSpacing: Float,
                  computeFullLayout: Boolean = true,
        ) : VerticalLayout {
            val lines = mutableListOf<VerticalLineMetrics>()
            val layouts = mutableListOf<VerticalLine>()

            val measure = VerticalTextMeasure(Locale.JAPANESE)

            var baseline = 0f
            var paraStart = start
            while (paraStart < end) {
                val paraEnd = text.indexOf('\n', paraStart + 1).let { if (it == -1) end else it }

                val intrinsic = measure.layoutText(text, paraStart, paraEnd, orientation, paint)
                val paraLines = VerticalLine.breakLine(intrinsic, height, baseline, lineSpacing)

                if (computeFullLayout) {
                    for (i in paraLines.indices) {
                        val lineStart = paraLines[i].offset
                        val lineEnd = if (i == paraLines.size - 1) {
                            paraEnd
                        } else {
                            paraLines[i + 1].offset
                        }
                        layouts.add(VerticalLine.layoutLine(lineStart, lineEnd, intrinsic))
                    }
                }

                lines.addAll(paraLines)
                baseline = paraLines.last().let { it.baseline - it.left  }
                paraStart = paraEnd
            }
            return VerticalLayout(text, lines, layouts)
        }
    }
}