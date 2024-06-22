package com.example.verticallayouttest.graphics

import android.icu.lang.UCharacter
import android.icu.lang.UCharacter.VerticalOrientation
import android.icu.lang.UProperty
import android.text.Spanned
import com.example.verticallayouttest.graphics.VerticalLayout.TextOrientation
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

    class DrawOrientationRun(
        val drawOrientation: DrawOrientation,
        val length: Int
    )

    fun analyzeVerticalOrientation(
        text: CharSequence,
        start: Int = 0,
        end: Int = text.length,
        textOrientation: TextOrientation
    ): Array<DrawOrientationRun> {

        val result = mutableListOf<DrawOrientationRun>()
        if (start == end) {
            result.toTypedArray()
        }

        val spanned = if (text is Spanned) text else {
            processNonTateChuYoko(text, start, end, textOrientation, result)
            return result.toTypedArray()
        }

        var spanI = 0
        while (spanI < end) {
            val next = spanned.nextSpanTransition(spanI, end, VerticalLayout.TextCombineUprightSpan::class.java)

            val hasSpan = spanned.getSpans(spanI, next, VerticalLayout.TextCombineUprightSpan::class.java).isNotEmpty()

            if (hasSpan) {
                result.add(DrawOrientationRun(DrawOrientation.TateChuYoko, next - spanI))
            } else {
                processNonTateChuYoko(text, spanI, next, textOrientation, result)
            }

            spanI = next
        }


        return result.toTypedArray()
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
                result.add(DrawOrientationRun(prevProp, i - prevStart))
                prevProp = prop
                prevStart = i
            }

            i = grIter.following(i)
        }

        result.add(DrawOrientationRun(prevProp, end - prevStart))
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