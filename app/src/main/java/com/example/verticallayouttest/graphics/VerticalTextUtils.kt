package com.example.verticallayouttest.graphics

import android.icu.lang.UCharacter
import android.icu.lang.UCharacter.VerticalOrientation
import android.icu.lang.UProperty
import android.text.Spanned
import com.example.verticallayouttest.graphics.TextOrientation
import java.text.BreakIterator
import java.text.CharacterIterator

object VerticalTextUtils {

    private fun drawOrientation(
        textOrientation: TextOrientation,
        verticalOrientation: Int
    ): DrawOrientation {
        return when (textOrientation) {
            TextOrientation.Mixed -> when (verticalOrientation) {
                VerticalOrientation.ROTATED -> DrawOrientation.Rotate
                else -> DrawOrientation.Upright
            }
            TextOrientation.Upright -> DrawOrientation.Upright
            TextOrientation.Sideways -> DrawOrientation.Rotate
        }
    }

    sealed class DrawOrientationRun(
        val orientation: DrawOrientation,
        val length: Int,
    )

    class UprightDrawOrientationRun(
        length: Int
    ) : DrawOrientationRun(DrawOrientation.Upright, length)

    class RotateDrawOrientationRun(
        length: Int
    ) : DrawOrientationRun(DrawOrientation.Rotate, length)

    class TateChuYokoDrawOrientationRun(
        length: Int
    ): DrawOrientationRun(DrawOrientation.TateChuYoko, length)
    class RubyDrawOrientationRun(
        length: Int,
        val contentRuns: List<DrawOrientationRun>,
        val rubySpan: RubySpan,
        val rubyRuns: List<DrawOrientationRun>
    ) :DrawOrientationRun(DrawOrientation.Ruby, length)

    fun analyzeVerticalOrientation(
        text: CharSequence,
        start: Int = 0,
        end: Int = text.length,
        textOrientation: TextOrientation
    ): List<DrawOrientationRun> {

        val result = mutableListOf<DrawOrientationRun>()
        if (start == end) {
            result.toTypedArray()
        }

        val spanned = if (text is Spanned) text else {
            processNonTateChuYoko(text, start, end, textOrientation, result)
            return result
        }

        var spanI = start
        while (spanI < end) {
            val next = spanned.nextSpanTransition(spanI, end, RubySpan::class.java)

            val rubySpans = spanned.getSpans(spanI, next, RubySpan::class.java)

            if (rubySpans.isNotEmpty()) {
                val rubyContentRuns = mutableListOf<DrawOrientationRun>()
                processTateChuYokoSpan(spanned, spanI, next, textOrientation, rubyContentRuns)
                val rubyTextRuns = mutableListOf<DrawOrientationRun>()
                val rubySpan = rubySpans[0] as RubySpan
                val rubyText = rubySpan.text
                if (rubyText is Spanned) {
                    processTateChuYokoSpan(rubyText, 0, rubyText.length, rubySpan.orientation, rubyTextRuns)
                } else {
                    processNonTateChuYoko(rubyText, 0, rubyText.length, rubySpan.orientation, rubyTextRuns)
                }
                result.add(RubyDrawOrientationRun(next - spanI, rubyContentRuns, rubySpan, rubyTextRuns))
            } else {
                processTateChuYokoSpan(spanned, spanI, next, textOrientation, result)
            }

            spanI = next
        }
        return result
    }

    private fun processTateChuYokoSpan(spanned: Spanned, start: Int, end: Int, textOrientation: TextOrientation,
                                       result: MutableList<DrawOrientationRun>) {
        var spanI = start
        while (spanI < end) {
            val next = spanned.nextSpanTransition(spanI, end, TextCombineUprightSpan::class.java)

            val hasSpan = spanned.getSpans(spanI, next, TextCombineUprightSpan::class.java).isNotEmpty()

            if (hasSpan) {
                result.add(TateChuYokoDrawOrientationRun(next - spanI))
            } else {
                processNonTateChuYoko(spanned, spanI, next, textOrientation, result)
            }

            spanI = next
        }
    }

    private fun processNonTateChuYoko(text: CharSequence, start: Int, end: Int, textOrientation: TextOrientation,
             result: MutableList<DrawOrientationRun>) {
        val grIter = BreakIterator.getCharacterInstance()
        grIter.text = StringCharacterIterator(text, start, end)

        var prevProp = DrawOrientation.Rotate // unused init value
        var prevStart = start
        var i = start
        while (i < end) {
            val cp = Character.codePointAt(text, i)

            val vertOrientation = UCharacter.getIntPropertyValue(cp, UProperty.VERTICAL_ORIENTATION)
            val prop = drawOrientation(textOrientation, vertOrientation)
            if (i == start) {
                prevProp = prop
                prevStart = start
            } else if (prevProp != prop) {
                val run = when (prevProp) {
                    DrawOrientation.Upright -> UprightDrawOrientationRun(i - prevStart)
                    DrawOrientation.Rotate -> RotateDrawOrientationRun(i - prevStart)
                    else -> throw RuntimeException("Unreachable code")
                }
                result.add(run)
                prevProp = prop
                prevStart = i
            }

            i = grIter.following(i)
        }

        val run = when (prevProp) {
            DrawOrientation.Upright -> UprightDrawOrientationRun(i - prevStart)
            DrawOrientation.Rotate -> RotateDrawOrientationRun(i - prevStart)
            else -> throw RuntimeException("Unreachable code")
        }
        result.add(run)
    }

    class StringCharacterIterator(
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