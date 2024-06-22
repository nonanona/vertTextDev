package com.example.verticallayouttest.graphics

import android.graphics.Paint
import android.graphics.fonts.Font
import android.graphics.fonts.SystemFonts
import android.icu.lang.UCharacter
import android.icu.lang.UProperty
import android.os.LocaleList
import java.util.Locale

enum class TextOrientation {
    Mixed,
    Upright,
    Sideways,
}

class TextCombineUprightSpan
class RubySpan(
    val text: CharSequence,
    val orientation: TextOrientation = TextOrientation.Mixed,
    val textScale: Float = 0.5f
)
