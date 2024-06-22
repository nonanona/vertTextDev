package com.example.verticallayouttest.graphics

import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.fonts.Font
import android.graphics.fonts.FontFamily
import android.graphics.fonts.SystemFonts
import android.graphics.text.TextRunShaper
import android.icu.lang.UCharacter.VerticalOrientation
import android.text.TextUtils
import android.util.Log
import com.example.verticallayouttest.graphics.VerticalLayout.TextOrientation
import java.text.BreakIterator
import java.util.Collections
import java.util.Locale

class VerticalTextMeasure {
    private val verticalFont: Font
    private val verticalTypeface: Typeface
    private val openType: OpenType
    private val vmtx: OpenTypeTable_vmtx
    private val vrt2: Map<Int, Int>

    constructor(verticalFont: Font) {
        this.verticalFont = verticalFont
        this.openType = OpenTypeUtils.parse(verticalFont.buffer, verticalFont.ttcIndex)
        this.vmtx = requireNotNull(this.openType.verticalMetrics)
        this.vrt2 = openType.glyphSubstitution?.getSingleSubstitution("kana", "JAN ", "vrt2")
            ?: Collections.emptyMap()
        this.verticalTypeface = Typeface.CustomFallbackBuilder(
            FontFamily.Builder(verticalFont).build()
        ).build()
    }

    constructor(locale: Locale) : this(SystemFonts.getAvailableFonts()
        .filterNotNull()
        .first {
            var foundLocale = false
            for (i in 0 until it.localeList.size()) {
                if (it.localeList.get(i).language == locale.language) {
                    foundLocale = true;
                }
            }
            foundLocale
        })

    private fun checkFont(font: Font) : Font {
        val isVerticalFont =
            (System.identityHashCode(font) == System.identityHashCode(verticalFont)) ||
                    (font.buffer.capacity() == verticalFont.buffer.capacity())
        return if (isVerticalFont) {
            verticalFont
        } else {
            font
        }
    }

    private fun shape(text: CharSequence, start: Int, end: Int, paint: Paint) =
        TextRunShaper.shapeTextRun(text, start, end - start, start, end - start, 0f, 0f, false, paint)

    private fun layoutTextRunUpright(
        text: CharSequence,
        runStart: Int,
        runEnd: Int,
        paint: Paint,
    ): OrientationRun {
        val glyphs = shape(text, runStart, runEnd, paint)

        val chars = CharArray(runEnd - runStart)
        val hAdvances = FloatArray(runEnd - runStart)
        TextUtils.getChars(text, runStart, runEnd, chars, 0)
        paint.getTextRunAdvances(chars, 0, chars.size, 0, chars.size, false, hAdvances, 0)

        val glyphIds = IntArray(glyphs.glyphCount())
        val fonts = Array(glyphs.glyphCount()) { verticalFont }
        val vAdvances = FloatArray(runEnd - runStart)

        val grIter = BreakIterator.getCharacterInstance()
        grIter.text = VerticalTextUtils.StringCharacterIterator(text, runStart, runEnd)

        var i = grIter.first()
        var outGlyphIndex = 0
        while (i != BreakIterator.DONE) {
            val next = grIter.next()
            if (next == BreakIterator.DONE) {
                break
            }
            if (i != next) {
                val gID = glyphs.getGlyphId(outGlyphIndex)
                glyphIds[outGlyphIndex] = vrt2[gID] ?: gID
                fonts[outGlyphIndex] = checkFont(glyphs.getFont(outGlyphIndex))
                vAdvances[i - runStart] = vmtx.getVAdvance(glyphIds[outGlyphIndex]) * paint.textSize
                outGlyphIndex++
            }
            i = next
        }

        if (outGlyphIndex != glyphs.glyphCount()) {
            Log.e("Debug", "The Grapheme count doesn't match with glyph count. $outGlyphIndex v.s. ${glyphs.glyphCount()}")
        }
        return OrientationRun(text, runStart, runEnd, glyphIds, fonts, vAdvances, hAdvances, DrawOrientation.Upright)
    }

    private fun layoutTextRunRotate(
        text: CharSequence,
        runStart: Int,
        runEnd: Int,
        paint: Paint,
    ): OrientationRun {
        val chars = CharArray(runEnd - runStart)
        val advances = FloatArray(runEnd - runStart)
        TextUtils.getChars(text, runStart, runEnd, chars, 0)
        paint.getTextRunAdvances(chars, 0, chars.size, 0, chars.size, false, advances, 0)
        return OrientationRun(text, runStart, runEnd, null, null, null, advances, DrawOrientation.Rotate)
    }

    fun layoutText(
        text: CharSequence,
        start: Int,
        end: Int,
        textOrientation: TextOrientation = TextOrientation.Mixed,
        paint: Paint,
    ) : IntrinsicVerticalLayout {
        paint.typeface = this.verticalTypeface

        // Outputs
        val result = mutableListOf<OrientationRun>()

        val vertRuns = VerticalTextUtils.analyzeVerticalOrientation(text, start, end, textOrientation)
        var runStart = start
        for (run in vertRuns) {
            val runEnd = runStart + run.length

            when (run.drawOrientation) {
                DrawOrientation.Upright -> {
                    result.add(layoutTextRunUpright(text, runStart, runEnd, paint))
                }
                DrawOrientation.Rotate -> {
                    result.add(layoutTextRunRotate(text, runStart, runEnd, paint))
                }
                DrawOrientation.TateChuYoko -> TODO()
            }

            runStart = runEnd
        }

        val fm = Paint.FontMetrics()
        val vhea = openType.verticalHeader
        if (vhea == null) {
            fm.ascent = -0.5f * paint.textSize
            fm.descent = 0.5f * paint.textSize
        } else {
            fm.ascent = vhea.ascender * paint.textSize
            fm.descent = vhea.descender * paint.textSize
        }
        fm.top = fm.ascent
        fm.bottom = fm.descent


        return IntrinsicVerticalLayout(text, openType, paint, fm, result)
    }
}