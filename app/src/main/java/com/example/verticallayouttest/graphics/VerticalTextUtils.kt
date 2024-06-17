package com.example.verticallayouttest.graphics

import android.icu.lang.UCharacter
import android.icu.lang.UProperty
import java.util.Collections

object VerticalTextUtils {

    @JvmInline
    value class VerticalOrientationRun(val bits: Int) {
        constructor(orientation:Int, length: Int) : this(pack(orientation, length))
        val orientation: Int
            get() = bits and 0b11
        val length: Int
            get() = bits ushr 2

        companion object {
            fun pack(orientation:Int, length: Int): Int = (length shl 2) or orientation
        }
    }

    fun analyzeVerticalOrientation(
        text: CharSequence,
        start: Int = 0,
        end: Int = text.length
    ): Array<VerticalOrientationRun> {

        val result = mutableListOf<VerticalOrientationRun>()
        if (start == end) {
            result.toTypedArray()
        }

        var prevProp = -1
        var prevStart = -1
        var i = start
        while (i < end) {
            val cp = Character.codePointAt(text, i)

            val prop = UCharacter.getIntPropertyValue(cp, UProperty.VERTICAL_ORIENTATION)
            if (i == start) {
                prevProp = prop
                prevStart = 0
            } else if (prevProp != prop) {
                result.add(VerticalOrientationRun(prevProp, i - prevStart))
                prevProp = prop
                prevStart = i
            }

            i += Character.charCount(cp)
        }

        result.add(VerticalOrientationRun(prevProp, end - prevStart))
        return result.toTypedArray()
    }
}