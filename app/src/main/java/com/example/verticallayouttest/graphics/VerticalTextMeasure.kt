package com.example.verticallayouttest.graphics

import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.fonts.Font
import android.graphics.fonts.FontFamily
import android.graphics.fonts.SystemFonts
import android.graphics.text.TextRunShaper
import java.text.BreakIterator
import java.text.CharacterIterator
import java.util.Collections
import java.util.Locale

class VerticalTextMeasure {
    private val verticalFont: Font
    private val paint = Paint()
    private val openType: OpenType
    private val vmtx: OpenTypeTable_vmtx
    private val vrt2: Map<Int, Int>

    constructor(verticalFont: Font, paint: Paint) {
        this.paint.set(paint)
        this.verticalFont = verticalFont
        this.openType = OpenTypeUtils.parse(verticalFont.buffer, verticalFont.ttcIndex)
        this.vmtx = requireNotNull(this.openType.verticalMetrics)
        this.vrt2 = openType.glyphSubstitution?.getSingleSubstitution("kana", "JAN ", "vrt2")
            ?: Collections.emptyMap()
        initPaint()
    }

    constructor(locale: Locale, paint: Paint) {
        this.paint.set(paint)
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

    data class VerticalTextRunLayout(
        val glyphIds: IntArray,
        val fonts: Array<Font>,
        val charAdvances: FloatArray)

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
        val advances = FloatArray(text.length)

        val grIter = BreakIterator.getCharacterInstance()
        grIter.text = StringCharacterIterator(text, start, end)
        var i = grIter.first()
        var gID = 0
        while (i != BreakIterator.DONE) {
            val next = grIter.next()
            if (next == BreakIterator.DONE) {
                break
            }
            if (i != next) {
                advances[i] = vmtx.getVAdvance(glyphIds[gID++]) * paint.textSize
            }
            i = next
        }

        return VerticalTextRunLayout(glyphIds, fonts, advances)
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
        override fun current(): Char = if (offset == end) {
            CharacterIterator.DONE
        } else {
            cs[offset]
        }
        override fun next(): Char = if (offset >= end - 1) {
            offset = end
            CharacterIterator.DONE
        } else {
            cs[++offset]
        }
        override fun previous(): Char = if (offset <= start){
            offset = start
            CharacterIterator.DONE
        } else {
            cs[--offset]
        }
        override fun getBeginIndex(): Int = start
        override fun getEndIndex(): Int = end
        override fun getIndex(): Int = offset
        override fun setIndex(position: Int): Char = if (position == end) {
            CharacterIterator.DONE
        } else {
            offset = position
            cs[position]
        }
    }

}