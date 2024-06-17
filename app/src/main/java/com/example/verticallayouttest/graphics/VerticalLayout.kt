package com.example.verticallayouttest.graphics

import android.graphics.Paint
import android.graphics.fonts.Font
import android.graphics.fonts.SystemFonts

class VerticalLayout {

    enum class TextOrientation {
        Mixed,
        Upright,
        Sideways,
    }

    class TextCombineUprightSpan
    class RubySpan(val text: String)

    companion object {
        fun build(
            text: CharSequence,
            start: Int = 0,
            end: Int = text.length,
            textOrientation: TextOrientation = TextOrientation.Mixed,
            height: Float,
            font: Font? = null,
            paint: Paint
        ) {
            val font = font ?: SystemFonts.getAvailableFonts()
                .filterNotNull()
                .first {
                    var foundLocale = false
                    for (i in 0 until it.localeList.size()) {
                        if (it.localeList.get(i).language == paint.textLocale.language) {
                            foundLocale = true;
                        }
                    }
                    foundLocale
                }

        }
    }


}