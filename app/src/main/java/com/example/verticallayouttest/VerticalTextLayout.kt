package com.example.verticallayouttest

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.fonts.Font
import android.graphics.text.PositionedGlyphs
import android.graphics.text.TextRunShaper
import android.util.Log
import java.util.IdentityHashMap

class VerticalTextLayout(
    val paint: Paint,
    val glyphs: PositionedGlyphs,
    val otCache: HashMap<Font, OpenType>,
    val glyphIds: IntArray,
    val vAdvances: FloatArray,
    val hAdvances: FloatArray,
) {

    companion object {
        fun build(text: String, paint: Paint): VerticalTextLayout {
            val glyphs = TextRunShaper.shapeTextRun(text, 0, text.length, 0, text.length, 0f, 0f, false, paint)

            val otCache = HashMap<Font, OpenType>()
            val vAdvances = FloatArray(glyphs.glyphCount())
            val hAdvances = FloatArray(glyphs.glyphCount())
            val glyphIds = IntArray(glyphs.glyphCount())

            for (i in 0 until glyphs.glyphCount()) {
                val font = glyphs.getFont(i)
                val ot = otCache.getOrPut(font) { OpenTypeUtils.parse(font.buffer, font.ttcIndex) }
                val map = ot.glyphSubstitution?.getSingleSubstitution("kana", "JAN ", "vrt2")
                if (map == null) {
                    // Vertical text is not supported
                    val glyphId = glyphs.getGlyphId(i)
                    glyphIds[i] = glyphId
                    vAdvances[i] = 0f
                    hAdvances[i] = ot.horizontalMetrics?.getHAdvance(glyphId) ?: 0f
                    hAdvances[i] *= paint.textSize
                } else {
                    val originalGlyphId = glyphs.getGlyphId(i)
                    val glyphId = map.getOrDefault(originalGlyphId, originalGlyphId)
                    glyphIds[i] = glyphId
                    vAdvances[i] = ot.verticalMetrics?.getVAdvance(glyphId) ?: 0f
                    vAdvances[i] *= paint.textSize
                    hAdvances[i] = ot.horizontalMetrics?.getHAdvance(glyphId) ?: 0f
                    hAdvances[i] *= paint.textSize
                }
            }

            return VerticalTextLayout(paint, glyphs, otCache, glyphIds, vAdvances, hAdvances)
        }
    }

    val baselinePaint = Paint().apply {
        color = Color.GREEN
        strokeWidth = 5f
    }

    fun draw(c: Canvas) {

        val baselineX = 200f

        c.drawLine(baselineX, 0f, baselineX, c.height.toFloat(), baselinePaint)

        val positions = FloatArray(glyphs.glyphCount() * 2)
        var prev = 0f
        for (i in 0 until glyphIds.size) {
            positions[i * 2] = baselineX
            positions[i * 2 + 1] = prev + vAdvances[i]
            prev += vAdvances[i]
        }

        c.drawGlyphs(glyphIds, 0, positions, 0, glyphIds.size, glyphs.getFont(0), paint)

    }

}