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
) {

    companion object {
        fun build(text: String, inPaint: Paint): VerticalTextLayout {
            val paint = Paint(inPaint).apply {
                setFontVariationSettings("'vrt2' 1, 'vert' 1")
            }
            val glyphs = TextRunShaper.shapeTextRun(text, 0, text.length, 0, text.length, 0f, 0f, false, paint)

            val otCache = HashMap<Font, OpenType>()
            val vAdvances = FloatArray(glyphs.glyphCount())
            val glyphIds = IntArray(glyphs.glyphCount())

            for (i in 0 until glyphs.glyphCount()) {
                val font = glyphs.getFont(i)
                val ot = otCache.getOrPut(font) { OpenTypeUtils.parse(font.buffer, font.ttcIndex) }
                val map = ot.glyphSubstitution?.getSingleSubstitution("kana", "JAN ", "vrt2")!!
                val originalGlyphId = glyphs.getGlyphId(i)
                val glyphId = map.getOrDefault(originalGlyphId, originalGlyphId)
                glyphIds[i] = glyphId
                vAdvances[i] = ot.verticalMetrics?.getVAdvance(glyphId) ?: 0f
                vAdvances[i] *= paint.textSize
            }

            return VerticalTextLayout(paint, glyphs, otCache, glyphIds, vAdvances)
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