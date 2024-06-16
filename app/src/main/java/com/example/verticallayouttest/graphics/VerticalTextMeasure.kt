package com.example.verticallayouttest.graphics

import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.fonts.Font
import android.graphics.fonts.FontFamily
import android.graphics.fonts.SystemFonts
import android.graphics.text.PositionedGlyphs
import android.graphics.text.TextRunShaper
import java.text.BreakIterator
import java.text.CharacterIterator
import java.util.Collections
import java.util.Locale

class VerticalPaint{
    private val verticalFont: Font
    private val paint = Paint()
    private val grBreaker = BreakIterator.getCharacterInstance()
    private val openType: OpenType
    private val vmtx: OpenTypeTable_vmtx
    private val vrt2: Map<Int, Int>

    // Cached arrays
    private var glyphIds = IntArray(100)
    private var advances = FloatArray(100)
    private var fonts = Array<Font?>(100) { null }

    constructor(verticalFont: Font) {
        this.verticalFont = verticalFont
        this.openType = OpenTypeUtils.parse(verticalFont.buffer, verticalFont.ttcIndex)
        this.vmtx = requireNotNull(this.openType.verticalMetrics)
        this.vrt2 = openType.glyphSubstitution?.getSingleSubstitution("kana", "JAN ", "vrt2")
            ?: Collections.emptyMap()
        initPaint()
    }

    constructor(locale: Locale) {
        this.verticalFont = SystemFonts.getAvailableFonts()
            .filterNotNull()
            .first {
                var foundLocale = false
                for (i in 0 until it.localeList.size()) {
                    if (it.localeList.get(i).language == locale.language) {
                        foundLocale = true;
                    }
                }
                foundLocale
            }
        this.openType = OpenTypeUtils.parse(verticalFont.buffer, verticalFont.ttcIndex)
        this.vmtx = requireNotNull(this.openType.verticalMetrics)
        this.vrt2 = openType.glyphSubstitution?.getSingleSubstitution("kana", "JAN ", "vrt2")
            ?: Collections.emptyMap()
        initPaint()
    }

    private fun initPaint() {
        paint.fontFeatureSettings = "\"liga\" off, \"clig\" off"
        paint.typeface = Typeface.CustomFallbackBuilder(
            FontFamily.Builder(verticalFont).build()
        ).build()
    }

    private fun fillGlyph(glyphs: PositionedGlyphs) {
        if (glyphIds.size >= glyphs.glyphCount()) {
            glyphIds = IntArray(glyphs.glyphCount())
            fonts = Array(glyphs.glyphCount()) { null }
        }
        for (i in 0 until glyphs.glyphCount()) {
            glyphIds[i] = glyphs.getGlyphId(i)
            val font = glyphs.getFont(i)
            // Fast equality check.
            val isVerticalFont =
                (System.identityHashCode(font) == System.identityHashCode(verticalFont)) ||
                        (font.buffer.capacity() == verticalFont.buffer.capacity())
            if (isVerticalFont) {
                fonts[i] = null
            } else {
                fonts[i] = font
            }
        }
    }

    data class VerticalTextRunLayout(
        val glyphIds: IntArray,
        val advances: FloatArray,
        val fonts: Array<Font>,
    )

    fun layoutText(
        text: CharSequence,
        start: Int,
        end: Int
    ) : VerticalTextRunLayout {
        val glyphs = TextRunShaper.shapeTextRun(text, start, end, start, end, 0f, 0f, false, paint)

        val glyphIds = IntArray(glyphs.glyphCount()) {
            val gID = glyphs.getGlyphId(it)
            vrt2.get(gID) ?: gID
        }
        val fonts = Array(glyphs.glyphCount()) {
            val font = glyphs.getFont(it)
            val isVerticalFont =
                (System.identityHashCode(font) == System.identityHashCode(verticalFont)) ||
                        (font.buffer.capacity() == verticalFont.buffer.capacity())
            if (isVerticalFont) {
                verticalFont
            } else {
                font
            }
        }
        val advances = FloatArray(glyphs.glyphCount()) {
            vmtx.getVAdvance(glyphIds[it]) * paint.textSize
        }

        return VerticalTextRunLayout(glyphIds, advances, fonts)
    }

    private class StringCharacterIterator(
        val cs: CharSequence,
        val start: Int,
        val end: Int,
        var offset: Int = start
    ) : CharacterIterator {
        override fun clone(): Any = StringCharacterIterator(cs, start, end, offset)
        override fun first(): Char = cs[start]
        override fun last(): Char = cs[end - 1]
        override fun current(): Char = cs[offset]
        override fun next(): Char = cs[++offset]
        override fun previous(): Char = cs[--offset]
        override fun getBeginIndex(): Int = start
        override fun getEndIndex(): Int = end
        override fun getIndex(): Int = offset
        override fun setIndex(position: Int): Char = cs[position].also{ offset = position }
    }

}