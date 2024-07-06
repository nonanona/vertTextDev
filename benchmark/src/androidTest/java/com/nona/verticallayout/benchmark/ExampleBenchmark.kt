package com.nona.verticallayout.benchmark

import android.graphics.Paint
import android.util.Log
import androidx.benchmark.junit4.BenchmarkRule
import androidx.benchmark.junit4.measureRepeated
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.nona.verticallayout.graphics.OpenTypeUtils
import com.nona.verticallayout.graphics.TextOrientation
import com.nona.verticallayout.graphics.VerticalLayout
import com.nona.verticallayout.graphics.VerticalTextMeasure
import com.nona.verticallayout.utils.HtmlUtil
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Locale

/**
 * Benchmark, which will execute on an Android device.
 *
 * The body of [BenchmarkRule.measureRepeated] is measured in a loop, and Studio will
 * output the result. Modify your code to see how it affects performance.
 */
@RunWith(AndroidJUnit4::class)
class ExampleBenchmark {

    @get:Rule
    val benchmarkRule = BenchmarkRule()

    @Test
    fun parseHtml() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        benchmarkRule.measureRepeated {
            HtmlUtil.parseAsset(context, "wagahaihanekodearu.txt")
        }
    }

    @Test
    fun end2end() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val spanned = HtmlUtil.parseAsset(context, "wagahaihanekodearu.txt")
        val paint = Paint().apply {
            textSize = 48f
        }
        benchmarkRule.measureRepeated {
            VerticalLayout.build(spanned, 0, spanned.length, TextOrientation.Mixed, paint, 2048f)
        }
    }

    @Test
    fun getGlyphId() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val spanned = HtmlUtil.parseAsset(context, "wagahaihanekodearu.txt")
        val vMeasure = VerticalTextMeasure(Locale.JAPANESE)
        val ot = OpenTypeUtils.parse(vMeasure.verticalFont.buffer, vMeasure.verticalFont.ttcIndex)
        benchmarkRule.measureRepeated {
            spanned.codePoints().forEach { cp ->
                ot.charMap.getGlyphId(cp)
            }
        }
    }
}