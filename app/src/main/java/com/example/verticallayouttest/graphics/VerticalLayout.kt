package com.example.verticallayouttest.graphics

import android.graphics.Paint
import android.graphics.fonts.Font
import android.graphics.fonts.SystemFonts
import android.icu.lang.UCharacter
import android.icu.lang.UProperty
import android.os.LocaleList
import java.util.Locale

class VerticalLayout {

    enum class TextOrientation {
        Mixed,
        Upright,
        Sideways,
    }

    class TextCombineUprightSpan
    class RubySpan(val text: String)

    companion object {
        private fun findFont(locale: Locale) = SystemFonts.getAvailableFonts()
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

        fun build(
            text: CharSequence,
            start: Int = 0,
            end: Int = text.length,
            textOrientation: TextOrientation = TextOrientation.Mixed,
            height: Float,
            font: Font? = null,
            paint: Paint
        ) {

        }
    }


}